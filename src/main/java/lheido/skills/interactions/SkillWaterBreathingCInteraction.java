package lheido.skills.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.component.CommandBuffer;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.component.Ref;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.protocol.InteractionState;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.protocol.InteractionType;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.Message;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.entity.entities.Player;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import javax.annotation.Nonnull;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import lheido.skills.components.WaterBreathingSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;

/**
 * Interaction to upgrade the WaterBreathing skill to level C.
 * Requires the player to already have the WaterBreathing skill at level B.
 * Improves the oxygen multiplier (+200%).
 */
public class SkillWaterBreathingCInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillWaterBreathingCInteraction> CODEC =
        BuilderCodec.builder(
            SkillWaterBreathingCInteraction.class,
            SkillWaterBreathingCInteraction::new,
            SimpleInstantInteraction.CODEC
        ).build();

    @Override
    protected void firstRun(
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext interactionContext,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer =
            interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(
            ref,
            Player.getComponentType()
        );
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Prerequisites are verified by CheckWaterBreathingUpgradeInteraction
        // Get the existing component (guaranteed by the check)
        WaterBreathingSkillComponent existingComponent =
            commandBuffer.getComponent(
                ref,
                WaterBreathingSkillComponent.getComponentType()
            );

        // Upgrade to level C
        WaterBreathingSkillComponent upgradedComponent =
            WaterBreathingSkillComponent.createLevelC();

        // Remove the old one and add the new one
        if (existingComponent != null) {
            commandBuffer.removeComponent(
                ref,
                WaterBreathingSkillComponent.getComponentType()
            );
        }

        commandBuffer.addComponent(
            ref,
            WaterBreathingSkillComponent.getComponentType(),
            upgradedComponent
        );

        sendMessage(player, 
            Message.raw(
                "Water Breathing skill upgraded to level 3! +200% oxygen duration."
            )
        );
    }
}
