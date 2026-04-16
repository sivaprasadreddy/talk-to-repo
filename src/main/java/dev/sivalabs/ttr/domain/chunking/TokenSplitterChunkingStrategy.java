package dev.sivalabs.ttr.domain.chunking;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(2)
public class TokenSplitterChunkingStrategy implements ChunkingStrategy {

    private static final TokenTextSplitter SPLITTER = TokenTextSplitter.builder().build();

    @Override
    public boolean supports(String fileExtension) {
        return true; // catch-all fallback
    }

    @Override
    public List<CodeChunk> chunk(String content, String filePath) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String ext = extensionOf(filePath);
        List<Document> docs = SPLITTER.apply(List.of(new Document(content, Map.of("filePath", filePath))));
        return docs.stream()
                .map(doc -> new CodeChunk(
                        doc.getFormattedContent(),
                        filePath,
                        ext,
                        "file",
                        null,
                        null,
                        0, 0,
                        List.of(),
                        null))
                .toList();
    }

    private static String extensionOf(String filePath) {
        int dot = filePath.lastIndexOf('.');
        return (dot >= 0) ? filePath.substring(dot + 1).toLowerCase() : "";
    }
}
