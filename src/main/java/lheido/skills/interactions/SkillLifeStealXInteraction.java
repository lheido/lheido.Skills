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
import lheido.skills.components.LifeStealSkillComponent;

/**
 * Interaction pour upgrader le skill LifeSteal vers le niveau X (maximum).
 * Requiert que le joueur possède déjà le skill LifeSteal niveau C.
 * Améliore le vol de vie (25% - niveau maximum).
 */
public class SkillLifeStealXInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillLifeStealXInteraction> CODEC =
        BuilderCodec.builder(
            SkillLifeStealXInteraction.class,
            SkillLifeStealXInteraction::new,
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

        // Les prérequis sont vérifiés par CheckLifeStealUpgradeInteraction
        // Récupérer le component existant (garanti par le check)
        LifeStealSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            LifeStealSkillComponent.getComponentType()
        );

        // Upgrade vers le niveau X (maximum)
        LifeStealSkillComponent upgradedComponent = LifeStealSkillComponent.createLevelX();

        // Supprimer l'ancien et ajouter le nouveau
        if (existingComponent != null) {
            commandBuffer.removeComponent(ref, LifeStealSkillComponent.getComponentType());
        }

        commandBuffer.addComponent(
            ref,
            LifeStealSkillComponent.getComponentType(),
            upgradedComponent
        );

        player.sendMessage(
            Message.raw("Life Steal skill maxed out! You recover 25% of damage dealt as health.")
        );
    }
}
