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
import lheido.skills.components.PoisonResistanceSkillComponent;
import lheido.skills.components.StaminaSkillComponent;
import lheido.skills.components.WaterBreathingSkillComponent;
import lheido.skills.ui.SkillSelectionPage;
import lheido.skills.utils.SkillIds;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Commande /skills pour ouvrir l'interface de selection des skills actifs.
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

        // Recuperer la map des skills possedes (prefix -> niveau)
        Map<String, Integer> ownedSkills = getOwnedSkills(store, ref);

        // Recuperer les skills actuellement actifs (prefixes)
        String[] currentActiveSkills = getCurrentActiveSkills(store, ref);

        // Creer et ouvrir la page de selection
        SkillSelectionPage page = new SkillSelectionPage(playerRef, ownedSkills, currentActiveSkills);

        // Ouvrir la page via le PageManager
        player.getPageManager().openCustomPage(ref, store, page);
    }

    /**
     * Recupere la map des skills possedes par le joueur.
     * Cle = prefix du skill, Valeur = niveau actuel
     */
    private Map<String, Integer> getOwnedSkills(Store<EntityStore> store, Ref<EntityStore> ref) {
        Map<String, Integer> ownedSkills = new HashMap<>();

        // Verifier Flying Skill
        FlyingSkillComponent flyingComponent = store.getComponent(ref, FlyingSkillComponent.getComponentType());
        if (flyingComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_FLYING, flyingComponent.getLevel());
        }

        // Verifier Water Breathing Skill
        WaterBreathingSkillComponent waterBreathingComponent = store.getComponent(ref, WaterBreathingSkillComponent.getComponentType());
        if (waterBreathingComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_WATER_BREATHING, waterBreathingComponent.getLevel());
        }

        // Verifier Stamina Skill
        StaminaSkillComponent staminaComponent = store.getComponent(ref, StaminaSkillComponent.getComponentType());
        if (staminaComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_STAMINA, staminaComponent.getLevel());
        }

        // Verifier Poison Resistance Skill
        PoisonResistanceSkillComponent poisonResistanceComponent = store.getComponent(ref, PoisonResistanceSkillComponent.getComponentType());
        if (poisonResistanceComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_POISON_RESISTANCE, poisonResistanceComponent.getLevel());
        }

        // Verifier Fire Resistance Skill
        FireResistanceSkillComponent fireResistanceComponent = store.getComponent(ref, FireResistanceSkillComponent.getComponentType());
        if (fireResistanceComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_FIRE_RESISTANCE, fireResistanceComponent.getLevel());
        }

        return ownedSkills;
    }

    /**
     * Recupere les skills actuellement actifs du joueur (prefixes).
     */
    private String[] getCurrentActiveSkills(Store<EntityStore> store, Ref<EntityStore> ref) {
        ActiveSkillsComponent activeComponent = store.getComponent(ref, ActiveSkillsComponent.getComponentType());
        if (activeComponent != null) {
            return activeComponent.getActiveSkills();
        }
        return new String[3];
    }
}
