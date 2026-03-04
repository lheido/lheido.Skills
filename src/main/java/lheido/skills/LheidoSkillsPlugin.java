package lheido.skills;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.commands.SkillsCommand;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.components.StaminaSkillComponent;
import lheido.skills.components.WaterBreathingSkillComponent;
import lheido.skills.interactions.CheckFlyingUpgradeInteraction;
import lheido.skills.interactions.OpenSkillSelectionInteraction;
import lheido.skills.interactions.CheckStaminaUpgradeInteraction;
import lheido.skills.interactions.CheckWaterBreathingUpgradeInteraction;
import lheido.skills.interactions.SkillFlyingBInteraction;
import lheido.skills.interactions.SkillFlyingCInteraction;
import lheido.skills.interactions.SkillFlyingInteraction;
import lheido.skills.interactions.SkillFlyingXInteraction;
import lheido.skills.interactions.SkillStaminaBInteraction;
import lheido.skills.interactions.SkillStaminaCInteraction;
import lheido.skills.interactions.SkillStaminaInteraction;
import lheido.skills.interactions.SkillStaminaXInteraction;
import lheido.skills.interactions.SkillWaterBreathingBInteraction;
import lheido.skills.interactions.SkillWaterBreathingCInteraction;
import lheido.skills.interactions.SkillWaterBreathingInteraction;
import lheido.skills.interactions.SkillWaterBreathingXInteraction;
import lheido.skills.systems.FlyingSystem;
import lheido.skills.systems.SkillEssenceDropSystem;
import lheido.skills.systems.StaminaSystem;
import lheido.skills.systems.WaterBreathingSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Main class for the Lheido Skills plugin.
 */
public class LheidoSkillsPlugin extends JavaPlugin {

    /**
     * Constructs a new instance of the Lheido Skills plugin.
     * @param init The initialization data for the plugin.
     */
    public LheidoSkillsPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    /**
     * Sets up the plugin. This method is called when the plugin is enabled.
     * Registers all skill components, interactions, and systems.
     */
    @Override
    protected void setup() {
        // Register Flying Skill Interaction
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_flying",
            SkillFlyingInteraction.class,
            SkillFlyingInteraction.CODEC
        );

        // Register Flying Skill B Interaction (upgrade)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_flying_b",
            SkillFlyingBInteraction.class,
            SkillFlyingBInteraction.CODEC
        );

        // Register Flying Skill C Interaction (upgrade)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_flying_c",
            SkillFlyingCInteraction.class,
            SkillFlyingCInteraction.CODEC
        );

        // Register Flying Skill X Interaction (ultimate upgrade)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_flying_x",
            SkillFlyingXInteraction.class,
            SkillFlyingXInteraction.CODEC
        );

        // Register Check Flying Upgrade Interaction (prerequisite checker)
        this.getCodecRegistry(Interaction.CODEC).register(
            "check_flying_upgrade",
            CheckFlyingUpgradeInteraction.class,
            CheckFlyingUpgradeInteraction.CODEC
        );

        // Register Flying Skill Component with Codec for persistence
        ComponentType<EntityStore, FlyingSkillComponent> flyingComponentType =
            this.getEntityStoreRegistry().registerComponent(
                FlyingSkillComponent.class,
                "FlyingSkillComponent",
                FlyingSkillComponent.CODEC
            );
        FlyingSkillComponent.setComponentType(flyingComponentType);

        // Register Flying System
        this.getEntityStoreRegistry().registerSystem(new FlyingSystem());

        // Register Skill Essence Drop System (drops essence when NPCs die)
        this.getEntityStoreRegistry().registerSystem(
            new SkillEssenceDropSystem()
        );

        // ============================================
        // Water Breathing Skill
        // ============================================

        // Register Water Breathing Skill Interaction (level A)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_water_breathing",
            SkillWaterBreathingInteraction.class,
            SkillWaterBreathingInteraction.CODEC
        );

        // Register Water Breathing Skill B Interaction (upgrade)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_water_breathing_b",
            SkillWaterBreathingBInteraction.class,
            SkillWaterBreathingBInteraction.CODEC
        );

        // Register Water Breathing Skill C Interaction (upgrade)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_water_breathing_c",
            SkillWaterBreathingCInteraction.class,
            SkillWaterBreathingCInteraction.CODEC
        );

        // Register Water Breathing Skill X Interaction (ultimate upgrade)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_water_breathing_x",
            SkillWaterBreathingXInteraction.class,
            SkillWaterBreathingXInteraction.CODEC
        );

        // Register Check Water Breathing Upgrade Interaction (prerequisite checker)
        this.getCodecRegistry(Interaction.CODEC).register(
            "check_water_breathing_upgrade",
            CheckWaterBreathingUpgradeInteraction.class,
            CheckWaterBreathingUpgradeInteraction.CODEC
        );

        // Register Water Breathing Skill Component with Codec for persistence
        ComponentType<EntityStore, WaterBreathingSkillComponent> waterBreathingComponentType =
            this.getEntityStoreRegistry().registerComponent(
                WaterBreathingSkillComponent.class,
                "WaterBreathingSkillComponent",
                WaterBreathingSkillComponent.CODEC
            );
        WaterBreathingSkillComponent.setComponentType(waterBreathingComponentType);

        // Register Water Breathing System
        this.getEntityStoreRegistry().registerSystem(new WaterBreathingSystem());

        // ============================================
        // Stamina Skill
        // ============================================

        // Register Stamina Skill Interaction (level A)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_stamina",
            SkillStaminaInteraction.class,
            SkillStaminaInteraction.CODEC
        );

        // Register Stamina Skill B Interaction (upgrade)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_stamina_b",
            SkillStaminaBInteraction.class,
            SkillStaminaBInteraction.CODEC
        );

        // Register Stamina Skill C Interaction (upgrade)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_stamina_c",
            SkillStaminaCInteraction.class,
            SkillStaminaCInteraction.CODEC
        );

        // Register Stamina Skill X Interaction (ultimate upgrade)
        this.getCodecRegistry(Interaction.CODEC).register(
            "skill_stamina_x",
            SkillStaminaXInteraction.class,
            SkillStaminaXInteraction.CODEC
        );

        // Register Check Stamina Upgrade Interaction (prerequisite checker)
        this.getCodecRegistry(Interaction.CODEC).register(
            "check_stamina_upgrade",
            CheckStaminaUpgradeInteraction.class,
            CheckStaminaUpgradeInteraction.CODEC
        );

        // Register Stamina Skill Component with Codec for persistence
        ComponentType<EntityStore, StaminaSkillComponent> staminaComponentType =
            this.getEntityStoreRegistry().registerComponent(
                StaminaSkillComponent.class,
                "StaminaSkillComponent",
                StaminaSkillComponent.CODEC
            );
        StaminaSkillComponent.setComponentType(staminaComponentType);

        // Register Stamina System
        this.getEntityStoreRegistry().registerSystem(new StaminaSystem());

        // ============================================
        // Active Skills Selection System
        // ============================================

        // Register Open Skill Selection Interaction
        this.getCodecRegistry(Interaction.CODEC).register(
            "open_skill_selection",
            OpenSkillSelectionInteraction.class,
            OpenSkillSelectionInteraction.CODEC
        );

        // Register Active Skills Component with Codec for persistence
        ComponentType<EntityStore, ActiveSkillsComponent> activeSkillsComponentType =
            this.getEntityStoreRegistry().registerComponent(
                ActiveSkillsComponent.class,
                "ActiveSkillsComponent",
                ActiveSkillsComponent.CODEC
            );
        ActiveSkillsComponent.setComponentType(activeSkillsComponentType);

        // ============================================
        // Commands
        // ============================================

        // Register /skills command
        this.getCommandRegistry().registerCommand(new SkillsCommand());
    }
}
