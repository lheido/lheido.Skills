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

/**
 * Interaction pour upgrader le skill WaterBreathing vers le niveau X (ultime).
 * Requiert que le joueur possède déjà le skill WaterBreathing niveau C.
 * Oxygène illimité!
 */
public class SkillWaterBreathingXInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillWaterBreathingXInteraction> CODEC =
        BuilderCodec.builder(
            SkillWaterBreathingXInteraction.class,
            SkillWaterBreathingXInteraction::new,
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

        // Les prérequis sont vérifiés par CheckWaterBreathingUpgradeInteraction
        // Upgrade vers le niveau X (ultime) - oxygène illimité
        WaterBreathingSkillComponent upgradedComponent = WaterBreathingSkillComponent.createLevelX();

        // Supprimer l'ancien component s'il existe
        WaterBreathingSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            WaterBreathingSkillComponent.getComponentType()
        );
        if (existingComponent != null) {
            commandBuffer.removeComponent(ref, WaterBreathingSkillComponent.getComponentType());
        }

        commandBuffer.addComponent(
            ref,
            WaterBreathingSkillComponent.getComponentType(),
            upgradedComponent
        );

        player.sendMessage(
            Message.raw("Water Breathing skill upgraded to ULTIMATE! Unlimited oxygen underwater!")
        );
    }
}
