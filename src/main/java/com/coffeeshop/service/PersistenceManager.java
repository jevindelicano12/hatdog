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
    private static final String REMOVED_INVENTORY_FILE = DATA_DIR + "/inventory_removed.json";
    private static final String CATEGORIES_FILE = DATA_DIR + "/categories.json";
    private static final String ACCOUNTS_FILE = DATA_DIR + "/accounts.json";
    private static final String ADDONS_FILE = DATA_DIR + "/addons.json";
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

    public static void saveRemovedInventory(Map<String, InventoryItem> removed) {
        ensureDataDirectory();
        try (Writer writer = new FileWriter(REMOVED_INVENTORY_FILE)) {
            gson.toJson(removed, writer);
        } catch (IOException e) {
            System.err.println("Error saving removed inventory: " + e.getMessage());
        }
    }

    public static void saveCategories(java.util.List<String> categories) {
        ensureDataDirectory();
        try (Writer writer = new FileWriter(CATEGORIES_FILE)) {
            gson.toJson(categories, writer);
        } catch (IOException e) {
            System.err.println("Error saving categories: " + e.getMessage());
        }
    }

    public static java.util.List<String> loadCategories() {
        ensureDataDirectory();
        File file = new File(CATEGORIES_FILE);
        if (!file.exists()) return null;
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<java.util.ArrayList<String>>(){}.getType();
            java.util.List<String> cats = gson.fromJson(reader, listType);
            return cats;
        } catch (IOException e) {
            System.err.println("Error loading categories: " + e.getMessage());
            return null;
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
            // If the file exists but contains an empty object, treat it as uninitialized and create defaults
            if (inventory == null || inventory.isEmpty()) return initializeDefaultInventory();
            return inventory;
        } catch (IOException e) {
            System.err.println("Error loading inventory: " + e.getMessage());
            return initializeDefaultInventory();
        }
    }

    public static Map<String, InventoryItem> loadRemovedInventory() {
        ensureDataDirectory();
        File file = new File(REMOVED_INVENTORY_FILE);
        if (!file.exists()) {
            return new HashMap<>();
        }

        try (Reader reader = new FileReader(REMOVED_INVENTORY_FILE)) {
            Type mapType = new TypeToken<HashMap<String, InventoryItem>>(){}.getType();
            Map<String, InventoryItem> removed = gson.fromJson(reader, mapType);
            return removed != null ? removed : new HashMap<>();
        } catch (IOException e) {
            System.err.println("Error loading removed inventory: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public static void saveAccounts(java.util.List<com.coffeeshop.model.CashierAccount> accounts) {
        ensureDataDirectory();
        try (Writer writer = new FileWriter(ACCOUNTS_FILE)) {
            gson.toJson(accounts, writer);
        } catch (IOException e) {
            System.err.println("Error saving accounts: " + e.getMessage());
        }
    }

    public static java.util.List<com.coffeeshop.model.CashierAccount> loadAccounts() {
        ensureDataDirectory();
        File file = new File(ACCOUNTS_FILE);
        if (!file.exists()) {
            // create default cashier accounts
            java.util.List<com.coffeeshop.model.CashierAccount> defaults = new java.util.ArrayList<>();
            defaults.add(new com.coffeeshop.model.CashierAccount("C001", "cashier1", "cashier1", true));
            defaults.add(new com.coffeeshop.model.CashierAccount("C002", "cashier2", "cashier2", true));
            saveAccounts(defaults);
            return defaults;
        }

        try (Reader reader = new FileReader(ACCOUNTS_FILE)) {
            Type listType = new TypeToken<java.util.ArrayList<com.coffeeshop.model.CashierAccount>>(){}.getType();
            java.util.List<com.coffeeshop.model.CashierAccount> accounts = gson.fromJson(reader, listType);
            return accounts != null ? accounts : new java.util.ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error loading accounts: " + e.getMessage());
            return new java.util.ArrayList<>();
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

    // Add-on persistence methods
    public static void saveAddOns(List<AddOn> addOns) {
        ensureDataDirectory();
        try (Writer writer = new FileWriter(ADDONS_FILE)) {
            gson.toJson(addOns, writer);
        } catch (IOException e) {
            System.err.println("Error saving add-ons: " + e.getMessage());
        }
    }

    public static List<AddOn> loadAddOns() {
        ensureDataDirectory();
        File file = new File(ADDONS_FILE);
        if (!file.exists()) {
            return initializeDefaultAddOns();
        }

        try (Reader reader = new FileReader(ADDONS_FILE)) {
            Type listType = new TypeToken<ArrayList<AddOn>>(){}.getType();
            List<AddOn> addOns = gson.fromJson(reader, listType);
            return addOns != null ? addOns : initializeDefaultAddOns();
        } catch (IOException e) {
            System.err.println("Error loading add-ons: " + e.getMessage());
            return initializeDefaultAddOns();
        }
    }

    private static List<AddOn> initializeDefaultAddOns() {
        List<AddOn> addOns = new ArrayList<>();
        
        // Coffee add-ons
        AddOn extraShot = new AddOn("A001", "Extra Shot", 1.00, "Coffee");
        extraShot.addApplicableProduct("P001"); // Espresso
        extraShot.addApplicableProduct("P002"); // Latte
        extraShot.addApplicableProduct("P003"); // Cappuccino
        extraShot.addApplicableProduct("P004"); // Americano
        addOns.add(extraShot);

        AddOn whippedCream = new AddOn("A002", "Whipped Cream", 0.50, "Coffee");
        addOns.add(whippedCream);

        AddOn vanillaSyrup = new AddOn("A003", "Vanilla Syrup", 0.75, "Coffee");
        addOns.add(vanillaSyrup);

        AddOn caramelSauce = new AddOn("A004", "Caramel Sauce", 0.75, "Coffee");
        addOns.add(caramelSauce);

        // Milk Tea add-ons
        AddOn tapiocaPearls = new AddOn("A005", "Tapioca Pearls", 10.00, "Milk Tea");
        addOns.add(tapiocaPearls);

        AddOn jellies = new AddOn("A006", "Jellies", 10.00, "Milk Tea");
        addOns.add(jellies);

        AddOn poppingBoba = new AddOn("A007", "Popping Boba", 10.00, "Milk Tea");
        addOns.add(poppingBoba);

        saveAddOns(addOns);
        return addOns;
    }
}
