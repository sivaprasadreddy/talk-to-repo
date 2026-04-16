package dev.sivalabs.ttr.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReadmeGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ReadmeGenerationService.class);
    private static final int TOP_K = 8;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final GitRepoRepository gitRepoRepository;

    public ReadmeGenerationService(ChatClient.Builder chatClientBuilder,
                                   VectorStore vectorStore,
                                   GitRepoRepository gitRepoRepository) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.gitRepoRepository = gitRepoRepository;
    }

    @Transactional
    public String generateReadme(GitRepo repo) {
        String filter = "repoId == " + repo.getId();

        String overviewCtx      = search("project overview purpose goals main functionality", filter);
        String techStackCtx     = search("technology stack frameworks libraries dependencies build tools", filter);
        String architectureCtx  = search("architecture modules packages layers entry points configuration", filter);

        log.info("Generating README for repo {}", repo.getRepoName());
        String readme = chatClient.prompt()
                .user(buildPrompt(repo.getRepoName(), overviewCtx, techStackCtx, architectureCtx))
                .call()
                .content();

        repo.setGeneratedReadme(readme);
        gitRepoRepository.save(repo);
        return readme;
    }

    private String search(String query, String filterExpression) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(TOP_K)
                        .filterExpression(filterExpression)
                        .build()
        );
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String buildPrompt(String repoName, String overviewCtx,
                                String techStackCtx, String architectureCtx) {
        return """
                You are a technical writer. Based ONLY on the source code excerpts provided below, \
                generate a professional README.md for the project "%s".
                Do NOT invent information that is not present in the excerpts.
                Use GitHub-flavoured Markdown. Include exactly these three sections in order:

                ## Overview
                Describe what the project does and its primary purpose.

                ## Tech Stack
                List the main languages, frameworks, libraries, and tools used.

                ## Architecture
                Describe the key modules, layers, and how they interact.

                ---

                ### Context: Project Overview
                %s

                ### Context: Technology Stack
                %s

                ### Context: Architecture
                %s

                Now generate the README.md content:
                """.formatted(repoName, overviewCtx, techStackCtx, architectureCtx);
    }
}
