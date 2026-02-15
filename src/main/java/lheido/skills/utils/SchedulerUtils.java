package lheido.skills.utils;

/**
 * Utilitaires pour la gestion du temps et des conversions.
 * Utilisé pour les calculs de durée et cooldown des skills.
 */
public final class SchedulerUtils {

    private SchedulerUtils() {
        // Utility class - pas d'instanciation
    }

    /**
     * Convertit des millisecondes en secondes.
     *
     * @param ms Millisecondes
     * @return Secondes
     */
    public static double msToSeconds(long ms) {
        return ms / 1000.0;
    }

    /**
     * Convertit des secondes en millisecondes.
     *
     * @param seconds Secondes
     * @return Millisecondes
     */
    public static long secondsToMs(double seconds) {
        return (long) (seconds * 1000);
    }

    /**
     * Convertit des ticks en millisecondes (20 ticks = 1 seconde).
     *
     * @param ticks Nombre de ticks
     * @return Millisecondes
     */
    public static long ticksToMs(int ticks) {
        return ticks * 50L; // 1 tick = 50ms (20 ticks/sec)
    }

    /**
     * Convertit des millisecondes en ticks (20 ticks = 1 seconde).
     *
     * @param ms Millisecondes
     * @return Nombre de ticks
     */
    public static int msToTicks(long ms) {
        return (int) (ms / 50L);
    }
}
