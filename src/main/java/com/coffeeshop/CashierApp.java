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
import java.util.List;
import java.util.UUID;

public class CashierApp extends Application {
    private Store store;
    private TextField customerNameInput;
    
    // Order queue management
    private ObservableList<Order> orderQueue = FXCollections.observableArrayList();
    private TableView<Order> orderQueueTable;
    private java.util.HashMap<String, String> orderCustomerNames = new java.util.HashMap<>();
    
    // Receipt management
    private ObservableList<Receipt> receiptHistory = FXCollections.observableArrayList();
    private TableView<Receipt> receiptHistoryTable;
    private TextArea receiptDetailArea;

    @Override
    public void start(Stage primaryStage) {
        store = Store.getInstance();
        loadReceiptHistory();
        loadPendingOrdersFromFile();

        primaryStage.setTitle("Coffee Shop - Cashier Terminal");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Header
        VBox header = createHeader();
        root.setTop(header);

        // Tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Tab 1: Order Queue
        Tab ordersTab = new Tab("📋 Order Queue");
        ordersTab.setContent(createOrderQueuePanel());
        
        // Tab 2: Receipt History
        Tab receiptHistoryTab = new Tab("🧾 Receipt History");
        receiptHistoryTab.setContent(createReceiptHistoryPanel());
        
        tabPane.getTabs().addAll(ordersTab, receiptHistoryTab);
        
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
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
            
            // Store customer name for this order
            orderCustomerNames.put(pendingOrder.getOrderId(), pendingOrder.getCustomerName());
            
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
    
    // ==================== ORDER QUEUE PANEL ====================
    
    private VBox createOrderQueuePanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        
        Label title = new Label("📋 Customer Order Queue");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        
        Label subtitle = new Label("All customer orders waiting to be processed");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#666"));
        
        // Search section
        HBox searchBox = new HBox(15);
        searchBox.setPadding(new Insets(15));
        searchBox.setStyle("-fx-background-color: white; -fx-border-color: #1565C0; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        
        Label searchLabel = new Label("Search Customer:");
        searchLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        
        customerNameInput = new TextField();
        customerNameInput.setPromptText("Enter customer name to search...");
        customerNameInput.setPrefWidth(250);
        
        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        searchBtn.setOnAction(e -> searchOrders());
        
        Button clearBtn = new Button("Show All");
        clearBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        clearBtn.setOnAction(e -> showAllOrders());
        
        searchBox.getChildren().addAll(searchLabel, customerNameInput, searchBtn, clearBtn);
        
        // Orders table
        orderQueueTable = new TableView<>();
        orderQueueTable.setItems(orderQueue);
        orderQueueTable.setPrefHeight(500);
        
        TableColumn<Order, String> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getOrderId()));
        idCol.setPrefWidth(120);
        
        TableColumn<Order, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(data -> {
            String customerName = orderCustomerNames.getOrDefault(data.getValue().getOrderId(), "—");
            return new javafx.beans.property.SimpleStringProperty(customerName);
        });
        customerCol.setPrefWidth(150);
        
        TableColumn<Order, String> timeCol = new TableColumn<>("Order Time");
        timeCol.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return new javafx.beans.property.SimpleStringProperty(
                data.getValue().getOrderTime().format(formatter));
        });
        timeCol.setPrefWidth(180);
        
        TableColumn<Order, Integer> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(
            data.getValue().getItems().size()).asObject());
        itemsCol.setPrefWidth(80);
        
        TableColumn<Order, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            "$" + String.format("%.2f", data.getValue().getTotalAmount())));
        totalCol.setPrefWidth(100);
        
        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().isPaid() ? "Paid" : "Pending"));
        statusCol.setPrefWidth(100);
        
        TableColumn<Order, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(col -> new TableCell<Order, Void>() {
            private final Button completeBtn = new Button("Complete Order");
            private final Button removeBtn = new Button("Remove");
            
            {
                completeBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold;");
                removeBtn.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold;");
                
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
                    setGraphic(buttons);
                }
            }
        });
        
        orderQueueTable.getColumns().addAll(idCol, customerCol, timeCol, itemsCol, totalCol, statusCol, actionCol);
        
        // Order details panel
        VBox detailsPanel = new VBox(10);
        detailsPanel.setPadding(new Insets(15));
        detailsPanel.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
        detailsPanel.setPrefHeight(200);
        
        Label detailsTitle = new Label("Order Details");
        detailsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        TextArea orderDetailsArea = new TextArea();
        orderDetailsArea.setEditable(false);
        orderDetailsArea.setWrapText(true);
        orderDetailsArea.setFont(Font.font("Courier New", 12));
        orderDetailsArea.setText("Select an order to view details...");
        orderDetailsArea.setPrefHeight(150);
        
        detailsPanel.getChildren().addAll(detailsTitle, orderDetailsArea);
        
        // Add selection listener to show order details
        orderQueueTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                String customerName = orderCustomerNames.getOrDefault(newSelection.getOrderId(), "Unknown");
                StringBuilder details = new StringBuilder();
                details.append("═══════════════════════════════════════════\n");
                details.append("               ORDER DETAILS\n");
                details.append("═══════════════════════════════════════════\n\n");
                details.append(String.format("Order ID:     %s\n", newSelection.getOrderId()));
                details.append(String.format("Customer:     %s\n", customerName));
                details.append(String.format("Order Time:   %s\n", 
                    newSelection.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
                details.append(String.format("Status:       %s\n\n", newSelection.isPaid() ? "Paid" : "Pending"));
                details.append("───────────────────────────────────────────\n");
                details.append("                   ITEMS\n");
                details.append("───────────────────────────────────────────\n\n");
                
                if (newSelection.getItems().isEmpty()) {
                    details.append("No items in this order.\n");
                } else {
                    for (OrderItem item : newSelection.getItems()) {
                        details.append(String.format("• %s\n", item.getProduct().getName()));
                        details.append(String.format("  Price:       ₱%.2f\n", item.getProduct().getPrice()));
                        details.append(String.format("  Quantity:    %d\n", item.getQuantity()));
                        details.append(String.format("  Temperature: %s\n", item.getTemperature()));
                        details.append(String.format("  Sugar Level: %d%%\n", item.getSugarLevel()));
                        details.append(String.format("  Subtotal:    ₱%.2f\n\n", item.getSubtotal()));
                    }
                }
                
                details.append("═══════════════════════════════════════════\n");
                details.append(String.format("TOTAL AMOUNT: ₱%.2f\n", newSelection.getTotalAmount()));
                details.append("═══════════════════════════════════════════\n");
                
                orderDetailsArea.setText(details.toString());
            } else {
                orderDetailsArea.setText("Select an order to view details...");
            }
        });
        
        Label queueCount = new Label("Total Orders in Queue: 0");
        queueCount.setFont(Font.font("Arial", FontWeight.BOLD, 14));
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

            // Generate receipt
            String receiptContent = order.printReceipt();
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
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        
        Label title = new Label("🧾 Receipt History");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        
        Label subtitle = new Label("All processed transactions and receipts");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#666"));
        
        // Search box
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search by Order ID or Customer Name...");
        searchField.setPrefWidth(400);
        
        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        searchBtn.setOnAction(e -> searchReceipts(searchField.getText()));
        
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        refreshBtn.setOnAction(e -> loadReceiptHistory());
        
        searchBox.getChildren().addAll(searchField, searchBtn, refreshBtn);
        
        // Split pane for table and details
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        
        // Receipt table
        receiptHistoryTable = new TableView<>();
        receiptHistoryTable.setItems(receiptHistory);
        receiptHistoryTable.setPrefWidth(800);
        
        TableColumn<Receipt, String> receiptIdCol = new TableColumn<>("Receipt ID");
        receiptIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getReceiptId()));
        receiptIdCol.setPrefWidth(120);
        
        TableColumn<Receipt, String> orderIdCol = new TableColumn<>("Order ID");
        orderIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getOrderId()));
        orderIdCol.setPrefWidth(120);
        
        TableColumn<Receipt, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getUserName()));
        customerCol.setPrefWidth(150);
        
        TableColumn<Receipt, String> receiptTimeCol = new TableColumn<>("Date/Time");
        receiptTimeCol.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return new javafx.beans.property.SimpleStringProperty(
                data.getValue().getReceiptTime().format(formatter));
        });
        receiptTimeCol.setPrefWidth(180);
        
        TableColumn<Receipt, String> amountCol = new TableColumn<>("Total");
        amountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            "$" + String.format("%.2f", data.getValue().getTotalAmount())));
        amountCol.setPrefWidth(100);
        
        TableColumn<Receipt, String> changeCol = new TableColumn<>("Change");
        changeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            "$" + String.format("%.2f", data.getValue().getChange())));
        changeCol.setPrefWidth(100);
        
        receiptHistoryTable.getColumns().addAll(receiptIdCol, orderIdCol, customerCol, 
                                                receiptTimeCol, amountCol, changeCol);
        
        receiptHistoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayReceiptDetails(newVal);
            }
        });
        
        // Receipt details
        VBox detailsBox = new VBox(10);
        detailsBox.setPadding(new Insets(10));
        detailsBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        
        Label detailsTitle = new Label("Receipt Details");
        detailsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        receiptDetailArea = new TextArea();
        receiptDetailArea.setEditable(false);
        receiptDetailArea.setFont(Font.font("Courier New", 11));
        receiptDetailArea.setPrefWidth(400);
        receiptDetailArea.setWrapText(true);
        receiptDetailArea.setText("Select a receipt to view details");
        
        Button printBtn = new Button("Print Receipt");
        printBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
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
        splitPane.setDividerPositions(0.6);
        
        Label countLabel = new Label("Total Receipts: 0");
        countLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
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
            details.append("Total Amount: $").append(String.format("%.2f", receipt.getTotalAmount())).append("\n");
            details.append("Cash Paid: $").append(String.format("%.2f", receipt.getCashPaid())).append("\n");
            details.append("Change: $").append(String.format("%.2f", receipt.getChange())).append("\n");
            details.append("═══════════════════════════════════════\n");
            details.append("       Thank you for your order!\n");
            details.append("═══════════════════════════════════════\n");
            
            receiptDetailArea.setText(details.toString());
        }
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("💳 Coffee Shop - Cashier Terminal");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#1565C0"));

        Label subtitle = new Label("Process customer orders and payments");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#666"));

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

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
