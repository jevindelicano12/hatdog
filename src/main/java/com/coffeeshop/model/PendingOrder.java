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
    private String status; // STATUS_* constants below
    private String orderType; // "Dine In" or "Take Away"

    public static final String STATUS_PENDING = "PENDING"; // created but not paid
    public static final String STATUS_PAID = "PAID"; // paid and waiting to be prepared
    public static final String STATUS_PREPARING = "PREPARING"; // being prepared
    public static final String STATUS_COMPLETED = "COMPLETED"; // picked up / finished

    public PendingOrder(String orderId, String customerName) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.orderTime = LocalDateTime.now();
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
        this.status = STATUS_PENDING;
        this.orderType = "Dine In"; // default
    }

    public PendingOrder(String orderId, String customerName, String orderType) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.orderTime = LocalDateTime.now();
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
        this.status = STATUS_PENDING;
        this.orderType = orderType;
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
        sb.append(orderType != null ? orderType : "Dine In").append("|");
        
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
        // Robust parsing: split metadata from items by locating the first '['
        int itemsStart = line.indexOf('[');
        String meta = line;
        String itemsStr = "";
        if (itemsStart >= 0) {
            meta = line.substring(0, itemsStart);
            itemsStr = line.substring(itemsStart);
        }

        String[] parts = meta.split("\\|");
        if (parts.length >= 5) {
            // Expected meta: orderId|customerName|orderTime|totalAmount|status|[orderType]
            String orderType = parts.length >= 6 ? parts[5] : "Dine In";
            PendingOrder order = new PendingOrder(parts[0], parts[1], orderType);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            try {
                order.orderTime = LocalDateTime.parse(parts[2], formatter);
            } catch (Exception ex) {
                // keep current time if parse fails
            }

            try {
                order.totalAmount = Double.parseDouble(parts[3]);
            } catch (Exception ex) {
                order.totalAmount = 0.0;
            }

            order.status = parts[4];

            if (itemsStr.startsWith("[") && itemsStr.endsWith("]")) {
                String inner = itemsStr.substring(1, itemsStr.length() - 1);
                if (!inner.isEmpty()) {
                    String[] itemsArray = inner.split(",");
                    for (String itemStr : itemsArray) {
                        String[] itemParts = itemStr.split("~");
                        if (itemParts.length >= 5) {
                            try {
                                order.addItem(
                                    itemParts[0], // productName
                                    Double.parseDouble(itemParts[1]), // price
                                    Integer.parseInt(itemParts[2]), // quantity
                                    itemParts[3], // temperature
                                    Integer.parseInt(itemParts[4]) // sugarLevel
                                );
                            } catch (Exception ignore) {
                                // skip malformed item
                            }
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

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    // paymentMethod removed in revert

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isActive() {
        return !STATUS_COMPLETED.equals(status);
    }

    public void markCompleted() {
        this.status = STATUS_COMPLETED;
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
