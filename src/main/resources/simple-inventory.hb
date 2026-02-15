{
  "title": "Simple Inventory Report",
  "date": "{{reportDate}}",
  "author": "{{authorName}}",

  "inventory": [
    {{#each inventoryItems}}
    {
      "itemId": {{id}},
      "productName": "{{name}}",
      "inStock": {{quantity}},
      "pricePerUnit": {{price}}
    }{{#unless @last}},{{/unless}}
    {{/each}}
  ],

  "summary": {
    "totalItemTypes": {{arraySize inventoryItems}},
    "totalValue": "{{totalInventoryValue}}"
  }
}