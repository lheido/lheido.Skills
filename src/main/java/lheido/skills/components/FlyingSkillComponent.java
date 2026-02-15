package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component pour le skill Flying.
 * Attaché au player pour gérer l'état du vol.
 */
public class FlyingSkillComponent implements Component<EntityStore> {

    // ============================================
    // Constantes par défaut (Niveau A)
    // ============================================

    /**
     * Durée de vol par défaut en millisecondes (10 secondes).
     */
    public static final long DEFAULT_FLY_DURATION_MS = 10_000L;

    /**
     * Cooldown par défaut en millisecondes (20 secondes).
     */
    public static final long DEFAULT_COOLDOWN_MS = 20_000L;

    // ============================================
    // Constantes Niveau B
    // ============================================

    /**
     * Durée de vol niveau B en millisecondes (15 secondes).
     */
    public static final long LEVEL_B_FLY_DURATION_MS = 15_000L;

    /**
     * Cooldown niveau B en millisecondes (18 secondes).
     */
    public static final long LEVEL_B_COOLDOWN_MS = 18_000L;

    // ============================================
    // Constantes Niveau C
    // ============================================

    /**
     * Durée de vol niveau C en millisecondes (20 secondes).
     */
    public static final long LEVEL_C_FLY_DURATION_MS = 20_000L;

    /**
     * Cooldown niveau C en millisecondes (15 secondes).
     */
    public static final long LEVEL_C_COOLDOWN_MS = 15_000L;

    /**
     * ComponentType pour accéder à ce component dans l'ECS.
     * Initialisé lors de l'enregistrement dans le plugin.
     * Volatile pour garantir la visibilité entre threads.
     */
    private static volatile ComponentType<
        EntityStore,
        FlyingSkillComponent
    > COMPONENT_TYPE;

    /**
     * Codec pour la sérialisation/désérialisation du component.
     * Permet la persistance des données quand le joueur se reconnecte.
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
            .build();

    /**
     * Durée du vol en millisecondes (configurable par niveau).
     */
    private long flyDurationMs;

    /**
     * Cooldown en millisecondes (configurable par niveau).
     */
    private long cooldownMs;

    /**
     * Timestamp du début du vol actuel (0 si pas en vol).
     */
    private long flyStartTime;

    /**
     * Timestamp de fin du cooldown (0 si pas de cooldown actif).
     */
    private long cooldownEndTime;

    /**
     * Niveau du skill (A=1, B=2, C=3, etc.).
     */
    private int level;

    public FlyingSkillComponent() {
        this.flyDurationMs = DEFAULT_FLY_DURATION_MS;
        this.cooldownMs = DEFAULT_COOLDOWN_MS;
        this.flyStartTime = 0L;
        this.cooldownEndTime = 0L;
        this.level = 1;
    }

    /**
     * Définit le ComponentType. Appelé lors de l'enregistrement.
     */
    public static void setComponentType(
        ComponentType<EntityStore, FlyingSkillComponent> componentType
    ) {
        COMPONENT_TYPE = componentType;
    }

    /**
     * Retourne le ComponentType pour accéder à ce component dans l'ECS.
     */
    public static ComponentType<
        EntityStore,
        FlyingSkillComponent
    > getComponentType() {
        return COMPONENT_TYPE;
    }

    // ============================================
    // Factory Methods pour chaque niveau
    // ============================================

    /**
     * Crée un component avec les paramètres du niveau A.
     * Durée: 10 secondes, Cooldown: 20 secondes
     */
    public static FlyingSkillComponent createLevelA() {
        FlyingSkillComponent component = new FlyingSkillComponent();
        component.setLevel(1);
        component.setFlyDurationMs(DEFAULT_FLY_DURATION_MS);
        component.setCooldownMs(DEFAULT_COOLDOWN_MS);
        return component;
    }

    /**
     * Crée un component avec les paramètres du niveau B.
     * Durée: 15 secondes, Cooldown: 18 secondes
     */
    public static FlyingSkillComponent createLevelB() {
        FlyingSkillComponent component = new FlyingSkillComponent();
        component.setLevel(2);
        component.setFlyDurationMs(LEVEL_B_FLY_DURATION_MS);
        component.setCooldownMs(LEVEL_B_COOLDOWN_MS);
        return component;
    }

    /**
     * Crée un component avec les paramètres du niveau C.
     * Durée: 20 secondes, Cooldown: 15 secondes
     */
    public static FlyingSkillComponent createLevelC() {
        FlyingSkillComponent component = new FlyingSkillComponent();
        component.setLevel(3);
        component.setFlyDurationMs(LEVEL_C_FLY_DURATION_MS);
        component.setCooldownMs(LEVEL_C_COOLDOWN_MS);
        return component;
    }

    /**
     * Crée un component pour un niveau donné.
     *
     * @param level Le niveau du skill (1=A, 2=B, 3=C)
     * @return Le component configuré pour ce niveau
     * @throws IllegalArgumentException si le niveau n'est pas supporté
     */
    public static FlyingSkillComponent createForLevel(int level) {
        return switch (level) {
            case 1 -> createLevelA();
            case 2 -> createLevelB();
            case 3 -> createLevelC();
            default -> throw new IllegalArgumentException(
                "Unsupported skill level: " + level
            );
        };
    }

    /**
     * Vérifie si le joueur est actuellement en train de voler avec ce skill.
     */
    public boolean isFlying() {
        return flyStartTime > 0;
    }

    /**
     * Vérifie si le skill est en cooldown.
     */
    public boolean isOnCooldown() {
        return cooldownEndTime > System.currentTimeMillis();
    }

    /**
     * Retourne le temps restant de vol en millisecondes.
     */
    public long getRemainingFlyTime() {
        if (!isFlying()) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - flyStartTime;
        return Math.max(0, flyDurationMs - elapsed);
    }

    /**
     * Retourne le temps restant de cooldown en millisecondes.
     */
    public long getRemainingCooldown() {
        if (!isOnCooldown()) {
            return 0;
        }
        return cooldownEndTime - System.currentTimeMillis();
    }

    /**
     * Démarre le vol.
     */
    public void startFlying() {
        this.flyStartTime = System.currentTimeMillis();
    }

    /**
     * Arrête le vol et démarre le cooldown.
     */
    public void stopFlying() {
        this.flyStartTime = 0L;
        this.cooldownEndTime = System.currentTimeMillis() + cooldownMs;
    }

    @Override
    public FlyingSkillComponent clone() {
        FlyingSkillComponent copy = new FlyingSkillComponent();
        copy.flyDurationMs = this.flyDurationMs;
        copy.cooldownMs = this.cooldownMs;
        copy.flyStartTime = this.flyStartTime;
        copy.cooldownEndTime = this.cooldownEndTime;
        copy.level = this.level;
        return copy;
    }

    // Getters et Setters
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

    public long getFlyStartTime() {
        return flyStartTime;
    }

    public void setFlyStartTime(long flyStartTime) {
        this.flyStartTime = flyStartTime;
    }

    public long getCooldownEndTime() {
        return cooldownEndTime;
    }

    public void setCooldownEndTime(long cooldownEndTime) {
        this.cooldownEndTime = cooldownEndTime;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
