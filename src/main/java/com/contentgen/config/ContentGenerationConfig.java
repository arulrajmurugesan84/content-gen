package com.contentgen.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Root configuration model for content generation service
 */
public record ContentGenerationConfig(
        @JsonProperty("configVersion") String configVersion,
        @JsonProperty("description") String description,
        @JsonProperty("dataSources") Map<String, DataSource> dataSources,
        @JsonProperty("mappings") List<PlaceholderMapping> mappings,
        @JsonProperty("options") ConfigOptions options
) {

    /**
     * Data source definition
     */
    public record DataSource(
            @JsonProperty("filePath") String filePath,
            @JsonProperty("priority") int priority
    ) {}

    /**
     * Placeholder to data source mapping
     */
    public record PlaceholderMapping(
            @JsonProperty("placeholder") String placeholder,
            @JsonProperty("source") String source,
            @JsonProperty("jsonPath") String jsonPath,
            @JsonProperty("fallbackSources") List<FallbackSource> fallbackSources
    ) {}

    /**
     * Fallback source for placeholder resolution
     */
    public record FallbackSource(
            @JsonProperty("source") String source,
            @JsonProperty("jsonPath") String jsonPath
    ) {}

    /**
     * Configuration options
     */
    public record ConfigOptions(
            @JsonProperty("cacheDataSources") boolean cacheDataSources,
            @JsonProperty("strictMode") boolean strictMode,
            @JsonProperty("defaultValue") String defaultValue,
            @JsonProperty("enableLogging") boolean enableLogging
    ) {
        public ConfigOptions {
            // Default values if null
            if (defaultValue == null) {
                defaultValue = "";
            }
        }
    }
}