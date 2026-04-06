package lheido.skills.components;

/**
 * Possible states of the Flying skill.
 */
public enum FlyingState {
    /**
     * The player can fly (double-space available).
     */
    READY,

    /**
     * The player is currently flying (timer active).
     */
    FLYING,

    /**
     * The skill is on cooldown (flight temporarily disabled).
     */
    COOLDOWN,
}
