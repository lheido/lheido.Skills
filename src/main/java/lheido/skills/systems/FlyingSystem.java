package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.server.core.entity.entities.Player;

import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.utils.MovementUtils;
import lheido.skills.utils.SkillIds;

/**
 * Système ECS qui gère la logique du skill Flying via une state machine.
 *
 * États:
 * - READY: Le joueur peut voler (double-espace disponible)
 * - FLYING: Timer de vol actif (décrémenté chaque tick, joueur peut voler/atterrir)
 * - COOLDOWN: Le skill est en cooldown (timer décrémenté chaque tick)
 *
 * Transitions:
 * - READY → FLYING: Joueur fait double-espace (timer démarre)
 * - FLYING → COOLDOWN: Timer de vol expiré (= 0)
 * - COOLDOWN → READY: Cooldown expiré
 *
 * Le joueur peut atterrir et revoler librement tant que le timer de vol n'est pas à 0.
 *
 * Ce système vérifie aussi périodiquement que canFly est synchronisé
 * avec l'état du skill (protection contre les désync après sommeil, etc.)
 */
public class FlyingSystem extends EntityTickingSystem<EntityStore> {

    private static final ComponentType<EntityStore, PlayerRef> PLAYER_REF_TYPE =
        PlayerRef.getComponentType();
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public FlyingSystem() {
        super();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return FlyingSkillComponent.getComponentType();
    }

    @Override
    public void tick(
        float deltaTime,
        int entityIndex,
        ArchetypeChunk<EntityStore> chunk,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);

        FlyingSkillComponent flyingComponent = commandBuffer.getComponent(
            entityRef,
            FlyingSkillComponent.getComponentType()
        );
        if (flyingComponent == null) {
            return;
        }

        Player player = commandBuffer.getComponent(
            entityRef,
            Player.getComponentType()
        );
        if (player == null) {
            return;
        }

        MovementManager movementManager = commandBuffer.getComponent(
            entityRef,
            MovementManager.getComponentType()
        );
        MovementStatesComponent statesComponent = commandBuffer.getComponent(
            entityRef,
            MovementStatesComponent.getComponentType()
        );
        PlayerRef playerRef = chunk.getComponent(entityIndex, PLAYER_REF_TYPE);
        PacketHandler packetHandler =
            playerRef != null ? playerRef.getPacketHandler() : null;

        if (
            movementManager == null ||
            statesComponent == null ||
            packetHandler == null
        ) {
            LOGGER.atWarning().log(
                "FlyingSystem: Missing required components for player"
            );
            return;
        }

        // Vérifier si le skill est actif dans ActiveSkillsComponent
        ActiveSkillsComponent activeSkills = commandBuffer.getComponent(
            entityRef,
            ActiveSkillsComponent.getComponentType()
        );
        boolean isSkillActive = isSkillActiveForPlayer(
            activeSkills,
            flyingComponent
        );

        if (!isSkillActive) {
            // Skill non actif: désactiver les effets et cacher le HUD
            handleInactiveSkill(
                flyingComponent,
                movementManager,
                statesComponent,
                packetHandler,
                player,
                playerRef
            );
            return;
        }

        // Décrémenter le timer approprié à chaque tick
        flyingComponent.decrementTimer(deltaTime);

        // Vérification périodique de la synchronisation canFly
        // Protection contre les désync (sommeil, téléportation, etc.)
        // On utilise un resync forcé périodique pour garantir que le client
        // a toujours la bonne valeur même après des événements de désync
        boolean forceResync = flyingComponent.accumulateResyncTime(deltaTime);
        syncCanFlyState(
            flyingComponent,
            movementManager,
            packetHandler,
            forceResync
        );

        // Traiter l'état actuel
        switch (flyingComponent.getState()) {
            case READY -> handleReadyState(
                flyingComponent,
                statesComponent,
                player,
                playerRef
            );
            case FLYING -> handleFlyingState(
                flyingComponent,
                movementManager,
                statesComponent,
                packetHandler,
                player,
                playerRef
            );
            case COOLDOWN -> handleCooldownState(
                flyingComponent,
                player,
                playerRef
            );
        }
    }

    /**
     * Vérifie si le skill Flying est actif pour le joueur.
     *
     * Un skill est considéré actif si:
     * - Le joueur a un ActiveSkillsComponent
     * - Un des skills actifs est un skill Flying (commence par "Skill_Flying_")
     */
    private boolean isSkillActiveForPlayer(
        ActiveSkillsComponent activeSkills,
        FlyingSkillComponent flyingComponent
    ) {
        if (activeSkills == null) {
            return false;
        }

        // Vérifier si un skill Flying est dans les slots actifs
        for (String activeSkill : activeSkills.getActiveSkills()) {
            if (SkillIds.isFlyingSkill(activeSkill)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gère le cas où le skill n'est pas actif.
     *
     * - Désactive le vol si le joueur volait
     * - Réinitialise l'état du skill
     * 
     * Note: L'affichage HUD est géré par SkillBarSystem.
     */
    private void handleInactiveSkill(
        FlyingSkillComponent component,
        MovementManager movementManager,
        MovementStatesComponent statesComponent,
        PacketHandler packetHandler,
        Player player,
        PlayerRef playerRef
    ) {
        // Forcer l'arrêt du vol si le joueur est en l'air
        boolean isActuallyFlying = MovementUtils.isCurrentlyFlying(
            statesComponent
        );
        if (isActuallyFlying) {
            MovementUtils.forceStopFlying(statesComponent, packetHandler);
        }

        // Désactiver canFly
        MovementSettings settings = movementManager.getSettings();
        if (settings != null && settings.canFly) {
            MovementUtils.setCanFly(movementManager, packetHandler, false);
        }

        // Réinitialiser l'état du skill pour être prêt si réactivé
        component.transitionToReady();
    }

    /**
     * Vérifie et corrige la synchronisation de canFly avec l'état du skill.
     * Appelé à chaque tick pour détecter les désynchronisations.
     *
     * @param component        Le FlyingSkillComponent du joueur
     * @param movementManager  Le MovementManager du joueur
     * @param packetHandler    Le PacketHandler pour envoyer les packets
     * @param forceResync      Si true, force l'envoi du packet même si la valeur
     *                         serveur est déjà correcte (utile après réveil du lit, etc.)
     */
    private void syncCanFlyState(
        FlyingSkillComponent component,
        MovementManager movementManager,
        PacketHandler packetHandler,
        boolean forceResync
    ) {
        MovementSettings settings = movementManager.getSettings();
        if (settings == null) {
            return;
        }

        boolean shouldCanFly = component.shouldCanFly();

        if (forceResync) {
            // Forcer l'envoi du packet pour resync le client
            MovementUtils.forceSetCanFly(
                movementManager,
                packetHandler,
                shouldCanFly
            );
        } else if (settings.canFly != shouldCanFly) {
            // Correction normale si désync détectée côté serveur
            MovementUtils.setCanFly(
                movementManager,
                packetHandler,
                shouldCanFly
            );
        }
    }

    /**
     * État READY: Le joueur peut voler.
     * Transition vers FLYING si le joueur fait double-espace.
     * 
     * Note: L'affichage HUD est géré par SkillBarSystem.
     */
    private void handleReadyState(
        FlyingSkillComponent component,
        MovementStatesComponent statesComponent,
        Player player,
        PlayerRef playerRef
    ) {
        // Vérifier si le joueur commence à voler (double-espace)
        boolean isPlayerFlying = MovementUtils.isCurrentlyFlying(
            statesComponent
        );
        if (isPlayerFlying) {
            component.transitionToFlying();
        }
    }

    /**
     * État FLYING: Le timer de vol est actif.
     * Le joueur peut voler/atterrir librement.
     * Transition vers COOLDOWN uniquement quand le timer expire.
     * 
     * Note: L'affichage HUD est géré par SkillBarSystem.
     */
    private void handleFlyingState(
        FlyingSkillComponent component,
        MovementManager movementManager,
        MovementStatesComponent statesComponent,
        PacketHandler packetHandler,
        Player player,
        PlayerRef playerRef
    ) {
        // Timer de vol expiré → cooldown
        if (component.isFlyTimeExpired()) {
            component.transitionToCooldown();

            // Forcer l'arrêt du vol si le joueur est en l'air
            boolean isActuallyFlying = MovementUtils.isCurrentlyFlying(
                statesComponent
            );
            if (isActuallyFlying) {
                MovementUtils.forceStopFlying(statesComponent, packetHandler);
            }
        }
    }

    /**
     * État COOLDOWN: Le skill est en cooldown.
     * Transition vers READY quand le cooldown expire.
     * 
     * Note: L'affichage HUD est géré par SkillBarSystem.
     */
    private void handleCooldownState(
        FlyingSkillComponent component,
        Player player,
        PlayerRef playerRef
    ) {
        if (component.isCooldownExpired()) {
            component.transitionToReady();
        }
    }
}
