package com.coffeeshop.model;

public class ItemRecord {
    private String itemName;
    private double price;
    private String typeOfDrink;
    private String size;
    private int sugarLevel;

    public ItemRecord(String itemName, double price, String typeOfDrink, String size, int sugarLevel) {
        this.itemName = itemName;
        this.price = price;
        this.typeOfDrink = typeOfDrink;
        this.size = size;
        this.sugarLevel = sugarLevel;
    }

    // Constructor for parsing from text
    public ItemRecord(String line) {
        String[] parts = line.split("\\|");
        if (parts.length >= 5) {
            this.itemName = parts[0].trim();
            this.price = Double.parseDouble(parts[1].trim());
            this.typeOfDrink = parts[2].trim();
            this.size = parts[3].trim();
            this.sugarLevel = Integer.parseInt(parts[4].trim());
        }
    }

    public String toTextRecord() {
        return itemName + "|" + price + "|" + typeOfDrink + "|" + size + "|" + sugarLevel;
    }

    // Getters
    public String getItemName() { return itemName; }
    public double getPrice() { return price; }
    public String getTypeOfDrink() { return typeOfDrink; }
    public String getSize() { return size; }
    public int getSugarLevel() { return sugarLevel; }

    // Setters
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setPrice(double price) { this.price = price; }
    public void setTypeOfDrink(String typeOfDrink) { this.typeOfDrink = typeOfDrink; }
    public void setSize(String size) { this.size = size; }
    public void setSugarLevel(int sugarLevel) { this.sugarLevel = sugarLevel; }

    @Override
    public String toString() {
        return "ItemRecord{" +
                "itemName='" + itemName + '\'' +
                ", price=" + price +
                ", typeOfDrink='" + typeOfDrink + '\'' +
                ", size='" + size + '\'' +
                ", sugarLevel=" + sugarLevel +
                '}';
    }
}
