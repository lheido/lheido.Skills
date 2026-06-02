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
 * Interaction to unlock the LifeSteal skill (level A).
 * When the item is used:
 * - Adds the LifeStealSkillComponent to the player
 */
public class SkillLifeStealInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillLifeStealInteraction> CODEC =
        BuilderCodec.builder(
            SkillLifeStealInteraction.class,
            SkillLifeStealInteraction::new,
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

        // Create the component with level A parameters
        LifeStealSkillComponent component =
            LifeStealSkillComponent.createLevelA();

        // Add the component to the player
        commandBuffer.addComponent(
            ref,
            LifeStealSkillComponent.getComponentType(),
            component
        );

        sendMessage(player, 
            Message.raw(
                "Life Steal skill unlocked! You recover 5% of damage dealt as health."
            )
        );
    }
}
