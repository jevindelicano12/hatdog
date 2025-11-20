# Quick Start Guide

## Running the Applications

### Option 1: Using the Launcher Script
Double-click `run.bat` and select which application to run.

### Option 2: Direct Scripts
- **Customer**: Double-click `run-customer.bat`
- **Cashier**: Double-click `run-cashier.bat`
- **Admin**: Double-click `run-admin.bat`

### Option 3: Command Line
```bash
# Customer interface
mvn exec:java@customer

# Cashier interface
mvn exec:java@cashier

# Admin interface
mvn exec:java@admin
```

## First Time Setup

1. **Build the project first**:
   ```bash
   mvn clean compile
   ```

2. **Run any application** - it will auto-create default data on first launch

## Quick Reference

### Customer App
- Browse products in the main grid
- Select product, customize (temperature, sugar, add-ons)
- Add to order basket (right panel)
- Click "Proceed to Checkout" when ready
- System validates stock and ingredients

### Cashier App
- Select products from dropdown to create order
- Set temperature, sugar level, quantity
- Click "Add Item to Order"
- Enter cash amount in payment panel
- Click "Process Payment" (can do multiple times if insufficient)
- Progress bar shows payment status:
  - 🔴 Red: Far from paid
  - 🟡 Orange: Getting close
  - 🟢 Green: Fully paid
- Receipt auto-generates after successful payment
- Low stock alerts appear if product stock ≤ 5

### Admin App
- **Product Management Tab**:
  - View all products with stock levels
  - Refill products (select → click Refill)
  - Remove products (with cascade option for ingredients)
  - Add new products
  
- **Inventory Management Tab**:
  - View all ingredients
  - Refill ingredients
  - Add new ingredients
  
- **Refill Status Tab**:
  - View color-coded alerts:
    - 🔴 CRITICAL: Stock = 0
    - 🟡 WARNING: Stock ≤ 5
    - ✓ OK: Stock > 5
  - Shows "Refill Needed" amount (20 - current stock)

## Common Tasks

### Task: Customer Orders a Drink
1. Run customer app: `mvn exec:java@customer`
2. Select product from grid
3. Set temperature and sugar level
4. Click "Add to Order"
5. Review order in right panel
6. Click "Proceed to Checkout"
7. Note the Order ID

### Task: Cashier Processes Payment
1. Run cashier app: `mvn exec:java@cashier`
2. Create order by selecting products
3. Enter cash amount (can be partial)
4. Click "Process Payment"
5. If insufficient, enter more cash and click again
6. When complete, receipt prints automatically
7. Watch for refill alerts

### Task: Admin Refills Low Stock
1. Run admin app: `mvn exec:java@admin`
2. Go to "Refill Status" tab
3. Note products with 🟡 or 🔴 status
4. Go to "Product Management" tab
5. Select product needing refill
6. Click "Refill Product"
7. Enter amount (e.g., 15 to bring back to 20)
8. Confirm

### Task: Admin Removes Product
1. Run admin app
2. Go to "Product Management" tab
3. Select product to remove
4. Click "Remove Product"
5. Choose option:
   - "Remove Product Only" - keeps ingredients
   - "Remove with Ingredients" - cascade delete
6. Confirm

## System Constants

- **MAX_STOCK**: 20 servings per product
- **REFILL_THRESHOLD**: 5 (alert triggers when stock ≤ 5)

## Data Files

Data is stored in `data/` directory:
- `products.json` - Products with recipes and stock
- `inventory.json` - Ingredients with quantities

**To reset data**: Delete the `data/` folder and restart any app.

## Troubleshooting

### "Module not found" errors
These are IDE warnings. The app will run fine with Maven.

### App doesn't start
1. Make sure Java 17+ is installed: `java -version`
2. Make sure Maven is installed: `mvn -version`
3. Build first: `mvn clean compile`

### No data files
They're auto-created on first run. Make sure the app has write permissions.

### Products show stock = 0
Delete `data/` folder and restart to regenerate with stock = 20.

## Tips

- **Run multiple apps simultaneously** to simulate real coffee shop operations
- **Customer and Cashier** can run together for full workflow testing
- **Admin app** updates are immediately visible to other running apps after restart
- Data persists between runs - changes are saved to JSON files
- Each app can run independently - no network required

## Keyboard Shortcuts

(JavaFX standard shortcuts)
- `Tab` - Navigate between fields
- `Enter` - Activate focused button
- `Esc` - Close dialogs
- `Alt+F4` - Close application

Enjoy your Coffee Shop Management System! ☕
