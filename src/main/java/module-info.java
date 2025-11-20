module com.coffeeshop {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    exports com.coffeeshop;
    exports com.coffeeshop.model;
    exports com.coffeeshop.service;
    
    opens com.coffeeshop to javafx.fxml;
    opens com.coffeeshop.model to com.google.gson, javafx.base;
    opens com.coffeeshop.service to com.google.gson;
}
