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
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

public class CashierApp extends Application {
    private Store store;
    private BorderPane rootPane;
    private String currentCashierId = null;
    private Label headerDateLabel;
    private Label cashierInfoLabel;
    private java.util.concurrent.ScheduledExecutorService scheduler;
    private javafx.collections.ObservableList<Receipt> receiptHistory = FXCollections.observableArrayList();
    private javafx.collections.ObservableList<PendingOrder> orderQueue = FXCollections.observableArrayList();
    private javafx.collections.ObservableList<PendingOrder> pendingList = FXCollections.observableArrayList();
    private javafx.collections.ObservableList<PendingOrder> preparingList = FXCollections.observableArrayList();
    private javafx.collections.ObservableList<PendingOrder> completedList = FXCollections.observableArrayList();
    private java.util.Map<String,String> orderCustomerNames = new java.util.HashMap<>();
    private java.util.Map<String,String> orderTypes = new java.util.HashMap<>();
    private TableView<Receipt> receiptHistoryTable;
    private Stage primaryStageRef;
    private TextArea receiptDetailArea;
    private double lastCashReceived = 0.0;
    private double lastChange = 0.0;

    @Override
    public void start(Stage primaryStage) {
        primaryStageRef = primaryStage;
        store = Store.getInstance();
        
        // Check maintenance mode first
        if (TextDatabase.isMaintenanceMode()) {
            showMaintenanceScreen(primaryStage);
            return;
        }
        
        // Show login dialog first - exit if login fails or is cancelled
        if (!showLoginDialog(null)) {
            Platform.exit();
            return;
        }
        
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

        tabPane.getTabs().addAll(ordersTab, orderStatusTab, returnsTab, receiptHistoryTab);
        
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
        VBox infoBox = new VBox(6);
        infoBox.setAlignment(Pos.CENTER_RIGHT);

        headerDateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        headerDateLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        headerDateLabel.setTextFill(Color.web("#6B7280"));

        cashierInfoLabel = new Label();
        cashierInfoLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        cashierInfoLabel.setTextFill(Color.web("#1F2937"));

        if (currentCashierId != null && !currentCashierId.isEmpty()) {
            HBox cashierRow = new HBox(8);
            cashierRow.setAlignment(Pos.CENTER_RIGHT);

            // user menu (clickable icon) with Logout option
            MenuButton userMenu = new MenuButton();
            Label userIcon = new Label("👤");
            userIcon.setFont(Font.font(12));
            userMenu.setGraphic(userIcon);
            userMenu.setText("");
            MenuItem logoutItem = new MenuItem("Logout");
            logoutItem.setOnAction(ev -> {
                // Stop background sync and clear session
                stopBackgroundSync();
                currentCashierId = null;
                
                // Hide the main window
                primaryStageRef.hide();
                
                // Show login dialog
                boolean ok = showLoginDialog(null);
                if (!ok) {
                    // User cancelled login; exit application
                    Platform.exit();
                    return;
                }

                // Login succeeded: reload everything and rebuild the UI
                loadReceiptHistory();
                loadPendingOrdersFromFile();
                
                // Rebuild the entire interface with new user
                rootPane.setTop(createHeader());
                
                // Recreate tabs
                TabPane tabPane = new TabPane();
                tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
                tabPane.setStyle("-fx-background-color: #FFFFFF; -fx-tab-min-height: 50px; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");
                
                Tab ordersTab = new Tab("   📦 Order Queue   ");
                ordersTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
                ordersTab.setContent(createOrderQueuePanel());
                
                Tab orderStatusTab = new Tab("   📺 Order Status   ");
                orderStatusTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
                orderStatusTab.setContent(createOrderStatusPanel());
                orderStatusTab.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    if (isSelected) {
                        orderStatusTab.setContent(createOrderStatusPanel());
                    }
                });
                
                Tab returnsTab = new Tab("   🔄 Returns   ");
                returnsTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
                returnsTab.setContent(createReturnsPanel());
                
                Tab receiptHistoryTab = new Tab("   📋 Receipt History   ");
                receiptHistoryTab.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
                receiptHistoryTab.setContent(createReceiptHistoryPanel());

                tabPane.getTabs().addAll(ordersTab, orderStatusTab, returnsTab, receiptHistoryTab);
                rootPane.setCenter(tabPane);
                
                // Show the main window again
                primaryStageRef.show();
                primaryStageRef.setMaximized(true);
                startBackgroundSync();
            });
            userMenu.getItems().add(logoutItem);

            cashierInfoLabel.setText(currentCashierId);

            Label statusBadge = new Label("Logged In");
            statusBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            statusBadge.setTextFill(Color.web("#10B981"));
            statusBadge.setStyle("-fx-background-color: #D1FAE5; -fx-padding: 4 10; -fx-background-radius: 12;");

            cashierRow.getChildren().addAll(userMenu, cashierInfoLabel, statusBadge);
            infoBox.getChildren().add(cashierRow);
        } else {
            cashierInfoLabel.setText("Not signed in");
            infoBox.getChildren().add(cashierInfoLabel);
        }

        // date row
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        bottomRow.getChildren().add(headerDateLabel);

        infoBox.getChildren().add(bottomRow);

        header.getChildren().addAll(brandBox, spacer, infoBox);
        
        VBox wrapper = new VBox(header);
        return wrapper;
    }

    /**
     * Show maintenance screen when system is under maintenance.
     * Periodically checks if maintenance mode has been disabled.
     */
    private void showMaintenanceScreen(Stage primaryStage) {
        VBox maintenanceBox = new VBox(30);
        maintenanceBox.setAlignment(Pos.CENTER);
        maintenanceBox.setStyle("-fx-background-color: linear-gradient(to bottom, #2C3E50, #1a252f);");
        maintenanceBox.setPadding(new Insets(60));
        
        // Maintenance icon
        Label iconLabel = new Label("🔧");
        iconLabel.setStyle("-fx-font-size: 80px;");
        
        // Title
        Label titleLabel = new Label("System Under Maintenance");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #E74C3C;");
        
        // Message
        Label messageLabel = new Label("The cashier system is currently unavailable.\nPlease wait while our team performs maintenance.");
        messageLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #ECF0F1; -fx-text-alignment: center;");
        messageLabel.setWrapText(true);
        
        // Coffee icon
        Label coffeeIcon = new Label("☕");
        coffeeIcon.setStyle("-fx-font-size: 60px;");
        
        // Please wait
        Label waitLabel = new Label("Please Wait...");
        waitLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #BDC3C7; -fx-font-style: italic;");
        
        maintenanceBox.getChildren().addAll(iconLabel, titleLabel, messageLabel, coffeeIcon, waitLabel);
        
        Scene maintenanceScene = new Scene(maintenanceBox, 800, 600);
        primaryStage.setScene(maintenanceScene);
        primaryStage.setTitle("Coffee Shop - Cashier Terminal (Maintenance)");
        primaryStage.show();
        
        // Periodically check if maintenance mode is still enabled
        javafx.animation.Timeline maintenanceChecker = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), e -> {
                if (!TextDatabase.isMaintenanceMode()) {
                    // Maintenance is over, restart the app
                    primaryStage.close();
                    Platform.runLater(() -> {
                        try {
                            new CashierApp().start(new Stage());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            })
        );
        maintenanceChecker.setCycleCount(javafx.animation.Animation.INDEFINITE);
        maintenanceChecker.play();
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
        // Filter to show only receipts processed by the current logged-in cashier
        for (Receipt r : receipts) {
            if (currentCashierId != null && currentCashierId.equals(r.getCashierId())) {
                receiptHistory.add(r);
            }
        }
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
        nameDialog.setHeaderText("Enter Customer Name for Order #" + orderId);
        
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPadding(new Insets(25));
        grid.setStyle("-fx-background-color: linear-gradient(to bottom, #FFFFFF, #F8F9FA);");
        
        Label nameLabel = new Label("👤 Customer Name:");
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.web("#2C3E50"));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Enter customer name here...");
        nameField.setPrefWidth(350);
        nameField.setStyle("-fx-font-size: 15px; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #3498DB; -fx-border-width: 2; -fx-border-radius: 8;");
        
        // Add a hint label
        Label hintLabel = new Label("💡 Leave empty to use 'Guest' as customer name");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(hintLabel, 1, 1);
        
        nameDialog.getDialogPane().setContent(grid);
        
        ButtonType okType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        nameDialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);
        
        // Auto-focus the text field when dialog opens
        Platform.runLater(() -> nameField.requestFocus());
        
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

    /**
     * Shows the same styled payment dialog for exchange additional payments.
     * Returns [amountReceived, change] or null if cancelled.
     */
    private double[] showExchangePaymentDialog(double amountDue, double returnCredit, double exchangeTotal) {
        Dialog<double[]> dialog = new Dialog<>();
        dialog.setTitle("Additional Payment Required");
        dialog.setHeaderText(null);
        
        // Main container with split layout
        HBox mainContainer = new HBox(0);
        mainContainer.setPrefSize(700, 500);
        mainContainer.setStyle("-fx-background-color: white;");
        
        // LEFT PANEL - Payment Details
        VBox leftPanel = new VBox(20);
        leftPanel.setPrefWidth(350);
        leftPanel.setPadding(new Insets(40, 30, 40, 30));
        leftPanel.setStyle("-fx-background-color: white;");
        
        // Header
        Label headerLabel = new Label("💳 EXCHANGE PAYMENT");
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        headerLabel.setTextFill(Color.web("#6B7280"));
        
        // Total amount section
        Label totalAmountLabel = new Label("₱" + String.format("%.2f", amountDue));
        totalAmountLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        totalAmountLabel.setTextFill(Color.web("#DC2626"));
        
        Label totalDueLabel = new Label("Additional Payment Due");
        totalDueLabel.setFont(Font.font("Segoe UI", 16));
        totalDueLabel.setTextFill(Color.web("#6B7280"));
        
        VBox totalSection = new VBox(8);
        totalSection.getChildren().addAll(totalAmountLabel, totalDueLabel);
        
        // Breakdown section
        VBox breakdownBox = new VBox(12);
        breakdownBox.setPadding(new Insets(20));
        breakdownBox.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 12;");
        
        HBox creditRow = new HBox();
        HBox.setHgrow(creditRow, Priority.ALWAYS);
        Label creditLabel = new Label("Return Credit");
        creditLabel.setFont(Font.font("Segoe UI", 14));
        creditLabel.setTextFill(Color.web("#374151"));
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Label creditValue = new Label("₱" + String.format("%.2f", returnCredit));
        creditValue.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        creditValue.setTextFill(Color.web("#10B981"));
        creditRow.getChildren().addAll(creditLabel, spacer1, creditValue);
        
        HBox exchangeRow = new HBox();
        HBox.setHgrow(exchangeRow, Priority.ALWAYS);
        Label exchangeLabel = new Label("Exchange Total");
        exchangeLabel.setFont(Font.font("Segoe UI", 14));
        exchangeLabel.setTextFill(Color.web("#374151"));
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        Label exchangeValue = new Label("₱" + String.format("%.2f", exchangeTotal));
        exchangeValue.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        exchangeValue.setTextFill(Color.web("#111827"));
        exchangeRow.getChildren().addAll(exchangeLabel, spacer2, exchangeValue);
        
        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        
        HBox dueRow = new HBox();
        HBox.setHgrow(dueRow, Priority.ALWAYS);
        Label dueLabel = new Label("Amount Due");
        dueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        dueLabel.setTextFill(Color.web("#111827"));
        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, Priority.ALWAYS);
        Label dueValue = new Label("₱" + String.format("%.2f", amountDue));
        dueValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        dueValue.setTextFill(Color.web("#DC2626"));
        dueRow.getChildren().addAll(dueLabel, spacer3, dueValue);
        
        breakdownBox.getChildren().addAll(creditRow, exchangeRow, separator, dueRow);

        // Change display
        HBox changeRow = new HBox();
        changeRow.setPadding(new Insets(12, 0, 0, 0));
        changeRow.setAlignment(Pos.CENTER_LEFT);
        
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

        leftPanel.getChildren().addAll(headerLabel, totalSection, breakdownBox, changeRow);
        
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
        String[] amounts = {"₱20", "₱50", "₱100", "₱200"};
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
                } else {
                    btn.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #1F2937; -fx-font-size: 20px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-border-width: 0; -fx-cursor: hand;");
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
        exactBtn.setOnAction(e -> {
            amountField.setText(String.format("%.2f", amountDue));
            amountField.setStyle("-fx-background-color: transparent; -fx-text-fill: #111827; -fx-border-width: 0; -fx-padding: 0; -fx-font-size: 40px; -fx-font-weight: bold;");
        });
        
        // Complete Transaction button
        Button completeBtn = new Button("Complete\nEXCHANGE");
        completeBtn.setPrefSize(272, 60);
        completeBtn.setStyle("-fx-background-color: #D1D5DB; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-cursor: hand;");
        completeBtn.setDisable(true);
        
        // Update complete button state and live change display
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double entered = 0;
                if (newVal != null && !newVal.trim().isEmpty() && !newVal.equals("0.00")) {
                    String sanitized = newVal.endsWith(".") ? newVal.substring(0, newVal.length()-1) : newVal;
                    entered = Double.parseDouble(sanitized);
                }
                double change = entered - amountDue;
                if (change < 0) {
                    changeValue.setText("₱0.00");
                    changeValue.setTextFill(Color.web("#DC2626"));
                    changeNote.setText("Short ₱" + String.format("%.2f", Math.abs(change)));
                    changeNote.setTextFill(Color.web("#DC2626"));
                } else {
                    changeValue.setText("₱" + String.format("%.2f", change));
                    changeValue.setTextFill(Color.web("#10B981"));
                    changeNote.setText("");
                }

                if (entered >= amountDue) {
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
        
        // Add hidden buttons
        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);
        
        dialog.getDialogPane().lookupButton(okType).setVisible(false);
        dialog.getDialogPane().lookupButton(cancelType).setVisible(false);
        
        final double[][] result = {null};
        completeBtn.setOnAction(e -> {
            try {
                double cashReceived = Double.parseDouble(amountField.getText());
                if (cashReceived >= amountDue) {
                    result[0] = new double[]{cashReceived, cashReceived - amountDue};
                    dialog.setResult(result[0]);
                    dialog.close();
                }
            } catch (Exception ex) {
                // Invalid input
            }
        });
        
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
    
    // ==================== ORDER QUEUE PANEL ====================
    
    private javafx.scene.control.ScrollPane createOrderQueuePanel() {
        // Main container - 2-column layout
        HBox mainContainer = new HBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #F3F4F6;");
        mainContainer.setFillHeight(true);
        
        // ==================== LEFT COLUMN: Order Lookup ====================
        VBox leftColumn = new VBox(15);
        leftColumn.setPadding(new Insets(0));
        leftColumn.setStyle("-fx-background-color: transparent;");
        leftColumn.setMinWidth(450);
        leftColumn.setPrefWidth(500);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        
        // Header section
        VBox headerBox = new VBox(5);
        headerBox.setAlignment(Pos.TOP_LEFT);
        
        Label title = new Label("🔍 Order Lookup");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#111827"));
        
        Label subtitle = new Label("Enter an order ID to retrieve and process");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.web("#6B7280"));
        
        headerBox.getChildren().addAll(title, subtitle);
        leftColumn.getChildren().add(headerBox);
        
        // Search section
        VBox searchSection = new VBox(10);
        searchSection.setPadding(new Insets(20));
        searchSection.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        
        Label searchLabel = new Label("🔍 Enter Order ID:");
        searchLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        searchLabel.setTextFill(Color.web("#374151"));
        
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        
        TextField orderIdField = new TextField();
        orderIdField.setPromptText("e.g., c25f5698");
        orderIdField.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-border-radius: 8; -fx-border-color: #E5E7EB;");
        orderIdField.setPrefWidth(200);
        HBox.setHgrow(orderIdField, Priority.ALWAYS);
        
        Button searchBtn = new Button("🔍 Search");
        searchBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        
        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("Segoe UI", 12));
        
        searchBox.getChildren().addAll(orderIdField, searchBtn);
        searchSection.getChildren().addAll(searchLabel, searchBox, statusLabel);
        leftColumn.getChildren().add(searchSection);
        
        // Results panel
        VBox resultsPanel = new VBox(12);
        resultsPanel.setPadding(new Insets(20));
        resultsPanel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        VBox.setVgrow(resultsPanel, Priority.ALWAYS);
        
        Label resultsTitle = new Label("📋 Order Details");
        resultsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        resultsTitle.setTextFill(Color.web("#111827"));
        
        TextArea orderDetailsArea = new TextArea();
        orderDetailsArea.setEditable(false);
        orderDetailsArea.setWrapText(true);
        orderDetailsArea.setFont(Font.font("Consolas", 12));
        orderDetailsArea.setText("Enter an order ID and click Search to retrieve order details...");
        orderDetailsArea.setPrefHeight(250);
        orderDetailsArea.setStyle("-fx-control-inner-background: #F9FAFB; -fx-background-color: #F9FAFB; -fx-font-size: 12px; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 8;");
        VBox.setVgrow(orderDetailsArea, Priority.ALWAYS);
        
        // Action buttons (hidden until order found)
        HBox actionButtonsBox = new HBox(8);
        actionButtonsBox.setAlignment(Pos.CENTER_LEFT);
        actionButtonsBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button payBtn = new Button("💳 Pay & Prepare");
        payBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand;");
        payBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(payBtn, Priority.ALWAYS);
        payBtn.setVisible(false);
        
        Button completeBtn = new Button("✓ Ready");
        completeBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand;");
        completeBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(completeBtn, Priority.ALWAYS);
        completeBtn.setVisible(false);
        
        Button cancelBtn = new Button("✗ Cancel");
        cancelBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);
        cancelBtn.setVisible(false);
        
        actionButtonsBox.getChildren().addAll(payBtn, completeBtn, cancelBtn);
        
        resultsPanel.getChildren().addAll(resultsTitle, orderDetailsArea, actionButtonsBox);
        leftColumn.getChildren().add(resultsPanel);
        
        // ==================== RIGHT COLUMN: Order Status ====================
        VBox rightColumn = new VBox(10);
        rightColumn.setPadding(new Insets(0));
        rightColumn.setStyle("-fx-background-color: transparent;");
        rightColumn.setMinWidth(320);
        rightColumn.setPrefWidth(380);
        rightColumn.setMaxWidth(420);
        
        // Right column header
        HBox rightHeader = new HBox(10);
        rightHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label rightTitle = new Label("📺 Live Order Status");
        rightTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        rightTitle.setTextFill(Color.web("#111827"));
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");
        
        rightHeader.getChildren().addAll(rightTitle, headerSpacer, refreshBtn);
        rightColumn.getChildren().add(rightHeader);
        
        // Two status columns stacked vertically (2 rows) with equal height
        VBox statusColumnsContainer = new VBox(10);
        statusColumnsContainer.setFillWidth(true);
        VBox.setVgrow(statusColumnsContainer, Priority.ALWAYS);
        
        // Refresh callback that rebuilds the status columns (use array to allow self-reference)
        final Runnable[] refreshStatusColumnsRef = new Runnable[1];
        refreshStatusColumnsRef[0] = () -> {
            loadPendingOrdersFromFile();
            statusColumnsContainer.getChildren().clear();
            VBox newPreparingCol = createCompactStatusColumn("⏳ PREPARING", "#F59E0B", preparingList, false, refreshStatusColumnsRef[0]);
            VBox newReadyCol = createCompactStatusColumn("✅ READY", "#10B981", completedList, true, refreshStatusColumnsRef[0]);
            newPreparingCol.setMinHeight(200);
            newPreparingCol.setPrefHeight(250);
            newReadyCol.setMinHeight(200);
            newReadyCol.setPrefHeight(250);
            VBox.setVgrow(newPreparingCol, Priority.ALWAYS);
            VBox.setVgrow(newReadyCol, Priority.ALWAYS);
            statusColumnsContainer.getChildren().addAll(newPreparingCol, newReadyCol);
        };
        
        // Preparing column (top row) - takes half the available height
        VBox preparingColumn = createCompactStatusColumn("⏳ PREPARING", "#F59E0B", preparingList, false, refreshStatusColumnsRef[0]);
        preparingColumn.setMinHeight(200);
        preparingColumn.setPrefHeight(250);
        VBox.setVgrow(preparingColumn, Priority.ALWAYS);
        
        // Ready column (bottom row) - takes half the available height
        VBox readyColumn = createCompactStatusColumn("✅ READY", "#10B981", completedList, true, refreshStatusColumnsRef[0]);
        readyColumn.setMinHeight(200);
        readyColumn.setPrefHeight(250);
        VBox.setVgrow(readyColumn, Priority.ALWAYS);
        
        statusColumnsContainer.getChildren().addAll(preparingColumn, readyColumn);
        rightColumn.getChildren().add(statusColumnsContainer);
        
        // Add both columns to main container
        mainContainer.getChildren().addAll(leftColumn, rightColumn);
        
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
            
            String customerName = orderCustomerNames.getOrDefault(foundOrder.getOrderId(), foundOrder.getCustomerName());
            if (customerName == null || customerName.isEmpty()) {
                customerName = "Guest";
            }
            
            // Check if it's an exchange order
            String orderType = foundOrder.getOrderType();
            boolean isExchange = "Exchange".equalsIgnoreCase(orderType);
            
            StringBuilder details = new StringBuilder();
            if (isExchange) {
                details.append("🔄 EXCHANGE ORDER\n");
            } else {
                details.append("ORDER DETAILS\n");
            }
            details.append("\n");
            details.append(String.format("Order #:    %s\n", foundOrder.getOrderId()));
            details.append(String.format("Customer:   %s\n", customerName));
            if (isExchange) {
                details.append(String.format("Type:       Exchange (Return/Exchange)\n"));
            }
            details.append(String.format("Time:       %s\n", 
                foundOrder.getOrderTime().format(DateTimeFormatter.ofPattern("hh:mm:ss a"))));
            details.append(String.format("Status:     %s\n\n", mapStatusToLabel(foundOrder.getStatus())));
            details.append("ITEMS\n");
            details.append("\n");

            double subtotalAmount = 0.0;
            if (foundOrder.getItems().isEmpty()) {
                details.append("No items.\n");
            } else {
                for (PendingOrder.OrderItemData item : foundOrder.getItems()) {
                    // Calculate item total (base price + size + addons) * quantity
                    double itemTotal = (item.price + item.sizeCost + item.addOnsCost) * item.quantity;
                    subtotalAmount += itemTotal;
                    
                    details.append(String.format("%-20s    ₱%.2f\n", item.productName, itemTotal));
                    
                    // Show Size if present
                    if (item.size != null && !item.size.isEmpty()) {
                        details.append(String.format("  Size: %s (+₱%.2f)\n", item.size, item.sizeCost));
                    }
                    
                    // Show Temperature if present
                    if (item.temperature != null && !item.temperature.isEmpty()) {
                        details.append(String.format("  Temperature: %s\n", item.temperature));
                    }
                    
                    // Show Add-ons if present
                    if (item.addOns != null && !item.addOns.isEmpty()) {
                        details.append(String.format("  Add-ons: %s (+₱%.2f)\n", item.addOns, item.addOnsCost));
                    }
                    
                    // Show Special Request / Remarks if present
                    if (item.specialRequest != null && !item.specialRequest.isEmpty()) {
                        details.append(String.format("  Remarks: %s\n", item.specialRequest));
                    }
                    
                    // Show quantity if more than 1
                    if (item.quantity > 1) {
                        details.append(String.format("  Qty: x%d\n", item.quantity));
                    }
                    
                    details.append("\n");
                }
            }

            // Use po.getTotalAmount() as the final VAT-inclusive total (consistent with payment dialog)
            // Calculate VAT breakdown from the stored total
            double totalWithVat = foundOrder.getTotalAmount();
            double subtotalBeforeVat = totalWithVat / 1.12;
            double vatAmount = totalWithVat - subtotalBeforeVat;
            
            details.append(String.format("Subtotal:   ₱%.2f\n", subtotalBeforeVat));
            details.append(String.format("VAT (12%%):  ₱%.2f\n", vatAmount));
            details.append(String.format("TOTAL:      ₱%.2f\n", totalWithVat));

            orderDetailsArea.setText(details.toString());
            
            // Show appropriate action buttons based on order status
            // Only PENDING (unpaid) orders can have action buttons
            payBtn.setVisible(false);
            completeBtn.setVisible(false);
            cancelBtn.setVisible(false);
            
            if (PendingOrder.STATUS_PENDING.equals(foundOrder.getStatus())) {
                payBtn.setVisible(true);
                payBtn.setText("💳 Pay & Prepare");
                cancelBtn.setVisible(true);  // Can only cancel if not yet paid
            }
            // After payment (PAID, PREPARING, COMPLETED) - no action buttons shown
            // Use the Live Order Status panel on the right to manage order flow
        };
        
        // Refresh button - rebuilds status columns
        refreshBtn.setOnAction(e -> refreshStatusColumnsRef[0].run());
        
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
                    // Refresh status columns
                    refreshBtn.fire();
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
                    // Refresh status columns
                    refreshBtn.fire();
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
                // Refresh status columns
                refreshBtn.fire();
            }
        });

        // Wrap the panel in a ScrollPane
        ScrollPane outerScroll = new ScrollPane(mainContainer);
        outerScroll.setFitToWidth(true);
        outerScroll.setFitToHeight(true);
        outerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        outerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        outerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        return outerScroll;
    }
    
    /**
     * Creates a compact status column for the Order Queue panel
     */
    private VBox createCompactStatusColumn(String title, String accentColor,
                                           ObservableList<PendingOrder> orders, boolean isReady,
                                           Runnable onRefresh) {
        VBox column = new VBox(0);
        column.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        column.setMinWidth(220);
        column.setAlignment(Pos.TOP_CENTER);
        
        // Column header
        VBox headerBox = new VBox(5);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(15, 15, 12, 15));
        headerBox.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 12 12 0 0;");
        
        Label headerLabel = new Label(title);
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        headerLabel.setTextFill(Color.WHITE);
        
        Label countBadge = new Label(orders.size() + " order" + (orders.size() != 1 ? "s" : ""));
        countBadge.setFont(Font.font("Segoe UI", 11));
        countBadge.setTextFill(Color.WHITE);
        countBadge.setOpacity(0.9);
        
        headerBox.getChildren().addAll(headerLabel, countBadge);
        
        // Order list
        VBox ordersList = new VBox(8);
        ordersList.setAlignment(Pos.TOP_CENTER);
        ordersList.setPadding(new Insets(12));
        
        if (orders.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(25, 10, 25, 10));
            
            Label emptyIcon = new Label(isReady ? "📭" : "☕");
            emptyIcon.setFont(Font.font(32));
            
            Label emptyLabel = new Label(isReady ? "No orders ready" : "No orders preparing");
            emptyLabel.setFont(Font.font("Segoe UI", 12));
            emptyLabel.setTextFill(Color.web("#9CA3AF"));
            
            emptyState.getChildren().addAll(emptyIcon, emptyLabel);
            ordersList.getChildren().add(emptyState);
        } else {
            for (PendingOrder order : orders) {
                HBox orderCard = createCompactOrderCard(order, accentColor, isReady, onRefresh);
                ordersList.getChildren().add(orderCard);
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(ordersList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMinHeight(200);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        column.getChildren().addAll(headerBox, scrollPane);
        
        return column;
    }
    
    /**
     * Creates a compact order card for the status columns
     */
    private HBox createCompactOrderCard(PendingOrder order, String accentColor, boolean isReady, Runnable onRefresh) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setCursor(javafx.scene.Cursor.HAND);
        
        String bgColor = isReady ? "#ECFDF5" : "#FEF3C7";
        String hoverBgColor = isReady ? "#D1FAE5" : "#FDE68A";
        String borderColor = accentColor;
        
        String baseStyle = "-fx-background-color: " + bgColor + "; " +
                     "-fx-background-radius: 8; " +
                     "-fx-border-color: " + borderColor + "; " +
                     "-fx-border-width: 0 0 0 4; " +
                     "-fx-border-radius: 8;";
        
        String hoverStyle = "-fx-background-color: " + hoverBgColor + "; " +
                     "-fx-background-radius: 8; " +
                     "-fx-border-color: " + borderColor + "; " +
                     "-fx-border-width: 0 0 0 4; " +
                     "-fx-border-radius: 8;";
        
        card.setStyle(baseStyle);
        
        // Hover effects for the entire card
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));
        
        // Order info
        VBox orderInfo = new VBox(2);
        orderInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(orderInfo, Priority.ALWAYS);
        
        Label numberLabel = new Label("#" + order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())));
        numberLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        numberLabel.setTextFill(Color.web("#111827"));
        
        String customerName = orderCustomerNames.getOrDefault(order.getOrderId(), order.getCustomerName());
        if (customerName == null || customerName.isEmpty()) {
            customerName = "Guest";
        }
        
        Label nameLabel = new Label(customerName);
        nameLabel.setFont(Font.font("Segoe UI", 11));
        nameLabel.setTextFill(Color.web("#6B7280"));
        
        orderInfo.getChildren().addAll(numberLabel, nameLabel);
        card.getChildren().add(orderInfo);
        
        // Action button for ready orders
        if (isReady) {
            Button pickedUpBtn = new Button("✓");
            pickedUpBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-radius: 6; -fx-cursor: hand;");
            pickedUpBtn.setOnAction(e -> {
                e.consume();
                completedList.remove(order);
                orderQueue.remove(order);
                TextDatabase.deletePendingOrder(order.getOrderId());
                if (onRefresh != null) onRefresh.run();
            });
            card.getChildren().add(pickedUpBtn);
            
            // Make entire card clickable - same action as button
            card.setOnMouseClicked(e -> {
                completedList.remove(order);
                orderQueue.remove(order);
                TextDatabase.deletePendingOrder(order.getOrderId());
                if (onRefresh != null) onRefresh.run();
            });
        } else {
            // Mark Ready button for preparing orders
            Button readyBtn = new Button("✓ Ready");
            readyBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5 8; -fx-background-radius: 6; -fx-cursor: hand;");
            readyBtn.setOnAction(e -> {
                e.consume();
                order.setStatus(PendingOrder.STATUS_COMPLETED);
                TextDatabase.savePendingOrder(order);
                if (onRefresh != null) onRefresh.run();
            });
            card.getChildren().add(readyBtn);
            
            // Make entire card clickable - same action as button (move to Ready)
            card.setOnMouseClicked(e -> {
                order.setStatus(PendingOrder.STATUS_COMPLETED);
                TextDatabase.savePendingOrder(order);
                if (onRefresh != null) onRefresh.run();
            });
        }
        
        return card;
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
        
        // Search ALL receipts (not just current cashier's) since any cashier can process returns
        List<Receipt> allReceipts = TextDatabase.loadAllReceipts();
        Receipt receipt = allReceipts.stream()
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
        Label title = new Label("My Transactions");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#1a1a1a"));
        
        // Show current cashier badge
        Label cashierBadge = new Label("  👤 " + (currentCashierId != null ? currentCashierId : "Unknown"));
        cashierBadge.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        cashierBadge.setTextFill(Color.web("#FFFFFF"));
        cashierBadge.setStyle("-fx-background-color: #6366F1; -fx-padding: 6 12; -fx-background-radius: 15;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label dateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        dateLabel.setFont(Font.font("Segoe UI", 14));
        dateLabel.setTextFill(Color.web("#6c757d"));
        
        headerBox.getChildren().addAll(title, cashierBadge, spacer, dateLabel);
        
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
                showPrintReceiptDialog(selected);
            } else {
                showAlert("No Selection", "Please select a receipt to print.", Alert.AlertType.WARNING);
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
            // Only show receipts processed by the current logged-in cashier
            if (currentCashierId != null && currentCashierId.equals(receipt.getCashierId())) {
                if (receipt.getOrderId().toLowerCase().contains(searchTerm.toLowerCase()) ||
                    receipt.getUserName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                    receipt.getReceiptId().toLowerCase().contains(searchTerm.toLowerCase())) {
                    receiptHistory.add(receipt);
                }
            }
        }
    }
    
    private void displayReceiptDetails(Receipt receipt) {
        // Always rebuild the receipt to ensure customer and cashier are shown
        StringBuilder details = new StringBuilder();
        details.append("═══════════════════════════════════════\n");
        details.append("          BREWISE COFFEE SHOP\n");
        details.append("═══════════════════════════════════════\n");
        details.append("Receipt ID: ").append(receipt.getReceiptId()).append("\n");
        details.append("Order ID: ").append(receipt.getOrderId()).append("\n");
        
        // Always show customer name
        String customerName = receipt.getUserName();
        if (customerName == null || customerName.isEmpty()) {
            customerName = "Guest";
        }
        details.append("Customer: ").append(customerName).append("\n");
        
        // Always show cashier
        String cashier = receipt.getCashierId();
        if (cashier != null && !cashier.isEmpty()) {
            details.append("Cashier: ").append(cashier).append("\n");
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        details.append("Date: ").append(receipt.getReceiptTime().format(formatter)).append("\n");
        details.append("───────────────────────────────────────\n");
        
        // Try to get items from the stored receipt content or order records
        String storedContent = receipt.getReceiptContent();
        if (storedContent != null && !storedContent.isEmpty()) {
            // Extract items section from stored content
            int itemsStart = storedContent.indexOf("ITEMS:");
            if (itemsStart == -1) {
                // Try alternate format
                int altStart = storedContent.indexOf("───");
                if (altStart > 0) {
                    int nextLine = storedContent.indexOf("\n", altStart);
                    if (nextLine > 0) {
                        int endSection = storedContent.indexOf("───", nextLine);
                        if (endSection > nextLine) {
                            details.append(storedContent.substring(nextLine + 1, endSection));
                        }
                    }
                }
            } else {
                // Find where items section ends (before Subtotal or TOTAL)
                int itemsEnd = storedContent.indexOf("Subtotal:");
                if (itemsEnd == -1) itemsEnd = storedContent.indexOf("TOTAL:");
                if (itemsEnd > itemsStart) {
                    details.append(storedContent.substring(itemsStart, itemsEnd));
                }
            }
        } else {
            // Try to load items from order records
            List<OrderRecord> orderRecords = TextDatabase.loadAllOrders();
            boolean foundItems = false;
            for (OrderRecord rec : orderRecords) {
                if (rec.getOrderId() != null && rec.getOrderId().equals(receipt.getOrderId())) {
                    if (!foundItems) {
                        details.append("ITEMS:\n");
                        foundItems = true;
                    }
                    details.append("  ").append(rec.getItemName()).append(" x1\n");
                }
            }
            if (!foundItems) {
                details.append("(Items not available)\n");
            }
        }
        
        details.append("───────────────────────────────────────\n");
        // Show VAT breakdown
        double totalAmt = receipt.getTotalAmount();
        double subtotalAmt = totalAmt / 1.12;
        double vatAmt = totalAmt - subtotalAmt;
        details.append(String.format("Subtotal:      ₱%.2f\n", subtotalAmt));
        details.append(String.format("VAT (12%%):     ₱%.2f\n", vatAmt));
        details.append(String.format("TOTAL:         ₱%.2f\n", totalAmt));
        details.append("───────────────────────────────────────\n");
        details.append(String.format("Cash Paid:     ₱%.2f\n", receipt.getCashPaid()));
        details.append(String.format("Change:        ₱%.2f\n", receipt.getChange()));
        details.append("═══════════════════════════════════════\n");
        details.append("       Thank you for your order!\n");
        details.append("═══════════════════════════════════════\n");
        
        receiptDetailArea.setText(details.toString());
    }

    /**
     * Shows a print preview dialog with the full receipt content.
     */
    private void showPrintReceiptDialog(Receipt receipt) {
        Dialog<Void> printDialog = new Dialog<>();
        printDialog.setTitle("Print Receipt Preview");
        printDialog.setHeaderText(null);
        printDialog.initModality(Modality.APPLICATION_MODAL);
        
        // Main container
        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(30));
        mainBox.setStyle("-fx-background-color: white;");
        mainBox.setAlignment(Pos.TOP_CENTER);
        
        // Header
        Label headerLabel = new Label("🖨️ Print Receipt Preview");
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        headerLabel.setTextFill(Color.web("#1F2937"));
        
        Label receiptIdLabel = new Label("Receipt #" + receipt.getReceiptId());
        receiptIdLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        receiptIdLabel.setTextFill(Color.web("#6366F1"));
        
        // Receipt content area with styled border to look like paper
        VBox receiptPaper = new VBox(0);
        receiptPaper.setStyle("-fx-background-color: #FFFEF7; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 4; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 2, 2);");
        receiptPaper.setPadding(new Insets(20));
        receiptPaper.setMaxWidth(450);
        
        // Build receipt content
        StringBuilder receiptContent = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        receiptContent.append("═══════════════════════════════════════\n");
        receiptContent.append("          BREWISE COFFEE SHOP\n");
        receiptContent.append("═══════════════════════════════════════\n\n");
        receiptContent.append(String.format("Receipt #:  %s\n", receipt.getReceiptId()));
        receiptContent.append(String.format("Order #:    %s\n", receipt.getOrderId()));
        receiptContent.append(String.format("Customer:   %s\n", receipt.getUserName() != null ? receipt.getUserName() : "Walk-in"));
        receiptContent.append(String.format("Cashier:    %s\n", receipt.getCashierId() != null ? receipt.getCashierId() : "-"));
        receiptContent.append(String.format("Date:       %s\n", receipt.getReceiptTime().format(formatter)));
        receiptContent.append("\n───────────────────────────────────────\n");
        receiptContent.append("ITEMS:\n");
        receiptContent.append("───────────────────────────────────────\n");
        
        // Try to get stored receipt content, otherwise show basic info
        String storedContent = receipt.getReceiptContent();
        if (storedContent != null && !storedContent.isEmpty()) {
            // Extract items section from stored content if available
            int itemsStart = storedContent.indexOf("ITEMS:");
            int itemsEnd = storedContent.indexOf("Subtotal:");
            if (itemsStart >= 0 && itemsEnd >= 0 && itemsEnd > itemsStart) {
                String itemsSection = storedContent.substring(itemsStart + 6, itemsEnd).trim();
                receiptContent.append(itemsSection).append("\n");
            } else {
                receiptContent.append("  (Item details not available)\n");
            }
        } else {
            receiptContent.append("  (Item details not available)\n");
        }
        
        receiptContent.append("\n───────────────────────────────────────\n");
        double totalAmt = receipt.getTotalAmount();
        double subtotalAmt = totalAmt / 1.12;
        double vatAmt = totalAmt - subtotalAmt;
        receiptContent.append(String.format("Subtotal:      ₱%.2f\n", subtotalAmt));
        receiptContent.append(String.format("VAT (12%%):     ₱%.2f\n", vatAmt));
        receiptContent.append(String.format("TOTAL:         ₱%.2f\n", totalAmt));
        receiptContent.append("───────────────────────────────────────\n");
        receiptContent.append(String.format("Cash Paid:     ₱%.2f\n", receipt.getCashPaid()));
        receiptContent.append(String.format("Change:        ₱%.2f\n", receipt.getChange()));
        receiptContent.append("\n═══════════════════════════════════════\n");
        receiptContent.append("       Thank you for your order!\n");
        receiptContent.append("          Please come again!\n");
        receiptContent.append("═══════════════════════════════════════\n");
        
        TextArea receiptArea = new TextArea(receiptContent.toString());
        receiptArea.setEditable(false);
        receiptArea.setWrapText(true);
        receiptArea.setFont(Font.font("Consolas", 12));
        receiptArea.setPrefWidth(420);
        receiptArea.setPrefHeight(450);
        receiptArea.setStyle("-fx-control-inner-background: #FFFEF7; -fx-border-width: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        
        receiptPaper.getChildren().add(receiptArea);
        
        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button confirmPrintBtn = new Button("🖨️ Confirm Print");
        confirmPrintBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 30; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmPrintBtn.setOnAction(ev -> {
            showAlert("Print Success", "Receipt #" + receipt.getReceiptId() + " has been sent to the printer.", Alert.AlertType.INFORMATION);
            printDialog.close();
        });
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 30; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(ev -> printDialog.close());
        
        buttonBox.getChildren().addAll(confirmPrintBtn, cancelBtn);
        
        mainBox.getChildren().addAll(headerLabel, receiptIdLabel, receiptPaper, buttonBox);
        
        printDialog.getDialogPane().setContent(mainBox);
        printDialog.getDialogPane().setStyle("-fx-background-color: #F3F4F6;");
        printDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        printDialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        
        printDialog.showAndWait();
    }

    private void showReturnExchangeDialog(Receipt receipt) {
        // Check if this receipt has already been processed for return/exchange
        if (TextDatabase.isReceiptAlreadyReturned(receipt.getReceiptId())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Return Not Allowed");
            alert.setHeaderText("Already Processed");
            alert.setContentText("This receipt has already been processed for return/exchange.\n\nEach receipt can only be used for one return/exchange transaction.");
            alert.showAndWait();
            return;
        }
        
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

        // Get original order - first try pending orders, then try order records
        PendingOrder originalOrder = TextDatabase.getPendingOrderById(receipt.getOrderId());
        
        // If pending order not found, try to reconstruct from order records
        if (originalOrder == null || originalOrder.getItems() == null || originalOrder.getItems().isEmpty()) {
            // Get all order records and filter by this order ID
            List<OrderRecord> allOrderRecords = TextDatabase.loadAllOrders();
            List<OrderRecord> orderRecords = new ArrayList<>();
            for (OrderRecord rec : allOrderRecords) {
                if (rec.getOrderId() != null && rec.getOrderId().equals(receipt.getOrderId())) {
                    orderRecords.add(rec);
                }
            }
            
            if (!orderRecords.isEmpty()) {
                // Reconstruct pending order from order records
                originalOrder = new PendingOrder(receipt.getOrderId(), receipt.getUserName());
                originalOrder.setStatus(PendingOrder.STATUS_COMPLETED);
                
                for (OrderRecord rec : orderRecords) {
                    // Find the product to get price
                    Product product = store.getProducts().stream()
                        .filter(p -> p.getName().equalsIgnoreCase(rec.getItemName()))
                        .findFirst()
                        .orElse(null);
                    
                    double price = product != null ? product.getPrice() : 0.0;
                    String temp = rec.getTypeOfDrink() != null ? rec.getTypeOfDrink() : "Hot";
                    
                    originalOrder.addItem(rec.getItemName(), price, 1, temp, 0);
                }
            }
        }
        
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
        
        // Right: Exchange items + Complaints
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setStyle("-fx-background-color: white;");
        
        // Exchange Items Section
        Label rightTitle = new Label("Exchange Items");
        rightTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        
        Label exchangeInfo = new Label("Select products to exchange returned items with:");
        exchangeInfo.setFont(Font.font("Segoe UI", 12));
        exchangeInfo.setTextFill(Color.web("#666"));
        exchangeInfo.setWrapText(true);
        
        ScrollPane exchangeScroll = new ScrollPane();
        exchangeScroll.setFitToWidth(true);
        exchangeScroll.setPrefHeight(200);
        exchangeScroll.setStyle("-fx-background-color: transparent;");
        
        VBox exchangeContainer = new VBox(10);
        javafx.collections.ObservableList<com.coffeeshop.model.OrderItem> exchangeItems = 
            javafx.collections.FXCollections.observableArrayList();
        
        Button addExchangeBtn = new Button("+ Add Exchange Item");
        addExchangeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20;");
        addExchangeBtn.setDisable(true); // Disabled initially
        addExchangeBtn.setOnAction(e -> showProductSelector(dialogStage, exchangeItems, exchangeContainer));
        
        exchangeScroll.setContent(exchangeContainer);
        
        // Complaints Section - Check if order already has a complaint
        Separator complaintSeparator = new Separator();
        complaintSeparator.setPadding(new Insets(10, 0, 10, 0));
        
        // Check existing complaint status for this order
        com.coffeeshop.model.Complaint existingComplaint = Store.getInstance().getComplaintByOrderId(receipt.getOrderId());
        boolean hasExistingComplaint = existingComplaint != null;
        boolean isComplaintResolved = hasExistingComplaint && "RESOLVED".equalsIgnoreCase(existingComplaint.getStatus());
        boolean isPendingApproval = hasExistingComplaint && "PENDING_APPROVAL".equalsIgnoreCase(existingComplaint.getStatus());
        boolean isRejected = hasExistingComplaint && "REJECTED".equalsIgnoreCase(existingComplaint.getStatus());
        boolean isOpen = hasExistingComplaint && "OPEN".equalsIgnoreCase(existingComplaint.getStatus());
        
        Label complaintTitle = new Label("😕 File a Complaint (Optional)");
        complaintTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        complaintTitle.setTextFill(Color.web("#3b82f6"));
        
        CheckBox fileComplaintCheck = new CheckBox("Include complaint with this return");
        fileComplaintCheck.setStyle("-fx-font-size: 12px;");
        
        // Show complaint status if already exists
        Label complaintStatusLabel = new Label();
        complaintStatusLabel.setFont(Font.font("Segoe UI", 12));
        complaintStatusLabel.setWrapText(true);
        
        // Button to request admin approval for resolution
        Button requestResolutionBtn = new Button("📩 Request Admin Approval");
        requestResolutionBtn.setStyle("-fx-background-color: #8B5CF6; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        requestResolutionBtn.setVisible(false);
        requestResolutionBtn.setManaged(false);
        
        if (hasExistingComplaint) {
            fileComplaintCheck.setDisable(true);
            fileComplaintCheck.setSelected(false);
            if (isComplaintResolved) {
                complaintStatusLabel.setText("✅ A complaint for this order has been RESOLVED.\nComplaint ID: " + existingComplaint.getId());
                complaintStatusLabel.setTextFill(Color.web("#10B981"));
                complaintTitle.setText("😊 Complaint Resolved");
                complaintTitle.setTextFill(Color.web("#10B981"));
            } else if (isPendingApproval) {
                complaintStatusLabel.setText("⏳ Resolution is PENDING ADMIN APPROVAL.\nComplaint ID: " + existingComplaint.getId() + "\nPlease wait for admin to approve.");
                complaintStatusLabel.setTextFill(Color.web("#8B5CF6"));
                complaintTitle.setText("⏳ Pending Admin Approval");
                complaintTitle.setTextFill(Color.web("#8B5CF6"));
            } else if (isRejected) {
                complaintStatusLabel.setText("❌ Resolution was REJECTED by admin.\nComplaint ID: " + existingComplaint.getId() + "\nYou may file a new complaint if needed.");
                complaintStatusLabel.setTextFill(Color.web("#EF4444"));
                complaintTitle.setText("❌ Resolution Rejected");
                complaintTitle.setTextFill(Color.web("#EF4444"));
                // Allow filing a new complaint if rejected
                fileComplaintCheck.setDisable(false);
            } else if (isOpen) {
                complaintStatusLabel.setText("🔴 Complaint is OPEN.\nComplaint ID: " + existingComplaint.getId() + "\nClick below to request admin approval for resolution.");
                complaintStatusLabel.setTextFill(Color.web("#F59E0B"));
                complaintTitle.setText("🔴 Complaint Open");
                complaintTitle.setTextFill(Color.web("#F59E0B"));
                // Show request resolution button
                requestResolutionBtn.setVisible(true);
                requestResolutionBtn.setManaged(true);
                
                // Handle request resolution button click
                final com.coffeeshop.model.Complaint complaintToResolve = existingComplaint;
                requestResolutionBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Request Resolution");
                    confirm.setHeaderText("Request Admin Approval");
                    confirm.setContentText("This will send a request to the admin to approve the resolution of this complaint.\n\nDo you want to proceed?");
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            complaintToResolve.setStatus("PENDING_APPROVAL");
                            Store.getInstance().saveComplaint(complaintToResolve);
                            
                            // Update UI
                            complaintStatusLabel.setText("⏳ Resolution is PENDING ADMIN APPROVAL.\nComplaint ID: " + complaintToResolve.getId() + "\nPlease wait for admin to approve.");
                            complaintStatusLabel.setTextFill(Color.web("#8B5CF6"));
                            complaintTitle.setText("⏳ Pending Admin Approval");
                            complaintTitle.setTextFill(Color.web("#8B5CF6"));
                            requestResolutionBtn.setVisible(false);
                            requestResolutionBtn.setManaged(false);
                            
                            Alert success = new Alert(Alert.AlertType.INFORMATION);
                            success.setTitle("Request Sent");
                            success.setHeaderText("Resolution Request Submitted");
                            success.setContentText("Your request has been sent to the admin for approval.");
                            success.showAndWait();
                        }
                    });
                });
            } else {
                complaintStatusLabel.setText("⏳ A complaint for this order is PENDING.\nComplaint ID: " + existingComplaint.getId() + "\nPlease wait for admin to resolve it.");
                complaintStatusLabel.setTextFill(Color.web("#F59E0B"));
                complaintTitle.setText("⏳ Complaint Pending");
                complaintTitle.setTextFill(Color.web("#F59E0B"));
            }
        }
        
        VBox complaintFieldsBox = new VBox(10);
        complaintFieldsBox.setVisible(false);
        complaintFieldsBox.setManaged(false);
        
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
        issueTypeCombo.setStyle("-fx-font-size: 12px;");
        issueTypeCombo.setPrefWidth(Double.MAX_VALUE);
        
        TextArea complaintDescArea = new TextArea();
        complaintDescArea.setPromptText("Describe the issue...");
        complaintDescArea.setPrefRowCount(3);
        complaintDescArea.setWrapText(true);
        complaintDescArea.setStyle("-fx-font-size: 12px;");
        
        complaintFieldsBox.getChildren().addAll(issueTypeCombo, complaintDescArea);
        
        fileComplaintCheck.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            complaintFieldsBox.setVisible(isSelected);
            complaintFieldsBox.setManaged(isSelected);
        });
        
        // Store complaint fields for later access
        final ComboBox<String> issueTypeComboRef = issueTypeCombo;
        final TextArea complaintDescAreaRef = complaintDescArea;
        final CheckBox fileComplaintCheckRef = fileComplaintCheck;
        final boolean hasExistingComplaintRef = hasExistingComplaint;
        
        // Build complaint section based on whether complaint exists
        if (hasExistingComplaint) {
            rightPanel.getChildren().addAll(rightTitle, new Separator(), exchangeInfo, addExchangeBtn, exchangeScroll,
                    complaintSeparator, complaintTitle, complaintStatusLabel, requestResolutionBtn);
        } else {
            rightPanel.getChildren().addAll(rightTitle, new Separator(), exchangeInfo, addExchangeBtn, exchangeScroll,
                    complaintSeparator, complaintTitle, fileComplaintCheck, complaintFieldsBox);
        }
        
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
            // Handle optional complaint submission (only if no existing complaint)
            if (fileComplaintCheckRef.isSelected() && !hasExistingComplaint) {
                String issueType = issueTypeComboRef.getValue();
                String description = complaintDescAreaRef.getText().trim();
                
                if (issueType == null || issueType.isEmpty()) {
                    showAlert("Validation Error", "Please select an issue type for the complaint.", Alert.AlertType.WARNING);
                    return;
                }
                if (description.isEmpty()) {
                    showAlert("Validation Error", "Please provide a description for the complaint.", Alert.AlertType.WARNING);
                    return;
                }
                
                // Double-check no complaint exists (in case of race condition)
                if (Store.getInstance().hasAnyComplaint(receipt.getOrderId())) {
                    showAlert("Complaint Exists", "A complaint has already been filed for this order.", Alert.AlertType.WARNING);
                    return;
                }
                
                // Submit the complaint
                try {
                    String cmpId = "CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    com.coffeeshop.model.Complaint complaint = new com.coffeeshop.model.Complaint(
                        cmpId,
                        receipt.getOrderId(),
                        receipt.getUserName(),
                        issueType,
                        description,
                        currentCashierId != null ? currentCashierId : "Unknown"
                    );
                    
                    // Save via Store so listeners are notified (admin will see it)
                    Store.getInstance().saveComplaint(complaint);
                } catch (Exception ex) {
                    showAlert("Complaint Error", "Failed to save complaint: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
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
        
        // If additional payment is required, use the same payment dialog as order checkout
        double amountReceived = 0.0;
        String paymentMethod = "";
        if (additionalPayment > 0) {
            // Show the same styled cash payment dialog used in order checkout
            double[] paymentResult = showExchangePaymentDialog(additionalPayment, returnCredit, exchangeTotal);
            if (paymentResult == null) {
                return; // User cancelled
            }
            amountReceived = paymentResult[0];
            paymentMethod = "CASH";
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
        
        // If there are exchange items, create a new order and add to Order Status as PREPARING
        String exchangeOrderId = "";
        if (!exchangeItems.isEmpty()) {
            // Generate new order ID for the exchange order
            exchangeOrderId = "EX-" + String.format("%03d", (int)(Math.random() * 1000));
            
            // Get the original customer name
            String customerName = originalReceipt.getUserName();
            if (customerName == null || customerName.isEmpty() || customerName.equalsIgnoreCase("Guest")) {
                customerName = "Exchange Customer";
            }
            
            // Create new pending order for exchange items with "Exchange" order type
            PendingOrder exchangeOrder = new PendingOrder(exchangeOrderId, customerName, "Exchange", currentCashierId);
            exchangeOrder.setStatus(PendingOrder.STATUS_PREPARING); // Set to PREPARING so it goes to Preparing container
            
            // Add each exchange item to the pending order
            for (com.coffeeshop.model.OrderItem item : exchangeItems) {
                String productName = item.getProduct().getName();
                double price = item.getProduct().getPrice();
                int quantity = item.getQuantity();
                String temp = "Hot"; // Default temperature
                int addOnCount = 0;
                
                exchangeOrder.addItem(productName, price, quantity, temp, addOnCount);
            }
            
            // Save and refresh the order queue
            TextDatabase.savePendingOrder(exchangeOrder);
            loadPendingOrdersFromFile();
        }
        
        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Success");
        success.setHeaderText("Return/Exchange Processed");
        
        // Build the success message with new structure
        StringBuilder successMsg = new StringBuilder();
        successMsg.append(String.format("Return ID: %s\n", returnId));
        if (!exchangeOrderId.isEmpty()) {
            successMsg.append(String.format("Order ID: %s\n", exchangeOrderId));
        }
        successMsg.append(String.format("Return Credit: ₱%.2f\n", returnTx.getReturnCredit()));
        successMsg.append(String.format("Exchange Total: ₱%.2f\n", returnTx.getExchangeTotal()));
        if (additionalPayment > 0) {
            successMsg.append(String.format("Payment Method: %s\n", paymentMethod));
            successMsg.append(String.format("Amount Received: ₱%.2f\n", returnTx.getAmountReceived()));
            successMsg.append(String.format("Change: ₱%.2f", returnTx.getChangeAmount()));
        }
        
        success.setContentText(successMsg.toString());
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
        panel.setStyle("-fx-background-color: linear-gradient(to bottom, #F8FAFC, #E2E8F0);");
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setFillWidth(true);
        
        // Header - Clean, minimal with good contrast
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(30, 20, 20, 20));
        header.setStyle("-fx-background-color: transparent;");
        
        Label titleLabel = new Label("🍽 ORDER STATUS");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        titleLabel.setTextFill(Color.web("#1F2937"));
        
        header.getChildren().add(titleLabel);
        
        // Main content - two columns with clear separation
        HBox columnsContainer = new HBox(40);
        columnsContainer.setAlignment(Pos.TOP_CENTER);
        columnsContainer.setPadding(new Insets(20, 50, 20, 50));
        columnsContainer.setFillHeight(true);
        HBox.setHgrow(columnsContainer, Priority.ALWAYS);
        
        // Left Column - PREPARING (Bright Orange)
        VBox preparingColumn = createCleanStatusColumn(
            "PREPARING",
            "#F59E0B",  // Bright amber/orange
            preparingList,
            false
        );
        HBox.setHgrow(preparingColumn, Priority.ALWAYS);
        
        // Right Column - READY FOR PICKUP (Vibrant Green)
        VBox readyColumn = createCleanStatusColumn(
            "READY FOR PICKUP", 
            "#10B981",  // Emerald green
            completedList,
            true
        );
        HBox.setHgrow(readyColumn, Priority.ALWAYS);
        
        columnsContainer.getChildren().addAll(preparingColumn, readyColumn);
        
        // Footer with timestamp
        HBox footer = new HBox(20);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(20));
        footer.setStyle("-fx-background-color: transparent;");
        
        Label timeLabel = new Label(java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy  •  hh:mm a")));
        timeLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        timeLabel.setTextFill(Color.web("#64748B"));
        
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
        column.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
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
        countBadge.setOpacity(0.9);
        
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
            emptyLabel.setTextFill(Color.web("#6B7280"));
            
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
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
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
        
        // Card styling - clean, modern look with light backgrounds that match the accent
        String bgColor = isReady ? "#ECFDF5" : "#FFFBEB";  // Light green / Light amber
        String borderColor = isReady ? "#10B981" : "#F59E0B";
        String textColor = isReady ? "#065F46" : "#78350F";  // Dark green / Dark amber text
        
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
        numberLabel.setTextFill(Color.web(textColor));
        
        // Customer name
        String customerName = orderCustomerNames.getOrDefault(order.getOrderId(), order.getCustomerName());
        if (customerName == null || customerName.isEmpty()) {
            customerName = "Guest";
        }
        
        Label nameLabel = new Label(customerName);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        nameLabel.setTextFill(Color.web("#6B7280"));
        
        orderInfo.getChildren().addAll(numberLabel, nameLabel);
        card.getChildren().add(orderInfo);
        
        // For ready orders, add "Picked Up" button
        if (isReady) {
            Button pickedUpBtn = new Button("✓ Picked Up");
            pickedUpBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            pickedUpBtn.setStyle("-fx-background-color: #10B981; " +
                               "-fx-text-fill: white; " +
                               "-fx-padding: 10 18; " +
                               "-fx-background-radius: 8; " +
                               "-fx-cursor: hand;");
            
            // Hover effect
            pickedUpBtn.setOnMouseEntered(e -> 
                pickedUpBtn.setStyle("-fx-background-color: #059669; " +
                                   "-fx-text-fill: white; " +
                                   "-fx-padding: 10 18; " +
                                   "-fx-background-radius: 8; " +
                                   "-fx-cursor: hand;"));
            pickedUpBtn.setOnMouseExited(e -> 
                pickedUpBtn.setStyle("-fx-background-color: #10B981; " +
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
            Label timeLabel = new Label("⏳ In Progress...");
            timeLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
            timeLabel.setTextFill(Color.web("#B45309"));  // Dark amber to match light amber background
            card.getChildren().add(timeLabel);
        }
        
        return card;
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
