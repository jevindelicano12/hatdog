# ✅ Setup Complete!

## Maven Installation Complete

Maven has been successfully installed to: `C:\maven\apache-maven-3.9.6`

The PATH has been updated in all batch scripts to include Maven automatically.

## How to Run the Applications

### Option 1: Use Batch Scripts (EASIEST - Just Double-Click!)

- **Customer Interface**: Double-click `run-customer.bat`
- **Cashier Interface**: Double-click `run-cashier.bat`  
- **Admin Interface**: Double-click `run-admin.bat`
- **Interactive Menu**: Double-click `run.bat` and choose

### Option 2: Command Line

Open a **NEW** PowerShell window (to get the updated PATH) and run:

```powershell
cd "C:\Users\jevin\OneDrive\Desktop\GUI nila jevin"

# Run Customer
mvn exec:java@customer

# Run Cashier
mvn exec:java@cashier

# Run Admin
mvn exec:java@admin
```

## What Was Installed

✅ Apache Maven 3.9.6
✅ Location: C:\maven\apache-maven-3.9.6
✅ Added to User PATH permanently
✅ All batch scripts updated with Maven path

## First Time Use

The project has already been built successfully! On first run, the application will:
1. Create a `data/` folder
2. Generate default products (5 coffee drinks)
3. Generate default inventory items (7 ingredients)
4. All products start with stock = 20

## Test It Now!

1. **Double-click** `run-customer.bat` 
2. The Customer GUI should open showing the coffee menu
3. Try adding items to your order!

## Warnings You Might See

The warnings you see in the terminal are normal and don't affect functionality:
- JavaFX configuration warnings (cosmetic only)
- Java restricted method warnings (from Maven dependencies)

The app works perfectly despite these warnings! ✅

## Troubleshooting

### If mvn command doesn't work in PowerShell:
1. **Close ALL PowerShell windows**
2. Open a **NEW** PowerShell window
3. The PATH should now be updated
4. Or just use the batch scripts (they include the Maven path)

### If batch scripts don't work:
Make sure you're double-clicking them from Windows Explorer, not running from an old PowerShell session.

## What's Next?

Try running all three applications:
1. Run `run-customer.bat` - Browse menu and create order
2. Run `run-cashier.bat` - Process payment with progress bar
3. Run `run-admin.bat` - View refill status and manage inventory

Enjoy your Coffee Shop Management System! ☕
