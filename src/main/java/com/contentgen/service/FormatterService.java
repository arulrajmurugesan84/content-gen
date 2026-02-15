package com.contentgen.service;

import com.contentgen.config.EnhancedContentGenerationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

/**
 * Service for formatting values based on configuration
 * Supports: date, currency, number formatting
 */
public class FormatterService {

    private static final Logger logger = LoggerFactory.getLogger(FormatterService.class);

    private final EnhancedContentGenerationConfig.FormatterConfig formatterConfig;
    private final boolean formattingEnabled;

    public FormatterService(
            EnhancedContentGenerationConfig.FormatterConfig formatterConfig,
            boolean formattingEnabled) {
        this.formatterConfig = formatterConfig;
        this.formattingEnabled = formattingEnabled;
    }

    /**
     * Format a value based on formatter definition
     */
    public Object format(Object value, EnhancedContentGenerationConfig.FormatterDefinition formatter) {
        if (!formattingEnabled || formatter == null || value == null) {
            return value;
        }

        try {
            return switch (formatter.type().toLowerCase()) {
                case "date" -> formatDate(value, formatter);
                case "currency" -> formatCurrency(value, formatter);
                case "number" -> formatNumber(value, formatter);
                default -> {
                    logger.warn("Unknown formatter type: {}", formatter.type());
                    yield value;
                }
            };
        } catch (Exception e) {
            logger.error("Error formatting value with {}: {}", formatter.type(), e.getMessage());
            return value; // Return original value on error
        }
    }

    /**
     * Format date value
     */
    private String formatDate(Object value, EnhancedContentGenerationConfig.FormatterDefinition formatter) {
        String inputFormat = formatter.inputFormat() != null
                ? formatter.inputFormat()
                : formatterConfig.date().defaultInputFormat();

        String outputFormat = formatter.outputFormat() != null
                ? formatter.outputFormat()
                : formatterConfig.date().defaultOutputFormat();

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputFormat);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);

        String dateString = value.toString();

        try {
            // Try parsing as LocalDateTime first
            if (dateString.contains("T")) {
                LocalDateTime dateTime = LocalDateTime.parse(dateString, inputFormatter);
                return dateTime.format(outputFormatter);
            } else {
                // Parse as LocalDate
                LocalDate date = LocalDate.parse(dateString, inputFormatter);
                return date.format(outputFormatter);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse date '{}' with format '{}': {}",
                    dateString, inputFormat, e.getMessage());
            return dateString; // Return original on parse error
        }
    }

    /**
     * Format currency value
     */
    private String formatCurrency(Object value, EnhancedContentGenerationConfig.FormatterDefinition formatter) {
        String currencyCode = formatter.currencyCode() != null
                ? formatter.currencyCode()
                : formatterConfig.currency().defaultCurrency();

        String localeStr = formatter.locale() != null
                ? formatter.locale()
                : formatterConfig.currency().defaultLocale();

        Locale locale = parseLocale(localeStr);

        // Convert value to double
        double amount;
        if (value instanceof Number number) {
            amount = number.doubleValue();
        } else {
            try {
                amount = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                logger.warn("Cannot parse '{}' as currency", value);
                return value.toString();
            }
        }

        // Create currency formatter
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        currencyFormatter.setCurrency(Currency.getInstance(currencyCode));

        // Apply decimal places if specified
        int decimalPlaces = formatter.decimalPlaces() != null
                ? formatter.decimalPlaces()
                : formatterConfig.currency().decimalPlaces();
        currencyFormatter.setMinimumFractionDigits(decimalPlaces);
        currencyFormatter.setMaximumFractionDigits(decimalPlaces);

        return currencyFormatter.format(amount);
    }

    /**
     * Format number value
     */
    private String formatNumber(Object value, EnhancedContentGenerationConfig.FormatterDefinition formatter) {
        String localeStr = formatter.locale() != null
                ? formatter.locale()
                : formatterConfig.number().defaultLocale();

        Locale locale = parseLocale(localeStr);

        // Convert value to number
        double number;
        if (value instanceof Number num) {
            number = num.doubleValue();
        } else {
            try {
                number = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                logger.warn("Cannot parse '{}' as number", value);
                return value.toString();
            }
        }

        // Create number formatter
        NumberFormat numberFormatter = NumberFormat.getNumberInstance(locale);

        // Apply grouping
        boolean useGrouping = formatter.useGrouping() != null
                ? formatter.useGrouping()
                : formatterConfig.number().useGrouping();
        numberFormatter.setGroupingUsed(useGrouping);

        // Apply fraction digits from global config
        numberFormatter.setMinimumFractionDigits(formatterConfig.number().minimumFractionDigits());
        numberFormatter.setMaximumFractionDigits(formatterConfig.number().maximumFractionDigits());

        return numberFormatter.format(number);
    }

    /**
     * Parse locale string (e.g., "en_US" -> Locale.US)
     */
    private Locale parseLocale(String localeStr) {
        if (localeStr == null || localeStr.isEmpty()) {
            return Locale.US;
        }

        String[] parts = localeStr.split("_");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else if (parts.length == 3) {
            return new Locale(parts[0], parts[1], parts[2]);
        }

        return Locale.US;
    }
}