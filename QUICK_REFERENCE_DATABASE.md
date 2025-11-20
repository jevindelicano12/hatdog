# Quick Reference: Text Database System

## 📋 Database Structure

### Items Database (`data/items_database.txt`)
```
itemName|price|typeOfDrink|size|sugarLevel
```
- **itemName**: Product name
- **price**: Price in dollars
- **typeOfDrink**: "Hot" or "Cold"
- **size**: "Small", "Medium", or "Large"
- **sugarLevel**: 0-100 (percentage)

### Orders Database (`data/orders_database.txt`)
```
orderId|userName|time|itemName|size|typeOfDrink
```
- **orderId**: Unique order identifier
- **userName**: Customer/cashier name
- **time**: Format: `yyyy-MM-dd HH:mm:ss`
- **itemName**: Product ordered
- **size**: Drink size
- **typeOfDrink**: "Hot" or "Cold"

## 🚀 Quick Start

### 1. Run the Demo
```bash
# Make sure you're in the project root
cd "c:\Users\jevin\OneDrive\Desktop\GUI nila jevin"

# Compile
mvn clean compile

# The demo will automatically run when CashierApp processes orders
```

### 2. View Database Files
```powershell
# View items
Get-Content data/items_database.txt

# View orders
Get-Content data/orders_database.txt
```

## 💻 Code Examples

### Save an Item
```java
ItemRecord item = new ItemRecord(
    "Espresso",    // itemName
    3.50,          // price
    "Hot",         // typeOfDrink
    "Small",       // size
    50             // sugarLevel
);
TextDatabase.saveItem(item);
```

### Save an Order
```java
OrderRecord order = new OrderRecord(
    "ORD001",              // orderId
    "John Doe",            // userName
    LocalDateTime.now(),   // orderTime
    "Espresso",            // itemName
    "Small",               // size
    "Hot"                  // typeOfDrink
);
TextDatabase.saveOrder(order);
```

### Load All Items
```java
List<ItemRecord> items = TextDatabase.loadAllItems();
for (ItemRecord item : items) {
    System.out.println(item.getItemName() + ": $" + item.getPrice());
}
```

### Search Items
```java
// Search by name
List<ItemRecord> results = TextDatabase.searchItemsByName("Latte");

// Get item count
int count = TextDatabase.getItemsCount();
```

### Search Orders
```java
// Search by order ID
OrderRecord order = TextDatabase.searchOrderById("ORD001");

// Search by username
List<OrderRecord> userOrders = TextDatabase.searchOrdersByUserName("John");

// Search by date range
LocalDateTime start = LocalDateTime.now().minusDays(7);
LocalDateTime end = LocalDateTime.now();
List<OrderRecord> dateOrders = TextDatabase.getOrdersByDateRange(start, end);
```

## 🔄 Automatic Integration

When you use the **CashierApp** to complete an order:
1. ✅ Items are automatically saved to `data/items_database.txt`
2. ✅ Orders are automatically saved to `data/orders_database.txt`
3. ✅ Data persists between application runs

## 📊 Example Output

After running CashierApp and completing orders, your files will look like:

**items_database.txt:**
```
Espresso|3.5|Hot|Small|50
Cappuccino|4.5|Hot|Regular|25
Iced Latte|4.75|Cold|Regular|75
```

**orders_database.txt:**
```
12ab34cd|Cashier|2025-11-20 19:47:44|Espresso|Regular|Hot
12ab34cd|Cashier|2025-11-20 19:47:44|Cappuccino|Regular|Hot
56ef78gh|Cashier|2025-11-20 19:50:15|Iced Latte|Regular|Cold
```

## 🛠️ Utility Functions

```java
// Clear databases
TextDatabase.clearItemsDatabase();
TextDatabase.clearOrdersDatabase();

// Get counts
int itemsCount = TextDatabase.getItemsCount();
int ordersCount = TextDatabase.getOrdersCount();

// Batch save
List<ItemRecord> items = new ArrayList<>();
items.add(new ItemRecord("Coffee", 2.50, "Hot", "Small", 50));
items.add(new ItemRecord("Tea", 2.00, "Hot", "Small", 0));
TextDatabase.saveAllItems(items);
```

## 📁 File Locations

- Items: `data/items_database.txt`
- Orders: `data/orders_database.txt`

Both files are **human-readable** and can be opened with any text editor!

## ✅ Status

- ✅ Java 21 LTS compatible
- ✅ Fully tested and working
- ✅ Integrated with CashierApp
- ✅ Complete documentation available
- ✅ Demo program included
