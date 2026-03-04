package lheido.skills.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.components.StaminaSkillComponent;
import lheido.skills.components.WaterBreathingSkillComponent;
import lheido.skills.ui.SkillSelectionPage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Interaction pour ouvrir la page de selection des skills actifs.
 * 
 * Quand l'item associe est utilise, cette interaction:
 * - Recupere la liste des skills possedes par le joueur
 * - Recupere les skills actuellement actifs
 * - Ouvre la page de selection de skills
 */
public class OpenSkillSelectionInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<OpenSkillSelectionInteraction> CODEC =
        BuilderCodec.builder(
            OpenSkillSelectionInteraction.class,
            OpenSkillSelectionInteraction::new,
            SimpleInstantInteraction.CODEC
        ).build();

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    protected void firstRun(
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext interactionContext,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("OpenSkillSelectionInteraction: CommandBuffer is null");
            return;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        
        // Recuperer le PlayerRef
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("OpenSkillSelectionInteraction: PlayerRef is null");
            return;
        }

        // Recuperer le Player
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("OpenSkillSelectionInteraction: Player is null");
            return;
        }

        // Recuperer la liste des skills possedes par le joueur
        List<String> ownedSkills = getOwnedSkills(commandBuffer, ref);
        
        // Recuperer les skills actuellement actifs
        String[] currentActiveSkills = getCurrentActiveSkills(commandBuffer, ref);

        // Creer et ouvrir la page de selection
        SkillSelectionPage page = new SkillSelectionPage(playerRef, ownedSkills, currentActiveSkills);
        
        // Ouvrir la page via le PageManager
        player.getPageManager().openCustomPage(
            ref,
            interactionContext.getCommandBuffer().getStore(),
            page
        );
    }

    /**
     * Recupere la liste des IDs de skills que le joueur possede.
     * 
     * Parcourt les composants de skills connus et retourne les IDs
     * des skills debloques.
     */
    private List<String> getOwnedSkills(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref) {
        List<String> ownedSkills = new ArrayList<>();

        // Verifier Flying Skill
        FlyingSkillComponent flyingComponent = commandBuffer.getComponent(ref, FlyingSkillComponent.getComponentType());
        if (flyingComponent != null) {
            // Ajouter l'ID du skill en fonction du niveau
            String skillId = getFlyingSkillId(flyingComponent.getLevel());
            if (skillId != null) {
                ownedSkills.add(skillId);
            }
        }

        // Verifier Water Breathing Skill
        WaterBreathingSkillComponent waterBreathingComponent = commandBuffer.getComponent(ref, WaterBreathingSkillComponent.getComponentType());
        if (waterBreathingComponent != null) {
            String skillId = getWaterBreathingSkillId(waterBreathingComponent.getLevel());
            if (skillId != null) {
                ownedSkills.add(skillId);
            }
        }

        // Verifier Stamina Skill
        StaminaSkillComponent staminaComponent = commandBuffer.getComponent(ref, StaminaSkillComponent.getComponentType());
        if (staminaComponent != null) {
            String skillId = getStaminaSkillId(staminaComponent.getLevel());
            if (skillId != null) {
                ownedSkills.add(skillId);
            }
        }

        return ownedSkills;
    }

    /**
     * Recupere les skills actuellement actifs du joueur.
     */
    private String[] getCurrentActiveSkills(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref) {
        ActiveSkillsComponent activeComponent = commandBuffer.getComponent(ref, ActiveSkillsComponent.getComponentType());
        if (activeComponent != null) {
            return activeComponent.getActiveSkills();
        }
        return new String[3];
    }

    /**
     * Retourne l'ID de l'item Flying skill en fonction du niveau.
     */
    private String getFlyingSkillId(int level) {
        return switch (level) {
            case 1 -> "Skill_Flying_A";
            case 2 -> "Skill_Flying_B";
            case 3 -> "Skill_Flying_C";
            case 4 -> "Skill_Flying_X";
            default -> null;
        };
    }

    /**
     * Retourne l'ID de l'item Water Breathing skill en fonction du niveau.
     */
    private String getWaterBreathingSkillId(int level) {
        return switch (level) {
            case 1 -> "Skill_WaterBreathing_A";
            case 2 -> "Skill_WaterBreathing_B";
            case 3 -> "Skill_WaterBreathing_C";
            case 4 -> "Skill_WaterBreathing_X";
            default -> null;
        };
    }

    /**
     * Retourne l'ID de l'item Stamina skill en fonction du niveau.
     */
    private String getStaminaSkillId(int level) {
        return switch (level) {
            case 1 -> "Skill_Stamina_A";
            case 2 -> "Skill_Stamina_B";
            case 3 -> "Skill_Stamina_C";
            case 4 -> "Skill_Stamina_X";
            default -> null;
        };
    }
}
