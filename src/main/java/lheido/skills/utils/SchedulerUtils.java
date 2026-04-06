package lheido.skills.utils;

/**
 * Utilities for time management and conversions.
 * Used for skill duration and cooldown calculations.
 */
public final class SchedulerUtils {

    private SchedulerUtils() {
        // Utility class - no instantiation
    }

    /**
     * Converts milliseconds to seconds.
     *
     * @param ms Milliseconds
     * @return Seconds
     */
    public static double msToSeconds(long ms) {
        return ms / 1000.0;
    }

    /**
     * Converts seconds to milliseconds.
     *
     * @param seconds Seconds
     * @return Milliseconds
     */
    public static long secondsToMs(double seconds) {
        return (long) (seconds * 1000);
    }

    /**
     * Converts ticks to milliseconds (20 ticks = 1 second).
     *
     * @param ticks Number of ticks
     * @return Milliseconds
     */
    public static long ticksToMs(int ticks) {
        return ticks * 50L; // 1 tick = 50ms (20 ticks/sec)
    }

    /**
     * Converts milliseconds to ticks (20 ticks = 1 second).
     *
     * @param ms Milliseconds
     * @return Number of ticks
     */
    public static int msToTicks(long ms) {
        return (int) (ms / 50L);
    }
}
