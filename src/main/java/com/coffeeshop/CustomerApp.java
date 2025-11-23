package com.coffeeshop;

import java.util.UUID;

import com.coffeeshop.model.Order;
import com.coffeeshop.model.OrderItem;
import com.coffeeshop.model.Product;
import com.coffeeshop.service.Store;
import com.coffeeshop.service.TextDatabase;
import com.coffeeshop.model.PendingOrder;
import com.coffeeshop.model.Receipt;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class CustomerApp extends Application {
    private Store store;
    private Order currentOrder;
    private OrderItem customizingOrderItem = null; // Track item being customized from cart
    private Stage primaryStage;
    private boolean keepMaximized = false; // remember we want windowed-fullscreen
    private HBox footerContainer;
    private Label footerItemsLabel;
    private Label footerTotalLabel;
    private Button footerViewCartBtn;
    private Button footerCheckoutBtn;
    private String orderType = ""; // "Dine In" or "Take Away"
    private Timeline inactivityTimer;
    private int countdownSeconds = 30;
    private Label countdownLabel;
    // Persistent container to avoid swapping entire Scene (prevents window flicker/minimize)
    private StackPane persistentRoot;
    private Scene persistentScene;
    // Simple in-memory cache for product images to avoid repeated disk I/O and decoding
    private java.util.Map<String, javafx.scene.image.Image> imageCache = new java.util.HashMap<>();
    // Simple index for quick lookup of image files by product id (built once)
    private java.util.Map<String, java.io.File> imageFileIndex = null;


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        store = Store.getInstance();
        
        primaryStage.setTitle("Brewise Coffee Shop - Kiosk");
        // Start in windowed-fullscreen: maximized with decorations so Windows taskbar remains visible
        try {
            primaryStage.setResizable(true);
            primaryStage.setFullScreen(false);
            primaryStage.setMaximized(true);
            keepMaximized = true;
        } catch (Exception ex) {
            System.err.println("Could not set maximized/windowed state: " + ex.getMessage());
        }

        // Create a persistent root + scene and set on stage to avoid swapping Scenes
        persistentRoot = new StackPane();
        persistentScene = new Scene(persistentRoot, 1600, 900);
        primaryStage.setScene(persistentScene);
        primaryStage.show();

        // Show welcome screen content inside the persistent scene
        showWelcomeScreen();
    }

    // Utility: set scene without unexpectedly changing windowed/maximized state
    private void setScenePreserveWindowSize(Scene scene) {
        // If we have a persistent root, swap its children with the new scene root instead
        if (persistentRoot != null) {
            Parent newRoot = scene.getRoot();
            boolean wasMax = primaryStage != null && primaryStage.isMaximized();
            double prevW = primaryStage != null ? primaryStage.getWidth() : 0;
            double prevH = primaryStage != null ? primaryStage.getHeight() : 0;

            System.out.println("[DEBUG] setScenePreserveWindowSize - swapping root: wasMax=" + wasMax + ", w=" + prevW + ", h=" + prevH);

            javafx.application.Platform.runLater(() -> {
                persistentRoot.getChildren().setAll(newRoot);
                try {
                    persistentScene.getStylesheets().clear();
                    persistentScene.getStylesheets().addAll(scene.getStylesheets());
                } catch (Exception ignored) {}

                try {
                    if (keepMaximized || wasMax) {
                        primaryStage.setMaximized(true);
                    } else {
                        if (prevW > 0 && prevH > 0) {
                            primaryStage.setWidth(prevW);
                            primaryStage.setHeight(prevH);
                        }
                    }
                } catch (Exception ex) { System.out.println("[DEBUG] restore error: " + ex.getMessage()); }

                if (!primaryStage.isShowing()) primaryStage.show();
                System.out.println("[DEBUG] setScenePreserveWindowSize - swapped: isMax=" + primaryStage.isMaximized() + ", w=" + primaryStage.getWidth() + ", h=" + primaryStage.getHeight());
            });
            return;
        }

        // fallback: if no persistent root available, set the scene directly
        if (primaryStage == null) primaryStage = new Stage();
        boolean wasMax = primaryStage.isMaximized();
        double prevW = primaryStage.getWidth();
        double prevH = primaryStage.getHeight();
        System.out.println("[DEBUG] setScenePreserveWindowSize - fallback setScene: wasMax=" + wasMax + ", w=" + prevW + ", h=" + prevH);
        primaryStage.setScene(scene);
        javafx.application.Platform.runLater(() -> {
            try {
                if (keepMaximized || wasMax) primaryStage.setMaximized(true);
                else if (prevW > 0 && prevH > 0) { primaryStage.setWidth(prevW); primaryStage.setHeight(prevH); }
            } catch (Exception ex) { System.out.println("[DEBUG] deferred fallback restore error: " + ex.getMessage()); }
            if (!primaryStage.isShowing()) primaryStage.show();
        });
    }

    // Attempt to apply Atlantafx theme stylesheet if available on classpath
    private void applyAtlantafx(Scene scene) {
        if (scene == null) return;
        try {
            String[] candidates = new String[] {
                "/styles/atlantafx-fallback.css",
                "/atlantafx.css",
                "/atlantafx/atlantafx.css",
                "/io/github/palexdev/atlantafx/atlantafx.css",
                "/META-INF/atlantafx.css"
            };

            for (String p : candidates) {
                java.net.URL u = getClass().getResource(p);
                if (u != null) {
                    scene.getStylesheets().add(u.toExternalForm());
                    return;
                }
            }

            // If CSS not found, check for Atlantafx classes (best-effort, non-failing)
            try {
                Class.forName("io.github.palexdev.atlantafx.AtlanTheme");
                // If present but stylesheet path is unknown, don't crash — user can run mvn to resolve.
            } catch (ClassNotFoundException ignored) {
            }
        } catch (Exception ex) {
            // Do not block startup if theme loading fails
            System.err.println("Atlantafx theme not applied: " + ex.getMessage());
        }
    }

    private void showWelcomeScreen() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #FFFFFF;"); // Modern White Background

        VBox welcomeBox = new VBox(40);
        welcomeBox.setAlignment(Pos.CENTER);
        welcomeBox.setPadding(new Insets(60));
        welcomeBox.setMaxWidth(1200);
        welcomeBox.setMaxHeight(800);
        // Larger, flush card (no rounded corners) - transparent background
        welcomeBox.setStyle("-fx-background-color: transparent; -fx-background-radius: 0; -fx-border-color: transparent; -fx-border-width: 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 20, 0, 0, 10);");

        // Welcome text
        VBox titleBox = new VBox(15);
        titleBox.setAlignment(Pos.CENTER);
        // Add large logo above the title - now more prominent
        javafx.scene.layout.StackPane logoContainer = createBREWISELogo(180);
        if (logoContainer != null) {
            titleBox.getChildren().add(logoContainer);
        }

        Label welcomeTitle = new Label("Welcome to Brewise");
        welcomeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        welcomeTitle.setTextFill(Color.web("#1A1A1A"));

        Label welcomeSubtitle = new Label("Premium Coffee Experience");
        welcomeSubtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 20));
        welcomeSubtitle.setTextFill(Color.web("#795548"));
        
        titleBox.getChildren().addAll(welcomeTitle, welcomeSubtitle);

        // Options Container
        HBox optionsBox = new HBox(30);
        optionsBox.setAlignment(Pos.CENTER);

        // Dine In button (use coffee cup icon)
        VBox dineInBox = createOptionCard("☕", "Dine In", "Enjoy your meal in our cozy space");
        dineInBox.setOnMouseClicked(e -> {
            orderType = "Dine In";
            showMenuScreen();
        });

        // Take Away button
        VBox takeAwayBox = createOptionCard("🛍", "Take Away", "Grab your coffee on the go");
        takeAwayBox.setOnMouseClicked(e -> {
            orderType = "Take Away";
            showMenuScreen();
        });

        optionsBox.getChildren().addAll(dineInBox, takeAwayBox);

        welcomeBox.getChildren().addAll(titleBox, optionsBox);
        root.getChildren().add(welcomeBox);

        Scene scene = new Scene(root, 1600, 900);
        setScenePreserveWindowSize(scene);
        applyAtlantafx(scene);
    }

    // Create an ImageView from the project image if available; returns null on failure
    private ImageView createLogoView(double size) {
        // Try classpath resource first (recommended for packaged app)
        try {
            java.net.URL u = getClass().getResource("/images/LOGO3.png");
            if (u != null) {
                Image img = new Image(u.toExternalForm(), size, size, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(size);
                iv.setFitHeight(size);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (Exception ignored) {}

        // Try file path (development workspace)
        try {
            String filePath = "file:data/images/LOGO3.png";
            java.io.File f = new java.io.File("data/images/LOGO3.png");
            if (f.exists()) {
                Image img = new Image(filePath, size, size, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(size);
                iv.setFitHeight(size);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (Exception ignored) {}

        // Fallback to LOGO3.png in data/images
        try {
            String legacy = "file:data/images/LOGO3.png";
            java.io.File lf = new java.io.File("data/images/LOGO3.png");
            if (lf.exists()) {
                Image img = new Image(legacy, size, size, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(size);
                iv.setFitHeight(size);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (Exception ignored) {}

        // Nothing found
        return null;
    }

    // Wrap logo in circular container without cutting
    private javafx.scene.layout.StackPane createCircularLogoContainer(double size) {
        ImageView logo = createLogoView(size);
        if (logo == null) return null;

        javafx.scene.layout.StackPane container = new javafx.scene.layout.StackPane();
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        container.setMinSize(size, size);
        
        // Create circular background
        javafx.scene.shape.Circle background = new javafx.scene.shape.Circle(size / 2);
        background.setFill(Color.WHITE);
        background.setStroke(Color.web("#E0E0E0"));
        background.setStrokeWidth(0.5);
        
        // Create circular clip for the image and center it correctly
        javafx.scene.shape.Circle imageClip = new javafx.scene.shape.Circle();
        imageClip.setCenterX(size / 2.0);
        imageClip.setCenterY(size / 2.0);
        imageClip.setRadius(Math.max(0, size / 2.0 - 4));
        logo.setClip(imageClip);
        
        container.getChildren().addAll(background, logo);
        return container;
    }

    // Create BREWISE circular logo with LOGO.jpg image for welcome screen
    private javafx.scene.layout.StackPane createBREWISELogo(double size) {
        javafx.scene.layout.StackPane container = new javafx.scene.layout.StackPane();
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        container.setMinSize(size, size);
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-background-color: transparent;");
        
        // Try to load LOGO3.png image
        ImageView logoImage = null;
        try {
            // Try classpath resource first
            java.net.URL u = getClass().getResource("/images/LOGO3.png");
            if (u != null) {
                Image img = new Image(u.toExternalForm(), size, size, true, true);
                logoImage = new ImageView(img);
                logoImage.setFitWidth(size);
                logoImage.setFitHeight(size);
                logoImage.setPreserveRatio(true);
                logoImage.setSmooth(true);
            }
        } catch (Exception ignored) {}
        
        // If not found in classpath, try file system
        if (logoImage == null) {
            try {
                java.io.File f = new java.io.File("data/images/LOGO3.png");
                if (f.exists()) {
                    Image img = new Image("file:data/images/LOGO3.png", size, size, true, true);
                    logoImage = new ImageView(img);
                    logoImage.setFitWidth(size);
                    logoImage.setFitHeight(size);
                    logoImage.setPreserveRatio(true);
                    logoImage.setSmooth(true);
                }
            } catch (Exception ignored) {}
        }
        
        if (logoImage != null) {
            // Display logo with circular background and circular clip
            javafx.scene.shape.Circle background = new javafx.scene.shape.Circle(size / 2);
            background.setFill(Color.WHITE);
            background.setStroke(Color.web("#E0E0E0"));
            background.setStrokeWidth(0.5);

            // Clip the image to a slightly smaller circle so the background border shows
            javafx.scene.shape.Circle imageClip = new javafx.scene.shape.Circle();
            imageClip.setCenterX(size / 2.0);
            imageClip.setCenterY(size / 2.0);
            imageClip.setRadius(Math.max(0, size / 2.0 - 4));
            logoImage.setClip(imageClip);

            container.getChildren().addAll(background, logoImage);
        } else {
            // Fallback if image not found
            javafx.scene.shape.Circle middleCircle = new javafx.scene.shape.Circle(size / 2 - 8);
            middleCircle.setFill(Color.WHITE);
            Label cupIcon = new Label("☕");
            cupIcon.setFont(Font.font("Segoe UI Emoji", size * 0.35));
            cupIcon.setTextFill(Color.web("#1A1A1A"));
            container.getChildren().addAll(middleCircle, cupIcon);
        }
        
        return container;
    }

    // Create compact BREWISE logo for header (smaller version)
    private javafx.scene.layout.StackPane createCompactBREWISELogo(double size) {
        javafx.scene.layout.StackPane container = new javafx.scene.layout.StackPane();
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        container.setMinSize(size, size);
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-background-color: transparent;");
        
        // Try to load LOGO3.png image
        ImageView logoImage = null;
        try {
            // Try classpath resource first
            java.net.URL u = getClass().getResource("/images/LOGO3.png");
            if (u != null) {
                Image img = new Image(u.toExternalForm(), size, size, true, true);
                logoImage = new ImageView(img);
                logoImage.setFitWidth(size);
                logoImage.setFitHeight(size);
                logoImage.setPreserveRatio(true);
                logoImage.setSmooth(true);
            }
        } catch (Exception ignored) {}
        
        // If not found in classpath, try file system
        if (logoImage == null) {
            try {
                java.io.File f = new java.io.File("data/images/LOGO3.png");
                if (f.exists()) {
                    Image img = new Image("file:data/images/LOGO3.png", size, size, true, true);
                    logoImage = new ImageView(img);
                    logoImage.setFitWidth(size);
                    logoImage.setFitHeight(size);
                    logoImage.setPreserveRatio(true);
                    logoImage.setSmooth(true);
                }
            } catch (Exception ignored) {}
        }
        
        if (logoImage != null) {
            // Display logo with circular background and circular clip
            javafx.scene.shape.Circle background = new javafx.scene.shape.Circle(size / 2);
            background.setFill(Color.WHITE);
            background.setStroke(Color.web("#E0E0E0"));
            background.setStrokeWidth(0.5);

            javafx.scene.shape.Circle imageClip = new javafx.scene.shape.Circle();
            imageClip.setCenterX(size / 2.0);
            imageClip.setCenterY(size / 2.0);
            imageClip.setRadius(Math.max(0, size / 2.0 - 4));
            logoImage.setClip(imageClip);

            container.getChildren().addAll(background, logoImage);
        } else {
            // Fallback with coffee cup
            javafx.scene.shape.Circle middleCircle = new javafx.scene.shape.Circle(size / 2 - 3);
            middleCircle.setFill(Color.web("#8D6E63"));
            
            Label cupIcon = new Label("☕");
            cupIcon.setFont(Font.font("Segoe UI Emoji", size * 0.6));
            cupIcon.setTextFill(Color.WHITE);
            
            container.getChildren().addAll(middleCircle, cupIcon);
        }
        
        return container;
    }

    private VBox createOptionCard(String icon, String title, String desc) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 30, 40, 30));
        card.setPrefWidth(380);
        card.setPrefHeight(380);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 0; -fx-border-width: 0; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 3);");
        
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("Segoe UI Emoji", 64));
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 22));
        titleLabel.setTextFill(Color.web("#1A1A1A"));
        
        Label descLabel = new Label(desc);
        descLabel.setFont(Font.font("Segoe UI", 14));
        descLabel.setTextFill(Color.web("#666666"));
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setWrapText(true);
        
        card.getChildren().addAll(iconLabel, titleLabel, descLabel);
        
        // Hover animation
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 0; -fx-border-width: 0; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(44, 44, 44, 0.15), 20, 0, 0, 5);");
            titleLabel.setTextFill(Color.web("#2C2C2C"));
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 0; -fx-border-width: 0; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.05), 15, 0, 0, 3);");
            titleLabel.setTextFill(Color.web("#1A1A1A"));
        });
        
        return card;
    }

    // Create option card using an image file name (prefers classpath `/images/<name>` then `data/images/<name>`)
    private VBox createOptionCardWithImage(String imageFileName, String title, String desc) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 30, 40, 30));
        card.setPrefWidth(380);
        card.setPrefHeight(380);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 0; -fx-border-width: 0; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 3);");

        ImageView iv = null;
        // try classpath
        try {
            java.net.URL u = getClass().getResource("/images/" + imageFileName);
            if (u != null) {
                Image img = new Image(u.toExternalForm(), 96, 96, true, true);
                iv = new ImageView(img);
                iv.setFitWidth(96);
                iv.setFitHeight(96);
                iv.setPreserveRatio(true);
            }
        } catch (Exception ignored) {}

        if (iv == null) {
            try {
                java.io.File f = new java.io.File("data/images/" + imageFileName);
                if (f.exists()) {
                    Image img = new Image("file:data/images/" + imageFileName, 96, 96, true, true);
                    iv = new ImageView(img);
                    iv.setFitWidth(96);
                    iv.setFitHeight(96);
                    iv.setPreserveRatio(true);
                }
            } catch (Exception ignored) {}
        }

        // fallback emoji
        Label iconLabel;
        if (iv != null) {
            iconLabel = new Label();
            iconLabel.setGraphic(iv);
        } else {
            iconLabel = new Label("🍽");
            iconLabel.setFont(Font.font("Segoe UI Emoji", 64));
        }

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 22));
        titleLabel.setTextFill(Color.web("#1A1A1A"));

        Label descLabel = new Label(desc);
        descLabel.setFont(Font.font("Segoe UI", 14));
        descLabel.setTextFill(Color.web("#666666"));
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setWrapText(true);

        card.getChildren().addAll(iconLabel, titleLabel, descLabel);

        // Hover animation (similar to createOptionCard)
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 0; -fx-border-width: 0; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(44, 44, 44, 0.15), 20, 0, 0, 5);");
            titleLabel.setTextFill(Color.web("#2C2C2C"));
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 0; -fx-border-width: 0; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.05), 15, 0, 0, 3);");
            titleLabel.setTextFill(Color.web("#1A1A1A"));
        });

        return card;
    }

    private String selectedCategory = "All"; // Default category
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
        HBox mainContent = new HBox(0);
        mainContent.setStyle("-fx-background-color: #FFFFFF;");
        mainContent.setMinHeight(500);
        mainContent.setAlignment(Pos.TOP_LEFT);
        mainContent.setFillHeight(true);
        
        // Left sidebar - Categories (conditionally shown)
        if (sidebarVisible) {
            categorySidebarContainer = createCategorySidebar();
            categorySidebarContainer.setMinHeight(600);
            mainContent.getChildren().add(categorySidebarContainer);
        }

        // Right - Product Menu (3-column grid)
        ScrollPane menuScroll = createProductMenu();
        menuScroll.setMinHeight(600);
        HBox.setHgrow(menuScroll, Priority.ALWAYS);
        mainContent.getChildren().add(menuScroll);
        
        root.setCenter(mainContent);
        
        // Bottom - Footer with cart info
        footerContainer = createFooter();
        root.setBottom(footerContainer);

        Scene scene = new Scene(root, 1600, 900);
        setScenePreserveWindowSize(scene);

        applyAtlantafx(scene);

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
        cancelButton.getStyleClass().add("cancel-button");
        cancelButton.setStyle("-fx-cursor: hand;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: #FFEBEE; -fx-border-color: #D32F2F; -fx-border-radius: 0; -fx-background-radius: 0; -fx-cursor: hand;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: transparent; -fx-border-color: #D32F2F; -fx-border-radius: 0; -fx-background-radius: 0; -fx-cursor: hand;"));
        
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

        // Center content - Now with BREWISE logo
        VBox centerBox = new VBox(8);
        centerBox.setAlignment(Pos.CENTER);

        // Logo and title with BREWISE circular logo
        HBox logoBox = new HBox(12);
        logoBox.setAlignment(Pos.CENTER);
        
        // Add circular logo to header
        javafx.scene.layout.StackPane headerLogo = createCompactBREWISELogo(50);
        if (headerLogo != null) {
            logoBox.getChildren().add(headerLogo);
        } else {
            // Fallback
            Label fallbackLogo = new Label("☕");
            fallbackLogo.setFont(Font.font("Segoe UI Emoji", 24));
            logoBox.getChildren().add(fallbackLogo);
        }
        
        Label brandName = new Label("BREWISE");
        brandName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        brandName.setTextFill(Color.web("#1A1A1A"));
        logoBox.getChildren().add(brandName);

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
        orderTypeBadge.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 0; -fx-border-radius: 0;");

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
        footerViewCartBtn.getStyleClass().add("secondary-button");
        footerViewCartBtn.setStyle("-fx-padding: 12 24; -fx-cursor: hand;");
        footerViewCartBtn.setOnMouseEntered(e -> footerViewCartBtn.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #2C2C2C; -fx-border-width: 2; -fx-border-radius: 0; -fx-text-fill: #1A1A1A; -fx-cursor: hand;"));
        footerViewCartBtn.setOnMouseExited(e -> footerViewCartBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #2C2C2C; -fx-border-width: 2; -fx-border-radius: 0; -fx-text-fill: #2C2C2C; -fx-cursor: hand;"));
        footerViewCartBtn.setOnAction(e -> showCartDialog());
        
        footerCheckoutBtn = new Button("Proceed to Checkout");
        footerCheckoutBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        footerCheckoutBtn.setPadding(new Insets(12, 24, 12, 24));
        footerCheckoutBtn.getStyleClass().add("primary-button");
        footerCheckoutBtn.setStyle("-fx-cursor: hand;");
        footerCheckoutBtn.setOnMouseEntered(e -> footerCheckoutBtn.setStyle("-fx-background-color: #43A047; -fx-text-fill: white; -fx-background-radius: 0; -fx-cursor: hand;"));
        footerCheckoutBtn.setOnMouseExited(e -> footerCheckoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 0; -fx-cursor: hand;"));
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
                footerViewCartBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #2C2C2C; -fx-border-width: 2; -fx-border-radius: 0; -fx-text-fill: #2C2C2C; -fx-cursor: hand;");
            } else {
                footerViewCartBtn.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #E0E0E0; -fx-border-width: 2; -fx-border-radius: 0; -fx-text-fill: #BDBDBD;");
            }
        }
        
        if (footerCheckoutBtn != null) {
            footerCheckoutBtn.setDisable(!hasItems);
            if (hasItems) {
                footerCheckoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 0; -fx-cursor: hand;");
            } else {
                footerCheckoutBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #BDBDBD; -fx-background-radius: 0;");
            }
        }
    }
    
    private VBox createCategorySidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("category-sidebar");
        sidebar.setPrefWidth(340);
        sidebar.setMaxWidth(340);
        sidebar.setMinWidth(340);
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
        backButton.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1A1A1A; -fx-background-radius: 0; -fx-min-width: 32; -fx-min-height: 32; -fx-cursor: hand; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 0;");
        backButton.setOnMouseEntered(e -> backButton.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #1A1A1A; -fx-background-radius: 0; -fx-min-width: 32; -fx-min-height: 32; -fx-cursor: hand; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-border-radius: 0;"));
        backButton.setOnMouseExited(e -> backButton.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1A1A1A; -fx-background-radius: 0; -fx-min-width: 32; -fx-min-height: 32; -fx-cursor: hand; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 0;"));
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
        
        // Load categories dynamically from Store
        java.util.List<String> storeCategories = store.getCategories();
        java.util.List<String> categories = new java.util.ArrayList<>();
        categories.add("All"); // Always add "All" first
        categories.addAll(storeCategories); // Add store categories
        
        // Icon mapping for categories
        java.util.Map<String, String> categoryIcons = new java.util.HashMap<>();
        categoryIcons.put("All", "⭐");
        categoryIcons.put("Coffee", "☕");
        categoryIcons.put("Milk Tea", "✨");
        categoryIcons.put("Frappe", "🧊");
        categoryIcons.put("Fruit Tea", "🍓");
        categoryIcons.put("Pastries", "🍰");
        
        for (String category : categories) {
            String icon = categoryIcons.getOrDefault(category, "📦"); // Default icon if not in map
            Button categoryBtn = createCategoryButton(category, icon);
            categoryContainer.getChildren().add(categoryBtn);
        }
        
        sidebar.getChildren().addAll(header, categoryContainer);
        return sidebar;
    }
    
    private Button createCategoryButton(String category, String icon) {
        Button btn = new Button();
        btn.setPrefWidth(340);
        btn.setPrefHeight(56);
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
        // remove any previous selection classes
        btn.getStyleClass().remove("category-selected");
        iconLabel.getStyleClass().remove("category-icon-selected");
        textLabel.getStyleClass().remove("category-text-selected");

        if (isSelected) {
            // Apply selected classes (CSS handles colors); also set a slightly larger, semibold text
            if (!btn.getStyleClass().contains("category-selected")) btn.getStyleClass().add("category-selected");
            if (!iconLabel.getStyleClass().contains("category-icon-selected")) iconLabel.getStyleClass().add("category-icon-selected");
            if (!textLabel.getStyleClass().contains("category-text-selected")) textLabel.getStyleClass().add("category-text-selected");
            iconLabel.setTextFill(Color.web("#FFFFFF"));
            textLabel.setTextFill(Color.web("#FFFFFF"));
            textLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
            // Ensure no inline hover style blocks selected appearance
            btn.setOnMouseEntered(e -> {});
            btn.setOnMouseExited(e -> {});
        } else {
            // Non-selected: transparent background and normal weights
            btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 0; -fx-border-width: 0; -fx-border-radius: 0;");
            iconLabel.setTextFill(Color.web("#1A1A1A"));
            textLabel.setTextFill(Color.web("#1A1A1A"));
            textLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));

            // Hover effect only for non-selected buttons; when hovered, lighten background
            btn.setOnMouseEntered(e -> {
                if (!selectedCategory.equals(textLabel.getText())) {
                    btn.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 0; -fx-border-width: 0; -fx-border-radius: 0;");
                    iconLabel.setTextFill(Color.web("#1A1A1A"));
                    textLabel.setTextFill(Color.web("#1A1A1A"));
                }
            });

            btn.setOnMouseExited(e -> {
                // Re-evaluate selection dynamically
                updateCategoryButtonStyle(btn, selectedCategory.equals(textLabel.getText()));
            });
        }
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
        menuContainer.setPadding(new Insets(30, 30, 80, 30));
        menuContainer.setStyle("-fx-background-color: #FFFFFF;");
        menuContainer.setMinHeight(1600); // increased height to fit all product cards without cutting off
        menuContainer.setMaxWidth(1000); // constrain width so it's centered nicely
        
        // Header row with show sidebar button and category title
        HBox menuHeader = new HBox(15);
        menuHeader.setAlignment(Pos.CENTER_LEFT);
        
        if (!sidebarVisible) {
            Button showSidebarBtn = new Button("☰ Categories");
            showSidebarBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
            showSidebarBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1A1A1A; -fx-background-radius: 0; -fx-padding: 10 16; -fx-cursor: hand; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 0;");
            showSidebarBtn.setOnMouseEntered(e -> showSidebarBtn.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #1A1A1A; -fx-background-radius: 0; -fx-padding: 10 16; -fx-cursor: hand; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-border-radius: 0;"));
            showSidebarBtn.setOnMouseExited(e -> showSidebarBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1A1A1A; -fx-background-radius: 0; -fx-padding: 10 16; -fx-cursor: hand; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 0;"));
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
        
        // Wrap the menuContainer in an outer VBox to center it horizontally inside the ScrollPane
        VBox outer = new VBox();
        outer.setAlignment(Pos.TOP_CENTER);
        outer.getChildren().add(menuContainer);

        ScrollPane scrollPane = new ScrollPane(outer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
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
        if ("All".equals(category)) {
            return true;
        }
        
        String productCategory = product.getCategory();
        if (productCategory == null) {
            return false;
        }
        
        return productCategory.equalsIgnoreCase(category);
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-cursor: hand;");
        
        // Responsive sizing based on sidebar visibility (expanded)
        int cardWidth = sidebarVisible ? 260 : 360;
        int cardHeight = sidebarVisible ? 320 : 380;
        
        card.setPrefWidth(cardWidth);
        card.setPrefHeight(cardHeight);

        // Product image with real image support
        StackPane imagePane = new StackPane();
        int imageHeight = sidebarVisible ? 180 : 240;
        imagePane.setPrefHeight(imageHeight);
        imagePane.setMaxHeight(imageHeight);
        imagePane.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 0; -fx-border-radius: 0;");
        
        // Try to load product image
        javafx.scene.image.ImageView productImage = loadProductImage(product.getId());
        if (productImage != null) {
            productImage.setFitHeight(imageHeight);
            productImage.setFitWidth(cardWidth);
            productImage.setPreserveRatio(false);
            imagePane.getChildren().add(productImage);
        } else {
            // Fallback to emoji if no image
            String gradient = product.getName().toLowerCase().contains("espresso") ? 
                "linear-gradient(to bottom right, #2C2C2C, #1A1A1A)" :
                product.getName().toLowerCase().contains("cappuccino") ?
                "linear-gradient(to bottom right, #404040, #2C2C2C)" :
                "linear-gradient(to bottom right, #505050, #2C2C2C)";
            imagePane.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 0; -fx-border-radius: 0;");
            
            Label imagePlaceholder = new Label("☕");
            int emojiSize = sidebarVisible ? 64 : 88;
            imagePlaceholder.setFont(Font.font("Segoe UI Emoji", emojiSize));
            imagePlaceholder.setTextFill(Color.web("#F5EFE7"));
            imagePane.getChildren().add(imagePlaceholder);
        }

        // Content area - simplified (responsive padding)
        VBox contentBox = new VBox(sidebarVisible ? 12 : 18);
        contentBox.setPadding(new Insets(sidebarVisible ? 18 : 24));
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

        contentBox.getChildren().addAll(nameLabel, priceLabel);
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
                card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5); -fx-cursor: hand;");
            }
        });
        
        card.setOnMouseExited(e -> {
            if (product.getStock() > 0) {
                card.setStyle("-fx-background-color: white; -fx-background-radius: 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-cursor: hand;");
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
        
        Scene customScene = new Scene(customizationLayout, 1280, 900);
        setScenePreserveWindowSize(customScene);
        primaryStage.setTitle("Customize " + product.getName() + " - Coffee Shop Kiosk");
        applyAtlantafx(customScene);
    }
    
    private VBox createCustomizationHeader(Product product) {
        VBox header = new VBox(15);
        header.setPadding(new Insets(30, 40, 20, 40));
        header.getStyleClass().add("custom-header");
        
        // Back button and title row
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        // Dynamic back button text and behavior based on whether we're editing or adding new
        String backText = customizingOrderItem != null ? "← Back to Cart" : "← Back to Menu";
        Button backBtn = new Button(backText);
        backBtn.getStyleClass().add("primary-button");
        backBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        
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
        
        titleRow.getChildren().addAll(backBtn, spacer, titleLabel, new Region());
        
        // Product info
        String infoText = customizingOrderItem != null ? 
            "₱" + String.format("%.2f", product.getPrice()) + " • Update your drink preferences" :
            "₱" + String.format("%.2f", product.getPrice()) + " • " + product.getStock() + " available";
        Label productInfo = new Label(infoText);
        productInfo.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        productInfo.getStyleClass().add("info");
        
        header.getChildren().addAll(titleRow, productInfo);
        return header;
    }
    
    private ScrollPane createCustomizationContent(Product product) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background: -fx-dominant-bg; -fx-background-color: -fx-dominant-bg;");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 30, 30, 30));
        
        // Product image and basic info
        VBox productSection = new VBox(12);
        productSection.getStyleClass().add("card");
        productSection.setPadding(new Insets(18));
        productSection.setAlignment(Pos.CENTER_LEFT);
        productSection.setMaxWidth(Double.MAX_VALUE);

        // Large product banner (wider, taller and centered content)
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(220);
        imagePane.setMaxWidth(Double.MAX_VALUE);
        String gradient = product.getName().toLowerCase().contains("espresso") ? 
            "linear-gradient(to bottom right, #2C2C2C, #1A1A1A)" :
            product.getName().toLowerCase().contains("cappuccino") ?
            "linear-gradient(to bottom right, #404040, #2C2C2C)" :
            "linear-gradient(to bottom right, #505050, #2C2C2C)";
        imagePane.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 0; -fx-border-radius: 0; -fx-padding: 24 24 24 24;");

        // Try to load actual product image
        javafx.scene.image.ImageView productImage = loadProductImage(product.getId());
        if (productImage != null) {
            productImage.setFitHeight(220);
            productImage.setPreserveRatio(true);
            imagePane.getChildren().add(productImage);
        } else {
            // Fallback to emoji if no image
            Label imagePlaceholder = new Label("☕");
            imagePlaceholder.setFont(Font.font("Segoe UI Emoji", 84));
            imagePlaceholder.setTextFill(Color.web("#F5EFE7"));
            imagePane.getChildren().add(imagePlaceholder);
        }

        Label productName = new Label(product.getName());
        productName.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 18));
        productName.setTextFill(Color.web("#1A1A1A"));
        productName.setPadding(new Insets(12, 0, 0, 0));

        productSection.getChildren().addAll(imagePane, productName);
        
        // Customization options (main form) and suggestions
        // Shared property to allow suggestion checkboxes to contribute to the visible total
        javafx.beans.property.DoubleProperty suggestionsExtra = new javafx.beans.property.SimpleDoubleProperty(0.0);
        javafx.collections.ObservableList<Product> selectedSuggestions = javafx.collections.FXCollections.observableArrayList();
        VBox customizationSection = createCustomizationOptions(product, suggestionsExtra, selectedSuggestions);
        HBox mainRow = new HBox(28);
        mainRow.setAlignment(Pos.TOP_LEFT);
        mainRow.setMaxWidth(Double.MAX_VALUE);

        // Left: form (takes most of the space)
        VBox leftCol = customizationSection;
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        // Right: suggestions
        VBox rightCol = new VBox(12);
        rightCol.setPrefWidth(360);
        rightCol.getChildren().addAll(new Label("You may also like"), createSuggestionRow(product, suggestionsExtra, selectedSuggestions));

        mainRow.getChildren().addAll(leftCol, rightCol);

        content.getChildren().addAll(productSection, mainRow);
        scrollPane.setContent(content);
        
        return scrollPane;
    }
    
    private VBox createCustomizationOptions(Product product, javafx.beans.property.DoubleProperty suggestionsExtra, javafx.collections.ObservableList<Product> selectedSuggestions) {
        VBox customSection = new VBox(14);
        customSection.getStyleClass().add("card");
        customSection.setPadding(new Insets(18));
        
        // Temperature selection
        VBox tempSection = new VBox(8);
        Label tempTitle = new Label("Temperature");
        tempTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        
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
        
        HBox sugarButtons = new HBox(8);
        ToggleGroup sugarGroup = new ToggleGroup();
        
        String[] sugarLevels = {"0%", "25%", "50%", "75%", "100%"};
        RadioButton[] sugarBtns = new RadioButton[5];
        
        for (int i = 0; i < sugarLevels.length; i++) {
            sugarBtns[i] = new RadioButton(sugarLevels[i]);
            sugarBtns[i].setToggleGroup(sugarGroup);
            sugarBtns[i].setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
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
        
        VBox addOnsList = new VBox(10);

        // Add-on rows: checkbox + quantity controls
        CheckBox extraShotCheck = createStyledCheckBox("Extra Shot (+₱1.00)");
        final int[] extraShotQty = {1};
        HBox extraRow = new HBox(8);
        extraRow.setAlignment(Pos.CENTER_LEFT);
        Label extraQtyLabel = new Label(String.valueOf(extraShotQty[0]));
        extraQtyLabel.getStyleClass().add("qty-label");
        Button extraMinus = new Button("−");
        extraMinus.getStyleClass().add("qty-button");
        extraMinus.setOnAction(ev -> { if (extraShotQty[0] > 1) { extraShotQty[0]--; extraQtyLabel.setText(String.valueOf(extraShotQty[0])); } });
        Button extraPlus = new Button("+");
        extraPlus.getStyleClass().add("qty-button");
        extraPlus.setOnAction(ev -> { extraShotQty[0]++; extraQtyLabel.setText(String.valueOf(extraShotQty[0])); });
        extraRow.getChildren().addAll(extraShotCheck, extraMinus, extraQtyLabel, extraPlus);

        CheckBox whippedCreamCheck = createStyledCheckBox("Whipped Cream (+₱0.50)");
        final int[] whippedQty = {1};
        HBox whipRow = new HBox(8);
        whipRow.setAlignment(Pos.CENTER_LEFT);
        Button whipMinus = new Button("−");
        whipMinus.getStyleClass().add("qty-button");
        Button whipPlus = new Button("+");
        whipPlus.getStyleClass().add("qty-button");
        Label whipQtyLabel = new Label(String.valueOf(whippedQty[0]));
        whipQtyLabel.getStyleClass().add("qty-label");
        whipMinus.setOnAction(ev -> { if (whippedQty[0] > 1) { whippedQty[0]--; whipQtyLabel.setText(String.valueOf(whippedQty[0])); } });
        whipPlus.setOnAction(ev -> { whippedQty[0]++; whipQtyLabel.setText(String.valueOf(whippedQty[0])); });
        whipRow.getChildren().addAll(whippedCreamCheck, whipMinus, whipQtyLabel, whipPlus);

        CheckBox vanillaSyrupCheck = createStyledCheckBox("Vanilla Syrup (+₱0.75)");
        final int[] vanillaQty = {1};
        HBox vanillaRow = new HBox(8);
        vanillaRow.setAlignment(Pos.CENTER_LEFT);
        Button vanMinus = new Button("−");
        vanMinus.getStyleClass().add("qty-button");
        vanMinus.setOnMouseEntered(e -> vanMinus.setStyle("-fx-background-color: #F0F0F0; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #1A1A1A; -fx-border-radius: 6; -fx-background-radius: 6;"));
        vanMinus.setOnMouseExited(e -> vanMinus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;"));
        Button vanPlus = new Button("+");
        vanPlus.setPrefSize(30, 30);
        vanPlus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;");
        vanPlus.setOnMouseEntered(e -> vanPlus.setStyle("-fx-background-color: #F0F0F0; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #1A1A1A; -fx-border-radius: 6; -fx-background-radius: 6;"));
        vanPlus.setOnMouseExited(e -> vanPlus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;"));
        Label vanQtyLabel = new Label(String.valueOf(vanillaQty[0]));
        vanMinus.setOnAction(ev -> { if (vanillaQty[0] > 1) { vanillaQty[0]--; vanQtyLabel.setText(String.valueOf(vanillaQty[0])); } });
        vanPlus.setOnAction(ev -> { vanillaQty[0]++; vanQtyLabel.setText(String.valueOf(vanillaQty[0])); });
        vanillaRow.getChildren().addAll(vanillaSyrupCheck, vanMinus, vanQtyLabel, vanPlus);

        CheckBox caramelSyrupCheck = createStyledCheckBox("Caramel Syrup (+₱0.75)");
        final int[] caramelQty = {1};
        HBox caramelRow = new HBox(8);
        caramelRow.setAlignment(Pos.CENTER_LEFT);
        Button carMinus = new Button("−");
        carMinus.setPrefSize(30, 30);
        carMinus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;");
        carMinus.setOnMouseEntered(e -> carMinus.setStyle("-fx-background-color: #F0F0F0; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #1A1A1A; -fx-border-radius: 6; -fx-background-radius: 6;"));
        carMinus.setOnMouseExited(e -> carMinus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;"));
        Button carPlus = new Button("+");
        carPlus.setPrefSize(30, 30);
        carPlus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;");
        carPlus.setOnMouseEntered(e -> carPlus.setStyle("-fx-background-color: #F0F0F0; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #1A1A1A; -fx-border-radius: 6; -fx-background-radius: 6;"));
        carPlus.setOnMouseExited(e -> carPlus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;"));
        Label carQtyLabel = new Label(String.valueOf(caramelQty[0]));
        carMinus.setOnAction(ev -> { if (caramelQty[0] > 1) { caramelQty[0]--; carQtyLabel.setText(String.valueOf(caramelQty[0])); } });
        carPlus.setOnAction(ev -> { caramelQty[0]++; carQtyLabel.setText(String.valueOf(caramelQty[0])); });
        caramelRow.getChildren().addAll(caramelSyrupCheck, carMinus, carQtyLabel, carPlus);

        CheckBox chocolateSyrupCheck = createStyledCheckBox("Chocolate Syrup (+₱0.75)");
        final int[] chocolateQty = {1};
        HBox chocRow = new HBox(8);
        chocRow.setAlignment(Pos.CENTER_LEFT);
        Button chocMinus = new Button("−");
        chocMinus.setPrefSize(30, 30);
        chocMinus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;");
        chocMinus.setOnMouseEntered(e -> chocMinus.setStyle("-fx-background-color: #F0F0F0; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #1A1A1A; -fx-border-radius: 6; -fx-background-radius: 6;"));
        chocMinus.setOnMouseExited(e -> chocMinus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;"));
        Button chocPlus = new Button("+");
        chocPlus.setPrefSize(30, 30);
        chocPlus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;");
        chocPlus.setOnMouseEntered(e -> chocPlus.setStyle("-fx-background-color: #F0F0F0; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #1A1A1A; -fx-border-radius: 6; -fx-background-radius: 6;"));
        chocPlus.setOnMouseExited(e -> chocPlus.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 14; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;"));
        Label chocQtyLabel = new Label(String.valueOf(chocolateQty[0]));
        chocMinus.setOnAction(ev -> { if (chocolateQty[0] > 1) { chocolateQty[0]--; chocQtyLabel.setText(String.valueOf(chocolateQty[0])); } });
        chocPlus.setOnAction(ev -> { chocolateQty[0]++; chocQtyLabel.setText(String.valueOf(chocolateQty[0])); });
        chocRow.getChildren().addAll(chocolateSyrupCheck, chocMinus, chocQtyLabel, chocPlus);

        // Pre-select add-ons if editing existing item
        if (customizingOrderItem != null && customizingOrderItem.getAddOns() != null) {
            String addOns = customizingOrderItem.getAddOns();
            extraShotCheck.setSelected(addOns.contains("Extra Shot"));
            whippedCreamCheck.setSelected(addOns.contains("Whipped Cream"));
            vanillaSyrupCheck.setSelected(addOns.contains("Vanilla Syrup"));
            caramelSyrupCheck.setSelected(addOns.contains("Caramel Syrup"));
            chocolateSyrupCheck.setSelected(addOns.contains("Chocolate Syrup"));
        }

        addOnsList.getChildren().addAll(extraRow, whipRow, vanillaRow, caramelRow, chocRow);
        
        addOnsSection.getChildren().addAll(addOnsTitle, addOnsList);
        
        // Quantity and Add button
        VBox bottomSection = new VBox(12);
        
        // Quantity selector (compact)
        HBox qtySection = new HBox(6);
        qtySection.setAlignment(Pos.CENTER_LEFT);
        // Remove background so the quantity selector is flush with the card
        qtySection.setStyle("-fx-padding: 6 8;");
        
        Label qtyLabel = new Label("Quantity:");
        qtyLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        qtyLabel.setTextFill(Color.web("#1A1A1A"));
        
        Button minusBtn = new Button("−");
        minusBtn.setPrefSize(34, 34);
        minusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;");
        minusBtn.setOnMouseEntered(e -> minusBtn.setStyle("-fx-background-color: #F0F0F0; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #1A1A1A; -fx-border-radius: 6; -fx-background-radius: 6;"));
        minusBtn.setOnMouseExited(e -> minusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;"));
        
        Label qtyValueLabel = new Label("1");
        qtyValueLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        qtyValueLabel.setTextFill(Color.web("#1A1A1A"));
        qtyValueLabel.setPrefWidth(36);
        qtyValueLabel.setAlignment(Pos.CENTER);
        
        Button plusBtn = new Button("+");
        plusBtn.setPrefSize(34, 34);
        plusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;");
        plusBtn.setOnMouseEntered(e -> plusBtn.setStyle("-fx-background-color: #F0F0F0; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #1A1A1A; -fx-border-radius: 6; -fx-background-radius: 6;"));
        plusBtn.setOnMouseExited(e -> plusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;"));
        
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
        addButton.setPrefSize(280, 48);
        addButton.getStyleClass().add("primary-button");
        addButton.setOnMouseEntered(e -> addButton.setStyle("-fx-background-color: #2C2C2C; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 0; -fx-cursor: hand;"));
        addButton.setOnMouseExited(e -> addButton.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 0; -fx-cursor: hand;"));
        
        // Label to show live total on the right of the Add button
        Label liveTotal = new Label("₱0.00");
        liveTotal.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        liveTotal.setTextFill(Color.web("#1A1A1A"));

        // Helper to recompute totals (reads current UI state)
        Runnable recompute = () -> {
            double addOnsCost = 0.0;
            if (extraShotCheck.isSelected()) addOnsCost += 1.00 * extraShotQty[0];
            if (whippedCreamCheck.isSelected()) addOnsCost += 0.50 * whippedQty[0];
            if (vanillaSyrupCheck.isSelected()) addOnsCost += 0.75 * vanillaQty[0];
            if (caramelSyrupCheck.isSelected()) addOnsCost += 0.75 * caramelQty[0];
            if (chocolateSyrupCheck.isSelected()) addOnsCost += 0.75 * chocolateQty[0];

            double suggestions = suggestionsExtra.get();
            double base = product.getPrice();
            double subtotal = (base + addOnsCost) * quantity[0] + suggestions;
            javafx.application.Platform.runLater(() -> liveTotal.setText(String.format("₱%.2f", subtotal)));
        };

        // attach recompute listeners to controls that affect price
        extraShotCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        whippedCreamCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        vanillaSyrupCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        caramelSyrupCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        chocolateSyrupCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());

        // quantity changes should also recompute
        plusBtn.setOnAction(e -> {
            if (quantity[0] < product.getStock()) {
                quantity[0]++;
                qtyValueLabel.setText(String.valueOf(quantity[0]));
                recompute.run();
            }
        });
        minusBtn.setOnAction(e -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                qtyValueLabel.setText(String.valueOf(quantity[0]));
                recompute.run();
            }
        });

        // Observe suggestionsExtra so recompute updates when suggestion checkboxes change
        suggestionsExtra.addListener((obs, o, n) -> recompute.run());
        // initialize displayed total
        recompute.run();

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
                
                // Calculate add-ons (recompute same values used by live total)
                StringBuilder addOnsText = new StringBuilder();
                double addOnsCost = 0.0;

                if (extraShotCheck.isSelected()) {
                    addOnsText.append("Extra Shot");
                    addOnsCost += 1.00 * extraShotQty[0];
                    if (extraShotQty[0] > 1) addOnsText.append(" x").append(extraShotQty[0]);
                }
                if (whippedCreamCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Whipped Cream");
                    addOnsCost += 0.50 * whippedQty[0];
                    if (whippedQty[0] > 1) addOnsText.append(" x").append(whippedQty[0]);
                }
                if (vanillaSyrupCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Vanilla Syrup");
                    addOnsCost += 0.75 * vanillaQty[0];
                    if (vanillaQty[0] > 1) addOnsText.append(" x").append(vanillaQty[0]);
                }
                if (caramelSyrupCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Caramel Syrup");
                    addOnsCost += 0.75 * caramelQty[0];
                    if (caramelQty[0] > 1) addOnsText.append(" x").append(caramelQty[0]);
                }
                if (chocolateSyrupCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Chocolate Syrup");
                    addOnsCost += 0.75 * chocolateQty[0];
                    if (chocolateQty[0] > 1) addOnsText.append(" x").append(chocolateQty[0]);
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
                    // If any suggested products were selected, add them as separate items
                    if (selectedSuggestions != null && !selectedSuggestions.isEmpty()) {
                        for (Product sp : selectedSuggestions) {
                            OrderItem sitem = new OrderItem(sp, 1, "Hot", 50);
                            sitem.setAddOnsCost(0.0);
                            currentOrder.addItem(sitem);
                        }
                        // clear selection cost tracker
                        suggestionsExtra.set(0.0);
                        selectedSuggestions.clear();
                    }
                    
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
                    // Add any selected suggestions to the order as separate items
                    if (selectedSuggestions != null && !selectedSuggestions.isEmpty()) {
                        for (Product sp : selectedSuggestions) {
                            OrderItem sitem = new OrderItem(sp, 1, "Hot", 50);
                            sitem.setAddOnsCost(0.0);
                            currentOrder.addItem(sitem);
                        }
                        suggestionsExtra.set(0.0);
                        selectedSuggestions.clear();
                    }
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
                // Log minimal stack info without using printStackTrace()
                System.err.println(java.util.Arrays.toString(ex.getStackTrace()).replaceAll("\n", " "));

                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to process item");
                errorAlert.setContentText("There was an error processing the item: " + ex.getMessage());
                errorAlert.showAndWait();
            }
        });
        
        // Layout: compact quantity on left, add button then live total on right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox totalBox = new HBox(12, addButton, liveTotal);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.setPadding(new Insets(0, 0, 0, 8));
        bottomSection.getChildren().addAll(qtySection, spacer, totalBox);
        bottomSection.setAlignment(Pos.CENTER_LEFT);
        
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

    private HBox createSuggestionRow(Product currentProduct, javafx.beans.property.DoubleProperty suggestionsExtra, javafx.collections.ObservableList<Product> selectedSuggestions) {
        // Build a vertical list of suggestion rows (one-per-line, full-width)
        VBox container = new VBox(12);
        container.setPadding(new Insets(8, 0, 8, 0));
        container.setAlignment(Pos.TOP_LEFT);

        int added = 0;
        for (Product p : store.getProducts()) {
            if (added >= 3) break;
            if (p.getName().equals(currentProduct.getName())) continue;
            if (p.getStock() <= 0) continue;

            HBox cardRow = new HBox(12);
            cardRow.setPadding(new Insets(10));
            cardRow.setAlignment(Pos.CENTER_LEFT);
            cardRow.setStyle("-fx-background-color: white; -fx-background-radius: 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);");
            cardRow.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cardRow, Priority.ALWAYS);

            // Image thumbnail
            StackPane img = new StackPane();
            img.setPrefSize(110, 90);
            String grad = p.getName().toLowerCase().contains("espresso") ? "linear-gradient(to bottom right, #2C2C2C, #1A1A1A)" : "linear-gradient(to bottom right, #505050, #2C2C2C)";
            img.setStyle("-fx-background-color: " + grad + "; -fx-background-radius: 0; -fx-border-radius: 0;");
            Label emoji = new Label("☕");
            emoji.setFont(Font.font("Segoe UI Emoji", 28));
            emoji.setTextFill(Color.web("#F5EFE7"));
            img.getChildren().add(emoji);

            // Middle: title and unit price
            VBox mid = new VBox(6);
            mid.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(p.getName());
            name.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
            name.setTextFill(Color.web("#1A1A1A"));

            Label unitPrice = new Label(String.format("₱%.2f", p.getPrice()));
            unitPrice.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            unitPrice.setTextFill(Color.web("#757575"));

            mid.getChildren().addAll(name, unitPrice);

            // Spacer to push qty/total to the right
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Right: checkbox, quantity controls and total price (total under qty, centered vertically)
            VBox rightBox = new VBox(6);
            rightBox.setAlignment(Pos.CENTER);

            CheckBox addChk = new CheckBox();
            addChk.setTooltip(new javafx.scene.control.Tooltip("Add this suggestion"));
            addChk.setStyle("-fx-cursor: hand;");

            // Quantity controls
            HBox qtyBox = new HBox(6);
            qtyBox.setAlignment(Pos.CENTER);
            // make qty controls transparent (no background)
            qtyBox.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            Button minus = new Button("−");
            minus.setPrefSize(30, 30);
            minus.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
            Button plus = new Button("+");
            plus.setPrefSize(30, 30);
            plus.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
            final int[] qty = {1};
            Label qtyLabel = new Label(String.valueOf(qty[0]));
            qtyLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
            qtyLabel.setPrefWidth(28);
            qtyLabel.setAlignment(Pos.CENTER);
            
            // Total price label (qty * unit price) - declare early so handlers can reference it
            Label totalPriceLabel = new Label(String.format("₱%.2f", p.getPrice() * qty[0]));
            totalPriceLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            totalPriceLabel.setTextFill(Color.web("#1A1A1A"));

            minus.setOnAction(ev -> {
                if (qty[0] > 1) {
                    qty[0]--;
                    qtyLabel.setText(String.valueOf(qty[0]));
                    if (addChk.isSelected()) {
                        double delta = -p.getPrice();
                        suggestionsExtra.set(suggestionsExtra.get() + delta);
                    }
                    totalPriceLabel.setText(String.format("₱%.2f", p.getPrice() * qty[0]));
                }
            });
            plus.setOnAction(ev -> {
                if (qty[0] < p.getStock()) {
                    qty[0]++;
                    qtyLabel.setText(String.valueOf(qty[0]));
                    if (addChk.isSelected()) {
                        double delta = p.getPrice();
                        suggestionsExtra.set(suggestionsExtra.get() + delta);
                    }
                    totalPriceLabel.setText(String.format("₱%.2f", p.getPrice() * qty[0]));
                }
            });

            qtyBox.getChildren().addAll(minus, qtyLabel, plus);
            // When checkbox toggles, update suggestionsExtra and selectedSuggestions
            addChk.selectedProperty().addListener((obs, oldV, newV) -> {
                double delta = newV ? p.getPrice() * qty[0] : -p.getPrice() * qty[0];
                suggestionsExtra.set(suggestionsExtra.get() + delta);
                if (newV) {
                    if (!selectedSuggestions.contains(p)) selectedSuggestions.add(p);
                } else {
                    selectedSuggestions.remove(p);
                }
            });

            rightBox.getChildren().addAll(addChk, qtyBox, totalPriceLabel);

            // Build the row
            cardRow.getChildren().addAll(img, mid, spacer, rightBox);
            container.getChildren().add(cardRow);
            added++;
        }

        // Wrap into an HBox to keep method signature compatibility (caller expects a Node)
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.getChildren().add(container);
        return wrapper;
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
        
        Scene cartScene = new Scene(cartLayout, 1280, 900);
        setScenePreserveWindowSize(cartScene);
        primaryStage.setTitle("Your Cart - Coffee Shop Kiosk");
        applyAtlantafx(cartScene);
    }
    
    private VBox createCartHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(30, 40, 20, 40));
        header.setStyle("-fx-background-color: #1A1A1A; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        
        // Back button and title row
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Button backBtn = new Button("← Back to Menu");
        backBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: #5D4037; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;"));
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
        itemCard.setStyle("-fx-background-color: white; -fx-background-radius: 0; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
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
        // Remove background so quantity controls are flush and minimal
        controls.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        Button minusBtn = new Button("−");
        minusBtn.setPrefSize(32, 32);
        minusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: transparent; -fx-background-radius: 6; -fx-font-size: 16px; -fx-text-fill: #757575; -fx-cursor: hand;");
        minusBtn.setOnMouseEntered(e -> minusBtn.setStyle("-fx-background-color: #F0F0F0; -fx-border-color: transparent; -fx-background-radius: 6; -fx-font-size: 16px; -fx-text-fill: #1A1A1A; -fx-cursor: hand;"));
        minusBtn.setOnMouseExited(e -> minusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: transparent; -fx-background-radius: 6; -fx-font-size: 16px; -fx-text-fill: #757575; -fx-cursor: hand;"));

        Label qtyLabel = new Label(String.valueOf(totalQty));
        qtyLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        qtyLabel.setTextFill(Color.web("#1A1A1A"));
        qtyLabel.setPrefWidth(32);
        qtyLabel.setAlignment(Pos.CENTER);

        Button plusBtn = new Button("+");
        plusBtn.setPrefSize(32, 32);
        plusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: transparent; -fx-background-radius: 6; -fx-font-size: 16px; -fx-text-fill: #757575; -fx-cursor: hand;");
        plusBtn.setOnMouseEntered(e -> plusBtn.setStyle("-fx-background-color: #F0F0F0; -fx-border-color: transparent; -fx-background-radius: 6; -fx-font-size: 16px; -fx-text-fill: #1A1A1A; -fx-cursor: hand;"));
        plusBtn.setOnMouseExited(e -> plusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: transparent; -fx-background-radius: 6; -fx-font-size: 16px; -fx-text-fill: #757575; -fx-cursor: hand;"));

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
        customizeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 0; -fx-background-radius: 0; -fx-font-size: 13px; -fx-text-fill: #757575; -fx-cursor: hand;");
        customizeBtn.setOnMouseEntered(e -> customizeBtn.setStyle("-fx-background-color: #F0F0F0; -fx-border-color: #D0D0D0; -fx-border-width: 1; -fx-border-radius: 0; -fx-background-radius: 0; -fx-font-size: 13px; -fx-text-fill: #1A1A1A; -fx-cursor: hand;"));
        customizeBtn.setOnMouseExited(e -> customizeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 0; -fx-background-radius: 0; -fx-font-size: 13px; -fx-text-fill: #757575; -fx-cursor: hand;"));

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
        continueShoppingBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;");
        continueShoppingBtn.setOnMouseEntered(e -> continueShoppingBtn.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: normal; -fx-background-radius: 0; -fx-cursor: hand;"));
        continueShoppingBtn.setOnMouseExited(e -> continueShoppingBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;"));
        continueShoppingBtn.setOnAction(e -> showMenuScreen());
        
        Button checkoutBtn = new Button("Proceed to Checkout");
        checkoutBtn.setPrefSize(250, 60);
        checkoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;");
        checkoutBtn.setOnMouseEntered(e -> checkoutBtn.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;"));
        checkoutBtn.setOnMouseExited(e -> checkoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;"));
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
        
        Scene checkoutScene = new Scene(checkoutLayout, 1280, 900);
        setScenePreserveWindowSize(checkoutScene);
        primaryStage.setTitle("Checkout - Coffee Shop Kiosk");
        applyAtlantafx(checkoutScene);
    }
    
    private VBox createCheckoutHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(30, 40, 20, 40));
        header.setStyle("-fx-background-color: #1A1A1A; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        
        // Back button and title row
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Button backBtn = new Button("← Back to Cart");
        backBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: #5D4037; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: #795548; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;"));
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
        
        // Order type is selected on the homepage; no radios shown here.
        // (Homepage selection is stored in `orderType`.)
        
        // Order summary
        VBox summarySection = createOrderSummarySection();
        
        // Payment buttons
        VBox paymentSection = createPaymentSection();
        
        content.getChildren().addAll(summarySection, paymentSection);
        return content;
    }
    
    private VBox createOrderSummarySection() {
        VBox summarySection = new VBox(15);
        summarySection.setStyle("-fx-background-color: white; -fx-background-radius: 0; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        summarySection.setPadding(new Insets(25));
        summarySection.setAlignment(Pos.CENTER);
        summarySection.setMaxWidth(500);
        
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
        cashBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;");
        cashBtn.setOnMouseEntered(e -> cashBtn.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;"));
        cashBtn.setOnMouseExited(e -> cashBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 0; -fx-cursor: hand;"));
        cashBtn.setOnAction(e -> completeOrder("Cash"));
        
        paymentButtons.getChildren().add(cashBtn);
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
            
            // Persist a pending order so the cashier can pick it up
            try {
                // Build PendingOrder from currentOrder
                PendingOrder p = new PendingOrder(currentOrder.getOrderId(), "Guest", (orderType != null && !orderType.isEmpty()) ? orderType : "Dine In");
                for (OrderItem oi : currentOrder.getItems()) {
                    double price = oi.getProduct().getPrice() + oi.getAddOnsCost();
                    p.addItem(oi.getProduct().getName(), price, oi.getQuantity(), oi.getTemperature(), oi.getSugarLevel());
                }
                TextDatabase.savePendingOrder(p);
            } catch (Exception ex) {
                System.err.println("Error saving pending order: " + ex.getMessage());
            }

            // Also save a receipt record for bookkeeping
            try {
                String rid = UUID.randomUUID().toString().substring(0, 8);
                Receipt receipt = new Receipt(rid, currentOrder.getOrderId(), "Guest", currentOrder.getTotalAmount(), currentOrder.getTotalAmount(), 0.0);
                // Include printable content for convenience
                try {
                    receipt.setReceiptContent(currentOrder.printReceipt());
                } catch (Exception ign) {}
                TextDatabase.saveReceipt(receipt);
            } catch (Exception ex) {
                System.err.println("Error saving receipt: " + ex.getMessage());
            }

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

    // Load product image from data/images folder
    private javafx.scene.image.ImageView loadProductImage(String productId) {
        try {
            // Check cache first
            if (imageCache.containsKey(productId)) {
                javafx.scene.image.Image cached = imageCache.get(productId);
                if (cached != null) {
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(cached);
                    iv.setPreserveRatio(true);
                    return iv;
                }
            }

            java.io.File imagesDir = new java.io.File("data/images");
            if (!imagesDir.exists()) return null;

            // Build index once for faster subsequent lookups
            if (imageFileIndex == null) {
                imageFileIndex = new java.util.HashMap<>();
                java.io.File[] files = imagesDir.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.isFile() && !file.getName().startsWith(".")) {
                            String name = file.getName();
                            int dot = name.indexOf('.');
                            if (dot > 0) {
                                String id = name.substring(0, dot);
                                imageFileIndex.put(id, file);
                            }
                        }
                    }
                }
            }

            // Lookup by product id in the index
            java.io.File match = imageFileIndex.get(productId);
            if (match != null && match.exists()) {
                try {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(match.toURI().toString(), true);
                    imageCache.put(productId, image);
                    javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                    imageView.setPreserveRatio(true);
                    return imageView;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        return null;
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
