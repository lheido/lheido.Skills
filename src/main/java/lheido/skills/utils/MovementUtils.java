package lheido.skills.utils;

import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.SavedMovementStates;
import com.hypixel.hytale.protocol.packets.player.SetMovementStates;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import javax.annotation.Nonnull;

/**
 * Utilities for managing player movements.
 * Usable by multiple movement-related skills (Flying, Swimming, etc.).
 */
public final class MovementUtils {

    private MovementUtils() {
        // Utility class - no instantiation
    }

    /**
     * Enables or disables the ability to fly for a player.
     * Does nothing if the state is already the requested one.
     *
     * @param movement      The player's MovementManager
     * @param packetHandler The PacketHandler to send updates to the client
     * @param canFly        true to allow flying, false otherwise
     */
    public static void setCanFly(
        @Nonnull MovementManager movement,
        @Nonnull PacketHandler packetHandler,
        boolean canFly
    ) {
        MovementSettings settings = movement.getSettings();
        if (settings.canFly == canFly) {
            return;
        }
        settings.canFly = canFly;
        movement.update(packetHandler);
    }

    /**
     * Forces sending the UpdateMovementSettings packet to the client,
     * even if the server-side value is already correct.
     *
     * Useful after events that can desynchronize the client
     * (waking from bed, teleportation, etc.) where the server has the correct
     * value but the client has been reset.
     *
     * @param movement      The player's MovementManager
     * @param packetHandler The PacketHandler to send updates to the client
     * @param canFly        true to allow flying, false otherwise
     */
    public static void forceSetCanFly(
        @Nonnull MovementManager movement,
        @Nonnull PacketHandler packetHandler,
        boolean canFly
    ) {
        MovementSettings settings = movement.getSettings();
        settings.canFly = canFly;
        // Always send the packet, even if the value hasn't changed
        movement.update(packetHandler);
    }

    /**
     * Forces a player to stop flying.
     * Useful when flight time expires or the skill is deactivated.
     *
     * @param statesComponent The player's MovementStatesComponent
     * @param packetHandler   The PacketHandler to send updates to the client
     */
    public static void forceStopFlying(
        @Nonnull MovementStatesComponent statesComponent,
        @Nonnull PacketHandler packetHandler
    ) {
        MovementStates movementStates = statesComponent.getMovementStates();
        if (movementStates == null || !movementStates.flying) {
            return;
        }
        movementStates.flying = false;
        statesComponent.setMovementStates(movementStates);
        packetHandler.writeNoCache(
            new SetMovementStates(new SavedMovementStates(false))
        );
    }

    /**
     * Checks if a player is currently flying.
     *
     * @param statesComponent The player's MovementStatesComponent
     * @return true if the player is flying, false otherwise
     */
    public static boolean isCurrentlyFlying(
        @Nonnull MovementStatesComponent statesComponent
    ) {
        MovementStates movementStates = statesComponent.getMovementStates();
        return movementStates != null && movementStates.flying;
    }

    /**
     * Enables flying for a player and updates the client.
     *
     * @param movement        The player's MovementManager
     * @param statesComponent The player's MovementStatesComponent
     * @param packetHandler   The PacketHandler to send updates to the client
     */
    public static void enableFlying(
        @Nonnull MovementManager movement,
        @Nonnull MovementStatesComponent statesComponent,
        @Nonnull PacketHandler packetHandler
    ) {
        // Allow flying
        setCanFly(movement, packetHandler, true);
    }

    /**
     * Forces flight to start for a player.
     * Used to restore the flight state after reconnection.
     *
     * @param movement        The player's MovementManager
     * @param statesComponent The player's MovementStatesComponent
     * @param packetHandler   The PacketHandler to send updates to the client
     */
    public static void forceStartFlying(
        @Nonnull MovementManager movement,
        @Nonnull MovementStatesComponent statesComponent,
        @Nonnull PacketHandler packetHandler
    ) {
        // Ensure canFly is enabled
        MovementSettings settings = movement.getSettings();
        if (settings != null) {
            settings.canFly = true;
            movement.update(packetHandler);
        }

        // Force the flight state
        MovementStates movementStates = statesComponent.getMovementStates();
        if (movementStates == null) {
            movementStates = new MovementStates();
            statesComponent.setMovementStates(movementStates);
        }
        movementStates.flying = true;
        statesComponent.setMovementStates(movementStates);
        packetHandler.writeNoCache(
            new SetMovementStates(new SavedMovementStates(true))
        );
    }

    /**
     * Completely disables flying for a player.
     * Forces flight to stop if in progress, then removes the ability to fly.
     *
     * @param movement        The player's MovementManager
     * @param statesComponent The player's MovementStatesComponent
     * @param packetHandler   The PacketHandler to send updates to the client
     */
    public static void disableFlying(
        @Nonnull MovementManager movement,
        @Nonnull MovementStatesComponent statesComponent,
        @Nonnull PacketHandler packetHandler
    ) {
        // Force stop flying if in progress
        forceStopFlying(statesComponent, packetHandler);
        // Remove the ability to fly
        setCanFly(movement, packetHandler, false);
    }
}
