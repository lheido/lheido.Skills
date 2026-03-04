package lheido.skills.utils;

/**
 * Classe utilitaire pour la gestion des IDs de skills.
 * 
 * Centralise les prefixes et les methodes de conversion niveau -> ID.
 * Les IDs sont sous la forme "Skill_<NomSkill>_<Niveau>" (ex: Skill_Flying_A).
 */
public final class SkillIds {

    // ============================================
    // Prefixes de skills
    // ============================================
    
    public static final String PREFIX_FLYING = "Skill_Flying_";
    public static final String PREFIX_WATER_BREATHING = "Skill_WaterBreathing_";
    public static final String PREFIX_STAMINA = "Skill_Stamina_";
    public static final String PREFIX_POISON_RESISTANCE = "Skill_PoisonResistance_";

    // ============================================
    // Suffixes de niveaux
    // ============================================
    
    private static final String[] LEVEL_SUFFIXES = {"A", "B", "C", "X"};

    private SkillIds() {
        // Utility class
    }

    // ============================================
    // Methodes de conversion niveau -> ID
    // ============================================

    /**
     * Retourne l'ID de l'item Flying skill en fonction du niveau.
     * 
     * @param level Le niveau du skill (1-4)
     * @return L'ID du skill ou null si niveau invalide
     */
    public static String getFlyingSkillId(int level) {
        return getSkillId(PREFIX_FLYING, level);
    }

    /**
     * Retourne l'ID de l'item WaterBreathing skill en fonction du niveau.
     * 
     * @param level Le niveau du skill (1-4)
     * @return L'ID du skill ou null si niveau invalide
     */
    public static String getWaterBreathingSkillId(int level) {
        return getSkillId(PREFIX_WATER_BREATHING, level);
    }

    /**
     * Retourne l'ID de l'item Stamina skill en fonction du niveau.
     * 
     * @param level Le niveau du skill (1-4)
     * @return L'ID du skill ou null si niveau invalide
     */
    public static String getStaminaSkillId(int level) {
        return getSkillId(PREFIX_STAMINA, level);
    }

    /**
     * Retourne l'ID de l'item PoisonResistance skill en fonction du niveau.
     * 
     * @param level Le niveau du skill (1-4)
     * @return L'ID du skill ou null si niveau invalide
     */
    public static String getPoisonResistanceSkillId(int level) {
        return getSkillId(PREFIX_POISON_RESISTANCE, level);
    }

    /**
     * Methode generique pour construire un ID de skill.
     * 
     * @param prefix Le prefix du skill (ex: "Skill_Flying_")
     * @param level Le niveau (1-4 correspond a A-X)
     * @return L'ID complet ou null si niveau invalide
     */
    public static String getSkillId(String prefix, int level) {
        if (level < 1 || level > LEVEL_SUFFIXES.length) {
            return null;
        }
        return prefix + LEVEL_SUFFIXES[level - 1];
    }

    // ============================================
    // Methodes de verification (pour ActiveSkillsComponent)
    // ============================================

    /**
     * Verifie si un skill ID correspond au prefix Flying (tous niveaux).
     * 
     * @param skillId L'ID a verifier
     * @return true si c'est un skill Flying
     */
    public static boolean isFlyingSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_FLYING);
    }

    /**
     * Verifie si un skill ID correspond au prefix WaterBreathing (tous niveaux).
     * 
     * @param skillId L'ID a verifier
     * @return true si c'est un skill WaterBreathing
     */
    public static boolean isWaterBreathingSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_WATER_BREATHING);
    }

    /**
     * Verifie si un skill ID correspond au prefix Stamina (tous niveaux).
     * 
     * @param skillId L'ID a verifier
     * @return true si c'est un skill Stamina
     */
    public static boolean isStaminaSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_STAMINA);
    }

    /**
     * Verifie si un skill ID correspond au prefix PoisonResistance (tous niveaux).
     * 
     * @param skillId L'ID a verifier
     * @return true si c'est un skill PoisonResistance
     */
    public static boolean isPoisonResistanceSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_POISON_RESISTANCE);
    }
}
