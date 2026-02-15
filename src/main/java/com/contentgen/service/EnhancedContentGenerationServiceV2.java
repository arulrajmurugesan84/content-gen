package com.contentgen.service;

import com.contentgen.config.EnhancedContentGenerationConfig;
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
 * Enhanced content generation service with:
 * - Conditional address selection based on type (MAILING preferred over PHYSICAL)
 * - Custom formatters (date, currency, number) configured in mapping
 * - Array support for Handlebars looping
 * - Custom calculations (sum, average, etc.)
 */
public class EnhancedContentGenerationServiceV2 {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedContentGenerationServiceV2.class);

    private final ObjectMapper objectMapper;
    private final Handlebars handlebars;
    private final EnhancedContentGenerationConfig config;
    private final EnhancedDataSourceResolver dataSourceResolver;
    private final FormatterService formatterService;
    private final EnhancedPlaceholderResolver placeholderResolver;
    private final String basePath;

    public EnhancedContentGenerationServiceV2(String configFilePath, String basePath)
            throws IOException {
        this.basePath = basePath;
        this.objectMapper = createObjectMapper();
        this.handlebars = new Handlebars();

        // Load enhanced configuration
        this.config = loadConfiguration(configFilePath);
        logger.info("Loaded enhanced configuration version: {}", config.configVersion());

        // Initialize data source resolver
        this.dataSourceResolver = new EnhancedDataSourceResolver(
                objectMapper,
                config.dataSources(),
                config.options().cacheDataSources(),
                basePath
        );

        // Initialize formatter service
        this.formatterService = new FormatterService(
                config.formatters(),
                config.options().enableFormatters()
        );

        // Initialize enhanced placeholder resolver
        this.placeholderResolver = new EnhancedPlaceholderResolver(
                dataSourceResolver,
                config.mappings(),
                formatterService,
                config.options()
        );

        // Preload data sources if caching is enabled
        if (config.options().cacheDataSources()) {
            dataSourceResolver.preloadDataSources();
        }

        logger.info("Enhanced service initialized with formatters and conditional selection");
    }

    /**
     * Create optimized ObjectMapper for JSON processing
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    /**
     * Load enhanced configuration from file
     */
    private EnhancedContentGenerationConfig loadConfiguration(String configFilePath)
            throws IOException {
        Path configPath = Paths.get(basePath, configFilePath);

        if (!Files.exists(configPath)) {
            throw new IOException("Configuration file not found: " + configPath);
        }

        String configContent = Files.readString(configPath);
        return objectMapper.readValue(configContent, EnhancedContentGenerationConfig.class);
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

        // Resolve all placeholders (with conditional selection, formatters, calculations)
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
    public String generateContentWithContext(
            String templateFilePath,
            Map<String, Object> additionalContext) throws IOException {

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
            context.putAll(additionalContext);
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
                "formattersEnabled", config.options().enableFormatters(),
                "conditionalSelectionEnabled", config.options().enableConditionalSelection(),
                "cacheStats", dataSourceResolver.getCacheStats()
        );
    }
}