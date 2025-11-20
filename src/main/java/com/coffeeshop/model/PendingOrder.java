package com.coffeeshop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PendingOrder {
    private String orderId;
    private String customerName;
    private LocalDateTime orderTime;
    private List<OrderItemData> items;
    private double totalAmount;
    private String status; // "PENDING" or "COMPLETED"

    public PendingOrder(String orderId, String customerName) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.orderTime = LocalDateTime.now();
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
        this.status = "PENDING";
    }

    public void addItem(String productName, double price, int quantity, String temperature, int sugarLevel) {
        OrderItemData item = new OrderItemData(productName, price, quantity, temperature, sugarLevel);
        items.add(item);
        calculateTotal();
    }

    private void calculateTotal() {
        totalAmount = items.stream()
                .mapToDouble(item -> item.price * item.quantity)
                .sum();
    }

    public String toTextRecord() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append(orderId).append("|");
        sb.append(customerName).append("|");
        sb.append(orderTime.format(formatter)).append("|");
        sb.append(totalAmount).append("|");
        sb.append(status).append("|");
        
        // Encode items as JSON-like string
        sb.append("[");
        for (int i = 0; i < items.size(); i++) {
            OrderItemData item = items.get(i);
            sb.append(item.productName).append("~");
            sb.append(item.price).append("~");
            sb.append(item.quantity).append("~");
            sb.append(item.temperature).append("~");
            sb.append(item.sugarLevel);
            if (i < items.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        
        return sb.toString();
    }

    public static PendingOrder fromTextRecord(String line) {
        String[] parts = line.split("\\|");
        if (parts.length >= 6) {
            PendingOrder order = new PendingOrder(parts[0], parts[1]);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            order.orderTime = LocalDateTime.parse(parts[2], formatter);
            order.totalAmount = Double.parseDouble(parts[3]);
            order.status = parts[4];
            
            // Parse items
            String itemsStr = parts[5];
            if (itemsStr.startsWith("[") && itemsStr.endsWith("]")) {
                itemsStr = itemsStr.substring(1, itemsStr.length() - 1);
                if (!itemsStr.isEmpty()) {
                    String[] itemsArray = itemsStr.split(",");
                    for (String itemStr : itemsArray) {
                        String[] itemParts = itemStr.split("~");
                        if (itemParts.length >= 5) {
                            order.addItem(
                                itemParts[0], // productName
                                Double.parseDouble(itemParts[1]), // price
                                Integer.parseInt(itemParts[2]), // quantity
                                itemParts[3], // temperature
                                Integer.parseInt(itemParts[4]) // sugarLevel
                            );
                        }
                    }
                }
            }
            
            return order;
        }
        return null;
    }

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public List<OrderItemData> getItems() {
        return items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public void markCompleted() {
        this.status = "COMPLETED";
    }

    @Override
    public String toString() {
        return "Order #" + orderId + " - " + customerName + " - " + items.size() + " items - ₱" + 
               String.format("%.2f", totalAmount) + " - " + status;
    }

    // Inner class for order item data
    public static class OrderItemData {
        public String productName;
        public double price;
        public int quantity;
        public String temperature;
        public int sugarLevel;

        public OrderItemData(String productName, double price, int quantity, String temperature, int sugarLevel) {
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
            this.temperature = temperature;
            this.sugarLevel = sugarLevel;
        }

        public double getSubtotal() {
            return price * quantity;
        }
    }
}
