package lheido.skills.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import lheido.skills.components.WaterBreathingSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Interaction to upgrade the WaterBreathing skill to level B.
 * Requires the player to already have the WaterBreathing skill at level A.
 * Improves the oxygen multiplier (+100% instead of +50%).
 */
public class SkillWaterBreathingBInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillWaterBreathingBInteraction> CODEC =
        BuilderCodec.builder(
            SkillWaterBreathingBInteraction.class,
            SkillWaterBreathingBInteraction::new,
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

        PlayerRef playerRef = commandBuffer.getComponent(
            ref,
            PlayerRef.getComponentType()
        );
        if (playerRef == null) {
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

        // Upgrade to level B
        WaterBreathingSkillComponent upgradedComponent =
            WaterBreathingSkillComponent.createLevelB();

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

        sendMessage(playerRef, 
            Message.raw(
                "Water Breathing skill upgraded to level 2! +100% oxygen duration."
            )
        );
    }
}
