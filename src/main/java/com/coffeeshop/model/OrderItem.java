package com.coffeeshop.model;

public class OrderItem {
    private Product product;
    private int quantity;
    private String temperature; // "Hot" or "Cold"
    private int sugarLevel; // 0, 25, 50, 75, 100
    private String addOns;
    private double addOnsCost;
    private String specialRequest; // Custom notes/requests
    private String size; // "Small", "Medium", "Large"
    private double sizeCost; // Cost surcharge for size

    public OrderItem(Product product, int quantity, String temperature, int sugarLevel) {
        this.product = product;
        this.quantity = quantity;
        this.temperature = temperature;
        this.sugarLevel = sugarLevel;
        this.addOns = "";
        this.addOnsCost = 0.0;
        this.specialRequest = "";
        this.size = "Regular"; // Default size
        this.sizeCost = 0.0; // Default to 0 - will be set properly by handleAddToCart
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

    public String getSpecialRequest() {
        return specialRequest;
    }

    public void setSpecialRequest(String specialRequest) {
        this.specialRequest = specialRequest;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public double getSizeCost() {
        return sizeCost;
    }

    public void setSizeCost(double sizeCost) {
        this.sizeCost = sizeCost;
    }

    public double getSubtotal() {
        // For beverages, sizeCost IS the full price (not a surcharge)
        // For pastries, product.getPrice() is the base and sizeCost is additional
        boolean isPastry = false;
        if (product.getCategory() != null) {
            String cat = product.getCategory().toLowerCase();
            isPastry = cat.contains("pastr") || cat.contains("bakery") || cat.contains("snack") || cat.contains("pastry");
        }
        double basePrice = isPastry ? product.getPrice() : 0.0;
        return (basePrice + sizeCost + addOnsCost) * quantity;
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
        if (specialRequest != null && !specialRequest.isEmpty()) {
            sb.append(", Note: ").append(specialRequest);
        }
        sb.append(")");
        return sb.toString();
    }
}
