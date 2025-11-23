package com.coffeeshop.service;

import com.coffeeshop.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class Store {
    public static final int MAX_STOCK = 20;
    public static final int REFILL_THRESHOLD = 5;

    private List<Product> products;
    private Map<String, InventoryItem> inventory;
    private java.util.LinkedHashSet<String> categories = new java.util.LinkedHashSet<>();
    private java.util.List<Runnable> categoryListeners = new java.util.ArrayList<>();
    private java.util.List<com.coffeeshop.model.CashierAccount> cashiers = new java.util.ArrayList<>();
    private static Store instance;

    private Store() {
        loadData();
    }

    public static Store getInstance() {
        if (instance == null) {
            instance = new Store();
        }
        return instance;
    }

    private void loadData() {
        products = PersistenceManager.loadProducts();
        inventory = PersistenceManager.loadInventory();
        // load cashier accounts
        try {
            cashiers = PersistenceManager.loadAccounts();
        } catch (Exception ignored) {
            cashiers = new java.util.ArrayList<>();
        }
        // initialize categories: prefer explicit categories file if present, otherwise infer from products
        categories.clear();
        try {
            java.util.List<String> persisted = PersistenceManager.loadCategories();
            if (persisted != null && !persisted.isEmpty()) {
                for (String c : persisted) if (c != null && !c.trim().isEmpty()) categories.add(c.trim());
            } else {
                for (Product p : products) {
                    try {
                        if (p.getCategory() != null && !p.getCategory().trim().isEmpty()) categories.add(p.getCategory());
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ex) {
            for (Product p : products) {
                try {
                    if (p.getCategory() != null && !p.getCategory().trim().isEmpty()) categories.add(p.getCategory());
                } catch (Exception ignored) {}
            }
        }
    }

    public void saveData() {
        PersistenceManager.saveProducts(products);
        PersistenceManager.saveInventory(inventory);
        try {
            PersistenceManager.saveAccounts(cashiers);
        } catch (Exception ignored) {}
        // save explicit categories file too
        try {
            PersistenceManager.saveCategories(new java.util.ArrayList<>(categories));
        } catch (Exception ignored) {}
    }

    // Cashier account management
    public java.util.List<com.coffeeshop.model.CashierAccount> getCashiers() {
        return new java.util.ArrayList<>(cashiers);
    }

    public com.coffeeshop.model.CashierAccount getCashierByUsername(String username) {
        for (com.coffeeshop.model.CashierAccount c : cashiers) {
            if (c.getUsername().equalsIgnoreCase(username)) return c;
        }
        return null;
    }

    public void addCashier(com.coffeeshop.model.CashierAccount account) {
        cashiers.add(account);
        saveData();
        notifyCategoryListeners();
    }

    public void updateCashier(com.coffeeshop.model.CashierAccount account) {
        for (int i = 0; i < cashiers.size(); i++) {
            if (cashiers.get(i).getId().equals(account.getId())) {
                cashiers.set(i, account);
                saveData();
                return;
            }
        }
    }

    public void setCashierActive(String id, boolean active) {
        for (com.coffeeshop.model.CashierAccount c : cashiers) {
            if (c.getId().equals(id)) {
                c.setActive(active);
                saveData();
                return;
            }
        }
    }

    public void changeCashierPassword(String id, String newPassword) {
        for (com.coffeeshop.model.CashierAccount c : cashiers) {
            if (c.getId().equals(id)) {
                c.setPassword(newPassword);
                saveData();
                return;
            }
        }
    }

    // Product operations
    public List<Product> getProducts() {
        return new ArrayList<>(products);
    }

    public Product getProductById(String id) {
        return products.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void addProduct(Product product) {
        products.add(product);
        // Add the product's category to the categories set
        if (product.getCategory() != null && !product.getCategory().trim().isEmpty()) {
            categories.add(product.getCategory().trim());
        }
        saveData();
    }

    public void removeProduct(String productId) {
        products.removeIf(p -> p.getId().equals(productId));
        saveData();
    }

    public void removeProductWithIngredients(String productId, boolean removeIngredients) {
        Product product = getProductById(productId);
        if (product != null && removeIngredients) {
            for (String ingredientName : product.getRecipe().keySet()) {
                inventory.remove(ingredientName);
            }
        }
        removeProduct(productId);
    }

    public void refillProduct(String productId, int amount) {
        Product product = getProductById(productId);
        if (product != null) {
            int newStock = Math.min(product.getStock() + amount, MAX_STOCK);
            product.setStock(newStock);
            saveData();
        }
    }

    // Inventory operations
    public Map<String, InventoryItem> getInventory() {
        return new HashMap<>(inventory);
    }

    public InventoryItem getInventoryItem(String name) {
        return inventory.get(name);
    }

    public void refillInventory(String itemName, double amount) {
        InventoryItem item = inventory.get(itemName);
        if (item != null) {
            item.refill(amount);
            saveData();
        }
    }

    public void addInventoryItem(InventoryItem item) {
        inventory.put(item.getName(), item);
        saveData();
    }

    // Category management (backwards-compat helpers for AdminApp)
    public void addCategory(String category) {
        if (category == null || category.trim().isEmpty()) return;
        categories.add(category.trim());
        saveData();
        notifyCategoryListeners();
    }

    public java.util.List<String> getCategories() {
        return new java.util.ArrayList<>(categories);
    }

    public void renameCategory(String oldName, String newName) {
        if (oldName == null || newName == null) return;
        if (!categories.remove(oldName)) return;
        categories.add(newName);
        // update products that referenced the old category
        for (Product p : products) {
            try {
                if (oldName.equals(p.getCategory())) p.setCategory(newName);
            } catch (Exception ignored) {}
        }
        saveData();
        notifyCategoryListeners();
    }

    public void removeCategory(String name) {
        if (name == null) return;
        categories.remove(name);
        saveData();
        notifyCategoryListeners();
    }

    // Reload categories from disk (called by other processes via file-watch)
    public void reloadCategoriesFromDisk() {
        try {
            java.util.List<String> persisted = PersistenceManager.loadCategories();
            if (persisted != null) {
                categories.clear();
                for (String c : persisted) if (c != null && !c.trim().isEmpty()) categories.add(c.trim());
                notifyCategoryListeners();
            }
        } catch (Exception ignored) {}
    }

    private void notifyCategoryListeners() {
        for (Runnable r : new java.util.ArrayList<>(categoryListeners)) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }

    public void addCategoryChangeListener(Runnable r) {
        if (r == null) return;
        categoryListeners.add(r);
    }

    public void removeCategoryChangeListener(Runnable r) {
        categoryListeners.remove(r);
    }

    // Order validation and processing
    public boolean isStockSufficient(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = getProductById(item.getProduct().getId());
            if (product == null || product.getStock() < item.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    public boolean isInventorySufficient(Order order) {
        Map<String, Double> requiredIngredients = new HashMap<>();
        
        for (OrderItem item : order.getItems()) {
            Product product = getProductById(item.getProduct().getId());
            if (product != null) {
                for (Map.Entry<String, Double> entry : product.getRecipe().entrySet()) {
                    String ingredientName = entry.getKey();
                    double requiredAmount = entry.getValue() * item.getQuantity();
                    requiredIngredients.merge(ingredientName, requiredAmount, Double::sum);
                }
            }
        }

        for (Map.Entry<String, Double> entry : requiredIngredients.entrySet()) {
            InventoryItem item = inventory.get(entry.getKey());
            if (item == null || !item.isSufficient(entry.getValue())) {
                return false;
            }
        }
        
        return true;
    }

    public String getInsufficientIngredient(Order order) {
        Map<String, Double> requiredIngredients = new HashMap<>();
        
        for (OrderItem item : order.getItems()) {
            Product product = getProductById(item.getProduct().getId());
            if (product != null) {
                for (Map.Entry<String, Double> entry : product.getRecipe().entrySet()) {
                    String ingredientName = entry.getKey();
                    double requiredAmount = entry.getValue() * item.getQuantity();
                    requiredIngredients.merge(ingredientName, requiredAmount, Double::sum);
                }
            }
        }

        for (Map.Entry<String, Double> entry : requiredIngredients.entrySet()) {
            InventoryItem item = inventory.get(entry.getKey());
            if (item == null || !item.isSufficient(entry.getValue())) {
                return entry.getKey();
            }
        }
        
        return null;
    }

    public void checkoutBasket(Order order) {
        // Recheck stock before deduction
        if (!isStockSufficient(order) || !isInventorySufficient(order)) {
            throw new IllegalStateException("Insufficient stock or ingredients");
        }

        // Deduct product stock
        for (OrderItem item : order.getItems()) {
            Product product = getProductById(item.getProduct().getId());
            if (product != null) {
                product.setStock(product.getStock() - item.getQuantity());
            }
        }

        // Deduct ingredients
        Map<String, Double> requiredIngredients = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            Product product = getProductById(item.getProduct().getId());
            if (product != null) {
                for (Map.Entry<String, Double> entry : product.getRecipe().entrySet()) {
                    String ingredientName = entry.getKey();
                    double requiredAmount = entry.getValue() * item.getQuantity();
                    requiredIngredients.merge(ingredientName, requiredAmount, Double::sum);
                }
            }
        }

        for (Map.Entry<String, Double> entry : requiredIngredients.entrySet()) {
            InventoryItem item = inventory.get(entry.getKey());
            if (item != null) {
                item.deduct(entry.getValue());
            }
        }

        order.setPaid(true);
        saveData();
    }

    // Refill alerts
    public boolean hasProductsNeedingRefill() {
        return products.stream().anyMatch(Product::needsRefill);
    }

    public List<Product> getProductsNeedingRefill() {
        return products.stream()
                .filter(Product::needsRefill)
                .collect(Collectors.toList());
    }

    public String getProductRefillAlerts() {
        StringBuilder alerts = new StringBuilder();
        alerts.append("\n╔═══════════════════════════════════════════════════════╗\n");
        alerts.append("║            PRODUCT REFILL STATUS                     ║\n");
        alerts.append("╠═══════════════════════════════════════════════════════╣\n");
        alerts.append(String.format("║ %-20s | %-10s | %-15s ║\n", "Product", "Stock", "Refill Needed"));
        alerts.append("╠═══════════════════════════════════════════════════════╣\n");

        for (Product product : products) {
            String severity;
            if (product.getStock() == 0) {
                severity = "🔴 CRITICAL";
            } else if (product.getStock() <= REFILL_THRESHOLD) {
                severity = "🟡 WARNING";
            } else {
                continue; // Skip products that don't need refill
            }

            alerts.append(String.format("║ %-20s | %-10d | %-15d ║ %s\n", 
                product.getName(), 
                product.getStock(), 
                product.getRefillNeeded(),
                severity));
        }

        alerts.append("╚═══════════════════════════════════════════════════════╝\n");
        return alerts.toString();
    }
}
