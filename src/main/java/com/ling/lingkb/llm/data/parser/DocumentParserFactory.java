package com.ling.lingkb.llm.data.parser;

import com.ling.lingkb.entity.CodeHint;
import com.ling.lingkb.entity.LingDocument;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Document Parser Factory
 * <p>
 * Responsible for assigning appropriate parsers based on file types, supporting automatic type detection
 * and batch parsing operations.
 * </p>
 *
 * @author shipotian
 * @version 1.0.0
 */
@Slf4j
@Component
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;
    private final Map<String, DocumentParser> parserCache = new ConcurrentHashMap<>();

    @Autowired
    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers.stream().sorted(Comparator.comparingInt(DocumentParser::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Gets the appropriate parser for the given file extension (with caching)
     *
     * @param extension the file extension to look up
     * @return the document parser instance
     */
    private DocumentParser getParser(String extension) {
        String lowerExt = extension.toLowerCase();
        return parserCache.computeIfAbsent(lowerExt, ext -> parsers.stream().filter(p -> p.supports(ext)).findFirst()
                .orElseThrow(() -> new RuntimeException("No parser found for extension: " + ext)));
    }

    /**
     * Automatically detects file type and parses the document
     *
     * @param file the file to parse
     * @return the parsing result
     * @throws Exception if parsing fails
     */
    @CodeHint
    public LingDocument parse(File file) throws Exception {
        Path path = file.toPath();
        String extension = getFileExtension(path);
        DocumentParser parser = getParser(extension);
        log.debug("Using parser [{}] for file: {}", parser.getClass().getSimpleName(), file.getName());
        LingDocument lingDocument = parser.parse(path);
        lingDocument.setSize(Files.size(path));
        return lingDocument;
    }

    @CodeHint
    public List<LingDocument> parseUrl(String url, String type) throws Exception {
        if (StringUtils.equalsIgnoreCase("serverPath", type)) {
            return batchParse(new File(url));
        } else {
            DocumentParser parser = getParser(type);
            log.debug("Using parser [{}] for url: {}", parser.getClass().getSimpleName(), url);
            return parser.parse(url);
        }
    }

    /**
     * Batch parses files/directories
     *
     * @param file the file or directory to parse
     * @return list of parsing results
     * @throws Exception if batch parsing fails
     */
    @CodeHint
    private List<LingDocument> batchParse(File file) throws Exception {
        if (file.isDirectory()) {
            return Arrays.stream(Objects.requireNonNull(file.listFiles())).parallel().flatMap(f -> {
                try {
                    return batchParse(f).stream();
                } catch (Exception e) {
                    log.warn("Failed to parse file: {}", f.getName(), e);
                    return Stream.empty();
                }
            }).collect(Collectors.toList());
        }
        return Collections.singletonList(parse(file));
    }

    /**
     * Gets the file extension (supports MIME type detection for extensionless files)
     *
     * @param path the file path
     * @return the detected file extension
     * @throws IOException if MIME type detection fails
     */
    private String getFileExtension(Path path) throws IOException {
        String filename = path.getFileName().toString();
        String extension = FilenameUtils.getExtension(filename);

        if (StringUtils.isBlank(extension)) {
            log.info("Could not detected file type for {}", filename);
            throw new IOException();
        }

        return extension.toLowerCase();
    }

    /**
     * Gets all supported file types
     *
     * @return set of supported file extensions
     */
    public Set<String> getSupportedTypes() {
        return parsers.stream().flatMap(p -> p.supportedTypes().stream()).collect(Collectors.toSet());
    }
}