# Mandatory/Optional Fields - Quick Reference

## Configuration Syntax

```json
{
  "placeholder": "fieldName",
  "source": "dataSource",
  "jsonPath": "$.path",
  "mandatory": true|false,    // Is this field required?
  "defaultValue": "string"    // Value if optional and missing
}
```

## Mandatory Field

```json
{
  "placeholder": "customerName",
  "source": "sales",
  "jsonPath": "$.customerName",
  "mandatory": true
}
```

| Data Present | Result |
|--------------|--------|
| ✅ Yes | Uses the value |
| ❌ No | **Throws IOException** |

## Optional Field

```json
{
  "placeholder": "phoneNumber",
  "source": "sales",
  "jsonPath": "$.phoneNumber",
  "mandatory": false,
  "defaultValue": "Not Provided"
}
```

| Data Present | Result |
|--------------|--------|
| ✅ Yes | Uses the value |
| ❌ No | Uses `"Not Provided"` |

## Common Patterns

### Pattern 1: Address Fields
```json
{
  "mappings": [
    {
      "placeholder": "street",
      "mandatory": true,
      "description": "Required - Address Line 1"
    },
    {
      "placeholder": "streetLine2",
      "mandatory": false,
      "defaultValue": "",
      "description": "Optional - Address Line 2"
    }
  ]
}
```

### Pattern 2: Contact Information
```json
{
  "mappings": [
    {
      "placeholder": "email",
      "mandatory": true,
      "description": "Required - Email"
    },
    {
      "placeholder": "phone",
      "mandatory": false,
      "defaultValue": "N/A",
      "description": "Optional - Phone"
    },
    {
      "placeholder": "fax",
      "mandatory": false,
      "defaultValue": "",
      "description": "Optional - Fax (empty if missing)"
    }
  ]
}
```

### Pattern 3: With Conditional Selection
```json
{
  "placeholder": "street",
  "mandatory": true,
  "conditionalSelection": {
    "enabled": true,
    "conditions": [
      {"jsonPath": "$.address[?(@.type == 'MAILING')].street"},
      {"jsonPath": "$.address[?(@.type == 'PHYSICAL')].street"}
    ],
    "extractFirstElement": true
  }
}
```

Tries all conditions, throws error if **all** fail.

### Pattern 4: With Fallback Sources
```json
{
  "placeholder": "author",
  "mandatory": true,
  "fallbackSources": [
    {"source": "inventory", "jsonPath": "$.authorName"},
    {"source": "office", "jsonPath": "$.authorName"}
  ]
}
```

Tries all sources, throws error if **all** fail.

## Error Messages

### Mandatory Field Missing
```
IOException: Mandatory placeholder 'street' could not be resolved. 
Check data source 'sales' and path '$.address[0].street'
```

### What It Tells You
- **Placeholder:** `street`
- **Data Source:** `sales`
- **Path:** `$.address[0].street`

## Java Error Handling

```java
try {
    service.generateContent("template.json");
} catch (IOException e) {
    if (e.getMessage().contains("Mandatory placeholder")) {
        // Handle missing required field
        logger.error("Missing required data: {}", e.getMessage());
    }
}
```

## Decision Guide

**Use Mandatory When:**
- ✅ Field is critical for document generation
- ✅ Template cannot work without this data
- ✅ Want to fail fast and alert developers

**Use Optional When:**
- ✅ Field is nice-to-have but not critical
- ✅ Have reasonable default value
- ✅ Template can handle missing data gracefully

## Default Values by Use Case

| Use Case | Example Default |
|----------|----------------|
| Empty is OK | `""` |
| Not applicable | `"N/A"` |
| Not provided | `"Not Provided"` |
| Unknown | `"Unknown"` |
| To be determined | `"TBD"` |
| Zero/None | `"0"` or `"None"` |

## Testing Checklist

✅ Test with all fields present  
✅ Test with mandatory field missing (should throw)  
✅ Test with optional field missing (should use default)  
✅ Test error message clarity  
✅ Verify default values appear in output

## Complete Example

**Config:**
```json
{
  "mappings": [
    {
      "placeholder": "customerName",
      "source": "order",
      "jsonPath": "$.customer.name",
      "mandatory": true
    },
    {
      "placeholder": "customerMiddleName",
      "source": "order",
      "jsonPath": "$.customer.middleName",
      "mandatory": false,
      "defaultValue": ""
    },
    {
      "placeholder": "phoneNumber",
      "source": "order",
      "jsonPath": "$.customer.phone",
      "mandatory": false,
      "defaultValue": "Not Provided"
    }
  ]
}
```

**Data:**
```json
{
  "customer": {
    "name": "John Doe",
    "phone": "555-1234"
  }
}
```

**Result:**
- `customerName = "John Doe"` ✅
- `customerMiddleName = ""` (uses default) ✅
- `phoneNumber = "555-1234"` ✅

**Missing Name (Error):**
```json
{
  "customer": {
    "phone": "555-1234"
  }
}
```

**Result:**
```
IOException: Mandatory placeholder 'customerName' could not be resolved.
```

---

For detailed documentation, see: **MANDATORY_OPTIONAL_FIELDS.md**