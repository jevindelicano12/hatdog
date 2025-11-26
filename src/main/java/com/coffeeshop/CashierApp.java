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
        Tab ordersTab = new Tab("   📦 Order Queue   ");
        ordersTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        ordersTab.setContent(createOrderQueuePanel());
        
        // Tab 2: Order Status Board (Customer Display)
        Tab orderStatusTab = new Tab("   📺 Order Status   ");
        orderStatusTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        orderStatusTab.setContent(createOrderStatusPanel());
        
        // Refresh order status when tab is selected
        orderStatusTab.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                orderStatusTab.setContent(createOrderStatusPanel());
            }
        });
        
        // Tab 3: Returns
        Tab returnsTab = new Tab("   🔄 Returns   ");
        returnsTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        returnsTab.setContent(createReturnsPanel());
        
        // Tab 3: Receipt History
        Tab receiptHistoryTab = new Tab("   📋 Receipt History   ");
        receiptHistoryTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        receiptHistoryTab.setContent(createReceiptHistoryPanel());
        
        // Tab 4: Complaints
        Tab complaintsTab = new Tab("   😕 Complaints   ");
        complaintsTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        complaintsTab.setContent(createComplaintsPanel());
        
        // Tab 5: Reports / Dashboard
        Tab reportsTab = new Tab("   📊 Dashboard   ");
        reportsTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        reportsTab.setContent(createDashboardPanel());
        
        // Refresh dashboard when tab is selected
        reportsTab.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                reportsTab.setContent(createDashboardPanel());
            }
        });

        // Tab 6: Remittance Report
        Tab remittanceTab = new Tab("   💰 Remittance   ");
        remittanceTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        remittanceTab.setContent(createRemittancePanel());
        
        // Refresh remittance when tab is selected
        remittanceTab.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                remittanceTab.setContent(createRemittancePanel());
            }
        });

        tabPane.getTabs().addAll(ordersTab, orderStatusTab, returnsTab, receiptHistoryTab, complaintsTab, reportsTab, remittanceTab);
        
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
        
        Label subtitleLabel = new Label("✓ Please sign in to continue");
        subtitleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 15));
        subtitleLabel.setTextFill(Color.web("#34495E"));
        
        Label userLbl = new Label("👤 Username:");
        userLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        userLbl.setTextFill(Color.web("#2C3E50"));
        
        TextField userField = new TextField();
        userField.setPromptText("Enter your username");
        userField.setStyle("-fx-font-size: 15px; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #3498DB; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: white;");
        userField.setPrefWidth(300);

        Label passLbl = new Label("🔒 Password:");
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

        Button loginBtn = new Button("✓ Login");
        loginBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #27AE60, #229954); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 14 40; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        loginBtn.setPrefWidth(140);
        
        Button cancelBtn = new Button("✗ Cancel");
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
        
        receipt.append("\n");
        receipt.append("         BREWISE COFFEE SHOP\n");
        receipt.append("\n");
        receipt.append("Order ID: ").append(order.getOrderId()).append("\n");
        receipt.append("Customer: ").append(customerName != null ? customerName : "Walk-in").append("\n");
        receipt.append("Order Type: ").append(orderType).append("\n");
        if (currentCashierId != null && !currentCashierId.isEmpty()) {
            receipt.append("Cashier: ").append(currentCashierId).append("\n");
        }
        receipt.append("Date: ").append(java.time.LocalDateTime.now().format(formatter)).append("\n");
        receipt.append("\n\n");
        
        receipt.append("ITEMS:\n");
        receipt.append("\n");
        
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
        
        receipt.append("\n");
        // Calculate VAT breakdown (assume order.getTotalAmount() includes VAT)
        double total = order.getTotalAmount();
        double subtotal = total / 1.12; // remove 12% VAT to get base
        double vat = total - subtotal;
        receipt.append(String.format("Subtotal:         ₱%.2f\n", subtotal));
        receipt.append(String.format("VAT (12%%):        ₱%.2f\n", vat));
        receipt.append(String.format("TOTAL:            ₱%.2f\n", total));

        // Show cash paid and change immediately below the TOTAL
        receipt.append("\n");
        receipt.append(String.format("Cash Paid: ₱%.2f\n", cashPaid));
        receipt.append(String.format("Change:    ₱%.2f\n", change));
        receipt.append("\n");
        receipt.append("       Thank you for your order!\n");
        receipt.append("\n");
        
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

                        // Avoid clearing or reconstructing the dashboard VBox here -
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
            // Only load orders that belong to current cashier or don't have a cashier ID (backwards compatibility)
            String orderCashierId = pendingOrder.getCashierId();
            if (!orderCashierId.isEmpty() && !orderCashierId.equals(currentCashierId)) {
                // Skip orders from other cashiers
                continue;
            }
            
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
        return status != null ? status : "-";
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
                    // propagate size information if present in pending item
                    try {
                        if (item.size != null && !item.size.isEmpty()) oi.setSize(item.size);
                        oi.setSizeCost(item.sizeCost);
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}
                order.addItem(oi);
            }
        }

        // Validate ingredients and process checkout
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
        Receipt receipt = new Receipt(receiptId, order.getOrderId(), customerName, currentCashierId, order.getTotalAmount(), lastCashReceived, lastChange);
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
        dialog.setHeaderText(null);
        
        // Calculate VAT (12%)
        double subtotal = po.getTotalAmount() / 1.12; // Remove VAT to get subtotal
        double vatAmount = po.getTotalAmount() - subtotal;
        double totalWithVat = po.getTotalAmount();
        
        // Main container with split layout
        HBox mainContainer = new HBox(0);
        mainContainer.setPrefSize(700, 500);
        mainContainer.setStyle("-fx-background-color: white;");
        
        // LEFT PANEL - Payment Details
        VBox leftPanel = new VBox(20);
        leftPanel.setPrefWidth(350);
        leftPanel.setPadding(new Insets(40, 30, 40, 30));
        leftPanel.setStyle("-fx-background-color: white;");
        
        // Back arrow
        Label backArrow = new Label("💳 PAYMENT DETAILS");
        backArrow.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        backArrow.setTextFill(Color.web("#6B7280"));
        backArrow.setCursor(javafx.scene.Cursor.HAND);
        
        // Total amount section
        Label totalAmountLabel = new Label("₱" + String.format("%.2f", totalWithVat));
        totalAmountLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        totalAmountLabel.setTextFill(Color.web("#111827"));
        
        Label totalDueLabel = new Label("Total Amount Due");
        totalDueLabel.setFont(Font.font("Segoe UI", 16));
        totalDueLabel.setTextFill(Color.web("#6B7280"));
        
        VBox totalSection = new VBox(8);
        totalSection.getChildren().addAll(totalAmountLabel, totalDueLabel);
        
        // Breakdown section
        VBox breakdownBox = new VBox(12);
        breakdownBox.setPadding(new Insets(20));
        breakdownBox.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 12;");
        
        HBox itemsRow = new HBox();
        HBox.setHgrow(itemsRow, Priority.ALWAYS);
        Label itemsLabel = new Label("Items (" + po.getItems().size() + ")");
        itemsLabel.setFont(Font.font("Segoe UI", 14));
        itemsLabel.setTextFill(Color.web("#374151"));
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Label itemsValue = new Label("₱" + String.format("%.2f", subtotal));
        itemsValue.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        itemsValue.setTextFill(Color.web("#111827"));
        itemsRow.getChildren().addAll(itemsLabel, spacer1, itemsValue);
        
        HBox vatRow = new HBox();
        HBox.setHgrow(vatRow, Priority.ALWAYS);
        Label vatLabel = new Label("VAT (12%)");
        vatLabel.setFont(Font.font("Segoe UI", 14));
        vatLabel.setTextFill(Color.web("#374151"));
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        Label vatValue = new Label("₱" + String.format("%.2f", vatAmount));
        vatValue.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        vatValue.setTextFill(Color.web("#111827"));
        vatRow.getChildren().addAll(vatLabel, spacer2, vatValue);
        
        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        
        HBox totalRow = new HBox();
        HBox.setHgrow(totalRow, Priority.ALWAYS);
        Label totalLabel = new Label("Total");
        totalLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        totalLabel.setTextFill(Color.web("#111827"));
        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, Priority.ALWAYS);
        Label totalValue = new Label("₱" + String.format("%.2f", totalWithVat));
        totalValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        totalValue.setTextFill(Color.web("#4F46E5"));
        totalRow.getChildren().addAll(totalLabel, spacer3, totalValue);
        
        breakdownBox.getChildren().addAll(itemsRow, vatRow, separator, totalRow);

        // Change display (updates live) with short/notes
        HBox changeRow = new HBox();
        changeRow.setPadding(new Insets(12, 0, 0, 0));
        changeRow.setAlignment(Pos.CENTER_LEFT);
        
        // Left: 'Change' label and small note (for "Short")
        VBox changeBoxLeft = new VBox(4);
        Label changeLabel = new Label("Change");
        changeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        changeLabel.setTextFill(Color.web("#374151"));
        Label changeNote = new Label("");
        changeNote.setFont(Font.font("Segoe UI", 11));
        changeNote.setTextFill(Color.web("#DC2626"));
        changeBoxLeft.getChildren().addAll(changeLabel, changeNote);

        Region changeSpacer = new Region();
        HBox.setHgrow(changeSpacer, Priority.ALWAYS);
        Label changeValue = new Label("₱0.00");
        changeValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        changeValue.setTextFill(Color.web("#10B981"));
        changeRow.getChildren().addAll(changeBoxLeft, changeSpacer, changeValue);

        // Payment required notice
        HBox noticeBox = new HBox(10);
        noticeBox.setPadding(new Insets(12, 16, 12, 16));
        noticeBox.setAlignment(Pos.CENTER_LEFT);
        noticeBox.setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 8;");
        Label noticeIcon = new Label("ℹ️");
        noticeIcon.setFont(Font.font(16));
        Label noticeText = new Label("Payment Required\nPlease enter cash amount received.");
        noticeText.setFont(Font.font("Segoe UI", 12));
        noticeText.setTextFill(Color.web("#92400E"));
        noticeBox.getChildren().addAll(noticeIcon, noticeText);

        leftPanel.getChildren().addAll(backArrow, totalSection, breakdownBox, changeRow, noticeBox);
        
        // RIGHT PANEL - Amount Input and Numpad
        VBox rightPanel = new VBox(20);
        rightPanel.setPrefWidth(350);
        rightPanel.setPadding(new Insets(40, 30, 40, 30));
        rightPanel.setStyle("-fx-background-color: #F9FAFB;");
        
        Label amountTenderedLabel = new Label("AMOUNT TENDERED");
        amountTenderedLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        amountTenderedLabel.setTextFill(Color.web("#9CA3AF"));
        
        TextField amountField = new TextField("0.00");
        amountField.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        amountField.setStyle("-fx-background-color: transparent; -fx-text-fill: #D1D5DB; -fx-border-width: 0; -fx-padding: 0;");
        amountField.setPrefHeight(60);
        amountField.setEditable(false);
        
        // Quick amount buttons
        HBox quickAmounts = new HBox(10);
        String[] amounts = {"₱5", "₱10", "₱20", "₱50"};
        for (String amt : amounts) {
            Button btn = new Button(amt);
            btn.setPrefSize(70, 36);
            btn.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4F46E5; -fx-font-weight: 600; -fx-font-size: 13px; -fx-background-radius: 8; -fx-border-width: 0; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #C7D2FE; -fx-text-fill: #4F46E5; -fx-font-weight: 600; -fx-font-size: 13px; -fx-background-radius: 8; -fx-border-width: 0; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4F46E5; -fx-font-weight: 600; -fx-font-size: 13px; -fx-background-radius: 8; -fx-border-width: 0; -fx-cursor: hand;"));
            int value = Integer.parseInt(amt.substring(1));
            btn.setOnAction(e -> {
                String current = amountField.getText().equals("0.00") ? "" : amountField.getText();
                double newAmount = (current.isEmpty() ? 0 : Double.parseDouble(current)) + value;
                amountField.setText(String.format("%.2f", newAmount));
                amountField.setStyle("-fx-background-color: transparent; -fx-text-fill: #111827; -fx-border-width: 0; -fx-padding: 0; -fx-font-size: 40px; -fx-font-weight: bold;");
            });
            quickAmounts.getChildren().add(btn);
        }
        
        // Numpad
        GridPane numpad = new GridPane();
        numpad.setHgap(12);
        numpad.setVgap(12);
        numpad.setAlignment(Pos.CENTER);
        
        String[][] keys = {
            {"1", "2", "3"},
            {"4", "5", "6"},
            {"7", "8", "9"},
            {".", "0", "C"}
        };
        
        for (int row = 0; row < keys.length; row++) {
            for (int col = 0; col < keys[row].length; col++) {
                String key = keys[row][col];
                Button btn = new Button(key);
                btn.setPrefSize(80, 60);
                
                if (key.equals("C")) {
                    btn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-size: 22px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-border-width: 0; -fx-cursor: hand;");
                    btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #FECACA; -fx-text-fill: #DC2626; -fx-font-size: 22px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-border-width: 0; -fx-cursor: hand;"));
                    btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-size: 22px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-border-width: 0; -fx-cursor: hand;"));
                } else {
                    btn.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #1F2937; -fx-font-size: 20px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-border-width: 0; -fx-cursor: hand;");
                    btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #D1D5DB; -fx-text-fill: #1F2937; -fx-font-size: 20px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-border-width: 0; -fx-cursor: hand;"));
                    btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #1F2937; -fx-font-size: 20px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-border-width: 0; -fx-cursor: hand;"));
                }
                
                btn.setOnAction(e -> {
                    String current = amountField.getText();
                    if (current.equals("0.00")) current = "";
                    
                    if (key.equals("C")) {
                        if (!current.isEmpty()) {
                            current = current.substring(0, current.length() - 1);
                            if (current.isEmpty()) current = "0.00";
                        }
                    } else if (key.equals(".")) {
                        if (!current.contains(".")) {
                            current += ".";
                        }
                    } else {
                        current += key;
                    }
                    
                    amountField.setText(current.isEmpty() ? "0.00" : current);
                    if (!amountField.getText().equals("0.00")) {
                        amountField.setStyle("-fx-background-color: transparent; -fx-text-fill: #111827; -fx-border-width: 0; -fx-padding: 0; -fx-font-size: 40px; -fx-font-weight: bold;");
                    }
                });
                
                numpad.add(btn, col, row);
            }
        }
        
        // Exact Amount button
        Button exactBtn = new Button("Exact Amount");
        exactBtn.setPrefSize(272, 50);
        exactBtn.setStyle("-fx-background-color: #1F2937; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-cursor: hand;");
        exactBtn.setOnMouseEntered(e -> exactBtn.setStyle("-fx-background-color: #111827; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-cursor: hand;"));
        exactBtn.setOnMouseExited(e -> exactBtn.setStyle("-fx-background-color: #1F2937; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-cursor: hand;"));
        exactBtn.setOnAction(e -> {
            amountField.setText(String.format("%.2f", totalWithVat));
            amountField.setStyle("-fx-background-color: transparent; -fx-text-fill: #111827; -fx-border-width: 0; -fx-padding: 0; -fx-font-size: 40px; -fx-font-weight: bold;");
        });
        
        // Complete Transaction button
        Button completeBtn = new Button("Complete\nTRANSACTION");
        completeBtn.setPrefSize(272, 60);
        completeBtn.setStyle("-fx-background-color: #D1D5DB; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-cursor: hand;");
        completeBtn.setDisable(true);
        
        // Update complete button state and live change display
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double entered = 0;
                if (newVal != null && !newVal.trim().isEmpty() && !newVal.equals("0.00")) {
                    // sanitize trailing dot
                    String sanitized = newVal.endsWith(".") ? newVal.substring(0, newVal.length()-1) : newVal;
                    entered = Double.parseDouble(sanitized);
                }
                double change = entered - totalWithVat;
                if (change < 0) {
                    // Insufficient: show short note and zero change
                    changeValue.setText("₱0.00");
                    changeValue.setTextFill(Color.web("#DC2626"));
                    changeNote.setText("Short ₱" + String.format("%.2f", Math.abs(change)));
                    changeNote.setTextFill(Color.web("#DC2626"));
                } else {
                    // Sufficient: show positive change and clear short note
                    changeValue.setText("₱" + String.format("%.2f", change));
                    changeValue.setTextFill(Color.web("#10B981"));
                    changeNote.setText("");
                }

                if (entered >= totalWithVat) {
                    completeBtn.setDisable(false);
                    completeBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-cursor: hand;");
                } else {
                    completeBtn.setDisable(true);
                    completeBtn.setStyle("-fx-background-color: #D1D5DB; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-cursor: hand;");
                }
            } catch (Exception ex) {
                completeBtn.setDisable(true);
                completeBtn.setStyle("-fx-background-color: #D1D5DB; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-cursor: hand;");
                changeValue.setText("₱0.00");
                changeValue.setTextFill(Color.web("#10B981"));
            }
        });
        
        rightPanel.getChildren().addAll(amountTenderedLabel, amountField, quickAmounts, numpad, exactBtn, completeBtn);
        
        mainContainer.getChildren().addAll(leftPanel, rightPanel);
        dialog.getDialogPane().setContent(mainContainer);
        
        // Add buttons (hidden, controlled by custom buttons)
        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);
        
        // Hide default buttons
        dialog.getDialogPane().lookupButton(okType).setVisible(false);
        dialog.getDialogPane().lookupButton(cancelType).setVisible(false);
        
        // Wire up custom buttons
        final boolean[] result = {false};
        completeBtn.setOnAction(e -> {
            try {
                double cashReceived = Double.parseDouble(amountField.getText());
                if (cashReceived >= totalWithVat) {
                    lastCashReceived = cashReceived;
                    lastChange = cashReceived - totalWithVat;
                    result[0] = true;
                    dialog.setResult(true);
                    dialog.close();
                }
            } catch (Exception ex) {
                // Invalid input
            }
        });
        
        backArrow.setOnMouseClicked(e -> {
            result[0] = false;
            dialog.close();
        });
        
        // Remove default dialog styling
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 0;");
        dialog.setResizable(false);
        
        dialog.showAndWait();
        return result[0];
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
        
        // Record cash transaction for remittance tracking
        CashTransaction tx = new CashTransaction(
            UUID.randomUUID().toString(),
            currentCashierId,
            java.time.LocalDateTime.now(),
            CashTransaction.TYPE_SALE,
            po.getTotalAmount(),
            po.getOrderId(),
            "Order completed for " + po.getCustomerName(),
            null
        );
        TextDatabase.saveCashTransaction(tx);
        
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
            Label icon1 = new Label("💰");
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
            Label icon2 = new Label("📊");
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
            Label icon3 = new Label("✅");
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
        VBox headerBox = new VBox(10);
        headerBox.setAlignment(Pos.TOP_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        
        Label title = new Label("Order Lookup");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#111827"));
        
        Label subtitle = new Label("Enter an order ID to retrieve and process");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#6B7280"));
        
        headerBox.getChildren().addAll(title, subtitle);
        panel.getChildren().add(headerBox);
        
        // Search section
        VBox searchSection = new VBox(12);
        searchSection.setPadding(new Insets(25));
        searchSection.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        searchSection.setPrefWidth(600);
        
        Label searchLabel = new Label("🔍 Enter Order ID:");
        searchLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        searchLabel.setTextFill(Color.web("#374151"));
        
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        
        TextField orderIdField = new TextField();
        orderIdField.setPromptText("e.g., c25f5698");
        orderIdField.setStyle("-fx-font-size: 14px; -fx-padding: 12; -fx-border-radius: 8; -fx-border-color: #E5E7EB;");
        orderIdField.setPrefWidth(300);
        
        Button searchBtn = new Button("\ud83d\udd0d Search");
        searchBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;");
        searchBtn.setPrefWidth(120);
        
        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("Segoe UI", 12));
        
        searchBox.getChildren().addAll(orderIdField, searchBtn, statusLabel);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        
        searchSection.getChildren().addAll(searchLabel, searchBox);
        panel.getChildren().add(searchSection);
        
        // Results panel
        VBox resultsPanel = new VBox(15);
        resultsPanel.setPadding(new Insets(25));
        resultsPanel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        resultsPanel.setPrefHeight(500);
        resultsPanel.setMaxHeight(Double.MAX_VALUE);
        
        Label resultsTitle = new Label("📋 Order Details");
        resultsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        resultsTitle.setTextFill(Color.web("#111827"));
        
        TextArea orderDetailsArea = new TextArea();
        orderDetailsArea.setEditable(false);
        orderDetailsArea.setWrapText(true);
        orderDetailsArea.setFont(Font.font("Consolas", 13));
        orderDetailsArea.setText("Enter an order ID and click Search to retrieve order details...");
        orderDetailsArea.setPrefHeight(350);
        orderDetailsArea.setMaxHeight(Double.MAX_VALUE);
        orderDetailsArea.setStyle("-fx-control-inner-background: #F9FAFB; -fx-background-color: #F9FAFB; -fx-font-size: 12px; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 8;");
        VBox.setVgrow(orderDetailsArea, Priority.ALWAYS);
        
        // Action buttons (hidden until order found)
        HBox actionButtonsBox = new HBox(10);
        actionButtonsBox.setAlignment(Pos.CENTER_LEFT);
        actionButtonsBox.setPadding(new Insets(15, 0, 0, 0));
        actionButtonsBox.setStyle("-fx-border-color: #E5E7EB; -fx-border-width: 1 0 0 0;");
        
        Button payBtn = new Button("💳 Pay & Start Preparing");
        payBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        payBtn.setPrefWidth(200);
        payBtn.setVisible(false);
        
        Button completeBtn = new Button("✓ Complete & Pickup");
        completeBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        completeBtn.setPrefWidth(200);
        completeBtn.setVisible(false);
        
        Button cancelBtn = new Button("✗ Cancel Order");
        cancelBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setPrefWidth(150);
        cancelBtn.setVisible(false);
        
        actionButtonsBox.getChildren().addAll(payBtn, completeBtn, cancelBtn);
        
        resultsPanel.getChildren().addAll(resultsTitle, orderDetailsArea, actionButtonsBox);
        panel.getChildren().add(resultsPanel);
        VBox.setVgrow(resultsPanel, Priority.ALWAYS);
        
        // Search functionality
        Runnable searchOrder = () -> {
            String orderIdToFind = orderIdField.getText().trim();
            if (orderIdToFind.isEmpty()) {
                statusLabel.setText("✗ Enter an order ID");
                statusLabel.setTextFill(Color.web("#EF4444"));
                payBtn.setVisible(false);
                completeBtn.setVisible(false);
                cancelBtn.setVisible(false);
                orderDetailsArea.setText("No order ID entered.");
                return;
            }
            
            // Search for order in pending orders
            PendingOrder foundOrder = null;
            for (PendingOrder po : orderQueue) {
                if (po.getOrderId().equalsIgnoreCase(orderIdToFind)) {
                    foundOrder = po;
                    break;
                }
            }
            
            if (foundOrder == null) {
                statusLabel.setText("✗ Order not found");
                statusLabel.setTextFill(Color.web("#EF4444"));
                payBtn.setVisible(false);
                completeBtn.setVisible(false);
                cancelBtn.setVisible(false);
                orderDetailsArea.setText("No order found with ID: " + orderIdToFind);
                return;
            }
            
            // Order found - show details
            statusLabel.setText("✓ Order found");
            statusLabel.setTextFill(Color.web("#10B981"));
            
            String customerName = orderCustomerNames.getOrDefault(foundOrder.getOrderId(), "Unknown");
            StringBuilder details = new StringBuilder();
            details.append("ORDER DETAILS\n");
            details.append("\n");
            details.append(String.format("Order #:    %s\n", foundOrder.getOrderId()));
            details.append(String.format("Customer:   %s\n", customerName));
            details.append(String.format("Time:       %s\n", 
                foundOrder.getOrderTime().format(DateTimeFormatter.ofPattern("hh:mm:ss a"))));
            details.append(String.format("Status:     %s\n\n", mapStatusToLabel(foundOrder.getStatus())));
            details.append("ITEMS\n");
            details.append("\n");

            if (foundOrder.getItems().isEmpty()) {
                details.append("No items.\n");
            } else {
                java.util.Map<String, Integer> qtyMap = new java.util.LinkedHashMap<>();
                java.util.Map<String, Double> priceMap = new java.util.HashMap<>();
                for (PendingOrder.OrderItemData item : foundOrder.getItems()) {
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
                    details.append(String.format("%-20s x%d    ₱%.2f\n", name, qty, subtotal));
                }
            }

            details.append("\n");
            details.append(String.format("TOTAL:      ₱%.2f\n", foundOrder.getTotalAmount()));

            orderDetailsArea.setText(details.toString());
            
            // Show appropriate action buttons based on order status
            payBtn.setVisible(false);
            completeBtn.setVisible(false);
            cancelBtn.setVisible(true);
            
            if (PendingOrder.STATUS_PENDING.equals(foundOrder.getStatus())) {
                payBtn.setVisible(true);
            } else if (PendingOrder.STATUS_PAID.equals(foundOrder.getStatus())) {
                payBtn.setText("\ud83c\udf74 Start Preparing");
                payBtn.setVisible(true);
            } else if (PendingOrder.STATUS_PREPARING.equals(foundOrder.getStatus())) {
                completeBtn.setVisible(true);
            } else if (PendingOrder.STATUS_COMPLETED.equals(foundOrder.getStatus())) {
                payBtn.setVisible(false);
                completeBtn.setVisible(false);
                cancelBtn.setText("🔄 Reopen");
            }
        };
        
        searchBtn.setOnAction(e -> searchOrder.run());
        orderIdField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                searchOrder.run();
            }
        });
        
        // Action button handlers
        final PendingOrder[] selectedOrder = {null};
        payBtn.setOnAction(e -> {
            String orderIdToFind = orderIdField.getText().trim();
            for (PendingOrder po : orderQueue) {
                if (po.getOrderId().equalsIgnoreCase(orderIdToFind)) {
                    selectedOrder[0] = po;
                    if (PendingOrder.STATUS_PENDING.equals(po.getStatus())) {
                        payAndStartPreparing(po);
                    } else if (PendingOrder.STATUS_PAID.equals(po.getStatus())) {
                        po.setStatus(PendingOrder.STATUS_PREPARING);
                        TextDatabase.savePendingOrder(po);
                        loadPendingOrdersFromFile();
                        statusLabel.setText("✓ Order moved to Preparing");
                    }
                    searchOrder.run();
                    break;
                }
            }
        });
        
        completeBtn.setOnAction(e -> {
            String orderIdToFind = orderIdField.getText().trim();
            for (PendingOrder po : orderQueue) {
                if (po.getOrderId().equalsIgnoreCase(orderIdToFind)) {
                    completePickup(po);
                    loadPendingOrdersFromFile();
                    orderIdField.clear();
                    statusLabel.setText("");
                    orderDetailsArea.setText("Order completed! Enter another order ID to continue...");
                    break;
                }
            }
        });
        
        cancelBtn.setOnAction(e -> {
            String orderIdToFind = orderIdField.getText().trim();
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Cancel Order");
            confirmDialog.setHeaderText("Are you sure?");
            confirmDialog.setContentText("This will permanently delete the order: " + orderIdToFind);
            
            if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                TextDatabase.deletePendingOrder(orderIdToFind);
                loadPendingOrdersFromFile();
                orderIdField.clear();
                statusLabel.setText("✓ Order cancelled");
                orderDetailsArea.setText("Order deleted. Enter another order ID...");
            }
        });

        // Wrap the panel in a ScrollPane
        ScrollPane outerScroll = new ScrollPane(panel);
        outerScroll.setFitToWidth(true);
        outerScroll.setFitToHeight(false);
        outerScroll.setPrefViewportHeight(800);
        outerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        outerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        outerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        panel.setPrefHeight(1000);

        return outerScroll;
    }
    
    // ==================== RETURNS PANEL ====================
    
    private ScrollPane createReturnsPanel() {
        VBox panel = new VBox(30);
        panel.setPadding(new Insets(40));
        panel.setStyle("-fx-background-color: #f8f9fa;");
        panel.setAlignment(Pos.TOP_CENTER);
        
        // Header
        Label title = new Label("🔄 Return / Exchange Items");
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
        Label icon = new Label("📦");
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
        
        Label infoTitle = new Label("📋 Return Policy");
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
    
    // ==================== COMPLAINTS PANEL ====================
    
    private ScrollPane createComplaintsPanel() {
        VBox panel = new VBox(30);
        panel.setPadding(new Insets(40));
        panel.setStyle("-fx-background-color: #f8f9fa;");
        panel.setAlignment(Pos.TOP_CENTER);
        
        // Header
        Label title = new Label("😕 Order Complaints");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#3b82f6"));
        
        Label subtitle = new Label("Submit and manage customer complaints for order issues");
        subtitle.setFont(Font.font("Segoe UI", 16));
        subtitle.setTextFill(Color.web("#6c757d"));
        
        // Main card
        VBox card = new VBox(25);
        card.setMaxWidth(700);
        card.setPadding(new Insets(40));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);");
        card.setAlignment(Pos.TOP_LEFT);
        
        // Icon
        Label icon = new Label("😕");
        icon.setFont(Font.font(80));
        icon.setAlignment(Pos.CENTER);
        VBox iconBox = new VBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        
        // Order ID input
        VBox orderIdBox = new VBox(10);
        Label orderIdLabel = new Label("Order ID: *");
        orderIdLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        orderIdLabel.setTextFill(Color.web("#374151"));
        
        TextField orderIdField = new TextField();
        orderIdField.setPromptText("e.g., e10b8209 or 915a2be6");
        orderIdField.setStyle("-fx-font-size: 16px; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-width: 2; -fx-border-radius: 8;");
        orderIdBox.getChildren().addAll(orderIdLabel, orderIdField);
        
        // Customer name input
        VBox customerBox = new VBox(10);
        Label customerLabel = new Label("Customer Name: *");
        customerLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        customerLabel.setTextFill(Color.web("#374151"));
        
        TextField customerField = new TextField();
        customerField.setPromptText("Customer name");
        customerField.setStyle("-fx-font-size: 16px; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-width: 2; -fx-border-radius: 8;");
        customerBox.getChildren().addAll(customerLabel, customerField);
        
        // Issue type dropdown
        VBox issueTypeBox = new VBox(10);
        Label issueTypeLabel = new Label("Issue Type: *");
        issueTypeLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        issueTypeLabel.setTextFill(Color.web("#374151"));
        
        ComboBox<String> issueTypeCombo = new ComboBox<>();
        issueTypeCombo.setPromptText("Select issue type");
        issueTypeCombo.getItems().addAll(
            "Wrong Order",
            "Missing Items",
            "Quality Issue",
            "Temperature Issue",
            "Taste/Flavor Issue",
            "Service Issue",
            "Other"
        );
        issueTypeCombo.setStyle("-fx-font-size: 16px; -fx-padding: 8;");
        issueTypeCombo.setPrefWidth(Double.MAX_VALUE);
        issueTypeBox.getChildren().addAll(issueTypeLabel, issueTypeCombo);
        
        // Description text area
        VBox descBox = new VBox(10);
        Label descLabel = new Label("Description: *");
        descLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        descLabel.setTextFill(Color.web("#374151"));
        
        TextArea descArea = new TextArea();
        descArea.setPromptText("Please describe the issue in detail...");
        descArea.setPrefRowCount(5);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-font-size: 14px; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-width: 2; -fx-border-radius: 8;");
        descBox.getChildren().addAll(descLabel, descArea);
        
        // Submit button
        Button submitBtn = new Button("Submit");
        submitBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 8; -fx-cursor: hand;");
        submitBtn.setPrefWidth(Double.MAX_VALUE);
        submitBtn.setOnMouseEntered(e -> submitBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 8; -fx-cursor: hand;"));
        submitBtn.setOnMouseExited(e -> submitBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15 40; -fx-background-radius: 8; -fx-cursor: hand;"));
        submitBtn.setOnAction(e -> {
            String orderId = orderIdField.getText().trim();
            String customer = customerField.getText().trim();
            String issueType = issueTypeCombo.getValue();
            String description = descArea.getText().trim();
            
            // Validation
            if (orderId.isEmpty()) {
                showAlert("Validation Error", "Please enter an Order ID.", Alert.AlertType.WARNING);
                return;
            }
            if (customer.isEmpty()) {
                showAlert("Validation Error", "Please enter the customer name.", Alert.AlertType.WARNING);
                return;
            }
            if (issueType == null || issueType.isEmpty()) {
                showAlert("Validation Error", "Please select an issue type.", Alert.AlertType.WARNING);
                return;
            }
            if (description.isEmpty()) {
                showAlert("Validation Error", "Please provide a description of the issue.", Alert.AlertType.WARNING);
                return;
            }
            
            // Check if order exists
            Receipt receipt = receiptHistory.stream()
                .filter(r -> r.getOrderId().equalsIgnoreCase(orderId))
                .findFirst()
                .orElse(null);
            
            if (receipt == null) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Order Not Found");
                confirmAlert.setHeaderText("Cannot verify Order ID");
                confirmAlert.setContentText("Order ID \"" + orderId + "\" was not found in receipt history.\nDo you want to submit the complaint anyway?");
                
                java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return;
                }
            }
            
            // Persist complaint and notify admin listeners
            try {
                String cmpId = "CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                com.coffeeshop.model.Complaint complaint = new com.coffeeshop.model.Complaint(
                    cmpId,
                    orderId,
                    customer,
                    issueType,
                    description,
                    currentCashierId != null ? currentCashierId : "Unknown"
                );

                // Save via Store so listeners are notified
                Store.getInstance().saveComplaint(complaint);

                String complaintRecord = String.format(
                    "Complaint Submitted:\n" +
                    "ID: %s\n" +
                    "Date: %s\n" +
                    "Order ID: %s\n" +
                    "Customer: %s\n" +
                    "Issue Type: %s\n" +
                    "Description: %s\n" +
                    "Cashier: %s",
                    complaint.getId(),
                    complaint.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    complaint.getOrderId(),
                    complaint.getCustomerName(),
                    complaint.getIssueType(),
                    complaint.getDescription(),
                    complaint.getCashierId()
                );

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Complaint Submitted");
                successAlert.setHeaderText("✓ Complaint filed successfully");
                successAlert.setContentText(complaintRecord);
                successAlert.showAndWait();

                // Clear form
                orderIdField.clear();
                customerField.clear();
                issueTypeCombo.setValue(null);
                descArea.clear();
            } catch (Exception ex) {
                showAlert("Save Error", "Failed to save complaint: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
        
        // Cancel button (clears the form / cancels)
        Button clearBtn = new Button("Cancel");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12 30; -fx-cursor: hand; -fx-font-size: 14px;");
        clearBtn.setPrefWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> {
            orderIdField.clear();
            customerField.clear();
            issueTypeCombo.setValue(null);
            descArea.clear();
        });
        
        HBox buttonBox = new HBox(15);
        buttonBox.getChildren().addAll(clearBtn, submitBtn);
        HBox.setHgrow(clearBtn, Priority.ALWAYS);
        HBox.setHgrow(submitBtn, Priority.ALWAYS);
        
        // Info box
        VBox infoBox = new VBox(10);
        infoBox.setMaxWidth(700);
        infoBox.setPadding(new Insets(20));
        infoBox.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 10; -fx-border-color: #3b82f6; -fx-border-width: 1; -fx-border-radius: 10;");
        
        Label infoTitle = new Label("⚠️ Complaint Guidelines");
        infoTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        infoTitle.setTextFill(Color.web("#1e40af"));
        
        Label infoText = new Label(
            "• Verify the Order ID from the customer's receipt\n" +
            "• Document all relevant details about the issue\n" +
            "• Be professional and empathetic when handling complaints\n" +
            "• Follow up with management for resolution tracking"
        );
        infoText.setFont(Font.font("Segoe UI", 12));
        infoText.setTextFill(Color.web("#6c757d"));
        infoText.setWrapText(true);
        
        infoBox.getChildren().addAll(infoTitle, infoText);
        
        card.getChildren().addAll(iconBox, orderIdBox, customerBox, issueTypeBox, descBox, buttonBox);
        panel.getChildren().addAll(title, subtitle, card, infoBox);
        
        // Wrap in ScrollPane for scrollability
        ScrollPane scrollPane = new ScrollPane(panel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f8f9fa; -fx-background: #f8f9fa;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        return scrollPane;
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
        
        Button searchBtn = new Button("\ud83d\udd0d Search");
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
            private final Button viewBtn = new Button("🔍");
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
        
        Label detailsTitle = new Label("📋 Receipt View");
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
        receiptDetailArea.setText("📄 Select a receipt to view details");
        receiptDetailArea.setStyle("-fx-control-inner-background: #F8F9FA; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 8;");
        
        Button printBtn = new Button("\ud83d\udda8 Print Receipt");
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
            details.append("\n");
            details.append("          COFFEE SHOP RECEIPT\n");
            details.append("\n");
            details.append("Receipt ID: ").append(receipt.getReceiptId()).append("\n");
            details.append("Order ID: ").append(receipt.getOrderId()).append("\n");
            details.append("Customer: ").append(receipt.getUserName()).append("\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            details.append("Date: ").append(receipt.getReceiptTime().format(formatter)).append("\n");
            details.append("\n");
            // Show VAT breakdown if possible (assume total includes VAT)
            double totalAmt = receipt.getTotalAmount();
            double subtotalAmt = totalAmt / 1.12;
            double vatAmt = totalAmt - subtotalAmt;
            details.append(String.format("Subtotal:      ₱%.2f\n", subtotalAmt));
            details.append(String.format("VAT (12%%):     ₱%.2f\n", vatAmt));
            details.append(String.format("TOTAL:         ₱%.2f\n", totalAmt));
            details.append(String.format("Cash Paid:     ₱%.2f\n", receipt.getCashPaid()));
            details.append(String.format("Change:        ₱%.2f\n", receipt.getChange()));
            details.append("\n");
            details.append("       Thank you for your order!\n");
            details.append("\n");
            
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
        
        Label title = new Label("🔄 Return / Exchange Items");
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
        addExchangeBtn.setDisable(true); // Disabled initially
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
        returnControls.forEach(rc -> rc.setOnActionChanged(() -> {
            // Check if any item has EXCHANGE action selected
            boolean hasExchange = returnControls.stream().anyMatch(r -> r.getAction() == ReturnAction.EXCHANGE);
            addExchangeBtn.setDisable(!hasExchange);
            exchangeInfo.setTextFill(hasExchange ? Color.web("#666") : Color.web("#999"));
            updateTotals.run();
        }));
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
        
        productsTable.getColumns().addAll(nameCol, priceCol);
        
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
        
        // Calculate if customer needs to pay additional amount
        double returnCredit = itemsToReturn.stream()
            .mapToDouble(rc -> rc.getItemData().getSubtotal())
            .sum();
        double exchangeTotal = exchangeItems.stream()
            .mapToDouble(com.coffeeshop.model.OrderItem::getSubtotal)
            .sum();
        double additionalPayment = exchangeTotal - returnCredit;
        
        // If additional payment is required, collect cash amount from the customer
        double amountReceived = 0.0;
        String paymentMethod = "";
        if (additionalPayment > 0) {
            // Keep prompting until a valid amount is entered or cashier cancels
            boolean ok = false;
            while (!ok) {
                Dialog<Double> cashDialog = new Dialog<>();
                cashDialog.setTitle("Additional Payment Required");
                cashDialog.setHeaderText("Customer owes additional ₱" + String.format("%.2f", additionalPayment));

                VBox paymentContent = new VBox(12);
                paymentContent.setPadding(new Insets(18));

                Label messageLabel = new Label("The customer is exchanging items with a higher total value.\n" +
                    "Please enter the CASH amount received from the customer:");
                messageLabel.setWrapText(true);

                GridPane paymentGrid = new GridPane();
                paymentGrid.setHgap(12);
                paymentGrid.setVgap(12);

                Label amountDueLabel = new Label("Amount Due:");
                amountDueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                Label amountDueValue = new Label("₱" + String.format("%.2f", additionalPayment));
                amountDueValue.setTextFill(Color.web("#F44336"));

                Label receivedLabel = new Label("Amount Received (CASH):");
                TextField receivedField = new TextField(String.format("%.2f", additionalPayment));
                receivedField.setPromptText("Enter cash received");

                // Restrict input to numeric with optional 2 decimals
                java.util.regex.Pattern validEditingState = java.util.regex.Pattern.compile("\\d*(\\.\\d{0,2})?");
                TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
                    String newText = change.getControlNewText();
                    if (validEditingState.matcher(newText).matches()) {
                        return change;
                    }
                    return null;
                });
                receivedField.setTextFormatter(textFormatter);

                Label changeLabel = new Label("₱0.00");
                changeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                changeLabel.setTextFill(Color.web("#4CAF50"));

                Label errorLabel = new Label("Invalid amount!");
                errorLabel.setTextFill(Color.web("#D32F2F"));
                errorLabel.setVisible(false);

                paymentGrid.add(amountDueLabel, 0, 0);
                paymentGrid.add(amountDueValue, 1, 0);
                paymentGrid.add(receivedLabel, 0, 1);
                paymentGrid.add(receivedField, 1, 1);
                paymentGrid.add(new Label("Change:"), 0, 2);
                paymentGrid.add(changeLabel, 1, 2);
                paymentGrid.add(errorLabel, 0, 3, 2, 1);

                paymentContent.getChildren().addAll(messageLabel, new Separator(), paymentGrid);

                cashDialog.getDialogPane().setContent(paymentContent);
                cashDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                // Disable OK until valid amount entered
                javafx.scene.Node okButton = cashDialog.getDialogPane().lookupButton(ButtonType.OK);
                okButton.setDisable(true);

                // Live-validate input and compute change
                receivedField.textProperty().addListener((obs, oldV, newV) -> {
                    errorLabel.setVisible(false);
                    if (newV == null || newV.isBlank()) {
                        okButton.setDisable(true);
                        changeLabel.setText("₱0.00");
                        return;
                    }
                    try {
                        double val = Double.parseDouble(newV);
                        if (val < additionalPayment) {
                            okButton.setDisable(true);
                            changeLabel.setText("₱0.00");
                        } else {
                            okButton.setDisable(false);
                            double ch = val - additionalPayment;
                            changeLabel.setText("₱" + String.format("%.2f", ch));
                        }
                    } catch (Exception ex) {
                        okButton.setDisable(true);
                        errorLabel.setVisible(true);
                        changeLabel.setText("₱0.00");
                    }
                });

                cashDialog.setResultConverter(btn -> {
                    if (btn == ButtonType.OK) {
                        try {
                            String txt = receivedField.getText().trim().replaceAll(",", "");
                            return Double.parseDouble(txt);
                        } catch (Exception ex) {
                            return Double.valueOf(-1);
                        }
                    }
                    return null;
                });

                java.util.Optional<Double> cashRes = cashDialog.showAndWait();
                if (cashRes.isEmpty()) {
                    return; // cancelled
                }
                Double val = cashRes.get();
                if (val == null || val < additionalPayment) {
                    showAlert("Insufficient Cash", "Amount received must be at least ₱" + String.format("%.2f", additionalPayment) + ". Please re-enter or cancel.", Alert.AlertType.WARNING);
                    continue; // re-prompt
                }
                amountReceived = val;
                paymentMethod = "CASH";
                ok = true;
            }
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
            
            // Inventory update not needed for returns (ingredient-based tracking)
        }
        
        // Add exchange items
        for (com.coffeeshop.model.OrderItem exchangeItem : exchangeItems) {
            returnTx.addExchangeItem(exchangeItem);
            
            // Inventory update not needed for exchanges (ingredient-based tracking)
        }
        
        // Calculate totals
        returnTx.calculateTotals();
        
        // Attach payment details (cash) if any
        if (additionalPayment > 0) {
            returnTx.setAmountReceived(amountReceived);
            double change = Math.max(0.0, amountReceived - additionalPayment);
            returnTx.setChangeAmount(change);
            returnTx.setPaymentMethod(paymentMethod);
        } else {
            // No additional payment; leave defaults
            returnTx.setAmountReceived(0.0);
            returnTx.setChangeAmount(0.0);
            returnTx.setPaymentMethod("");
        }

        // Save return transaction
        TextDatabase.saveReturnTransaction(returnTx);

        // Generate return receipt
        generateReturnReceipt(returnTx, originalReceipt);
        
        dialogStage.close();
        
        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Success");
        success.setHeaderText("Return/Exchange Processed");
        String paymentInfo = "";
        if (additionalPayment > 0) {
            paymentInfo = String.format("\nPayment Method: %s\nAmount Received: ₱%.2f\nChange: ₱%.2f", paymentMethod, returnTx.getAmountReceived(), returnTx.getChangeAmount());
        }
        success.setContentText(String.format(
            "Return ID: %s\n" +
            "Return Credit: ₱%.2f\n" +
            "Exchange Total: ₱%.2f\n" +
            "%s: ₱%.2f%s",
            returnId,
            returnTx.getReturnCredit(),
            returnTx.getExchangeTotal(),
            returnTx.getRefundAmount() >= 0 ? "Refund Amount" : "Additional Collected",
            Math.abs(returnTx.getRefundAmount()),
            paymentInfo
        ));
        success.showAndWait();
    }

    private void generateReturnReceipt(com.coffeeshop.model.ReturnTransaction returnTx, Receipt originalReceipt) {
        StringBuilder receipt = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        receipt.append("\n");
        receipt.append("         BREWISE COFFEE SHOP\n");
        receipt.append("        RETURN/EXCHANGE RECEIPT\n");
        receipt.append("\n");
        receipt.append("Return ID: ").append(returnTx.getReturnId()).append("\n");
        receipt.append("Original Receipt: ").append(originalReceipt.getReceiptId()).append("\n");
        receipt.append("Customer: ").append(originalReceipt.getUserName()).append("\n");
        receipt.append("Cashier: ").append(returnTx.getCashierId()).append("\n");
        receipt.append("Date: ").append(returnTx.getReturnTime().format(formatter)).append("\n");
        receipt.append("\n\n");
        
        if (!returnTx.getReturnedItems().isEmpty()) {
            receipt.append("RETURNED ITEMS:\n");
            receipt.append("\n");
            for (com.coffeeshop.model.ReturnTransaction.ReturnItem item : returnTx.getReturnedItems()) {
                receipt.append(String.format("- %-20s  -₱%.2f\n", item.getProductName(), item.getItemSubtotal()));
                receipt.append("  Reason: ").append(item.getReason()).append("\n");
            }
            receipt.append("\n");
        }
        
        if (!returnTx.getExchangeItems().isEmpty()) {
            receipt.append("EXCHANGE ITEMS:\n");
            receipt.append("\n");
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
        
        receipt.append("\n");
        receipt.append(String.format("Original Total:       ₱%.2f\n", returnTx.getOriginalTotal()));
        receipt.append(String.format("Return Credit:       -₱%.2f\n", returnTx.getReturnCredit()));
        receipt.append(String.format("Exchange Total:      +₱%.2f\n", returnTx.getExchangeTotal()));
        receipt.append("\n");
        if (returnTx.getRefundAmount() >= 0) {
            receipt.append(String.format("REFUND DUE:          ₱%.2f\n", returnTx.getRefundAmount()));
        } else {
            receipt.append(String.format("PAYMENT COLLECTED:   ₱%.2f\n", Math.abs(returnTx.getRefundAmount())));
            if (returnTx.getPaymentMethod() != null && !returnTx.getPaymentMethod().isEmpty()) {
                receipt.append(String.format("Payment Method:      %s\n", returnTx.getPaymentMethod()));
            }
            if (returnTx.getAmountReceived() > 0) {
                receipt.append(String.format("Amount Received:     ₱%.2f\n", returnTx.getAmountReceived()));
            }
            if (returnTx.getChangeAmount() > 0) {
                receipt.append(String.format("Change:              ₱%.2f\n", returnTx.getChangeAmount()));
            }
        }
        receipt.append("\n");
        receipt.append("       Thank you for your patronage!\n");
        receipt.append("\n");
        
        System.out.println(receipt.toString());
    }

    /**
     * Creates the Order Status Board panel - a customer-facing display showing
     * orders that are being prepared and orders ready for pickup.
     * Designed with HCI principles: clear visual hierarchy, high contrast, 
     * intuitive icons, and accessible interactions.
     */
    private VBox createOrderStatusPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0a0a0a;");
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setFillWidth(true);
        
        // Header - Clean, minimal with good contrast
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(30, 20, 20, 20));
        header.setStyle("-fx-background-color: #0a0a0a;");
        
        Label titleLabel = new Label("ORDER STATUS");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        titleLabel.setTextFill(Color.WHITE);
        
        header.getChildren().add(titleLabel);
        
        // Main content - two columns with clear separation
        HBox columnsContainer = new HBox(40);
        columnsContainer.setAlignment(Pos.TOP_CENTER);
        columnsContainer.setPadding(new Insets(20, 50, 20, 50));
        columnsContainer.setFillHeight(true);
        HBox.setHgrow(columnsContainer, Priority.ALWAYS);
        
        // Left Column - PREPARING (Warm Orange)
        VBox preparingColumn = createCleanStatusColumn(
            "PREPARING",
            "#FF9500",  // Warm orange
            preparingList,
            false
        );
        HBox.setHgrow(preparingColumn, Priority.ALWAYS);
        
        // Right Column - READY FOR PICKUP (Success Green)
        VBox readyColumn = createCleanStatusColumn(
            "READY FOR PICKUP", 
            "#34C759",  // Success green
            completedList,
            true
        );
        HBox.setHgrow(readyColumn, Priority.ALWAYS);
        
        columnsContainer.getChildren().addAll(preparingColumn, readyColumn);
        
        // Footer with timestamp
        HBox footer = new HBox(20);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(20));
        footer.setStyle("-fx-background-color: #0a0a0a;");
        
        Label timeLabel = new Label(java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy  •  hh:mm a")));
        timeLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        timeLabel.setTextFill(Color.web("#666666"));
        
        footer.getChildren().add(timeLabel);
        
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);
        panel.getChildren().addAll(header, columnsContainer, footer);
        
        return panel;
    }
    
    /**
     * Creates a clean, HCI-compliant status column.
     * Principles: Clear visual hierarchy, adequate spacing, readable fonts, 
     * consistent alignment, and intuitive feedback.
     */
    private VBox createCleanStatusColumn(String title, String accentColor,
                                          ObservableList<PendingOrder> orders, boolean isReady) {
        VBox column = new VBox(0);
        column.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 20;");
        column.setMinWidth(400);
        column.setMaxWidth(550);
        column.setAlignment(Pos.TOP_CENTER);
        
        // Column header with icon
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(25, 20, 20, 20));
        headerBox.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 20 20 0 0;");
        
        // Status icon
        Label iconLabel = new Label(isReady ? "✓" : "⏳");
        iconLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        iconLabel.setTextFill(Color.WHITE);
        
        // Title
        Label headerLabel = new Label(title);
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        headerLabel.setTextFill(Color.WHITE);
        
        // Order count badge
        Label countBadge = new Label(orders.size() + " order" + (orders.size() != 1 ? "s" : ""));
        countBadge.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        countBadge.setTextFill(Color.WHITE);
        countBadge.setOpacity(0.85);
        
        headerBox.getChildren().addAll(iconLabel, headerLabel, countBadge);
        
        // Order list container
        VBox ordersList = new VBox(12);
        ordersList.setAlignment(Pos.TOP_CENTER);
        ordersList.setPadding(new Insets(20));
        ordersList.setStyle("-fx-background-color: transparent;");
        
        if (orders.isEmpty()) {
            // Empty state with helpful message
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(40, 20, 40, 20));
            
            Label emptyIcon = new Label(isReady ? "📭" : "☕");
            emptyIcon.setFont(Font.font("Segoe UI", 48));
            
            Label emptyLabel = new Label(isReady ? "No orders ready yet" : "No orders in queue");
            emptyLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
            emptyLabel.setTextFill(Color.web("#666666"));
            
            emptyState.getChildren().addAll(emptyIcon, emptyLabel);
            ordersList.getChildren().add(emptyState);
        } else {
            // Display orders as clean cards
            for (PendingOrder order : orders) {
                HBox orderCard = createCleanOrderCard(order, accentColor, isReady);
                ordersList.getChildren().add(orderCard);
            }
        }
        
        // ScrollPane for overflow
        ScrollPane scrollPane = new ScrollPane(ordersList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMinHeight(300);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        column.getChildren().addAll(headerBox, scrollPane);
        
        return column;
    }
    
    /**
     * Creates a clean order card with HCI-friendly design.
     * For ready orders, includes a "Picked Up" button to remove from queue.
     */
    private HBox createCleanOrderCard(PendingOrder order, String accentColor, boolean isReady) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        
        // Card styling - clean, modern look
        String bgColor = isReady ? "#1e3d2f" : "#2d2d44";
        String borderColor = isReady ? "#34C759" : "#FF9500";
        
        card.setStyle("-fx-background-color: " + bgColor + "; " +
                     "-fx-background-radius: 12; " +
                     "-fx-border-color: " + borderColor + "; " +
                     "-fx-border-width: 0 0 0 5; " +  // Left accent border only
                     "-fx-border-radius: 12;");
        
        // Order number - large and prominent
        VBox orderInfo = new VBox(4);
        orderInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(orderInfo, Priority.ALWAYS);
        
        Label numberLabel = new Label("#" + order.getOrderId());
        numberLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        numberLabel.setTextFill(Color.WHITE);
        
        // Customer name
        String customerName = orderCustomerNames.getOrDefault(order.getOrderId(), order.getCustomerName());
        if (customerName == null || customerName.isEmpty()) {
            customerName = "Guest";
        }
        
        Label nameLabel = new Label(customerName);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        nameLabel.setTextFill(Color.web("#aaaaaa"));
        
        orderInfo.getChildren().addAll(numberLabel, nameLabel);
        card.getChildren().add(orderInfo);
        
        // For ready orders, add "Picked Up" button
        if (isReady) {
            Button pickedUpBtn = new Button("✓ Picked Up");
            pickedUpBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            pickedUpBtn.setStyle("-fx-background-color: #34C759; " +
                               "-fx-text-fill: white; " +
                               "-fx-padding: 10 18; " +
                               "-fx-background-radius: 8; " +
                               "-fx-cursor: hand;");
            
            // Hover effect
            pickedUpBtn.setOnMouseEntered(e -> 
                pickedUpBtn.setStyle("-fx-background-color: #2da84a; " +
                                   "-fx-text-fill: white; " +
                                   "-fx-padding: 10 18; " +
                                   "-fx-background-radius: 8; " +
                                   "-fx-cursor: hand;"));
            pickedUpBtn.setOnMouseExited(e -> 
                pickedUpBtn.setStyle("-fx-background-color: #34C759; " +
                                   "-fx-text-fill: white; " +
                                   "-fx-padding: 10 18; " +
                                   "-fx-background-radius: 8; " +
                                   "-fx-cursor: hand;"));
            
            // Action: Remove from completed list and delete from database
            pickedUpBtn.setOnAction(e -> {
                // Confirm before removing
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Pickup");
                confirm.setHeaderText("Order #" + order.getOrderId());
                confirm.setContentText("Mark this order as picked up?\nIt will be removed from the status board.");
                
                java.util.Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // Remove from lists
                    completedList.remove(order);
                    orderQueue.remove(order);
                    
                    // Delete from database
                    TextDatabase.deletePendingOrder(order.getOrderId());
                    
                    // Refresh the panel
                    Tab currentTab = null;
                    for (Tab tab : ((TabPane) rootPane.getCenter()).getTabs()) {
                        if (tab.getText().contains("Order Status")) {
                            currentTab = tab;
                            break;
                        }
                    }
                    if (currentTab != null) {
                        currentTab.setContent(createOrderStatusPanel());
                    }
                    
                    // Show brief confirmation
                    showAlert("Order Picked Up", 
                             "Order #" + order.getOrderId() + " has been marked as picked up.", 
                             Alert.AlertType.INFORMATION);
                }
            });
            
            card.getChildren().add(pickedUpBtn);
        } else {
            // For preparing orders, show time indicator
            Label timeLabel = new Label("In Progress...");
            timeLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            timeLabel.setTextFill(Color.web("#FF9500"));
            card.getChildren().add(timeLabel);
        }
        
        return card;
    }

    private VBox createRemittancePanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(25));
        panel.setStyle("-fx-background-color: #f8f9fa;");

        // Title
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label("Remittance Report");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#1a1a1a"));
        Label subtitleLabel = new Label("Detailed cash transaction history for " + currentCashierId);
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setTextFill(Color.web("#6c757d"));
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        panel.getChildren().add(titleBox);

        // Summary cards
        HBox summaryBox = new HBox(15);
        summaryBox.setPrefHeight(120);

        VBox totalSalesBox = createSummaryCard("\ud83d\udcb5 Total Sales", "\u20b10.00", "#28a745");
        VBox totalRefundsBox = createSummaryCard("\ud83d\udd04 Total Refunds", "\u20b10.00", "#dc3545");
        VBox netBox = createSummaryCard("\ud83d\udcc8 Net Amount", "\u20b10.00", "#007bff");

        summaryBox.getChildren().addAll(totalSalesBox, totalRefundsBox, netBox);
        HBox.setHgrow(totalSalesBox, Priority.ALWAYS);
        HBox.setHgrow(totalRefundsBox, Priority.ALWAYS);
        HBox.setHgrow(netBox, Priority.ALWAYS);
        panel.getChildren().add(summaryBox);

        // Transactions table
        TableView<CashTransaction> transactionTable = new TableView<>();
        transactionTable.setPrefHeight(400);

        TableColumn<CashTransaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setPrefWidth(100);
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<CashTransaction, String> timeCol = new TableColumn<>("Time");
        timeCol.setPrefWidth(150);
        timeCol.setCellValueFactory(cellData -> {
            CashTransaction tx = cellData.getValue();
            String formatted = tx.getTransactionTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        TableColumn<CashTransaction, Double> amountCol = new TableColumn<>("Amount (₱)");
        amountCol.setPrefWidth(120);
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(col -> new TableCell<CashTransaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                    setStyle(item >= 0 ? "-fx-text-fill: #28a745;" : "-fx-text-fill: #dc3545;");
                }
            }
        });

        TableColumn<CashTransaction, String> referenceCol = new TableColumn<>("Reference");
        referenceCol.setPrefWidth(120);
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));

        TableColumn<CashTransaction, String> notesCol = new TableColumn<>("Notes");
        notesCol.setPrefWidth(200);
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        transactionTable.getColumns().addAll(typeCol, timeCol, amountCol, referenceCol, notesCol);

        // Load transactions for current cashier
        java.util.List<CashTransaction> transactions = TextDatabase.loadCashTransactionsByCashier(currentCashierId);
        ObservableList<CashTransaction> data = FXCollections.observableArrayList(transactions);
        transactionTable.setItems(data);

        // Calculate and update summary
        double totalSales = transactions.stream()
            .filter(tx -> CashTransaction.TYPE_SALE.equals(tx.getType()))
            .mapToDouble(CashTransaction::getAmount)
            .sum();
        double totalRefunds = transactions.stream()
            .filter(tx -> CashTransaction.TYPE_REFUND.equals(tx.getType()))
            .mapToDouble(CashTransaction::getAmount)
            .sum();
        double net = totalSales + totalRefunds;

        updateSummaryCard(totalSalesBox, String.format("₱%.2f", totalSales));
        updateSummaryCard(totalRefundsBox, String.format("₱%.2f", totalRefunds));
        updateSummaryCard(netBox, String.format("₱%.2f", net));

        ScrollPane scrollPane = new ScrollPane(transactionTable);
        scrollPane.setFitToWidth(true);
        panel.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return panel;
    }

    private VBox createSummaryCard(String label, String value, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        card.setAlignment(Pos.CENTER_LEFT);

        Label labelLbl = new Label(label);
        labelLbl.setFont(Font.font("Segoe UI", 14));
        labelLbl.setTextFill(Color.web("#6c757d"));

        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        valueLbl.setTextFill(Color.web(color));
        valueLbl.setId("summaryValue"); // For easy updating

        card.getChildren().addAll(labelLbl, valueLbl);
        return card;
    }

    private void updateSummaryCard(VBox card, String newValue) {
        for (javafx.scene.Node node : card.getChildren()) {
            if (node instanceof Label) {
                Label lbl = (Label) node;
                if (lbl.getId() != null && lbl.getId().equals("summaryValue")) {
                    lbl.setText(newValue);
                }
            }
        }
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
