package lheido.skills.utils;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Utility methods for sending messages to players with the current Hytale API.
 */
public final class PlayerMessageUtils {

    private PlayerMessageUtils() {}

    public static void sendMessage(PlayerRef playerRef, Message message) {
        if (playerRef == null || message == null) {
            return;
        }

        playerRef.sendMessage(message);
    }
}
