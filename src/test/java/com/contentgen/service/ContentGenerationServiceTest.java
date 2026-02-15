package com.contentgen.service;

import com.contentgen.config.ContentGenerationConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContentGenerationServiceTest {

    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testServiceInitialization() throws IOException {
        // Create test data files
        createTestDataFiles();
        createTestConfig();

        ContentGenerationService service = new ContentGenerationService(
                "test-config.json",
                tempDir.toString()
        );

        assertNotNull(service);

        Map<String, Object> stats = service.getStatistics();
        assertEquals("1.0", stats.get("configVersion"));
        assertEquals(2, stats.get("dataSourceCount"));
    }

    @Test
    void testPlaceholderResolution() throws IOException {
        createTestDataFiles();
        createTestConfig();

        ContentGenerationService service = new ContentGenerationService(
                "test-config.json",
                tempDir.toString()
        );

        Map<String, Object> context = service.getResolvedContext();

        assertNotNull(context);
        assertEquals("John Doe", context.get("authorName"));
        assertEquals("2024-06-30", context.get("reportDate"));
    }

    @Test
    void testContentGeneration() throws IOException {
        createTestDataFiles();
        createTestConfig();

        Path templatePath = tempDir.resolve("test-template.json");
        String template = """
            {
                "author": "{{authorName}}",
                "date": "{{reportDate}}"
            }
            """;
        Files.writeString(templatePath, template);

        ContentGenerationService service = new ContentGenerationService(
                "test-config.json",
                tempDir.toString()
        );

        JsonNode result = service.generateContentAsJson("test-template.json");

        assertNotNull(result);
        assertEquals("John Doe", result.get("author").asText());
        assertEquals("2024-06-30", result.get("date").asText());
    }

    @Test
    void testInlineTemplateGeneration() throws IOException {
        createTestDataFiles();
        createTestConfig();

        ContentGenerationService service = new ContentGenerationService(
                "test-config.json",
                tempDir.toString()
        );

        String template = "Author: {{authorName}}, Date: {{reportDate}}";
        String result = service.generateContentFromString(template);

        assertEquals("Author: John Doe, Date: 2024-06-30", result);
    }

    @Test
    void testAdditionalContext() throws IOException {
        createTestDataFiles();
        createTestConfig();

        Path templatePath = tempDir.resolve("custom-template.json");
        String template = """
            {
                "author": "{{authorName}}",
                "custom": "{{customValue}}"
            }
            """;
        Files.writeString(templatePath, template);

        ContentGenerationService service = new ContentGenerationService(
                "test-config.json",
                tempDir.toString()
        );

        Map<String, Object> additionalContext = Map.of("customValue", "test123");
        String result = service.generateContentWithContext("custom-template.json", additionalContext);

        JsonNode jsonResult = objectMapper.readTree(result);
        assertEquals("John Doe", jsonResult.get("author").asText());
        assertEquals("test123", jsonResult.get("custom").asText());
    }

    @Test
    void testFallbackResolution() throws IOException {
        // Create data file without primary field but with fallback field
        Path dataFile1 = tempDir.resolve("source1.json");
        Files.writeString(dataFile1, """
            {
                "fallbackField": "Fallback Value"
            }
            """);

        Path dataFile2 = tempDir.resolve("source2.json");
        Files.writeString(dataFile2, """
            {
                "primaryField": "Primary Value"
            }
            """);

        // Create config with fallback
        String config = """
            {
                "configVersion": "1.0",
                "dataSources": {
                    "source1": {"filePath": "source1.json", "priority": 1},
                    "source2": {"filePath": "source2.json", "priority": 2}
                },
                "mappings": [
                    {
                        "placeholder": "testValue",
                        "source": "source2",
                        "jsonPath": "$.nonexistent",
                        "fallbackSources": [
                            {
                                "source": "source1",
                                "jsonPath": "$.fallbackField"
                            }
                        ]
                    }
                ],
                "options": {
                    "cacheDataSources": true,
                    "strictMode": false,
                    "defaultValue": "",
                    "enableLogging": false
                }
            }
            """;

        Path configPath = tempDir.resolve("fallback-config.json");
        Files.writeString(configPath, config);

        ContentGenerationService service = new ContentGenerationService(
                "fallback-config.json",
                tempDir.toString()
        );

        Map<String, Object> context = service.getResolvedContext();
        assertEquals("Fallback Value", context.get("testValue"));
    }

    private void createTestDataFiles() throws IOException {
        // Create source1.json
        String source1 = """
            {
                "authorName": "John Doe",
                "reportDate": "2024-06-30"
            }
            """;
        Files.writeString(tempDir.resolve("source1.json"), source1);

        // Create source2.json
        String source2 = """
            {
                "contactEmail": "test@example.com"
            }
            """;
        Files.writeString(tempDir.resolve("source2.json"), source2);
    }

    private void createTestConfig() throws IOException {
        String config = """
            {
                "configVersion": "1.0",
                "description": "Test configuration",
                "dataSources": {
                    "source1": {
                        "filePath": "source1.json",
                        "priority": 1
                    },
                    "source2": {
                        "filePath": "source2.json",
                        "priority": 2
                    }
                },
                "mappings": [
                    {
                        "placeholder": "authorName",
                        "source": "source1",
                        "jsonPath": "$.authorName"
                    },
                    {
                        "placeholder": "reportDate",
                        "source": "source1",
                        "jsonPath": "$.reportDate"
                    },
                    {
                        "placeholder": "contactEmail",
                        "source": "source2",
                        "jsonPath": "$.contactEmail"
                    }
                ],
                "options": {
                    "cacheDataSources": true,
                    "strictMode": false,
                    "defaultValue": "",
                    "enableLogging": false
                }
            }
            """;

        Files.writeString(tempDir.resolve("test-config.json"), config);
    }
}