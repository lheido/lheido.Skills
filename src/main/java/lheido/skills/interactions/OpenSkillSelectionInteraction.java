package lheido.skills.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.component.CommandBuffer;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.component.Ref;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.logger.HytaleLogger;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.protocol.InteractionState;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.protocol.InteractionType;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.entity.entities.Player;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import lheido.skills.components.ActiveSkillsComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import lheido.skills.components.FireResistanceSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import lheido.skills.components.FlyingSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import lheido.skills.components.LifeStealSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import lheido.skills.components.PoisonResistanceSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import lheido.skills.components.StaminaSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import lheido.skills.components.WaterBreathingSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import lheido.skills.ui.SkillSelectionPage;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import lheido.skills.utils.SkillIds;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;

import javax.annotation.Nonnull;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import java.util.HashMap;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import java.util.Map;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;

/**
 * Interaction to open the active skills selection page.
 * 
 * When the associated item is used, this interaction:
 * - Retrieves the list of skills owned by the player (prefix -> level)
 * - Retrieves the currently active skills (prefixes)
 * - Opens the skill selection page
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
        
        // Retrieve the PlayerRef
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("OpenSkillSelectionInteraction: PlayerRef is null");
            return;
        }

        // Retrieve the Player
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("OpenSkillSelectionInteraction: Player is null");
            return;
        }

        // Retrieve the map of owned skills (prefix -> level)
        Map<String, Integer> ownedSkills = getOwnedSkills(commandBuffer, ref);
        
        // Retrieve the currently active skills (prefixes)
        String[] currentActiveSkills = getCurrentActiveSkills(commandBuffer, ref);

        // Create and open the selection page
        SkillSelectionPage page = new SkillSelectionPage(playerRef, ownedSkills, currentActiveSkills);
        
        // Open the page via the PageManager
        player.getPageManager().openCustomPage(
            ref,
            interactionContext.getCommandBuffer().getStore(),
            page
        );
    }

    /**
     * Retrieves the map of skills owned by the player.
     * Key = skill prefix, Value = current level
     */
    private Map<String, Integer> getOwnedSkills(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref) {
        Map<String, Integer> ownedSkills = new HashMap<>();

        // Check Flying Skill
        FlyingSkillComponent flyingComponent = commandBuffer.getComponent(ref, FlyingSkillComponent.getComponentType());
        if (flyingComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_FLYING, flyingComponent.getLevel());
        }

        // Check Water Breathing Skill
        WaterBreathingSkillComponent waterBreathingComponent = commandBuffer.getComponent(ref, WaterBreathingSkillComponent.getComponentType());
        if (waterBreathingComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_WATER_BREATHING, waterBreathingComponent.getLevel());
        }

        // Check Stamina Skill
        StaminaSkillComponent staminaComponent = commandBuffer.getComponent(ref, StaminaSkillComponent.getComponentType());
        if (staminaComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_STAMINA, staminaComponent.getLevel());
        }

        // Check Poison Resistance Skill
        PoisonResistanceSkillComponent poisonResistanceComponent = commandBuffer.getComponent(ref, PoisonResistanceSkillComponent.getComponentType());
        if (poisonResistanceComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_POISON_RESISTANCE, poisonResistanceComponent.getLevel());
        }

        // Check Fire Resistance Skill
        FireResistanceSkillComponent fireResistanceComponent = commandBuffer.getComponent(ref, FireResistanceSkillComponent.getComponentType());
        if (fireResistanceComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_FIRE_RESISTANCE, fireResistanceComponent.getLevel());
        }

        // Check Life Steal Skill
        LifeStealSkillComponent lifeStealComponent = commandBuffer.getComponent(ref, LifeStealSkillComponent.getComponentType());
        if (lifeStealComponent != null) {
            ownedSkills.put(SkillIds.PREFIX_LIFE_STEAL, lifeStealComponent.getLevel());
        }

        return ownedSkills;
    }

    /**
     * Retrieves the currently active skills of the player (prefixes).
     */
    private String[] getCurrentActiveSkills(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref) {
        ActiveSkillsComponent activeComponent = commandBuffer.getComponent(ref, ActiveSkillsComponent.getComponentType());
        if (activeComponent != null) {
            return activeComponent.getActiveSkills();
        }
        return new String[3];
    }
}
