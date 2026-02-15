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
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.utils.MovementUtils;

/**
 * Interaction pour débloquer le skill Flying.
 * Quand l'item est utilisé :
 * - Ajoute le FlyingSkillComponent au joueur
 * - Active canFly pour permettre le double-espace
 * Le FlyingSystem gère ensuite toute la logique de vol/cooldown.
 */
public class SkillFlyingInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillFlyingInteraction> CODEC =
        BuilderCodec.builder(
            SkillFlyingInteraction.class,
            SkillFlyingInteraction::new,
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
                "SkillFlyingInteraction: CommandBuffer is null"
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
            LOGGER.atWarning().log("SkillFlyingInteraction: Player is null");
            return;
        }

        // Créer le component avec les paramètres du niveau A
        FlyingSkillComponent component = FlyingSkillComponent.createLevelA();

        // Ajouter le component au joueur
        commandBuffer.addComponent(
            ref,
            FlyingSkillComponent.getComponentType(),
            component
        );

        // Activer canFly pour permettre le double-espace
        PlayerRef playerRef = commandBuffer.getComponent(
            ref,
            PlayerRef.getComponentType()
        );
        MovementManager movementManager = commandBuffer.getComponent(
            ref,
            MovementManager.getComponentType()
        );

        if (playerRef != null && movementManager != null) {
            MovementUtils.setCanFly(
                movementManager,
                playerRef.getPacketHandler(),
                true
            );
        }

        player.sendMessage(
            Message.raw("Flying skill unlocked! Double-tap space to fly.")
        );

        LOGGER.atInfo().log("Player unlocked Flying skill");
    }
}
