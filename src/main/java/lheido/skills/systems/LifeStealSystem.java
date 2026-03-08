package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.LifeStealSkillComponent;
import lheido.skills.utils.SkillIds;

/**
 * Système ECS qui gère le vol de vie (Life Steal / Vampirisme).
 * 
 * Ce système écoute les événements de dégâts et soigne l'attaquant
 * d'un pourcentage des dégâts infligés si celui-ci possède le skill
 * LifeSteal actif.
 * 
 * Fonctionnement:
 * - Intercepte les dégâts sur n'importe quelle entité
 * - Vérifie si la source des dégâts est un joueur avec le skill LifeSteal
 * - Soigne le joueur attaquant selon le pourcentage de son niveau de skill
 * 
 * Note: Ce système s'exécute dans le FilterDamageGroup pour intercepter
 * les dégâts avant qu'ils ne soient appliqués.
 */
public class LifeStealSystem extends EntityEventSystem<EntityStore, Damage> {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public LifeStealSystem() {
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
        // Ne pas traiter les dégâts annulés
        if (damageEvent.isCancelled()) {
            return;
        }

        float damageAmount = damageEvent.getAmount();
        if (damageAmount <= 0) {
            return;
        }

        // Récupérer la source des dégâts (l'attaquant)
        Damage.Source source = damageEvent.getSource();
        if (source == null) {
            return;
        }

        // Récupérer la référence de l'entité source
        // Damage.Source est une interface. Pour les dégâts infligés par une entité,
        // la source est de type Damage.EntitySource qui expose getRef().
        if (!(source instanceof Damage.EntitySource entitySource)) {
            // Si ce n'est pas une EntitySource (ex: dégâts d'environnement), ignorer
            return;
        }
        
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null) {
            return;
        }

        // Vérifier si l'attaquant est un joueur
        Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
        if (attackerPlayer == null) {
            return;
        }

        // Vérifier si l'attaquant a le skill LifeSteal
        LifeStealSkillComponent lifeStealComponent = store.getComponent(
            attackerRef,
            LifeStealSkillComponent.getComponentType()
        );
        if (lifeStealComponent == null) {
            return;
        }

        // Vérifier si le skill est actif
        ActiveSkillsComponent activeSkills = store.getComponent(
            attackerRef,
            ActiveSkillsComponent.getComponentType()
        );
        if (!isSkillActiveForPlayer(activeSkills)) {
            return;
        }

        // Calculer le montant de soin
        float healAmount = lifeStealComponent.calculateHealAmount(damageAmount);
        if (healAmount <= 0) {
            return;
        }

        // Soigner l'attaquant
        healPlayer(attackerRef, healAmount, store);

        LOGGER.atFine().log(
            "LifeStealSystem: Healed player for " + healAmount + 
            " HP (dealt " + damageAmount + " damage)"
        );
    }

    /**
     * Vérifie si le skill LifeSteal est actif pour le joueur.
     */
    private boolean isSkillActiveForPlayer(ActiveSkillsComponent activeSkills) {
        if (activeSkills == null) {
            return false;
        }
        
        for (String activeSkill : activeSkills.getActiveSkills()) {
            if (SkillIds.isLifeStealSkill(activeSkill)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Soigne un joueur du montant spécifié.
     * 
     * Utilise addStatValue pour ajouter des points de vie.
     * 
     * @param playerRef Référence vers l'entité joueur
     * @param healAmount Montant de vie à restaurer
     * @param store Le store ECS
     */
    private void healPlayer(
        Ref<EntityStore> playerRef,
        float healAmount,
        Store<EntityStore> store
    ) {
        // Récupérer l'EntityStatMap du joueur
        ComponentType<EntityStore, EntityStatMap> statMapType =
            EntityStatsModule.get().getEntityStatMapComponentType();
        
        EntityStatMap statMap = store.getComponent(playerRef, statMapType);
        if (statMap == null) {
            LOGGER.atWarning().log("LifeStealSystem: EntityStatMap is null for player");
            return;
        }

        // Récupérer l'index de la stat Health
        int healthIndex = DefaultEntityStatTypes.getHealth();

        // Ajouter de la vie au joueur
        // addStatValue ajoute la valeur spécifiée à la santé actuelle
        // sans dépasser le maximum
        statMap.addStatValue(healthIndex, healAmount);
    }

    /**
     * Place ce système dans le FilterDamageGroup pour intercepter les dégâts.
     */
    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    /**
     * Query pour toutes les entités ayant des stats (santé, etc.).
     * 
     * Ce système s'exécute sur TOUTES les entités qui peuvent recevoir des dégâts,
     * puis nous vérifions si la SOURCE des dégâts (l'attaquant) a le skill LifeSteal.
     * 
     * En utilisant EntityStatMapComponentType, on cible toutes les entités
     * avec un système de stats, donc toutes celles qui peuvent être endommagées.
     */
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return EntityStatsModule.get().getEntityStatMapComponentType();
    }
}
