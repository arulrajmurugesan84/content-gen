{
  "reportTitle": "Comprehensive Inventory and Sales Report",
  "reportDate": "{{reportDate}}",
  "generatedOn": "{{generationDate}}",
  "author": "{{authorName}}",

  "contactInfo": {
    "email": "{{contactEmail}}"
  },

  "officeAddress": {
    "street": "{{street}}",
    "city": "{{city}}",
    "state": "{{state}}",
    "zipCode": "{{zipCode}}"
  },

  "inventorySummary": {
    "totalItems": "{{inventoryCount}}",
    "totalValue": "{{totalInventoryValue}}",

    "items": [
      {{#each inventoryItems}}
      {
        "id": {{id}},
      "name": "{{name}}",
      "quantity": {{quantity}},
      "price": {{price}},
      "totalValue": {{multiply quantity price}}
    }{{#unless @last}},{{/unless}}
      {{/each}}
    ]
  },

  "detailedInventoryReport": {
    "title": "Item-by-Item Breakdown",
    "entries": [
      {{#each inventoryItems}}
      {
        "itemNumber": {{add @index 1}},
      "productName": "{{name}}",
      "stockLevel": {{quantity}},
      "unitPrice": {{price}},
      "lineTotal": {{multiply quantity price}},
      "status": "{{#ifGt quantity 20}}In Stock{{else}}{{#ifGt quantity 10}}Low Stock{{else}}Critical{{/ifGt}}{{/ifGt}}"
      }{{#unless @last}},{{/unless}}
      {{/each}}
    ]
  },

  "highValueItems": {
    "description": "Items with price over $500",
    "items": [
      {{#each inventoryItems}}
      {{#ifGt price 500}}
      {
        "name": "{{name}}",
        "price": {{price}},
      "quantity": {{quantity}}
    }{{#unless @last}},{{/unless}}
      {{/ifGt}}
      {{/each}}
    ]
  },

  "inventoryByCategory": {
    "electronics": [
      {{#each inventoryItems}}
      {{#ifEquals name "Laptop"}}
      {
        "product": "{{name}}",
        "available": {{quantity}}
    }{{#unless @last}},{{/unless}}
      {{/ifEquals}}
      {{#ifEquals name "Smartphone"}}
      {
        "product": "{{name}}",
        "available": {{quantity}}
    }{{#unless @last}},{{/unless}}
      {{/ifEquals}}
      {{/each}}
    ],
    "accessories": [
      {{#each inventoryItems}}
      {{#ifEquals name "Headphones"}}
      {
        "product": "{{name}}",
        "available": {{quantity}}
    }
      {{/ifEquals}}
      {{/each}}
    ]
  },

  "formattedReport": {
    "note": "This section demonstrates formatters applied via config",
    "reportDate": "{{reportDate}}",
    "totalInventoryValue": "{{totalInventoryValue}}",
    "itemCount": "{{inventoryCount}}"
  },

  "metadata": {
    "generatedBy": "Enhanced Content Generation Service v2.0",
    "timestamp": "{{generationDate}}",
    "addressType": "MAILING address preferred, PHYSICAL as fallback"
  }
}