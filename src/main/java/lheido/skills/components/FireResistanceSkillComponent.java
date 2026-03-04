package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component pour le skill FireResistance.
 * Attaché au player pour gérer la résistance au feu.
 *
 * Niveaux:
 * - A (1): 25% de réduction des dégâts de feu
 * - B (2): 50% de réduction des dégâts de feu
 * - C (3): 75% de réduction des dégâts de feu
 * - X (4): Immunité totale au feu
 */
public class FireResistanceSkillComponent implements Component<EntityStore> {

    // ============================================
    // Constantes par niveau - Multiplicateur de résistance
    // ============================================

    public static final float LEVEL_A_RESISTANCE = 0.25f;
    public static final float LEVEL_B_RESISTANCE = 0.50f;
    public static final float LEVEL_C_RESISTANCE = 0.75f;
    public static final float LEVEL_X_RESISTANCE = 1.0f; // 1.0 = immunité totale

    public static final int LEVEL_X = 4;

    /**
     * ComponentType pour accéder à ce component dans l'ECS.
     */
    private static volatile ComponentType<EntityStore, FireResistanceSkillComponent> COMPONENT_TYPE;

    /**
     * Codec pour la sérialisation/désérialisation du component.
     */
    public static final BuilderCodec<FireResistanceSkillComponent> CODEC =
        BuilderCodec.builder(FireResistanceSkillComponent.class, FireResistanceSkillComponent::new)
            .append(
                new KeyedCodec<>("FireResistanceLevel", Codec.INTEGER),
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

    public FireResistanceSkillComponent() {
        this.level = 1;
        this.resistanceMultiplier = LEVEL_A_RESISTANCE;
    }

    // ============================================
    // ComponentType
    // ============================================

    public static void setComponentType(
        ComponentType<EntityStore, FireResistanceSkillComponent> componentType
    ) {
        COMPONENT_TYPE = componentType;
    }

    public static ComponentType<EntityStore, FireResistanceSkillComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    // ============================================
    // Factory Methods
    // ============================================

    public static FireResistanceSkillComponent createLevelA() {
        FireResistanceSkillComponent component = new FireResistanceSkillComponent();
        component.setLevel(1);
        component.setResistanceMultiplier(LEVEL_A_RESISTANCE);
        return component;
    }

    public static FireResistanceSkillComponent createLevelB() {
        FireResistanceSkillComponent component = new FireResistanceSkillComponent();
        component.setLevel(2);
        component.setResistanceMultiplier(LEVEL_B_RESISTANCE);
        return component;
    }

    public static FireResistanceSkillComponent createLevelC() {
        FireResistanceSkillComponent component = new FireResistanceSkillComponent();
        component.setLevel(3);
        component.setResistanceMultiplier(LEVEL_C_RESISTANCE);
        return component;
    }

    public static FireResistanceSkillComponent createLevelX() {
        FireResistanceSkillComponent component = new FireResistanceSkillComponent();
        component.setLevel(LEVEL_X);
        component.setResistanceMultiplier(LEVEL_X_RESISTANCE);
        return component;
    }

    public static FireResistanceSkillComponent createForLevel(int level) {
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
     * Vérifie si le joueur est immunisé au feu (niveau X).
     */
    public boolean isImmuneToFire() {
        return resistanceMultiplier >= 1.0f;
    }

    /**
     * Calcule les dégâts de feu après réduction.
     * @param originalDamage Les dégâts de feu originaux
     * @return Les dégâts après application de la résistance
     */
    public float calculateReducedFireDamage(float originalDamage) {
        if (isImmuneToFire()) {
            return 0f;
        }
        return originalDamage * (1.0f - resistanceMultiplier);
    }

    // ============================================
    // Clone
    // ============================================

    @Override
    public FireResistanceSkillComponent clone() {
        FireResistanceSkillComponent copy = new FireResistanceSkillComponent();
        copy.level = this.level;
        copy.resistanceMultiplier = this.resistanceMultiplier;
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

    public float getResistanceMultiplier() {
        return resistanceMultiplier;
    }

    public void setResistanceMultiplier(float resistanceMultiplier) {
        this.resistanceMultiplier = resistanceMultiplier;
    }
}
