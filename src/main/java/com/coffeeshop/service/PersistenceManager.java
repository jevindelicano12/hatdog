package com.coffeeshop.service;

import com.coffeeshop.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistenceManager {
    private static final String DATA_DIR = "data";
    private static final String PRODUCTS_FILE = DATA_DIR + "/products.json";
    private static final String INVENTORY_FILE = DATA_DIR + "/inventory.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void ensureDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void saveProducts(List<Product> products) {
        ensureDataDirectory();
        try (Writer writer = new FileWriter(PRODUCTS_FILE)) {
            gson.toJson(products, writer);
        } catch (IOException e) {
            System.err.println("Error saving products: " + e.getMessage());
        }
    }

    public static List<Product> loadProducts() {
        ensureDataDirectory();
        File file = new File(PRODUCTS_FILE);
        if (!file.exists()) {
            return initializeDefaultProducts();
        }

        try (Reader reader = new FileReader(PRODUCTS_FILE)) {
            Type listType = new TypeToken<ArrayList<Product>>(){}.getType();
            List<Product> products = gson.fromJson(reader, listType);
            
            // Ensure stock defaults to 20 if not set
            if (products != null) {
                for (Product p : products) {
                    if (p.getStock() == 0 && p.getRecipe() != null && !p.getRecipe().isEmpty()) {
                        p.setStock(20);
                    }
                }
                return products;
            }
            return initializeDefaultProducts();
        } catch (IOException e) {
            System.err.println("Error loading products: " + e.getMessage());
            return initializeDefaultProducts();
        }
    }

    public static void saveInventory(Map<String, InventoryItem> inventory) {
        ensureDataDirectory();
        try (Writer writer = new FileWriter(INVENTORY_FILE)) {
            gson.toJson(inventory, writer);
        } catch (IOException e) {
            System.err.println("Error saving inventory: " + e.getMessage());
        }
    }

    public static Map<String, InventoryItem> loadInventory() {
        ensureDataDirectory();
        File file = new File(INVENTORY_FILE);
        if (!file.exists()) {
            return initializeDefaultInventory();
        }

        try (Reader reader = new FileReader(INVENTORY_FILE)) {
            Type mapType = new TypeToken<HashMap<String, InventoryItem>>(){}.getType();
            Map<String, InventoryItem> inventory = gson.fromJson(reader, mapType);
            return inventory != null ? inventory : initializeDefaultInventory();
        } catch (IOException e) {
            System.err.println("Error loading inventory: " + e.getMessage());
            return initializeDefaultInventory();
        }
    }

    private static List<Product> initializeDefaultProducts() {
        List<Product> products = new ArrayList<>();
        
        // Espresso
        Map<String, Double> espressoRecipe = new HashMap<>();
        espressoRecipe.put("Coffee Beans", 18.0);
        espressoRecipe.put("Water", 30.0);
        products.add(new Product("P001", "Espresso", 3.50, 20, espressoRecipe, "Coffee"));

        // Cappuccino
        Map<String, Double> cappuccinoRecipe = new HashMap<>();
        cappuccinoRecipe.put("Coffee Beans", 18.0);
        cappuccinoRecipe.put("Milk", 120.0);
        cappuccinoRecipe.put("Water", 30.0);
        products.add(new Product("P002", "Cappuccino", 4.50, 20, cappuccinoRecipe, "Coffee"));

        // Latte
        Map<String, Double> latteRecipe = new HashMap<>();
        latteRecipe.put("Coffee Beans", 18.0);
        latteRecipe.put("Milk", 200.0);
        latteRecipe.put("Water", 30.0);
        products.add(new Product("P003", "Latte", 4.75, 20, latteRecipe, "Coffee"));

        // Matcha Latte
        Map<String, Double> matchaRecipe = new HashMap<>();
        matchaRecipe.put("Matcha Powder", 5.0);
        matchaRecipe.put("Milk", 200.0);
        matchaRecipe.put("Water", 50.0);
        products.add(new Product("P004", "Matcha Latte", 5.00, 20, matchaRecipe, "Milk Tea"));

        // Mocha
        Map<String, Double> mochaRecipe = new HashMap<>();
        mochaRecipe.put("Coffee Beans", 18.0);
        mochaRecipe.put("Milk", 180.0);
        mochaRecipe.put("Chocolate Syrup", 30.0);
        mochaRecipe.put("Water", 30.0);
        products.add(new Product("P005", "Mocha", 5.25, 20, mochaRecipe, "Coffee"));

        // Iced Frappe
        Map<String, Double> frappeRecipe = new HashMap<>();
        frappeRecipe.put("Milk", 200.0);
        frappeRecipe.put("Coffee Beans", 15.0);
        frappeRecipe.put("Ice", 100.0);
        products.add(new Product("P006", "Iced Frappe", 4.50, 20, frappeRecipe, "Frappe"));

        // Mango Frappe
        Map<String, Double> mangoFrappeRecipe = new HashMap<>();
        mangoFrappeRecipe.put("Milk", 200.0);
        mangoFrappeRecipe.put("Mango Puree", 50.0);
        mangoFrappeRecipe.put("Ice", 100.0);
        products.add(new Product("P007", "Mango Frappe", 5.00, 20, mangoFrappeRecipe, "Frappe"));

        // Strawberry Tea
        Map<String, Double> strawberryTeaRecipe = new HashMap<>();
        strawberryTeaRecipe.put("Water", 100.0);
        strawberryTeaRecipe.put("Strawberry Puree", 30.0);
        strawberryTeaRecipe.put("Tea Leaves", 5.0);
        products.add(new Product("P008", "Strawberry Tea", 4.00, 20, strawberryTeaRecipe, "Fruit Tea"));

        // Mango Tea
        Map<String, Double> mangoTeaRecipe = new HashMap<>();
        mangoTeaRecipe.put("Water", 100.0);
        mangoTeaRecipe.put("Mango Puree", 30.0);
        mangoTeaRecipe.put("Tea Leaves", 5.0);
        products.add(new Product("P009", "Mango Tea", 4.00, 20, mangoTeaRecipe, "Fruit Tea"));

        // Croissant
        Map<String, Double> croissantRecipe = new HashMap<>();
        croissantRecipe.put("Croissant Dough", 100.0);
        products.add(new Product("P010", "Croissant", 3.50, 20, croissantRecipe, "Pastries"));

        // Blueberry Muffin
        Map<String, Double> muffinRecipe = new HashMap<>();
        muffinRecipe.put("Muffin Mix", 150.0);
        muffinRecipe.put("Blueberries", 50.0);
        products.add(new Product("P011", "Blueberry Muffin", 3.00, 20, muffinRecipe, "Pastries"));

        saveProducts(products);
        return products;
    }

    private static Map<String, InventoryItem> initializeDefaultInventory() {
        Map<String, InventoryItem> inventory = new HashMap<>();
        
        inventory.put("Coffee Beans", new InventoryItem("Coffee Beans", 5000.0, "g"));
        inventory.put("Milk", new InventoryItem("Milk", 10000.0, "ml"));
        inventory.put("Water", new InventoryItem("Water", 20000.0, "ml"));
        inventory.put("Matcha Powder", new InventoryItem("Matcha Powder", 500.0, "g"));
        inventory.put("Chocolate Syrup", new InventoryItem("Chocolate Syrup", 2000.0, "ml"));
        inventory.put("Vanilla Syrup", new InventoryItem("Vanilla Syrup", 2000.0, "ml"));
        inventory.put("Caramel Syrup", new InventoryItem("Caramel Syrup", 2000.0, "ml"));
        inventory.put("Ice", new InventoryItem("Ice", 5000.0, "g"));
        inventory.put("Mango Puree", new InventoryItem("Mango Puree", 2000.0, "ml"));
        inventory.put("Strawberry Puree", new InventoryItem("Strawberry Puree", 2000.0, "ml"));
        inventory.put("Tea Leaves", new InventoryItem("Tea Leaves", 1000.0, "g"));
        inventory.put("Croissant Dough", new InventoryItem("Croissant Dough", 5000.0, "g"));
        inventory.put("Muffin Mix", new InventoryItem("Muffin Mix", 5000.0, "g"));
        inventory.put("Blueberries", new InventoryItem("Blueberries", 2000.0, "g"));

        saveInventory(inventory);
        return inventory;
    }

    public static void save(List<Product> products, Map<String, InventoryItem> inventory) {
        saveProducts(products);
        saveInventory(inventory);
    }
}
