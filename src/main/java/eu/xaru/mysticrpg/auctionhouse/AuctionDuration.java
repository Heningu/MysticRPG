package eu.xaru.mysticrpg.auctionhouse;

/**
 * Utility class for managing auction durations.
 * Provides predefined durations and formatting methods.
 */
public class AuctionDuration {
    
    public static final long ONE_HOUR = 60 * 60 * 1000L; // 1 hour in milliseconds
    public static final long SIX_HOURS = 6 * 60 * 60 * 1000L; // 6 hours in milliseconds
    public static final long TWELVE_HOURS = 12 * 60 * 60 * 1000L; // 12 hours in milliseconds
    public static final long TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000L; // 24 hours in milliseconds
    public static final long FORTY_EIGHT_HOURS = 48 * 60 * 60 * 1000L; // 48 hours in milliseconds
    
    private static final long[] DURATIONS = {
        ONE_HOUR,
        SIX_HOURS,
        TWELVE_HOURS,
        TWENTY_FOUR_HOURS,
        FORTY_EIGHT_HOURS
    };
    
    private static final String[] DURATION_NAMES = {
        "1 Hour",
        "6 Hours", 
        "12 Hours",
        "24 Hours",
        "48 Hours"
    };
    
    /**
     * Get all available durations in milliseconds.
     */
    public static long[] getAllDurations() {
        return DURATIONS.clone();
    }
    
    /**
     * Get all duration display names.
     */
    public static String[] getAllDurationNames() {
        return DURATION_NAMES.clone();
    }
    
    /**
     * Get duration at specific index.
     */
    public static long getDuration(int index) {
        if (index < 0 || index >= DURATIONS.length) {
            return TWENTY_FOUR_HOURS; // Default to 24 hours
        }
        return DURATIONS[index];
    }
    
    /**
     * Get duration name at specific index.
     */
    public static String getDurationName(int index) {
        if (index < 0 || index >= DURATION_NAMES.length) {
            return "24 Hours"; // Default to 24 hours
        }
        return DURATION_NAMES[index];
    }
    
    /**
     * Get the index of the next duration (cycles through all durations).
     */
    public static int getNextDurationIndex(int currentIndex) {
        return (currentIndex + 1) % DURATIONS.length;
    }
    
    /**
     * Get the total number of available durations.
     */
    public static int getDurationCount() {
        return DURATIONS.length;
    }
    
    /**
     * Format remaining time into human-readable string.
     */
    public static String formatTimeRemaining(long remainingMillis) {
        if (remainingMillis <= 0) {
            return "Expired";
        }
        
        long seconds = remainingMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        
        if (days > 0) {
            return String.format("%dd %02dh %02dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}