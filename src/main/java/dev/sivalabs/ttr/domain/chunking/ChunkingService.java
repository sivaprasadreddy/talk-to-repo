package dev.sivalabs.ttr.domain.chunking;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChunkingService {

    private final List<ChunkingStrategy> strategies;

    public ChunkingService(List<ChunkingStrategy> strategies) {
        this.strategies = strategies;
    }

    public List<CodeChunk> chunk(String content, String filePath) {
        String ext = extensionOf(filePath);
        return strategies.stream()
                .filter(s -> s.supports(ext))
                .findFirst()
                .map(s -> s.chunk(content, filePath))
                .orElse(List.of());
    }

    private static String extensionOf(String filePath) {
        int dot = filePath.lastIndexOf('.');
        return (dot >= 0) ? filePath.substring(dot + 1).toLowerCase() : "";
    }
}
