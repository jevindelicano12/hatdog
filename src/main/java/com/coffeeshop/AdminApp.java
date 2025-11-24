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
    private TextArea alertsArea;
    private Label netSalesLabel;
    private Label pendingOrdersLabel;
    private Label completedOrdersLabel;
    private Label lowStockLabel;
    private javafx.scene.control.ListView<String> categoriesListView;
    private TextArea dashboardAlertsArea;
    private javafx.scene.control.ListView<String> productsInCategoryListView;

    @Override
    public void start(Stage primaryStage) {
        store = Store.getInstance();

        primaryStage.setTitle("Coffee Shop - Admin Panel");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Header
        VBox header = createHeader();
        root.setTop(header);

        // Center - Tabs
        TabPane tabPane = createTabPane();
        root.setCenter(tabPane);

        // Show admin login BEFORE setting up the scene
        boolean ok = showAdminLogin(null);
        if (!ok) {
            // User cancelled or failed to login - exit application
            javafx.application.Platform.exit();
            return;
        }

        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);

        // Apply bundled Atlantafx fallback stylesheet (no external dependency required)
        applyAtlantafx(scene);

        primaryStage.setMaximized(true);
        primaryStage.show();

        refreshData();
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

    

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("🛡️ Coffee Shop - Admin Panel");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#D32F2F"));

        Label subtitle = new Label("Manage products, inventory, and view refill alerts");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#666"));

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();

        Tab dashboardTab = new Tab("📊 Dashboard", createDashboardTab());
        dashboardTab.setClosable(false);

        Tab productsTab = new Tab("Product Management", createProductsTab());
        productsTab.setClosable(false);

        Tab inventoryTab = new Tab("Inventory Management", createInventoryTab());
        inventoryTab.setClosable(false);

        Tab refillTab = new Tab("Refill Status", createRefillTab());
        refillTab.setClosable(false);

        Tab categoriesTab = new Tab("Categories", createCategoriesTab());
        categoriesTab.setClosable(false);

        Tab salesTab = new Tab("Sales Reports", createSalesTab());
        salesTab.setClosable(false);

        Tab accountsTab = new Tab("Accounts", createAccountsTab());
        accountsTab.setClosable(false);

        tabPane.getTabs().addAll(dashboardTab, productsTab, inventoryTab, refillTab, categoriesTab, salesTab, accountsTab);
        return tabPane;
    }

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
        actionsCol.setPrefWidth(300);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(8);
            private final Button toggleBtn = new Button();
            private final Button pwdBtn = new Button("Change Password");

            {
                toggleBtn.setStyle("-fx-font-weight: bold;");
                pwdBtn.setStyle("-fx-font-size: 11px;");
                box.getChildren().addAll(toggleBtn, pwdBtn);
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
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));

        Label title = new Label("📊 Dashboard - Business Overview");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#1565C0"));

        // Statistics Cards Row
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);

        // Create labels that will be updated
        netSalesLabel = new Label();
        netSalesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        netSalesLabel.setTextFill(Color.web("#2E7D32"));

        pendingOrdersLabel = new Label();
        pendingOrdersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        pendingOrdersLabel.setTextFill(Color.web("#FF6B6B"));

        completedOrdersLabel = new Label();
        completedOrdersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        completedOrdersLabel.setTextFill(Color.web("#1565C0"));

        lowStockLabel = new Label();
        lowStockLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        lowStockLabel.setTextFill(Color.web("#FFA726"));

        // Create stat cards using labels
        VBox salesCard = createStatCard("💰 Net Sales (This Month)", netSalesLabel, "#2E7D32");
        VBox pendingCard = createStatCard("⏳ Pending Orders", pendingOrdersLabel, "#FF6B6B");
        VBox completedCard = createStatCard("✅ Completed Orders", completedOrdersLabel, "#1565C0");
        VBox alertCard = createStatCard("⚠️ Low Stock Alerts", lowStockLabel, "#FFA726");

        statsRow.getChildren().addAll(salesCard, pendingCard, completedCard, alertCard);

        // Refill Alerts Section
        VBox alertsSection = new VBox(10);
        alertsSection.setPadding(new Insets(20));
        alertsSection.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label alertsTitle = new Label("⚠️ Refill Alerts");
        alertsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        dashboardAlertsArea = new TextArea();
        dashboardAlertsArea.setEditable(false);
        dashboardAlertsArea.setPrefHeight(200);
        dashboardAlertsArea.setWrapText(true);
        dashboardAlertsArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

        Button refreshBtn = new Button("🔄 Refresh Dashboard");
        refreshBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        refreshBtn.setOnAction(e -> refreshDashboard());

        alertsSection.getChildren().addAll(alertsTitle, dashboardAlertsArea, refreshBtn);

        panel.getChildren().addAll(title, new Separator(), statsRow, new Separator(), alertsSection);
        // Initial data load
        updateDashboardData();

        return panel;
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

        Button editButton = new Button("Edit Product");
        editButton.setStyle("-fx-background-color: #0277BD; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        editButton.setOnAction(e -> editProduct());

        Button removeButton = new Button("Remove Product");
        removeButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        removeButton.setOnAction(e -> removeProduct());

        Button addButton = new Button("Add New Product");
        addButton.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        addButton.setDisable(false);
        addButton.setFocusTraversable(true);
        addButton.setMouseTransparent(false);
        addButton.setOnAction(e -> addNewProduct());
        // Defensive mouse-click handler in case an overlay prevents normal action events
        addButton.setOnMouseClicked(e -> {
            System.out.println("DEBUG: Add New Product clicked");
            addNewProduct();
        });

        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #FFA726; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
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

        TableColumn<InventoryRow, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(100);

        inventoryTable.getColumns().addAll(nameCol, quantityCol, unitCol);

        // Controls
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button refillIngredientButton = new Button("Refill Ingredient");
        refillIngredientButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        refillIngredientButton.setOnAction(e -> refillIngredient());

        Button addIngredientButton = new Button("Add Ingredient");
        addIngredientButton.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        addIngredientButton.setOnAction(e -> addIngredient());

        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #FFA726; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        refreshButton.setOnAction(e -> refreshData());

        controls.getChildren().addAll(refillIngredientButton, addIngredientButton, refreshButton);

        panel.getChildren().addAll(title, inventoryTable, controls);
        return panel;
    }

    private VBox createRefillTab() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label title = new Label("⚠️ Refill Status & Alerts");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        Label infoLabel = new Label("Products needing refill (stock ≤ 5):");
        infoLabel.setFont(Font.font("Arial", 14));

        alertsArea = new TextArea();
        alertsArea.setEditable(false);
        alertsArea.setFont(Font.font("Courier New", 12));
        alertsArea.setPrefHeight(500);
        alertsArea.setStyle("-fx-control-inner-background: #fff3cd;");

        Button refreshButton = new Button("Refresh Alerts");
        refreshButton.setStyle("-fx-background-color: #FFA726; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        refreshButton.setOnAction(e -> refreshRefillAlerts());

        panel.getChildren().addAll(title, infoLabel, alertsArea, refreshButton);
        return panel;
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

        // Refresh alerts
        refreshRefillAlerts();
    }

    private void refreshRefillAlerts() {
        if (store.hasProductsNeedingRefill()) {
            alertsArea.setText(store.getProductRefillAlerts());
            alertsArea.setStyle("-fx-control-inner-background: #fff3cd;");
        } else {
            alertsArea.setText("\n\n✓ All products are sufficiently stocked!\n\nNo refill alerts at this time.");
            alertsArea.setStyle("-fx-control-inner-background: #d4edda;");
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

        // Name (read-only)
        Label nameLabel = new Label("Name:");
        Label nameValueLabel = new Label(product.getName());

        // Price (editable)
        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        priceField.setPromptText("Enter price");

        // Stock (editable - for adding stock)
        grid.add(idLabel, 0, 0);
        grid.add(idValueLabel, 1, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(nameValueLabel, 1, 1);
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}
