# Content Generation Service

A high-performance Java 21 service for generating content from Handlebars templates with data scattered across multiple JSON files.

## Features

- ✅ **Multi-source data resolution**: Resolve template placeholders from multiple JSON files
- ✅ **Fallback mechanism**: Define fallback data sources for resilient resolution
- ✅ **JsonPath support**: Flexible JSON querying with JsonPath expressions
- ✅ **Caching**: Optional in-memory caching for improved performance
- ✅ **Strict/Lenient modes**: Choose between strict validation or graceful degradation
- ✅ **Java 21**: Modern Java with records and latest features
- ✅ **High performance**: Uses Jackson for efficient JSON processing
- ✅ **Handlebars templates**: Full Handlebars.java support

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  ContentGenerationService                    │
│  - Orchestrates the entire content generation process       │
│  - Manages configuration and service lifecycle               │
└─────────────────────┬───────────────────────────────────────┘
                      │
         ┌────────────┴────────────┐
         │                         │
┌────────▼──────────┐    ┌────────▼──────────┐
│ PlaceholderResolver│    │ DataSourceResolver│
│ - Maps placeholders│    │ - Loads JSON files│
│ - Handles fallbacks│    │ - Caches data     │
└────────────────────┘    │ - JsonPath queries│
                          └───────────────────┘
```

## Quick Start

### 1. Add Dependencies

```xml
<dependency>
    <groupId>com.github.jknack</groupId>
    <artifactId>handlebars</artifactId>
    <version>4.3.1</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.0</version>
</dependency>
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>2.9.0</version>
</dependency>
```

### 2. Create Configuration

Create `data-mapping-config.json`:

```json
{
  "configVersion": "1.0",
  "dataSources": {
    "sales": {
      "filePath": "sales.json",
      "priority": 1
    },
    "inventory": {
      "filePath": "inventory.json",
      "priority": 2
    }
  },
  "mappings": [
    {
      "placeholder": "reportDate",
      "source": "sales",
      "jsonPath": "$.reportDate",
      "fallbackSources": [
        {
          "source": "inventory",
          "jsonPath": "$.reportDate"
        }
      ]
    }
  ],
  "options": {
    "cacheDataSources": true,
    "strictMode": false,
    "defaultValue": "",
    "enableLogging": true
  }
}
```

### 3. Use the Service

```java
// Initialize service
ContentGenerationService service = new ContentGenerationService(
    "data-mapping-config.json",
    "/path/to/data/directory"
);

// Generate content from template file
String result = service.generateContent("report-template.json");

// Or use inline template
String template = "Report Date: {{reportDate}}, Author: {{authorName}}";
String result = service.generateContentFromString(template);

// Get as JSON
JsonNode jsonResult = service.generateContentAsJson("report-template.json");
```

## Configuration Reference

### Data Sources

Define all your JSON data files:

```json
"dataSources": {
  "sourceName": {
    "filePath": "path/to/file.json",
    "priority": 1
  }
}
```

- **sourceName**: Unique identifier for the data source
- **filePath**: Relative path from base directory
- **priority**: Used for ordering (lower = higher priority)

### Mappings

Map Handlebars placeholders to JSON paths:

```json
"mappings": [
  {
    "placeholder": "authorName",
    "source": "sales",
    "jsonPath": "$.author",
    "fallbackSources": [
      {
        "source": "inventory",
        "jsonPath": "$.authorName"
      }
    ]
  }
]
```

- **placeholder**: The Handlebars variable name (without `{{}}`)
- **source**: Primary data source name
- **jsonPath**: JsonPath expression to extract value
- **fallbackSources**: Optional array of fallback sources

### JsonPath Examples

```json
"$.author"                    // Simple field
"$.inventory[0].name"         // Array element
"$.address.city"              // Nested field
"$.items[?(@.price > 100)]"   // Filter
"$..name"                     // Recursive descent
```

### Options

```json
"options": {
  "cacheDataSources": true,    // Cache JSON files in memory
  "strictMode": false,         // Throw error on missing placeholder
  "defaultValue": "",          // Default value for missing placeholders
  "enableLogging": true        // Enable detailed logging
}
```

## Advanced Usage

### Custom Context

Add runtime values to the context:

```java
Map<String, Object> customContext = Map.of(
    "timestamp", System.currentTimeMillis(),
    "user", getCurrentUser()
);

String result = service.generateContentWithContext(
    "template.json",
    customContext
);
```

### Debugging

Get the resolved context without generating content:

```java
Map<String, Object> context = service.getResolvedContext();
context.forEach((key, value) -> 
    System.out.println(key + " = " + value)
);
```

### Statistics

Monitor service performance:

```java
Map<String, Object> stats = service.getStatistics();
System.out.println("Cached sources: " + stats.get("cacheStats"));
```

### Cache Management

```java
// Clear cache to free memory
service.clearCache();

// Cache will be rebuilt on next generation
String result = service.generateContent("template.json");
```

## Performance Optimization

### 1. Enable Caching

```json
"options": {
  "cacheDataSources": true
}
```

Benefits:
- Data files loaded once and cached in memory
- Subsequent generations are 10-100x faster
- Ideal for templates generated frequently

Trade-offs:
- Increased memory usage
- Changes to data files not reflected until cache cleared

### 2. JsonPath Optimization

**Good**: Direct paths
```json
"jsonPath": "$.author.name"
```

**Avoid**: Expensive operations
```json
"jsonPath": "$..name"  // Recursive search - slow on large files
"jsonPath": "$.items[*]"  // Returns entire array
```

### 3. Data Source Priority

Order sources by access frequency:

```json
"dataSources": {
  "frequently_used": {"priority": 1},
  "rarely_used": {"priority": 99}
}
```

### 4. Preload Data

For batch processing:

```java
ContentGenerationService service = new ContentGenerationService(config, basePath);
// Data is preloaded if cacheDataSources=true

// Fast generation in loop
for (Template template : templates) {
    String result = service.generateContent(template);
}
```

## Memory Considerations

### Estimated Memory Usage

For a service with:
- 10 data sources
- 1MB average file size
- Caching enabled

**Memory**: ~10MB for cached data + ~5MB for service overhead = **~15MB**

### Large Files

For data files > 10MB:

1. **Disable caching**:
```json
"options": {"cacheDataSources": false}
```

2. **Use specific JsonPath queries** to extract only needed data
3. **Split large files** into smaller, domain-specific files

## Best Practices

### 1. Configuration Organization

```
project/
├── config/
│   ├── dev-config.json
│   ├── prod-config.json
│   └── test-config.json
├── data/
│   ├── sales.json
│   ├── inventory.json
│   └── office.json
└── templates/
    ├── report-template.json
    └── email-template.json
```

### 2. Naming Conventions

- **Data sources**: Use domain names (`sales`, `inventory`, `users`)
- **Placeholders**: Use camelCase (`reportDate`, `authorName`)
- **Files**: Use kebab-case (`sales-data.json`, `user-profile.json`)

### 3. Fallback Strategy

Always define fallbacks for critical placeholders:

```json
{
  "placeholder": "authorName",
  "source": "sales",
  "jsonPath": "$.author",
  "fallbackSources": [
    {"source": "inventory", "jsonPath": "$.authorName"},
    {"source": "office", "jsonPath": "$.authorName"}
  ]
}
```

### 4. Error Handling

```java
try {
    String result = service.generateContent("template.json");
} catch (IOException e) {
    logger.error("Content generation failed", e);
    // Handle missing files, invalid JSON, etc.
}
```

### 5. Testing

Create test-specific configurations:

```java
@Test
void testGeneration() {
    ContentGenerationService service = new ContentGenerationService(
        "test-config.json",
        testDataDirectory
    );
    
    String result = service.generateContent("test-template.json");
    
    assertNotNull(result);
    assertTrue(result.contains("expected value"));
}
```

## Library Choices

### Why These Libraries?

1. **Jackson** (JSON processing)
   - Fastest Java JSON library
   - Low memory footprint
   - Streaming support for large files
   - Wide industry adoption

2. **Handlebars.java** (Templating)
   - Full Handlebars spec compliance
   - Efficient template caching
   - Extensible with helpers

3. **JsonPath** (Querying)
   - Flexible path expressions
   - Null-safe operations
   - Integration with Jackson

## Troubleshooting

### Placeholder Not Resolved

**Problem**: `{{placeholder}}` appears in output

**Solutions**:
1. Check mapping exists in config
2. Verify JsonPath expression
3. Enable logging to see resolution attempts
4. Check data source file exists and is valid JSON

### OutOfMemoryError

**Problem**: Service crashes with OOM

**Solutions**:
1. Disable caching: `"cacheDataSources": false`
2. Reduce number of data sources
3. Split large JSON files
4. Increase JVM heap: `-Xmx2g`

### Slow Performance

**Problem**: Generation takes too long

**Solutions**:
1. Enable caching
2. Optimize JsonPath queries (avoid `$..` and `[*]`)
3. Reduce fallback chains
4. Profile with JVM tools

## License

This project uses open-source libraries:
- Handlebars.java (Apache 2.0)
- Jackson (Apache 2.0)
- JsonPath (Apache 2.0)

## Support

For issues or questions, check:
- JsonPath syntax: https://github.com/json-path/JsonPath
- Handlebars syntax: https://handlebarsjs.com/
- Jackson documentation: https://github.com/FasterXML/jackson
