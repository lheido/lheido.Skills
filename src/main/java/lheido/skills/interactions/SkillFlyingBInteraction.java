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
 * Interaction pour upgrader le skill Flying vers le niveau B.
 * Requiert que le joueur possède déjà le skill Flying niveau A.
 * Améliore la durée de vol (15s au lieu de 10s) et réduit le cooldown (18s au lieu de 20s).
 */
public class SkillFlyingBInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillFlyingBInteraction> CODEC =
        BuilderCodec.builder(
            SkillFlyingBInteraction.class,
            SkillFlyingBInteraction::new,
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
                "SkillFlyingBInteraction: CommandBuffer is null"
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
            LOGGER.atWarning().log("SkillFlyingBInteraction: Player is null");
            return;
        }

        // Vérifier si le joueur possède déjà le skill Flying
        FlyingSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            FlyingSkillComponent.getComponentType()
        );

        if (existingComponent == null) {
            // Le joueur n'a pas le skill Flying, il doit d'abord débloquer le niveau A
            player.sendMessage(
                Message.raw("You must unlock Flying (1) first before upgrading!")
            );
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atInfo().log(
                "Player tried to upgrade Flying to level B without having level A"
            );
            return;
        }

        if (existingComponent.getLevel() >= 2) {
            // Le joueur a déjà le niveau B ou supérieur
            player.sendMessage(
                Message.raw("You already have Flying level 2 or higher!")
            );
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atInfo().log(
                "Player tried to upgrade Flying to level B but already has level " 
                + existingComponent.getLevel()
            );
            return;
        }

        // Upgrade vers le niveau B - mettre à jour le component existant
        FlyingSkillComponent upgradedComponent = FlyingSkillComponent.createLevelB();
        
        // Conserver l'état actuel si le joueur était en vol ou en cooldown
        if (existingComponent.isFlying()) {
            upgradedComponent.transitionToFlying();
        } else if (existingComponent.isOnCooldown()) {
            upgradedComponent.transitionToCooldown();
        }

        // Remplacer le component
        commandBuffer.removeComponent(ref, FlyingSkillComponent.getComponentType());
        commandBuffer.addComponent(
            ref,
            FlyingSkillComponent.getComponentType(),
            upgradedComponent
        );

        player.sendMessage(
            Message.raw("Flying skill upgraded to level 2! Fly duration: 15s, Cooldown: 18s")
        );

        LOGGER.atInfo().log("Player upgraded Flying skill to level B");
    }
}
