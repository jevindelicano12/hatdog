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
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;

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
        root.setStyle("-fx-background-color: #f5f5f5;");

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

        tabPane.getTabs().addAll(dashboardTab, productsTab, inventoryTab, refillTab, categoriesTab);
        return tabPane;
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

        // List view of categories
        javafx.scene.control.ListView<String> listView = new javafx.scene.control.ListView<>();
        listView.setPrefHeight(300);
        listView.getItems().addAll(store.getCategories());

        HBox actions = new HBox(8);
        Button renameBtn = new Button("Rename");
        Button deleteBtn = new Button("Delete");

        renameBtn.setOnAction(e -> {
            String sel = listView.getSelectionModel().getSelectedItem();
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
            String sel = listView.getSelectionModel().getSelectedItem();
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

        actions.getChildren().addAll(renameBtn, deleteBtn);

        panel.getChildren().addAll(title, new Separator(), addRow, listView, actions);
        return panel;
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

        TableColumn<ProductRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(100);

        TableColumn<ProductRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<ProductRow, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);

        TableColumn<ProductRow, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        stockCol.setPrefWidth(100);

        TableColumn<ProductRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        productTable.getColumns().addAll(idCol, nameCol, priceCol, stockCol, statusCol);

        // Controls
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button refillButton = new Button("Refill Product");
        refillButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        refillButton.setOnAction(e -> refillProduct());

        Button editButton = new Button("Edit Product");
        editButton.setStyle("-fx-background-color: #0277BD; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        editButton.setOnAction(e -> editProduct());

        Button removeButton = new Button("Remove Product");
        removeButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        removeButton.setOnAction(e -> removeProduct());

        Button addButton = new Button("Add New Product");
        addButton.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        addButton.setOnAction(e -> addNewProduct());

        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #FFA726; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        refreshButton.setOnAction(e -> refreshData());

        controls.getChildren().addAll(refillButton, editButton, removeButton, addButton, refreshButton);

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
            productTable.getItems().add(new ProductRow(p.getId(), p.getName(), p.getPrice(), p.getStock(), status));
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
            alertsArea.setStyle("-fx-control-inner-background: #fff3cd;");
        } else {
            alertsArea.setText("\n\n✓ All products are sufficiently stocked!\n\nNo refill alerts at this time.");
            alertsArea.setStyle("-fx-control-inner-background: #d4edda;");
        }
    }

    private void refillProduct() {
        ProductRow selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a product to refill.", Alert.AlertType.WARNING);
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refill Product");
        dialog.setHeaderText("Refill: " + selected.getName());
        dialog.setContentText("Enter amount to add (max " + (Store.MAX_STOCK - selected.getStock()) + "):");
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
        dialog.setWidth(700);
        dialog.setHeight(900);

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

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
            categoryBox.getItems().addAll("Coffee", "Pastries", "Beverages", "Snacks", "Other");
            categoryBox.setValue("Coffee");
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

        // Ingredients section
        Label ingredientLabel = new Label("🧪 Select Ingredients:");
        ingredientLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        grid.add(ingredientLabel, 0, 5, 2, 1);

        // Get available ingredients from inventory
        java.util.List<String> availableIngredients = new java.util.ArrayList<>(store.getInventory().keySet());

        // Create ingredient selector UI
        VBox ingredientSection = new VBox(10);
        ingredientSection.setPadding(new Insets(10));
        ingredientSection.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #fafafa;");
        
        java.util.Map<String, Double> selectedIngredients = new java.util.HashMap<>();
        
        ScrollPane ingredientScroll = new ScrollPane();
        ingredientScroll.setPrefHeight(250);
        VBox ingredientList = new VBox(8);
        
        for (String ingredientName : availableIngredients) {
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
        
        ingredientScroll.setContent(ingredientList);
        ingredientScroll.setFitToWidth(true);
        ingredientSection.getChildren().add(ingredientScroll);
        grid.add(ingredientSection, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);

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

                    return new Product(id, name, price, 20, new HashMap<>(selectedIngredients), category);
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
                    refreshData();
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
        TextField stockField = new TextField("0");
        stockField.setPromptText("Amount to add");

        grid.add(idLabel, 0, 0);
        grid.add(idValueLabel, 1, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(nameValueLabel, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Add Stock:"), 0, 3);
        grid.add(stockField, 1, 3);

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

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try {
                    double newPrice = Double.parseDouble(priceField.getText());
                    int addStock = Integer.parseInt(stockField.getText());

                    // Update price
                    product.setPrice(newPrice);

                    // Update stock
                    if (addStock > 0) {
                        product.setStock(product.getStock() + addStock);
                    }

                    store.saveData();

                    // Update image if selected
                    if (selectedImageFile[0] != null && selectedImageFile[0].exists()) {
                        File imagesDir = new File("data/images");
                        if (!imagesDir.exists()) {
                            imagesDir.mkdirs();
                        }
                        String fileName = selectedImageFile[0].getName();
                        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
                        File destFile = new File(imagesDir, product.getId() + fileExtension);
                        Files.copy(selectedImageFile[0].toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

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
        private int stock;
        private String status;

        public ProductRow(String id, String name, double price, int stock, String status) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.stock = stock;
            this.status = status;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public int getStock() { return stock; }
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
