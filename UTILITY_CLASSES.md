# Coffee Shop Utility Classes

This document describes the utility classes added to improve code quality and maintainability.

## Package: `com.coffeeshop.util`

### 1. Constants.java
Centralized configuration values to eliminate magic numbers.

```java
import com.coffeeshop.util.Constants;

// Example usage:
double tax = subtotal * Constants.VAT_RATE;  // 12% VAT
String formatted = Constants.formatCurrency(100.50);  // "₱100.50"
```

**Key Constants:**
- `VAT_RATE` - 12% tax rate
- `INACTIVITY_TIMEOUT_SECONDS` - 30 seconds for auto-logout
- `BACKUP_INTERVAL_HOURS` - 24 hours between backups
- `DEFAULT_CATEGORIES` - Array of category names

---

### 2. Logger.java
Structured logging with file output.

```java
import com.coffeeshop.util.Logger;

Logger logger = Logger.getInstance();
logger.setLogLevel(Logger.LogLevel.DEBUG);

logger.debug("Processing order...");
logger.info("Order #123 completed");
logger.warn("Low inventory: Coffee Beans");
logger.error("Failed to save data: " + e.getMessage());
```

**Features:**
- Four log levels: DEBUG, INFO, WARN, ERROR
- Console and file output (data/logs/)
- Timestamped entries
- Thread-safe

---

### 3. StyleUtils.java
Centralized button styling utilities.

```java
import com.coffeeshop.util.StyleUtils;

Button saveBtn = new Button("Save");
StyleUtils.applyPrimaryButton(saveBtn);   // Green button
StyleUtils.applyDangerButton(cancelBtn);  // Red button
StyleUtils.applySuccessButton(confirmBtn); // Green button
StyleUtils.applySecondaryButton(backBtn);  // Blue button

// Card styling for containers
String cardStyle = StyleUtils.getCardStyle();
myPane.setStyle(cardStyle);
```

---

### 4. ValidationUtils.java
Input validation helpers.

```java
import com.coffeeshop.util.ValidationUtils;

// Apply numeric filter to text field (integers only)
ValidationUtils.applyNumericFilter(quantityField);

// Apply price filter (decimals allowed)
ValidationUtils.applyPriceFilter(priceField);

// Validate inputs
if (!ValidationUtils.validateNotEmpty(nameField.getText(), "Name")) {
    return; // Shows alert automatically
}

if (!ValidationUtils.validatePositiveNumber(priceField.getText(), "Price")) {
    return;
}

// Email validation
if (!ValidationUtils.validateEmail(emailField.getText())) {
    // Invalid email
}
```

---

### 5. BackupManager.java
Automatic backup of JSON data files.

```java
import com.coffeeshop.util.BackupManager;

// Perform a backup manually
BackupManager.performBackup();

// Get backup directory
Path backupDir = BackupManager.getBackupDirectory();

// Cleanup old backups (keeps last 7 days)
BackupManager.cleanupOldBackups();
```

**Features:**
- Backs up all JSON files in data/ folder
- Saves to data/backups/ with timestamps
- Auto-cleanup of backups older than 7 days
- Called automatically by Store.saveData() every 24 hours

---

### 6. AlertUtils.java
Standardized alert dialogs.

```java
import com.coffeeshop.util.AlertUtils;

// Information message
AlertUtils.showInfo("Success", "Order saved successfully!");

// Warning message
AlertUtils.showWarning("Warning", "Low stock for this item.");

// Error message
AlertUtils.showError("Error", "Failed to connect to database.");

// Confirmation dialog
boolean confirmed = AlertUtils.showConfirmation(
    "Confirm Delete", 
    "Are you sure you want to delete this item?"
);
if (confirmed) {
    // User clicked OK
}

// Input dialog
Optional<String> result = AlertUtils.showInputDialog(
    "New Category",
    "Enter category name:",
    "Beverages"  // default value
);
result.ifPresent(name -> {
    // Use the input
});
```

---

## Integration Status

### Already Integrated:
- ✅ Store.java - Uses Logger and BackupManager for auto-backup on save

### Ready to Integrate:
- CustomerApp.java - Replace inline styles with StyleUtils
- AdminApp.java - Replace debug prints with Logger, use ValidationUtils
- CashierApp.java - Replace alert dialogs with AlertUtils

## Directory Structure

```
src/main/java/com/coffeeshop/
├── util/
│   ├── AlertUtils.java
│   ├── BackupManager.java
│   ├── Constants.java
│   ├── Logger.java
│   ├── StyleUtils.java
│   └── ValidationUtils.java
├── model/
│   └── ... (data classes)
├── service/
│   └── Store.java (enhanced with logging)
├── AdminApp.java
├── CashierApp.java
├── CustomerApp.java
└── UIUtils.java (enhanced with hover effects)
```

## Data Directories

```
data/
├── logs/           # Log files (created by Logger)
│   └── app_YYYY-MM-DD.log
├── backups/        # Backup files (created by BackupManager)
│   └── YYYY-MM-DD_HH-mm-ss/
│       ├── accounts.json
│       ├── products.json
│       └── ...
├── accounts.json
├── products.json
└── ...
```
