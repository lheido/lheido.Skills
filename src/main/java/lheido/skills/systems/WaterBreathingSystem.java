package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.WaterBreathingSkillComponent;

/**
 * Système ECS qui gère la logique du skill WaterBreathing.
 *
 * Ce système ajoute un modificateur multiplicatif sur la stat Oxygen du joueur
 * en fonction du niveau du skill:
 * - Niveau A (1): +50% oxygen (multiplier 1.5x)
 * - Niveau B (2): +100% oxygen (multiplier 2.0x)
 * - Niveau C (3): +200% oxygen (multiplier 3.0x)
 * - Niveau X (4): Oxygen max très élevé (simule l'illimité)
 *
 * Le modificateur est appliqué une seule fois lors de l'initialisation
 * et mis à jour si le niveau change.
 */
public class WaterBreathingSystem extends EntityTickingSystem<EntityStore> {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Identifiant unique du modifier pour ce skill.
     * Permet de le retrouver et le mettre à jour/supprimer.
     */
    private static final String MODIFIER_ID = "lheido_water_breathing";

    /**
     * Valeur multiplicative très haute pour simuler l'oxygène illimité (niveau X).
     */
    private static final float UNLIMITED_MULTIPLIER = 1000.0f;

    public WaterBreathingSystem() {
        super();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return WaterBreathingSkillComponent.getComponentType();
    }

    @Override
    public void tick(
        float deltaTime,
        int entityIndex,
        ArchetypeChunk<EntityStore> chunk,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);

        WaterBreathingSkillComponent waterBreathingComponent =
            commandBuffer.getComponent(
                entityRef,
                WaterBreathingSkillComponent.getComponentType()
            );
        if (waterBreathingComponent == null) {
            return;
        }

        Player player = commandBuffer.getComponent(
            entityRef,
            Player.getComponentType()
        );
        if (player == null) {
            return;
        }

        // Récupérer le ComponentType pour EntityStatMap depuis le module
        ComponentType<EntityStore, EntityStatMap> statMapType =
            EntityStatsModule.get().getEntityStatMapComponentType();

        EntityStatMap statMap = commandBuffer.getComponent(
            entityRef,
            statMapType
        );
        if (statMap == null) {
            LOGGER.atWarning().log(
                "WaterBreathingSystem: EntityStatMap is null for player"
            );
            return;
        }

        // Récupérer l'index de la stat Oxygen
        int oxygenStatIndex = DefaultEntityStatTypes.getOxygen();
        if (oxygenStatIndex < 0) {
            LOGGER.atWarning().log(
                "WaterBreathingSystem: Oxygen stat index not found"
            );
            return;
        }

        // Calculer le multiplicateur effectif
        float effectiveMultiplier = waterBreathingComponent.isUnlimitedOxygen()
            ? UNLIMITED_MULTIPLIER
            : waterBreathingComponent.getOxygenMultiplier();

        // Vérifier si le modifier existe déjà et s'il a la bonne valeur
        // On utilise putModifier qui remplace le modifier existant s'il existe
        applyOxygenModifier(statMap, oxygenStatIndex, effectiveMultiplier);
    }

    /**
     * Applique ou met à jour le modificateur d'oxygène sur le joueur.
     *
     * @param statMap L'EntityStatMap du joueur
     * @param oxygenStatIndex L'index de la stat Oxygen
     * @param multiplier Le multiplicateur à appliquer
     */
    private void applyOxygenModifier(
        EntityStatMap statMap,
        int oxygenStatIndex,
        float multiplier
    ) {
        // Créer un modifier multiplicatif sur l'oxygène max
        // StaticModifier avec CalculationType.MULTIPLICATIVE multiplie la valeur max
        StaticModifier oxygenModifier = new StaticModifier(
            Modifier.ModifierTarget.MAX,
            StaticModifier.CalculationType.MULTIPLICATIVE,
            multiplier
        );

        // putModifier remplace automatiquement si un modifier avec le même ID existe
        statMap.putModifier(
            EntityStatMap.Predictable.NONE,
            oxygenStatIndex,
            MODIFIER_ID,
            oxygenModifier
        );
        // Si previousModifier != null, le modifier existait déjà et a été remplacé
    }
}
