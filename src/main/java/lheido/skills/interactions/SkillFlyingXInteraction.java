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
import lheido.skills.components.FlyingSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;

/**
 * Interaction to upgrade the Flying skill to level X (ultimate).
 * Requires the player to already have the Flying skill at level C.
 * Unlimited flight with no cooldown!
 */
public class SkillFlyingXInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillFlyingXInteraction> CODEC =
        BuilderCodec.builder(
            SkillFlyingXInteraction.class,
            SkillFlyingXInteraction::new,
            SimpleInstantInteraction.CODEC
        ).build();

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
            LOGGER.atWarning().log(
                "SkillFlyingXInteraction: CommandBuffer is null"
            );
            return;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(
            ref,
            Player.getComponentType()
        );
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("SkillFlyingXInteraction: Player is null");
            return;
        }

        // Prerequisites are verified by CheckFlyingUpgradeInteraction
        // Upgrade to level X (ultimate) - unlimited flight
        FlyingSkillComponent upgradedComponent =
            FlyingSkillComponent.createLevelX();

        // Remove the old component if it exists
        FlyingSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            FlyingSkillComponent.getComponentType()
        );
        if (existingComponent != null) {
            commandBuffer.removeComponent(
                ref,
                FlyingSkillComponent.getComponentType()
            );
        }

        commandBuffer.addComponent(
            ref,
            FlyingSkillComponent.getComponentType(),
            upgradedComponent
        );

        sendMessage(player, 
            Message.raw(
                "Flying skill upgraded to ULTIMATE! Unlimited flight, no cooldown!"
            )
        );
    }
}
