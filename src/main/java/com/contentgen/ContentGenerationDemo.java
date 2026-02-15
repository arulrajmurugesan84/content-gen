package com.contentgen;

import com.contentgen.service.ContentGenerationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Example usage of ContentGenerationService
 */
public class ContentGenerationDemo {

    private static final Logger logger = LoggerFactory.getLogger(ContentGenerationDemo.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        try {
            // Initialize service with config file and base path
            String basePath = "C:/Arul-work/content-gen/src/main/resources"; // Adjust to your data directory
            String configFile = "data-mapping-config.json";

            ContentGenerationService service = new ContentGenerationService(configFile, basePath);

            logger.info("=== Content Generation Service Demo ===\n");

            // Example 1: Show service statistics
            demonstrateStatistics(service);

            // Example 2: Show resolved context
            demonstrateResolvedContext(service);

            // Example 3: Generate content from template file
            demonstrateTemplateGeneration(service);

            // Example 4: Generate with inline template
            demonstrateInlineTemplate(service);

            // Example 5: Generate with additional context
            demonstrateAdditionalContext(service);

            logger.info("\n=== Demo completed successfully ===");

        } catch (Exception e) {
            logger.error("Demo failed with error", e);
        }
    }

    private static void demonstrateStatistics(ContentGenerationService service) {
        logger.info("--- Service Statistics ---");
        Map<String, Object> stats = service.getStatistics();
        stats.forEach((key, value) -> logger.info("{}: {}", key, value));
        logger.info("");
    }

    private static void demonstrateResolvedContext(ContentGenerationService service) throws Exception {
        logger.info("--- Resolved Context ---");
        Map<String, Object> context = service.getResolvedContext();
        String contextJson = objectMapper.writeValueAsString(context);
        logger.info("Context:\n{}\n", contextJson);
    }

    private static void demonstrateTemplateGeneration(ContentGenerationService service) throws Exception {
        logger.info("--- Generate from Template File ---");

        JsonNode result = service.generateContentAsJson("report-template.json");
        String formattedJson = objectMapper.writeValueAsString(result);

        logger.info("Generated content:\n{}\n", formattedJson);
    }

    private static void demonstrateInlineTemplate(ContentGenerationService service) throws Exception {
        logger.info("--- Generate from Inline Template ---");

        String inlineTemplate = """
            {
                "report": {
                    "title": "Quick Report",
                    "date": "{{reportDate}}",
                    "author": "{{authorName}}",
                    "contact": "{{contactEmail}}"
                }
            }
            """;

        String result = service.generateContentFromString(inlineTemplate);

        JsonNode jsonResult = objectMapper.readTree(result);
        String formattedJson = objectMapper.writeValueAsString(jsonResult);

        logger.info("Generated content:\n{}\n", formattedJson);
    }

    private static void demonstrateAdditionalContext(ContentGenerationService service) throws Exception {
        logger.info("--- Generate with Additional Context ---");

        Map<String, Object> additionalContext = Map.of(
                "customField", "Custom Value",
                "timestamp", System.currentTimeMillis()
        );

        String template = """
            {
                "report": {
                    "author": "{{authorName}}",
                    "date": "{{reportDate}}",
                    "customField": "{{customField}}",
                    "timestamp": {{timestamp}}
                }
            }
            """;

        // Write template to temp file for demo
        java.nio.file.Path tempTemplate = java.nio.file.Files.createTempFile("template", ".json");
        java.nio.file.Files.writeString(tempTemplate, template);

        String result = service.generateContentWithContext(
                tempTemplate.getFileName().toString(),
                additionalContext
        );

        JsonNode jsonResult = objectMapper.readTree(result);
        String formattedJson = objectMapper.writeValueAsString(jsonResult);

        logger.info("Generated content:\n{}\n", formattedJson);

        // Cleanup
        java.nio.file.Files.deleteIfExists(tempTemplate);
    }
}