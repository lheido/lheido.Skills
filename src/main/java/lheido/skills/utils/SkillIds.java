package lheido.skills.utils;

/**
 * Utility class for managing skill IDs.
 *
 * Centralizes prefixes and level-to-ID conversion methods.
 * IDs follow the format "Skill_<SkillName>_<Level>" (e.g.: Skill_Flying_A).
 */
public final class SkillIds {

    // ============================================
    // Skill prefixes
    // ============================================

    public static final String PREFIX_FLYING = "Skill_Flying_";
    public static final String PREFIX_WATER_BREATHING = "Skill_WaterBreathing_";
    public static final String PREFIX_STAMINA = "Skill_Stamina_";
    public static final String PREFIX_POISON_RESISTANCE =
        "Skill_PoisonResistance_";
    public static final String PREFIX_FIRE_RESISTANCE = "Skill_FireResistance_";
    public static final String PREFIX_LIFE_STEAL = "Skill_LifeSteal_";

    // ============================================
    // Level suffixes
    // ============================================

    private static final String[] LEVEL_SUFFIXES = { "A", "B", "C", "X" };

    private SkillIds() {
        // Utility class
    }

    // ============================================
    // Level-to-ID conversion methods
    // ============================================

    /**
     * Returns the item ID for the Flying skill based on the level.
     *
     * @param level The skill level (1-4)
     * @return The skill ID or null if the level is invalid
     */
    public static String getFlyingSkillId(int level) {
        return getSkillId(PREFIX_FLYING, level);
    }

    /**
     * Returns the item ID for the WaterBreathing skill based on the level.
     *
     * @param level The skill level (1-4)
     * @return The skill ID or null if the level is invalid
     */
    public static String getWaterBreathingSkillId(int level) {
        return getSkillId(PREFIX_WATER_BREATHING, level);
    }

    /**
     * Returns the item ID for the Stamina skill based on the level.
     *
     * @param level The skill level (1-4)
     * @return The skill ID or null if the level is invalid
     */
    public static String getStaminaSkillId(int level) {
        return getSkillId(PREFIX_STAMINA, level);
    }

    /**
     * Returns the item ID for the PoisonResistance skill based on the level.
     *
     * @param level The skill level (1-4)
     * @return The skill ID or null if the level is invalid
     */
    public static String getPoisonResistanceSkillId(int level) {
        return getSkillId(PREFIX_POISON_RESISTANCE, level);
    }

    /**
     * Returns the item ID for the FireResistance skill based on the level.
     *
     * @param level The skill level (1-4)
     * @return The skill ID or null if the level is invalid
     */
    public static String getFireResistanceSkillId(int level) {
        return getSkillId(PREFIX_FIRE_RESISTANCE, level);
    }

    /**
     * Returns the item ID for the LifeSteal skill based on the level.
     *
     * @param level The skill level (1-4)
     * @return The skill ID or null if the level is invalid
     */
    public static String getLifeStealSkillId(int level) {
        return getSkillId(PREFIX_LIFE_STEAL, level);
    }

    /**
     * Generic method to build a skill ID.
     *
     * @param prefix The skill prefix (e.g.: "Skill_Flying_")
     * @param level The level (1-4 corresponds to A-X)
     * @return The complete ID or null if the level is invalid
     */
    public static String getSkillId(String prefix, int level) {
        if (level < 1 || level > LEVEL_SUFFIXES.length) {
            return null;
        }
        return prefix + LEVEL_SUFFIXES[level - 1];
    }

    // ============================================
    // Verification methods (for ActiveSkillsComponent)
    // ============================================

    /**
     * Checks if a skill ID matches the Flying prefix (all levels).
     *
     * @param skillId The ID to check
     * @return true if it is a Flying skill
     */
    public static boolean isFlyingSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_FLYING);
    }

    /**
     * Checks if a skill ID matches the WaterBreathing prefix (all levels).
     *
     * @param skillId The ID to check
     * @return true if it is a WaterBreathing skill
     */
    public static boolean isWaterBreathingSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_WATER_BREATHING);
    }

    /**
     * Checks if a skill ID matches the Stamina prefix (all levels).
     *
     * @param skillId The ID to check
     * @return true if it is a Stamina skill
     */
    public static boolean isStaminaSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_STAMINA);
    }

    /**
     * Checks if a skill ID matches the PoisonResistance prefix (all levels).
     *
     * @param skillId The ID to check
     * @return true if it is a PoisonResistance skill
     */
    public static boolean isPoisonResistanceSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_POISON_RESISTANCE);
    }

    /**
     * Checks if a skill ID matches the FireResistance prefix (all levels).
     *
     * @param skillId The ID to check
     * @return true if it is a FireResistance skill
     */
    public static boolean isFireResistanceSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_FIRE_RESISTANCE);
    }

    /**
     * Checks if a skill ID matches the LifeSteal prefix (all levels).
     *
     * @param skillId The ID to check
     * @return true if it is a LifeSteal skill
     */
    public static boolean isLifeStealSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_LIFE_STEAL);
    }

    // ============================================
    // Utility methods for prefixes
    // ============================================

    /**
     * Extracts the prefix from a complete skill ID.
     * E.g.: "Skill_Flying_A" -> "Skill_Flying_"
     *
     * @param skillId The complete skill ID
     * @return The prefix or null if invalid
     */
    public static String extractPrefix(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return null;
        }

        // Find the last underscore to extract the prefix
        int lastUnderscore = skillId.lastIndexOf('_');
        if (lastUnderscore > 0) {
            return skillId.substring(0, lastUnderscore + 1);
        }
        return null;
    }

    /**
     * Checks if a skill prefix matches an active skill.
     * Compares the stored prefix with a given prefix.
     *
     * @param activePrefix The prefix stored in ActiveSkillsComponent
     * @param targetPrefix The prefix to check against
     * @return true if the prefixes match
     */
    public static boolean prefixMatches(
        String activePrefix,
        String targetPrefix
    ) {
        if (activePrefix == null || targetPrefix == null) {
            return false;
        }
        return activePrefix.equals(targetPrefix);
    }

    /**
     * Returns all known prefixes.
     */
    public static String[] getAllPrefixes() {
        return new String[] {
            PREFIX_FLYING,
            PREFIX_WATER_BREATHING,
            PREFIX_STAMINA,
            PREFIX_POISON_RESISTANCE,
            PREFIX_FIRE_RESISTANCE,
            PREFIX_LIFE_STEAL,
        };
    }
}
