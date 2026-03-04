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
    public static final String PREFIX_FIRE_RESISTANCE = "Skill_FireResistance_";

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
     * Retourne l'ID de l'item FireResistance skill en fonction du niveau.
     * 
     * @param level Le niveau du skill (1-4)
     * @return L'ID du skill ou null si niveau invalide
     */
    public static String getFireResistanceSkillId(int level) {
        return getSkillId(PREFIX_FIRE_RESISTANCE, level);
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

    /**
     * Verifie si un skill ID correspond au prefix FireResistance (tous niveaux).
     * 
     * @param skillId L'ID a verifier
     * @return true si c'est un skill FireResistance
     */
    public static boolean isFireResistanceSkill(String skillId) {
        return skillId != null && skillId.startsWith(PREFIX_FIRE_RESISTANCE);
    }

    // ============================================
    // Methodes utilitaires pour les prefixes
    // ============================================

    /**
     * Extrait le prefix d'un skill ID complet.
     * Ex: "Skill_Flying_A" -> "Skill_Flying_"
     * 
     * @param skillId L'ID complet du skill
     * @return Le prefix ou null si invalide
     */
    public static String extractPrefix(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return null;
        }
        
        // Chercher le dernier underscore pour extraire le prefix
        int lastUnderscore = skillId.lastIndexOf('_');
        if (lastUnderscore > 0) {
            return skillId.substring(0, lastUnderscore + 1);
        }
        return null;
    }

    /**
     * Verifie si un prefix de skill correspond a un skill actif.
     * Compare le prefix stocke avec un prefix donne.
     * 
     * @param activePrefix Le prefix stocke dans ActiveSkillsComponent
     * @param targetPrefix Le prefix a verifier
     * @return true si les prefixes correspondent
     */
    public static boolean prefixMatches(String activePrefix, String targetPrefix) {
        if (activePrefix == null || targetPrefix == null) {
            return false;
        }
        return activePrefix.equals(targetPrefix);
    }

    /**
     * Retourne tous les prefixes connus.
     */
    public static String[] getAllPrefixes() {
        return new String[] {
            PREFIX_FLYING,
            PREFIX_WATER_BREATHING,
            PREFIX_STAMINA,
            PREFIX_POISON_RESISTANCE,
            PREFIX_FIRE_RESISTANCE
        };
    }
}
