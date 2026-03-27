package lheido.skills.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import java.util.Random;
import javax.annotation.Nonnull;

/**
 * System that handles dropping Skill Essence when entities (NPCs/creatures) die.
 *
 * This system extends DeathSystems.OnDeathSystem to listen for entity deaths.
 * When an entity dies (excluding players), hostile and neutral mobs can drop Skill Essence.
 * The drop quantity scales with the entity's maxHealth and whether it can fly.
 *
 * Usage:
 * - Registered in LheidoSkillsPlugin.setup()
 * - Automatically triggered when entities matching the query die
 */
public class SkillEssenceDropSystem extends DeathSystems.OnDeathSystem {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * The item ID for Skill Essence ingredient.
     * This should match the JSON file name: Ingredient_Skill_Essence.json
     */
    private static final String SKILL_ESSENCE_ITEM_ID =
        "Ingredient_Skill_Essence";

    /**
     * Base minimum quantity of essence to drop.
     */
    private static final int MIN_DROP_QUANTITY = 1;

    /**
     * Health thresholds that each grant +1 additional essence.
     * If an entity's maxHealth >= threshold, quantity increases by 1 for that threshold.
     */
    private static final int[] HEALTH_QUANTITY_THRESHOLDS = {
        25, // weak+         → 2
        50, // medium        → 3
        100, // strong       → 4
        150, // strong+      → 5
        // --- elite gap ---
        200, //              → 6
        215, //              → 7
        230, //              → 8
        245, //              → 9
        260, //              → 10
        275, //              → 11
        290, //              → 12
        305, //              → 13
        320, //              → 14
        335, //              → 15
        350, //              → 16
        365, //              → 17
        380, //              → 18
        395, //              → 19
        400, //              → 20
    };

    /**
     * Extra quantity bonus for flying/gliding entities.
     */
    private static final int FLYING_QUANTITY_BONUS = 1;

    private final Random random = new Random();

    /**
     * Defines which entities this system applies to.
     * We target all entities that can die, but exclude Players.
     */
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.not(Player.getComponentType());
    }

    /**
     * Called when a DeathComponent is added to an entity matching our query.
     * This means a non-player entity has died.
     */
    @Override
    public void onComponentAdded(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull DeathComponent component,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        MobDropProfile profile = getMobDropProfile(ref, store);

        float dropChance = getDropChanceForEntity(profile);

        float roll = random.nextFloat();
        if (roll >= dropChance) {
            return;
        }

        int quantity = getDropQuantityForEntity(profile);

        dropSkillEssence(ref, store, commandBuffer, quantity);
    }

    /**
     * Builds a simplified profile used to determine drop rules.
     *
     * @param ref   The entity reference
     * @param store The entity store
     * @return A profile containing attitude, maxHealth, and flying state
     */
    private MobDropProfile getMobDropProfile(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        String playerAttitude = "unknown";
        int maxHealth = 0;
        boolean flying = false;

        try {
            NPCEntity npcEntity = store.getComponent(
                ref,
                NPCEntity.getComponentType()
            );
            if (npcEntity == null) {
                return new MobDropProfile(playerAttitude, maxHealth, flying);
            }

            Role role = npcEntity.getRole();
            if (role != null) {
                var worldSupport = role.getWorldSupport();
                if (worldSupport != null) {
                    var defaultPlayerAttitude =
                        worldSupport.getDefaultPlayerAttitude();
                    if (defaultPlayerAttitude != null) {
                        playerAttitude = defaultPlayerAttitude.name();
                    }
                }

                maxHealth = role.getInitialMaxHealth();
            }

            MovementStatesComponent movementStatesComponent =
                store.getComponent(
                    ref,
                    MovementStatesComponent.getComponentType()
                );
            if (movementStatesComponent != null) {
                var movementStates =
                    movementStatesComponent.getMovementStates();
                if (movementStates != null) {
                    flying = movementStates.flying || movementStates.gliding;
                }
            }

            if (!flying) {
                flying = npcEntity.getHoverHeight() > 0;
            }
        } catch (Exception e) {
            LOGGER.atInfo().log(
                "SkillEssenceDropSystem: Could not build mob drop profile - " +
                    e.getMessage()
            );
        }

        return new MobDropProfile(playerAttitude, maxHealth, flying);
    }

    /**
     * Drops Skill Essence at the entity's death location.
     *
     * @param ref           The entity reference
     * @param store         The entity store
     * @param commandBuffer The command buffer
     * @param quantity      The quantity to drop
     * @return true if the drop was successful, false otherwise
     */
    private boolean dropSkillEssence(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        int quantity
    ) {
        try {
            World world = store.getExternalData().getWorld();
            if (world == null) {
                LOGGER.atWarning().log(
                    "SkillEssenceDropSystem: World is null, cannot drop item"
                );
                return false;
            }

            var transformType = EntityModule.get().getTransformComponentType();
            var transform = store.getComponent(ref, transformType);

            if (transform == null) {
                LOGGER.atWarning().log(
                    "SkillEssenceDropSystem: Entity has no TransformComponent"
                );
                return false;
            }

            var position = transform.getPosition();

            ItemStack itemStack = new ItemStack(
                getSkillEssenceItemId(),
                quantity
            );

            world.execute(() -> {
                try {
                    ItemUtils.dropItem(ref, itemStack, commandBuffer);

                    LOGGER.atInfo().log(
                        "SkillEssenceDropSystem: Successfully dropped item at " +
                            position
                    );
                } catch (Exception innerEx) {
                    LOGGER.atWarning().log(
                        "SkillEssenceDropSystem: Failed to drop item - " +
                            innerEx.getMessage()
                    );
                }
            });

            return true;
        } catch (Exception e) {
            LOGGER.atWarning().log(
                "SkillEssenceDropSystem: Error dropping Skill Essence - " +
                    e.getMessage()
            );
            return false;
        }
    }

    /**
     * Gets the drop chance based on the mob's attitude towards the player.
     * Only hostile and neutral mobs drop Skill Essence.
     *
     * @param profile The mob drop profile
     * @return The drop chance (0.0 to 1.0)
     */
    protected float getDropChanceForEntity(@Nonnull MobDropProfile profile) {
        if (
            "HOSTILE".equalsIgnoreCase(profile.playerAttitude()) ||
            "NEUTRAL".equalsIgnoreCase(profile.playerAttitude())
        ) {
            return 1.0f;
        }

        return 0.0f;
    }

    /**
     * Gets the drop quantity based on the entity's maxHealth and flying state.
     * Starts at MIN_DROP_QUANTITY, then adds +1 for each health threshold met,
     * plus a flying bonus if applicable.
     *
     * @param profile The mob drop profile
     * @return The quantity to drop
     */
    protected int getDropQuantityForEntity(@Nonnull MobDropProfile profile) {
        int quantity = MIN_DROP_QUANTITY;

        for (int threshold : HEALTH_QUANTITY_THRESHOLDS) {
            if (profile.maxHealth() >= threshold) {
                quantity++;
            }
        }

        if (profile.flying()) {
            quantity += FLYING_QUANTITY_BONUS;
        }

        return Math.max(quantity, MIN_DROP_QUANTITY);
    }

    /**
     * Gets the item ID for Skill Essence.
     * Can be overridden in subclasses if needed.
     *
     * @return The item ID string
     */
    protected String getSkillEssenceItemId() {
        return SKILL_ESSENCE_ITEM_ID;
    }

    private record MobDropProfile(
        @Nonnull String playerAttitude,
        int maxHealth,
        boolean flying
    ) {}
}
