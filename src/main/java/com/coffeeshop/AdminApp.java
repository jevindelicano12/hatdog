package com.coffeeshop;

import com.coffeeshop.model.*;
import com.coffeeshop.service.Store;
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
    private javafx.scene.control.ListView<com.coffeeshop.model.InventoryItem> alertsListView;
    private Label adminTimeLabel;
    private Label netSalesLabel;
    private Label pendingOrdersLabel;
    private Label completedOrdersLabel;
    private Label lowStockLabel;
    private javafx.scene.control.ListView<String> categoriesListView;
    private TextArea dashboardAlertsArea;
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
    private VBox dashboardContent, productsContent, inventoryContent, addOnsContent, specialRequestsContent, categoriesContent, accountsContent, refillAlertsContent, reportsContent, transactionHistoryContent;
    private VBox archivedContent;
    private TableView<InventoryRow> archivedTable;

    @Override
    public void start(Stage primaryStage) {
        store = Store.getInstance();

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
        refillAlertsContent = createRefillAlertsTab();
        archivedContent = createArchivedTab();
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

    private VBox createSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(230);
        sidebar.setMaxWidth(230);
        sidebar.setStyle("-fx-background-color: #1F2937;");
        
        // Logo and branding at top
        VBox brandSection = new VBox(5);
        brandSection.setPadding(new Insets(25, 20, 25, 20));
        brandSection.setAlignment(Pos.CENTER_LEFT);
        
        HBox logoRow = new HBox(12);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        
        // Coffee cup icon
        Label cupIcon = new Label("☕");
        cupIcon.setFont(Font.font("Segoe UI", 24));
        cupIcon.setTextFill(Color.web("#FFFFFF"));
        
        VBox brandText = new VBox(0);
        Label brewiseLabel = new Label("brewise");
        brewiseLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        brewiseLabel.setTextFill(Color.web("#FFFFFF"));
        
        Label adminLabel = new Label("ADMIN PANEL");
        adminLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        adminLabel.setTextFill(Color.web("#9CA3AF"));
        
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
        
        VBox businessSection = new VBox(0);
        businessSection.getChildren().addAll(businessHeader, dashboardBtn, reportsBtn, transactionsBtn);
        
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
        
        // SYSTEM section
        Label systemHeader = new Label("SYSTEM");
        systemHeader.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        systemHeader.setTextFill(Color.web("#6B7280"));
        systemHeader.setPadding(new Insets(5, 20, 5, 20));
        
        Button refillBtn = createNavButton("🔔", "Refill Alerts", false);
        Button archivedBtn = createNavButton("🗄️", "Archived Items", false);
        
        VBox systemSection = new VBox(0);
        systemSection.getChildren().addAll(systemHeader, refillBtn);
        
        navContainer.getChildren().addAll(businessSection, managementSection, systemSection);
        
        // Admin user at bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        HBox adminUserBox = new HBox(10);
        adminUserBox.setPadding(new Insets(15, 20, 20, 20));
        adminUserBox.setAlignment(Pos.CENTER_LEFT);
        adminUserBox.setStyle("-fx-background-color: #374151; -fx-background-radius: 10;");
        
        Label adminIcon = new Label("👤");
        adminIcon.setFont(Font.font(20));
        adminIcon.setTextFill(Color.web("#FFA500"));
        
        Label adminNameLabel = new Label("Administrator");
        adminNameLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        adminNameLabel.setTextFill(Color.web("#FFFFFF"));
        
        adminUserBox.getChildren().addAll(adminIcon, adminNameLabel);
        
        VBox adminContainer = new VBox();
        adminContainer.setPadding(new Insets(0, 15, 15, 15));
        adminContainer.getChildren().add(adminUserBox);
        
        sidebar.getChildren().addAll(brandSection, navContainer, spacer, adminContainer);
        
        // Wire up navigation buttons
        dashboardBtn.setOnAction(e -> { setActiveNav(dashboardBtn); showContent(dashboardContent); });
        reportsBtn.setOnAction(e -> { setActiveNav(reportsBtn); showContent(reportsContent); });
        transactionsBtn.setOnAction(e -> { setActiveNav(transactionsBtn); refreshTransactionHistoryContent(); showContent(transactionHistoryContent); });
        productsBtn.setOnAction(e -> { setActiveNav(productsBtn); showContent(productsContent); });
        inventoryBtn.setOnAction(e -> { setActiveNav(inventoryBtn); showContent(inventoryContent); });
        addOnsBtn.setOnAction(e -> { setActiveNav(addOnsBtn); refreshAddOnsContent(); showContent(addOnsContent); });
        specialReqBtn.setOnAction(e -> { setActiveNav(specialReqBtn); refreshSpecialRequestsContent(); showContent(specialRequestsContent); });
        categoriesBtn.setOnAction(e -> { setActiveNav(categoriesBtn); showContent(categoriesContent); });
        accountsBtn.setOnAction(e -> { setActiveNav(accountsBtn); showContent(accountsContent); });
        refillBtn.setOnAction(e -> { setActiveNav(refillBtn); showContent(refillAlertsContent); });
        archivedBtn.setOnAction(e -> { setActiveNav(archivedBtn); refreshArchivedItems(); showContent(archivedContent); });
        
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
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-cursor: hand;");
            iconLabel.setTextFill(Color.web("#9CA3AF"));
            textLabel.setTextFill(Color.web("#9CA3AF"));
        }
        
        btn.setOnMouseEntered(e -> {
            if (btn != activeNavButton) {
                btn.setStyle("-fx-background-color: #374151; -fx-text-fill: #FFFFFF; -fx-background-radius: 8; -fx-cursor: hand;");
                iconLabel.setTextFill(Color.web("#FFFFFF"));
                textLabel.setTextFill(Color.web("#FFFFFF"));
            }
        });
        
        btn.setOnMouseExited(e -> {
            if (btn != activeNavButton) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-cursor: hand;");
                iconLabel.setTextFill(Color.web("#9CA3AF"));
                textLabel.setTextFill(Color.web("#9CA3AF"));
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
            
            activeNavButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-cursor: hand;");
            oldIcon.setTextFill(Color.web("#9CA3AF"));
            oldText.setTextFill(Color.web("#9CA3AF"));
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
        actionsCol.setPrefWidth(400);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(8);
            private final Button toggleBtn = new Button();
            private final Button pwdBtn = new Button("Change Password");
            private final Button userBtn = new Button("Change Username");

            {
                toggleBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                pwdBtn.setStyle("-fx-font-size: 11px;");
                userBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #1976D2; -fx-text-fill: white;");
                box.getChildren().addAll(toggleBtn, pwdBtn, userBtn);
                
                toggleBtn.setOnAction(e -> {
                    CashierRow row = getTableView().getItems().get(getIndex());
                    boolean newActive = !row.isActiveFlag();
                    Store.getInstance().setCashierActive(row.getId(), newActive);
                    row.setStatus(newActive ? "Active" : "Inactive");
                    row.setActiveFlag(newActive);
                    getTableView().refresh();
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
        
        // Export PDF button
        Button exportPdfBtn = new Button("📄 Export PDF");
        exportPdfBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        exportPdfBtn.setOnMouseEntered(e -> exportPdfBtn.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;"));
        exportPdfBtn.setOnMouseExited(e -> exportPdfBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;"));
        exportPdfBtn.setOnAction(e -> exportDashboardToPdf(panel));
        
        header.getChildren().addAll(title, headerSpacer, exportPdfBtn);

        // Top metric cards
        HBox metricsRow = new HBox(14);
        metricsRow.setPadding(new Insets(6,0,6,0));

        // Create cards with icons and proper styling
        VBox cardTotal = createMetricCardWithIcon("💵", "Total Revenue", "₱0.00", "+2%", "#E8F8F5");
        VBox cardOnProgress = createMetricCardWithIcon("⏱", "On Progress", "10", "Orders", "#FEF6E6");
        VBox cardPerformance = createMetricCardWithIcon("✓", "Performance", "Good", "2/24", "#E8F5E9");
        VBox cardToday = createMetricCardWithIcon("📊", "Today Sales", "234", "+2%", "#E8F5E9");
        VBox cardOnProgress2 = createMetricCardWithIcon("📈", "On Progress", "10", "Orders", "#E3F2FD");

        HBox.setHgrow(cardTotal, Priority.ALWAYS);
        HBox.setHgrow(cardOnProgress, Priority.ALWAYS);
        HBox.setHgrow(cardPerformance, Priority.ALWAYS);
        HBox.setHgrow(cardToday, Priority.ALWAYS);
        HBox.setHgrow(cardOnProgress2, Priority.ALWAYS);

        metricsRow.getChildren().addAll(cardTotal, cardOnProgress, cardPerformance, cardToday, cardOnProgress2);

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

        // Right column: score gauge + complaint cards
        VBox rightCol = new VBox(12);
        rightCol.setPrefWidth(350);
        rightCol.setMinWidth(350);

        // Score card with gauge visualization
        VBox scoreCard = new VBox(12);
        scoreCard.setPadding(new Insets(18));
        scoreCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10,0,0,2);");
        
        HBox scoreHeader = new HBox();
        scoreHeader.setAlignment(Pos.CENTER_LEFT);
        Label scoreTitle = new Label("⭐ Score");
        scoreTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        Region scoreSpacer = new Region();
        HBox.setHgrow(scoreSpacer, Priority.ALWAYS);
        Label moreIcon = new Label("⋯");
        moreIcon.setFont(Font.font(20));
        scoreHeader.getChildren().addAll(scoreTitle, scoreSpacer, moreIcon);
        
        // Score gauge
        Canvas gaugeCanvas = new Canvas(320, 180);
        drawScoreGauge(gaugeCanvas, 98);
        
        Label scoreSub = new Label("2/98 order Complains");
        scoreSub.setFont(Font.font("Segoe UI", 12));
        scoreSub.setTextFill(Color.web("#6c757d"));
        scoreSub.setAlignment(Pos.CENTER);
        
        scoreCard.getChildren().addAll(scoreHeader, gaugeCanvas, scoreSub);

        // Complaint cards (only showing unresolved complaints)
        VBox complaintCard1 = createComplaintCard("🔴", "Wrong Menu", "Andrew Tate", "Solve");

        rightCol.getChildren().addAll(scoreCard, complaintCard1);

        mainRow.getChildren().addAll(leftCol, rightCol);

        // Build loader to populate charts and lists
        Runnable load = () -> {
            List<Receipt> receipts = TextDatabase.loadAllReceipts();
            List<ItemRecord> items = TextDatabase.loadAllItems();

            // Populate metric cards - update the value labels
            double totalAll = SalesAnalytics.getTotalSales(receipts);
            double today = SalesAnalytics.getTotalSalesForDate(receipts, LocalDate.now());
            long ordersToday = SalesAnalytics.getOrderCountForDate(receipts, LocalDate.now());
            
            updateMetricCardValue(cardTotal, String.format("₱%.2f", totalAll));
            updateMetricCardValue(cardOnProgress, String.valueOf(ordersToday));
            updateMetricCardValue(cardToday, String.valueOf(ordersToday));

            // Update the sales chart using actual receipts/orders aggregation
            // Use the current granularity (set when filters are clicked)
            updateSalesChart(salesChart, salesGranularity);

            // Recent transactions
            recentTable.getItems().clear();
            int max = Math.min(4, receipts.size());
            for (int i = 0; i < max; i++) recentTable.getItems().add(receipts.get(i));
        };

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

        lowStockLabel = new Label();
        lowStockLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        lowStockLabel.setTextFill(Color.web("#111827"));

        // Create modern stat cards
        VBox salesCard = createModernStatCard("💵", "NET SALES (MONTH)", netSalesLabel, "#10B981");
        VBox pendingCard = createModernStatCard("🕐", "PENDING ORDERS", pendingOrdersLabel, "#F59E0B");
        VBox completedCard = createModernStatCard("✓", "COMPLETED ORDERS", completedOrdersLabel, "#3B82F6");
        VBox alertCard = createModernStatCard("⚠", "LOW STOCK ALERTS", lowStockLabel, "#EF4444");

        statsRow.getChildren().addAll(salesCard, pendingCard, completedCard, alertCard);

        // Refill Alerts Section
        VBox alertsSection = new VBox(15);
        alertsSection.setPadding(new Insets(25));
        alertsSection.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        
        HBox alertsHeader = new HBox();
        alertsHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label alertsTitle = new Label("⚠ Refill Alerts");
        alertsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        alertsTitle.setTextFill(Color.web("#111827"));
        
        Region alertsSpacer = new Region();
        HBox.setHgrow(alertsSpacer, Priority.ALWAYS);
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: 600;");
        refreshBtn.setOnMouseEntered(e -> refreshBtn.setStyle("-fx-background-color: #F9FAFB; -fx-text-fill: #6B7280; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: 600;"));
        refreshBtn.setOnMouseExited(e -> refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: 600;"));
        refreshBtn.setOnAction(e -> refreshDashboard());
        
        alertsHeader.getChildren().addAll(alertsTitle, alertsSpacer, refreshBtn);

        dashboardAlertsArea = new TextArea();
        dashboardAlertsArea.setEditable(false);
        dashboardAlertsArea.setPrefHeight(150);
        dashboardAlertsArea.setWrapText(true);
        dashboardAlertsArea.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-control-inner-background: #F9FAFB; -fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-background-radius: 8;");

        alertsSection.getChildren().addAll(alertsHeader, dashboardAlertsArea);

        panel.getChildren().addAll(header, statsRow, alertsSection);
        // Initial data load
        updateDashboardData();

        return panel;
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
     * Export Sales Dashboard data to PDF file
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
            
            // Get recent transactions
            List<Receipt> recentReceipts = receipts.size() > 10 ? receipts.subList(0, 10) : receipts;
            
            // Get items performance data
            List<OrderRecord> orders = TextDatabase.loadAllOrders();
            Map<String, Integer> itemCounts = new LinkedHashMap<>();
            for (OrderRecord order : orders) {
                String itemName = order.getItemName();
                itemCounts.put(itemName, itemCounts.getOrDefault(itemName, 0) + 1);
            }
            
            // Create PDF using basic text format (simplified PDF)
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // PDF Header
                writer.println("%PDF-1.4");
                writer.println("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj");
                writer.println("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj");
                writer.println("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj");
                writer.println("5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj");
                
                // Build content
                StringBuilder content = new StringBuilder();
                content.append("BT\n");
                content.append("/F1 24 Tf\n");
                content.append("50 750 Td\n");
                content.append("(BREWISE COFFEE SHOP - Sales Dashboard Report) Tj\n");
                
                content.append("/F1 12 Tf\n");
                content.append("0 -30 Td\n");
                content.append("(Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ") Tj\n");
                
                content.append("0 -40 Td\n");
                content.append("/F1 16 Tf\n");
                content.append("(=== SALES SUMMARY ===) Tj\n");
                
                content.append("/F1 12 Tf\n");
                content.append("0 -25 Td\n");
                content.append("(Total Revenue: P" + String.format("%.2f", totalRevenue) + ") Tj\n");
                
                content.append("0 -20 Td\n");
                content.append("(Today's Revenue: P" + String.format("%.2f", todayRevenue) + ") Tj\n");
                
                content.append("0 -20 Td\n");
                content.append("(Total Orders: " + totalOrders + ") Tj\n");
                
                content.append("0 -20 Td\n");
                content.append("(Today's Orders: " + ordersToday + ") Tj\n");
                
                content.append("0 -40 Td\n");
                content.append("/F1 16 Tf\n");
                content.append("(=== TOP SELLING ITEMS ===) Tj\n");
                
                content.append("/F1 12 Tf\n");
                int itemCount = 0;
                for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                    if (itemCount >= 10) break;
                    content.append("0 -20 Td\n");
                    String itemLine = entry.getKey().replaceAll("[()]", "") + " - " + entry.getValue() + " sold";
                    content.append("(" + itemLine + ") Tj\n");
                    itemCount++;
                }
                
                content.append("0 -40 Td\n");
                content.append("/F1 16 Tf\n");
                content.append("(=== RECENT TRANSACTIONS ===) Tj\n");
                
                content.append("/F1 10 Tf\n");
                for (Receipt receipt : recentReceipts) {
                    content.append("0 -18 Td\n");
                    String receiptLine = receipt.getUserName().replaceAll("[()]", "") + " | P" + String.format("%.2f", receipt.getTotalAmount()) + " | " + receipt.getReceiptTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
                    content.append("(" + receiptLine + ") Tj\n");
                }
                
                content.append("ET\n");
                
                String contentStr = content.toString();
                writer.println("4 0 obj << /Length " + contentStr.length() + " >> stream");
                writer.print(contentStr);
                writer.println("endstream endobj");
                
                writer.println("xref");
                writer.println("0 6");
                writer.println("0000000000 65535 f");
                writer.println("0000000009 00000 n");
                writer.println("0000000058 00000 n");
                writer.println("0000000115 00000 n");
                writer.println("0000000270 00000 n");
                writer.println("0000000350 00000 n");
                writer.println("trailer << /Size 6 /Root 1 0 R >>");
                writer.println("startxref");
                writer.println("450");
                writer.println("%%EOF");
            }
            
            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText(null);
            alert.setContentText("Sales Dashboard report has been exported to:\n" + file.getAbsolutePath());
            alert.showAndWait();
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failed");
            alert.setHeaderText(null);
            alert.setContentText("Failed to export PDF: " + e.getMessage());
            alert.showAndWait();
        }
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
        // Low-stock counter/alerts removed per configuration: keep UI neutral
        lowStockLabel.setText("—");

        // Dashboard alerts: no refill/low-stock alerts shown here anymore.
        dashboardAlertsArea.setText("✓ All products are well-stocked!");
        dashboardAlertsArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: #2E7D32;");
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

        Button undoButton = new Button("↶ Undo");
        styleSecondaryButton(undoButton);
        undoButton.setOnAction(e -> undoLastAction());

        Button refreshButton = new Button("🔄 Refresh");
        styleSecondaryButton(refreshButton);
        refreshButton.setOnAction(e -> refreshData());

        controls.getChildren().addAll(refillIngredientButton, deductQuantityButton, deleteIngredientButton, addIngredientButton, undoButton, refreshButton);

        panel.getChildren().addAll(title, inventoryTable, controls);
        return panel;
    }

    private VBox createRefillAlertsTab() {
        VBox panel = new VBox(25);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color: #F3F4F6;");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Refill Alerts");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#111827"));
        
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
        
        header.getChildren().addAll(title, spacer, dateBox);

        // Filter buttons
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        
        Button allBtn = new Button("All Ingredients");
        allBtn.setStyle("-fx-background-color: #1F2937; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        
        Button lowStockBtn = new Button("⚠ Low Stock");
        lowStockBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        
        filterBox.getChildren().addAll(allBtn, lowStockBtn);

        // Alerts content (selectable list)
        VBox alertsSection = new VBox(15);
        alertsSection.setPadding(new Insets(25));
        alertsSection.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        alertsListView = new javafx.scene.control.ListView<>();
        alertsListView.setPrefHeight(500);
        alertsListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        alertsListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.coffeeshop.model.InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String txt = String.format("%s — %.2f %s", item.getName(), item.getQuantity(), item.getUnit());
                    setText(txt);
                    // Visual cue only for out-of-stock items (low-stock notifications removed)
                    if (item.getQuantity() == 0) {
                        setStyle("-fx-background-color: rgba(254,226,226,0.6);");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button refillSelectedBtn = new Button("🔄 Refill Selected");
        refillSelectedBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");

        // 'Add Ingredient' removed as requested (no Add button)

        buttonBox.getChildren().addAll(refillSelectedBtn);

        alertsSection.getChildren().addAll(alertsListView, buttonBox);

        panel.getChildren().addAll(header, filterBox, alertsSection);
        
        // Wire up refresh
        allBtn.setOnAction(e -> refreshRefillAlerts(false));
        lowStockBtn.setOnAction(e -> refreshRefillAlerts(true));

        // Refill selected action: prompt for amount and confirm before applying
        refillSelectedBtn.setOnAction(e -> {
            java.util.List<com.coffeeshop.model.InventoryItem> sel = new java.util.ArrayList<>(alertsListView.getSelectionModel().getSelectedItems());
            if (sel.isEmpty()) {
                showAlert("No Selection", "Please select one or more ingredients to refill.", Alert.AlertType.WARNING);
                return;
            }

            TextInputDialog amountDlg = new TextInputDialog();
            amountDlg.setTitle("Refill Amount");
            amountDlg.setHeaderText("Enter amount to add per selected ingredient");
            amountDlg.setContentText("Amount (numeric). Leave blank to top up to " + Store.MAX_STOCK + ":");

            java.util.Optional<String> result = amountDlg.showAndWait();
            if (!result.isPresent()) return; // user cancelled
            String txt = result.get().trim();
            boolean topUp = txt.isEmpty();
            double fixedAmount = 0;
            if (!topUp) {
                try {
                    fixedAmount = Double.parseDouble(txt);
                    if (fixedAmount <= 0) { showAlert("Invalid", "Amount must be a positive number.", Alert.AlertType.ERROR); return; }
                } catch (NumberFormatException ex) {
                    showAlert("Invalid", "Please enter a valid numeric amount or leave blank to top up.", Alert.AlertType.ERROR);
                    return;
                }
            }

            // Build confirmation content
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-30s %-12s %-12s %-12s\n", "Ingredient", "Current", "To Add", "After"));
            sb.append("---------------------------------------------------------------\n");
            java.util.Map<String, Double> toAddMap = new java.util.HashMap<>();
            for (com.coffeeshop.model.InventoryItem it : sel) {
                double add;
                if (topUp) {
                    add = Math.max(0, Store.MAX_STOCK - it.getQuantity());
                } else {
                    add = fixedAmount;
                }
                toAddMap.put(it.getName(), add);
                double after = it.getQuantity() + add;
                sb.append(String.format("%-30s %-12.2f %-12.2f %-12.2f\n", it.getName(), it.getQuantity(), add, after));
            }

            boolean nothingToDo = true;
            for (double v : toAddMap.values()) if (v > 0) { nothingToDo = false; break; }
            if (nothingToDo) {
                showAlert("Nothing to Refill", "All selected items are already at or above the target level.", Alert.AlertType.INFORMATION);
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Refill");
            confirm.setHeaderText("Please confirm the refill amounts for the selected ingredients:");

            // Build an editable grid: Ingredient | Current | To Add (editable) | After | Delete
            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(8);
            grid.setPadding(new Insets(8));

            // Header row
            grid.add(new Label("Ingredient"), 0, 0);
            grid.add(new Label("Current"), 1, 0);
            grid.add(new Label("To Add"), 2, 0);
            grid.add(new Label("After"), 3, 0);
            grid.add(new Label(""), 4, 0);

            java.util.Map<String, TextField> inputFields = new java.util.HashMap<>();
            java.util.Map<String, Label> afterLabels = new java.util.HashMap<>();
            java.util.List<com.coffeeshop.model.InventoryItem> workingList = new java.util.ArrayList<>(sel);

            int row = 1;
            for (com.coffeeshop.model.InventoryItem it : workingList) {
                double defaultAdd = toAddMap.getOrDefault(it.getName(), 0.0);

                Label nameLbl = new Label(it.getName());
                Label curLbl = new Label(String.format("%.2f %s", it.getQuantity(), it.getUnit()));
                TextField addField = new TextField(String.format("%.2f", defaultAdd));
                addField.setPrefWidth(100);
                Label afterLbl = new Label(String.format("%.2f %s", it.getQuantity() + defaultAdd, it.getUnit()));
                Button delBtn = new Button("Delete");

                grid.add(nameLbl, 0, row);
                grid.add(curLbl, 1, row);
                grid.add(addField, 2, row);
                grid.add(afterLbl, 3, row);
                grid.add(delBtn, 4, row);

                inputFields.put(it.getName(), addField);
                afterLabels.put(it.getName(), afterLbl);

                // Update 'after' when user edits the addField
                addField.textProperty().addListener((obs, o, n) -> {
                    double val = 0;
                    try { if (n != null && !n.trim().isEmpty()) val = Double.parseDouble(n.trim()); } catch (Exception ex) { val = 0; }
                    double clamped = Math.max(0, Math.min(val, Store.MAX_STOCK - it.getQuantity()));
                    afterLbl.setText(String.format("%.2f %s", it.getQuantity() + clamped, it.getUnit()));
                });

                // Delete row handler
                delBtn.setOnAction(ev -> {
                    // hide nodes for this row
                    nameLbl.setVisible(false); curLbl.setVisible(false); addField.setVisible(false); afterLbl.setVisible(false); delBtn.setVisible(false);
                    inputFields.remove(it.getName());
                    afterLabels.remove(it.getName());
                    toAddMap.remove(it.getName());
                });

                row++;
            }

            VBox content = new VBox(8, grid);
            content.setPadding(new Insets(6));

            DialogPane pane = confirm.getDialogPane();
            pane.setContent(content);
            pane.setPrefWidth(760);
            pane.setPrefHeight(Math.min(900, 160 + sel.size() * 36));
            pane.setMinHeight(Region.USE_PREF_SIZE);
            confirm.setResizable(true);

            java.util.Optional<javafx.scene.control.ButtonType> conf = confirm.showAndWait();
            if (conf.isPresent() && conf.get() == ButtonType.OK) {
                // validate and apply per-field values
                for (com.coffeeshop.model.InventoryItem it : sel) {
                    TextField tf = inputFields.get(it.getName());
                    if (tf == null) continue; // deleted or removed
                    double add = 0;
                    String s = tf.getText() == null ? "" : tf.getText().trim();
                    if (s.isEmpty()) continue;
                    try { add = Double.parseDouble(s); } catch (NumberFormatException ex) { add = 0; }
                    if (add <= 0) continue;
                    // clamp to not exceed MAX_STOCK
                    double maxAdd = Math.max(0, Store.MAX_STOCK - it.getQuantity());
                    double toApply = Math.min(add, maxAdd);
                    if (toApply > 0) store.refillInventory(it.getName(), toApply);
                }
                showAlert("Refilled", "Selected ingredients were refilled.", Alert.AlertType.INFORMATION);
                refreshData();
            }
        });

        // Start realtime clock update for header time
        if (adminTimeLabel == null) adminTimeLabel = timeLabel;
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            try {
                adminTimeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
            } catch (Exception ignored) {}
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
        
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
            String status;
            if (p.getStock() == 0) {
                status = "🔴 OUT OF STOCK";
            } else {
                status = "✓ OK";
            }
            productTable.getItems().add(new ProductRow(p.getId(), p.getName(), p.getPrice(), status));
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

        // Refresh alerts (show all by default)
        refreshRefillAlerts(false);
        // Refresh archived items list if tab exists
        try { refreshArchivedItems(); } catch (Exception ignored) {}
    }

    private void refreshRefillAlerts(boolean lowOnly) {
        java.util.Collection<com.coffeeshop.model.InventoryItem> inv = store.getInventory().values();
        java.util.List<com.coffeeshop.model.InventoryItem> items = new java.util.ArrayList<>(inv);
        if (lowOnly) {
            // Show only out-of-stock ingredients when filtering for 'Low Stock'
            java.util.List<com.coffeeshop.model.InventoryItem> low = new java.util.ArrayList<>();
            for (com.coffeeshop.model.InventoryItem it : items) {
                if (it.getQuantity() == 0) low.add(it);
            }
            alertsListView.getItems().setAll(low);
            if (low.isEmpty()) {
                alertsListView.getItems().clear();
            }
        } else {
            // Show all ingredients (no low-stock prioritization)
            alertsListView.getItems().setAll(items);
        }
    }

    // Refill product UI removed — use inventory management workflow instead.

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

        grid.add(idLabel, 0, 0);
        grid.add(idValueLabel, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);

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
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryBox, 1, 3);

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

        grid.add(imageSection, 0, 4, 2, 1);

        // Ingredients section (limited by selected category)
        Label ingredientLabel = new Label("🧪 Select Ingredients:");
        ingredientLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        grid.add(ingredientLabel, 0, 5, 2, 1);

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

        // initial build and rebuild on category change
        rebuild.run();
        categoryBox.valueProperty().addListener((obs, o, n) -> rebuild.run());

        ingredientList.setMaxWidth(Double.MAX_VALUE);
        ingredientSection.getChildren().add(ingredientList);
        grid.add(ingredientSection, 0, 6, 2, 1);

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

                    return new Product(id, name, price, 20, cleaned, category);
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

        dialog.showAndWait().ifPresent(product -> {
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

        // Price (editable)
        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        priceField.setPromptText("Enter price");

        // Stock (editable - for adding stock)
        grid.add(idLabel, 0, 0);
        grid.add(idValueLabel, 1, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);

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

        grid.add(imageSection, 0, 4, 2, 1);

        // Ingredients editor: allow admin to edit which ingredients and quantities are associated with the product
        Label ingredientLabel = new Label("🧪 Edit Ingredients:");
        ingredientLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        grid.add(ingredientLabel, 0, 5, 2, 1);

        // Build ingredient editor limited by product category
        VBox ingredientSection = new VBox(10);
        ingredientSection.setPadding(new Insets(10));
        ingredientSection.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #fafafa;");
        // Ensure the ingredient section is tall enough to show multiple items
        ingredientSection.setPrefHeight(320);

        java.util.Map<String, Double> selectedIngredients = new java.util.HashMap<>();
        java.util.Map<String, Double> existingRecipe = new java.util.HashMap<>();
        try { if (product.getRecipe() != null) existingRecipe.putAll(product.getRecipe()); } catch (Exception ignored) {}

        // Choice row + scrollable ingredient list so admins can select or add ingredients easily
        HBox addChoiceRow = new HBox(8);
        addChoiceRow.setAlignment(Pos.CENTER_LEFT);
        ComboBox<String> ingredientChoiceBox = new ComboBox<>();
        ingredientChoiceBox.setPromptText("Select ingredient");
        ingredientChoiceBox.setPrefWidth(360);
        Button addChoiceBtn = new Button("Add");
        addChoiceBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
        addChoiceRow.getChildren().addAll(ingredientChoiceBox, addChoiceBtn);

        VBox ingredientList = new VBox(8);
        ingredientList.setFillWidth(true);
        VBox.setVgrow(ingredientList, Priority.ALWAYS);
        ScrollPane ingredientScroll = new ScrollPane(ingredientList);
        ingredientScroll.setFitToWidth(true);
        ingredientScroll.setPrefHeight(260);

        final java.util.List<String> baseIngredients = new java.util.ArrayList<>(store.getInventory().keySet());
        // fallback defaults when inventory empty
        if (baseIngredients.isEmpty()) {
            baseIngredients.addAll(java.util.Arrays.asList(
                "Coffee Beans", "Milk", "Water", "Matcha Powder", "Chocolate Syrup",
                "Vanilla Syrup", "Caramel Syrup", "Ice", "Mango Puree", "Strawberry Puree",
                "Tea Leaves", "Flour", "Butter", "Sugar", "Egg"
            ));
        }
        // ensure existing recipe ingredients are present
        try { for (String k : existingRecipe.keySet()) if (k != null && !k.trim().isEmpty() && !baseIngredients.contains(k)) baseIngredients.add(k); } catch (Exception ignored) {}

        Runnable rebuildEdit = () -> {
            ingredientList.getChildren().clear();
            selectedIngredients.clear();
            java.util.List<String> choices = filterIngredientsByCategory(baseIngredients, product.getCategory());
            if (choices == null) choices = new java.util.ArrayList<>(baseIngredients);
            // populate combo box choices
            ingredientChoiceBox.getItems().setAll(choices);
            if (!ingredientChoiceBox.getItems().isEmpty() && ingredientChoiceBox.getValue() == null) ingredientChoiceBox.setValue(ingredientChoiceBox.getItems().get(0));

            if (choices.isEmpty()) {
                Label hint = new Label("No inventory items available.");
                hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
                ingredientList.getChildren().add(hint);
            }

            for (String ingredientName : choices) {
                HBox ingredientRow = new HBox(10);
                ingredientRow.setAlignment(Pos.CENTER_LEFT);

                CheckBox checkBox = new CheckBox(ingredientName);
                checkBox.setStyle("-fx-font-size: 11;");

                TextField quantityField = new TextField();
                quantityField.setPromptText("Qty (e.g. 100.0)");
                quantityField.setPrefWidth(100);
                quantityField.setDisable(true);

                if (existingRecipe.containsKey(ingredientName)) {
                    double q = existingRecipe.getOrDefault(ingredientName, 0.0);
                    checkBox.setSelected(true);
                    quantityField.setText(String.valueOf(q));
                    quantityField.setDisable(false);
                    selectedIngredients.put(ingredientName, q);
                }

                checkBox.setOnAction(e -> {
                    if (checkBox.isSelected()) {
                        quantityField.setDisable(false);
                        try { double v = Double.parseDouble(quantityField.getText().isEmpty() ? "0" : quantityField.getText()); selectedIngredients.put(ingredientName, v); } catch (NumberFormatException ex) { selectedIngredients.put(ingredientName, 0.0); }
                    } else {
                        quantityField.setDisable(true);
                        selectedIngredients.remove(ingredientName);
                    }
                });

                quantityField.setOnKeyReleased(e -> {
                    try {
                        if (!quantityField.getText().isEmpty() && !quantityField.isDisable()) {
                            double qty = Double.parseDouble(quantityField.getText());
                            selectedIngredients.put(ingredientName, qty);
                        }
                    } catch (NumberFormatException ignored) {}
                });

                ingredientRow.getChildren().addAll(checkBox, quantityField);
                ingredientList.getChildren().add(ingredientRow);
            }
        };

        // initial build
        rebuildEdit.run();

        ingredientList.setMaxWidth(Double.MAX_VALUE);

        // add-button behavior: select existing or prepend a new ingredient row
        addChoiceBtn.setOnAction(evt -> {
            String choice = ingredientChoiceBox.getValue();
            if (choice == null || choice.trim().isEmpty()) return;
            for (javafx.scene.Node node : ingredientList.getChildren()) {
                if (!(node instanceof HBox)) continue;
                HBox row = (HBox) node;
                if (row.getChildren().isEmpty()) continue;
                javafx.scene.Node n0 = row.getChildren().get(0);
                javafx.scene.Node n1 = row.getChildren().get(1);
                if (n0 instanceof CheckBox && ((CheckBox)n0).getText().equals(choice) && n1 instanceof TextField) {
                    CheckBox cb = (CheckBox) n0; TextField tf = (TextField) n1;
                    cb.setSelected(true); tf.setDisable(false); if (tf.getText().isEmpty()) tf.setText("1.0");
                    try { selectedIngredients.put(choice, Double.parseDouble(tf.getText())); } catch (Exception ignored) {}
                    return;
                }
            }
            // not found -> add new row
            HBox ingredientRow = new HBox(10);
            ingredientRow.setAlignment(Pos.CENTER_LEFT);
            CheckBox checkBox = new CheckBox(choice); checkBox.setStyle("-fx-font-size: 11;"); checkBox.setSelected(true);
            TextField quantityField = new TextField("1.0"); quantityField.setPromptText("Qty (e.g. 100.0)"); quantityField.setPrefWidth(100); quantityField.setDisable(false);
            checkBox.setOnAction(e2 -> { if (!checkBox.isSelected()) { quantityField.setDisable(true); selectedIngredients.remove(choice); } else { quantityField.setDisable(false); try { selectedIngredients.put(choice, Double.parseDouble(quantityField.getText())); } catch (Exception ignored) {} } });
            quantityField.setOnKeyReleased(e2 -> { try { if (!quantityField.getText().isEmpty()) selectedIngredients.put(choice, Double.parseDouble(quantityField.getText())); } catch (Exception ignored) {} });
            ingredientRow.getChildren().addAll(checkBox, quantityField);
            ingredientList.getChildren().add(0, ingredientRow);
            selectedIngredients.put(choice, 1.0);
        });

        ingredientSection.getChildren().addAll(addChoiceRow, ingredientScroll);
        grid.add(ingredientSection, 0, 6, 2, 1);

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
                    
                    double newPrice = Double.parseDouble(priceField.getText());
                    // Update price
                    product.setPrice(newPrice);

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

        dialog.showAndWait().ifPresent(updatedProduct -> {
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

    public static void main(String[] args) {
        launch(args);
    }
}
