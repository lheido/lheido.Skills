package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Component to store a player's 3 active skills.
 *
 * This component is attached to the Player and persists the IDs of the skills
 * that the player has chosen as active. A player can have a maximum
 * of 3 active skills simultaneously.
 */
public class ActiveSkillsComponent implements Component<EntityStore> {

    // ============================================
    // Constants
    // ============================================

    public static final int MAX_ACTIVE_SKILLS = 3;

    // ============================================
    // ComponentType
    // ============================================

    private static volatile ComponentType<
        EntityStore,
        ActiveSkillsComponent
    > COMPONENT_TYPE;

    /**
     * Codec for serialization/deserialization of the component.
     * Skills are stored as a list of strings.
     */
    public static final BuilderCodec<ActiveSkillsComponent> CODEC =
        BuilderCodec.builder(
            ActiveSkillsComponent.class,
            ActiveSkillsComponent::new
        )
            .append(
                new KeyedCodec<>("Skill0", Codec.STRING),
                (data, value) -> data.activeSkills[0] = value,
                data -> data.activeSkills[0] != null ? data.activeSkills[0] : ""
            )
            .add()
            .append(
                new KeyedCodec<>("Skill1", Codec.STRING),
                (data, value) -> data.activeSkills[1] = value,
                data -> data.activeSkills[1] != null ? data.activeSkills[1] : ""
            )
            .add()
            .append(
                new KeyedCodec<>("Skill2", Codec.STRING),
                (data, value) -> data.activeSkills[2] = value,
                data -> data.activeSkills[2] != null ? data.activeSkills[2] : ""
            )
            .add()
            .build();

    // ============================================
    // Data
    // ============================================

    /** The 3 active skill slots (may contain nulls or empty strings) */
    private final String[] activeSkills;

    // ============================================
    // Constructor
    // ============================================

    public ActiveSkillsComponent() {
        this.activeSkills = new String[MAX_ACTIVE_SKILLS];
    }

    // ============================================
    // ComponentType Management
    // ============================================

    public static void setComponentType(
        ComponentType<EntityStore, ActiveSkillsComponent> componentType
    ) {
        COMPONENT_TYPE = componentType;
    }

    public static ComponentType<
        EntityStore,
        ActiveSkillsComponent
    > getComponentType() {
        return COMPONENT_TYPE;
    }

    // ============================================
    // Getters and Setters
    // ============================================

    /**
     * Returns a copy of the active skills.
     */
    public String[] getActiveSkills() {
        return activeSkills.clone();
    }

    /**
     * Sets the active skills.
     *
     * @param skills Array of skills (will be copied, can be null)
     */
    public void setActiveSkills(String[] skills) {
        // Reset all slots
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            this.activeSkills[i] = null;
        }

        // Copy the provided skills
        if (skills != null) {
            for (int i = 0; i < MAX_ACTIVE_SKILLS && i < skills.length; i++) {
                this.activeSkills[i] = skills[i];
            }
        }
    }

    /**
     * Returns the active skill at the given index.
     *
     * @param index Slot index (0-2)
     * @return The skill ID or null if the slot is empty
     */
    public String getActiveSkill(int index) {
        if (index >= 0 && index < MAX_ACTIVE_SKILLS) {
            return activeSkills[index];
        }
        return null;
    }

    /**
     * Sets the active skill at the given index.
     *
     * @param index Slot index (0-2)
     * @param skillId Skill ID (can be null to clear the slot)
     */
    public void setActiveSkill(int index, String skillId) {
        if (index >= 0 && index < MAX_ACTIVE_SKILLS) {
            activeSkills[index] = skillId;
        }
    }

    /**
     * Checks if a skill is currently active.
     * Supports full IDs AND prefixes.
     *
     * @param skillIdOrPrefix The skill ID or the prefix to check
     * @return true if the skill is in one of the active slots
     */
    public boolean isSkillActive(String skillIdOrPrefix) {
        if (skillIdOrPrefix == null || skillIdOrPrefix.isEmpty()) {
            return false;
        }
        for (String active : activeSkills) {
            if (active == null || active.isEmpty()) {
                continue;
            }
            // Exact comparison (for stored prefixes)
            if (skillIdOrPrefix.equals(active)) {
                return true;
            }
            // Prefix comparison (for backward compatibility with old full IDs)
            if (
                active.startsWith(skillIdOrPrefix) ||
                skillIdOrPrefix.startsWith(active)
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a skill with the given prefix is active.
     *
     * @param prefix The skill prefix (e.g. "Skill_Flying_")
     * @return true if a skill with this prefix is active
     */
    public boolean isSkillPrefixActive(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return false;
        }
        for (String active : activeSkills) {
            if (active != null && active.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the list of non-null active skills.
     */
    public List<String> getActiveSkillsList() {
        List<String> result = new ArrayList<>();
        for (String skill : activeSkills) {
            if (skill != null && !skill.isEmpty()) {
                result.add(skill);
            }
        }
        return result;
    }

    /**
     * Returns the number of occupied slots.
     */
    public int getActiveSkillCount() {
        int count = 0;
        for (String skill : activeSkills) {
            if (skill != null && !skill.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if there are available slots remaining.
     */
    public boolean hasAvailableSlot() {
        return getActiveSkillCount() < MAX_ACTIVE_SKILLS;
    }

    /**
     * Adds a skill to the first available slot.
     *
     * @param skillId The skill ID to add
     * @return true if the skill was added, false if all slots are full or the skill is already active
     */
    public boolean addSkill(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return false;
        }

        // Check if already active
        if (isSkillActive(skillId)) {
            return false;
        }

        // Find an empty slot
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            if (activeSkills[i] == null || activeSkills[i].isEmpty()) {
                activeSkills[i] = skillId;
                return true;
            }
        }

        return false; // All slots are full
    }

    /**
     * Removes a skill from the active slots.
     *
     * @param skillId The skill ID to remove
     * @return true if the skill was removed
     */
    public boolean removeSkill(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return false;
        }

        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            if (skillId.equals(activeSkills[i])) {
                activeSkills[i] = null;
                return true;
            }
        }

        return false;
    }

    // ============================================
    // Clone
    // ============================================

    @Override
    public ActiveSkillsComponent clone() {
        ActiveSkillsComponent copy = new ActiveSkillsComponent();
        System.arraycopy(
            this.activeSkills,
            0,
            copy.activeSkills,
            0,
            MAX_ACTIVE_SKILLS
        );
        return copy;
    }
}
