package com.contentgen;

import com.contentgen.service.EnhancedContentGenerationServiceV2;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Demo of enhanced features:
 * 1. Conditional address selection (MAILING preferred over PHYSICAL)
 * 2. Custom formatters (date, currency, number) via config
 * 3. Array looping in Handlebars templates
 * 4. Custom calculations (sum, average, etc.)
 */
public class EnhancedContentGenerationDemo {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedContentGenerationDemo.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        try {
            // Initialize service with enhanced config
            String basePath = "C:/Arul-work/content-gen/src/main/resources"; // Adjust to your data directory
            String configFile = "enhanced-data-mapping-config.json";

            EnhancedContentGenerationServiceV2 service =
                    new EnhancedContentGenerationServiceV2(configFile, basePath);

            logger.info("=== Enhanced Content Generation Service Demo ===\n");

            // Example 1: Show service statistics
            demonstrateStatistics(service);

            // Example 2: Show resolved context with formatters
            demonstrateResolvedContext(service);

            // Example 3: Demonstrate conditional address selection
            demonstrateConditionalAddressSelection(service);

            // Example 4: Generate JSON with inventory looping
            demonstrateInventoryLoop(service);

            // Example 5: Generate HTML report
            demonstrateHTMLReport(service);

            // Example 6: Simple inventory template
            demonstrateSimpleTemplate(service);

            logger.info("\n=== Demo completed successfully ===");

        } catch (Exception e) {
            logger.error("Demo failed with error", e);
            e.printStackTrace();
        }
    }

    private static void demonstrateStatistics(EnhancedContentGenerationServiceV2 service) {
        logger.info("--- Service Statistics ---");
        Map<String, Object> stats = service.getStatistics();
        stats.forEach((key, value) -> logger.info("{}: {}", key, value));
        logger.info("");
    }

    private static void demonstrateResolvedContext(EnhancedContentGenerationServiceV2 service)
            throws Exception {
        logger.info("--- Resolved Context (with Formatters Applied) ---");
        Map<String, Object> context = service.getResolvedContext();

        // Show key resolved values
        logger.info("Report Date (formatted): {}", context.get("reportDate"));
        logger.info("Generation Date (formatted): {}", context.get("generationDate"));
        logger.info("Total Inventory Value (formatted): {}", context.get("totalInventoryValue"));
        logger.info("Inventory Count (formatted): {}", context.get("inventoryCount"));
        logger.info("");
    }

    private static void demonstrateConditionalAddressSelection(
            EnhancedContentGenerationServiceV2 service) throws Exception {

        logger.info("--- Conditional Address Selection Demo ---");
        logger.info("Config is set to prefer MAILING address, fallback to PHYSICAL\n");

        Map<String, Object> context = service.getResolvedContext();

        logger.info("Selected Address:");
        logger.info("  Street: {}", context.get("street"));
        logger.info("  City: {}", context.get("city"));
        logger.info("  State: {}", context.get("state"));
        logger.info("  Zip: {}", context.get("zipCode"));
        logger.info("");
        logger.info("This should be the MAILING address (123 Main St, Anytown, CA 12345)");
        logger.info("If MAILING is not available, it falls back to PHYSICAL address\n");
    }

    private static void demonstrateInventoryLoop(EnhancedContentGenerationServiceV2 service)
            throws Exception {

        logger.info("--- Inventory Loop Example ---");

        JsonNode result = service.generateContentAsJson("inventory-loop-example.json");
        String formattedJson = objectMapper.writeValueAsString(result);

        logger.info("Generated JSON with inventory items loop:");
        logger.info("\n{}\n", formattedJson);
    }

    private static void demonstrateHTMLReport(EnhancedContentGenerationServiceV2 service)
            throws Exception {

        logger.info("--- HTML Report Generation ---");

        String htmlContent = service.generateContent("inventory-report.html");

        // Save to file
        Path outputPath = Path.of("generated-inventory-report.html");
        Files.writeString(outputPath, htmlContent);

        logger.info("Generated HTML report saved to: {}", outputPath.toAbsolutePath());
        logger.info("Open the file in a browser to see the formatted report");
        logger.info("Features:");
        logger.info("  - Loops through inventory items");
        logger.info("  - Conditional formatting (low stock, high value)");
        logger.info("  - Uses formatters from config");
        logger.info("  - Shows conditional address selection\n");
    }

    private static void demonstrateSimpleTemplate(EnhancedContentGenerationServiceV2 service)
            throws Exception {

        logger.info("--- Simple Inventory Template ---");

        JsonNode result = service.generateContentAsJson("simple-inventory-loop.json");
        String formattedJson = objectMapper.writeValueAsString(result);

        logger.info("Simple template with basic looping:");
        logger.info("\n{}\n", formattedJson);
    }

    private static void demonstrateInlineLoopTemplate(EnhancedContentGenerationServiceV2 service)
            throws Exception {

        logger.info("--- Inline Template with Loop ---");

        String template = """
            {
              "report": "Inventory Summary",
              "items": [
                {{#each inventoryItems}}
                {
                  "name": "{{name}}",
                  "qty": {{quantity}},
                  "price": "{{formatCurrency price}}"
                }{{#unless @last}},{{/unless}}
                {{/each}}
              ],
              "total": "{{totalInventoryValue}}"
            }
            """;

        String result = service.generateContentFromString(template);

        JsonNode jsonResult = objectMapper.readTree(result);
        String formattedJson = objectMapper.writeValueAsString(jsonResult);

        logger.info("Inline template result:");
        logger.info("\n{}\n", formattedJson);
    }
}