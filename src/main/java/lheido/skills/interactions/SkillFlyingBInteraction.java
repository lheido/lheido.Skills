package lheido.skills.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import lheido.skills.components.FlyingSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Interaction to upgrade the Flying skill to level B.
 * Requires that the player already has the Flying skill at level A.
 * Improves flight duration (15s instead of 10s) and reduces cooldown (18s instead of 20s).
 */
public class SkillFlyingBInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillFlyingBInteraction> CODEC =
        BuilderCodec.builder(
            SkillFlyingBInteraction.class,
            SkillFlyingBInteraction::new,
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
                "SkillFlyingBInteraction: CommandBuffer is null"
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
            return;
        }

        PlayerRef playerRef = commandBuffer.getComponent(
            ref,
            PlayerRef.getComponentType()
        );
        if (playerRef == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("SkillFlyingBInteraction: Player is null");
            return;
        }

        // Prerequisites are verified by CheckFlyingUpgradeInteraction
        // Get the existing component (guaranteed by the check)
        FlyingSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            FlyingSkillComponent.getComponentType()
        );

        // Upgrade to level B
        FlyingSkillComponent upgradedComponent =
            FlyingSkillComponent.createLevelB();

        // Remove the old component if it exists
        // Note: We don't preserve state because the upgrade provides new parameters
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

        sendMessage(playerRef, 
            Message.raw(
                "Flying skill upgraded to level 2! Fly duration: 15s, Cooldown: 18s"
            )
        );
    }
}
