package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.PoisonResistanceSkillComponent;
import lheido.skills.utils.SkillIds;

/**
 * ECS system that handles poison resistance.
 *
 * This system intercepts damage events (Damage) and:
 * - Reduces poison damage based on the skill level (levels A-C)
 * - Completely cancels poison damage for level X (immunity)
 *
 * The system runs in the FilterDamageGroup to intercept damage
 * before it is applied to health.
 *
 * Damage types considered as poison:
 * - DamageCause whose ID contains "poison", "venom", or "toxic"
 */
public class PoisonResistanceSystem
    extends EntityEventSystem<EntityStore, Damage>
{

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public PoisonResistanceSystem() {
        super(Damage.class);
    }

    @Override
    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Damage damageEvent
    ) {
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);

        // Check if the entity has the poison resistance skill
        PoisonResistanceSkillComponent resistanceComponent = store.getComponent(
            entityRef,
            PoisonResistanceSkillComponent.getComponentType()
        );
        if (resistanceComponent == null) {
            return;
        }

        // Check if the skill is active
        ActiveSkillsComponent activeSkills = store.getComponent(
            entityRef,
            ActiveSkillsComponent.getComponentType()
        );
        if (!isSkillActiveForPlayer(activeSkills)) {
            return;
        }

        // Check if this is poison damage
        DamageCause cause = damageEvent.getCause();
        if (!isPoisonDamage(cause)) {
            return;
        }

        // Apply the resistance
        if (resistanceComponent.isImmuneToPosion()) {
            // Level X: Total immunity - cancel the damage
            damageEvent.setCancelled(true);

            // Log for debug (optional)
            Player player = store.getComponent(
                entityRef,
                Player.getComponentType()
            );
            if (player != null) {
                LOGGER.atFine().log(
                    "PoisonResistanceSystem: Poison damage cancelled for player (immunity)"
                );
            }
        } else {
            // Levels A-C: Reduce the damage
            float originalAmount = damageEvent.getAmount();
            float reducedAmount =
                resistanceComponent.calculateReducedPoisonDamage(
                    originalAmount
                );

            // Use setAmount if available, otherwise cancel if damage <= 0
            if (reducedAmount <= 0) {
                damageEvent.setCancelled(true);
            } else {
                // Attempt to modify the damage amount
                // Note: If setAmount doesn't exist, we can only cancel
                try {
                    damageEvent.setAmount(reducedAmount);

                    LOGGER.atFine().log(
                        "PoisonResistanceSystem: Poison damage reduced from " +
                            originalAmount +
                            " to " +
                            reducedAmount
                    );
                } catch (Exception e) {
                    // If setAmount is not available, log the error
                    LOGGER.atWarning().log(
                        "PoisonResistanceSystem: Could not set damage amount - " +
                            e.getMessage()
                    );
                }
            }
        }
    }

    /**
     * Checks if the PoisonResistance skill is active for the player.
     */
    private boolean isSkillActiveForPlayer(ActiveSkillsComponent activeSkills) {
        if (activeSkills == null) {
            return false;
        }

        for (String activeSkill : activeSkills.getActiveSkills()) {
            if (SkillIds.isPoisonResistanceSkill(activeSkill)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the damage cause is poison.
     *
     * Types considered as poison:
     * - Any DamageCause whose ID contains "poison", "venom", or "toxic"
     *
     * Note: This list can be extended based on the game's needs.
     * DamageCause is an asset, so we use getId() to retrieve the identifier.
     */
    private boolean isPoisonDamage(DamageCause cause) {
        if (cause == null) {
            return false;
        }

        String causeId = cause.getId();
        if (causeId == null) {
            return false;
        }

        // Check the different possible poison causes
        String lowerCauseId = causeId.toLowerCase();
        return (
            lowerCauseId.contains("poison") ||
            lowerCauseId.contains("venom") ||
            lowerCauseId.contains("toxic")
        );
    }

    /**
     * Places this system in the FilterDamageGroup to intercept damage
     * before it is applied to health.
     */
    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    /**
     * Query for entities that have the PoisonResistanceSkillComponent.
     */
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PoisonResistanceSkillComponent.getComponentType();
    }
}
