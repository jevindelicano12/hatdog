package com.coffeeshop.model;

import java.util.HashMap;
import java.util.Map;

public class Product {
    private String id;
    private String name;
    private String description = ""; // Product description
    private double price;
    private Map<String, Double> recipe; // ingredient name -> quantity needed per serving
    private String category;
    private Map<String, Double> sizeSurcharges; // e.g. Small->0.0, Medium->20.0, Large->30.0
    private boolean hasSizes = true;
    private boolean hasSmall = true;
    private boolean hasMedium = true;
    private boolean hasLarge = true;
    // Milk options (for Milk Tea and similar products)
    private boolean hasMilkOptions = false;
    private boolean hasOatMilk = false;
    private boolean hasAlmondMilk = false;
    private boolean hasSoyMilk = false;
    private double oatMilkPrice = 25.0;
    private double almondMilkPrice = 25.0;
    private double soyMilkPrice = 25.0;

    // Sugar level options (0%, 25%, 50%, 75%, 100%)
    private java.util.List<String> sugarLevels;

    public Product(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.recipe = new HashMap<>();
        this.category = "Uncategorized";
        this.sizeSurcharges = new HashMap<>();
        // sensible defaults
        this.sizeSurcharges.put("Small", 0.0);
        this.sizeSurcharges.put("Medium", 20.0);
        this.sizeSurcharges.put("Large", 30.0);
        // Sugar levels will be initialized lazily in getSugarLevels()
    }

    public Product(String id, String name, double price, Map<String, Double> recipe) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.recipe = recipe;
        this.category = "Uncategorized";
        // Default: all sugar levels available
        // Sugar levels will be initialized lazily in getSugarLevels()
    }

    public Product(String id, String name, double price, Map<String, Double> recipe, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.recipe = recipe != null ? recipe : new HashMap<>();
        this.category = category != null ? category : "Uncategorized";
        this.sizeSurcharges = new HashMap<>();
        this.sizeSurcharges.put("Small", 0.0);
        this.sizeSurcharges.put("Medium", 20.0);
        this.sizeSurcharges.put("Large", 30.0);
        // Sugar levels will be initialized lazily in getSugarLevels()
    }

    // Getters and setters
        // Sugar level getters and setters
        public java.util.List<String> getSugarLevels() {
            if (sugarLevels == null) {
                // Lazy initialization of default sugar levels
                sugarLevels = new java.util.ArrayList<>();
                sugarLevels.add("0% sugar");
                sugarLevels.add("25% sugar");
                sugarLevels.add("50% sugar");
                sugarLevels.add("75% sugar");
                sugarLevels.add("100% sugar");
            }
            return sugarLevels;
        }

        public void setSugarLevels(java.util.List<String> sugarLevels) {
            this.sugarLevels = sugarLevels != null ? new java.util.ArrayList<>(sugarLevels) : null;
        }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Map<String, Double> getRecipe() {
        return recipe;
    }

    public void setRecipe(Map<String, Double> recipe) {
        this.recipe = recipe;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<String, Double> getSizeSurcharges() {
        if (this.sizeSurcharges == null) {
            this.sizeSurcharges = new HashMap<>();
            // Don't add default surcharges here - let PersistenceManager handle it
            // based on product category (pastries don't have sizes)
        }
        return sizeSurcharges;
    }

    public boolean isHasSizes() {
        return hasSizes;
    }

    public void setHasSizes(boolean hasSizes) {
        this.hasSizes = hasSizes;
    }

    public boolean isHasSmall() {
        return hasSmall;
    }

    public void setHasSmall(boolean hasSmall) {
        this.hasSmall = hasSmall;
    }

    public boolean isHasMedium() {
        return hasMedium;
    }

    public void setHasMedium(boolean hasMedium) {
        this.hasMedium = hasMedium;
    }

    public boolean isHasLarge() {
        return hasLarge;
    }

    public void setHasLarge(boolean hasLarge) {
        this.hasLarge = hasLarge;
    }

    // Milk Options getters and setters
    public boolean isHasMilkOptions() {
        return hasMilkOptions;
    }

    public void setHasMilkOptions(boolean hasMilkOptions) {
        this.hasMilkOptions = hasMilkOptions;
    }

    public boolean isHasOatMilk() {
        return hasOatMilk;
    }

    public void setHasOatMilk(boolean hasOatMilk) {
        this.hasOatMilk = hasOatMilk;
    }

    public boolean isHasAlmondMilk() {
        return hasAlmondMilk;
    }

    public void setHasAlmondMilk(boolean hasAlmondMilk) {
        this.hasAlmondMilk = hasAlmondMilk;
    }

    public boolean isHasSoyMilk() {
        return hasSoyMilk;
    }

    public void setHasSoyMilk(boolean hasSoyMilk) {
        this.hasSoyMilk = hasSoyMilk;
    }

    public double getOatMilkPrice() {
        return oatMilkPrice;
    }

    public void setOatMilkPrice(double oatMilkPrice) {
        this.oatMilkPrice = oatMilkPrice;
    }

    public double getAlmondMilkPrice() {
        return almondMilkPrice;
    }

    public void setAlmondMilkPrice(double almondMilkPrice) {
        this.almondMilkPrice = almondMilkPrice;
    }

    public double getSoyMilkPrice() {
        return soyMilkPrice;
    }

    public void setSoyMilkPrice(double soyMilkPrice) {
        this.soyMilkPrice = soyMilkPrice;
    }

    public void setSizeSurcharges(Map<String, Double> sizeSurcharges) {
        this.sizeSurcharges = sizeSurcharges != null ? sizeSurcharges : new HashMap<>();
    }

    public void addIngredient(String ingredientName, double quantity) {
        this.recipe.put(ingredientName, quantity);
    }

    @Override
    public String toString() {
        return name + " - ₱" + String.format("%.2f", price);
    }
}
