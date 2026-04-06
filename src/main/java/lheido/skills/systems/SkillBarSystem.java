package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.components.FlyingState;
import lheido.skills.hud.SkillBarHud;
import lheido.skills.hud.SkillBarHud.SlotInfo;
import lheido.skills.hud.SkillBarHud.SlotState;
import lheido.skills.utils.SkillIds;

/**
 * ECS system that manages the SkillBar HUD display.
 *
 * This system updates the skillbar to display the player's 3 active skills
 * with their respective states (READY, ACTIVE, COOLDOWN).
 *
 * The skillbar replaces the old FlyingSkillHud and centralizes the display
 * of all active skills.
 */
public class SkillBarSystem extends EntityTickingSystem<EntityStore> {

    private static final ComponentType<EntityStore, PlayerRef> PLAYER_REF_TYPE =
        PlayerRef.getComponentType();

    private static final int MAX_SLOTS = 3;

    public SkillBarSystem() {
        super();
    }

    @Override
    public Query<EntityStore> getQuery() {
        // This system runs for all entities with ActiveSkillsComponent
        return ActiveSkillsComponent.getComponentType();
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

        // Get the player
        Player player = commandBuffer.getComponent(
            entityRef,
            Player.getComponentType()
        );
        if (player == null) {
            return;
        }

        PlayerRef playerRef = chunk.getComponent(entityIndex, PLAYER_REF_TYPE);
        if (playerRef == null) {
            return;
        }

        // Get the HudManager
        HudManager hudManager = player.getHudManager();
        if (hudManager == null) {
            return;
        }

        // Get or create the SkillBarHud
        SkillBarHud skillBarHud = getOrCreateSkillBarHud(hudManager, playerRef);
        if (skillBarHud == null) {
            return;
        }

        // Get the active skills
        ActiveSkillsComponent activeSkills = commandBuffer.getComponent(
            entityRef,
            ActiveSkillsComponent.getComponentType()
        );
        if (activeSkills == null) {
            // No active skills - clear the skillbar
            clearSkillBar(skillBarHud);
            return;
        }

        // Update each slot
        SlotInfo[] slotInfos = new SlotInfo[MAX_SLOTS];
        String[] skills = activeSkills.getActiveSkills();

        for (int i = 0; i < MAX_SLOTS; i++) {
            String skillPrefix = (i < skills.length) ? skills[i] : null;
            slotInfos[i] = buildSlotInfo(skillPrefix, entityRef, commandBuffer);
        }

        skillBarHud.updateAllSlots(slotInfos);
    }

    /**
     * Gets the existing SkillBarHud or creates a new one.
     */
    private SkillBarHud getOrCreateSkillBarHud(
        HudManager hudManager,
        PlayerRef playerRef
    ) {
        CustomUIHud currentHud = hudManager.getCustomHud();

        if (currentHud instanceof SkillBarHud existingHud) {
            return existingHud;
        }

        // Create a new HUD
        SkillBarHud newHud = new SkillBarHud(playerRef);
        hudManager.setCustomHud(playerRef, newHud);
        newHud.show();
        return newHud;
    }

    /**
     * Clears the skillbar (all slots empty).
     */
    private void clearSkillBar(SkillBarHud skillBarHud) {
        SlotInfo[] emptySlots = new SlotInfo[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            emptySlots[i] = SlotInfo.empty();
        }
        skillBarHud.updateAllSlots(emptySlots);
    }

    /**
     * Builds slot information based on the skill and its state.
     *
     * @param skillPrefix The skill prefix (e.g. "Skill_Flying_") or null if slot is empty
     * @param entityRef Reference to the player entity
     * @param commandBuffer Buffer to access components
     * @return The slot information
     */
    private SlotInfo buildSlotInfo(
        String skillPrefix,
        Ref<EntityStore> entityRef,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        if (skillPrefix == null || skillPrefix.isEmpty()) {
            return SlotInfo.empty();
        }

        // Determine the skill type and get its state
        if (SkillIds.isFlyingSkill(skillPrefix)) {
            return buildFlyingSlotInfo(skillPrefix, entityRef, commandBuffer);
        }

        // For passive skills (WaterBreathing, Stamina, etc.)
        // They don't have ACTIVE/COOLDOWN state, so always READY
        return buildPassiveSlotInfo(skillPrefix, entityRef, commandBuffer);
    }

    /**
     * Builds slot information for the Flying skill.
     */
    private SlotInfo buildFlyingSlotInfo(
        String skillPrefix,
        Ref<EntityStore> entityRef,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        FlyingSkillComponent flyingComp = commandBuffer.getComponent(
            entityRef,
            FlyingSkillComponent.getComponentType()
        );

        if (flyingComp == null) {
            // Skill equipped but component not found - display as READY
            String skillId = SkillIds.getSkillId(skillPrefix, 1);
            return SlotInfo.ready(skillId);
        }

        // Build the full skill ID with the current level
        String skillId = SkillIds.getFlyingSkillId(flyingComp.getLevel());
        if (skillId == null) {
            skillId = SkillIds.getSkillId(skillPrefix, 1);
        }

        FlyingState state = flyingComp.getState();

        return switch (state) {
            case READY -> SlotInfo.ready(skillId);
            case FLYING -> {
                if (flyingComp.isUnlimitedFlight()) {
                    // Unlimited flight - no timer
                    yield SlotInfo.ready(skillId);
                }
                int remainingSeconds = (int) Math.ceil(
                    flyingComp.getRemainingFlyTimeMs() / 1000.0
                );
                yield SlotInfo.active(skillId, remainingSeconds);
            }
            case COOLDOWN -> {
                int remainingSeconds = (int) Math.ceil(
                    flyingComp.getRemainingCooldownMs() / 1000.0
                );
                yield SlotInfo.cooldown(skillId, remainingSeconds);
            }
        };
    }

    /**
     * Builds slot information for a passive skill.
     * Passive skills don't have active/cooldown state.
     */
    private SlotInfo buildPassiveSlotInfo(
        String skillPrefix,
        Ref<EntityStore> entityRef,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        // Get the passive skill level
        int level = getPassiveSkillLevel(skillPrefix, entityRef, commandBuffer);
        String skillId = SkillIds.getSkillId(skillPrefix, level);

        if (skillId == null) {
            skillId = skillPrefix + "A"; // Fallback
        }

        return SlotInfo.ready(skillId);
    }

    /**
     * Gets the level of a passive skill.
     */
    private int getPassiveSkillLevel(
        String skillPrefix,
        Ref<EntityStore> entityRef,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        // Check each passive skill type
        if (SkillIds.isWaterBreathingSkill(skillPrefix)) {
            var comp = commandBuffer.getComponent(
                entityRef,
                lheido.skills.components.WaterBreathingSkillComponent.getComponentType()
            );
            return (comp != null) ? comp.getLevel() : 1;
        }

        if (SkillIds.isStaminaSkill(skillPrefix)) {
            var comp = commandBuffer.getComponent(
                entityRef,
                lheido.skills.components.StaminaSkillComponent.getComponentType()
            );
            return (comp != null) ? comp.getLevel() : 1;
        }

        if (SkillIds.isPoisonResistanceSkill(skillPrefix)) {
            var comp = commandBuffer.getComponent(
                entityRef,
                lheido.skills.components.PoisonResistanceSkillComponent.getComponentType()
            );
            return (comp != null) ? comp.getLevel() : 1;
        }

        if (SkillIds.isFireResistanceSkill(skillPrefix)) {
            var comp = commandBuffer.getComponent(
                entityRef,
                lheido.skills.components.FireResistanceSkillComponent.getComponentType()
            );
            return (comp != null) ? comp.getLevel() : 1;
        }

        if (SkillIds.isLifeStealSkill(skillPrefix)) {
            var comp = commandBuffer.getComponent(
                entityRef,
                lheido.skills.components.LifeStealSkillComponent.getComponentType()
            );
            return (comp != null) ? comp.getLevel() : 1;
        }

        return 1; // Default
    }
}
