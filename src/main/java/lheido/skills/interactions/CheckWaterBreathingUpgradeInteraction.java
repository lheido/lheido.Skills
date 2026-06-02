package lheido.skills.interactions;

import com.hypixel.hytale.codec.Codec;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.codec.KeyedCodec;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
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
 * Interaction to check the upgrade prerequisites for the WaterBreathing skill.
 * Configurable via JSON with RequiredLevel and TargetLevel.
 *
 * - RequiredLevel: The minimum required level (0 = no prerequisite, for WaterBreathing A)
 * - TargetLevel: The level to upgrade to
 *
 * Fails if:
 * - The player does not have the required level
 * - The player already has the target level or higher
 */
public class CheckWaterBreathingUpgradeInteraction
    extends SimpleInstantInteraction
{

    public static final BuilderCodec<
        CheckWaterBreathingUpgradeInteraction
    > CODEC = BuilderCodec.builder(
        CheckWaterBreathingUpgradeInteraction.class,
        CheckWaterBreathingUpgradeInteraction::new,
        SimpleInstantInteraction.CODEC
    )
        .append(
            new KeyedCodec<>("RequiredLevel", Codec.INTEGER),
            (data, value) -> data.requiredLevel = value,
            data -> data.requiredLevel
        )
        .add()
        .append(
            new KeyedCodec<>("TargetLevel", Codec.INTEGER),
            (data, value) -> data.targetLevel = value,
            data -> data.targetLevel
        )
        .add()
        .build();

    private int requiredLevel = 0;
    private int targetLevel = 1;

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

        // Get the existing WaterBreathing component (may be null)
        WaterBreathingSkillComponent existingComponent =
            commandBuffer.getComponent(
                ref,
                WaterBreathingSkillComponent.getComponentType()
            );

        int currentLevel =
            existingComponent != null ? existingComponent.getLevel() : 0;

        // Check if the player has the required level
        if (currentLevel < requiredLevel) {
            String message =
                requiredLevel == 0
                    ? "You cannot use this skill!"
                    : "You must have Water Breathing (" +
                      requiredLevel +
                      ") before upgrading!";
            sendMessage(player, Message.raw(message));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Check if the player already has the target level or higher
        if (currentLevel >= targetLevel) {
            String message =
                targetLevel == 4
                    ? "You already have the ultimate Water Breathing skill!"
                    : "You already have Water Breathing level " +
                      targetLevel +
                      " or higher!";
            sendMessage(player, Message.raw(message));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Prerequisites validated, the next interaction can execute
    }
}
