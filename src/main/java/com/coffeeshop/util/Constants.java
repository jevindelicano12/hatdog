package com.coffeeshop.util;

/**
 * Centralized constants for the Coffee Shop POS system.
 * Avoids magic numbers scattered throughout the codebase.
 */
public final class Constants {
    
    private Constants() {
        // Prevent instantiation
    }
    
    // ==================== BUSINESS RULES ====================
    
    /** VAT rate (12% in Philippines) */
    public static final double VAT_RATE = 0.12;
    
    /** Maximum return window in hours */
    public static final int RETURN_WINDOW_HOURS = 2;
    
    /** Default sugar level percentage */
    public static final int DEFAULT_SUGAR_LEVEL = 50;
    
    /** Default temperature for drinks */
    public static final String DEFAULT_TEMPERATURE = "Hot";
    
    // ==================== PRICING ====================
    
    /** Size upgrade cost: Small to Medium */
    public static final double SIZE_MEDIUM_UPGRADE = 5.0;
    
    /** Size upgrade cost: Small to Large */
    public static final double SIZE_LARGE_UPGRADE = 10.0;
    
    /** Extra shot add-on price */
    public static final double ADDON_EXTRA_SHOT = 1.00;
    
    /** Whipped cream add-on price */
    public static final double ADDON_WHIPPED_CREAM = 0.50;
    
    /** Syrup add-on price (vanilla, caramel, chocolate) */
    public static final double ADDON_SYRUP = 0.75;
    
    /** Tapioca pearls add-on price */
    public static final double ADDON_TAPIOCA = 10.00;
    
    /** Jellies add-on price */
    public static final double ADDON_JELLIES = 10.00;
    
    /** Popping boba add-on price */
    public static final double ADDON_POPPING_BOBA = 12.00;
    
    // ==================== UI TIMERS ====================
    
    /** Inactivity timeout in seconds (customer kiosk) */
    public static final int INACTIVITY_TIMEOUT_SECONDS = 30;
    
    /** Warning threshold for countdown display (turns red) */
    public static final int COUNTDOWN_WARNING_THRESHOLD = 10;
    
    /** Auto-refresh interval in milliseconds */
    public static final int AUTO_REFRESH_INTERVAL_MS = 30000;
    
    // ==================== UI DIMENSIONS ====================
    
    /** Default window width */
    public static final int DEFAULT_WINDOW_WIDTH = 1280;
    
    /** Default window height */
    public static final int DEFAULT_WINDOW_HEIGHT = 900;
    
    /** Sidebar width for admin panel */
    public static final int SIDEBAR_WIDTH = 260;
    
    /** Product card width */
    public static final int PRODUCT_CARD_WIDTH = 220;
    
    /** Product card height */
    public static final int PRODUCT_CARD_HEIGHT = 280;
    
    /** Thumbnail size */
    public static final int THUMBNAIL_SIZE = 150;
    
    // ==================== COLORS ====================
    
    /** Primary brand color */
    public static final String COLOR_PRIMARY = "#1A1A1A";
    
    /** Secondary brand color */
    public static final String COLOR_SECONDARY = "#795548";
    
    /** Success color (green) */
    public static final String COLOR_SUCCESS = "#4CAF50";
    
    /** Warning color (orange) */
    public static final String COLOR_WARNING = "#FF9800";
    
    /** Danger color (red) */
    public static final String COLOR_DANGER = "#F44336";
    
    /** Info color (blue) */
    public static final String COLOR_INFO = "#2196F3";
    
    /** Background color */
    public static final String COLOR_BACKGROUND = "#FAFAFA";
    
    /** Card background */
    public static final String COLOR_CARD = "#FFFFFF";
    
    /** Text primary */
    public static final String COLOR_TEXT_PRIMARY = "#1A1A1A";
    
    /** Text secondary */
    public static final String COLOR_TEXT_SECONDARY = "#757575";
    
    /** Border color */
    public static final String COLOR_BORDER = "#E0E0E0";
    
    // ==================== FONTS ====================
    
    /** Primary font family */
    public static final String FONT_PRIMARY = "Segoe UI";
    
    /** Monospace font for receipts */
    public static final String FONT_MONOSPACE = "Consolas";
    
    // ==================== DATA PATHS ====================
    
    /** Data directory */
    public static final String DATA_DIR = "data";
    
    /** Images directory */
    public static final String IMAGES_DIR = "data/images";
    
    /** Backup directory */
    public static final String BACKUP_DIR = "data/backups";
    
    // ==================== VALIDATION ====================
    
    /** Minimum product price */
    public static final double MIN_PRODUCT_PRICE = 0.01;
    
    /** Maximum product price */
    public static final double MAX_PRODUCT_PRICE = 99999.99;
    
    /** Minimum quantity */
    public static final int MIN_QUANTITY = 1;
    
    /** Maximum quantity per order item */
    public static final int MAX_QUANTITY = 100;
    
    /** Maximum product name length */
    public static final int MAX_PRODUCT_NAME_LENGTH = 100;
    
    // ==================== RECEIPT ====================
    
    /** Receipt header width (characters) */
    public static final int RECEIPT_WIDTH = 40;
    
    /** Shop name for receipts */
    public static final String SHOP_NAME = "BREWISE COFFEE SHOP";
    
    /** Receipt thank you message */
    public static final String RECEIPT_THANK_YOU = "Thank you for your order!";
    
    // ==================== DEFAULT CATEGORIES ====================
    
    /** Default product categories */
    public static final String[] DEFAULT_CATEGORIES = {
        "Coffee", "Milk Tea", "Frappe", "Fruit Tea", "Pastries"
    };
    
    // ==================== ORDER STATUS ====================
    
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    
    // ==================== CURRENCY ====================
    
    /** Currency symbol (Philippine Peso) */
    public static final String CURRENCY_SYMBOL = "₱";
    
    /** Currency format pattern */
    public static final String CURRENCY_FORMAT = "₱%.2f";
    
    /**
     * Format amount as currency string
     */
    public static String formatCurrency(double amount) {
        return String.format(CURRENCY_FORMAT, amount);
    }
}
