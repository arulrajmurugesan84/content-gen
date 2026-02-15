package com.contentgen.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Enhanced configuration model with formatters and conditional selection
 */
public record EnhancedContentGenerationConfig(
        @JsonProperty("configVersion") String configVersion,
        @JsonProperty("description") String description,
        @JsonProperty("dataSources") Map<String, DataSource> dataSources,
        @JsonProperty("mappings") List<PlaceholderMapping> mappings,
        @JsonProperty("formatters") FormatterConfig formatters,
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
     * Enhanced placeholder mapping with formatters and conditional selection
     */
    public record PlaceholderMapping(
            @JsonProperty("placeholder") String placeholder,
            @JsonProperty("source") String source,
            @JsonProperty("jsonPath") String jsonPath,
            @JsonProperty("description") String description,
            @JsonProperty("formatter") FormatterDefinition formatter,
            @JsonProperty("conditionalSelection") ConditionalSelection conditionalSelection,
            @JsonProperty("customCalculation") CustomCalculation customCalculation,
            @JsonProperty("fallbackSources") List<FallbackSource> fallbackSources,
            @JsonProperty("mandatory") Boolean mandatory,
            @JsonProperty("defaultValue") String defaultValue,
            @JsonProperty("parentCollectionPath") String parentCollectionPath,
            @JsonProperty("validateParentNotEmpty") Boolean validateParentNotEmpty
    ) {
        public PlaceholderMapping {
            // Set defaults if null
            if (mandatory == null) {
                mandatory = false;
            }
            if (validateParentNotEmpty == null) {
                validateParentNotEmpty = false;
            }
        }
    }

    /**
     * Fallback source for placeholder resolution
     */
    public record FallbackSource(
            @JsonProperty("source") String source,
            @JsonProperty("jsonPath") String jsonPath,
            @JsonProperty("description") String description
    ) {}

    /**
     * Formatter definition for a specific placeholder
     */
    public record FormatterDefinition(
            @JsonProperty("type") String type, // date, currency, number
            @JsonProperty("inputFormat") String inputFormat,
            @JsonProperty("outputFormat") String outputFormat,
            @JsonProperty("currencyCode") String currencyCode,
            @JsonProperty("locale") String locale,
            @JsonProperty("useGrouping") Boolean useGrouping,
            @JsonProperty("decimalPlaces") Integer decimalPlaces
    ) {}

    /**
     * Conditional selection for choosing values based on conditions
     */
    public record ConditionalSelection(
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("conditions") List<Condition> conditions,
            @JsonProperty("extractFirstElement") boolean extractFirstElement
    ) {
        public ConditionalSelection {
            if (conditions == null) {
                conditions = List.of();
            }
        }
    }

    /**
     * Individual condition for conditional selection
     */
    public record Condition(
            @JsonProperty("jsonPath") String jsonPath,
            @JsonProperty("description") String description
    ) {}

    /**
     * Custom calculation definition (e.g., sum, average)
     */
    public record CustomCalculation(
            @JsonProperty("type") String type, // sum, average, count, min, max
            @JsonProperty("field") String field,
            @JsonProperty("description") String description
    ) {}

    /**
     * Global formatter configurations
     */
    public record FormatterConfig(
            @JsonProperty("date") DateFormatterConfig date,
            @JsonProperty("currency") CurrencyFormatterConfig currency,
            @JsonProperty("number") NumberFormatterConfig number
    ) {}

    /**
     * Date formatter configuration
     */
    public record DateFormatterConfig(
            @JsonProperty("defaultInputFormat") String defaultInputFormat,
            @JsonProperty("defaultOutputFormat") String defaultOutputFormat,
            @JsonProperty("locale") String locale,
            @JsonProperty("timezone") String timezone
    ) {}

    /**
     * Currency formatter configuration
     */
    public record CurrencyFormatterConfig(
            @JsonProperty("defaultCurrency") String defaultCurrency,
            @JsonProperty("defaultLocale") String defaultLocale,
            @JsonProperty("symbolPosition") String symbolPosition,
            @JsonProperty("decimalPlaces") int decimalPlaces
    ) {}

    /**
     * Number formatter configuration
     */
    public record NumberFormatterConfig(
            @JsonProperty("defaultLocale") String defaultLocale,
            @JsonProperty("useGrouping") boolean useGrouping,
            @JsonProperty("minimumFractionDigits") int minimumFractionDigits,
            @JsonProperty("maximumFractionDigits") int maximumFractionDigits
    ) {}

    /**
     * Configuration options
     */
    public record ConfigOptions(
            @JsonProperty("cacheDataSources") boolean cacheDataSources,
            @JsonProperty("strictMode") boolean strictMode,
            @JsonProperty("defaultValue") String defaultValue,
            @JsonProperty("enableLogging") boolean enableLogging,
            @JsonProperty("enableConditionalSelection") boolean enableConditionalSelection,
            @JsonProperty("enableFormatters") boolean enableFormatters
    ) {
        public ConfigOptions {
            if (defaultValue == null) {
                defaultValue = "";
            }
        }
    }
}