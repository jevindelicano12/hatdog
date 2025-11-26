package com.coffeeshop.util;

import javafx.scene.control.TextFormatter;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Input validation utilities for the Coffee Shop POS system.
 * Provides reusable validators and text formatters.
 */
public final class ValidationUtils {
    
    private ValidationUtils() {
        // Prevent instantiation
    }
    
    // ==================== REGEX PATTERNS ====================
    
    /** Pattern for positive decimal numbers */
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^\\d*(\\.\\d*)?$");
    
    /** Pattern for positive integers */
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^\\d*$");
    
    /** Pattern for currency amounts (up to 2 decimal places) */
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^\\d*(\\.\\d{0,2})?$");
    
    /** Pattern for valid product/category names */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-'&.,()]+$");
    
    // ==================== TEXT FORMATTERS ====================
    
    /**
     * Create a TextFormatter that only allows positive decimal numbers
     */
    public static TextFormatter<String> createDecimalFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (DECIMAL_PATTERN.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        return new TextFormatter<>(filter);
    }
    
    /**
     * Create a TextFormatter that only allows positive integers
     */
    public static TextFormatter<String> createIntegerFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (INTEGER_PATTERN.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        return new TextFormatter<>(filter);
    }
    
    /**
     * Create a TextFormatter for currency amounts (max 2 decimal places)
     */
    public static TextFormatter<String> createCurrencyFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (CURRENCY_PATTERN.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        return new TextFormatter<>(filter);
    }
    
    /**
     * Create a TextFormatter that limits text length
     */
    public static TextFormatter<String> createLengthLimitFormatter(int maxLength) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.length() <= maxLength) {
                return change;
            }
            return null;
        };
        return new TextFormatter<>(filter);
    }
    
    // ==================== VALIDATION METHODS ====================
    
    /**
     * Check if a string is null or empty (after trimming)
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Check if a string is not null and not empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Validate a product name
     */
    public static boolean isValidProductName(String name) {
        if (isEmpty(name)) return false;
        if (name.length() > Constants.MAX_PRODUCT_NAME_LENGTH) return false;
        return NAME_PATTERN.matcher(name).matches();
    }
    
    /**
     * Validate a price value
     */
    public static boolean isValidPrice(double price) {
        return price >= Constants.MIN_PRODUCT_PRICE && price <= Constants.MAX_PRODUCT_PRICE;
    }
    
    /**
     * Validate a price string and return parsed value, or -1 if invalid
     */
    public static double parsePrice(String priceStr) {
        if (isEmpty(priceStr)) return -1;
        try {
            double price = Double.parseDouble(priceStr.trim());
            return isValidPrice(price) ? price : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Validate a quantity value
     */
    public static boolean isValidQuantity(int quantity) {
        return quantity >= Constants.MIN_QUANTITY && quantity <= Constants.MAX_QUANTITY;
    }
    
    /**
     * Validate a quantity string and return parsed value, or -1 if invalid
     */
    public static int parseQuantity(String quantityStr) {
        if (isEmpty(quantityStr)) return -1;
        try {
            int quantity = Integer.parseInt(quantityStr.trim());
            return isValidQuantity(quantity) ? quantity : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Parse a decimal string, returning 0 if invalid
     */
    public static double parseDecimal(String value, double defaultValue) {
        if (isEmpty(value)) return defaultValue;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Parse an integer string, returning default if invalid
     */
    public static int parseInteger(String value, int defaultValue) {
        if (isEmpty(value)) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    // ==================== STRING UTILITIES ====================
    
    /**
     * Safely trim a string, returning empty string if null
     */
    public static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
    
    /**
     * Truncate a string to max length, adding ellipsis if truncated
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Capitalize the first letter of each word
     */
    public static String toTitleCase(String value) {
        if (isEmpty(value)) return "";
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : value.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
    
    /**
     * Sanitize input by removing potentially dangerous characters
     */
    public static String sanitize(String input) {
        if (input == null) return "";
        // Remove control characters and limit to printable ASCII + common Unicode
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
    }
}
