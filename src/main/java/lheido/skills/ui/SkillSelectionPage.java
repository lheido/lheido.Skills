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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Page interactive pour selectionner 3 skills actifs parmi les skills possedes.
 * 
 * L'utilisateur peut:
 * - Voir ses skills actuellement actifs (3 slots maximum)
 * - Selectionner un skill de la liste pour l'ajouter aux slots actifs
 * - Retirer un skill d'un slot actif
 */
public class SkillSelectionPage extends InteractiveCustomUIPage<EventAction> {

    // ============================================
    // Constantes
    // ============================================
    
    private static final int MAX_ACTIVE_SKILLS = 3;

    // ============================================
    // Etat de la page
    // ============================================
    
    /** Liste des IDs de skills que le joueur possede */
    private final List<String> ownedSkills;
    
    /** Les 3 skills actuellement actifs (peut contenir des null) */
    private final String[] activeSkills;

    // ============================================
    // Constructeur
    // ============================================
    
    /**
     * Cree une nouvelle page de selection de skills.
     * 
     * @param playerRef Reference au joueur
     * @param ownedSkills Liste des IDs de skills possedes par le joueur
     * @param currentActiveSkills Skills actuellement actifs (peut etre null ou incomplet)
     */
    public SkillSelectionPage(
            @Nonnull PlayerRef playerRef,
            @Nonnull List<String> ownedSkills,
            String[] currentActiveSkills) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventAction.CODEC);
        
        this.ownedSkills = new ArrayList<>(ownedSkills);
        this.activeSkills = new String[MAX_ACTIVE_SKILLS];
        
        // Copier les skills actifs actuels
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
            
            // Enregistrer l'evenement de clic sur le slot pour retirer le skill
            String slotSelector = "#Slot" + i;
            uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                slotSelector,
                EventData.of("Data", "remove:" + i),
                false
            );
        }
        
        // Remplir la liste des skills disponibles
        populateAvailableSkills(uiCommandBuilder, uiEventBuilder);
        
        // Evenement pour le bouton fermer - utilise une seule clé "data"
        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseBtn",
            EventData.of("Data", "close"),
            false
        );
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

        switch (action) {
            case "select":
                handleSelectSkill(param);
                break;
                
            case "remove":
                handleRemoveSkill(parseSlotIndex(param));
                break;
                
            case "close":
                saveAndClose(ref, store);
                return;
                
            default:
                // Action inconnue, on met juste a jour l'UI
                break;
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
     */
    private void handleSelectSkill(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return;
        }
        
        // Verifier si le skill est deja equipe
        for (String activeSkill : activeSkills) {
            if (skillId.equals(activeSkill)) {
                return; // Deja equipe, ne rien faire
            }
        }
        
        // Trouver le premier slot vide
        for (int i = 0; i < MAX_ACTIVE_SKILLS; i++) {
            if (activeSkills[i] == null || activeSkills[i].isEmpty()) {
                activeSkills[i] = skillId;
                return;
            }
        }
        
        // Tous les slots sont pleins - ne rien faire
        // On pourrait afficher un message d'erreur ici
    }
    
    /**
     * Retire un skill d'un slot actif.
     */
    private void handleRemoveSkill(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < MAX_ACTIVE_SKILLS) {
            activeSkills[slotIndex] = null;
        }
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
     */
    private void updateActiveSlotUI(UICommandBuilder builder, int slotIndex) {
        String skillId = activeSkills[slotIndex];
        String slotSelector = "#Slot" + slotIndex;
        
        if (skillId != null && !skillId.isEmpty()) {
            // Slot avec un skill - afficher l'item
            builder.set(slotSelector + " #Item.Visible", true);
            builder.set(slotSelector + " #Item.ItemId", skillId);
            builder.set(slotSelector + " #Item.Quantity", 1);
        } else {
            // Slot vide - masquer l'ItemSlot pour eviter le tooltip "Invalid Item"
            builder.set(slotSelector + " #Item.Visible", false);
        }
    }
    
    /**
     * Remplit la liste des skills disponibles.
     */
    private void populateAvailableSkills(UICommandBuilder builder, UIEventBuilder eventBuilder) {
        // Pour chaque skill possede, ajouter un element cliquable
        for (int i = 0; i < ownedSkills.size(); i++) {
            String skillId = ownedSkills.get(i);
            
            // Ajouter le template du skill
            builder.append("#SkillsList", "SkillSlotTemplate.ui");
            
            // Configurer l'item du slot - #SkillsList[i] pointe vers #SkillBtn (racine du template)
            String selector = "#SkillsList[" + i + "]";
            builder.set(selector + " #Item.ItemId", skillId);
            builder.set(selector + " #Item.Quantity", 1);
            
            // Enregistrer l'evenement de click sur le bouton - utilise "data" avec format "select:skillId"
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                EventData.of("Data", "select:" + skillId),
                false
            );
        }
    }

    // ============================================
    // Sauvegarde et fermeture
    // ============================================
    
    /**
     * Sauvegarde les skills actifs et ferme la page.
     */
    private void saveAndClose(Ref<EntityStore> ref, Store<EntityStore> store) {
        // Recuperer la reference du joueur pour acceder aux composants
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null) {
            close();
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
        
        // Fermer la page
        close();
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
