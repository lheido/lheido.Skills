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
import lheido.skills.components.WaterBreathingSkillComponent;
import static lheido.skills.utils.PlayerMessageUtils.sendMessage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Interaction to unlock the WaterBreathing skill.
 * When the item is used:
 * - Adds the WaterBreathingSkillComponent to the player
 * The WaterBreathingSystem then handles the oxygen logic.
 */
public class SkillWaterBreathingInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillWaterBreathingInteraction> CODEC =
        BuilderCodec.builder(
            SkillWaterBreathingInteraction.class,
            SkillWaterBreathingInteraction::new,
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

        PlayerRef playerRef = commandBuffer.getComponent(
            ref,
            PlayerRef.getComponentType()
        );
        if (playerRef == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Create the component with level A parameters
        WaterBreathingSkillComponent component =
            WaterBreathingSkillComponent.createLevelA();

        // Add the component to the player
        commandBuffer.addComponent(
            ref,
            WaterBreathingSkillComponent.getComponentType(),
            component
        );

        sendMessage(playerRef, 
            Message.raw(
                "Water Breathing skill unlocked! +50% oxygen duration underwater."
            )
        );
    }
}
