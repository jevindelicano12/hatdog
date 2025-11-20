# Customer-Cashier Order Integration

## Overview
Customer orders are now automatically saved to a text file and appear in the cashier's order queue. When payment is completed, the order status is updated to "COMPLETED" in the file.

## How It Works

### 1. Customer Places Order

**Customer App Workflow:**
1. Customer browses menu and adds items to order
2. Customer clicks **"Proceed to Checkout"**
3. System validates stock and ingredients
4. Customer enters their name
5. **Order is saved to `data/pending_orders.txt`**
6. Confirmation message: "Your order has been sent to the cashier"

**File Format:** `data/pending_orders.txt`
```
orderId|customerName|orderTime|totalAmount|status|[items]
```

**Example:**
```
a1b2c3d4|John Doe|2025-11-20 20:15:30|7.50|PENDING|[Espresso~3.5~1~Hot~50,Cappuccino~4.0~1~Cold~25]
```

### 2. Order Appears in Cashier Queue

**Cashier App Auto-Load:**
- When cashier app starts, it automatically loads all PENDING orders from file
- Orders appear in the order queue table
- Shows: Order ID, Time, Items count, Total, Status

**Order Queue Table Display:**
- Order #a1b2c3d4
- John Doe
- 2 items
- $7.50
- PENDING

### 3. Cashier Processes Order

**Processing Workflow:**
1. Cashier sees order in queue (already has items from customer)
2. Cashier clicks **"Process"** button
3. If order has items: Goes directly to payment panel
4. If order is empty: Opens dialog to add items manually
5. Payment processing proceeds as normal

### 4. Payment Completion Updates Status

**When Payment Complete:**
1. Cashier processes payment successfully
2. Receipt is generated
3. **Order status updated to "COMPLETED" in file**
4. Order marked as completed in system
5. Confirmation shows "Order marked as COMPLETED"

**Updated File Entry:**
```
a1b2c3d4|John Doe|2025-11-20 20:15:30|7.50|COMPLETED|[Espresso~3.5~1~Hot~50,Cappuccino~4.0~1~Cold~25]
```

## Status Values

- **PENDING** - Customer placed order, waiting for payment
- **COMPLETED** - Payment received, order fulfilled

## Files Created

### `data/pending_orders.txt`
Contains all customer orders with their current status.

**Format Breakdown:**
- **orderId**: Unique order identifier (e.g., "a1b2c3d4")
- **customerName**: Customer's name (e.g., "John Doe")
- **orderTime**: When order was placed (yyyy-MM-dd HH:mm:ss)
- **totalAmount**: Order total in dollars
- **status**: PENDING or COMPLETED
- **items**: Array of items [productName~price~quantity~temperature~sugarLevel,...]

## Benefits

✅ **Seamless Integration** - Customer orders automatically appear in cashier queue  
✅ **No Manual Entry** - Cashier sees what customer ordered  
✅ **Status Tracking** - Know which orders are paid vs pending  
✅ **Audit Trail** - Complete history in text file  
✅ **Simple Format** - Human-readable pipe-delimited text  
✅ **Persistent Storage** - Orders saved even if apps restart  

## Complete Workflow Example

**Step 1: Customer Side**
```
Customer → Browse Menu → Add Items → Checkout
System: "Enter your name" → Customer enters "Alice"
System saves to pending_orders.txt with status=PENDING
Message: "Order sent to cashier. Please proceed to payment."
```

**Step 2: Cashier Side**
```
Cashier opens app → System auto-loads pending orders from file
Cashier sees: Order #xyz123 | Alice | 3 items | $12.50 | PENDING
Cashier clicks "Process" → Order details load automatically
Cashier enters payment → Processes transaction
System updates file: status=COMPLETED
Message: "Order marked as COMPLETED in system"
```

**Step 3: File Contents**

Before payment:
```
xyz123|Alice|2025-11-20 20:30:00|12.50|PENDING|[Latte~4.5~2~Hot~50,Mocha~5.5~1~Cold~75]
```

After payment:
```
xyz123|Alice|2025-11-20 20:30:00|12.50|COMPLETED|[Latte~4.5~2~Hot~50,Mocha~5.5~1~Cold~75]
```

## Technical Details

### PendingOrder Model
- Stores order data separately from main Order class
- Includes customer name with order
- Tracks status (PENDING/COMPLETED)
- Converts to/from text record format

### TextDatabase Methods
```java
savePendingOrder(PendingOrder)       // Save/update order
loadPendingOrders()                  // Load only PENDING orders
loadAllPendingOrders()               // Load all orders (any status)
markOrderCompleted(orderId)          // Update status to COMPLETED
deletePendingOrder(orderId)          // Remove order from file
getPendingOrderById(orderId)         // Find specific order
```

### Integration Points

**CustomerApp:**
- `proceedToCheckout()` - Saves order to pending_orders.txt

**CashierApp:**
- `start()` - Auto-loads pending orders on startup
- `loadPendingOrdersFromFile()` - Converts PendingOrder to Order objects
- `completePayment()` - Marks order as COMPLETED

## Viewing Order History

**To see all orders (pending and completed):**
```java
List<PendingOrder> allOrders = TextDatabase.loadAllPendingOrders();
```

**To see only pending orders:**
```java
List<PendingOrder> pendingOnly = TextDatabase.loadPendingOrders();
```

**To check an order's status:**
```java
PendingOrder order = TextDatabase.getPendingOrderById("a1b2c3d4");
if (order.isPending()) {
    // Order waiting for payment
} else {
    // Order completed
}
```

## Summary

🔄 **Customer places order** → Saved as PENDING  
📋 **Cashier sees order** → Auto-loaded from file  
💰 **Payment processed** → Status updated to COMPLETED  
✅ **Complete tracking** → Full order lifecycle in one file  

This creates a complete end-to-end order management system with persistent storage!
