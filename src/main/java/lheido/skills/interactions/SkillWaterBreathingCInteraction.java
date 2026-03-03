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
 * Interaction pour upgrader le skill WaterBreathing vers le niveau C.
 * Requiert que le joueur possède déjà le skill WaterBreathing niveau B.
 * Améliore le multiplicateur d'oxygène (+200%).
 */
public class SkillWaterBreathingCInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillWaterBreathingCInteraction> CODEC =
        BuilderCodec.builder(
            SkillWaterBreathingCInteraction.class,
            SkillWaterBreathingCInteraction::new,
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
        // Récupérer le component existant (garanti par le check)
        WaterBreathingSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            WaterBreathingSkillComponent.getComponentType()
        );

        // Upgrade vers le niveau C
        WaterBreathingSkillComponent upgradedComponent = WaterBreathingSkillComponent.createLevelC();

        // Supprimer l'ancien et ajouter le nouveau
        if (existingComponent != null) {
            commandBuffer.removeComponent(ref, WaterBreathingSkillComponent.getComponentType());
        }

        commandBuffer.addComponent(
            ref,
            WaterBreathingSkillComponent.getComponentType(),
            upgradedComponent
        );

        player.sendMessage(
            Message.raw("Water Breathing skill upgraded to level 3! +200% oxygen duration.")
        );
    }
}
