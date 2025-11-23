package com.coffeeshop;

import com.coffeeshop.model.*;
import com.coffeeshop.service.Store;
import com.coffeeshop.service.TextDatabase;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.coffeeshop.service.SalesAnalytics;

public class CashierApp extends Application {
    private Store store;
    private TextField customerNameInput;
    
    // Order queue management
    private ObservableList<Order> orderQueue = FXCollections.observableArrayList();
    private TableView<Order> orderQueueTable;
    private java.util.HashMap<String, String> orderCustomerNames = new java.util.HashMap<>();
    private java.util.HashMap<String, String> orderTypes = new java.util.HashMap<>(); // orderId -> orderType
    
    // Receipt management
    private ObservableList<Receipt> receiptHistory = FXCollections.observableArrayList();
    private TableView<Receipt> receiptHistoryTable;
    private TextArea receiptDetailArea;
    private VBox dashboardPanel; // cached dashboard panel

    private ScheduledExecutorService scheduler;

    @Override
    public void start(Stage primaryStage) {
        store = Store.getInstance();
        loadReceiptHistory();
        loadPendingOrdersFromFile();

        primaryStage.setTitle("Coffee Shop - Cashier Terminal");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F4F1EA;"); // Modern Cream Background

        // Header
        VBox header = createHeader();
        root.setTop(header);

        // Tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");
        
        // Tab 1: Order Queue
        Tab ordersTab = new Tab("📋 Order Queue");
        ordersTab.setContent(createOrderQueuePanel());
        
        // Tab 2: Receipt History
        Tab receiptHistoryTab = new Tab("🧾 Receipt History");
        receiptHistoryTab.setContent(createReceiptHistoryPanel());
        
        // Tab 3: Reports / Dashboard
        Tab reportsTab = new Tab("📊 Dashboard");
        reportsTab.setContent(createDashboardPanel());

        tabPane.getTabs().addAll(ordersTab, receiptHistoryTab, reportsTab);
        
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 800);
        // Add global stylesheet for TabPane if needed, or inline styles
        // For now, we rely on inline styles for components
        
        setScenePreserveWindowSize(scene);

        // Start background scheduler to refresh data for near real-time sync
        startBackgroundSync();
    }

    // Utility to preserve window size/maximized state when switching scenes
    private void setScenePreserveWindowSize(Scene scene) {
        Stage primaryStage = (Stage) javafx.stage.Window.getWindows().filtered(w -> w.isShowing()).get(0);
        boolean wasMax = primaryStage.isMaximized();
        double prevW = primaryStage.getWidth();
        double prevH = primaryStage.getHeight();
        primaryStage.setScene(scene);
        if (wasMax) primaryStage.setMaximized(true);
        else {
            if (prevW > 0 && prevH > 0) {
                primaryStage.setWidth(prevW);
                primaryStage.setHeight(prevH);
            }
        }
        primaryStage.show();
    }

    private VBox createHeader() {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("Cashier Terminal");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#3E2723"));

        Label subtitle = new Label("Process customer orders and payments");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#795548"));

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    // ====================  DATABASE HELPERS ====================
    
    private void saveOrderToDatabase(Order order, String customerName) {
        try {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            
            // Save each item in the order as a separate order record
            for (OrderItem item : order.getItems()) {
                // Save to items database
                ItemRecord itemRecord = new ItemRecord(
                    item.getProduct().getName(),
                    item.getProduct().getPrice(),
                    item.getTemperature(), // type of drink (Hot/Cold)
                    "Regular", // size (you can customize this)
                    item.getSugarLevel()
                );
                TextDatabase.saveItem(itemRecord);
                
                // Save to orders database
                OrderRecord orderRecord = new OrderRecord(
                    order.getOrderId(),
                    customerName, // customer name from input
                    now,
                    item.getProduct().getName(),
                    "Regular", // size
                    item.getTemperature() // type of drink
                );
                TextDatabase.saveOrder(orderRecord);
            }
        } catch (Exception e) {
            System.err.println("Error saving order to database: " + e.getMessage());
        }
    }

    private String generateReceiptWithOrderType(Order order, String customerName, String orderType) {
        StringBuilder receipt = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("         BREWISE COFFEE SHOP\n");
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("Order ID: ").append(order.getOrderId()).append("\n");
        receipt.append("Customer: ").append(customerName != null ? customerName : "Walk-in").append("\n");
        receipt.append("Order Type: ").append(orderType).append("\n");
        receipt.append("Date: ").append(java.time.LocalDateTime.now().format(formatter)).append("\n");
        receipt.append("═══════════════════════════════════════\n\n");
        
        receipt.append("ITEMS:\n");
        receipt.append("───────────────────────────────────────\n");
        
        for (OrderItem item : order.getItems()) {
            receipt.append(String.format("%-20s  ₱%.2f\n", 
                item.getProduct().getName(), item.getProduct().getPrice()));
            
            if (item.getTemperature() != null && !item.getTemperature().isEmpty()) {
                receipt.append("  Temperature: ").append(item.getTemperature()).append("\n");
            }
            
            if (item.getSugarLevel() > 0) {
                receipt.append("  Sugar Level: ").append(item.getSugarLevel()).append("%\n");
            }
            
            if (item.getAddOns() != null && !item.getAddOns().isEmpty()) {
                receipt.append("  Add-ons: ").append(String.join(", ", item.getAddOns())).append("\n");
            }
            receipt.append("\n");
        }
        
        receipt.append("───────────────────────────────────────\n");
        receipt.append(String.format("Subtotal:         ₱%.2f\n", order.getTotalAmount()));
        receipt.append(String.format("TOTAL:            ₱%.2f\n", order.getTotalAmount()));
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("       Thank you for your order!\n");
        receipt.append("═══════════════════════════════════════\n");
        
        return receipt.toString();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void startBackgroundSync() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        // every 8 seconds reload pending orders and receipts and refresh dashboard
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Perform file I/O off the FX thread
                List<Receipt> receipts = TextDatabase.loadAllReceipts();
                List<PendingOrder> pendingOrders = TextDatabase.loadPendingOrders();

                // Prepare converted orders and maps
                java.util.List<Order> newOrders = new java.util.ArrayList<>();
                java.util.Map<String, String> newCustomerNames = new java.util.HashMap<>();
                java.util.Map<String, String> newOrderTypes = new java.util.HashMap<>();

                for (PendingOrder pendingOrder : pendingOrders) {
                    Order order = new Order(pendingOrder.getOrderId());
                    newCustomerNames.put(pendingOrder.getOrderId(), pendingOrder.getCustomerName());
                    newOrderTypes.put(pendingOrder.getOrderId(), pendingOrder.getOrderType());

                    for (PendingOrder.OrderItemData itemData : pendingOrder.getItems()) {
                        Product product = store.getProducts().stream()
                                .filter(p -> p.getName().equals(itemData.productName))
                                .findFirst()
                                .orElse(null);
                        if (product != null) {
                            OrderItem orderItem = new OrderItem(
                                    product,
                                    itemData.quantity,
                                    itemData.temperature,
                                    itemData.sugarLevel
                            );
                            order.addItem(orderItem);
                        }
                    }
                    newOrders.add(order);
                }

                // Update UI data on FX thread
                javafx.application.Platform.runLater(() -> {
                    try {
                        receiptHistory.setAll(receipts);

                        orderQueue.setAll(newOrders);

                        orderCustomerNames.clear();
                        orderCustomerNames.putAll(newCustomerNames);

                        orderTypes.clear();
                        orderTypes.putAll(newOrderTypes);

                        // If dashboard exists, trigger its refresh by firing the refresh button logic
                        if (dashboardPanel != null) {
                            // rebuild dashboard by recreating panel content
                            // simplest approach: replace children with a fresh dashboard
                            VBox parent = dashboardPanel;
                            parent.getChildren().clear();
                            parent.getChildren().addAll(createDashboardPanel().getChildren());
                        }
                    } catch (Exception ex) {
                        System.err.println("Error applying background sync to UI: " + ex.getMessage());
                    }
                });
            } catch (Exception e) {
                System.err.println("Background sync error: " + e.getMessage());
            }
        }, 8, 8, TimeUnit.SECONDS);
    }

    private void stopBackgroundSync() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
    
    private void loadReceiptHistory() {
        receiptHistory.clear();
        List<Receipt> receipts = TextDatabase.loadAllReceipts();
        receiptHistory.addAll(receipts);
    }
    
    private void loadPendingOrdersFromFile() {
        List<PendingOrder> pendingOrders = TextDatabase.loadPendingOrders();
        
        for (PendingOrder pendingOrder : pendingOrders) {
            // Convert PendingOrder to Order
            Order order = new Order(pendingOrder.getOrderId());
            
            // Store customer name and order type for this order
            orderCustomerNames.put(pendingOrder.getOrderId(), pendingOrder.getCustomerName());
            orderTypes.put(pendingOrder.getOrderId(), pendingOrder.getOrderType());
            
            for (PendingOrder.OrderItemData itemData : pendingOrder.getItems()) {
                // Find the product in store
                Product product = store.getProducts().stream()
                    .filter(p -> p.getName().equals(itemData.productName))
                    .findFirst()
                    .orElse(null);
                
                if (product != null) {
                    OrderItem orderItem = new OrderItem(
                        product,
                        itemData.quantity,
                        itemData.temperature,
                        itemData.sugarLevel
                    );
                    order.addItem(orderItem);
                }
            }
            
            orderQueue.add(order);
        }
    }

    private VBox createDashboardPanel() {
        dashboardPanel = new VBox(20);
        dashboardPanel.setPadding(new Insets(30));
        dashboardPanel.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        Label title = new Label("Sales Dashboard");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#3E2723"));

        Label totalSalesLabel = new Label();
        totalSalesLabel.setFont(Font.font("Segoe UI", 18));
        totalSalesLabel.setTextFill(Color.web("#4CAF50"));

        Label ordersTodayLabel = new Label();
        ordersTodayLabel.setFont(Font.font("Segoe UI", 16));
        ordersTodayLabel.setTextFill(Color.web("#1976D2"));

        Label topProductsLabel = new Label("Top Products");
        topProductsLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        topProductsLabel.setTextFill(Color.web("#3E2723"));
        
        ListView<String> topList = new ListView<>();
        topList.setPrefHeight(220);
        topList.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-radius: 8;");

        Button refreshBtn = new Button("Refresh Data");
        refreshBtn.setStyle("-fx-background-color: #6F4E37; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> {
            // refresh data
            loadReceiptHistory();
            List<com.coffeeshop.model.ItemRecord> items = com.coffeeshop.service.TextDatabase.loadAllItems();
            List<Map.Entry<String,Integer>> top = com.coffeeshop.service.SalesAnalytics.getTopProducts(items, 5);
            topList.getItems().clear();
            for (Map.Entry<String,Integer> en : top) {
                topList.getItems().add(String.format("%s — %d sold", en.getKey(), en.getValue()));
            }

            double total = com.coffeeshop.service.SalesAnalytics.getTotalSales(receiptHistory);
            double today = com.coffeeshop.service.SalesAnalytics.getTotalSalesForDate(receiptHistory, LocalDate.now());
            long ordersToday = com.coffeeshop.service.SalesAnalytics.getOrderCountForDate(receiptHistory, LocalDate.now());

            totalSalesLabel.setText(String.format("Total Sales (All time): ₱%.2f  |  Today: ₱%.2f", total, today));
            ordersTodayLabel.setText("Orders Today: " + ordersToday);
        });

        // initial populate
        refreshBtn.fire();

        dashboardPanel.getChildren().addAll(title, new Separator(), totalSalesLabel, ordersTodayLabel, new Separator(), topProductsLabel, topList, refreshBtn);
        return dashboardPanel;
    }
    
    // ==================== ORDER QUEUE PANEL ====================
    
    private VBox createOrderQueuePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        
        Label title = new Label("Order Queue");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#3E2723"));
        
        Label subtitle = new Label("Manage incoming customer orders");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#795548"));
        
        // Search section
        HBox searchBox = new HBox(15);
        searchBox.setPadding(new Insets(20));
        searchBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        
        Label searchLabel = new Label("Search:");
        searchLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        customerNameInput = new TextField();
        customerNameInput.setPromptText("Customer Name...");
        customerNameInput.setPrefWidth(300);
        customerNameInput.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-radius: 6; -fx-padding: 8;");
        
        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        searchBtn.setOnAction(e -> searchOrders());
        
        Button clearBtn = new Button("Show All");
        clearBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> showAllOrders());
        
        searchBox.getChildren().addAll(searchLabel, customerNameInput, searchBtn, clearBtn);
        
        // Orders table
        orderQueueTable = new TableView<>();
        orderQueueTable.setItems(orderQueue);
        orderQueueTable.setPrefHeight(500);
        orderQueueTable.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 8;");
        
        TableColumn<Order, String> idCol = new TableColumn<>("Order #");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getOrderId()));
        idCol.setPrefWidth(100);
        
        TableColumn<Order, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(data -> {
            String customerName = orderCustomerNames.getOrDefault(data.getValue().getOrderId(), "—");
            return new javafx.beans.property.SimpleStringProperty(customerName);
        });
        customerCol.setPrefWidth(150);
        
        TableColumn<Order, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            return new javafx.beans.property.SimpleStringProperty(
                data.getValue().getOrderTime().format(formatter));
        });
        timeCol.setPrefWidth(100);
        
        TableColumn<Order, Integer> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(
            data.getValue().getItems().size()).asObject());
        itemsCol.setPrefWidth(80);
        
        TableColumn<Order, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            "₱" + String.format("%.2f", data.getValue().getTotalAmount())));
        totalCol.setPrefWidth(100);
        
        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().isPaid() ? "Paid" : "Pending"));
        statusCol.setPrefWidth(100);
        
        TableColumn<Order, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(220);
        actionCol.setCellFactory(col -> new TableCell<Order, Void>() {
            private final Button completeBtn = new Button("Complete");
            private final Button removeBtn = new Button("✕");
            
            {
                completeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
                removeBtn.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #D32F2F; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
                
                completeBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    if (order.getItems().isEmpty()) {
                        showAlert("No Items", "This order has no items. Please add items first or remove the order.", Alert.AlertType.WARNING);
                        return;
                    }
                    completeOrderDirectly(order);
                });
                
                removeBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    // Remove from main queue
                    orderQueue.remove(order);
                    orderCustomerNames.remove(order.getOrderId());
                    // Also remove from current table view if it's a filtered list
                    getTableView().getItems().remove(order);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(8, completeBtn, removeBtn);
                    buttons.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(buttons);
                }
            }
        });
        
        orderQueueTable.getColumns().addAll(idCol, customerCol, timeCol, itemsCol, totalCol, statusCol, actionCol);
        
        // Order details panel
        VBox detailsPanel = new VBox(10);
        detailsPanel.setPadding(new Insets(20));
        detailsPanel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        detailsPanel.setPrefHeight(200);
        
        Label detailsTitle = new Label("Order Details");
        detailsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        detailsTitle.setTextFill(Color.web("#3E2723"));
        
        TextArea orderDetailsArea = new TextArea();
        orderDetailsArea.setEditable(false);
        orderDetailsArea.setWrapText(true);
        orderDetailsArea.setFont(Font.font("Consolas", 12));
        orderDetailsArea.setText("Select an order to view details...");
        orderDetailsArea.setPrefHeight(150);
        orderDetailsArea.setStyle("-fx-control-inner-background: #FAFAFA; -fx-background-color: #FAFAFA;");
        
        detailsPanel.getChildren().addAll(detailsTitle, orderDetailsArea);
        
        // Add selection listener to show order details
        orderQueueTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                String customerName = orderCustomerNames.getOrDefault(newSelection.getOrderId(), "Unknown");
                StringBuilder details = new StringBuilder();
                details.append("ORDER DETAILS\n");
                details.append("-------------------------------------------\n");
                details.append(String.format("Order #:    %s\n", newSelection.getOrderId()));
                details.append(String.format("Customer:   %s\n", customerName));
                details.append(String.format("Time:       %s\n", 
                    newSelection.getOrderTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
                details.append(String.format("Status:     %s\n\n", newSelection.isPaid() ? "Paid" : "Pending"));
                details.append("ITEMS\n");
                details.append("-------------------------------------------\n");
                
                if (newSelection.getItems().isEmpty()) {
                    details.append("No items.\n");
                } else {
                    java.util.Map<String, Integer> qtyMap = new java.util.LinkedHashMap<>();
                    java.util.Map<String, Double> priceMap = new java.util.HashMap<>();
                    for (OrderItem item : newSelection.getItems()) {
                        String name = item.getProduct().getName();
                        qtyMap.put(name, qtyMap.getOrDefault(name, 0) + item.getQuantity());
                        priceMap.put(name, item.getProduct().getPrice());
                    }

                    for (java.util.Map.Entry<String, Integer> e : qtyMap.entrySet()) {
                        String name = e.getKey();
                        int qty = e.getValue();
                        double price = priceMap.getOrDefault(name, 0.0);
                        double subtotal = price * qty;
                        details.append(String.format("%-20s x%d   ₱%.2f\n", name, qty, subtotal));
                    }
                }
                
                details.append("-------------------------------------------\n");
                details.append(String.format("TOTAL:      ₱%.2f\n", newSelection.getTotalAmount()));
                
                orderDetailsArea.setText(details.toString());
            } else {
                orderDetailsArea.setText("Select an order to view details...");
            }
        });
        
        Label queueCount = new Label("Total Orders in Queue: 0");
        queueCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        queueCount.setTextFill(Color.web("#795548"));
        orderQueue.addListener((javafx.collections.ListChangeListener.Change<? extends Order> c) -> {
            queueCount.setText("Total Orders in Queue: " + orderQueue.size());
        });
        
        panel.getChildren().addAll(title, subtitle, new Separator(), searchBox, 
                                   orderQueueTable, queueCount, new Separator(), detailsPanel);
        
        return panel;
    }
    
    private void searchOrders() {
        String searchText = customerNameInput.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            showAlert("Input Required", "Please enter a customer name to search.", Alert.AlertType.WARNING);
            return;
        }
        
        // Filter orders by customer name
        ObservableList<Order> filteredOrders = FXCollections.observableArrayList();
        for (Order order : orderQueue) {
            String customerName = orderCustomerNames.getOrDefault(order.getOrderId(), "");
            if (customerName.toLowerCase().contains(searchText)) {
                filteredOrders.add(order);
            }
        }
        
        orderQueueTable.setItems(filteredOrders);
        
        if (filteredOrders.isEmpty()) {
            showAlert("No Results", "No orders found for customer: " + searchText, Alert.AlertType.INFORMATION);
        }
    }
    
    private void showAllOrders() {
        orderQueueTable.setItems(orderQueue);
        customerNameInput.clear();
    }
    
    private void completeOrderDirectly(Order order) {
        // Validate stock and inventory
        if (!store.isStockSufficient(order)) {
            showAlert("Stock Error", "Insufficient stock for some items.", Alert.AlertType.ERROR);
            return;
        }

        if (!store.isInventorySufficient(order)) {
            showAlert("Ingredient Error", "Insufficient ingredients for some items.", Alert.AlertType.ERROR);
            return;
        }

        try {
            // Get customer name from stored map, or prompt if not found
            String customerName = orderCustomerNames.getOrDefault(order.getOrderId(), null);
            if (customerName == null) {
                TextInputDialog nameDialog = new TextInputDialog();
                nameDialog.setTitle("Customer Name");
                nameDialog.setHeaderText("Enter Customer Name");
                nameDialog.setContentText("Name:");
                customerName = nameDialog.showAndWait().orElse("Guest");
            }
            
            // Process checkout
            store.checkoutBasket(order);
            order.setPaid(true);

            // Get order type
            String orderType = orderTypes.getOrDefault(order.getOrderId(), "Dine In");

            // Generate receipt with order type
            String receiptContent = generateReceiptWithOrderType(order, customerName, orderType);
            String receiptId = "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // Save receipt to database
            Receipt receipt = new Receipt(
                receiptId,
                order.getOrderId(),
                customerName,
                order.getTotalAmount(),
                order.getTotalAmount(), // paid amount same as total
                0.0 // no change
            );
            receipt.setReceiptContent(receiptContent);
            TextDatabase.saveReceipt(receipt);
            
            // Add to receipt history
            receiptHistory.add(0, receipt);

            // Save order items to text database
            saveOrderToDatabase(order, customerName);
            
            // Mark order as completed in pending orders file
            TextDatabase.markOrderCompleted(order.getOrderId());

            // Check for refill alerts
            if (store.hasProductsNeedingRefill()) {
                String alerts = store.getProductRefillAlerts();
                Alert refillAlert = new Alert(Alert.AlertType.WARNING);
                refillAlert.setTitle("⚠ Refill Alert");
                refillAlert.setHeaderText("Low Stock Detected");
                refillAlert.setContentText("Please notify admin:\n\n" + alerts);
                refillAlert.showAndWait();
            }

            // Show success confirmation and wait for user to click OK
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Success");
            successAlert.setHeaderText("Order Completed Successfully!");
            successAlert.setContentText("Order ID: " + order.getOrderId() + 
                     "\nReceipt ID: " + receiptId + "\n\nOrder marked as COMPLETED in system.");
            
            successAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // Only remove from queue if user clicked OK
                    orderQueue.remove(order);
                    orderCustomerNames.remove(order.getOrderId());
                    orderTypes.remove(order.getOrderId());
                    // Also remove from current table view if it's a filtered list
                    orderQueueTable.getItems().remove(order);
                }
            });

        } catch (Exception e) {
            showAlert("Checkout Error", "Failed to complete checkout: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
        }
    }
    
    // ==================== RECEIPT HISTORY PANEL ====================
    
    private VBox createReceiptHistoryPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        
        Label title = new Label("Receipt History");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#3E2723"));
        
        Label subtitle = new Label("View past transactions");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#795548"));
        
        // Search box
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(20));
        searchBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search Order ID / Customer...");
        searchField.setPrefWidth(350);
        searchField.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-radius: 6; -fx-padding: 8;");
        
        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        searchBtn.setOnAction(e -> searchReceipts(searchField.getText()));
        
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> loadReceiptHistory());
        
        searchBox.getChildren().addAll(searchField, searchBtn, refreshBtn);
        
        // Split pane for table and details
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        splitPane.setStyle("-fx-background-color: transparent;");
        
        // Receipt table
        receiptHistoryTable = new TableView<>();
        receiptHistoryTable.setItems(receiptHistory);
        receiptHistoryTable.setPrefWidth(800);
        receiptHistoryTable.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 8;");
        
        TableColumn<Receipt, String> receiptIdCol = new TableColumn<>("Receipt #");
        receiptIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getReceiptId()));
        receiptIdCol.setPrefWidth(120);
        
        TableColumn<Receipt, String> orderIdCol = new TableColumn<>("Order #");
        orderIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getOrderId()));
        orderIdCol.setPrefWidth(120);
        
        TableColumn<Receipt, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getUserName()));
        customerCol.setPrefWidth(150);
        
        TableColumn<Receipt, String> receiptTimeCol = new TableColumn<>("Date/Time");
        receiptTimeCol.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                data.getValue().getReceiptTime().format(formatter));
        });
        receiptTimeCol.setPrefWidth(150);
        
        TableColumn<Receipt, String> amountCol = new TableColumn<>("Total");
        amountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            "₱" + String.format("%.2f", data.getValue().getTotalAmount())));
        amountCol.setPrefWidth(100);
        
        receiptHistoryTable.getColumns().addAll(receiptIdCol, orderIdCol, customerCol, 
                                                receiptTimeCol, amountCol);
        
        receiptHistoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayReceiptDetails(newVal);
            }
        });
        
        // Receipt details
        VBox detailsBox = new VBox(15);
        detailsBox.setPadding(new Insets(20));
        detailsBox.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        
        Label detailsTitle = new Label("Receipt View");
        detailsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        detailsTitle.setTextFill(Color.web("#3E2723"));
        
        receiptDetailArea = new TextArea();
        receiptDetailArea.setEditable(false);
        receiptDetailArea.setFont(Font.font("Consolas", 11));
        receiptDetailArea.setPrefWidth(400);
        receiptDetailArea.setWrapText(true);
        receiptDetailArea.setText("Select a receipt to view details");
        receiptDetailArea.setStyle("-fx-control-inner-background: #FAFAFA;");
        
        Button printBtn = new Button("Print Receipt");
        printBtn.setStyle("-fx-background-color: #6F4E37; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        printBtn.setPrefWidth(200);
        
        printBtn.setOnAction(e -> {
            Receipt selected = receiptHistoryTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAlert("Print", "Receipt #" + selected.getReceiptId() + " sent to printer.", 
                         Alert.AlertType.INFORMATION);
            }
        });
        
        detailsBox.getChildren().addAll(detailsTitle, new Separator(), receiptDetailArea, printBtn);
        
        splitPane.getItems().addAll(receiptHistoryTable, detailsBox);
        splitPane.setDividerPositions(0.65);
        
        Label countLabel = new Label("Total Receipts: 0");
        countLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        countLabel.setTextFill(Color.web("#795548"));
        receiptHistory.addListener((javafx.collections.ListChangeListener.Change<? extends Receipt> c) -> {
            countLabel.setText("Total Receipts: " + receiptHistory.size());
        });
        countLabel.setText("Total Receipts: " + receiptHistory.size());
        
        panel.getChildren().addAll(title, subtitle, searchBox, new Separator(), splitPane, countLabel);
        
        return panel;
    }
    
    private void searchReceipts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            loadReceiptHistory();
            return;
        }
        
        receiptHistory.clear();
        List<Receipt> allReceipts = TextDatabase.loadAllReceipts();
        
        for (Receipt receipt : allReceipts) {
            if (receipt.getOrderId().toLowerCase().contains(searchTerm.toLowerCase()) ||
                receipt.getUserName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                receipt.getReceiptId().toLowerCase().contains(searchTerm.toLowerCase())) {
                receiptHistory.add(receipt);
            }
        }
    }
    
    private void displayReceiptDetails(Receipt receipt) {
        if (receipt.getReceiptContent() != null && !receipt.getReceiptContent().isEmpty()) {
            receiptDetailArea.setText(receipt.getReceiptContent());
        } else {
            StringBuilder details = new StringBuilder();
            details.append("═══════════════════════════════════════\n");
            details.append("          COFFEE SHOP RECEIPT\n");
            details.append("═══════════════════════════════════════\n");
            details.append("Receipt ID: ").append(receipt.getReceiptId()).append("\n");
            details.append("Order ID: ").append(receipt.getOrderId()).append("\n");
            details.append("Customer: ").append(receipt.getUserName()).append("\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            details.append("Date: ").append(receipt.getReceiptTime().format(formatter)).append("\n");
            details.append("───────────────────────────────────────\n");
            details.append("Total Amount: ₱").append(String.format("%.2f", receipt.getTotalAmount())).append("\n");
            details.append("Cash Paid: ₱").append(String.format("%.2f", receipt.getCashPaid())).append("\n");
            details.append("Change: ₱").append(String.format("%.2f", receipt.getChange())).append("\n");
            details.append("═══════════════════════════════════════\n");
            details.append("       Thank you for your order!\n");
            details.append("═══════════════════════════════════════\n");
            
            receiptDetailArea.setText(details.toString());
        }
    }

    // Entry point for Maven exec plugin and runnable jar
    public static void main(String[] args) {
        // Launch JavaFX application
        Application.launch(CashierApp.class, args);
    }
}
