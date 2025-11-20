package com.coffeeshop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OrderRecord {
    private String orderId;
    private String userName;
    private LocalDateTime orderTime;
    private String itemName;
    private String size;
    private String typeOfDrink;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OrderRecord(String orderId, String userName, LocalDateTime orderTime, 
                      String itemName, String size, String typeOfDrink) {
        this.orderId = orderId;
        this.userName = userName;
        this.orderTime = orderTime;
        this.itemName = itemName;
        this.size = size;
        this.typeOfDrink = typeOfDrink;
    }

    // Constructor for parsing from text
    public OrderRecord(String line) {
        String[] parts = line.split("\\|");
        if (parts.length >= 6) {
            this.orderId = parts[0].trim();
            this.userName = parts[1].trim();
            this.orderTime = LocalDateTime.parse(parts[2].trim(), formatter);
            this.itemName = parts[3].trim();
            this.size = parts[4].trim();
            this.typeOfDrink = parts[5].trim();
        }
    }

    public String toTextRecord() {
        return orderId + "|" + userName + "|" + orderTime.format(formatter) + 
               "|" + itemName + "|" + size + "|" + typeOfDrink;
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getUserName() { return userName; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public String getItemName() { return itemName; }
    public String getSize() { return size; }
    public String getTypeOfDrink() { return typeOfDrink; }

    // Setters
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setSize(String size) { this.size = size; }
    public void setTypeOfDrink(String typeOfDrink) { this.typeOfDrink = typeOfDrink; }

    @Override
    public String toString() {
        return "OrderRecord{" +
                "orderId='" + orderId + '\'' +
                ", userName='" + userName + '\'' +
                ", orderTime=" + orderTime +
                ", itemName='" + itemName + '\'' +
                ", size='" + size + '\'' +
                ", typeOfDrink='" + typeOfDrink + '\'' +
                '}';
    }
}
