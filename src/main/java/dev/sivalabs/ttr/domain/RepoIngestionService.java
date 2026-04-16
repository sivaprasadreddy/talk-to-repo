package dev.sivalabs.ttr.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
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

    public RepoIngestionService(VectorStore vectorStore, GitRepoRepository gitRepoRepository) {
        this.vectorStore = vectorStore;
        this.gitRepoRepository = gitRepoRepository;
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
    public void ingest(GitRepo repo) {
        List<Document> documents = collectDocuments(repo);
        if (documents.isEmpty()) {
            log.warn("No source files found for repo {}", repo.getRepoName());
        } else {
            List<Document> chunks = TokenTextSplitter.builder().build().apply(documents);
            vectorStore.add(chunks);
            log.info("Ingested {} chunks from {} files for repo {}", chunks.size(), documents.size(), repo.getRepoName());
        }
        repo.setIngestedAt(LocalDateTime.now());
        gitRepoRepository.save(repo);
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
}
