package com.coffeeshop.service;

import com.coffeeshop.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class Store {
    public static final int MAX_STOCK = 20;
    public static final int REFILL_THRESHOLD = 5;

    private List<Product> products;
    private Map<String, InventoryItem> inventory;
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
    }

    public void saveData() {
        PersistenceManager.save(products, inventory);
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
