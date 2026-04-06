package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.utils.MovementUtils;
import lheido.skills.utils.SkillIds;

/**
 * ECS system that manages the Flying skill logic via a state machine.
 *
 * States:
 * - READY: The player can fly (double-space available)
 * - FLYING: Flight timer active (decremented each tick, player can fly/land)
 * - COOLDOWN: The skill is on cooldown (timer decremented each tick)
 *
 * Transitions:
 * - READY → FLYING: Player double-presses space (timer starts)
 * - FLYING → COOLDOWN: Flight timer expired (= 0)
 * - COOLDOWN → READY: Cooldown expired
 *
 * The player can land and fly again freely as long as the flight timer has not reached 0.
 *
 * This system also periodically checks that canFly is synchronized
 * with the skill state (protection against desync after sleeping, etc.)
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

        // Check if the skill is active in ActiveSkillsComponent
        ActiveSkillsComponent activeSkills = commandBuffer.getComponent(
            entityRef,
            ActiveSkillsComponent.getComponentType()
        );
        boolean isSkillActive = isSkillActiveForPlayer(
            activeSkills,
            flyingComponent
        );

        if (!isSkillActive) {
            // Skill not active: disable effects and hide the HUD
            handleInactiveSkill(
                flyingComponent,
                movementManager,
                statesComponent,
                packetHandler,
                player,
                playerRef
            );
            return;
        }

        // Decrement the appropriate timer each tick
        flyingComponent.decrementTimer(deltaTime);

        // Periodic canFly synchronization check
        // Protection against desync (sleeping, teleportation, etc.)
        // We use a periodic forced resync to ensure the client
        // always has the correct value even after desync events
        boolean forceResync = flyingComponent.accumulateResyncTime(deltaTime);
        syncCanFlyState(
            flyingComponent,
            movementManager,
            packetHandler,
            forceResync
        );

        // Process the current state
        switch (flyingComponent.getState()) {
            case READY -> handleReadyState(
                flyingComponent,
                statesComponent,
                player,
                playerRef
            );
            case FLYING -> handleFlyingState(
                flyingComponent,
                movementManager,
                statesComponent,
                packetHandler,
                player,
                playerRef
            );
            case COOLDOWN -> handleCooldownState(
                flyingComponent,
                player,
                playerRef
            );
        }
    }

    /**
     * Checks if the Flying skill is active for the player.
     *
     * A skill is considered active if:
     * - The player has an ActiveSkillsComponent
     * - One of the active skills is a Flying skill (starts with "Skill_Flying_")
     */
    private boolean isSkillActiveForPlayer(
        ActiveSkillsComponent activeSkills,
        FlyingSkillComponent flyingComponent
    ) {
        if (activeSkills == null) {
            return false;
        }

        // Check if a Flying skill is in the active slots
        for (String activeSkill : activeSkills.getActiveSkills()) {
            if (SkillIds.isFlyingSkill(activeSkill)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the case where the skill is not active.
     *
     * - Disables flight if the player was flying
     * - Resets the skill state
     *
     * Note: HUD display is managed by SkillBarSystem.
     */
    private void handleInactiveSkill(
        FlyingSkillComponent component,
        MovementManager movementManager,
        MovementStatesComponent statesComponent,
        PacketHandler packetHandler,
        Player player,
        PlayerRef playerRef
    ) {
        // Force stop flying if the player is in the air
        boolean isActuallyFlying = MovementUtils.isCurrentlyFlying(
            statesComponent
        );
        if (isActuallyFlying) {
            MovementUtils.forceStopFlying(statesComponent, packetHandler);
        }

        // Disable canFly
        MovementSettings settings = movementManager.getSettings();
        if (settings != null && settings.canFly) {
            MovementUtils.setCanFly(movementManager, packetHandler, false);
        }

        // Reset the skill state to be ready if reactivated
        component.transitionToReady();
    }

    /**
     * Checks and corrects the canFly synchronization with the skill state.
     * Called each tick to detect desynchronizations.
     *
     * @param component        The player's FlyingSkillComponent
     * @param movementManager  The player's MovementManager
     * @param packetHandler    The PacketHandler to send packets
     * @param forceResync      If true, forces sending the packet even if the server
     *                         value is already correct (useful after waking from bed, etc.)
     */
    private void syncCanFlyState(
        FlyingSkillComponent component,
        MovementManager movementManager,
        PacketHandler packetHandler,
        boolean forceResync
    ) {
        MovementSettings settings = movementManager.getSettings();
        if (settings == null) {
            return;
        }

        boolean shouldCanFly = component.shouldCanFly();

        if (forceResync) {
            // Force send the packet to resync the client
            MovementUtils.forceSetCanFly(
                movementManager,
                packetHandler,
                shouldCanFly
            );
        } else if (settings.canFly != shouldCanFly) {
            // Normal correction if desync detected on server side
            MovementUtils.setCanFly(
                movementManager,
                packetHandler,
                shouldCanFly
            );
        }
    }

    /**
     * READY state: The player can fly.
     * Transitions to FLYING if the player double-presses space.
     *
     * Note: HUD display is managed by SkillBarSystem.
     */
    private void handleReadyState(
        FlyingSkillComponent component,
        MovementStatesComponent statesComponent,
        Player player,
        PlayerRef playerRef
    ) {
        // Check if the player starts flying (double-space)
        boolean isPlayerFlying = MovementUtils.isCurrentlyFlying(
            statesComponent
        );
        if (isPlayerFlying) {
            component.transitionToFlying();
        }
    }

    /**
     * FLYING state: The flight timer is active.
     * The player can fly/land freely.
     * Transitions to COOLDOWN only when the timer expires.
     *
     * Note: HUD display is managed by SkillBarSystem.
     */
    private void handleFlyingState(
        FlyingSkillComponent component,
        MovementManager movementManager,
        MovementStatesComponent statesComponent,
        PacketHandler packetHandler,
        Player player,
        PlayerRef playerRef
    ) {
        // Flight timer expired → cooldown
        if (component.isFlyTimeExpired()) {
            component.transitionToCooldown();

            // Force stop flying if the player is in the air
            boolean isActuallyFlying = MovementUtils.isCurrentlyFlying(
                statesComponent
            );
            if (isActuallyFlying) {
                MovementUtils.forceStopFlying(statesComponent, packetHandler);
            }
        }
    }

    /**
     * COOLDOWN state: The skill is on cooldown.
     * Transitions to READY when the cooldown expires.
     *
     * Note: HUD display is managed by SkillBarSystem.
     */
    private void handleCooldownState(
        FlyingSkillComponent component,
        Player player,
        PlayerRef playerRef
    ) {
        if (component.isCooldownExpired()) {
            component.transitionToReady();
        }
    }
}
