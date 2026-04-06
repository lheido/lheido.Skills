package lheido.skills.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.utils.MovementUtils;
import lheido.skills.utils.SkillIds;

/**
 * Event handler to resynchronize the Flying skill state
 * after certain game events (connection, etc.).
 *
 * The problem: When a player sleeps and wakes up, the game resets
 * the MovementSettings (canFly = false). Our FlyingSystem detects this
 * every tick, but there can be a delay.
 *
 * The solution: Listen to the PlayerReadyEvent which is triggered
 * when a player is ready for gameplay. We then immediately force
 * the resynchronization of canFly.
 *
 * Note: This event handler is complementary to the check in
 * FlyingSystem.syncCanFlyState() which detects desyncs every tick.
 */
public class FlyingSkillResyncHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Handler called when a player is ready for gameplay.
     *
     * @param event The PlayerReadyEvent
     */
    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        Ref<EntityStore> entityRef = event.getPlayerRef();
        if (entityRef == null) {
            return;
        }

        // Retrieve the World to access the Store
        World world = player.getWorld();
        if (world == null) {
            return;
        }

        EntityStore entityStore = world.getEntityStore();
        if (entityStore == null) {
            return;
        }

        Store<EntityStore> store = entityStore.getStore();
        if (store == null) {
            return;
        }

        // Check if the player has the FlyingSkillComponent
        FlyingSkillComponent flyingComponent = store.getComponent(
            entityRef,
            FlyingSkillComponent.getComponentType()
        );
        if (flyingComponent == null) {
            return;
        }

        // Check if the Flying skill is active
        ActiveSkillsComponent activeSkills = store.getComponent(
            entityRef,
            ActiveSkillsComponent.getComponentType()
        );

        if (!isSkillActiveForPlayer(activeSkills)) {
            return;
        }

        // Retrieve the PlayerRef for the PacketHandler
        PlayerRef playerRef = store.getComponent(
            entityRef,
            PlayerRef.getComponentType()
        );
        if (playerRef == null) {
            return;
        }

        // Retrieve the MovementManager to update canFly
        MovementManager movementManager = store.getComponent(
            entityRef,
            MovementManager.getComponentType()
        );
        if (movementManager == null) {
            return;
        }

        // Force the resynchronization of canFly
        // We use forceSetCanFly because the client may have been reset
        // even if the server already has the correct value
        boolean shouldCanFly = flyingComponent.shouldCanFly();
        MovementUtils.forceSetCanFly(
            movementManager,
            playerRef.getPacketHandler(),
            shouldCanFly
        );

        LOGGER.atInfo().log(
            "Resynced canFly=%s for player %s after PlayerReadyEvent",
            shouldCanFly,
            playerRef.getUsername()
        );
    }

    /**
     * Checks if the Flying skill is active for the player.
     */
    private static boolean isSkillActiveForPlayer(
        ActiveSkillsComponent activeSkills
    ) {
        if (activeSkills == null) {
            return false;
        }

        for (String activeSkill : activeSkills.getActiveSkills()) {
            if (SkillIds.isFlyingSkill(activeSkill)) {
                return true;
            }
        }
        return false;
    }
}
