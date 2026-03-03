package lheido.skills.interactions;

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
import lheido.skills.components.FlyingSkillComponent;

/**
 * Interaction pour upgrader le skill Flying vers le niveau C.
 * Requiert que le joueur possède déjà le skill Flying niveau B.
 * Améliore la durée de vol (20s) et réduit le cooldown (15s).
 */
public class SkillFlyingCInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillFlyingCInteraction> CODEC =
        BuilderCodec.builder(
            SkillFlyingCInteraction.class,
            SkillFlyingCInteraction::new,
            SimpleInstantInteraction.CODEC
        ).build();

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
            LOGGER.atWarning().log(
                "SkillFlyingCInteraction: CommandBuffer is null"
            );
            return;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(
            ref,
            Player.getComponentType()
        );
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("SkillFlyingCInteraction: Player is null");
            return;
        }

        // Les prérequis sont vérifiés par CheckFlyingUpgradeInteraction
        // Récupérer le component existant (garanti par le check)
        FlyingSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            FlyingSkillComponent.getComponentType()
        );

        // Upgrade vers le niveau C
        FlyingSkillComponent upgradedComponent = FlyingSkillComponent.createLevelC();

        // Supprimer l'ancien component s'il existe
        // Note: On ne conserve pas l'état car l'upgrade donne de nouveaux paramètres
        if (existingComponent != null) {
            commandBuffer.removeComponent(ref, FlyingSkillComponent.getComponentType());
        }

        commandBuffer.addComponent(
            ref,
            FlyingSkillComponent.getComponentType(),
            upgradedComponent
        );

        player.sendMessage(
            Message.raw("Flying skill upgraded to level 3! Fly duration: 20s, Cooldown: 15s")
        );
    }
}
