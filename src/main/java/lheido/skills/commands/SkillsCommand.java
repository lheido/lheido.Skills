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
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.components.StaminaSkillComponent;
import lheido.skills.components.WaterBreathingSkillComponent;
import lheido.skills.ui.SkillSelectionPage;
import lheido.skills.utils.SkillIds;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

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

        // Recuperer la liste des skills possedes par le joueur
        List<String> ownedSkills = getOwnedSkills(store, ref);

        // Recuperer les skills actuellement actifs
        String[] currentActiveSkills = getCurrentActiveSkills(store, ref);

        // Creer et ouvrir la page de selection
        SkillSelectionPage page = new SkillSelectionPage(playerRef, ownedSkills, currentActiveSkills);

        // Ouvrir la page via le PageManager
        player.getPageManager().openCustomPage(ref, store, page);
    }

    /**
     * Recupere la liste des IDs de skills que le joueur possede.
     */
    private List<String> getOwnedSkills(Store<EntityStore> store, Ref<EntityStore> ref) {
        List<String> ownedSkills = new ArrayList<>();

        // Verifier Flying Skill
        FlyingSkillComponent flyingComponent = store.getComponent(ref, FlyingSkillComponent.getComponentType());
        if (flyingComponent != null) {
            String skillId = SkillIds.getFlyingSkillId(flyingComponent.getLevel());
            if (skillId != null) {
                ownedSkills.add(skillId);
            }
        }

        // Verifier Water Breathing Skill
        WaterBreathingSkillComponent waterBreathingComponent = store.getComponent(ref, WaterBreathingSkillComponent.getComponentType());
        if (waterBreathingComponent != null) {
            String skillId = SkillIds.getWaterBreathingSkillId(waterBreathingComponent.getLevel());
            if (skillId != null) {
                ownedSkills.add(skillId);
            }
        }

        // Verifier Stamina Skill
        StaminaSkillComponent staminaComponent = store.getComponent(ref, StaminaSkillComponent.getComponentType());
        if (staminaComponent != null) {
            String skillId = SkillIds.getStaminaSkillId(staminaComponent.getLevel());
            if (skillId != null) {
                ownedSkills.add(skillId);
            }
        }

        return ownedSkills;
    }

    /**
     * Recupere les skills actuellement actifs du joueur.
     */
    private String[] getCurrentActiveSkills(Store<EntityStore> store, Ref<EntityStore> ref) {
        ActiveSkillsComponent activeComponent = store.getComponent(ref, ActiveSkillsComponent.getComponentType());
        if (activeComponent != null) {
            return activeComponent.getActiveSkills();
        }
        return new String[3];
    }
}
