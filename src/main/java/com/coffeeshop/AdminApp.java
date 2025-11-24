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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private VBox dashboardContent, productsContent, inventoryContent, categoriesContent, accountsContent, refillAlertsContent, reportsContent;
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
        categoriesContent = createCategoriesTab();
        accountsContent = createAccountsTab();
        refillAlertsContent = createRefillAlertsTab();
        archivedContent = createArchivedTab();
        reportsContent = createReportsTab();
        
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
        
        VBox businessSection = new VBox(0);
        businessSection.getChildren().addAll(businessHeader, dashboardBtn, reportsBtn);
        
        // MANAGEMENT section
        Label managementHeader = new Label("MANAGEMENT");
        managementHeader.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
        managementHeader.setTextFill(Color.web("#6B7280"));
        managementHeader.setPadding(new Insets(5, 20, 5, 20));
        
        Button productsBtn = createNavButton("📦", "Products", false);
        Button inventoryBtn = createNavButton("📋", "Inventory", false);
        Button categoriesBtn = createNavButton("🗂️", "Categories", false);
        Button accountsBtn = createNavButton("👥", "Accounts", false);
        
        VBox managementSection = new VBox(0);
        managementSection.getChildren().addAll(managementHeader, productsBtn, inventoryBtn, categoriesBtn, accountsBtn);
        
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
        productsBtn.setOnAction(e -> { setActiveNav(productsBtn); showContent(productsContent); });
        inventoryBtn.setOnAction(e -> { setActiveNav(inventoryBtn); showContent(inventoryContent); });
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
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(20));

        Label title = new Label("📈 Sales Reports & Analytics");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        // Metrics
        Label totalSales = new Label();
        totalSales.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        Label todaySales = new Label();
        todaySales.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        Label ordersToday = new Label();
        ordersToday.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        VBox metricsBox = new VBox(8);
        metricsBox.setPadding(new Insets(10));
        metricsBox.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-background-radius: 8;");
        metricsBox.getChildren().addAll(new Label("Total Sales (All time):"), totalSales,
                                        new Label("Sales Today:"), todaySales,
                                        new Label("Orders Today:"), ordersToday);

        // Top products bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> topChart = new BarChart<>(xAxis, yAxis);
        topChart.setTitle("Top Products (by units sold)");
        xAxis.setLabel("Product");
        yAxis.setLabel("Units Sold");
        topChart.setLegendVisible(false);
        topChart.setPrefHeight(300);

        // Sales trend line chart (last 7 days)
        CategoryAxis trendX = new CategoryAxis();
        NumberAxis trendY = new NumberAxis();
        LineChart<String, Number> trendChart = new LineChart<>(trendX, trendY);
        trendChart.setTitle("Sales Trend (Last 7 days)");
        trendX.setLabel("Date");
        trendY.setLabel("Sales (₱)");
        trendChart.setPrefHeight(300);

        Button refreshBtn = new Button("Refresh Reports");
        refreshBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold;");

        // Loader function
        Runnable load = () -> {
            List<Receipt> receipts = TextDatabase.loadAllReceipts();
            List<ItemRecord> items = TextDatabase.loadAllItems();

            double total = SalesAnalytics.getTotalSales(receipts);
            double today = SalesAnalytics.getTotalSalesForDate(receipts, LocalDate.now());
            long orders = SalesAnalytics.getOrderCountForDate(receipts, LocalDate.now());

            totalSales.setText(String.format("₱%.2f", total));
            todaySales.setText(String.format("₱%.2f", today));
            ordersToday.setText(String.valueOf(orders));

            // Top products
            topChart.getData().clear();
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            List<Map.Entry<String,Integer>> top = SalesAnalytics.getTopProducts(items, 8);
            for (Map.Entry<String,Integer> e : top) {
                s.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            }
            topChart.getData().add(s);

            // Trend - last 7 days
            trendChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = 6; i >= 0; i--) {
                LocalDate d = LocalDate.now().minusDays(i);
                double salesForDay = SalesAnalytics.getTotalSalesForDate(receipts, d);
                series.getData().add(new XYChart.Data<>(d.format(fmt), salesForDay));
            }
            trendChart.getData().add(series);
        };

        refreshBtn.setOnAction(e -> load.run());

        // initial load
        load.run();

        panel.getChildren().addAll(title, new Separator(), metricsBox, new Separator(), topChart, trendChart, refreshBtn);
        return panel;
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

        // Low stock products
        long lowStockCount = store.getProducts().stream()
            .filter(p -> p.getStock() <= Store.REFILL_THRESHOLD)
            .count();

        // Update labels
        netSalesLabel.setText(String.format("₱%.2f", netSales));
        pendingOrdersLabel.setText(String.valueOf(pendingCount));
        completedOrdersLabel.setText(String.valueOf(completedCount));
        lowStockLabel.setText(String.valueOf(lowStockCount));

        // Update alerts area
        if (store.hasProductsNeedingRefill()) {
            dashboardAlertsArea.setText(store.getProductRefillAlerts());
            dashboardAlertsArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;"); 
        } else {
            dashboardAlertsArea.setText("✓ All products are well-stocked!");
            dashboardAlertsArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: #2E7D32;");
        }
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
                    // Simple low-stock visual cue
                    if (item.getQuantity() <= Store.REFILL_THRESHOLD) {
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
            } else if (p.getStock() <= 5) {
                status = "🟡 LOW STOCK";
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
            java.util.List<com.coffeeshop.model.InventoryItem> low = new java.util.ArrayList<>();
            for (com.coffeeshop.model.InventoryItem it : items) {
                if (it.getQuantity() <= Store.REFILL_THRESHOLD) low.add(it);
            }
            alertsListView.getItems().setAll(low);
            if (low.isEmpty()) {
                alertsListView.getItems().clear();
            }
        } else {
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
