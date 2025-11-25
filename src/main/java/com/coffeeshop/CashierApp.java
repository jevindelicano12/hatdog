package com.coffeeshop;

import com.coffeeshop.model.*;
import com.coffeeshop.service.Store;
import com.coffeeshop.service.TextDatabase;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import java.io.File;

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
    private BorderPane rootPane;
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
    // Last cash payment recorded from the cash dialog (so receipts include correct values)
    private double lastCashReceived = 0.0;
    private double lastChange = 0.0;
    
    // Receipt management
    private ObservableList<Receipt> receiptHistory = FXCollections.observableArrayList();
    private TableView<Receipt> receiptHistoryTable;
    private TextArea receiptDetailArea;
    private VBox dashboardPanel; // cached dashboard panel

    private ScheduledExecutorService scheduler;
    private Stage primaryStageRef;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStageRef = primaryStage;
        // Require login before initializing the UI
        currentCashierId = null;
        boolean ok = showLoginDialog(null);
        System.out.println("DEBUG: Login result: ok=" + ok + ", currentCashierId=" + currentCashierId);
        if (!ok) {
            // user cancelled or failed to login; exit
            System.out.println("DEBUG: Login failed, exiting");
            javafx.application.Platform.exit();
            return;
        }

        System.out.println("DEBUG: Setting up main UI...");
        store = Store.getInstance();
        loadReceiptHistory();
        loadPendingOrdersFromFile();

        primaryStage.setTitle("Coffee Shop - Cashier Terminal");

        rootPane = new BorderPane();
        rootPane.setPadding(new Insets(0));
        rootPane.setStyle("-fx-background-color: #F3F4F6;");
        // Add CSS class so external stylesheet can style the app background
        rootPane.getStyleClass().add("cashier-root");

        // Header
        VBox header = createHeader();
        rootPane.setTop(header);

        // Tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #FFFFFF; -fx-tab-min-height: 50px; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");
        
        // Tab 1: Order Queue
        Tab ordersTab = new Tab("   📋 Order Queue   ");
        ordersTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        ordersTab.setContent(createOrderQueuePanel());
        
        // Tab 2: Returns
        Tab returnsTab = new Tab("   ↩ Returns   ");
        returnsTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        returnsTab.setContent(createReturnsPanel());
        
        // Tab 3: Receipt History
        Tab receiptHistoryTab = new Tab("   📜 Receipt History   ");
        receiptHistoryTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        receiptHistoryTab.setContent(createReceiptHistoryPanel());
        
        // Tab 4: Reports / Dashboard
        Tab reportsTab = new Tab("   📊 Dashboard   ");
        reportsTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        reportsTab.setContent(createDashboardPanel());
        
        // Refresh dashboard when tab is selected
        reportsTab.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                reportsTab.setContent(createDashboardPanel());
            }
        });

        tabPane.getTabs().addAll(ordersTab, returnsTab, receiptHistoryTab, reportsTab);
        
        rootPane.setCenter(tabPane);

        Scene scene = new Scene(rootPane, 1400, 800);
        // Load our custom cashier background stylesheet if available
        try {
            java.net.URL cssUrl = getClass().getResource("/styles/cashier-background.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception ignored) {}
        // Add global stylesheet for TabPane if needed, or inline styles
        // For now, we rely on inline styles for components
        
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
        System.out.println("DEBUG: Main window shown");
        
        // Apply bundled Atlantafx fallback stylesheet (no external dependency required)
        applyAtlantafx(scene);

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
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 30, 20, 30));
        header.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");

        // Logo/Brand section
        VBox brandBox = new VBox(5);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        
        HBox logoRow = new HBox(12);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        
        ImageView logoImageView = null;
        try {
            File logoFile = new File("data/images/LOGO3.png");
            if (logoFile.exists()) {
                Image logoImage = new Image(logoFile.toURI().toString());
                logoImageView = new ImageView(logoImage);
                logoImageView.setFitWidth(48);
                logoImageView.setFitHeight(48);
                logoImageView.setPreserveRatio(true);
                // Make logo circular
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(24, 24, 24);
                logoImageView.setClip(clip);
            }
        } catch (Exception e) {
            System.err.println("Could not load logo: " + e.getMessage());
        }
        
        if (logoImageView == null) {
            Label coffeeIcon = new Label("☕");
            coffeeIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
            coffeeIcon.setTextFill(Color.web("#FFFFFF"));
            coffeeIcon.setStyle("-fx-background-color: #6366F1; -fx-padding: 10; -fx-background-radius: 50;");
            logoRow.getChildren().add(coffeeIcon);
        } else {
            logoRow.getChildren().add(logoImageView);
        }
        
        Label title = new Label("BREWISE Cashier");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#1F2937"));
        
        logoRow.getChildren().add(title);
        brandBox.getChildren().add(logoRow);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Date and cashier info
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_RIGHT);
        
        Label dateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        dateLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        dateLabel.setTextFill(Color.web("#6B7280"));
        
        if (currentCashierId != null && !currentCashierId.isEmpty()) {
            HBox cashierRow = new HBox(8);
            cashierRow.setAlignment(Pos.CENTER_RIGHT);
            
            Label cashierIcon = new Label("👤");
            cashierIcon.setFont(Font.font(12));
            
            Label cashierLabel = new Label(currentCashierId);
            cashierLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            cashierLabel.setTextFill(Color.web("#1F2937"));
            
            Label statusBadge = new Label("Logged In");
            statusBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            statusBadge.setTextFill(Color.web("#10B981"));
            statusBadge.setStyle("-fx-background-color: #D1FAE5; -fx-padding: 4 10; -fx-background-radius: 12;");
            
            cashierRow.getChildren().addAll(cashierIcon, cashierLabel, statusBadge);
            infoBox.getChildren().add(cashierRow);
        }
        
        infoBox.getChildren().add(dateLabel);
        
        header.getChildren().addAll(brandBox, spacer, infoBox);
        
        VBox wrapper = new VBox(header);
        return wrapper;
    }

    /**
     * Show a simple modal login dialog for cashier users.
     * Accepts two accounts: cashier1/cashier1 and cashier2/cashier2.
     * Returns true if login succeeded and sets `currentCashierId`.
     */
    private boolean showLoginDialog(Stage owner) {
        final Stage dialog = new Stage();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Cashier Login");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(18);
        grid.setPadding(new Insets(40));
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: linear-gradient(to bottom right, #ECF0F1, #D5DBDB); -fx-background-radius: 15;");

        // Logo Section
        ImageView logo = null;
        try {
            File logoFile = new File("data/images/LOGO3.png");
            if (logoFile.exists()) {
                Image logoImage = new Image(logoFile.toURI().toString());
                logo = new ImageView(logoImage);
                logo.setFitWidth(150);
                logo.setFitHeight(150);
                logo.setPreserveRatio(true);
                // Make logo circular
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(150, 150);
                clip.setArcWidth(150);
                clip.setArcHeight(150);
                logo.setClip(clip);
            }
        } catch (Exception e) {
            System.err.println("Could not load logo: " + e.getMessage());
        }

        // Title Section with better icon
        Label titleLabel = new Label("🔐 Cashier Login");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        titleLabel.setTextFill(Color.web("#2C3E50"));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);");
        
        Label subtitleLabel = new Label("🎯 Please sign in to continue");
        subtitleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 15));
        subtitleLabel.setTextFill(Color.web("#34495E"));
        
        Label userLbl = new Label("👤 Username:");
        userLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        userLbl.setTextFill(Color.web("#2C3E50"));
        
        TextField userField = new TextField();
        userField.setPromptText("Enter your username");
        userField.setStyle("-fx-font-size: 15px; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #3498DB; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: white;");
        userField.setPrefWidth(300);

        Label passLbl = new Label("🔑 Password:");
        passLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        passLbl.setTextFill(Color.web("#2C3E50"));
        
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password");
        passField.setStyle("-fx-font-size: 15px; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #3498DB; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: white;");
        passField.setPrefWidth(300);

        // Visible text field used when "Show Password" is toggled
        TextField passVisible = new TextField();
        passVisible.setPromptText("Enter your password");
        passVisible.setStyle("-fx-font-size: 15px; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #3498DB; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: white;");
        passVisible.setPrefWidth(300);
        passVisible.setManaged(false);
        passVisible.setVisible(false);

        CheckBox showPass = new CheckBox("Show Password");
        showPass.setFont(Font.font("Segoe UI", 12));
        showPass.setTextFill(Color.web("#5D4037"));

        Label msg = new Label();
        msg.setTextFill(javafx.scene.paint.Color.web("#D32F2F"));
        msg.setFont(Font.font("Segoe UI", 12));

        Button loginBtn = new Button("✅ Login");
        loginBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #27AE60, #229954); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 14 40; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        loginBtn.setPrefWidth(140);
        
        Button cancelBtn = new Button("❌ Cancel");
        cancelBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #E74C3C, #C0392B); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 14 40; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        cancelBtn.setPrefWidth(140);

        HBox actions = new HBox(10, loginBtn, cancelBtn);
        actions.setAlignment(Pos.CENTER);

        int row = 0;
        if (logo != null) {
            VBox logoBox = new VBox(logo);
            logoBox.setAlignment(Pos.CENTER);
            grid.add(logoBox, 0, row++, 2, 1);
        }
        grid.add(titleLabel, 0, row++, 2, 1);
        grid.add(subtitleLabel, 0, row++, 2, 1);
        grid.add(new Separator(), 0, row++, 2, 1);
        grid.add(userLbl, 0, 3);
        grid.add(userField, 0, 4, 2, 1);
        grid.add(passLbl, 0, 5);
        grid.add(passField, 0, 6, 2, 1);
        grid.add(passVisible, 0, 6, 2, 1);
        grid.add(showPass, 0, 7, 2, 1);
        grid.add(msg, 0, 8, 2, 1);
        grid.add(actions, 0, 9, 2, 1);

        // Quick login when Enter pressed in password (or visible password field)
        passField.setOnAction(e -> loginBtn.fire());
        passVisible.setOnAction(e -> loginBtn.fire());

        // Toggle password visibility
        showPass.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                passVisible.setText(passField.getText());
                passVisible.setVisible(true);
                passVisible.setManaged(true);
                passField.setVisible(false);
                passField.setManaged(false);
            } else {
                passField.setText(passVisible.getText());
                passField.setVisible(true);
                passField.setManaged(true);
                passVisible.setVisible(false);
                passVisible.setManaged(false);
            }
        });

        loginBtn.setOnAction(e -> {
            String user = userField.getText() == null ? "" : userField.getText().trim();
            String pass = showPass.isSelected() ? passVisible.getText() : passField.getText();
            pass = pass == null ? "" : pass;
            try {
                java.util.List<com.coffeeshop.model.CashierAccount> accounts = com.coffeeshop.service.PersistenceManager.loadAccounts();
                com.coffeeshop.model.CashierAccount match = null;
                for (com.coffeeshop.model.CashierAccount a : accounts) {
                    if (a.getUsername().equals(user) && a.getPassword().equals(pass)) { 
                        match = a; 
                        break; 
                    }
                }
                if (match != null && match.isActive()) {
                    currentCashierId = match.getUsername();
                    System.out.println("DEBUG: Cashier logged in: " + currentCashierId);
                    dialog.close();
                } else if (match != null && !match.isActive()) {
                    msg.setText("Account deactivated. Contact admin.");
                } else {
                    msg.setText("Invalid username or password.");
                }
            } catch (Exception ex) {
                msg.setText("Login error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        cancelBtn.setOnAction(e -> {
            dialog.close();
        });

        Scene scene = new Scene(grid, 550, 700);
        scene.setFill(Color.web("#F4F1EA"));
        // attach fallback stylesheet to login dialog if available
        try {
            java.net.URL u = getClass().getResource("/styles/atlantafx-fallback.css");
            if (u != null) scene.getStylesheets().add(u.toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();

        return currentCashierId != null;
    }

    // Load bundled AtlantisFX fallback stylesheet if present and apply to the scene.
    private void applyAtlantafx(Scene scene) {
        if (scene == null) return;
        try {
            java.net.URL u = getClass().getResource("/styles/atlantafx-fallback.css");
            if (u != null) scene.getStylesheets().add(u.toExternalForm());
        } catch (Exception ignored) {}
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
        return generateReceiptWithOrderType(order, customerName, orderType, 0.0, 0.0);
    }

    private String generateReceiptWithOrderType(Order order, String customerName, String orderType, double cashPaid, double change) {
        StringBuilder receipt = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("         BREWISE COFFEE SHOP\n");
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("Order ID: ").append(order.getOrderId()).append("\n");
        receipt.append("Customer: ").append(customerName != null ? customerName : "Walk-in").append("\n");
        receipt.append("Order Type: ").append(orderType).append("\n");
        if (currentCashierId != null && !currentCashierId.isEmpty()) {
            receipt.append("Cashier: ").append(currentCashierId).append("\n");
        }
        receipt.append("Date: ").append(java.time.LocalDateTime.now().format(formatter)).append("\n");
        receipt.append("═══════════════════════════════════════\n\n");
        
        receipt.append("ITEMS:\n");
        receipt.append("───────────────────────────────────────\n");
        
        for (OrderItem item : order.getItems()) {
            receipt.append(String.format("%-20s  ₱%.2f\n", 
                item.getProduct().getName(), item.getSubtotal() / item.getQuantity()));
            
            if (item.getSize() != null && !item.getSize().isEmpty()) {
                receipt.append("  Size: ").append(item.getSize()).append(" (+₱").append(String.format("%.2f", item.getSizeCost())).append(")\n");
            }
            
            if (item.getTemperature() != null && !item.getTemperature().isEmpty()) {
                receipt.append("  Temperature: ").append(item.getTemperature()).append("\n");
            }
            
            if (item.getSugarLevel() > 0) {
                receipt.append("  Sugar Level: ").append(item.getSugarLevel()).append("%\n");
            }
            
            if (item.getAddOns() != null && !item.getAddOns().isEmpty()) {
                receipt.append("  Add-ons: ").append(item.getAddOns()).append("\n");
            }
            if (item.getSpecialRequest() != null && !item.getSpecialRequest().isEmpty()) {
                receipt.append("  Remarks: ").append(item.getSpecialRequest()).append("\n");
            }
            receipt.append("\n");
        }
        
        receipt.append("───────────────────────────────────────\n");
        receipt.append(String.format("Subtotal:         ₱%.2f\n", order.getTotalAmount()));
        receipt.append(String.format("TOTAL:            ₱%.2f\n", order.getTotalAmount()));

        // Show cash paid and change immediately below the TOTAL
        receipt.append("───────────────────────────────────────\n");
        receipt.append(String.format("Cash Paid: ₱%.2f\n", cashPaid));
        receipt.append(String.format("Change:    ₱%.2f\n", change));
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

                        // Avoid clearing or reconstructing the dashboard VBox here —
                        // removing its children on the FX thread can produce a
                        // brief empty view (observed as the dashboard disappearing)
                        // while the scheduler is running. Instead, keep dashboard
                        // structure intact and let the user-triggered refresh button
                        // update visible metrics. We still update the underlying
                        // data models (`receiptHistory`, pending lists) above.
                        // (No-op for dashboardPanel to prevent flicker/vanish.)
                        if (dashboardPanel != null) {
                            // Intentionally left blank to avoid UI disruption.
                        }
                        // Check cashier account active status from persisted accounts for realtime deactivation
                        try {
                            if (currentCashierId != null && !currentCashierId.isEmpty()) {
                                java.util.List<com.coffeeshop.model.CashierAccount> accounts = com.coffeeshop.service.PersistenceManager.loadAccounts();
                                com.coffeeshop.model.CashierAccount found = null;
                                for (com.coffeeshop.model.CashierAccount a : accounts) {
                                    if (a.getUsername().equals(currentCashierId)) { found = a; break; }
                                }
                                if (found == null || !found.isActive()) {
                                    // Force logout and show login dialog
                                    Alert a = new Alert(Alert.AlertType.WARNING);
                                    a.setTitle("Logged Out");
                                    a.setHeaderText(null);
                                    a.setContentText("Your account has been deactivated. Please login with an active account.");
                                    a.showAndWait();

                                    // clear current id and prompt re-login
                                    currentCashierId = null;
                                    if (rootPane != null) rootPane.setTop(createHeader());

                                    boolean ok = showLoginDialog(primaryStageRef);
                                    if (!ok) {
                                        javafx.application.Platform.exit();
                                    } else {
                                        // successful login: refresh header
                                        if (rootPane != null) rootPane.setTop(createHeader());
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("Account check failed: " + ex.getMessage());
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
                payAndStartPreparing(po); // Updated: now handles customer name + payment + receipt
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

    private String payAndQueue(PendingOrder po, String customerName) {
        // Build an Order object from PendingOrder to run checkout logic
        Order order = new Order(po.getOrderId());
        if (po.getOrderTime() != null) order.setOrderTime(po.getOrderTime());
        for (PendingOrder.OrderItemData item : po.getItems()) {
            Product product = store.getProducts().stream().filter(p -> p.getName().equals(item.productName)).findFirst().orElse(null);
            if (product != null) {
                OrderItem oi = new OrderItem(product, item.quantity, item.temperature, item.sugarLevel);
                // propagate add-ons and special requests from pending order item data
                try {
                    if (item.addOns != null && !item.addOns.isEmpty()) oi.setAddOns(item.addOns);
                    oi.setAddOnsCost(item.addOnsCost);
                    if (item.specialRequest != null && !item.specialRequest.isEmpty()) oi.setSpecialRequest(item.specialRequest);
                } catch (Exception ignored) {}
                order.addItem(oi);
            }
        }

        // Validate stock and process checkout
        if (!store.isStockSufficient(order)) {
            showAlert("Stock Error", "Insufficient stock for some items.", Alert.AlertType.ERROR);
            return "";
        }
        if (!store.isInventorySufficient(order)) {
            showAlert("Ingredient Error", "Insufficient ingredients for some items.", Alert.AlertType.ERROR);
            return "";
        }

        store.checkoutBasket(order);

        // Generate and save receipt with cashier name
        String orderType = orderTypes.getOrDefault(po.getOrderId(), "Dine In");
        String receiptContent = generateReceiptWithOrderType(order, customerName, orderType, lastCashReceived, lastChange);

        String receiptId = "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // Use recorded cash/payment values from the payment dialog so receipt shows real cash/change
        Receipt receipt = new Receipt(receiptId, order.getOrderId(), customerName, order.getTotalAmount(), lastCashReceived, lastChange);
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
        
        return receiptContent;
    }

    private void startMaking(PendingOrder po) {
        po.setStatus(PendingOrder.STATUS_PREPARING);
        TextDatabase.savePendingOrder(po);
        loadPendingOrdersFromFile();
    }

    // Pay the pending order and immediately mark it as preparing (ready for production)
    private void payAndStartPreparing(PendingOrder po) {
        if (po == null) return;
        
        // Step 1: Ask for customer name (optional)
        String customerName = askForCustomerName(po.getOrderId());
        // Save the entered customer name to the PendingOrder so it persists
        // and is visible in completed/receipt views.
        po.setCustomerName(customerName);
        orderCustomerNames.put(po.getOrderId(), customerName);
        // Persist the updated PendingOrder immediately so the name is stored
        // even if later steps fail or another thread reloads orders.
        try {
            TextDatabase.savePendingOrder(po);
        } catch (Exception ex) {
            System.err.println("Warning: failed to persist customer name for order " + po.getOrderId() + ": " + ex.getMessage());
        }
        
        // Step 2: Show cash payment dialog
        boolean paymentSuccess = showCashPaymentDialog(po);
        if (!paymentSuccess) {
            return; // User cancelled payment
        }
        
        // Step 3: Process payment (deduct stock, create receipt, mark as PAID)
        String receiptContent = payAndQueue(po, customerName);
        // Step 4: Show receipt
        showReceipt(receiptContent);
        // Step 5: Move to preparing state
        startMaking(po);
    }
    
    private String askForCustomerName(String orderId) {
        Dialog<String> nameDialog = new Dialog<>();
        nameDialog.setTitle("Customer Information");
        nameDialog.setHeaderText("Enter customer name (optional)");
        
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPadding(new Insets(25));
        grid.setStyle("-fx-background-color: linear-gradient(to bottom, #FFFFFF, #F8F9FA);");
        
        Label nameLabel = new Label("👤 Customer Name:");
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.web("#2C3E50"));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Enter name or leave empty for 'Guest'");
        nameField.setPrefWidth(350);
        nameField.setStyle("-fx-font-size: 15px; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #3498DB; -fx-border-width: 2; -fx-border-radius: 8;");
        
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        
        nameDialog.getDialogPane().setContent(grid);
        
        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        nameDialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);
        
        nameDialog.setResultConverter(buttonType -> {
            if (buttonType == okType) {
                String name = nameField.getText();
                return (name == null || name.trim().isEmpty()) ? "Guest" : name.trim();
            }
            return "Guest";
        });
        
        return nameDialog.showAndWait().orElse("Guest");
    }
    
    private void showReceipt(String receiptContent) {
        Dialog<Void> receiptDialog = new Dialog<>();
        receiptDialog.setTitle("Receipt");
        receiptDialog.setHeaderText(null);
        
        TextArea receiptArea = new TextArea();
        receiptArea.setText(receiptContent);
        receiptArea.setEditable(false);
        receiptArea.setWrapText(true);
        receiptArea.setFont(Font.font("Consolas", 13));
        receiptArea.setPrefWidth(550);
        receiptArea.setPrefHeight(450);
        receiptArea.setStyle("-fx-control-inner-background: #F8F9FA; -fx-border-color: #3498DB; -fx-border-width: 2; -fx-border-radius: 8;");
        
        receiptDialog.getDialogPane().setContent(receiptArea);
        receiptDialog.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom, #FFFFFF, #F0F3F4);");
        receiptDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        
        Button okButton = (Button) receiptDialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: linear-gradient(to right, #27AE60, #229954); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 12 30; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        
        receiptDialog.showAndWait();
    }
    
    private boolean showCashPaymentDialog(PendingOrder po) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Cash Payment");
        dialog.setHeaderText("Process Payment for Order: " + po.getOrderId());
        
        // Dialog content
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPadding(new Insets(25));
        grid.setStyle("-fx-background-color: linear-gradient(to bottom, #FFFFFF, #F8F9FA); -fx-background-radius: 12;");
        
        // Total amount label
        Label totalLabel = new Label("💰 Total Amount:");
        totalLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        totalLabel.setTextFill(Color.web("#2C3E50"));
        
        Label totalValue = new Label("₱" + String.format("%.2f", po.getTotalAmount()));
        totalValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        totalValue.setTextFill(Color.web("#E74C3C"));
        totalValue.setStyle("-fx-padding: 10; -fx-background-color: #FADBD8; -fx-background-radius: 8;");
        
        // Cash received field
        Label cashLabel = new Label("💵 Cash Received:");
        cashLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        cashLabel.setTextFill(Color.web("#2C3E50"));
        
        TextField cashField = new TextField();
        cashField.setPromptText("Enter cash amount");
        cashField.setPrefWidth(250);
        cashField.setStyle("-fx-font-size: 18px; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #3498DB; -fx-border-width: 2; -fx-border-radius: 8;");
        
        // Change label
        Label changeLabel = new Label("💸 Change:");
        changeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        changeLabel.setTextFill(Color.web("#2C3E50"));
        
        Label changeValue = new Label("₱0.00");
        changeValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        changeValue.setTextFill(Color.web("#27AE60"));
        changeValue.setStyle("-fx-padding: 10; -fx-background-color: #D5F4E6; -fx-background-radius: 8;");
        
        // Error message label
        Label errorLabel = new Label("");
        errorLabel.setFont(Font.font("Segoe UI", 12));
        errorLabel.setTextFill(Color.web("#D32F2F"));
        errorLabel.setVisible(false);
        
        // Calculate change when cash amount changes
        cashField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (newVal == null || newVal.trim().isEmpty()) {
                    changeValue.setText("₱0.00");
                    errorLabel.setVisible(false);
                    return;
                }
                
                double cashReceived = Double.parseDouble(newVal.trim());
                double total = po.getTotalAmount();
                double change = cashReceived - total;
                
                if (change < 0) {
                    changeValue.setText("₱0.00");
                    changeValue.setTextFill(Color.web("#D32F2F"));
                    errorLabel.setText("Insufficient cash amount!");
                    errorLabel.setVisible(true);
                } else {
                    changeValue.setText("₱" + String.format("%.2f", change));
                    changeValue.setTextFill(Color.web("#4CAF50"));
                    errorLabel.setVisible(false);
                }
            } catch (NumberFormatException e) {
                changeValue.setText("₱0.00");
                errorLabel.setText("Invalid amount!");
                errorLabel.setVisible(true);
            }
        });
        
        // Add components to grid
        grid.add(totalLabel, 0, 0);
        grid.add(totalValue, 1, 0);
        grid.add(new Separator(), 0, 1, 2, 1);
        grid.add(cashLabel, 0, 2);
        grid.add(cashField, 1, 2);
        grid.add(changeLabel, 0, 3);
        grid.add(changeValue, 1, 3);
        grid.add(errorLabel, 0, 4, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Add buttons
        ButtonType confirmButtonType = new ButtonType("Confirm Payment", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);
        
        // Style the buttons
        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setStyle("-fx-background-color: linear-gradient(to right, #27AE60, #229954); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 12 30; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelButton.setStyle("-fx-background-color: linear-gradient(to right, #95A5A6, #7F8C8D); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 12 30; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        
        // Disable confirm button initially
        confirmButton.setDisable(true);
        
        // Enable confirm button only when cash is sufficient
        cashField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (newVal == null || newVal.trim().isEmpty()) {
                    confirmButton.setDisable(true);
                    return;
                }
                double cashReceived = Double.parseDouble(newVal.trim());
                confirmButton.setDisable(cashReceived < po.getTotalAmount());
            } catch (NumberFormatException e) {
                confirmButton.setDisable(true);
            }
        });
        
        // Focus on cash field
        Platform.runLater(() -> cashField.requestFocus());
        
        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                try {
                    double cashReceived = Double.parseDouble(cashField.getText().trim());
                    if (cashReceived >= po.getTotalAmount()) {
                        // record amounts for receipt
                        lastCashReceived = cashReceived;
                        lastChange = cashReceived - po.getTotalAmount();
                        return true;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        });
        
        // Show dialog and return result
        return dialog.showAndWait().orElse(false);
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
        // Keep customer name and order type mapping so completed orders still show correct customer
        // (do not remove from orderCustomerNames/orderTypes)
        showAlert("Completed", "Order marked as completed (picked up).", Alert.AlertType.INFORMATION);
    }

    private Node createDashboardPanel() {
        dashboardPanel = new VBox(30);
        dashboardPanel.setPadding(new Insets(30));
        dashboardPanel.setStyle("-fx-background-color: #f8f9fa;");
        dashboardPanel.getStyleClass().add("panel-card");

        // Header with title and refresh button
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label("Sales Overview");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#1a1a1a"));
        Label subtitleLabel = new Label("Real-time overview of today's performance");
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setTextFill(Color.web("#6c757d"));
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button refreshBtn = new Button("🔄 Refresh Data");
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-size: 14px;");
        
        headerBox.getChildren().addAll(titleBox, spacer, refreshBtn);

        Label dashboardTitle = new Label("Sales Dashboard");
        dashboardTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 20));
        dashboardTitle.setTextFill(Color.web("#1a1a1a"));
        dashboardTitle.setPadding(new Insets(10, 0, 10, 0));

        // Statistics Cards with gradient backgrounds
        HBox statsBox = new HBox(20);
        
        VBox totalSalesCard = new VBox(15);
        totalSalesCard.setPadding(new Insets(30));
        totalSalesCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");
        totalSalesCard.setMaxWidth(Double.MAX_VALUE);
        
        VBox todayRevenueCard = new VBox(15);
        todayRevenueCard.setPadding(new Insets(30));
        todayRevenueCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");
        todayRevenueCard.setMaxWidth(Double.MAX_VALUE);
        
        VBox ordersProcessedCard = new VBox(15);
        ordersProcessedCard.setPadding(new Insets(30));
        ordersProcessedCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");
        ordersProcessedCard.setMaxWidth(Double.MAX_VALUE);
        
        // Initialize with placeholder content
        Label loadingLabel1 = new Label("Loading...");
        loadingLabel1.setFont(Font.font("Segoe UI", 14));
        loadingLabel1.setTextFill(Color.web("#6c757d"));
        totalSalesCard.getChildren().add(loadingLabel1);
        
        Label loadingLabel2 = new Label("Loading...");
        loadingLabel2.setFont(Font.font("Segoe UI", 14));
        loadingLabel2.setTextFill(Color.web("#6c757d"));
        todayRevenueCard.getChildren().add(loadingLabel2);
        
        Label loadingLabel3 = new Label("Loading...");
        loadingLabel3.setFont(Font.font("Segoe UI", 14));
        loadingLabel3.setTextFill(Color.web("#6c757d"));
        ordersProcessedCard.getChildren().add(loadingLabel3);
        
        statsBox.getChildren().addAll(totalSalesCard, todayRevenueCard, ordersProcessedCard);
        HBox.setHgrow(totalSalesCard, Priority.ALWAYS);
        HBox.setHgrow(todayRevenueCard, Priority.ALWAYS);
        HBox.setHgrow(ordersProcessedCard, Priority.ALWAYS);
        
        // Top Selling Products Section
        HBox contentBox = new HBox(20);
        
        // Left: Top Products
        VBox topProductsBox = new VBox(15);
        topProductsBox.setPadding(new Insets(25));
        topProductsBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");
        topProductsBox.setMaxWidth(Double.MAX_VALUE);
        
        HBox topHeader = new HBox(10);
        topHeader.setAlignment(Pos.CENTER_LEFT);
        Label topIcon = new Label("🏆");
        topIcon.setFont(Font.font(20));
        Label topProductsLabel = new Label("Top Selling Products");
        topProductsLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        topProductsLabel.setTextFill(Color.web("#1a1a1a"));
        topHeader.getChildren().addAll(topIcon, topProductsLabel);
        
        ListView<String> topList = new ListView<>();
        topList.setPrefHeight(300);
        topList.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        topProductsBox.getChildren().addAll(topHeader, topList);
        HBox.setHgrow(topProductsBox, Priority.ALWAYS);
        
        // Right: Product Performance (placeholder for chart)
        VBox perfBox = new VBox(15);
        perfBox.setPadding(new Insets(25));
        perfBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");
        perfBox.setMaxWidth(Double.MAX_VALUE);
        
        Label perfLabel = new Label("Product Performance");
        perfLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        perfLabel.setTextFill(Color.web("#1a1a1a"));
        
        // Create a bar chart for product performance
        CategoryAxis perfXAxis = new CategoryAxis();
        NumberAxis perfYAxis = new NumberAxis();
        perfXAxis.setLabel("");
        perfYAxis.setLabel("Sold");
        BarChart<String, Number> perfChart = new BarChart<>(perfXAxis, perfYAxis);
        perfChart.setLegendVisible(false);
        perfChart.setTitle("");
        perfChart.setPrefHeight(300);
        perfChart.setAnimated(false);
        perfChart.setStyle("-fx-background-color: transparent;");

        perfBox.getChildren().addAll(perfLabel, perfChart);
        HBox.setHgrow(perfBox, Priority.ALWAYS);
        
        contentBox.getChildren().addAll(topProductsBox, perfBox);

        refreshBtn.setOnAction(e -> {
            // refresh data
            loadReceiptHistory();
            List<com.coffeeshop.model.ItemRecord> items = com.coffeeshop.service.TextDatabase.loadAllItems();
            List<Map.Entry<String,Integer>> top = com.coffeeshop.service.SalesAnalytics.getTopProducts(items, 8);
            topList.getItems().clear();
            int rank = 1;
            for (Map.Entry<String,Integer> en : top) {
                topList.getItems().add(String.format("%d    %s                                        %d sold", rank++, en.getKey(), en.getValue()));
            }

            // Update product performance chart
            try {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                for (Map.Entry<String,Integer> en : top) {
                    series.getData().add(new XYChart.Data<>(en.getKey(), en.getValue()));
                }
                perfChart.getData().clear();
                perfChart.getData().add(series);
                // minor attempt to adjust label rotation for long names
                perfChart.getXAxis().setTickLabelRotation( -20 );
            } catch (Exception ignore) {}

            double total = com.coffeeshop.service.SalesAnalytics.getTotalSales(receiptHistory);
            double today = com.coffeeshop.service.SalesAnalytics.getTotalSalesForDate(receiptHistory, LocalDate.now());
            long ordersToday = com.coffeeshop.service.SalesAnalytics.getOrderCountForDate(receiptHistory, LocalDate.now());
            long ordersTotal = receiptHistory.size();

            // Update total sales card
            totalSalesCard.getChildren().clear();
            totalSalesCard.setPadding(new Insets(30));
            totalSalesCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");
            totalSalesCard.setMaxWidth(Double.MAX_VALUE);
            
            StackPane iconCircle1 = new StackPane();
            iconCircle1.setPrefSize(60, 60);
            iconCircle1.setStyle("-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2); -fx-background-radius: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);");
            Label icon1 = new Label("💵");
            icon1.setFont(Font.font(28));
            iconCircle1.getChildren().add(icon1);
            
            Label title1 = new Label("Total Sales (All Time)");
            title1.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 13));
            title1.setTextFill(Color.web("#6c757d"));
            
            Label value1 = new Label(String.format("₱%.2f", total));
            value1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
            value1.setTextFill(Color.web("#1a1a1a"));
            
            totalSalesCard.getChildren().addAll(iconCircle1, title1, value1);
            
            // Update today revenue card
            todayRevenueCard.getChildren().clear();
            todayRevenueCard.setPadding(new Insets(30));
            todayRevenueCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");
            todayRevenueCard.setMaxWidth(Double.MAX_VALUE);
            
            StackPane iconCircle2 = new StackPane();
            iconCircle2.setPrefSize(60, 60);
            iconCircle2.setStyle("-fx-background-color: linear-gradient(to bottom right, #0ba360, #3cba92); -fx-background-radius: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);");
            Label icon2 = new Label("📈");
            icon2.setFont(Font.font(28));
            iconCircle2.getChildren().add(icon2);
            
            Label title2 = new Label("Today's Revenue");
            title2.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 13));
            title2.setTextFill(Color.web("#6c757d"));
            
            Label value2 = new Label(String.format("₱%.2f", today));
            value2.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
            value2.setTextFill(Color.web("#1a1a1a"));
            
            Label subtitle2 = new Label("+12% vs yesterday");
            subtitle2.setFont(Font.font("Segoe UI", 12));
            subtitle2.setTextFill(Color.web("#0ba360"));
            subtitle2.setStyle("-fx-background-color: #d4edda; -fx-padding: 4 10; -fx-background-radius: 12;");
            
            todayRevenueCard.getChildren().addAll(iconCircle2, title2, value2, subtitle2);
            
            // Update orders processed card
            ordersProcessedCard.getChildren().clear();
            ordersProcessedCard.setPadding(new Insets(30));
            ordersProcessedCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");
            ordersProcessedCard.setMaxWidth(Double.MAX_VALUE);
            
            StackPane iconCircle3 = new StackPane();
            iconCircle3.setPrefSize(60, 60);
            iconCircle3.setStyle("-fx-background-color: linear-gradient(to bottom right, #4facfe, #00f2fe); -fx-background-radius: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);");
            Label icon3 = new Label("🛍️");
            icon3.setFont(Font.font(28));
            iconCircle3.getChildren().add(icon3);
            
            Label title3 = new Label("Orders Processed");
            title3.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 13));
            title3.setTextFill(Color.web("#6c757d"));
            
            Label value3 = new Label(String.valueOf(ordersTotal));
            value3.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
            value3.setTextFill(Color.web("#1a1a1a"));
            
            Label subtitle3 = new Label(ordersToday + " Today");
            subtitle3.setFont(Font.font("Segoe UI", 12));
            subtitle3.setTextFill(Color.web("#4facfe"));
            subtitle3.setStyle("-fx-background-color: #e7f5ff; -fx-padding: 4 10; -fx-background-radius: 12;");
            
            ordersProcessedCard.getChildren().addAll(iconCircle3, title3, value3, subtitle3);
        });

        // initial populate
        refreshBtn.fire();

        dashboardPanel.getChildren().addAll(headerBox, dashboardTitle, statsBox, contentBox);
        
        // Wrap dashboardPanel in ScrollPane to allow scrolling when content exceeds viewport
        ScrollPane scrollPane = new ScrollPane(dashboardPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f8f9fa; -fx-control-inner-background: #f8f9fa;");
        return scrollPane;
    }
    
    // ==================== ORDER QUEUE PANEL ====================
    
    private javafx.scene.control.ScrollPane createOrderQueuePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.getStyleClass().add("panel-card");
        panel.setStyle("-fx-background-color: #F3F4F6;");
        
        // Header section
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        
        Label title = new Label("Order Management");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#111827"));
        
        Label subtitle = new Label("Track orders through each stage of preparation");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#6B7280"));
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        headerBox.getChildren().addAll(title, headerSpacer);
        
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
                actionBtn.getStyleClass().add("cashier-accent");
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
                    actionBtn.setText("💳 Pay");
                    actionBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
                    removeBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 12; -fx-background-radius: 8; -fx-cursor: hand;");
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
            private final Button completeBtn = new Button("✅ Complete");
            {
                completeBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
                completeBtn.getStyleClass().add("cashier-accent");
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

        // Modern column headers with badges
        HBox pendingHeader = new HBox(10);
        pendingHeader.setAlignment(Pos.CENTER_LEFT);
        pendingHeader.setPadding(new Insets(15));
        pendingHeader.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12 12 0 0; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 2 0;");
        
        Label pendingIcon = new Label("⏳");
        pendingIcon.setFont(Font.font(18));
        Label pendingLabel = new Label("Pending");
        pendingLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        pendingLabel.setTextFill(Color.web("#374151"));
        Label pendingBadge = new Label("0");
        pendingBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        pendingBadge.setTextFill(Color.web("#F59E0B"));
        pendingBadge.setStyle("-fx-background-color: #FEF3C7; -fx-padding: 4 12; -fx-background-radius: 12;");
        pendingList.addListener((javafx.collections.ListChangeListener<PendingOrder>) c -> 
            pendingBadge.setText(String.valueOf(pendingList.size())));
        pendingHeader.getChildren().addAll(pendingIcon, pendingLabel, pendingBadge);
        
        HBox preparingHeader = new HBox(10);
        preparingHeader.setAlignment(Pos.CENTER_LEFT);
        preparingHeader.setPadding(new Insets(15));
        preparingHeader.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12 12 0 0; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 2 0;");
        
        Label preparingIcon = new Label("⏱️");
        preparingIcon.setFont(Font.font(18));
        Label preparingLabel = new Label("Preparing");
        preparingLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        preparingLabel.setTextFill(Color.web("#374151"));
        Label preparingBadge = new Label("0");
        preparingBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        preparingBadge.setTextFill(Color.web("#3B82F6"));
        preparingBadge.setStyle("-fx-background-color: #DBEAFE; -fx-padding: 4 12; -fx-background-radius: 12;");
        preparingList.addListener((javafx.collections.ListChangeListener<PendingOrder>) c -> 
            preparingBadge.setText(String.valueOf(preparingList.size())));
        preparingHeader.getChildren().addAll(preparingIcon, preparingLabel, preparingBadge);
        
        HBox completedHeader = new HBox(10);
        completedHeader.setAlignment(Pos.CENTER_LEFT);
        completedHeader.setPadding(new Insets(15));
        completedHeader.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12 12 0 0; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 2 0;");
        
        Label completedIcon = new Label("✅");
        completedIcon.setFont(Font.font(18));
        Label completedLabel = new Label("Completed");
        completedLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        completedLabel.setTextFill(Color.web("#374151"));
        Label completedBadge = new Label("0");
        completedBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        completedBadge.setTextFill(Color.web("#10B981"));
        completedBadge.setStyle("-fx-background-color: #D1FAE5; -fx-padding: 4 12; -fx-background-radius: 12;");
        completedList.addListener((javafx.collections.ListChangeListener<PendingOrder>) c -> 
            completedBadge.setText(String.valueOf(completedList.size())));
        completedHeader.getChildren().addAll(completedIcon, completedLabel, completedBadge);
        
        VBox pendingBox = new VBox(0);
        pendingBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        pendingTable.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        pendingBox.getChildren().addAll(pendingHeader, pendingTable);
        
        VBox preparingBox = new VBox(0);
        preparingBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        preparingTable.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        preparingBox.getChildren().addAll(preparingHeader, preparingTable);
        
        VBox completedBox = new VBox(0);
        completedBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        completedTable.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        completedBox.getChildren().addAll(completedHeader, completedTable);

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
        
        // Order details panel with modern design
        VBox detailsPanel = new VBox(15);
        detailsPanel.setPadding(new Insets(20));
        detailsPanel.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        detailsPanel.getStyleClass().add("panel-card");
        detailsPanel.setPrefHeight(380);
        detailsPanel.setMinHeight(300);

        Label detailsTitle = new Label("📄 Receipt View");
        detailsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        detailsTitle.setTextFill(Color.web("#111827"));

        TextArea orderDetailsArea = new TextArea();
        orderDetailsArea.setEditable(false);
        orderDetailsArea.setWrapText(true);
        // Use larger monospace font for alignment and readability
        orderDetailsArea.setFont(Font.font("Consolas", 15));
        orderDetailsArea.setText("👆 Select an order to view details...");
        orderDetailsArea.setPrefHeight(320);
        orderDetailsArea.setMaxHeight(Double.MAX_VALUE);
        orderDetailsArea.setPrefWidth(Double.MAX_VALUE);
        orderDetailsArea.setStyle("-fx-control-inner-background: #F9FAFB; -fx-background-color: #F9FAFB; -fx-font-size: 13px; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 8;");
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
        
        HBox activeOrdersBox = new HBox(12);
        activeOrdersBox.setAlignment(Pos.CENTER_LEFT);
        activeOrdersBox.setPadding(new Insets(15));
        activeOrdersBox.setStyle("-fx-background-color: #EEF2FF; -fx-background-radius: 10;");
        
        Label activeIcon = new Label("📈");
        activeIcon.setFont(Font.font(16));
        
        Label queueCount = new Label("0 Active Orders");
        queueCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        queueCount.setTextFill(Color.web("#6366F1"));
        
        activeOrdersBox.getChildren().addAll(activeIcon, queueCount);
        // update count based on all three lists
        javafx.collections.ListChangeListener<PendingOrder> updateCount = c -> {
            int total = pendingList.size() + preparingList.size() + completedList.size();
            queueCount.setText(total + " Active Orders");
        };
        pendingList.addListener(updateCount);
        preparingList.addListener(updateCount);
        completedList.addListener(updateCount);
        
        panel.getChildren().addAll(activeOrdersBox, detailsPanel);

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
    
    
    
    // ==================== RETURNS PANEL ====================
    
    private ScrollPane createReturnsPanel() {
        VBox panel = new VBox(30);
        panel.setPadding(new Insets(40));
        panel.setStyle("-fx-background-color: #f8f9fa;");
        panel.setAlignment(Pos.TOP_CENTER);
        
        // Header
        Label title = new Label("↩ Return / Exchange Items");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#ff9800"));
        
        Label subtitle = new Label("Enter the Order ID to process return or exchange");
        subtitle.setFont(Font.font("Segoe UI", 16));
        subtitle.setTextFill(Color.web("#6c757d"));
        
        // Main card
        VBox card = new VBox(25);
        card.setMaxWidth(600);
        card.setPadding(new Insets(40));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);");
        card.setAlignment(Pos.CENTER);
        
        // Icon
        Label icon = new Label("📋");
        icon.setFont(Font.font(80));
        
        // Order ID input
        VBox inputBox = new VBox(10);
        inputBox.setAlignment(Pos.CENTER);
        
        Label inputLabel = new Label("Order ID:");
        inputLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        inputLabel.setTextFill(Color.web("#374151"));
        
        TextField orderIdField = new TextField();
        orderIdField.setPromptText("e.g., e10b8209 or 915a2be6");
        orderIdField.setMaxWidth(400);
        orderIdField.setStyle("-fx-font-size: 18px; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-width: 2; -fx-border-radius: 8;");
        orderIdField.setOnAction(e -> processReturnByOrderId(orderIdField.getText().trim()));
        
        // Instructions
        Label instructions = new Label("💡 Tip: The Order ID can be found on the customer's receipt");
        instructions.setFont(Font.font("Segoe UI", 12));
        instructions.setTextFill(Color.web("#9ca3af"));
        instructions.setWrapText(true);
        instructions.setMaxWidth(400);
        instructions.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        inputBox.getChildren().addAll(inputLabel, orderIdField, instructions);
        
        // Process button
        Button processBtn = new Button("Process Return/Exchange");
        processBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 8; -fx-cursor: hand;");
        processBtn.setOnMouseEntered(e -> processBtn.setStyle("-fx-background-color: #f57c00; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 8; -fx-cursor: hand;"));
        processBtn.setOnMouseExited(e -> processBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 8; -fx-cursor: hand;"));
        processBtn.setOnAction(e -> processReturnByOrderId(orderIdField.getText().trim()));
        
        // Info box
        VBox infoBox = new VBox(10);
        infoBox.setMaxWidth(500);
        infoBox.setPadding(new Insets(20));
        infoBox.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 10; -fx-border-color: #ff9800; -fx-border-width: 1; -fx-border-radius: 10;");
        
        Label infoTitle = new Label("⏰ Return Policy");
        infoTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        infoTitle.setTextFill(Color.web("#e65100"));
        
        Label infoText = new Label("• Returns are accepted within 2 hours of purchase\n• Items must be in original condition\n• Exchange for same or different items available\n• Refund will be processed for eligible returns");
        infoText.setFont(Font.font("Segoe UI", 12));
        infoText.setTextFill(Color.web("#6c757d"));
        infoText.setWrapText(true);
        
        infoBox.getChildren().addAll(infoTitle, infoText);
        
        card.getChildren().addAll(icon, inputBox, processBtn);
        panel.getChildren().addAll(title, subtitle, card, infoBox);
        
        // Wrap in ScrollPane for scrollability
        ScrollPane scrollPane = new ScrollPane(panel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f8f9fa; -fx-background: #f8f9fa;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        return scrollPane;
    }
    
    private void processReturnByOrderId(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Order ID Required");
            alert.setHeaderText("Please enter an Order ID");
            alert.setContentText("You must provide a valid Order ID to process a return or exchange.");
            alert.showAndWait();
            return;
        }
        
        // Find the receipt by order ID
        Receipt receipt = receiptHistory.stream()
            .filter(r -> r.getOrderId().equalsIgnoreCase(orderId))
            .findFirst()
            .orElse(null);
        
        if (receipt == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Order Not Found");
            alert.setHeaderText("Invalid Order ID");
            alert.setContentText("No order found with ID: " + orderId + "\n\nPlease check the Order ID and try again.");
            alert.showAndWait();
            return;
        }
        
        // Open the return/exchange dialog
        showReturnExchangeDialog(receipt);
    }
    
    // ==================== RECEIPT HISTORY PANEL ====================
    
    private VBox createReceiptHistoryPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color: #f8f9fa;");
        
        // Header
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Transaction History");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#1a1a1a"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label dateLabel = new Label("Monday, November 24, 2025");
        dateLabel.setFont(Font.font("Segoe UI", 14));
        dateLabel.setTextFill(Color.web("#6c757d"));
        
        headerBox.getChildren().addAll(title, spacer, dateLabel);
        
        // Search and refresh box
        HBox searchBox = new HBox(15);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search Order ID / Customer...");
        searchField.setPrefWidth(400);
        searchField.setStyle("-fx-background-color: #343a40; -fx-text-fill: white; -fx-prompt-text-fill: #6c757d; -fx-background-radius: 8; -fx-padding: 12 15; -fx-font-size: 14px; -fx-border-color: transparent;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        Button searchBtn = new Button("🔍 Search");
        searchBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-size: 14px;");
        searchBtn.setOnAction(e -> searchReceipts(searchField.getText()));
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #0ba360; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-weight: 600; -fx-font-size: 14px;");
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
        
        // Add action column with view button only
        TableColumn<Receipt, Void> actionCol = new TableColumn<>("");
        actionCol.setPrefWidth(80);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("›");
            {
                viewBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 5;");
                viewBtn.setOnMouseEntered(ev -> viewBtn.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #495057; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 5;"));
                viewBtn.setOnMouseExited(ev -> viewBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 5;"));
                viewBtn.setOnAction(event -> {
                    Receipt receipt = getTableView().getItems().get(getIndex());
                    displayReceiptDetails(receipt);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
                setAlignment(Pos.CENTER);
            }
        });
        
        receiptHistoryTable.getColumns().addAll(receiptIdCol, orderIdCol, customerCol, 
                                                receiptTimeCol, amountCol, actionCol);
        
        receiptHistoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayReceiptDetails(newVal);
            }
        });
        
        // Receipt details
        VBox detailsBox = new VBox(20);
        detailsBox.setPadding(new Insets(25));
        detailsBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #6366f1; -fx-border-width: 2; -fx-border-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        Label detailsTitle = new Label("📄 Receipt View");
        detailsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        detailsTitle.setTextFill(Color.web("#1a1a1a"));
        
        String receiptNumber = "RCP-2533B391";
        Label receiptNumLabel = new Label(receiptNumber);
        receiptNumLabel.setFont(Font.font("Segoe UI", 14));
        receiptNumLabel.setTextFill(Color.web("#6c757d"));
        
        receiptDetailArea = new TextArea();
        receiptDetailArea.setEditable(false);
        receiptDetailArea.setFont(Font.font("Consolas", 13));
        receiptDetailArea.setPrefWidth(400);
        receiptDetailArea.setWrapText(true);
        receiptDetailArea.setText("👆 Select a receipt to view details");
        receiptDetailArea.setStyle("-fx-control-inner-background: #F8F9FA; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 8;");
        
        Button printBtn = new Button("🖨️ Print Receipt");
        printBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 14 20; -fx-background-radius: 10; -fx-cursor: hand;");
        printBtn.setPrefWidth(Double.MAX_VALUE);
        
        printBtn.setOnAction(e -> {
            Receipt selected = receiptHistoryTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAlert("Print", "Receipt #" + selected.getReceiptId() + " sent to printer.", 
                         Alert.AlertType.INFORMATION);
            }
        });
        
        detailsBox.getChildren().addAll(detailsTitle, receiptNumLabel, new Separator(), receiptDetailArea, printBtn);
        
        splitPane.getItems().addAll(receiptHistoryTable, detailsBox);
        splitPane.setDividerPositions(0.65);
        
        Label countLabel = new Label("📊 Total Receipts: 0");
        countLabel.setFont(Font.font("Segoe UI", 13));
        countLabel.setTextFill(Color.web("#6c757d"));
        countLabel.setStyle("-fx-padding: 10 0 0 0;");
        receiptHistory.addListener((javafx.collections.ListChangeListener.Change<? extends Receipt> c) -> {
            countLabel.setText("Total Receipts: " + receiptHistory.size());
        });
        countLabel.setText("Total Receipts: " + receiptHistory.size());
        
        panel.getChildren().addAll(headerBox, searchBox, splitPane, countLabel);
        
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

    private void showReturnExchangeDialog(Receipt receipt) {
        // Check if return is within time limit (2 hours)
        java.time.Duration timeSincePurchase = java.time.Duration.between(
            receipt.getReceiptTime(), 
            java.time.LocalDateTime.now()
        );
        
        if (timeSincePurchase.toHours() > 2) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Return Not Allowed");
            alert.setHeaderText("Time Limit Exceeded");
            alert.setContentText("Returns are only accepted within 2 hours of purchase.\nThis receipt is " + 
                timeSincePurchase.toHours() + " hours old.");
            alert.showAndWait();
            return;
        }

        // Get original order
        PendingOrder originalOrder = TextDatabase.getPendingOrderById(receipt.getOrderId());
        if (originalOrder == null || originalOrder.getItems() == null || originalOrder.getItems().isEmpty()) {
            showAlert("Error", "Could not find original order details.", Alert.AlertType.ERROR);
            return;
        }

        Stage dialogStage = new Stage();
        dialogStage.setTitle("Return / Exchange - Receipt #" + receipt.getReceiptId());
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f5f5;");
        
        // Header
        VBox header = new VBox(10);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: #ff9800;");
        
        Label title = new Label("↩ Return / Exchange Items");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        
        Label subtitle = new Label("Receipt #" + receipt.getReceiptId() + " | Customer: " + receipt.getUserName());
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#FFE0B2"));
        
        header.getChildren().addAll(title, subtitle);
        root.setTop(header);
        
        // Main content
        SplitPane mainContent = new SplitPane();
        mainContent.setDividerPositions(0.6);
        
        // Left: Original items
        VBox leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setStyle("-fx-background-color: white;");
        
        Label leftTitle = new Label("Original Order Items");
        leftTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        
        ScrollPane itemsScroll = new ScrollPane();
        itemsScroll.setFitToWidth(true);
        itemsScroll.setStyle("-fx-background-color: transparent;");
        
        VBox itemsContainer = new VBox(10);
        java.util.List<ReturnItemControl> returnControls = new java.util.ArrayList<>();
        
        // Convert PendingOrder.OrderItemData to display format
        for (PendingOrder.OrderItemData itemData : originalOrder.getItems()) {
            ReturnItemControl control = new ReturnItemControl(itemData);
            returnControls.add(control);
            itemsContainer.getChildren().add(control.getRoot());
        }
        
        itemsScroll.setContent(itemsContainer);
        leftPanel.getChildren().addAll(leftTitle, new Separator(), itemsScroll);
        
        // Right: Exchange items
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setStyle("-fx-background-color: white;");
        
        Label rightTitle = new Label("Exchange Items");
        rightTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        
        Label exchangeInfo = new Label("Select products to exchange returned items with:");
        exchangeInfo.setFont(Font.font("Segoe UI", 12));
        exchangeInfo.setTextFill(Color.web("#666"));
        exchangeInfo.setWrapText(true);
        
        ScrollPane exchangeScroll = new ScrollPane();
        exchangeScroll.setFitToWidth(true);
        exchangeScroll.setStyle("-fx-background-color: transparent;");
        
        VBox exchangeContainer = new VBox(10);
        javafx.collections.ObservableList<com.coffeeshop.model.OrderItem> exchangeItems = 
            javafx.collections.FXCollections.observableArrayList();
        
        Button addExchangeBtn = new Button("+ Add Exchange Item");
        addExchangeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20;");
        addExchangeBtn.setOnAction(e -> showProductSelector(dialogStage, exchangeItems, exchangeContainer));
        
        exchangeScroll.setContent(exchangeContainer);
        rightPanel.getChildren().addAll(rightTitle, new Separator(), exchangeInfo, addExchangeBtn, exchangeScroll);
        
        mainContent.getItems().addAll(leftPanel, rightPanel);
        root.setCenter(mainContent);
        
        // Footer with summary and actions
        VBox footer = new VBox(15);
        footer.setPadding(new Insets(20));
        footer.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 1 0 0 0;");
        
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(20);
        summaryGrid.setVgap(10);
        
        Label returnCreditLabel = new Label("Return Credit:");
        returnCreditLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        Label returnCreditValue = new Label("₱0.00");
        returnCreditValue.setFont(Font.font("Segoe UI", 14));
        returnCreditValue.setTextFill(Color.web("#4CAF50"));
        
        Label exchangeTotalLabel = new Label("Exchange Total:");
        exchangeTotalLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        Label exchangeTotalValue = new Label("₱0.00");
        exchangeTotalValue.setFont(Font.font("Segoe UI", 14));
        
        Label refundLabel = new Label("Net Refund:");
        refundLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        Label refundValue = new Label("₱0.00");
        refundValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        refundValue.setTextFill(Color.web("#F44336"));
        
        summaryGrid.add(returnCreditLabel, 0, 0);
        summaryGrid.add(returnCreditValue, 1, 0);
        summaryGrid.add(exchangeTotalLabel, 0, 1);
        summaryGrid.add(exchangeTotalValue, 1, 1);
        summaryGrid.add(new Separator(), 0, 2, 2, 1);
        summaryGrid.add(refundLabel, 0, 3);
        summaryGrid.add(refundValue, 1, 3);
        
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 30; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialogStage.close());
        
        Button processBtn = new Button("Process Return/Exchange");
        processBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 30; -fx-cursor: hand;");
        processBtn.setOnAction(e -> {
            processReturnExchange(receipt, returnControls, exchangeItems, dialogStage);
        });
        
        buttonBox.getChildren().addAll(cancelBtn, processBtn);
        footer.getChildren().addAll(summaryGrid, buttonBox);
        root.setBottom(footer);
        
        // Update totals when items change
        Runnable updateTotals = () -> {
            double returnCredit = returnControls.stream()
                .filter(rc -> rc.getAction() == ReturnAction.REFUND || rc.getAction() == ReturnAction.EXCHANGE)
                .mapToDouble(rc -> rc.getItemData().getSubtotal())
                .sum();
            
            double exchangeTotal = exchangeItems.stream()
                .mapToDouble(com.coffeeshop.model.OrderItem::getSubtotal)
                .sum();
            
            double netRefund = returnCredit - exchangeTotal;
            
            returnCreditValue.setText("₱" + String.format("%.2f", returnCredit));
            exchangeTotalValue.setText("₱" + String.format("%.2f", exchangeTotal));
            refundValue.setText((netRefund >= 0 ? "₱" : "-₱") + String.format("%.2f", Math.abs(netRefund)));
            refundValue.setTextFill(netRefund >= 0 ? Color.web("#4CAF50") : Color.web("#F44336"));
            
            if (netRefund >= 0) {
                refundLabel.setText("Net Refund:");
            } else {
                refundLabel.setText("Additional Payment:");
            }
        };
        
        // Listen to changes
        returnControls.forEach(rc -> rc.setOnActionChanged(updateTotals));
        exchangeItems.addListener((javafx.collections.ListChangeListener.Change<? extends com.coffeeshop.model.OrderItem> c) -> updateTotals.run());
        
        Scene scene = new Scene(root, 1200, 700);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private void showProductSelector(Stage parentStage, javafx.collections.ObservableList<com.coffeeshop.model.OrderItem> exchangeItems, VBox container) {
        Stage selectorStage = new Stage();
        selectorStage.setTitle("Select Product for Exchange");
        selectorStage.initModality(Modality.WINDOW_MODAL);
        selectorStage.initOwner(parentStage);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");
        
        // Category filter
        HBox categoryBox = new HBox(10);
        categoryBox.setAlignment(Pos.CENTER_LEFT);
        Label categoryLabel = new Label("Category:");
        categoryLabel.setFont(Font.font("Segoe UI", 12));
        
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setStyle("-fx-font-size: 12;");
        categoryCombo.getItems().add("All Categories");
        
        Store.getInstance().getCategories().forEach(cat -> categoryCombo.getItems().add(cat));
        categoryCombo.setValue("All Categories");
        
        categoryBox.getChildren().addAll(categoryLabel, categoryCombo);
        
        // Products table
        TableView<Product> productsTable = new TableView<>();
        productsTable.setPrefHeight(300);
        
        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);
        
        TableColumn<Product, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);
        priceCol.setCellFactory(col -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : String.format("₱%.2f", item));
            }
        });
        
        TableColumn<Product, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        stockCol.setPrefWidth(80);
        
        productsTable.getColumns().addAll(nameCol, priceCol, stockCol);
        
        // Quantity and add-ons section
        GridPane itemDetailsGrid = new GridPane();
        itemDetailsGrid.setHgap(10);
        itemDetailsGrid.setVgap(10);
        itemDetailsGrid.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 15;");
        
        Label quantityLabel = new Label("Quantity:");
        quantityLabel.setFont(Font.font("Segoe UI", 12));
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 100, 1);
        quantitySpinner.setEditable(true);
        
        Label addOnsLabel = new Label("Add-ons:");
        addOnsLabel.setFont(Font.font("Segoe UI", 12));
        ComboBox<String> addOnsCombo = new ComboBox<>();
        addOnsCombo.setStyle("-fx-font-size: 11;");
        addOnsCombo.getItems().add("None");
        Store.getInstance().getAddOns().forEach(addon -> addOnsCombo.getItems().add(addon.getName() + " (₱" + String.format("%.2f", addon.getPrice()) + ")"));
        addOnsCombo.setValue("None");
        
        Label remarksLabel = new Label("Remarks:");
        remarksLabel.setFont(Font.font("Segoe UI", 12));
        TextField remarksField = new TextField();
        remarksField.setPromptText("Special requests or notes...");
        remarksField.setPrefWidth(300);
        
        itemDetailsGrid.add(quantityLabel, 0, 0);
        itemDetailsGrid.add(quantitySpinner, 1, 0);
        itemDetailsGrid.add(addOnsLabel, 0, 1);
        itemDetailsGrid.add(addOnsCombo, 1, 1);
        itemDetailsGrid.add(remarksLabel, 0, 2);
        itemDetailsGrid.add(remarksField, 1, 2);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button confirmBtn = new Button("Add to Exchange");
        confirmBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> {
            Product selected = productsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Selection Error", "Please select a product.", Alert.AlertType.WARNING);
                return;
            }
            
            int qty = quantitySpinner.getValue();
            if (qty <= 0) {
                showAlert("Quantity Error", "Quantity must be at least 1.", Alert.AlertType.WARNING);
                return;
            }
            
            String addOnSelection = addOnsCombo.getValue();
            double addOnsCost = 0;
            String addOnsDisplay = "";
            
            if (!addOnSelection.equals("None")) {
                String[] parts = addOnSelection.split(" \\(₱");
                addOnsDisplay = parts[0];
                try {
                    addOnsCost = Double.parseDouble(parts[1].replace(")", ""));
                } catch (Exception ex) { addOnsCost = 0; }
            }
            
            String remarks = remarksField.getText().trim();
            
            // Create OrderItem for exchange
            com.coffeeshop.model.OrderItem exchangeItem = new com.coffeeshop.model.OrderItem(selected, qty, "Hot", 50);
            exchangeItem.setAddOns(addOnsDisplay);
            exchangeItem.setAddOnsCost(addOnsCost);
            
            // Store remarks in a way we can access later (we'll add a field to OrderItem if needed)
            // For now, we can encode it with the product name or store it separately
            // Let's add it to add-ons display for now
            String finalAddOns = addOnsDisplay + (remarks.isEmpty() ? "" : " | Remarks: " + remarks);
            exchangeItem.setAddOns(finalAddOns);
            
            exchangeItems.add(exchangeItem);
            
            // Create visual representation in container
            VBox itemBox = new VBox(5);
            itemBox.setPadding(new Insets(10));
            itemBox.setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #4CAF50; -fx-border-width: 1; -fx-border-radius: 5;");
            
            Label itemLabel = new Label(selected.getName() + " x" + qty);
            itemLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            
            Label costLabel = new Label(String.format("₱%.2f", exchangeItem.getSubtotal()));
            costLabel.setFont(Font.font("Segoe UI", 11));
            costLabel.setTextFill(Color.web("#2196F3"));
            
            String detailsText = "";
            if (!addOnsDisplay.isEmpty()) {
                detailsText += "Add-ons: " + addOnsDisplay;
            }
            if (!remarks.isEmpty()) {
                if (!detailsText.isEmpty()) detailsText += " | ";
                detailsText += "Remarks: " + remarks;
            }
            
            if (!detailsText.isEmpty()) {
                Label detailsLabel = new Label(detailsText);
                detailsLabel.setFont(Font.font("Segoe UI", 10));
                detailsLabel.setTextFill(Color.web("#666"));
                detailsLabel.setWrapText(true);
                itemBox.getChildren().addAll(itemLabel, costLabel, detailsLabel);
            } else {
                itemBox.getChildren().addAll(itemLabel, costLabel);
            }
            
            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 5 10;");
            removeBtn.setOnAction(removeEvent -> {
                container.getChildren().remove(itemBox);
                exchangeItems.remove(exchangeItem);
            });
            itemBox.getChildren().add(removeBtn);
            
            container.getChildren().add(itemBox);
            
            selectorStage.close();
        });
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> selectorStage.close());
        
        buttonBox.getChildren().addAll(confirmBtn, cancelBtn);
        
        // Update products table based on category selection
        Runnable updateProductsTable = () -> {
            String selectedCategory = categoryCombo.getValue();
            ObservableList<Product> products;
            
            if ("All Categories".equals(selectedCategory)) {
                products = FXCollections.observableArrayList(Store.getInstance().getProducts());
            } else {
                java.util.List<Product> filtered = Store.getInstance().getProducts().stream()
                    .filter(p -> p.getCategory().equals(selectedCategory))
                    .collect(java.util.stream.Collectors.toList());
                products = FXCollections.observableArrayList(filtered);
            }
            
            productsTable.setItems(products);
        };
        
        categoryCombo.setOnAction(e -> updateProductsTable.run());
        updateProductsTable.run();
        
        root.getChildren().addAll(categoryBox, new Separator(), productsTable, itemDetailsGrid, buttonBox);
        
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        Scene scene = new Scene(scroll, 800, 600);
        selectorStage.setScene(scene);
        selectorStage.showAndWait();
    }

    private void processReturnExchange(Receipt originalReceipt, java.util.List<ReturnItemControl> returnControls, 
                                      javafx.collections.ObservableList<com.coffeeshop.model.OrderItem> exchangeItems, Stage dialogStage) {
        // Collect returned items
        java.util.List<ReturnItemControl> itemsToReturn = returnControls.stream()
            .filter(rc -> rc.getAction() != ReturnAction.KEEP)
            .collect(java.util.stream.Collectors.toList());
        
        if (itemsToReturn.isEmpty() && exchangeItems.isEmpty()) {
            showAlert("No Changes", "No items selected for return or exchange.", Alert.AlertType.WARNING);
            return;
        }
        
        // Create return transaction
        String returnId = "RTN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        com.coffeeshop.model.ReturnTransaction returnTx = new com.coffeeshop.model.ReturnTransaction(
            returnId, 
            originalReceipt.getReceiptId(),
            currentCashierId != null ? currentCashierId : "CASHIER"
        );
        
        returnTx.setOriginalTotal(originalReceipt.getTotalAmount());
        
        // Add returned items
        for (ReturnItemControl rc : itemsToReturn) {
            com.coffeeshop.model.ReturnTransaction.ReturnItem returnItem = 
                new com.coffeeshop.model.ReturnTransaction.ReturnItem(
                    rc.getItemData().productName,
                    rc.getItemData().quantity,
                    rc.getReason(),
                    rc.getItemData().getSubtotal()
                );
            returnTx.addReturnedItem(returnItem);
            
            // Update inventory - add returned items back to stock
            try {
                // Find product by name
                Product product = Store.getInstance().getProducts().stream()
                    .filter(p -> p.getName().equals(rc.getItemData().productName))
                    .findFirst()
                    .orElse(null);
                if (product != null) {
                    Store.getInstance().refillProduct(product.getId(), rc.getItemData().quantity);
                }
            } catch (Exception ignored) {}
        }
        
        // Add exchange items
        for (com.coffeeshop.model.OrderItem exchangeItem : exchangeItems) {
            returnTx.addExchangeItem(exchangeItem);
            
            // Update inventory - remove exchange items from stock (decrement stock)
            try {
                Product product = exchangeItem.getProduct();
                if (product != null) {
                    // Decrement by setting to current minus quantity
                    int newStock = Math.max(0, product.getStock() - exchangeItem.getQuantity());
                    product.setStock(newStock);
                    Store.getInstance().saveData();
                }
            } catch (Exception ignored) {}
        }
        
        // Calculate totals
        returnTx.calculateTotals();
        
        // Save return transaction
        TextDatabase.saveReturnTransaction(returnTx);
        
        // Generate return receipt
        generateReturnReceipt(returnTx, originalReceipt);
        
        dialogStage.close();
        
        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Success");
        success.setHeaderText("Return/Exchange Processed");
        success.setContentText(String.format(
            "Return ID: %s\n" +
            "Return Credit: ₱%.2f\n" +
            "Exchange Total: ₱%.2f\n" +
            "%s: ₱%.2f",
            returnId,
            returnTx.getReturnCredit(),
            returnTx.getExchangeTotal(),
            returnTx.getRefundAmount() >= 0 ? "Refund Amount" : "Additional Collected",
            Math.abs(returnTx.getRefundAmount())
        ));
        success.showAndWait();
    }

    private void generateReturnReceipt(com.coffeeshop.model.ReturnTransaction returnTx, Receipt originalReceipt) {
        StringBuilder receipt = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("         BREWISE COFFEE SHOP\n");
        receipt.append("        RETURN/EXCHANGE RECEIPT\n");
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("Return ID: ").append(returnTx.getReturnId()).append("\n");
        receipt.append("Original Receipt: ").append(originalReceipt.getReceiptId()).append("\n");
        receipt.append("Customer: ").append(originalReceipt.getUserName()).append("\n");
        receipt.append("Cashier: ").append(returnTx.getCashierId()).append("\n");
        receipt.append("Date: ").append(returnTx.getReturnTime().format(formatter)).append("\n");
        receipt.append("═══════════════════════════════════════\n\n");
        
        if (!returnTx.getReturnedItems().isEmpty()) {
            receipt.append("RETURNED ITEMS:\n");
            receipt.append("───────────────────────────────────────\n");
            for (com.coffeeshop.model.ReturnTransaction.ReturnItem item : returnTx.getReturnedItems()) {
                receipt.append(String.format("- %-20s  -₱%.2f\n", item.getProductName(), item.getItemSubtotal()));
                receipt.append("  Reason: ").append(item.getReason()).append("\n");
            }
            receipt.append("\n");
        }
        
        if (!returnTx.getExchangeItems().isEmpty()) {
            receipt.append("EXCHANGE ITEMS:\n");
            receipt.append("───────────────────────────────────────\n");
            for (com.coffeeshop.model.OrderItem item : returnTx.getExchangeItems()) {
                receipt.append(String.format("+ %-20s (x%d)  +₱%.2f\n", 
                    item.getProduct().getName(), item.getQuantity(), item.getSubtotal()));
                
                // Show add-ons if present
                if (!item.getAddOns().isEmpty()) {
                    receipt.append("  Add-ons/Remarks: ").append(item.getAddOns()).append("\n");
                }
            }
            receipt.append("\n");
        }
        
        receipt.append("═══════════════════════════════════════\n");
        receipt.append(String.format("Original Total:       ₱%.2f\n", returnTx.getOriginalTotal()));
        receipt.append(String.format("Return Credit:       -₱%.2f\n", returnTx.getReturnCredit()));
        receipt.append(String.format("Exchange Total:      +₱%.2f\n", returnTx.getExchangeTotal()));
        receipt.append("───────────────────────────────────────\n");
        if (returnTx.getRefundAmount() >= 0) {
            receipt.append(String.format("REFUND DUE:          ₱%.2f\n", returnTx.getRefundAmount()));
        } else {
            receipt.append(String.format("PAYMENT COLLECTED:   ₱%.2f\n", Math.abs(returnTx.getRefundAmount())));
        }
        receipt.append("═══════════════════════════════════════\n");
        receipt.append("       Thank you for your patronage!\n");
        receipt.append("═══════════════════════════════════════\n");
        
        System.out.println(receipt.toString());
    }

    // Helper enum and class for return dialog
    private enum ReturnAction {
        KEEP, REFUND, EXCHANGE
    }

    private class ReturnItemControl {
        private VBox root;
        private PendingOrder.OrderItemData itemData;
        private ReturnAction action = ReturnAction.KEEP;
        private String reason = "";
        private Runnable onActionChanged;
        
        public ReturnItemControl(PendingOrder.OrderItemData itemData) {
            this.itemData = itemData;
            buildUI();
        }
        
        private void buildUI() {
            root = new VBox(10);
            root.setPadding(new Insets(15));
            root.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
            
            // Item info
            Label itemName = new Label(itemData.productName);
            itemName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            
            Label itemDetails = new Label(String.format("Qty: %d | Total: ₱%.2f", 
                itemData.quantity, itemData.getSubtotal()));
            itemDetails.setFont(Font.font("Segoe UI", 12));
            itemDetails.setTextFill(Color.web("#666"));
            
            // Action selection
            HBox actionBox = new HBox(10);
            actionBox.setAlignment(Pos.CENTER_LEFT);
            
            ToggleGroup actionGroup = new ToggleGroup();
            
            RadioButton keepBtn = new RadioButton("Keep");
            keepBtn.setToggleGroup(actionGroup);
            keepBtn.setSelected(true);
            keepBtn.setOnAction(e -> {
                action = ReturnAction.KEEP;
                if (onActionChanged != null) onActionChanged.run();
            });
            
            RadioButton refundBtn = new RadioButton("Refund");
            refundBtn.setToggleGroup(actionGroup);
            refundBtn.setOnAction(e -> {
                action = ReturnAction.REFUND;
                showReasonDialog();
                if (onActionChanged != null) onActionChanged.run();
            });
            
            RadioButton exchangeBtn = new RadioButton("Exchange");
            exchangeBtn.setToggleGroup(actionGroup);
            exchangeBtn.setOnAction(e -> {
                action = ReturnAction.EXCHANGE;
                showReasonDialog();
                if (onActionChanged != null) onActionChanged.run();
            });
            
            actionBox.getChildren().addAll(keepBtn, refundBtn, exchangeBtn);
            root.getChildren().addAll(itemName, itemDetails, actionBox);
        }
        
        private void showReasonDialog() {
            ChoiceDialog<String> dialog = new ChoiceDialog<>("Wrong item ordered", 
                "Wrong item ordered",
                "Defective/Quality issue",
                "Wrong size/temperature",
                "Customer changed mind",
                "Wrong customization",
                "Other"
            );
            dialog.setTitle("Return Reason");
            dialog.setHeaderText("Select reason for return:");
            dialog.setContentText("Reason:");
            
            dialog.showAndWait().ifPresent(r -> reason = r);
        }
        
        public VBox getRoot() {
            return root;
        }
        
        public PendingOrder.OrderItemData getItemData() {
            return itemData;
        }
        
        public ReturnAction getAction() {
            return action;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setOnActionChanged(Runnable callback) {
            this.onActionChanged = callback;
        }
    }

    // Entry point for Maven exec plugin and runnable jar
    public static void main(String[] args) {
        // Launch JavaFX application
        Application.launch(CashierApp.class, args);
    }
}
