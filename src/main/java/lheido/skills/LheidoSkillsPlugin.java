package lheido.skills;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
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
     * This is where you can register event listeners, commands, and perform any necessary initialization.
     *
     * Example:
     *
     * CommandRegistry commandRegistry = this.getCommandRegistry();
     * commandRegistry.registerCommand(new FirstCommand());
     *
     * this.getEntityStoreRegistry().registerComponent(
     *     MyComponent.class,
     *     MyComponent::new
     * );
     *
     * this.getEntityStoreRegistry().registerSystem(new MySystem());
     *
     */
    @Override
    protected void setup() {}
}
