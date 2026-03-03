package lheido.skills.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/**
 * HUD pour afficher le temps de vol restant ou le cooldown du skill Flying.
 * 
 * États possibles:
 * - Rien affiché (READY ou pas de skill)
 * - Timer de vol (FLYING) - affiche le temps restant en vert
 * - Timer de cooldown (COOLDOWN) - affiche le temps restant en orange
 */
public class FlyingSkillHud extends CustomUIHud {

    /**
     * État actuel du HUD.
     */
    public enum HudState {
        HIDDEN,
        FLYING,
        COOLDOWN
    }

    private HudState currentState = HudState.HIDDEN;
    private int currentSeconds = 0;

    public FlyingSkillHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("FlyingSkillHud.ui");
    }

    /**
     * Met à jour le HUD pour afficher le timer de vol.
     * 
     * @param remainingSeconds Temps de vol restant en secondes
     */
    public void showFlightTimer(int remainingSeconds) {
        if (currentState == HudState.FLYING && currentSeconds == remainingSeconds) {
            return; // Pas de changement nécessaire
        }

        currentState = HudState.FLYING;
        currentSeconds = remainingSeconds;

        UICommandBuilder builder = new UICommandBuilder();
        
        // Afficher le HUD principal
        builder.set("#FlyingHud.Visible", true);
        
        // Afficher le timer de vol, masquer le cooldown
        builder.set("#FlightTimer.Visible", true);
        builder.set("#CooldownTimer.Visible", false);
        
        // Configurer le timer
        builder.set("#FlightTime.Seconds", remainingSeconds);
        builder.set("#FlightTime.Paused", false);

        update(false, builder);
    }

    /**
     * Met à jour le HUD pour afficher le timer de cooldown.
     * 
     * @param remainingSeconds Temps de cooldown restant en secondes
     */
    public void showCooldownTimer(int remainingSeconds) {
        if (currentState == HudState.COOLDOWN && currentSeconds == remainingSeconds) {
            return; // Pas de changement nécessaire
        }

        currentState = HudState.COOLDOWN;
        currentSeconds = remainingSeconds;

        UICommandBuilder builder = new UICommandBuilder();
        
        // Afficher le HUD principal
        builder.set("#FlyingHud.Visible", true);
        
        // Afficher le cooldown, masquer le timer de vol
        builder.set("#FlightTimer.Visible", false);
        builder.set("#CooldownTimer.Visible", true);
        
        // Configurer le timer
        builder.set("#CooldownTime.Seconds", remainingSeconds);
        builder.set("#CooldownTime.Paused", false);

        update(false, builder);
    }

    /**
     * Masque le HUD complètement.
     */
    public void hide() {
        if (currentState == HudState.HIDDEN) {
            return; // Déjà masqué
        }

        currentState = HudState.HIDDEN;
        currentSeconds = 0;

        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#FlyingHud.Visible", false);
        builder.set("#FlightTimer.Visible", false);
        builder.set("#CooldownTimer.Visible", false);
        
        // Pauser les timers
        builder.set("#FlightTime.Paused", true);
        builder.set("#CooldownTime.Paused", true);

        update(false, builder);
    }

    /**
     * Retourne l'état actuel du HUD.
     */
    public HudState getCurrentState() {
        return currentState;
    }
}
