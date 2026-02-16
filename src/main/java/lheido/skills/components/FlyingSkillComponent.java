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
 * - FLYING: Le joueur vole (timer actif)
 * - COOLDOWN: Le skill est en cooldown
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
                new KeyedCodec<>("WasFlying", Codec.BOOLEAN),
                (data, value) -> data.wasFlying = value,
                data -> data.wasFlying
            )
            .add()
            .build();

    // ============================================
    // État de la state machine
    // ============================================

    private FlyingState state;
    private long stateStartTime;
    
    /**
     * Flag persisté indiquant si le joueur était en vol lors de la sauvegarde.
     * Utilisé pour restaurer l'état de vol après reconnexion (Flying X).
     */
    private boolean wasFlying = false;
    
    /**
     * Flag indiquant si le component vient d'être chargé (après reconnexion).
     * Utilisé pour forcer la synchronisation de canFly avec le client.
     */
    private transient boolean needsResync = true;

    // ============================================
    // Configuration
    // ============================================

    private long flyDurationMs;
    private long cooldownMs;
    private int level;

    public FlyingSkillComponent() {
        this.state = FlyingState.READY;
        this.stateStartTime = 0L;
        this.flyDurationMs = DEFAULT_FLY_DURATION_MS;
        this.cooldownMs = DEFAULT_COOLDOWN_MS;
        this.level = 1;
        this.needsResync = true; // Force resync après chargement
    }

    // ============================================
    // Resync Management
    // ============================================

    /**
     * Vérifie si le component nécessite une resynchronisation avec le client.
     * @return true si une resync est nécessaire
     */
    public boolean needsResync() {
        return needsResync;
    }

    /**
     * Marque le component comme synchronisé.
     */
    public void markSynced() {
        this.needsResync = false;
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
    // State Machine - Transitions
    // ============================================

    /**
     * Transition vers l'état FLYING.
     * Appelé quand le joueur fait double-espace.
     */
    public void transitionToFlying() {
        this.state = FlyingState.FLYING;
        this.stateStartTime = System.currentTimeMillis();
        this.wasFlying = true;
    }

    /**
     * Transition vers l'état COOLDOWN.
     * Appelé quand le timer de vol est terminé.
     */
    public void transitionToCooldown() {
        this.state = FlyingState.COOLDOWN;
        this.stateStartTime = System.currentTimeMillis();
        this.wasFlying = false;
    }

    /**
     * Transition vers l'état READY.
     * Appelé quand le cooldown est terminé.
     */
    public void transitionToReady() {
        this.state = FlyingState.READY;
        this.stateStartTime = 0L;
        this.wasFlying = false;
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
     * Vérifie si le joueur était en vol lors de la dernière sauvegarde.
     * Utilisé pour restaurer l'état de vol après reconnexion.
     * @return true si le joueur était en vol
     */
    public boolean wasFlying() {
        return wasFlying;
    }

    /**
     * Retourne le temps écoulé dans l'état actuel.
     */
    public long getTimeInCurrentState() {
        if (stateStartTime == 0L) {
            return 0L;
        }
        return System.currentTimeMillis() - stateStartTime;
    }

    /**
     * Retourne le temps restant de vol (0 si pas en vol).
     * Retourne Long.MAX_VALUE si le vol est illimité.
     */
    public long getRemainingFlyTime() {
        if (!isFlying()) {
            return 0L;
        }
        if (isUnlimitedFlight()) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, flyDurationMs - getTimeInCurrentState());
    }

    /**
     * Retourne le temps restant de cooldown (0 si pas en cooldown).
     */
    public long getRemainingCooldown() {
        if (!isOnCooldown()) {
            return 0L;
        }
        return Math.max(0L, cooldownMs - getTimeInCurrentState());
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
        return isFlying() && getRemainingFlyTime() <= 0;
    }

    /**
     * Vérifie si le cooldown est expiré.
     */
    public boolean isCooldownExpired() {
        return isOnCooldown() && getRemainingCooldown() <= 0;
    }

    // ============================================
    // Clone
    // ============================================

    @Override
    public FlyingSkillComponent clone() {
        FlyingSkillComponent copy = new FlyingSkillComponent();
        copy.state = this.state;
        copy.stateStartTime = this.stateStartTime;
        copy.flyDurationMs = this.flyDurationMs;
        copy.cooldownMs = this.cooldownMs;
        copy.level = this.level;
        copy.wasFlying = this.wasFlying;
        copy.needsResync = false; // Le clone n'a pas besoin de resync
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
