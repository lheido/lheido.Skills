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
 * Interaction pour ouvrir la page de selection des skills actifs.
 * 
 * Quand l'item associe est utilise, cette interaction:
 * - Recupere la liste des skills possedes par le joueur (prefix -> niveau)
 * - Recupere les skills actuellement actifs (prefixes)
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

        // Recuperer la map des skills possedes (prefix -> niveau)
        Map<String, Integer> ownedSkills = getOwnedSkills(commandBuffer, ref);
        
        // Recuperer les skills actuellement actifs (prefixes)
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
     * Recupere la map des skills possedes par le joueur.
     * Cle = prefix du skill, Valeur = niveau actuel
     */
    private Map<String, Integer> getOwnedSkills(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref) {
        Map<String, Integer> ownedSkills = new HashMap<>();

        // Verifier Flying Skill
        FlyingSkillComponent flyingComponent = commandBuffer.getComponent(ref, FlyingSkillComponent.getComponentType());
        if (flyingComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_FLYING, flyingComponent.getLevel());
        }

        // Verifier Water Breathing Skill
        WaterBreathingSkillComponent waterBreathingComponent = commandBuffer.getComponent(ref, WaterBreathingSkillComponent.getComponentType());
        if (waterBreathingComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_WATER_BREATHING, waterBreathingComponent.getLevel());
        }

        // Verifier Stamina Skill
        StaminaSkillComponent staminaComponent = commandBuffer.getComponent(ref, StaminaSkillComponent.getComponentType());
        if (staminaComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_STAMINA, staminaComponent.getLevel());
        }

        // Verifier Poison Resistance Skill
        PoisonResistanceSkillComponent poisonResistanceComponent = commandBuffer.getComponent(ref, PoisonResistanceSkillComponent.getComponentType());
        if (poisonResistanceComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_POISON_RESISTANCE, poisonResistanceComponent.getLevel());
        }

        // Verifier Fire Resistance Skill
        FireResistanceSkillComponent fireResistanceComponent = commandBuffer.getComponent(ref, FireResistanceSkillComponent.getComponentType());
        if (fireResistanceComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_FIRE_RESISTANCE, fireResistanceComponent.getLevel());
        }

        // Verifier Life Steal Skill
        LifeStealSkillComponent lifeStealComponent = commandBuffer.getComponent(ref, LifeStealSkillComponent.getComponentType());
        if (lifeStealComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_LIFE_STEAL, lifeStealComponent.getLevel());
        }

        return ownedSkills;
    }

    /**
     * Recupere les skills actuellement actifs du joueur (prefixes).
     */
    private String[] getCurrentActiveSkills(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref) {
        ActiveSkillsComponent activeComponent = commandBuffer.getComponent(ref, ActiveSkillsComponent.getComponentType());
        if (activeComponent != null) {
            return activeComponent.getActiveSkills();
        }
        return new String[3];
    }
}
