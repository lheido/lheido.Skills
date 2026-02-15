package lheido.skills;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.interactions.SkillFlyingInteraction;
import lheido.skills.systems.FlyingSystem;
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
    }
}
