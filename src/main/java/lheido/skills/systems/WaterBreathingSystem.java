package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.WaterBreathingSkillComponent;
import lheido.skills.utils.SkillIds;

/**
 * ECS system that manages the WaterBreathing skill logic.
 *
 * This system adds a multiplicative modifier on the player's Oxygen stat
 * based on the skill level:
 * - Level A (1): +50% oxygen (multiplier 1.5x)
 * - Level B (2): +100% oxygen (multiplier 2.0x)
 * - Level C (3): +200% oxygen (multiplier 3.0x)
 * - Level X (4): Very high max oxygen (simulates unlimited)
 *
 * The modifier is applied once during initialization
 * and updated if the level changes.
 */
public class WaterBreathingSystem extends EntityTickingSystem<EntityStore> {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Unique identifier for this skill's modifier.
     * Allows finding and updating/removing it.
     */
    private static final String MODIFIER_ID = "lheido_water_breathing";

    /**
     * Very high multiplicative value to simulate unlimited oxygen (level X).
     */
    private static final float UNLIMITED_MULTIPLIER = 1000.0f;

    public WaterBreathingSystem() {
        super();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return WaterBreathingSkillComponent.getComponentType();
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

        WaterBreathingSkillComponent waterBreathingComponent =
            commandBuffer.getComponent(
                entityRef,
                WaterBreathingSkillComponent.getComponentType()
            );
        if (waterBreathingComponent == null) {
            return;
        }

        Player player = commandBuffer.getComponent(
            entityRef,
            Player.getComponentType()
        );
        if (player == null) {
            return;
        }

        // Get the ComponentType for EntityStatMap from the module
        ComponentType<EntityStore, EntityStatMap> statMapType =
            EntityStatsModule.get().getEntityStatMapComponentType();

        EntityStatMap statMap = commandBuffer.getComponent(
            entityRef,
            statMapType
        );
        if (statMap == null) {
            LOGGER.atWarning().log(
                "WaterBreathingSystem: EntityStatMap is null for player"
            );
            return;
        }

        // Get the Oxygen stat index
        int oxygenStatIndex = DefaultEntityStatTypes.getOxygen();
        if (oxygenStatIndex < 0) {
            LOGGER.atWarning().log(
                "WaterBreathingSystem: Oxygen stat index not found"
            );
            return;
        }

        // Check if the skill is active in ActiveSkillsComponent
        ActiveSkillsComponent activeSkills = commandBuffer.getComponent(
            entityRef,
            ActiveSkillsComponent.getComponentType()
        );
        boolean isSkillActive = isSkillActiveForPlayer(activeSkills);

        if (!isSkillActive) {
            // Skill not active: remove the modifier
            removeOxygenModifier(statMap, oxygenStatIndex);
            return;
        }

        // Calculate the effective multiplier
        float effectiveMultiplier = waterBreathingComponent.isUnlimitedOxygen()
            ? UNLIMITED_MULTIPLIER
            : waterBreathingComponent.getOxygenMultiplier();

        // Check if the modifier already exists and has the correct value
        // We use putModifier which replaces the existing modifier if it exists
        applyOxygenModifier(statMap, oxygenStatIndex, effectiveMultiplier);
    }

    /**
     * Checks if the WaterBreathing skill is active for the player.
     *
     * A skill is considered active if one of the active skills is a
     * WaterBreathing skill (starts with "Skill_WaterBreathing_")
     */
    private boolean isSkillActiveForPlayer(ActiveSkillsComponent activeSkills) {
        if (activeSkills == null) {
            return false;
        }

        for (String activeSkill : activeSkills.getActiveSkills()) {
            if (SkillIds.isWaterBreathingSkill(activeSkill)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the oxygen modifier from the player.
     * Called when the skill is not active.
     */
    private void removeOxygenModifier(
        EntityStatMap statMap,
        int oxygenStatIndex
    ) {
        statMap.removeModifier(
            EntityStatMap.Predictable.NONE,
            oxygenStatIndex,
            MODIFIER_ID
        );
    }

    /**
     * Applies or updates the oxygen modifier on the player.
     *
     * @param statMap The player's EntityStatMap
     * @param oxygenStatIndex The Oxygen stat index
     * @param multiplier The multiplier to apply
     */
    private void applyOxygenModifier(
        EntityStatMap statMap,
        int oxygenStatIndex,
        float multiplier
    ) {
        // Create a multiplicative modifier on max oxygen
        // StaticModifier with CalculationType.MULTIPLICATIVE multiplies the max value
        StaticModifier oxygenModifier = new StaticModifier(
            Modifier.ModifierTarget.MAX,
            StaticModifier.CalculationType.MULTIPLICATIVE,
            multiplier
        );

        // putModifier automatically replaces if a modifier with the same ID exists
        statMap.putModifier(
            EntityStatMap.Predictable.NONE,
            oxygenStatIndex,
            MODIFIER_ID,
            oxygenModifier
        );
        // If previousModifier != null, the modifier already existed and was replaced
    }
}
