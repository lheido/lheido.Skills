package lheido.skills.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.utils.MovementUtils;
import lheido.skills.utils.SkillIds;

/**
 * Handler d'evenement pour resynchroniser l'etat du skill Flying
 * apres certains evenements du jeu (connexion, etc.).
 *
 * Le probleme: Quand un joueur dort et se reveille, le jeu reinitialise
 * les MovementSettings (canFly = false). Notre FlyingSystem detecte cela
 * a chaque tick, mais il peut y avoir un delai.
 *
 * La solution: Ecouter l'evenement PlayerReadyEvent qui est declenche
 * quand un joueur est pret pour le gameplay. On force alors
 * immediatement la resynchronisation de canFly.
 *
 * Note: Cet event handler est complementaire a la verification dans
 * FlyingSystem.syncCanFlyState() qui detecte les desync a chaque tick.
 */
public class FlyingSkillResyncHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Handler appele quand un joueur est pret pour le gameplay.
     *
     * @param event L'evenement PlayerReadyEvent
     */
    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        Ref<EntityStore> entityRef = event.getPlayerRef();
        if (entityRef == null) {
            return;
        }

        // Recuperer le World pour acceder au Store
        World world = player.getWorld();
        if (world == null) {
            return;
        }

        EntityStore entityStore = world.getEntityStore();
        if (entityStore == null) {
            return;
        }

        Store<EntityStore> store = entityStore.getStore();
        if (store == null) {
            return;
        }

        // Verifier si le joueur a le FlyingSkillComponent
        FlyingSkillComponent flyingComponent = store.getComponent(
            entityRef,
            FlyingSkillComponent.getComponentType()
        );
        if (flyingComponent == null) {
            return;
        }

        // Verifier si le skill Flying est actif
        ActiveSkillsComponent activeSkills = store.getComponent(
            entityRef,
            ActiveSkillsComponent.getComponentType()
        );

        if (!isSkillActiveForPlayer(activeSkills)) {
            return;
        }

        // Recuperer le PlayerRef pour le PacketHandler
        PlayerRef playerRef = store.getComponent(
            entityRef,
            PlayerRef.getComponentType()
        );
        if (playerRef == null) {
            return;
        }

        // Recuperer le MovementManager pour mettre a jour canFly
        MovementManager movementManager = store.getComponent(
            entityRef,
            MovementManager.getComponentType()
        );
        if (movementManager == null) {
            return;
        }

        // Forcer la resynchronisation de canFly
        // On utilise forceSetCanFly car le client peut avoir été réinitialisé
        // même si le serveur a déjà la bonne valeur
        boolean shouldCanFly = flyingComponent.shouldCanFly();
        MovementUtils.forceSetCanFly(
            movementManager,
            playerRef.getPacketHandler(),
            shouldCanFly
        );

        LOGGER.atInfo().log(
            "Resynced canFly=%s for player %s after PlayerReadyEvent",
            shouldCanFly,
            playerRef.getUsername()
        );
    }

    /**
     * Verifie si le skill Flying est actif pour le joueur.
     */
    private static boolean isSkillActiveForPlayer(
        ActiveSkillsComponent activeSkills
    ) {
        if (activeSkills == null) {
            return false;
        }

        for (String activeSkill : activeSkills.getActiveSkills()) {
            if (SkillIds.isFlyingSkill(activeSkill)) {
                return true;
            }
        }
        return false;
    }
}
