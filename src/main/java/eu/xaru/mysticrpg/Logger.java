package eu.xaru.mysticrpg;

import java.util.logging.Level;

public class Logger {
    public static void log(String message) {
        Main.getInstance().getLogger().log(Level.INFO, message);
    }

    public static void error(String message) {
        Main.getInstance().getLogger().log(Level.SEVERE, message);
    }
}
