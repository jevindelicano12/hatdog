package com.coffeeshop;

import com.coffeeshop.model.*;
import com.coffeeshop.service.Store;
import com.coffeeshop.service.PersistenceManager;
import com.coffeeshop.service.TextDatabase;
import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.canvas.Canvas;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import com.coffeeshop.service.SalesAnalytics;
import com.coffeeshop.model.Receipt;
import com.coffeeshop.model.ItemRecord;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import java.time.format.DateTimeFormatter;

public class AdminApp extends Application {
    private Store store;
    // current sales chart granularity (Day/Month/Year/All/Custom)
    private String salesGranularity = "Day";
    private TableView<ProductRow> productTable;
    private TableView<InventoryRow> inventoryTable;
    private java.util.Deque<UndoAction> undoStack = new java.util.ArrayDeque<>();
    private Label adminTimeLabel;
    private Label netSalesLabel;
    private Label pendingOrdersLabel;
    private Label completedOrdersLabel;
    private javafx.scene.control.ListView<String> categoriesListView;
    private javafx.scene.control.ListView<String> productsInCategoryListView;

    // Modern button styling helpers
    private void stylePrimaryButton(Button btn) {
        btn.setStyle("-fx-background-color: linear-gradient(to right, #6366F1, #4F46E5); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #4F46E5, #4338CA); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #6366F1, #4F46E5); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);"));
    }

    private void styleSuccessButton(Button btn) {
        btn.setStyle("-fx-background-color: linear-gradient(to right, #27AE60, #229954); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #229954, #1E8449); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #27AE60, #229954); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);"));
    }

    private void styleDangerButton(Button btn) {
        btn.setStyle("-fx-background-color: linear-gradient(to right, #E74C3C, #C0392B); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #C0392B, #A93226); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #E74C3C, #C0392B); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);"));
    }

    private void styleSecondaryButton(Button btn) {
        btn.setStyle("-fx-background-color: linear-gradient(to right, #95A5A6, #7F8C8D); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #7F8C8D, #6C7A89); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #95A5A6, #7F8C8D); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);"));
    }

    private VBox currentContentArea;
    private VBox dashboardContent, productsContent, inventoryContent, addOnsContent, specialRequestsContent, categoriesContent, accountsContent, reportsContent, transactionHistoryContent, maintenanceContent;
    private VBox archivedContent;
    private VBox complaintsContent;
    private VBox remittanceContent;
    private TableView<InventoryRow> archivedTable;

    @Override
    public void start(Stage primaryStage) {
        store = Store.getInstance();
        // Listen for inventory changes so admin UI updates ingredient lists and inventory table
        try {
            Store.getInstance().addInventoryChangeListener(() -> {
                javafx.application.Platform.runLater(() -> {
                    try { refreshData(); } catch (Exception ignored) {}
                });
            });
        } catch (Exception ignored) {}

        primaryStage.setTitle("brewise - Admin Panel");

        // Show admin login BEFORE setting up the scene
        boolean ok = showAdminLogin(null);
        if (!ok) {
            // User cancelled or failed to login - exit application
            javafx.application.Platform.exit();
            return;
        }

        // Main container with sidebar
        HBox root = new HBox(0);
        root.setStyle("-fx-background-color: #F3F4F6;");

        // Left Sidebar
        VBox sidebar = createSidebar();
        
        // Right Content Area (will be swapped based on nav selection)
        currentContentArea = new VBox();
        currentContentArea.setStyle("-fx-background-color: #F3F4F6;");
        HBox.setHgrow(currentContentArea, Priority.ALWAYS);
        
        // Initialize all content panels
        dashboardContent = createDashboardTab();
        productsContent = createProductsTab();
        inventoryContent = createInventoryTab();
        addOnsContent = createAddOnsTab();
        specialRequestsContent = createSpecialRequestsTab();
        categoriesContent = createCategoriesTab();
        accountsContent = createAccountsTab();
        archivedContent = createArchivedTab();
        maintenanceContent = createMaintenanceTab();
        complaintsContent = createComplaintsTab();
        remittanceContent = createRemittanceTab();
        reportsContent = createReportsTab();
        transactionHistoryContent = createTransactionHistoryTab();
        
        // Show dashboard by default
        showContent(dashboardContent);
        
        root.getChildren().addAll(sidebar, currentContentArea);

        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);

        // Apply bundled Atlantafx fallback stylesheet (no external dependency required)
        applyAtlantafx(scene);

        primaryStage.setMaximized(true);
        primaryStage.show();

        refreshData();

        // Register complaint listener: show a brief notification and allow opening complaints view
        try {
            Store.getInstance().addComplaintChangeListener(() -> {
                // run on FX thread
                Platform.runLater(() -> {
                    try {
                        java.util.List<com.coffeeshop.model.Complaint> all = Store.getInstance().getAllComplaints();
                        if (all == null || all.isEmpty()) return;
                        com.coffeeshop.model.Complaint latest = all.get(all.size() - 1);

                        Alert a = new Alert(Alert.AlertType.INFORMATION);
                        a.setTitle("New Complaint Received");
                        a.setHeaderText("📝 New complaint: " + latest.getIssueType());
                        String summary = String.format("Order: %s\nCustomer: %s\nCashier: %s\n%<s", latest.getOrderId(), latest.getCustomerName(), latest.getCashierId());
                        a.setContentText(summary);

                        ButtonType openBtn = new ButtonType("Open", ButtonBar.ButtonData.OK_DONE);
                        a.getButtonTypes().setAll(openBtn, ButtonType.CLOSE);
                        java.util.Optional<ButtonType> res = a.showAndWait();
                        if (res.isPresent() && res.get() == openBtn) {
                            showComplaintsDialog();
                        }
                    } catch (Exception ignored) {}
                });
            });
        } catch (Exception ignored) {}
    }
    
    private void showContent(VBox content) {
        currentContentArea.getChildren().clear();
        currentContentArea.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
    }

    // Helper to filter available inventory ingredient names by product category
    private java.util.List<String> filterIngredientsByCategory(java.util.List<String> base, String category) {
        if (base == null) return new java.util.ArrayList<>();
        if (category == null) return new java.util.ArrayList<>(base);

        String c = category.toLowerCase();
        java.util.List<String> out = new java.util.ArrayList<>();

        for (String ing : base) {
            String l = ing.toLowerCase();
            boolean add = false;
            if (c.contains("coffee")) {
                add = l.contains("coffee") || l.contains("bean") || l.contains("espresso") || l.contains("milk") || l.contains("water") || l.contains("syrup") || l.contains("sugar");
            } else if (c.contains("frappe") || c.contains("blended")) {
                add = l.contains("milk") || l.contains("ice") || l.contains("cream") || l.contains("syrup") || l.contains("sugar") || l.contains("fruit");
            } else if (c.contains("tea") || c.contains("milk tea")) {
                add = l.contains("tea") || l.contains("milk") || l.contains("syrup") || l.contains("sugar") || l.contains("fruit");
            } else if (c.contains("pastr") || c.contains("snack") || c.contains("bakery")) {
                add = !l.contains("water") && !l.contains("ice") && (l.contains("flour") || l.contains("sugar") || l.contains("butter") || l.contains("chocolate") || l.contains("egg") || l.contains("milk"));
            } else {
                // default: include most common pantry items
                add = true;
            }

            if (add) out.add(ing);
        }

        // If filtering produced empty list, fall back to full base list
        if (out.isEmpty()) return new java.util.ArrayList<>(base);
        return out;
    }

    // Show a simple admin login dialog. Returns true if login successful.
    private boolean showAdminLogin(Stage owner) {
        final Stage dialog = new Stage();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Admin Login");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(40));
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: #F4F1EA;");

        // Logo Section
        ImageView logo = null;
        try {
            File logoFile = new File("data/images/LOGO3.png");
            if (logoFile.exists()) {
                javafx.scene.image.Image logoImage = new javafx.scene.image.Image(logoFile.toURI().toString());
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
        Label titleLabel = new Label("🔐 Admin Login");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        titleLabel.setTextFill(Color.web("#3E2723"));
        titleLabel.setStyle("-fx-font-weight: bold;");
        
        Label subtitleLabel = new Label("Enter admin credentials to access the Admin Panel");
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setTextFill(Color.web("#795548"));
        
        Label userLabel = new Label("Username:");
        userLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        userLabel.setTextFill(Color.web("#3E2723"));
        
        TextField userField = new TextField();
        userField.setPromptText("username");
        userField.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #D7CCC8; -fx-border-radius: 6;");
        userField.setPrefWidth(300);

        Label passLabel = new Label("Password:");
        passLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        passLabel.setTextFill(Color.web("#3E2723"));
        
        PasswordField passField = new PasswordField();
        passField.setPromptText("password");
        passField.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #D7CCC8; -fx-border-radius: 6;");
        passField.setPrefWidth(300);

        // Visible text field for optional "show password" behavior
        TextField passVisible = new TextField();
        passVisible.setPromptText("password");
        passVisible.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #D7CCC8; -fx-border-radius: 6;");
        passVisible.setPrefWidth(300);
        passVisible.setManaged(false);
        passVisible.setVisible(false);

        CheckBox showPass = new CheckBox("Show Password");
        showPass.setFont(Font.font("Segoe UI", 12));
        showPass.setTextFill(Color.web("#5D4037"));

        Button loginBtn = new Button("Login");
        loginBtn.setStyle("-fx-background-color: #6F4E37; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 40; -fx-background-radius: 8; -fx-cursor: hand;");
        loginBtn.setPrefWidth(140);
        loginBtn.setDisable(true);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #5D4037; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 40; -fx-background-radius: 8; -fx-cursor: hand;");
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
        grid.add(userLabel, 0, row++, 2, 1);
        grid.add(userField, 0, row++, 2, 1);
        grid.add(passLabel, 0, row++, 2, 1);
        grid.add(passField, 0, row, 2, 1);
        grid.add(passVisible, 0, row, 2, 1);
        row++;
        grid.add(showPass, 0, row++, 2, 1);
        grid.add(actions, 0, row++, 2, 1);

        Scene scene = new Scene(grid, 550, 700);
        scene.setFill(Color.web("#F4F1EA"));
        dialog.setScene(scene);
        dialog.setResizable(false);

        // Helper to update login button disabled state
        Runnable updateLoginDisabled = () -> {
            String userTxt = userField.getText() == null ? "" : userField.getText().trim();
            String passTxt = showPass.isSelected() ? passVisible.getText() : passField.getText();
            passTxt = passTxt == null ? "" : passTxt;
            loginBtn.setDisable(userTxt.isEmpty() || passTxt.trim().isEmpty());
        };

        userField.textProperty().addListener((obs, o, n) -> updateLoginDisabled.run());
        passField.textProperty().addListener((obs, o, n) -> updateLoginDisabled.run());
        passVisible.textProperty().addListener((obs, o, n) -> updateLoginDisabled.run());

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
            updateLoginDisabled.run();
        });

        // Allow pressing Enter on password fields
        passField.setOnAction(e -> loginBtn.fire());
        passVisible.setOnAction(e -> loginBtn.fire());

        final boolean[] loginSuccess = {false};
        loginBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = showPass.isSelected() ? passVisible.getText() : passField.getText();
            if ("admin".equals(user) && "admin123".equals(pass)) {
                loginSuccess[0] = true;
                dialog.close();
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Login Failed");
                a.setHeaderText(null);
                a.setContentText("Invalid username or password. Please try again.");
                a.showAndWait();
            }
        });

        cancelBtn.setOnAction(e -> {
            loginSuccess[0] = false;
            dialog.close();
        });

        dialog.showAndWait();
        return loginSuccess[0];
    }

    private Button activeNavButton = null;
    private boolean maintenanceMode = TextDatabase.isMaintenanceMode(); // Load from file on startup

    private VBox createSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(230);
        sidebar.setMaxWidth(230);
        sidebar.setStyle("-fx-background-color: #FFFFFF;");
        
        // Logo and branding at top
        VBox brandSection = new VBox(5);
        brandSection.setPadding(new Insets(25, 20, 25, 20));
        brandSection.setAlignment(Pos.CENTER_LEFT);
        
        HBox logoRow = new HBox(12);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        
        // Coffee cup icon
        Label cupIcon = new Label("☕");
        cupIcon.setFont(Font.font("Segoe UI", 24));
        cupIcon.setTextFill(Color.web("#1F2937"));
        
        VBox brandText = new VBox(0);
        Label brewiseLabel = new Label("brewise");
        brewiseLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        brewiseLabel.setTextFill(Color.web("#1F2937"));
        
        Label adminLabel = new Label("ADMIN PANEL");
        adminLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        adminLabel.setTextFill(Color.web("#6B7280"));
        
        brandText.getChildren().addAll(brewiseLabel, adminLabel);
        logoRow.getChildren().addAll(cupIcon, brandText);
        brandSection.getChildren().add(logoRow);
        
        // Navigation sections
        VBox navContainer = new VBox(15);
        navContainer.setPadding(new Insets(10, 0, 20, 0));
        
        // BUSINESS section
        Label businessHeader = new Label("BUSINESS");
        businessHeader.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        businessHeader.setTextFill(Color.web("#6B7280"));
        businessHeader.setPadding(new Insets(5, 20, 5, 20));
        
        Button dashboardBtn = createNavButton("📊", "Dashboard", true);
        Button reportsBtn = createNavButton("📈", "Sales Reports", false);
        Button transactionsBtn = createNavButton("💳", "Transactions", false);
        Button remittanceBtn = createNavButton("💰", "Remittance", false);
        
        VBox businessSection = new VBox(0);
        businessSection.getChildren().addAll(businessHeader, dashboardBtn, reportsBtn, transactionsBtn, remittanceBtn);
        
        // MANAGEMENT section
        Label managementHeader = new Label("MANAGEMENT");
        managementHeader.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        managementHeader.setTextFill(Color.web("#6B7280"));
        managementHeader.setPadding(new Insets(5, 20, 5, 20));
        
        Button productsBtn = createNavButton("📦", "Products", false);
        Button inventoryBtn = createNavButton("📋", "Inventory", false);
        Button addOnsBtn = createNavButton("➕", "Add-ons", false);
        Button specialReqBtn = createNavButton("💬", "Special Requests", false);
        Button categoriesBtn = createNavButton("🗂️", "Categories", false);
        Button accountsBtn = createNavButton("👥", "Accounts", false);
        
        VBox managementSection = new VBox(0);
        managementSection.getChildren().addAll(managementHeader, productsBtn, inventoryBtn, addOnsBtn, specialReqBtn, categoriesBtn, accountsBtn);
        
        // SUPPORT section
        Label supportHeader = new Label("SUPPORT");
        supportHeader.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        supportHeader.setTextFill(Color.web("#6B7280"));
        supportHeader.setPadding(new Insets(5, 20, 5, 20));
        
        Button complaintsBtn = createNavButton("😕", "Complaints", false);
        
        VBox supportSection = new VBox(0);
        supportSection.getChildren().addAll(supportHeader, complaintsBtn);
        
        // SYSTEM section
        Label systemHeader = new Label("SYSTEM");
        systemHeader.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        systemHeader.setTextFill(Color.web("#6B7280"));
        systemHeader.setPadding(new Insets(5, 20, 5, 20));
        
        // Archived items removed from sidebar
        Button maintenanceBtn = createNavButton("🛠️", "Maintenance", false);
        
        VBox systemSection = new VBox(0);
        systemSection.getChildren().addAll(systemHeader, maintenanceBtn);
        
        navContainer.getChildren().addAll(businessSection, managementSection, supportSection, systemSection);
        
        // Admin user at bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        HBox adminUserBox = new HBox(10);
        adminUserBox.setPadding(new Insets(15, 20, 20, 20));
        adminUserBox.setAlignment(Pos.CENTER_LEFT);
        adminUserBox.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 10; -fx-border-color: #E5E7EB; -fx-border-radius: 10; -fx-border-width: 1;");
        
        Label adminIcon = new Label("👤");
        adminIcon.setFont(Font.font(20));
        adminIcon.setTextFill(Color.web("#FFA500"));
        
        Label adminNameLabel = new Label("Administrator");
        adminNameLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        adminNameLabel.setTextFill(Color.web("#374151"));
        
        adminUserBox.getChildren().addAll(adminIcon, adminNameLabel);
        
        VBox adminContainer = new VBox();
        adminContainer.setPadding(new Insets(0, 15, 15, 15));
        adminContainer.getChildren().add(adminUserBox);

        // Small debug/info label showing where data is persisted (helps confirm persistence path)
        try {
            Label dataPathLabel = new Label("Data: " + PersistenceManager.getDataDirectory());
            dataPathLabel.setFont(Font.font("Segoe UI", 10));
            dataPathLabel.setTextFill(Color.web("#6B7280"));
            dataPathLabel.setWrapText(true);
            adminContainer.getChildren().add(dataPathLabel);
        } catch (Exception ignored) {}
        
        sidebar.getChildren().addAll(brandSection, navContainer, spacer, adminContainer);
        
        // Wire up navigation buttons
        dashboardBtn.setOnAction(e -> { setActiveNav(dashboardBtn); showContent(dashboardContent); });
        reportsBtn.setOnAction(e -> { setActiveNav(reportsBtn); showContent(reportsContent); });
        transactionsBtn.setOnAction(e -> { setActiveNav(transactionsBtn); refreshTransactionHistoryContent(); showContent(transactionHistoryContent); });
        remittanceBtn.setOnAction(e -> { setActiveNav(remittanceBtn); refreshRemittanceContent(); showContent(remittanceContent); });
        productsBtn.setOnAction(e -> { setActiveNav(productsBtn); showContent(productsContent); });
        inventoryBtn.setOnAction(e -> { setActiveNav(inventoryBtn); showContent(inventoryContent); });
        addOnsBtn.setOnAction(e -> { setActiveNav(addOnsBtn); refreshAddOnsContent(); showContent(addOnsContent); });
        specialReqBtn.setOnAction(e -> { setActiveNav(specialReqBtn); refreshSpecialRequestsContent(); showContent(specialRequestsContent); });
        categoriesBtn.setOnAction(e -> { setActiveNav(categoriesBtn); showContent(categoriesContent); });
        accountsBtn.setOnAction(e -> { setActiveNav(accountsBtn); showContent(accountsContent); });
        // Archived items navigation removed
        maintenanceBtn.setOnAction(e -> { setActiveNav(maintenanceBtn); showContent(maintenanceContent); updateMaintenanceUI(); });
        complaintsBtn.setOnAction(e -> { setActiveNav(complaintsBtn); refreshComplaintsContent(); showContent(complaintsContent); });
        
        return sidebar;
    }
    
    private Button createNavButton(String icon, String text, boolean active) {
        Button btn = new Button();
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(16));
        
        Label textLabel = new Label(text);
        textLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        
        content.getChildren().addAll(iconLabel, textLabel);
        btn.setGraphic(content);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 20, 12, 20));
        
        if (active) {
            btn.setStyle("-fx-background-color: #FFA500; -fx-text-fill: #FFFFFF; -fx-background-radius: 8; -fx-cursor: hand;");
            iconLabel.setTextFill(Color.web("#FFFFFF"));
            textLabel.setTextFill(Color.web("#FFFFFF"));
            textLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            activeNavButton = btn;
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #374151; -fx-cursor: hand;");
            iconLabel.setTextFill(Color.web("#374151"));
            textLabel.setTextFill(Color.web("#374151"));
        }
        
        btn.setOnMouseEntered(e -> {
            if (btn != activeNavButton) {
                btn.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #374151; -fx-background-radius: 8; -fx-cursor: hand;");
                iconLabel.setTextFill(Color.web("#374151"));
                textLabel.setTextFill(Color.web("#374151"));
            }
        });

        btn.setOnMouseExited(e -> {
            if (btn != activeNavButton) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #374151; -fx-cursor: hand;");
                iconLabel.setTextFill(Color.web("#374151"));
                textLabel.setTextFill(Color.web("#374151"));
            }
        });
        
        return btn;
    }
    
    private void setActiveNav(Button btn) {
        if (activeNavButton != null && activeNavButton != btn) {
            // Reset previous active button
            HBox oldContent = (HBox) activeNavButton.getGraphic();
            Label oldIcon = (Label) oldContent.getChildren().get(0);
            Label oldText = (Label) oldContent.getChildren().get(1);
            
            activeNavButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #374151; -fx-cursor: hand;");
            oldIcon.setTextFill(Color.web("#374151"));
            oldText.setTextFill(Color.web("#374151"));
            oldText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        }
        
        // Set new active button
        HBox content = (HBox) btn.getGraphic();
        Label icon = (Label) content.getChildren().get(0);
        Label text = (Label) content.getChildren().get(1);
        
        btn.setStyle("-fx-background-color: #FFA500; -fx-text-fill: #FFFFFF; -fx-background-radius: 8; -fx-cursor: hand;");
        icon.setTextFill(Color.web("#FFFFFF"));
        text.setTextFill(Color.web("#FFFFFF"));
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        activeNavButton = btn;
    }

    private void toggleMaintenance() {
        maintenanceMode = !maintenanceMode;
        String msg = maintenanceMode ? "Maintenance Mode enabled. The system is now in maintenance mode." : "Maintenance Mode disabled. System is operational.";
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Maintenance Mode");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
        // Update the maintenance page UI to reflect new state (if open)
        updateMaintenanceUI();
    }

    private void setMaintenance(boolean enabled) {
        maintenanceMode = enabled;
        // Persist to file so other apps can check
        TextDatabase.setMaintenanceMode(enabled);
        
        String msg = maintenanceMode ? "Maintenance Mode ENABLED.\n\nCustomerApp and CashierApp will be blocked until maintenance is disabled." : "Maintenance Mode DISABLED.\n\nSystem is now operational.";
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Maintenance Mode");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
        // Update maintenance UI (status label, etc.) after changing state
        updateMaintenanceUI();
    }
    
    // Keep UI in sync when maintenance changed programmatically
    private void updateMaintenanceUI() {
        if (maintenanceStatusLabel != null) {
            maintenanceStatusLabel.setText(maintenanceMode ? "Status: ENABLED" : "Status: OFF");
            maintenanceStatusLabel.setTextFill(Color.web(maintenanceMode ? "#DC2626" : "#16A34A"));
        }
    }

    // UI controls for maintenance page
    private Label maintenanceStatusLabel;

    private VBox createMaintenanceTab() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(24));

        Label title = new Label("Maintenance Mode");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        Label desc = new Label("Toggle maintenance mode to disable certain system features for maintenance windows.");
        desc.setFont(Font.font("Segoe UI", 12));
        desc.setWrapText(true);

        HBox controlRow = new HBox(12);
        controlRow.setAlignment(Pos.CENTER_LEFT);

        Button activateBtn = new Button("Activate");
        activateBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: #FFFFFF; -fx-background-radius: 6;");
        activateBtn.setOnAction(e -> setMaintenance(true));

        Button deactivateBtn = new Button("Deactivate");
        deactivateBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #FFFFFF; -fx-background-radius: 6;");
        deactivateBtn.setOnAction(e -> setMaintenance(false));

        maintenanceStatusLabel = new Label(maintenanceMode ? "Status: ENABLED" : "Status: OFF");
        maintenanceStatusLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        maintenanceStatusLabel.setTextFill(Color.web(maintenanceMode ? "#DC2626" : "#16A34A"));

        controlRow.getChildren().addAll(activateBtn, deactivateBtn, maintenanceStatusLabel);

        panel.getChildren().addAll(title, desc, controlRow);

        return panel;
    }

    // Removed createTabPane - now using sidebar navigation instead

    private VBox createAccountsTab() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(20));

        Label title = new Label("👥 Cashier Accounts");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        TableView<CashierRow> table = new TableView<>();
        table.setPrefHeight(400);

        TableColumn<CashierRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(100);

        TableColumn<CashierRow, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userCol.setPrefWidth(200);

        TableColumn<CashierRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        TableColumn<CashierRow, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(500);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(8);
            private final Button toggleBtn = new Button();
            private final Button viewPwdBtn = new Button("View Password");
            private final Button pwdBtn = new Button("Change Password");
            private final Button userBtn = new Button("Change Username");

            {
                toggleBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                viewPwdBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #9C27B0; -fx-text-fill: white;");
                pwdBtn.setStyle("-fx-font-size: 11px;");
                userBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #1976D2; -fx-text-fill: white;");
                box.getChildren().addAll(toggleBtn, viewPwdBtn, pwdBtn, userBtn);
                
                toggleBtn.setOnAction(e -> {
                    CashierRow row = getTableView().getItems().get(getIndex());
                    boolean newActive = !row.isActiveFlag();
                    Store.getInstance().setCashierActive(row.getId(), newActive);
                    row.setStatus(newActive ? "Active" : "Inactive");
                    row.setActiveFlag(newActive);
                    getTableView().refresh();
                });

                viewPwdBtn.setOnAction(e -> {
                    CashierRow row = getTableView().getItems().get(getIndex());
                    // Find the password from the store
                    String password = "";
                    for (CashierAccount acc : Store.getInstance().getCashiers()) {
                        if (acc.getId().equals(row.getId())) {
                            password = acc.getPassword();
                            break;
                        }
                    }
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("View Password");
                    alert.setHeaderText("Password for: " + row.getUsername());
                    alert.setContentText("Password: " + password);
                    alert.showAndWait();
                });

                pwdBtn.setOnAction(e -> {
                    CashierRow row = getTableView().getItems().get(getIndex());
                    TextInputDialog dlg = new TextInputDialog();
                    dlg.setTitle("Change Password");
                    dlg.setHeaderText("Change password for: " + row.getUsername());
                    dlg.setContentText("New password:");
                    dlg.showAndWait().ifPresent(pw -> {
                        if (pw.trim().isEmpty()) { showAlert("Invalid", "Password cannot be empty", Alert.AlertType.ERROR); return; }
                        Store.getInstance().changeCashierPassword(row.getId(), pw);
                        showAlert("Success", "Password changed.", Alert.AlertType.INFORMATION);
                    });
                });

                userBtn.setOnAction(e -> {
                    CashierRow row = getTableView().getItems().get(getIndex());
                    TextInputDialog dlg = new TextInputDialog(row.getUsername());
                    dlg.setTitle("Change Username");
                    dlg.setHeaderText("Change username for cashier ID: " + row.getId());
                    dlg.setContentText("New username:");
                    dlg.showAndWait().ifPresent(newUsername -> {
                        if (newUsername.trim().isEmpty()) { 
                            showAlert("Invalid", "Username cannot be empty", Alert.AlertType.ERROR); 
                            return; 
                        }
                        // Check if username already exists
                        for (CashierAccount acc : Store.getInstance().getCashiers()) {
                            if (acc.getUsername().equals(newUsername.trim()) && !acc.getId().equals(row.getId())) {
                                showAlert("Error", "Username already exists. Please choose a different username.", Alert.AlertType.ERROR);
                                return;
                            }
                        }
                        Store.getInstance().changeCashierUsername(row.getId(), newUsername.trim());
                        row.setUsername(newUsername.trim());
                        getTableView().refresh();
                        showAlert("Success", "Username changed to: " + newUsername.trim(), Alert.AlertType.INFORMATION);
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CashierRow row = getTableView().getItems().get(getIndex());
                    toggleBtn.setText(row.isActiveFlag() ? "Deactivate" : "Activate");
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(idCol, userCol, statusCol, actionsCol);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);
        Button addBtn = new Button("Add New Cashier");
        addBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> {
            Dialog<com.coffeeshop.model.CashierAccount> dlg = new Dialog<>();
            dlg.setTitle("Add Cashier");
            dlg.setHeaderText("Create a new cashier account");
            ButtonType create = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
            dlg.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
            TextField userField = new TextField(); userField.setPromptText("username");
            PasswordField passField = new PasswordField(); passField.setPromptText("password");
            grid.add(new Label("Username:"), 0, 0); grid.add(userField, 1, 0);
            grid.add(new Label("Password:"), 0, 1); grid.add(passField, 1, 1);
            dlg.getDialogPane().setContent(grid);
            dlg.setResultConverter(bt -> {
                if (bt == create) {
                    String u = userField.getText().trim(); String p = passField.getText();
                    if (u.isEmpty() || p.isEmpty()) return null;
                    // generate id
                    int max = 0;
                    for (com.coffeeshop.model.CashierAccount c : Store.getInstance().getCashiers()) {
                        try { String idn = c.getId().replaceAll("[^0-9]", ""); if (!idn.isEmpty()) max = Math.max(max, Integer.parseInt(idn)); } catch (Exception ignored) {}
                    }
                    String id = "C" + String.format("%03d", max + 1);
                    return new com.coffeeshop.model.CashierAccount(id, u, p, true);
                }
                return null;
            });

            dlg.showAndWait().ifPresent(acc -> {
                Store.getInstance().addCashier(acc);
                table.getItems().add(new CashierRow(acc.getId(), acc.getUsername(), acc.isActive() ? "Active" : "Inactive", acc.isActive()));
            });
        });

        controls.getChildren().addAll(addBtn);

        // load existing
        for (com.coffeeshop.model.CashierAccount c : Store.getInstance().getCashiers()) {
            table.getItems().add(new CashierRow(c.getId(), c.getUsername(), c.isActive() ? "Active" : "Inactive", c.isActive()));
        }

        panel.getChildren().addAll(title, new Separator(), table, controls);
        return panel;
    }

    // Helper row class for cashier table
    public static class CashierRow {
        private String id;
        private String username;
        private String status;
        private boolean activeFlag;

        public CashierRow(String id, String username, String status, boolean activeFlag) {
            this.id = id; this.username = username; this.status = status; this.activeFlag = activeFlag;
        }
        public String getId() { return id; }
        public String getUsername() { return username; }
        public void setUsername(String u) { username = u; }
        public String getStatus() { return status; }
        public void setStatus(String s) { status = s; }
        public boolean isActiveFlag() { return activeFlag; }
        public void setActiveFlag(boolean v) { activeFlag = v; }
    }

    private VBox createSalesTab() {
        VBox panel = new VBox(18);
        panel.setPadding(new Insets(24));
        panel.setStyle("-fx-background-color: #F0F2F0;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Sales Dashboard");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#1a1a1a"));
        Region headerSpacer = new Region(); HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        // Refresh button
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        refreshBtn.setOnMouseEntered(e -> refreshBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;"));
        refreshBtn.setOnMouseExited(e -> refreshBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;"));
        
        // Export PDF button
        Button exportPdfBtn = new Button("📊 Export Report");
        exportPdfBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        exportPdfBtn.setOnMouseEntered(e -> exportPdfBtn.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;"));
        exportPdfBtn.setOnMouseExited(e -> exportPdfBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;"));
        exportPdfBtn.setOnAction(e -> exportDashboardToPdf(panel));
        
        header.getChildren().addAll(title, headerSpacer, refreshBtn, exportPdfBtn);

        // Top metric cards
        HBox metricsRow = new HBox(14);
        metricsRow.setPadding(new Insets(6,0,6,0));

        // Create Revenue card with toggle (Today/Total)
        VBox cardRevenue = new VBox(8);
        cardRevenue.setPadding(new Insets(18));
        cardRevenue.setStyle("-fx-background-color: #E8F8F5; -fx-background-radius: 14;");
        cardRevenue.setAlignment(Pos.TOP_LEFT);
        
        // Toggle buttons for Today/Total
        HBox revenueToggle = new HBox(5);
        revenueToggle.setAlignment(Pos.CENTER_LEFT);
        
        Button todayRevenueBtn = new Button("Today");
        todayRevenueBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 12; -fx-cursor: hand;");
        
        Button totalRevenueBtn = new Button("Total");
        totalRevenueBtn.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 12; -fx-cursor: hand;");
        
        revenueToggle.getChildren().addAll(todayRevenueBtn, totalRevenueBtn);
        
        // Icon and title
        HBox revenueTitleRow = new HBox(8);
        revenueTitleRow.setAlignment(Pos.CENTER_LEFT);
        Label revenueIcon = new Label("💵");
        revenueIcon.setFont(Font.font(20));
        Label revenueTitleLabel = new Label("Today's Revenue");
        revenueTitleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        revenueTitleLabel.setTextFill(Color.web("#374151"));
        revenueTitleRow.getChildren().addAll(revenueIcon, revenueTitleLabel);
        
        // Value label
        Label revenueValueLabel = new Label("₱0.00");
        revenueValueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        revenueValueLabel.setTextFill(Color.web("#111827"));
        revenueValueLabel.setId("revenueValue");
        
        // Badge/subtitle
        Label revenueBadge = new Label("Updated live");
        revenueBadge.setFont(Font.font("Segoe UI", 11));
        revenueBadge.setTextFill(Color.web("#10B981"));
        
        cardRevenue.getChildren().addAll(revenueToggle, revenueTitleRow, revenueValueLabel, revenueBadge);
        
        // Toggle button actions
        final boolean[] showingToday = {true};
        
        todayRevenueBtn.setOnAction(e -> {
            showingToday[0] = true;
            todayRevenueBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 12; -fx-cursor: hand;");
            totalRevenueBtn.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 12; -fx-cursor: hand;");
            revenueTitleLabel.setText("Today's Revenue");
            List<Receipt> receipts = TextDatabase.loadAllReceipts();
            double todayRev = SalesAnalytics.getTotalSalesForDate(receipts, LocalDate.now());
            revenueValueLabel.setText(String.format("₱%.2f", todayRev));
            revenueBadge.setText("Today only");
        });
        
        totalRevenueBtn.setOnAction(e -> {
            showingToday[0] = false;
            totalRevenueBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 12; -fx-cursor: hand;");
            todayRevenueBtn.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #374151; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 12; -fx-cursor: hand;");
            revenueTitleLabel.setText("Total Revenue");
            List<Receipt> receipts = TextDatabase.loadAllReceipts();
            double totalRev = SalesAnalytics.getTotalSales(receipts);
            revenueValueLabel.setText(String.format("₱%.2f", totalRev));
            revenueBadge.setText("All time");
        });

        // Other metric cards
        VBox cardOnProgress = createMetricCardWithIcon("⏱", "On Progress", "10", "Orders", "#FEF6E6");
        VBox cardPerformance = createMetricCardWithIcon("✓", "Performance", "Good", "2/24", "#E8F5E9");
        VBox cardToday = createMetricCardWithIcon("📊", "Today Orders", "0", "orders", "#E8F5E9");

        HBox.setHgrow(cardRevenue, Priority.ALWAYS);
        HBox.setHgrow(cardOnProgress, Priority.ALWAYS);
        HBox.setHgrow(cardPerformance, Priority.ALWAYS);
        HBox.setHgrow(cardToday, Priority.ALWAYS);

        metricsRow.getChildren().addAll(cardRevenue, cardOnProgress, cardPerformance, cardToday);

        // Main content: left (charts) and right (score + recent transactions)
        HBox mainRow = new HBox(18);

        // Left: sales statistic (large) + items performance + recent transactions
        VBox leftCol = new VBox(12);
        leftCol.setPrefWidth(900);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        // Sales statistic card with controls
        VBox salesStatCard = new VBox(10);
        salesStatCard.setPadding(new Insets(18));
        salesStatCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10,0,0,2);");
        
        // Chart header with title and controls
        HBox chartHeader = new HBox(12);
        chartHeader.setAlignment(Pos.CENTER_LEFT);
        Label chartTitle = new Label("📊 Sales Statistic");
        chartTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        Region chartSpacer = new Region();
        HBox.setHgrow(chartSpacer, Priority.ALWAYS);
        
        // Filter buttons
        HBox filterButtons = new HBox(5);
        Button dayBtn = createFilterButton("Day", true);
        Button monthBtn = createFilterButton("Month", false);
        Button yearBtn = createFilterButton("Year", false);
        Button allBtn = createFilterButton("All", false);
        Button customBtn = createFilterButton("Custom", false);
        filterButtons.getChildren().addAll(dayBtn, monthBtn, yearBtn, allBtn, customBtn);
        
        Label maxLabel = new Label("≈ ₱120,00.00");
        maxLabel.setFont(Font.font("Segoe UI", 11));
        maxLabel.setTextFill(Color.web("#6c757d"));
        
        chartHeader.getChildren().addAll(chartTitle, chartSpacer, filterButtons, maxLabel);
        
        // Multi-line chart with legend
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> salesChart = new LineChart<>(xAxis, yAxis);
        salesChart.setLegendVisible(true);
        salesChart.setLegendSide(javafx.geometry.Side.TOP);
        salesChart.setPrefHeight(280);
        salesChart.setCreateSymbols(true);
        salesChart.setStyle("-fx-background-color: transparent;");
        xAxis.setLabel("");
        yAxis.setLabel("");

        salesStatCard.getChildren().addAll(chartHeader, salesChart);

        // Wire filter buttons to update the chart when clicked and toggle active styling
        dayBtn.setOnAction(e -> {
            salesGranularity = "Day";
            setActiveFilterButtons(dayBtn, monthBtn, yearBtn, allBtn, customBtn);
            updateSalesChart(salesChart, salesGranularity);
        });
        monthBtn.setOnAction(e -> {
            salesGranularity = "Month";
            setActiveFilterButtons(monthBtn, dayBtn, yearBtn, allBtn, customBtn);
            updateSalesChart(salesChart, salesGranularity);
        });
        yearBtn.setOnAction(e -> {
            salesGranularity = "Year";
            setActiveFilterButtons(yearBtn, dayBtn, monthBtn, allBtn, customBtn);
            updateSalesChart(salesChart, salesGranularity);
        });
        allBtn.setOnAction(e -> {
            salesGranularity = "All";
            setActiveFilterButtons(allBtn, dayBtn, monthBtn, yearBtn, customBtn);
            updateSalesChart(salesChart, salesGranularity);
        });
        customBtn.setOnAction(e -> {
            salesGranularity = "Custom";
            setActiveFilterButtons(customBtn, dayBtn, monthBtn, yearBtn, allBtn);
            updateSalesChart(salesChart, salesGranularity);
        });

        // Initial chart load
        setActiveFilterButtons(dayBtn, monthBtn, yearBtn, allBtn, customBtn);
        updateSalesChart(salesChart, salesGranularity);

        // Small hover affordance: slight lift so users notice interactivity
        salesStatCard.setOnMouseEntered(e -> {
            salesStatCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 14,0,0,3);");
            salesStatCard.setTranslateY(-4);
        });
        salesStatCard.setOnMouseExited(e -> {
            salesStatCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10,0,0,2);");
            salesStatCard.setTranslateY(0);
        });
        
        // Bottom row: Items Performance and Recent Transaction
        HBox bottomRow = new HBox(12);
        
        // Items Performance with radar-style placeholder
        VBox itemsPerfCard = new VBox(10);
        itemsPerfCard.setPadding(new Insets(18));
        itemsPerfCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10,0,0,2);");
        HBox.setHgrow(itemsPerfCard, Priority.ALWAYS);
        
        Label ipTitle = new Label("📦 Items Performance");
        ipTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        // Radar chart placeholder - for now show a canvas with simple visualization
        Canvas radarCanvas = new Canvas(400, 250);
        drawRadarChart(radarCanvas);
        
        itemsPerfCard.getChildren().addAll(ipTitle, radarCanvas);
        
        // Recent Transaction table
        VBox recentCard = new VBox(10);
        recentCard.setPadding(new Insets(18));
        recentCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10,0,0,2);");
        HBox.setHgrow(recentCard, Priority.ALWAYS);
        
        HBox recentHeader = new HBox(10);
        recentHeader.setAlignment(Pos.CENTER_LEFT);
        Label recentTitle = new Label("🔄 Recent Transaction");
        recentTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        Region recentSpacer = new Region();
        HBox.setHgrow(recentSpacer, Priority.ALWAYS);
        
        // Create table first so it can be referenced in button handlers
        final TableView<Receipt> recentTable = new TableView<>();
        
        HBox filterBtns = new HBox(8);
        Button allTxBtn = new Button("All");
        allTxBtn.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
        Button teaBtn = new Button("Tea");
        teaBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
        Button coffeeBtn = new Button("Coffee");
        coffeeBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
        Button snackBtn = new Button("Snack");
        snackBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
        
        // Filter button click handlers
        allTxBtn.setOnAction(e -> {
            allTxBtn.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            teaBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            coffeeBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            snackBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            filterTransactionTable(recentTable, null);
        });
        
        teaBtn.setOnAction(e -> {
            allTxBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            teaBtn.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            coffeeBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            snackBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            filterTransactionTable(recentTable, "Tea");
        });
        
        coffeeBtn.setOnAction(e -> {
            allTxBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            teaBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            coffeeBtn.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            snackBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            filterTransactionTable(recentTable, "Coffee");
        });
        
        snackBtn.setOnAction(e -> {
            allTxBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            teaBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            coffeeBtn.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            snackBtn.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11; -fx-cursor: hand;");
            filterTransactionTable(recentTable, "Snack");
        });
        
        filterBtns.getChildren().addAll(allTxBtn, teaBtn, coffeeBtn, snackBtn);
        
        recentHeader.getChildren().addAll(recentTitle, recentSpacer, filterBtns);
        
        // Configure table properties
        recentTable.setPrefHeight(250);
        recentTable.setStyle("-fx-background-color: transparent;");
        
        TableColumn<Receipt, String> custCol = new TableColumn<>("Customer Name");
        custCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUserName()));
        custCol.setPrefWidth(180);
        
        TableColumn<Receipt, String> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellValueFactory(data -> {
            String orderId = data.getValue().getOrderId();
            List<OrderRecord> orders = TextDatabase.loadAllOrders();
            StringBuilder items = new StringBuilder();
            for (OrderRecord order : orders) {
                if (order.getOrderId().equals(orderId)) {
                    if (items.length() > 0) items.append(", ");
                    items.append(order.getItemName());
                }
            }
            return new javafx.beans.property.SimpleStringProperty(items.length() > 0 ? items.toString() : "N/A");
        });
        itemsCol.setPrefWidth(320);
        
        TableColumn<Receipt, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.format("₱%.2f", data.getValue().getTotalAmount())));
        valueCol.setPrefWidth(100);
        
        recentTable.getColumns().addAll(custCol, itemsCol, valueCol);
        recentCard.getChildren().addAll(recentHeader, recentTable);
        
        bottomRow.getChildren().addAll(itemsPerfCard, recentCard);
        
        leftCol.getChildren().addAll(salesStatCard, bottomRow);

        // Main content area - just left column with charts
        mainRow.getChildren().addAll(leftCol);

        // Build loader to populate charts and lists
        Runnable load = () -> {
            List<Receipt> receipts = TextDatabase.loadAllReceipts();
            List<ItemRecord> items = TextDatabase.loadAllItems();

            // Populate metric cards - update the value labels
            double totalAll = SalesAnalytics.getTotalSales(receipts);
            double todayRev = SalesAnalytics.getTotalSalesForDate(receipts, LocalDate.now());
            long ordersToday = SalesAnalytics.getOrderCountForDate(receipts, LocalDate.now());
            long pendingOrders = TextDatabase.loadAllPendingOrders().stream()
                .filter(po -> !PendingOrder.STATUS_COMPLETED.equals(po.getStatus()))
                .count();
            
            // Update revenue card based on current toggle state
            if (showingToday[0]) {
                revenueValueLabel.setText(String.format("₱%.2f", todayRev));
            } else {
                revenueValueLabel.setText(String.format("₱%.2f", totalAll));
            }
            
            updateMetricCardValue(cardOnProgress, String.valueOf(pendingOrders));
            updateMetricCardValue(cardToday, String.valueOf(ordersToday));

            // Update the sales chart using actual receipts/orders aggregation
            // Use the current granularity (set when filters are clicked)
            updateSalesChart(salesChart, salesGranularity);

            // Recent transactions
            recentTable.getItems().clear();
            int max = Math.min(4, receipts.size());
            for (int i = 0; i < max; i++) recentTable.getItems().add(receipts.get(i));
        };
        
        // Connect refresh button to load function
        refreshBtn.setOnAction(e -> {
            load.run();
            // Show brief feedback
            refreshBtn.setText("✓ Refreshed");
            refreshBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
            new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                refreshBtn.setText("🔄 Refresh");
                refreshBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
            })).play();
        });

        // initial load
        load.run();

        // Periodically refresh sales data so the chart tracks live sales/receipts
        Timeline salesRefresher = new Timeline(new KeyFrame(Duration.seconds(5), ev -> {
            try {
                load.run();
            } catch (Exception ignored) {}
        }));
        salesRefresher.setCycleCount(Timeline.INDEFINITE);
        salesRefresher.play();

        panel.getChildren().addAll(header, metricsRow, mainRow);
        return panel;
    }

    private VBox createAddOnsTab() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(20));

        Label title = new Label("➕ Manage Add-ons");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(title);

        Button addNewBtn = new Button("+ Add New Add-on");
        addNewBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        addNewBtn.setOnAction(e -> showAddOnDialog(null));
        header.getChildren().add(addNewBtn);

        // Table for add-ons
        javafx.scene.control.TableView<com.coffeeshop.model.AddOn> table = new javafx.scene.control.TableView<>();
        
        javafx.scene.control.TableColumn<com.coffeeshop.model.AddOn, String> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(80);

        javafx.scene.control.TableColumn<com.coffeeshop.model.AddOn, String> nameCol = new javafx.scene.control.TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        javafx.scene.control.TableColumn<com.coffeeshop.model.AddOn, String> priceCol = new javafx.scene.control.TableColumn<>("Price");
        priceCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFormattedPrice()));
        priceCol.setPrefWidth(100);

        javafx.scene.control.TableColumn<com.coffeeshop.model.AddOn, String> categoryCol = new javafx.scene.control.TableColumn<>("Category");
        categoryCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));
        categoryCol.setPrefWidth(120);

        javafx.scene.control.TableColumn<com.coffeeshop.model.AddOn, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
        statusCol.setPrefWidth(100);

        javafx.scene.control.TableColumn<com.coffeeshop.model.AddOn, String> productsCol = new javafx.scene.control.TableColumn<>("Assigned Products");
        productsCol.setPrefWidth(150);
        productsCol.setCellFactory(col -> new javafx.scene.control.TableCell<com.coffeeshop.model.AddOn, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                com.coffeeshop.model.AddOn addOn = (com.coffeeshop.model.AddOn) getTableRow().getItem();
                java.util.List<String> ids = addOn.getApplicableProductIds();
                String category = addOn.getCategory();

                if (ids == null || ids.isEmpty()) {
                    String txt = (category == null || "All".equalsIgnoreCase(category)) ? "All in category" : category;
                    setText(txt);
                    setTooltip(null);
                } else {
                    java.util.List<String> names = new java.util.ArrayList<>();
                    for (String pid : ids) {
                        Product p = store.getProductById(pid);
                        if (p != null) names.add(p.getName());
                        else names.add(pid);
                    }
                    String shortDisplay = names.size() > 3 ? String.join(", ", names.subList(0, 3)) + "... (" + names.size() + ")" : String.join(", ", names);
                    setText(shortDisplay);
                    Tooltip tt = new Tooltip(String.join("\n", names));
                    setTooltip(tt);
                }
            }
        });

        javafx.scene.control.TableColumn<com.coffeeshop.model.AddOn, Void> actionsCol = new javafx.scene.control.TableColumn<>("Actions");
        actionsCol.setPrefWidth(200);
        actionsCol.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final Button assignBtn = new Button("Assign");
            private final Button editBtn = new Button("Edit");
            private final Button toggleBtn = new Button("Toggle");
            private final Button deleteBtn = new Button("Delete");

            {
                assignBtn.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white; -fx-font-size: 11;");
                editBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 11;");
                toggleBtn.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-font-size: 11;");
                deleteBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11;");

                assignBtn.setOnAction(e -> {
                    com.coffeeshop.model.AddOn addOn = getTableView().getItems().get(getIndex());
                    // reuse add-on dialog to assign products/categories
                    showAddOnDialog(addOn);
                });

                editBtn.setOnAction(e -> {
                    com.coffeeshop.model.AddOn addOn = getTableView().getItems().get(getIndex());
                    showAddOnDialog(addOn);
                });

                toggleBtn.setOnAction(e -> {
                    com.coffeeshop.model.AddOn addOn = getTableView().getItems().get(getIndex());
                    store.toggleAddOnActive(addOn.getId());
                    refreshAddOnsContent();
                });

                deleteBtn.setOnAction(e -> {
                    com.coffeeshop.model.AddOn addOn = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Add-on");
                    confirm.setHeaderText("Are you sure you want to delete this add-on?");
                    confirm.setContentText(addOn.getName());
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == javafx.scene.control.ButtonType.OK) {
                            store.deleteAddOn(addOn.getId());
                            refreshAddOnsContent();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, assignBtn, editBtn, toggleBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, priceCol, categoryCol, statusCol, productsCol, actionsCol);
        table.setItems(javafx.collections.FXCollections.observableArrayList(store.getAddOns()));

        VBox.setVgrow(table, Priority.ALWAYS);
        panel.getChildren().addAll(header, table);

        return panel;
    }

    private void refreshAddOnsContent() {
        addOnsContent.getChildren().clear();
        addOnsContent.getChildren().addAll(createAddOnsTab().getChildren());
    }

    private void refreshSpecialRequestsContent() {
        if (specialRequestsContent == null) return;
        specialRequestsContent.getChildren().clear();
        specialRequestsContent.getChildren().addAll(createSpecialRequestsTab().getChildren());
    }

    private VBox createSpecialRequestsTab() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(20));

        Label title = new Label("💬 Manage Special Requests");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(title);

        Button addNewBtn = new Button("+ Add New Special Request");
        addNewBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        addNewBtn.setOnAction(e -> showSpecialRequestDialog(null));
        header.getChildren().add(addNewBtn);

        javafx.scene.control.TableView<com.coffeeshop.model.SpecialRequest> table = new javafx.scene.control.TableView<>();

        javafx.scene.control.TableColumn<com.coffeeshop.model.SpecialRequest, String> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(100);

        javafx.scene.control.TableColumn<com.coffeeshop.model.SpecialRequest, String> textCol = new javafx.scene.control.TableColumn<>("Text");
        textCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getText()));
        textCol.setPrefWidth(300);

        javafx.scene.control.TableColumn<com.coffeeshop.model.SpecialRequest, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
        statusCol.setPrefWidth(100);

        javafx.scene.control.TableColumn<com.coffeeshop.model.SpecialRequest, String> assignedCol = new javafx.scene.control.TableColumn<>("Assigned To");
        assignedCol.setPrefWidth(220);
        assignedCol.setCellFactory(col -> new javafx.scene.control.TableCell<com.coffeeshop.model.SpecialRequest, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setTooltip(null);
                    setGraphic(null);
                    return;
                }
                com.coffeeshop.model.SpecialRequest req = (com.coffeeshop.model.SpecialRequest) getTableRow().getItem();
                java.util.List<String> ids = req.getApplicableProductIds();
                String category = req.getCategory();

                if (ids == null || ids.isEmpty()) {
                    String txt = (category == null || "All".equalsIgnoreCase(category)) ? "All Products" : category;
                    setText(txt);
                    setTooltip(null);
                    // add small view button to allow copyable view
                    Button viewBtn = new Button("👁");
                    viewBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    viewBtn.setOnAction(e -> showAssignedProductsDialog(req));
                    HBox box = new HBox(6, new Label(txt), viewBtn);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                } else {
                    java.util.List<String> names = new java.util.ArrayList<>();
                    for (String pid : ids) {
                        Product p = store.getProductById(pid);
                        if (p != null) names.add(p.getName());
                        else names.add(pid);
                    }
                    String shortDisplay = names.size() > 3 ? String.join(", ", names.subList(0, 3)) + "... (" + names.size() + ")" : String.join(", ", names);
                    setText(shortDisplay);
                    Tooltip tt = new Tooltip(String.join("\n", names));
                    setTooltip(tt);
                    Button viewBtn = new Button("👁");
                    viewBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    viewBtn.setOnAction(e -> showAssignedProductsDialog(req));
                    HBox box = new HBox(6, new Label(shortDisplay), viewBtn);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        javafx.scene.control.TableColumn<com.coffeeshop.model.SpecialRequest, Void> actionsCol = new javafx.scene.control.TableColumn<>("Actions");
        actionsCol.setPrefWidth(260);
        actionsCol.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final Button assignBtn = new Button("Assign");
            private final Button editBtn = new Button("Edit");
            private final Button toggleBtn = new Button("Toggle");
            private final Button deleteBtn = new Button("Delete");

            {
                assignBtn.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white; -fx-font-size: 11;");
                editBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 11;");
                toggleBtn.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-font-size: 11;");
                deleteBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11;");

                assignBtn.setOnAction(e -> {
                    com.coffeeshop.model.SpecialRequest r = getTableView().getItems().get(getIndex());
                    // reuse the edit dialog to allow assigning category/products
                    showSpecialRequestDialog(r);
                });

                editBtn.setOnAction(e -> {
                    com.coffeeshop.model.SpecialRequest r = getTableView().getItems().get(getIndex());
                    showSpecialRequestDialog(r);
                });

                toggleBtn.setOnAction(e -> {
                    com.coffeeshop.model.SpecialRequest r = getTableView().getItems().get(getIndex());
                    store.toggleSpecialRequestActive(r.getId());
                    refreshSpecialRequestsContent();
                });

                deleteBtn.setOnAction(e -> {
                    com.coffeeshop.model.SpecialRequest r = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Special Request");
                    confirm.setHeaderText("Are you sure you want to delete this special request?");
                    confirm.setContentText(r.getText());
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == javafx.scene.control.ButtonType.OK) {
                            store.deleteSpecialRequest(r.getId());
                            refreshSpecialRequestsContent();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(new HBox(6, assignBtn, editBtn, toggleBtn, deleteBtn));
            }
        });

        table.getColumns().addAll(idCol, textCol, statusCol, assignedCol, actionsCol);
        table.setItems(javafx.collections.FXCollections.observableArrayList(store.getSpecialRequests()));

        VBox.setVgrow(table, Priority.ALWAYS);
        panel.getChildren().addAll(header, table);
        return panel;
    }

    private void showSpecialRequestDialog(com.coffeeshop.model.SpecialRequest existing) {
        Dialog<com.coffeeshop.model.SpecialRequest> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Special Request" : "Edit Special Request");
        dialog.setHeaderText(existing == null ? "Create a new special request" : "Edit special request");

        javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField textField = new TextField();
        textField.setPromptText("e.g., Less Ice");
        javafx.scene.control.CheckBox activeCheck = new javafx.scene.control.CheckBox("Active");

        if (existing != null) {
            textField.setText(existing.getText());
            activeCheck.setSelected(existing.isActive());
        } else {
            activeCheck.setSelected(true);
        }

        grid.add(new Label("Text:"), 0, 0);
        grid.add(textField, 1, 0);
        grid.add(activeCheck, 1, 1);

        // Product assignment section
        Label productLabel = new Label("Assign to specific products (optional):");
        javafx.scene.control.ListView<javafx.scene.control.CheckBox> productList = new javafx.scene.control.ListView<>();
        productList.setPrefHeight(200);
        javafx.collections.ObservableList<javafx.scene.control.CheckBox> productCheckBoxes = javafx.collections.FXCollections.observableArrayList();
        for (Product p : store.getProducts()) {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(p.getName() + " (" + p.getCategory() + ")");
            cb.setUserData(p.getId());
            if (existing != null && existing.getApplicableProductIds().contains(p.getId())) cb.setSelected(true);
            productCheckBoxes.add(cb);
        }
        productList.setItems(productCheckBoxes);

        // Category selector
        javafx.scene.control.ComboBox<String> categoryCombo = new javafx.scene.control.ComboBox<>();
        try {
            java.util.List<String> cats = new java.util.ArrayList<>(store.getCategories());
            if (!cats.contains("All")) cats.add(0, "All");
            categoryCombo.getItems().addAll(cats);
        } catch (Exception ex) {
            categoryCombo.getItems().addAll("All");
        }
        if (existing != null && existing.getCategory() != null) categoryCombo.setValue(existing.getCategory());
        else categoryCombo.setValue("All");

        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(productLabel, 0, 3);
        grid.add(productList, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Ensure the edit dialog has size fields accessible to the result converter
        final TextField smallField = new TextField(); smallField.setPrefWidth(80);
        final TextField mediumField = new TextField(); mediumField.setPrefWidth(80);
        final TextField largeField = new TextField(); largeField.setPrefWidth(80);
        // Default size surcharge values
        smallField.setText("0"); mediumField.setText("20"); largeField.setText("30");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String txt = textField.getText();
                if (txt == null || txt.trim().isEmpty()) {
                    showAlert("Invalid", "Text cannot be empty.", Alert.AlertType.ERROR);
                    return null;
                }
                if (existing != null) {
                    existing.setText(txt.trim());
                    existing.setActive(activeCheck.isSelected());
                    existing.setCategory(categoryCombo.getValue());
                    java.util.List<String> sel = new java.util.ArrayList<>();
                    for (javafx.scene.control.CheckBox cb : productCheckBoxes) if (cb.isSelected()) sel.add((String) cb.getUserData());
                    existing.setApplicableProductIds(sel);
                    return existing;
                } else {
                    // generate new ID (Rxxx sequence)
                    int maxId = 0;
                    for (com.coffeeshop.model.SpecialRequest r : store.getSpecialRequests()) {
                        try {
                            String num = r.getId().replaceAll("[^0-9]", "");
                            if (!num.isEmpty()) maxId = Math.max(maxId, Integer.parseInt(num));
                        } catch (Exception ignored) {}
                    }
                    String newId = "R" + String.format("%03d", maxId + 1);
                    java.util.List<String> sel = new java.util.ArrayList<>();
                    for (javafx.scene.control.CheckBox cb : productCheckBoxes) if (cb.isSelected()) sel.add((String) cb.getUserData());
                    String cat = categoryCombo.getValue() == null ? "All" : categoryCombo.getValue();
                    return new com.coffeeshop.model.SpecialRequest(newId, txt.trim(), activeCheck.isSelected(), cat, sel);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            if (existing != null) store.updateSpecialRequest(res);
            else store.addSpecialRequest(res);
            refreshSpecialRequestsContent();
        });
    }

    private void showAddOnDialog(com.coffeeshop.model.AddOn existingAddOn) {
        Dialog<com.coffeeshop.model.AddOn> dialog = new Dialog<>();
        dialog.setTitle(existingAddOn == null ? "Add New Add-on" : "Edit Add-on");
        dialog.setHeaderText(existingAddOn == null ? "Create a new add-on" : "Edit add-on details");

        javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Add-on name");
        TextField priceField = new TextField();
        priceField.setPromptText("Price (e.g., 1.00)");
        // Restrict input to numeric values only (digits and optional decimal point)
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> priceFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^\\d*(\\.\\d*)?$$")) {
                return change;
            }
            return null;
        };
        priceField.setTextFormatter(new javafx.scene.control.TextFormatter<>(priceFilter));
        javafx.scene.control.ComboBox<String> categoryCombo = new javafx.scene.control.ComboBox<>();
        // Populate category choices from store so all categories are available
        try {
            java.util.List<String> cats = new java.util.ArrayList<>(store.getCategories());
            // Ensure 'All' option is present
            if (!cats.contains("All")) cats.add(0, "All");
            categoryCombo.getItems().addAll(cats);
        } catch (Exception ex) {
            categoryCombo.getItems().addAll("Coffee", "Milk Tea", "All");
        }
        javafx.scene.control.CheckBox activeCheck = new javafx.scene.control.CheckBox("Active");

        if (existingAddOn != null) {
            nameField.setText(existingAddOn.getName());
            priceField.setText(String.valueOf(existingAddOn.getPrice()));
            categoryCombo.setValue(existingAddOn.getCategory());
            activeCheck.setSelected(existingAddOn.isActive());
        } else {
            categoryCombo.setValue("Coffee");
            activeCheck.setSelected(true);
        }

        // Product assignment section
        Label productLabel = new Label("Assign to specific products (optional):");
        javafx.scene.control.ListView<javafx.scene.control.CheckBox> productList = new javafx.scene.control.ListView<>();
        productList.setPrefHeight(200);
        
        javafx.collections.ObservableList<javafx.scene.control.CheckBox> productCheckBoxes = javafx.collections.FXCollections.observableArrayList();
        for (Product p : store.getProducts()) {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(p.getName() + " (" + p.getCategory() + ")");
            cb.setUserData(p.getId());
            if (existingAddOn != null && existingAddOn.getApplicableProductIds().contains(p.getId())) {
                cb.setSelected(true);
            }
            productCheckBoxes.add(cb);
        }
        productList.setItems(productCheckBoxes);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Price:"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(activeCheck, 1, 3);
        grid.add(productLabel, 0, 4);
        grid.add(productList, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String name = nameField.getText().trim();
                    double price = Double.parseDouble(priceField.getText().trim());
                    String category = categoryCombo.getValue();
                    boolean active = activeCheck.isSelected();

                    java.util.List<String> selectedProducts = new java.util.ArrayList<>();
                    for (javafx.scene.control.CheckBox cb : productCheckBoxes) {
                        if (cb.isSelected()) {
                            selectedProducts.add((String) cb.getUserData());
                        }
                    }

                    if (existingAddOn != null) {
                        existingAddOn.setName(name);
                        existingAddOn.setPrice(price);
                        existingAddOn.setCategory(category);
                        existingAddOn.setActive(active);
                        existingAddOn.setApplicableProductIds(selectedProducts);
                        return existingAddOn;
                    } else {
                        // Generate new ID
                        int maxId = 0;
                        for (com.coffeeshop.model.AddOn a : store.getAddOns()) {
                            try {
                                String numPart = a.getId().replaceAll("[^0-9]", "");
                                if (!numPart.isEmpty()) maxId = Math.max(maxId, Integer.parseInt(numPart));
                            } catch (Exception ignored) {}
                        }
                        String newId = "A" + String.format("%03d", maxId + 1);
                        return new com.coffeeshop.model.AddOn(newId, name, price, category, selectedProducts, active);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter a valid price.", Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(addOn -> {
            if (existingAddOn != null) {
                store.updateAddOn(addOn);
            } else {
                store.addAddOn(addOn);
            }
            refreshAddOnsContent();
        });
    }

    // Show a dialog listing assigned products for a SpecialRequest (copyable)
    private void showAssignedProductsDialog(com.coffeeshop.model.SpecialRequest req) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Assigned Products");
        dlg.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefWidth(480);
        ta.setPrefHeight(320);

        java.util.List<String> ids = req.getApplicableProductIds();
        String category = req.getCategory();

        if (ids == null || ids.isEmpty()) {
            if (category == null || "All".equalsIgnoreCase(category)) {
                ta.setText("All Products");
            } else {
                StringBuilder sb = new StringBuilder();
                for (Product p : store.getProducts()) {
                    if (category.equals(p.getCategory())) {
                        sb.append(p.getId()).append(" - ").append(p.getName()).append("\n");
                    }
                }
                if (sb.length() == 0) ta.setText("All in category: " + category);
                else ta.setText(sb.toString());
            }
        } else {
            java.util.List<String> names = new java.util.ArrayList<>();
            for (String pid : ids) {
                Product p = store.getProductById(pid);
                if (p != null) names.add(p.getId() + " - " + p.getName());
                else names.add(pid);
            }
            ta.setText(String.join("\n", names));
        }

        dlg.getDialogPane().setContent(ta);
        dlg.setResizable(true);
        dlg.showAndWait();
    }

    // Simple complaints viewer dialog
    private void showComplaintsDialog() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Complaints");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox root = new VBox(12);
        root.setPadding(new Insets(12));
        root.setPrefSize(700, 480);

        ListView<String> list = new ListView<>();
        TextArea details = new TextArea();
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefHeight(320);

        java.util.List<com.coffeeshop.model.Complaint> allComplaints = Store.getInstance().getAllComplaints();
        if (allComplaints == null) allComplaints = new java.util.ArrayList<>();
        final java.util.List<com.coffeeshop.model.Complaint> complaints = allComplaints;
        for (com.coffeeshop.model.Complaint c : complaints) {
            list.getItems().add(String.format("%s | %s | %s", c.getId(), c.getIssueType(), c.getOrderId()));
        }

        list.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            int idx = newV == null ? -1 : newV.intValue();
            if (idx >= 0 && idx < complaints.size()) {
                com.coffeeshop.model.Complaint c = complaints.get(idx);
                StringBuilder sb = new StringBuilder();
                sb.append("Complaint ID: ").append(c.getId()).append("\n");
                sb.append("Order ID: ").append(c.getOrderId()).append("\n");
                sb.append("Customer: ").append(c.getCustomerName()).append("\n");
                sb.append("Issue: ").append(c.getIssueType()).append("\n");
                sb.append("Description:\n").append(c.getDescription()).append("\n\n");
                sb.append("Cashier: ").append(c.getCashierId()).append("\n");
                sb.append("Status: ").append(c.getStatus()).append("\n");
                sb.append("Created: ").append(c.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
                details.setText(sb.toString());
            } else {
                details.setText("");
            }
        });

        HBox content = new HBox(12, list, details);
        HBox.setHgrow(list, Priority.ALWAYS);
        HBox.setHgrow(details, Priority.ALWAYS);

        root.getChildren().addAll(new Label("Complaints"), content);
        dlg.getDialogPane().setContent(root);
        dlg.setResizable(true);
        dlg.showAndWait();
    }

    // ==================== REMITTANCE TAB ====================
    
    private void refreshRemittanceContent() {
        if (remittanceContent == null) return;
        remittanceContent.getChildren().clear();
        remittanceContent.getChildren().addAll(createRemittanceTab().getChildren());
    }
    
    private VBox createRemittanceTab() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color: #F3F4F6;");

        // Header
        VBox headerBox = new VBox(5);
        Label titleLabel = new Label("💰 Remittance Report");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#1F2937"));
        Label subtitleLabel = new Label("Cash transaction history for all cashiers");
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setTextFill(Color.web("#6B7280"));
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);
        panel.getChildren().add(headerBox);

        // Cashier filter dropdown
        HBox filterBox = new HBox(15);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setPadding(new Insets(10, 0, 10, 0));
        
        Label filterLabel = new Label("Filter by Cashier:");
        filterLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        filterLabel.setTextFill(Color.web("#374151"));
        
        ComboBox<String> cashierFilter = new ComboBox<>();
        cashierFilter.getItems().add("All Cashiers");
        
        // Load all cashiers from accounts AND from actual transaction history
        // This ensures we show all cashiers who have transactions, even if deleted from accounts
        java.util.Set<String> cashierNames = new java.util.TreeSet<>();
        
        // Add cashiers from accounts
        java.util.List<CashierAccount> accounts = com.coffeeshop.service.PersistenceManager.loadAccounts();
        for (CashierAccount acc : accounts) {
            if (acc.getUsername() != null && !acc.getUsername().trim().isEmpty()) {
                cashierNames.add(acc.getUsername());
            }
        }
        
        // Add cashiers from actual transactions (in case some are not in accounts anymore)
        java.util.List<CashTransaction> allTx = TextDatabase.loadAllCashTransactions();
        for (CashTransaction tx : allTx) {
            if (tx.getCashierId() != null && !tx.getCashierId().trim().isEmpty()) {
                cashierNames.add(tx.getCashierId());
            }
        }
        
        // Add all unique cashier names to the filter dropdown
        for (String name : cashierNames) {
            cashierFilter.getItems().add(name);
        }
        
        cashierFilter.setValue("All Cashiers");
        cashierFilter.setStyle("-fx-font-size: 14px; -fx-pref-width: 200px;");
        
        filterBox.getChildren().addAll(filterLabel, cashierFilter);
        panel.getChildren().add(filterBox);

        // Summary cards
        HBox summaryBox = new HBox(20);
        summaryBox.setPrefHeight(120);

        VBox totalSalesBox = createRemittanceSummaryCard("💵 Total Sales", "₱0.00", "#10B981");
        VBox totalRefundsBox = createRemittanceSummaryCard("🔄 Total Refunds", "₱0.00", "#EF4444");
        VBox netBox = createRemittanceSummaryCard("📈 Net Amount", "₱0.00", "#3B82F6");

        summaryBox.getChildren().addAll(totalSalesBox, totalRefundsBox, netBox);
        HBox.setHgrow(totalSalesBox, Priority.ALWAYS);
        HBox.setHgrow(totalRefundsBox, Priority.ALWAYS);
        HBox.setHgrow(netBox, Priority.ALWAYS);
        panel.getChildren().add(summaryBox);

        // Transactions table
        TableView<CashTransaction> transactionTable = new TableView<>();
        transactionTable.setPrefHeight(400);
        transactionTable.setStyle("-fx-background-color: white; -fx-background-radius: 12;");

        TableColumn<CashTransaction, String> cashierCol = new TableColumn<>("Cashier");
        cashierCol.setPrefWidth(120);
        cashierCol.setCellValueFactory(new PropertyValueFactory<>("cashierId"));

        TableColumn<CashTransaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setPrefWidth(100);
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setCellFactory(col -> new TableCell<CashTransaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (CashTransaction.TYPE_SALE.equals(item)) {
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                    } else if (CashTransaction.TYPE_REFUND.equals(item)) {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #6B7280;");
                    }
                }
            }
        });

        TableColumn<CashTransaction, String> timeCol = new TableColumn<>("Time");
        timeCol.setPrefWidth(180);
        timeCol.setCellValueFactory(cellData -> {
            CashTransaction tx = cellData.getValue();
            String formatted = tx.getTransactionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        TableColumn<CashTransaction, Double> amountCol = new TableColumn<>("Amount (₱)");
        amountCol.setPrefWidth(130);
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(col -> new TableCell<CashTransaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f", item));
                    setStyle(item >= 0 ? "-fx-text-fill: #10B981; -fx-font-weight: bold;" : "-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<CashTransaction, String> referenceCol = new TableColumn<>("Reference");
        referenceCol.setPrefWidth(150);
        referenceCol.setCellValueFactory(new PropertyValueFactory<>("reference"));

        TableColumn<CashTransaction, String> notesCol = new TableColumn<>("Notes");
        notesCol.setPrefWidth(250);
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        transactionTable.getColumns().addAll(cashierCol, typeCol, timeCol, amountCol, referenceCol, notesCol);

        // Load all transactions initially
        java.util.List<CashTransaction> allTransactions = TextDatabase.loadAllCashTransactions();
        javafx.collections.ObservableList<CashTransaction> data = javafx.collections.FXCollections.observableArrayList(allTransactions);
        transactionTable.setItems(data);

        // Update summary with all transactions
        updateRemittanceSummary(allTransactions, totalSalesBox, totalRefundsBox, netBox);

        // Filter listener
        cashierFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            java.util.List<CashTransaction> filtered;
            if ("All Cashiers".equals(newVal)) {
                filtered = TextDatabase.loadAllCashTransactions();
            } else {
                filtered = TextDatabase.loadCashTransactionsByCashier(newVal);
            }
            data.setAll(filtered);
            updateRemittanceSummary(filtered, totalSalesBox, totalRefundsBox, netBox);
        });

        VBox tableContainer = new VBox(transactionTable);
        tableContainer.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");
        tableContainer.setPadding(new Insets(15));
        VBox.setVgrow(transactionTable, Priority.ALWAYS);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        panel.getChildren().add(tableContainer);

        return panel;
    }
    
    private VBox createRemittanceSummaryCard(String label, String value, String color) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");
        card.setAlignment(Pos.CENTER_LEFT);

        Label labelLbl = new Label(label);
        labelLbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        labelLbl.setTextFill(Color.web("#6B7280"));

        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        valueLbl.setTextFill(Color.web(color));
        valueLbl.setId("remittanceSummaryValue");

        card.getChildren().addAll(labelLbl, valueLbl);
        return card;
    }
    
    private void updateRemittanceSummary(java.util.List<CashTransaction> transactions, VBox salesCard, VBox refundsCard, VBox netCard) {
        double totalSales = transactions.stream()
            .filter(tx -> CashTransaction.TYPE_SALE.equals(tx.getType()))
            .mapToDouble(CashTransaction::getAmount)
            .sum();
        double totalRefunds = transactions.stream()
            .filter(tx -> CashTransaction.TYPE_REFUND.equals(tx.getType()))
            .mapToDouble(CashTransaction::getAmount)
            .sum();
        double net = totalSales + totalRefunds;

        updateRemittanceCardValue(salesCard, String.format("₱%.2f", totalSales));
        updateRemittanceCardValue(refundsCard, String.format("₱%.2f", totalRefunds));
        updateRemittanceCardValue(netCard, String.format("₱%.2f", net));
    }
    
    private void updateRemittanceCardValue(VBox card, String newValue) {
        for (javafx.scene.Node node : card.getChildren()) {
            if (node instanceof Label) {
                Label lbl = (Label) node;
                if ("remittanceSummaryValue".equals(lbl.getId())) {
                    lbl.setText(newValue);
                }
            }
        }
    }

    // ==================== COMPLAINTS TAB ====================
    
    private void refreshComplaintsContent() {
        if (complaintsContent == null) return;
        complaintsContent.getChildren().clear();
        complaintsContent.getChildren().addAll(createComplaintsTab().getChildren());
    }
    
    private VBox createComplaintsTab() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color: #F3F4F6;");
        
        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("😕 Customer Complaints");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#111827"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> refreshComplaintsContent());
        
        header.getChildren().addAll(title, spacer, refreshBtn);
        
        // Complaints table
        TableView<com.coffeeshop.model.Complaint> complaintsTable = new TableView<>();
        complaintsTable.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        VBox.setVgrow(complaintsTable, Priority.ALWAYS);
        
        TableColumn<com.coffeeshop.model.Complaint, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(120);
        
        TableColumn<com.coffeeshop.model.Complaint, String> orderIdCol = new TableColumn<>("Order ID");
        orderIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getOrderId()));
        orderIdCol.setPrefWidth(120);
        
        TableColumn<com.coffeeshop.model.Complaint, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCustomerName()));
        customerCol.setPrefWidth(150);
        
        TableColumn<com.coffeeshop.model.Complaint, String> issueCol = new TableColumn<>("Issue Type");
        issueCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getIssueType()));
        issueCol.setPrefWidth(130);
        
        TableColumn<com.coffeeshop.model.Complaint, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        descCol.setPrefWidth(250);
        
        TableColumn<com.coffeeshop.model.Complaint, String> cashierCol = new TableColumn<>("Cashier");
        cashierCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCashierId()));
        cashierCol.setPrefWidth(100);
        
        TableColumn<com.coffeeshop.model.Complaint, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<com.coffeeshop.model.Complaint, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("RESOLVED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                    } else if ("PENDING_APPROVAL".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #8B5CF6; -fx-font-weight: bold;"); // Purple for pending approval
                    } else if ("REJECTED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;"); // Red for rejected
                    } else if ("OPEN".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;"); // Orange for open
                    } else {
                        setStyle("-fx-text-fill: #6B7280; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        TableColumn<com.coffeeshop.model.Complaint, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        ));
        dateCol.setPrefWidth(140);
        
        TableColumn<com.coffeeshop.model.Complaint, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(280);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button approveBtn = new Button("✓ Approve");
            private final Button rejectBtn = new Button("✗ Reject");
            private final Button viewBtn = new Button("👁 View");
            private final HBox buttons = new HBox(8, viewBtn, approveBtn, rejectBtn);
            
            {
                approveBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                rejectBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                viewBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
                buttons.setAlignment(Pos.CENTER);
                
                viewBtn.setOnAction(e -> {
                    com.coffeeshop.model.Complaint c = getTableView().getItems().get(getIndex());
                    showComplaintDetails(c);
                });
                
                approveBtn.setOnAction(e -> {
                    com.coffeeshop.model.Complaint c = getTableView().getItems().get(getIndex());
                    // Show confirmation dialog
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Approve Resolution");
                    confirm.setHeaderText("Approve Complaint Resolution");
                    confirm.setContentText("Are you sure you want to approve the resolution for this complaint?\n\nComplaint ID: " + c.getId() + "\nIssue: " + c.getIssueType());
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            c.setStatus("RESOLVED");
                            Store.getInstance().saveComplaint(c);
                            refreshComplaintsContent();
                        }
                    });
                });
                
                rejectBtn.setOnAction(e -> {
                    com.coffeeshop.model.Complaint c = getTableView().getItems().get(getIndex());
                    // Show rejection dialog with reason
                    TextInputDialog reasonDialog = new TextInputDialog();
                    reasonDialog.setTitle("Reject Resolution");
                    reasonDialog.setHeaderText("Reject Complaint Resolution");
                    reasonDialog.setContentText("Enter reason for rejection (optional):");
                    reasonDialog.showAndWait().ifPresent(reason -> {
                        c.setStatus("REJECTED");
                        // Append rejection reason to description if provided
                        if (reason != null && !reason.trim().isEmpty()) {
                            c.setDescription(c.getDescription() + " [REJECTED: " + reason + "]");
                        }
                        Store.getInstance().saveComplaint(c);
                        refreshComplaintsContent();
                    });
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    com.coffeeshop.model.Complaint c = getTableView().getItems().get(getIndex());
                    String status = c.getStatus();
                    // Show approve/reject only for PENDING_APPROVAL
                    boolean isPendingApproval = "PENDING_APPROVAL".equalsIgnoreCase(status);
                    boolean isResolved = "RESOLVED".equalsIgnoreCase(status);
                    boolean isRejected = "REJECTED".equalsIgnoreCase(status);
                    
                    approveBtn.setDisable(!isPendingApproval);
                    rejectBtn.setDisable(!isPendingApproval);
                    approveBtn.setVisible(isPendingApproval || isResolved);
                    rejectBtn.setVisible(isPendingApproval || isRejected);
                    
                    setGraphic(buttons);
                }
            }
        });
        
        complaintsTable.getColumns().addAll(idCol, orderIdCol, customerCol, issueCol, descCol, cashierCol, statusCol, dateCol, actionsCol);
        
        // Load complaints data
        java.util.List<com.coffeeshop.model.Complaint> allComplaints = Store.getInstance().getAllComplaints();
        if (allComplaints == null) allComplaints = new java.util.ArrayList<>();
        
        // Sort by date (newest first)
        allComplaints.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        complaintsTable.setItems(javafx.collections.FXCollections.observableArrayList(allComplaints));
        
        // Stats cards
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        
        int totalComplaints = allComplaints.size();
        long openCount = allComplaints.stream().filter(c -> "OPEN".equalsIgnoreCase(c.getStatus())).count();
        long pendingApprovalCount = allComplaints.stream().filter(c -> "PENDING_APPROVAL".equalsIgnoreCase(c.getStatus())).count();
        long resolvedCount = allComplaints.stream().filter(c -> "RESOLVED".equalsIgnoreCase(c.getStatus())).count();
        
        VBox totalCard = createComplaintStatCard("📋", "Total Complaints", String.valueOf(totalComplaints), "#3B82F6");
        VBox openCard = createComplaintStatCard("🔴", "Open", String.valueOf(openCount), "#EF4444");
        VBox pendingApprovalCard = createComplaintStatCard("⏳", "Pending Approval", String.valueOf(pendingApprovalCount), "#8B5CF6");
        VBox resolvedCard = createComplaintStatCard("✅", "Resolved", String.valueOf(resolvedCount), "#10B981");
        
        statsRow.getChildren().addAll(totalCard, openCard, pendingApprovalCard, resolvedCard);
        
        // Empty state message
        if (allComplaints.isEmpty()) {
            VBox emptyState = new VBox(15);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(60));
            
            Label emptyIcon = new Label("📭");
            emptyIcon.setFont(Font.font(60));
            
            Label emptyMsg = new Label("No complaints yet");
            emptyMsg.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 18));
            emptyMsg.setTextFill(Color.web("#6B7280"));
            
            Label emptyDesc = new Label("Complaints filed from the Returns/Exchange dialog will appear here.");
            emptyDesc.setFont(Font.font("Segoe UI", 14));
            emptyDesc.setTextFill(Color.web("#9CA3AF"));
            
            emptyState.getChildren().addAll(emptyIcon, emptyMsg, emptyDesc);
            panel.getChildren().addAll(header, statsRow, emptyState);
        } else {
            panel.getChildren().addAll(header, statsRow, complaintsTable);
        }
        
        return panel;
    }
    
    private VBox createComplaintStatCard(String icon, String label, String value, String color) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        card.setMinWidth(180);
        
        HBox iconRow = new HBox(10);
        iconRow.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(24));
        
        Label labelText = new Label(label);
        labelText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        labelText.setTextFill(Color.web("#6B7280"));
        
        iconRow.getChildren().addAll(iconLabel, labelText);
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        valueLabel.setTextFill(Color.web(color));
        
        card.getChildren().addAll(iconRow, valueLabel);
        return card;
    }
    
    private void showComplaintDetails(com.coffeeshop.model.Complaint c) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Complaint Details");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);
        
        Label titleLabel = new Label("😕 Complaint: " + c.getId());
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        
        int row = 0;
        grid.add(new Label("Order ID:"), 0, row);
        grid.add(new Label(c.getOrderId()), 1, row++);
        
        grid.add(new Label("Customer:"), 0, row);
        grid.add(new Label(c.getCustomerName()), 1, row++);
        
        grid.add(new Label("Issue Type:"), 0, row);
        Label issueLabel = new Label(c.getIssueType());
        issueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #EF4444;");
        grid.add(issueLabel, 1, row++);
        
        grid.add(new Label("Cashier:"), 0, row);
        grid.add(new Label(c.getCashierId()), 1, row++);
        
        grid.add(new Label("Status:"), 0, row);
        Label statusLabel = new Label(c.getStatus());
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + 
            ("RESOLVED".equalsIgnoreCase(c.getStatus()) ? "#10B981" : "#F59E0B") + ";");
        grid.add(statusLabel, 1, row++);
        
        grid.add(new Label("Date:"), 0, row);
        grid.add(new Label(c.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))), 1, row++);
        
        Label descTitle = new Label("Description:");
        descTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        
        TextArea descArea = new TextArea(c.getDescription());
        descArea.setEditable(false);
        descArea.setWrapText(true);
        descArea.setPrefRowCount(4);
        
        content.getChildren().addAll(titleLabel, new Separator(), grid, descTitle, descArea);
        
        dlg.getDialogPane().setContent(content);
        dlg.setResizable(true);
        dlg.showAndWait();
    }

    private VBox createCategoriesTab() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(20));

        Label title = new Label("🗂️ Manage Categories");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        HBox addRow = new HBox(8);
        TextField newCatField = new TextField();
        newCatField.setPromptText("New category name");
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            String val = newCatField.getText();
            if (val != null && !val.trim().isEmpty()) {
                store.addCategory(val.trim());
                newCatField.clear();
                refreshData();
            }
        });
        addRow.getChildren().addAll(newCatField, addBtn);

        // List view of categories — enforce canonical categories order
        categoriesListView = new javafx.scene.control.ListView<>();
        categoriesListView.setPrefHeight(200);

        java.util.List<String> defaultCats = Arrays.asList("Coffee", "Milk Tea", "Frappe", "Fruit Tea", "Pastries");
        // Ensure default categories exist in store (add if missing)
        for (String c : defaultCats) {
            try { if (!store.getCategories().contains(c)) store.addCategory(c); } catch (Exception ignored) {}
        }
        // Show ALL categories from store, not just the defaults
        categoriesListView.getItems().setAll(store.getCategories());
        // select the first category (products list will be populated after ListView is created)
        if (!categoriesListView.getItems().isEmpty()) {
            categoriesListView.getSelectionModel().selectFirst();
        }

        // Products list for selected category
        Label productsLabel = new Label("Products in Category:");
        productsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        productsInCategoryListView = new ListView<>();
        productsInCategoryListView.setPrefHeight(220);
        productsInCategoryListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        // Update product list when category selection changes
        categoriesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            populateProductsInCategory(productsInCategoryListView, newV);
        });

        // Populate initial products list for the selected category
        String initialCategory = categoriesListView.getSelectionModel().getSelectedItem();
        if (initialCategory != null) {
            populateProductsInCategory(productsInCategoryListView, initialCategory);
        }

        HBox actions = new HBox(8);
        Button renameBtn = new Button("Rename");
        Button deleteBtn = new Button("Delete");
        Button assignBtn = new Button("Assign to Category");

        // Assign selected products to a different category
        assignBtn.setOnAction(e -> {
            java.util.List<String> selected = productsInCategoryListView.getSelectionModel().getSelectedItems();
            if (selected == null || selected.isEmpty()) { showAlert("No Selection", "Select one or more products to assign.", Alert.AlertType.WARNING); return; }

            java.util.List<String> choices = new java.util.ArrayList<>(store.getCategories());
            choices.add("<Create new category...>");
            ChoiceDialog<String> dlg = new ChoiceDialog<>(choices.isEmpty() ? "" : choices.get(0), choices);
            dlg.setTitle("Assign Category");
            dlg.setHeaderText("Choose a category to assign selected products to");
            dlg.setContentText("Category:");

            java.util.Optional<String> res = dlg.showAndWait();
            res.ifPresent(targetCat -> {
                if (targetCat == null || targetCat.trim().isEmpty()) return;
                String catTrim = targetCat.trim();
                if ("<Create new category...>".equals(catTrim)) {
                    TextInputDialog newCatDlg = new TextInputDialog();
                    newCatDlg.setTitle("Create Category");
                    newCatDlg.setHeaderText("Create a new category");
                    newCatDlg.setContentText("Category name:");
                    newCatDlg.showAndWait().ifPresent(ncat -> {
                        if (ncat != null && !ncat.trim().isEmpty()) {
                            String created = ncat.trim();
                            store.addCategory(created);
                            // assign to created
                            for (String item : selected) {
                                String id = item.split(" - ")[0].trim();
                                for (Product p : store.getProducts()) {
                                    if (p.getId().equals(id)) { p.setCategory(created); break; }
                                }
                            }
                            store.saveData();
                            refreshData();
                            showAlert("Success", "Created category '" + created + "' and assigned selected products.", Alert.AlertType.INFORMATION);
                        }
                    });
                } else {
                    // ensure category exists
                    store.addCategory(catTrim);
                    // assign products
                    for (String item : selected) {
                        // item format: ID - Name
                        String id = item.split(" - ")[0].trim();
                        for (Product p : store.getProducts()) {
                            if (p.getId().equals(id)) {
                                p.setCategory(catTrim);
                                break;
                            }
                        }
                    }
                    store.saveData();
                    refreshData();
                    showAlert("Success", "Assigned selected products to '" + catTrim + "'", Alert.AlertType.INFORMATION);
                }
            });
        });

        renameBtn.setOnAction(e -> {
            String sel = categoriesListView.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("No Selection", "Select a category to rename.", Alert.AlertType.WARNING); return; }
            TextInputDialog dlg = new TextInputDialog(sel);
            dlg.setTitle("Rename Category");
            dlg.setHeaderText("Rename: " + sel);
            dlg.setContentText("New name:");
            dlg.showAndWait().ifPresent(newName -> {
                if (newName != null && !newName.trim().isEmpty()) {
                    store.renameCategory(sel, newName.trim());
                    refreshData();
                }
            });
        });

        deleteBtn.setOnAction(e -> {
            String sel = categoriesListView.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("No Selection", "Select a category to delete.", Alert.AlertType.WARNING); return; }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Category");
            confirm.setHeaderText("Delete: " + sel);
            confirm.setContentText("This will remove the category from the list. Products with this category will keep the text but the category will be removed from the master list. Continue?");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.OK) {
                    store.removeCategory(sel);
                    refreshData();
                }
            });
        });

        actions.getChildren().addAll(renameBtn, deleteBtn, assignBtn);

        panel.getChildren().addAll(title, new Separator(), addRow, categoriesListView, productsLabel, productsInCategoryListView, actions);
        return panel;
    }

    // Helper to fill product list for a given category
    private void populateProductsInCategory(javafx.scene.control.ListView<String> listView, String category) {
        listView.getItems().clear();
        if (category == null) return;
        for (Product p : store.getProducts()) {
            String cat = p.getCategory();
            if (cat == null && "Uncategorized".equals(category)) {
                listView.getItems().add(p.getId() + " - " + p.getName());
            } else if (cat != null && cat.equals(category)) {
                listView.getItems().add(p.getId() + " - " + p.getName());
            }
        }
    }

    private VBox createDashboardTab() {
        VBox panel = new VBox(25);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color: #F3F4F6;");

        // Header with title and time
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        Label title = new Label("Business Overview");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#111827"));
        titleBox.getChildren().add(title);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label timeLabel = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        timeLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        timeLabel.setTextFill(Color.web("#6B7280"));
        
        VBox dateBox = new VBox(2);
        dateBox.setAlignment(Pos.TOP_RIGHT);
        Label dateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd")));
        dateLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        dateLabel.setTextFill(Color.web("#9CA3AF"));
        dateBox.getChildren().addAll(timeLabel, dateLabel);
        
        header.getChildren().addAll(titleBox, spacer, dateBox);

        // Statistics Cards Row
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.TOP_CENTER);

        // Create labels that will be updated
        netSalesLabel = new Label();
        netSalesLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        netSalesLabel.setTextFill(Color.web("#111827"));

        pendingOrdersLabel = new Label();
        pendingOrdersLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        pendingOrdersLabel.setTextFill(Color.web("#111827"));

        completedOrdersLabel = new Label();
        completedOrdersLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        completedOrdersLabel.setTextFill(Color.web("#111827"));

        // Create modern stat cards
        VBox salesCard = createModernStatCard("💵", "NET SALES (MONTH)", netSalesLabel, "#10B981");
        VBox pendingCard = createModernStatCard("🕐", "PENDING ORDERS", pendingOrdersLabel, "#F59E0B");
        VBox completedCard = createModernStatCard("✓", "COMPLETED ORDERS", completedOrdersLabel, "#3B82F6");

        statsRow.getChildren().addAll(salesCard, pendingCard, completedCard);

        // Stock Status Section
        VBox stockSection = new VBox(15);
        stockSection.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        stockSection.setPadding(new Insets(20));
        
        Label stockTitle = new Label("📦 Inventory Stock Status");
        stockTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        stockTitle.setTextFill(Color.web("#111827"));
        
        // Create the stock status bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Stock Status");
        yAxis.setLabel("Number of Items");
        
        BarChart<String, Number> stockChart = new BarChart<>(xAxis, yAxis);
        stockChart.setTitle("Inventory Stock Levels");
        stockChart.setLegendVisible(false);
        stockChart.setPrefHeight(300);
        stockChart.setAnimated(true);
        stockChart.setCategoryGap(40);
        stockChart.setBarGap(5);
        
        // Calculate stock levels from inventory
        int outOfStockCount = 0; // 0 stock
        int criticalCount = 0;   // 1-5 stock (< 6)
        int lowCount = 0;        // 6-15 stock (< 16)
        int normalCount = 0;     // >= 16 stock
        
        for (InventoryItem item : store.getInventory().values()) {
            double qty = item.getQuantity();
            if (qty == 0) {
                outOfStockCount++;
            } else if (qty < 6) {
                criticalCount++;
            } else if (qty < 16) {
                lowCount++;
            } else {
                normalCount++;
            }
        }
        
        XYChart.Series<String, Number> stockSeries = new XYChart.Series<>();
        stockSeries.setName("Stock Status");
        
        XYChart.Data<String, Number> outOfStockData = new XYChart.Data<>("⚫ Out of Stock (0)", outOfStockCount);
        XYChart.Data<String, Number> criticalData = new XYChart.Data<>("🔴 Critical (<6)", criticalCount);
        XYChart.Data<String, Number> lowData = new XYChart.Data<>("🟡 Low (<16)", lowCount);
        XYChart.Data<String, Number> normalData = new XYChart.Data<>("🟢 Normal (≥16)", normalCount);
        
        stockSeries.getData().addAll(outOfStockData, criticalData, lowData, normalData);
        stockChart.getData().add(stockSeries);
        
        // Style the bars with colors after they're added to the scene
        stockChart.setStyle("-fx-font-family: 'Segoe UI';");
        
        // Apply colors to bars
        Platform.runLater(() -> {
            // Out of Stock - Black
            if (outOfStockData.getNode() != null) {
                outOfStockData.getNode().setStyle("-fx-bar-fill: #1F2937;");
            }
            // Critical - Red
            if (criticalData.getNode() != null) {
                criticalData.getNode().setStyle("-fx-bar-fill: #EF4444;");
            }
            // Low - Yellow/Amber
            if (lowData.getNode() != null) {
                lowData.getNode().setStyle("-fx-bar-fill: #F59E0B;");
            }
            // Normal - Green
            if (normalData.getNode() != null) {
                normalData.getNode().setStyle("-fx-bar-fill: #10B981;");
            }
        });
        
        // Stock summary cards row
        HBox stockSummaryRow = new HBox(15);
        stockSummaryRow.setAlignment(Pos.CENTER);
        
        VBox outOfStockCard = createStockAlertCard("⚫", "OUT OF STOCK", String.valueOf(outOfStockCount), "#E5E7EB", "#1F2937");
        VBox criticalCard = createStockAlertCard("🔴", "CRITICAL", String.valueOf(criticalCount), "#FEE2E2", "#EF4444");
        VBox lowCard = createStockAlertCard("🟡", "LOW STOCK", String.valueOf(lowCount), "#FEF3C7", "#F59E0B");
        VBox normalCard = createStockAlertCard("🟢", "NORMAL", String.valueOf(normalCount), "#D1FAE5", "#10B981");
        
        stockSummaryRow.getChildren().addAll(outOfStockCard, criticalCard, lowCard, normalCard);
        
        // Low stock items table (show items that need attention)
        VBox lowStockList = new VBox(10);
        lowStockList.setPadding(new Insets(15, 0, 0, 0));
        
        Label lowStockTitle = new Label("⚠️ Items Needing Attention");
        lowStockTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lowStockTitle.setTextFill(Color.web("#374151"));
        
        // Create TableView for stock alerts
        TableView<StockAlertRow> alertTable = new TableView<>();
        alertTable.setStyle("-fx-background-color: transparent; -fx-border-color: #E5E7EB; -fx-border-radius: 8;");
        alertTable.setPrefHeight(200);
        alertTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<StockAlertRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(80);
        statusCol.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<StockAlertRow, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);
        
        TableColumn<StockAlertRow, String> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(120);
        quantityCol.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<StockAlertRow, String> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<>("level"));
        levelCol.setPrefWidth(120);
        levelCol.setStyle("-fx-alignment: CENTER;");
        
        // Color code the Level column based on status
        levelCol.setCellFactory(column -> new TableCell<StockAlertRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                } else {
                    setText(item);
                    if (item.equals("OUT OF STOCK")) {
                        setStyle("-fx-alignment: CENTER; -fx-background-color: #1F2937; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else if (item.equals("CRITICAL")) {
                        setStyle("-fx-alignment: CENTER; -fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else if (item.equals("LOW")) {
                        setStyle("-fx-alignment: CENTER; -fx-background-color: #F59E0B; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });
        
        alertTable.getColumns().addAll(statusCol, nameCol, quantityCol, levelCol);
        
        // Populate initial data
        javafx.collections.ObservableList<StockAlertRow> alertData = javafx.collections.FXCollections.observableArrayList();
        for (InventoryItem item : store.getInventory().values()) {
            if (item.getQuantity() < 16) {
                String status = item.getQuantity() == 0 ? "⚫" : (item.getQuantity() < 6 ? "🔴" : "🟡");
                String levelText = item.getQuantity() == 0 ? "OUT OF STOCK" : (item.getQuantity() < 6 ? "CRITICAL" : "LOW");
                String qtyStr = String.format("%.1f %s", item.getQuantity(), item.getUnit());
                alertData.add(new StockAlertRow(status, item.getName(), qtyStr, levelText));
            }
        }
        alertTable.setItems(alertData);
        
        // Placeholder when no items need attention
        alertTable.setPlaceholder(new Label("✅ All inventory items are at normal stock levels!"));
        
        lowStockList.getChildren().addAll(lowStockTitle, alertTable);
        
        stockSection.getChildren().addAll(stockTitle, stockSummaryRow, stockChart, lowStockList);

        panel.getChildren().addAll(header, statsRow, stockSection);
        // Initial data load
        updateDashboardData();

        // Auto-refresh dashboard data and stock chart every 5 seconds
        Timeline dashboardRefresher = new Timeline(new KeyFrame(Duration.seconds(5), ev -> {
            // Update time display
            timeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
            dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd")));
            
            // Recalculate stock levels
            int newOutOfStock = 0;
            int newCritical = 0;
            int newLow = 0;
            int newNormal = 0;
            
            for (InventoryItem item : store.getInventory().values()) {
                double qty = item.getQuantity();
                if (qty == 0) {
                    newOutOfStock++;
                } else if (qty < 6) {
                    newCritical++;
                } else if (qty < 16) {
                    newLow++;
                } else {
                    newNormal++;
                }
            }
            
            // Update chart data
            outOfStockData.setYValue(newOutOfStock);
            criticalData.setYValue(newCritical);
            lowData.setYValue(newLow);
            normalData.setYValue(newNormal);
            
            // Update summary cards
            updateStockAlertCardValue(outOfStockCard, String.valueOf(newOutOfStock));
            updateStockAlertCardValue(criticalCard, String.valueOf(newCritical));
            updateStockAlertCardValue(lowCard, String.valueOf(newLow));
            updateStockAlertCardValue(normalCard, String.valueOf(newNormal));
            
            // Update alert table
            alertData.clear();
            for (InventoryItem item : store.getInventory().values()) {
                if (item.getQuantity() < 16) {
                    String status = item.getQuantity() == 0 ? "⚫" : (item.getQuantity() < 6 ? "🔴" : "🟡");
                    String levelText = item.getQuantity() == 0 ? "OUT OF STOCK" : (item.getQuantity() < 6 ? "CRITICAL" : "LOW");
                    String qtyStr = String.format("%.1f %s", item.getQuantity(), item.getUnit());
                    alertData.add(new StockAlertRow(status, item.getName(), qtyStr, levelText));
                }
            }
            
            // Update dashboard stats
            updateDashboardData();
        }));
        dashboardRefresher.setCycleCount(Timeline.INDEFINITE);
        dashboardRefresher.play();

        return panel;
    }
    
    private VBox createStockAlertCard(String icon, String label, String value, String bgColor, String textColor) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15, 25, 15, 25));
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10;");
        card.setMinWidth(120);
        
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(24));
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        valueLabel.setTextFill(Color.web(textColor));
        
        Label labelText = new Label(label);
        labelText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        labelText.setTextFill(Color.web(textColor));
        
        card.getChildren().addAll(iconLabel, valueLabel, labelText);
        return card;
    }
    
    private void updateStockAlertCardValue(VBox card, String newValue) {
        for (Node node : card.getChildren()) {
            if (node instanceof Label) {
                Label lbl = (Label) node;
                // The value label has bold font and larger size
                if (lbl.getFont().getSize() >= 28) {
                    lbl.setText(newValue);
                    break;
                }
            }
        }
    }
    
    private VBox createModernStatCard(String icon, String label, Label valueLabel, String accentColor) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        card.setPrefWidth(280);
        card.setMinHeight(140);
        HBox.setHgrow(card, Priority.ALWAYS);
        
        // Icon circle
        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(50, 50);
        iconCircle.setMaxSize(50, 50);
        iconCircle.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 25;");
        
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("Segoe UI", 24));
        iconLabel.setTextFill(Color.web("#FFFFFF"));
        iconCircle.getChildren().add(iconLabel);
        
        Label titleLabel = new Label(label);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        titleLabel.setTextFill(Color.web("#6B7280"));
        titleLabel.setWrapText(true);
        
        card.getChildren().addAll(iconCircle, titleLabel, valueLabel);
        return card;
    }

    // Helper for metric cards with icons and badges
    private VBox createMetricCardWithIcon(String icon, String title, String value, String badge, String bgColor) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
        
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(16));
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        titleLabel.setTextFill(Color.web("#6c757d"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label detailsLabel = new Label("Details");
        detailsLabel.setFont(Font.font(10));
        detailsLabel.setTextFill(Color.web("#9ca3af"));
        topRow.getChildren().addAll(iconLabel, titleLabel, spacer, detailsLabel);
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        valueLabel.setTextFill(Color.web("#111827"));
        
        Label badgeLabel = new Label(badge);
        badgeLabel.setFont(Font.font("Segoe UI", 11));
        badgeLabel.setTextFill(Color.web("#10B981"));
        
        card.getChildren().addAll(topRow, valueLabel, badgeLabel);
        return card;
    }
    
    private void updateMetricCardValue(VBox card, String newValue) {
        if (card.getChildren().size() >= 2) {
            Label valueLabel = (Label) card.getChildren().get(1);
            valueLabel.setText(newValue);
        }
    }
    
    /**
     * Export Sales Dashboard data to PDF file with improved design
     */
    private void exportDashboardToPdf(VBox dashboardPanel) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Sales Dashboard Report");
        fileChooser.setInitialFileName("SalesDashboard_" + LocalDate.now().toString() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        File file = fileChooser.showSaveDialog(dashboardPanel.getScene().getWindow());
        if (file == null) return;
        
        try {
            // Gather sales data
            List<Receipt> receipts = TextDatabase.loadAllReceipts();
            double totalRevenue = SalesAnalytics.getTotalSales(receipts);
            double todayRevenue = SalesAnalytics.getTotalSalesForDate(receipts, LocalDate.now());
            long ordersToday = SalesAnalytics.getOrderCountForDate(receipts, LocalDate.now());
            long totalOrders = receipts.size();
            
            // Calculate average order value
            double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;
            double avgOrderToday = ordersToday > 0 ? todayRevenue / ordersToday : 0;
            
            // Get recent transactions (last 15)
            List<Receipt> sortedReceipts = new java.util.ArrayList<>(receipts);
            sortedReceipts.sort((a, b) -> b.getReceiptTime().compareTo(a.getReceiptTime()));
            List<Receipt> recentReceipts = sortedReceipts.size() > 15 ? sortedReceipts.subList(0, 15) : sortedReceipts;
            
            // Get items performance data
            List<OrderRecord> orders = TextDatabase.loadAllOrders();
            Map<String, Integer> itemCounts = new LinkedHashMap<>();
            Map<String, Double> itemRevenue = new LinkedHashMap<>();
            
            // Load products to get prices
            Map<String, Double> productPrices = new java.util.HashMap<>();
            for (Product p : store.getProducts()) {
                productPrices.put(p.getName(), p.getPrice());
            }
            
            for (OrderRecord order : orders) {
                String itemName = order.getItemName();
                itemCounts.put(itemName, itemCounts.getOrDefault(itemName, 0) + 1);
                double price = productPrices.getOrDefault(itemName, 50.0); // default price if not found
                itemRevenue.put(itemName, itemRevenue.getOrDefault(itemName, 0.0) + price);
            }
            
            // Sort by count descending
            List<Map.Entry<String, Integer>> sortedItems = new java.util.ArrayList<>(itemCounts.entrySet());
            sortedItems.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            
            // Create proper PDF file
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "rw")) {
                StringBuilder pdf = new StringBuilder();
                
                // PDF Header
                pdf.append("%PDF-1.4\n");
                pdf.append("%\u00E2\u00E3\u00CF\u00D3\n");
                
                // Object 1: Catalog
                int obj1Pos = pdf.length();
                pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
                
                // Object 2: Pages
                int obj2Pos = pdf.length();
                pdf.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
                
                // Object 5: Font
                int obj5Pos = pdf.length();
                pdf.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");
                
                // Object 6: Bold Font
                int obj6Pos = pdf.length();
                pdf.append("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");
                
                // Build page content
                StringBuilder content = new StringBuilder();
                int yPos = 780;
                
                // White background
                content.append("q\n1 1 1 rg\n0 0 612 792 re\nf\nQ\n");
                
                // Dark header bar
                content.append("q\n0.1 0.1 0.18 rg\n0 720 612 72 re\nf\nQ\n");
                
                // Title
                content.append("BT\n1 1 1 rg\n/F2 22 Tf\n50 752 Td\n(BREWISE COFFEE SHOP) Tj\nET\n");
                content.append("BT\n1 0.85 0 rg\n/F1 12 Tf\n50 732 Td\n(Sales Dashboard Report) Tj\nET\n");
                String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy - hh:mm a"));
                content.append("BT\n0.8 0.8 0.8 rg\n/F1 9 Tf\n400 745 Td\n(" + dateStr + ") Tj\nET\n");
                
                yPos = 690;
                
                // Section: Sales Summary
                content.append("BT\n0.3 0.2 0.5 rg\n/F2 14 Tf\n50 " + yPos + " Td\n(SALES SUMMARY) Tj\nET\n");
                content.append("q\n0.3 0.2 0.5 RG\n1.5 w\n50 " + (yPos - 5) + " m\n200 " + (yPos - 5) + " l\nS\nQ\n");
                
                yPos -= 35;
                
                // Stats boxes
                int bx = 50, bw = 125, bh = 50, gap = 10;
                
                // Box 1: Total Revenue
                content.append("q\n0.96 0.96 0.96 rg\n" + bx + " " + (yPos - bh) + " " + bw + " " + bh + " re\nf\nQ\n");
                content.append("q\n0.16 0.65 0.27 RG\n3 w\n" + bx + " " + (yPos - bh) + " m\n" + bx + " " + yPos + " l\nS\nQ\n");
                content.append("BT\n0.1 0.1 0.15 rg\n/F2 12 Tf\n" + (bx + 8) + " " + (yPos - 22) + " Td\n(P" + String.format("%,.2f", totalRevenue) + ") Tj\nET\n");
                content.append("BT\n0.4 0.4 0.45 rg\n/F1 8 Tf\n" + (bx + 8) + " " + (yPos - 40) + " Td\n(Total Revenue) Tj\nET\n");
                
                // Box 2: Today's Revenue
                bx += bw + gap;
                content.append("q\n0.96 0.96 0.96 rg\n" + bx + " " + (yPos - bh) + " " + bw + " " + bh + " re\nf\nQ\n");
                content.append("q\n0 0.48 1 RG\n3 w\n" + bx + " " + (yPos - bh) + " m\n" + bx + " " + yPos + " l\nS\nQ\n");
                content.append("BT\n0.1 0.1 0.15 rg\n/F2 12 Tf\n" + (bx + 8) + " " + (yPos - 22) + " Td\n(P" + String.format("%,.2f", todayRevenue) + ") Tj\nET\n");
                content.append("BT\n0.4 0.4 0.45 rg\n/F1 8 Tf\n" + (bx + 8) + " " + (yPos - 40) + " Td\n(Today Revenue) Tj\nET\n");
                
                // Box 3: Total Orders
                bx += bw + gap;
                content.append("q\n0.96 0.96 0.96 rg\n" + bx + " " + (yPos - bh) + " " + bw + " " + bh + " re\nf\nQ\n");
                content.append("q\n1 0.76 0.03 RG\n3 w\n" + bx + " " + (yPos - bh) + " m\n" + bx + " " + yPos + " l\nS\nQ\n");
                content.append("BT\n0.1 0.1 0.15 rg\n/F2 12 Tf\n" + (bx + 8) + " " + (yPos - 22) + " Td\n(" + totalOrders + ") Tj\nET\n");
                content.append("BT\n0.4 0.4 0.45 rg\n/F1 8 Tf\n" + (bx + 8) + " " + (yPos - 40) + " Td\n(Total Orders) Tj\nET\n");
                
                // Box 4: Avg Order
                bx += bw + gap;
                content.append("q\n0.96 0.96 0.96 rg\n" + bx + " " + (yPos - bh) + " " + bw + " " + bh + " re\nf\nQ\n");
                content.append("q\n0.44 0.26 0.76 RG\n3 w\n" + bx + " " + (yPos - bh) + " m\n" + bx + " " + yPos + " l\nS\nQ\n");
                content.append("BT\n0.1 0.1 0.15 rg\n/F2 12 Tf\n" + (bx + 8) + " " + (yPos - 22) + " Td\n(P" + String.format("%,.2f", avgOrderValue) + ") Tj\nET\n");
                content.append("BT\n0.4 0.4 0.45 rg\n/F1 8 Tf\n" + (bx + 8) + " " + (yPos - 40) + " Td\n(Avg Order) Tj\nET\n");
                
                yPos -= 70;
                
                // Second row
                bx = 50;
                content.append("q\n0.96 0.96 0.96 rg\n" + bx + " " + (yPos - bh) + " " + bw + " " + bh + " re\nf\nQ\n");
                content.append("q\n0 0.48 1 RG\n3 w\n" + bx + " " + (yPos - bh) + " m\n" + bx + " " + yPos + " l\nS\nQ\n");
                content.append("BT\n0.1 0.1 0.15 rg\n/F2 12 Tf\n" + (bx + 8) + " " + (yPos - 22) + " Td\n(" + ordersToday + ") Tj\nET\n");
                content.append("BT\n0.4 0.4 0.45 rg\n/F1 8 Tf\n" + (bx + 8) + " " + (yPos - 40) + " Td\n(Orders Today) Tj\nET\n");
                
                bx += bw + gap;
                content.append("q\n0.96 0.96 0.96 rg\n" + bx + " " + (yPos - bh) + " " + bw + " " + bh + " re\nf\nQ\n");
                content.append("q\n0.44 0.26 0.76 RG\n3 w\n" + bx + " " + (yPos - bh) + " m\n" + bx + " " + yPos + " l\nS\nQ\n");
                content.append("BT\n0.1 0.1 0.15 rg\n/F2 12 Tf\n" + (bx + 8) + " " + (yPos - 22) + " Td\n(P" + String.format("%,.2f", avgOrderToday) + ") Tj\nET\n");
                content.append("BT\n0.4 0.4 0.45 rg\n/F1 8 Tf\n" + (bx + 8) + " " + (yPos - 40) + " Td\n(Avg Today) Tj\nET\n");
                
                yPos -= 80;
                
                // Section: Top Selling Items
                content.append("BT\n0.3 0.2 0.5 rg\n/F2 12 Tf\n50 " + yPos + " Td\n(TOP SELLING ITEMS) Tj\nET\n");
                content.append("q\n0.3 0.2 0.5 RG\n1.5 w\n50 " + (yPos - 5) + " m\n200 " + (yPos - 5) + " l\nS\nQ\n");
                
                yPos -= 25;
                
                // Table header
                content.append("q\n0.1 0.1 0.15 rg\n50 " + (yPos - 3) + " 510 18 re\nf\nQ\n");
                content.append("BT\n1 1 1 rg\n/F2 9 Tf\n55 " + yPos + " Td\n(#) Tj\n25 0 Td\n(PRODUCT NAME) Tj\n180 0 Td\n(UNITS SOLD) Tj\n90 0 Td\n(REVENUE) Tj\nET\n");
                
                yPos -= 22;
                
                int rank = 1;
                for (Map.Entry<String, Integer> entry : sortedItems) {
                    if (rank > 10 || yPos < 280) break;
                    
                    if (rank % 2 == 0) {
                        content.append("q\n0.96 0.96 0.96 rg\n50 " + (yPos - 3) + " 510 16 re\nf\nQ\n");
                    }
                    
                    double rev = itemRevenue.getOrDefault(entry.getKey(), 0.0);
                    String name = entry.getKey();
                    if (name.length() > 22) name = name.substring(0, 19) + "...";
                    
                    content.append("BT\n0.1 0.1 0.1 rg\n/F1 9 Tf\n55 " + yPos + " Td\n(" + rank + ") Tj\n");
                    content.append("/F2 9 Tf\n25 0 Td\n(" + escapePdfText(name) + ") Tj\n");
                    content.append("/F1 9 Tf\n180 0 Td\n(" + entry.getValue() + ") Tj\n");
                    content.append("0.16 0.65 0.27 rg\n90 0 Td\n(P" + String.format("%,.2f", rev) + ") Tj\nET\n");
                    
                    yPos -= 16;
                    rank++;
                }
                
                yPos -= 25;
                
                // Section: Recent Transactions
                content.append("BT\n0.3 0.2 0.5 rg\n/F2 12 Tf\n50 " + yPos + " Td\n(RECENT TRANSACTIONS) Tj\nET\n");
                content.append("q\n0.3 0.2 0.5 RG\n1.5 w\n50 " + (yPos - 5) + " m\n220 " + (yPos - 5) + " l\nS\nQ\n");
                
                yPos -= 25;
                
                // Table header
                content.append("q\n0.1 0.1 0.15 rg\n50 " + (yPos - 3) + " 510 18 re\nf\nQ\n");
                content.append("BT\n1 1 1 rg\n/F2 9 Tf\n55 " + yPos + " Td\n(CUSTOMER) Tj\n110 0 Td\n(ORDER ID) Tj\n110 0 Td\n(AMOUNT) Tj\n90 0 Td\n(DATE/TIME) Tj\nET\n");
                
                yPos -= 22;
                
                int txCount = 0;
                for (Receipt receipt : recentReceipts) {
                    if (txCount >= 8 || yPos < 60) break;
                    
                    if (txCount % 2 == 0) {
                        content.append("q\n0.96 0.96 0.96 rg\n50 " + (yPos - 3) + " 510 16 re\nf\nQ\n");
                    }
                    
                    String cName = receipt.getUserName() != null ? receipt.getUserName() : "Guest";
                    if (cName.length() > 14) cName = cName.substring(0, 11) + "...";
                    String oId = receipt.getOrderId() != null ? receipt.getOrderId() : receipt.getReceiptId();
                    if (oId.length() > 14) oId = oId.substring(0, 11) + "...";
                    String dtStr = receipt.getReceiptTime().format(DateTimeFormatter.ofPattern("MM/dd/yy hh:mm a"));
                    
                    content.append("BT\n0.1 0.1 0.1 rg\n/F2 9 Tf\n55 " + yPos + " Td\n(" + escapePdfText(cName) + ") Tj\n");
                    content.append("/F1 8 Tf\n110 0 Td\n(" + escapePdfText(oId) + ") Tj\n");
                    content.append("0.16 0.65 0.27 rg\n/F2 9 Tf\n110 0 Td\n(P" + String.format("%,.2f", receipt.getTotalAmount()) + ") Tj\n");
                    content.append("0.4 0.4 0.4 rg\n/F1 8 Tf\n90 0 Td\n(" + dtStr + ") Tj\nET\n");
                    
                    yPos -= 16;
                    txCount++;
                }
                
                // Footer
                content.append("BT\n0.5 0.5 0.5 rg\n/F1 8 Tf\n180 35 Td\n(c " + LocalDate.now().getYear() + " Brewise Coffee Shop - Generated by POS System) Tj\nET\n");
                
                String contentStr = content.toString();
                byte[] contentBytes = contentStr.getBytes("ISO-8859-1");
                
                // Object 4: Content stream
                int obj4Pos = pdf.length();
                pdf.append("4 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
                
                // Write header
                raf.write(pdf.toString().getBytes("ISO-8859-1"));
                // Write content
                raf.write(contentBytes);
                
                StringBuilder pdfEnd = new StringBuilder();
                pdfEnd.append("endstream\nendobj\n");
                
                // Object 3: Page
                int obj3Pos = (int)(pdf.length() + contentBytes.length + pdfEnd.length());
                pdfEnd.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n");
                
                // Cross-reference
                int xrefPos = (int)(pdf.length() + contentBytes.length + pdfEnd.length());
                pdfEnd.append("xref\n0 7\n");
                pdfEnd.append("0000000000 65535 f \n");
                pdfEnd.append(String.format("%010d 00000 n \n", obj1Pos));
                pdfEnd.append(String.format("%010d 00000 n \n", obj2Pos));
                pdfEnd.append(String.format("%010d 00000 n \n", obj3Pos));
                pdfEnd.append(String.format("%010d 00000 n \n", obj4Pos));
                pdfEnd.append(String.format("%010d 00000 n \n", obj5Pos));
                pdfEnd.append(String.format("%010d 00000 n \n", obj6Pos));
                pdfEnd.append("trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n" + xrefPos + "\n%%EOF\n");
                
                raf.write(pdfEnd.toString().getBytes("ISO-8859-1"));
            }
            
            // Success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText("PDF Report Generated!");
            alert.setContentText("Sales Dashboard exported to:\n" + file.getAbsolutePath());
            alert.showAndWait();
            
            // Open PDF
            try {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", file.getAbsolutePath()});
            } catch (Exception ignored) {}
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failed");
            alert.setHeaderText(null);
            alert.setContentText("Failed to export PDF: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Escape special PDF text characters
     */
    private String escapePdfText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("(", "\\(")
                   .replace(")", "\\)");
    }
    
    private Button createFilterButton(String text, boolean active) {
        Button btn = new Button(text);
        String activeStyle = "-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11;";
        String inactiveStyle = "-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11;";
        String hoverStyle = "-fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11;";

        btn.setStyle(active ? activeStyle : inactiveStyle);
        btn.setCursor(Cursor.HAND);
        btn.setFocusTraversable(true);

        // Hover effects
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(active ? activeStyle : inactiveStyle));

        return btn;
    }
    
    private void drawRadarChart(Canvas canvas) {
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        double centerX = canvas.getWidth() / 2;
        double centerY = canvas.getHeight() / 2;
        double radius = 80;
        
        // Draw radar background
        gc.setStroke(Color.web("#e5e7eb"));
        gc.setLineWidth(1);
        for (int i = 1; i <= 4; i++) {
            double r = radius * i / 4;
            gc.strokeOval(centerX - r, centerY - r, r * 2, r * 2);
        }
        
        // Draw axes
        String[] labels = {"Espresso", "Ice Coffee", "Americano", "Latte", "Mocha", "Flat White"};
        int points = labels.length;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI / 2 - (2 * Math.PI * i / points);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY - radius * Math.sin(angle);
            gc.strokeLine(centerX, centerY, x, y);
            
            // Draw labels
            double labelX = centerX + (radius + 30) * Math.cos(angle);
            double labelY = centerY - (radius + 30) * Math.sin(angle);
            gc.setFill(Color.web("#6c757d"));
            gc.setFont(Font.font(10));
            gc.fillText(labels[i], labelX - 20, labelY);
        }
        
        // Draw data polygon
        gc.setStroke(Color.web("#FFC107"));
        gc.setFill(Color.web("#FFC107", 0.2));
        gc.setLineWidth(2);
        
        double[] xPoints = new double[points];
        double[] yPoints = new double[points];
        double[] values = {0.7, 0.8, 0.6, 0.5, 0.9, 0.4};
        
        for (int i = 0; i < points; i++) {
            double angle = Math.PI / 2 - (2 * Math.PI * i / points);
            xPoints[i] = centerX + radius * values[i] * Math.cos(angle);
            yPoints[i] = centerY - radius * values[i] * Math.sin(angle);
        }
        
        gc.beginPath();
        gc.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < points; i++) {
            gc.lineTo(xPoints[i], yPoints[i]);
        }
        gc.closePath();
        gc.fill();
        gc.stroke();
    }
    
    private void drawScoreGauge(Canvas canvas, int score) {
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        double centerX = canvas.getWidth() / 2;
        double centerY = canvas.getHeight() - 30;
        double radius = 100;
        
        // Draw gauge background arcs
        gc.setLineWidth(12);
        gc.setStroke(Color.web("#e5e7eb"));
        gc.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 0, 180, javafx.scene.shape.ArcType.OPEN);
        
        // Draw score arc with gradient effect (yellow-green)
        double scoreAngle = 180 * score / 100.0;
        gc.setStroke(Color.web("#FFC107"));
        gc.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 180, -scoreAngle, javafx.scene.shape.ArcType.OPEN);
        
        // Draw tick marks
        for (int i = 0; i <= 10; i++) {
            double angle = Math.toRadians(180 - i * 18);
            double x1 = centerX + (radius - 6) * Math.cos(angle);
            double y1 = centerY - (radius - 6) * Math.sin(angle);
            double x2 = centerX + (radius + 6) * Math.cos(angle);
            double y2 = centerY - (radius + 6) * Math.sin(angle);
            
            if (i * 10 <= score) {
                gc.setStroke(Color.web("#FFC107"));
            } else {
                gc.setStroke(Color.web("#e5e7eb"));
            }
            gc.setLineWidth(3);
            gc.strokeLine(x1, y1, x2, y2);
        }
        
        // Draw score value
        gc.setFill(Color.web("#16A34A"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 56));
        String scoreText = String.valueOf(score);
        gc.fillText(scoreText, centerX - 35, centerY - 10);
    }
    
    private void filterTransactionTable(TableView<Receipt> table, String category) {
        List<Receipt> allReceipts = TextDatabase.loadAllReceipts();
        table.getItems().clear();
        
        if (category == null) {
            // Show all transactions
            int max = Math.min(4, allReceipts.size());
            for (int i = 0; i < max; i++) {
                table.getItems().add(allReceipts.get(i));
            }
        } else {
            // Filter by category based on order items
            List<OrderRecord> allOrders = TextDatabase.loadAllOrders();
            int count = 0;
            for (Receipt receipt : allReceipts) {
                if (count >= 4) break;
                String orderId = receipt.getOrderId();
                
                // Check if this order contains items from the selected category
                boolean hasCategory = false;
                for (OrderRecord order : allOrders) {
                    if (order.getOrderId().equals(orderId)) {
                        String typeOfDrink = order.getTypeOfDrink();
                        if (typeOfDrink != null && typeOfDrink.equalsIgnoreCase(category)) {
                            hasCategory = true;
                            break;
                        }
                    }
                }
                
                if (hasCategory) {
                    table.getItems().add(receipt);
                    count++;
                }
            }
        }
    }

    // Helper to toggle active styling for the filter buttons
    private void setActiveFilterButtons(Button active, Button b1, Button b2, Button b3, Button b4) {
        String activeStyle = "-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11;";
        String inactiveStyle = "-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 11;";
        active.setStyle(activeStyle);
        b1.setStyle(inactiveStyle);
        b2.setStyle(inactiveStyle);
        b3.setStyle(inactiveStyle);
        b4.setStyle(inactiveStyle);
    }

    // Update the sales chart data based on selected granularity using real order records
    private void updateSalesChart(LineChart<String, Number> chart, String granularity) {
        chart.getData().clear();

        List<OrderRecord> orders = TextDatabase.loadAllOrders();
        List<Receipt> receipts = TextDatabase.loadAllReceipts();
        LocalDate today = LocalDate.now();

        // map orderId -> list of order records (items)
        Map<String, List<OrderRecord>> ordersById = new HashMap<>();
        for (OrderRecord or : orders) {
            ordersById.computeIfAbsent(or.getOrderId(), k -> new ArrayList<>()).add(or);
        }

        List<String> labels = new ArrayList<>();
        // Map label -> total sales amount
        Map<String, Double> salesAmounts = new LinkedHashMap<>();

        if ("Day".equalsIgnoreCase(granularity)) {
            for (int h = 10; h <= 16; h++) {
                String label = String.format("%02d:00%s", (h <= 12 ? h : h - 12), (h < 12 ? "AM" : "PM"));
                labels.add(label);
                salesAmounts.put(label, 0.0);
            }

            for (Receipt r : receipts) {
                if (!r.getReceiptTime().toLocalDate().equals(today)) continue;
                int hour = r.getReceiptTime().getHour();
                if (hour < 10 || hour > 16) continue;
                String label = String.format("%02d:00%s", (hour <= 12 ? hour : hour - 12), (hour < 12 ? "AM" : "PM"));
                double prev = salesAmounts.getOrDefault(label, 0.0);
                salesAmounts.put(label, prev + r.getTotalAmount());
            }

        } else if ("Month".equalsIgnoreCase(granularity)) {
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                String label = d.getMonthValue() + "/" + d.getDayOfMonth();
                labels.add(label);
                salesAmounts.put(label, 0.0);
            }
            for (Receipt r : receipts) {
                String label = r.getReceiptTime().getMonthValue() + "/" + r.getReceiptTime().getDayOfMonth();
                // accumulate only if this label is part of the displayed labels
                if (!salesAmounts.containsKey(label)) continue;
                double prev = salesAmounts.getOrDefault(label, 0.0);
                salesAmounts.put(label, prev + r.getTotalAmount());
            }

        } else if ("Year".equalsIgnoreCase(granularity)) {
            LocalDate start = today.minusMonths(5);
            for (int i = 0; i < 6; i++) {
                LocalDate d = start.plusMonths(i);
                String label = d.getMonth().toString().substring(0,3);
                labels.add(label);
                salesAmounts.put(label, 0.0);
            }
            for (Receipt r : receipts) {
                String label = r.getReceiptTime().getMonth().toString().substring(0,3);
                if (!salesAmounts.containsKey(label)) continue;
                double prev = salesAmounts.getOrDefault(label, 0.0);
                salesAmounts.put(label, prev + r.getTotalAmount());
            }

        } else {
            // All/Custom: use years from receipts
            Set<Integer> years = receipts.stream().map(rc -> rc.getReceiptTime().getYear()).collect(Collectors.toCollection(TreeSet::new));
            List<Integer> ylist = new ArrayList<>(years);
            if (ylist.isEmpty()) {
                labels.add(String.valueOf(today.getYear()));
                salesAmounts.put(String.valueOf(today.getYear()), 0.0);
            } else {
                int startIdx = Math.max(0, ylist.size() - 4);
                for (int i = startIdx; i < ylist.size(); i++) {
                    String label = String.valueOf(ylist.get(i));
                    labels.add(label);
                    salesAmounts.put(label, 0.0);
                }
                for (Receipt r : receipts) {
                    String label = String.valueOf(r.getReceiptTime().getYear());
                    if (!salesAmounts.containsKey(label)) continue;
                    double prev = salesAmounts.getOrDefault(label, 0.0);
                    salesAmounts.put(label, prev + r.getTotalAmount());
                }
            }
        }

        // Build total sales series (revenue) using receipts
        XYChart.Series<String, Number> salesSeries = new XYChart.Series<>();
        salesSeries.setName("Total Sales");
        for (String lbl : labels) {
            double amt = salesAmounts.getOrDefault(lbl, 0.0);
            salesSeries.getData().add(new XYChart.Data<>(lbl, amt));
        }

        chart.getData().addAll(salesSeries);

        // Attach tooltips to the data nodes after layout
        Platform.runLater(() -> {
            for (XYChart.Series<String, Number> s : chart.getData()) {
                for (XYChart.Data<String, Number> d : s.getData()) {
                    Node node = d.getNode();
                    if (node != null) {
                        String seriesName = s.getName();
                        String tip;
                        if ("Total Sales".equals(seriesName)) {
                            tip = seriesName + " — " + d.getXValue() + ": ₱" + String.format("%.2f", d.getYValue().doubleValue());
                        } else {
                            tip = seriesName + " — " + d.getXValue() + ": " + d.getYValue();
                        }
                        Tooltip.install(node, new Tooltip(tip));
                        node.setCursor(Cursor.HAND);
                    }
                }
            }
        });
    }

    private String inferType(OrderRecord o) {
        String type = o.getTypeOfDrink();
        if (type != null && !type.trim().isEmpty()) return type.trim();
        String name = o.getItemName() == null ? "" : o.getItemName().toLowerCase();
        if (name.contains("tea")) return "Tea";
        if (name.contains("coffee") || name.contains("espresso") || name.contains("latte") || name.contains("americano")) return "Coffee";
        if (name.contains("snack") || name.contains("cake") || name.contains("cookie") || name.contains("sandwich")) return "Snack";
        return "Other";
    }

    private void incrementCount(String label, String type, Map<String,Integer> tea, Map<String,Integer> coffee, Map<String,Integer> snack) {
        if (type == null) return;
        switch (type.toLowerCase()) {
            case "tea":
                tea.put(label, tea.getOrDefault(label,0) + 1);
                break;
            case "coffee":
                coffee.put(label, coffee.getOrDefault(label,0) + 1);
                break;
            case "snack":
                snack.put(label, snack.getOrDefault(label,0) + 1);
                break;
            default:
                // ignore other types for chart
                break;
        }
    }
    
    private VBox createComplaintCard(String iconColor, String title, String subtitle, String actionText) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10,0,0,2);");
        
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label(iconColor);
        icon.setFont(Font.font(14));
        
        VBox textBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Segoe UI", 11));
        subtitleLabel.setTextFill(Color.web("#6c757d"));
        textBox.getChildren().addAll(titleLabel, subtitleLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button solveBtn = new Button(actionText);
        solveBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 16; -fx-font-size: 11; -fx-font-weight: bold;");
        
        Label moreIcon = new Label("⋮");
        moreIcon.setFont(Font.font(16));
        moreIcon.setTextFill(Color.web("#EF4444"));
        
        topRow.getChildren().addAll(icon, textBox, spacer, solveBtn, moreIcon);
        card.getChildren().add(topRow);
        
        return card;
    }

    private VBox createStatCard(String label, String value, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(250);
        card.setPrefHeight(120);

        Label titleLabel = new Label(label);
        titleLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        titleLabel.setTextFill(Color.web("#666"));
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.CENTER);

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        valueLabel.setTextFill(Color.web(color));

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private VBox createStatCard(String label, Label valueLabel, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(250);
        card.setPrefHeight(120);

        Label titleLabel = new Label(label);
        titleLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        titleLabel.setTextFill(Color.web("#666"));
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.CENTER);

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private void updateDashboardData() {
        // Calculate month range
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Receipt> allReceipts = TextDatabase.loadAllReceipts();
        List<PendingOrder> allPendingOrders = TextDatabase.loadAllPendingOrders();

        // Net Sales for current month
        double netSales = allReceipts.stream()
            .filter(r -> r.getReceiptTime().isAfter(monthStart) && r.getReceiptTime().isBefore(monthEnd))
            .mapToDouble(Receipt::getTotalAmount)
            .sum();

        // Count orders
        long pendingCount = allPendingOrders.stream()
            .filter(o -> "PENDING".equals(o.getStatus()))
            .count();

        long completedCount = allPendingOrders.stream()
            .filter(o -> "COMPLETED".equals(o.getStatus()))
            .count();

        // Update labels
        netSalesLabel.setText(String.format("₱%.2f", netSales));
        pendingOrdersLabel.setText(String.valueOf(pendingCount));
        completedOrdersLabel.setText(String.valueOf(completedCount));
    }

    private void refreshDashboard() {
        refreshData();
        updateDashboardData();
        showAlert("Dashboard Refreshed", "All statistics and alerts have been updated.", Alert.AlertType.INFORMATION);
    }

    private VBox createProductsTab() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label title = new Label("📦 Product Management");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        // Product table
        productTable = new TableView<>();
        productTable.setPrefHeight(400);
        productTable.setMaxHeight(400);

        TableColumn<ProductRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(100);

        TableColumn<ProductRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<ProductRow, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);

        // Stock column removed as per UI simplification

        TableColumn<ProductRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        productTable.getColumns().addAll(idCol, nameCol, priceCol, statusCol);

        // Controls
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPickOnBounds(true);

        Button editButton = new Button("✏️ Edit Product");
        stylePrimaryButton(editButton);
        editButton.setOnAction(e -> editProduct());

        Button removeButton = new Button("🗑️ Remove Product");
        styleDangerButton(removeButton);
        removeButton.setOnAction(e -> removeProduct());

        Button addButton = new Button("➕ Add New Product");
        styleSuccessButton(addButton);
        addButton.setDisable(false);
        addButton.setFocusTraversable(true);
        addButton.setMouseTransparent(false);
        addButton.setOnAction(e -> addNewProduct());
        // Defensive mouse-click handler in case an overlay prevents normal action events
        addButton.setOnMouseClicked(e -> {
            System.out.println("DEBUG: Add New Product clicked");
            addNewProduct();
        });

        Button refreshButton = new Button("🔄 Refresh");
        styleSecondaryButton(refreshButton);
        refreshButton.setDisable(false);
        refreshButton.setFocusTraversable(true);
        refreshButton.setMouseTransparent(false);
        refreshButton.setOnAction(e -> refreshData());
        refreshButton.setOnMouseClicked(e -> {
            System.out.println("DEBUG: Refresh Products clicked");
            refreshData();
        });

        controls.getChildren().addAll(editButton, removeButton, addButton, refreshButton);

        panel.getChildren().addAll(title, productTable, controls);
        return panel;
    }

    private VBox createInventoryTab() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label title = new Label("📊 Inventory Management");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        // Inventory table
        inventoryTable = new TableView<>();
        inventoryTable.setPrefHeight(400);

        TableColumn<InventoryRow, String> nameCol = new TableColumn<>("Ingredient");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);

        TableColumn<InventoryRow, Double> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(150);
        // Make quantity editable inline
        inventoryTable.setEditable(true);
        quantityCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        quantityCol.setOnEditCommit(evt -> {
            InventoryRow row = evt.getRowValue();
            double newVal = evt.getNewValue() == null ? row.getQuantity() : evt.getNewValue();
            // Update underlying store
            try {
                com.coffeeshop.service.Store.getInstance().getInventoryItem(row.getName()).setQuantity(newVal);
                com.coffeeshop.service.Store.getInstance().saveData();
            } catch (Exception ignored) {}
            row.setQuantity(newVal);
            inventoryTable.refresh();
        });

        TableColumn<InventoryRow, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(100);

        inventoryTable.getColumns().addAll(nameCol, quantityCol, unitCol);

        // Controls
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button refillIngredientButton = new Button("🔄 Refill Ingredient");
        stylePrimaryButton(refillIngredientButton);
        refillIngredientButton.setOnAction(e -> refillIngredient());

        Button deductQuantityButton = new Button("➖ Deduct Quantity");
        styleDangerButton(deductQuantityButton);
        deductQuantityButton.setOnAction(e -> deductInventoryQuantity());

        Button deleteIngredientButton = new Button("🗑️ Delete Ingredient");
        styleDangerButton(deleteIngredientButton);
        deleteIngredientButton.setOnAction(e -> deleteInventoryItem());

        Button addIngredientButton = new Button("➕ Add Ingredient");
        styleSuccessButton(addIngredientButton);
        addIngredientButton.setOnAction(e -> addIngredient());

        Button importFromProductsButton = new Button("📥 Import Ingredients from Products");
        styleSecondaryButton(importFromProductsButton);
        importFromProductsButton.setOnAction(e -> importIngredientsFromProducts());

        Button undoButton = new Button("↶ Undo");
        styleSecondaryButton(undoButton);
        undoButton.setOnAction(e -> undoLastAction());

        Button refreshButton = new Button("🔄 Refresh");
        styleSecondaryButton(refreshButton);
        refreshButton.setOnAction(e -> refreshData());

        controls.getChildren().addAll(refillIngredientButton, deductQuantityButton, deleteIngredientButton, addIngredientButton, importFromProductsButton, undoButton, refreshButton);

        panel.getChildren().addAll(title, inventoryTable, controls);
        return panel;
    }

    private VBox createArchivedTab() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label title = new Label("🗄️ Archived Inventory");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        archivedTable = new TableView<>();
        archivedTable.setPrefHeight(400);

        TableColumn<InventoryRow, String> nameCol = new TableColumn<>("Ingredient");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(300);

        TableColumn<InventoryRow, Double> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(150);

        TableColumn<InventoryRow, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(100);

        TableColumn<InventoryRow, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(300);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(8);
            private final Button restoreBtn = new Button("Restore");
            private final Button deleteBtn = new Button("Permanently Delete");
            {
                restoreBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold;");
                deleteBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold;");
                box.getChildren().addAll(restoreBtn, deleteBtn);

                restoreBtn.setOnAction(e -> {
                    InventoryRow row = getTableView().getItems().get(getIndex());
                    if (row == null) return;
                    store.restoreRemovedItem(row.getName());
                    showAlert("Restored", "Ingredient '" + row.getName() + "' restored to inventory.", Alert.AlertType.INFORMATION);
                    refreshData();
                });

                deleteBtn.setOnAction(e -> {
                    InventoryRow row = getTableView().getItems().get(getIndex());
                    if (row == null) return;
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Permanently Delete");
                    confirm.setHeaderText("Permanently delete: " + row.getName());
                    confirm.setContentText("This action cannot be undone. Continue?");
                    confirm.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.OK) {
                            store.permanentlyDeleteRemovedItem(row.getName());
                            showAlert("Deleted", "Ingredient permanently deleted.", Alert.AlertType.INFORMATION);
                            refreshData();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(box);
            }
        });

        archivedTable.getColumns().addAll(nameCol, quantityCol, unitCol, actionsCol);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button restoreSelectedBtn = new Button("Restore Selected");
        restoreSelectedBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold;");
        restoreSelectedBtn.setOnAction(e -> {
            java.util.List<InventoryRow> sel = archivedTable.getSelectionModel().getSelectedItems();
            if (sel == null || sel.isEmpty()) { showAlert("No Selection", "Select one or more archived items to restore.", Alert.AlertType.WARNING); return; }
            for (InventoryRow r : sel) store.restoreRemovedItem(r.getName());
            showAlert("Restored", "Selected items restored.", Alert.AlertType.INFORMATION);
            refreshData();
        });

        Button deleteSelectedBtn = new Button("Permanently Delete Selected");
        deleteSelectedBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteSelectedBtn.setOnAction(e -> {
            java.util.List<InventoryRow> sel = archivedTable.getSelectionModel().getSelectedItems();
            if (sel == null || sel.isEmpty()) { showAlert("No Selection", "Select one or more archived items to delete.", Alert.AlertType.WARNING); return; }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Permanently Delete");
            confirm.setHeaderText("Permanently delete selected archived items");
            confirm.setContentText("This action cannot be undone. Continue?");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.OK) {
                    for (InventoryRow r : sel) store.permanentlyDeleteRemovedItem(r.getName());
                    showAlert("Deleted", "Selected items permanently deleted.", Alert.AlertType.INFORMATION);
                    refreshData();
                }
            });
        });

        Button purgeAllBtn = new Button("Purge All Archived Items");
        purgeAllBtn.setStyle("-fx-background-color: linear-gradient(to right, #E53935, #B71C1C); -fx-text-fill: white; -fx-font-weight: bold;");
        purgeAllBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Purge All");
            confirm.setHeaderText("Purge all archived items");
            confirm.setContentText("This will permanently delete all archived inventory items. Continue?");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.OK) {
                    store.purgeAllRemovedItems();
                    showAlert("Purged", "All archived items permanently deleted.", Alert.AlertType.INFORMATION);
                    refreshData();
                }
            });
        });

        controls.getChildren().addAll(restoreSelectedBtn, deleteSelectedBtn, purgeAllBtn);

        panel.getChildren().addAll(title, new Separator(), archivedTable, controls);

        // allow multiple selection
        archivedTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        return panel;
    }

    private void refreshArchivedItems() {
        if (archivedTable == null) return;
        archivedTable.getItems().clear();
        Map<String, InventoryItem> removed = store.getRemovedInventory();
        for (InventoryItem it : removed.values()) {
            archivedTable.getItems().add(new InventoryRow(it.getName(), it.getQuantity(), it.getUnit()));
        }
    }
    
    private VBox createReportsTab() {
        // Alias to createSalesTab for the "Sales Reports" navigation item
        return createSalesTab();
    }

    private void refreshData() {
        // Refresh products table
        productTable.getItems().clear();
        for (Product p : store.getProducts()) {
            productTable.getItems().add(new ProductRow(p.getId(), p.getName(), p.getPrice(), "✓ OK"));
        }

        // Refresh inventory table
        inventoryTable.getItems().clear();
        for (InventoryItem item : store.getInventory().values()) {
            inventoryTable.getItems().add(new InventoryRow(item.getName(), item.getQuantity(), item.getUnit()));
        }

        // Refresh categories list view if present and repopulate products list
        if (categoriesListView != null) {
            // Save current selection before updating list
            String previousSelection = categoriesListView.getSelectionModel().getSelectedItem();
            categoriesListView.getItems().setAll(store.getCategories());
            // Restore selection or select first
            if (!categoriesListView.getItems().isEmpty()) {
                if (previousSelection != null && categoriesListView.getItems().contains(previousSelection)) {
                    categoriesListView.getSelectionModel().select(previousSelection);
                } else {
                    categoriesListView.getSelectionModel().selectFirst();
                }
                String sel = categoriesListView.getSelectionModel().getSelectedItem();
                if (productsInCategoryListView != null && sel != null) {
                    populateProductsInCategory(productsInCategoryListView, sel);
                }
            } else {
                if (productsInCategoryListView != null) productsInCategoryListView.getItems().clear();
            }
        }

        // Refresh archived items list if tab exists
        try { refreshArchivedItems(); } catch (Exception ignored) {}
    }

    private void removeProduct() {
        ProductRow selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a product to remove.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Product");
        confirm.setHeaderText("Remove: " + selected.getName());
        confirm.setContentText("Choose removal option:");

        ButtonType removeProductOnly = new ButtonType("Remove Product Only");
        ButtonType removeWithIngredients = new ButtonType("Remove with Ingredients (Cascade)");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        confirm.getButtonTypes().setAll(removeProductOnly, removeWithIngredients, cancel);

        confirm.showAndWait().ifPresent(response -> {
            if (response == removeProductOnly) {
                store.removeProductWithIngredients(selected.getId(), false);
                showAlert("Success", "Product removed (ingredients kept).", Alert.AlertType.INFORMATION);
                refreshData();
            } else if (response == removeWithIngredients) {
                Alert cascadeConfirm = new Alert(Alert.AlertType.WARNING);
                cascadeConfirm.setTitle("Cascade Delete Confirmation");
                cascadeConfirm.setHeaderText("This will remove both product and its ingredients!");
                cascadeConfirm.setContentText("Are you sure?");
                cascadeConfirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.OK) {
                        store.removeProductWithIngredients(selected.getId(), true);
                        showAlert("Success", "Product and ingredients removed.", Alert.AlertType.INFORMATION);
                        refreshData();
                    }
                });
            }
        });
    }

    private void addNewProduct() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Add New Product");
        dialog.setHeaderText("Enter product details:");
        dialog.setWidth(700);
        dialog.setHeight(900);

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(140);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        // Add a small Close/Back button to ensure dialog can be dismissed if it's offscreen
        Button topCloseEdit = new Button("Close");
        topCloseEdit.setOnAction(evt -> dialog.close());
        topCloseEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #333; -fx-cursor: hand;");
        grid.add(topCloseEdit, 2, 0);

        // Add a small Close/Back button to ensure dialog can be dismissed if it's offscreen
        Button topCloseBtn = new Button("Close");
        topCloseBtn.setOnAction(evt -> dialog.close());
        topCloseBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333; -fx-cursor: hand;");
        grid.add(topCloseBtn, 2, 0);

        // Auto-generate next product ID
        int maxId = 0;
        for (Product p : store.getProducts()) {
            try {
                String id = p.getId().replaceAll("[^0-9]", "");
                if (!id.isEmpty()) {
                    maxId = Math.max(maxId, Integer.parseInt(id));
                }
            } catch (Exception ignored) {}
        }
        String nextProductId = "P" + String.format("%03d", maxId + 1);
        
        // Display auto-generated ID
        Label idLabel = new Label("ID:");
        Label idValueLabel = new Label(nextProductId);
        idValueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        
        TextField nameField = new TextField();
        nameField.setPromptText("Product Name");
        TextField priceField = new TextField();
        priceField.setPromptText("5.00");
        // Restrict input to numeric values only (digits and optional decimal point)
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> newPriceFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^\\d*(\\.\\d*)?$$")) {
                return change;
            }
            return null;
        };
        priceField.setTextFormatter(new javafx.scene.control.TextFormatter<>(newPriceFilter));

        grid.add(idLabel, 0, 0);
        grid.add(idValueLabel, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);
        
        // Description field
        Label descLabel = new Label("Description:");
        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("Enter product description (shown to customers)");
        descriptionField.setPrefRowCount(2);
        descriptionField.setWrapText(true);
        descriptionField.setPrefHeight(60);
        grid.add(descLabel, 0, 3);
        grid.add(descriptionField, 1, 3);

        // Available sizes rows for new product (each row: checkbox + surcharge field)
        Label sizesAvailLabel = new Label("Available Sizes:");
        sizesAvailLabel.setStyle("-fx-font-weight: bold;");

        CheckBox smallAdd = new CheckBox("Small");
        TextField smallPriceAdd = new TextField("0.0");
        smallPriceAdd.setPrefWidth(100);
        CheckBox mediumAdd = new CheckBox("Medium");
        TextField mediumPriceAdd = new TextField("20.0");
        mediumPriceAdd.setPrefWidth(100);
        CheckBox largeAdd = new CheckBox("Large");
        TextField largePriceAdd = new TextField("30.0");
        largePriceAdd.setPrefWidth(100);

        // defaults
        smallAdd.setSelected(false);
        mediumAdd.setSelected(true);
        largeAdd.setSelected(true);

        // numeric filter for price fields
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> priceFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^\\d*(\\.\\d*)?$") ) return change;
            return null;
        };
        smallPriceAdd.setTextFormatter(new javafx.scene.control.TextFormatter<>(priceFilter));
        mediumPriceAdd.setTextFormatter(new javafx.scene.control.TextFormatter<>(priceFilter));
        largePriceAdd.setTextFormatter(new javafx.scene.control.TextFormatter<>(priceFilter));

        VBox sizesBoxAdd = new VBox(8);
        HBox rowSmallAdd = new HBox(12, smallAdd, new Label("Surcharge:"), smallPriceAdd);
        HBox rowMediumAdd = new HBox(12, mediumAdd, new Label("Surcharge:"), mediumPriceAdd);
        HBox rowLargeAdd = new HBox(12, largeAdd, new Label("Surcharge:"), largePriceAdd);
        rowSmallAdd.setAlignment(Pos.CENTER_LEFT);
        rowMediumAdd.setAlignment(Pos.CENTER_LEFT);
        rowLargeAdd.setAlignment(Pos.CENTER_LEFT);
        sizesBoxAdd.getChildren().addAll(rowSmallAdd, rowMediumAdd, rowLargeAdd);
        grid.add(sizesAvailLabel, 0, 4);
        grid.add(sizesBoxAdd, 1, 4);

        // Milk Options Section (for products like Milk Tea, Coffee, etc.)
        Label milkOptionsLabel = new Label("🥛 Milk Options:");
        milkOptionsLabel.setStyle("-fx-font-weight: bold;");
        
        CheckBox enableMilkOptionsAdd = new CheckBox("Enable Milk Options");
        enableMilkOptionsAdd.setSelected(false);
        
        CheckBox oatMilkAdd = new CheckBox("Oat Milk");
        TextField oatMilkPriceAdd = new TextField("25.0");
        oatMilkPriceAdd.setPrefWidth(80);
        oatMilkPriceAdd.setDisable(true);
        
        CheckBox almondMilkAdd = new CheckBox("Almond Milk");
        TextField almondMilkPriceAdd = new TextField("25.0");
        almondMilkPriceAdd.setPrefWidth(80);
        almondMilkPriceAdd.setDisable(true);
        
        CheckBox soyMilkAdd = new CheckBox("Soy Milk");
        TextField soyMilkPriceAdd = new TextField("25.0");
        soyMilkPriceAdd.setPrefWidth(80);
        soyMilkPriceAdd.setDisable(true);
        
        // Numeric filter for milk prices
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> milkPriceFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^\\d*(\\.\\d*)?$")) return change;
            return null;
        };
        oatMilkPriceAdd.setTextFormatter(new javafx.scene.control.TextFormatter<>(milkPriceFilter));
        almondMilkPriceAdd.setTextFormatter(new javafx.scene.control.TextFormatter<>(milkPriceFilter));
        soyMilkPriceAdd.setTextFormatter(new javafx.scene.control.TextFormatter<>(milkPriceFilter));
        
        // Enable/disable milk option rows based on main toggle
        enableMilkOptionsAdd.selectedProperty().addListener((obs, oldV, newV) -> {
            oatMilkAdd.setDisable(!newV);
            almondMilkAdd.setDisable(!newV);
            soyMilkAdd.setDisable(!newV);
            if (!newV) {
                oatMilkAdd.setSelected(false);
                almondMilkAdd.setSelected(false);
                soyMilkAdd.setSelected(false);
            }
        });
        
        // Enable price field when milk type is selected
        oatMilkAdd.selectedProperty().addListener((obs, oldV, newV) -> oatMilkPriceAdd.setDisable(!newV));
        almondMilkAdd.selectedProperty().addListener((obs, oldV, newV) -> almondMilkPriceAdd.setDisable(!newV));
        soyMilkAdd.selectedProperty().addListener((obs, oldV, newV) -> soyMilkPriceAdd.setDisable(!newV));
        
        VBox milkOptionsBoxAdd = new VBox(8);
        milkOptionsBoxAdd.getChildren().add(enableMilkOptionsAdd);
        HBox rowOatAdd = new HBox(12, oatMilkAdd, new Label("Price (+₱):"), oatMilkPriceAdd);
        HBox rowAlmondAdd = new HBox(12, almondMilkAdd, new Label("Price (+₱):"), almondMilkPriceAdd);
        HBox rowSoyAdd = new HBox(12, soyMilkAdd, new Label("Price (+₱):"), soyMilkPriceAdd);
        rowOatAdd.setAlignment(Pos.CENTER_LEFT);
        rowAlmondAdd.setAlignment(Pos.CENTER_LEFT);
        rowSoyAdd.setAlignment(Pos.CENTER_LEFT);
        milkOptionsBoxAdd.getChildren().addAll(rowOatAdd, rowAlmondAdd, rowSoyAdd);
        
        // Initially disable milk type checkboxes
        oatMilkAdd.setDisable(true);
        almondMilkAdd.setDisable(true);
        soyMilkAdd.setDisable(true);

        // Category selector
        ComboBox<String> categoryBox = new ComboBox<>();
        try {
            categoryBox.getItems().addAll(store.getCategories());
            if (!categoryBox.getItems().isEmpty()) categoryBox.setValue(categoryBox.getItems().get(0));
            else categoryBox.setValue("Coffee");
        } catch (Exception ignored) {
            // fallback to canonical default categories (local list)
            java.util.List<String> fallbackCats = java.util.Arrays.asList("Coffee", "Milk Tea", "Frappe", "Fruit Tea", "Pastries");
            categoryBox.getItems().addAll(fallbackCats);
            categoryBox.setValue(fallbackCats.get(0));
        }
        categoryBox.setEditable(true);
        categoryBox.setPromptText("Select or type a category");
        grid.add(new Label("Category:"), 0, 5);
        grid.add(categoryBox, 1, 5);
        
        // Add milk options section to grid
        grid.add(milkOptionsLabel, 0, 6);
        grid.add(milkOptionsBoxAdd, 1, 6);

        // Image preview and upload
        javafx.scene.image.ImageView imagePreview = new javafx.scene.image.ImageView();
        imagePreview.setFitWidth(150);
        imagePreview.setFitHeight(150);
        imagePreview.setPreserveRatio(true);
        imagePreview.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 5;");

        Label imagePathLabel = new Label("No image selected");
        imagePathLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

        File[] selectedImageFile = new File[1];

        Button uploadImageBtn = new Button("📷 Choose Image");
        uploadImageBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8 15; -fx-font-weight: bold;");
        uploadImageBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Product Image");
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File file = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null && file.exists()) {
                selectedImageFile[0] = file;
                try {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(file.toURI().toString(), 150, 150, true, true);
                    imagePreview.setImage(img);
                    imagePathLabel.setText(file.getName());
                } catch (Exception ex) {
                    showAlert("Error", "Failed to load image: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });

        VBox imageSection = new VBox(10);
        imageSection.setPadding(new Insets(15));
        imageSection.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");
        imageSection.getChildren().addAll(
            new Label("📸 Product Image:"),
            imagePreview,
            uploadImageBtn,
            imagePathLabel
        );

        grid.add(imageSection, 0, 7, 2, 1);

        // Ingredients section (limited by selected category)
        Label ingredientLabel = new Label("🧪 Select Ingredients:");
        ingredientLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        grid.add(ingredientLabel, 0, 8, 2, 1);

        // Create ingredient selector UI (will be rebuilt when category changes)
        VBox ingredientSection = new VBox(10);
        ingredientSection.setPadding(new Insets(10));
        ingredientSection.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #fafafa;");

        java.util.Map<String, Double> selectedIngredients = new java.util.HashMap<>();

        // Use a plain VBox for the ingredient list so it expands to fit the dialog. The outer dialog ScrollPane will handle overflow.
        VBox ingredientList = new VBox(8);
        ingredientList.setFillWidth(true);
        VBox.setVgrow(ingredientList, Priority.ALWAYS);

        final java.util.List<String> baseIngredients = new java.util.ArrayList<>(store.getInventory().keySet());
        // If inventory is empty (fresh install), provide sensible defaults so admins can edit recipes
        if (baseIngredients.isEmpty()) {
            baseIngredients.addAll(java.util.Arrays.asList(
                "Coffee Beans", "Milk", "Water", "Matcha Powder", "Chocolate Syrup",
                "Vanilla Syrup", "Caramel Syrup", "Ice", "Mango Puree", "Strawberry Puree",
                "Tea Leaves", "Flour", "Butter", "Sugar", "Egg"
            ));
        }
        // No existing recipe to merge for new product; proceed with baseIngredients
        // If inventory is empty (fresh install), provide sensible defaults so admins can compose recipes
        if (baseIngredients.isEmpty()) {
            baseIngredients.addAll(java.util.Arrays.asList(
                "Coffee Beans", "Milk", "Water", "Matcha Powder", "Chocolate Syrup",
                "Vanilla Syrup", "Caramel Syrup", "Ice", "Mango Puree", "Strawberry Puree",
                "Tea Leaves", "Croissant Dough", "Muffin Mix", "Blueberries"
            ));
        }
        Runnable rebuild = () -> {
            ingredientList.getChildren().clear();
            selectedIngredients.clear();
            java.util.List<String> choices = filterIngredientsByCategory(baseIngredients, categoryBox.getValue());
            // If filtering yields nothing (unexpected), fall back to full list and show a hint
            if (choices == null || choices.isEmpty()) {
                choices = new java.util.ArrayList<>(baseIngredients);
                Label hint = new Label("No inventory items matched this category — showing all ingredients.");
                hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
                ingredientList.getChildren().add(hint);
            }
            for (String ingredientName : choices) {
                HBox ingredientRow = new HBox(10);
                ingredientRow.setAlignment(Pos.CENTER_LEFT);

                CheckBox checkBox = new CheckBox(ingredientName);
                checkBox.setStyle("-fx-font-size: 11;");

                TextField quantityField = new TextField();
                quantityField.setPromptText("Qty");
                quantityField.setPrefWidth(80);
                quantityField.setDisable(true);
                // Restrict to numeric input
                java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> qtyFilter = change -> {
                    String newText = change.getControlNewText();
                    if (newText.matches("^\\d*(\\.\\d*)?$")) return change;
                    return null;
                };
                quantityField.setTextFormatter(new javafx.scene.control.TextFormatter<>(qtyFilter));

                checkBox.setOnAction(e -> {
                    if (checkBox.isSelected()) {
                        quantityField.setDisable(false);
                    } else {
                        quantityField.setDisable(true);
                        selectedIngredients.remove(ingredientName);
                    }
                });

                quantityField.setOnKeyReleased(e -> {
                    try {
                        if (!quantityField.getText().isEmpty()) {
                            double qty = Double.parseDouble(quantityField.getText());
                            selectedIngredients.put(ingredientName, qty);
                        }
                    } catch (NumberFormatException ignored) {}
                });

                ingredientRow.getChildren().addAll(checkBox, quantityField);
                ingredientList.getChildren().add(ingredientRow);
            }
        };

        // register an inventory listener so ingredient choices update if inventory changes while dialog is open
        final Runnable invListenerAdd = () -> javafx.application.Platform.runLater(rebuild);
        try { store.addInventoryChangeListener(invListenerAdd); } catch (Exception ignored) {}
        // initial build and rebuild on category change
        rebuild.run();
        categoryBox.valueProperty().addListener((obs, o, n) -> rebuild.run());

        ingredientList.setMaxWidth(Double.MAX_VALUE);
        ingredientSection.getChildren().add(ingredientList);
        grid.add(ingredientSection, 0, 9, 2, 1);

        // Wrap the grid in a ScrollPane so the dialog content can scroll and the button bar remains visible
        ScrollPane addScroll = new ScrollPane(grid);
        addScroll.setFitToWidth(true);
        addScroll.setFitToHeight(true);
        addScroll.setPrefViewportHeight(520);
        addScroll.setPrefViewportWidth(720);
        dialog.getDialogPane().setContent(addScroll);
        // attach fallback stylesheet to the dialog pane
        java.net.URL addCss = getClass().getResource("/styles/atlantafx-fallback.css");
        if (addCss != null) dialog.getDialogPane().getStylesheets().add(addCss.toExternalForm());
        dialog.getDialogPane().setPrefHeight(700);
        dialog.getDialogPane().setPrefWidth(760);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String id = nextProductId;  // Use auto-generated ID
                    String name = nameField.getText();
                    double price = Double.parseDouble(priceField.getText());
                    String category = categoryBox.getValue();

                    if (name.trim().isEmpty()) {
                        throw new IllegalArgumentException("Name cannot be empty");
                    }

                    // Validate selected ingredient quantities: ensure any checked ingredient has a positive numeric qty
                    java.util.Map<String, Double> cleaned = new java.util.HashMap<>();
                    for (javafx.scene.Node node : ingredientList.getChildren()) {
                        if (!(node instanceof HBox)) continue;
                        HBox row = (HBox) node;
                        if (row.getChildren().size() < 2) continue;
                        javafx.scene.Node n0 = row.getChildren().get(0);
                        javafx.scene.Node n1 = row.getChildren().get(1);
                        if (!(n0 instanceof CheckBox) || !(n1 instanceof TextField)) continue;
                        CheckBox cb = (CheckBox) n0;
                        TextField tf = (TextField) n1;
                        if (cb.isSelected()) {
                            String txt = tf.getText();
                            if (txt == null || txt.trim().isEmpty()) {
                                showAlert("Invalid Ingredient Quantity", "Ingredient '" + cb.getText() + "' requires a quantity.", Alert.AlertType.ERROR);
                                return null;
                            }
                            try {
                                double v = Double.parseDouble(txt.trim());
                                if (v <= 0) {
                                    showAlert("Invalid Ingredient Quantity", "Ingredient '" + cb.getText() + "' must have a positive quantity.", Alert.AlertType.ERROR);
                                    return null;
                                }
                                cleaned.put(cb.getText(), v);
                            } catch (NumberFormatException nfe) {
                                showAlert("Invalid Ingredient Quantity", "Ingredient '" + cb.getText() + "' has invalid number format.", Alert.AlertType.ERROR);
                                return null;
                            }
                        }
                    }

                    Product newProd = new Product(id, name, price, cleaned, category);
                    try {
                        // Save description
                        newProd.setDescription(descriptionField.getText());
                        
                        newProd.setHasSizes((smallAdd.isSelected() || mediumAdd.isSelected() || largeAdd.isSelected()));
                        newProd.setHasSmall(smallAdd.isSelected());
                        newProd.setHasMedium(mediumAdd.isSelected());
                        newProd.setHasLarge(largeAdd.isSelected());
                        java.util.Map<String, Double> sizesMap = new java.util.HashMap<>();
                        sizesMap.put("Small", Double.parseDouble(smallPriceAdd.getText().isEmpty() ? "0" : smallPriceAdd.getText()));
                        sizesMap.put("Medium", Double.parseDouble(mediumPriceAdd.getText().isEmpty() ? "0" : mediumPriceAdd.getText()));
                        sizesMap.put("Large", Double.parseDouble(largePriceAdd.getText().isEmpty() ? "0" : largePriceAdd.getText()));
                        newProd.setSizeSurcharges(sizesMap);
                        
                        // Save milk options
                        newProd.setHasMilkOptions(enableMilkOptionsAdd.isSelected());
                        newProd.setHasOatMilk(oatMilkAdd.isSelected());
                        newProd.setHasAlmondMilk(almondMilkAdd.isSelected());
                        newProd.setHasSoyMilk(soyMilkAdd.isSelected());
                        newProd.setOatMilkPrice(Double.parseDouble(oatMilkPriceAdd.getText().isEmpty() ? "25" : oatMilkPriceAdd.getText()));
                        newProd.setAlmondMilkPrice(Double.parseDouble(almondMilkPriceAdd.getText().isEmpty() ? "25" : almondMilkPriceAdd.getText()));
                        newProd.setSoyMilkPrice(Double.parseDouble(soyMilkPriceAdd.getText().isEmpty() ? "25" : soyMilkPriceAdd.getText()));
                    } catch (Exception ignored) {}
                    return newProd;
                } catch (NumberFormatException ex) {
                    showAlert("Error", "Invalid price format. Please enter a valid number.", Alert.AlertType.ERROR);
                    return null;
                } catch (Exception ex) {
                    showAlert("Error", "Invalid input: " + ex.getMessage(), Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        java.util.Optional<Product> dlgRes = dialog.showAndWait();
        try { store.removeInventoryChangeListener(invListenerAdd); } catch (Exception ignored) {}
        dlgRes.ifPresent(product -> {
            if (product != null) {
                try {
                    String chosenCat = product.getCategory();
                    if (chosenCat != null && !chosenCat.trim().isEmpty()) {
                        store.addCategory(chosenCat.trim());
                    }

                    store.addProduct(product);

                    // Copy image to product images folder with product ID as filename
                    if (selectedImageFile[0] != null && selectedImageFile[0].exists()) {
                        File imagesDir = new File("data/images");
                        if (!imagesDir.exists()) {
                            imagesDir.mkdirs();
                        }
                        // Get file extension from original file
                        String fileName = selectedImageFile[0].getName();
                        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
                        // Save with product ID as filename
                        File destFile = new File(imagesDir, product.getId() + fileExtension);
                        Files.copy(selectedImageFile[0].toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    showAlert("Success", "Product added successfully!" + (selectedImageFile[0] != null ? "\nImage saved to data/images/" : ""), Alert.AlertType.INFORMATION);
                    // Refresh UI and make the new product visible in the Categories tab
                    refreshData();
                    try {
                        if (chosenCat != null && !chosenCat.trim().isEmpty() && categoriesListView != null) {
                            // ensure categories list is up-to-date
                            categoriesListView.getItems().setAll(store.getCategories());
                            // select the category of the newly added product
                            categoriesListView.getSelectionModel().select(chosenCat.trim());
                            // populate the products list for that category and select the newly added product
                            if (productsInCategoryListView != null) {
                                populateProductsInCategory(productsInCategoryListView, chosenCat.trim());
                                String search = product.getId() + " - ";
                                for (int i = 0; i < productsInCategoryListView.getItems().size(); i++) {
                                    String it = productsInCategoryListView.getItems().get(i);
                                    if (it != null && it.startsWith(search)) {
                                        productsInCategoryListView.getSelectionModel().clearSelection();
                                        productsInCategoryListView.getSelectionModel().select(i);
                                        productsInCategoryListView.scrollTo(i);
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                } catch (Exception ex) {
                    showAlert("Error", "Failed to add product: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void editProduct() {
        ProductRow selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a product to edit.", Alert.AlertType.WARNING);
            return;
        }

        // Find the actual Product object
        Product foundProduct = null;
        for (Product p : store.getProducts()) {
            if (p.getId().equals(selected.getId())) {
                foundProduct = p;
                break;
            }
        }

        if (foundProduct == null) {
            showAlert("Error", "Product not found.", Alert.AlertType.ERROR);
            return;
        }

        final Product product = foundProduct; // Make it effectively final

        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Edit Product");
        dialog.setHeaderText("Edit product: " + product.getName());
        dialog.setWidth(700);
        dialog.setHeight(900);

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        // ID (read-only)
        Label idLabel = new Label("ID:");
        Label idValueLabel = new Label(product.getId());
        idValueLabel.setStyle("-fx-font-weight: bold;");

        // Name (editable)
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField(product.getName());
        nameField.setPromptText("Enter product name");

        // Price (editable) - only show for Pastries, hide for Coffee/Tea (they use size pricing)
        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        priceField.setPromptText("Enter price");
        // Restrict input to numeric values only (digits and optional decimal point)
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> editPriceFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^\\d*(\\.\\d*)?$$")) {
                return change;
            }
            return null;
        };
        priceField.setTextFormatter(new javafx.scene.control.TextFormatter<>(editPriceFilter));

        // Check if product is Pastry (show price field) or Coffee/Tea (hide price field - use size pricing)
        String productCategory = product.getCategory();
        boolean isPastry = productCategory != null && productCategory.toLowerCase().contains("pastr");
        
        // Stock (editable - for adding stock)
        grid.add(idLabel, 0, 0);
        grid.add(idValueLabel, 1, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(nameField, 1, 1);
        
        // Only add price field for Pastries
        if (isPastry) {
            grid.add(new Label("Price:"), 0, 2);
            grid.add(priceField, 1, 2);
        }
        
        // Description field
        Label descEditLabel = new Label("Description:");
        TextArea descriptionEditField = new TextArea(product.getDescription());
        descriptionEditField.setPromptText("Enter product description (shown to customers)");
        descriptionEditField.setPrefRowCount(2);
        descriptionEditField.setWrapText(true);
        descriptionEditField.setPrefHeight(60);
        grid.add(descEditLabel, 0, 3);
        grid.add(descriptionEditField, 1, 3);

        // Available sizes rows for editing this product (each row: checkbox + price field)
        // For Coffee/Tea: sizes show actual price; For Pastries: sizes are hidden (single price)
        Label sizesAvailEditLabel = new Label("Available Sizes:");
        sizesAvailEditLabel.setStyle("-fx-font-weight: bold;");
        CheckBox smallEdit = new CheckBox("Small");
        CheckBox mediumEdit = new CheckBox("Medium");
        CheckBox largeEdit = new CheckBox("Large");
        TextField smallPriceEdit = new TextField();
        TextField mediumPriceEdit = new TextField();
        TextField largePriceEdit = new TextField();
        try {
            smallEdit.setSelected(product.isHasSmall());
            mediumEdit.setSelected(product.isHasMedium());
            largeEdit.setSelected(product.isHasLarge());
        } catch (Exception ignored) {
            smallEdit.setSelected(false); mediumEdit.setSelected(true); largeEdit.setSelected(true);
        }
        java.util.Map<String, Double> existing = product.getSizeSurcharges();
        smallPriceEdit.setText(String.valueOf(existing.getOrDefault("Small", 0.0)));
        mediumPriceEdit.setText(String.valueOf(existing.getOrDefault("Medium", 0.0)));
        largePriceEdit.setText(String.valueOf(existing.getOrDefault("Large", 0.0)));

        // numeric filter for edit price fields
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> editPriceFilter2 = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^\\d*(\\.\\d*)?$") ) return change;
            return null;
        };
        smallPriceEdit.setTextFormatter(new javafx.scene.control.TextFormatter<>(editPriceFilter2));
        mediumPriceEdit.setTextFormatter(new javafx.scene.control.TextFormatter<>(editPriceFilter2));
        largePriceEdit.setTextFormatter(new javafx.scene.control.TextFormatter<>(editPriceFilter2));

        VBox sizesBoxEdit = new VBox(8);
        // For Coffee/Tea show "Price:", for Pastries show "Surcharge:"
        String sizePriceLabel = isPastry ? "Surcharge:" : "Price:";
        HBox rowSmallEdit = new HBox(12, smallEdit, new Label(sizePriceLabel), smallPriceEdit);
        HBox rowMediumEdit = new HBox(12, mediumEdit, new Label(sizePriceLabel), mediumPriceEdit);
        HBox rowLargeEdit = new HBox(12, largeEdit, new Label(sizePriceLabel), largePriceEdit);
        rowSmallEdit.setAlignment(Pos.CENTER_LEFT);
        rowMediumEdit.setAlignment(Pos.CENTER_LEFT);
        rowLargeEdit.setAlignment(Pos.CENTER_LEFT);
        sizesBoxEdit.getChildren().addAll(rowSmallEdit, rowMediumEdit, rowLargeEdit);
        
        // Only show sizes section for non-Pastry products (Coffee/Tea)
        if (!isPastry) {
            grid.add(sizesAvailEditLabel, 0, 4);
            grid.add(sizesBoxEdit, 1, 4);
        }

        // Milk Options Section for Edit
        Label milkOptionsEditLabel = new Label("🥛 Milk Options:");
        milkOptionsEditLabel.setStyle("-fx-font-weight: bold;");
        
        CheckBox enableMilkOptionsEdit = new CheckBox("Enable Milk Options");
        enableMilkOptionsEdit.setSelected(product.isHasMilkOptions());
        
        CheckBox oatMilkEdit = new CheckBox("Oat Milk");
        oatMilkEdit.setSelected(product.isHasOatMilk());
        TextField oatMilkPriceEdit = new TextField(String.valueOf(product.getOatMilkPrice()));
        oatMilkPriceEdit.setPrefWidth(80);
        
        CheckBox almondMilkEdit = new CheckBox("Almond Milk");
        almondMilkEdit.setSelected(product.isHasAlmondMilk());
        TextField almondMilkPriceEdit = new TextField(String.valueOf(product.getAlmondMilkPrice()));
        almondMilkPriceEdit.setPrefWidth(80);
        
        CheckBox soyMilkEdit = new CheckBox("Soy Milk");
        soyMilkEdit.setSelected(product.isHasSoyMilk());
        TextField soyMilkPriceEdit = new TextField(String.valueOf(product.getSoyMilkPrice()));
        soyMilkPriceEdit.setPrefWidth(80);
        
        // Numeric filter for milk prices
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> milkPriceEditFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^\\d*(\\.\\d*)?$")) return change;
            return null;
        };
        oatMilkPriceEdit.setTextFormatter(new javafx.scene.control.TextFormatter<>(milkPriceEditFilter));
        almondMilkPriceEdit.setTextFormatter(new javafx.scene.control.TextFormatter<>(milkPriceEditFilter));
        soyMilkPriceEdit.setTextFormatter(new javafx.scene.control.TextFormatter<>(milkPriceEditFilter));
        
        // Enable/disable milk option rows based on main toggle
        enableMilkOptionsEdit.selectedProperty().addListener((obs, oldV, newV) -> {
            oatMilkEdit.setDisable(!newV);
            almondMilkEdit.setDisable(!newV);
            soyMilkEdit.setDisable(!newV);
            if (!newV) {
                oatMilkEdit.setSelected(false);
                almondMilkEdit.setSelected(false);
                soyMilkEdit.setSelected(false);
            }
        });
        
        // Enable price field when milk type is selected
        oatMilkEdit.selectedProperty().addListener((obs, oldV, newV) -> oatMilkPriceEdit.setDisable(!newV));
        almondMilkEdit.selectedProperty().addListener((obs, oldV, newV) -> almondMilkPriceEdit.setDisable(!newV));
        soyMilkEdit.selectedProperty().addListener((obs, oldV, newV) -> soyMilkPriceEdit.setDisable(!newV));
        
        VBox milkOptionsBoxEdit = new VBox(8);
        milkOptionsBoxEdit.getChildren().add(enableMilkOptionsEdit);
        HBox rowOatEdit = new HBox(12, oatMilkEdit, new Label("Price (+₱):"), oatMilkPriceEdit);
        HBox rowAlmondEdit = new HBox(12, almondMilkEdit, new Label("Price (+₱):"), almondMilkPriceEdit);
        HBox rowSoyEdit = new HBox(12, soyMilkEdit, new Label("Price (+₱):"), soyMilkPriceEdit);
        rowOatEdit.setAlignment(Pos.CENTER_LEFT);
        rowAlmondEdit.setAlignment(Pos.CENTER_LEFT);
        rowSoyEdit.setAlignment(Pos.CENTER_LEFT);
        milkOptionsBoxEdit.getChildren().addAll(rowOatEdit, rowAlmondEdit, rowSoyEdit);
        
        // Set initial disable state based on current product settings
        boolean milkEnabled = product.isHasMilkOptions();
        oatMilkEdit.setDisable(!milkEnabled);
        almondMilkEdit.setDisable(!milkEnabled);
        soyMilkEdit.setDisable(!milkEnabled);
        oatMilkPriceEdit.setDisable(!product.isHasOatMilk());
        almondMilkPriceEdit.setDisable(!product.isHasAlmondMilk());
        soyMilkPriceEdit.setDisable(!product.isHasSoyMilk());
        
        grid.add(milkOptionsEditLabel, 0, 5);
        grid.add(milkOptionsBoxEdit, 1, 5);

        // Image preview and upload
        javafx.scene.image.ImageView imagePreview = new javafx.scene.image.ImageView();
        imagePreview.setFitWidth(150);
        imagePreview.setFitHeight(150);
        imagePreview.setPreserveRatio(true);
        imagePreview.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #f5f5f5;");

        Label imagePathLabel = new Label("No image selected");
        final File[] selectedImageFile = {null};

        Button uploadImageBtn = new Button("📷 Change Image");
        uploadImageBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8 15; -fx-font-weight: bold;");
        uploadImageBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Product Image");
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File file = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null && file.exists()) {
                selectedImageFile[0] = file;
                try {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(file.toURI().toString(), 150, 150, true, true);
                    imagePreview.setImage(img);
                    imagePathLabel.setText(file.getName());
                } catch (Exception ex) {
                    showAlert("Error", "Failed to load image: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });

        VBox imageSection = new VBox(10);
        imageSection.setPadding(new Insets(15));
        imageSection.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");
        imageSection.getChildren().addAll(
            new Label("📸 Product Image:"),
            imagePreview,
            uploadImageBtn,
            imagePathLabel
        );

        grid.add(imageSection, 0, 5, 2, 1);

        // Ingredients editor: use a button to open a popup dialog instead of inline container
        java.util.Map<String, Double> selectedIngredients = new java.util.HashMap<>();
        java.util.Map<String, Double> existingRecipe = new java.util.HashMap<>();
        try { if (product.getRecipe() != null) existingRecipe.putAll(product.getRecipe()); } catch (Exception ignored) {}
        selectedIngredients.putAll(existingRecipe);

        // Create a label to show current ingredient count
        Label ingredientCountLabel = new Label();
        Runnable updateIngredientCount = () -> {
            int count = selectedIngredients.size();
            if (count == 0) {
                ingredientCountLabel.setText("No ingredients assigned");
                ingredientCountLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            } else {
                ingredientCountLabel.setText(count + " ingredient" + (count > 1 ? "s" : "") + " assigned");
                ingredientCountLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
            }
        };
        updateIngredientCount.run();

        Button editIngredientsBtn = new Button("🧪 Edit Ingredients");
        editIngredientsBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 10 20; -fx-font-weight: bold; -fx-font-size: 13;");
        editIngredientsBtn.setOnAction(evt -> {
            // Open ingredient editor dialog
            Dialog<java.util.Map<String, Double>> ingredientDialog = new Dialog<>();
            ingredientDialog.setTitle("Edit Ingredients");
            ingredientDialog.setHeaderText("Select ingredients for: " + product.getName() + "\n(Amount to deduct per product in inventory unit)");
            ingredientDialog.initOwner(dialog.getDialogPane().getScene().getWindow());
            
            ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            ingredientDialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
            
            VBox content = new VBox(12);
            content.setPadding(new Insets(15));
            content.setPrefWidth(500);
            content.setPrefHeight(400);
            
            // Get available ingredients
            final java.util.List<String> baseIngredients = new java.util.ArrayList<>(store.getInventory().keySet());
            if (baseIngredients.isEmpty()) {
                baseIngredients.addAll(java.util.Arrays.asList(
                    "Coffee Beans", "Milk", "Water", "Matcha Powder", "Chocolate Syrup",
                    "Vanilla Syrup", "Caramel Syrup", "Ice", "Mango Puree", "Strawberry Puree",
                    "Tea Leaves", "Flour", "Butter", "Sugar", "Egg"
                ));
            }
            for (String k : selectedIngredients.keySet()) {
                if (k != null && !k.trim().isEmpty() && !baseIngredients.contains(k)) baseIngredients.add(k);
            }
            
            java.util.List<String> filteredIngredients = filterIngredientsByCategory(baseIngredients, product.getCategory());
            if (filteredIngredients == null) filteredIngredients = new java.util.ArrayList<>(baseIngredients);
            
            // Temporary map for this dialog
            java.util.Map<String, Double> tempSelection = new java.util.HashMap<>(selectedIngredients);
            
            // Build a map of ingredient info for display (name -> unit, stock)
            java.util.Map<String, String> ingredientUnits = new java.util.HashMap<>();
            java.util.Map<String, Double> ingredientStocks = new java.util.HashMap<>();
            for (String ingName : filteredIngredients) {
                com.coffeeshop.model.InventoryItem invItem = null;
                try { invItem = store.getInventoryItem(ingName); } catch (Exception ignored) {}
                String unit = (invItem != null && invItem.getUnit() != null) ? invItem.getUnit() : "";
                double stock = (invItem != null) ? invItem.getQuantity() : 0.0;
                ingredientUnits.put(ingName, unit);
                ingredientStocks.put(ingName, stock);
            }
            
            // Add ingredient row
            HBox addRow = new HBox(10);
            addRow.setAlignment(Pos.CENTER_LEFT);
            ComboBox<String> ingredientCombo = new ComboBox<>();
            // Show name + unit + stock in dropdown
            for (String ingName : filteredIngredients) {
                String unit = ingredientUnits.getOrDefault(ingName, "");
                double stock = ingredientStocks.getOrDefault(ingName, 0.0);
                String displayText = ingName + " [" + stock + " " + unit + " available]";
                ingredientCombo.getItems().add(displayText);
            }
            ingredientCombo.setPromptText("Select ingredient to add");
            ingredientCombo.setPrefWidth(350);
            
            // Unit label that updates when selection changes
            Label unitLabel = new Label("");
            unitLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            unitLabel.setPrefWidth(60);
            
            ingredientCombo.setOnAction(e -> {
                String sel = ingredientCombo.getValue();
                if (sel != null && sel.contains(" [")) {
                    String ingName = sel.substring(0, sel.indexOf(" ["));
                    String unit = ingredientUnits.getOrDefault(ingName, "");
                    unitLabel.setText(unit);
                }
            });
            
            TextField addQtyField = new TextField("1.0");
            addQtyField.setPrefWidth(70);
            addQtyField.setPromptText("Qty");
            Button addBtn = new Button("+ Add");
            addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            addRow.getChildren().addAll(ingredientCombo, addQtyField, unitLabel, addBtn);
            
            // Selected ingredients display using FlowPane (tag-style chips)
            Label selectedLabel = new Label("Selected Ingredients:");
            selectedLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
            
            FlowPane chipsPane = new FlowPane(8, 8);
            chipsPane.setPadding(new Insets(10));
            chipsPane.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");
            chipsPane.setPrefWrapLength(460);
            chipsPane.setMinHeight(200);
            
            Runnable rebuildChips = () -> {
                chipsPane.getChildren().clear();
                if (tempSelection.isEmpty()) {
                    Label emptyLabel = new Label("No ingredients selected. Use the dropdown above to add.");
                    emptyLabel.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
                    chipsPane.getChildren().add(emptyLabel);
                } else {
                    for (java.util.Map.Entry<String, Double> entry : tempSelection.entrySet()) {
                        HBox chip = new HBox(6);
                        chip.setAlignment(Pos.CENTER_LEFT);
                        chip.setPadding(new Insets(6, 10, 6, 10));
                        chip.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 15; -fx-border-color: #1976D2; -fx-border-radius: 15;");
                        
                        Label nameLabel2 = new Label(entry.getKey());
                        nameLabel2.setStyle("-fx-font-weight: bold; -fx-text-fill: #1565C0;");
                        
                        TextField qtyEdit = new TextField(String.valueOf(entry.getValue()));
                        qtyEdit.setPrefWidth(60);
                        qtyEdit.setStyle("-fx-background-color: white; -fx-border-color: #90CAF9; -fx-border-radius: 3;");
                        qtyEdit.setOnKeyReleased(e -> {
                            try {
                                double val = Double.parseDouble(qtyEdit.getText());
                                tempSelection.put(entry.getKey(), val);
                            } catch (Exception ignored) {}
                        });
                        
                        Button removeBtn = new Button("✕");
                        removeBtn.setStyle("-fx-background-color: #EF5350; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 2 6; -fx-background-radius: 10;");
                        final String ingredientName = entry.getKey();
                        final Runnable[] rebuildRef = new Runnable[1];
                        removeBtn.setOnAction(e -> {
                            tempSelection.remove(ingredientName);
                            if (rebuildRef[0] != null) rebuildRef[0].run();
                        });
                        
                        chip.getChildren().addAll(nameLabel2, qtyEdit, removeBtn);
                        chipsPane.getChildren().add(chip);
                        
                        // Store reference for remove button
                        removeBtn.setUserData(rebuildRef);
                    }
                    // Update all remove button references
                    for (javafx.scene.Node node : chipsPane.getChildren()) {
                        if (node instanceof HBox) {
                            HBox chip = (HBox) node;
                            for (javafx.scene.Node child : chip.getChildren()) {
                                if (child instanceof Button) {
                                    Object data = child.getUserData();
                                    if (data instanceof Runnable[]) {
                                        ((Runnable[]) data)[0] = () -> {
                                            chipsPane.getChildren().clear();
                                            if (tempSelection.isEmpty()) {
                                                Label emptyLabel2 = new Label("No ingredients selected. Use the dropdown above to add.");
                                                emptyLabel2.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
                                                chipsPane.getChildren().add(emptyLabel2);
                                            } else {
                                                // Rebuild will happen via the outer rebuildChips
                                            }
                                        };
                                    }
                                }
                            }
                        }
                    }
                }
            };
            
            // Use a wrapper to allow recursive reference
            final Runnable[] rebuildChipsRef = {null};
            rebuildChipsRef[0] = () -> {
                chipsPane.getChildren().clear();
                if (tempSelection.isEmpty()) {
                    Label emptyLabel = new Label("No ingredients selected. Use the dropdown above to add.");
                    emptyLabel.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
                    chipsPane.getChildren().add(emptyLabel);
                } else {
                    for (java.util.Map.Entry<String, Double> entry : tempSelection.entrySet()) {
                        HBox chip = new HBox(6);
                        chip.setAlignment(Pos.CENTER_LEFT);
                        chip.setPadding(new Insets(6, 10, 6, 10));
                        chip.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 15; -fx-border-color: #1976D2; -fx-border-radius: 15;");
                        
                        Label nameLabel2 = new Label(entry.getKey());
                        nameLabel2.setStyle("-fx-font-weight: bold; -fx-text-fill: #1565C0;");
                        
                        TextField qtyEdit = new TextField(String.valueOf(entry.getValue()));
                        qtyEdit.setPrefWidth(60);
                        qtyEdit.setStyle("-fx-background-color: white; -fx-border-color: #90CAF9; -fx-border-radius: 3;");
                        qtyEdit.setOnKeyReleased(e -> {
                            try {
                                double val = Double.parseDouble(qtyEdit.getText());
                                tempSelection.put(entry.getKey(), val);
                            } catch (Exception ignored) {}
                        });
                        
                        // Add unit label
                        String chipUnit = ingredientUnits.getOrDefault(entry.getKey(), "");
                        Label chipUnitLabel = new Label(chipUnit);
                        chipUnitLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");
                        
                        Button removeBtn = new Button("✕");
                        removeBtn.setStyle("-fx-background-color: #EF5350; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 2 6; -fx-background-radius: 10;");
                        final String ingredientName = entry.getKey();
                        removeBtn.setOnAction(e -> {
                            tempSelection.remove(ingredientName);
                            rebuildChipsRef[0].run();
                        });
                        
                        chip.getChildren().addAll(nameLabel2, qtyEdit, chipUnitLabel, removeBtn);
                        chipsPane.getChildren().add(chip);
                    }
                }
            };
            rebuildChipsRef[0].run();
            
            addBtn.setOnAction(e -> {
                String selectedDisplay = ingredientCombo.getValue();
                if (selectedDisplay == null || selectedDisplay.trim().isEmpty()) return;
                // Extract actual ingredient name from display text "Name [stock unit available]"
                String selectedIngr = selectedDisplay.contains(" [") ? selectedDisplay.substring(0, selectedDisplay.indexOf(" [")) : selectedDisplay;
                try {
                    double qty = Double.parseDouble(addQtyField.getText().isEmpty() ? "1.0" : addQtyField.getText());
                    tempSelection.put(selectedIngr, qty);
                    rebuildChipsRef[0].run();
                    addQtyField.setText("1.0");
                } catch (Exception ignored) {}
            });
            
            content.getChildren().addAll(addRow, selectedLabel, chipsPane);
            ingredientDialog.getDialogPane().setContent(content);
            
            ingredientDialog.setResultConverter(btn -> {
                if (btn == saveBtn) {
                    return tempSelection;
                }
                return null;
            });
            
            java.util.Optional<java.util.Map<String, Double>> result = ingredientDialog.showAndWait();
            result.ifPresent(newIngredients -> {
                selectedIngredients.clear();
                selectedIngredients.putAll(newIngredients);
                updateIngredientCount.run();
            });
        });

        HBox ingredientRow = new HBox(15);
        ingredientRow.setAlignment(Pos.CENTER_LEFT);
        ingredientRow.getChildren().addAll(editIngredientsBtn, ingredientCountLabel);
        grid.add(ingredientRow, 0, 6, 2, 1);

        // Wrap edit grid in a ScrollPane so action buttons stay visible on small screens
        ScrollPane editScroll = new ScrollPane(grid);
        editScroll.setFitToWidth(true);
        editScroll.setFitToHeight(true);
        editScroll.setPrefViewportHeight(620);
        editScroll.setPrefViewportWidth(760);
        dialog.getDialogPane().setContent(editScroll);
        // attach fallback stylesheet to the dialog pane
        java.net.URL editCss = getClass().getResource("/styles/atlantafx-fallback.css");
        if (editCss != null) dialog.getDialogPane().getStylesheets().add(editCss.toExternalForm());
        // Make dialog more square and taller so content fits comfortably
        dialog.getDialogPane().setPrefWidth(760);
        dialog.getDialogPane().setPrefHeight(760);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try {
                    // Update name
                    String newName = nameField.getText().trim();
                    if (newName.isEmpty()) {
                        showAlert("Error", "Product name cannot be empty.", Alert.AlertType.ERROR);
                        return null;
                    }
                    product.setName(newName);
                    
                    // Update description
                    product.setDescription(descriptionEditField.getText());
                    
                    double newPrice = Double.parseDouble(priceField.getText());
                    // Update price
                    product.setPrice(newPrice);

                    // Update whether this product has sizes and surcharges
                    try {
                        product.setHasSizes((smallEdit.isSelected() || mediumEdit.isSelected() || largeEdit.isSelected()));
                        product.setHasSmall(smallEdit.isSelected());
                        product.setHasMedium(mediumEdit.isSelected());
                        product.setHasLarge(largeEdit.isSelected());
                        java.util.Map<String, Double> sizesMap = new java.util.HashMap<>();
                        sizesMap.put("Small", Double.parseDouble(smallPriceEdit.getText().isEmpty() ? "0" : smallPriceEdit.getText()));
                        sizesMap.put("Medium", Double.parseDouble(mediumPriceEdit.getText().isEmpty() ? "0" : mediumPriceEdit.getText()));
                        sizesMap.put("Large", Double.parseDouble(largePriceEdit.getText().isEmpty() ? "0" : largePriceEdit.getText()));
                        product.setSizeSurcharges(sizesMap);
                        
                        // Update milk options
                        product.setHasMilkOptions(enableMilkOptionsEdit.isSelected());
                        product.setHasOatMilk(oatMilkEdit.isSelected());
                        product.setHasAlmondMilk(almondMilkEdit.isSelected());
                        product.setHasSoyMilk(soyMilkEdit.isSelected());
                        product.setOatMilkPrice(Double.parseDouble(oatMilkPriceEdit.getText().isEmpty() ? "25" : oatMilkPriceEdit.getText()));
                        product.setAlmondMilkPrice(Double.parseDouble(almondMilkPriceEdit.getText().isEmpty() ? "25" : almondMilkPriceEdit.getText()));
                        product.setSoyMilkPrice(Double.parseDouble(soyMilkPriceEdit.getText().isEmpty() ? "25" : soyMilkPriceEdit.getText()));
                    } catch (Exception ignored) {}

                    // Validate selected ingredient quantities
                    if (selectedIngredients != null) {
                        for (java.util.Map.Entry<String, Double> e : selectedIngredients.entrySet()) {
                            Double v = e.getValue();
                            if (v == null || v <= 0) {
                                showAlert("Invalid Ingredient Quantity", "Ingredient '" + e.getKey() + "' must have a positive quantity.", Alert.AlertType.ERROR);
                                return null;
                            }
                        }

                        // remove any zero-quantity entries and set cleaned recipe
                        java.util.Map<String, Double> cleaned = new java.util.HashMap<>();
                        for (java.util.Map.Entry<String, Double> e : selectedIngredients.entrySet()) {
                            if (e.getValue() != null && e.getValue() > 0) cleaned.put(e.getKey(), e.getValue());
                        }
                        product.setRecipe(cleaned);
                    }

                    store.saveData();

                    // Update image if selected
                    if (selectedImageFile[0] != null && selectedImageFile[0].exists()) {
                        File imagesDir = new File("data/images");
                        if (!imagesDir.exists()) {
                            imagesDir.mkdirs();
                        }
                        
                        // Delete all existing images for this product ID (any extension)
                        String productId = product.getId();
                        File[] existingImages = imagesDir.listFiles((dir, name) -> 
                            name.startsWith(productId + ".") || name.equals(productId + ".jpg") || 
                            name.equals(productId + ".jpeg") || name.equals(productId + ".png") || 
                            name.equals(productId + ".gif") || name.equals(productId + ".bmp"));
                        if (existingImages != null) {
                            for (File oldImage : existingImages) {
                                oldImage.delete();
                            }
                        }
                        
                        // Copy new image with product ID as filename
                        String fileName = selectedImageFile[0].getName();
                        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
                        File destFile = new File(imagesDir, product.getId() + fileExtension);
                        Files.copy(selectedImageFile[0].toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    // persist changes
                    store.saveData();
                    return product;
                } catch (NumberFormatException ex) {
                    showAlert("Error", "Invalid price or stock format.", Alert.AlertType.ERROR);
                    return null;
                } catch (Exception ex) {
                    showAlert("Error", "Failed to update product: " + ex.getMessage(), Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        java.util.Optional<Product> dlgRes = dialog.showAndWait();
        dlgRes.ifPresent(updatedProduct -> {
            if (updatedProduct != null) {
                showAlert("Success", "Product updated successfully!", Alert.AlertType.INFORMATION);
                refreshData();
            }
        });
    }

    // Load bundled AtlantisFX fallback stylesheet if present and apply to the scene.
    private void applyAtlantafx(Scene scene) {
        if (scene == null) return;
        try {
            java.net.URL u = getClass().getResource("/styles/atlantafx-fallback.css");
            if (u != null) scene.getStylesheets().add(u.toExternalForm());
        } catch (Exception ignored) {}
    }

    private void refillIngredient() {
        InventoryRow selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an ingredient to refill.", Alert.AlertType.WARNING);
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refill Ingredient");
        dialog.setHeaderText("Refill: " + selected.getName());
        dialog.setContentText("Enter amount to add (" + selected.getUnit() + "):");
        // Apply numeric filter to the editor
        dialog.getEditor().setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^\\d*(\\.\\d*)?$")) return change;
            return null;
        }));

        dialog.showAndWait().ifPresent(amount -> {
            try {
                double refillAmount = Double.parseDouble(amount);
                if (refillAmount <= 0) {
                    showAlert("Invalid Amount", "Please enter a positive number.", Alert.AlertType.ERROR);
                    return;
                }

                store.refillInventory(selected.getName(), refillAmount);
                showAlert("Success", "Ingredient refilled successfully!", Alert.AlertType.INFORMATION);
                refreshData();
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter a valid number.", Alert.AlertType.ERROR);
            }
        });
    }

    private void deductInventoryQuantity() {
        InventoryRow selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Please select an ingredient to deduct.", Alert.AlertType.WARNING); return; }

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Deduct Quantity");
        dlg.setHeaderText("Deduct from: " + selected.getName());
        dlg.setContentText("Amount to deduct (" + selected.getUnit() + "):");
        // Apply numeric filter to the editor
        dlg.getEditor().setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^\\d*(\\.\\d*)?$")) return change;
            return null;
        }));

        dlg.showAndWait().ifPresent(s -> {
            try {
                double amt = Double.parseDouble(s);
                if (amt <= 0) { showAlert("Invalid", "Please enter a positive number.", Alert.AlertType.ERROR); return; }
                // push undo action
                com.coffeeshop.model.InventoryItem snapshot = new com.coffeeshop.model.InventoryItem(selected.getName(), selected.getQuantity(), selected.getUnit());
                undoStack.push(new UndoAction(UndoAction.Type.DEDUCT, snapshot, amt));
                Store.getInstance().deductInventory(selected.getName(), amt);
                showAlert("Success", "Quantity deducted.", Alert.AlertType.INFORMATION);
                refreshData();
            } catch (NumberFormatException ex) {
                showAlert("Invalid Input", "Please enter a valid number.", Alert.AlertType.ERROR);
            }
        });
    }

    private void deleteInventoryItem() {
        InventoryRow selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("No Selection", "Please select an ingredient to delete.", Alert.AlertType.WARNING); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Ingredient");
        confirm.setHeaderText("Delete: " + selected.getName());
        confirm.setContentText("Are you sure you want to permanently remove this ingredient from inventory?");
        java.util.Optional<javafx.scene.control.ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            // push undo action (store snapshot)
            com.coffeeshop.model.InventoryItem snapshot = new com.coffeeshop.model.InventoryItem(selected.getName(), selected.getQuantity(), selected.getUnit());
            undoStack.push(new UndoAction(UndoAction.Type.DELETE, snapshot, 0));
            Store.getInstance().removeInventoryItem(selected.getName());
            showAlert("Deleted", "Ingredient moved to archive (removed items). Use Undo to restore.", Alert.AlertType.INFORMATION);
            refreshData();
        }
    }

    private void undoLastAction() {
        if (undoStack.isEmpty()) { showAlert("Nothing to Undo", "There are no recent delete/deduct actions to undo.", Alert.AlertType.INFORMATION); return; }
        UndoAction act = undoStack.pop();
        if (act == null) { showAlert("Nothing to Undo", "No action found.", Alert.AlertType.INFORMATION); return; }

        if (act.type == UndoAction.Type.DELETE) {
            // restore removed item
            if (act.snapshot != null) {
                Store.getInstance().restoreRemovedItem(act.snapshot.getName());
                showAlert("Restored", "Deleted ingredient was restored.", Alert.AlertType.INFORMATION);
                refreshData();
            }
        } else if (act.type == UndoAction.Type.DEDUCT) {
            // add back the deducted amount
            if (act.snapshot != null && act.amount > 0) {
                Store.getInstance().refillInventory(act.snapshot.getName(), act.amount);
                showAlert("Undone", "Deduction undone and quantity restored.", Alert.AlertType.INFORMATION);
                refreshData();
            }
        }
    }

    private void importIngredientsFromProducts() {
        // Collect all ingredient names referenced by product recipes
        java.util.Set<String> referenced = new java.util.HashSet<>();
        for (Product p : store.getProducts()) {
            try {
                if (p.getRecipe() != null) referenced.addAll(p.getRecipe().keySet());
            } catch (Exception ignored) {}
        }

        if (referenced.isEmpty()) {
            showAlert("No Ingredients Found", "No ingredients were found in any product recipes.", Alert.AlertType.INFORMATION);
            return;
        }

        int added = 0;
        for (String name : referenced) {
            if (name == null) continue;
            String key = name.trim();
            if (key.isEmpty()) continue;
            if (store.getInventoryItem(key) == null && !store.getRemovedInventory().containsKey(key)) {
                // Create with zero quantity and default unit "pcs"
                com.coffeeshop.model.InventoryItem it = new com.coffeeshop.model.InventoryItem(key, 0.0, "pcs");
                store.addInventoryItem(it);
                added++;
            }
        }

        if (added > 0) {
            showAlert("Imported", String.format("Added %d missing ingredient(s) to inventory.", added), Alert.AlertType.INFORMATION);
        } else {
            showAlert("Up to Date", "All ingredients referenced by products are already present in inventory.", Alert.AlertType.INFORMATION);
        }
        refreshData();
    }

    private static class UndoAction {
        enum Type { DELETE, DEDUCT }
        Type type;
        com.coffeeshop.model.InventoryItem snapshot;
        double amount;
        UndoAction(Type type, com.coffeeshop.model.InventoryItem snapshot, double amount) { this.type = type; this.snapshot = snapshot; this.amount = amount; }
    }

    private void addIngredient() {
        Dialog<InventoryItem> dialog = new Dialog<>();
        dialog.setTitle("Add Ingredient");
        dialog.setHeaderText("Enter ingredient details:");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Ingredient Name");
        TextField quantityField = new TextField();
        quantityField.setPromptText("1000");
        // Restrict to numeric input
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> invQtyFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^\\d*(\\.\\d*)?$")) return change;
            return null;
        };
        quantityField.setTextFormatter(new javafx.scene.control.TextFormatter<>(invQtyFilter));
        TextField unitField = new TextField();
        unitField.setPromptText("ml/g/pcs");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Quantity:"), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(new Label("Unit:"), 0, 2);
        grid.add(unitField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String name = nameField.getText();
                    double quantity = Double.parseDouble(quantityField.getText());
                    String unit = unitField.getText();
                    return new InventoryItem(name, quantity, unit);
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(item -> {
            if (item != null) {
                store.addInventoryItem(item);
                showAlert("Success", "Ingredient added successfully!", Alert.AlertType.INFORMATION);
                refreshData();
            } else {
                showAlert("Error", "Invalid input. Please try again.", Alert.AlertType.ERROR);
            }
        });
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // TableView data classes
    // ==================== TRANSACTION HISTORY TAB ====================

    private void refreshTransactionHistoryContent() {
        if (transactionHistoryContent == null) return;
        transactionHistoryContent.getChildren().clear();
        transactionHistoryContent.getChildren().addAll(createTransactionHistoryTab().getChildren());
    }

    private VBox createTransactionHistoryTab() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(25));
        panel.setStyle("-fx-background-color: #F3F4F6;");

        // Header with filters
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("💳 Transaction History & Performance");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#111827"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Date filter
        ComboBox<String> dateFilter = new ComboBox<>();
        dateFilter.getItems().addAll("Today", "This Week", "This Month", "All Time", "Custom Range");
        dateFilter.setValue("Today");
        dateFilter.setStyle("-fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-border-radius: 8;");
        
        // Cashier filter
        ComboBox<String> cashierFilter = new ComboBox<>();
        cashierFilter.getItems().add("All Cashiers");
        for (CashierAccount acc : store.getCashiers()) {
            cashierFilter.getItems().add(acc.getUsername());
        }
        cashierFilter.setValue("All Cashiers");
        cashierFilter.setStyle("-fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-border-radius: 8;");
        
        // Export button
        Button exportBtn = new Button("📥 Export CSV");
        exportBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        
        header.getChildren().addAll(title, spacer, dateFilter, cashierFilter, exportBtn);

        // Load receipts
        List<Receipt> allReceipts = TextDatabase.loadAllReceipts();
        
        // Performance cards row
        HBox perfCards = new HBox(15);
        perfCards.setAlignment(Pos.TOP_CENTER);
        
        // Calculate per-cashier stats
        Map<String, Double> cashierSales = new HashMap<>();
        Map<String, Integer> cashierTransactions = new HashMap<>();
        
        for (Receipt r : allReceipts) {
            String cashier = r.getCashierId();
            if (cashier == null || cashier.isEmpty()) cashier = "Unknown";
            
            cashierSales.put(cashier, cashierSales.getOrDefault(cashier, 0.0) + r.getTotalAmount());
            cashierTransactions.put(cashier, cashierTransactions.getOrDefault(cashier, 0) + 1);
        }
        
        // Create performance cards for each cashier
        for (String cashier : cashierSales.keySet()) {
            double totalSales = cashierSales.get(cashier);
            int txCount = cashierTransactions.get(cashier);
            double avgSale = txCount > 0 ? totalSales / txCount : 0;
            
            VBox card = createCashierPerformanceCard(cashier, totalSales, txCount, avgSale);
            HBox.setHgrow(card, Priority.ALWAYS);
            perfCards.getChildren().add(card);
        }
        
        // If no cashiers found, show placeholder
        if (perfCards.getChildren().isEmpty()) {
            Label placeholder = new Label("No transaction data available");
            placeholder.setFont(Font.font("Segoe UI", 14));
            placeholder.setTextFill(Color.web("#6B7280"));
            perfCards.getChildren().add(placeholder);
        }
        
        // Charts row
        HBox chartsRow = new HBox(15);
        
        // Sales by Cashier Bar Chart
        VBox salesChartCard = new VBox(12);
        salesChartCard.setPadding(new Insets(20));
        salesChartCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        HBox.setHgrow(salesChartCard, Priority.ALWAYS);
        
        Label salesChartTitle = new Label("📊 Sales by Cashier");
        salesChartTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        CategoryAxis xAxisSales = new CategoryAxis();
        NumberAxis yAxisSales = new NumberAxis();
        BarChart<String, Number> salesBarChart = new BarChart<>(xAxisSales, yAxisSales);
        salesBarChart.setLegendVisible(false);
        salesBarChart.setPrefHeight(250);
        salesBarChart.setStyle("-fx-background-color: transparent;");
        
        XYChart.Series<String, Number> salesSeries = new XYChart.Series<>();
        for (Map.Entry<String, Double> entry : cashierSales.entrySet()) {
            salesSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        salesBarChart.getData().add(salesSeries);
        
        salesChartCard.getChildren().addAll(salesChartTitle, salesBarChart);
        
        // Transactions Over Time Line Chart
        VBox timeChartCard = new VBox(12);
        timeChartCard.setPadding(new Insets(20));
        timeChartCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        HBox.setHgrow(timeChartCard, Priority.ALWAYS);
        
        Label timeChartTitle = new Label("📈 Transactions Today");
        timeChartTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        CategoryAxis xAxisTime = new CategoryAxis();
        NumberAxis yAxisTime = new NumberAxis();
        LineChart<String, Number> timeLineChart = new LineChart<>(xAxisTime, yAxisTime);
        timeLineChart.setLegendVisible(false);
        timeLineChart.setPrefHeight(250);
        timeLineChart.setStyle("-fx-background-color: transparent;");
        timeLineChart.setCreateSymbols(true);
        
        // Group by hour for today
        Map<Integer, Integer> hourlyTransactions = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (Receipt r : allReceipts) {
            if (r.getReceiptTime().toLocalDate().equals(today)) {
                int hour = r.getReceiptTime().getHour();
                hourlyTransactions.put(hour, hourlyTransactions.getOrDefault(hour, 0) + 1);
            }
        }
        
        XYChart.Series<String, Number> timeSeries = new XYChart.Series<>();
        for (int i = 0; i < 24; i++) {
            timeSeries.getData().add(new XYChart.Data<>(i + ":00", hourlyTransactions.getOrDefault(i, 0)));
        }
        timeLineChart.getData().add(timeSeries);
        
        timeChartCard.getChildren().addAll(timeChartTitle, timeLineChart);
        
        chartsRow.getChildren().addAll(salesChartCard, timeChartCard);
        
        // Transaction History Table
        VBox tableCard = new VBox(12);
        tableCard.setPadding(new Insets(20));
        tableCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        
        Label tableTitle = new Label("📋 Transaction History");
        tableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        TableView<Receipt> transactionTable = new TableView<>();
        transactionTable.setPrefHeight(350);
        
        TableColumn<Receipt, String> receiptIdCol = new TableColumn<>("Receipt ID");
        receiptIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getReceiptId()));
        receiptIdCol.setPrefWidth(120);
        
        TableColumn<Receipt, String> cashierCol = new TableColumn<>("Cashier");
        cashierCol.setCellValueFactory(data -> {
            String cashier = data.getValue().getCashierId();
            return new javafx.beans.property.SimpleStringProperty(cashier != null && !cashier.isEmpty() ? cashier : "Unknown");
        });
        cashierCol.setPrefWidth(120);
        
        TableColumn<Receipt, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUserName()));
        customerCol.setPrefWidth(150);
        
        TableColumn<Receipt, String> timeCol = new TableColumn<>("Date & Time");
        timeCol.setCellValueFactory(data -> {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
            return new javafx.beans.property.SimpleStringProperty(data.getValue().getReceiptTime().format(fmt));
        });
        timeCol.setPrefWidth(180);
        
        TableColumn<Receipt, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.format("₱%.2f", data.getValue().getTotalAmount())));
        amountCol.setPrefWidth(120);
        amountCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        
        TableColumn<Receipt, String> orderIdCol = new TableColumn<>("Order ID");
        orderIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getOrderId()));
        orderIdCol.setPrefWidth(150);
        
        transactionTable.getColumns().addAll(receiptIdCol, cashierCol, customerCol, timeCol, amountCol, orderIdCol);
        
        // Populate table (show most recent first)
        javafx.collections.ObservableList<Receipt> receiptData = javafx.collections.FXCollections.observableArrayList(allReceipts);
        receiptData.sort((r1, r2) -> r2.getReceiptTime().compareTo(r1.getReceiptTime()));
        transactionTable.setItems(receiptData);
        
        // Filter logic
        Runnable applyFilters = () -> {
            String selectedDate = dateFilter.getValue();
            String selectedCashier = cashierFilter.getValue();
            
            List<Receipt> filtered = new ArrayList<>(allReceipts);
            
            // Date filter
            LocalDateTime now = LocalDateTime.now();
            if ("Today".equals(selectedDate)) {
                filtered.removeIf(r -> !r.getReceiptTime().toLocalDate().equals(now.toLocalDate()));
            } else if ("This Week".equals(selectedDate)) {
                LocalDateTime weekStart = now.minusDays(7);
                filtered.removeIf(r -> r.getReceiptTime().isBefore(weekStart));
            } else if ("This Month".equals(selectedDate)) {
                LocalDateTime monthStart = now.minusMonths(1);
                filtered.removeIf(r -> r.getReceiptTime().isBefore(monthStart));
            }
            
            // Cashier filter
            if (!"All Cashiers".equals(selectedCashier)) {
                filtered.removeIf(r -> {
                    String c = r.getCashierId();
                    return c == null || !c.equals(selectedCashier);
                });
            }
            
            filtered.sort((r1, r2) -> r2.getReceiptTime().compareTo(r1.getReceiptTime()));
            transactionTable.setItems(javafx.collections.FXCollections.observableArrayList(filtered));
        };
        
        dateFilter.setOnAction(e -> applyFilters.run());
        cashierFilter.setOnAction(e -> applyFilters.run());
        
        // Export functionality
        exportBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Transactions");
            fileChooser.setInitialFileName("transactions_" + LocalDate.now() + ".csv");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = fileChooser.showSaveDialog(null);
            
            if (file != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write("Receipt ID,Cashier,Customer,Date,Time,Amount,Order ID\n");
                    for (Receipt r : transactionTable.getItems()) {
                        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
                        writer.write(String.format("%s,%s,%s,%s,%s,%.2f,%s\n",
                            r.getReceiptId(),
                            r.getCashierId() != null ? r.getCashierId() : "Unknown",
                            r.getUserName(),
                            r.getReceiptTime().format(dateFmt),
                            r.getReceiptTime().format(timeFmt),
                            r.getTotalAmount(),
                            r.getOrderId()));
                    }
                    showAlert("Export Successful", "Transactions exported to: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
                } catch (IOException ex) {
                    showAlert("Export Failed", "Error: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
        
        tableCard.getChildren().addAll(tableTitle, transactionTable);
        
        panel.getChildren().addAll(header, perfCards, chartsRow, tableCard);
        return panel;
    }
    
    private VBox createCashierPerformanceCard(String cashierName, double totalSales, int transactionCount, double avgSale) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        card.setPrefWidth(250);
        card.setMinHeight(150);
        
        // Cashier icon and name
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("👤");
        icon.setFont(Font.font(20));
        Label name = new Label(cashierName);
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        name.setTextFill(Color.web("#111827"));
        nameRow.getChildren().addAll(icon, name);
        
        // Total sales
        Label salesLabel = new Label(String.format("₱%.2f", totalSales));
        salesLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        salesLabel.setTextFill(Color.web("#10B981"));
        
        // Transaction count
        Label txLabel = new Label(transactionCount + " transactions");
        txLabel.setFont(Font.font("Segoe UI", 13));
        txLabel.setTextFill(Color.web("#6B7280"));
        
        // Average sale
        Label avgLabel = new Label("Avg: ₱" + String.format("%.2f", avgSale));
        avgLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        avgLabel.setTextFill(Color.web("#3B82F6"));
        
        card.getChildren().addAll(nameRow, salesLabel, txLabel, avgLabel);
        return card;
    }

    // ==================== INNER CLASSES ====================

    public static class ProductRow {
        private String id;
        private String name;
        private double price;
        private String status;

        public ProductRow(String id, String name, double price, String status) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.status = status;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public String getStatus() { return status; }
    }

    public static class InventoryRow {
        private String name;
        private double quantity;
        private String unit;

        public InventoryRow(String name, double quantity, String unit) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
        }

        public String getName() { return name; }
        public double getQuantity() { return quantity; }
        public String getUnit() { return unit; }
        public void setQuantity(double q) { this.quantity = q; }
    }

    public static class StockAlertRow {
        private String status;
        private String name;
        private String quantity;
        private String level;

        public StockAlertRow(String status, String name, String quantity, String level) {
            this.status = status;
            this.name = name;
            this.quantity = quantity;
            this.level = level;
        }

        public String getStatus() { return status; }
        public String getName() { return name; }
        public String getQuantity() { return quantity; }
        public String getLevel() { return level; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
