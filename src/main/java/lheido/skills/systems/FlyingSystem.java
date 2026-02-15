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
 * Système ECS qui gère la logique du skill Flying.
 *
 * Cycle de vie :
 * 1. canFly = true (set par l'interaction lors du unlock)
 * 2. Joueur fait double-espace → movementStates.flying = true → démarrage du timer
 * 3. Timer à 0 → forceStopFlying + canFly = false + démarrage cooldown
 * 4. Cooldown à 0 → canFly = true
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

        // Récupérer le component Flying
        FlyingSkillComponent flyingComponent = commandBuffer.getComponent(
            entityRef,
            FlyingSkillComponent.getComponentType()
        );
        if (flyingComponent == null) {
            return;
        }

        // Récupérer les composants nécessaires
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

        // Vérifier si le joueur est en train de voler (via double-espace)
        boolean isPlayerFlying = MovementUtils.isCurrentlyFlying(
            statesComponent
        );

        // État 1: Le joueur vole et le timer n'a pas commencé → démarrer le timer
        if (isPlayerFlying && !flyingComponent.isFlying()) {
            startFlyingTimer(flyingComponent, player);
            return;
        }

        // État 2: Le timer de vol est actif → vérifier si le temps est écoulé
        if (flyingComponent.isFlying()) {
            if (flyingComponent.getRemainingFlyTime() <= 0) {
                // Temps de vol terminé → arrêter le vol et démarrer le cooldown
                stopFlying(
                    flyingComponent,
                    movementManager,
                    statesComponent,
                    packetHandler,
                    player
                );
            }
            return;
        }

        // État 3: En cooldown → vérifier si le cooldown est terminé
        if (flyingComponent.isOnCooldown()) {
            if (flyingComponent.getRemainingCooldown() <= 0) {
                // Cooldown terminé → réactiver canFly
                enableCanFly(
                    flyingComponent,
                    movementManager,
                    packetHandler,
                    player
                );
            }
            return;
        }

        // État 4: Le joueur a le component mais canFly n'est pas activé (ex: reconnexion)
        // → Réactiver canFly pour permettre le double-espace
        if (!movementManager.getSettings().canFly) {
            MovementUtils.setCanFly(movementManager, packetHandler, true);
            LOGGER.atInfo().log(
                "Player reconnected with Flying skill, canFly re-enabled"
            );
        }
    }

    /**
     * Démarre le timer de vol quand le joueur fait double-espace.
     */
    private void startFlyingTimer(
        FlyingSkillComponent component,
        Player player
    ) {
        component.startFlying();

        long durationSeconds = (long) msToSeconds(component.getFlyDurationMs());
        player.sendMessage(
            Message.raw("Flying for " + durationSeconds + " seconds!")
        );

        LOGGER.atInfo().log("Player started flying, timer started");
    }

    /**
     * Arrête le vol, désactive canFly et démarre le cooldown.
     */
    private void stopFlying(
        FlyingSkillComponent component,
        MovementManager movementManager,
        MovementStatesComponent statesComponent,
        PacketHandler packetHandler,
        Player player
    ) {
        // Arrêter le timer et démarrer le cooldown
        component.stopFlying();

        // Forcer l'arrêt du vol et désactiver canFly
        MovementUtils.disableFlying(
            movementManager,
            statesComponent,
            packetHandler
        );

        // Notifier le joueur
        long cooldownSeconds = (long) msToSeconds(component.getCooldownMs());
        player.sendMessage(
            Message.raw(
                "Flying ended! Cooldown: " + cooldownSeconds + " seconds."
            )
        );

        LOGGER.atInfo().log("Player flying ended, cooldown started");
    }

    /**
     * Réactive canFly après la fin du cooldown.
     */
    private void enableCanFly(
        FlyingSkillComponent component,
        MovementManager movementManager,
        PacketHandler packetHandler,
        Player player
    ) {
        // Reset le cooldown end time pour éviter de réactiver en boucle
        component.setCooldownEndTime(0L);

        // Réactiver canFly
        MovementUtils.setCanFly(movementManager, packetHandler, true);

        player.sendMessage(
            Message.raw("Flying is ready! Double-tap space to fly.")
        );

        LOGGER.atInfo().log("Player flying cooldown ended, canFly re-enabled");
    }
}
