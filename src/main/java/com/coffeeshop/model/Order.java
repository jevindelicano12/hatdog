package com.coffeeshop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private String orderId;
    private List<OrderItem> items;
    private LocalDateTime orderTime;
    private double totalAmount;
    private boolean isPaid;

    public Order(String orderId) {
        this.orderId = orderId;
        this.items = new ArrayList<>();
        this.orderTime = LocalDateTime.now();
        this.totalAmount = 0.0;
        this.isPaid = false;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
        calculateTotal();
    }

    public void removeItem(OrderItem item) {
        this.items.remove(item);
        calculateTotal();
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    private void calculateTotal() {
        totalAmount = items.stream()
                .mapToDouble(OrderItem::getSubtotal)
                .sum();
    }

    public String printReceipt() {
        StringBuilder receipt = new StringBuilder();
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("          COFFEE SHOP RECEIPT\n");
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("Order ID: ").append(orderId).append("\n");
        receipt.append("Date: ").append(orderTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        receipt.append("───────────────────────────────────────\n");
        
        for (OrderItem item : items) {
            receipt.append(item.getProduct().getName()).append(" x").append(item.getQuantity()).append("\n");
            receipt.append("  Temperature: ").append(item.getTemperature()).append("\n");
            receipt.append("  Sugar: ").append(item.getSugarLevel()).append("%\n");
            if (!item.getAddOns().isEmpty()) {
                receipt.append("  Add-ons: ").append(item.getAddOns()).append("\n");
            }
            receipt.append("  Subtotal: ₱").append(String.format("%.2f", item.getSubtotal())).append("\n");
            receipt.append("───────────────────────────────────────\n");
        }
        
        receipt.append("TOTAL: ₱").append(String.format("%.2f", totalAmount)).append("\n");
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("       Thank you for your order!\n");
        receipt.append("═══════════════════════════════════════\n");
        
        return receipt.toString();
    }

    @Override
    public String toString() {
        return "Order #" + orderId + " - " + items.size() + " items - ₱" + String.format("%.2f", totalAmount);
    }
}
