package com.coffeeshop.service;

import com.coffeeshop.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class Store {
    private List<Product> products;
    private Map<String, InventoryItem> inventory;
    private Map<String, InventoryItem> removedInventory = new HashMap<>();
    private java.util.LinkedHashSet<String> categories = new java.util.LinkedHashSet<>();
    private java.util.List<Runnable> categoryListeners = new java.util.ArrayList<>();
    private java.util.List<Runnable> productListeners = new java.util.ArrayList<>();
    private java.util.List<Runnable> inventoryListeners = new java.util.ArrayList<>();
    private java.util.List<Runnable> specialRequestListeners = new java.util.ArrayList<>();
    private java.util.List<Runnable> addOnListeners = new java.util.ArrayList<>();
    private java.util.List<Runnable> complaintListeners = new java.util.ArrayList<>();
    private java.util.List<com.coffeeshop.model.CashierAccount> cashiers = new java.util.ArrayList<>();
    private java.util.List<com.coffeeshop.model.AddOn> addOns = new java.util.ArrayList<>();
    private java.util.List<com.coffeeshop.model.SpecialRequest> specialRequests = new java.util.ArrayList<>();
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
        try {
            Map<String, InventoryItem> rem = PersistenceManager.loadRemovedInventory();
            if (rem != null) removedInventory = new HashMap<>(rem);
        } catch (Exception ignored) { removedInventory = new HashMap<>(); }
        // load cashier accounts
        try {
            cashiers = PersistenceManager.loadAccounts();
        } catch (Exception ignored) {
            cashiers = new java.util.ArrayList<>();
        }
        // load add-ons
        try {
            addOns = PersistenceManager.loadAddOns();
        } catch (Exception ignored) {
            addOns = new java.util.ArrayList<>();
        }
        // load special requests
        try {
            specialRequests = PersistenceManager.loadSpecialRequests();
        } catch (Exception ignored) {
            specialRequests = new java.util.ArrayList<>();
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
        // Ensure canonical default categories exist (preserve existing categories)
        java.util.List<String> defaultCats = java.util.Arrays.asList("Coffee", "Milk Tea", "Frappe", "Fruit Tea", "Pastries");
        for (String c : defaultCats) {
            if (c != null && !c.trim().isEmpty() && !categories.contains(c)) categories.add(c);
        }
    }

    public void saveData() {
        PersistenceManager.saveProducts(products);
        PersistenceManager.saveInventory(inventory);
        // Notify any listeners that inventory (or removed inventory) may have changed
        notifyInventoryListeners();
        try { PersistenceManager.saveRemovedInventory(removedInventory); } catch (Exception ignored) {}
        try {
            PersistenceManager.saveAccounts(cashiers);
        } catch (Exception ignored) {}
        // save explicit categories file too
        try {
            PersistenceManager.saveCategories(new java.util.ArrayList<>(categories));
            // Notify listeners that products (or categories/inventory) may have changed
            notifyProductListeners();
        } catch (Exception ignored) {}
        // save add-ons
        try {
            PersistenceManager.saveAddOns(addOns);
            // notify add-on listeners
            notifyAddOnListeners();
        } catch (Exception ignored) {}
        // save special requests
        try {
            PersistenceManager.saveSpecialRequests(specialRequests);
        } catch (Exception ignored) {}
        // notify listeners specifically interested in special-request changes
        notifySpecialRequestListeners();
        // Notify listeners that products (or categories/inventory) may have changed
        notifyProductListeners();
    }

    // Reload products from disk (called by other processes via file-watch)
    public void reloadProductsFromDisk() {
        try {
            java.util.List<Product> persisted = PersistenceManager.loadProducts();
            if (persisted != null) {
                products.clear();
                products.addAll(persisted);
                notifyProductListeners();
            }
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

    public void changeCashierUsername(String id, String newUsername) {
        for (com.coffeeshop.model.CashierAccount c : cashiers) {
            if (c.getId().equals(id)) {
                c.setUsername(newUsername);
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

    // Move an inventory item to the removed/archive collection (soft-delete)
    public void removeInventoryItem(String name) {
        if (name == null) return;
        InventoryItem it = inventory.remove(name);
        if (it != null) {
            removedInventory.put(name, it);
            saveData();
        }
    }

    // Restore an item from the removed archive back into active inventory
    public void restoreRemovedItem(String name) {
        if (name == null) return;
        InventoryItem it = removedInventory.remove(name);
        if (it != null) {
            inventory.put(name, it);
            saveData();
        }
    }

    // Return a copy of removed/archived inventory
    public Map<String, InventoryItem> getRemovedInventory() {
        return new HashMap<>(removedInventory);
    }

    // Permanently delete an archived item
    public void permanentlyDeleteRemovedItem(String name) {
        if (name == null) return;
        if (removedInventory.remove(name) != null) saveData();
    }

    // Purge all archived items
    public void purgeAllRemovedItems() {
        if (!removedInventory.isEmpty()) {
            removedInventory.clear();
            saveData();
        }
    }

    public void deductInventory(String itemName, double amount) {
        InventoryItem item = inventory.get(itemName);
        if (item != null) {
            item.deduct(amount);
            saveData();
        }
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

    private void notifyProductListeners() {
        for (Runnable r : new java.util.ArrayList<>(productListeners)) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }

    private void notifySpecialRequestListeners() {
        for (Runnable r : new java.util.ArrayList<>(specialRequestListeners)) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }

    private void notifyAddOnListeners() {
        for (Runnable r : new java.util.ArrayList<>(addOnListeners)) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }

    private void notifyInventoryListeners() {
        for (Runnable r : new java.util.ArrayList<>(inventoryListeners)) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }

    private void notifyComplaintListeners() {
        for (Runnable r : new java.util.ArrayList<>(complaintListeners)) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }

    public void addCategoryChangeListener(Runnable r) {
        if (r == null) return;
        categoryListeners.add(r);
    }

    public void addInventoryChangeListener(Runnable r) {
        if (r == null) return;
        inventoryListeners.add(r);
    }

    public void removeInventoryChangeListener(Runnable r) {
        if (r == null) return;
        inventoryListeners.remove(r);
    }

    public void addProductChangeListener(Runnable r) {
        if (r == null) return;
        productListeners.add(r);
    }

    public void addSpecialRequestChangeListener(Runnable r) {
        if (r == null) return;
        specialRequestListeners.add(r);
    }

    public void removeSpecialRequestChangeListener(Runnable r) {
        specialRequestListeners.remove(r);
    }

    public void addAddOnChangeListener(Runnable r) {
        if (r == null) return;
        addOnListeners.add(r);
    }

    public void addComplaintChangeListener(Runnable r) {
        if (r == null) return;
        complaintListeners.add(r);
    }

    public void removeComplaintChangeListener(Runnable r) {
        complaintListeners.remove(r);
    }

    public void removeAddOnChangeListener(Runnable r) {
        addOnListeners.remove(r);
    }

    public void removeProductChangeListener(Runnable r) {
        productListeners.remove(r);
    }

    public void removeCategoryChangeListener(Runnable r) {
        categoryListeners.remove(r);
    }

    // Order validation and processing
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
        // Check ingredient inventory before deduction
        if (!isInventorySufficient(order)) {
            throw new IllegalStateException("Insufficient ingredients");
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

    // Add-on management methods
    public java.util.List<com.coffeeshop.model.AddOn> getAddOns() {
        return new java.util.ArrayList<>(addOns);
    }

    // Special requests management
    public java.util.List<com.coffeeshop.model.SpecialRequest> getSpecialRequests() {
        return new java.util.ArrayList<>(specialRequests);
    }

    public java.util.List<com.coffeeshop.model.SpecialRequest> getActiveSpecialRequests() {
        return specialRequests.stream().filter(com.coffeeshop.model.SpecialRequest::isActive).collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<com.coffeeshop.model.SpecialRequest> getActiveSpecialRequestsForProduct(String productId) {
        if (productId == null) return getActiveSpecialRequests();
        Product p = getProductById(productId);
        String prodCat = p == null ? "" : (p.getCategory() == null ? "" : p.getCategory());
        java.util.List<com.coffeeshop.model.SpecialRequest> out = new java.util.ArrayList<>();
        for (com.coffeeshop.model.SpecialRequest r : specialRequests) {
            if (!r.isActive()) continue;
            String cat = r.getCategory() == null ? "All" : r.getCategory();
            // if category is All or matches product category, and product-specific list either empty or contains productId
            boolean categoryMatch = "All".equalsIgnoreCase(cat) || (!prodCat.isEmpty() && cat.equalsIgnoreCase(prodCat));
            if (!categoryMatch) continue;
            java.util.List<String> apps = r.getApplicableProductIds();
            if (apps == null || apps.isEmpty() || apps.contains(productId)) {
                out.add(r);
            }
        }
        return out;
    }

    public void addSpecialRequest(com.coffeeshop.model.SpecialRequest r) {
        specialRequests.add(r);
        saveData();
    }

    public void updateSpecialRequest(com.coffeeshop.model.SpecialRequest r) {
        for (int i = 0; i < specialRequests.size(); i++) {
            if (specialRequests.get(i).getId().equals(r.getId())) {
                specialRequests.set(i, r);
                saveData();
                return;
            }
        }
    }

    public void deleteSpecialRequest(String id) {
        specialRequests.removeIf(s -> s.getId().equals(id));
        saveData();
    }

    public void toggleSpecialRequestActive(String id) {
        for (com.coffeeshop.model.SpecialRequest r : specialRequests) {
            if (r.getId().equals(id)) { r.setActive(!r.isActive()); saveData(); return; }
        }
    }

    public java.util.List<com.coffeeshop.model.AddOn> getActiveAddOns() {
        return addOns.stream()
                .filter(com.coffeeshop.model.AddOn::isActive)
                .collect(Collectors.toList());
    }

    public java.util.List<com.coffeeshop.model.AddOn> getAddOnsForProduct(String productId) {
        Product product = getProductById(productId);
        if (product == null) return new java.util.ArrayList<>();
        
        String category = product.getCategory();
        return addOns.stream()
                .filter(com.coffeeshop.model.AddOn::isActive)
                .filter(addOn -> {
                    // Check if add-on applies to this product's category
                    if (addOn.getCategory().equalsIgnoreCase("All")) return true;
                    if (addOn.getCategory().equalsIgnoreCase(category)) {
                        // Check if specifically assigned to this product
                        return addOn.isApplicableToProduct(productId);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public com.coffeeshop.model.AddOn getAddOnById(String id) {
        return addOns.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void addAddOn(com.coffeeshop.model.AddOn addOn) {
        addOns.add(addOn);
        saveData();
    }

    public void updateAddOn(com.coffeeshop.model.AddOn addOn) {
        for (int i = 0; i < addOns.size(); i++) {
            if (addOns.get(i).getId().equals(addOn.getId())) {
                addOns.set(i, addOn);
                saveData();
                return;
            }
        }
    }

    public void deleteAddOn(String id) {
        addOns.removeIf(a -> a.getId().equals(id));
        saveData();
    }

    public void toggleAddOnActive(String id) {
        for (com.coffeeshop.model.AddOn addOn : addOns) {
            if (addOn.getId().equals(id)) {
                addOn.setActive(!addOn.isActive());
                saveData();
                return;
            }
        }
    }

    // Complaints persistence helpers (wrapper over TextDatabase to centralize notify)
    public void saveComplaint(com.coffeeshop.model.Complaint complaint) {
        if (complaint == null) return;
        try {
            TextDatabase.saveComplaint(complaint);
        } catch (Exception ignored) {}
        notifyComplaintListeners();
    }

    public java.util.List<com.coffeeshop.model.Complaint> getAllComplaints() {
        try {
            return TextDatabase.loadAllComplaints();
        } catch (Exception ignored) { return new java.util.ArrayList<>(); }
    }
}
