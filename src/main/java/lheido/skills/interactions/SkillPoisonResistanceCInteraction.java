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
import lheido.skills.components.PoisonResistanceSkillComponent;

/**
 * Interaction pour upgrader le skill PoisonResistance vers le niveau C.
 * Requiert que le joueur possède déjà le skill PoisonResistance niveau B.
 * Améliore la résistance au poison (75% au lieu de 50%).
 */
public class SkillPoisonResistanceCInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillPoisonResistanceCInteraction> CODEC =
        BuilderCodec.builder(
            SkillPoisonResistanceCInteraction.class,
            SkillPoisonResistanceCInteraction::new,
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

        // Les prérequis sont vérifiés par CheckPoisonResistanceUpgradeInteraction
        // Récupérer le component existant (garanti par le check)
        PoisonResistanceSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            PoisonResistanceSkillComponent.getComponentType()
        );

        // Upgrade vers le niveau C
        PoisonResistanceSkillComponent upgradedComponent = PoisonResistanceSkillComponent.createLevelC();

        // Supprimer l'ancien et ajouter le nouveau
        if (existingComponent != null) {
            commandBuffer.removeComponent(ref, PoisonResistanceSkillComponent.getComponentType());
        }

        commandBuffer.addComponent(
            ref,
            PoisonResistanceSkillComponent.getComponentType(),
            upgradedComponent
        );

        player.sendMessage(
            Message.raw("Poison Resistance skill upgraded to level 3! 75% poison damage reduction.")
        );
    }
}
