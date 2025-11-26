package com.coffeeshop.util;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

/**
 * Centralized styling utilities for consistent UI across the application.
 * Eliminates duplicate inline styles and provides reusable components.
 */
public final class StyleUtils {
    
    private StyleUtils() {
        // Prevent instantiation
    }
    
    // ==================== BUTTON STYLES ====================
    
    /** Primary button style (dark background, white text) */
    public static final String BUTTON_PRIMARY = 
        "-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
    
    public static final String BUTTON_PRIMARY_HOVER = 
        "-fx-background-color: #333333; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
    
    /** Secondary button style (light background, dark text) */
    public static final String BUTTON_SECONDARY = 
        "-fx-background-color: #F5F5F5; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24; " +
        "-fx-border-color: #E0E0E0; -fx-border-radius: 8;";
    
    public static final String BUTTON_SECONDARY_HOVER = 
        "-fx-background-color: #EEEEEE; -fx-text-fill: #1A1A1A; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24; " +
        "-fx-border-color: #BDBDBD; -fx-border-radius: 8;";
    
    /** Success button style (green) */
    public static final String BUTTON_SUCCESS = 
        "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
    
    public static final String BUTTON_SUCCESS_HOVER = 
        "-fx-background-color: #43A047; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
    
    /** Danger button style (red) */
    public static final String BUTTON_DANGER = 
        "-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
    
    public static final String BUTTON_DANGER_HOVER = 
        "-fx-background-color: #E53935; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
    
    /** Warning button style (orange) */
    public static final String BUTTON_WARNING = 
        "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
    
    public static final String BUTTON_WARNING_HOVER = 
        "-fx-background-color: #FB8C00; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
    
    /** Info button style (blue) */
    public static final String BUTTON_INFO = 
        "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
    
    public static final String BUTTON_INFO_HOVER = 
        "-fx-background-color: #1E88E5; -fx-text-fill: white; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 12 24;";
    
    /** Transparent/ghost button */
    public static final String BUTTON_GHOST = 
        "-fx-background-color: transparent; -fx-text-fill: #666666; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 8 16; -fx-border-color: #E0E0E0; -fx-border-radius: 8;";
    
    public static final String BUTTON_GHOST_HOVER = 
        "-fx-background-color: #F5F5F5; -fx-text-fill: #333333; -fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 8 16; -fx-border-color: #BDBDBD; -fx-border-radius: 8;";
    
    // ==================== PILL/TAG STYLES ====================
    
    public static final String PILL_DEFAULT = 
        "-fx-text-fill: #666666; -fx-border-color: #E0E0E0; -fx-border-width: 1; " +
        "-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; " +
        "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;";
    
    public static final String PILL_SELECTED = 
        "-fx-text-fill: #FFFFFF; -fx-border-color: #2C2C2C; -fx-border-width: 1; " +
        "-fx-background-color: #2C2C2C; -fx-background-radius: 20; -fx-border-radius: 20; " +
        "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;";
    
    public static final String PILL_HOVER = 
        "-fx-text-fill: #333333; -fx-border-color: #CCCCCC; -fx-border-width: 1; " +
        "-fx-background-color: #F5F5F5; -fx-background-radius: 20; -fx-border-radius: 20; " +
        "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;";
    
    // ==================== CARD STYLES ====================
    
    public static final String CARD = 
        "-fx-background-color: white; -fx-background-radius: 12; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);";
    
    public static final String CARD_HOVER = 
        "-fx-background-color: white; -fx-background-radius: 12; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);";
    
    public static final String CARD_BORDERED = 
        "-fx-background-color: white; -fx-background-radius: 12; " +
        "-fx-border-color: #E0E0E0; -fx-border-radius: 12; -fx-border-width: 1;";
    
    // ==================== INPUT STYLES ====================
    
    public static final String INPUT_DEFAULT = 
        "-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 8; " +
        "-fx-background-radius: 8; -fx-padding: 12; -fx-font-size: 14px;";
    
    public static final String INPUT_FOCUS = 
        "-fx-background-color: white; -fx-border-color: #1A1A1A; -fx-border-radius: 8; " +
        "-fx-background-radius: 8; -fx-padding: 12; -fx-font-size: 14px; -fx-border-width: 2;";
    
    public static final String INPUT_DARK = 
        "-fx-background-color: #343a40; -fx-text-fill: white; -fx-prompt-text-fill: #6c757d; " +
        "-fx-background-radius: 8; -fx-padding: 12 15; -fx-font-size: 14px; -fx-border-color: transparent;";
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Apply primary button styling with hover effects
     */
    public static void applyPrimaryStyle(Button button) {
        button.setStyle(BUTTON_PRIMARY);
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_PRIMARY_HOVER));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_PRIMARY));
    }
    
    /**
     * Apply secondary button styling with hover effects
     */
    public static void applySecondaryStyle(Button button) {
        button.setStyle(BUTTON_SECONDARY);
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_SECONDARY_HOVER));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_SECONDARY));
    }
    
    /**
     * Apply success button styling with hover effects
     */
    public static void applySuccessStyle(Button button) {
        button.setStyle(BUTTON_SUCCESS);
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_SUCCESS_HOVER));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_SUCCESS));
    }
    
    /**
     * Apply danger button styling with hover effects
     */
    public static void applyDangerStyle(Button button) {
        button.setStyle(BUTTON_DANGER);
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_DANGER_HOVER));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_DANGER));
    }
    
    /**
     * Apply warning button styling with hover effects
     */
    public static void applyWarningStyle(Button button) {
        button.setStyle(BUTTON_WARNING);
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_WARNING_HOVER));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_WARNING));
    }
    
    /**
     * Apply info button styling with hover effects
     */
    public static void applyInfoStyle(Button button) {
        button.setStyle(BUTTON_INFO);
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_INFO_HOVER));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_INFO));
    }
    
    /**
     * Apply ghost/transparent button styling with hover effects
     */
    public static void applyGhostStyle(Button button) {
        button.setStyle(BUTTON_GHOST);
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_GHOST_HOVER));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_GHOST));
    }
    
    /**
     * Apply card styling with hover effect
     */
    public static void applyCardStyle(Region region) {
        region.setStyle(CARD);
        region.setOnMouseEntered(e -> region.setStyle(CARD_HOVER));
        region.setOnMouseExited(e -> region.setStyle(CARD));
    }
    
    /**
     * Apply pill/tag toggle styling
     */
    public static void applyPillToggleStyle(Button button, boolean selected) {
        button.setStyle(selected ? PILL_SELECTED : PILL_DEFAULT);
        if (!selected) {
            button.setOnMouseEntered(e -> {
                if (!button.getStyle().equals(PILL_SELECTED)) {
                    button.setStyle(PILL_HOVER);
                }
            });
            button.setOnMouseExited(e -> {
                if (!button.getStyle().equals(PILL_SELECTED)) {
                    button.setStyle(PILL_DEFAULT);
                }
            });
        }
    }
    
    // ==================== LABEL FACTORY ====================
    
    /**
     * Create a title label
     */
    public static Label createTitleLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(Constants.FONT_PRIMARY, FontWeight.BOLD, 28));
        label.setTextFill(Color.web(Constants.COLOR_TEXT_PRIMARY));
        return label;
    }
    
    /**
     * Create a subtitle label
     */
    public static Label createSubtitleLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(Constants.FONT_PRIMARY, FontWeight.SEMI_BOLD, 18));
        label.setTextFill(Color.web(Constants.COLOR_TEXT_PRIMARY));
        return label;
    }
    
    /**
     * Create a section header label
     */
    public static Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(Constants.FONT_PRIMARY, FontWeight.BOLD, 14));
        label.setTextFill(Color.web(Constants.COLOR_TEXT_SECONDARY));
        return label;
    }
    
    /**
     * Create a body text label
     */
    public static Label createBodyLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(Constants.FONT_PRIMARY, FontWeight.NORMAL, 14));
        label.setTextFill(Color.web(Constants.COLOR_TEXT_PRIMARY));
        return label;
    }
    
    /**
     * Create a caption/small text label
     */
    public static Label createCaptionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(Constants.FONT_PRIMARY, FontWeight.NORMAL, 12));
        label.setTextFill(Color.web(Constants.COLOR_TEXT_SECONDARY));
        return label;
    }
    
    /**
     * Create a price label
     */
    public static Label createPriceLabel(double amount) {
        Label label = new Label(Constants.formatCurrency(amount));
        label.setFont(Font.font(Constants.FONT_PRIMARY, FontWeight.BOLD, 16));
        label.setTextFill(Color.web(Constants.COLOR_TEXT_PRIMARY));
        return label;
    }
    
    // ==================== BUTTON FACTORY ====================
    
    /**
     * Create a primary styled button
     */
    public static Button createPrimaryButton(String text) {
        Button button = new Button(text);
        applyPrimaryStyle(button);
        return button;
    }
    
    /**
     * Create a secondary styled button
     */
    public static Button createSecondaryButton(String text) {
        Button button = new Button(text);
        applySecondaryStyle(button);
        return button;
    }
    
    /**
     * Create a success styled button
     */
    public static Button createSuccessButton(String text) {
        Button button = new Button(text);
        applySuccessStyle(button);
        return button;
    }
    
    /**
     * Create a danger styled button
     */
    public static Button createDangerButton(String text) {
        Button button = new Button(text);
        applyDangerStyle(button);
        return button;
    }
    
    /**
     * Create a warning styled button
     */
    public static Button createWarningButton(String text) {
        Button button = new Button(text);
        applyWarningStyle(button);
        return button;
    }
    
    // ==================== LAYOUT FACTORY ====================
    
    /**
     * Create a standard card VBox
     */
    public static VBox createCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle(CARD);
        return card;
    }
    
    /**
     * Create a horizontal button bar
     */
    public static HBox createButtonBar(Button... buttons) {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getChildren().addAll(buttons);
        return bar;
    }
}
