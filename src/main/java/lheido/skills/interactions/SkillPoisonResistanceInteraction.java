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
 * Interaction pour débloquer le skill PoisonResistance.
 * Quand l'item est utilisé :
 * - Ajoute le PoisonResistanceSkillComponent au joueur
 */
public class SkillPoisonResistanceInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillPoisonResistanceInteraction> CODEC =
        BuilderCodec.builder(
            SkillPoisonResistanceInteraction.class,
            SkillPoisonResistanceInteraction::new,
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

        // Créer le component avec les paramètres du niveau A
        PoisonResistanceSkillComponent component = PoisonResistanceSkillComponent.createLevelA();

        // Ajouter le component au joueur
        commandBuffer.addComponent(
            ref,
            PoisonResistanceSkillComponent.getComponentType(),
            component
        );

        player.sendMessage(
            Message.raw("Poison Resistance skill unlocked! 25% poison damage reduction.")
        );
    }
}
