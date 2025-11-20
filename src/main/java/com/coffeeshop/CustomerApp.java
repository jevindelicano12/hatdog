package com.coffeeshop;

import com.coffeeshop.model.*;
import com.coffeeshop.service.Store;
import com.coffeeshop.service.TextDatabase;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.UUID;

public class CustomerApp extends Application {
    private Store store;
    private Order currentOrder;
    private VBox orderItemsBox;
    private Label totalLabel;

    @Override
    public void start(Stage primaryStage) {
        store = Store.getInstance();
        currentOrder = new Order(UUID.randomUUID().toString().substring(0, 8));

        primaryStage.setTitle("Coffee Shop - Customer");
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Header
        VBox header = createHeader();
        root.setTop(header);

        // Center - Product Menu
        ScrollPane menuScroll = createProductMenu();
        root.setCenter(menuScroll);

        // Right - Current Order
        VBox orderPanel = createOrderPanel();
        root.setRight(orderPanel);

        Scene scene = new Scene(root, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("☕ Coffee Shop - Customer Menu");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#6F4E37"));

        Label subtitle = new Label("Browse our menu and customize your drinks");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#666"));

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private ScrollPane createProductMenu() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(10));

        int col = 0;
        int row = 0;

        for (Product product : store.getProducts()) {
            VBox productCard = createProductCard(product);
            grid.add(productCard, col, row);

            col++;
            if (col > 1) {
                col = 0;
                row++;
            }
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #f5f5f5; -fx-border-color: transparent;");
        return scroll;
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-background-radius: 8;");
        card.setPrefWidth(350);
        card.setMinHeight(200);

        // Product name
        Label nameLabel = new Label(product.getName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#6F4E37"));

        // Price and stock
        HBox infoBox = new HBox(15);
        Label priceLabel = new Label("₱" + String.format("%.2f", product.getPrice()));
        priceLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 16));
        priceLabel.setTextFill(Color.web("#2E7D32"));

        Label stockLabel = new Label("Stock: " + product.getStock());
        stockLabel.setFont(Font.font("Arial", 12));
        if (product.getStock() == 0) {
            stockLabel.setTextFill(Color.RED);
            stockLabel.setText("Not Available Right Now");
        } else if (product.getStock() <= 5) {
            stockLabel.setTextFill(Color.ORANGE);
        } else {
            stockLabel.setTextFill(Color.web("#666"));
        }

        infoBox.getChildren().addAll(priceLabel, stockLabel);

        // Customization controls
        Label tempLabel = new Label("Temperature:");
        ComboBox<String> tempCombo = new ComboBox<>();
        tempCombo.getItems().addAll("Hot", "Cold");
        tempCombo.setValue("Hot");
        tempCombo.setPrefWidth(100);

        Label sugarLabel = new Label("Sugar Level:");
        ComboBox<Integer> sugarCombo = new ComboBox<>();
        sugarCombo.getItems().addAll(0, 25, 50, 75, 100);
        sugarCombo.setValue(50);
        sugarCombo.setPrefWidth(100);

        HBox customBox = new HBox(10);
        customBox.getChildren().addAll(tempLabel, tempCombo, sugarLabel, sugarCombo);

        // Add-ons checkboxes
        Label addOnsLabel = new Label("Add-ons (optional):");
        VBox addOnsBox = new VBox(8);
        addOnsBox.setPadding(new Insets(10));
        addOnsBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        CheckBox extraShotCheck = new CheckBox("Extra Shot (+₱1.00)");
        CheckBox whippedCreamCheck = new CheckBox("Whipped Cream (+₱0.50)");
        CheckBox vanillaSyrupCheck = new CheckBox("Vanilla Syrup (+₱0.75)");
        CheckBox caramelSyrupCheck = new CheckBox("Caramel Syrup (+₱0.75)");
        CheckBox chocolateSyrupCheck = new CheckBox("Chocolate Syrup (+₱0.75)");
        
        addOnsBox.getChildren().addAll(extraShotCheck, whippedCreamCheck, vanillaSyrupCheck, 
                                        caramelSyrupCheck, chocolateSyrupCheck);

        // Quantity
        HBox quantityBox = new HBox(10);
        quantityBox.setAlignment(Pos.CENTER_LEFT);
        Label qtyLabel = new Label("Quantity:");
        Spinner<Integer> qtySpinner = new Spinner<>(1, product.getStock(), 1);
        qtySpinner.setPrefWidth(80);
        quantityBox.getChildren().addAll(qtyLabel, qtySpinner);

        // Add to order button
        Button addButton = new Button("Add to Order");
        addButton.setStyle("-fx-background-color: #6F4E37; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        addButton.setPrefWidth(200);

        if (product.getStock() == 0) {
            addButton.setDisable(true);
            addButton.setText("Out of Stock");
        }

        addButton.setOnAction(e -> {
            OrderItem item = new OrderItem(
                product,
                qtySpinner.getValue(),
                tempCombo.getValue(),
                sugarCombo.getValue()
            );

            // Collect selected add-ons
            StringBuilder addOnsText = new StringBuilder();
            double addOnsCost = 0.0;
            
            if (extraShotCheck.isSelected()) {
                if (addOnsText.length() > 0) addOnsText.append(", ");
                addOnsText.append("Extra Shot");
                addOnsCost += 1.0;
            }
            if (whippedCreamCheck.isSelected()) {
                if (addOnsText.length() > 0) addOnsText.append(", ");
                addOnsText.append("Whipped Cream");
                addOnsCost += 0.5;
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
            
            if (addOnsText.length() > 0) {
                item.setAddOns(addOnsText.toString());
                item.setAddOnsCost(addOnsCost);
            }

            currentOrder.addItem(item);
            updateOrderPanel();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Added to Order");
            alert.setHeaderText(null);
            alert.setContentText(product.getName() + " added to your order!");
            alert.showAndWait();
        });

        card.getChildren().addAll(nameLabel, infoBox, new Separator(), 
                                   customBox, addOnsLabel, addOnsBox,
                                   quantityBox, addButton);

        return card;
    }

    private VBox createOrderPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-background-radius: 8;");
        panel.setPrefWidth(350);
        panel.setMinWidth(350);

        Label orderTitle = new Label("📋 Current Order");
        orderTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        orderTitle.setTextFill(Color.web("#6F4E37"));

        orderItemsBox = new VBox(10);
        ScrollPane orderScroll = new ScrollPane(orderItemsBox);
        orderScroll.setFitToWidth(true);
        orderScroll.setPrefHeight(400);
        orderScroll.setStyle("-fx-background: white; -fx-border-color: #eee;");

        totalLabel = new Label("Total: ₱0.00");
        totalLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        totalLabel.setTextFill(Color.web("#2E7D32"));

        Button checkoutButton = new Button("Proceed to Checkout");
        checkoutButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 20; -fx-font-size: 14;");
        checkoutButton.setPrefWidth(300);
        checkoutButton.setOnAction(e -> proceedToCheckout());

        Button clearButton = new Button("Clear Order");
        clearButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        clearButton.setPrefWidth(300);
        clearButton.setOnAction(e -> {
            currentOrder = new Order(UUID.randomUUID().toString().substring(0, 8));
            updateOrderPanel();
        });

        panel.getChildren().addAll(orderTitle, new Separator(), orderScroll, 
                                    new Separator(), totalLabel, checkoutButton, clearButton);

        return panel;
    }

    private void updateOrderPanel() {
        orderItemsBox.getChildren().clear();

        if (currentOrder.getItems().isEmpty()) {
            Label emptyLabel = new Label("No items in order");
            emptyLabel.setFont(Font.font("Arial", 14));
            emptyLabel.setTextFill(Color.web("#999"));
            orderItemsBox.getChildren().add(emptyLabel);
        } else {
            for (OrderItem item : currentOrder.getItems()) {
                VBox itemBox = new VBox(5);
                itemBox.setPadding(new Insets(10));
                itemBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");

                Label itemName = new Label(item.getProduct().getName() + " x" + item.getQuantity());
                itemName.setFont(Font.font("Arial", FontWeight.BOLD, 14));

                Label itemDetails = new Label(item.getTemperature() + " | " + item.getSugarLevel() + "% sugar");
                itemDetails.setFont(Font.font("Arial", 12));
                itemDetails.setTextFill(Color.web("#666"));

                if (!item.getAddOns().isEmpty()) {
                    Label addOnsLabel = new Label("+ " + item.getAddOns());
                    addOnsLabel.setFont(Font.font("Arial", 12));
                    addOnsLabel.setTextFill(Color.web("#6F4E37"));
                    itemBox.getChildren().add(addOnsLabel);
                }

                Label itemPrice = new Label("₱" + String.format("%.2f", item.getSubtotal()));
                itemPrice.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
                itemPrice.setTextFill(Color.web("#2E7D32"));

                Button removeBtn = new Button("Remove");
                removeBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 11;");
                removeBtn.setOnAction(e -> {
                    currentOrder.removeItem(item);
                    updateOrderPanel();
                });

                HBox bottomBox = new HBox(10);
                bottomBox.setAlignment(Pos.CENTER_LEFT);
                bottomBox.getChildren().addAll(itemPrice, removeBtn);

                itemBox.getChildren().addAll(itemName, itemDetails, bottomBox);
                orderItemsBox.getChildren().add(itemBox);
            }
        }

        totalLabel.setText("Total: ₱" + String.format("%.2f", currentOrder.getTotalAmount()));
    }

    private void proceedToCheckout() {
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
            alert.setContentText("Sorry, one or more items in your order are out of stock.");
            alert.showAndWait();
            return;
        }

        // Validate ingredients
        if (!store.isInventorySufficient(currentOrder)) {
            String ingredient = store.getInsufficientIngredient(currentOrder);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Insufficient Ingredients");
            alert.setHeaderText("Cannot Process Order");
            alert.setContentText("Sorry, we don't have enough " + ingredient + " to complete your order.");
            alert.showAndWait();
            return;
        }

        // Ask for customer name
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Customer Name");
        nameDialog.setHeaderText("Enter Your Name");
        nameDialog.setContentText("Name:");
        String customerName = nameDialog.showAndWait().orElse("Guest");

        // Save order to pending orders file
        PendingOrder pendingOrder = new PendingOrder(currentOrder.getOrderId(), customerName);
        for (OrderItem item : currentOrder.getItems()) {
            pendingOrder.addItem(
                item.getProduct().getName(),
                item.getProduct().getPrice(),
                item.getQuantity(),
                item.getTemperature(),
                item.getSugarLevel()
            );
        }
        TextDatabase.savePendingOrder(pendingOrder);

        // Show order summary and payment info
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Order Sent to Cashier");
        alert.setHeaderText("Order ID: " + currentOrder.getOrderId());
        alert.setContentText("Customer: " + customerName +
                           "\nTotal Amount: ₱" + String.format("%.2f", currentOrder.getTotalAmount()) + 
                           "\n\nYour order has been sent to the cashier.\nPlease proceed to payment counter.");
        alert.showAndWait();

        // Create new order for next customer
        currentOrder = new Order(UUID.randomUUID().toString().substring(0, 8));
        updateOrderPanel();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
