package com.coffeeshop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReturnTransaction {
    private String returnId;
    private String originalReceiptId;
    private String cashierId;
    private LocalDateTime returnTime;
    private List<ReturnItem> returnedItems;
    private List<OrderItem> exchangeItems;
    private double originalTotal;
    private double returnCredit;
    private double exchangeTotal;
    private double refundAmount; // positive = refund to customer, negative = collect from customer
    private String notes;
    private double amountReceived; // amount customer gave when paying additional
    private double changeAmount; // change returned to customer
    private String paymentMethod; // e.g., CASH

    public ReturnTransaction(String returnId, String originalReceiptId, String cashierId) {
        this.returnId = returnId;
        this.originalReceiptId = originalReceiptId;
        this.cashierId = cashierId;
        this.returnTime = LocalDateTime.now();
        this.returnedItems = new ArrayList<>();
        this.exchangeItems = new ArrayList<>();
        this.originalTotal = 0.0;
        this.returnCredit = 0.0;
        this.exchangeTotal = 0.0;
        this.refundAmount = 0.0;
        this.notes = "";
        this.amountReceived = 0.0;
        this.changeAmount = 0.0;
        this.paymentMethod = "";
    }

    public String getReturnId() {
        return returnId;
    }

    public void setReturnId(String returnId) {
        this.returnId = returnId;
    }

    public String getOriginalReceiptId() {
        return originalReceiptId;
    }

    public void setOriginalReceiptId(String originalReceiptId) {
        this.originalReceiptId = originalReceiptId;
    }

    public String getCashierId() {
        return cashierId;
    }

    public void setCashierId(String cashierId) {
        this.cashierId = cashierId;
    }

    public LocalDateTime getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(LocalDateTime returnTime) {
        this.returnTime = returnTime;
    }

    public List<ReturnItem> getReturnedItems() {
        return returnedItems;
    }

    public void addReturnedItem(ReturnItem item) {
        this.returnedItems.add(item);
    }

    public List<OrderItem> getExchangeItems() {
        return exchangeItems;
    }

    public void addExchangeItem(OrderItem item) {
        this.exchangeItems.add(item);
    }

    public double getOriginalTotal() {
        return originalTotal;
    }

    public void setOriginalTotal(double originalTotal) {
        this.originalTotal = originalTotal;
    }

    public double getReturnCredit() {
        return returnCredit;
    }

    public void setReturnCredit(double returnCredit) {
        this.returnCredit = returnCredit;
    }

    public double getExchangeTotal() {
        return exchangeTotal;
    }

    public void setExchangeTotal(double exchangeTotal) {
        this.exchangeTotal = exchangeTotal;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(double amountReceived) {
        this.amountReceived = amountReceived;
    }

    public double getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(double changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void calculateTotals() {
        returnCredit = returnedItems.stream()
            .mapToDouble(item -> item.getItemSubtotal())
            .sum();
        
        exchangeTotal = exchangeItems.stream()
            .mapToDouble(OrderItem::getSubtotal)
            .sum();
        
        refundAmount = returnCredit - exchangeTotal;
    }

    public String toTextRecord() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        
        // Basic info
        sb.append(returnId).append("|");
        sb.append(originalReceiptId).append("|");
        sb.append(cashierId).append("|");
        sb.append(returnTime.format(formatter)).append("|");
        sb.append(originalTotal).append("|");
        sb.append(returnCredit).append("|");
        sb.append(exchangeTotal).append("|");
        sb.append(refundAmount).append("|");
        
        // Returned items (encoded)
        StringBuilder returnedItemsStr = new StringBuilder();
        for (int i = 0; i < returnedItems.size(); i++) {
            ReturnItem item = returnedItems.get(i);
            if (i > 0) returnedItemsStr.append(";;");
            returnedItemsStr.append(item.getProductName()).append("::")
                .append(item.getQuantity()).append("::")
                .append(item.getReason()).append("::")
                .append(item.getItemSubtotal());
        }
        sb.append(java.util.Base64.getEncoder().encodeToString(
            returnedItemsStr.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))).append("|");
        
        // Exchange items count
        sb.append(exchangeItems.size()).append("|");

        // Payment info
        sb.append(amountReceived).append("|");
        sb.append(changeAmount).append("|");
        sb.append(paymentMethod != null ? paymentMethod : "").append("|");
        
        // Notes
        sb.append(java.util.Base64.getEncoder().encodeToString(
            notes.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        
        return sb.toString();
    }

    public static ReturnTransaction fromTextRecord(String line) {
        String[] parts = line.split("\\|");
        if (parts.length < 11) return null;

        ReturnTransaction rt = new ReturnTransaction(parts[0], parts[1], parts[2]);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        rt.setReturnTime(LocalDateTime.parse(parts[3], formatter));
        rt.setOriginalTotal(Double.parseDouble(parts[4]));
        rt.setReturnCredit(Double.parseDouble(parts[5]));
        rt.setExchangeTotal(Double.parseDouble(parts[6]));
        rt.setRefundAmount(Double.parseDouble(parts[7]));
        
        // Decode returned items
        try {
            String returnedItemsStr = new String(
                java.util.Base64.getDecoder().decode(parts[8]),
                java.nio.charset.StandardCharsets.UTF_8
            );
            if (!returnedItemsStr.isEmpty()) {
                String[] items = returnedItemsStr.split(";;");
                for (String itemStr : items) {
                    String[] itemParts = itemStr.split("::");
                    if (itemParts.length >= 4) {
                        ReturnItem item = new ReturnItem(
                            itemParts[0],
                            Integer.parseInt(itemParts[1]),
                            itemParts[2],
                            Double.parseDouble(itemParts[3])
                        );
                        rt.addReturnedItem(item);
                    }
                }
            }
        } catch (Exception ignored) {}
        
        // Additional fields: amountReceived, changeAmount, paymentMethod and notes
        try {
            if (parts.length >= 14) {
                try { rt.setAmountReceived(Double.parseDouble(parts[10])); } catch (Exception ex) { rt.setAmountReceived(0.0); }
                try { rt.setChangeAmount(Double.parseDouble(parts[11])); } catch (Exception ex) { rt.setChangeAmount(0.0); }
                rt.setPaymentMethod(parts[12] != null ? parts[12] : "");
                rt.setNotes(new String(
                    java.util.Base64.getDecoder().decode(parts[13]),
                    java.nio.charset.StandardCharsets.UTF_8
                ));
            } else if (parts.length >= 11) {
                // legacy format: notes at index 10
                rt.setNotes(new String(
                    java.util.Base64.getDecoder().decode(parts[10]),
                    java.nio.charset.StandardCharsets.UTF_8
                ));
            }
        } catch (Exception ignored) {}
        
        return rt;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "Return #" + returnId + " | Receipt #" + originalReceiptId + 
               " | " + returnTime.format(formatter) + " | Refund: ₱" + 
               String.format("%.2f", refundAmount);
    }

    public static class ReturnItem {
        private String productName;
        private int quantity;
        private String reason;
        private double itemSubtotal;

        public ReturnItem(String productName, int quantity, String reason, double itemSubtotal) {
            this.productName = productName;
            this.quantity = quantity;
            this.reason = reason;
            this.itemSubtotal = itemSubtotal;
        }

        public String getProductName() {
            return productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getReason() {
            return reason;
        }

        public double getItemSubtotal() {
            return itemSubtotal;
        }
    }
}
