package com.coffeeshop;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

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
import java.util.List;
import java.util.ArrayList;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.nio.file.*;

public class CustomerApp extends Application {
    private Store store;
    // Track currently open customization product so it can be refreshed when admin updates special requests
    private Product currentCustomizationProduct = null;
    private Double customizationDefaultSize = null; // stores selected default size on card clicks
    private Order currentOrder;
    private OrderItem customizingOrderItem = null; // Track item being customized from cart
    private Stage primaryStage;
    private boolean keepMaximized = false; // remember we want windowed-fullscreen
    private HBox footerContainer;
    private Label footerItemsLabel;
    private Label footerTotalLabel;
    private Button footerViewCartBtn;
    private Button footerCheckoutBtn;
    private String orderType = ""; // "Dine In" or "Take Out"
    private Timeline inactivityTimer;
    private int countdownSeconds = 30;
    private Label countdownLabel;
    // Persistent container to avoid swapping entire Scene (prevents window flicker/minimize)
    private StackPane persistentRoot;
    private Scene persistentScene;
    // Flag to block async scene changes while receipt screen is showing
    private volatile boolean showingReceiptScreen = false;
    // Simple in-memory cache for product images to avoid repeated disk I/O and decoding
    private java.util.Map<String, javafx.scene.image.Image> imageCache = new java.util.HashMap<>();
    // Simple index for quick lookup of image files by product id (built once)
    private java.util.Map<String, java.io.File> imageFileIndex = null;
    // Sidebar category container reference so it can be refreshed when categories change
    private VBox categoryContainerSidebar = null;
    // Cashier ID - set via command line parameter or defaults to empty string (kiosk mode, visible to all cashiers)
    private String currentCashierId = "";


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        store = Store.getInstance();
        
        // Read cashier ID from parameters if provided
        java.util.List<String> params = getParameters().getRaw();
        if (!params.isEmpty()) {
            currentCashierId = params.get(0);
        }
        
        // Listen for category changes so customer UI updates in real-time
        try {
            Store.getInstance().addCategoryChangeListener(() -> {
                javafx.application.Platform.runLater(() -> {
                    try { refreshCategoriesSidebar(); } catch (Exception ignored) {}
                });
            });
            // Listen for product changes so customer UI updates in real-time when Admin edits products
            // Clear image caches/index so newly-copied images are discovered when Admin updates product images
            Store.getInstance().addProductChangeListener(() -> {
                javafx.application.Platform.runLater(() -> {
                    try {
                        try { imageCache.clear(); } catch (Exception ignored) {}
                        imageFileIndex = null; // force rebuild of index on next image load
                        refreshProductGrid();
                        // If customization dialog is open for a product, refresh it using the latest Product instance
                        if (currentCustomizationProduct != null) {
                            try {
                                Product updated = Store.getInstance().getProductById(currentCustomizationProduct.getId());
                                if (updated != null) showCustomizationPage(updated);
                            } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                });
            });
            // Listen for special-request changes so open customization dialogs update their quick-buttons
            Store.getInstance().addSpecialRequestChangeListener(() -> {
                javafx.application.Platform.runLater(() -> {
                    try {
                        if (currentCustomizationProduct != null) {
                            try {
                                Product updated = Store.getInstance().getProductById(currentCustomizationProduct.getId());
                                if (updated != null) showCustomizationPage(updated);
                            } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                });
            });
            // Listen for add-on changes so open customization dialogs update their add-on pills
            Store.getInstance().addAddOnChangeListener(() -> {
                javafx.application.Platform.runLater(() -> {
                    try {
                        if (currentCustomizationProduct != null) {
                            try {
                                Product updated = Store.getInstance().getProductById(currentCustomizationProduct.getId());
                                if (updated != null) showCustomizationPage(updated);
                            } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                });
            });
        } catch (Exception ignored) {}
        
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
        // Listen for maximize changes so we can tweak inner layout to avoid visible gaps
        try {
            primaryStage.maximizedProperty().addListener((obs, oldV, newV) -> {
                javafx.application.Platform.runLater(() -> {
                    if (persistentRoot != null && !persistentRoot.getChildren().isEmpty()) {
                        javafx.scene.Node cur = persistentRoot.getChildren().get(0);
                        if (cur instanceof Parent) adjustRootForWindowSize((Parent) cur);
                    }

                    // If the user restored from maximized to windowed, ensure the
                    // stage is centered/visible so the OS window controls are reachable.
                    try {
                        if (primaryStage != null && !primaryStage.isMaximized()) {
                            // Center on screen to avoid the window being positioned off-screen
                            primaryStage.centerOnScreen();
                            // Also ensure the Y position is not negative (put below taskbar)
                            if (primaryStage.getY() < 0) primaryStage.setY(24);
                        }
                    } catch (Exception ignored) {}
                });
            });
        } catch (Exception ignored) {}
        // Show welcome screen content inside the persistent scene
        showWelcomeScreen();
        // Start background watcher to detect external category changes (Admin edits)
        startCategoryFileWatcher();
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
                // Block if receipt screen is showing - don't let async calls overwrite it
                if (showingReceiptScreen) {
                    System.out.println("[DEBUG] setScenePreserveWindowSize - BLOCKED, receipt screen is showing");
                    return;
                }
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
            // Block if receipt screen is showing
            if (showingReceiptScreen) return;
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

    // Adjust certain layout constraints when window is maximized to avoid
    // large exterior gaps due to fixed max widths and large paddings.
    private void adjustRootForWindowSize(Parent root) {
        boolean isMax = primaryStage != null && primaryStage.isMaximized();
        try {
            adjustNodeRecursively(root, isMax);
        } catch (Exception ignored) {}
    }

    private void adjustNodeRecursively(javafx.scene.Node node, boolean isMax) {
        if (node instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region r = (javafx.scene.layout.Region) node;
            if (isMax) {
                // Expand constrained containers to use available width
                if (r.getMaxWidth() > 0 && r.getMaxWidth() < 1200) {
                    r.setMaxWidth(Double.MAX_VALUE);
                }
                // Reduce very large paddings for fullscreen so edges align with window
                try {
                    Insets p = r.getPadding();
                    if (p != null && (p.getTop() > 40 || p.getLeft() > 40)) {
                        r.setPadding(new Insets(12));
                    }
                } catch (Exception ignored) {}
            } else {
                // When returning to windowed mode, do not aggressively restore here;
                // original layout code will set intended paddings/max widths when scenes are rebuilt.
            }
        }

        if (node instanceof Parent) {
            for (javafx.scene.Node child : ((Parent) node).getChildrenUnmodifiable()) {
                adjustNodeRecursively(child, isMax);
            }
        }
    }
    
    /**
     * Show maintenance mode screen when system is under maintenance
     */
    private void showMaintenanceScreen() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");
        
        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(60));
        
        // Maintenance icon
        Label icon = new Label("🔧");
        icon.setFont(Font.font(100));
        
        Label title = new Label("System Under Maintenance");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        title.setTextFill(Color.web("#FF6B6B"));
        
        Label subtitle = new Label("The ordering kiosk is currently unavailable.\nPlease wait while our team performs maintenance.");
        subtitle.setFont(Font.font("Segoe UI", 18));
        subtitle.setTextFill(Color.web("#a0a0a0"));
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        subtitle.setWrapText(true);
        
        // Coffee cup icon
        Label coffeeIcon = new Label("☕");
        coffeeIcon.setFont(Font.font(60));
        coffeeIcon.setTextFill(Color.web("#a0a0a0"));
        
        Label waitLabel = new Label("Please Wait...");
        waitLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 16));
        waitLabel.setTextFill(Color.web("#a0a0a0"));
        
        // Auto-retry button
        Button retryBtn = new Button("🔄 Check Again");
        retryBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        retryBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-padding: 15 40; -fx-background-radius: 30; -fx-cursor: hand;");
        retryBtn.setOnMouseEntered(e -> retryBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 15 40; -fx-background-radius: 30; -fx-cursor: hand;"));
        retryBtn.setOnMouseExited(e -> retryBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-padding: 15 40; -fx-background-radius: 30; -fx-cursor: hand;"));
        retryBtn.setOnAction(e -> showWelcomeScreen());
        
        content.getChildren().addAll(icon, title, subtitle, coffeeIcon, waitLabel, retryBtn);
        root.getChildren().add(content);
        
        // Directly set on persistentRoot instead of using setScenePreserveWindowSize
        if (persistentRoot != null) {
            persistentRoot.getChildren().setAll(root);
        } else {
            Scene scene = new Scene(root, 1600, 900);
            setScenePreserveWindowSize(scene);
        }
        
        // Periodically check if maintenance mode has been disabled
        Timeline maintenanceChecker = new Timeline(new KeyFrame(Duration.seconds(10), ev -> {
            if (!TextDatabase.isMaintenanceMode()) {
                showWelcomeScreen();
            }
        }));
        maintenanceChecker.setCycleCount(Timeline.INDEFINITE);
        maintenanceChecker.play();
    }

    private void showWelcomeScreen() {
        // Reset all customization state when returning to welcome screen
        currentCustomizationProduct = null;
        customizingOrderItem = null;
        
        // Check if maintenance mode is enabled
        if (TextDatabase.isMaintenanceMode()) {
            showMaintenanceScreen();
            return;
        }
        
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

        // Take Out button
        VBox takeOutBox = createOptionCard("🛍", "Take Out", "Grab your coffee on the go");
        takeOutBox.setOnMouseClicked(e -> {
            orderType = "Take Out";
            showMenuScreen();
        });

        optionsBox.getChildren().addAll(dineInBox, takeOutBox);

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

    // Simple cart graphic composed from shapes so it's consistent across platforms
    private javafx.scene.Node createCartIcon(double size) {
        javafx.scene.layout.StackPane g = new javafx.scene.layout.StackPane();
        g.setPrefSize(size + 8, size + 8);

        javafx.scene.shape.Circle bg = new javafx.scene.shape.Circle((size + 8) / 2.0);
        bg.setFill(Color.web("#FFFFFF"));
        bg.setStroke(Color.web("#D0D0D0"));
        bg.setStrokeWidth(1.2);

        // Use a simple cart glyph using a Label to avoid SVG/font issues
        Label glyph = new Label("🛒");
        glyph.setFont(Font.font("Segoe UI Emoji", size));

        g.getChildren().addAll(bg, glyph);
        return g;
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
            currentOrder = new Order(TextDatabase.getNextOrderNumber());
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

        // Inactivity timer disabled per user request (was 30s).
        // To re-enable, call `startInactivityTimer()` and attach the reset handlers below.
        // scene.setOnMouseMoved(e -> resetInactivityTimer());
        // scene.setOnMouseClicked(e -> resetInactivityTimer());
        // scene.setOnKeyPressed(e -> resetInactivityTimer());
        // scene.setOnTouchPressed(e -> resetInactivityTimer());
        // scene.setOnScroll(e -> resetInactivityTimer());
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
        categoryContainerSidebar = new VBox(2);
        categoryContainerSidebar.setPadding(new Insets(0));
        
        // Get all categories from the store (dynamically managed via Admin)
        java.util.List<String> categories = new java.util.ArrayList<>();
        categories.add("All");
        categories.addAll(store.getCategories());
        
        // Icon mapping for known categories (new categories get default icon)
        java.util.Map<String, String> categoryIcons = new java.util.HashMap<>();
        categoryIcons.put("All", "⭐");
        categoryIcons.put("Coffee", "☕");
        categoryIcons.put("Milk Tea", "✨");
        categoryIcons.put("Frappe", "🧊");
        categoryIcons.put("Fruit Tea", "🍓");
        categoryIcons.put("Pastries", "🍰");
        categoryIcons.put("Tea", "🍵");
        categoryIcons.put("Drinks", "🥤");
        categoryIcons.put("Snacks", "🍪");
        categoryIcons.put("Desserts", "🍨");
        categoryIcons.put("Breakfast", "🥐");
        categoryIcons.put("Sandwiches", "🥪");
        
        for (String category : categories) {
            String icon = categoryIcons.getOrDefault(category, "📦"); // Default icon if not in map
            Button categoryBtn = createCategoryButton(category, icon);
            categoryContainerSidebar.getChildren().add(categoryBtn);
        }

        sidebar.getChildren().addAll(header, categoryContainerSidebar);
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

    // Rebuild the categories sidebar when Store categories change
    private void refreshCategoriesSidebar() {
        try {
            if (categoryContainerSidebar == null) return; // nothing to refresh yet
            categoryContainerSidebar.getChildren().clear();

            // Get all categories from the store (dynamically managed via Admin)
            java.util.List<String> categories = new java.util.ArrayList<>();
            categories.add("All");
            categories.addAll(store.getCategories());

            // Icon mapping for known categories (new categories get default icon)
            java.util.Map<String, String> categoryIcons = new java.util.HashMap<>();
            categoryIcons.put("All", "⭐");
            categoryIcons.put("Coffee", "☕");
            categoryIcons.put("Milk Tea", "✨");
            categoryIcons.put("Frappe", "🧊");
            categoryIcons.put("Fruit Tea", "🍓");
            categoryIcons.put("Pastries", "🍰");
            categoryIcons.put("Tea", "🍵");
            categoryIcons.put("Drinks", "🥤");
            categoryIcons.put("Snacks", "🍪");
            categoryIcons.put("Desserts", "🍨");
            categoryIcons.put("Breakfast", "🥐");
            categoryIcons.put("Sandwiches", "🥪");

            for (String category : categories) {
                String icon = categoryIcons.getOrDefault(category, "📦");
                Button categoryBtn = createCategoryButton(category, icon);
                categoryContainerSidebar.getChildren().add(categoryBtn);
            }
        } catch (Exception ignored) {}
    }

    // Start a background WatchService that watches data/categories.json and reloads categories
    private void startCategoryFileWatcher() {
        try {
            Path dataDir = Paths.get("data");
            if (!Files.exists(dataDir)) return;

            final WatchService watcher = FileSystems.getDefault().newWatchService();
            dataDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        WatchKey key = watcher.take();
                        for (WatchEvent<?> ev : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = ev.kind();
                            Path changed = (Path) ev.context();
                            if (changed != null) {
                                String name = changed.toString();
                                // small debounce to wait for file write to complete
                                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                                if ("categories.json".equals(name)) {
                                    try {
                                        Store.getInstance().reloadCategoriesFromDisk();
                                    } catch (Exception ex) {
                                        System.err.println("Failed to reload categories: " + ex.getMessage());
                                    }
                                } else if ("products.json".equals(name)) {
                                    try {
                                        Store.getInstance().reloadProductsFromDisk();
                                    } catch (Exception ex) {
                                        System.err.println("Failed to reload products: " + ex.getMessage());
                                    }
                                }
                                continue;
                            }
                        }
                        key.reset();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (ClosedWatchServiceException cwse) {
                        break;
                    } catch (Exception ex) {
                        System.err.println("Category watcher error: " + ex.getMessage());
                        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    }
                }
            }, "CategoryFileWatcher");
            t.setDaemon(true);
            t.start();
        } catch (Exception ex) {
            System.err.println("Category watcher failed to start: " + ex.getMessage());
        }
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
        // If the user selected the special "All" category, return every product (no 5-item limit)
        if (selectedCategory != null && selectedCategory.equalsIgnoreCase("All")) {
            java.util.List<Product> copy = new java.util.ArrayList<>(allProducts);
            try {
                copy.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            } catch (Exception ignored) {}
            return copy;
        }

        java.util.List<Product> filtered = new java.util.ArrayList<>();
        for (Product product : allProducts) {
            if (productMatchesCategory(product, selectedCategory)) {
                filtered.add(product);
            }
        }

        // Ensure deterministic order
        try {
            filtered.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        } catch (Exception ignored) {}

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
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1); -fx-cursor: hand; -fx-border-color: #F0F0F0; -fx-border-width: 1; -fx-border-radius: 12;");
        
        // More compact card sizing like reference
        int cardWidth = sidebarVisible ? 200 : 240;
        int cardHeight = sidebarVisible ? 280 : 320;
        
        card.setPrefWidth(cardWidth);
        card.setPrefHeight(cardHeight);

        // Product image with real image support
        StackPane imagePane = new StackPane();
        int imageHeight = sidebarVisible ? 140 : 180;
        imagePane.setPrefHeight(imageHeight);
        imagePane.setMaxHeight(imageHeight);
        imagePane.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 12 12 0 0;");
        
        // Try to load product image
        javafx.scene.image.ImageView productImage = loadProductImage(product.getId());
        if (productImage != null) {
            productImage.setFitHeight(imageHeight);
            productImage.setFitWidth(cardWidth);
            productImage.setPreserveRatio(true);
            // Clip to rounded corners
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(cardWidth, imageHeight);
            clip.setArcWidth(24);
            clip.setArcHeight(24);
            imagePane.setClip(clip);
            imagePane.getChildren().add(productImage);
        } else {
            // Fallback to emoji if no image
            imagePane.setStyle("-fx-background-color: linear-gradient(to bottom right, #D4B5A0, #C4A890); -fx-background-radius: 12 12 0 0;");
            
            Label imagePlaceholder = new Label("☕");
            int emojiSize = sidebarVisible ? 50 : 60;
            imagePlaceholder.setFont(Font.font("Segoe UI Emoji", emojiSize));
            imagePlaceholder.setTextFill(Color.web("#FFFFFF"));
            imagePane.getChildren().add(imagePlaceholder);
        }

        // Content area with proper spacing
        VBox contentBox = new VBox(4);
        contentBox.setPadding(new Insets(12, 14, 12, 14));
        contentBox.setAlignment(Pos.TOP_LEFT);

        // Product name (left-aligned)
        Label nameLabel = new Label(product.getName());
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        nameLabel.setTextFill(Color.web("#1A1A1A"));
        nameLabel.setWrapText(false);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        
        // Bottom row: price on left, button on right
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.setPadding(new Insets(2, 0, 0, 0));
        
        // For beverages, show starting price from size; for pastries, show base price
        boolean isPastryProduct = false;
        if (product.getCategory() != null) {
            String cat = product.getCategory().toLowerCase();
            isPastryProduct = cat.contains("pastr") || cat.contains("bakery") || cat.contains("snack") || cat.contains("pastry");
        }
        
        double displayPrice;
        if (isPastryProduct) {
            displayPrice = product.getPrice();
        } else {
            // For beverages, get the smallest available size price
            Map<String, Double> sizes = product.getSizeSurcharges();
            if (product.isHasSmall()) {
                displayPrice = sizes.getOrDefault("Small", 0.0);
            } else if (product.isHasMedium()) {
                displayPrice = sizes.getOrDefault("Medium", 0.0);
            } else if (product.isHasLarge()) {
                displayPrice = sizes.getOrDefault("Large", 0.0);
            } else {
                displayPrice = sizes.getOrDefault("Small", 0.0);
            }
        }
        
        Label priceLabel = new Label("₱" + String.format("%.1f", displayPrice));
        priceLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        priceLabel.setTextFill(Color.web("#666666"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Cart button: use a graphic instead of relying on emoji fonts (prevents fallback to "...")
        Button addBtn = new Button();
        addBtn.setGraphic(createCartIcon(14));
        addBtn.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-pref-width: 32; -fx-pref-height: 32; -fx-cursor: hand; -fx-border-color: #D0D0D0; -fx-border-width: 1.5; -fx-border-radius: 20;");
        addBtn.setPrefSize(32, 32);
        addBtn.setFocusTraversable(false);
        addBtn.setOnMouseEntered(e -> addBtn.setStyle("-fx-background-color: #F0F0F0; -fx-background-radius: 20; -fx-pref-width: 32; -fx-pref-height: 32; -fx-cursor: hand; -fx-border-color: #A0A0A0; -fx-border-width: 1.5; -fx-border-radius: 20;"));
        addBtn.setOnMouseExited(e -> addBtn.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-pref-width: 32; -fx-pref-height: 32; -fx-cursor: hand; -fx-border-color: #D0D0D0; -fx-border-width: 1.5; -fx-border-radius: 20;"));

        // Open customization when user clicks the cart icon so they can select add-ons / sugar etc.
        addBtn.setTooltip(new javafx.scene.control.Tooltip("Customize before adding"));
        addBtn.setOnMouseClicked(ev -> {
            ev.consume();
            try {
                resetInactivityTimer();
                // Open the customization page for this product (user can add to cart from there)
                showCustomizationPage(product);
            } catch (Exception ex) {
                System.err.println("Failed to open customization: " + ex.getMessage());
            }
        });

        bottomRow.getChildren().addAll(priceLabel, spacer, addBtn);

        contentBox.getChildren().addAll(nameLabel, bottomRow);
        card.getChildren().addAll(imagePane, contentBox);

        // Click handler - open customization on card click
        card.setOnMouseClicked(e -> {
            if (e.getTarget() != addBtn) {
                resetInactivityTimer();
                showCustomizationPage(product);
            }
        });

        // Subtle hover effects
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 6, 0, 0, 2); -fx-cursor: hand; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 12;");
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 1); -fx-cursor: hand; -fx-border-color: #F0F0F0; -fx-border-width: 1; -fx-border-radius: 12;");
        });

        return card;
    }
    
    private void showCustomizationPage(Product product) {
        // track currently-customizing product so listeners can refresh this modal
        this.currentCustomizationProduct = product;
        // Create modal-style dialog
        StackPane modalRoot = new StackPane();
        modalRoot.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        
        // Modal card
        VBox modalCard = new VBox(20);
        modalCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 30, 0, 0, 10);");
        modalCard.setPadding(new Insets(40));
        modalCard.setPrefWidth(800);
        modalCard.setMaxWidth(800);
        modalCard.setAlignment(Pos.TOP_CENTER);
        
        // Header with close button
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPrefHeight(60);
        
        // Product image and info (left side)
        HBox leftHeader = new HBox(20);
        leftHeader.setAlignment(Pos.CENTER_LEFT);
        
        StackPane imageBox = new StackPane();
        imageBox.setPrefSize(80, 80);
        imageBox.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 12;");
        ImageView productImg = loadProductImage(product.getId());
        if (productImg != null) {
            productImg.setFitWidth(80);
            productImg.setFitHeight(80);
            productImg.setPreserveRatio(true);
            imageBox.getChildren().add(productImg);
        } else {
            Label placeholder = new Label("☕");
            placeholder.setFont(Font.font("Segoe UI Emoji", 40));
            imageBox.getChildren().add(placeholder);
        }
        
        VBox productInfo = new VBox(6);
        Label productName = new Label(product.getName());
        productName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        productName.setTextFill(Color.web("#1A1A1A"));
        
        String descText = product.getDescription();
        if (descText == null || descText.trim().isEmpty()) {
            // Category-appropriate fallback description
            String category = product.getCategory();
            if (category != null) {
                String catLower = category.toLowerCase();
                if (catLower.contains("pastr") || catLower.contains("bakery") || catLower.contains("snack")) {
                    descText = "Customize your order exactly how you like it.";
                } else if (catLower.contains("coffee")) {
                    descText = "Customize your coffee exactly how you like it.";
                } else if (catLower.contains("tea")) {
                    descText = "Customize your drink exactly how you like it.";
                } else {
                    descText = "Customize your order exactly how you like it.";
                }
            } else {
                descText = "Customize your order exactly how you like it.";
            }
        }
        Label productDesc = new Label(descText);
        productDesc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        productDesc.setTextFill(Color.web("#666666"));
        productDesc.setWrapText(true);
        
        productInfo.getChildren().addAll(productName, productDesc);
        leftHeader.getChildren().addAll(imageBox, productInfo);
        
        // Close button (right side)
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999999; -fx-cursor: hand; -fx-padding: 0;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333333; -fx-cursor: hand; -fx-padding: 0;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999999; -fx-cursor: hand; -fx-padding: 0;"));
        closeBtn.setOnAction(e -> {
            this.currentCustomizationProduct = null;
            showMenuScreen();
        });
        
        headerRow.getChildren().addAll(leftHeader, spacer1, closeBtn);
        
        // Content: Size, Milk Options, Add-ons in a grid
        Label totalPrice = new Label("₱0.00"); // Starts at 0, updates when size selected
        
        // Quantity state - declared early so form can access it
        final int[] qty = {1};
        
        // Recompute callback - will be set after form is created
        final Runnable[] recomputeTotalRef = new Runnable[1];
        
        VBox customContent = createCompactCustomizationForm(product, totalPrice, customizationDefaultSize, qty, recomputeTotalRef);
        // Clear default after it has been used to avoid leaking selection to other products
        customizationDefaultSize = null;
        
        // Bottom section: Quantity, Total, Add to Order button
        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 0; -fx-background-color: #EEEEEE;");
        
        HBox footerRow = new HBox(20);
        footerRow.setAlignment(Pos.CENTER);
        footerRow.setPadding(new Insets(20, 0, 0, 0));
        
        // Quantity
        HBox qtyBox = new HBox(12);
        qtyBox.setAlignment(Pos.CENTER);
        Label qtyLbl = new Label("Quantity:");
        qtyLbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        qtyLbl.setTextFill(Color.web("#333333"));
        
        Button qtyMinus = new Button("−");
        qtyMinus.setPrefSize(34, 34);
        qtyMinus.setStyle("-fx-background-color: #F5F5F5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 16; -fx-cursor: hand;");
        qtyMinus.setOnMouseEntered(e -> qtyMinus.setStyle("-fx-background-color: #EEEEEE; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 16; -fx-cursor: hand;"));
        qtyMinus.setOnMouseExited(e -> qtyMinus.setStyle("-fx-background-color: #F5F5F5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 16; -fx-cursor: hand;"));
        
        Label qtyVal = new Label("1");
        qtyVal.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        qtyVal.setTextFill(Color.web("#1A1A1A"));
        qtyVal.setPrefWidth(30);
        qtyVal.setAlignment(Pos.CENTER);
        
        Button qtyPlus = new Button("+");
        qtyPlus.setPrefSize(34, 34);
        qtyPlus.setStyle("-fx-background-color: #F5F5F5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 16; -fx-cursor: hand;");
        qtyPlus.setOnMouseEntered(e -> qtyPlus.setStyle("-fx-background-color: #EEEEEE; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 16; -fx-cursor: hand;"));
        qtyPlus.setOnMouseExited(e -> qtyPlus.setStyle("-fx-background-color: #F5F5F5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 16; -fx-cursor: hand;"));
        
        // qty array was declared earlier - update handlers to also recompute total
        qtyMinus.setOnAction(e -> { 
            if (qty[0] > 1) { 
                qty[0]--; 
                qtyVal.setText(String.valueOf(qty[0])); 
                if (recomputeTotalRef[0] != null) recomputeTotalRef[0].run();
            } 
        });
        qtyPlus.setOnAction(e -> { 
            qty[0]++; 
            qtyVal.setText(String.valueOf(qty[0])); 
            if (recomputeTotalRef[0] != null) recomputeTotalRef[0].run();
        });
        
        qtyBox.getChildren().addAll(qtyLbl, qtyMinus, qtyVal, qtyPlus);
        
        // Spacer
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        
        // Total and Add button
        VBox totalBox = new VBox(4);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        Label totalLbl = new Label("TOTAL");
        totalLbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        totalLbl.setTextFill(Color.web("#999999"));
        totalPrice.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        totalPrice.setTextFill(Color.web("#1A1A1A"));
        totalBox.getChildren().addAll(totalLbl, totalPrice);
        
        Button addBtn = new Button("Add to Order →");
        addBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        addBtn.setPrefSize(160, 48);
        addBtn.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        addBtn.setOnMouseEntered(e -> addBtn.setStyle("-fx-background-color: #000000; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
        addBtn.setOnMouseExited(e -> addBtn.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
        addBtn.setOnAction(e -> {
            try {
                resetInactivityTimer();
                // Get customization selections and add to cart
                handleAddToCart(product, qty[0], customContent);
            } catch (Exception ex) {
                System.err.println("Error adding to cart: " + ex.getMessage());
            }
        });
        
        footerRow.getChildren().addAll(qtyBox, spacer2, totalBox, addBtn);
        
        modalCard.getChildren().addAll(headerRow, sep, customContent, footerRow);
        
        // Add modal to center of root
        modalRoot.getChildren().add(modalCard);
        StackPane.setAlignment(modalCard, Pos.CENTER);
        
        Scene customScene = new Scene(modalRoot, 1600, 900);
        setScenePreserveWindowSize(customScene);
        applyAtlantafx(customScene);
    }
    
    // Create compact customization form with size, milk, and add-ons
    private VBox createCompactCustomizationForm(Product product, Label totalPrice, Double initialSize, int[] qty, Runnable[] recomputeTotalRef) {
        VBox form = new VBox(20);
        form.setAlignment(Pos.TOP_LEFT);
        
        boolean isCoffee = product.getCategory() != null && product.getCategory().equalsIgnoreCase("Coffee");
        boolean isMilkTea = product.getCategory() != null && product.getCategory().equalsIgnoreCase("Milk Tea");
        boolean isEspresso = product.getName() != null && product.getName().equalsIgnoreCase("Espresso");
        boolean isPastry = false;
        if (product.getCategory() != null) {
            String c = product.getCategory().toLowerCase();
            isPastry = c.contains("pastr") || c.contains("bakery") || c.contains("snack");
        }
        
        // Keep state for milk/add-on recompute (declared here so all sections can see it)
        final double[] selectedMilkCost = {0.0};
        // Read size surcharges from product (no hardcoded fallbacks - use 0 if not set)
        Map<String, Double> _sizes = null;
        try { _sizes = product.getSizeSurcharges(); } catch (Exception ignored) { _sizes = new HashMap<>(); }
        final double smallS = _sizes.getOrDefault("Small", 0.0);
        final double mediumS = _sizes.getOrDefault("Medium", 0.0);
        final double largeS = _sizes.getOrDefault("Large", 0.0);
        // Determine which sizes are available for this product
        boolean hasSmall = true, hasMedium = true, hasLarge = true;
        try { hasSmall = product.isHasSmall(); } catch (Exception ignored) {}
        try { hasMedium = product.isHasMedium(); } catch (Exception ignored) {}
        try { hasLarge = product.isHasLarge(); } catch (Exception ignored) {}

        // Pick a sensible default selected size (first available: Small -> Medium -> Large) or use initialSize if provided
        // For pastries, size cost is always 0 (they don't have size options)
        double defaultSize = 0.0;
        if (!isPastry) {
            if (initialSize != null) {
                defaultSize = initialSize;
                // ensure initial size exists in available sizes; otherwise fallback
                boolean matching = false;
                try { if (hasSmall && Math.abs(initialSize - smallS) < 0.001) matching = true; } catch (Exception ignored) {}
                try { if (hasMedium && Math.abs(initialSize - mediumS) < 0.001) matching = true; } catch (Exception ignored) {}
                try { if (hasLarge && Math.abs(initialSize - largeS) < 0.001) matching = true; } catch (Exception ignored) {}
                if (!matching) initialSize = null; // ignore if not matching
            }
            if (initialSize == null) {
                if (hasSmall) defaultSize = smallS;
                else if (hasMedium) defaultSize = mediumS;
                else if (hasLarge) defaultSize = largeS;
            }
        }
        final double[] selectedSizeCost = { defaultSize }; // declared at method level so ADD-ONS section can access it
        final Runnable[] recomputeRef = new Runnable[1];
        
        // Reference to addOnsGrid for recompute function (will be set later if add-ons exist)
        final HBox[] addOnsGridRef = new HBox[1];
        
        // Create recompute function that updates total based on qty, size, milk, and add-ons
        final boolean isPastryFinal = isPastry;
        Runnable recomputeTotal = () -> {
            try {
                double unitPrice;
                if (isPastryFinal) {
                    // Pastries: use base price only (no sizes)
                    unitPrice = product.getPrice();
                } else {
                    // Beverages: size price IS the actual price, not a surcharge
                    unitPrice = selectedSizeCost[0] + selectedMilkCost[0];
                }
                // Add selected add-ons if grid exists
                if (addOnsGridRef[0] != null) {
                    for (javafx.scene.Node n : addOnsGridRef[0].getChildren()) {
                        if (n instanceof javafx.scene.control.Button) {
                            javafx.scene.control.Button b = (javafx.scene.control.Button) n;
                            if (b.getStyle() != null && b.getStyle().equals(getPillSelectedStyle())) {
                                Object ud = b.getUserData();
                                if (ud instanceof com.coffeeshop.model.AddOn) {
                                    unitPrice += ((com.coffeeshop.model.AddOn) ud).getPrice();
                                }
                            }
                        }
                    }
                }
                double total = unitPrice * qty[0];
                final double finalTotal = total;
                javafx.application.Platform.runLater(() -> totalPrice.setText("₱" + String.format("%.2f", finalTotal)));
            } catch (Exception ignored) {}
        };
        // Set the reference so quantity buttons and add-ons can call it
        recomputeTotalRef[0] = recomputeTotal;
        recomputeRef[0] = recomputeTotal;

        // SIZE SECTION - converted to clickable pills (only show if at least one size is available)
        boolean hasAnySizes = hasSmall || hasMedium || hasLarge;
        if (!isPastry && hasAnySizes) {
            VBox sizeSection = new VBox(12);
            Label sizeTitle = new Label("SELECT SIZE");
            sizeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            sizeTitle.setTextFill(Color.web("#999999"));
            sizeTitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px;");
            
            HBox sizeButtons = new HBox(10);
            sizeButtons.setAlignment(Pos.CENTER_LEFT);
            
            Label sizeDefault = new Label("(Small is selected by default)");
            sizeDefault.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
            sizeDefault.setTextFill(Color.web("#888888"));
            
            // Track size name along with cost
            final String[] selectedSizeName = { hasSmall ? "Small" : (hasMedium ? "Medium" : "Large") };
            
            final Button[] smallBtnRef = new Button[1];
            final Button[] mediumBtnRef = new Button[1];
            final Button[] largeBtnRef = new Button[1];

            // Create buttons only for sizes that are available
            if (hasSmall) {
                Button smallBtn = new Button(String.format("Small (₱%.2f)", smallS));
                smallBtnRef[0] = smallBtn;
                smallBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                smallBtn.setPadding(new Insets(8, 16, 8, 16));
                smallBtn.setStyle(Math.abs(selectedSizeCost[0] - smallS) < 0.001 ? getPillSelectedStyle() : getPillDefaultStyle());
                smallBtn.setOnAction(e -> {
                    selectedSizeCost[0] = smallS;
                    selectedSizeName[0] = "Small";
                    if (smallBtnRef[0] != null) smallBtnRef[0].setStyle(getPillSelectedStyle());
                    if (mediumBtnRef[0] != null) mediumBtnRef[0].setStyle(getPillDefaultStyle());
                    if (largeBtnRef[0] != null) largeBtnRef[0].setStyle(getPillDefaultStyle());
                    recomputeTotal.run();
                });
                smallBtn.setOnMouseEntered(e -> {
                    if (selectedSizeCost[0] != smallS) smallBtn.setStyle("-fx-text-fill: #333333; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-background-color: #F5F5F5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
                });
                smallBtn.setOnMouseExited(e -> {
                    if (Math.abs(selectedSizeCost[0] - smallS) >= 0.001) smallBtn.setStyle(getPillDefaultStyle());
                    else smallBtn.setStyle(getPillSelectedStyle());
                });
                sizeButtons.getChildren().add(smallBtn);
            }

            if (hasMedium) {
                Button mediumBtn = new Button(String.format("Medium (₱%.2f)", mediumS));
                mediumBtnRef[0] = mediumBtn;
                mediumBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                mediumBtn.setPadding(new Insets(8, 16, 8, 16));
                mediumBtn.setStyle(Math.abs(selectedSizeCost[0] - mediumS) < 0.001 ? getPillSelectedStyle() : getPillDefaultStyle());
                mediumBtn.setOnAction(e -> {
                    selectedSizeCost[0] = mediumS;
                    selectedSizeName[0] = "Medium";
                    if (smallBtnRef[0] != null) smallBtnRef[0].setStyle(getPillDefaultStyle());
                    if (mediumBtnRef[0] != null) mediumBtnRef[0].setStyle(getPillSelectedStyle());
                    if (largeBtnRef[0] != null) largeBtnRef[0].setStyle(getPillDefaultStyle());
                    recomputeTotal.run();
                });
                mediumBtn.setOnMouseEntered(e -> {
                    if (selectedSizeCost[0] != mediumS) mediumBtn.setStyle("-fx-text-fill: #333333; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-background-color: #F5F5F5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
                });
                mediumBtn.setOnMouseExited(e -> {
                    if (Math.abs(selectedSizeCost[0] - mediumS) >= 0.001) mediumBtn.setStyle(getPillDefaultStyle());
                    else mediumBtn.setStyle(getPillSelectedStyle());
                });
                sizeButtons.getChildren().add(mediumBtn);
            }

            if (hasLarge) {
                Button largeBtn = new Button(String.format("Large (₱%.2f)", largeS));
                largeBtnRef[0] = largeBtn;
                largeBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                largeBtn.setPadding(new Insets(8, 16, 8, 16));
                largeBtn.setStyle(Math.abs(selectedSizeCost[0] - largeS) < 0.001 ? getPillSelectedStyle() : getPillDefaultStyle());
                largeBtn.setOnAction(e -> {
                    selectedSizeCost[0] = largeS;
                    selectedSizeName[0] = "Large";
                    if (smallBtnRef[0] != null) smallBtnRef[0].setStyle(getPillDefaultStyle());
                    if (mediumBtnRef[0] != null) mediumBtnRef[0].setStyle(getPillDefaultStyle());
                    if (largeBtnRef[0] != null) largeBtnRef[0].setStyle(getPillSelectedStyle());
                    recomputeTotal.run();
                });
                largeBtn.setOnMouseEntered(e -> {
                    if (selectedSizeCost[0] != largeS) largeBtn.setStyle("-fx-text-fill: #333333; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-background-color: #F5F5F5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
                });
                largeBtn.setOnMouseExited(e -> {
                    if (Math.abs(selectedSizeCost[0] - largeS) >= 0.001) largeBtn.setStyle(getPillDefaultStyle());
                    else largeBtn.setStyle(getPillSelectedStyle());
                });
                sizeButtons.getChildren().add(largeBtn);
            }
            sizeSection.getChildren().addAll(sizeTitle, sizeDefault, sizeButtons);
            form.getChildren().add(sizeSection);
            
            // Store the selected size cost and name in the form's user data for later retrieval
            form.setUserData(new Object[]{selectedSizeCost, selectedSizeName});
        }
        
        // MILK OPTIONS SECTION (uses product settings from admin panel)
        boolean hasMilkOptions = product.isHasMilkOptions();
        if (hasMilkOptions) {
            VBox milkSection = new VBox(12);
            Label milkTitle = new Label("MILK OPTIONS");
            milkTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            milkTitle.setTextFill(Color.web("#999999"));
            
            HBox milkButtons = new HBox(12);
            milkButtons.setAlignment(Pos.CENTER_LEFT);
            
            // Get prices from product settings
            double oatPrice = product.getOatMilkPrice();
            double almondPrice = product.getAlmondMilkPrice();
            double soyPrice = product.getSoyMilkPrice();
            
            // Create buttons only for enabled milk options
            java.util.List<Button> allMilkBtns = new java.util.ArrayList<>();
            
            Button oatBtn = null;
            Button almondBtn = null;
            Button soyBtn = null;
            
            if (product.isHasOatMilk()) {
                oatBtn = createMilkOptionPill("Oat Milk (+₱" + String.format("%.0f", oatPrice) + ")");
                oatBtn.setUserData(new com.coffeeshop.model.AddOn("milk-oat", "Oat Milk", oatPrice, "Milk"));
                allMilkBtns.add(oatBtn);
                milkButtons.getChildren().add(oatBtn);
            }
            
            if (product.isHasAlmondMilk()) {
                almondBtn = createMilkOptionPill("Almond Milk (+₱" + String.format("%.0f", almondPrice) + ")");
                almondBtn.setUserData(new com.coffeeshop.model.AddOn("milk-almond", "Almond Milk", almondPrice, "Milk"));
                allMilkBtns.add(almondBtn);
                milkButtons.getChildren().add(almondBtn);
            }
            
            if (product.isHasSoyMilk()) {
                soyBtn = createMilkOptionPill("Soy Milk (+₱" + String.format("%.0f", soyPrice) + ")");
                soyBtn.setUserData(new com.coffeeshop.model.AddOn("milk-soy", "Soy Milk", soyPrice, "Milk"));
                allMilkBtns.add(soyBtn);
                milkButtons.getChildren().add(soyBtn);
            }

            // Make milk options toggleable and mutually exclusive
            for (Button btn : allMilkBtns) {
                final Button currentBtn = btn;
                final double milkCost = ((com.coffeeshop.model.AddOn) btn.getUserData()).getPrice();
                btn.setOnAction(e -> {
                    boolean nowSelected = !currentBtn.getStyle().equals(getPillSelectedStyle());
                    if (nowSelected) {
                        // Deselect all others
                        for (Button other : allMilkBtns) {
                            if (other != currentBtn) {
                                other.setStyle(getPillDefaultStyle());
                            }
                        }
                        currentBtn.setStyle(getPillSelectedStyle());
                        selectedMilkCost[0] = milkCost;
                    } else {
                        currentBtn.setStyle(getPillDefaultStyle());
                        selectedMilkCost[0] = 0.0;
                    }
                    if (recomputeRef[0] != null) recomputeRef[0].run();
                });
            }
            
            if (!milkButtons.getChildren().isEmpty()) {
                milkSection.getChildren().addAll(milkTitle, milkButtons);
                form.getChildren().add(milkSection);
            }
        }
        
        // SUGAR LEVEL SECTION (only for non-pastry products)
        if (!isPastry) {
            // debug: show value of sugar levels
            System.out.println("DEBUG: product=" + product.getName() + " category=" + product.getCategory() + " sugarLevels=" + product.getSugarLevels());
            VBox sugarSection = new VBox(12);
            Label sugarTitle = new Label("SUGAR LEVEL");
            sugarTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            sugarTitle.setTextFill(Color.web("#999999"));
            sugarTitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px;");

            HBox sugarButtons = new HBox(10);
            sugarButtons.setAlignment(Pos.CENTER_LEFT);

            List<String> sugarLevels = product.getSugarLevels();
            System.out.println("DEBUG: compact form product " + product.getName() + " sugarLevels: " + sugarLevels);
            // Interpretation rules:
            // - null: admin did not configure sugar levels -> show sensible defaults
            // - empty list: admin explicitly disabled sugar for this product -> do not show sugar UI
            // - non-empty list: use the configured values
            String[] allowedLevels;
            if (sugarLevels == null) {
                // no admin configuration, fall back to defaults
                allowedLevels = new String[]{"0%", "25%", "50%", "75%", "100%"};
            } else if (sugarLevels.isEmpty()) {
                // admin explicitly disabled sugar options -> no allowed levels
                allowedLevels = new String[0];
            } else {
                java.util.List<String> normalized = new ArrayList<>();
                for (String s : sugarLevels) {
                    int n = parseSugarLevel(s);
                    normalized.add(n + "%");
                }
                allowedLevels = normalized.toArray(new String[0]);
            }

            List<Button> sugarPills = new ArrayList<>();
            final int[] selectedSugarLevel = {50};
            // Determine initial selection: if editing an item, use its sugar; otherwise prefer 50% or first allowed level
            int defaultSugar = 50;
            if (customizingOrderItem != null) defaultSugar = customizingOrderItem.getSugarLevel();
            else {
                for (String s : allowedLevels) if (s.equals("50%")) { defaultSugar = 50; break; }
                if (allowedLevels.length > 0 && defaultSugar != 50) {
                    try { defaultSugar = parseSugarLevel(allowedLevels[0]); } catch (Exception ignored) {}
                }
            }
            for (String level : allowedLevels) {
                Button pill = new Button(level);
                pill.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                pill.setPadding(new Insets(8, 16, 8, 16));
                int numeric = parseSugarLevel(level);
                pill.setStyle(numeric == defaultSugar ? getPillSelectedStyle() : getPillDefaultStyle());
                if (numeric == defaultSugar) selectedSugarLevel[0] = numeric;
                pill.setUserData(numeric); // store numeric sugar level
                pill.setOnAction(e -> {
                    // Single-select behavior: deselect all others
                    boolean nowSelected = !pill.getStyle().equals(getPillSelectedStyle());
                    for (Button other : sugarPills) {
                        if (other != pill) other.setStyle(getPillDefaultStyle());
                    }
                    pill.setStyle(nowSelected ? getPillSelectedStyle() : getPillDefaultStyle());
                    // store selection
                    try { selectedSugarLevel[0] = (Integer) pill.getUserData(); } catch (Exception ignored) {}
                });
                pill.setOnMouseEntered(e -> {
                    if (!pill.getStyle().equals(getPillSelectedStyle())) {
                        pill.setStyle("-fx-text-fill: #333333; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-background-color: #F5F5F5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
                    }
                });
                pill.setOnMouseExited(e -> {
                    if (!pill.getStyle().equals(getPillSelectedStyle())) {
                        pill.setStyle(getPillDefaultStyle());
                    }
                });
                sugarPills.add(pill);
                sugarButtons.getChildren().add(pill);
            }
            sugarSection.getChildren().addAll(sugarTitle, sugarButtons);
            // Only add sugar section if there are allowed levels configured (or defaults exist)
            if (allowedLevels.length > 0) form.getChildren().add(sugarSection);
            // Temporary fix: hide sugar for Cappuccino
            if (product.getName().equals("Cappuccino")) {
                form.getChildren().remove(sugarSection);
            }
        }

        // ADD-ONS SECTION - Dynamically loaded from database
        java.util.List<com.coffeeshop.model.AddOn> availableAddOns = store.getAddOnsForProduct(product.getId());
        if (!availableAddOns.isEmpty()) {
            VBox addOnsSection = new VBox(12);
            Label addOnsTitle = new Label("ADD-ONS");
            addOnsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            addOnsTitle.setTextFill(Color.web("#999999"));
            addOnsTitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px;");
            
            HBox addOnsGrid = new HBox(10);
            addOnsGrid.setAlignment(Pos.CENTER_LEFT);
            addOnsGrid.setStyle("-fx-wrap-text: true;");
            
            // Create pills dynamically from database
                for (com.coffeeshop.model.AddOn addOn : availableAddOns) {
                Button addOnPill = new Button(addOn.getName() + " (" + addOn.getFormattedPrice() + ")");
                addOnPill.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                addOnPill.setPadding(new Insets(8, 16, 8, 16));
                addOnPill.setStyle(getPillDefaultStyle());
                addOnPill.setUserData(addOn); // Store the AddOn object for later retrieval
                addOnPill.setOnAction(e -> {
                    addOnPill.setStyle(addOnPill.getStyle().equals(getPillSelectedStyle()) ? getPillDefaultStyle() : getPillSelectedStyle());
                    if (recomputeRef[0] != null) recomputeRef[0].run();
                });
                addOnPill.setOnMouseEntered(e -> {
                    if (!addOnPill.getStyle().equals(getPillSelectedStyle())) {
                        addOnPill.setStyle("-fx-text-fill: #333333; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-background-color: #F5F5F5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
                    }
                });
                addOnPill.setOnMouseExited(e -> {
                    if (!addOnPill.getStyle().equals(getPillSelectedStyle())) {
                        addOnPill.setStyle(getPillDefaultStyle());
                    }
                });
                addOnsGrid.getChildren().add(addOnPill);
            }
            
            addOnsSection.getChildren().addAll(addOnsTitle, addOnsGrid);
            // Set the addOnsGrid reference so recomputeTotal can access it
            addOnsGridRef[0] = addOnsGrid;
            // Initial compute with add-ons
            if (recomputeRef[0] != null) recomputeRef[0].run();
            form.getChildren().add(addOnsSection);
        }
        
        // SPECIAL REQUESTS SECTION
        VBox specialRequestsSection = new VBox(12);
        Label requestsTitle = new Label("💬 SPECIAL REQUESTS");
        requestsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        requestsTitle.setTextFill(Color.web("#999999"));
        requestsTitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px;");
        
        // Quick select buttons for common requests (loaded from admin-configured list)
        HBox quickButtons = new HBox(8);
        quickButtons.setAlignment(Pos.CENTER_LEFT);
        
        java.util.List<com.coffeeshop.model.SpecialRequest> commonRequests = store.getActiveSpecialRequestsForProduct(product.getId());
        javafx.scene.control.TextArea requestTextArea = new javafx.scene.control.TextArea();
        requestTextArea.setPromptText("Add special instructions here...");
        requestTextArea.setPrefRowCount(2);
        requestTextArea.setWrapText(true);
        requestTextArea.setStyle("-fx-font-size: 12px; -fx-border-color: #E0E0E0; -fx-border-radius: 6; -fx-background-radius: 6;");
        
        for (com.coffeeshop.model.SpecialRequest sr : commonRequests) {
            String request = sr.getText();
            Button quickBtn = new Button(request);
            quickBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
            quickBtn.setPadding(new Insets(6, 12, 6, 12));
            quickBtn.setStyle("-fx-text-fill: #666666; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-background-color: #FFFFFF; -fx-background-radius: 15; -fx-border-radius: 15; -fx-cursor: hand; -fx-font-size: 11px;");
            quickBtn.setOnMouseEntered(e -> quickBtn.setStyle("-fx-text-fill: #333333; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-background-color: #F5F5F5; -fx-background-radius: 15; -fx-border-radius: 15; -fx-cursor: hand; -fx-font-size: 11px;"));
            quickBtn.setOnMouseExited(e -> quickBtn.setStyle("-fx-text-fill: #666666; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-background-color: #FFFFFF; -fx-background-radius: 15; -fx-border-radius: 15; -fx-cursor: hand; -fx-font-size: 11px;"));
            quickBtn.setOnAction(e -> {
                String currentText = requestTextArea.getText();
                if (currentText.isEmpty()) {
                    requestTextArea.setText(request);
                } else if (!currentText.contains(request)) {
                    requestTextArea.setText(currentText + ", " + request);
                }
            });
            quickButtons.getChildren().add(quickBtn);
        }
        
        specialRequestsSection.getChildren().addAll(requestsTitle, quickButtons, requestTextArea);
        form.getChildren().add(specialRequestsSection);
        
        // Compute initial total price based on default selections (size, milk, add-ons, quantity)
        if (recomputeRef[0] != null) {
            recomputeRef[0].run();
        }
        
        return form;
    }
    
    // Create a milk option pill button
    private Button createMilkOptionPill(String label) {
        Button pill = new Button(label);
        pill.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        pill.setPadding(new Insets(8, 16, 8, 16));
        pill.setStyle(getPillDefaultStyle());
        
        // Fix hover animation - preserve selected state
        pill.setOnMouseEntered(e -> {
            if (!pill.getStyle().equals(getPillSelectedStyle())) {
                pill.setStyle("-fx-text-fill: #333333; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-background-color: #F5F5F5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand;");
            }
        });
        pill.setOnMouseExited(e -> {
            if (!pill.getStyle().equals(getPillSelectedStyle())) {
                pill.setStyle(getPillDefaultStyle());
            }
        });
        
        return pill;
    }
    
    // Create an add-on label with price
    private HBox createAddOnLabel(String name, String price) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLbl = new Label(name);
        nameLbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        nameLbl.setTextFill(Color.web("#333333"));
        
        Label priceLbl = new Label(price);
        priceLbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        priceLbl.setTextFill(Color.web("#666666"));
        
        box.getChildren().addAll(nameLbl, priceLbl);
        return box;
    }
    
    // Handle adding item to cart
    private void handleAddToCart(Product product, int quantity, VBox customContent) {
        try {
            // Validate quantity - must be at least 1
            if (quantity < 1) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Invalid Quantity");
                alert.setHeaderText(null);
                alert.setContentText("Please select at least 1 item before adding to order.");
                alert.showAndWait();
                return;
            }
            
            // Check if product is a pastry (no size options)
            boolean isPastryProduct = false;
            if (product.getCategory() != null) {
                String cat = product.getCategory().toLowerCase();
                isPastryProduct = cat.contains("pastr") || cat.contains("bakery") || cat.contains("snack") || cat.contains("pastry");
            }
            
            // Create order item with basic details
            OrderItem item = new OrderItem(product, quantity, "Hot", 0);
            
            // Extract selected size cost and name from the form's user data
            // For pastries, size cost is always 0 (they don't have size options)
            Object userDataObj = customContent.getUserData();
            String selectedSize = "Regular"; // Default size
            if (!isPastryProduct && userDataObj instanceof Object[]) {
                Object[] sizeData = (Object[]) userDataObj;
                // sizeData[0] = double[] for size cost, sizeData[1] = String[] for size name
                if (sizeData.length > 0 && sizeData[0] instanceof double[]) {
                    double[] sizeCostArray = (double[]) sizeData[0];
                    if (sizeCostArray.length > 0) {
                        item.setSizeCost(sizeCostArray[0]);
                    }
                }
                if (sizeData.length > 1 && sizeData[1] instanceof String[]) {
                    String[] sizeNameArray = (String[]) sizeData[1];
                    if (sizeNameArray.length > 0 && sizeNameArray[0] != null) {
                        selectedSize = sizeNameArray[0];
                    }
                }
            } else if (!isPastryProduct && userDataObj instanceof double[]) {
                // Fallback for old format (just cost array)
                double[] sizeCostArray = (double[]) userDataObj;
                if (sizeCostArray.length > 0) {
                    item.setSizeCost(sizeCostArray[0]);
                }
            }
            // For pastries, sizeCost stays at 0 (default)

            // Set the size name from the stored value
            item.setSize(selectedSize);
            
            // Extract selected add-ons and special requests from the customContent VBox
            java.util.List<String> selectedAddOnNames = new java.util.ArrayList<>();
            final double[] addOnsTotalCost = {0.0};
            final String[] specialRequest = {""};
            
            // First, find ALL TextAreas and collect their text (special requests)
            // Use a simple recursive function to find all TextAreas
            java.util.function.Consumer<javafx.scene.Node> findAllTextAreas = new java.util.function.Consumer<javafx.scene.Node>() {
                @Override
                public void accept(javafx.scene.Node node) {
                    if (node instanceof javafx.scene.control.TextArea) {
                        javafx.scene.control.TextArea textArea = (javafx.scene.control.TextArea) node;
                        String text = textArea.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            // Append all found text areas together (usually only one for special requests)
                            if (specialRequest[0].isEmpty()) {
                                specialRequest[0] = text.trim();
                            } else {
                                specialRequest[0] += ", " + text.trim();
                            }
                        }
                    } else if (node instanceof javafx.scene.layout.Pane) {
                        javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) node;
                        for (javafx.scene.Node child : pane.getChildren()) {
                            accept(child);
                        }
                    }
                }
            };
            
            // Find all TextAreas
            for (javafx.scene.Node child : customContent.getChildren()) {
                findAllTextAreas.accept(child);
            }
            
            // Now find ALL selected add-on buttons (looking for dark background style)
            java.util.function.Consumer<javafx.scene.Node> findSelectedAddOns = new java.util.function.Consumer<javafx.scene.Node>() {
                @Override
                public void accept(javafx.scene.Node node) {
                    if (node instanceof javafx.scene.control.Button) {
                        javafx.scene.control.Button btn = (javafx.scene.control.Button) node;
                        // Check if button style indicates it's selected (dark background)
                        String style = btn.getStyle();
                        if (style != null && (style.contains("#2C2C2C") || style.contains("2C2C2C"))) {
                            Object userData = btn.getUserData();
                            if (userData instanceof com.coffeeshop.model.AddOn) {
                                com.coffeeshop.model.AddOn addOn = (com.coffeeshop.model.AddOn) userData;
                                selectedAddOnNames.add(addOn.getName());
                                addOnsTotalCost[0] += addOn.getPrice();
                            }
                        }
                    } else if (node instanceof javafx.scene.layout.Pane) {
                        javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) node;
                        for (javafx.scene.Node child : pane.getChildren()) {
                            accept(child);
                        }
                    }
                }
            };
            
            // Find all selected add-ons
            for (javafx.scene.Node child : customContent.getChildren()) {
                findSelectedAddOns.accept(child);
            }
            
            // Set add-ons on the order item
            if (!selectedAddOnNames.isEmpty()) {
                item.setAddOns(String.join(", ", selectedAddOnNames));
                item.setAddOnsCost(addOnsTotalCost[0]);
            }
            
            // Set special request on the order item
            if (!specialRequest[0].isEmpty()) {
                item.setSpecialRequest(specialRequest[0]);
            }
            
            // Add to current order
            if (currentOrder != null) {
                currentOrder.addItem(item);
                updateFooter();
            }
            
            // Show menu screen
            showMenuScreen();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to add item: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    // Create a detailed product view with customization options
    private ScrollPane createProductDetailView(Product product) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-padding: 20; -fx-background-color: #FFFFFF;");
        
        VBox content = new VBox(28);
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Product section with image and name
        VBox productSection = new VBox(12);
        HBox imagePane = new HBox();
        imagePane.setAlignment(Pos.CENTER);
        HBox imageBackground = new HBox();
        imageBackground.setStyle("-fx-background-color: #F5EFE7; -fx-background-radius: 12;");
        imageBackground.setAlignment(Pos.CENTER);
        
        // Placeholder: In actual implementation, load product image
        Label imagePlaceholder = new Label("☕");
        imagePlaceholder.setFont(Font.font("Segoe UI Emoji", 84));
        imagePlaceholder.setTextFill(Color.web("#F5EFE7"));
        imageBackground.setPrefWidth(200);
        imageBackground.setMinWidth(200);
        imageBackground.setMaxWidth(200);
        imageBackground.getChildren().add(imagePlaceholder);
        imagePane.getChildren().add(imageBackground);

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
    
    // Helper to create an add-on pill button with toggle behavior
    private Button createAddOnPill(String label, CheckBox checkbox, double cost) {
        Button pill = new Button(label);
        pill.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        pill.setPadding(new Insets(8, 16, 8, 16));
        pill.setStyle(getPillDefaultStyle());
        pill.setStyle("-fx-text-fill: #666666; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
        
        pill.setOnAction(e -> {
            if (checkbox != null) {
                boolean selected = !checkbox.isSelected();
                checkbox.setSelected(selected);
                if (selected) {
                    pill.setStyle(getPillSelectedStyle());
                } else {
                    pill.setStyle(getPillDefaultStyle());
                }
            }
        });
        
        pill.setOnMouseEntered(e -> {
            if (!checkbox.isSelected()) {
                pill.setStyle("-fx-text-fill: #333333; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-background-color: #F5F5F5; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
            }
        });
        
        pill.setOnMouseExited(e -> {
            if (checkbox.isSelected()) {
                pill.setStyle(getPillSelectedStyle());
            } else {
                pill.setStyle(getPillDefaultStyle());
            }
        });
        
        return pill;
    }
    
    // Style for default (unselected) add-on pill
    private String getPillDefaultStyle() {
        return "-fx-text-fill: #666666; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;";
    }
    
    // Style for selected add-on pill
    private String getPillSelectedStyle() {
        return "-fx-text-fill: #FFFFFF; -fx-border-color: #2C2C2C; -fx-border-width: 1; -fx-background-color: #2C2C2C; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;";
    }

    // Parse sugar level string like "50%" or "50% sugar" and return numeric percentage.
    // Returns 50 as a safe default on parse failure.
    private int parseSugarLevel(String level) {
        if (level == null) return 50;
        try {
            // Extract numeric portion (digits) using regex
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(level);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {}
        return 50;
    }
    
    // Update total price based on product base price + size cost
    private void updateTotalPrice(Label totalPriceLabel, Product product, double sizeCost) {
        double total = product.getPrice() + sizeCost;
        totalPriceLabel.setText("₱" + String.format("%.2f", total));
    }
    
    private VBox createCustomizationOptions(Product product, javafx.beans.property.DoubleProperty suggestionsExtra, javafx.collections.ObservableList<Product> selectedSuggestions) {
        VBox customSection = new VBox(14);
        customSection.getStyleClass().add("card");
        customSection.setPadding(new Insets(18));
        
        // Temperature selection (only for Coffee category, excluding Espresso)
        boolean isEspresso = product.getName() != null && product.getName().equalsIgnoreCase("Espresso");
        // Treat pastry/bakery/snack categories as non-drink products where no drink customizations apply
        final boolean isPastry;
        if (product.getCategory() != null) {
            String c = product.getCategory().toLowerCase();
            isPastry = c.contains("pastr") || c.contains("bakery") || c.contains("snack") || c.contains("pastry");
        } else {
            isPastry = false;
        }
        final ToggleGroup tempGroup = (product.getCategory() != null && product.getCategory().equalsIgnoreCase("Coffee") && !isEspresso) ? new ToggleGroup() : null;
        final VBox tempSection = (tempGroup != null) ? new VBox(8) : null;
        final RadioButton hotBtn = (tempGroup != null) ? new RadioButton("☕ Hot") : null;
        final RadioButton coldBtn = (tempGroup != null) ? new RadioButton("🧊 Cold") : null;
        if (tempGroup != null) {
            Label tempTitle = new Label("Temperature");
            tempTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));

            HBox tempButtons = new HBox(12);

            hotBtn.setToggleGroup(tempGroup);
            hotBtn.setUserData("Hot");
            hotBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            hotBtn.setTextFill(Color.web("#2C2C2C"));

            coldBtn.setToggleGroup(tempGroup);
            coldBtn.setUserData("Cold");
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
        }

        // Sugar level (omit for pastry/bakery items unless admin configured)
        java.util.List<String> productSugarList = product.getSugarLevels();
        System.out.println("DEBUG: detailed form product " + product.getName() + " sugarLevels: " + productSugarList);
        String[] sugarLevels;
        // Same interpretation here: null => defaults, empty => admin-disabled => hide
        if (productSugarList == null) {
            sugarLevels = new String[]{"0%", "25%", "50%", "75%", "100%"};
        } else if (productSugarList.isEmpty()) {
            sugarLevels = new String[0];
        } else {
            java.util.List<String> normalized = new java.util.ArrayList<>();
            for (String s : productSugarList) {
                normalized.add(parseSugarLevel(s) + "%");
            }
            sugarLevels = normalized.toArray(new String[0]);
        }
        boolean hasSugarOptions = sugarLevels.length > 0;
        final VBox sugarSection = hasSugarOptions && !isPastry ? new VBox(8) : null;
        final HBox sugarButtons = hasSugarOptions && !isPastry ? new HBox(8) : null;
        final ToggleGroup sugarGroup = hasSugarOptions && !isPastry ? new ToggleGroup() : null;
        final RadioButton[] sugarBtns = hasSugarOptions && !isPastry ? new RadioButton[sugarLevels.length] : null;
        if (sugarSection != null) {
            System.out.println("DEBUG: detailed sugar form building for product=" + product.getName() + " sugarLevels=" + product.getSugarLevels());
            Label sugarTitle = new Label("Sugar Level");
            sugarTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));

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
                    if (parseSugarLevel(sugarLevels[i]) == currentSugar) {
                        sugarBtns[i].setSelected(true);
                        break;
                    }
                }
            } else {
                // Try to select 50% if present, otherwise choose first available
                int idx50 = -1;
                for (int i = 0; i < sugarLevels.length; i++) if (parseSugarLevel(sugarLevels[i]) == 50) { idx50 = i; break; }
                if (idx50 >= 0) sugarBtns[idx50].setSelected(true);
                else if (sugarBtns.length > 0) sugarBtns[0].setSelected(true);
            }

            sugarSection.getChildren().addAll(new Label("Sugar Level"), sugarButtons);
        }

        // Add-ons section (omit entirely for Espresso and pastries)
        final VBox addOnsSection = (!isEspresso && !isPastry) ? new VBox(8) : null;
        final int[] extraShotQty = {1};
        final int[] whippedQty = {1};
        final int[] vanillaQty = {1};
        final int[] caramelQty = {1};
        final int[] chocolateQty = {1};
        // Milk tea topping quantities
        final int[] tapiocaQty = {1};
        final int[] jelliesQty = {1};
        final int[] poppingQty = {1};
        boolean isMilkTea = product.getCategory() != null && product.getCategory().equalsIgnoreCase("Milk Tea");
        final CheckBox extraShotCheck = (!isEspresso && !isPastry && !isMilkTea) ? createStyledCheckBox("Extra Shot (+₱1.00)") : null;
        final CheckBox whippedCreamCheck = (!isEspresso && !isPastry && !isMilkTea) ? createStyledCheckBox("Whipped Cream (+₱0.50)") : null;
        final CheckBox vanillaSyrupCheck = (!isEspresso && !isPastry && !isMilkTea) ? createStyledCheckBox("Vanilla Syrup (+₱0.75)") : null;
        final CheckBox caramelSyrupCheck = (!isEspresso && !isPastry && !isMilkTea) ? createStyledCheckBox("Caramel Syrup (+₱0.75)") : null;
        final CheckBox chocolateSyrupCheck = (!isEspresso && !isPastry && !isMilkTea) ? createStyledCheckBox("Chocolate Syrup (+₱0.75)") : null;
        // Milk tea specific toppings
        final CheckBox tapiocaCheck = (isMilkTea) ? createStyledCheckBox("Tapioca Pearls (+₱10.00)") : null;
        final CheckBox jelliesCheck = (isMilkTea) ? createStyledCheckBox("Jellies (+₱10.00)") : null;
        final CheckBox poppingCheck = (isMilkTea) ? createStyledCheckBox("Popping Boba (+₱12.00)") : null;
        // Forward-reference holder so lambdas created above can call recompute safely
        final Runnable[] recomputeRef = new Runnable[1];

            if (addOnsSection != null) {
                Label addOnsTitle = new Label("ADD-ONS");
                addOnsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                addOnsTitle.setTextFill(Color.web("#999999"));
                addOnsTitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px;");
                
                HBox addOnsGrid = new HBox(10);
                addOnsGrid.setAlignment(Pos.CENTER_LEFT);
                addOnsGrid.setStyle("-fx-wrap-text: true;");

                if (isMilkTea) {
                    // Milk Tea add-ons as toggleable pills
                    Button tapiPill = createAddOnPill("Tapioca Pearls (+₱10.00)", tapiocaCheck, 10.00);
                    Button jelliPill = createAddOnPill("Jellies (+₱10.00)", jelliesCheck, 10.00);
                    Button poppingPill = createAddOnPill("Popping Boba (+₱12.00)", poppingCheck, 12.00);

                    // Attach listeners to checkboxes so pills update when selections change
                    if (tapiocaCheck != null) {
                        tapiocaCheck.selectedProperty().addListener((obs, o, n) -> {
                            tapiPill.setStyle(n ? getPillSelectedStyle() : getPillDefaultStyle());
                            recomputeRef[0].run();
                        });
                    }
                    if (jelliesCheck != null) {
                        jelliesCheck.selectedProperty().addListener((obs, o, n) -> {
                            jelliPill.setStyle(n ? getPillSelectedStyle() : getPillDefaultStyle());
                            recomputeRef[0].run();
                        });
                    }
                    if (poppingCheck != null) {
                        poppingCheck.selectedProperty().addListener((obs, o, n) -> {
                            poppingPill.setStyle(n ? getPillSelectedStyle() : getPillDefaultStyle());
                            recomputeRef[0].run();
                        });
                    }

                    addOnsGrid.getChildren().addAll(tapiPill, jelliPill, poppingPill);

                    // Pre-select add-ons if editing existing item
                    if (customizingOrderItem != null && customizingOrderItem.getAddOns() != null) {
                        String addOns = customizingOrderItem.getAddOns();
                        if (tapiocaCheck != null && addOns.contains("Tapioca")) { tapiocaCheck.setSelected(true); tapiPill.setStyle(getPillSelectedStyle()); }
                        if (jelliesCheck != null && addOns.contains("Jellies")) { jelliesCheck.setSelected(true); jelliPill.setStyle(getPillSelectedStyle()); }
                        if (poppingCheck != null && addOns.contains("Popping")) { poppingCheck.setSelected(true); poppingPill.setStyle(getPillSelectedStyle()); }
                    }
                } else {
                    // Coffee add-ons as toggleable pills
                    Button extraShotPill = createAddOnPill("Extra Shot (+₱1.00)", extraShotCheck, 1.00);
                    Button whippedPill = createAddOnPill("Whipped Cream (+₱0.50)", whippedCreamCheck, 0.50);
                    Button vanillaPill = createAddOnPill("Vanilla Syrup (+₱0.75)", vanillaSyrupCheck, 0.75);
                    Button caramelPill = createAddOnPill("Caramel Sauce (+₱0.75)", caramelSyrupCheck, 0.75);
                    Button chocolatePill = createAddOnPill("Chocolate Syrup (+₱0.75)", chocolateSyrupCheck, 0.75);

                    // Attach listeners to update pill styles and recompute
                    if (extraShotCheck != null) {
                        extraShotCheck.selectedProperty().addListener((obs, o, n) -> {
                            extraShotPill.setStyle(n ? getPillSelectedStyle() : getPillDefaultStyle());
                            recomputeRef[0].run();
                        });
                    }
                    if (whippedCreamCheck != null) {
                        whippedCreamCheck.selectedProperty().addListener((obs, o, n) -> {
                            whippedPill.setStyle(n ? getPillSelectedStyle() : getPillDefaultStyle());
                            recomputeRef[0].run();
                        });
                    }
                    if (vanillaSyrupCheck != null) {
                        vanillaSyrupCheck.selectedProperty().addListener((obs, o, n) -> {
                            vanillaPill.setStyle(n ? getPillSelectedStyle() : getPillDefaultStyle());
                            recomputeRef[0].run();
                        });
                    }
                    if (caramelSyrupCheck != null) {
                        caramelSyrupCheck.selectedProperty().addListener((obs, o, n) -> {
                            caramelPill.setStyle(n ? getPillSelectedStyle() : getPillDefaultStyle());
                            recomputeRef[0].run();
                        });
                    }
                    if (chocolateSyrupCheck != null) {
                        chocolateSyrupCheck.selectedProperty().addListener((obs, o, n) -> {
                            chocolatePill.setStyle(n ? getPillSelectedStyle() : getPillDefaultStyle());
                            recomputeRef[0].run();
                        });
                    }

                    addOnsGrid.getChildren().addAll(extraShotPill, whippedPill, vanillaPill, caramelPill, chocolatePill);

                    // Pre-select add-ons if editing existing item
                    if (customizingOrderItem != null && customizingOrderItem.getAddOns() != null) {
                        String addOns = customizingOrderItem.getAddOns();
                        if (extraShotCheck != null && addOns.contains("Extra Shot")) { extraShotCheck.setSelected(true); extraShotPill.setStyle(getPillSelectedStyle()); }
                        if (whippedCreamCheck != null && addOns.contains("Whipped Cream")) { whippedCreamCheck.setSelected(true); whippedPill.setStyle(getPillSelectedStyle()); }
                        if (vanillaSyrupCheck != null && addOns.contains("Vanilla Syrup")) { vanillaSyrupCheck.setSelected(true); vanillaPill.setStyle(getPillSelectedStyle()); }
                        if (caramelSyrupCheck != null && addOns.contains("Caramel")) { caramelSyrupCheck.setSelected(true); caramelPill.setStyle(getPillSelectedStyle()); }
                        if (chocolateSyrupCheck != null && addOns.contains("Chocolate")) { chocolateSyrupCheck.setSelected(true); chocolatePill.setStyle(getPillSelectedStyle()); }
                    }
                }
                    addOnsSection.getChildren().addAll(addOnsTitle, addOnsGrid);
                }

        // Cup size selection (drinks): use product-configured prices, displayed as clickable pills
        Map<String, Double> _sz = null;
        try { _sz = product.getSizeSurcharges(); } catch (Exception ignored) { _sz = new HashMap<>(); }
        final double sSmall = _sz.getOrDefault("Small", 0.0);
        final double sMedium = _sz.getOrDefault("Medium", 0.0);
        final double sLarge = _sz.getOrDefault("Large", 0.0);

        boolean hasSmall2 = true, hasMedium2 = true, hasLarge2 = true;
        try { hasSmall2 = product.isHasSmall(); } catch (Exception ignored) {}
        try { hasMedium2 = product.isHasMedium(); } catch (Exception ignored) {}
        try { hasLarge2 = product.isHasLarge(); } catch (Exception ignored) {}

        final double[] selectedSizeCost = (!isPastry) ? new double[1] : null;
        if (selectedSizeCost != null) {
            if (hasSmall2) selectedSizeCost[0] = sSmall;
            else if (hasMedium2) selectedSizeCost[0] = sMedium;
            else if (hasLarge2) selectedSizeCost[0] = sLarge;
            else selectedSizeCost[0] = sSmall;
        }

        final Button[] sizeSmallPillRef = new Button[1];
        final Button[] sizeMediumPillRef = new Button[1];
        final Button[] sizeLargePillRef = new Button[1];

        final VBox sizeSection = (!isPastry) ? new VBox(8) : null;
        if (sizeSection != null) {
            Label sizeTitle = new Label("CUP SIZE");
            sizeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            sizeTitle.setTextFill(Color.web("#999999"));
            sizeTitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px;");

            HBox sizeButtons = new HBox(10);
            sizeButtons.setAlignment(Pos.CENTER_LEFT);

            if (hasSmall2) {
                // For beverages, size price IS the full price; for pastries, add to base price
                double smallPrice = isPastry ? product.getPrice() + sSmall : sSmall;
                Button sizeSmallPill = new Button(String.format("Small (₱%.2f)", smallPrice));
                sizeSmallPillRef[0] = sizeSmallPill;
                sizeSmallPill.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                sizeSmallPill.setPadding(new Insets(8, 16, 8, 16));
                sizeSmallPill.setStyle(selectedSizeCost[0] == sSmall ? getPillSelectedStyle() : getPillDefaultStyle());
                sizeSmallPill.setOnAction(e -> {
                    selectedSizeCost[0] = sSmall;
                    if (sizeSmallPillRef[0] != null) sizeSmallPillRef[0].setStyle(getPillSelectedStyle());
                    if (sizeMediumPillRef[0] != null) sizeMediumPillRef[0].setStyle(getPillDefaultStyle());
                    if (sizeLargePillRef[0] != null) sizeLargePillRef[0].setStyle(getPillDefaultStyle());
                    recomputeRef[0].run();
                });
                sizeButtons.getChildren().add(sizeSmallPill);
            }

            if (hasMedium2) {
                // For beverages, size price IS the full price; for pastries, add to base price
                double mediumPrice = isPastry ? product.getPrice() + sMedium : sMedium;
                Button sizeMediumPill = new Button(String.format("Medium (₱%.2f)", mediumPrice));
                sizeMediumPillRef[0] = sizeMediumPill;
                sizeMediumPill.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                sizeMediumPill.setPadding(new Insets(8, 16, 8, 16));
                sizeMediumPill.setStyle(selectedSizeCost[0] == sMedium ? getPillSelectedStyle() : getPillDefaultStyle());
                sizeMediumPill.setOnAction(e -> {
                    selectedSizeCost[0] = sMedium;
                    if (sizeSmallPillRef[0] != null) sizeSmallPillRef[0].setStyle(getPillDefaultStyle());
                    if (sizeMediumPillRef[0] != null) sizeMediumPillRef[0].setStyle(getPillSelectedStyle());
                    if (sizeLargePillRef[0] != null) sizeLargePillRef[0].setStyle(getPillDefaultStyle());
                    recomputeRef[0].run();
                });
                sizeButtons.getChildren().add(sizeMediumPill);
            }

            if (hasLarge2) {
                // For beverages, size price IS the full price; for pastries, add to base price
                double largePrice = isPastry ? product.getPrice() + sLarge : sLarge;
                Button sizeLargePill = new Button(String.format("Large (₱%.2f)", largePrice));
                sizeLargePillRef[0] = sizeLargePill;
                sizeLargePill.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                sizeLargePill.setPadding(new Insets(8, 16, 8, 16));
                sizeLargePill.setStyle(selectedSizeCost[0] == sLarge ? getPillSelectedStyle() : getPillDefaultStyle());
                sizeLargePill.setOnAction(e -> {
                    selectedSizeCost[0] = sLarge;
                    if (sizeSmallPillRef[0] != null) sizeSmallPillRef[0].setStyle(getPillDefaultStyle());
                    if (sizeMediumPillRef[0] != null) sizeMediumPillRef[0].setStyle(getPillDefaultStyle());
                    if (sizeLargePillRef[0] != null) sizeLargePillRef[0].setStyle(getPillSelectedStyle());
                    recomputeRef[0].run();
                });
                sizeButtons.getChildren().add(sizeLargePill);
            }

            sizeSection.getChildren().addAll(sizeTitle, sizeButtons);
        }
        
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
        
        Button minusBtn = new Button("-");
        minusBtn.setPrefSize(34, 34);
        minusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;");
        minusBtn.setOnMouseEntered(e -> minusBtn.setStyle("-fx-background-color: #F0F0F0; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #1A1A1A; -fx-border-radius: 6; -fx-background-radius: 6;"));
        minusBtn.setOnMouseExited(e -> minusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;"));
        
        Label qtyValueLabel = new Label("1");
        qtyValueLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        qtyValueLabel.setTextFill(Color.web("#1A1A1A"));
        qtyValueLabel.setPrefWidth(36);
        qtyValueLabel.setAlignment(Pos.CENTER);
        
        Button plusBtn = new Button();
        Label plusBtnLabel = new Label("+");
        plusBtnLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        plusBtn.setGraphic(plusBtnLabel);
        plusBtn.setText(null);
        plusBtn.setMnemonicParsing(false);
        plusBtn.setPrefSize(34, 34);
        plusBtn.setStyle("-fx-alignment: center; -fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;");
        plusBtn.setOnMouseEntered(e -> plusBtn.setStyle("-fx-alignment: center; -fx-background-color: #F0F0F0; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #1A1A1A; -fx-border-radius: 6; -fx-background-radius: 6;"));
        plusBtn.setOnMouseExited(e -> plusBtn.setStyle("-fx-alignment: center; -fx-background-color: #FAFAFA; -fx-cursor: hand; -fx-font-size: 18; -fx-text-fill: #333333; -fx-border-radius: 6; -fx-background-radius: 6;"));
        
        final int[] quantity = {1};
        minusBtn.setOnAction(e -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                qtyValueLabel.setText(String.valueOf(quantity[0]));
            }
        });
        plusBtn.setOnAction(e -> {
            quantity[0]++;
            qtyValueLabel.setText(String.valueOf(quantity[0]));
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
            // Extra shot may be null for certain categories (e.g., Milk Tea) or omitted for Espresso
            if (extraShotCheck != null && extraShotCheck.isSelected()) addOnsCost += 1.00 * extraShotQty[0];
            if (whippedCreamCheck != null && whippedCreamCheck.isSelected()) addOnsCost += 0.50 * whippedQty[0];
            if (vanillaSyrupCheck != null && vanillaSyrupCheck.isSelected()) addOnsCost += 0.75 * vanillaQty[0];
            if (caramelSyrupCheck != null && caramelSyrupCheck.isSelected()) addOnsCost += 0.75 * caramelQty[0];
            if (chocolateSyrupCheck != null && chocolateSyrupCheck.isSelected()) addOnsCost += 0.75 * chocolateQty[0];
            // Milk tea toppings
            if (tapiocaCheck != null && tapiocaCheck.isSelected()) addOnsCost += 10.00 * tapiocaQty[0];
            if (jelliesCheck != null && jelliesCheck.isSelected()) addOnsCost += 10.00 * jelliesQty[0];
            if (poppingCheck != null && poppingCheck.isSelected()) addOnsCost += 12.00 * poppingQty[0];

            // For beverages (products with sizes), the size value IS the price, not a surcharge
            // For pastries, use the product's base price (no size cost)
            double sizeDelta = (selectedSizeCost != null) ? selectedSizeCost[0] : 0.0;
            double base = isPastry ? product.getPrice() : 0.0; // Beverages: size IS the price, Pastries: use base price
            addOnsCost += sizeDelta;

            double suggestions = suggestionsExtra.get();
            double subtotal = (base + addOnsCost) * quantity[0] + suggestions;
            javafx.application.Platform.runLater(() -> liveTotal.setText(String.format("₱%.2f", subtotal)));
        };
        // assign the actual recompute implementation to the forward-reference
        recomputeRef[0] = recompute;

        // attach recompute listeners to controls that affect price
        if (extraShotCheck != null) extraShotCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        if (whippedCreamCheck != null) whippedCreamCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        if (vanillaSyrupCheck != null) vanillaSyrupCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        if (caramelSyrupCheck != null) caramelSyrupCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        if (chocolateSyrupCheck != null) chocolateSyrupCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        if (tapiocaCheck != null) tapiocaCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        if (jelliesCheck != null) jelliesCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());
        if (poppingCheck != null) poppingCheck.selectedProperty().addListener((obs, o, n) -> recompute.run());

        // quantity changes should also recompute
        plusBtn.setOnAction(e -> {
            quantity[0]++;
            qtyValueLabel.setText(String.valueOf(quantity[0]));
            recompute.run();
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
                String temperature = "Hot";
                if (product.getCategory() != null && product.getCategory().equalsIgnoreCase("Coffee")) {
                    javafx.scene.control.Toggle selected = tempGroup.getSelectedToggle();
                    if (selected != null && selected.getUserData() != null) {
                        temperature = selected.getUserData().toString();
                    }
                }
                int sugarLevel = 50; // default
                if (sugarBtns != null) {
                    for (int i = 0; i < sugarBtns.length; i++) {
                        if (sugarBtns[i].isSelected()) {
                            sugarLevel = parseSugarLevel(sugarLevels[i]);
                            break;
                        }
                    }
                } else {
                    // Check compact form: look for either RadioButtons or Buttons with Integer userData
                    final int[] foundSugar = {-1};
                    java.util.function.Consumer<javafx.scene.Node> findSugar = new java.util.function.Consumer<javafx.scene.Node>() {
                        @Override
                        public void accept(javafx.scene.Node node) {
                            if (node instanceof javafx.scene.control.RadioButton) {
                                javafx.scene.control.RadioButton rb = (javafx.scene.control.RadioButton) node;
                                String txt = rb.getText();
                                if (txt != null && txt.trim().endsWith("%") && rb.isSelected()) {
                                    try { foundSugar[0] = Integer.parseInt(txt.replace("%", "")); } catch (Exception ignored) {}
                                }
                            } else if (node instanceof javafx.scene.control.Button) {
                                javafx.scene.control.Button b = (javafx.scene.control.Button) node;
                                Object ud = b.getUserData();
                                if (ud instanceof Integer) {
                                    // check if visually selected (pill style)
                                    if (b.getStyle() != null && b.getStyle().equals(getPillSelectedStyle())) {
                                        foundSugar[0] = (Integer) ud;
                                    }
                                }
                            } else if (node instanceof javafx.scene.layout.Pane) {
                                javafx.scene.layout.Pane p = (javafx.scene.layout.Pane) node;
                                for (javafx.scene.Node child : p.getChildren()) accept(child);
                            }
                        }
                    };
                    for (javafx.scene.Node child : customSection.getChildren()) { findSugar.accept(child); }
                    if (foundSugar[0] != -1) sugarLevel = foundSugar[0];
                    else {
                        final boolean isPastryForScan = isPastry;
                        sugarLevel = isPastryForScan ? 0 : 50; // fallback
                    }
                }
                
                // Calculate add-ons (recompute same values used by live total)
                StringBuilder addOnsText = new StringBuilder();
                double addOnsCost = 0.0;

                // Include selected cup size (if any)
                double selSizeDelta = selectedSizeCost != null ? selectedSizeCost[0] : 0.0;
                String selSizeLabel = null;
                if (selectedSizeCost != null && selectedSizeCost[0] > 0) {
                    if (selectedSizeCost[0] == sMedium) {
                        selSizeLabel = "Medium";
                    } else if (selectedSizeCost[0] == sLarge) {
                        selSizeLabel = "Large";
                    }
                }

                // Apply extra shot only if control exists for this product
                if (extraShotCheck != null && extraShotCheck.isSelected()) {
                    addOnsText.append("Extra Shot");
                    addOnsCost += 1.00 * extraShotQty[0];
                    if (extraShotQty[0] > 1) addOnsText.append(" x").append(extraShotQty[0]);
                }
                if (whippedCreamCheck != null && whippedCreamCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Whipped Cream");
                    addOnsCost += 0.50 * whippedQty[0];
                    if (whippedQty[0] > 1) addOnsText.append(" x").append(whippedQty[0]);
                }
                if (vanillaSyrupCheck != null && vanillaSyrupCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Vanilla Syrup");
                    addOnsCost += 0.75 * vanillaQty[0];
                    if (vanillaQty[0] > 1) addOnsText.append(" x").append(vanillaQty[0]);
                }
                if (caramelSyrupCheck != null && caramelSyrupCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Caramel Syrup");
                    addOnsCost += 0.75 * caramelQty[0];
                    if (caramelQty[0] > 1) addOnsText.append(" x").append(caramelQty[0]);
                }
                if (chocolateSyrupCheck != null && chocolateSyrupCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Chocolate Syrup");
                    addOnsCost += 0.75 * chocolateQty[0];
                    if (chocolateQty[0] > 1) addOnsText.append(" x").append(chocolateQty[0]);
                }

                // Milk tea toppings
                if (tapiocaCheck != null && tapiocaCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Tapioca Pearls");
                    addOnsCost += 10.00 * tapiocaQty[0];
                    if (tapiocaQty[0] > 1) addOnsText.append(" x").append(tapiocaQty[0]);
                }
                if (jelliesCheck != null && jelliesCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Jellies");
                    addOnsCost += 10.00 * jelliesQty[0];
                    if (jelliesQty[0] > 1) addOnsText.append(" x").append(jelliesQty[0]);
                }
                if (poppingCheck != null && poppingCheck.isSelected()) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append("Popping Boba");
                    addOnsCost += 12.00 * poppingQty[0];
                    if (poppingQty[0] > 1) addOnsText.append(" x").append(poppingQty[0]);
                }

                // Append size information (don't add to addOnsCost - it's set separately via setSizeCost)
                if (selSizeLabel != null) {
                    if (addOnsText.length() > 0) addOnsText.append(", ");
                    addOnsText.append(selSizeLabel);
                }
                
                if (customizingOrderItem != null) {
                    // We're editing an existing item from cart
                    System.out.println("Debug: Updating existing cart item - " + product.getName());
                    
                    // Remove the old item
                    currentOrder.removeItem(customizingOrderItem);
                    
                    // Create updated item with new customizations but keep original quantity
                    OrderItem updatedItem = new OrderItem(product, customizingOrderItem.getQuantity(), temperature, sugarLevel);
                    updatedItem.setSizeCost(selSizeDelta); // Set size cost separately
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
                    
                    this.currentCustomizationProduct = null;
                    showCartPage();
                } else {
                    // Adding new item to cart
                    System.out.println("Debug: Creating order item - " + product.getName() + ", qty: " + quantity[0] + ", temp: " + temperature + ", sugar: " + sugarLevel);
                    OrderItem item = new OrderItem(product, quantity[0], temperature, sugarLevel);
                    item.setSizeCost(selSizeDelta); // Set size cost separately
                    
                    if (addOnsText.length() > 0) {
                        item.setAddOns(addOnsText.toString());
                        item.setAddOnsCost(addOnsCost);
                    }
    
                    System.out.println("Debug: Adding item to currentOrder - " + product.getName());
                    System.out.println("Debug: CurrentOrder before add - items: " + (currentOrder != null ? currentOrder.getItems().size() : "null"));
                    if (currentOrder == null) {
                        System.err.println("WARNING: currentOrder is null! Creating new order...");
                        currentOrder = new Order(TextDatabase.getNextOrderNumber());
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
                    
                    this.currentCustomizationProduct = null;
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
        
        // Add sections only if they were created (Espresso/pastry may omit sugar/add-ons/size)
        if (tempSection != null) customSection.getChildren().add(tempSection);
        if (sugarSection != null) customSection.getChildren().add(sugarSection);
        // Temporary fix: hide sugar for Cappuccino
        if (product.getName().equals("Cappuccino")) {
            customSection.getChildren().remove(sugarSection);
        }
        if (addOnsSection != null) customSection.getChildren().add(addOnsSection);
        if (sizeSection != null) customSection.getChildren().add(sizeSection);
        customSection.getChildren().add(bottomSection);
        
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

            // Determine if this suggestion product is a pastry/bakery or a beverage
            boolean isSuggestionPastry = false;
            try {
                String cat = p.getCategory().toLowerCase();
                isSuggestionPastry = cat.contains("pastr") || cat.contains("bakery") || cat.contains("snack") || cat.contains("pastry");
            } catch (Exception ignored) {}

            // Calculate the display price - for pastries use base price, for beverages use smallest size price
            final double displayPrice;
            if (isSuggestionPastry) {
                displayPrice = p.getPrice();
            } else {
                // Beverage - get the default (smallest) size price
                Map<String, Double> sizes = null;
                try { sizes = p.getSizeSurcharges(); } catch (Exception ignored) {}
                if (sizes != null && !sizes.isEmpty()) {
                    // Get the smallest size price (first entry or minimum)
                    displayPrice = sizes.values().stream().min(Double::compare).orElse(0.0);
                } else {
                    displayPrice = p.getPrice(); // fallback
                }
            }

            HBox cardRow = new HBox(12);
            cardRow.setPadding(new Insets(10));
            cardRow.setAlignment(Pos.CENTER_LEFT);
            cardRow.setStyle("-fx-background-color: white; -fx-background-radius: 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);");
            cardRow.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cardRow, Priority.ALWAYS);

            // Image thumbnail (try product image then fallback to emoji)
            StackPane img = new StackPane();
            img.setPrefSize(110, 90);
            javafx.scene.image.ImageView thumb = loadProductImage(p.getId());
            if (thumb != null) {
                try {
                    // Size it to fill the thumbnail area while preserving aspect ratio
                    thumb.setFitWidth(110);
                    thumb.setFitHeight(90);
                    thumb.setPreserveRatio(true);
                    thumb.setSmooth(true);
                    img.getChildren().add(thumb);
                } catch (Exception ignored) {
                    // fallback to emoji below
                    String grad = p.getName().toLowerCase().contains("espresso") ? "linear-gradient(to bottom right, #2C2C2C, #1A1A1A)" : "linear-gradient(to bottom right, #505050, #2C2C2C)";
                    img.setStyle("-fx-background-color: " + grad + "; -fx-background-radius: 0; -fx-border-radius: 0;");
                    Label emoji = new Label("☕");
                    emoji.setFont(Font.font("Segoe UI Emoji", 28));
                    emoji.setTextFill(Color.web("#F5EFE7"));
                    img.getChildren().add(emoji);
                }
            } else {
                String grad = p.getName().toLowerCase().contains("espresso") ? "linear-gradient(to bottom right, #2C2C2C, #1A1A1A)" : "linear-gradient(to bottom right, #505050, #2C2C2C)";
                img.setStyle("-fx-background-color: " + grad + "; -fx-background-radius: 0; -fx-border-radius: 0;");
                Label emoji = new Label("☕");
                emoji.setFont(Font.font("Segoe UI Emoji", 28));
                emoji.setTextFill(Color.web("#F5EFE7"));
                img.getChildren().add(emoji);
            }

            // Middle: title and unit price
            VBox mid = new VBox(6);
            mid.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(p.getName());
            name.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
            name.setTextFill(Color.web("#1A1A1A"));

            Label unitPrice = new Label(String.format("₱%.2f", displayPrice));
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
            Button minus = new Button("-");
            minus.setPrefSize(30, 30);
            minus.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
            Button plus = new Button();
            Label plusLbl = new Label("+");
            plusLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            plus.setGraphic(plusLbl);
            plus.setText(null);
            plus.setMnemonicParsing(false);
            plus.setPrefSize(30, 30);
            plus.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
            final int[] qty = {1};
            Label qtyLabel = new Label(String.valueOf(qty[0]));
            qtyLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
            qtyLabel.setPrefWidth(28);
            qtyLabel.setAlignment(Pos.CENTER);
            
            // Total price label (qty * unit price) - declare early so handlers can reference it
            Label totalPriceLabel = new Label(String.format("₱%.2f", displayPrice * qty[0]));
            totalPriceLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            totalPriceLabel.setTextFill(Color.web("#1A1A1A"));

            minus.setOnAction(ev -> {
                if (qty[0] > 1) {
                    qty[0]--;
                    qtyLabel.setText(String.valueOf(qty[0]));
                    if (addChk.isSelected()) {
                        double delta = -displayPrice;
                        suggestionsExtra.set(suggestionsExtra.get() + delta);
                    }
                    totalPriceLabel.setText(String.format("₱%.2f", displayPrice * qty[0]));
                }
            });
            plus.setOnAction(ev -> {
                qty[0]++;
                qtyLabel.setText(String.valueOf(qty[0]));
                if (addChk.isSelected()) {
                    double delta = displayPrice;
                    suggestionsExtra.set(suggestionsExtra.get() + delta);
                }
                totalPriceLabel.setText(String.format("₱%.2f", displayPrice * qty[0]));
            });

            qtyBox.getChildren().addAll(minus, qtyLabel, plus);
            // When checkbox toggles, update suggestionsExtra and selectedSuggestions
            addChk.selectedProperty().addListener((obs, oldV, newV) -> {
                double delta = newV ? displayPrice * qty[0] : -displayPrice * qty[0];
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
        
        // For beverages, sizeCost IS the price; for pastries, add base price
        boolean isPastryItem = false;
        if (item.getProduct().getCategory() != null) {
            String cat = item.getProduct().getCategory().toLowerCase();
            isPastryItem = cat.contains("pastr") || cat.contains("bakery") || cat.contains("snack") || cat.contains("pastry");
        }
        double basePrice = isPastryItem ? item.getProduct().getPrice() : 0.0;
        double unitPrice = basePrice + item.getSizeCost() + item.getAddOnsCost();
        double subtotal = unitPrice * totalQty;
        Label priceLabel = new Label("₱" + String.format("%.2f", subtotal));
        priceLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 18));
        priceLabel.setTextFill(Color.web("#1A1A1A"));
        
        // Quantity controls with minus and plus buttons
        HBox controls = new HBox(6);
        controls.setAlignment(Pos.CENTER_RIGHT);
        // Remove background so quantity controls are flush and minimal
        controls.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        Button minusBtn = new Button("-");
        minusBtn.setPrefSize(32, 32);
        minusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: transparent; -fx-background-radius: 6; -fx-font-size: 16px; -fx-text-fill: #757575; -fx-cursor: hand;");
        minusBtn.setOnMouseEntered(e -> minusBtn.setStyle("-fx-background-color: #F0F0F0; -fx-border-color: transparent; -fx-background-radius: 6; -fx-font-size: 16px; -fx-text-fill: #1A1A1A; -fx-cursor: hand;"));
        minusBtn.setOnMouseExited(e -> minusBtn.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: transparent; -fx-background-radius: 6; -fx-font-size: 16px; -fx-text-fill: #757575; -fx-cursor: hand;"));

        Label qtyLabel = new Label(String.valueOf(totalQty));
        qtyLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        qtyLabel.setTextFill(Color.web("#1A1A1A"));
        qtyLabel.setPrefWidth(32);
        qtyLabel.setAlignment(Pos.CENTER);

        Button plusBtn = new Button();
        Label plusBtnLbl = new Label("+");
        plusBtnLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        plusBtn.setGraphic(plusBtnLbl);
        plusBtn.setText(null);
        plusBtn.setMnemonicParsing(false);
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
        
        // Add Order ID display
        HBox orderIdBox = new HBox();
        orderIdBox.setAlignment(Pos.CENTER);
        orderIdBox.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 8; -fx-padding: 15;");
        
        Label orderIdLabel = new Label("Order #:");
        orderIdLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        orderIdLabel.setTextFill(Color.web("#666666"));
        
        Label orderIdValue = new Label(currentOrder.getOrderId());
        orderIdValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        orderIdValue.setTextFill(Color.web("#1A1A1A"));
        orderIdValue.setStyle("-fx-padding: 0 10 0 10;");
        
        orderIdBox.getChildren().addAll(orderIdLabel, orderIdValue);
        
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
            
            // For beverages, sizeCost IS the price; for pastries, add base price
            boolean isPastrySample = false;
            if (sample.getProduct().getCategory() != null) {
                String cat = sample.getProduct().getCategory().toLowerCase();
                isPastrySample = cat.contains("pastr") || cat.contains("bakery") || cat.contains("snack") || cat.contains("pastry");
            }
            double basePrice = isPastrySample ? sample.getProduct().getPrice() : 0.0;
            double unitPrice = basePrice + sample.getSizeCost() + sample.getAddOnsCost();
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
        
        summarySection.getChildren().addAll(summaryTitle, orderIdBox, itemsList, separator, totalRow);
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

        // Process payment and complete order
        try {
            store.checkoutBasket(currentOrder);
            
            // Persist a pending order so the cashier can pick it up (name will be set by cashier)
            try {
                // Build PendingOrder from currentOrder - customer name is "Guest" until cashier sets it
                PendingOrder p = new PendingOrder(currentOrder.getOrderId(), "Guest", (orderType != null && !orderType.isEmpty()) ? orderType : "Dine In", currentCashierId);
                for (OrderItem oi : currentOrder.getItems()) {
                    // For beverages, sizeCost IS the price (base=0); for pastries, base price is product.getPrice()
                    // Store base price separately - sizeCost and addOnsCost are stored in their own fields
                    boolean isPastryPending = false;
                    if (oi.getProduct().getCategory() != null) {
                        String cat = oi.getProduct().getCategory().toLowerCase();
                        isPastryPending = cat.contains("pastr") || cat.contains("bakery") || cat.contains("snack") || cat.contains("pastry");
                    }
                    double basePrice = isPastryPending ? oi.getProduct().getPrice() : 0.0;
                    String size = oi.getSize();
                    double sizeCost = oi.getSizeCost();
                    // Pass basePrice as price - sizeCost and addOnsCost are added separately by CashierApp
                    p.addItem(oi.getProduct().getName(), basePrice, oi.getQuantity(), oi.getTemperature(), oi.getSugarLevel(), oi.getAddOns(), oi.getAddOnsCost(), oi.getSpecialRequest(), size, sizeCost);
                }
                TextDatabase.savePendingOrder(p);
            } catch (Exception ex) {
                System.err.println("Error saving pending order: " + ex.getMessage());
            }

            // Capture order info before resetting state
            final String completedOrderId = currentOrder.getOrderId();
            final double completedTotal = currentOrder.getTotalAmount();
            final String completedPaymentMethod = paymentMethod;
            
            // Reset all state for new order FIRST
            currentCustomizationProduct = null;
            customizingOrderItem = null;
            currentOrder = new Order(TextDatabase.getNextOrderNumber());
            
            // Show receipt-style confirmation screen AFTER state is reset
            showOrderReceiptScreen(completedOrderId, completedPaymentMethod, completedTotal);
            
        } catch (IllegalStateException e) {
            // Handle inventory issues specifically
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Insufficient")) {
                String insufficientItem = store.getInsufficientIngredient(currentOrder);
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Order Failed");
                errorAlert.setHeaderText("Insufficient Inventory");
                errorAlert.setContentText("Sorry, we don't have enough " + (insufficientItem != null ? insufficientItem : "ingredients") + " to complete this order.\n\nPlease try again later.");
                errorAlert.showAndWait();
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Order Failed");
                errorAlert.setHeaderText("Payment Failed");
                errorAlert.setContentText(errorMsg != null ? errorMsg : "There was an error processing your order. Please try again.");
                errorAlert.showAndWait();
            }
        } catch (NullPointerException e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Order Failed");
            errorAlert.setHeaderText("Invalid Order");
            errorAlert.setContentText("One or more items in your order are invalid.\n\nPlease try again.");
            errorAlert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Order Failed");
            errorAlert.setHeaderText("Payment Failed");
            errorAlert.setContentText("There was an error processing your order.\n\nError: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
    
    /**
     * Show a receipt-style confirmation screen and auto-return to welcome screen
     */
    private void showOrderReceiptScreen(String orderId, String paymentMethod, double total) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #F5F5F5;");
        
        VBox receiptCard = new VBox(20);
        receiptCard.setAlignment(Pos.CENTER);
        receiptCard.setPadding(new Insets(50));
        receiptCard.setMaxWidth(500);
        receiptCard.setMaxHeight(600);
        receiptCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 5);");
        
        // Success icon
        Label checkIcon = new Label("✓");
        checkIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 60));
        checkIcon.setTextFill(Color.WHITE);
        StackPane iconCircle = new StackPane(checkIcon);
        iconCircle.setPrefSize(100, 100);
        iconCircle.setMaxSize(100, 100);
        iconCircle.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 50;");
        
        // Thank you message
        Label thankYou = new Label("Thank You!");
        thankYou.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        thankYou.setTextFill(Color.web("#1A1A1A"));
        
        Label orderPlaced = new Label("Your order has been placed");
        orderPlaced.setFont(Font.font("Segoe UI", 18));
        orderPlaced.setTextFill(Color.web("#666666"));
        
        // Receipt details
        VBox receiptDetails = new VBox(12);
        receiptDetails.setAlignment(Pos.CENTER);
        receiptDetails.setPadding(new Insets(20));
        receiptDetails.setStyle("-fx-background-color: #F9F9F9; -fx-background-radius: 10;");
        
        Label orderLabel = new Label("Order #" + orderId);
        orderLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        orderLabel.setTextFill(Color.web("#333333"));
        
        Label totalLabel = new Label("Total: ₱" + String.format("%.2f", total));
        totalLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 20));
        totalLabel.setTextFill(Color.web("#4CAF50"));
        
        Label paymentLabel = new Label("Payment: " + paymentMethod);
        paymentLabel.setFont(Font.font("Segoe UI", 16));
        paymentLabel.setTextFill(Color.web("#666666"));
        
        Label typeLabel = new Label("Order Type: " + orderType);
        typeLabel.setFont(Font.font("Segoe UI", 16));
        typeLabel.setTextFill(Color.web("#666666"));
        
        receiptDetails.getChildren().addAll(orderLabel, totalLabel, paymentLabel, typeLabel);
        
        // Instructions
        Label instructions = new Label("Please proceed to the counter.\nThe cashier will ask for your name.");
        instructions.setFont(Font.font("Segoe UI", 14));
        instructions.setTextFill(Color.web("#888888"));
        instructions.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        instructions.setWrapText(true);
        
        // Auto-redirect countdown
        Label countdown = new Label("Returning to menu in 30 seconds...");
        countdown.setFont(Font.font("Segoe UI", 12));
        countdown.setTextFill(Color.web("#AAAAAA"));
        
        // Done button
        Button doneBtn = new Button("Done");
        doneBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        doneBtn.setPadding(new Insets(15, 60, 15, 60));
        doneBtn.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand;");
        doneBtn.setOnMouseEntered(e -> doneBtn.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand;"));
        doneBtn.setOnMouseExited(e -> doneBtn.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand;"));
        
        receiptCard.getChildren().addAll(iconCircle, thankYou, orderPlaced, receiptDetails, instructions, countdown, doneBtn);
        root.getChildren().add(receiptCard);
        
        // Block any pending async scene changes from overwriting the receipt screen
        showingReceiptScreen = true;
        
        // Set scene directly to ensure it displays immediately (not async)
        if (persistentRoot != null) {
            persistentRoot.getChildren().setAll(root);
        } else {
            Scene scene = new Scene(root, 1600, 900);
            primaryStage.setScene(scene);
        }
        if (!primaryStage.isShowing()) primaryStage.show();
        
        // Force layout update to ensure scene is visible
        root.applyCss();
        root.layout();
        
        // Auto-return to welcome screen after 7 seconds
        final int[] secondsLeft = {7};
        Timeline autoReturn = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            secondsLeft[0]--;
            if (secondsLeft[0] > 0) {
                countdown.setText("Returning to menu in " + secondsLeft[0] + " seconds...");
            } else {
                showingReceiptScreen = false;
                showWelcomeScreen();
            }
        }));
        autoReturn.setCycleCount(7);
        autoReturn.play();
        
        // Done button returns immediately
        doneBtn.setOnAction(e -> {
            autoReturn.stop();
            showingReceiptScreen = false;
            showWelcomeScreen();
        });
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
                    // Ensure the image view does not consume mouse events so parent card handles clicks
                    imageView.setMouseTransparent(true);
                    imageView.setFocusTraversable(false);
                    imageView.setSmooth(true);
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
