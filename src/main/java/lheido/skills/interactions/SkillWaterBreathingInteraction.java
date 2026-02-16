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
import lheido.skills.components.WaterBreathingSkillComponent;

/**
 * Interaction pour débloquer le skill WaterBreathing.
 * Quand l'item est utilisé :
 * - Ajoute le WaterBreathingSkillComponent au joueur
 * Le WaterBreathingSystem gère ensuite la logique d'oxygène.
 */
public class SkillWaterBreathingInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillWaterBreathingInteraction> CODEC =
        BuilderCodec.builder(
            SkillWaterBreathingInteraction.class,
            SkillWaterBreathingInteraction::new,
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
                "SkillWaterBreathingInteraction: CommandBuffer is null"
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
            LOGGER.atWarning().log("SkillWaterBreathingInteraction: Player is null");
            return;
        }

        // Créer le component avec les paramètres du niveau A
        WaterBreathingSkillComponent component = WaterBreathingSkillComponent.createLevelA();

        // Ajouter le component au joueur
        commandBuffer.addComponent(
            ref,
            WaterBreathingSkillComponent.getComponentType(),
            component
        );

        player.sendMessage(
            Message.raw("Water Breathing skill unlocked! +50% oxygen duration underwater.")
        );

        LOGGER.atInfo().log("Player unlocked Water Breathing skill");
    }
}
