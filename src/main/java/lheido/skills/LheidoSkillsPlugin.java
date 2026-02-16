package lheido.skills;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.interactions.CheckFlyingUpgradeInteraction;
import lheido.skills.interactions.SkillFlyingBInteraction;
import lheido.skills.interactions.SkillFlyingCInteraction;
import lheido.skills.interactions.SkillFlyingInteraction;
import lheido.skills.interactions.SkillFlyingXInteraction;
import lheido.skills.systems.FlyingSystem;
import lheido.skills.systems.SkillEssenceDropSystem;
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
    }
}
