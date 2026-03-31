package lheido.skills.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import javax.annotation.Nonnull;
import lheido.skills.components.PendingSkillEssenceComponent;

/**
 * Death-time system that attaches a {@link PendingSkillEssenceComponent} to
 * every hostile/neutral NPC when it dies.
 *
 * <p>The component stores the pre-calculated Skill Essence quantity (based on
 * the NPC's {@code maxHealth} and flying state). A companion
 * {@link SkillEssenceDropTickSystem} later reads this component and performs
 * the actual item drop at the correct moment (after the death animation),
 * exactly like the native {@code NPCDamageSystems$DropDeathItems} system.</p>
 *
 * <h3>Registration</h3>
 * Registered in {@code LheidoSkillsPlugin.setup()}.
 */
public class SkillEssenceDropSystem extends DeathSystems.OnDeathSystem {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // ── Drop configuration ──────────────────────────────────────────────

    /**
     * Base minimum quantity of essence to drop.
     */
    private static final int MIN_DROP_QUANTITY = 1;

    /**
     * Health thresholds that each grant +1 additional essence.
     * If an entity's maxHealth &gt;= threshold, quantity increases by 1.
     */
    private static final int[] HEALTH_QUANTITY_THRESHOLDS = {
        25, // weak+         → 2
        50, // medium        → 3
        100, // strong        → 4
        150, // strong+       → 5
        // --- elite gap ---
        200, //               → 6
        215, //               → 7
        230, //               → 8
        245, //               → 9
        260, //               → 10
        275, //               → 11
        290, //               → 12
        305, //               → 13
        320, //               → 14
        335, //               → 15
        350, //               → 16
        365, //               → 17
        380, //               → 18
        395, //               → 19
        400, //               → 20
    };

    /**
     * Extra quantity bonus for flying/gliding entities.
     */
    private static final int FLYING_QUANTITY_BONUS = 1;

    // ── OnDeathSystem wiring ────────────────────────────────────────────

    /**
     * Only target non-player entities.
     */
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.not(Player.getComponentType());
    }

    // ── Callback ────────────────────────────────────────────────────────

    /**
     * Called when a {@link DeathComponent} is added (entity dies).
     * We build a profile, decide whether it qualifies, compute the
     * quantity and attach a {@link PendingSkillEssenceComponent} so
     * the tick system can drop it after the death animation.
     */
    @Override
    public void onComponentAdded(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull DeathComponent deathComponent,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        try {
            MobDropProfile profile = buildProfile(ref, store);

            if (!shouldReceiveEssence(profile)) {
                return;
            }

            int quantity = computeQuantity(profile);

            commandBuffer.addComponent(
                ref,
                PendingSkillEssenceComponent.getComponentType(),
                new PendingSkillEssenceComponent(quantity)
            );
        } catch (Exception e) {
            LOGGER.atWarning().log(
                "SkillEssenceDropSystem: Could not attach pending essence - " +
                    e.getMessage()
            );
        }
    }

    // ── Profile helpers ─────────────────────────────────────────────────

    /**
     * Builds a lightweight profile from the NPC's {@link Role} at death.
     */
    private MobDropProfile buildProfile(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        String playerAttitude = "unknown";
        int maxHealth = 0;
        boolean flying = false;

        NPCEntity npcEntity = store.getComponent(
            ref,
            NPCEntity.getComponentType()
        );
        if (npcEntity == null) {
            return new MobDropProfile(playerAttitude, maxHealth, flying);
        }

        Role role = npcEntity.getRole();
        if (role != null) {
            var worldSupport = role.getWorldSupport();
            if (worldSupport != null) {
                var attitude = worldSupport.getDefaultPlayerAttitude();
                if (attitude != null) {
                    playerAttitude = attitude.name();
                }
            }
            maxHealth = role.getInitialMaxHealth();
        }

        MovementStatesComponent msc = store.getComponent(
            ref,
            MovementStatesComponent.getComponentType()
        );
        if (msc != null) {
            var ms = msc.getMovementStates();
            if (ms != null) {
                flying = ms.flying || ms.gliding;
            }
        }
        if (!flying) {
            flying = npcEntity.getHoverHeight() > 0;
        }

        return new MobDropProfile(playerAttitude, maxHealth, flying);
    }

    /**
     * Only hostile and neutral mobs receive Skill Essence.
     */
    protected boolean shouldReceiveEssence(@Nonnull MobDropProfile profile) {
        return (
            "HOSTILE".equalsIgnoreCase(profile.playerAttitude()) ||
            "NEUTRAL".equalsIgnoreCase(profile.playerAttitude())
        );
    }

    /**
     * Computes the drop quantity based on maxHealth and flying state.
     */
    protected int computeQuantity(@Nonnull MobDropProfile profile) {
        int quantity = MIN_DROP_QUANTITY;

        for (int threshold : HEALTH_QUANTITY_THRESHOLDS) {
            if (profile.maxHealth() >= threshold) {
                quantity++;
            }
        }

        if (profile.flying()) {
            quantity += FLYING_QUANTITY_BONUS;
        }

        return Math.max(quantity, MIN_DROP_QUANTITY);
    }

    // ── Inner record ────────────────────────────────────────────────────

    /**
     * Simplified profile used to determine drop rules.
     */
    private record MobDropProfile(
        @Nonnull String playerAttitude,
        int maxHealth,
        boolean flying
    ) {}
}
