package com.coffeeshop.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * Centralized alert/dialog utilities for consistent user notifications.
 */
public final class AlertUtils {
    
    private AlertUtils() {
        // Prevent instantiation
    }
    
    // ==================== INFORMATION ALERTS ====================
    
    /**
     * Show a simple information alert
     */
    public static void showInfo(String message) {
        showInfo("Information", message);
    }
    
    /**
     * Show an information alert with custom title
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }
    
    /**
     * Show a success notification
     */
    public static void showSuccess(String message) {
        showInfo("✓ Success", message);
    }
    
    /**
     * Show a success notification with custom title
     */
    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("✓ " + title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }
    
    // ==================== WARNING ALERTS ====================
    
    /**
     * Show a warning alert
     */
    public static void showWarning(String message) {
        showWarning("Warning", message);
    }
    
    /**
     * Show a warning alert with custom title
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("⚠ " + title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }
    
    // ==================== ERROR ALERTS ====================
    
    /**
     * Show an error alert
     */
    public static void showError(String message) {
        showError("Error", message);
    }
    
    /**
     * Show an error alert with custom title
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("✗ " + title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }
    
    /**
     * Show an error alert with exception details
     */
    public static void showError(String title, String message, Throwable exception) {
        String fullMessage = message;
        if (exception != null && exception.getMessage() != null) {
            fullMessage += "\n\nDetails: " + exception.getMessage();
        }
        showError(title, fullMessage);
    }
    
    // ==================== CONFIRMATION DIALOGS ====================
    
    /**
     * Show a confirmation dialog
     * @return true if user clicked OK
     */
    public static boolean confirm(String message) {
        return confirm("Confirm", message);
    }
    
    /**
     * Show a confirmation dialog with custom title
     * @return true if user clicked OK
     */
    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Show a confirmation dialog with custom buttons
     * @return the selected ButtonType, or null if cancelled
     */
    public static ButtonType confirm(String title, String message, ButtonType... buttons) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(buttons);
        styleAlert(alert);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.orElse(null);
    }
    
    /**
     * Show a Yes/No confirmation dialog
     * @return true if user clicked Yes
     */
    public static boolean confirmYesNo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        styleAlert(alert);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
    
    /**
     * Show a dangerous action confirmation (e.g., delete)
     */
    public static boolean confirmDanger(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("⚠ " + title);
        alert.setHeaderText("This action cannot be undone!");
        alert.setContentText(message);
        alert.getButtonTypes().setAll(
            new ButtonType("Yes, proceed"),
            ButtonType.CANCEL
        );
        styleAlert(alert);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && !result.get().equals(ButtonType.CANCEL);
    }
    
    // ==================== INPUT DIALOGS ====================
    
    /**
     * Show a text input dialog
     * @return the entered text, or null if cancelled
     */
    public static String promptText(String title, String message) {
        return promptText(title, message, "");
    }
    
    /**
     * Show a text input dialog with default value
     * @return the entered text, or null if cancelled
     */
    public static String promptText(String title, String message, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
    
    /**
     * Show a numeric input dialog
     * @return the entered number, or -1 if cancelled/invalid
     */
    public static double promptNumber(String title, String message) {
        return promptNumber(title, message, 0);
    }
    
    /**
     * Show a numeric input dialog with default value
     * @return the entered number, or -1 if cancelled/invalid
     */
    public static double promptNumber(String title, String message, double defaultValue) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(defaultValue));
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        
        // Add numeric validation
        dialog.getEditor().setTextFormatter(ValidationUtils.createDecimalFormatter());
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                return Double.parseDouble(result.get());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
    
    /**
     * Show a choice dialog
     * @return the selected choice, or null if cancelled
     */
    public static <T> T promptChoice(String title, String message, List<T> choices) {
        if (choices == null || choices.isEmpty()) return null;
        
        ChoiceDialog<T> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        
        Optional<T> result = dialog.showAndWait();
        return result.orElse(null);
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Apply consistent styling to alerts
     */
    private static void styleAlert(Alert alert) {
        // Get the dialog pane and apply styling
        try {
            alert.getDialogPane().setStyle(
                "-fx-font-family: 'Segoe UI'; " +
                "-fx-background-color: white;"
            );
        } catch (Exception ignored) {
            // Styling is optional
        }
    }
    
    /**
     * Show a notification that auto-closes after a few seconds
     * Note: This requires additional JavaFX setup for toast-style notifications
     */
    public static void toast(String message) {
        // For now, just show a regular info alert
        // TODO: Implement proper toast notification
        showInfo(message);
    }
}
