package com.coffeeshop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CashTransaction {
    public static final String TYPE_SALE = "SALE";           // Order completed
    public static final String TYPE_REFUND = "REFUND";       // Order returned
    public static final String TYPE_DISCOUNT = "DISCOUNT";   // Discount applied
    public static final String TYPE_OPENING = "OPENING";     // Opening cash
    public static final String TYPE_CLOSING = "CLOSING";     // Closing cash (remittance)
    public static final String TYPE_ADJUSTMENT = "ADJUSTMENT"; // Cash adjustment/correction

    private String transactionId;
    private String cashierId;
    private LocalDateTime transactionTime;
    private String type;              // TYPE_* constants above
    private double amount;            // Positive for income, negative for expenses
    private String reference;         // Order ID, receipt ID, or description
    private String notes;             // Additional notes/reason
    private String shiftId;           // Optional: shift identifier

    public CashTransaction(String transactionId, String cashierId, String type, double amount, String reference) {
        this.transactionId = transactionId;
        this.cashierId = cashierId;
        this.transactionTime = LocalDateTime.now();
        this.type = type;
        this.amount = amount;
        this.reference = reference;
        this.notes = "";
        this.shiftId = "";
    }

    public CashTransaction(String transactionId, String cashierId, String type, double amount, String reference, String notes) {
        this.transactionId = transactionId;
        this.cashierId = cashierId;
        this.transactionTime = LocalDateTime.now();
        this.type = type;
        this.amount = amount;
        this.reference = reference;
        this.notes = notes;
        this.shiftId = "";
    }

    public CashTransaction(String transactionId, String cashierId, LocalDateTime transactionTime, String type, double amount, String reference, String notes, String shiftId) {
        this.transactionId = transactionId;
        this.cashierId = cashierId;
        this.transactionTime = transactionTime;
        this.type = type;
        this.amount = amount;
        this.reference = reference;
        this.notes = notes != null ? notes : "";
        this.shiftId = shiftId != null ? shiftId : "";
    }

    public String toTextRecord() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return transactionId + "|" +
               cashierId + "|" +
               transactionTime.format(formatter) + "|" +
               type + "|" +
               amount + "|" +
               (reference != null ? reference.replace("|", "\\|") : "") + "|" +
               (notes != null ? notes.replace("|", "\\|") : "") + "|" +
               (shiftId != null ? shiftId : "");
    }

    public static CashTransaction fromTextRecord(String line) {
        String[] parts = line.split("\\|(?!\\\\)"); // Split by | but not by escaped \|
        if (parts.length >= 6) {
            String reference = parts[5].replace("\\|", "|");
            String notes = parts.length > 6 ? parts[6].replace("\\|", "|") : "";
            String shiftId = parts.length > 7 ? parts[7] : "";

            CashTransaction tx = new CashTransaction(
                parts[0],                              // transactionId
                parts[1],                              // cashierId
                parts[3],                              // type
                Double.parseDouble(parts[4]),          // amount
                reference                              // reference
            );
            tx.notes = notes;
            tx.shiftId = shiftId;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            try {
                tx.transactionTime = LocalDateTime.parse(parts[2], formatter);
            } catch (Exception ignored) {}

            return tx;
        }
        return null;
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public String getCashierId() {
        return cashierId;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getReference() {
        return reference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes : "";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId != null ? shiftId : "";
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "TX #" + transactionId + " | " + cashierId + " | " + type + 
               " | ₱" + String.format("%.2f", amount) + " | " + transactionTime.format(formatter);
    }
}
