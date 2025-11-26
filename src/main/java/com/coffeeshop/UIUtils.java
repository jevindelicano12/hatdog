package com.coffeeshop;

import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

/**
 * UI Utility methods for consistent button styling and effects.
 */
public final class UIUtils {
    private UIUtils() {}
    
    /**
     * Applies a hover effect to a button (subtle glow and scale).
     */
    public static void applyHoverEffect(Button btn) {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(100, 150, 255, 0.5));
        glow.setRadius(10);
        
        btn.setOnMouseEntered(e -> {
            btn.setEffect(glow);
            btn.setScaleX(1.02);
            btn.setScaleY(1.02);
        });
        
        btn.setOnMouseExited(e -> {
            btn.setEffect(null);
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
    }
    
    /**
     * Creates a styled primary action button.
     */
    public static Button createPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        applyHoverEffect(btn);
        return btn;
    }
    
    /**
     * Creates a styled danger/cancel button.
     */
    public static Button createDangerButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        applyHoverEffect(btn);
        return btn;
    }
    
    /**
     * Creates a styled secondary button.
     */
    public static Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        applyHoverEffect(btn);
        return btn;
    }
}
