# System Architecture & Flow Documentation

## Overview

This Coffee Shop Management System is built with JavaFX and follows a modular architecture with three separate user interfaces that share a common backend through the Store service layer.

## Architecture Layers

```
┌─────────────────────────────────────────────────┐
│         Presentation Layer (JavaFX)             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │Customer  │  │ Cashier  │  │  Admin   │     │
│  │   App    │  │   App    │  │   App    │     │
│  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│            Service Layer                        │
│              ┌──────────┐                       │
│              │  Store   │  (Singleton)          │
│              └──────────┘                       │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│          Persistence Layer                      │
│         ┌──────────────────┐                    │
│         │PersistenceManager│                    │
│         └──────────────────┘                    │
│                  ↓                               │
│         ┌──────────────────┐                    │
│         │   JSON Files     │                    │
│         │ (data/*.json)    │                    │
│         └──────────────────┘                    │
└─────────────────────────────────────────────────┘
```

## Data Models

### Product
```java
- id: String           // Unique identifier (e.g., "P001")
- name: String         // Product name (e.g., "Cappuccino")
- price: double        // Price in dollars
- stock: int           // Available servings (0-20)
- recipe: Map<String, Double>  // Ingredient → quantity mapping
```

### InventoryItem
```java
- name: String         // Ingredient name
- quantity: double     // Available amount
- unit: String         // Measurement unit (ml, g, pcs)
```

### Order
```java
- orderId: String      // Unique order identifier
- items: List<OrderItem>
- orderTime: LocalDateTime
- totalAmount: double
- isPaid: boolean
```

### OrderItem
```java
- product: Product
- quantity: int
- temperature: String   // "Hot" or "Cold"
- sugarLevel: int      // 0, 25, 50, 75, 100
- addOns: String       // Optional add-ons text
- addOnsCost: double   // Cost of add-ons
```

## Flow Diagrams

### Customer Flow

```
START (Customer App)
    ↓
Browse Products
    ↓
Select Product
    ↓
Customize (Temperature, Sugar, Add-ons)
    ↓
Add to Order Basket
    ↓
More items? → Yes (loop back to Select Product)
    ↓ No
Proceed to Checkout
    ↓
[Validation]
├─ Check Product Stock (isStockSufficient)
├─ Check Ingredient Availability (isInventorySufficient)
│
├─ FAIL → Show Error Message → END
│
└─ PASS → Display Order Summary
           ↓
       Show "Proceed to Cashier" message
           ↓
       Create New Order
           ↓
       END (ready for next customer)
```

### Cashier Flow

```
START (Cashier App)
    ↓
Create New Order
    ↓
Select Products & Customize
    ↓
Add Items to Order
    ↓
More items? → Yes (loop back)
    ↓ No
Payment Processing
    ↓
Enter Cash Amount
    ↓
totalPaid < orderTotal?
    ↓ Yes
Update Progress Bar (Red/Orange)
Show "Remaining: $X.XX"
Allow Additional Cash Entry → (loop back)
    ↓ No (totalPaid >= orderTotal)
[Final Validation]
├─ Re-check Stock
├─ Re-check Ingredients
│
└─ Call Store.checkoutBasket(order)
       ↓
   [checkoutBasket process]
   ├─ Deduct product.stock -= quantity
   ├─ Deduct inventory ingredients
   ├─ Save to JSON files
   ├─ Mark order as paid
   └─ Check for refill alerts
           ↓
   Calculate Change (if any)
           ↓
   Print Receipt
           ↓
   Show Refill Alerts (if stock ≤ 5)
           ↓
   Reset for Next Order
           ↓
   END
```

### Admin Flow - Refill Product

```
START (Admin App)
    ↓
Go to "Refill Status" Tab
    ↓
View Alert Table
├─ 🔴 CRITICAL (stock = 0)
├─ 🟡 WARNING (stock ≤ 5)
└─ ✓ OK (stock > 5)
    ↓
Go to "Product Management" Tab
    ↓
Select Product Needing Refill
    ↓
Click "Refill Product"
    ↓
Enter Refill Amount
    ↓
[Validation]
├─ Amount > 0?
└─ newStock = currentStock + amount
   (max 20)
    ↓
Store.refillProduct(productId, amount)
    ↓
product.stock = newStock
    ↓
PersistenceManager.save()
    ↓
Refresh Display
    ↓
Alert Clears (if stock > 5)
    ↓
END
```

### Admin Flow - Remove Product with Cascade

```
START (Admin App → Product Management)
    ↓
Select Product to Remove
    ↓
Click "Remove Product"
    ↓
[Confirmation Dialog]
    ↓
Choose Option:
├─ Option A: Remove Product Only
│       ↓
│   Store.removeProduct(productId)
│       ↓
│   Keep ingredients in inventory
│       ↓
│   PersistenceManager.save()
│       ↓
│   END
│
└─ Option B: Remove with Ingredients (Cascade)
        ↓
    [Cascade Confirmation]
        ↓
    Confirmed?
        ↓ Yes
    Store.removeProductWithIngredients(productId, true)
        ↓
    For each ingredient in product.recipe:
        inventory.remove(ingredientName)
        ↓
    Remove product from products list
        ↓
    PersistenceManager.save()
        ↓
    Refresh Display
        ↓
    END
```

## Key Validation Points

### Pre-Checkout Validation (Customer Side)
```java
// In CustomerModule before checkout
1. Store.isStockSufficient(order)
   - Check each item.quantity <= product.stock
   
2. Store.isInventorySufficient(order)
   - Calculate total ingredients needed
   - Check each ingredient.quantity >= required
```

### Pre-Deduction Validation (Cashier Side)
```java
// In CashierModule.completePayment()
1. Payment collected (totalPaid >= orderTotal)

2. Re-validate stock (race condition protection)
   - Store.isStockSufficient(order)
   
3. Re-validate ingredients
   - Store.isInventorySufficient(order)

4. Only then: Store.checkoutBasket(order)
```

### Admin Input Validation
```java
// Refill amount
- Must be > 0
- Result must not exceed MAX_STOCK (20)

// New product
- ID must be unique
- Price must be >= 0
- Name must not be empty

// New ingredient
- Name must not be empty
- Quantity must be > 0
- Unit must be specified
```

## Persistence Strategy

### Save Points
- After every order checkout (Store.checkoutBasket)
- After product refill (Store.refillProduct)
- After product removal (Store.removeProduct)
- After inventory changes (Store.refillInventory)
- After adding new items (Store.addProduct, Store.addInventoryItem)

### Load Points
- On application startup (Store constructor)
- Calls PersistenceManager.loadProducts() and loadInventory()

### Data Format (JSON)
```json
// products.json
[
  {
    "id": "P001",
    "name": "Espresso",
    "price": 3.50,
    "stock": 20,
    "recipe": {
      "Coffee Beans": 18.0,
      "Water": 30.0
    }
  }
]

// inventory.json
{
  "Coffee Beans": {
    "name": "Coffee Beans",
    "quantity": 5000.0,
    "unit": "g"
  }
}
```

## State Management

### Store (Singleton Pattern)
- Single instance shared across all apps
- Maintains in-memory cache of products and inventory
- Synchronizes with JSON files on every state change

### Order State
- Created in Customer or Cashier app
- Validated before checkout
- Processed through Store.checkoutBasket()
- Receipt generated from Order.printReceipt()

### Refill Alerts
- Checked after every checkout
- Store.hasProductsNeedingRefill() → true if any product.stock ≤ 5
- Store.getProductRefillAlerts() → formatted table of low-stock items

## Error Recovery

### Failed Checkout
- No changes made until Store.checkoutBasket() completes
- If exception occurs, order remains unpaid
- Inventory unchanged
- Can retry or cancel

### Concurrent Access (Future Enhancement)
Current implementation:
- File-based, last-write-wins
- No locking mechanism

Recommended for production:
- Add file locks during read/write
- Or migrate to database with transactions
- Add optimistic locking with version numbers

## Extension Points

### Adding New Product Types
1. Admin creates product with new recipe
2. Ensure required ingredients exist in inventory
3. Set initial stock and price
4. System handles rest automatically

### Adding Custom Add-ons
1. Cashier/Customer enters add-on text
2. Simple cost mapping in OrderItem
3. Can extend with Add-on class for complex pricing

### Multi-Location Support (Future)
1. Add Location entity
2. Separate inventory per location
3. Centralized product catalog
4. Location-specific stock levels

## Testing Strategy

### Unit Tests (Recommended)
- Store.isStockSufficient()
- Store.isInventorySufficient()
- Order.calculateTotal()
- Product.needsRefill()

### Integration Tests
- Full customer order → cashier checkout flow
- Admin refill → verify persistence
- Cascade delete → verify cleanup

### Manual Testing Checklist
See README.md section 7 for comprehensive test cases.

## Performance Considerations

### Current Implementation
- Synchronous file I/O (acceptable for single terminal)
- In-memory data structures (fast lookups)
- JSON serialization (readable, portable)

### Scalability Improvements
- Implement lazy loading for large product catalogs
- Add caching layer with TTL
- Move to database for multi-terminal support
- Add indexing for product lookups

## Security Considerations

### Current State
- No authentication (all apps trust each other)
- File-based storage (readable by anyone with file access)

### Production Recommendations
- Add user authentication per role
- Encrypt sensitive data in JSON
- Add audit logging for admin actions
- Implement session management
- Use HTTPS for remote terminals

---

**End of Architecture Documentation**

For quick start instructions, see `QUICK_START.md`
For detailed feature documentation, see `README.md`
