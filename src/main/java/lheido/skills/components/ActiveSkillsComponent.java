package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Component pour stocker les 3 skills actifs d'un joueur.
 *
 * Ce component est attache au Player et persiste les IDs des skills
 * que le joueur a choisi comme actifs. Un joueur peut avoir un maximum
 * de 3 skills actifs simultanement.
 */
public class ActiveSkillsComponent implements Component<EntityStore> {

    // ============================================
    // Constantes
    // ============================================

    public static final int MAX_ACTIVE_SKILLS = 3;

    // ============================================
    // ComponentType
    // ============================================

    private static volatile ComponentType<
        EntityStore,
        ActiveSkillsComponent
    > COMPONENT_TYPE;

    /**
     * Codec pour la serialisation/deserialisation du component.
     * Les skills sont stockes comme une liste de strings.
     */
    public static final BuilderCodec<ActiveSkillsComponent> CODEC =
        BuilderCodec.builder(
            ActiveSkillsComponent.class,
            ActiveSkillsComponent::new
        )
            .append(
                new KeyedCodec<>("Skill0", Codec.STRING),
                (data, value) -> data.activeSkills[0] = value,
                data -> data.activeSkills[0] != null ? data.activeSkills[0] : ""
            )
            .add()
            .append(
                new KeyedCodec<>("Skill1", Codec.STRING),
                (data, value) -> data.activeSkills[1] = value,
                data -> data.activeSkills[1] != null ? data.activeSkills[1] : ""
            )
            .add()
            .append(
                new KeyedCodec<>("Skill2", Codec.STRING),
                (data, value) -> data.activeSkills[2] = value,
                data -> data.activeSkills[2] != null ? data.activeSkills[2] : ""
            )
            .add()
            .build();

    // ============================================
    // Donnees
    // ============================================

    /** Les 3 slots de skills actifs (peut contenir des null ou des chaines vides) */
    private final String[] activeSkills;

    // ============================================
    // Constructeur
    // ============================================

    public ActiveSkillsComponent() {
        this.activeSkills = new String[MAX_ACTIVE_SKILLS];
    }

    // ============================================
    // ComponentType Management
    // ============================================

    public static void setComponentType(
        ComponentType<EntityStore, ActiveSkillsComponent> componentType
    ) {
        COMPONENT_TYPE = componentType;
    }

    public static ComponentType<
        EntityStore,
        ActiveSkillsComponent
    > getComponentType() {
        return COMPONENT_TYPE;
    }

    // ============================================
    // Getters et Setters
    // ============================================

    /**
     * Retourne une copie des skills actifs.
     */
    public String[] getActiveSkills() {
        return activeSkills.clone();
    }

    /**
     * Definit les skills actifs.
     *
     * @param skills Tableau de skills (sera copie, peut etre null)
     */
    public void setActiveSkills(String[] skills) {
        // Reinitialiser tous les slots
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            this.activeSkills[i] = null;
        }

        // Copier les skills fournis
        if (skills != null) {
            for (int i = 0; i < MAX_ACTIVE_SKILLS && i < skills.length; i++) {
                this.activeSkills[i] = skills[i];
            }
        }
    }

    /**
     * Retourne le skill actif a l'index donne.
     *
     * @param index Index du slot (0-2)
     * @return L'ID du skill ou null si le slot est vide
     */
    public String getActiveSkill(int index) {
        if (index >= 0 && index < MAX_ACTIVE_SKILLS) {
            return activeSkills[index];
        }
        return null;
    }

    /**
     * Definit le skill actif a l'index donne.
     *
     * @param index Index du slot (0-2)
     * @param skillId ID du skill (peut etre null pour vider le slot)
     */
    public void setActiveSkill(int index, String skillId) {
        if (index >= 0 && index < MAX_ACTIVE_SKILLS) {
            activeSkills[index] = skillId;
        }
    }

    /**
     * Verifie si un skill est actuellement actif.
     * Supporte les IDs complets ET les prefixes.
     *
     * @param skillIdOrPrefix L'ID du skill ou le prefix a verifier
     * @return true si le skill est dans un des slots actifs
     */
    public boolean isSkillActive(String skillIdOrPrefix) {
        if (skillIdOrPrefix == null || skillIdOrPrefix.isEmpty()) {
            return false;
        }
        for (String active : activeSkills) {
            if (active == null || active.isEmpty()) {
                continue;
            }
            // Comparaison exacte (pour les prefixes stockes)
            if (skillIdOrPrefix.equals(active)) {
                return true;
            }
            // Comparaison par prefix (pour retrocompatibilite avec anciens IDs complets)
            if (
                active.startsWith(skillIdOrPrefix) ||
                skillIdOrPrefix.startsWith(active)
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifie si un skill avec le prefix donne est actif.
     *
     * @param prefix Le prefix du skill (ex: "Skill_Flying_")
     * @return true si un skill avec ce prefix est actif
     */
    public boolean isSkillPrefixActive(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return false;
        }
        for (String active : activeSkills) {
            if (active != null && active.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retourne la liste des skills actifs non-null.
     */
    public List<String> getActiveSkillsList() {
        List<String> result = new ArrayList<>();
        for (String skill : activeSkills) {
            if (skill != null && !skill.isEmpty()) {
                result.add(skill);
            }
        }
        return result;
    }

    /**
     * Retourne le nombre de slots occupes.
     */
    public int getActiveSkillCount() {
        int count = 0;
        for (String skill : activeSkills) {
            if (skill != null && !skill.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Verifie s'il reste des slots disponibles.
     */
    public boolean hasAvailableSlot() {
        return getActiveSkillCount() < MAX_ACTIVE_SKILLS;
    }

    /**
     * Ajoute un skill au premier slot disponible.
     *
     * @param skillId L'ID du skill a ajouter
     * @return true si le skill a ete ajoute, false si tous les slots sont pleins ou le skill est deja actif
     */
    public boolean addSkill(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return false;
        }

        // Verifier si deja actif
        if (isSkillActive(skillId)) {
            return false;
        }

        // Trouver un slot vide
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            if (activeSkills[i] == null || activeSkills[i].isEmpty()) {
                activeSkills[i] = skillId;
                return true;
            }
        }

        return false; // Tous les slots sont pleins
    }

    /**
     * Retire un skill des slots actifs.
     *
     * @param skillId L'ID du skill a retirer
     * @return true si le skill a ete retire
     */
    public boolean removeSkill(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return false;
        }

        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            if (skillId.equals(activeSkills[i])) {
                activeSkills[i] = null;
                return true;
            }
        }

        return false;
    }

    // ============================================
    // Clone
    // ============================================

    @Override
    public ActiveSkillsComponent clone() {
        ActiveSkillsComponent copy = new ActiveSkillsComponent();
        System.arraycopy(
            this.activeSkills,
            0,
            copy.activeSkills,
            0,
            MAX_ACTIVE_SKILLS
        );
        return copy;
    }
}
