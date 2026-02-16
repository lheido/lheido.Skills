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
 * Utilitaires pour la gestion des mouvements des joueurs.
 * Utilisable par plusieurs skills liés au mouvement (Flying, Swimming, etc.).
 */
public final class MovementUtils {

    private MovementUtils() {
        // Utility class - pas d'instanciation
    }

    /**
     * Active ou désactive la capacité de voler pour un joueur.
     * Ne fait rien si l'état est déjà celui demandé.
     *
     * @param movement      Le MovementManager du joueur
     * @param packetHandler Le PacketHandler pour envoyer les updates au client
     * @param canFly        true pour permettre le vol, false sinon
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
     * Force l'arrêt du vol pour un joueur.
     * Utile quand le temps de vol expire ou que le skill est désactivé.
     *
     * @param statesComponent Le MovementStatesComponent du joueur
     * @param packetHandler   Le PacketHandler pour envoyer les updates au client
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
     * Vérifie si un joueur est actuellement en train de voler.
     *
     * @param statesComponent Le MovementStatesComponent du joueur
     * @return true si le joueur vole, false sinon
     */
    public static boolean isCurrentlyFlying(@Nonnull MovementStatesComponent statesComponent) {
        MovementStates movementStates = statesComponent.getMovementStates();
        return movementStates != null && movementStates.flying;
    }

    /**
     * Active le vol pour un joueur et met à jour le client.
     *
     * @param movement        Le MovementManager du joueur
     * @param statesComponent Le MovementStatesComponent du joueur
     * @param packetHandler   Le PacketHandler pour envoyer les updates au client
     */
    public static void enableFlying(
            @Nonnull MovementManager movement,
            @Nonnull MovementStatesComponent statesComponent,
            @Nonnull PacketHandler packetHandler
    ) {
        // Permettre le vol
        setCanFly(movement, packetHandler, true);
    }

    /**
     * Force le démarrage du vol pour un joueur.
     * Utilisé pour restaurer l'état de vol après reconnexion.
     *
     * @param movement        Le MovementManager du joueur
     * @param statesComponent Le MovementStatesComponent du joueur
     * @param packetHandler   Le PacketHandler pour envoyer les updates au client
     */
    public static void forceStartFlying(
            @Nonnull MovementManager movement,
            @Nonnull MovementStatesComponent statesComponent,
            @Nonnull PacketHandler packetHandler
    ) {
        // S'assurer que canFly est activé
        MovementSettings settings = movement.getSettings();
        if (settings != null) {
            settings.canFly = true;
            movement.update(packetHandler);
        }
        
        // Forcer l'état de vol
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
     * Désactive complètement le vol pour un joueur.
     * Force l'arrêt du vol s'il est en cours, puis retire la capacité de voler.
     *
     * @param movement        Le MovementManager du joueur
     * @param statesComponent Le MovementStatesComponent du joueur
     * @param packetHandler   Le PacketHandler pour envoyer les updates au client
     */
    public static void disableFlying(
            @Nonnull MovementManager movement,
            @Nonnull MovementStatesComponent statesComponent,
            @Nonnull PacketHandler packetHandler
    ) {
        // Force l'arrêt du vol si en cours
        forceStopFlying(statesComponent, packetHandler);
        // Retire la capacité de voler
        setCanFly(movement, packetHandler, false);
    }
}
