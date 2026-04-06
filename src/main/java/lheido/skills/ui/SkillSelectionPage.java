package lheido.skills.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.utils.SkillIds;

/**
 * Interactive page to select 3 active skills from owned skills.
 *
 * The user can:
 * - View their currently active skills (3 slots maximum)
 * - Select a skill from the list to add it to active slots
 * - Remove a skill from an active slot
 *
 * Changes are saved automatically.
 */
public class SkillSelectionPage extends InteractiveCustomUIPage<EventAction> {

    // ============================================
    // Constants
    // ============================================

    private static final int MAX_ACTIVE_SKILLS = 3;

    // ============================================
    // Page state
    // ============================================

    /** Map of owned skills (prefix -> level) */
    private final Map<String, Integer> ownedSkills;

    /** List of owned skill prefixes (for display order) */
    private final List<String> ownedSkillPrefixes;

    /** The 3 currently active skills (prefixes, may contain nulls) */
    private final String[] activeSkills;

    // ============================================
    // Constructor
    // ============================================

    /**
     * Creates a new skill selection page.
     *
     * @param playerRef Reference to the player
     * @param ownedSkills Map of owned skills (prefix -> level)
     * @param currentActiveSkills Prefixes of currently active skills (can be null or incomplete)
     */
    public SkillSelectionPage(
        @Nonnull PlayerRef playerRef,
        @Nonnull Map<String, Integer> ownedSkills,
        String[] currentActiveSkills
    ) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventAction.CODEC);
        this.ownedSkills = new HashMap<>(ownedSkills);
        this.ownedSkillPrefixes = new ArrayList<>(ownedSkills.keySet());
        Collections.sort(this.ownedSkillPrefixes);
        this.activeSkills = new String[MAX_ACTIVE_SKILLS];

        // Copy current active skills (these are prefixes)
        if (currentActiveSkills != null) {
            for (
                int i = 0;
                i < MAX_ACTIVE_SKILLS && i < currentActiveSkills.length;
                i++
            ) {
                this.activeSkills[i] = currentActiveSkills[i];
            }
        }
    }

    // ============================================
    // UI Construction
    // ============================================

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder uiCommandBuilder,
        @Nonnull UIEventBuilder uiEventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        // Load the main UI file
        uiCommandBuilder.append("SkillSelectionPage.ui");

        // Fill active slots with their current state and register remove events
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            updateActiveSlotUI(uiCommandBuilder, i);

            // Register the click event on the slot wrapper to remove the skill
            String slotSelector = "#SlotWrapper" + i + " #Slot" + i;
            uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                slotSelector,
                EventData.of("Data", "remove:" + i),
                false
            );
        }

        // Fill the list of available skills
        populateAvailableSkills(uiCommandBuilder, uiEventBuilder);

        // Show message if no skills available
        if (ownedSkillPrefixes.isEmpty()) {
            uiCommandBuilder.set("#NoSkillsMessage.Visible", true);
            uiCommandBuilder.set("#SkillsList.Visible", false);
        }
    }

    // ============================================
    // Event handling
    // ============================================

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        EventAction eventAction
    ) {
        super.handleDataEvent(ref, store, eventAction);

        String data = eventAction.getData();

        // Parse the format "action:param" or just "action"
        String action = data;
        String param = "";
        int colonIndex = data.indexOf(':');
        if (colonIndex > 0) {
            action = data.substring(0, colonIndex);
            param = data.substring(colonIndex + 1);
        }

        boolean changed = false;

        switch (action) {
            case "select":
                changed = handleSelectSkill(param);
                break;
            case "remove":
                changed = handleRemoveSkill(parseSlotIndex(param));
                break;
            default:
                break;
        }

        // Auto-save if changed
        if (changed) {
            saveActiveSkills(ref, store);
        }

        // Update the UI after each action
        refreshUI();
    }

    private static int parseSlotIndex(String value) {
        if (value == null || value.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ============================================
    // Business logic
    // ============================================

    /**
     * Handles the selection of a skill to add it to active slots.
     * @param prefix The skill prefix (e.g. "Skill_Flying_")
     * @return true if a change was made
     */
    private boolean handleSelectSkill(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return false;
        }

        // Check if the skill is already equipped
        for (String activeSkill : activeSkills) {
            if (prefix.equals(activeSkill)) {
                return false; // Already equipped, do nothing
            }
        }

        // Find the first empty slot
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            if (activeSkills[i] == null || activeSkills[i].isEmpty()) {
                activeSkills[i] = prefix;
                return true;
            }
        }

        // All slots are full - do nothing
        return false;
    }

    /**
     * Removes a skill from an active slot.
     * @return true if a change was made
     */
    private boolean handleRemoveSkill(int slotIndex) {
        if (
            slotIndex >= 0 &&
            slotIndex < MAX_ACTIVE_SKILLS &&
            activeSkills[slotIndex] != null
        ) {
            activeSkills[slotIndex] = null;
            return true;
        }
        return false;
    }

    // ============================================
    // UI Update
    // ============================================

    /**
     * Refreshes the entire UI with the current state.
     */
    private void refreshUI() {
        UICommandBuilder builder = new UICommandBuilder();

        // Update each active slot
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            updateActiveSlotUI(builder, i);
        }

        sendUpdate(builder);
    }

    /**
     * Updates the display of an active slot.
     * activeSkills contains prefixes, we rebuild the full ID for display.
     */
    private void updateActiveSlotUI(UICommandBuilder builder, int slotIndex) {
        String prefix = activeSkills[slotIndex];
        String slotSelector = "#SlotWrapper" + slotIndex + " #Slot" + slotIndex;

        if (prefix != null && !prefix.isEmpty()) {
            // Get the skill level for this prefix
            Integer level = ownedSkills.get(prefix);
            String fullSkillId = SkillIds.getSkillId(
                prefix,
                level != null ? level : 1
            );

            if (fullSkillId != null) {
                // Slot with a skill - display the item
                builder.set(slotSelector + " #Item.Visible", true);
                builder.set(slotSelector + " #Item.ItemId", fullSkillId);
                builder.set(slotSelector + " #Item.Quantity", 1);
            } else {
                builder.set(slotSelector + " #Item.Visible", false);
            }
        } else {
            // Empty slot - hide the ItemSlot
            builder.set(slotSelector + " #Item.Visible", false);
        }
    }

    /**
     * Fills the list of available skills.
     */
    private void populateAvailableSkills(
        UICommandBuilder builder,
        UIEventBuilder eventBuilder
    ) {
        final int SLOTS_PER_ROW = 5;
        int skillCount = ownedSkillPrefixes.size();
        int rowCount = (skillCount + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW; // Round up

        // Create the necessary rows
        for (int row = 0; row < rowCount; row++) {
            builder.append("#SkillsList", "SkillRowTemplate.ui");
        }

        // Fill each row with the skills
        for (int i = 0; i < skillCount; i++) {
            String prefix = ownedSkillPrefixes.get(i);
            Integer level = ownedSkills.get(prefix);

            // Rebuild the full ID for display (e.g. "Skill_Flying_A")
            String fullSkillId = SkillIds.getSkillId(
                prefix,
                level != null ? level : 1
            );
            if (fullSkillId == null) {
                continue;
            }

            int rowIndex = i / SLOTS_PER_ROW;
            int slotInRow = i % SLOTS_PER_ROW;

            // Row selector
            String rowSelector = "#SkillsList[" + rowIndex + "]";

            // Add the skill template to the row
            builder.append(rowSelector, "SkillSlotTemplate.ui");

            // Configure the slot item
            String slotSelector = rowSelector + "[" + slotInRow + "]";
            builder.set(slotSelector + " #Item.ItemId", fullSkillId);
            builder.set(slotSelector + " #Item.Quantity", 1);

            // Register the click event - we send the PREFIX (not the full ID)
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                slotSelector,
                EventData.of("Data", "select:" + prefix),
                false
            );
        }
    }

    // ============================================
    // Saving
    // ============================================

    /**
     * Saves the active skills in the player's component.
     */
    private void saveActiveSkills(
        Ref<EntityStore> ref,
        Store<EntityStore> store
    ) {
        // Get the player reference to access components
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null) {
            return;
        }

        // Save the active skills in the player's component
        ActiveSkillsComponent component = store.getComponent(
            playerEntityRef,
            ActiveSkillsComponent.getComponentType()
        );
        if (component != null) {
            component.setActiveSkills(activeSkills);
        } else {
            // Create the component if it doesn't exist
            ActiveSkillsComponent newComponent = new ActiveSkillsComponent();
            newComponent.setActiveSkills(activeSkills);
            store.addComponent(
                playerEntityRef,
                ActiveSkillsComponent.getComponentType(),
                newComponent
            );
        }
    }

    // ============================================
    // Getters
    // ============================================

    /**
     * Returns a copy of the active skills.
     */
    public String[] getActiveSkills() {
        return activeSkills.clone();
    }
}
