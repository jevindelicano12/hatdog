package com.coffeeshop.model;

public class OrderItem {
    private Product product;
    private int quantity;
    private String temperature; // "Hot" or "Cold"
    private int sugarLevel; // 0, 25, 50, 75, 100
    private String addOns;
    private double addOnsCost;

    public OrderItem(Product product, int quantity, String temperature, int sugarLevel) {
        this.product = product;
        this.quantity = quantity;
        this.temperature = temperature;
        this.sugarLevel = sugarLevel;
        this.addOns = "";
        this.addOnsCost = 0.0;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public int getSugarLevel() {
        return sugarLevel;
    }

    public void setSugarLevel(int sugarLevel) {
        this.sugarLevel = sugarLevel;
    }

    public String getAddOns() {
        return addOns;
    }

    public void setAddOns(String addOns) {
        this.addOns = addOns;
    }

    public double getAddOnsCost() {
        return addOnsCost;
    }

    public void setAddOnsCost(double addOnsCost) {
        this.addOnsCost = addOnsCost;
    }

    public double getSubtotal() {
        return (product.getPrice() + addOnsCost) * quantity;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(product.getName())
          .append(" x").append(quantity)
          .append(" (").append(temperature)
          .append(", ").append(sugarLevel).append("% sugar");
        if (!addOns.isEmpty()) {
            sb.append(", +").append(addOns);
        }
        sb.append(")");
        return sb.toString();
    }
}
