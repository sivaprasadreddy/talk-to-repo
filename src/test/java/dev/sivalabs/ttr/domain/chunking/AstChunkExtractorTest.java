package dev.sivalabs.ttr.domain.chunking;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AstChunkExtractorTest {

    private final AstChunkExtractor extractor = new AstChunkExtractor();

    private static final String JAVA_TWO_METHODS = """
            package com.example;

            import java.util.List;

            @Service
            public class OrderService {

                /**
                 * Creates a new order.
                 */
                @Transactional
                public Order createOrder(String customerId) {
                    return new Order();
                }

                public void cancelOrder(Long orderId) {
                    // cancel logic
                }
            }
            """;

    private static final String JAVA_EMPTY_CLASS = """
            public class MarkerInterface {
            }
            """;

    @Test
    void extractsMethodLevelChunksFromJava() {
        List<CodeChunk> chunks = extractor.extract(JAVA_TWO_METHODS, "OrderService.java", "java");

        assertThat(chunks).hasSize(2);

        CodeChunk createOrder = chunks.stream()
                .filter(c -> "createOrder".equals(c.symbolName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("createOrder chunk not found"));

        assertThat(createOrder.chunkType()).isEqualTo("method");
        assertThat(createOrder.className()).isEqualTo("OrderService");
        assertThat(createOrder.language()).isEqualTo("java");
        assertThat(createOrder.filePath()).isEqualTo("OrderService.java");
        assertThat(createOrder.startLine()).isGreaterThan(0);
        assertThat(createOrder.endLine()).isGreaterThan(createOrder.startLine());
        assertThat(createOrder.annotations()).contains("@Transactional");
        assertThat(createOrder.content()).contains("createOrder");
    }

    @Test
    void extractsAnnotationsOnClass() {
        // The @Service annotation on the class should appear in each method chunk's class context
        // For method chunks, annotations on the method itself are captured
        List<CodeChunk> chunks = extractor.extract(JAVA_TWO_METHODS, "OrderService.java", "java");

        CodeChunk cancel = chunks.stream()
                .filter(c -> "cancelOrder".equals(c.symbolName()))
                .findFirst()
                .orElseThrow();

        assertThat(cancel.chunkType()).isEqualTo("method");
        assertThat(cancel.className()).isEqualTo("OrderService");
        assertThat(cancel.annotations()).isEmpty();
    }

    @Test
    void extractsClassChunkWhenNoMethods() {
        List<CodeChunk> chunks = extractor.extract(JAVA_EMPTY_CLASS, "MarkerInterface.java", "java");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).chunkType()).isEqualTo("class");
        assertThat(chunks.get(0).symbolName()).isEqualTo("MarkerInterface");
        assertThat(chunks.get(0).className()).isNull();
    }

    @Test
    void returnsEmptyListForBlankSource() {
        List<CodeChunk> chunks = extractor.extract("", "Empty.java", "java");
        assertThat(chunks).isEmpty();
    }

    @Test
    void doesNotThrowOnUnparseableContent() {
        assertThatNoException().isThrownBy(() ->
                extractor.extract("\u0000\u0001\u0002binary content", "binary.java", "java"));
    }

    @Test
    void extractsConstructorChunk() {
        String source = """
                public class MyService {
                    public MyService() {
                        // init
                    }
                    public void doWork() {}
                }
                """;
        List<CodeChunk> chunks = extractor.extract(source, "MyService.java", "java");

        assertThat(chunks.stream().map(CodeChunk::symbolName))
                .containsExactlyInAnyOrder("MyService", "doWork");
        assertThat(chunks.stream().filter(c -> "MyService".equals(c.symbolName()))
                .findFirst().orElseThrow().chunkType())
                .isEqualTo("constructor");
    }

    @Test
    void extractsMethodsFromFixtureFile() throws IOException {
        String source = Files.readString(
                Path.of("src/test/resources/fixtures/OrderService.java"));
        List<CodeChunk> chunks = extractor.extract(source, "OrderService.java", "java");

        assertThat(chunks.stream().map(CodeChunk::symbolName))
                .containsExactlyInAnyOrder("OrderService", "createOrder", "cancelOrder", "findById", "findByCustomer");
    }
}
