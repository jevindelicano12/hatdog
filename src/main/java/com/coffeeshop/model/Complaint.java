package com.coffeeshop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Complaint {
    private String id;
    private String orderId;
    private String customerName;
    private String issueType;
    private String description;
    private String cashierId;
    private LocalDateTime createdAt;
    private String status; // e.g., OPEN, RESOLVED

    public Complaint(String id, String orderId, String customerName, String issueType, String description, String cashierId) {
        this.id = id;
        this.orderId = orderId;
        this.customerName = customerName;
        this.issueType = issueType;
        this.description = description;
        this.cashierId = cashierId;
        this.createdAt = LocalDateTime.now();
        this.status = "OPEN";
    }

    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getIssueType() { return issueType; }
    public String getDescription() { return description; }
    public String getCashierId() { return cashierId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String toTextRecord() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append(id).append("|")
          .append(orderId != null ? orderId : "").append("|")
          .append(customerName != null ? java.util.Base64.getEncoder().encodeToString(customerName.getBytes(java.nio.charset.StandardCharsets.UTF_8)) : "").append("|")
          .append(issueType != null ? issueType : "").append("|")
          .append(java.util.Base64.getEncoder().encodeToString((description != null ? description : "").getBytes(java.nio.charset.StandardCharsets.UTF_8))).append("|")
          .append(cashierId != null ? cashierId : "").append("|")
          .append(createdAt.format(f)).append("|")
          .append(status != null ? status : "OPEN");
        return sb.toString();
    }

    public static Complaint fromTextRecord(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length < 8) return null;
            Complaint c = new Complaint(parts[0], parts[1], "", parts[3], "", parts[5]);
            try {
                String decodedName = parts[2] != null && !parts[2].isEmpty() ? new String(java.util.Base64.getDecoder().decode(parts[2]), java.nio.charset.StandardCharsets.UTF_8) : "";
                String decodedDesc = parts[4] != null && !parts[4].isEmpty() ? new String(java.util.Base64.getDecoder().decode(parts[4]), java.nio.charset.StandardCharsets.UTF_8) : "";
                c.customerName = decodedName;
                c.description = decodedDesc;
            } catch (Exception ignored) {}
            try {
                DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                c.createdAt = LocalDateTime.parse(parts[6], f);
            } catch (Exception ignored) {}
            c.status = parts.length >= 8 ? parts[7] : "OPEN";
            return c;
        } catch (Exception ex) { return null; }
    }
}
