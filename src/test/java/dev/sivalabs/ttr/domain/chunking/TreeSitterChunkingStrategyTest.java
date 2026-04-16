package dev.sivalabs.ttr.domain.chunking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TreeSitterChunkingStrategyTest {

    private final TreeSitterChunkingStrategy strategy = new TreeSitterChunkingStrategy();

    @ParameterizedTest
    @ValueSource(strings = {"java", "kt", "kts", "py", "js", "jsx", "ts", "tsx", "go"})
    void supportsCodeLanguages(String ext) {
        assertThat(strategy.supports(ext)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"xml", "sql", "yml", "yaml", "properties", "md", "txt", "html", "css"})
    void doesNotSupportNonCodeLanguages(String ext) {
        assertThat(strategy.supports(ext)).isFalse();
    }

    @Test
    void producesMethodLevelChunksForJava() {
        String source = """
                public class Greeter {
                    public String hello(String name) {
                        return "Hello " + name;
                    }
                    public String goodbye(String name) {
                        return "Goodbye " + name;
                    }
                }
                """;

        List<CodeChunk> chunks = strategy.chunk(source, "Greeter.java");

        assertThat(chunks).hasSize(2);
        assertThat(chunks.stream().map(CodeChunk::symbolName))
                .containsExactlyInAnyOrder("hello", "goodbye");
        chunks.forEach(c -> assertThat(c.chunkType()).isEqualTo("method"));
    }

    @Test
    void returnsEmptyListForBinaryContent() {
        assertThatNoException().isThrownBy(() -> {
            List<CodeChunk> chunks = strategy.chunk("\u0000\u0001binary", "file.java");
            assertThat(chunks).isNotNull();
        });
    }

    @Test
    void chunkHasCorrectFilePath() {
        String source = "public class Foo { public void bar() {} }";
        List<CodeChunk> chunks = strategy.chunk(source, "src/main/Foo.java");

        assertThat(chunks).isNotEmpty();
        chunks.forEach(c -> assertThat(c.filePath()).isEqualTo("src/main/Foo.java"));
    }
}
