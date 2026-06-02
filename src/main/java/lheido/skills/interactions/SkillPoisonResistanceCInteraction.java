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
 * Interaction to upgrade the PoisonResistance skill to level C.
 * Requires the player to already have the PoisonResistance skill at level B.
 * Improves poison resistance (75% instead of 50%).
 */
public class SkillPoisonResistanceCInteraction
    extends SimpleInstantInteraction
{

    public static final BuilderCodec<SkillPoisonResistanceCInteraction> CODEC =
        BuilderCodec.builder(
            SkillPoisonResistanceCInteraction.class,
            SkillPoisonResistanceCInteraction::new,
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

        // Upgrade to level C
        PoisonResistanceSkillComponent upgradedComponent =
            PoisonResistanceSkillComponent.createLevelC();

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
                "Poison Resistance skill upgraded to level 3! 75% poison damage reduction."
            )
        );
    }
}
