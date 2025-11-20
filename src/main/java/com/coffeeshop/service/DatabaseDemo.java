package com.coffeeshop.service;

import com.coffeeshop.model.ItemRecord;
import com.coffeeshop.model.OrderRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Utility class to demonstrate Text Database functionality
 */
public class DatabaseDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Coffee Shop Text Database Demo ===\n");
        
        // 1. Add sample items
        System.out.println("1. Adding sample items to database...");
        TextDatabase.saveItem(new ItemRecord("Espresso", 3.50, "Hot", "Small", 50));
        TextDatabase.saveItem(new ItemRecord("Iced Latte", 4.75, "Cold", "Medium", 75));
        TextDatabase.saveItem(new ItemRecord("Cappuccino", 4.50, "Hot", "Large", 25));
        System.out.println("   Items added successfully!\n");
        
        // 2. Load and display all items
        System.out.println("2. Loading all items from database:");
        List<ItemRecord> items = TextDatabase.loadAllItems();
        for (ItemRecord item : items) {
            System.out.println("   - " + item.getItemName() + " | ₱" + item.getPrice() + 
                             " | " + item.getTypeOfDrink() + " | " + item.getSize() + 
                             " | Sugar: " + item.getSugarLevel() + "%");
        }
        System.out.println("   Total items: " + TextDatabase.getItemsCount() + "\n");
        
        // 3. Add sample orders
        System.out.println("3. Adding sample orders to database...");
        LocalDateTime now = LocalDateTime.now();
        TextDatabase.saveOrder(new OrderRecord("ORD001", "John Doe", now, "Espresso", "Small", "Hot"));
        TextDatabase.saveOrder(new OrderRecord("ORD002", "Jane Smith", now.minusMinutes(30), "Iced Latte", "Medium", "Cold"));
        TextDatabase.saveOrder(new OrderRecord("ORD003", "Bob Wilson", now.minusHours(1), "Cappuccino", "Large", "Hot"));
        System.out.println("   Orders added successfully!\n");
        
        // 4. Load and display all orders
        System.out.println("4. Loading all orders from database:");
        List<OrderRecord> orders = TextDatabase.loadAllOrders();
        for (OrderRecord order : orders) {
            System.out.println("   - Order ID: " + order.getOrderId() + 
                             " | Customer: " + order.getUserName() +
                             " | Item: " + order.getItemName() +
                             " | Size: " + order.getSize() +
                             " | Type: " + order.getTypeOfDrink() +
                             " | Time: " + order.getOrderTime());
        }
        System.out.println("   Total orders: " + TextDatabase.getOrdersCount() + "\n");
        
        // 5. Search functionality
        System.out.println("5. Searching for items containing 'Latte':");
        List<ItemRecord> searchResults = TextDatabase.searchItemsByName("Latte");
        for (ItemRecord item : searchResults) {
            System.out.println("   - Found: " + item.getItemName());
        }
        
        System.out.println("\n6. Searching for orders by 'Jane':");
        List<OrderRecord> orderSearchResults = TextDatabase.searchOrdersByUserName("Jane");
        for (OrderRecord order : orderSearchResults) {
            System.out.println("   - Found: " + order.getOrderId() + " for " + order.getUserName());
        }
        
        System.out.println("\n=== Database files location ===");
        System.out.println("Items: data/items_database.txt");
        System.out.println("Orders: data/orders_database.txt");
        System.out.println("\nYou can open these files with any text editor to view the data!");
    }
}
