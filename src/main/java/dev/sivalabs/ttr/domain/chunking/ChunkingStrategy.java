package dev.sivalabs.ttr.domain.chunking;

import java.util.List;

public interface ChunkingStrategy {
    boolean supports(String fileExtension);
    List<CodeChunk> chunk(String content, String filePath);
}
