# Cashier App - Quick Start Guide

## Getting Started

Run the cashier application:
```bash
mvn exec:java@cashier
```

Or use the batch file:
```bash
run-cashier.bat
```

## Interface Overview

The application opens with 2 main tabs:

### 💳 Process Orders Tab
This is your main working area with two panels side-by-side.

### 🧾 Receipt History Tab
View and search all past transactions.

---

## Processing an Order - Step by Step

### Step 1: Add Customer to Queue

**Left Panel - Order Queue:**

1. Type customer name in the text field (e.g., "John Doe")
2. Click **"Add to Queue"** button
3. Order appears in the queue table below

**Queue Table shows:**
- Order ID (e.g., "a1b2c3d4")
- Order Time
- Number of Items (starts at 0)
- Total Amount (starts at $0.00)
- Status (Pending)
- Action buttons (Process / Remove)

### Step 2: Load Order for Processing

1. Find the order in the queue table
2. Click the green **"Process"** button
3. Order loads in the right panel
4. Order is removed from queue

### Step 3: Take Payment

**Right Panel - Payment Processing:**

1. **View Order Details**: See items and total (initially empty)
2. **Enter Cash**: Type amount in the "Enter Cash Amount" field
3. **Process Payment**: Click **"Process Payment"** button
4. **Repeat if needed**: For partial payments, enter more cash

**Payment Progress Bar** shows:
- Red: Less than 70% paid
- Orange: 70-99% paid  
- Green: 100% paid (complete)

### Step 4: Complete Transaction

When payment is complete:

1. System shows **"Change Due"** dialog if applicable
2. **Customer Name** prompt appears - confirm or edit
3. **Receipt generated** and displayed
4. **Receipt saved** to database
5. **Order & items saved** to database
6. **Inventory updated** automatically

**Refill Alert:** If any product stock ≤ 5, you'll see a warning to notify admin.

---

## Receipt History Tab

### Viewing Receipts

1. Switch to **"🧾 Receipt History"** tab
2. See all processed transactions in the table
3. Click any row to view full receipt details on the right

**Table Columns:**
- Receipt ID
- Order ID
- Customer Name
- Date/Time
- Total Amount
- Change Given

### Searching Receipts

1. Type search term in search box (Order ID, Receipt ID, or Customer Name)
2. Click **"Search"** button
3. Click **"Refresh"** to show all receipts again

### Printing Receipt

1. Select a receipt from the table
2. View details in the right panel
3. Click **"Print Receipt"** button
4. Confirmation dialog appears

---

## Quick Tips

✅ **Multiple Customers**: Add several orders to the queue before processing  
✅ **Partial Payments**: Accept cash in multiple steps - progress bar updates  
✅ **Order Review**: Check items and total before accepting payment  
✅ **Cancel Anytime**: Use "Cancel Order" button to abort current transaction  
✅ **Queue Management**: Remove orders from queue if customer leaves  
✅ **Receipt Lookup**: Search history anytime to verify past transactions  

## Common Scenarios

### Scenario 1: Busy Time - Multiple Customers
```
1. Customer A arrives → Add "Customer A" to queue
2. Customer B arrives → Add "Customer B" to queue  
3. Customer C arrives → Add "Customer C" to queue
4. Process orders one by one using "Process" button
```

### Scenario 2: Partial Payment
```
1. Order total: $15.50
2. Customer gives $10.00 → Enter → Process Payment (progress: 64%)
3. Customer gives $5.00 → Enter → Process Payment (progress: 97%)
4. Customer gives $1.00 → Enter → Process Payment (complete!)
5. Change due: $0.50
```

### Scenario 3: Finding Old Receipt
```
1. Go to "Receipt History" tab
2. Type customer name: "John"
3. Click "Search"
4. View results, click row to see full receipt
5. Print if needed
```

## Database Files

All data saved automatically in `data/` folder:
- `receipts_database.txt` - All receipts
- `orders_database.txt` - All orders
- `items_database.txt` - All items sold

**Backup Tip:** Copy the entire `data/` folder regularly for backup!

---

## Troubleshooting

**Problem**: Can't process payment  
**Solution**: Make sure you've loaded an order from the queue first

**Problem**: Receipt not showing  
**Solution**: Complete the full payment (progress bar = 100%)

**Problem**: Low stock alert appears  
**Solution**: Notify admin to refill products using Admin interface

**Problem**: Queue table empty  
**Solution**: Add customers using "Add to Queue" button first

---

## Need Help?

- **Admin Tasks**: Use Admin interface (`mvn exec:java@admin`)
- **View Products**: Use Customer interface (`mvn exec:java@customer`)
- **Documentation**: See `CASHIER_FEATURES.md` for detailed technical info

**Remember**: Always complete payments before closing the application to ensure all data is saved!
