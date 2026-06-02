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
import lheido.skills.components.PoisonResistanceSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;

/**
 * Interaction to upgrade the PoisonResistance skill to level B.
 * Requires the player to already have the PoisonResistance skill at level A.
 * Improves poison resistance (50% instead of 25%).
 */
public class SkillPoisonResistanceBInteraction
    extends SimpleInstantInteraction
{

    public static final BuilderCodec<SkillPoisonResistanceBInteraction> CODEC =
        BuilderCodec.builder(
            SkillPoisonResistanceBInteraction.class,
            SkillPoisonResistanceBInteraction::new,
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

        // Upgrade to level B
        PoisonResistanceSkillComponent upgradedComponent =
            PoisonResistanceSkillComponent.createLevelB();

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

        sendMessage(player, 
            Message.raw(
                "Poison Resistance skill upgraded to level 2! 50% poison damage reduction."
            )
        );
    }
}
