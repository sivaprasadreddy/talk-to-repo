package dev.sivalabs.ttr.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepoChatService {

    private static final Logger log = LoggerFactory.getLogger(RepoChatService.class);
    private static final int TOP_K = 6;
    private static final int MAX_MESSAGES = 20;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    public RepoChatService(ChatClient.Builder chatClientBuilder,
                           VectorStore vectorStore,
                           JdbcChatMemoryRepository chatMemoryRepository) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(MAX_MESSAGES)
                .build();
    }

    public String chat(GitRepo repo, String question, String conversationId) {
        log.info("Chat request for repo {} (id={}) [conv={}]: {}", repo.getRepoName(), repo.getId(), conversationId, question);
        String filter = "repoId == " + repo.getId();

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(TOP_K)
                        .filterExpression(filter)
                        .build()
        );

        if (docs.isEmpty()) {
            log.warn("No relevant documents found for repo {} on question: {}", repo.getRepoName(), question);
            return "I could not find relevant information about that in this repository's indexed content.";
        }

        log.debug("Found {} relevant documents for repo {}", docs.size(), repo.getRepoName());
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        log.debug("Sending chat prompt for repo {} with memory advisor [conv={}]", repo.getRepoName(), conversationId);
        return chatClient.prompt()
                .system(buildSystemPrompt(repo.getRepoName()))
                .user(buildUserMessage(question, context))
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .call()
                .content();
    }

    private String buildSystemPrompt(String repoName) {
        return """
                You are a code assistant. Answer questions about the "%s" repository \
                using ONLY the source code excerpts provided in each message.
                If the excerpts do not contain enough information to answer the question fully, \
                say "I don't have enough information in the indexed content to answer that." \
                and briefly explain what was found, if anything.
                Do NOT invent, assume, or infer any code, APIs, classes, methods, or features \
                that are not explicitly present in the excerpts.
                """.formatted(repoName);
    }

    private String buildUserMessage(String question, String context) {
        return """
                ### Relevant source code excerpts:
                %s

                ### Question:
                %s
                """.formatted(context, question);
    }
}
