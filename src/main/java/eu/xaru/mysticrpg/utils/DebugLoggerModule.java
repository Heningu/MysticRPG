package eu.xaru.mysticrpg.utils;

import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * DebugLoggerModule handles logging functionalities for the plugin.
 */
public class DebugLoggerModule implements IBaseModule {

    private final boolean debuggingEnabled = true;

    @Override
    public void initialize() {
        log(Level.INFO, "DebugLoggerModule initialized", 0);
    }

    @Override
    public void start() {
        log(Level.INFO, "DebugLoggerModule started", 0);
    }

    @Override
    public void stop() {
        log(Level.INFO, "DebugLoggerModule stopped", 0);
    }

    @Override
    public void unload() {
        log(Level.INFO, "DebugLoggerModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();  // No dependencies as it should be the first loaded
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.FIRST;
    }

    /**
     * Logs a message with the specified level and depth.
     *
     * @param level   The logging level.
     * @param message The message to log.
     * @param depth   The stack trace depth to identify the caller.
     */
    public void log(Level level, String message, int depth) {
        if (debuggingEnabled) {
            String className = getCallerClassName(depth);
            String formattedMessage = formatLogMessage(level, className, message);
            Bukkit.getConsoleSender().sendMessage(Utils.getInstance().$(formattedMessage));
        }
    }

    /**
     * Logs a simple informational message.
     *
     * @param message The message to log.
     */
    public void log(String message) {
        log(Level.INFO, message, 2);
    }

    /**
     * Logs a formatted informational message with arguments.
     *
     * @param message The message format.
     * @param args    The arguments for formatting.
     */
    public void log(String message, Object... args) {
        log(Level.INFO, String.format(message, args), 2);
    }

    /**
     * Logs the state of an object.
     *
     * @param obj The object to log.
     */
    public void logObject(Object obj) {
        log(Level.INFO, "Object state:", 2);
        log(Level.INFO, formatObject(obj), 3);
    }

    /**
     * Logs a warning message.
     *
     * @param message The warning message.
     */
    public void warn(String message) {
        log(WARNING, message, 2);
    }

    /**
     * Logs an error message.
     *
     * @param message The error message.
     */
    public void error(String message) {
        log(SEVERE, message, 2);
    }

    /**
     * Logs an error message along with a throwable and additional data.
     *
     * @param message   The error message.
     * @param throwable The throwable to log.
     * @param data      Additional data to log.
     */
    public void error(String message, Throwable throwable, Map<String, Object> data) {
        log(SEVERE, message, 2);
        log(SEVERE, getStackTraceAsString(throwable), 3);
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                log(SEVERE, String.format("%s: %s", entry.getKey(), entry.getValue()), 3);
            }
        }
    }

    /**
     * Formats the log message with color codes and prefixes.
     *
     * @param level     The logging level.
     * @param className The name of the class logging the message.
     * @param message   The message to format.
     * @return The formatted message.
     */
    private String formatLogMessage(Level level, String className, String message) {
        String prefix;
        ChatColor levelColor;

        if (Objects.equals(level, WARNING)) {
            levelColor = ChatColor.YELLOW;
            prefix = "&8[&eWarning &8| " + levelColor + level.getName() + "&8] &r";
        } else if (Objects.equals(level, SEVERE)) {
            levelColor = ChatColor.RED;
            prefix = "&8[&cError &8| " + levelColor + level.getName() + "&8] &r";
        } else {
            levelColor = ChatColor.WHITE;
            prefix = "&8[&bDebug &8| " + levelColor + level.getName() + "&8] &r";
        }

        String formattedPrefix = String.format("%s%s%s: ", ChatColor.DARK_GRAY, ChatColor.BOLD, prefix);
        return String.format("%s%s%s%s", formattedPrefix, className, ": ", message);
    }

    /**
     * Formats an object's state by reflecting its fields.
     *
     * @param obj The object to format.
     * @return The formatted object state.
     */
    private String formatObject(Object obj) {
        StringBuilder sb = new StringBuilder();
        if (obj == null) {
            return "Object is null";
        }
        sb.append("Object of class ").append(obj.getClass().getName()).append(" {\n");
        if (obj.getClass().isArray()) {
            int length = Array.getLength(obj);
            for (int i = 0; i < length; i++) {
                sb.append("  [").append(i).append("]: ").append(formatObject(Array.get(obj, i))).append("\n");
            }
        } else {
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    sb.append("  ").append(field.getName()).append(": ").append(field.get(obj)).append("\n");
                } catch (IllegalAccessException e) {
                    sb.append("  ").append(field.getName()).append(": [access denied]\n");
                }
            }
        }
        sb.append("}");
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
     * @return The caller's class name.
     */
    private String getCallerClassName(int stackTraceDepth) {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(stream -> stream.skip(3 + stackTraceDepth).findFirst())
                .map(StackWalker.StackFrame::getClassName)
                .map(fullClassName -> fullClassName.substring(fullClassName.lastIndexOf(".") + 1))
                .orElse("UnknownClass");
    }

}
