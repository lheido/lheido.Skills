package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.LifeStealSkillComponent;
import lheido.skills.utils.SkillIds;

/**
 * ECS system that handles life steal (Life Steal / Vampirism).
 *
 * This system listens to damage events and heals the attacker
 * by a percentage of the damage dealt if they have the
 * LifeSteal skill active.
 *
 * How it works:
 * - Intercepts damage on any entity
 * - Checks if the damage source is a player with the LifeSteal skill
 * - Heals the attacking player based on their skill level percentage
 *
 * Note: This system runs in the FilterDamageGroup to intercept
 * damage before it is applied.
 */
public class LifeStealSystem extends EntityEventSystem<EntityStore, Damage> {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public LifeStealSystem() {
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
        // Do not process cancelled damage
        if (damageEvent.isCancelled()) {
            return;
        }

        float damageAmount = damageEvent.getAmount();
        if (damageAmount <= 0) {
            return;
        }

        // Get the damage source (the attacker)
        Damage.Source source = damageEvent.getSource();
        if (source == null) {
            return;
        }

        // Get the source entity reference
        // Damage.Source is an interface. For damage dealt by an entity,
        // the source is of type Damage.EntitySource which exposes getRef().
        if (!(source instanceof Damage.EntitySource entitySource)) {
            // If it's not an EntitySource (e.g. environmental damage), ignore
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null) {
            return;
        }

        // Check if the attacker is a player
        Player attackerPlayer = store.getComponent(
            attackerRef,
            Player.getComponentType()
        );
        if (attackerPlayer == null) {
            return;
        }

        // Check if the attacker has the LifeSteal skill
        LifeStealSkillComponent lifeStealComponent = store.getComponent(
            attackerRef,
            LifeStealSkillComponent.getComponentType()
        );
        if (lifeStealComponent == null) {
            return;
        }

        // Check if the skill is active
        ActiveSkillsComponent activeSkills = store.getComponent(
            attackerRef,
            ActiveSkillsComponent.getComponentType()
        );
        if (!isSkillActiveForPlayer(activeSkills)) {
            return;
        }

        // Calculate the heal amount
        float healAmount = lifeStealComponent.calculateHealAmount(damageAmount);
        if (healAmount <= 0) {
            return;
        }

        // Heal the attacker
        healPlayer(attackerRef, healAmount, store);

        LOGGER.atFine().log(
            "LifeStealSystem: Healed player for " +
                healAmount +
                " HP (dealt " +
                damageAmount +
                " damage)"
        );
    }

    /**
     * Checks if the LifeSteal skill is active for the player.
     */
    private boolean isSkillActiveForPlayer(ActiveSkillsComponent activeSkills) {
        if (activeSkills == null) {
            return false;
        }

        for (String activeSkill : activeSkills.getActiveSkills()) {
            if (SkillIds.isLifeStealSkill(activeSkill)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Heals a player by the specified amount.
     *
     * Uses addStatValue to add health points.
     *
     * @param playerRef Reference to the player entity
     * @param healAmount Amount of health to restore
     * @param store The ECS store
     */
    private void healPlayer(
        Ref<EntityStore> playerRef,
        float healAmount,
        Store<EntityStore> store
    ) {
        // Get the player's EntityStatMap
        ComponentType<EntityStore, EntityStatMap> statMapType =
            EntityStatsModule.get().getEntityStatMapComponentType();

        EntityStatMap statMap = store.getComponent(playerRef, statMapType);
        if (statMap == null) {
            LOGGER.atWarning().log(
                "LifeStealSystem: EntityStatMap is null for player"
            );
            return;
        }

        // Get the Health stat index
        int healthIndex = DefaultEntityStatTypes.getHealth();

        // Add health to the player
        // addStatValue adds the specified value to the current health
        // without exceeding the maximum
        statMap.addStatValue(healthIndex, healAmount);
    }

    /**
     * Places this system in the FilterDamageGroup to intercept damage.
     */
    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    /**
     * Query for all entities that have stats (health, etc.).
     *
     * This system runs on ALL entities that can receive damage,
     * then we check if the SOURCE of the damage (the attacker) has the LifeSteal skill.
     *
     * By using EntityStatMapComponentType, we target all entities
     * with a stats system, i.e. all those that can be damaged.
     */
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return EntityStatsModule.get().getEntityStatMapComponentType();
    }
}
