package com.contentgen.service;

import com.contentgen.config.ContentGenerationConfig;
import com.contentgen.config.EnhancedContentGenerationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves data from multiple JSON data sources with caching support
 */
public class EnhancedDataSourceResolver {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedDataSourceResolver.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Object> dataSourceCache;
    private final Map<String, EnhancedContentGenerationConfig.DataSource> dataSourceConfigs;
    private final boolean cachingEnabled;
    private final String basePath;

    // JsonPath configuration for flexible querying
    private final Configuration jsonPathConfig;

    public EnhancedDataSourceResolver(
            ObjectMapper objectMapper,
            Map<String, EnhancedContentGenerationConfig.DataSource> dataSourceConfigs,
            boolean cachingEnabled,
            String basePath) {
        this.objectMapper = objectMapper;
        this.dataSourceConfigs = dataSourceConfigs;
        this.cachingEnabled = cachingEnabled;
        this.basePath = basePath;
        this.dataSourceCache = new ConcurrentHashMap<>();

        // Configure JsonPath for null-safe operations
        this.jsonPathConfig = Configuration.defaultConfiguration()
                .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
                .addOptions(Option.SUPPRESS_EXCEPTIONS);
    }

    /**
     * Load all data sources into cache
     */
    public void preloadDataSources() throws IOException {
        logger.info("Preloading {} data sources", dataSourceConfigs.size());

        for (Map.Entry<String, EnhancedContentGenerationConfig.DataSource> entry : dataSourceConfigs.entrySet()) {
            String sourceName = entry.getKey();
            String filePath = entry.getValue().filePath();

            loadDataSource(sourceName, filePath);
        }

        logger.info("Successfully preloaded all data sources");
    }

    /**
     * Load a single data source
     */
    private Object loadDataSource(String sourceName, String filePath) throws IOException {
        // Check cache first
        if (cachingEnabled && dataSourceCache.containsKey(sourceName)) {
            logger.debug("Returning cached data source: {}", sourceName);
            return dataSourceCache.get(sourceName);
        }

        Path fullPath = Paths.get(basePath, filePath);
        logger.debug("Loading data source '{}' from: {}", sourceName, fullPath);

        if (!Files.exists(fullPath)) {
            throw new IOException("Data source file not found: " + fullPath);
        }

        // Read and parse JSON
        String jsonContent = Files.readString(fullPath);
        Object parsedData = Configuration.defaultConfiguration().jsonProvider().parse(jsonContent);

        // Cache if enabled
        if (cachingEnabled) {
            dataSourceCache.put(sourceName, parsedData);
        }

        logger.debug("Successfully loaded data source: {}", sourceName);
        return parsedData;
    }

    /**
     * Resolve a value from a data source using JsonPath
     */
    public Object resolveValue(String sourceName, String jsonPath) throws IOException {
        logger.debug("Resolving value from source '{}' with path '{}'", sourceName, jsonPath);

        EnhancedContentGenerationConfig.DataSource sourceConfig = dataSourceConfigs.get(sourceName);
        if (sourceConfig == null) {
            logger.warn("Data source not found in configuration: {}", sourceName);
            return null;
        }

        Object dataSource = loadDataSource(sourceName, sourceConfig.filePath());

        try {
            Object value = JsonPath.using(jsonPathConfig).parse(dataSource).read(jsonPath);
            logger.debug("Resolved value: {} -> {}", jsonPath, value);
            return value;
        } catch (Exception e) {
            logger.warn("Failed to resolve path '{}' in source '{}': {}",
                    jsonPath, sourceName, e.getMessage());
            return null;
        }
    }

    /**
     * Clear the cache
     */
    public void clearCache() {
        dataSourceCache.clear();
        logger.info("Data source cache cleared");
    }

    /**
     * Get cache statistics
     */
    public Map<String, Integer> getCacheStats() {
        return Map.of(
                "cachedSources", dataSourceCache.size(),
                "configuredSources", dataSourceConfigs.size()
        );
    }
}