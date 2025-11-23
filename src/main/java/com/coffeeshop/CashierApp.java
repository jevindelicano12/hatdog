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
    // currently logged-in cashier id (e.g., "cashier1")
    private String currentCashierId;
    // Removed customer search input to simplify UI and enlarge order queue
    
    // Order queue management (use PendingOrder so we preserve status lifecycle)
    private javafx.collections.ObservableList<com.coffeeshop.model.PendingOrder> orderQueue = FXCollections.observableArrayList();
    private TableView<com.coffeeshop.model.PendingOrder> orderQueueTable;
    // Per-stage lists & tables
    private ObservableList<com.coffeeshop.model.PendingOrder> pendingList = FXCollections.observableArrayList();
    private ObservableList<com.coffeeshop.model.PendingOrder> preparingList = FXCollections.observableArrayList();
    private ObservableList<com.coffeeshop.model.PendingOrder> completedList = FXCollections.observableArrayList();
    private TableView<com.coffeeshop.model.PendingOrder> pendingTable;
    private TableView<com.coffeeshop.model.PendingOrder> preparingTable;
    private TableView<com.coffeeshop.model.PendingOrder> completedTable;
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
        // Require login before initializing the UI
        currentCashierId = null;
        boolean ok = showLoginDialog(primaryStage);
        if (!ok) {
            // user cancelled or failed to login; exit
            javafx.application.Platform.exit();
            return;
        }

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
        
        setScenePreserveWindowSize(primaryStage, scene);

        // Start background scheduler to refresh data for near real-time sync
        startBackgroundSync();
    }

    // Utility to preserve window size/maximized state when switching scenes
    private void setScenePreserveWindowSize(Stage primaryStage, Scene scene) {
        if (primaryStage == null) {
            // Fallback: try to find a visible window
            java.util.Optional<javafx.stage.Window> any = javafx.stage.Window.getWindows().stream().filter(javafx.stage.Window::isShowing).findFirst();
            if (any.isPresent()) {
                primaryStage = (Stage) any.get();
            } else {
                // As a last resort, create a new Stage
                primaryStage = new Stage();
            }
        }

        boolean wasMax = false;
        double prevW = -1, prevH = -1;
        try {
            wasMax = primaryStage.isMaximized();
            prevW = primaryStage.getWidth();
            prevH = primaryStage.getHeight();
        } catch (Exception ignored) {}

        primaryStage.setScene(scene);
        if (wasMax) {
            primaryStage.setMaximized(true);
        } else {
            if (prevW > 0 && prevH > 0) {
                primaryStage.setWidth(prevW);
                primaryStage.setHeight(prevH);
            }
        }
        if (!primaryStage.isShowing()) primaryStage.show();
    }

    private VBox createHeader() {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("Cashier Terminal");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#3E2723"));

        String sub = "Process customer orders and payments";
        if (currentCashierId != null && !currentCashierId.isEmpty()) {
            sub += " — Logged in: " + currentCashierId;
        }
        Label subtitle = new Label(sub);
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#795548"));

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    /**
     * Show a simple modal login dialog for cashier users.
     * Accepts two accounts: cashier1/cashier1 and cashier2/cashier2.
     * Returns true if login succeeded and sets `currentCashierId`.
     */
    private boolean showLoginDialog(Stage owner) {
        final Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Cashier Login");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        Label userLbl = new Label("Username:");
        TextField userField = new TextField();
        userField.setPromptText("cashier1 or cashier2");

        Label passLbl = new Label("Password:");
        PasswordField passField = new PasswordField();
        passField.setPromptText("password");

        Label msg = new Label();
        msg.setTextFill(javafx.scene.paint.Color.web("#D32F2F"));

        Button loginBtn = new Button("Login");
        Button cancelBtn = new Button("Cancel");

        HBox actions = new HBox(8, loginBtn, cancelBtn);

        grid.add(userLbl, 0, 0);
        grid.add(userField, 1, 0);
        grid.add(passLbl, 0, 1);
        grid.add(passField, 1, 1);
        grid.add(msg, 0, 2, 2, 1);
        grid.add(actions, 1, 3);

        // Quick login when Enter pressed in password
        passField.setOnAction(e -> loginBtn.fire());

        loginBtn.setOnAction(e -> {
            String user = userField.getText() == null ? "" : userField.getText().trim();
            String pass = passField.getText() == null ? "" : passField.getText();
            if (("cashier1".equals(user) && "cashier1".equals(pass)) || ("cashier2".equals(user) && "cashier2".equals(pass))) {
                currentCashierId = user;
                dialog.close();
            } else {
                msg.setText("Invalid username or password.");
            }
        });

        cancelBtn.setOnAction(e -> {
            dialog.close();
        });

        Scene scene = new Scene(grid, 360, 180);
        dialog.setScene(scene);
        dialog.showAndWait();

        return currentCashierId != null;
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

                // Prepare maps
                java.util.Map<String, String> newCustomerNames = new java.util.HashMap<>();
                java.util.Map<String, String> newOrderTypes = new java.util.HashMap<>();
                for (PendingOrder po : pendingOrders) {
                    newCustomerNames.put(po.getOrderId(), po.getCustomerName());
                    newOrderTypes.put(po.getOrderId(), po.getOrderType());
                }

                // Update UI data on FX thread
                javafx.application.Platform.runLater(() -> {
                    try {
                        receiptHistory.setAll(receipts);
                        // reload all pending orders and populate stage lists
                        loadPendingOrdersFromFile();

                        if (dashboardPanel != null) {
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
        List<PendingOrder> allOrders = TextDatabase.loadAllPendingOrders();

        orderQueue.clear();
        pendingList.clear();
        preparingList.clear();
        completedList.clear();

        for (PendingOrder pendingOrder : allOrders) {
            orderCustomerNames.put(pendingOrder.getOrderId(), pendingOrder.getCustomerName());
            orderTypes.put(pendingOrder.getOrderId(), pendingOrder.getOrderType());
            orderQueue.add(pendingOrder);

            String st = pendingOrder.getStatus();
            // Map: show unpaid (PENDING) and paid orders in the left table so cashier can pay or start preparing
            if (PendingOrder.STATUS_PENDING.equals(st) || PendingOrder.STATUS_PAID.equals(st)) {
                pendingList.add(pendingOrder);
            } else if (PendingOrder.STATUS_PREPARING.equals(st)) {
                preparingList.add(pendingOrder);
            } else if (PendingOrder.STATUS_COMPLETED.equals(st)) {
                completedList.add(pendingOrder);
            }
        }
    }

    // Map internal status codes to friendly labels
    private String mapStatusToLabel(String status) {
        if (PendingOrder.STATUS_PENDING.equals(status)) return "Pending (Unpaid)";
        if (PendingOrder.STATUS_PAID.equals(status)) return "Paid";
        if (PendingOrder.STATUS_PREPARING.equals(status)) return "Preparing";
        if (PendingOrder.STATUS_COMPLETED.equals(status)) return "Completed";
        return status != null ? status : "—";
    }

    // Primary action handler for lifecycle transitions
    private void handlePrimaryAction(PendingOrder po) {
        if (po == null) return;
        String status = po.getStatus();
        try {
            if (PendingOrder.STATUS_PENDING.equals(status)) {
                payAndQueue(po);
            } else if (PendingOrder.STATUS_PAID.equals(status)) {
                startMaking(po);
            } else if (PendingOrder.STATUS_PREPARING.equals(status)) {
                // finish the order
                completePickup(po);
            }
        } catch (Exception ex) {
            showAlert("Action Error", "Failed to perform action: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void payAndQueue(PendingOrder po) {
        // Build an Order object from PendingOrder to run checkout logic
        Order order = new Order(po.getOrderId());
        if (po.getOrderTime() != null) order.setOrderTime(po.getOrderTime());
        for (PendingOrder.OrderItemData item : po.getItems()) {
            Product product = store.getProducts().stream().filter(p -> p.getName().equals(item.productName)).findFirst().orElse(null);
            if (product != null) {
                OrderItem oi = new OrderItem(product, item.quantity, item.temperature, item.sugarLevel);
                order.addItem(oi);
            }
        }

        // Confirm customer name
        String customerName = orderCustomerNames.getOrDefault(po.getOrderId(), null);
        if (customerName == null) {
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Customer Name");
            nameDialog.setHeaderText("Enter Customer Name");
            nameDialog.setContentText("Name:");
            customerName = nameDialog.showAndWait().orElse("Guest");
        }

        // Validate stock and process checkout
        if (!store.isStockSufficient(order)) {
            showAlert("Stock Error", "Insufficient stock for some items.", Alert.AlertType.ERROR);
            return;
        }
        if (!store.isInventorySufficient(order)) {
            showAlert("Ingredient Error", "Insufficient ingredients for some items.", Alert.AlertType.ERROR);
            return;
        }

        store.checkoutBasket(order);

        // Generate and save receipt
        String orderType = orderTypes.getOrDefault(po.getOrderId(), "Dine In");
        String receiptContent = generateReceiptWithOrderType(order, customerName, orderType);
        String receiptId = "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Receipt receipt = new Receipt(receiptId, order.getOrderId(), customerName, order.getTotalAmount(), order.getTotalAmount(), 0.0);
        receipt.setReceiptContent(receiptContent);
        TextDatabase.saveReceipt(receipt);
        receiptHistory.add(0, receipt);

        // Save order items to order/item DBs
        saveOrderToDatabase(order, customerName);

        // Update pending order status to PAID and persist
        po.setStatus(PendingOrder.STATUS_PAID);
        TextDatabase.savePendingOrder(po);
        // refresh UI
        loadPendingOrdersFromFile();
        showAlert("Payment Successful", "Order paid and added to production queue.", Alert.AlertType.INFORMATION);
    }

    private void startMaking(PendingOrder po) {
        po.setStatus(PendingOrder.STATUS_PREPARING);
        TextDatabase.savePendingOrder(po);
        loadPendingOrdersFromFile();
    }

    // Pay the pending order and immediately mark it as preparing (ready for production)
    private void payAndStartPreparing(PendingOrder po) {
        if (po == null) return;
        // Process payment (deduct stock, create receipt, mark as PAID)
        payAndQueue(po);
        // Then move to preparing state
        startMaking(po);
    }

    private void markReady(PendingOrder po) {
        // Legacy method repurposed: mark as completed
        po.setStatus(PendingOrder.STATUS_COMPLETED);
        TextDatabase.savePendingOrder(po);
        loadPendingOrdersFromFile();
    }

    private void completePickup(PendingOrder po) {
        po.setStatus(PendingOrder.STATUS_COMPLETED);
        TextDatabase.savePendingOrder(po);
        // Also mark completed in helper (keeps behavior consistent)
        TextDatabase.markOrderCompleted(po.getOrderId());
        orderQueue.remove(po);
        orderCustomerNames.remove(po.getOrderId());
        orderTypes.remove(po.getOrderId());
        showAlert("Completed", "Order marked as completed (picked up).", Alert.AlertType.INFORMATION);
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
    
    private javafx.scene.control.ScrollPane createOrderQueuePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        
        Label title = new Label("Order Queue");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#3E2723"));
        
        Label subtitle = new Label("Manage incoming customer orders");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#795548"));
        
        // Removed search box to simplify UI; order queue expanded instead
        
        // Build three tables for the 3 steps: Paid, Preparing, Completed (use class-level lists)
        // paidList, preparingList, completedList are class fields
        pendingTable = new TableView<>(pendingList);
        preparingTable = new TableView<>(preparingList);
        completedTable = new TableView<>(completedList);

        // Create separate column instances for each table (TableColumn cannot be shared between TableViews)
        // Pending table columns
        TableColumn<PendingOrder, String> idColP = new TableColumn<>("Order #");
        idColP.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getOrderId()));
        idColP.setPrefWidth(100);

        TableColumn<PendingOrder, String> customerColP = new TableColumn<>("Customer");
        customerColP.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            orderCustomerNames.getOrDefault(data.getValue().getOrderId(), "—")));
        customerColP.setPrefWidth(140);

        TableColumn<PendingOrder, String> timeColP = new TableColumn<>("Time");
        timeColP.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getOrderTime().format(DateTimeFormatter.ofPattern("hh:mm:ss a"))));
        timeColP.setPrefWidth(100);

        TableColumn<PendingOrder, Integer> itemsColP = new TableColumn<>("Items");
        itemsColP.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(
            data.getValue().getItems().size()).asObject());
        itemsColP.setPrefWidth(70);

        TableColumn<PendingOrder, String> totalColP = new TableColumn<>("Total");
        totalColP.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            "₱" + String.format("%.2f", data.getValue().getTotalAmount())));
        totalColP.setPrefWidth(90);

        // Preparing table columns
        TableColumn<PendingOrder, String> idColM = new TableColumn<>("Order #");
        idColM.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getOrderId()));
        idColM.setPrefWidth(100);

        TableColumn<PendingOrder, String> customerColM = new TableColumn<>("Customer");
        customerColM.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            orderCustomerNames.getOrDefault(data.getValue().getOrderId(), "—")));
        customerColM.setPrefWidth(140);

        TableColumn<PendingOrder, String> timeColM = new TableColumn<>("Time");
        timeColM.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getOrderTime().format(DateTimeFormatter.ofPattern("hh:mm:ss a"))));
        timeColM.setPrefWidth(100);

        TableColumn<PendingOrder, Integer> itemsColM = new TableColumn<>("Items");
        itemsColM.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(
            data.getValue().getItems().size()).asObject());
        itemsColM.setPrefWidth(70);

        TableColumn<PendingOrder, String> totalColM = new TableColumn<>("Total");
        totalColM.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            "₱" + String.format("%.2f", data.getValue().getTotalAmount())));
        totalColM.setPrefWidth(90);

        // Completed table columns
        TableColumn<PendingOrder, String> idColD = new TableColumn<>("Order #");
        idColD.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getOrderId()));
        idColD.setPrefWidth(100);

        TableColumn<PendingOrder, String> customerColD = new TableColumn<>("Customer");
        customerColD.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            orderCustomerNames.getOrDefault(data.getValue().getOrderId(), "—")));
        customerColD.setPrefWidth(140);

        TableColumn<PendingOrder, String> timeColD = new TableColumn<>("Time");
        timeColD.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getOrderTime().format(DateTimeFormatter.ofPattern("hh:mm:ss a"))));
        timeColD.setPrefWidth(100);

        TableColumn<PendingOrder, Integer> itemsColD = new TableColumn<>("Items");
        itemsColD.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(
            data.getValue().getItems().size()).asObject());
        itemsColD.setPrefWidth(70);

        TableColumn<PendingOrder, String> totalColD = new TableColumn<>("Total");
        totalColD.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            "₱" + String.format("%.2f", data.getValue().getTotalAmount())));
        totalColD.setPrefWidth(90);

        // Pending table action: Pay (cashier will mark paid and ready for preparing)
        TableColumn<PendingOrder, Void> pendingAction = new TableColumn<>("Actions");
        pendingAction.setPrefWidth(160);
        pendingAction.setCellFactory(col -> new TableCell<PendingOrder, Void>() {
            private final Button actionBtn = new Button();
            private final Button removeBtn = new Button("✕");
            {
                actionBtn.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");
                removeBtn.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #D32F2F; -fx-font-weight: bold;");
                actionBtn.setOnAction(e -> {
                    PendingOrder p = getTableView().getItems().get(getIndex());
                    // Pay and immediately mark as ready for preparing
                    payAndStartPreparing(p);
                });
                removeBtn.setOnAction(e -> {
                    PendingOrder p = getTableView().getItems().get(getIndex());
                    TextDatabase.deletePendingOrder(p.getOrderId());
                    loadPendingOrdersFromFile();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                PendingOrder p = getTableView().getItems().get(getIndex());
                if (p == null) { setGraphic(null); return; }
                String st = p.getStatus();
                // Paid table should only contain PAID orders, so show Start Preparing action
                if (PendingOrder.STATUS_PENDING.equals(st)) {
                    actionBtn.setText("Pay");
                    actionBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold;");
                    setGraphic(new HBox(8, actionBtn, removeBtn));
                } else {
                    setGraphic(null);
                }
            }
        });

        // Preparing table action: Complete
        TableColumn<PendingOrder, Void> prepAction = new TableColumn<>("Actions");
        prepAction.setPrefWidth(120);
        prepAction.setCellFactory(col -> new TableCell<PendingOrder, Void>() {
            private final Button completeBtn = new Button("Complete");
            {
                completeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
                completeBtn.setOnAction(e -> {
                    PendingOrder p = getTableView().getItems().get(getIndex());
                    completePickup(p);
                    loadPendingOrdersFromFile();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                setGraphic(completeBtn);
            }
        });

        // Completed table has no actions
        TableColumn<PendingOrder, Void> doneAction = new TableColumn<>("Actions");
        doneAction.setPrefWidth(80);
        doneAction.setCellFactory(col -> new TableCell<PendingOrder, Void>() {
            @Override protected void updateItem(Void item, boolean empty) { super.updateItem(item, empty); setGraphic(null); }
        });

        // Assemble columns for each table
        pendingTable.getColumns().addAll(idColP, customerColP, timeColP, itemsColP, totalColP, pendingAction);
        preparingTable.getColumns().addAll(idColM, customerColM, timeColM, itemsColM, totalColM, prepAction);
        completedTable.getColumns().addAll(idColD, customerColD, timeColD, itemsColD, totalColD, doneAction);

        // Make tables taller and let columns resize to avoid horizontal scrolling
        pendingTable.setPrefHeight(600);
        preparingTable.setPrefHeight(600);
        completedTable.setPrefHeight(600);
        pendingTable.setMinHeight(300);
        preparingTable.setMinHeight(300);
        completedTable.setMinHeight(300);
        pendingTable.setMaxHeight(Double.MAX_VALUE);
        preparingTable.setMaxHeight(Double.MAX_VALUE);
        completedTable.setMaxHeight(Double.MAX_VALUE);

        // Ensure tables expand horizontally and columns fill available width
        pendingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        preparingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        completedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        pendingTable.setMaxWidth(Double.MAX_VALUE);
        preparingTable.setMaxWidth(Double.MAX_VALUE);
        completedTable.setMaxWidth(Double.MAX_VALUE);

        VBox pendingBox = new VBox(6, new Label("Pending Orders"), pendingTable);
        VBox preparingBox = new VBox(6, new Label("Preparing"), preparingTable);
        VBox completedBox = new VBox(6, new Label("Completed"), completedTable);

        // Allow the boxes to grow vertically so tables can expand
        VBox.setVgrow(pendingTable, Priority.ALWAYS);
        VBox.setVgrow(preparingTable, Priority.ALWAYS);
        VBox.setVgrow(completedTable, Priority.ALWAYS);
        VBox.setVgrow(pendingBox, Priority.ALWAYS);
        VBox.setVgrow(preparingBox, Priority.ALWAYS);
        VBox.setVgrow(completedBox, Priority.ALWAYS);

        HBox tablesRow = new HBox(12, pendingBox, preparingBox, completedBox);
        tablesRow.setPrefWidth(Double.MAX_VALUE);
        HBox.setHgrow(pendingBox, Priority.ALWAYS);
        HBox.setHgrow(preparingBox, Priority.ALWAYS);
        HBox.setHgrow(completedBox, Priority.ALWAYS);

        // Replace old single-table UI: we will populate these lists from file loader
        panel.getChildren().addAll(title, subtitle, new Separator(), tablesRow, new Separator());
        
        // Order details panel (larger and more readable)
        VBox detailsPanel = new VBox(12);
        detailsPanel.setPadding(new Insets(22));
        detailsPanel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        detailsPanel.setPrefHeight(380);
        detailsPanel.setMinHeight(300);

        Label detailsTitle = new Label("Order Details");
        detailsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        detailsTitle.setTextFill(Color.web("#3E2723"));

        TextArea orderDetailsArea = new TextArea();
        orderDetailsArea.setEditable(false);
        orderDetailsArea.setWrapText(true);
        // Use larger monospace font for alignment and readability
        orderDetailsArea.setFont(Font.font("Consolas", 14));
        orderDetailsArea.setText("Select an order to view details...");
        orderDetailsArea.setPrefHeight(320);
        orderDetailsArea.setMaxHeight(Double.MAX_VALUE);
        orderDetailsArea.setPrefWidth(Double.MAX_VALUE);
        orderDetailsArea.setStyle("-fx-control-inner-background: #FAFAFA; -fx-background-color: #FAFAFA; -fx-font-size: 14px;");
        VBox.setVgrow(orderDetailsArea, Priority.ALWAYS);

        detailsPanel.getChildren().addAll(detailsTitle, orderDetailsArea);
        
        // Add selection listeners to each table to show order details (for PendingOrder)
        javafx.beans.value.ChangeListener<PendingOrder> detailsListener = (obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                String customerName = orderCustomerNames.getOrDefault(newSelection.getOrderId(), "Unknown");
                StringBuilder details = new StringBuilder();
                details.append("ORDER DETAILS\n");
                details.append("-------------------------------------------\n");
                details.append(String.format("Order #:    %s\n", newSelection.getOrderId()));
                details.append(String.format("Customer:   %s\n", customerName));
                details.append(String.format("Time:       %s\n", 
                    newSelection.getOrderTime().format(DateTimeFormatter.ofPattern("hh:mm:ss a"))));
                details.append(String.format("Status:     %s\n\n", mapStatusToLabel(newSelection.getStatus())));
                details.append("ITEMS\n");
                details.append("-------------------------------------------\n");

                if (newSelection.getItems().isEmpty()) {
                    details.append("No items.\n");
                } else {
                    java.util.Map<String, Integer> qtyMap = new java.util.LinkedHashMap<>();
                    java.util.Map<String, Double> priceMap = new java.util.HashMap<>();
                    for (PendingOrder.OrderItemData item : newSelection.getItems()) {
                        String name = item.productName;
                        qtyMap.put(name, qtyMap.getOrDefault(name, 0) + item.quantity);
                        Product product = store.getProducts().stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
                        priceMap.put(name, product != null ? product.getPrice() : item.price);
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
        };

        pendingTable.getSelectionModel().selectedItemProperty().addListener(detailsListener);
        preparingTable.getSelectionModel().selectedItemProperty().addListener(detailsListener);
        completedTable.getSelectionModel().selectedItemProperty().addListener(detailsListener);
        
        Label queueCount = new Label("Total Orders in Queue: 0");
        queueCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        queueCount.setTextFill(Color.web("#795548"));
        // update count based on all three lists
        javafx.collections.ListChangeListener<PendingOrder> updateCount = c -> queueCount.setText("Total Orders in Queue: " + (pendingList.size() + preparingList.size() + completedList.size()));
        pendingList.addListener(updateCount);
        preparingList.addListener(updateCount);
        completedList.addListener(updateCount);
        
        panel.getChildren().addAll(queueCount, new Separator(), detailsPanel);

        // Wrap the panel in a ScrollPane so the entire Order Queue area can scroll vertically
        ScrollPane outerScroll = new ScrollPane(panel);
        outerScroll.setFitToWidth(true);
        // Allow the content to be taller than the viewport (so tables can be tall and scroll vertically)
        outerScroll.setFitToHeight(false);
        outerScroll.setPrefViewportHeight(820);
        outerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        outerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        outerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Prefer a taller panel so the tables have room before scrolling
        panel.setPrefHeight(1200);

        return outerScroll;
    }
    
    // Search functionality removed — orders are visible in the main queue and updated in real-time.
    
    
    
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
