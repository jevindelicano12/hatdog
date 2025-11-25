package com.coffeeshop.model;

import java.util.ArrayList;
import java.util.List;

public class SpecialRequest {
    private String id;
    private String text;
    private boolean active = true;
    // assignment: category (e.g., "Coffee", "All") and specific product IDs
    private String category = "All";
    private List<String> applicableProductIds = new ArrayList<>();

    public SpecialRequest() {}

    public SpecialRequest(String id, String text) {
        this.id = id;
        this.text = text;
        this.active = true;
        this.category = "All";
        this.applicableProductIds = new ArrayList<>();
    }

    public SpecialRequest(String id, String text, boolean active) {
        this.id = id;
        this.text = text;
        this.active = active;
        this.category = "All";
        this.applicableProductIds = new ArrayList<>();
    }

    public SpecialRequest(String id, String text, boolean active, String category, List<String> applicableProductIds) {
        this.id = id;
        this.text = text;
        this.active = active;
        this.category = category == null ? "All" : category;
        this.applicableProductIds = applicableProductIds == null ? new ArrayList<>() : applicableProductIds;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getApplicableProductIds() { return new ArrayList<>(applicableProductIds); }
    public void setApplicableProductIds(List<String> ids) { this.applicableProductIds = ids == null ? new ArrayList<>() : new ArrayList<>(ids); }

    public void addApplicableProduct(String productId) {
        if (productId == null) return;
        if (!this.applicableProductIds.contains(productId)) this.applicableProductIds.add(productId);
    }

    public boolean isApplicableToProduct(String productId) {
        if (productId == null) return false;
        if (this.applicableProductIds == null || this.applicableProductIds.isEmpty()) return true; // empty list -> applies to whole category
        return this.applicableProductIds.contains(productId);
    }
}
