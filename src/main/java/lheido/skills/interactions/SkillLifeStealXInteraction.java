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
import lheido.skills.components.LifeStealSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;

/**
 * Interaction to upgrade the LifeSteal skill to level X (maximum).
 * Requires the player to already have the LifeSteal skill at level C.
 * Improves life steal (25% - maximum level).
 */
public class SkillLifeStealXInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillLifeStealXInteraction> CODEC =
        BuilderCodec.builder(
            SkillLifeStealXInteraction.class,
            SkillLifeStealXInteraction::new,
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

        // Prerequisites are verified by CheckLifeStealUpgradeInteraction
        // Get the existing component (guaranteed by the check)
        LifeStealSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            LifeStealSkillComponent.getComponentType()
        );

        // Upgrade to level X (maximum)
        LifeStealSkillComponent upgradedComponent =
            LifeStealSkillComponent.createLevelX();

        // Remove the old one and add the new one
        if (existingComponent != null) {
            commandBuffer.removeComponent(
                ref,
                LifeStealSkillComponent.getComponentType()
            );
        }

        commandBuffer.addComponent(
            ref,
            LifeStealSkillComponent.getComponentType(),
            upgradedComponent
        );

        sendMessage(player, 
            Message.raw(
                "Life Steal skill maxed out! You recover 25% of damage dealt as health."
            )
        );
    }
}
