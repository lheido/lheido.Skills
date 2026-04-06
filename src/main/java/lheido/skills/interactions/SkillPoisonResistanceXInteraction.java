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
import lheido.skills.components.PoisonResistanceSkillComponent;

/**
 * Interaction to upgrade the PoisonResistance skill to level X (ultimate).
 * Requires the player to already have the PoisonResistance skill at level C.
 * Grants full poison immunity.
 */
public class SkillPoisonResistanceXInteraction
    extends SimpleInstantInteraction
{

    public static final BuilderCodec<SkillPoisonResistanceXInteraction> CODEC =
        BuilderCodec.builder(
            SkillPoisonResistanceXInteraction.class,
            SkillPoisonResistanceXInteraction::new,
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

        // Prerequisites are verified by CheckPoisonResistanceUpgradeInteraction
        // Get the existing component (guaranteed by the check)
        PoisonResistanceSkillComponent existingComponent =
            commandBuffer.getComponent(
                ref,
                PoisonResistanceSkillComponent.getComponentType()
            );

        // Upgrade to level X (ultimate)
        PoisonResistanceSkillComponent upgradedComponent =
            PoisonResistanceSkillComponent.createLevelX();

        // Remove the old one and add the new one
        if (existingComponent != null) {
            commandBuffer.removeComponent(
                ref,
                PoisonResistanceSkillComponent.getComponentType()
            );
        }

        commandBuffer.addComponent(
            ref,
            PoisonResistanceSkillComponent.getComponentType(),
            upgradedComponent
        );

        player.sendMessage(
            Message.raw(
                "Poison Resistance skill upgraded to ultimate level! Full poison immunity!"
            )
        );
    }
}
