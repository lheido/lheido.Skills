package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component for the PoisonResistance skill.
 * Attached to the player to manage poison resistance.
 *
 * Levels:
 * - A (1): 25% poison damage reduction
 * - B (2): 50% poison damage reduction
 * - C (3): 75% poison damage reduction
 * - X (4): Full immunity to poison
 */
public class PoisonResistanceSkillComponent implements Component<EntityStore> {

    // ============================================
    // Per-level constants - Resistance multiplier
    // ============================================

    public static final float LEVEL_A_RESISTANCE = 0.25f;
    public static final float LEVEL_B_RESISTANCE = 0.50f;
    public static final float LEVEL_C_RESISTANCE = 0.75f;
    public static final float LEVEL_X_RESISTANCE = 1.0f; // 1.0 = full immunity

    public static final int LEVEL_X = 4;

    /**
     * ComponentType to access this component in the ECS.
     */
    private static volatile ComponentType<
        EntityStore,
        PoisonResistanceSkillComponent
    > COMPONENT_TYPE;

    /**
     * Codec for serialization/deserialization of the component.
     */
    public static final BuilderCodec<PoisonResistanceSkillComponent> CODEC =
        BuilderCodec.builder(
            PoisonResistanceSkillComponent.class,
            PoisonResistanceSkillComponent::new
        )
            .append(
                new KeyedCodec<>("PoisonResistanceLevel", Codec.INTEGER),
                (data, value) -> data.level = value,
                data -> data.level
            )
            .add()
            .append(
                new KeyedCodec<>("ResistanceMultiplier", Codec.FLOAT),
                (data, value) -> data.resistanceMultiplier = value,
                data -> data.resistanceMultiplier
            )
            .add()
            .build();

    // ============================================
    // Configuration
    // ============================================

    private int level;
    private float resistanceMultiplier;

    public PoisonResistanceSkillComponent() {
        this.level = 1;
        this.resistanceMultiplier = LEVEL_A_RESISTANCE;
    }

    // ============================================
    // ComponentType
    // ============================================

    public static void setComponentType(
        ComponentType<EntityStore, PoisonResistanceSkillComponent> componentType
    ) {
        COMPONENT_TYPE = componentType;
    }

    public static ComponentType<
        EntityStore,
        PoisonResistanceSkillComponent
    > getComponentType() {
        return COMPONENT_TYPE;
    }

    // ============================================
    // Factory Methods
    // ============================================

    public static PoisonResistanceSkillComponent createLevelA() {
        PoisonResistanceSkillComponent component =
            new PoisonResistanceSkillComponent();
        component.setLevel(1);
        component.setResistanceMultiplier(LEVEL_A_RESISTANCE);
        return component;
    }

    public static PoisonResistanceSkillComponent createLevelB() {
        PoisonResistanceSkillComponent component =
            new PoisonResistanceSkillComponent();
        component.setLevel(2);
        component.setResistanceMultiplier(LEVEL_B_RESISTANCE);
        return component;
    }

    public static PoisonResistanceSkillComponent createLevelC() {
        PoisonResistanceSkillComponent component =
            new PoisonResistanceSkillComponent();
        component.setLevel(3);
        component.setResistanceMultiplier(LEVEL_C_RESISTANCE);
        return component;
    }

    public static PoisonResistanceSkillComponent createLevelX() {
        PoisonResistanceSkillComponent component =
            new PoisonResistanceSkillComponent();
        component.setLevel(LEVEL_X);
        component.setResistanceMultiplier(LEVEL_X_RESISTANCE);
        return component;
    }

    public static PoisonResistanceSkillComponent createForLevel(int level) {
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
     * Checks if the player is immune to poison (level X).
     */
    public boolean isImmuneToPosion() {
        return resistanceMultiplier >= 1.0f;
    }

    /**
     * Calculates poison damage after reduction.
     * @param originalDamage The original poison damage
     * @return The damage after applying resistance
     */
    public float calculateReducedPoisonDamage(float originalDamage) {
        if (isImmuneToPosion()) {
            return 0f;
        }
        return originalDamage * (1.0f - resistanceMultiplier);
    }

    // ============================================
    // Clone
    // ============================================

    @Override
    public PoisonResistanceSkillComponent clone() {
        PoisonResistanceSkillComponent copy =
            new PoisonResistanceSkillComponent();
        copy.level = this.level;
        copy.resistanceMultiplier = this.resistanceMultiplier;
        return copy;
    }

    // ============================================
    // Getters and Setters
    // ============================================

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public float getResistanceMultiplier() {
        return resistanceMultiplier;
    }

    public void setResistanceMultiplier(float resistanceMultiplier) {
        this.resistanceMultiplier = resistanceMultiplier;
    }
}
