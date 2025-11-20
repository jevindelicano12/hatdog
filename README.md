# Coffee Shop Management System

A JavaFX-based coffee shop management system with separate interfaces for Customer, Cashier, and Admin roles.

## Features

### Customer Interface
- Browse product menu with real-time stock information
- Customize drinks (temperature, sugar level, add-ons checkboxes)
- Build orders with multiple items
- View order summary and total
- Stock validation before checkout

### Cashier Interface
- View customer order queue (auto-loaded from file)
- Complete orders with one click
- Generate and save receipts automatically
- Automatic inventory deduction
- Refill alerts after checkout
- Receipt history with search functionality

### Admin Interface
- Product management (add, refill, remove)
- Inventory management (add ingredients, refill)
- Cascade delete option for products with ingredients
- Real-time refill status monitoring
- Color-coded alerts (Critical, Warning, OK)

## Constants

- `MAX_STOCK = 20` servings per product
- `REFILL_THRESHOLD = 5` (alert when stock ≤ 5)

## Project Structure

```
coffee-shop-system/
├── src/main/java/com/coffeeshop/
│   ├── CustomerApp.java          # Customer GUI entry point
│   ├── CashierApp.java           # Cashier GUI entry point
│   ├── AdminApp.java             # Admin GUI entry point
│   ├── model/
│   │   ├── Product.java
│   │   ├── InventoryItem.java
│   │   ├── Order.java
│   │   └── OrderItem.java
│   └── service/
│       ├── Store.java            # Business logic
│       └── PersistenceManager.java  # JSON persistence
├── data/
│   ├── products.json             # Auto-generated
│   └── inventory.json            # Auto-generated
├── pom.xml
└── README.md
```

## Requirements

- Java 21 LTS or higher
- Maven 3.6 or higher
- JavaFX 21.0.5 (included in dependencies)

## Installation

1. Clone or download the project
2. Navigate to the project directory:
   ```bash
   cd "c:\Users\jevin\OneDrive\Desktop\GUI nila jevin"
   ```

3. Build the project:
   ```bash
   mvn clean compile
   ```

## Running the Applications

### IMPORTANT: Each role runs as a separate application

### Run Customer Interface
```bash
mvn exec:java@customer
```

### Run Cashier Interface
```bash
mvn exec:java@cashier
```

### Run Admin Interface
```bash
mvn exec:java@admin
```

## Initial Setup

On first run, the system automatically creates:
- Default products (Espresso, Cappuccino, Latte, Matcha Latte, Mocha)
- Default inventory items (Coffee Beans, Milk, Water, Matcha Powder, Chocolate Syrup, etc.)
- All products start with stock = 20

Data is persisted in the `data/` directory as JSON files.

## Workflow Examples

### Example 1: Cashier Processes Customer Order

1. **Cashier** runs cashier interface and:
   - **Order Queue Tab:**
     - View all pending customer orders in a table
     - Orders display: Customer Name, Order ID, Order Time, Items Count, Total Amount, Status
     - Orders from customers are automatically loaded from file
     - Click "Complete Order" button to process payment directly
     - System automatically:
       - Validates stock and inventory
       - Uses stored customer name (no prompt needed)
       - Generates and saves receipt
       - Saves order to text database
       - Deducts inventory automatically
       - Marks order as COMPLETED in system
       - Shows refill alert if stock ≤ 5
   
   - **Receipt History Tab:**
     - View all processed transactions
     - Search receipts by Order ID or customer name
     - Click on any receipt to view full details
     - Print receipt option available

### Example 2: Admin Manages Inventory

1. **Admin** runs admin interface and:
   - Views "Refill Status" tab
   - Sees products with low stock (color-coded alerts)
   - Selects product needing refill
   - Refills product (adds units up to MAX_STOCK = 20)
   - Alert clears when stock > REFILL_THRESHOLD

### Example 3: Admin Removes Product

1. **Admin** selects a product to remove
2. System prompts:
   - **Option A**: Remove product only (keep ingredients)
   - **Option B**: Remove product and ingredients (cascade)
3. Admin confirms choice
4. System updates and persists changes

## Data Persistence

### JSON Storage (Product & Inventory)
All changes are automatically saved to JSON files:
- `data/products.json` - Product list with stock levels
- `data/inventory.json` - Ingredient quantities

On startup, the system loads data from these files. If files don't exist, default data is created.

### Text Database (Orders & Sales)
The system also maintains a text-based database for tracking:
- `data/items_database.txt` - Item records with pricing and customization
- `data/orders_database.txt` - Complete order history with timestamps

**Items Database Format:**
```
itemName|price|typeOfDrink|size|sugarLevel
```

**Orders Database Format:**
```
orderId|userName|time|itemName|size|typeOfDrink
```

These files are **human-readable** and can be opened with any text editor. Orders are automatically saved when the Cashier completes a payment.

For detailed documentation, see:
- `TEXT_DATABASE.md` - Complete API documentation
- `QUICK_REFERENCE_DATABASE.md` - Quick start guide

## Error Handling

### Customer Side
- Products with `stock = 0` show "Not Available Right Now" (blocked from selection)
- Pre-checkout validation checks stock and ingredients
- Clear error messages for insufficient resources

### Cashier Side
- Multi-attempt payment loop with visual feedback
- Re-validation before final checkout
- Refill alerts displayed after successful checkout

### Admin Side
- Confirmation dialogs for destructive operations
- Validation for input fields
- Cascade delete confirmation for safety

## Testing Checklist

### Customer
- ✓ Order item with customization (temperature, sugar)
- ✓ Try ordering product at stock=0 (should be blocked)
- ✓ Verify receipt shows all customization details

### Cashier
- ✓ Enter insufficient payment (verify progress bar turns red/orange)
- ✓ Add more cash until payment complete (verify turns green)
- ✓ Complete payment and verify stock deduction
- ✓ Verify refill alerts appear when stock ≤ 5

### Admin
- ✓ Check refill status (verify refillNeeded = 20 - stock)
- ✓ Refill product and verify alert clears
- ✓ Remove product with cascade confirmation
- ✓ Add new product/ingredient

## Troubleshooting

### Issue: "JavaFX runtime components are missing"
**Solution**: Make sure you're using Maven to run the applications (not `java -jar`). Maven handles JavaFX dependencies automatically.

### Issue: Data files not found
**Solution**: The `data/` directory is created automatically on first run. Make sure the application has write permissions in the project directory.

### Issue: Refill alerts don't appear
**Solution**: Check that `Store.hasProductsNeedingRefill()` is called after `checkoutBasket()`. This is handled automatically in the Cashier app.

### Issue: Products load with stock=0
**Solution**: Delete the `data/` directory and restart. The system will recreate default data with stock=20.

## Architecture Notes

### Separation of Concerns
- **Model**: Data classes (Product, Order, etc.)
- **Service**: Business logic (Store) and persistence
- **View**: JavaFX GUI applications (Customer, Cashier, Admin)

### Thread Safety
- Current implementation uses synchronous file I/O
- For production, consider:
  - Adding file locks for concurrent access
  - Using a database instead of JSON files
  - Implementing background save operations

### Extensibility
- Add new products via Admin interface
- Add new ingredients dynamically
- Recipe system allows flexible product definitions

## Database Features

### Text Database API
```java
// Save items and orders
TextDatabase.saveItem(new ItemRecord("Latte", 4.75, "Hot", "Medium", 50));
TextDatabase.saveOrder(new OrderRecord("ORD001", "John", LocalDateTime.now(), 
                                       "Latte", "Medium", "Hot"));

// Load and search
List<ItemRecord> items = TextDatabase.loadAllItems();
List<OrderRecord> orders = TextDatabase.searchOrdersByUserName("John");
OrderRecord order = TextDatabase.searchOrderById("ORD001");

// Date range queries
List<OrderRecord> recentOrders = TextDatabase.getOrdersByDateRange(startDate, endDate);
```

See `DatabaseDemo.java` for a working example.

## Future Enhancements

- [ ] User authentication/authorization
- [x] Sales tracking with text database
- [x] Order history with search functionality
- [ ] Sales reports and analytics dashboard
- [ ] Real-time sync between multiple terminals
- [ ] Database backend (MySQL/PostgreSQL)
- [ ] Barcode scanner integration
- [ ] Cloud-based inventory management
- [ ] Export database to CSV/Excel

## License

This project is for educational purposes.

## Contact

For issues or questions, please contact the development team.
