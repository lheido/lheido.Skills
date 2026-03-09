package lheido.skills.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HUD pour afficher les 3 skills actifs du joueur.
 * 
 * Chaque slot peut afficher:
 * - L'icone du skill (via ItemSlot)
 * - Un overlay de cooldown avec timer
 * - Un overlay d'activation avec timer (skill en cours d'utilisation)
 * - Un tiret "-" si le slot est vide
 */
public class SkillBarHud extends CustomUIHud {

    /**
     * Etat d'un slot de skill.
     */
    public enum SlotState {
        /** Slot vide, pas de skill equipe */
        EMPTY,
        /** Skill equipe et pret a l'utilisation */
        READY,
        /** Skill en cours d'utilisation (avec timer) */
        ACTIVE,
        /** Skill en cooldown (avec timer) */
        COOLDOWN
    }

    /**
     * Donnees d'un slot de skill pour le tracking des changements.
     */
    private static class SlotData {
        SlotState state = SlotState.EMPTY;
        String skillId = null;
        int timerSeconds = 0;

        boolean hasChanged(SlotState newState, String newSkillId, int newTimer) {
            return state != newState 
                || !java.util.Objects.equals(skillId, newSkillId)
                || timerSeconds != newTimer;
        }

        void update(SlotState newState, String newSkillId, int newTimer) {
            this.state = newState;
            this.skillId = newSkillId;
            this.timerSeconds = newTimer;
        }
    }

    private static final int MAX_SLOTS = 3;
    private final SlotData[] slots;

    public SkillBarHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
        this.slots = new SlotData[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            this.slots[i] = new SlotData();
        }
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("SkillBarHud.ui");
    }

    /**
     * Met a jour un slot avec un skill pret (pas de timer affiche).
     * 
     * @param slotIndex Index du slot (0-2)
     * @param skillId ID complet du skill (ex: "Skill_Flying_A")
     */
    public void setSlotReady(int slotIndex, @Nonnull String skillId) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return;
        }

        SlotData slot = slots[slotIndex];
        if (!slot.hasChanged(SlotState.READY, skillId, 0)) {
            return;
        }
        slot.update(SlotState.READY, skillId, 0);

        UICommandBuilder builder = new UICommandBuilder();
        updateSlotUI(builder, slotIndex, SlotState.READY, skillId, 0);
        update(false, builder);
    }

    /**
     * Met a jour un slot avec un skill actif (en cours d'utilisation).
     * 
     * @param slotIndex Index du slot (0-2)
     * @param skillId ID complet du skill
     * @param remainingSeconds Temps restant en secondes
     */
    public void setSlotActive(int slotIndex, @Nonnull String skillId, int remainingSeconds) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return;
        }

        SlotData slot = slots[slotIndex];
        if (!slot.hasChanged(SlotState.ACTIVE, skillId, remainingSeconds)) {
            return;
        }
        slot.update(SlotState.ACTIVE, skillId, remainingSeconds);

        UICommandBuilder builder = new UICommandBuilder();
        updateSlotUI(builder, slotIndex, SlotState.ACTIVE, skillId, remainingSeconds);
        update(false, builder);
    }

    /**
     * Met a jour un slot avec un skill en cooldown.
     * 
     * @param slotIndex Index du slot (0-2)
     * @param skillId ID complet du skill
     * @param remainingSeconds Temps de cooldown restant en secondes
     */
    public void setSlotCooldown(int slotIndex, @Nonnull String skillId, int remainingSeconds) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return;
        }

        SlotData slot = slots[slotIndex];
        if (!slot.hasChanged(SlotState.COOLDOWN, skillId, remainingSeconds)) {
            return;
        }
        slot.update(SlotState.COOLDOWN, skillId, remainingSeconds);

        UICommandBuilder builder = new UICommandBuilder();
        updateSlotUI(builder, slotIndex, SlotState.COOLDOWN, skillId, remainingSeconds);
        update(false, builder);
    }

    /**
     * Vide un slot (plus de skill equipe).
     * 
     * @param slotIndex Index du slot (0-2)
     */
    public void setSlotEmpty(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return;
        }

        SlotData slot = slots[slotIndex];
        if (!slot.hasChanged(SlotState.EMPTY, null, 0)) {
            return;
        }
        slot.update(SlotState.EMPTY, null, 0);

        UICommandBuilder builder = new UICommandBuilder();
        updateSlotUI(builder, slotIndex, SlotState.EMPTY, null, 0);
        update(false, builder);
    }

    /**
     * Met a jour tous les slots en une seule commande (optimisation).
     * 
     * @param skillStates Tableau de 3 etats de slots (peut contenir des null pour les slots vides)
     */
    public void updateAllSlots(@Nonnull SlotInfo[] skillStates) {
        UICommandBuilder builder = new UICommandBuilder();
        boolean hasChanges = false;

        for (int i = 0; i < MAX_SLOTS; i++) {
            SlotInfo info = (i < skillStates.length) ? skillStates[i] : null;
            
            SlotState state = (info != null) ? info.state : SlotState.EMPTY;
            String skillId = (info != null) ? info.skillId : null;
            int timer = (info != null) ? info.timerSeconds : 0;

            SlotData slot = slots[i];
            if (slot.hasChanged(state, skillId, timer)) {
                slot.update(state, skillId, timer);
                updateSlotUI(builder, i, state, skillId, timer);
                hasChanges = true;
            }
        }

        if (hasChanges) {
            update(false, builder);
        }
    }

    /**
     * Construit les commandes UI pour mettre a jour un slot.
     */
    private void updateSlotUI(UICommandBuilder builder, int slotIndex, 
                              SlotState state, @Nullable String skillId, int timerSeconds) {
        // Selecteurs basés sur la nouvelle structure UI
        String slotPrefix = "#Slot" + slotIndex + " #SkillContainer" + slotIndex;
        String itemSelector = slotPrefix + " #Item" + slotIndex;
        String cooldownOverlay = slotPrefix + " #CooldownOverlay" + slotIndex;
        String activeOverlay = slotPrefix + " #ActiveOverlay" + slotIndex;
        
        switch (state) {
            case EMPTY -> {
                // Masquer l'item et les overlays
                builder.set(itemSelector + ".Visible", false);
                builder.set(cooldownOverlay + ".Visible", false);
                builder.set(activeOverlay + ".Visible", false);
            }
            case READY -> {
                // Afficher l'item, masquer les overlays
                builder.set(itemSelector + ".Visible", true);
                builder.set(itemSelector + ".ItemId", skillId);
                builder.set(itemSelector + ".Quantity", 1);
                builder.set(cooldownOverlay + ".Visible", false);
                builder.set(activeOverlay + ".Visible", false);
            }
            case ACTIVE -> {
                // Afficher l'item avec overlay vert (actif)
                builder.set(itemSelector + ".Visible", true);
                builder.set(itemSelector + ".ItemId", skillId);
                builder.set(itemSelector + ".Quantity", 1);
                builder.set(cooldownOverlay + ".Visible", false);
                builder.set(activeOverlay + ".Visible", true);
                builder.set(activeOverlay + " #ActiveText" + slotIndex + ".Text", timerSeconds + "s");
            }
            case COOLDOWN -> {
                // Afficher l'item avec overlay orange (cooldown)
                builder.set(itemSelector + ".Visible", true);
                builder.set(itemSelector + ".ItemId", skillId);
                builder.set(itemSelector + ".Quantity", 1);
                builder.set(activeOverlay + ".Visible", false);
                builder.set(cooldownOverlay + ".Visible", true);
                builder.set(cooldownOverlay + " #CooldownText" + slotIndex + ".Text", timerSeconds + "s");
            }
        }
    }

    /**
     * Retourne l'etat actuel d'un slot.
     */
    public SlotState getSlotState(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return SlotState.EMPTY;
        }
        return slots[slotIndex].state;
    }

    /**
     * Informations pour mettre a jour un slot.
     */
    public static class SlotInfo {
        public final SlotState state;
        public final String skillId;
        public final int timerSeconds;

        public SlotInfo(SlotState state, String skillId, int timerSeconds) {
            this.state = state;
            this.skillId = skillId;
            this.timerSeconds = timerSeconds;
        }

        public static SlotInfo empty() {
            return new SlotInfo(SlotState.EMPTY, null, 0);
        }

        public static SlotInfo ready(String skillId) {
            return new SlotInfo(SlotState.READY, skillId, 0);
        }

        public static SlotInfo active(String skillId, int timerSeconds) {
            return new SlotInfo(SlotState.ACTIVE, skillId, timerSeconds);
        }

        public static SlotInfo cooldown(String skillId, int timerSeconds) {
            return new SlotInfo(SlotState.COOLDOWN, skillId, timerSeconds);
        }
    }
}
