package com.coffeeshop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Receipt {
    private String receiptId;
    private String orderId;
    private String userName;
    private String cashierId;
    private LocalDateTime receiptTime;
    private double totalAmount;
    private double cashPaid;
    private double change;
    private String receiptContent;

    public Receipt(String receiptId, String orderId, String userName, 
                   double totalAmount, double cashPaid, double change) {
        this(receiptId, orderId, userName, null, totalAmount, cashPaid, change);
    }

    public Receipt(String receiptId, String orderId, String userName, String cashierId,
                   double totalAmount, double cashPaid, double change) {
        this.receiptId = receiptId;
        this.orderId = orderId;
        this.userName = userName;
        this.cashierId = cashierId;
        this.receiptTime = LocalDateTime.now();
        this.totalAmount = totalAmount;
        this.cashPaid = cashPaid;
        this.change = change;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCashierId() {
        return cashierId;
    }

    public void setCashierId(String cashierId) {
        this.cashierId = cashierId;
    }

    public LocalDateTime getReceiptTime() {
        return receiptTime;
    }

    public void setReceiptTime(LocalDateTime receiptTime) {
        this.receiptTime = receiptTime;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        this.cashPaid = cashPaid;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public String getReceiptContent() {
        return receiptContent;
    }

    public void setReceiptContent(String receiptContent) {
        this.receiptContent = receiptContent;
    }

    public String toTextRecord() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // Encode receiptContent to Base64 to preserve newlines and special characters
        String encodedContent = "";
        try {
            if (receiptContent != null && !receiptContent.isEmpty()) {
                encodedContent = java.util.Base64.getEncoder().encodeToString(receiptContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}

        return receiptId + "|" + orderId + "|" + userName + "|" + 
               (cashierId != null ? cashierId : "") + "|" + 
               receiptTime.format(formatter) + "|" + totalAmount + "|" + 
               cashPaid + "|" + change + "|" + encodedContent;
    }

    public static Receipt fromTextRecord(String line) {
        String[] parts = line.split("\\|");
        if (parts.length >= 9) {
            // New format with cashierId and encoded receiptContent
            Receipt receipt = new Receipt(
                parts[0], // receiptId
                parts[1], // orderId
                parts[2], // userName
                parts[3], // cashierId
                Double.parseDouble(parts[5]), // totalAmount
                Double.parseDouble(parts[6]), // cashPaid
                Double.parseDouble(parts[7])  // change
            );
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            receipt.setReceiptTime(LocalDateTime.parse(parts[4], formatter));
            // decode content
            try {
                String encoded = parts[8];
                if (encoded != null && !encoded.isEmpty()) {
                    byte[] decoded = java.util.Base64.getDecoder().decode(encoded);
                    receipt.setReceiptContent(new String(decoded, java.nio.charset.StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
            return receipt;
        } else if (parts.length >= 7) {
            // Old format without cashierId (backward compatibility)
            Receipt receipt = new Receipt(
                parts[0], // receiptId
                parts[1], // orderId
                parts[2], // userName
                Double.parseDouble(parts[4]), // totalAmount
                Double.parseDouble(parts[5]), // cashPaid
                Double.parseDouble(parts[6])  // change
            );
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            receipt.setReceiptTime(LocalDateTime.parse(parts[3], formatter));
            return receipt;
        }
        return null;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "Receipt #" + receiptId + " | Order #" + orderId + " | " + 
               userName + " | " + receiptTime.format(formatter) + " | ₱" + 
               String.format("%.2f", totalAmount);
    }
}
