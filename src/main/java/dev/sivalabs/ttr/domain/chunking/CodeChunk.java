package dev.sivalabs.ttr.domain.chunking;

import java.util.List;

public record CodeChunk(
        String content,
        String filePath,
        String language,
        String chunkType,
        String symbolName,
        String className,
        int startLine,
        int endLine,
        List<String> annotations,
        String javadoc
) {}
