package com.contentgen.service;

import com.contentgen.config.ContentGenerationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves Handlebars placeholders to actual values from configured data sources
 */
public class PlaceholderResolver {

    private static final Logger logger = LoggerFactory.getLogger(PlaceholderResolver.class);

    private final DataSourceResolver dataSourceResolver;
    private final List<ContentGenerationConfig.PlaceholderMapping> mappings;
    private final boolean strictMode;
    private final String defaultValue;
    private final boolean loggingEnabled;

    public PlaceholderResolver(
            DataSourceResolver dataSourceResolver,
            List<ContentGenerationConfig.PlaceholderMapping> mappings,
            ContentGenerationConfig.ConfigOptions options) {
        this.dataSourceResolver = dataSourceResolver;
        this.mappings = mappings;
        this.strictMode = options.strictMode();
        this.defaultValue = options.defaultValue();
        this.loggingEnabled = options.enableLogging();
    }

    /**
     * Resolve all placeholders to create a context map for Handlebars
     */
    public Map<String, Object> resolveAllPlaceholders() throws IOException {
        Map<String, Object> context = new HashMap<>();

        if (loggingEnabled) {
            logger.info("Resolving {} placeholders", mappings.size());
        }

        for (ContentGenerationConfig.PlaceholderMapping mapping : mappings) {
            String placeholder = mapping.placeholder();
            Object value = resolvePlaceholder(mapping);

            context.put(placeholder, value);

            if (loggingEnabled) {
                logger.debug("Resolved '{}' = '{}'", placeholder, value);
            }
        }

        if (loggingEnabled) {
            logger.info("Successfully resolved all placeholders");
        }

        return context;
    }

    /**
     * Resolve a single placeholder with fallback support
     */
    private Object resolvePlaceholder(ContentGenerationConfig.PlaceholderMapping mapping)
            throws IOException {

        String placeholder = mapping.placeholder();

        // Try primary source first
        Object value = dataSourceResolver.resolveValue(mapping.source(), mapping.jsonPath());

        if (value != null) {
            return value;
        }

        // Try fallback sources
        if (mapping.fallbackSources() != null && !mapping.fallbackSources().isEmpty()) {
            if (loggingEnabled) {
                logger.debug("Primary source failed for '{}', trying {} fallbacks",
                        placeholder, mapping.fallbackSources().size());
            }

            for (ContentGenerationConfig.FallbackSource fallback : mapping.fallbackSources()) {
                value = dataSourceResolver.resolveValue(fallback.source(), fallback.jsonPath());

                if (value != null) {
                    if (loggingEnabled) {
                        logger.debug("Resolved '{}' from fallback source '{}'",
                                placeholder, fallback.source());
                    }
                    return value;
                }
            }
        }

        // Handle unresolved placeholder
        if (strictMode) {
            throw new IOException("Failed to resolve placeholder: " + placeholder);
        }

        if (loggingEnabled) {
            logger.warn("Could not resolve placeholder '{}', using default value: '{}'",
                    placeholder, defaultValue);
        }

        return defaultValue;
    }

    /**
     * Resolve a specific placeholder by name
     */
    public Object resolvePlaceholder(String placeholderName) throws IOException {
        ContentGenerationConfig.PlaceholderMapping mapping = mappings.stream()
                .filter(m -> m.placeholder().equals(placeholderName))
                .findFirst()
                .orElse(null);

        if (mapping == null) {
            if (strictMode) {
                throw new IOException("Placeholder mapping not found: " + placeholderName);
            }
            logger.warn("No mapping found for placeholder: {}", placeholderName);
            return defaultValue;
        }

        return resolvePlaceholder(mapping);
    }
}