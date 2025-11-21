package com.coffeeshop.service;

import com.coffeeshop.model.Receipt;
import com.coffeeshop.model.ItemRecord;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class SalesAnalytics {
    // Compute total sales across all receipts
    public static double getTotalSales(List<Receipt> receipts) {
        return receipts.stream().mapToDouble(Receipt::getTotalAmount).sum();
    }

    // Compute total sales for today
    public static double getTotalSalesForDate(List<Receipt> receipts, LocalDate date) {
        return receipts.stream()
                .filter(r -> r.getReceiptTime().toLocalDate().equals(date))
                .mapToDouble(Receipt::getTotalAmount)
                .sum();
    }

    // Count orders for given date
    public static long getOrderCountForDate(List<Receipt> receipts, LocalDate date) {
        return receipts.stream()
                .filter(r -> r.getReceiptTime().toLocalDate().equals(date))
                .count();
    }

    // Top N products sold (by count) using ItemRecord list
    public static List<Map.Entry<String, Integer>> getTopProducts(List<ItemRecord> items, int n) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemRecord it : items) {
            counts.put(it.getItemName(), counts.getOrDefault(it.getItemName(), 0) + 1);
        }
        return counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(n)
                .collect(Collectors.toList());
    }
}
