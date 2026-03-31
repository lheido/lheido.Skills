package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeferredCorpseRemoval;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.systems.NPCDamageSystems;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lheido.skills.components.PendingSkillEssenceComponent;

/**
 * Ticking system that drops Skill Essence after the death animation
 * completes, mirroring the timing of the native
 * {@code NPCDamageSystems$DropDeathItems} system.
 *
 * <p>Each tick, for every entity that has both a
 * {@link PendingSkillEssenceComponent} and a {@link DeathComponent}, the
 * system checks whether it is time to drop:</p>
 * <ul>
 *   <li>If the role says {@code dropDeathItemsInstantly} → drop now.</li>
 *   <li>Otherwise wait until {@link DeferredCorpseRemoval#shouldRemove()}
 *       returns {@code true} (death animation finished).</li>
 * </ul>
 *
 * <p>Once dropped, the component is flagged so the item is never
 * duplicated.</p>
 *
 * <h3>Registration</h3>
 * Registered in {@code LheidoSkillsPlugin.setup()}.
 */
public class SkillEssenceDropTickSystem
    extends EntityTickingSystem<EntityStore>
{

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * The item ID for Skill Essence ingredient.
     * Must match the JSON file name: {@code Ingredient_Skill_Essence.json}.
     */
    private static final String SKILL_ESSENCE_ITEM_ID =
        "Ingredient_Skill_Essence";

    // ── Query & dependencies ────────────────────────────────────────────

    /**
     * Matches non-player entities that are dead and carry a pending
     * Skill Essence drop.
     */
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PendingSkillEssenceComponent.getComponentType(),
            DeathComponent.getComponentType(),
            NPCEntity.getComponentType(),
            Query.not(Player.getComponentType())
        );
    }

    /**
     * Run after the native NPC drop system so we stay consistent with
     * the engine's drop ordering.
     */
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(
            new SystemDependency<>(
                Order.AFTER,
                NPCDamageSystems.DropDeathItems.class
            )
        );
    }

    // ── Tick ─────────────────────────────────────────────────────────────

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        PendingSkillEssenceComponent pending = chunk.getComponent(
            index,
            PendingSkillEssenceComponent.getComponentType()
        );

        // Already processed — nothing to do.
        if (pending == null || pending.isDropped()) {
            return;
        }

        // Check whether the NPC's role requests an instant drop or
        // whether we should wait for the death animation to finish.
        NPCEntity npcEntity = chunk.getComponent(
            index,
            NPCEntity.getComponentType()
        );
        if (npcEntity == null) {
            return;
        }

        boolean shouldDropNow = false;

        Role role = npcEntity.getRole();
        if (role != null && role.isDropDeathItemsInstantly()) {
            shouldDropNow = true;
        } else {
            // Wait for the corpse-removal timer (death animation end).
            DeferredCorpseRemoval dcr = chunk.getComponent(
                index,
                DeferredCorpseRemoval.getComponentType()
            );
            if (dcr != null && dcr.shouldRemove()) {
                shouldDropNow = true;
            }
        }

        if (!shouldDropNow) {
            return;
        }

        // Mark as dropped to prevent duplicates on subsequent ticks.
        pending.setDropped();

        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        try {
            ItemStack essenceStack = new ItemStack(
                SKILL_ESSENCE_ITEM_ID,
                pending.getQuantity()
            );

            ItemUtils.dropItem(ref, essenceStack, commandBuffer);
        } catch (Exception e) {
            LOGGER.atWarning().log(
                "SkillEssenceDropTickSystem: Failed to drop Skill Essence - " +
                    e.getMessage()
            );
        }
    }
}
