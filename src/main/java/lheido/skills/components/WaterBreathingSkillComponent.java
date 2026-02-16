package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component pour le skill WaterBreathing.
 * Attaché au player pour gérer la respiration sous-marine.
 *
 * Niveaux:
 * - A (1): +50% oxygen duration (multiplier 1.5)
 * - B (2): +100% oxygen duration (multiplier 2.0)
 * - C (3): +200% oxygen duration (multiplier 3.0)
 * - X (4): Unlimited oxygen (no consumption)
 */
public class WaterBreathingSkillComponent implements Component<EntityStore> {

    // ============================================
    // Constantes par niveau - Multiplicateur d'oxygène
    // ============================================

    public static final float LEVEL_A_MULTIPLIER = 1.5f;
    public static final float LEVEL_B_MULTIPLIER = 2.0f;
    public static final float LEVEL_C_MULTIPLIER = 3.0f;
    public static final float LEVEL_X_MULTIPLIER = -1.0f; // -1 = illimité

    public static final int LEVEL_X = 4;

    /**
     * ComponentType pour accéder à ce component dans l'ECS.
     */
    private static volatile ComponentType<EntityStore, WaterBreathingSkillComponent> COMPONENT_TYPE;

    /**
     * Codec pour la sérialisation/désérialisation du component.
     */
    public static final BuilderCodec<WaterBreathingSkillComponent> CODEC =
        BuilderCodec.builder(WaterBreathingSkillComponent.class, WaterBreathingSkillComponent::new)
            .append(
                new KeyedCodec<>("WaterBreathingLevel", Codec.INTEGER),
                (data, value) -> data.level = value,
                data -> data.level
            )
            .add()
            .append(
                new KeyedCodec<>("OxygenMultiplier", Codec.FLOAT),
                (data, value) -> data.oxygenMultiplier = value,
                data -> data.oxygenMultiplier
            )
            .add()
            .build();

    // ============================================
    // Configuration
    // ============================================

    private int level;
    private float oxygenMultiplier;

    public WaterBreathingSkillComponent() {
        this.level = 1;
        this.oxygenMultiplier = LEVEL_A_MULTIPLIER;
    }

    // ============================================
    // ComponentType
    // ============================================

    public static void setComponentType(
        ComponentType<EntityStore, WaterBreathingSkillComponent> componentType
    ) {
        COMPONENT_TYPE = componentType;
    }

    public static ComponentType<EntityStore, WaterBreathingSkillComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    // ============================================
    // Factory Methods
    // ============================================

    public static WaterBreathingSkillComponent createLevelA() {
        WaterBreathingSkillComponent component = new WaterBreathingSkillComponent();
        component.setLevel(1);
        component.setOxygenMultiplier(LEVEL_A_MULTIPLIER);
        return component;
    }

    public static WaterBreathingSkillComponent createLevelB() {
        WaterBreathingSkillComponent component = new WaterBreathingSkillComponent();
        component.setLevel(2);
        component.setOxygenMultiplier(LEVEL_B_MULTIPLIER);
        return component;
    }

    public static WaterBreathingSkillComponent createLevelC() {
        WaterBreathingSkillComponent component = new WaterBreathingSkillComponent();
        component.setLevel(3);
        component.setOxygenMultiplier(LEVEL_C_MULTIPLIER);
        return component;
    }

    public static WaterBreathingSkillComponent createLevelX() {
        WaterBreathingSkillComponent component = new WaterBreathingSkillComponent();
        component.setLevel(LEVEL_X);
        component.setOxygenMultiplier(LEVEL_X_MULTIPLIER);
        return component;
    }

    public static WaterBreathingSkillComponent createForLevel(int level) {
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
    // Queries
    // ============================================

    /**
     * Vérifie si l'oxygène est illimité (niveau X).
     */
    public boolean isUnlimitedOxygen() {
        return oxygenMultiplier < 0;
    }

    // ============================================
    // Clone
    // ============================================

    @Override
    public WaterBreathingSkillComponent clone() {
        WaterBreathingSkillComponent copy = new WaterBreathingSkillComponent();
        copy.level = this.level;
        copy.oxygenMultiplier = this.oxygenMultiplier;
        return copy;
    }

    // ============================================
    // Getters et Setters
    // ============================================

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public float getOxygenMultiplier() {
        return oxygenMultiplier;
    }

    public void setOxygenMultiplier(float oxygenMultiplier) {
        this.oxygenMultiplier = oxygenMultiplier;
    }
}
