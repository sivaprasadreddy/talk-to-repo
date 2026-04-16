package dev.sivalabs.ttr.domain.chunking;

import org.treesitter.*;

import java.util.*;

/**
 * Extracts structured code chunks from source files using Tree-sitter AST parsing.
 * Produces method/function-level chunks enriched with annotations, Javadoc, and class context.
 */
public class AstChunkExtractor {

    // Java node types to capture
    private static final Set<String> JAVA_METHOD_TYPES = Set.of(
            "method_declaration", "constructor_declaration");
    private static final Set<String> JAVA_CLASS_TYPES = Set.of(
            "class_declaration", "interface_declaration", "enum_declaration",
            "annotation_type_declaration", "record_declaration");

    // Kotlin node types
    private static final Set<String> KOTLIN_FUNCTION_TYPES = Set.of("function_declaration");
    private static final Set<String> KOTLIN_CLASS_TYPES = Set.of(
            "class_declaration", "object_declaration", "interface_declaration");

    // Python node types
    private static final Set<String> PYTHON_FUNCTION_TYPES = Set.of("function_definition");
    private static final Set<String> PYTHON_CLASS_TYPES = Set.of("class_definition");

    // JS/TS node types
    private static final Set<String> JS_FUNCTION_TYPES = Set.of(
            "function_declaration", "method_definition");
    private static final Set<String> JS_CLASS_TYPES = Set.of("class_declaration");

    // Go node types
    private static final Set<String> GO_FUNCTION_TYPES = Set.of(
            "function_declaration", "method_declaration");
    private static final Set<String> GO_CLASS_TYPES = Set.of("type_declaration");

    public List<CodeChunk> extract(String source, String filePath, String language) {
        if (source == null || source.isBlank()) {
            return List.of();
        }

        Optional<TSLanguage> tsLanguage = LanguageRegistry.forExtension(language);
        if (tsLanguage.isEmpty()) {
            return List.of();
        }

        try {
            TSParser parser = new TSParser();
            parser.setLanguage(tsLanguage.get());
            TSTree tree = parser.parseString(null, source);
            TSNode root = tree.getRootNode();

            if (root.isNull() || root.hasError()) {
                // parse errors still yield a partial tree — attempt extraction anyway
            }

            List<CodeChunk> chunks = new ArrayList<>();
            traverseRoot(root, source, filePath, language, chunks);
            return chunks;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void traverseRoot(TSNode root, String source, String filePath,
                               String language, List<CodeChunk> chunks) {
        Set<String> functionTypes = functionTypesFor(language);
        Set<String> classTypes = classTypesFor(language);

        traverseNode(root, source, filePath, language, functionTypes, classTypes, null, chunks);
    }

    private void traverseNode(TSNode node, String source, String filePath, String language,
                               Set<String> functionTypes, Set<String> classTypes,
                               String enclosingClass, List<CodeChunk> chunks) {
        if (node.isNull()) return;

        String type = node.getType();

        if (functionTypes.contains(type)) {
            chunks.add(buildFunctionChunk(node, source, filePath, language, enclosingClass));
            return; // don't recurse into method body
        }

        if (classTypes.contains(type)) {
            String className = extractName(node, source);
            List<CodeChunk> inner = new ArrayList<>();
            for (int i = 0; i < node.getNamedChildCount(); i++) {
                traverseNode(node.getNamedChild(i), source, filePath, language,
                        functionTypes, classTypes, className, inner);
            }
            if (inner.isEmpty()) {
                // no methods — emit the whole class as one chunk
                chunks.add(buildClassChunk(node, source, filePath, language, className));
            } else {
                chunks.addAll(inner);
            }
            return;
        }

        for (int i = 0; i < node.getNamedChildCount(); i++) {
            traverseNode(node.getNamedChild(i), source, filePath, language,
                    functionTypes, classTypes, enclosingClass, chunks);
        }
    }

    private CodeChunk buildFunctionChunk(TSNode node, String source, String filePath,
                                          String language, String enclosingClass) {
        String symbolName = extractName(node, source);
        String chunkType = chunkTypeFor(node.getType());
        int startLine = node.getStartPoint().getRow() + 1;
        int endLine = node.getEndPoint().getRow() + 1;
        String content = extractText(node, source);
        List<String> annotations = extractAnnotations(node, source);
        String javadoc = extractJavadoc(node, source);

        return new CodeChunk(content, filePath, language, chunkType,
                symbolName, enclosingClass, startLine, endLine, annotations, javadoc);
    }

    private CodeChunk buildClassChunk(TSNode node, String source, String filePath,
                                       String language, String className) {
        int startLine = node.getStartPoint().getRow() + 1;
        int endLine = node.getEndPoint().getRow() + 1;
        String content = extractText(node, source);
        List<String> annotations = extractAnnotations(node, source);
        String javadoc = extractJavadoc(node, source);
        String chunkType = chunkTypeFor(node.getType());

        return new CodeChunk(content, filePath, language, chunkType,
                className, null, startLine, endLine, annotations, javadoc);
    }

    private String extractName(TSNode node, String source) {
        // Try field "name" first (Java, Go, Kotlin)
        TSNode nameNode = node.getChildByFieldName("name");
        if (!nameNode.isNull()) {
            return extractText(nameNode, source);
        }
        // Python uses "name" too but might differ — fall back to first identifier child
        for (int i = 0; i < node.getNamedChildCount(); i++) {
            TSNode child = node.getNamedChild(i);
            if ("identifier".equals(child.getType()) || "type_identifier".equals(child.getType())) {
                return extractText(child, source);
            }
        }
        return "unknown";
    }

    private String extractText(TSNode node, String source) {
        int start = node.getStartByte();
        int end = node.getEndByte();
        byte[] bytes = source.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (start >= bytes.length || end > bytes.length) return source;
        return new String(bytes, start, end - start, java.nio.charset.StandardCharsets.UTF_8);
    }

    private List<String> extractAnnotations(TSNode node, String source) {
        List<String> annotations = new ArrayList<>();
        // Annotations appear inside a `modifiers` child in Java/Kotlin
        for (int i = 0; i < node.getNamedChildCount(); i++) {
            TSNode child = node.getNamedChild(i);
            String childType = child.getType();
            if ("modifiers".equals(childType)) {
                for (int j = 0; j < child.getNamedChildCount(); j++) {
                    TSNode mod = child.getNamedChild(j);
                    if ("marker_annotation".equals(mod.getType()) || "annotation".equals(mod.getType())) {
                        annotations.add("@" + extractAnnotationName(mod, source));
                    }
                }
            }
        }
        return annotations;
    }

    private String extractAnnotationName(TSNode annotationNode, String source) {
        // annotation node has a `name` field or first identifier child
        TSNode nameNode = annotationNode.getChildByFieldName("name");
        if (!nameNode.isNull()) {
            return extractText(nameNode, source);
        }
        for (int i = 0; i < annotationNode.getNamedChildCount(); i++) {
            TSNode child = annotationNode.getNamedChild(i);
            if ("identifier".equals(child.getType())) {
                return extractText(child, source);
            }
        }
        return extractText(annotationNode, source).replaceAll("^@", "");
    }

    private String extractJavadoc(TSNode node, String source) {
        // Javadoc is typically the block_comment immediately preceding the node
        TSNode prev = node.getPrevNamedSibling();
        if (!prev.isNull()) {
            String prevType = prev.getType();
            if ("block_comment".equals(prevType) || "line_comment".equals(prevType)) {
                String text = extractText(prev, source).strip();
                if (text.startsWith("/**") || text.startsWith("//")) {
                    return text;
                }
            }
        }
        return null;
    }

    private String chunkTypeFor(String nodeType) {
        return switch (nodeType) {
            case "method_declaration" -> "method";
            case "constructor_declaration" -> "constructor";
            case "interface_declaration" -> "interface";
            case "enum_declaration" -> "enum";
            case "function_declaration", "method_definition", "function_definition" -> "function";
            case "method_declaration_go" -> "method";
            default -> "class";
        };
    }

    private Set<String> functionTypesFor(String language) {
        return switch (language) {
            case "java" -> JAVA_METHOD_TYPES;
            case "kt", "kts" -> KOTLIN_FUNCTION_TYPES;
            case "py" -> PYTHON_FUNCTION_TYPES;
            case "js", "jsx", "ts", "tsx" -> JS_FUNCTION_TYPES;
            case "go" -> GO_FUNCTION_TYPES;
            default -> Set.of();
        };
    }

    private Set<String> classTypesFor(String language) {
        return switch (language) {
            case "java" -> JAVA_CLASS_TYPES;
            case "kt", "kts" -> KOTLIN_CLASS_TYPES;
            case "py" -> PYTHON_CLASS_TYPES;
            case "js", "jsx", "ts", "tsx" -> JS_CLASS_TYPES;
            case "go" -> GO_CLASS_TYPES;
            default -> Set.of();
        };
    }
}
