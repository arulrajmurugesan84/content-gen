package com.contentgen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for parent collection validation feature
 * Ensures that when a placeholder references a child node of a collection (array),
 * the parent collection is validated to not be empty or null.
 */
class ParentCollectionValidationTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Empty setup
    }

    @Test
    void testParentCollectionExists_WithElements() throws IOException {
        createDataWithValidAddress();
        createConfigWithParentValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        Map<String, Object> context = service.getResolvedContext();

        // Should successfully resolve when parent collection has elements
        assertEquals("123 Main St", context.get("street"));
        assertEquals("Anytown", context.get("city"));
    }

    @Test
    void testParentCollectionEmpty_ThrowsException() throws IOException {
        createDataWithEmptyAddressArray();
        createConfigWithParentValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        // Should throw exception when parent collection is empty
        IOException exception = assertThrows(IOException.class, () -> {
            service.getResolvedContext();
        });

        assertTrue(exception.getMessage().contains("Parent collection validation failed"));
        assertTrue(exception.getMessage().contains("is empty"));
        assertTrue(exception.getMessage().contains("street"));
        assertTrue(exception.getMessage().contains("$.address"));
    }

    @Test
    void testParentCollectionNull_ThrowsException() throws IOException {
        createDataWithoutAddressField();
        createConfigWithParentValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        // Should throw exception when parent collection is null/missing
        IOException exception = assertThrows(IOException.class, () -> {
            service.getResolvedContext();
        });

        assertTrue(exception.getMessage().contains("Parent collection validation failed"));
        assertTrue(exception.getMessage().contains("is null or does not exist"));
    }

    @Test
    void testParentValidationDisabled_AllowsEmptyArray() throws IOException {
        createDataWithEmptyAddressArray();
        createConfigWithoutParentValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        // Should not throw when validation is disabled
        Map<String, Object> context = service.getResolvedContext();

        // Field will be empty/default since array is empty
        assertNotNull(context);
    }

    @Test
    void testMultipleFieldsShareParentCollection() throws IOException {
        createDataWithValidAddress();
        createConfigWithMultipleFieldsValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        Map<String, Object> context = service.getResolvedContext();

        // All fields referencing same parent should validate once and succeed
        assertEquals("123 Main St", context.get("street"));
        assertEquals("Anytown", context.get("city"));
        assertEquals("CA", context.get("state"));
        assertEquals("12345", context.get("zipCode"));
    }

    @Test
    void testMultipleFieldsShareParentCollection_EmptyArray() throws IOException {
        createDataWithEmptyAddressArray();
        createConfigWithMultipleFieldsValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        // Should throw on first field that validates parent
        IOException exception = assertThrows(IOException.class, () -> {
            service.getResolvedContext();
        });

        assertTrue(exception.getMessage().contains("Parent collection validation failed"));
        assertTrue(exception.getMessage().contains("is empty"));
    }

    @Test
    void testNestedArrayValidation() throws IOException {
        createDataWithNestedArray();
        createConfigWithNestedArrayValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        Map<String, Object> context = service.getResolvedContext();

        // Should validate nested parent collection
        assertEquals("555-1234", context.get("phoneNumber"));
    }

    @Test
    void testNestedArrayValidation_EmptyParent() throws IOException {
        createDataWithEmptyNestedArray();
        createConfigWithNestedArrayValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        // Should throw when nested parent array is empty
        IOException exception = assertThrows(IOException.class, () -> {
            service.getResolvedContext();
        });

        assertTrue(exception.getMessage().contains("Parent collection validation failed"));
        assertTrue(exception.getMessage().contains("contacts"));
    }

    @Test
    void testParentValidationWithConditionalSelection() throws IOException {
        createDataWithMultipleAddressTypes();
        createConfigWithConditionalAndParentValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        Map<String, Object> context = service.getResolvedContext();

        // Should validate parent exists, then use conditional selection
        assertEquals("123 Main St", context.get("street")); // MAILING
    }

    @Test
    void testOptionalFieldWithParentValidation() throws IOException {
        createDataWithValidAddress();
        createConfigWithOptionalFieldAndParentValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        Map<String, Object> context = service.getResolvedContext();

        // Optional field should still validate parent collection is not empty
        assertNotNull(context.get("streetLine2"));
    }

    @Test
    void testErrorMessageQuality() throws IOException {
        createDataWithEmptyAddressArray();
        createConfigWithParentValidation();

        EnhancedContentGenerationServiceV2 service = new EnhancedContentGenerationServiceV2(
                "test-config.json",
                tempDir.toString()
        );

        IOException exception = assertThrows(IOException.class, () -> {
            service.getResolvedContext();
        });

        String message = exception.getMessage();

        // Verify error message contains all necessary information
        assertTrue(message.contains("street")); // placeholder name
        assertTrue(message.contains("$.address")); // parent path
        assertTrue(message.contains("sales")); // source name
        assertTrue(message.contains("empty")); // what's wrong
        assertTrue(message.contains("At least one element is required")); // helpful hint
    }

    // Helper methods to create test data

    private void createDataWithValidAddress() throws IOException {
        String data = """
            {
                "address": [
                    {
                        "type": "MAILING",
                        "street": "123 Main St",
                        "city": "Anytown",
                        "state": "CA",
                        "zip": "12345"
                    }
                ]
            }
            """;
        Files.writeString(tempDir.resolve("data.json"), data);
    }

    private void createDataWithEmptyAddressArray() throws IOException {
        String data = """
            {
                "address": []
            }
            """;
        Files.writeString(tempDir.resolve("data.json"), data);
    }

    private void createDataWithoutAddressField() throws IOException {
        String data = """
            {
                "name": "Test Data",
                "otherField": "value"
            }
            """;
        Files.writeString(tempDir.resolve("data.json"), data);
    }

    private void createDataWithNestedArray() throws IOException {
        String data = """
            {
                "customer": {
                    "contacts": [
                        {
                            "type": "PRIMARY",
                            "phone": "555-1234"
                        }
                    ]
                }
            }
            """;
        Files.writeString(tempDir.resolve("data.json"), data);
    }

    private void createDataWithEmptyNestedArray() throws IOException {
        String data = """
            {
                "customer": {
                    "contacts": []
                }
            }
            """;
        Files.writeString(tempDir.resolve("data.json"), data);
    }

    private void createDataWithMultipleAddressTypes() throws IOException {
        String data = """
            {
                "address": [
                    {
                        "type": "MAILING",
                        "street": "123 Main St",
                        "city": "Anytown",
                        "state": "CA",
                        "zip": "12345"
                    },
                    {
                        "type": "PHYSICAL",
                        "street": "456 Elm St",
                        "city": "Othertown",
                        "state": "NY",
                        "zip": "67890"
                    }
                ]
            }
            """;
        Files.writeString(tempDir.resolve("data.json"), data);
    }

    // Helper methods to create test configs

    private void createConfigWithParentValidation() throws IOException {
        String config = """
            {
              "configVersion": "2.0",
              "dataSources": {
                "data": {"filePath": "data.json", "priority": 1}
              },
              "mappings": [
                {
                  "placeholder": "street",
                  "source": "data",
                  "jsonPath": "$.address[0].street",
                  "mandatory": true,
                  "parentCollectionPath": "$.address",
                  "validateParentNotEmpty": true
                },
                {
                  "placeholder": "city",
                  "source": "data",
                  "jsonPath": "$.address[0].city",
                  "mandatory": true,
                  "parentCollectionPath": "$.address",
                  "validateParentNotEmpty": true
                }
              ],
              "formatters": {
                "date": {"defaultInputFormat": "yyyy-MM-dd", "defaultOutputFormat": "MM/dd/yyyy", "locale": "en_US", "timezone": "UTC"},
                "currency": {"defaultCurrency": "USD", "defaultLocale": "en_US", "symbolPosition": "before", "decimalPlaces": 2},
                "number": {"defaultLocale": "en_US", "useGrouping": true, "minimumFractionDigits": 0, "maximumFractionDigits": 2}
              },
              "options": {
                "cacheDataSources": false,
                "strictMode": false,
                "defaultValue": "",
                "enableLogging": false,
                "enableConditionalSelection": true,
                "enableFormatters": true
              }
            }
            """;
        Files.writeString(tempDir.resolve("test-config.json"), config);
    }

    private void createConfigWithoutParentValidation() throws IOException {
        String config = """
            {
              "configVersion": "2.0",
              "dataSources": {
                "data": {"filePath": "data.json", "priority": 1}
              },
              "mappings": [
                {
                  "placeholder": "street",
                  "source": "data",
                  "jsonPath": "$.address[0].street",
                  "mandatory": false,
                  "defaultValue": ""
                }
              ],
              "formatters": {
                "date": {"defaultInputFormat": "yyyy-MM-dd", "defaultOutputFormat": "MM/dd/yyyy", "locale": "en_US", "timezone": "UTC"},
                "currency": {"defaultCurrency": "USD", "defaultLocale": "en_US", "symbolPosition": "before", "decimalPlaces": 2},
                "number": {"defaultLocale": "en_US", "useGrouping": true, "minimumFractionDigits": 0, "maximumFractionDigits": 2}
              },
              "options": {
                "cacheDataSources": false,
                "strictMode": false,
                "defaultValue": "",
                "enableLogging": false,
                "enableConditionalSelection": true,
                "enableFormatters": true
              }
            }
            """;
        Files.writeString(tempDir.resolve("test-config.json"), config);
    }

    private void createConfigWithMultipleFieldsValidation() throws IOException {
        String config = """
            {
              "configVersion": "2.0",
              "dataSources": {
                "data": {"filePath": "data.json", "priority": 1}
              },
              "mappings": [
                {
                  "placeholder": "street",
                  "source": "data",
                  "jsonPath": "$.address[0].street",
                  "mandatory": true,
                  "parentCollectionPath": "$.address",
                  "validateParentNotEmpty": true
                },
                {
                  "placeholder": "city",
                  "source": "data",
                  "jsonPath": "$.address[0].city",
                  "mandatory": true,
                  "parentCollectionPath": "$.address",
                  "validateParentNotEmpty": true
                },
                {
                  "placeholder": "state",
                  "source": "data",
                  "jsonPath": "$.address[0].state",
                  "mandatory": true,
                  "parentCollectionPath": "$.address",
                  "validateParentNotEmpty": true
                },
                {
                  "placeholder": "zipCode",
                  "source": "data",
                  "jsonPath": "$.address[0].zip",
                  "mandatory": true,
                  "parentCollectionPath": "$.address",
                  "validateParentNotEmpty": true
                }
              ],
              "formatters": {
                "date": {"defaultInputFormat": "yyyy-MM-dd", "defaultOutputFormat": "MM/dd/yyyy", "locale": "en_US", "timezone": "UTC"},
                "currency": {"defaultCurrency": "USD", "defaultLocale": "en_US", "symbolPosition": "before", "decimalPlaces": 2},
                "number": {"defaultLocale": "en_US", "useGrouping": true, "minimumFractionDigits": 0, "maximumFractionDigits": 2}
              },
              "options": {
                "cacheDataSources": false,
                "strictMode": false,
                "defaultValue": "",
                "enableLogging": false,
                "enableConditionalSelection": true,
                "enableFormatters": true
              }
            }
            """;
        Files.writeString(tempDir.resolve("test-config.json"), config);
    }

    private void createConfigWithNestedArrayValidation() throws IOException {
        String config = """
            {
              "configVersion": "2.0",
              "dataSources": {
                "data": {"filePath": "data.json", "priority": 1}
              },
              "mappings": [
                {
                  "placeholder": "phoneNumber",
                  "source": "data",
                  "jsonPath": "$.customer.contacts[0].phone",
                  "mandatory": true,
                  "parentCollectionPath": "$.customer.contacts",
                  "validateParentNotEmpty": true
                }
              ],
              "formatters": {
                "date": {"defaultInputFormat": "yyyy-MM-dd", "defaultOutputFormat": "MM/dd/yyyy", "locale": "en_US", "timezone": "UTC"},
                "currency": {"defaultCurrency": "USD", "defaultLocale": "en_US", "symbolPosition": "before", "decimalPlaces": 2},
                "number": {"defaultLocale": "en_US", "useGrouping": true, "minimumFractionDigits": 0, "maximumFractionDigits": 2}
              },
              "options": {
                "cacheDataSources": false,
                "strictMode": false,
                "defaultValue": "",
                "enableLogging": false,
                "enableConditionalSelection": true,
                "enableFormatters": true
              }
            }
            """;
        Files.writeString(tempDir.resolve("test-config.json"), config);
    }

    private void createConfigWithConditionalAndParentValidation() throws IOException {
        String config = """
            {
              "configVersion": "2.0",
              "dataSources": {
                "data": {"filePath": "data.json", "priority": 1}
              },
              "mappings": [
                {
                  "placeholder": "street",
                  "source": "data",
                  "jsonPath": "$.address[?(@.type == 'MAILING')].street",
                  "mandatory": true,
                  "parentCollectionPath": "$.address",
                  "validateParentNotEmpty": true,
                  "conditionalSelection": {
                    "enabled": true,
                    "conditions": [
                      {"jsonPath": "$.address[?(@.type == 'MAILING')].street"},
                      {"jsonPath": "$.address[?(@.type == 'PHYSICAL')].street"}
                    ],
                    "extractFirstElement": true
                  }
                }
              ],
              "formatters": {
                "date": {"defaultInputFormat": "yyyy-MM-dd", "defaultOutputFormat": "MM/dd/yyyy", "locale": "en_US", "timezone": "UTC"},
                "currency": {"defaultCurrency": "USD", "defaultLocale": "en_US", "symbolPosition": "before", "decimalPlaces": 2},
                "number": {"defaultLocale": "en_US", "useGrouping": true, "minimumFractionDigits": 0, "maximumFractionDigits": 2}
              },
              "options": {
                "cacheDataSources": false,
                "strictMode": false,
                "defaultValue": "",
                "enableLogging": false,
                "enableConditionalSelection": true,
                "enableFormatters": true
              }
            }
            """;
        Files.writeString(tempDir.resolve("test-config.json"), config);
    }

    private void createConfigWithOptionalFieldAndParentValidation() throws IOException {
        String config = """
            {
              "configVersion": "2.0",
              "dataSources": {
                "data": {"filePath": "data.json", "priority": 1}
              },
              "mappings": [
                {
                  "placeholder": "streetLine2",
                  "source": "data",
                  "jsonPath": "$.address[0].streetLine2",
                  "mandatory": false,
                  "defaultValue": "",
                  "parentCollectionPath": "$.address",
                  "validateParentNotEmpty": true
                }
              ],
              "formatters": {
                "date": {"defaultInputFormat": "yyyy-MM-dd", "defaultOutputFormat": "MM/dd/yyyy", "locale": "en_US", "timezone": "UTC"},
                "currency": {"defaultCurrency": "USD", "defaultLocale": "en_US", "symbolPosition": "before", "decimalPlaces": 2},
                "number": {"defaultLocale": "en_US", "useGrouping": true, "minimumFractionDigits": 0, "maximumFractionDigits": 2}
              },
              "options": {
                "cacheDataSources": false,
                "strictMode": false,
                "defaultValue": "",
                "enableLogging": false,
                "enableConditionalSelection": true,
                "enableFormatters": true
              }
            }
            """;
        Files.writeString(tempDir.resolve("test-config.json"), config);
    }
}