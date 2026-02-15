package com.contentgen.service;

import com.contentgen.config.ContentGenerationConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Main service for generating content from Handlebars templates with data from multiple JSON sources
 */
public class ContentGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ContentGenerationService.class);

    private final ObjectMapper objectMapper;
    private final Handlebars handlebars;
    private final ContentGenerationConfig config;
    private final DataSourceResolver dataSourceResolver;
    private final PlaceholderResolver placeholderResolver;
    private final String basePath;

    public ContentGenerationService(String configFilePath, String basePath) throws IOException {
        this.basePath = basePath;
        this.objectMapper = createObjectMapper();
        this.handlebars = new Handlebars();

        // Load configuration
        this.config = loadConfiguration(configFilePath);
        logger.info("Loaded configuration version: {}", config.configVersion());

        // Initialize resolvers
        this.dataSourceResolver = new DataSourceResolver(
                objectMapper,
                config.dataSources(),
                config.options().cacheDataSources(),
                basePath
        );

        this.placeholderResolver = new PlaceholderResolver(
                dataSourceResolver,
                config.mappings(),
                config.options()
        );

        // Preload data sources if caching is enabled
        if (config.options().cacheDataSources()) {
            dataSourceResolver.preloadDataSources();
        }
    }

    /**
     * Create optimized ObjectMapper for JSON processing
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Register JSR-310 module for date/time
        return mapper;
    }

    /**
     * Load configuration from file
     */
    private ContentGenerationConfig loadConfiguration(String configFilePath) throws IOException {
        Path configPath = Paths.get(basePath, configFilePath);

        if (!Files.exists(configPath)) {
            throw new IOException("Configuration file not found: " + configPath);
        }

        String configContent = Files.readString(configPath);
        return objectMapper.readValue(configContent, ContentGenerationConfig.class);
    }

    /**
     * Generate content from a template file
     */
    public String generateContent(String templateFilePath) throws IOException {
        logger.info("Generating content from template: {}", templateFilePath);

        // Load template
        Path templatePath = Paths.get(basePath, templateFilePath);
        if (!Files.exists(templatePath)) {
            throw new IOException("Template file not found: " + templatePath);
        }

        String templateContent = Files.readString(templatePath);

        // Resolve all placeholders
        Map<String, Object> context = placeholderResolver.resolveAllPlaceholders();

        // Compile and apply template
        Template template = handlebars.compileInline(templateContent);
        String result = template.apply(context);

        logger.info("Successfully generated content");
        return result;
    }

    /**
     * Generate content from template string
     */
    public String generateContentFromString(String templateString) throws IOException {
        logger.info("Generating content from template string");

        // Resolve all placeholders
        Map<String, Object> context = placeholderResolver.resolveAllPlaceholders();

        // Compile and apply template
        Template template = handlebars.compileInline(templateString);
        String result = template.apply(context);

        logger.info("Successfully generated content");
        return result;
    }

    /**
     * Generate content and return as JsonNode
     */
    public JsonNode generateContentAsJson(String templateFilePath) throws IOException {
        String content = generateContent(templateFilePath);
        return objectMapper.readTree(content);
    }

    /**
     * Generate content with custom context (merged with resolved placeholders)
     */
    public String generateContentWithContext(String templateFilePath, Map<String, Object> additionalContext)
            throws IOException {
        logger.info("Generating content with additional context");

        // Load template
        Path templatePath = Paths.get(basePath, templateFilePath);
        if (!Files.exists(templatePath)) {
            throw new IOException("Template file not found: " + templatePath);
        }

        String templateContent = Files.readString(templatePath);

        // Resolve placeholders and merge with additional context
        Map<String, Object> context = placeholderResolver.resolveAllPlaceholders();
        if (additionalContext != null) {
            context.putAll(additionalContext); // Additional context overrides
        }

        // Compile and apply template
        Template template = handlebars.compileInline(templateContent);
        String result = template.apply(context);

        logger.info("Successfully generated content with custom context");
        return result;
    }

    /**
     * Get resolved context (useful for debugging)
     */
    public Map<String, Object> getResolvedContext() throws IOException {
        return placeholderResolver.resolveAllPlaceholders();
    }

    /**
     * Clear data source cache
     */
    public void clearCache() {
        dataSourceResolver.clearCache();
    }

    /**
     * Get service statistics
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
                "configVersion", config.configVersion(),
                "dataSourceCount", config.dataSources().size(),
                "mappingCount", config.mappings().size(),
                "cachingEnabled", config.options().cacheDataSources(),
                "cacheStats", dataSourceResolver.getCacheStats()
        );
    }
}