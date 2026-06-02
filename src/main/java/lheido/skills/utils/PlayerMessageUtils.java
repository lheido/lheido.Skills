package lheido.skills.utils;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;

/**
 * Utility methods for sending messages to players with the current Hytale API.
 */
public final class PlayerMessageUtils {

    private PlayerMessageUtils() {}

    public static void sendMessage(Player player, Message message) {
        if (
            player == null || player.getPlayerRef() == null || message == null
        ) {
            return;
        }

        player.getPlayerRef().sendMessage(message);
    }
}
