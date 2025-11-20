package com.coffeeshop.model;

public class InventoryItem {
    private String name;
    private double quantity;
    private String unit; // e.g., "ml", "g", "pcs"

    public InventoryItem(String name, double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isSufficient(double requiredQuantity) {
        return quantity >= requiredQuantity;
    }

    public void deduct(double amount) {
        this.quantity -= amount;
        if (this.quantity < 0) {
            this.quantity = 0;
        }
    }

    public void refill(double amount) {
        this.quantity += amount;
    }

    @Override
    public String toString() {
        return name + ": " + String.format("%.2f", quantity) + " " + unit;
    }
}
