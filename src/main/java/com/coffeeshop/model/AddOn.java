package com.coffeeshop.model;

import java.util.ArrayList;
import java.util.List;

public class AddOn {
    private String id;
    private String name;
    private double price;
    private String category; // "Coffee", "Milk Tea", "All", etc.
    private List<String> applicableProductIds; // List of product IDs this add-on can be used with
    private boolean active; // Whether this add-on is currently available

    public AddOn(String id, String name, double price, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.applicableProductIds = new ArrayList<>();
        this.active = true;
    }

    public AddOn(String id, String name, double price, String category, List<String> applicableProductIds, boolean active) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.applicableProductIds = applicableProductIds != null ? applicableProductIds : new ArrayList<>();
        this.active = active;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getApplicableProductIds() {
        return applicableProductIds;
    }

    public void setApplicableProductIds(List<String> applicableProductIds) {
        this.applicableProductIds = applicableProductIds;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void addApplicableProduct(String productId) {
        if (!this.applicableProductIds.contains(productId)) {
            this.applicableProductIds.add(productId);
        }
    }

    public void removeApplicableProduct(String productId) {
        this.applicableProductIds.remove(productId);
    }

    public boolean isApplicableToProduct(String productId) {
        // If no specific products are assigned, it's applicable to all products in the category
        if (applicableProductIds.isEmpty()) {
            return true;
        }
        return applicableProductIds.contains(productId);
    }

    public String getFormattedPrice() {
        return String.format("₱%.2f", price);
    }

    @Override
    public String toString() {
        return name + " (+" + getFormattedPrice() + ")";
    }
}
