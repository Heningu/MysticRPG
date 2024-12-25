package eu.xaru.mysticrpg.utils;

import eu.xaru.mysticrpg.cores.MysticCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Level;

/**
 * DebugLogger is a singleton class that handles comprehensive logging functionalities for the plugin.
 * It provides various overloaded methods to log messages, warnings, errors, debug information,
 * and object states with enhanced formatting.
 */
public class DebugLogger {

    // Singleton instance
    private static DebugLogger instance;

    // Flag to enable or disable debugging globally
    private volatile boolean debuggingEnabled;

    // DateTimeFormatter for timestamps
    private final DateTimeFormatter dtFormatter;

    // Private constructor to prevent external instantiation
    private DebugLogger() {
        this.debuggingEnabled = true; // Default to enabled; can be toggled via methods
        this.dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Retrieves the singleton instance of DebugLogger.
     *
     * @return The DebugLogger instance.
     */
    public static synchronized DebugLogger getInstance() {
        if (instance == null) {
            instance = new DebugLogger();
        }
        return instance;
    }

    /**
     * Enables debugging.
     */
    public void enableDebugging() {
        this.debuggingEnabled = true;
        debug("Debugging enabled.");
    }

    /**
     * Disables debugging.
     */
    public void disableDebugging() {
        this.debuggingEnabled = false;
        debug("Debugging disabled.");
    }

    /**
     * Checks if debugging is enabled.
     *
     * @return True if debugging is enabled, false otherwise.
     */
    public boolean isDebuggingEnabled() {
        return this.debuggingEnabled;
    }

    /* ===========================
       ======== LOG METHODS ========
       =========================== */

    /**
     * Logs a general informational message.
     *
     * @param message The message to log.
     */
    public void log(String message) {
        send(Level.INFO, message, 2, null);
    }

    /**
     * Logs a formatted informational message.
     *
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void log(String format, Object... args) {
        send(Level.INFO, formatMessage(format, args), 2, null);
    }

    /**
     * Logs an exception as an informational message.
     *
     * @param throwable The throwable to log.
     */
    public void log(Throwable throwable) {
        send(Level.INFO, getStackTraceAsString(throwable), 2, throwable);
    }

    /**
     * Logs an informational message with a specific log level.
     *
     * @param level   The logging level.
     * @param message The message to log.
     */
    public void log(Level level, String message) {
        send(level, message, 2, null);
    }

    /**
     * Logs a formatted message with a specific log level.
     *
     * @param level  The logging level.
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void log(Level level, String format, Object... args) {
        send(level, formatMessage(format, args), 2, null);
    }

    /**
     * Logs an exception with a specific log level.
     *
     * @param level     The logging level.
     * @param throwable The throwable to log.
     */
    public void log(Level level, Throwable throwable) {
        send(level, getStackTraceAsString(throwable), 2, throwable);
    }

    /**
     * Logs a message and an exception with a specific log level.
     *
     * @param level     The logging level.
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    public void log(Level level, String message, Throwable throwable) {
        send(level, message, 2, throwable);
        send(level, getStackTraceAsString(throwable), 3, throwable);
    }

    /**
     * Logs a message and an exception.
     *
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    public void log(String message, Throwable throwable) {
        send(Level.INFO, message, 2, throwable);
        send(Level.INFO, getStackTraceAsString(throwable), 3, throwable);
    }

    /**
     * Logs an object's state by reflecting its fields.
     *
     * @param object The object to log.
     */
    public void logObject(Object object) {
        if (object == null) {
            log("Attempted to log a null object.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(object.getClass().getName()).append(" {");

        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(object);
                sb.append("\n  ").append(field.getName()).append(": ").append(formatObject(value));
            } catch (IllegalAccessException e) {
                sb.append("\n  ").append(field.getName()).append(": [access denied]");
            }
        }
        sb.append("\n}");

        log(sb.toString());
    }

    /* ===========================
       ====== WARNING METHODS =====
       =========================== */

    /**
     * Logs a warning message.
     *
     * @param message The warning message.
     */
    public void warning(String message) {
        send(Level.WARNING, message, 2, null);
    }

    /**
     * Logs a formatted warning message.
     *
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void warning(String format, Object... args) {
        send(Level.WARNING, formatMessage(format, args), 2, null);
    }

    /**
     * Logs an exception as a warning message.
     *
     * @param throwable The throwable to log.
     */
    public void warning(Throwable throwable) {
        send(Level.WARNING, getStackTraceAsString(throwable), 2, throwable);
    }

    /**
     * Logs a warning message with a specific log level.
     *
     * @param level   The logging level.
     * @param message The message to log.
     */
    public void warning(Level level, String message) {
        send(level, message, 2, null);
    }

    /**
     * Logs a formatted warning message with a specific log level.
     *
     * @param level  The logging level.
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void warning(Level level, String format, Object... args) {
        send(level, formatMessage(format, args), 2, null);
    }

    /**
     * Logs an exception with a specific log level as a warning.
     *
     * @param level     The logging level.
     * @param throwable The throwable to log.
     */
    public void warning(Level level, Throwable throwable) {
        send(level, getStackTraceAsString(throwable), 2, throwable);
    }

    /**
     * Logs a warning message and an exception with a specific log level.
     *
     * @param level     The logging level.
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    public void warning(Level level, String message, Throwable throwable) {
        send(level, message, 2, throwable);
        send(level, getStackTraceAsString(throwable), 3, throwable);
    }

    /**
     * Logs a message and an exception as a warning.
     *
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    public void warning(String message, Throwable throwable) {
        send(Level.WARNING, message, 2, throwable);
        send(Level.WARNING, getStackTraceAsString(throwable), 3, throwable);
    }

    /* ===========================
       ====== ERROR METHODS =======
       =========================== */

    /**
     * Logs an error message.
     *
     * @param message The error message.
     */
    public void error(String message) {
        send(Level.SEVERE, message, 2, null);
    }

    /**
     * Logs a formatted error message.
     *
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void error(String format, Object... args) {
        send(Level.SEVERE, formatMessage(format, args), 2, null);
    }

    /**
     * Logs an exception as an error message.
     *
     * @param throwable The throwable to log.
     */
    public void error(Throwable throwable) {
        send(Level.SEVERE, getStackTraceAsString(throwable), 2, throwable);
    }

    /**
     * Logs an error message with a specific log level.
     *
     * @param level   The logging level.
     * @param message The message to log.
     */
    public void error(Level level, String message) {
        send(level, message, 2, null);
    }

    /**
     * Logs a formatted error message with a specific log level.
     *
     * @param level  The logging level.
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void error(Level level, String format, Object... args) {
        send(level, formatMessage(format, args), 2, null);
    }

    /**
     * Logs an exception with a specific log level as an error.
     *
     * @param level     The logging level.
     * @param throwable The throwable to log.
     */
    public void error(Level level, Throwable throwable) {
        send(level, getStackTraceAsString(throwable), 2, throwable);
    }

    /**
     * Logs an error message and an exception with a specific log level.
     *
     * @param level     The logging level.
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    public void error(Level level, String message, Throwable throwable) {
        send(level, message, 2, throwable);
        send(level, getStackTraceAsString(throwable), 3, throwable);
    }

    /**
     * Logs a message and an exception as an error.
     *
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    public void error(String message, Throwable throwable) {
        send(Level.SEVERE, message, 2, throwable);
        send(Level.SEVERE, getStackTraceAsString(throwable), 3, throwable);
    }

    /* ===========================
       ====== SEVERE METHODS ======
       =========================== */

    /**
     * Logs a severe error message.
     *
     * @param message The severe error message.
     */
    public void severe(String message) {
        send(Level.SEVERE, message, 2, null);
    }

    /**
     * Logs a formatted severe error message.
     *
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void severe(String format, Object... args) {
        send(Level.SEVERE, formatMessage(format, args), 2, null);
    }

    /**
     * Logs an exception as a severe error message.
     *
     * @param throwable The throwable to log.
     */
    public void severe(Throwable throwable) {
        send(Level.SEVERE, getStackTraceAsString(throwable), 2, throwable);
    }

    /**
     * Logs a severe error message with a specific log level.
     *
     * @param level   The logging level.
     * @param message The message to log.
     */
    public void severe(Level level, String message) {
        send(level, message, 2, null);
    }

    /**
     * Logs a formatted severe error message with a specific log level.
     *
     * @param level  The logging level.
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void severe(Level level, String format, Object... args) {
        send(level, formatMessage(format, args), 2, null);
    }

    /**
     * Logs an exception with a specific log level as a severe error.
     *
     * @param level     The logging level.
     * @param throwable The throwable to log.
     */
    public void severe(Level level, Throwable throwable) {
        send(level, getStackTraceAsString(throwable), 2, throwable);
    }

    /**
     * Logs a severe error message and an exception with a specific log level.
     *
     * @param level     The logging level.
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    public void severe(Level level, String message, Throwable throwable) {
        send(level, message, 2, throwable);
        send(level, getStackTraceAsString(throwable), 3, throwable);
    }

    /**
     * Logs a message and an exception as a severe error.
     *
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    public void severe(String message, Throwable throwable) {
        send(Level.SEVERE, message, 2, throwable);
        send(Level.SEVERE, getStackTraceAsString(throwable), 3, throwable);
    }

    /* ===========================
       ====== WARN METHODS ========
       =========================== */

    /**
     * Logs a warning message.
     *
     * @param message The warning message.
     */
    public void warn(String message) {
        warning(message);
    }

    /**
     * Logs a formatted warning message.
     *
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void warn(String format, Object... args) {
        warning(format, args);
    }

    /**
     * Logs an exception as a warning message.
     *
     * @param throwable The throwable to log.
     */
    public void warn(Throwable throwable) {
        warning(throwable);
    }

    /**
     * Logs a warning message with a specific log level.
     *
     * @param level   The logging level.
     * @param message The message to log.
     */
    public void warn(Level level, String message) {
        warning(level, message);
    }

    /**
     * Logs a formatted warning message with a specific log level.
     *
     * @param level  The logging level.
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void warn(Level level, String format, Object... args) {
        warning(level, format, args);
    }

    /**
     * Logs an exception with a specific log level as a warning.
     *
     * @param level     The logging level.
     * @param throwable The throwable to log.
     */
    public void warn(Level level, Throwable throwable) {
        warning(level, throwable);
    }

    /**
     * Logs a warning message and an exception with a specific log level.
     *
     * @param level     The logging level.
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    public void warn(Level level, String message, Throwable throwable) {
        warning(level, message, throwable);
    }

    /**
     * Logs a message and an exception as a warning.
     *
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    public void warn(String message, Throwable throwable) {
        warning(message, throwable);
    }

    /* ===========================
       ====== DEBUG METHODS =======
       =========================== */

    /**
     * Logs a debug message.
     *
     * @param message The debug message.
     */
    public void debug(String message) {
        if (!debuggingEnabled) return;
        send(Level.INFO, message, 2, null);
    }

    /**
     * Logs a formatted debug message.
     *
     * @param format The message format.
     * @param args   The arguments for formatting.
     */
    public void debug(String format, Object... args) {
        if (!debuggingEnabled) return;
        send(Level.INFO, formatMessage(format, args), 2, null);
    }

    /* ===========================
       ====== CORE METHODS =========
       =========================== */

    /**
     * Core method to handle sending log messages to the console.
     *
     * @param level     The logging level.
     * @param message   The message to log.
     * @param depth     The stack trace depth to identify the caller.
     * @param throwable The throwable to log (can be null).
     */
    private void send(Level level, String message, int depth, Throwable throwable) {
        if (!debuggingEnabled) return;

        String className = getCallerClassName(depth);
        String timestamp = LocalDateTime.now().format(dtFormatter);
        String formattedMessage = formatLogMessage(level, timestamp, className, message);

        // Send message to Bukkit console with colors via Utils.$()
        MysticCore.getInstance().getServer().getConsoleSender().sendMessage(Utils.getInstance().$(formattedMessage));
    }

    /**
     * Formats the log message with color codes, prefixes, and timestamps.
     *
     * @param level     The logging level.
     * @param timestamp The current timestamp.
     * @param className The name of the class logging the message.
     * @param message   The message to format.
     * @return The formatted log message.
     */
    private String formatLogMessage(Level level, String timestamp, String className, String message) {
        String prefix;
        ChatColor levelColor;

        if (level == Level.WARNING) {
            levelColor = ChatColor.YELLOW;
            prefix = "&8[&eWarning &8| " + levelColor + level.getName() + "&8] ";
        } else if (level == Level.SEVERE) {
            levelColor = ChatColor.RED;
            prefix = "&8[&cError &8| " + levelColor + level.getName() + "&8] ";
        } else { // INFO and others
            levelColor = ChatColor.GREEN;
            prefix = "&8[&aInfo &8| " + levelColor + level.getName() + "&8] ";
        }

        String formattedPrefix = String.format("%s%s [%s] %s: ", ChatColor.DARK_GRAY, ChatColor.BOLD, timestamp, prefix);
        return String.format("%s%s%s", formattedPrefix, ChatColor.RESET, message);
    }

    /**
     * Formats a message by replacing '{}' with '%s' and applying String.format.
     *
     * @param format The message format containing '{}' or '%s' placeholders.
     * @param args   The arguments for formatting.
     * @return The formatted message.
     */
    private String formatMessage(String format, Object... args) {
        if (format == null) return "null";
        // Replace '{}' with '%s'
        String formatted = format.replace("{}", "%s");
        try {
            return String.format(formatted, args);
        } catch (Exception e) {
            // If formatting fails, return the original message with a warning
            warning("Failed to format message: " + format, e);
            return format;
        }
    }

    /**
     * Converts an object's value to a string, handling nulls and complex objects.
     *
     * @param obj The object to convert.
     * @return The string representation of the object.
     */
    private String formatObject(Object obj) {
        if (obj == null) return "null";

        // For simple types, return their toString()
        if (obj instanceof Number || obj instanceof Boolean || obj instanceof Character || obj instanceof String) {
            return obj.toString();
        }

        // For arrays, list their contents
        if (obj.getClass().isArray()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            int length = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < length; i++) {
                sb.append(formatObject(java.lang.reflect.Array.get(obj, i)));
                if (i < length - 1) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }

        // For other objects, use reflection to get fields
        StringBuilder sb = new StringBuilder();
        sb.append(obj.getClass().getSimpleName()).append(" {");

        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                sb.append("\n  ").append(field.getName()).append(": ").append(formatObject(value));
            } catch (IllegalAccessException e) {
                sb.append("\n  ").append(field.getName()).append(": [access denied]");
            }
        }
        sb.append("\n}");
        return sb.toString();
    }

    /**
     * Converts a throwable's stack trace to a string.
     *
     * @param throwable The throwable to convert.
     * @return The stack trace as a string.
     */
    private String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Retrieves the caller's class name based on the stack trace depth using StackWalker.
     *
     * @param stackTraceDepth The depth in the stack trace.
     * @return The caller's class name without package.
     */
    private String getCallerClassName(int stackTraceDepth) {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(stream -> stream.skip(3 + stackTraceDepth).findFirst())
                .map(StackWalker.StackFrame::getClassName)
                .map(fullClassName -> {
                    int lastDot = fullClassName.lastIndexOf(".");
                    return lastDot != -1 ? fullClassName.substring(lastDot + 1) : fullClassName;
                })
                .orElse("UnknownClass");
    }
}
