package com.coffeeshop.util;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logging utility for the Coffee Shop POS system.
 * Replaces scattered System.out.println() calls with structured logging.
 */
public final class Logger {
    
    public enum Level {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR");
        
        private final int priority;
        private final String label;
        
        Level(int priority, String label) {
            this.priority = priority;
            this.label = label;
        }
        
        public int getPriority() { return priority; }
        public String getLabel() { return label; }
    }
    
    private static Level currentLevel = Level.INFO;
    private static boolean consoleEnabled = true;
    private static boolean fileEnabled = false;
    private static String logFilePath = "data/app.log";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final String className;
    
    private Logger(String className) {
        this.className = className;
    }
    
    /**
     * Get a logger for the specified class
     */
    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz.getSimpleName());
    }
    
    /**
     * Get a logger with a custom name
     */
    public static Logger getLogger(String name) {
        return new Logger(name);
    }
    
    // ==================== CONFIGURATION ====================
    
    /**
     * Set the minimum log level (messages below this level are ignored)
     */
    public static void setLevel(Level level) {
        currentLevel = level;
    }
    
    /**
     * Enable or disable console output
     */
    public static void setConsoleEnabled(boolean enabled) {
        consoleEnabled = enabled;
    }
    
    /**
     * Enable or disable file logging
     */
    public static void setFileEnabled(boolean enabled) {
        fileEnabled = enabled;
    }
    
    /**
     * Set the log file path
     */
    public static void setLogFilePath(String path) {
        logFilePath = path;
    }
    
    /**
     * Enable debug mode (shows all messages)
     */
    public static void enableDebugMode() {
        currentLevel = Level.DEBUG;
    }
    
    /**
     * Disable debug mode (shows only INFO and above)
     */
    public static void disableDebugMode() {
        currentLevel = Level.INFO;
    }
    
    // ==================== LOGGING METHODS ====================
    
    public void debug(String message) {
        log(Level.DEBUG, message, null);
    }
    
    public void debug(String message, Object... args) {
        log(Level.DEBUG, String.format(message, args), null);
    }
    
    public void info(String message) {
        log(Level.INFO, message, null);
    }
    
    public void info(String message, Object... args) {
        log(Level.INFO, String.format(message, args), null);
    }
    
    public void warn(String message) {
        log(Level.WARN, message, null);
    }
    
    public void warn(String message, Object... args) {
        log(Level.WARN, String.format(message, args), null);
    }
    
    public void warn(String message, Throwable throwable) {
        log(Level.WARN, message, throwable);
    }
    
    public void error(String message) {
        log(Level.ERROR, message, null);
    }
    
    public void error(String message, Object... args) {
        log(Level.ERROR, String.format(message, args), null);
    }
    
    public void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }
    
    // ==================== CORE LOGGING ====================
    
    private void log(Level level, String message, Throwable throwable) {
        if (level.getPriority() < currentLevel.getPriority()) {
            return;
        }
        
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String formattedMessage = String.format("[%s] [%s] [%s] %s", 
            timestamp, level.getLabel(), className, message);
        
        if (consoleEnabled) {
            if (level == Level.ERROR) {
                System.err.println(formattedMessage);
            } else {
                System.out.println(formattedMessage);
            }
            
            if (throwable != null) {
                throwable.printStackTrace(level == Level.ERROR ? System.err : System.out);
            }
        }
        
        if (fileEnabled) {
            writeToFile(formattedMessage, throwable);
        }
    }
    
    private synchronized void writeToFile(String message, Throwable throwable) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println(message);
            if (throwable != null) {
                throwable.printStackTrace(writer);
            }
        } catch (Exception e) {
            // Silently fail - don't want logging to break the app
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Log method entry (for debugging)
     */
    public void entering(String methodName) {
        debug(">>> Entering: %s", methodName);
    }
    
    /**
     * Log method exit (for debugging)
     */
    public void exiting(String methodName) {
        debug("<<< Exiting: %s", methodName);
    }
    
    /**
     * Log method entry with parameters (for debugging)
     */
    public void entering(String methodName, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append(">>> Entering: ").append(methodName).append("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(params[i]);
        }
        sb.append(")");
        debug(sb.toString());
    }
    
    /**
     * Log a performance measurement
     */
    public void performance(String operation, long durationMs) {
        info("Performance: %s took %dms", operation, durationMs);
    }
}
