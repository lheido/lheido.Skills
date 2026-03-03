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
import lheido.skills.components.StaminaSkillComponent;

public class SkillStaminaCInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillStaminaCInteraction> CODEC =
        BuilderCodec.builder(
            SkillStaminaCInteraction.class,
            SkillStaminaCInteraction::new,
            SimpleInstantInteraction.CODEC
        ).build();

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

        StaminaSkillComponent upgradedComponent = StaminaSkillComponent.createLevelC();

        if (existingComponent != null) {
            commandBuffer.removeComponent(ref, StaminaSkillComponent.getComponentType());
        }
        commandBuffer.addComponent(
            ref,
            StaminaSkillComponent.getComponentType(),
            upgradedComponent
        );

        player.sendMessage(
            Message.raw("Stamina skill upgraded to level 3! +200% stamina capacity.")
        );
    }
}
