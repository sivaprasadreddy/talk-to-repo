package dev.sivalabs.ttr.domain.chunking;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class TreeSitterChunkingStrategy implements ChunkingStrategy {

    private final AstChunkExtractor extractor = new AstChunkExtractor();

    @Override
    public boolean supports(String fileExtension) {
        return LanguageRegistry.SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }

    @Override
    public List<CodeChunk> chunk(String content, String filePath) {
        String ext = extensionOf(filePath);
        return extractor.extract(content, filePath, ext);
    }

    private static String extensionOf(String filePath) {
        int dot = filePath.lastIndexOf('.');
        return (dot >= 0) ? filePath.substring(dot + 1).toLowerCase() : "";
    }
}
