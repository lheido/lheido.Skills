package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component pour le skill Stamina.
 * Attaché au player pour gérer l'endurance.
 *
 * Niveaux:
 * - A (1): +50% stamina max (multiplier 1.5)
 * - B (2): +100% stamina max (multiplier 2.0)
 * - C (3): +200% stamina max (multiplier 3.0)
 * - X (4): Unlimited stamina
 */
public class StaminaSkillComponent implements Component<EntityStore> {

    // ============================================
    // Constantes par niveau - Multiplicateur de stamina
    // ============================================

    public static final float LEVEL_A_MULTIPLIER = 1.5f;
    public static final float LEVEL_B_MULTIPLIER = 2.0f;
    public static final float LEVEL_C_MULTIPLIER = 3.0f;
    public static final float LEVEL_X_MULTIPLIER = -1.0f; // -1 = illimité

    public static final int LEVEL_X = 4;

    /**
     * ComponentType pour accéder à ce component dans l'ECS.
     */
    private static volatile ComponentType<EntityStore, StaminaSkillComponent> COMPONENT_TYPE;

    /**
     * Codec pour la sérialisation/désérialisation du component.
     */
    public static final BuilderCodec<StaminaSkillComponent> CODEC =
        BuilderCodec.builder(StaminaSkillComponent.class, StaminaSkillComponent::new)
            .append(
                new KeyedCodec<>("StaminaLevel", Codec.INTEGER),
                (data, value) -> data.level = value,
                data -> data.level
            )
            .add()
            .append(
                new KeyedCodec<>("StaminaMultiplier", Codec.FLOAT),
                (data, value) -> data.staminaMultiplier = value,
                data -> data.staminaMultiplier
            )
            .add()
            .build();

    // ============================================
    // Configuration
    // ============================================

    private int level;
    private float staminaMultiplier;

    public StaminaSkillComponent() {
        this.level = 1;
        this.staminaMultiplier = LEVEL_A_MULTIPLIER;
    }

    // ============================================
    // ComponentType
    // ============================================

    public static void setComponentType(
        ComponentType<EntityStore, StaminaSkillComponent> componentType
    ) {
        COMPONENT_TYPE = componentType;
    }

    public static ComponentType<EntityStore, StaminaSkillComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    // ============================================
    // Factory Methods
    // ============================================

    public static StaminaSkillComponent createLevelA() {
        StaminaSkillComponent component = new StaminaSkillComponent();
        component.setLevel(1);
        component.setStaminaMultiplier(LEVEL_A_MULTIPLIER);
        return component;
    }

    public static StaminaSkillComponent createLevelB() {
        StaminaSkillComponent component = new StaminaSkillComponent();
        component.setLevel(2);
        component.setStaminaMultiplier(LEVEL_B_MULTIPLIER);
        return component;
    }

    public static StaminaSkillComponent createLevelC() {
        StaminaSkillComponent component = new StaminaSkillComponent();
        component.setLevel(3);
        component.setStaminaMultiplier(LEVEL_C_MULTIPLIER);
        return component;
    }

    public static StaminaSkillComponent createLevelX() {
        StaminaSkillComponent component = new StaminaSkillComponent();
        component.setLevel(LEVEL_X);
        component.setStaminaMultiplier(LEVEL_X_MULTIPLIER);
        return component;
    }

    public static StaminaSkillComponent createForLevel(int level) {
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
     * Vérifie si la stamina est illimitée (niveau X).
     */
    public boolean isUnlimitedStamina() {
        return staminaMultiplier < 0;
    }

    // ============================================
    // Clone
    // ============================================

    @Override
    public StaminaSkillComponent clone() {
        StaminaSkillComponent copy = new StaminaSkillComponent();
        copy.level = this.level;
        copy.staminaMultiplier = this.staminaMultiplier;
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

    public float getStaminaMultiplier() {
        return staminaMultiplier;
    }

    public void setStaminaMultiplier(float staminaMultiplier) {
        this.staminaMultiplier = staminaMultiplier;
    }
}
