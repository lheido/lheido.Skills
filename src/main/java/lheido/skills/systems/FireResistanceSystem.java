package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.FireResistanceSkillComponent;
import lheido.skills.utils.SkillIds;

/**
 * Système ECS qui gère la résistance au feu.
 * 
 * Ce système intercepte les événements de dégâts (Damage) et :
 * - Réduit les dégâts de feu selon le niveau du skill (niveaux A-C)
 * - Annule complètement les dégâts de feu pour le niveau X (immunité)
 * 
 * Le système s'exécute dans le FilterDamageGroup pour intercepter les dégâts
 * avant qu'ils ne soient appliqués à la santé.
 * 
 * Types de dégâts considérés comme feu :
 * - DamageCause dont l'ID contient "fire", "burn", "flame", ou "lava"
 */
public class FireResistanceSystem extends EntityEventSystem<EntityStore, Damage> {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public FireResistanceSystem() {
        super(Damage.class);
    }

    @Override
    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Damage damageEvent
    ) {
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);

        // Vérifier si l'entité a le skill de résistance au feu
        FireResistanceSkillComponent resistanceComponent = store.getComponent(
            entityRef,
            FireResistanceSkillComponent.getComponentType()
        );
        if (resistanceComponent == null) {
            return;
        }

        // Vérifier si le skill est actif
        ActiveSkillsComponent activeSkills = store.getComponent(
            entityRef,
            ActiveSkillsComponent.getComponentType()
        );
        if (!isSkillActiveForPlayer(activeSkills)) {
            return;
        }

        // Vérifier si c'est un dégât de feu
        DamageCause cause = damageEvent.getCause();
        if (!isFireDamage(cause)) {
            return;
        }

        // Appliquer la résistance
        if (resistanceComponent.isImmuneToFire()) {
            // Niveau X : Immunité totale - annuler les dégâts
            damageEvent.setCancelled(true);
            
            // Log pour debug (optionnel)
            Player player = store.getComponent(entityRef, Player.getComponentType());
            if (player != null) {
                LOGGER.atFine().log(
                    "FireResistanceSystem: Fire damage cancelled for player (immunity)"
                );
            }
        } else {
            // Niveaux A-C : Réduire les dégâts
            float originalAmount = damageEvent.getAmount();
            float reducedAmount = resistanceComponent.calculateReducedFireDamage(originalAmount);
            
            // Utiliser setAmount si disponible, sinon annuler si dégâts <= 0
            if (reducedAmount <= 0) {
                damageEvent.setCancelled(true);
            } else {
                // Tenter de modifier le montant des dégâts
                // Note: Si setAmount n'existe pas, on ne peut que cancel
                try {
                    damageEvent.setAmount(reducedAmount);
                    
                    LOGGER.atFine().log(
                        "FireResistanceSystem: Fire damage reduced from " + 
                        originalAmount + " to " + reducedAmount
                    );
                } catch (Exception e) {
                    // Si setAmount n'est pas disponible, on log l'erreur
                    LOGGER.atWarning().log(
                        "FireResistanceSystem: Could not set damage amount - " + 
                        e.getMessage()
                    );
                }
            }
        }
    }

    /**
     * Vérifie si le skill FireResistance est actif pour le joueur.
     */
    private boolean isSkillActiveForPlayer(ActiveSkillsComponent activeSkills) {
        if (activeSkills == null) {
            return false;
        }
        
        for (String activeSkill : activeSkills.getActiveSkills()) {
            if (SkillIds.isFireResistanceSkill(activeSkill)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si la cause de dégât est du feu.
     * 
     * Les types considérés comme feu :
     * - Toute DamageCause dont l'ID contient "fire", "burn", "flame", ou "lava"
     * 
     * Note: Cette liste peut être étendue selon les besoins du jeu.
     * DamageCause est un asset, donc on utilise getId() pour récupérer l'identifiant.
     */
    private boolean isFireDamage(DamageCause cause) {
        if (cause == null) {
            return false;
        }
        
        String causeId = cause.getId();
        if (causeId == null) {
            return false;
        }
        
        // Vérifier les différentes causes possibles de feu
        String lowerCauseId = causeId.toLowerCase();
        return lowerCauseId.contains("fire") || 
               lowerCauseId.contains("burn") ||
               lowerCauseId.contains("flame") ||
               lowerCauseId.contains("lava");
    }

    /**
     * Place ce système dans le FilterDamageGroup pour intercepter les dégâts
     * avant qu'ils ne soient appliqués à la santé.
     */
    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    /**
     * Query pour les entités qui ont le component FireResistanceSkillComponent.
     */
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return FireResistanceSkillComponent.getComponentType();
    }
}
