package com.coffeeshop.model;

import java.util.HashMap;
import java.util.Map;

public class Product {
    private String id;
    private String name;
    private double price;
    private int stock;
    private Map<String, Double> recipe; // ingredient name -> quantity needed per serving

    public Product(String id, String name, double price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.recipe = new HashMap<>();
    }

    public Product(String id, String name, double price, int stock, Map<String, Double> recipe) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.recipe = recipe;
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

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public Map<String, Double> getRecipe() {
        return recipe;
    }

    public void setRecipe(Map<String, Double> recipe) {
        this.recipe = recipe;
    }

    public void addIngredient(String ingredientName, double quantity) {
        this.recipe.put(ingredientName, quantity);
    }

    public boolean isAvailable() {
        return stock > 0;
    }

    public boolean needsRefill() {
        return stock <= 5;
    }

    public int getRefillNeeded() {
        return Math.max(0, 20 - stock);
    }

    @Override
    public String toString() {
        return name + " - ₱" + String.format("%.2f", price) + " (Stock: " + stock + ")";
    }
}
