package lheido.skills.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.utils.SkillIds;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Page interactive pour selectionner 3 skills actifs parmi les skills possedes.
 * 
 * L'utilisateur peut:
 * - Voir ses skills actuellement actifs (3 slots maximum)
 * - Selectionner un skill de la liste pour l'ajouter aux slots actifs
 * - Retirer un skill d'un slot actif
 * 
 * Les changements sont sauvegardes automatiquement.
 */
public class SkillSelectionPage extends InteractiveCustomUIPage<EventAction> {

    // ============================================
    // Constantes
    // ============================================
    
    private static final int MAX_ACTIVE_SKILLS = 3;

    // ============================================
    // Etat de la page
    // ============================================
    
    /** Map des skills possedes (prefix -> niveau) */
    private final Map<String, Integer> ownedSkills;
    
    /** Liste des prefixes de skills possedes (pour l'ordre d'affichage) */
    private final List<String> ownedSkillPrefixes;
    
    /** Les 3 skills actuellement actifs (prefixes, peut contenir des null) */
    private final String[] activeSkills;

    // ============================================
    // Constructeur
    // ============================================
    
    /**
     * Cree une nouvelle page de selection de skills.
     * 
     * @param playerRef Reference au joueur
     * @param ownedSkills Map des skills possedes (prefix -> niveau)
     * @param currentActiveSkills Prefixes des skills actuellement actifs (peut etre null ou incomplet)
     */
    public SkillSelectionPage(
            @Nonnull PlayerRef playerRef,
            @Nonnull Map<String, Integer> ownedSkills,
            String[] currentActiveSkills) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventAction.CODEC);
        
        this.ownedSkills = new HashMap<>(ownedSkills);
        this.ownedSkillPrefixes = new ArrayList<>(ownedSkills.keySet());
        this.activeSkills = new String[MAX_ACTIVE_SKILLS];
        
        // Copier les skills actifs actuels (ce sont des prefixes)
        if (currentActiveSkills != null) {
            for (int i = 0; i < MAX_ACTIVE_SKILLS && i < currentActiveSkills.length; i++) {
                this.activeSkills[i] = currentActiveSkills[i];
            }
        }
    }

    // ============================================
    // Construction de l'UI
    // ============================================
    
    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder uiCommandBuilder,
            @Nonnull UIEventBuilder uiEventBuilder,
            @Nonnull Store<EntityStore> store) {
        
        // Charger le fichier UI principal
        uiCommandBuilder.append("SkillSelectionPage.ui");
        
        // Remplir les slots actifs avec leur etat actuel et enregistrer les evenements remove
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            updateActiveSlotUI(uiCommandBuilder, i);
            
            // Enregistrer l'evenement de clic sur le slot wrapper pour retirer le skill
            String slotSelector = "#SlotWrapper" + i + " #Slot" + i;
            uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                slotSelector,
                EventData.of("Data", "remove:" + i),
                false
            );
        }
        
        // Remplir la liste des skills disponibles
        populateAvailableSkills(uiCommandBuilder, uiEventBuilder);
        
        // Afficher le message si aucun skill disponible
        if (ownedSkillPrefixes.isEmpty()) {
            uiCommandBuilder.set("#NoSkillsMessage.Visible", true);
            uiCommandBuilder.set("#SkillsList.Visible", false);
        }
    }

    // ============================================
    // Gestion des evenements
    // ============================================
    
    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            EventAction eventAction) {
        super.handleDataEvent(ref, store, eventAction);
        
        String data = eventAction.getData();
        
        // Parse le format "action:param" ou juste "action"
        String action = data;
        String param = "";
        int colonIndex = data.indexOf(':');
        if (colonIndex > 0) {
            action = data.substring(0, colonIndex);
            param = data.substring(colonIndex + 1);
        }

        boolean changed = false;
        
        switch (action) {
            case "select":
                changed = handleSelectSkill(param);
                break;
                
            case "remove":
                changed = handleRemoveSkill(parseSlotIndex(param));
                break;
                
            default:
                break;
        }
        
        // Sauvegarder automatiquement si changement
        if (changed) {
            saveActiveSkills(ref, store);
        }
        
        // Mettre a jour l'UI apres chaque action
        refreshUI();
    }
    
    private static int parseSlotIndex(String value) {
        if (value == null || value.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ============================================
    // Logique metier
    // ============================================
    
    /**
     * Gere la selection d'un skill pour l'ajouter aux slots actifs.
     * @param prefix Le prefix du skill (ex: "Skill_Flying_")
     * @return true si un changement a ete effectue
     */
    private boolean handleSelectSkill(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return false;
        }
        
        // Verifier si le skill est deja equipe
        for (String activeSkill : activeSkills) {
            if (prefix.equals(activeSkill)) {
                return false; // Deja equipe, ne rien faire
            }
        }
        
        // Trouver le premier slot vide
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            if (activeSkills[i] == null || activeSkills[i].isEmpty()) {
                activeSkills[i] = prefix;
                return true;
            }
        }
        
        // Tous les slots sont pleins - ne rien faire
        return false;
    }
    
    /**
     * Retire un skill d'un slot actif.
     * @return true si un changement a ete effectue
     */
    private boolean handleRemoveSkill(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < MAX_ACTIVE_SKILLS && activeSkills[slotIndex] != null) {
            activeSkills[slotIndex] = null;
            return true;
        }
        return false;
    }

    // ============================================
    // Mise a jour de l'UI
    // ============================================
    
    /**
     * Rafraichit toute l'UI avec l'etat actuel.
     */
    private void refreshUI() {
        UICommandBuilder builder = new UICommandBuilder();
        
        // Mettre a jour chaque slot actif
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            updateActiveSlotUI(builder, i);
        }
        
        sendUpdate(builder);
    }
    
    /**
     * Met a jour l'affichage d'un slot actif.
     * activeSkills contient des prefixes, on reconstruit l'ID complet pour l'affichage.
     */
    private void updateActiveSlotUI(UICommandBuilder builder, int slotIndex) {
        String prefix = activeSkills[slotIndex];
        String slotSelector = "#SlotWrapper" + slotIndex + " #Slot" + slotIndex;
        
        if (prefix != null && !prefix.isEmpty()) {
            // Recuperer le niveau du skill pour ce prefix
            Integer level = ownedSkills.get(prefix);
            String fullSkillId = SkillIds.getSkillId(prefix, level != null ? level : 1);
            
            if (fullSkillId != null) {
                // Slot avec un skill - afficher l'item
                builder.set(slotSelector + " #Item.Visible", true);
                builder.set(slotSelector + " #Item.ItemId", fullSkillId);
                builder.set(slotSelector + " #Item.Quantity", 1);
            } else {
                builder.set(slotSelector + " #Item.Visible", false);
            }
        } else {
            // Slot vide - masquer l'ItemSlot
            builder.set(slotSelector + " #Item.Visible", false);
        }
    }
    
    /**
     * Remplit la liste des skills disponibles.
     */
    private void populateAvailableSkills(UICommandBuilder builder, UIEventBuilder eventBuilder) {
        // Pour chaque skill possede, ajouter un element cliquable
        for (int i = 0; i < ownedSkillPrefixes.size(); i++) {
            String prefix = ownedSkillPrefixes.get(i);
            Integer level = ownedSkills.get(prefix);
            
            // Reconstruire l'ID complet pour l'affichage (ex: "Skill_Flying_A")
            String fullSkillId = SkillIds.getSkillId(prefix, level != null ? level : 1);
            if (fullSkillId == null) {
                continue;
            }
            
            // Ajouter le template du skill
            builder.append("#SkillsList", "SkillSlotTemplate.ui");
            
            // Configurer l'item du slot
            String selector = "#SkillsList[" + i + "]";
            builder.set(selector + " #Item.ItemId", fullSkillId);
            builder.set(selector + " #Item.Quantity", 1);
            
            // Enregistrer l'evenement de click - on envoie le PREFIX (pas l'ID complet)
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                EventData.of("Data", "select:" + prefix),
                false
            );
        }
    }

    // ============================================
    // Sauvegarde
    // ============================================
    
    /**
     * Sauvegarde les skills actifs dans le composant du joueur.
     */
    private void saveActiveSkills(Ref<EntityStore> ref, Store<EntityStore> store) {
        // Recuperer la reference du joueur pour acceder aux composants
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null) {
            return;
        }
        
        // Sauvegarder les skills actifs dans le composant du joueur
        ActiveSkillsComponent component = store.getComponent(playerEntityRef, ActiveSkillsComponent.getComponentType());
        if (component != null) {
            component.setActiveSkills(activeSkills);
        } else {
            // Creer le composant s'il n'existe pas
            ActiveSkillsComponent newComponent = new ActiveSkillsComponent();
            newComponent.setActiveSkills(activeSkills);
            store.addComponent(playerEntityRef, ActiveSkillsComponent.getComponentType(), newComponent);
        }
    }

    // ============================================
    // Getters
    // ============================================
    
    /**
     * Retourne une copie des skills actifs.
     */
    public String[] getActiveSkills() {
        return activeSkills.clone();
    }
}
