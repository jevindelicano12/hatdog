package com.coffeeshop.model;

import java.util.HashMap;
import java.util.Map;

public class Product {
    private String id;
    private String name;
    private double price;
    private Map<String, Double> recipe; // ingredient name -> quantity needed per serving
    private String category;

    public Product(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.recipe = new HashMap<>();
        this.category = "Uncategorized";
    }

    public Product(String id, String name, double price, Map<String, Double> recipe) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.recipe = recipe;
        this.category = "Uncategorized";
    }

    public Product(String id, String name, double price, Map<String, Double> recipe, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.recipe = recipe != null ? recipe : new HashMap<>();
        this.category = category != null ? category : "Uncategorized";
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

    public Map<String, Double> getRecipe() {
        return recipe;
    }

    public void setRecipe(Map<String, Double> recipe) {
        this.recipe = recipe;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void addIngredient(String ingredientName, double quantity) {
        this.recipe.put(ingredientName, quantity);
    }

    @Override
    public String toString() {
        return name + " - ₱" + String.format("%.2f", price);
    }
}
