package com.coffeeshop.service;

import com.coffeeshop.model.ItemRecord;
import com.coffeeshop.model.OrderRecord;
import com.coffeeshop.model.Receipt;
import com.coffeeshop.model.PendingOrder;
import com.coffeeshop.model.ReturnTransaction;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TextDatabase {
    private static final String DATA_DIR = "data";
    private static final String ITEMS_DB = DATA_DIR + "/items_database.txt";
    private static final String ORDERS_DB = DATA_DIR + "/orders_database.txt";
    private static final String RECEIPTS_DB = DATA_DIR + "/receipts_database.txt";
    private static final String PENDING_ORDERS_DB = DATA_DIR + "/pending_orders.txt";
    private static final String RETURNS_DB = DATA_DIR + "/returns_database.txt";

    // Ensure data directory exists
    public static void ensureDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // ==================== ITEMS DATABASE ====================

    /**
     * Save an item to the items database
     */
    public static void saveItem(ItemRecord item) {
        ensureDataDirectory();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ITEMS_DB, true))) {
            writer.write(item.toTextRecord());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error saving item: " + e.getMessage());
        }
    }

    /**
     * Save multiple items to the items database (overwrites existing)
     */
    public static void saveAllItems(List<ItemRecord> items) {
        ensureDataDirectory();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ITEMS_DB, false))) {
            for (ItemRecord item : items) {
                writer.write(item.toTextRecord());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving items: " + e.getMessage());
        }
    }

    /**
     * Load all items from the items database
     */
    public static List<ItemRecord> loadAllItems() {
        List<ItemRecord> items = new ArrayList<>();
        ensureDataDirectory();
        
        File file = new File(ITEMS_DB);
        if (!file.exists()) {
            return items;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(ITEMS_DB))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        items.add(new ItemRecord(line));
                    } catch (Exception e) {
                        System.err.println("Error parsing item line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading items: " + e.getMessage());
        }
        return items;
    }

    /**
     * Search items by name
     */
    public static List<ItemRecord> searchItemsByName(String searchTerm) {
        List<ItemRecord> results = new ArrayList<>();
        List<ItemRecord> allItems = loadAllItems();
        
        for (ItemRecord item : allItems) {
            if (item.getItemName().toLowerCase().contains(searchTerm.toLowerCase())) {
                results.add(item);
            }
        }
        return results;
    }

    /**
     * Clear all items from database
     */
    public static void clearItemsDatabase() {
        ensureDataDirectory();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ITEMS_DB, false))) {
            // Just open and close to clear the file
        } catch (IOException e) {
            System.err.println("Error clearing items database: " + e.getMessage());
        }
    }

    // ==================== ORDERS DATABASE ====================

    /**
     * Save an order to the orders database
     */
    public static void saveOrder(OrderRecord order) {
        ensureDataDirectory();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ORDERS_DB, true))) {
            writer.write(order.toTextRecord());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error saving order: " + e.getMessage());
        }
    }

    /**
     * Save multiple orders to the orders database (overwrites existing)
     */
    public static void saveAllOrders(List<OrderRecord> orders) {
        ensureDataDirectory();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ORDERS_DB, false))) {
            for (OrderRecord order : orders) {
                writer.write(order.toTextRecord());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving orders: " + e.getMessage());
        }
    }

    /**
     * Load all orders from the orders database
     */
    public static List<OrderRecord> loadAllOrders() {
        List<OrderRecord> orders = new ArrayList<>();
        ensureDataDirectory();
        
        File file = new File(ORDERS_DB);
        if (!file.exists()) {
            return orders;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(ORDERS_DB))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        orders.add(new OrderRecord(line));
                    } catch (Exception e) {
                        System.err.println("Error parsing order line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading orders: " + e.getMessage());
        }
        return orders;
    }

    /**
     * Search orders by order ID
     */
    public static OrderRecord searchOrderById(String orderId) {
        List<OrderRecord> allOrders = loadAllOrders();
        
        for (OrderRecord order : allOrders) {
            if (order.getOrderId().equals(orderId)) {
                return order;
            }
        }
        return null;
    }

    /**
     * Search orders by user name
     */
    public static List<OrderRecord> searchOrdersByUserName(String userName) {
        List<OrderRecord> results = new ArrayList<>();
        List<OrderRecord> allOrders = loadAllOrders();
        
        for (OrderRecord order : allOrders) {
            if (order.getUserName().toLowerCase().contains(userName.toLowerCase())) {
                results.add(order);
            }
        }
        return results;
    }

    /**
     * Get orders within a date range
     */
    public static List<OrderRecord> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<OrderRecord> results = new ArrayList<>();
        List<OrderRecord> allOrders = loadAllOrders();
        
        for (OrderRecord order : allOrders) {
            LocalDateTime orderTime = order.getOrderTime();
            if ((orderTime.isEqual(startDate) || orderTime.isAfter(startDate)) &&
                (orderTime.isEqual(endDate) || orderTime.isBefore(endDate))) {
                results.add(order);
            }
        }
        return results;
    }

    /**
     * Clear all orders from database
     */
    public static void clearOrdersDatabase() {
        ensureDataDirectory();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ORDERS_DB, false))) {
            // Just open and close to clear the file
        } catch (IOException e) {
            System.err.println("Error clearing orders database: " + e.getMessage());
        }
    }

    /**
     * Get total number of items in database
     */
    public static int getItemsCount() {
        return loadAllItems().size();
    }

    /**
     * Get total number of orders in database
     */
    public static int getOrdersCount() {
        return loadAllOrders().size();
    }

    // ==================== RECEIPTS DATABASE ====================

    /**
     * Save a receipt to the receipts database
     */
    public static void saveReceipt(Receipt receipt) {
        ensureDataDirectory();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RECEIPTS_DB, true))) {
            writer.write(receipt.toTextRecord());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error saving receipt: " + e.getMessage());
        }
    }

    /**
     * Load all receipts from the receipts database
     */
    public static List<Receipt> loadAllReceipts() {
        List<Receipt> receipts = new ArrayList<>();
        ensureDataDirectory();
        
        File file = new File(RECEIPTS_DB);
        if (!file.exists()) {
            return receipts;
        }

        final int MAX_LINE_LENGTH = 2000;
        int lineNumber = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(RECEIPTS_DB))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                if (line.length() > MAX_LINE_LENGTH) {
                    System.err.println("Skipping suspiciously long receipt line "
                        + "(length=" + line.length() + ") at " + RECEIPTS_DB + ":" + lineNumber);
                    continue;
                }

                try {
                    Receipt receipt = Receipt.fromTextRecord(line);
                    if (receipt != null) {
                        receipts.add(receipt);
                    }
                } catch (Exception e) {
                    String snippet = line.length() > 200 ? line.substring(0, 200) + "..." : line;
                    System.err.println("Error parsing receipt at " + RECEIPTS_DB + ":" + lineNumber + ". Skipping. snippet='" + snippet + "'");
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading receipts: " + e.getMessage());
        }
        return receipts;
    }

    /**
     * Search receipts by order ID
     */
    public static Receipt searchReceiptByOrderId(String orderId) {
        List<Receipt> allReceipts = loadAllReceipts();
        
        for (Receipt receipt : allReceipts) {
            if (receipt.getOrderId().equals(orderId)) {
                return receipt;
            }
        }
        return null;
    }

    /**
     * Search receipts by user name
     */
    public static List<Receipt> searchReceiptsByUserName(String userName) {
        List<Receipt> results = new ArrayList<>();
        List<Receipt> allReceipts = loadAllReceipts();
        
        for (Receipt receipt : allReceipts) {
            if (receipt.getUserName().toLowerCase().contains(userName.toLowerCase())) {
                results.add(receipt);
            }
        }
        return results;
    }

    /**
     * Get receipts within a date range
     */
    public static List<Receipt> getReceiptsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Receipt> results = new ArrayList<>();
        List<Receipt> allReceipts = loadAllReceipts();
        
        for (Receipt receipt : allReceipts) {
            LocalDateTime receiptTime = receipt.getReceiptTime();
            if ((receiptTime.isEqual(startDate) || receiptTime.isAfter(startDate)) &&
                (receiptTime.isEqual(endDate) || receiptTime.isBefore(endDate))) {
                results.add(receipt);
            }
        }
        return results;
    }

    /**
     * Get total number of receipts in database
     */
    public static int getReceiptsCount() {
        return loadAllReceipts().size();
    }

    // ==================== PENDING ORDERS DATABASE ====================

    /**
     * Save a pending order to the pending orders database
     */
    public static void savePendingOrder(PendingOrder order) {
        ensureDataDirectory();
        
        // Load all orders, update if exists, or add new
        List<PendingOrder> allOrders = loadAllPendingOrders();
        boolean found = false;
        
        for (int i = 0; i < allOrders.size(); i++) {
            if (allOrders.get(i).getOrderId().equals(order.getOrderId())) {
                allOrders.set(i, order);
                found = true;
                break;
            }
        }
        
        if (!found) {
            allOrders.add(order);
        }
        
        // Save all orders
        saveAllPendingOrders(allOrders);
    }

    /**
     * Save all pending orders (overwrites existing)
     */
    private static void saveAllPendingOrders(List<PendingOrder> orders) {
        ensureDataDirectory();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PENDING_ORDERS_DB, false))) {
            for (PendingOrder order : orders) {
                writer.write(order.toTextRecord());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving pending orders: " + e.getMessage());
        }
    }

    /**
     * Load all pending orders from the database
     */
    public static List<PendingOrder> loadAllPendingOrders() {
        List<PendingOrder> orders = new ArrayList<>();
        ensureDataDirectory();
        
        File file = new File(PENDING_ORDERS_DB);
        if (!file.exists()) {
            return orders;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(PENDING_ORDERS_DB))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        PendingOrder order = PendingOrder.fromTextRecord(line);
                        if (order != null) {
                            orders.add(order);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing pending order line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading pending orders: " + e.getMessage());
        }
        return orders;
    }

    /**
     * Load only pending orders (not completed)
     */
    public static List<PendingOrder> loadPendingOrders() {
        // Return any order that is not completed so the UI can show IN_QUEUE, IN_PROGRESS, READY, etc.
        return loadAllPendingOrders().stream()
            .filter(order -> order.isActive())
            .collect(Collectors.toList());
    }

    /**
     * Mark an order as completed
     */
    public static void markOrderCompleted(String orderId) {
        List<PendingOrder> allOrders = loadAllPendingOrders();
        
        for (PendingOrder order : allOrders) {
            if (order.getOrderId().equals(orderId)) {
                order.setStatus(PendingOrder.STATUS_COMPLETED);
                break;
            }
        }
        
        saveAllPendingOrders(allOrders);
    }

    /**
     * Delete an order from pending orders
     */
    public static void deletePendingOrder(String orderId) {
        List<PendingOrder> allOrders = loadAllPendingOrders();
        allOrders.removeIf(order -> order.getOrderId().equals(orderId));
        saveAllPendingOrders(allOrders);
    }

    /**
     * Get a specific pending order by ID
     */
    public static PendingOrder getPendingOrderById(String orderId) {
        List<PendingOrder> allOrders = loadAllPendingOrders();
        
        for (PendingOrder order : allOrders) {
            if (order.getOrderId().equals(orderId)) {
                return order;
            }
        }
        return null;
    }

    // ==================== RETURNS DATABASE ====================

    /**
     * Save a return transaction to the returns database
     */
    public static void saveReturnTransaction(ReturnTransaction returnTx) {
        ensureDataDirectory();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RETURNS_DB, true))) {
            writer.write(returnTx.toTextRecord());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error saving return transaction: " + e.getMessage());
        }
    }

    /**
     * Load all return transactions from the returns database
     */
    public static List<ReturnTransaction> loadAllReturns() {
        List<ReturnTransaction> returns = new ArrayList<>();
        File file = new File(RETURNS_DB);
        
        if (!file.exists()) {
            return returns;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    ReturnTransaction rt = ReturnTransaction.fromTextRecord(line);
                    if (rt != null) {
                        returns.add(rt);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading return transactions: " + e.getMessage());
        }
        
        return returns;
    }
}
