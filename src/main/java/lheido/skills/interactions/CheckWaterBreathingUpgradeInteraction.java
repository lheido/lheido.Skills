package lheido.skills.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
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
 * Interaction pour vérifier les prérequis d'upgrade du skill WaterBreathing.
 * Paramétrable via JSON avec RequiredLevel et TargetLevel.
 *
 * - RequiredLevel: Le niveau minimum requis (0 = aucun prérequis, pour WaterBreathing A)
 * - TargetLevel: Le niveau vers lequel on upgrade
 *
 * Échoue si:
 * - Le joueur n'a pas le niveau requis
 * - Le joueur a déjà le niveau cible ou supérieur
 */
public class CheckWaterBreathingUpgradeInteraction
    extends SimpleInstantInteraction
{

    public static final BuilderCodec<
        CheckWaterBreathingUpgradeInteraction
    > CODEC = BuilderCodec.builder(
        CheckWaterBreathingUpgradeInteraction.class,
        CheckWaterBreathingUpgradeInteraction::new,
        SimpleInstantInteraction.CODEC
    )
        .append(
            new KeyedCodec<>("RequiredLevel", Codec.INTEGER),
            (data, value) -> data.requiredLevel = value,
            data -> data.requiredLevel
        )
        .add()
        .append(
            new KeyedCodec<>("TargetLevel", Codec.INTEGER),
            (data, value) -> data.targetLevel = value,
            data -> data.targetLevel
        )
        .add()
        .build();

    private int requiredLevel = 0;
    private int targetLevel = 1;

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

        // Récupérer le component WaterBreathing existant (peut être null)
        WaterBreathingSkillComponent existingComponent =
            commandBuffer.getComponent(
                ref,
                WaterBreathingSkillComponent.getComponentType()
            );

        int currentLevel =
            existingComponent != null ? existingComponent.getLevel() : 0;

        // Vérifier si le joueur a le niveau requis
        if (currentLevel < requiredLevel) {
            String message =
                requiredLevel == 0
                    ? "You cannot use this skill!"
                    : "You must have Water Breathing (" +
                      requiredLevel +
                      ") before upgrading!";
            player.sendMessage(Message.raw(message));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Vérifier si le joueur a déjà le niveau cible ou supérieur
        if (currentLevel >= targetLevel) {
            String message =
                targetLevel == 4
                    ? "You already have the ultimate Water Breathing skill!"
                    : "You already have Water Breathing level " +
                      targetLevel +
                      " or higher!";
            player.sendMessage(Message.raw(message));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Prérequis validés, l'interaction suivante peut s'exécuter
    }
}
