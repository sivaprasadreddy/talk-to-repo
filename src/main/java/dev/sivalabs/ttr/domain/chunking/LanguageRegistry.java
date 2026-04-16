package dev.sivalabs.ttr.domain.chunking;

import org.treesitter.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

class LanguageRegistry {

    private static final Map<String, TSLanguage> LANGUAGES = Map.of(
            "java",   new TreeSitterJava(),
            "kt",     new TreeSitterKotlin(),
            "kts",    new TreeSitterKotlin(),
            "py",     new TreeSitterPython(),
            "js",     new TreeSitterJavascript(),
            "jsx",    new TreeSitterJavascript(),
            "ts",     new TreeSitterTypescript(),
            "tsx",    new TreeSitterTypescript(),
            "go",     new TreeSitterGo()
    );

    static final Set<String> SUPPORTED_EXTENSIONS = LANGUAGES.keySet();

    static Optional<TSLanguage> forExtension(String ext) {
        return Optional.ofNullable(LANGUAGES.get(ext.toLowerCase()));
    }
}
