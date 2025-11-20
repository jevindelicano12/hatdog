# Text-Based Database System

## Overview
The Coffee Shop Management System now includes a simple text-based database for storing items and orders persistently.

## Database Files

### 1. Items Database (`data/items_database.txt`)
Stores information about coffee shop items with the following fields:
- **itemName**: Name of the item (e.g., "Espresso", "Latte")
- **price**: Price of the item (e.g., 3.50)
- **typeOfDrink**: Hot or Cold
- **size**: Small, Medium, or Large
- **sugarLevel**: Sugar percentage (0-100)

**Format:** `itemName|price|typeOfDrink|size|sugarLevel`

**Example:**
```
Espresso|3.5|Hot|Small|50
Iced Latte|4.75|Cold|Medium|75
Cappuccino|4.5|Hot|Large|25
```

### 2. Orders Database (`data/orders_database.txt`)
Stores completed order information with the following fields:
- **orderId**: Unique order identifier
- **userName**: Customer or cashier name
- **time**: Order timestamp (YYYY-MM-DD HH:MM:SS)
- **itemName**: Name of the ordered item
- **size**: Size of the drink
- **typeOfDrink**: Hot or Cold

**Format:** `orderId|userName|time|itemName|size|typeOfDrink`

**Example:**
```
ORD001|John Doe|2025-11-20 19:45:30|Espresso|Small|Hot
ORD002|Jane Smith|2025-11-20 19:15:00|Iced Latte|Medium|Cold
ORD003|Bob Wilson|2025-11-20 18:45:30|Cappuccino|Large|Hot
```

## Usage

### Adding Items
```java
import com.coffeeshop.model.ItemRecord;
import com.coffeeshop.service.TextDatabase;

ItemRecord item = new ItemRecord("Espresso", 3.50, "Hot", "Small", 50);
TextDatabase.saveItem(item);
```

### Loading Items
```java
List<ItemRecord> allItems = TextDatabase.loadAllItems();
for (ItemRecord item : allItems) {
    System.out.println(item.getItemName() + ": $" + item.getPrice());
}
```

### Adding Orders
```java
import com.coffeeshop.model.OrderRecord;
import java.time.LocalDateTime;

OrderRecord order = new OrderRecord(
    "ORD001", 
    "John Doe", 
    LocalDateTime.now(),
    "Espresso",
    "Small",
    "Hot"
);
TextDatabase.saveOrder(order);
```

### Loading Orders
```java
List<OrderRecord> allOrders = TextDatabase.loadAllOrders();
for (OrderRecord order : allOrders) {
    System.out.println(order.getOrderId() + " - " + order.getUserName());
}
```

### Search Operations

**Search Items by Name:**
```java
List<ItemRecord> results = TextDatabase.searchItemsByName("Latte");
```

**Search Orders by User Name:**
```java
List<OrderRecord> results = TextDatabase.searchOrdersByUserName("John");
```

**Search Orders by Order ID:**
```java
OrderRecord order = TextDatabase.searchOrderById("ORD001");
```

**Get Orders by Date Range:**
```java
LocalDateTime startDate = LocalDateTime.now().minusDays(7);
LocalDateTime endDate = LocalDateTime.now();
List<OrderRecord> orders = TextDatabase.getOrdersByDateRange(startDate, endDate);
```

## Integration with Cashier App

The `CashierApp` automatically saves orders to the text database when a payment is completed. Each order item is saved with:
- Item information (name, price, type, size, sugar level)
- Order information (order ID, username, timestamp)

## Running the Demo

To test the text database functionality:

```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.coffeeshop.service.DatabaseDemo"
```

Or run directly from your IDE:
1. Open `DatabaseDemo.java`
2. Run the main method
3. Check the `data/` folder for generated database files

## Database Statistics

Get counts:
```java
int itemsCount = TextDatabase.getItemsCount();
int ordersCount = TextDatabase.getOrdersCount();
```

## Clearing Database

Clear all items:
```java
TextDatabase.clearItemsDatabase();
```

Clear all orders:
```java
TextDatabase.clearOrdersDatabase();
```

## File Location

Database files are stored in:
- `data/items_database.txt`
- `data/orders_database.txt`

You can view and edit these files with any text editor.

## Notes

- The database uses `|` (pipe) as a field delimiter
- Date/time format: `yyyy-MM-dd HH:mm:ss`
- Files are created automatically in the `data/` directory
- All operations handle file I/O errors gracefully
- Data persists between application runs
