package lheido.skills.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
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
import lheido.skills.components.StaminaSkillComponent;

public class CheckStaminaUpgradeInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<CheckStaminaUpgradeInteraction> CODEC =
        BuilderCodec.builder(
            CheckStaminaUpgradeInteraction.class,
            CheckStaminaUpgradeInteraction::new,
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

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private int requiredLevel = 0;
    private int targetLevel = 1;

    @Override
    protected void firstRun(
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext interactionContext,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
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

        StaminaSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            StaminaSkillComponent.getComponentType()
        );

        int currentLevel = existingComponent != null ? existingComponent.getLevel() : 0;

        if (currentLevel < requiredLevel) {
            String message = "You must have Stamina (" + requiredLevel + ") before upgrading!";
            player.sendMessage(Message.raw(message));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        if (currentLevel >= targetLevel) {
            player.sendMessage(Message.raw("You already have this level or higher!"));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        LOGGER.atInfo().log("Stamina upgrade check passed: " + currentLevel + " -> " + targetLevel);
    }
}
