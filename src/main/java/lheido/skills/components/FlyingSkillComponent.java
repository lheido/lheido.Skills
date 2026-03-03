package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component pour le skill Flying.
 * Attaché au player pour gérer l'état du vol via une state machine.
 *
 * États:
 * - READY: Le joueur peut voler (double-espace disponible)
 * - FLYING: Timer de vol actif (le joueur peut voler/atterrir librement)
 * - COOLDOWN: Le skill est en cooldown (timer actif, décrémenté chaque tick)
 *
 * Comportement: Une fois le timer de vol démarré, il continue à décrémenter
 * que le joueur soit en l'air ou au sol. Le cooldown commence uniquement
 * quand le timer de vol atteint 0.
 */
public class FlyingSkillComponent implements Component<EntityStore> {

    // ============================================
    // Constantes par défaut (Niveau A)
    // ============================================

    public static final long DEFAULT_FLY_DURATION_MS = 10_000L;
    public static final long DEFAULT_COOLDOWN_MS = 20_000L;

    // ============================================
    // Constantes Niveau B
    // ============================================

    public static final long LEVEL_B_FLY_DURATION_MS = 15_000L;
    public static final long LEVEL_B_COOLDOWN_MS = 18_000L;

    // ============================================
    // Constantes Niveau C
    // ============================================

    public static final long LEVEL_C_FLY_DURATION_MS = 20_000L;
    public static final long LEVEL_C_COOLDOWN_MS = 15_000L;

    // ============================================
    // Constantes Niveau X (Ultime - Vol illimité)
    // ============================================

    public static final int LEVEL_X = 4;
    public static final long LEVEL_X_FLY_DURATION_MS = -1L; // -1 = illimité
    public static final long LEVEL_X_COOLDOWN_MS = 0L;

    /**
     * ComponentType pour accéder à ce component dans l'ECS.
     */
    private static volatile ComponentType<EntityStore, FlyingSkillComponent> COMPONENT_TYPE;

    /**
     * Codec pour la sérialisation/désérialisation du component.
     * Persiste: level, flyDurationMs, cooldownMs, state, remainingFlyTimeMs, remainingCooldownMs
     */
    public static final BuilderCodec<FlyingSkillComponent> CODEC =
        BuilderCodec.builder(FlyingSkillComponent.class, FlyingSkillComponent::new)
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
    // État de la state machine
    // ============================================

    private FlyingState state;
    
    /**
     * Temps de vol restant (en millisecondes).
     * Continue à décrémenter même si le joueur est au sol.
     */
    private long remainingFlyTimeMs;
    
    /**
     * Temps de cooldown restant (en millisecondes).
     */
    private long remainingCooldownMs;

    // ============================================
    // Configuration
    // ============================================

    private long flyDurationMs;
    private long cooldownMs;
    private int level;

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

    public static ComponentType<EntityStore, FlyingSkillComponent> getComponentType() {
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
    // Timer Management (appelé par FlyingSystem)
    // ============================================

    /**
     * Décrémente le timer approprié en fonction de l'état actuel.
     * Appelé par FlyingSystem à chaque tick.
     * 
     * @param deltaTimeSeconds Le temps écoulé depuis le dernier tick (en secondes)
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
                // Rien à décrémenter
            }
        }
    }

    // ============================================
    // State Machine - Transitions
    // ============================================

    /**
     * Transition vers l'état FLYING.
     * Le timer est réinitialisé à la durée maximale.
     */
    public void transitionToFlying() {
        this.state = FlyingState.FLYING;
        if (!isUnlimitedFlight()) {
            this.remainingFlyTimeMs = flyDurationMs;
        }
    }

    /**
     * Transition vers l'état COOLDOWN.
     * Le temps de vol restant est toujours réinitialisé (perd tout temps non utilisé).
     */
    public void transitionToCooldown() {
        this.state = FlyingState.COOLDOWN;
        this.remainingCooldownMs = cooldownMs;
        this.remainingFlyTimeMs = 0L;
    }

    /**
     * Transition vers l'état READY.
     * Appelé quand le cooldown est terminé.
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
     * Retourne le temps de vol restant.
     */
    public long getRemainingFlyTimeMs() {
        return remainingFlyTimeMs;
    }

    /**
     * Retourne le temps de cooldown restant.
     */
    public long getRemainingCooldownMs() {
        return remainingCooldownMs;
    }

    /**
     * Vérifie si le vol est illimité (niveau X).
     */
    public boolean isUnlimitedFlight() {
        return flyDurationMs < 0;
    }

    /**
     * Vérifie si le timer de vol est expiré.
     * Retourne toujours false si le vol est illimité.
     */
    public boolean isFlyTimeExpired() {
        if (isUnlimitedFlight()) {
            return false;
        }
        return state == FlyingState.FLYING && remainingFlyTimeMs <= 0;
    }

    /**
     * Vérifie si le cooldown est expiré.
     */
    public boolean isCooldownExpired() {
        return isOnCooldown() && remainingCooldownMs <= 0;
    }

    /**
     * Retourne true si canFly devrait être activé pour l'état actuel.
     */
    public boolean shouldCanFly() {
        return state != FlyingState.COOLDOWN;
    }

    /**
     * Vérifie si le joueur a du temps de vol restant d'un vol précédent.
     */
    public boolean hasRemainingFlyTime() {
        return remainingFlyTimeMs > 0 || isUnlimitedFlight();
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
    // Getters et Setters
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
