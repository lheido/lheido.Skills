package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component pour le skill LifeSteal (Vampirisme).
 * Attaché au player pour gérer le vol de vie lors des attaques.
 *
 * Niveaux:
 * - A (1): 5% des dégâts infligés récupérés en vie
 * - B (2): 10% des dégâts infligés récupérés en vie
 * - C (3): 15% des dégâts infligés récupérés en vie
 * - X (4): 25% des dégâts infligés récupérés en vie
 */
public class LifeStealSkillComponent implements Component<EntityStore> {

    // ============================================
    // Constantes par niveau - Pourcentage de vol de vie
    // ============================================

    public static final float LEVEL_A_PERCENTAGE = 0.05f;  // 5%
    public static final float LEVEL_B_PERCENTAGE = 0.10f;  // 10%
    public static final float LEVEL_C_PERCENTAGE = 0.15f;  // 15%
    public static final float LEVEL_X_PERCENTAGE = 0.25f;  // 25%

    public static final int LEVEL_X = 4;

    /**
     * ComponentType pour accéder à ce component dans l'ECS.
     */
    private static volatile ComponentType<EntityStore, LifeStealSkillComponent> COMPONENT_TYPE;

    /**
     * Codec pour la sérialisation/désérialisation du component.
     */
    public static final BuilderCodec<LifeStealSkillComponent> CODEC =
        BuilderCodec.builder(LifeStealSkillComponent.class, LifeStealSkillComponent::new)
            .append(
                new KeyedCodec<>("LifeStealLevel", Codec.INTEGER),
                (data, value) -> data.level = value,
                data -> data.level
            )
            .add()
            .append(
                new KeyedCodec<>("LifeStealPercentage", Codec.FLOAT),
                (data, value) -> data.lifeStealPercentage = value,
                data -> data.lifeStealPercentage
            )
            .add()
            .build();

    // ============================================
    // Configuration
    // ============================================

    private int level;
    private float lifeStealPercentage;

    public LifeStealSkillComponent() {
        this.level = 1;
        this.lifeStealPercentage = LEVEL_A_PERCENTAGE;
    }

    // ============================================
    // ComponentType
    // ============================================

    public static void setComponentType(
        ComponentType<EntityStore, LifeStealSkillComponent> componentType
    ) {
        COMPONENT_TYPE = componentType;
    }

    public static ComponentType<EntityStore, LifeStealSkillComponent> getComponentType() {
        return COMPONENT_TYPE;
    }

    // ============================================
    // Factory Methods
    // ============================================

    public static LifeStealSkillComponent createLevelA() {
        LifeStealSkillComponent component = new LifeStealSkillComponent();
        component.setLevel(1);
        component.setLifeStealPercentage(LEVEL_A_PERCENTAGE);
        return component;
    }

    public static LifeStealSkillComponent createLevelB() {
        LifeStealSkillComponent component = new LifeStealSkillComponent();
        component.setLevel(2);
        component.setLifeStealPercentage(LEVEL_B_PERCENTAGE);
        return component;
    }

    public static LifeStealSkillComponent createLevelC() {
        LifeStealSkillComponent component = new LifeStealSkillComponent();
        component.setLevel(3);
        component.setLifeStealPercentage(LEVEL_C_PERCENTAGE);
        return component;
    }

    public static LifeStealSkillComponent createLevelX() {
        LifeStealSkillComponent component = new LifeStealSkillComponent();
        component.setLevel(LEVEL_X);
        component.setLifeStealPercentage(LEVEL_X_PERCENTAGE);
        return component;
    }

    public static LifeStealSkillComponent createForLevel(int level) {
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
    // Calculs
    // ============================================

    /**
     * Calcule le montant de vie à récupérer en fonction des dégâts infligés.
     * @param damageDealt Les dégâts infligés à la cible
     * @return Le montant de vie à récupérer
     */
    public float calculateHealAmount(float damageDealt) {
        return damageDealt * lifeStealPercentage;
    }

    // ============================================
    // Clone
    // ============================================

    @Override
    public LifeStealSkillComponent clone() {
        LifeStealSkillComponent copy = new LifeStealSkillComponent();
        copy.level = this.level;
        copy.lifeStealPercentage = this.lifeStealPercentage;
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

    public float getLifeStealPercentage() {
        return lifeStealPercentage;
    }

    public void setLifeStealPercentage(float lifeStealPercentage) {
        this.lifeStealPercentage = lifeStealPercentage;
    }
}
