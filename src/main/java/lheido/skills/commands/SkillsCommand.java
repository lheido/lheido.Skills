package lheido.skills.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.FireResistanceSkillComponent;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.components.LifeStealSkillComponent;
import lheido.skills.components.PoisonResistanceSkillComponent;
import lheido.skills.components.StaminaSkillComponent;
import lheido.skills.components.WaterBreathingSkillComponent;
import lheido.skills.ui.SkillSelectionPage;
import lheido.skills.utils.SkillIds;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Command /skills to open the active skill selection interface.
 */
public class SkillsCommand extends AbstractPlayerCommand {

    public SkillsCommand() {
        super("skills", "Open the skill selection menu");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        // Retrieve the map of owned skills (prefix -> level)
        Map<String, Integer> ownedSkills = getOwnedSkills(store, ref);

        // Retrieve the currently active skills (prefixes)
        String[] currentActiveSkills = getCurrentActiveSkills(store, ref);

        // Create and open the selection page
        SkillSelectionPage page = new SkillSelectionPage(playerRef, ownedSkills, currentActiveSkills);

        // Open the page via the PageManager
        player.getPageManager().openCustomPage(ref, store, page);
    }

    /**
     * Retrieves the map of skills owned by the player.
     * Key = skill prefix, Value = current level
     */
    private Map<String, Integer> getOwnedSkills(Store<EntityStore> store, Ref<EntityStore> ref) {
        Map<String, Integer> ownedSkills = new HashMap<>();

        // Check Flying Skill
        FlyingSkillComponent flyingComponent = store.getComponent(ref, FlyingSkillComponent.getComponentType());
        if (flyingComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_FLYING, flyingComponent.getLevel());
        }

        // Check Water Breathing Skill
        WaterBreathingSkillComponent waterBreathingComponent = store.getComponent(ref, WaterBreathingSkillComponent.getComponentType());
        if (waterBreathingComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_WATER_BREATHING, waterBreathingComponent.getLevel());
        }

        // Check Stamina Skill
        StaminaSkillComponent staminaComponent = store.getComponent(ref, StaminaSkillComponent.getComponentType());
        if (staminaComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_STAMINA, staminaComponent.getLevel());
        }

        // Check Poison Resistance Skill
        PoisonResistanceSkillComponent poisonResistanceComponent = store.getComponent(ref, PoisonResistanceSkillComponent.getComponentType());
        if (poisonResistanceComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_POISON_RESISTANCE, poisonResistanceComponent.getLevel());
        }

        // Check Fire Resistance Skill
        FireResistanceSkillComponent fireResistanceComponent = store.getComponent(ref, FireResistanceSkillComponent.getComponentType());
        if (fireResistanceComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_FIRE_RESISTANCE, fireResistanceComponent.getLevel());
        }

        // Check Life Steal Skill
        LifeStealSkillComponent lifeStealComponent = store.getComponent(ref, LifeStealSkillComponent.getComponentType());
        if (lifeStealComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_LIFE_STEAL, lifeStealComponent.getLevel());
        }

        return ownedSkills;
    }

    /**
     * Retrieves the currently active skills of the player (prefixes).
     */
    private String[] getCurrentActiveSkills(Store<EntityStore> store, Ref<EntityStore> ref) {
        ActiveSkillsComponent activeComponent = store.getComponent(ref, ActiveSkillsComponent.getComponentType());
        if (activeComponent != null) {
            return activeComponent.getActiveSkills();
        }
        return new String[3];
    }
}
