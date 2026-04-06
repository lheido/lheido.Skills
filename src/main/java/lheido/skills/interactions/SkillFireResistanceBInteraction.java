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
import lheido.skills.components.FireResistanceSkillComponent;

/**
 * Interaction to upgrade the FireResistance skill to level B.
 * Requires the player to already have the FireResistance skill at level A.
 * Improves fire resistance (50% instead of 25%).
 */
public class SkillFireResistanceBInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillFireResistanceBInteraction> CODEC =
        BuilderCodec.builder(
            SkillFireResistanceBInteraction.class,
            SkillFireResistanceBInteraction::new,
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

        // Prerequisites are verified by CheckFireResistanceUpgradeInteraction
        // Get the existing component (guaranteed by the check)
        FireResistanceSkillComponent existingComponent =
            commandBuffer.getComponent(
                ref,
                FireResistanceSkillComponent.getComponentType()
            );

        // Upgrade to level B
        FireResistanceSkillComponent upgradedComponent =
            FireResistanceSkillComponent.createLevelB();

        // Remove the old one and add the new one
        if (existingComponent != null) {
            commandBuffer.removeComponent(
                ref,
                FireResistanceSkillComponent.getComponentType()
            );
        }

        commandBuffer.addComponent(
            ref,
            FireResistanceSkillComponent.getComponentType(),
            upgradedComponent
        );

        player.sendMessage(
            Message.raw(
                "Fire Resistance skill upgraded to level 2! 50% fire damage reduction."
            )
        );
    }
}
