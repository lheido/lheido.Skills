package lheido.skills.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Random;
import javax.annotation.Nonnull;

/**
 * System that handles dropping Skill Essence when entities (NPCs/creatures) die.
 *
 * This system extends DeathSystems.OnDeathSystem to listen for entity deaths.
 * When an entity dies (excluding players), there's a chance to drop a Skill Essence item.
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
     * Base drop chance (0.0 to 1.0).
     * 0.25 = 25% chance to drop.
     */
    private static final float BASE_DROP_CHANCE = 1f;

    /**
     * Minimum quantity of essence to drop.
     */
    private static final int MIN_DROP_QUANTITY = 1;

    /**
     * Maximum quantity of essence to drop.
     */
    private static final int MAX_DROP_QUANTITY = 3;

    private final Random random = new Random();

    /**
     * Defines which entities this system applies to.
     * We target all entities that can die, but exclude Players.
     */
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // Query for entities that are NOT Players
        // This targets monsters, animals, and other NPCs that can die
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
        // Try to get the entity's identifier for customized drops
        String entityId = getEntityId(ref, store);

        // Check if we should drop essence based on random chance (using entity-specific rates)
        float dropChance = getDropChanceForEntity(entityId);
        if (random.nextFloat() >= dropChance) {
            LOGGER.atFine().log(
                "SkillEssenceDropSystem: No drop for entity " +
                    entityId +
                    " (random chance failed)"
            );
            return;
        }

        // Calculate drop quantity (using entity-specific amounts)
        int quantity = getDropQuantityForEntity(entityId);

        // Attempt to drop the Skill Essence
        boolean success = dropSkillEssence(ref, store, commandBuffer, quantity);

        if (success) {
            LOGGER.atInfo().log(
                "SkillEssenceDropSystem: Dropped " +
                    quantity +
                    " Skill Essence from " +
                    entityId
            );
        }
    }

    /**
     * Gets the entity's identifier/type name from its model.
     *
     * @param ref   The entity reference
     * @param store The entity store
     * @return The entity ID string, or "unknown" if not found
     */
    private String getEntityId(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        try {
            // Try to get the PersistentModel component to identify the entity type
            PersistentModel persistentModel = store.getComponent(
                ref,
                PersistentModel.getComponentType()
            );
            if (persistentModel != null) {
                var modelRef = persistentModel.getModelReference();
                if (modelRef != null) {
                    String modelId = modelRef.toString();
                    if (modelId != null && !modelId.isEmpty()) {
                        return modelId;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.atFine().log(
                "SkillEssenceDropSystem: Could not get entity ID - " +
                    e.getMessage()
            );
        }
        return "unknown";
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
            // Get the world from the store's external data
            World world = store.getExternalData().getWorld();
            if (world == null) {
                LOGGER.atWarning().log(
                    "SkillEssenceDropSystem: World is null, cannot drop item"
                );
                return false;
            }

            // Get entity's transform to find position
            var transformType = EntityModule.get().getTransformComponentType();
            var transform = store.getComponent(ref, transformType);

            if (transform == null) {
                LOGGER.atWarning().log(
                    "SkillEssenceDropSystem: Entity has no TransformComponent"
                );
                return false;
            }

            // Get the entity's position
            var position = transform.getPosition();

            // Create the item stack
            ItemStack itemStack = new ItemStack(
                getSkillEssenceItemId(),
                quantity
            );

            // Use ItemUtils to drop the item at the entity's position
            // This is the proper Hytale API for dropping items
            world.execute(() -> {
                try {
                    // Use ItemUtils.dropItem to spawn a dropped item entity
                    ItemUtils.dropItem(ref, itemStack, commandBuffer);

                    LOGGER.atFine().log(
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
     * Calculates the quantity of essence to drop.
     * Returns a random value between MIN_DROP_QUANTITY and MAX_DROP_QUANTITY (inclusive).
     *
     * @return the quantity to drop
     */
    private int calculateDropQuantity() {
        if (MIN_DROP_QUANTITY >= MAX_DROP_QUANTITY) {
            return MIN_DROP_QUANTITY;
        }
        return (
            MIN_DROP_QUANTITY +
            random.nextInt(MAX_DROP_QUANTITY - MIN_DROP_QUANTITY + 1)
        );
    }

    /**
     * Gets the drop chance for a specific entity type.
     * Customizes drop rates for different creatures.
     *
     * @param entityId The entity's identifier
     * @return The drop chance (0.0 to 1.0)
     */
    protected float getDropChanceForEntity(String entityId) {
        if (entityId == null || entityId.equals("unknown")) {
            return BASE_DROP_CHANCE;
        }

        String lowerEntityId = entityId.toLowerCase();

        // Bosses have guaranteed drop
        if (lowerEntityId.contains("boss")) {
            return 1.0f;
        }

        // Hostile creatures have higher chance
        if (
            lowerEntityId.contains("trork") ||
            lowerEntityId.contains("scarak") ||
            lowerEntityId.contains("hostile")
        ) {
            return 0.35f;
        }

        // Passive animals have lower chance
        if (
            lowerEntityId.contains("ram") ||
            lowerEntityId.contains("chicken") ||
            lowerEntityId.contains("cow") ||
            lowerEntityId.contains("sheep")
        ) {
            return 0.10f;
        }

        return BASE_DROP_CHANCE;
    }

    /**
     * Gets the drop quantity for a specific entity type.
     * Customizes drop amounts for different creatures.
     *
     * @param entityId The entity's identifier
     * @return The quantity to drop
     */
    protected int getDropQuantityForEntity(String entityId) {
        if (entityId == null || entityId.equals("unknown")) {
            return calculateDropQuantity();
        }

        String lowerEntityId = entityId.toLowerCase();

        // Bosses drop significantly more
        if (lowerEntityId.contains("boss")) {
            return 5 + random.nextInt(6); // 5-10
        }

        // Mini-bosses/elites drop moderately more
        if (
            lowerEntityId.contains("miniboss") ||
            lowerEntityId.contains("elite")
        ) {
            return 3 + random.nextInt(4); // 3-6
        }

        return calculateDropQuantity();
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
}
