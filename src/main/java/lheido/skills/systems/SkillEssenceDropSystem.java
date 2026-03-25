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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nonnull;

/**
 * System that handles dropping Skill Essence when entities (NPCs/creatures) die.
 *
 * This system extends DeathSystems.OnDeathSystem to listen for entity deaths.
 * When an entity dies (excluding players), hostile and neutral mobs can drop Skill Essence.
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
     * Path to the generated NPC tier map bundled in resources.
     */
    private static final String NPC_TIER_MAP_RESOURCE_PATH =
        "/Common/Data/npc-tier-map.json";

    /**
     * Fallback minimum quantity of essence to drop when no tier entry is found.
     */
    private static final int MIN_DROP_QUANTITY = 1;

    /**
     * Fallback health thresholds that each grant +1 additional essence.
     */
    private static final int[] HEALTH_QUANTITY_THRESHOLDS = {
        50,
        100,
        150,
        225,
        325,
        450,
        600,
    };

    /**
     * Fallback flying bonus when no tier entry is found.
     */
    private static final int FLYING_QUANTITY_BONUS = 1;

    private static final TierMapData NPC_TIER_MAP = loadTierMap();

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
        // Build a simplified mob profile used for drop rules
        MobDropProfile profile = getMobDropProfile(ref, store);

        // Check if this mob should drop essence based on tier data, with
        // fallback to the legacy attitude rules when no tier entry is found
        TierMapEntry tierEntry = getTierMapEntryForProfile(profile);
        float dropChance = getDropChanceForEntity(profile, tierEntry);

        float roll = random.nextFloat();
        if (roll >= dropChance) {
            return;
        }

        // Calculate drop quantity using tier base quantity first, then health
        // thresholds and flying bonus
        int quantity = getDropQuantityForEntity(profile, tierEntry);

        // Attempt to drop the Skill Essence
        dropSkillEssence(ref, store, commandBuffer, quantity);
    }

    /**
     * Builds a simplified profile used to determine drop rules.
     *
     * @param ref   The entity reference
     * @param store The entity store
     * @return A profile containing attitude, health, and flying state
     */
    private MobDropProfile getMobDropProfile(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        String playerAttitude = "unknown";
        int maxHealth = 0;
        boolean flying = false;
        String npcId = "unknown";
        String normalizedNpcId = "unknown";

        try {
            NPCEntity npcEntity = store.getComponent(
                ref,
                NPCEntity.getComponentType()
            );
            if (npcEntity == null) {
                return new MobDropProfile(
                    playerAttitude,
                    maxHealth,
                    flying,
                    npcId,
                    normalizedNpcId
                );
            }

            String runtimeNpcTypeId = npcEntity.getNPCTypeId();
            String runtimeRoleName = npcEntity.getRoleName();

            if (runtimeNpcTypeId != null && !runtimeNpcTypeId.isBlank()) {
                npcId = runtimeNpcTypeId.trim();
                normalizedNpcId = normalizeNpcId(runtimeNpcTypeId);
            } else if (runtimeRoleName != null && !runtimeRoleName.isBlank()) {
                npcId = runtimeRoleName.trim();
                normalizedNpcId = normalizeNpcId(runtimeRoleName);
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

                if (
                    "unknown".equalsIgnoreCase(npcId) ||
                    "unknown".equalsIgnoreCase(normalizedNpcId)
                ) {
                    String resolvedNpcId = resolveNpcId(role);
                    if (!resolvedNpcId.isBlank()) {
                        npcId = resolvedNpcId;
                        normalizedNpcId = normalizeNpcId(resolvedNpcId);
                    }
                }
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

        return new MobDropProfile(
            playerAttitude,
            maxHealth,
            flying,
            npcId,
            normalizedNpcId
        );
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
     * Gets the drop chance from the mob attitude.
     *
     * @param profile The mob difficulty profile
     * @return The drop chance (0.0 to 1.0)
     */
    protected float getDropChanceForEntity(
        @Nonnull MobDropProfile profile,
        TierMapEntry tierEntry
    ) {
        if (tierEntry != null) {
            return tierEntry.dropChance();
        }

        if (
            "HOSTILE".equalsIgnoreCase(profile.playerAttitude()) ||
            "NEUTRAL".equalsIgnoreCase(profile.playerAttitude())
        ) {
            return 1.0f;
        }

        return 0.0f;
    }

    /**
     * Gets the drop quantity from tier base quantity plus health thresholds and
     * flying bonus.
     *
     * @param profile  The mob difficulty profile
     * @param tierEntry The resolved tier map entry, if any
     * @return The quantity to drop
     */
    protected int getDropQuantityForEntity(
        @Nonnull MobDropProfile profile,
        TierMapEntry tierEntry
    ) {
        int quantity =
            tierEntry != null ? tierEntry.quantity() : MIN_DROP_QUANTITY;

        if (tierEntry != null) {
            return Math.max(quantity, 0);
        }

        for (int threshold : HEALTH_QUANTITY_THRESHOLDS) {
            if (profile.maxHealth() >= threshold) {
                quantity++;
            }
        }

        if (profile.flying()) {
            quantity += FLYING_QUANTITY_BONUS;
        }

        return Math.max(quantity, 0);
    }

    private TierMapEntry getTierMapEntryForProfile(
        @Nonnull MobDropProfile profile
    ) {
        if (NPC_TIER_MAP.entries().isEmpty()) {
            return null;
        }

        String candidateId = profile.normalizedNpcId();
        while (candidateId != null && !candidateId.isBlank()) {
            TierMapEntry match = NPC_TIER_MAP.entries().get(candidateId);
            if (match != null) {
                return match;
            }

            int lastUnderscoreIndex = candidateId.lastIndexOf('_');
            if (lastUnderscoreIndex < 0) {
                break;
            }

            candidateId = candidateId.substring(0, lastUnderscoreIndex);
        }

        return null;
    }

    private static String resolveNpcId(@Nonnull Role role) {
        try {
            String roleName = role.getRoleName();
            if (roleName != null && !roleName.isBlank()) {
                return roleName.trim();
            }
        } catch (Exception ignored) {}

        try {
            String appearanceName = role.getAppearanceName();
            if (appearanceName != null && !appearanceName.isBlank()) {
                return appearanceName.trim();
            }
        } catch (Exception ignored) {}

        try {
            String balanceAsset = role.getBalanceAsset();
            if (balanceAsset != null && !balanceAsset.isBlank()) {
                int lastSlashIndex = balanceAsset.lastIndexOf('/');
                String candidate =
                    lastSlashIndex >= 0
                        ? balanceAsset.substring(lastSlashIndex + 1)
                        : balanceAsset;
                candidate = candidate.replace(".json", "").trim();
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {}

        try {
            String roleClassName = role.getClass().getSimpleName();
            if (
                roleClassName != null &&
                !roleClassName.isBlank() &&
                !"Role".equals(roleClassName)
            ) {
                return roleClassName.trim();
            }
        } catch (Exception ignored) {}

        return "unknown";
    }

    private static String normalizeNpcId(@Nonnull String npcId) {
        String normalized = npcId.trim().replace('-', '_').replace(' ', '_');

        normalized = normalized.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
        normalized = normalized.replaceAll("[^A-Za-z0-9_]", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_", "");
        normalized = normalized.replaceAll("_$", "");

        return normalized.toLowerCase();
    }

    private static TierMapData loadTierMap() {
        try (
            InputStream inputStream =
                SkillEssenceDropSystem.class.getResourceAsStream(
                    NPC_TIER_MAP_RESOURCE_PATH
                )
        ) {
            if (inputStream == null) {
                LOGGER.atWarning().log(
                    "SkillEssenceDropSystem: Tier map resource not found at " +
                        NPC_TIER_MAP_RESOURCE_PATH
                );
                return new TierMapData(new HashMap<>());
            }

            StringBuilder jsonBuilder = new StringBuilder();
            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                )
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }

            return parseTierMap(jsonBuilder.toString());
        } catch (Exception e) {
            LOGGER.atWarning().log(
                "SkillEssenceDropSystem: Failed to load tier map - " +
                    e.getMessage()
            );
            return new TierMapData(new HashMap<>());
        }
    }

    private static TierMapData parseTierMap(@Nonnull String json) {
        Map<String, TierMapEntry> entries = new HashMap<>();

        try {
            int npcsIndex = json.indexOf("\"npcs\"");
            if (npcsIndex < 0) {
                return new TierMapData(entries);
            }

            int arrayStart = json.indexOf('[', npcsIndex);
            int arrayEnd = json.lastIndexOf(']');
            if (arrayStart < 0 || arrayEnd <= arrayStart) {
                return new TierMapData(entries);
            }

            String npcArray = json.substring(arrayStart + 1, arrayEnd);
            String[] rawEntries = npcArray.split("\\},\\s*\\{");

            for (String rawEntry : rawEntries) {
                String normalizedNpcId = extractJsonStringValue(
                    rawEntry,
                    "normalized_id"
                );
                if (normalizedNpcId == null || normalizedNpcId.isBlank()) {
                    continue;
                }

                int tier = extractJsonIntValue(rawEntry, "tier", 1);
                float dropChance = extractJsonFloatValue(
                    rawEntry,
                    "drop_chance",
                    0.0f
                );
                int baseQuantity = extractJsonIntValue(
                    rawEntry,
                    "base_quantity",
                    MIN_DROP_QUANTITY
                );
                int quantity = extractJsonIntValue(
                    rawEntry,
                    "quantity",
                    baseQuantity
                );

                entries.put(
                    normalizedNpcId,
                    new TierMapEntry(tier, dropChance, baseQuantity, quantity)
                );
            }
        } catch (Exception e) {
            LOGGER.atWarning().log(
                "SkillEssenceDropSystem: Failed to parse tier map - " +
                    e.getMessage()
            );
        }

        return new TierMapData(entries);
    }

    private static String extractJsonStringValue(
        @Nonnull String json,
        @Nonnull String fieldName
    ) {
        String search = "\"" + fieldName + "\"";
        int fieldIndex = json.indexOf(search);
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        if (colonIndex < 0) {
            return null;
        }

        int firstQuote = json.indexOf('"', colonIndex + 1);
        if (firstQuote < 0) {
            return null;
        }

        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return null;
        }

        return json.substring(firstQuote + 1, secondQuote);
    }

    private static int extractJsonIntValue(
        @Nonnull String json,
        @Nonnull String fieldName,
        int defaultValue
    ) {
        try {
            String number = extractJsonNumberValue(json, fieldName);
            return number == null ? defaultValue : Integer.parseInt(number);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static float extractJsonFloatValue(
        @Nonnull String json,
        @Nonnull String fieldName,
        float defaultValue
    ) {
        try {
            String number = extractJsonNumberValue(json, fieldName);
            return number == null ? defaultValue : Float.parseFloat(number);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String extractJsonNumberValue(
        @Nonnull String json,
        @Nonnull String fieldName
    ) {
        String search = "\"" + fieldName + "\"";
        int fieldIndex = json.indexOf(search);
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = colonIndex + 1;
        while (
            valueStart < json.length() &&
            Character.isWhitespace(json.charAt(valueStart))
        ) {
            valueStart++;
        }

        int valueEnd = valueStart;
        while (
            valueEnd < json.length() &&
            (Character.isDigit(json.charAt(valueEnd)) ||
                json.charAt(valueEnd) == '.' ||
                json.charAt(valueEnd) == '-')
        ) {
            valueEnd++;
        }

        if (valueEnd <= valueStart) {
            return null;
        }

        return json.substring(valueStart, valueEnd);
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
        boolean flying,
        @Nonnull String npcId,
        @Nonnull String normalizedNpcId
    ) {}

    private record TierMapEntry(
        int tier,
        float dropChance,
        int baseQuantity,
        int quantity
    ) {}

    private record TierMapData(@Nonnull Map<String, TierMapEntry> entries) {}
}
