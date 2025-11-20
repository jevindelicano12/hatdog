# Cashier App Features

## Overview
The Cashier application has been redesigned with an order queue system and comprehensive receipt management. All orders are now processed through a queue-based workflow, ensuring organized transaction handling.

## Key Features

### 1. Order Queue System
- **Add Orders to Queue**: Enter customer name and add orders to the processing queue
- **Order Table Display**: View all pending orders with:
  - Order ID
  - Order Time
  - Number of items
  - Total amount
  - Payment status (Pending/Paid)
- **Queue Actions**:
  - **Process**: Load order for payment processing
  - **Remove**: Delete order from queue

### 2. Payment Processing
- **Unified Processing Panel**: Process orders loaded from the queue
- **Order Details Display**: View all items in the current order
- **Multi-Step Payment**: 
  - Accept partial payments
  - Progress bar shows payment completion
  - Automatic change calculation
- **Customer Name Capture**: Prompts for customer name during payment completion
- **Real-time Validation**:
  - Stock verification before checkout
  - Inventory sufficiency checks
  - Low stock alerts after transaction

### 3. Receipt Management
- **Automatic Receipt Generation**: Every completed transaction generates a receipt
- **Receipt Database**: All receipts saved to `data/receipts_database.txt`
- **Receipt History Tab**:
  - View all processed transactions
  - Search by Order ID, Receipt ID, or Customer Name
  - Sortable columns: Receipt ID, Order ID, Customer, Date/Time, Total, Change
  - Detailed receipt viewer
  - Print receipt functionality

### 4. Data Persistence
All transaction data is automatically saved to text databases:

#### Items Database (`data/items_database.txt`)
Format: `itemName|price|typeOfDrink|size|sugarLevel`
```
Espresso|3.5|Hot|Regular|50
Cappuccino|4.0|Cold|Regular|25
```

#### Orders Database (`data/orders_database.txt`)
Format: `orderId|userName|time|itemName|size|typeOfDrink`
```
ORD-12a4|John Doe|2025-11-20 20:00:00|Espresso|Regular|Hot
ORD-12a4|John Doe|2025-11-20 20:00:00|Cappuccino|Regular|Cold
```

#### Receipts Database (`data/receipts_database.txt`)
Format: `receiptId|orderId|userName|time|totalAmount|cashPaid|change`
```
RCP-A1B2C3D4|ORD-12a4|John Doe|2025-11-20 20:00:15|7.50|10.00|2.50
```

## User Interface Layout

### Tab 1: Process Orders (Two-Panel Design)

**Left Panel - Order Queue (700px)**
- Customer name input field
- "Add to Queue" button
- Order queue table with action buttons
- Queue counter

**Right Panel - Payment Processing (600px)**
- Order items display area
- Total amount label
- Payment status indicator
- Payment progress bar
- Cash input field
- "Process Payment" button
- "Cancel Order" button
- Receipt display area

### Tab 2: Receipt History

**Split View Design**
- **Left**: Receipt history table (60% width)
- **Right**: Receipt details viewer (40% width)
- Search functionality with refresh
- Total receipts counter

## Workflow

1. **Customer arrives** → Cashier enters customer name → Adds to queue
2. **Cashier selects order** → Clicks "Process" button
3. **Order loads** → Items and total displayed in payment panel
4. **Accept payment** → Enter cash amount → Click "Process Payment"
5. **Complete transaction** → System prompts for customer name
6. **Receipt generated** → Saved to database and displayed
7. **Data persisted** → Order, items, and receipt saved to text files

## Benefits

✅ **Organized Workflow**: Queue system prevents order confusion  
✅ **Complete Audit Trail**: All transactions saved with receipts  
✅ **Easy Search**: Find any receipt by order ID or customer name  
✅ **Data Persistence**: Human-readable text file format  
✅ **Customer Service**: Customer names tracked for all orders  
✅ **Payment Flexibility**: Accept partial payments with progress tracking  
✅ **Stock Management**: Real-time inventory validation and alerts  

## Technical Implementation

- **JavaFX TableView**: For order queue and receipt history
- **Observable Lists**: Real-time UI updates
- **Text Database**: Simple pipe-delimited format for data storage
- **Receipt Model**: Complete receipt object with all transaction details
- **Date/Time Formatting**: Consistent timestamp format across all records
- **UUID Generation**: Unique IDs for orders and receipts

## Database Files Location

All database files are stored in the `data/` directory:
- `items_database.txt` - Product items sold
- `orders_database.txt` - Order records
- `receipts_database.txt` - Receipt records

These files can be backed up, analyzed, or imported into other systems as needed.
