package dev.sivalabs.ttr.domain;

import dev.sivalabs.ttr.domain.chunking.ChunkingService;
import dev.sivalabs.ttr.domain.chunking.CodeChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RepoIngestionService {

    private static final Logger log = LoggerFactory.getLogger(RepoIngestionService.class);
    private static final long MAX_FILE_BYTES = 100 * 1024L;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".java", ".kt", ".groovy", ".scala",
            ".xml", ".yaml", ".yml", ".properties", ".toml",
            ".json", ".sql", ".md", ".txt",
            ".js", ".ts", ".jsx", ".tsx",
            ".py", ".rb", ".go", ".rs", ".c", ".cpp", ".h",
            ".html", ".css", ".sh"
    );

    private final VectorStore vectorStore;
    private final GitRepoRepository gitRepoRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ChunkingService chunkingService;

    public RepoIngestionService(VectorStore vectorStore, GitRepoRepository gitRepoRepository,
                                JdbcTemplate jdbcTemplate, ChunkingService chunkingService) {
        this.vectorStore = vectorStore;
        this.gitRepoRepository = gitRepoRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.chunkingService = chunkingService;
    }

    @Transactional
    public void ingestIfNeeded(GitRepo repo) {
        if (repo.getIngestedAt() != null) {
            log.info("Repo {} already ingested at {}, skipping", repo.getRepoName(), repo.getIngestedAt());
            return;
        }
        ingest(repo);
    }

    @Transactional
    public void reingest(GitRepo repo) {
        log.info("Re-ingesting repo {} (id={})", repo.getRepoName(), repo.getId());
        int deleted = jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'repoId' = ?",
                repo.getId().toString());
        log.info("Deleted {} existing embeddings for repo {}", deleted, repo.getRepoName());
        repo.setIngestedAt(null);
        ingest(repo);
    }

    @Transactional
    public void ingest(GitRepo repo) {
        log.info("Starting ingestion for repo {} at {}", repo.getRepoName(), repo.getLocalPath());
        List<Document> documents = collectDocuments(repo);
        log.info("Collected {} files for repo {}", documents.size(), repo.getRepoName());
        if (documents.isEmpty()) {
            log.warn("No source files found for repo {}", repo.getRepoName());
        } else {
            log.info("Chunking {} files for repo {}", documents.size(), repo.getRepoName());
            List<Document> chunks = documents.stream()
                    .flatMap(rawDoc -> {
                        String filePath = (String) rawDoc.getMetadata().get("filePath");
                        List<CodeChunk> codeChunks = chunkingService.chunk(rawDoc.getText(), filePath);
                        return codeChunks.stream().map(c -> toDocument(c, repo.getId()));
                    })
                    .toList();
            log.info("Adding {} chunks to vector store for repo {}", chunks.size(), repo.getRepoName());
            vectorStore.add(chunks);
            log.info("Ingested {} chunks from {} files for repo {}", chunks.size(), documents.size(), repo.getRepoName());
        }
        repo.setIngestedAt(LocalDateTime.now());
        gitRepoRepository.save(repo);
        log.info("Ingestion complete for repo {}", repo.getRepoName());
    }

    private List<Document> collectDocuments(GitRepo repo) {
        List<Document> docs = new ArrayList<>();
        Path repoRoot = Path.of(repo.getLocalPath());

        try {
            Files.walkFileTree(repoRoot, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName().toString();
                    if (name.equals(".git") || name.startsWith(".") || name.equals("node_modules")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!isSupportedFile(file, attrs)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String content = Files.readString(file);
                    String relativePath = repoRoot.relativize(file).toString();
                    log.debug("Collected file: {}", relativePath);
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("repoId", repo.getId());
                    metadata.put("filePath", relativePath);
                    docs.add(new Document(content, metadata));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk repo directory: " + repoRoot, e);
        }
        return docs;
    }

    private boolean isSupportedFile(Path file, BasicFileAttributes attrs) {
        if (attrs.size() > MAX_FILE_BYTES) return false;
        String name = file.getFileName().toString().toLowerCase();
        if (name.equals("dockerfile") || name.startsWith("dockerfile.")) return true;
        int dot = name.lastIndexOf('.');
        return dot >= 0 && SUPPORTED_EXTENSIONS.contains(name.substring(dot));
    }

    private Document toDocument(CodeChunk chunk, Long repoId) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("repoId", repoId);
        meta.put("filePath", chunk.filePath());
        meta.put("language", chunk.language());
        meta.put("chunkType", chunk.chunkType());
        if (chunk.symbolName() != null) meta.put("symbolName", chunk.symbolName());
        if (chunk.className() != null) meta.put("className", chunk.className());
        meta.put("startLine", chunk.startLine());
        meta.put("endLine", chunk.endLine());
        if (chunk.javadoc() != null) meta.put("javadoc", chunk.javadoc());
        if (chunk.annotations() != null && !chunk.annotations().isEmpty()) {
            meta.put("annotations", String.join(",", chunk.annotations()));
        }
        return new Document(buildChunkText(chunk), meta);
    }

    private String buildChunkText(CodeChunk chunk) {
        StringBuilder sb = new StringBuilder();
        sb.append("// File: ").append(chunk.filePath());
        if (chunk.className() != null) sb.append(" [class: ").append(chunk.className()).append("]");
        if (chunk.symbolName() != null) {
            sb.append(" [").append(chunk.chunkType()).append(": ").append(chunk.symbolName()).append("]");
        }
        if (chunk.startLine() > 0) {
            sb.append(" lines ").append(chunk.startLine()).append("-").append(chunk.endLine());
        }
        sb.append("\n");
        if (chunk.annotations() != null && !chunk.annotations().isEmpty()) {
            sb.append("// Annotations: ").append(String.join(", ", chunk.annotations())).append("\n");
        }
        if (chunk.javadoc() != null) {
            String summary = chunk.javadoc().lines().findFirst().orElse("").strip();
            sb.append("// ").append(summary).append("\n");
        }
        sb.append("\n").append(chunk.content());
        return sb.toString();
    }
}
