package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component for the Flying skill.
 * Attached to the player to manage flight state via a state machine.
 *
 * States:
 * - READY: The player can fly (double-space available)
 * - FLYING: Flight timer active (the player can fly/land freely)
 * - COOLDOWN: The skill is on cooldown (timer active, decremented each tick)
 *
 * Behavior: Once the flight timer is started, it continues to decrement
 * whether the player is in the air or on the ground. The cooldown starts only
 * when the flight timer reaches 0.
 */
public class FlyingSkillComponent implements Component<EntityStore> {

    // ============================================
    // Default constants (Level A)
    // ============================================

    public static final long DEFAULT_FLY_DURATION_MS = 10_000L;
    public static final long DEFAULT_COOLDOWN_MS = 20_000L;

    // ============================================
    // Level B constants
    // ============================================

    public static final long LEVEL_B_FLY_DURATION_MS = 15_000L;
    public static final long LEVEL_B_COOLDOWN_MS = 18_000L;

    // ============================================
    // Level C constants
    // ============================================

    public static final long LEVEL_C_FLY_DURATION_MS = 20_000L;
    public static final long LEVEL_C_COOLDOWN_MS = 15_000L;

    // ============================================
    // Level X constants (Ultimate - Unlimited flight)
    // ============================================

    public static final int LEVEL_X = 4;
    public static final long LEVEL_X_FLY_DURATION_MS = -1L; // -1 = unlimited
    public static final long LEVEL_X_COOLDOWN_MS = 0L;

    /**
     * ComponentType to access this component in the ECS.
     */
    private static volatile ComponentType<
        EntityStore,
        FlyingSkillComponent
    > COMPONENT_TYPE;

    /**
     * Codec for component serialization/deserialization.
     * Persists: level, flyDurationMs, cooldownMs, state, remainingFlyTimeMs, remainingCooldownMs
     */
    public static final BuilderCodec<FlyingSkillComponent> CODEC =
        BuilderCodec.builder(
            FlyingSkillComponent.class,
            FlyingSkillComponent::new
        )
            .append(
                new KeyedCodec<>("Level", Codec.INTEGER),
                (data, value) -> data.level = value,
                data -> data.level
            )
            .add()
            .append(
                new KeyedCodec<>("FlyDurationMs", Codec.LONG),
                (data, value) -> data.flyDurationMs = value,
                data -> data.flyDurationMs
            )
            .add()
            .append(
                new KeyedCodec<>("CooldownMs", Codec.LONG),
                (data, value) -> data.cooldownMs = value,
                data -> data.cooldownMs
            )
            .add()
            .append(
                new KeyedCodec<>("State", Codec.STRING),
                (data, value) -> data.state = FlyingState.valueOf(value),
                data -> data.state.name()
            )
            .add()
            .append(
                new KeyedCodec<>("RemainingFlyTimeMs", Codec.LONG),
                (data, value) -> data.remainingFlyTimeMs = value,
                data -> data.remainingFlyTimeMs
            )
            .add()
            .append(
                new KeyedCodec<>("RemainingCooldownMs", Codec.LONG),
                (data, value) -> data.remainingCooldownMs = value,
                data -> data.remainingCooldownMs
            )
            .add()
            .build();

    // ============================================
    // State machine state
    // ============================================

    private FlyingState state;

    /**
     * Remaining flight time (in milliseconds).
     * Continues to decrement even if the player is on the ground.
     */
    private long remainingFlyTimeMs;

    /**
     * Remaining cooldown time (in milliseconds).
     */
    private long remainingCooldownMs;

    // ============================================
    // Configuration
    // ============================================

    private long flyDurationMs;
    private long cooldownMs;
    private int level;

    // ============================================
    // Resync tracking (transient, not persisted)
    // ============================================

    /**
     * Counter to force periodic resync.
     * Reset to 0 after each forced resync.
     * Not persisted as it is a transient state.
     */
    private transient float resyncAccumulatorSeconds = 0f;

    /**
     * Interval between each forced resync (in seconds).
     * A forced resync sends the packet even if the server value is correct,
     * which fixes client desync (bed wakeup, etc.).
     */
    private static final float FORCE_RESYNC_INTERVAL_SECONDS = 2.0f;

    public FlyingSkillComponent() {
        this.state = FlyingState.READY;
        this.remainingFlyTimeMs = 0L;
        this.remainingCooldownMs = 0L;
        this.flyDurationMs = DEFAULT_FLY_DURATION_MS;
        this.cooldownMs = DEFAULT_COOLDOWN_MS;
        this.level = 1;
    }

    // ============================================
    // ComponentType
    // ============================================

    public static void setComponentType(
        ComponentType<EntityStore, FlyingSkillComponent> componentType
    ) {
        COMPONENT_TYPE = componentType;
    }

    public static ComponentType<
        EntityStore,
        FlyingSkillComponent
    > getComponentType() {
        return COMPONENT_TYPE;
    }

    // ============================================
    // Factory Methods
    // ============================================

    public static FlyingSkillComponent createLevelA() {
        FlyingSkillComponent component = new FlyingSkillComponent();
        component.setLevel(1);
        component.setFlyDurationMs(DEFAULT_FLY_DURATION_MS);
        component.setCooldownMs(DEFAULT_COOLDOWN_MS);
        return component;
    }

    public static FlyingSkillComponent createLevelB() {
        FlyingSkillComponent component = new FlyingSkillComponent();
        component.setLevel(2);
        component.setFlyDurationMs(LEVEL_B_FLY_DURATION_MS);
        component.setCooldownMs(LEVEL_B_COOLDOWN_MS);
        return component;
    }

    public static FlyingSkillComponent createLevelC() {
        FlyingSkillComponent component = new FlyingSkillComponent();
        component.setLevel(3);
        component.setFlyDurationMs(LEVEL_C_FLY_DURATION_MS);
        component.setCooldownMs(LEVEL_C_COOLDOWN_MS);
        return component;
    }

    public static FlyingSkillComponent createLevelX() {
        FlyingSkillComponent component = new FlyingSkillComponent();
        component.setLevel(LEVEL_X);
        component.setFlyDurationMs(LEVEL_X_FLY_DURATION_MS);
        component.setCooldownMs(LEVEL_X_COOLDOWN_MS);
        return component;
    }

    public static FlyingSkillComponent createForLevel(int level) {
        return switch (level) {
            case 1 -> createLevelA();
            case 2 -> createLevelB();
            case 3 -> createLevelC();
            case LEVEL_X -> createLevelX();
            default -> throw new IllegalArgumentException(
                "Unsupported skill level: " + level
            );
        };
    }

    // ============================================
    // Timer Management (called by FlyingSystem)
    // ============================================

    /**
     * Decrements the appropriate timer based on the current state.
     * Called by FlyingSystem on each tick.
     *
     * @param deltaTimeSeconds The time elapsed since the last tick (in seconds)
     */
    public void decrementTimer(float deltaTimeSeconds) {
        long deltaMs = (long) (deltaTimeSeconds * 1000f);

        switch (state) {
            case FLYING -> {
                if (!isUnlimitedFlight()) {
                    remainingFlyTimeMs -= deltaMs;
                    if (remainingFlyTimeMs < 0) {
                        remainingFlyTimeMs = 0;
                    }
                }
            }
            case COOLDOWN -> {
                remainingCooldownMs -= deltaMs;
                if (remainingCooldownMs < 0) {
                    remainingCooldownMs = 0;
                }
            }
            case READY -> {
                // Nothing to decrement
            }
        }
    }

    // ============================================
    // State Machine - Transitions
    // ============================================

    /**
     * Transition to FLYING state.
     * The timer is reset to the maximum duration.
     */
    public void transitionToFlying() {
        this.state = FlyingState.FLYING;
        if (!isUnlimitedFlight()) {
            this.remainingFlyTimeMs = flyDurationMs;
        }
    }

    /**
     * Transition to COOLDOWN state.
     * The remaining flight time is always reset (loses any unused time).
     */
    public void transitionToCooldown() {
        this.state = FlyingState.COOLDOWN;
        this.remainingCooldownMs = cooldownMs;
        this.remainingFlyTimeMs = 0L;
    }

    /**
     * Transition to READY state.
     * Called when the cooldown is finished.
     */
    public void transitionToReady() {
        this.state = FlyingState.READY;
        this.remainingCooldownMs = 0L;
    }

    // ============================================
    // State Machine - Queries
    // ============================================

    public FlyingState getState() {
        return state;
    }

    public boolean isReady() {
        return state == FlyingState.READY;
    }

    public boolean isFlying() {
        return state == FlyingState.FLYING;
    }

    public boolean isOnCooldown() {
        return state == FlyingState.COOLDOWN;
    }

    /**
     * Returns the remaining flight time.
     */
    public long getRemainingFlyTimeMs() {
        return remainingFlyTimeMs;
    }

    /**
     * Returns the remaining cooldown time.
     */
    public long getRemainingCooldownMs() {
        return remainingCooldownMs;
    }

    /**
     * Checks if flight is unlimited (level X).
     */
    public boolean isUnlimitedFlight() {
        return flyDurationMs < 0;
    }

    /**
     * Checks if the flight timer has expired.
     * Always returns false if flight is unlimited.
     */
    public boolean isFlyTimeExpired() {
        if (isUnlimitedFlight()) {
            return false;
        }
        return state == FlyingState.FLYING && remainingFlyTimeMs <= 0;
    }

    /**
     * Checks if the cooldown has expired.
     */
    public boolean isCooldownExpired() {
        return isOnCooldown() && remainingCooldownMs <= 0;
    }

    /**
     * Returns true if canFly should be enabled for the current state.
     */
    public boolean shouldCanFly() {
        return state != FlyingState.COOLDOWN;
    }

    /**
     * Checks if the player has remaining flight time from a previous flight.
     */
    public boolean hasRemainingFlyTime() {
        return remainingFlyTimeMs > 0 || isUnlimitedFlight();
    }

    // ============================================
    // Resync Management
    // ============================================

    /**
     * Accumulates elapsed time for periodic resync.
     * Returns true if a forced resync should be performed.
     *
     * @param deltaTimeSeconds The time elapsed since the last tick
     * @return true if a forced resync is needed
     */
    public boolean accumulateResyncTime(float deltaTimeSeconds) {
        resyncAccumulatorSeconds += deltaTimeSeconds;
        if (resyncAccumulatorSeconds >= FORCE_RESYNC_INTERVAL_SECONDS) {
            resyncAccumulatorSeconds = 0f;
            return true;
        }
        return false;
    }

    /**
     * Resets the resync counter.
     * Called after a forced resync or when the skill changes state.
     */
    public void resetResyncAccumulator() {
        resyncAccumulatorSeconds = 0f;
    }

    // ============================================
    // Clone
    // ============================================

    @Override
    public FlyingSkillComponent clone() {
        FlyingSkillComponent copy = new FlyingSkillComponent();
        copy.state = this.state;
        copy.remainingFlyTimeMs = this.remainingFlyTimeMs;
        copy.remainingCooldownMs = this.remainingCooldownMs;
        copy.flyDurationMs = this.flyDurationMs;
        copy.cooldownMs = this.cooldownMs;
        copy.level = this.level;
        return copy;
    }

    // ============================================
    // Getters and Setters
    // ============================================

    public long getFlyDurationMs() {
        return flyDurationMs;
    }

    public void setFlyDurationMs(long flyDurationMs) {
        this.flyDurationMs = flyDurationMs;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public void setCooldownMs(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
