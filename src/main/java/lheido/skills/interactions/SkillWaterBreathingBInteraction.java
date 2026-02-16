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
 * Interaction pour upgrader le skill WaterBreathing vers le niveau B.
 * Requiert que le joueur possède déjà le skill WaterBreathing niveau A.
 * Améliore le multiplicateur d'oxygène (+100% au lieu de +50%).
 */
public class SkillWaterBreathingBInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<SkillWaterBreathingBInteraction> CODEC =
        BuilderCodec.builder(
            SkillWaterBreathingBInteraction.class,
            SkillWaterBreathingBInteraction::new,
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
                "SkillWaterBreathingBInteraction: CommandBuffer is null"
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
            LOGGER.atWarning().log("SkillWaterBreathingBInteraction: Player is null");
            return;
        }

        // Les prérequis sont vérifiés par CheckWaterBreathingUpgradeInteraction
        // Récupérer le component existant (garanti par le check)
        WaterBreathingSkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            WaterBreathingSkillComponent.getComponentType()
        );

        // Upgrade vers le niveau B
        WaterBreathingSkillComponent upgradedComponent = WaterBreathingSkillComponent.createLevelB();

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
            Message.raw("Water Breathing skill upgraded to level 2! +100% oxygen duration.")
        );

        LOGGER.atInfo().log("Player upgraded Water Breathing skill to level B");
    }
}
