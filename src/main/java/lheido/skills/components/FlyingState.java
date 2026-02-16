package lheido.skills.components;

/**
 * États possibles du skill Flying.
 */
public enum FlyingState {
    /**
     * Le joueur peut voler (double-espace disponible).
     */
    READY,

    /**
     * Le joueur est en train de voler (timer actif).
     */
    FLYING,

    /**
     * Le skill est en cooldown (vol désactivé temporairement).
     */
    COOLDOWN
}
