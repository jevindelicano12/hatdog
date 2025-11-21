package com.coffeeshop;

import com.coffeeshop.model.*;
import com.coffeeshop.service.Store;
import com.coffeeshop.service.TextDatabase;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminApp extends Application {
    private Store store;
    private TableView<ProductRow> productTable;
    private TableView<InventoryRow> inventoryTable;
    private TextArea alertsArea;
    private Label netSalesLabel;
    private Label pendingOrdersLabel;
    private Label completedOrdersLabel;
    private Label lowStockLabel;
    private TextArea dashboardAlertsArea;

    @Override
    public void start(Stage primaryStage) {
        store = Store.getInstance();

        primaryStage.setTitle("Coffee Shop - Admin Panel");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F4F1EA;");

        // Header
        VBox header = createHeader();
        root.setTop(header);

        // Center - Tabs
        TabPane tabPane = createTabPane();
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        refreshData();
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("⚙️ Coffee Shop - Admin Panel");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#3E2723"));

        Label subtitle = new Label("Manage products, inventory, and view refill alerts");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#795548"));

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

        tabPane.getTabs().addAll(dashboardTab, productsTab, inventoryTab, refillTab);
        return tabPane;
    }

    private VBox createDashboardTab() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        
        Label title = new Label("📊 Dashboard - Business Overview");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#3E2723"));
        
        // Statistics Cards Row
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);
        
        // Create labels that will be updated
        netSalesLabel = new Label();
        netSalesLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        netSalesLabel.setTextFill(Color.web("#4CAF50"));
        
        pendingOrdersLabel = new Label();
        pendingOrdersLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        pendingOrdersLabel.setTextFill(Color.web("#FF7043"));
        
        completedOrdersLabel = new Label();
        completedOrdersLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        completedOrdersLabel.setTextFill(Color.web("#1976D2"));
        
        lowStockLabel = new Label();
        lowStockLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        lowStockLabel.setTextFill(Color.web("#FFA000"));
        
        // Create stat cards using labels
        VBox salesCard = createStatCard("💰 Net Sales (This Month)", netSalesLabel, "#4CAF50");
        VBox pendingCard = createStatCard("⏳ Pending Orders", pendingOrdersLabel, "#FF7043");
        VBox completedCard = createStatCard("✅ Completed Orders", completedOrdersLabel, "#1976D2");
        VBox alertCard = createStatCard("⚠️ Low Stock Alerts", lowStockLabel, "#FFA000");
        
        statsRow.getChildren().addAll(salesCard, pendingCard, completedCard, alertCard);
        
        // Refill Alerts Section
        VBox alertsSection = new VBox(10);
        alertsSection.setPadding(new Insets(20));
        alertsSection.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        
        Label alertsTitle = new Label("⚠️ Refill Alerts");
        alertsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        alertsTitle.setTextFill(Color.web("#3E2723"));
        
        dashboardAlertsArea = new TextArea();
        dashboardAlertsArea.setEditable(false);
        dashboardAlertsArea.setPrefHeight(200);
        dashboardAlertsArea.setWrapText(true);
        dashboardAlertsArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-control-inner-background: #FAFAFA;");
        
        Button refreshBtn = new Button("🔄 Refresh Dashboard");
        refreshBtn.setStyle("-fx-background-color: #6F4E37; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
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
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        card.setPrefWidth(250);
        card.setPrefHeight(120);
        
        Label titleLabel = new Label(label);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        titleLabel.setTextFill(Color.web("#795548"));
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.CENTER);
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        valueLabel.setTextFill(Color.web(color));
        
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }
    
    private VBox createStatCard(String label, Label valueLabel, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        card.setPrefWidth(250);
        card.setPrefHeight(120);
        
        Label titleLabel = new Label(label);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        titleLabel.setTextFill(Color.web("#795548"));
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
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#3E2723"));

        // Product table
        productTable = new TableView<>();
        productTable.setPrefHeight(400);
        productTable.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 8;");

        TableColumn<ProductRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(100);

        TableColumn<ProductRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<ProductRow, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);

        // stock column removed from product management display (stock still tracked internally)

        TableColumn<ProductRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        productTable.getColumns().addAll(idCol, nameCol, priceCol, statusCol);

        // Controls
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button refillButton = new Button("Refill Product");
        refillButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        refillButton.setOnAction(e -> refillProduct());

        Button removeButton = new Button("Remove Product");
        removeButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        removeButton.setOnAction(e -> removeProduct());

        Button addButton = new Button("Add New Product");
        addButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        addButton.setOnAction(e -> addNewProduct());

        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #FFA000; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        refreshButton.setOnAction(e -> refreshData());

        controls.getChildren().addAll(refillButton, removeButton, addButton, refreshButton);

        panel.getChildren().addAll(title, productTable, controls);
        return panel;
    }

    private VBox createInventoryTab() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label title = new Label("📊 Inventory Management");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#3E2723"));

        // Inventory table
        inventoryTable = new TableView<>();
        inventoryTable.setPrefHeight(400);
        inventoryTable.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 8;");

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
        refillIngredientButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        refillIngredientButton.setOnAction(e -> refillIngredient());

        Button addIngredientButton = new Button("Add Ingredient");
        addIngredientButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        addIngredientButton.setOnAction(e -> addIngredient());

        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #FFA000; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        refreshButton.setOnAction(e -> refreshData());

        controls.getChildren().addAll(refillIngredientButton, addIngredientButton, refreshButton);

        panel.getChildren().addAll(title, inventoryTable, controls);
        return panel;
    }

    private VBox createRefillTab() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label title = new Label("⚠️ Refill Status & Alerts");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#3E2723"));

        Label infoLabel = new Label("Products needing refill (stock ≤ 5):");
        infoLabel.setFont(Font.font("Segoe UI", 14));
        infoLabel.setTextFill(Color.web("#795548"));

        alertsArea = new TextArea();
        alertsArea.setEditable(false);
        alertsArea.setFont(Font.font("Consolas", 12));
        alertsArea.setPrefHeight(500);
        alertsArea.setStyle("-fx-control-inner-background: #FFF3E0; -fx-background-color: #FFF3E0;");

        Button refreshButton = new Button("Refresh Alerts");
        refreshButton.setStyle("-fx-background-color: #FFA000; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
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
            } else if (p.getStock() <= Store.REFILL_THRESHOLD) {
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

        // Refresh alerts
        refreshRefillAlerts();
    }

    private void refreshRefillAlerts() {
        if (store.hasProductsNeedingRefill()) {
            alertsArea.setText(store.getProductRefillAlerts());
            alertsArea.setStyle("-fx-control-inner-background: #FFF3E0;");
        } else {
            alertsArea.setText("\n\n✓ All products are sufficiently stocked!\n\nNo refill alerts at this time.");
            alertsArea.setStyle("-fx-control-inner-background: #E8F5E9;");
        }
    }

    private void refillProduct() {
        ProductRow selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a product to refill.", Alert.AlertType.WARNING);
            return;
        }
        // fetch real product to get current stock
        Product prod = store.getProductById(selected.getId());
        if (prod == null) {
            showAlert("Error", "Selected product not found.", Alert.AlertType.ERROR);
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refill Product");
        dialog.setHeaderText("Refill: " + selected.getName());
        dialog.setContentText("Enter amount to add (max " + (Store.MAX_STOCK - prod.getStock()) + "):\n");

        dialog.showAndWait().ifPresent(amount -> {
            try {
                int refillAmount = Integer.parseInt(amount);
                if (refillAmount <= 0) {
                    showAlert("Invalid Amount", "Please enter a positive number.", Alert.AlertType.ERROR);
                    return;
                }

                store.refillProduct(selected.getId(), refillAmount);
                showAlert("Success", "Product refilled successfully!", Alert.AlertType.INFORMATION);
                refreshData();
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter a valid number.", Alert.AlertType.ERROR);
            }
        });
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

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField idField = new TextField();
        idField.setPromptText("P006");
        TextField nameField = new TextField();
        nameField.setPromptText("Product Name");
        TextField priceField = new TextField();
        priceField.setPromptText("5.00");

        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String id = idField.getText();
                    String name = nameField.getText();
                    double price = Double.parseDouble(priceField.getText());
                    return new Product(id, name, price, 20, new HashMap<>());
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(product -> {
            if (product != null) {
                // Ask for ingredients for the new product — show inventory checkboxes and allow custom entries
                Dialog<Map<String, Double>> ingDialog = new Dialog<>();
                ingDialog.setTitle("Product Ingredients");
                ingDialog.setHeaderText("Define ingredients for: " + product.getName());

                ButtonType okType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
                ingDialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

                // Content: list of existing inventory items with checkbox + amount field
                VBox content = new VBox(8);
                content.setPadding(new Insets(10));

                Map<String, TextField> existingAmountFields = new HashMap<>();

                Label existingLabel = new Label("Select existing inventory items and enter amount needed per serving:");
                content.getChildren().add(existingLabel);

                Map<String, InventoryItem> inv = store.getInventory();
                if (inv.isEmpty()) {
                    content.getChildren().add(new Label("(No inventory items yet)"));
                } else {
                    for (InventoryItem ii : inv.values()) {
                        HBox row = new HBox(8);
                        row.setAlignment(Pos.CENTER_LEFT);
                        CheckBox cb = new CheckBox();
                        Label nameLbl = new Label(ii.getName() + " (" + ii.getUnit() + ")");
                        TextField amtField = new TextField();
                        amtField.setPromptText("amount");
                        amtField.setPrefWidth(100);
                        amtField.setDisable(true);
                        cb.selectedProperty().addListener((obs, oldV, newV) -> amtField.setDisable(!newV));
                        row.getChildren().addAll(cb, nameLbl, amtField);
                        content.getChildren().add(row);
                        existingAmountFields.put(ii.getName(), amtField);
                    }
                }

                // Section for custom ingredients (multiple)
                Separator sep = new Separator();
                Label customLabel = new Label("Add custom ingredients (one per line) in format name:amount[:unit] :");
                TextArea customArea = new TextArea();
                customArea.setPromptText("Sugar:10\nVanilla Syrup:15:ml");
                customArea.setPrefRowCount(6);

                content.getChildren().addAll(sep, customLabel, customArea);

                ingDialog.getDialogPane().setContent(content);

                ingDialog.setResultConverter(button -> {
                    if (button == okType) {
                        Map<String, Double> recipe = new HashMap<>();
                        // collect existing selected
                        for (Map.Entry<String, TextField> e : existingAmountFields.entrySet()) {
                            TextField tf = e.getValue();
                            if (!tf.isDisabled()) {
                                String txt = tf.getText();
                                try {
                                    double amt = Double.parseDouble(txt);
                                    if (amt > 0) recipe.put(e.getKey(), amt);
                                } catch (NumberFormatException ex) {
                                    // skip invalid
                                }
                            }
                        }

                        // parse customArea lines
                        String custom = customArea.getText();
                        if (custom != null && !custom.trim().isEmpty()) {
                            String[] lines = custom.split("\\r?\\n");
                            for (String line : lines) {
                                String l = line.trim();
                                if (l.isEmpty()) continue;
                                String[] parts = l.split(":" );
                                if (parts.length >= 2) {
                                    String iname = parts[0].trim();
                                    try {
                                        double amt = Double.parseDouble(parts[1].trim());
                                        recipe.put(iname, amt);
                                        // ensure inventory has this ingredient; add with default large qty if missing
                                        if (store.getInventoryItem(iname) == null) {
                                            String unit = parts.length >= 3 ? parts[2].trim() : "unit";
                                            store.addInventoryItem(new InventoryItem(iname, 1000.0, unit));
                                        }
                                    } catch (NumberFormatException ex) {
                                        // skip invalid number
                                    }
                                }
                            }
                        }

                        return recipe;
                    }
                    return null;
                });

                ingDialog.showAndWait().ifPresent(recipe -> {
                    if (recipe != null) {
                        product.setRecipe(recipe);
                        store.addProduct(product);
                        showAlert("Success", "Product added successfully!", Alert.AlertType.INFORMATION);
                        refreshData();
                    }
                });
            } else {
                showAlert("Error", "Invalid input. Please try again.", Alert.AlertType.ERROR);
            }
        });
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
        // Allow adding multiple ingredients at once (one per line)
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Add Ingredients");
        dialog.setHeaderText("Enter ingredients (one per line) in the format: name:quantity[:unit]");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        TextArea area = new TextArea();
        area.setPromptText("Sugar:100\nMilk:1000:ml\nVanilla Syrup:50:ml");
        area.setPrefRowCount(8);

        Label note = new Label("Format: name:quantity[:unit]. Unit is optional (default 'unit').");
        box.getChildren().addAll(note, area);
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> btn == addButtonType);

        dialog.showAndWait().ifPresent(added -> {
            if (Boolean.TRUE.equals(added)) {
                String text = area.getText();
                if (text == null || text.trim().isEmpty()) {
                    showAlert("No Input", "Please enter at least one ingredient.", Alert.AlertType.WARNING);
                    return;
                }
                String[] lines = text.split("\\r?\\n");
                int addedCount = 0;
                for (String line : lines) {
                    String l = line.trim();
                    if (l.isEmpty()) continue;
                    String[] parts = l.split(":" );
                    if (parts.length >= 2) {
                        String name = parts[0].trim();
                        try {
                            double qty = Double.parseDouble(parts[1].trim());
                            String unit = parts.length >= 3 ? parts[2].trim() : "unit";
                            InventoryItem item = new InventoryItem(name, qty, unit);
                            store.addInventoryItem(item);
                            addedCount++;
                        } catch (NumberFormatException ex) {
                            // skip invalid
                        }
                    }
                }
                if (addedCount > 0) {
                    showAlert("Success", addedCount + " ingredient(s) added.", Alert.AlertType.INFORMATION);
                    refreshData();
                } else {
                    showAlert("No Valid Items", "No valid ingredient lines found. Use format name:quantity[:unit].", Alert.AlertType.ERROR);
                }
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
