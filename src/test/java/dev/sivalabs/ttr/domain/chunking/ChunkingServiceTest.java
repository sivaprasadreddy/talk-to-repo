package dev.sivalabs.ttr.domain.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceTest {

    private final ChunkingService service = new ChunkingService(
            List.of(new TreeSitterChunkingStrategy(), new TokenSplitterChunkingStrategy()));

    @Test
    void usesTreeSitterForJavaFile() {
        String source = """
                public class Calc {
                    public int add(int a, int b) { return a + b; }
                    public int sub(int a, int b) { return a - b; }
                }
                """;

        List<CodeChunk> chunks = service.chunk(source, "Calc.java");

        assertThat(chunks).hasSize(2);
        assertThat(chunks.stream().map(CodeChunk::symbolName))
                .containsExactlyInAnyOrder("add", "sub");
        chunks.forEach(c -> assertThat(c.chunkType()).isEqualTo("method"));
    }

    @Test
    void fallsBackToTokenSplitterForXmlFile() {
        String source = """
                <beans>
                    <bean id="foo" class="com.example.Foo"/>
                </beans>
                """;

        List<CodeChunk> chunks = service.chunk(source, "config.xml");

        assertThat(chunks).isNotEmpty();
        chunks.forEach(c -> assertThat(c.chunkType()).isEqualTo("file"));
    }

    @Test
    void fallsBackToTokenSplitterForUnknownExtension() {
        List<CodeChunk> chunks = service.chunk("some content", "notes.txt");

        assertThat(chunks).isNotEmpty();
    }
}
