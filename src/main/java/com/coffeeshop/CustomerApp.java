package com.coffeeshop;

import com.coffeeshop.model.*;
import com.coffeeshop.service.Store;
import com.coffeeshop.service.TextDatabase;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
// removed TranslateTransition (overlay transitions reverted)
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
// Removed javafx.animation Timeline/KeyFrame usage; using ScheduledExecutorService

import java.util.UUID;

public class CustomerApp extends Application {
    private Store store;
    private Order currentOrder;
    private OrderItem customizingOrderItem = null; // Track item being customized from cart
    private Stage primaryStage;
    private HBox footerContainer;
    private Label footerItemsLabel;
    private Label footerTotalLabel;
    private Button footerViewCartBtn;
    private Button footerCheckoutBtn;
    private String orderType = ""; // "Dine In" or "Take Out"
    private Timeline inactivityTimer;
    private int countdownSeconds = 30;
    private Label countdownLabel;


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        store = Store.getInstance();
        
        primaryStage.setTitle("Brewise Coffee Shop - Kiosk");
        
        // Show welcome screen first
        showWelcomeScreen();
    }

    private void showWelcomeScreen() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #FFFFFF;"); // Modern White Background

        VBox welcomeBox = new VBox(40);
        welcomeBox.setAlignment(Pos.CENTER);
        welcomeBox.setPadding(new Insets(60));
        welcomeBox.setMaxWidth(800);
        welcomeBox.setMaxHeight(600);
        // Glassmorphism-like effect or clean card
        welcomeBox.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 20, 0, 0, 10);");

        // Welcome text
        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);
        
        Label welcomeTitle = new Label("Welcome to Brewise");
        welcomeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        welcomeTitle.setTextFill(Color.web("#1A1A1A"));

        Label welcomeSubtitle = new Label("Freshly brewed coffee, just for you.");
        welcomeSubtitle.setFont(Font.font("Segoe UI", 18));
        welcomeSubtitle.setTextFill(Color.web("#795548"));
        
        titleBox.getChildren().addAll(welcomeTitle, welcomeSubtitle);

        // Options Container
        HBox optionsBox = new HBox(30);
        optionsBox.setAlignment(Pos.CENTER);

        // Dine In button (load custom image resource if available)
        Node dineIconNode;
        try {
            java.io.InputStream is = CustomerApp.class.getResourceAsStream("/images/dine_in.png");
            if (is != null) {
                Image dineImg = new Image(is);
                ImageView dineView = new ImageView(dineImg);
                dineView.setFitWidth(96);
                dineView.setFitHeight(96);
                dineView.setPreserveRatio(true);
                dineIconNode = dineView;
            } else {
                Label iconLabel = new Label("🍽");
                iconLabel.setFont(Font.font("Segoe UI Emoji", 64));
                dineIconNode = iconLabel;
            }
        } catch (Exception ex) {
            Label iconLabel = new Label("🍽");
            iconLabel.setFont(Font.font("Segoe UI Emoji", 64));
            dineIconNode = iconLabel;
        }

        VBox dineInBox = createOptionCard(dineIconNode, "Dine In", "Enjoy your coffee in our cozy space");
        dineInBox.setOnMouseClicked(e -> {
            orderType = "Dine In";
            showMenuScreen();
        });

        // Take Out button
        Label takeOutIcon = new Label("🛍");
        takeOutIcon.setFont(Font.font("Segoe UI Emoji", 64));
        VBox takeOutBox = createOptionCard(takeOutIcon, "Take Out", "Grab your coffee on the go");
        takeOutBox.setOnMouseClicked(e -> {
            orderType = "Take Out";
            showMenuScreen();
        });

        optionsBox.getChildren().addAll(dineInBox, takeOutBox);

        welcomeBox.getChildren().addAll(titleBox, optionsBox);
        root.getChildren().add(welcomeBox);

        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createOptionCard(Node iconNode, String title, String desc) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 30, 40, 30));
        card.setPrefWidth(320);
        card.setPrefHeight(320);
        card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 16; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 16; -fx-cursor: hand;");
        
        // iconNode can be an ImageView or a Label (emoji)
        if (iconNode instanceof Label) {
            ((Label) iconNode).setFont(Font.font("Segoe UI Emoji", 64));
        }
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 22));
        titleLabel.setTextFill(Color.web("#1A1A1A"));
        
        Label descLabel = new Label(desc);
        descLabel.setFont(Font.font("Segoe UI", 14));
        descLabel.setTextFill(Color.web("#666666"));
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setWrapText(true);
        
        card.getChildren().addAll(iconNode, titleLabel, descLabel);
        
        // Hover animation
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-border-color: #2C2C2C; -fx-border-width: 2; -fx-border-radius: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(44, 44, 44, 0.15), 12, 0, 0, 3);");
            titleLabel.setTextFill(Color.web("#2C2C2C"));
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 8, 0, 0, 2);");
            titleLabel.setTextFill(Color.web("#1A1A1A"));
        });
        
        return card;
    }

    private String selectedCategory = "Beverages"; // Default category
    private boolean sidebarVisible = true; // Sidebar visibility state
    private VBox categorySidebarContainer; // Reference to sidebar for hiding/showing
    
    private void showMenuScreen() {
        if (currentOrder == null) {
            currentOrder = new Order(UUID.randomUUID().toString().substring(0, 8));
        }
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: #FFFFFF;");

        // Header
        VBox header = createHeader();
        root.setTop(header);

        // Main content with sidebar and product grid
        HBox mainContent = new HBox();
        mainContent.setStyle("-fx-background-color: #FFFFFF;");
        mainContent.setMinHeight(500); // Ensure main content has minimum height
        
        // Left sidebar - Categories (conditionally shown)
        if (sidebarVisible) {
            categorySidebarContainer = createCategorySidebar();
            mainContent.getChildren().add(categorySidebarContainer);
        }
        
        // Right - Product Menu (3-column grid)
        ScrollPane menuScroll = createProductMenu();
        HBox.setHgrow(menuScroll, Priority.ALWAYS);
        mainContent.getChildren().add(menuScroll);
        
        root.setCenter(mainContent);
        
        // Bottom - Footer with cart info
        footerContainer = createFooter();
        root.setBottom(footerContainer);

        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);

        // Update footer after scene is set
        updateFooter();

        // Start inactivity timer and reset on any interaction
        startInactivityTimer();
        scene.setOnMouseMoved(e -> resetInactivityTimer());
        scene.setOnMouseClicked(e -> resetInactivityTimer());
        scene.setOnKeyPressed(e -> resetInactivityTimer());
        scene.setOnTouchPressed(e -> resetInactivityTimer());
        scene.setOnScroll(e -> resetInactivityTimer());
    }

    private VBox createHeader() {
        BorderPane headerPane = new BorderPane();
        headerPane.setPadding(new Insets(20, 30, 20, 30));
        headerPane.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        // Left - Cancel button
        VBox leftBox = new VBox(10);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        
        Button cancelButton = new Button("✕ Cancel");
        cancelButton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        cancelButton.setTextFill(Color.web("#D32F2F"));
        cancelButton.setPadding(new Insets(10, 20, 10, 20));
        cancelButton.setStyle("-fx-background-color: transparent; -fx-border-color: #D32F2F; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: #FFEBEE; -fx-border-color: #D32F2F; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: transparent; -fx-border-color: #D32F2F; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand;"));
        
        cancelButton.setOnAction(e -> {
            // Clear current order and reset state, then return to welcome screen
            stopInactivityTimer();
            if (currentOrder != null) {
                currentOrder.getItems().clear();
                currentOrder = null;
            }
            orderType = "";
            showWelcomeScreen();
        });
        
        // Countdown label
        countdownLabel = new Label("⏱ " + countdownSeconds + "s");
        countdownLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        countdownLabel.setTextFill(Color.web("#757575"));
        
        // Do not display the countdown label in the header UI (timer still runs in background)
        countdownLabel.setVisible(false);
        countdownLabel.setManaged(false);
        leftBox.getChildren().add(cancelButton);

        // Center content
        VBox centerBox = new VBox(2);
        centerBox.setAlignment(Pos.CENTER);

        // Logo and title
        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER);
        Label logo = new Label("☕");
        logo.setFont(Font.font("Segoe UI Emoji", 24));
        Label brandName = new Label("BREWISE");
        brandName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        brandName.setTextFill(Color.web("#1A1A1A"));
        logoBox.getChildren().addAll(logo, brandName);

        Label subtitle = new Label("Premium Coffee Experience");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#8D6E63"));

        centerBox.getChildren().addAll(logoBox, subtitle);

        // Order type badge on right
        VBox rightBox = new VBox();
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        
        Label orderTypeBadge = new Label(orderType.toUpperCase());
        orderTypeBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        orderTypeBadge.setTextFill(Color.web("#E65100"));
        orderTypeBadge.setPadding(new Insets(8, 16, 8, 16));
        orderTypeBadge.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 20;");
        
        rightBox.getChildren().add(orderTypeBadge);

        headerPane.setLeft(leftBox);
        headerPane.setCenter(centerBox);
        headerPane.setRight(rightBox);

        VBox header = new VBox(headerPane);
        return header;
    }
    
    private HBox createFooter() {
        HBox footer = new HBox(20);
        footer.setPadding(new Insets(20, 30, 20, 30));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, -2);");
        
        // Left side - Total info
        VBox totalInfo = new VBox(2);
        totalInfo.setAlignment(Pos.CENTER_LEFT);
        
        footerItemsLabel = new Label(getItemsCountText());
        footerItemsLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        footerItemsLabel.setTextFill(Color.web("#795548"));
        
        footerTotalLabel = new Label(getTotalText());
        footerTotalLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 18));
        footerTotalLabel.setTextFill(Color.web("#1A1A1A"));
        
        totalInfo.getChildren().addAll(footerItemsLabel, footerTotalLabel);
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Right side - Action buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        footerViewCartBtn = new Button("View Cart");
        footerViewCartBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        footerViewCartBtn.setPadding(new Insets(12, 24, 12, 24));
        footerViewCartBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #2C2C2C; -fx-border-width: 2; -fx-border-radius: 8; -fx-text-fill: #2C2C2C; -fx-cursor: hand;");
        footerViewCartBtn.setOnMouseEntered(e -> footerViewCartBtn.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #2C2C2C; -fx-border-width: 2; -fx-border-radius: 8; -fx-text-fill: #1A1A1A; -fx-cursor: hand;"));
        footerViewCartBtn.setOnMouseExited(e -> footerViewCartBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #2C2C2C; -fx-border-width: 2; -fx-border-radius: 8; -fx-text-fill: #2C2C2C; -fx-cursor: hand;"));
        footerViewCartBtn.setOnAction(e -> showCartDialog());
        
        footerCheckoutBtn = new Button("Proceed to Checkout");
        footerCheckoutBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        footerCheckoutBtn.setPadding(new Insets(12, 24, 12, 24));
        footerCheckoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        footerCheckoutBtn.setOnMouseEntered(e -> footerCheckoutBtn.setStyle("-fx-background-color: #43A047; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
        footerCheckoutBtn.setOnMouseExited(e -> footerCheckoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
        footerCheckoutBtn.setOnAction(e -> {
            if (currentOrder.getItems().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Empty Cart");
                alert.setHeaderText(null);
                alert.setContentText("Your cart is empty. Add some items first!");
                alert.showAndWait();
            } else {
                showCheckoutPage();
            }
        });
        
        // Update button states
        updateFooterButtons();
        
        buttonBox.getChildren().addAll(footerViewCartBtn, footerCheckoutBtn);
        footer.getChildren().addAll(totalInfo, spacer, buttonBox);
        
        return footer;
    }
    
    private void updateFooter() {
        if (footerItemsLabel != null) {
            footerItemsLabel.setText(getItemsCountText());
        }
        if (footerTotalLabel != null) {
            footerTotalLabel.setText(getTotalText());
        }
        updateFooterButtons();
    }
    
    private void updateFooterButtons() {
        boolean hasItems = currentOrder != null && !currentOrder.getItems().isEmpty();
        
        if (footerViewCartBtn != null) {
            footerViewCartBtn.setDisable(!hasItems);
            if (hasItems) {
                footerViewCartBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #2C2C2C; -fx-border-width: 2; -fx-border-radius: 8; -fx-text-fill: #2C2C2C; -fx-cursor: hand;");
            } else {
                footerViewCartBtn.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #E0E0E0; -fx-border-width: 2; -fx-border-radius: 8; -fx-text-fill: #BDBDBD;");
            }
        }
        
        if (footerCheckoutBtn != null) {
            footerCheckoutBtn.setDisable(!hasItems);
            if (hasItems) {
                footerCheckoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
            } else {
                footerCheckoutBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #BDBDBD; -fx-background-radius: 8;");
            }
        }
    }
    
    private VBox createCategorySidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(280);
        sidebar.setMaxWidth(280);
        sidebar.setMinWidth(280);
        sidebar.setStyle("-fx-background-color: #FFFFFF; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 2, 0); -fx-border-color: #E0E0E0; -fx-border-width: 0 1 0 0;");
        
        // Sidebar header with back button - Clean white theme
        VBox header = new VBox(12);
        header.setPadding(new Insets(20, 20, 18, 20));
        header.setStyle("-fx-background-color: #F8F9FA; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");
        
        // Header row with back button and title
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Button backButton = new Button("←");
        backButton.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        backButton.setTextFill(Color.web("#1A1A1A"));
        backButton.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1A1A1A; -fx-background-radius: 18; -fx-min-width: 32; -fx-min-height: 32; -fx-cursor: hand; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 18;");
        backButton.setOnMouseEntered(e -> backButton.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #1A1A1A; -fx-background-radius: 18; -fx-min-width: 32; -fx-min-height: 32; -fx-cursor: hand; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-border-radius: 18;"));
        backButton.setOnMouseExited(e -> backButton.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1A1A1A; -fx-background-radius: 18; -fx-min-width: 32; -fx-min-height: 32; -fx-cursor: hand; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 18;"));
        backButton.setOnAction(e -> toggleSidebar());
        
        Label categoriesTitle = new Label("Categories");
        categoriesTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 18));
        categoriesTitle.setTextFill(Color.web("#1A1A1A"));
        
        headerRow.getChildren().addAll(backButton, categoriesTitle);
        
        // Subtitle below the title
        Label subtitle = new Label("Select a category");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        subtitle.setTextFill(Color.web("#666666"));
        subtitle.setPadding(new Insets(0, 0, 0, 44)); // Align with title
        
        header.getChildren().addAll(headerRow, subtitle);
        
        // Category buttons
        VBox categoryContainer = new VBox(2);
        categoryContainer.setPadding(new Insets(0));
        
        String[] categories = {"Beverages", "Espresso", "Specialty", "Cold Drinks", "Snacks", "Pastries"};
        String[] icons = {"☕", "☘", "✨", "🧊", "🍪", "🍰"};
        
        for (int i = 0; i < categories.length; i++) {
            Button categoryBtn = createCategoryButton(categories[i], icons[i]);
            categoryContainer.getChildren().add(categoryBtn);
        }
        
        sidebar.getChildren().addAll(header, categoryContainer);
        return sidebar;
    }
    
    private Button createCategoryButton(String category, String icon) {
        Button btn = new Button();
        btn.setPrefWidth(280);
        btn.setPrefHeight(48);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(0, 20, 0, 20));
        
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("Segoe UI Emoji", 16));
        iconLabel.setTextFill(Color.web("#1A1A1A")); // Dark text for white background
        
        Label textLabel = new Label(category);
        textLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        textLabel.setTextFill(Color.web("#1A1A1A")); // Dark text for white background
        
        content.getChildren().addAll(iconLabel, textLabel);
        btn.setGraphic(content);
        
        // Style based on selection
        updateCategoryButtonStyle(btn, category.equals(selectedCategory));
        
        btn.setOnAction(e -> {
            selectedCategory = category;
            refreshProductGrid();
        });
        
        return btn;
    }
    
    private void updateCategoryButtonStyle(Button btn, boolean isSelected) {
        HBox content = (HBox) btn.getGraphic();
        Label iconLabel = (Label) content.getChildren().get(0);
        Label textLabel = (Label) content.getChildren().get(1);
        
        if (isSelected) {
            btn.setStyle("-fx-background-color: #2C2C2C; -fx-background-radius: 8; -fx-border-width: 0;");
            iconLabel.setTextFill(Color.web("#FFFFFF"));
            textLabel.setTextFill(Color.web("#FFFFFF"));
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-border-width: 0;");
            iconLabel.setTextFill(Color.web("#1A1A1A"));
            textLabel.setTextFill(Color.web("#1A1A1A"));
        }
        
        btn.setOnMouseEntered(e -> {
            if (!isSelected) {
                btn.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-border-width: 0;");
                iconLabel.setTextFill(Color.web("#1A1A1A"));
                textLabel.setTextFill(Color.web("#1A1A1A"));
            }
        });
        
        btn.setOnMouseExited(e -> updateCategoryButtonStyle(btn, isSelected));
    }
    
    private void refreshProductGrid() {
        showMenuScreen(); // Refresh the entire menu screen
    }
    
    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        showMenuScreen(); // Refresh with new sidebar state
    }

    private ScrollPane createProductMenu() {
        VBox menuContainer = new VBox(25);
        menuContainer.setPadding(new Insets(30, 30, 30, 30));
        menuContainer.setStyle("-fx-background-color: #FFFFFF;");
        menuContainer.setMinHeight(600); // Ensure container fills available space
        
        // Header row with show sidebar button and category title
        HBox menuHeader = new HBox(15);
        menuHeader.setAlignment(Pos.CENTER_LEFT);
        
        if (!sidebarVisible) {
            Button showSidebarBtn = new Button("☰ Categories");
            showSidebarBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
            showSidebarBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1A1A1A; -fx-background-radius: 8; -fx-padding: 10 16; -fx-cursor: hand; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 8;");
            showSidebarBtn.setOnMouseEntered(e -> showSidebarBtn.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #1A1A1A; -fx-background-radius: 8; -fx-padding: 10 16; -fx-cursor: hand; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-border-radius: 8;"));
            showSidebarBtn.setOnMouseExited(e -> showSidebarBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1A1A1A; -fx-background-radius: 8; -fx-padding: 10 16; -fx-cursor: hand; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 8;"));
            showSidebarBtn.setOnAction(e -> toggleSidebar());
            menuHeader.getChildren().add(showSidebarBtn);
        }
        
        Label categoryTitle = new Label(selectedCategory);
        categoryTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 24));
        categoryTitle.setTextFill(Color.web("#1A1A1A"));
        categoryTitle.setPadding(new Insets(10, 0, 0, 0)); // Add top padding for better spacing
        
        menuHeader.getChildren().add(categoryTitle);
        
        // Product grid - 3 columns max
        GridPane productGrid = new GridPane();
        productGrid.setHgap(20);
        productGrid.setVgap(20);
        productGrid.setAlignment(Pos.TOP_LEFT);
        
        // Load and filter products
        java.util.List<Product> products = getFilteredProducts();
        int col = 0;
        int row = 0;
        
        for (Product product : products) {
            VBox productCard = createProductCard(product);
            productGrid.add(productCard, col, row);
            
            col++;
            if (col >= 3) { // Max 3 columns
                col = 0;
                row++;
            }
        }
        
        menuContainer.getChildren().addAll(menuHeader, productGrid);
        
        ScrollPane scrollPane = new ScrollPane(menuContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF; -fx-background-insets: 0;");

        return scrollPane;
    }
    
    private java.util.List<Product> getFilteredProducts() {
        java.util.List<Product> allProducts = store.getProducts();
        java.util.List<Product> filtered = new java.util.ArrayList<>();
        
        for (Product product : allProducts) {
            if (productMatchesCategory(product, selectedCategory)) {
                filtered.add(product);
            }
        }
        
        return filtered;
    }
    
    private boolean productMatchesCategory(Product product, String category) {
        String name = product.getName().toLowerCase();
        switch (category) {
            case "Beverages":
                return name.contains("coffee") || name.contains("tea");
            case "Espresso":
                return name.contains("espresso") || name.contains("americano");
            case "Specialty":
                return name.contains("latte") || name.contains("mocha") || name.contains("macchiato");
            case "Cold Drinks":
                return name.contains("iced") || name.contains("cold") || name.contains("frappe");
            case "Snacks":
                return name.contains("muffin") || name.contains("cookie") || name.contains("sandwich");
            case "Pastries":
                return name.contains("cake") || name.contains("croissant") || name.contains("pastry");
            default:
                return true;
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-cursor: hand;");
        
        // Responsive sizing based on sidebar visibility
        int cardWidth = sidebarVisible ? 220 : 280;
        int cardHeight = sidebarVisible ? 280 : 320;
        
        card.setPrefWidth(cardWidth);
        card.setPrefHeight(cardHeight);

        // Product image placeholder with gradient (responsive height)
        StackPane imagePane = new StackPane();
        int imageHeight = sidebarVisible ? 160 : 200;
        imagePane.setPrefHeight(imageHeight);
        imagePane.setMaxHeight(imageHeight);
        String gradient = product.getName().toLowerCase().contains("espresso") ? 
            "linear-gradient(to bottom right, #2C2C2C, #1A1A1A)" :
            product.getName().toLowerCase().contains("cappuccino") ?
            "linear-gradient(to bottom right, #404040, #2C2C2C)" :
            "linear-gradient(to bottom right, #505050, #2C2C2C)";
        imagePane.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 16 16 0 0;");
        
        Label imagePlaceholder = new Label("☕");
        int emojiSize = sidebarVisible ? 56 : 72;
        imagePlaceholder.setFont(Font.font("Segoe UI Emoji", emojiSize));
        imagePlaceholder.setTextFill(Color.web("#F5EFE7"));
        imagePane.getChildren().add(imagePlaceholder);

        // Content area - simplified (responsive padding)
        VBox contentBox = new VBox(sidebarVisible ? 10 : 15);
        contentBox.setPadding(new Insets(sidebarVisible ? 15 : 20));
        contentBox.setAlignment(Pos.CENTER);

        // Product name (responsive font size)
        Label nameLabel = new Label(product.getName());
        int nameSize = sidebarVisible ? 16 : 20;
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, nameSize));
        nameLabel.setTextFill(Color.web("#1A1A1A"));
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Price (responsive font size)
        Label priceLabel = new Label("₱" + String.format("%.2f", product.getPrice()));
        int priceSize = sidebarVisible ? 16 : 18;
        priceLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, priceSize));
        priceLabel.setTextFill(Color.web("#1A1A1A"));

        // Stock indicator
        Label stockLabel = new Label();
        if (product.getStock() == 0) {
            stockLabel.setText("Out of Stock");
            stockLabel.setTextFill(Color.web("#D32F2F"));
            stockLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-opacity: 0.7;");
        } else {
            stockLabel.setText("In Stock: " + product.getStock());
            stockLabel.setTextFill(Color.web("#4CAF50"));
            stockLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        }

        contentBox.getChildren().addAll(nameLabel, priceLabel, stockLabel);
        card.getChildren().addAll(imagePane, contentBox);

        // Click handler - navigate to customization page
        card.setOnMouseClicked(e -> {
            if (product.getStock() > 0) {
                resetInactivityTimer();
                showCustomizationPage(product);
            }
        });

        // Hover effects
        card.setOnMouseEntered(e -> {
            if (product.getStock() > 0) {
                card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5); -fx-cursor: hand;");
            }
        });
        
        card.setOnMouseExited(e -> {
            if (product.getStock() > 0) {
                card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-cursor: hand;");
            }
        });

        return card;
    }
    
    private void showCustomizationPage(Product product) {
        BorderPane customizationLayout = new BorderPane();
        customizationLayout.setStyle("-fx-background-color: #FFFFFF;");
        
        // Header
        VBox header = createCustomizationHeader(product);
        customizationLayout.setTop(header);
        
        // Content
        ScrollPane content = createCustomizationContent(product);
        customizationLayout.setCenter(content);
        
        Scene customScene = new Scene(customizationLayout, 1024, 768);
        primaryStage.setScene(customScene);
        primaryStage.setTitle("Customize " + product.getName() + " - Coffee Shop Kiosk");
    }
    
    private VBox createCustomizationHeader(Product product) {
        VBox header = new VBox(15);
        header.setPadding(new Insets(30, 40, 20, 40));
        header.setStyle("-fx-background-color: #1A1A1A; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        
        // Back button and title row
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        // Dynamic back button text and behavior based on whether we're editing or adding new
        String backText = customizingOrderItem != null ? "← Back to Cart" : "← Back to Menu";
        Button backBtn = new Button(backText);
        backBtn.setStyle("-fx-background-color: #2C2C2C; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: normal; -fx-background-radius: 8; -fx-cursor: hand;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: #404040; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: normal; -fx-background-radius: 8; -fx-cursor: hand;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: #2C2C2C; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: normal; -fx-background-radius: 8; -fx-cursor: hand;"));
        
        if (customizingOrderItem != null) {
            backBtn.setOnAction(e -> {
                customizingOrderItem = null; // Clear the customizing reference
                showCartPage();
            });
        } else {
            backBtn.setOnAction(e -> showMenuScreen());
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        String headerText = customizingOrderItem != null ? "Edit " + product.getName() : "Customize " + product.getName();
        Label titleLabel = new Label(headerText);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.WHITE);
        
        titleRow.getChildren().addAll(backBtn, spacer, titleLabel, new Region());
        
        // Product info
        String infoText = customizingOrderItem != null ? 
            "₱" + String.format("%.2f", product.getPrice()) + " • Update your drink preferences" :
            "₱" + String.format("%.2f", product.getPrice()) + " • " + product.getStock() + " available";
        Label productInfo = new Label(infoText);
        productInfo.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        productInfo.setTextFill(Color.web("#BCAAA4"));
        
        header.getChildren().addAll(titleRow, productInfo);
        return header;
    }
    
    private ScrollPane createCustomizationContent(Product product) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background: #FAFAFA; -fx-background-color: #FAFAFA;");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 20, 20, 20));
        
        // Product image and basic info
        VBox productSection = new VBox(12);
        productSection.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 1);");
        productSection.setPadding(new Insets(16));
        productSection.setAlignment(Pos.CENTER);
        
        // Large product image
        StackPane imagePane = new StackPane();
        imagePane.setPrefSize(120, 120);
        String gradient = product.getName().toLowerCase().contains("espresso") ? 
            "linear-gradient(to bottom right, #2C2C2C, #1A1A1A)" :
            product.getName().toLowerCase().contains("cappuccino") ?
            "linear-gradient(to bottom right, #404040, #2C2C2C)" :
            "linear-gradient(to bottom right, #505050, #2C2C2C)";
        imagePane.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 12;");
        
        Label imagePlaceholder = new Label("☕");
        imagePlaceholder.setFont(Font.font("Segoe UI Emoji", 60));
        imagePlaceholder.setTextFill(Color.web("#F5EFE7"));
        imagePane.getChildren().add(imagePlaceholder);
        
        Label productName = new Label(product.getName());
        productName.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        productName.setTextFill(Color.web("#1A1A1A"));
        
        productSection.getChildren().addAll(imagePane, productName);
        
        // Customization options
        VBox customizationSection = createCustomizationOptions(product);
        
        content.getChildren().addAll(productSection, customizationSection);
        scrollPane.setContent(content);
        
        return scrollPane;
    }
    
    private VBox createCustomizationOptions(Product product) {
        VBox customSection = new VBox(14);
        customSection.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 1);");
        customSection.setPadding(new Insets(18));
        
        // Temperature selection
        VBox tempSection = new VBox(8);
        Label tempTitle = new Label("Temperature");
        tempTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        tempTitle.setTextFill(Color.web("#1A1A1A"));
        
        HBox tempButtons = new HBox(12);
        ToggleGroup tempGroup = new ToggleGroup();
        
        RadioButton hotBtn = new RadioButton("☕ Hot");
        hotBtn.setToggleGroup(tempGroup);
        hotBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        hotBtn.setTextFill(Color.web("#2C2C2C"));
        
        RadioButton coldBtn = new RadioButton("🧊 Cold");
        coldBtn.setToggleGroup(tempGroup);
        coldBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        coldBtn.setTextFill(Color.web("#2C2C2C"));
        
        // Pre-select temperature if editing existing item
        if (customizingOrderItem != null) {
            if (customizingOrderItem.getTemperature().equals("Hot")) {
                hotBtn.setSelected(true);
            } else {
                coldBtn.setSelected(true);
            }
        } else {
            hotBtn.setSelected(true); // Default to hot
        }
        
        tempButtons.getChildren().addAll(hotBtn, coldBtn);
        tempSection.getChildren().addAll(tempTitle, tempButtons);
        
        // Sugar level
        VBox sugarSection = new VBox(8);
        Label sugarTitle = new Label("Sugar Level");
        sugarTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        sugarTitle.setTextFill(Color.web("#1A1A1A"));
        
        HBox sugarButtons = new HBox(8);
        ToggleGroup sugarGroup = new ToggleGroup();
        
        String[] sugarLevels = {"0%", "25%", "50%", "75%", "100%"};
        RadioButton[] sugarBtns = new RadioButton[5];
        
        for (int i = 0; i < sugarLevels.length; i++) {
            sugarBtns[i] = new RadioButton(sugarLevels[i]);
            sugarBtns[i].setToggleGroup(sugarGroup);
            sugarBtns[i].setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
            sugarBtns[i].setTextFill(Color.web("#2C2C2C"));
            sugarButtons.getChildren().add(sugarBtns[i]);
        }
        
        // Pre-select sugar level if editing existing item
        if (customizingOrderItem != null) {
            int currentSugar = customizingOrderItem.getSugarLevel();
            for (int i = 0; i < sugarLevels.length; i++) {
                if (Integer.parseInt(sugarLevels[i].replace("%", "")) == currentSugar) {
                    sugarBtns[i].setSelected(true);
                    break;
                }
            }
        } else {
            sugarBtns[2].setSelected(true); // 50% default for new items
        }
        
        sugarSection.getChildren().addAll(sugarTitle, sugarButtons);
        
        // Add-ons section
        VBox addOnsSection = new VBox(8);
        Label addOnsTitle = new Label("Add-ons");
        addOnsTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        addOnsTitle.setTextFill(Color.web("#1A1A1A"));
        
        VBox addOnsList = new VBox(6);
        CheckBox extraShotCheck = createStyledCheckBox("Extra Shot (+₱1.00)");
        CheckBox whippedCreamCheck = createStyledCheckBox("Whipped Cream (+₱0.50)");
        CheckBox vanillaSyrupCheck = createStyledCheckBox("Vanilla Syrup (+₱0.75)");
        CheckBox caramelSyrupCheck = createStyledCheckBox("Caramel Syrup (+₱0.75)");
        CheckBox chocolateSyrupCheck = createStyledCheckBox("Chocolate Syrup (+₱0.75)");
        
        // Pre-select add-ons if editing existing item
        if (customizingOrderItem != null && customizingOrderItem.getAddOns() != null) {
            String addOns = customizingOrderItem.getAddOns();
            extraShotCheck.setSelected(addOns.contains("Extra Shot"));
            whippedCreamCheck.setSelected(addOns.contains("Whipped Cream"));
            vanillaSyrupCheck.setSelected(addOns.contains("Vanilla Syrup"));
            caramelSyrupCheck.setSelected(addOns.contains("Caramel Syrup"));
            chocolateSyrupCheck.setSelected(addOns.contains("Chocolate Syrup"));
        }
        
        addOnsList.getChildren().addAll(extraShotCheck, whippedCreamCheck, vanillaSyrupCheck, 
                                       caramelSyrupCheck, chocolateSyrupCheck);
        
        addOnsSection.getChildren().addAll(addOnsTitle, addOnsList);
        
        // Quantity and Add button
        VBox bottomSection = new VBox(12);
        
        // Quantity selector
        HBox qtySection = new HBox(10);
        qtySection.setAlignment(Pos.CENTER_LEFT);
        
        Label qtyLabel = new Label("Quantity:");
        qtyLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        qtyLabel.setTextFill(Color.web("#1A1A1A"));
        
        Button minusBtn = new Button("−");
        minusBtn.setStyle("-fx-background-color: #F8F8F8; -fx-border-color: #E0E0E0; -fx-border-radius: 16; -fx-background-radius: 16; -fx-min-width: 28; -fx-min-height: 28; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #757575;");
        minusBtn.setOnMouseEntered(e -> minusBtn.setStyle("-fx-background-color: #E0E0E0; -fx-border-color: #D0D0D0; -fx-border-radius: 16; -fx-background-radius: 16; -fx-min-width: 28; -fx-min-height: 28; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #1A1A1A;"));
        minusBtn.setOnMouseExited(e -> minusBtn.setStyle("-fx-background-color: #F8F8F8; -fx-border-color: #E0E0E0; -fx-border-radius: 16; -fx-background-radius: 16; -fx-min-width: 28; -fx-min-height: 28; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #757575;"));
        
        Label qtyValueLabel = new Label("1");
        qtyValueLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        qtyValueLabel.setTextFill(Color.web("#1A1A1A"));
        qtyValueLabel.setMinWidth(24);
        qtyValueLabel.setAlignment(Pos.CENTER);
        
        Button plusBtn = new Button("+");
        plusBtn.setStyle("-fx-background-color: #F8F8F8; -fx-border-color: #E0E0E0; -fx-border-radius: 16; -fx-background-radius: 16; -fx-min-width: 28; -fx-min-height: 28; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #757575;");
        plusBtn.setOnMouseEntered(e -> plusBtn.setStyle("-fx-background-color: #E0E0E0; -fx-border-color: #D0D0D0; -fx-border-radius: 16; -fx-background-radius: 16; -fx-min-width: 28; -fx-min-height: 28; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #1A1A1A;"));
        plusBtn.setOnMouseExited(e -> plusBtn.setStyle("-fx-background-color: #F8F8F8; -fx-border-color: #E0E0E0; -fx-border-radius: 16; -fx-background-radius: 16; -fx-min-width: 28; -fx-min-height: 28; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #757575;"));
        
        final int[] quantity = {1};
        minusBtn.setOnAction(e -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                qtyValueLabel.setText(String.valueOf(quantity[0]));
            }
        });
        plusBtn.setOnAction(e -> {
            if (quantity[0] < product.getStock()) {
                quantity[0]++;
                qtyValueLabel.setText(String.valueOf(quantity[0]));
            }
        });
        
        qtySection.getChildren().addAll(qtyLabel, minusBtn, qtyValueLabel, plusBtn);
        
        // Add to cart button (text changes based on whether we're editing or adding new)
        String buttonText = customizingOrderItem != null ? "Update Item" : "Add to Order";
        Button addButton = new Button(buttonText);
        addButton.setPrefSize(240, 44);
        addButton.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-cursor: hand;");
        addButton.setOnMouseEntered(e -> addButton.setStyle("-fx-background-color: #2C2C2C; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-cursor: hand;"));
        addButton.setOnMouseExited(e -> addButton.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-cursor: hand;"));
        
        addButton.setOnAction(e -> {
            try {
                // Get selections
                String temperature = hotBtn.isSelected() ? "Hot" : "Cold";
                int sugarLevel = 50; // default
                for (int i = 0; i < sugarBtns.length; i++) {
                    if (sugarBtns[i].isSelected()) {
                        sugarLevel = Integer.parseInt(sugarLevels[i].replace("%", ""));
                        break;
                    }
                }
                
                // Calculate add-ons
                StringBuilder addOnsText = new StringBuilder();
                double addOnsCost = 0.0;
                
                if (extraShotCheck.isSelected()) {
                    addOnsText.append("Extra Shot");
                    addOnsCost += 1.00;
                }
                if (whippedCreamCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Whipped Cream");
                    addOnsCost += 0.50;
                }
                if (vanillaSyrupCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Vanilla Syrup");
                    addOnsCost += 0.75;
                }
                if (caramelSyrupCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Caramel Syrup");
                    addOnsCost += 0.75;
                }
                if (chocolateSyrupCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Chocolate Syrup");
                    addOnsCost += 0.75;
                }
                
                if (customizingOrderItem != null) {
                    // We're editing an existing item from cart
                    System.out.println("Debug: Updating existing cart item - " + product.getName());
                    
                    // Remove the old item
                    currentOrder.removeItem(customizingOrderItem);
                    
                    // Create updated item with new customizations but keep original quantity
                    OrderItem updatedItem = new OrderItem(product, customizingOrderItem.getQuantity(), temperature, sugarLevel);
                    if (addOnsText.length() > 0) {
                        updatedItem.setAddOns(addOnsText.toString());
                        updatedItem.setAddOnsCost(addOnsCost);
                    }
                    
                    // Add updated item back to order
                    currentOrder.addItem(updatedItem);
                    
                    // Clear the customizing reference
                    customizingOrderItem = null;
                    
                    System.out.println("Debug: Item updated successfully. Total items now: " + currentOrder.getItems().size());
                    
                    updateFooter();
                    
                    // Show success and go back to cart
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Item Updated");
                    alert.setHeaderText(null);
                    alert.setContentText(product.getName() + " has been updated in your cart!");
                    alert.showAndWait();
                    
                    showCartPage();
                } else {
                    // Adding new item to cart
                    System.out.println("Debug: Creating order item - " + product.getName() + ", qty: " + quantity[0] + ", temp: " + temperature + ", sugar: " + sugarLevel);
                    OrderItem item = new OrderItem(product, quantity[0], temperature, sugarLevel);
                    
                    if (addOnsText.length() > 0) {
                        item.setAddOns(addOnsText.toString());
                        item.setAddOnsCost(addOnsCost);
                    }
    
                    System.out.println("Debug: Adding item to currentOrder - " + product.getName());
                    System.out.println("Debug: CurrentOrder before add - items: " + (currentOrder != null ? currentOrder.getItems().size() : "null"));
                    if (currentOrder == null) {
                        System.err.println("WARNING: currentOrder is null! Creating new order...");
                        currentOrder = new Order(UUID.randomUUID().toString().substring(0, 8));
                    }
                    currentOrder.addItem(item);
                    System.out.println("Debug: Item added successfully. Total items now: " + currentOrder.getItems().size());
                    System.out.println("Debug: Order ID: " + currentOrder.getOrderId());
                    
                    updateFooter();
                    System.out.println("Debug: Footer updated");
                    
                    // Show success and go back to menu
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Added to Order");
                    alert.setHeaderText(null);
                    alert.setContentText(quantity[0] + "x " + product.getName() + " added to your order!");
                    alert.showAndWait();
                    
                    showMenuScreen();
                }
            } catch (Exception ex) {
                System.err.println("Error processing item: " + ex.getMessage());
                ex.printStackTrace();
                
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to process item");
                errorAlert.setContentText("There was an error processing the item: " + ex.getMessage());
                errorAlert.showAndWait();
            }
        });
        
        bottomSection.getChildren().addAll(qtySection, addButton);
        bottomSection.setAlignment(Pos.CENTER);
        
        customSection.getChildren().addAll(tempSection, sugarSection, addOnsSection, bottomSection);
        
        return customSection;
    }

    private CheckBox createStyledCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #2C2C2C; -fx-font-weight: normal;");
        cb.setOnMouseEntered(e -> cb.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #1A1A1A; -fx-font-weight: normal;"));
        cb.setOnMouseExited(e -> cb.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #2C2C2C; -fx-font-weight: normal;"));
        return cb;
    }


    
    private String getItemsCountText() {
        if (currentOrder == null || currentOrder.getItems().isEmpty()) {
            return "No items in cart";
        }
        int count = currentOrder.getItems().size();
        return count == 1 ? "1 item" : count + " items";
    }
    
    private String getTotalText() {
        if (currentOrder == null) {
            return "₱0.00";
        }
        return "₱" + String.format("%.2f", currentOrder.getTotalAmount());
    }
    
    private void showCartDialog() {
        if (currentOrder == null || currentOrder.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Empty Cart");
            alert.setHeaderText(null);
            alert.setContentText("Your cart is empty. Add some items first!");
            alert.showAndWait();
            return;
        }
        
        // Navigate to cart page
        showCartPage();
    }

    private void showCartPage() {
        System.out.println("Debug: showCartPage called");
        System.out.println("Debug: currentOrder is null? " + (currentOrder == null));
        if (currentOrder != null) {
            System.out.println("Debug: currentOrder has " + currentOrder.getItems().size() + " items");
            for (OrderItem item : currentOrder.getItems()) {
                System.out.println("Debug: Item - " + item.getProduct().getName());
            }
        }
        
        BorderPane cartLayout = new BorderPane();
        cartLayout.setStyle("-fx-background-color: #FAFAFA;");
        
        // Header
        VBox header = createCartHeader();
        cartLayout.setTop(header);
        
        // Cart content
        ScrollPane cartContent = createCartContent();
        cartLayout.setCenter(cartContent);
        
        // Footer with total and buttons
        VBox cartFooter = createCartFooter();
        cartLayout.setBottom(cartFooter);
        
        Scene cartScene = new Scene(cartLayout, 1024, 768);
        primaryStage.setScene(cartScene);
        primaryStage.setTitle("Your Cart - Coffee Shop Kiosk");
    }
    
    private VBox createCartHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(30, 40, 20, 40));
        header.setStyle("-fx-background-color: #1A1A1A; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        
        // Back button and title row
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Button backBtn = new Button("← Back to Menu");
        backBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: #5D4037; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        backBtn.setOnAction(e -> showMainMenu());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label titleLabel = new Label("Your Order");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.WHITE);
        
        titleRow.getChildren().addAll(backBtn, spacer, titleLabel, new Region());
        
        // Order info
        Label orderInfo = new Label("Order ID: " + currentOrder.getOrderId());
        orderInfo.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        orderInfo.setTextFill(Color.web("#BCAAA4"));
        
        header.getChildren().addAll(titleRow, orderInfo);
        return header;
    }
    
    private ScrollPane createCartContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background: #FAFAFA; -fx-background-color: #FAFAFA;");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        VBox itemsContainer = new VBox(15);
        itemsContainer.setPadding(new Insets(20, 40, 20, 40));
        itemsContainer.setStyle("-fx-background-color: #FAFAFA;");
        
        System.out.println("Debug: Creating cart content - currentOrder items: " + currentOrder.getItems().size());
        
        if (currentOrder.getItems().isEmpty()) {
            Label emptyLabel = new Label("Your cart is empty");
            emptyLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 18));
            emptyLabel.setTextFill(Color.web("#757575"));
            itemsContainer.getChildren().add(emptyLabel);
            System.out.println("Debug: Showing empty cart message");
        } else {
            // Group items by product + customizations
            java.util.Map<String, Integer> qtyMap = new java.util.LinkedHashMap<>();
            java.util.Map<String, OrderItem> sampleMap = new java.util.HashMap<>();
            
            for (OrderItem item : currentOrder.getItems()) {
                String key = item.getProduct().getName() + "|" + item.getTemperature() + "|" + item.getSugarLevel() + "|" + (item.getAddOns() != null ? item.getAddOns() : "");
                qtyMap.put(key, qtyMap.getOrDefault(key, 0) + item.getQuantity());
                if (!sampleMap.containsKey(key)) sampleMap.put(key, item);
            }
            
            System.out.println("Debug: Grouped into " + qtyMap.size() + " unique items");
            
            for (java.util.Map.Entry<String, Integer> entry : qtyMap.entrySet()) {
                String key = entry.getKey();
                int totalQty = entry.getValue();
                OrderItem sample = sampleMap.get(key);
                
                System.out.println("Debug: Creating card for: " + sample.getProduct().getName() + " x" + totalQty);
                VBox itemCard = createCartItemCard(sample, totalQty);
                itemsContainer.getChildren().add(itemCard);
            }
            
            System.out.println("Debug: Added " + itemsContainer.getChildren().size() + " items to container");
        }
        
        scrollPane.setContent(itemsContainer);
        return scrollPane;
    }
    
    private VBox createCartItemCard(OrderItem item, int totalQty) {
        System.out.println("Debug: Creating item card for " + item.getProduct().getName() + " x" + totalQty);
        
        VBox itemCard = new VBox(10);
        itemCard.setPadding(new Insets(20));
        itemCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        itemCard.setMaxWidth(Double.MAX_VALUE); // Allow full width
        itemCard.setMinHeight(80);  // Set minimum height
        
        // Item header with name and price
        HBox itemHeader = new HBox();
        itemHeader.setAlignment(Pos.CENTER_LEFT);
        
        VBox itemInfo = new VBox(5);
        Label itemName = new Label(item.getProduct().getName());
        itemName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        itemName.setTextFill(Color.web("#1A1A1A"));
        
        Label itemDetails = new Label(totalQty + "x • " + item.getTemperature() + " • " + item.getSugarLevel() + "% Sugar");
        itemDetails.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        itemDetails.setTextFill(Color.web("#757575"));
        
        if (item.getAddOns() != null && !item.getAddOns().isEmpty()) {
            Label addOns = new Label("+ " + item.getAddOns());
            addOns.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            addOns.setTextFill(Color.web("#9E9E9E"));
            itemInfo.getChildren().addAll(itemName, itemDetails, addOns);
        } else {
            itemInfo.getChildren().addAll(itemName, itemDetails);
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        double unitPrice = item.getProduct().getPrice() + item.getAddOnsCost();
        double subtotal = unitPrice * totalQty;
        Label priceLabel = new Label("₱" + String.format("%.2f", subtotal));
        priceLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 18));
        priceLabel.setTextFill(Color.web("#1A1A1A"));
        
        // Quantity controls with minus and plus buttons
        HBox controls = new HBox(6);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setStyle("-fx-background-color: #F8F8F8; -fx-background-radius: 20; -fx-padding: 4;");

        Button minusBtn = new Button("−");
        minusBtn.setPrefSize(32, 32);
        minusBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-radius: 16; -fx-font-size: 16px; -fx-text-fill: #757575; -fx-cursor: hand;");
        minusBtn.setOnMouseEntered(e -> minusBtn.setStyle("-fx-background-color: #E0E0E0; -fx-border-color: transparent; -fx-background-radius: 16; -fx-font-size: 16px; -fx-text-fill: #1A1A1A; -fx-cursor: hand;"));
        minusBtn.setOnMouseExited(e -> minusBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-radius: 16; -fx-font-size: 16px; -fx-text-fill: #757575; -fx-cursor: hand;"));

        Label qtyLabel = new Label(String.valueOf(totalQty));
        qtyLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        qtyLabel.setTextFill(Color.web("#1A1A1A"));
        qtyLabel.setPrefWidth(32);
        qtyLabel.setAlignment(Pos.CENTER);

        Button plusBtn = new Button("+");
        plusBtn.setPrefSize(32, 32);
        plusBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-radius: 16; -fx-font-size: 16px; -fx-text-fill: #757575; -fx-cursor: hand;");
        plusBtn.setOnMouseEntered(e -> plusBtn.setStyle("-fx-background-color: #E0E0E0; -fx-border-color: transparent; -fx-background-radius: 16; -fx-font-size: 16px; -fx-text-fill: #1A1A1A; -fx-cursor: hand;"));
        plusBtn.setOnMouseExited(e -> plusBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-radius: 16; -fx-font-size: 16px; -fx-text-fill: #757575; -fx-cursor: hand;"));

        minusBtn.setOnAction(e -> {
            // Build a key that matches the grouping logic used when rendering the cart
            String key = item.getProduct().getName() + "|" + item.getTemperature() + "|" + item.getSugarLevel() + "|" + (item.getAddOns() != null ? item.getAddOns() : "");
            OrderItem match = null;
            for (OrderItem oi : currentOrder.getItems()) {
                String k = oi.getProduct().getName() + "|" + oi.getTemperature() + "|" + oi.getSugarLevel() + "|" + (oi.getAddOns() != null ? oi.getAddOns() : "");
                if (k.equals(key)) {
                    match = oi;
                    break;
                }
            }

            if (match != null) {
                if (match.getQuantity() > 1) {
                    // decrement quantity and force total recalculation by remove+add
                    match.setQuantity(match.getQuantity() - 1);
                    currentOrder.removeItem(match);
                    currentOrder.addItem(match);
                    System.out.println("Debug: Decremented quantity for " + match.getProduct().getName() + " to " + match.getQuantity());
                } else {
                    // remove the item entirely
                    currentOrder.removeItem(match);
                    System.out.println("Debug: Removed item " + match.getProduct().getName());
                }
            }

            // Refresh the cart UI
            showCartPage();
        });

        plusBtn.setOnAction(e -> {
            // Build a key that matches the grouping logic used when rendering the cart
            String key = item.getProduct().getName() + "|" + item.getTemperature() + "|" + item.getSugarLevel() + "|" + (item.getAddOns() != null ? item.getAddOns() : "");
            OrderItem match = null;
            for (OrderItem oi : currentOrder.getItems()) {
                String k = oi.getProduct().getName() + "|" + oi.getTemperature() + "|" + oi.getSugarLevel() + "|" + (oi.getAddOns() != null ? oi.getAddOns() : "");
                if (k.equals(key)) {
                    match = oi;
                    break;
                }
            }

            if (match != null) {
                // increment quantity and force total recalculation by remove+add
                match.setQuantity(match.getQuantity() + 1);
                currentOrder.removeItem(match);
                currentOrder.addItem(match);
                System.out.println("Debug: Incremented quantity for " + match.getProduct().getName() + " to " + match.getQuantity());
            }

            // Refresh the cart UI
            showCartPage();
        });

        controls.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

        itemHeader.getChildren().addAll(itemInfo, spacer, controls, priceLabel);
        
        // Customize button row
        HBox customizeRow = new HBox();
        customizeRow.setAlignment(Pos.CENTER_LEFT);
        customizeRow.setPadding(new Insets(10, 0, 0, 0));
        
        Button customizeBtn = new Button("✏ Customize");
        customizeBtn.setPrefSize(120, 32);
        customizeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16; -fx-font-size: 13px; -fx-text-fill: #757575; -fx-cursor: hand;");
        customizeBtn.setOnMouseEntered(e -> customizeBtn.setStyle("-fx-background-color: #F0F0F0; -fx-border-color: #D0D0D0; -fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16; -fx-font-size: 13px; -fx-text-fill: #1A1A1A; -fx-cursor: hand;"));
        customizeBtn.setOnMouseExited(e -> customizeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16; -fx-font-size: 13px; -fx-text-fill: #757575; -fx-cursor: hand;"));

        customizeBtn.setOnAction(e -> {
            // Find the matching item and navigate to customization page
            String key = item.getProduct().getName() + "|" + item.getTemperature() + "|" + item.getSugarLevel() + "|" + (item.getAddOns() != null ? item.getAddOns() : "");
            OrderItem match = null;
            for (OrderItem oi : currentOrder.getItems()) {
                String k = oi.getProduct().getName() + "|" + oi.getTemperature() + "|" + oi.getSugarLevel() + "|" + (oi.getAddOns() != null ? oi.getAddOns() : "");
                if (k.equals(key)) {
                    match = oi;
                    break;
                }
            }

            if (match != null) {
                System.out.println("Debug: Customizing item " + match.getProduct().getName());
                // Store the item we're customizing for later reference
                customizingOrderItem = match;
                showCustomizationPage(match.getProduct());
            }
        });

        customizeRow.getChildren().add(customizeBtn);
        
        itemCard.getChildren().addAll(itemHeader, customizeRow);
        
        System.out.println("Debug: Item card created for " + item.getProduct().getName() + " - Price: ₱" + String.format("%.2f", subtotal));
        
        return itemCard;
    }
    
    private VBox createCartFooter() {
        VBox footer = new VBox(20);
        footer.setPadding(new Insets(30, 40, 40, 40));
        footer.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, -2);");
        
        // Total section
        HBox totalSection = new HBox();
        totalSection.setAlignment(Pos.CENTER_LEFT);
        
        Label totalLabel = new Label("Total:");
        totalLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        totalLabel.setTextFill(Color.web("#1A1A1A"));
        
        Region totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);
        
        Label totalAmount = new Label("₱" + String.format("%.2f", currentOrder.getTotalAmount()));
        totalAmount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        totalAmount.setTextFill(Color.web("#4CAF50"));
        
        totalSection.getChildren().addAll(totalLabel, totalSpacer, totalAmount);
        
        // Buttons
        HBox buttonSection = new HBox(20);
        buttonSection.setAlignment(Pos.CENTER);
        
        Button continueShoppingBtn = new Button("Continue Shopping");
        continueShoppingBtn.setPrefSize(200, 60);
        continueShoppingBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;");
        continueShoppingBtn.setOnMouseEntered(e -> continueShoppingBtn.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: normal; -fx-background-radius: 12; -fx-cursor: hand;"));
        continueShoppingBtn.setOnMouseExited(e -> continueShoppingBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;"));
        continueShoppingBtn.setOnAction(e -> showMenuScreen());
        
        Button checkoutBtn = new Button("Proceed to Checkout");
        checkoutBtn.setPrefSize(250, 60);
        checkoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;");
        checkoutBtn.setOnMouseEntered(e -> checkoutBtn.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;"));
        checkoutBtn.setOnMouseExited(e -> checkoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;"));
        checkoutBtn.setOnAction(e -> showCheckoutPage());
        
        buttonSection.getChildren().addAll(continueShoppingBtn, checkoutBtn);
        
        footer.getChildren().addAll(totalSection, buttonSection);
        return footer;
    }
    
    private void showMainMenu() {
        // Go back to the main menu scene without restarting the app
        showMenuScreen();
    }
    
    private void showCheckoutPage() {
        BorderPane checkoutLayout = new BorderPane();
        checkoutLayout.setStyle("-fx-background-color: #FAFAFA;");
        
        // Header
        VBox header = createCheckoutHeader();
        checkoutLayout.setTop(header);
        
        // Checkout content
        VBox checkoutContent = createCheckoutContent();
        ScrollPane scrollPane = new ScrollPane(checkoutContent);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        checkoutLayout.setCenter(scrollPane);
        
        Scene checkoutScene = new Scene(checkoutLayout, 1024, 768);
        primaryStage.setScene(checkoutScene);
        primaryStage.setTitle("Checkout - Coffee Shop Kiosk");
    }
    
    private VBox createCheckoutHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(30, 40, 20, 40));
        header.setStyle("-fx-background-color: #1A1A1A; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        
        // Back button and title row
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Button backBtn = new Button("← Back to Cart");
        backBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: #5D4037; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        backBtn.setOnAction(e -> showCartPage());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label titleLabel = new Label("Checkout");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.WHITE);
        
        titleRow.getChildren().addAll(backBtn, spacer, titleLabel, new Region());
        
        // Order info
        Label orderInfo = new Label("Order ID: " + currentOrder.getOrderId() + " • " + currentOrder.getItems().size() + " items");
        orderInfo.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        orderInfo.setTextFill(Color.web("#BCAAA4"));
        
        header.getChildren().addAll(titleRow, orderInfo);
        return header;
    }
    
    private VBox createCheckoutContent() {
        VBox content = new VBox(30);
        content.setPadding(new Insets(30, 40, 40, 40));
        
        // Order type selection
        VBox orderTypeSection = new VBox(15);
        Label orderTypeTitle = new Label("Order Type");
        orderTypeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        orderTypeTitle.setTextFill(Color.web("#1A1A1A"));
        
        HBox orderTypeButtons = new HBox(20);
        orderTypeButtons.setAlignment(Pos.CENTER_LEFT);
        
        ToggleGroup orderTypeGroup = new ToggleGroup();
        
        RadioButton dineInBtn = new RadioButton("Dine In");
        dineInBtn.setToggleGroup(orderTypeGroup);
        dineInBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        dineInBtn.setSelected(true);
        orderType = "Dine In";
        
        RadioButton takeOutBtn = new RadioButton("Take Out");
        takeOutBtn.setToggleGroup(orderTypeGroup);
        takeOutBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        
        orderTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == dineInBtn) {
                orderType = "Dine In";
            } else if (newVal == takeOutBtn) {
                orderType = "Take Out";
            }
        });
        
        orderTypeButtons.getChildren().addAll(dineInBtn, takeOutBtn);
        orderTypeSection.getChildren().addAll(orderTypeTitle, orderTypeButtons);
        
        // Order summary
        VBox summarySection = createOrderSummarySection();
        
        // Payment buttons
        VBox paymentSection = createPaymentSection();
        
        content.getChildren().addAll(orderTypeSection, summarySection, paymentSection);
        return content;
    }
    
    private VBox createOrderSummarySection() {
        VBox summarySection = new VBox(15);
        summarySection.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        summarySection.setPadding(new Insets(25));
        
        Label summaryTitle = new Label("Order Summary");
        summaryTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        summaryTitle.setTextFill(Color.web("#1A1A1A"));
        
        VBox itemsList = new VBox(10);
        
        // Group items by product + customizations
        java.util.Map<String, Integer> qtyMap = new java.util.LinkedHashMap<>();
        java.util.Map<String, OrderItem> sampleMap = new java.util.HashMap<>();
        
        for (OrderItem item : currentOrder.getItems()) {
            String key = item.getProduct().getName() + "|" + item.getTemperature() + "|" + item.getSugarLevel() + "|" + (item.getAddOns() != null ? item.getAddOns() : "");
            qtyMap.put(key, qtyMap.getOrDefault(key, 0) + item.getQuantity());
            if (!sampleMap.containsKey(key)) sampleMap.put(key, item);
        }
        
        for (java.util.Map.Entry<String, Integer> entry : qtyMap.entrySet()) {
            String key = entry.getKey();
            int totalQty = entry.getValue();
            OrderItem sample = sampleMap.get(key);
            
            HBox itemRow = new HBox();
            itemRow.setAlignment(Pos.CENTER_LEFT);
            
            VBox itemInfo = new VBox(2);
            Label itemName = new Label(sample.getProduct().getName());
            itemName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            itemName.setTextFill(Color.web("#1A1A1A"));
            
            String details = totalQty + "x • " + sample.getTemperature() + " • " + sample.getSugarLevel() + "% Sugar";
            if (sample.getAddOns() != null && !sample.getAddOns().isEmpty()) {
                details += " • +" + sample.getAddOns();
            }
            
            Label itemDetails = new Label(details);
            itemDetails.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            itemDetails.setTextFill(Color.web("#757575"));
            
            itemInfo.getChildren().addAll(itemName, itemDetails);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            double unitPrice = sample.getProduct().getPrice() + sample.getAddOnsCost();
            double subtotal = unitPrice * totalQty;
            Label priceLabel = new Label("₱" + String.format("%.2f", subtotal));
            priceLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            priceLabel.setTextFill(Color.web("#1A1A1A"));
            
            itemRow.getChildren().addAll(itemInfo, spacer, priceLabel);
            itemsList.getChildren().add(itemRow);
        }
        
        // Total row
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #E0E0E0;");
        
        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setPadding(new Insets(10, 0, 0, 0));
        
        Label totalLabel = new Label("Total:");
        totalLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        totalLabel.setTextFill(Color.web("#1A1A1A"));
        
        Region totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);
        
        Label totalAmount = new Label("₱" + String.format("%.2f", currentOrder.getTotalAmount()));
        totalAmount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        totalAmount.setTextFill(Color.web("#4CAF50"));
        
        totalRow.getChildren().addAll(totalLabel, totalSpacer, totalAmount);
        
        summarySection.getChildren().addAll(summaryTitle, itemsList, separator, totalRow);
        return summarySection;
    }
    
    private VBox createPaymentSection() {
        VBox paymentSection = new VBox(15);
        Label paymentTitle = new Label("Payment Method");
        paymentTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        paymentTitle.setTextFill(Color.web("#1A1A1A"));
        
        HBox paymentButtons = new HBox(20);
        paymentButtons.setAlignment(Pos.CENTER);
        
        Button cashBtn = new Button("💵 Pay with Cash");
        cashBtn.setPrefSize(200, 60);
        cashBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;");
        cashBtn.setOnMouseEntered(e -> cashBtn.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;"));
        cashBtn.setOnMouseExited(e -> cashBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;"));
        cashBtn.setOnAction(e -> completeOrder("Cash"));
        
        Button cardBtn = new Button("💳 Pay with Card");
        cardBtn.setPrefSize(200, 60);
        cardBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;");
        cardBtn.setOnMouseEntered(e -> cardBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;"));
        cardBtn.setOnMouseExited(e -> cardBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;"));
        cardBtn.setOnAction(e -> completeOrder("Card"));
        
        paymentButtons.getChildren().addAll(cashBtn, cardBtn);
        paymentSection.getChildren().addAll(paymentTitle, paymentButtons);
        
        return paymentSection;
    }
    
    private void completeOrder(String paymentMethod) {
        if (currentOrder.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Empty Order");
            alert.setHeaderText(null);
            alert.setContentText("Please add items to your order before checkout.");
            alert.showAndWait();
            return;
        }

        // Validate stock
        if (!store.isStockSufficient(currentOrder)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Insufficient Stock");
            alert.setHeaderText("Cannot Process Order");
            alert.setContentText("Some items in your order are out of stock. Please modify your order.");
            alert.showAndWait();
            return;
        }

        // Process payment and complete order
        try {
            store.checkoutBasket(currentOrder);
            
            // Show success message
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Order Completed");
            successAlert.setHeaderText("Payment Successful!");
            successAlert.setContentText("""
                Your order has been placed successfully.

                Order ID: %s
                Payment Method: %s
                Total: ₱%.2f
                Order Type: %s

                Please wait for your order to be prepared.
                """.formatted(
                    currentOrder.getOrderId(),
                    paymentMethod,
                    currentOrder.getTotalAmount(),
                    orderType
                ));
            successAlert.showAndWait();
            
            // Reset for new order and go back to main menu
            currentOrder = new Order(UUID.randomUUID().toString().substring(0, 8));
            showMainMenu();
            
        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Order Failed");
            errorAlert.setHeaderText("Payment Failed");
            errorAlert.setContentText("There was an error processing your order. Please try again.");
            errorAlert.showAndWait();
        }
    }



    // ==================== INACTIVITY TIMER ====================
    
    private void startInactivityTimer() {
        stopInactivityTimer(); // Stop any existing timer
        countdownSeconds = 30;

        inactivityTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            countdownSeconds--;

            if (countdownLabel != null) {
                // Timeline runs on FX thread, update directly
                countdownLabel.setText("⏱ " + countdownSeconds + "s");

                if (countdownSeconds <= 10) {
                    countdownLabel.setTextFill(Color.web("#D32F2F"));
                    countdownLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
                } else {
                    countdownLabel.setTextFill(Color.web("#666"));
                    countdownLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
                }
            }

            // Return to welcome when timer reaches 0
            if (countdownSeconds <= 0) {
                stopInactivityTimer();
                showWelcomeScreen();
            }
        }));

        inactivityTimer.setCycleCount(Timeline.INDEFINITE);
        inactivityTimer.play();
    }
    
    private void resetInactivityTimer() {
        if (inactivityTimer != null && inactivityTimer.getStatus() == Timeline.Status.RUNNING) {
            countdownSeconds = 30;
            if (countdownLabel != null) {
                countdownLabel.setText("⏱ " + countdownSeconds + "s");
                countdownLabel.setTextFill(Color.web("#666"));
                countdownLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
            }
        }
    }
    
    private void stopInactivityTimer() {
        if (inactivityTimer != null) {
            inactivityTimer.stop();
            inactivityTimer = null;
        }
    }

    // Overlay/transition methods removed

    public static void main(String[] args) {
        launch(args);
    }
}
