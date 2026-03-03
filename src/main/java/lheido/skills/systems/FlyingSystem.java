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
 * - FLYING: Timer de vol actif (décrémenté chaque tick, joueur peut voler/atterrir)
 * - COOLDOWN: Le skill est en cooldown (timer décrémenté chaque tick)
 *
 * Transitions:
 * - READY → FLYING: Joueur fait double-espace (timer démarre)
 * - FLYING → COOLDOWN: Timer de vol expiré (= 0)
 * - COOLDOWN → READY: Cooldown expiré
 *
 * Le joueur peut atterrir et revoler librement tant que le timer de vol n'est pas à 0.
 *
 * Ce système vérifie aussi périodiquement que canFly est synchronisé
 * avec l'état du skill (protection contre les désync après sommeil, etc.)
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

        // Décrémenter le timer approprié à chaque tick
        flyingComponent.decrementTimer(deltaTime);

        // Vérification périodique de la synchronisation canFly
        // Protection contre les désync (sommeil, téléportation, etc.)
        syncCanFlyState(flyingComponent, movementManager, packetHandler);

        // Traiter l'état actuel
        switch (flyingComponent.getState()) {
            case READY -> handleReadyState(
                flyingComponent,
                statesComponent,
                player
            );
            case FLYING -> handleFlyingState(
                flyingComponent,
                movementManager,
                statesComponent,
                packetHandler,
                player
            );
            case COOLDOWN -> handleCooldownState(flyingComponent, player);
        }
    }

    /**
     * Vérifie et corrige la synchronisation de canFly avec l'état du skill.
     * Appelé à chaque tick pour détecter les désynchronisations.
     */
    private void syncCanFlyState(
        FlyingSkillComponent component,
        MovementManager movementManager,
        PacketHandler packetHandler
    ) {
        MovementSettings settings = movementManager.getSettings();
        if (settings == null) {
            return;
        }

        boolean shouldCanFly = component.shouldCanFly();
        if (settings.canFly != shouldCanFly) {
            MovementUtils.setCanFly(
                movementManager,
                packetHandler,
                shouldCanFly
            );
        }
    }

    /**
     * État READY: Le joueur peut voler.
     * Transition vers FLYING si le joueur fait double-espace.
     */
    private void handleReadyState(
        FlyingSkillComponent component,
        MovementStatesComponent statesComponent,
        Player player
    ) {
        // Vérifier si le joueur commence à voler (double-espace)
        boolean isPlayerFlying = MovementUtils.isCurrentlyFlying(
            statesComponent
        );
        if (isPlayerFlying) {
            component.transitionToFlying();

            if (component.isUnlimitedFlight()) {
                player.sendMessage(
                    Message.raw("Flying with unlimited duration!")
                );
            } else {
                long durationSeconds = (long) msToSeconds(
                    component.getFlyDurationMs()
                );
                player.sendMessage(
                    Message.raw("Flying for " + durationSeconds + " seconds!")
                );
            }

        }
    }

    /**
     * État FLYING: Le timer de vol est actif.
     * Le joueur peut voler/atterrir librement.
     * Transition vers COOLDOWN uniquement quand le timer expire.
     */
    private void handleFlyingState(
        FlyingSkillComponent component,
        MovementManager movementManager,
        MovementStatesComponent statesComponent,
        PacketHandler packetHandler,
        Player player
    ) {
        // Timer de vol expiré → cooldown
        if (component.isFlyTimeExpired()) {
            component.transitionToCooldown();

            // Forcer l'arrêt du vol si le joueur est en l'air
            boolean isActuallyFlying = MovementUtils.isCurrentlyFlying(statesComponent);
            if (isActuallyFlying) {
                MovementUtils.forceStopFlying(statesComponent, packetHandler);
            }

            long cooldownSeconds = (long) msToSeconds(component.getCooldownMs());
            player.sendMessage(
                Message.raw("Flying ended! Cooldown: " + cooldownSeconds + " seconds.")
            );
        }
    }

    /**
     * État COOLDOWN: Le skill est en cooldown.
     * Transition vers READY quand le cooldown expire.
     */
    private void handleCooldownState(
        FlyingSkillComponent component,
        Player player
    ) {
        if (component.isCooldownExpired()) {
            component.transitionToReady();
            player.sendMessage(
                Message.raw("Flying is ready! Double-tap space to fly.")
            );
        }
    }
}
