# Project Structure

```
content-generation-service/
├── pom.xml                                    # Maven build configuration
├── README.md                                  # Main documentation
├── data-mapping-config.json                   # Basic configuration example
├── advanced-config.json                       # Advanced configuration example
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── contentgen/
│   │   │           ├── config/
│   │   │           │   └── ContentGenerationConfig.java    # Configuration model
│   │   │           │
│   │   │           ├── service/
│   │   │           │   ├── ContentGenerationService.java   # Main service
│   │   │           │   ├── DataSourceResolver.java         # Data loading & caching
│   │   │           │   └── PlaceholderResolver.java        # Placeholder mapping
│   │   │           │
│   │   │           ├── spring/                             # Spring Boot integration (optional)
│   │   │           │   ├── ContentGenerationServiceConfig.java
│   │   │           │   └── ContentGenerationController.java
│   │   │           │
│   │   │           └── ContentGenerationDemo.java          # Demo application
│   │   │
│   │   └── resources/
│   │       └── logback.xml                    # Logging configuration
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── contentgen/
│                   └── service/
│                       └── ContentGenerationServiceTest.java  # Unit tests
│
└── data/                                      # Your data directory (create this)
    ├── data-mapping-config.json              # Copy your config here
    ├── report-template.json                  # Your templates
    ├── sales.json                            # Your data files
    ├── inventory.json
    └── office.json
```

## Getting Started

### Step 1: Build the Project

```bash
mvn clean install
```

### Step 2: Set Up Your Data Directory

Create a `data` directory in your project root and copy your files:

```bash
mkdir data
cp inventory.json data/
cp office.json data/
cp sales.json data/
cp report-template.json data/
cp data-mapping-config.json data/
```

### Step 3: Run the Demo

```bash
mvn exec:java -Dexec.mainClass="com.contentgen.ContentGenerationDemo"
```

Or using Java directly:

```bash
java -cp target/content-generation-service-1.0.0.jar:target/lib/* \
     com.contentgen.ContentGenerationDemo
```

## Integration Patterns

### Pattern 1: Standalone Service

```java
public class MyApplication {
    public static void main(String[] args) throws IOException {
        ContentGenerationService service = new ContentGenerationService(
            "config/data-mapping-config.json",
            "."
        );
        
        String result = service.generateContent("templates/report.json");
        System.out.println(result);
    }
}
```

### Pattern 2: Spring Boot Application

#### application.yml
```yaml
content:
  generation:
    config-file: classpath:data-mapping-config.json
    base-path: /data
```

#### Controller
```java
@RestController
public class MyController {
    
    @Autowired
    private ContentGenerationService service;
    
    @GetMapping("/generate")
    public String generate() throws IOException {
        return service.generateContent("template.json");
    }
}
```

### Pattern 3: Batch Processing

```java
public class BatchReportGenerator {
    
    private final ContentGenerationService service;
    
    public void generateMonthlyReports(List<String> months) throws IOException {
        // Service caches data sources once
        for (String month : months) {
            Map<String, Object> context = Map.of("month", month);
            String report = service.generateContentWithContext(
                "monthly-template.json",
                context
            );
            
            saveReport(month, report);
        }
    }
}
```

### Pattern 4: Multi-Tenant

```java
public class MultiTenantService {
    
    private final Map<String, ContentGenerationService> tenantServices;
    
    public MultiTenantService() {
        tenantServices = new ConcurrentHashMap<>();
    }
    
    public String generateForTenant(String tenantId, String template) 
            throws IOException {
        
        ContentGenerationService service = tenantServices.computeIfAbsent(
            tenantId,
            id -> {
                try {
                    return new ContentGenerationService(
                        "config/tenant-" + id + "-config.json",
                        "data/tenant-" + id
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
        
        return service.generateContent(template);
    }
}
```

## Configuration Examples

### Example 1: Simple Report

**Data Source** (`sales.json`):
```json
{
  "reportDate": "2024-06-30",
  "totalSales": 125000,
  "topProduct": "Widget X"
}
```

**Template** (`report-template.json`):
```json
{
  "title": "Sales Report",
  "date": "{{reportDate}}",
  "summary": "Total sales: ${{totalSales}}",
  "highlight": "Top product: {{topProduct}}"
}
```

**Config**:
```json
{
  "dataSources": {
    "sales": {"filePath": "sales.json", "priority": 1}
  },
  "mappings": [
    {"placeholder": "reportDate", "source": "sales", "jsonPath": "$.reportDate"},
    {"placeholder": "totalSales", "source": "sales", "jsonPath": "$.totalSales"},
    {"placeholder": "topProduct", "source": "sales", "jsonPath": "$.topProduct"}
  ],
  "options": {
    "cacheDataSources": true,
    "strictMode": false,
    "defaultValue": "N/A",
    "enableLogging": true
  }
}
```

### Example 2: Nested Data

**Data Source** (`employees.json`):
```json
{
  "employees": [
    {
      "id": 1,
      "name": "John Doe",
      "department": "Engineering",
      "salary": 100000
    },
    {
      "id": 2,
      "name": "Jane Smith",
      "department": "Sales",
      "salary": 95000
    }
  ]
}
```

**Config with JsonPath**:
```json
{
  "mappings": [
    {
      "placeholder": "firstEmployeeName",
      "source": "employees",
      "jsonPath": "$.employees[0].name"
    },
    {
      "placeholder": "engineeringEmployees",
      "source": "employees",
      "jsonPath": "$.employees[?(@.department == 'Engineering')]"
    },
    {
      "placeholder": "highEarners",
      "source": "employees",
      "jsonPath": "$.employees[?(@.salary > 95000)]"
    }
  ]
}
```

### Example 3: Multi-Source with Fallbacks

**Use Case**: Get author from multiple sources with fallback

**Config**:
```json
{
  "dataSources": {
    "primary": {"filePath": "primary.json", "priority": 1},
    "secondary": {"filePath": "secondary.json", "priority": 2},
    "default": {"filePath": "default.json", "priority": 3}
  },
  "mappings": [
    {
      "placeholder": "authorName",
      "source": "primary",
      "jsonPath": "$.author.name",
      "fallbackSources": [
        {"source": "secondary", "jsonPath": "$.metadata.author"},
        {"source": "default", "jsonPath": "$.defaultAuthor"}
      ]
    }
  ]
}
```

## Environment-Specific Configurations

### Development
```json
{
  "options": {
    "cacheDataSources": false,
    "strictMode": true,
    "enableLogging": true
  }
}
```

### Production
```json
{
  "options": {
    "cacheDataSources": true,
    "strictMode": false,
    "enableLogging": false
  }
}
```

## Common Use Cases

### 1. Email Generation
```java
String emailTemplate = """
    Dear {{recipientName}},
    
    Your order #{{orderId}} has been shipped.
    Tracking: {{trackingNumber}}
    
    Best regards,
    {{senderName}}
    """;

String email = service.generateContentFromString(emailTemplate);
```

### 2. PDF Report Generation
```java
// Generate JSON content
JsonNode reportData = service.generateContentAsJson("report-template.json");

// Use with PDF library (e.g., iText, Apache PDFBox)
PdfGenerator.create(reportData);
```

### 3. Dynamic HTML Pages
```java
String htmlTemplate = Files.readString(Path.of("page-template.html"));
String html = service.generateContentFromString(htmlTemplate);

// Serve via web framework
return ResponseEntity.ok(html);
```

### 4. Configuration File Generation
```java
String configTemplate = """
    server:
      port: {{serverPort}}
      host: {{serverHost}}
    database:
      url: {{dbUrl}}
      username: {{dbUser}}
    """;

String config = service.generateContentFromString(configTemplate);
Files.writeString(Path.of("application.yml"), config);
```

## Troubleshooting

### Issue: "Configuration file not found"
**Solution**: Ensure the config file path is relative to the base path:
```java
new ContentGenerationService(
    "data-mapping-config.json",  // Filename only
    "/absolute/path/to/data"     // Full path to directory
)
```

### Issue: "Data source file not found"
**Solution**: Check that `filePath` in config is relative to base path:
```json
{
  "dataSources": {
    "sales": {
      "filePath": "sales.json"  // Not "/data/sales.json"
    }
  }
}
```

### Issue: Placeholder not resolved
**Solution**: 
1. Check spelling in template and config
2. Verify JsonPath expression
3. Enable logging to see resolution attempts
4. Test JsonPath at: https://jsonpath.com/

### Issue: High memory usage
**Solution**:
1. Disable caching: `"cacheDataSources": false`
2. Split large JSON files into smaller ones
3. Use specific JsonPath queries instead of broad ones

## Next Steps

1. Review the example configurations in this document
2. Adapt the config to your data structure
3. Run the demo to verify setup
4. Write unit tests for your specific use cases
5. Integrate into your application using one of the patterns above

For more details, see the main [README.md](README.md).
