package lheido.skills.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
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
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Interaction to check the upgrade prerequisites for the PoisonResistance skill.
 * Configurable via JSON with RequiredLevel and TargetLevel.
 *
 * - RequiredLevel: The minimum required level (0 = no prerequisite, for PoisonResistance A)
 * - TargetLevel: The level to upgrade to
 *
 * Fails if:
 * - The player does not have the required level
 * - The player already has the target level or higher
 */
public class CheckPoisonResistanceUpgradeInteraction
    extends SimpleInstantInteraction
{

    public static final BuilderCodec<
        CheckPoisonResistanceUpgradeInteraction
    > CODEC = BuilderCodec.builder(
        CheckPoisonResistanceUpgradeInteraction.class,
        CheckPoisonResistanceUpgradeInteraction::new,
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

        PlayerRef playerRef = commandBuffer.getComponent(
            ref,
            PlayerRef.getComponentType()
        );
        if (playerRef == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Get the existing PoisonResistance component (may be null)
        PoisonResistanceSkillComponent existingComponent =
            commandBuffer.getComponent(
                ref,
                PoisonResistanceSkillComponent.getComponentType()
            );

        int currentLevel =
            existingComponent != null ? existingComponent.getLevel() : 0;

        // Check if the player has the required level
        if (currentLevel < requiredLevel) {
            String message =
                requiredLevel == 0
                    ? "You cannot use this skill!"
                    : "You must have Poison Resistance (" +
                      requiredLevel +
                      ") before upgrading!";
            sendMessage(playerRef, Message.raw(message));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Check if the player already has the target level or higher
        if (currentLevel >= targetLevel) {
            String message =
                targetLevel == 4
                    ? "You already have full poison immunity!"
                    : "You already have Poison Resistance level " +
                      targetLevel +
                      " or higher!";
            sendMessage(playerRef, Message.raw(message));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Prerequisites validated, the next interaction can execute
    }
}
