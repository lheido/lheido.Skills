package lheido.skills.systems;

import static lheido.skills.utils.SchedulerUtils.msToSeconds;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.utils.MovementUtils;

/**
 * Système ECS qui gère la logique du skill Flying via une state machine.
 *
 * États:
 * - READY: Le joueur peut voler (double-espace disponible)
 * - FLYING: Le joueur vole (timer actif)
 * - COOLDOWN: Le skill est en cooldown
 *
 * Transitions:
 * - READY → FLYING: Joueur fait double-espace
 * - FLYING → COOLDOWN: Timer de vol expiré
 * - COOLDOWN → READY: Cooldown expiré
 */
public class FlyingSystem extends EntityTickingSystem<EntityStore> {

    private static final ComponentType<EntityStore, PlayerRef> PLAYER_REF_TYPE =
        PlayerRef.getComponentType();
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public FlyingSystem() {
        super();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return FlyingSkillComponent.getComponentType();
    }

    @Override
    public void tick(
        float deltaTime,
        int entityIndex,
        ArchetypeChunk<EntityStore> chunk,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);

        FlyingSkillComponent flyingComponent = commandBuffer.getComponent(
            entityRef,
            FlyingSkillComponent.getComponentType()
        );
        if (flyingComponent == null) {
            return;
        }

        Player player = commandBuffer.getComponent(
            entityRef,
            Player.getComponentType()
        );
        if (player == null) {
            return;
        }

        MovementManager movementManager = commandBuffer.getComponent(
            entityRef,
            MovementManager.getComponentType()
        );
        MovementStatesComponent statesComponent = commandBuffer.getComponent(
            entityRef,
            MovementStatesComponent.getComponentType()
        );
        PlayerRef playerRef = chunk.getComponent(entityIndex, PLAYER_REF_TYPE);
        PacketHandler packetHandler =
            playerRef != null ? playerRef.getPacketHandler() : null;

        if (
            movementManager == null ||
            statesComponent == null ||
            packetHandler == null
        ) {
            LOGGER.atWarning().log(
                "FlyingSystem: Missing required components for player"
            );
            return;
        }

        // Traiter l'état actuel
        // Note: Après reconnexion, state est toujours READY (non persisté)
        switch (flyingComponent.getState()) {
            case READY -> handleReadyState(
                flyingComponent,
                movementManager,
                statesComponent,
                packetHandler,
                player
            );
            case FLYING -> handleFlyingState(
                flyingComponent,
                movementManager,
                statesComponent,
                packetHandler,
                player
            );
            case COOLDOWN -> handleCooldownState(
                flyingComponent,
                movementManager,
                packetHandler,
                player
            );
        }
    }

    /**
     * État READY: Le joueur peut voler.
     * Transition vers FLYING si le joueur fait double-espace.
     */
    private void handleReadyState(
        FlyingSkillComponent component,
        MovementManager movementManager,
        MovementStatesComponent statesComponent,
        PacketHandler packetHandler,
        Player player
    ) {
        MovementSettings settings = movementManager.getSettings();
        
        // Après reconnexion ou premier chargement
        if (component.needsResync()) {
            if (settings != null) {
                // Forcer canFly = true et synchroniser avec le client
                settings.canFly = true;
                movementManager.update(packetHandler);
            }
            
            // Si le joueur était en vol et a un vol illimité, restaurer l'état de vol
            if (component.wasFlying() && component.isUnlimitedFlight()) {
                MovementUtils.forceStartFlying(
                    movementManager,
                    statesComponent,
                    packetHandler
                );
                component.transitionToFlying();
                
                player.sendMessage(
                    Message.raw("Flight restored - unlimited duration!")
                );
                LOGGER.atInfo().log(
                    "Player with Flying X reconnected, flight state restored"
                );
            } else {
                LOGGER.atInfo().log(
                    "Player with Flying skill level " + component.getLevel() 
                    + " reconnected, canFly force-synced"
                );
            }
            
            component.markSynced();
            return; // Ne pas continuer le traitement normal ce tick
        }
        
        // Cas normal: réactiver canFly si désactivé
        if (settings != null && !settings.canFly) {
            MovementUtils.setCanFly(movementManager, packetHandler, true);
            LOGGER.atInfo().log(
                "Player with Flying skill level " + component.getLevel() 
                + ", canFly re-enabled"
            );
        }

        // Vérifier si le joueur commence à voler (double-espace)
        boolean isPlayerFlying = MovementUtils.isCurrentlyFlying(statesComponent);
        if (isPlayerFlying) {
            component.transitionToFlying();

            if (component.isUnlimitedFlight()) {
                player.sendMessage(
                    Message.raw("Flying with unlimited duration!")
                );
            } else {
                long durationSeconds = (long) msToSeconds(component.getFlyDurationMs());
                player.sendMessage(
                    Message.raw("Flying for " + durationSeconds + " seconds!")
                );
            }

            LOGGER.atInfo().log("Player started flying, timer started");
        }
    }

    /**
     * État FLYING: Le joueur est en vol.
     * Transition vers COOLDOWN quand le timer expire.
     */
    private void handleFlyingState(
        FlyingSkillComponent component,
        MovementManager movementManager,
        MovementStatesComponent statesComponent,
        PacketHandler packetHandler,
        Player player
    ) {
        if (component.isFlyTimeExpired()) {
            component.transitionToCooldown();

            // Forcer l'arrêt du vol et désactiver canFly
            MovementUtils.disableFlying(
                movementManager,
                statesComponent,
                packetHandler
            );

            long cooldownSeconds = (long) msToSeconds(component.getCooldownMs());
            player.sendMessage(
                Message.raw(
                    "Flying ended! Cooldown: " + cooldownSeconds + " seconds."
                )
            );

            LOGGER.atInfo().log("Player flying ended, cooldown started");
        }
    }

    /**
     * État COOLDOWN: Le skill est en cooldown.
     * Transition vers READY quand le cooldown expire.
     */
    private void handleCooldownState(
        FlyingSkillComponent component,
        MovementManager movementManager,
        PacketHandler packetHandler,
        Player player
    ) {
        if (component.isCooldownExpired()) {
            component.transitionToReady();

            // Réactiver canFly
            MovementUtils.setCanFly(movementManager, packetHandler, true);

            player.sendMessage(
                Message.raw("Flying is ready! Double-tap space to fly.")
            );

            LOGGER.atInfo().log("Player flying cooldown ended, canFly re-enabled");
        }
    }
}
