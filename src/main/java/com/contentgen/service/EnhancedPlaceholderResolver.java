package com.contentgen.service;

import com.contentgen.config.EnhancedContentGenerationConfig;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Enhanced resolver with conditional selection, formatters, and custom calculations
 */
public class EnhancedPlaceholderResolver {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedPlaceholderResolver.class);

    private final EnhancedDataSourceResolver dataSourceResolver;
    private final List<EnhancedContentGenerationConfig.PlaceholderMapping> mappings;
    private final FormatterService formatterService;
    private final boolean strictMode;
    private final String defaultValue;
    private final boolean loggingEnabled;
    private final boolean conditionalSelectionEnabled;

    public EnhancedPlaceholderResolver(
            EnhancedDataSourceResolver dataSourceResolver,
            List<EnhancedContentGenerationConfig.PlaceholderMapping> mappings,
            FormatterService formatterService,
            EnhancedContentGenerationConfig.ConfigOptions options) {
        this.dataSourceResolver = dataSourceResolver;
        this.mappings = mappings;
        this.formatterService = formatterService;
        this.strictMode = options.strictMode();
        this.defaultValue = options.defaultValue();
        this.loggingEnabled = options.enableLogging();
        this.conditionalSelectionEnabled = options.enableConditionalSelection();
    }

    /**
     * Resolve all placeholders to create a context map for Handlebars
     */
    public Map<String, Object> resolveAllPlaceholders() throws IOException {
        Map<String, Object> context = new HashMap<>();

        if (loggingEnabled) {
            logger.info("Resolving {} placeholders", mappings.size());
        }

        for (EnhancedContentGenerationConfig.PlaceholderMapping mapping : mappings) {
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
     * Resolve a single placeholder with conditional selection, formatters, and calculations
     */
    private Object resolvePlaceholder(EnhancedContentGenerationConfig.PlaceholderMapping mapping)
            throws IOException {

        String placeholder = mapping.placeholder();
        Object value = null;

        // Step 0: Validate parent collection if required
        if (mapping.validateParentNotEmpty() != null && mapping.validateParentNotEmpty()) {
            validateParentCollection(mapping);
        }

        // Step 1: Try conditional selection if enabled
        if (conditionalSelectionEnabled &&
                mapping.conditionalSelection() != null &&
                mapping.conditionalSelection().enabled()) {

            value = resolveWithConditionalSelection(mapping);

            if (value != null) {
                if (loggingEnabled) {
                    logger.debug("Resolved '{}' using conditional selection", placeholder);
                }
            }
        }

        // Step 2: If no value yet, try primary source
        if (value == null) {
            value = dataSourceResolver.resolveValue(mapping.source(), mapping.jsonPath());
        }

        // Step 3: Try fallback sources if still null
        if (value == null && mapping.fallbackSources() != null) {
            value = resolveWithFallbacks(mapping);
        }

        // Step 4: Apply custom calculation if defined
        if (value != null && mapping.customCalculation() != null) {
            value = applyCustomCalculation(value, mapping.customCalculation());
        }

        // Step 5: Apply formatter if defined
        if (value != null && mapping.formatter() != null) {
            value = formatterService.format(value, mapping.formatter());
        }

        // Step 6: Handle unresolved placeholder based on mandatory flag
        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
            // Check if this placeholder is mandatory
            boolean isMandatory = mapping.mandatory() != null && mapping.mandatory();

            if (isMandatory) {
                // Mandatory field - throw exception
                String errorMsg = String.format(
                        "Mandatory placeholder '%s' could not be resolved. " +
                                "Check data source '%s' and path '%s'",
                        placeholder, mapping.source(), mapping.jsonPath()
                );
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }

            // Optional field - use default value
            String placeholderDefault = mapping.defaultValue();
            String finalDefault = placeholderDefault != null ? placeholderDefault : defaultValue;

            if (loggingEnabled) {
                logger.warn("Could not resolve optional placeholder '{}', using default value: '{}'",
                        placeholder, finalDefault);
            }

            return finalDefault;
        }

        return value;
    }

    /**
     * Validate that parent collection is not empty
     */
    private void validateParentCollection(EnhancedContentGenerationConfig.PlaceholderMapping mapping)
            throws IOException {

        String parentPath = mapping.parentCollectionPath();

        if (parentPath == null || parentPath.isEmpty()) {
            logger.warn("validateParentNotEmpty is true but parentCollectionPath is not specified for '{}'",
                    mapping.placeholder());
            return;
        }

        try {
            Object parentCollection = dataSourceResolver.resolveValue(
                    mapping.source(),
                    parentPath
            );

            if (parentCollection == null) {
                String errorMsg = String.format(
                        "Parent collection validation failed for placeholder '%s'. " +
                                "Parent collection at path '%s' in source '%s' is null or does not exist.",
                        mapping.placeholder(), parentPath, mapping.source()
                );
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }

            // Check if it's a collection and if it's empty
            if (parentCollection instanceof List<?> list) {
                if (list.isEmpty()) {
                    String errorMsg = String.format(
                            "Parent collection validation failed for placeholder '%s'. " +
                                    "Parent collection at path '%s' in source '%s' is empty. " +
                                    "At least one element is required.",
                            mapping.placeholder(), parentPath, mapping.source()
                    );
                    logger.error(errorMsg);
                    throw new IOException(errorMsg);
                }

                if (loggingEnabled) {
                    logger.debug("Parent collection validation passed for '{}'. Collection has {} elements.",
                            mapping.placeholder(), list.size());
                }
            } else {
                logger.warn("Parent collection at path '{}' is not a List, skipping empty validation for '{}'",
                        parentPath, mapping.placeholder());
            }

        } catch (IOException e) {
            // Re-throw IOException (from validation failure)
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format(
                    "Error validating parent collection for placeholder '%s' at path '%s': %s",
                    mapping.placeholder(), parentPath, e.getMessage()
            );
            logger.error(errorMsg, e);
            throw new IOException(errorMsg, e);
        }
    }

    /**
     * Resolve value using conditional selection
     */
    private Object resolveWithConditionalSelection(
            EnhancedContentGenerationConfig.PlaceholderMapping mapping) throws IOException {

        EnhancedContentGenerationConfig.ConditionalSelection selection = mapping.conditionalSelection();

        for (EnhancedContentGenerationConfig.Condition condition : selection.conditions()) {
            try {
                Object value = dataSourceResolver.resolveValue(
                        mapping.source(),
                        condition.jsonPath()
                );

                if (value != null) {
                    // Extract first element if it's an array and extractFirstElement is true
                    if (selection.extractFirstElement() && value instanceof List<?> list) {
                        if (!list.isEmpty()) {
                            value = list.get(0);
                        } else {
                            continue; // Empty array, try next condition
                        }
                    }

                    if (loggingEnabled && condition.description() != null) {
                        logger.debug("Condition matched: {}", condition.description());
                    }

                    return value;
                }
            } catch (Exception e) {
                if (loggingEnabled) {
                    logger.debug("Condition failed: {} - {}",
                            condition.jsonPath(), e.getMessage());
                }
                // Continue to next condition
            }
        }

        return null; // No condition matched
    }

    /**
     * Resolve with fallback sources
     */
    private Object resolveWithFallbacks(EnhancedContentGenerationConfig.PlaceholderMapping mapping)
            throws IOException {

        if (loggingEnabled) {
            logger.debug("Primary source failed for '{}', trying {} fallbacks",
                    mapping.placeholder(), mapping.fallbackSources().size());
        }

        for (EnhancedContentGenerationConfig.FallbackSource fallback : mapping.fallbackSources()) {
            Object value = dataSourceResolver.resolveValue(fallback.source(), fallback.jsonPath());

            if (value != null) {
                if (loggingEnabled) {
                    logger.debug("Resolved '{}' from fallback source '{}'",
                            mapping.placeholder(), fallback.source());
                }
                return value;
            }
        }

        return null;
    }

    /**
     * Apply custom calculation (sum, average, count, min, max)
     */
    private Object applyCustomCalculation(
            Object value,
            EnhancedContentGenerationConfig.CustomCalculation calculation) {

        if (!(value instanceof List<?> list)) {
            logger.warn("Custom calculation requires array value, got: {}",
                    value.getClass().getSimpleName());
            return value;
        }

        try {
            return switch (calculation.type().toLowerCase()) {
                case "sum" -> calculateSum(list, calculation.field());
                case "average", "avg" -> calculateAverage(list, calculation.field());
                case "count" -> list.size();
                case "min" -> calculateMin(list, calculation.field());
                case "max" -> calculateMax(list, calculation.field());
                default -> {
                    logger.warn("Unknown calculation type: {}", calculation.type());
                    yield value;
                }
            };
        } catch (Exception e) {
            logger.error("Error applying calculation {}: {}",
                    calculation.type(), e.getMessage());
            return value;
        }
    }

    /**
     * Calculate sum of field values in array
     */
    private double calculateSum(List<?> list, String field) {
        return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> ((Map<?, ?>) item).get(field))
                .filter(Objects::nonNull)
                .mapToDouble(this::toDouble)
                .sum();
    }

    /**
     * Calculate average of field values in array
     */
    private double calculateAverage(List<?> list, String field) {
        OptionalDouble avg = list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> ((Map<?, ?>) item).get(field))
                .filter(Objects::nonNull)
                .mapToDouble(this::toDouble)
                .average();

        return avg.orElse(0.0);
    }

    /**
     * Calculate minimum of field values in array
     */
    private double calculateMin(List<?> list, String field) {
        OptionalDouble min = list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> ((Map<?, ?>) item).get(field))
                .filter(Objects::nonNull)
                .mapToDouble(this::toDouble)
                .min();

        return min.orElse(0.0);
    }

    /**
     * Calculate maximum of field values in array
     */
    private double calculateMax(List<?> list, String field) {
        OptionalDouble max = list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> ((Map<?, ?>) item).get(field))
                .filter(Objects::nonNull)
                .mapToDouble(this::toDouble)
                .max();

        return max.orElse(0.0);
    }

    /**
     * Convert object to double
     */
    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Cannot convert to double: {}", value);
            return 0.0;
        }
    }
}