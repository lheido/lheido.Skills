package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.ActiveSkillsComponent;
import lheido.skills.components.FlyingSkillComponent;
import lheido.skills.components.FlyingState;
import lheido.skills.hud.SkillBarHud;
import lheido.skills.hud.SkillBarHud.SlotInfo;
import lheido.skills.hud.SkillBarHud.SlotState;
import lheido.skills.utils.SkillIds;

/**
 * Systeme ECS qui gere l'affichage de la SkillBar HUD.
 * 
 * Ce systeme met a jour la skillbar pour afficher les 3 skills actifs du joueur
 * avec leurs etats respectifs (READY, ACTIVE, COOLDOWN).
 * 
 * La skillbar remplace l'ancien FlyingSkillHud et centralise l'affichage
 * de tous les skills actifs.
 */
public class SkillBarSystem extends EntityTickingSystem<EntityStore> {

    private static final ComponentType<EntityStore, PlayerRef> PLAYER_REF_TYPE =
        PlayerRef.getComponentType();

    private static final int MAX_SLOTS = 3;

    public SkillBarSystem() {
        super();
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Ce systeme s'execute pour toutes les entites avec ActiveSkillsComponent
        return ActiveSkillsComponent.getComponentType();
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

        // Recuperer le joueur
        Player player = commandBuffer.getComponent(
            entityRef,
            Player.getComponentType()
        );
        if (player == null) {
            return;
        }

        PlayerRef playerRef = chunk.getComponent(entityIndex, PLAYER_REF_TYPE);
        if (playerRef == null) {
            return;
        }

        // Recuperer le HudManager
        HudManager hudManager = player.getHudManager();
        if (hudManager == null) {
            return;
        }

        // Recuperer ou creer le SkillBarHud
        SkillBarHud skillBarHud = getOrCreateSkillBarHud(hudManager, playerRef);
        if (skillBarHud == null) {
            return;
        }

        // Recuperer les skills actifs
        ActiveSkillsComponent activeSkills = commandBuffer.getComponent(
            entityRef,
            ActiveSkillsComponent.getComponentType()
        );
        if (activeSkills == null) {
            // Pas de skills actifs - vider la skillbar
            clearSkillBar(skillBarHud);
            return;
        }

        // Mettre a jour chaque slot
        SlotInfo[] slotInfos = new SlotInfo[MAX_SLOTS];
        String[] skills = activeSkills.getActiveSkills();

        for (int i = 0; i < MAX_SLOTS; i++) {
            String skillPrefix = (i < skills.length) ? skills[i] : null;
            slotInfos[i] = buildSlotInfo(skillPrefix, entityRef, commandBuffer);
        }

        skillBarHud.updateAllSlots(slotInfos);
    }

    /**
     * Recupere le SkillBarHud existant ou en cree un nouveau.
     */
    private SkillBarHud getOrCreateSkillBarHud(HudManager hudManager, PlayerRef playerRef) {
        CustomUIHud currentHud = hudManager.getCustomHud();
        
        if (currentHud instanceof SkillBarHud existingHud) {
            return existingHud;
        }

        // Creer un nouveau HUD
        SkillBarHud newHud = new SkillBarHud(playerRef);
        hudManager.setCustomHud(playerRef, newHud);
        newHud.show();
        return newHud;
    }

    /**
     * Vide la skillbar (tous les slots vides).
     */
    private void clearSkillBar(SkillBarHud skillBarHud) {
        SlotInfo[] emptySlots = new SlotInfo[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            emptySlots[i] = SlotInfo.empty();
        }
        skillBarHud.updateAllSlots(emptySlots);
    }

    /**
     * Construit les informations d'un slot en fonction du skill et de son etat.
     * 
     * @param skillPrefix Le prefix du skill (ex: "Skill_Flying_") ou null si slot vide
     * @param entityRef Reference vers l'entite joueur
     * @param commandBuffer Buffer pour acceder aux composants
     * @return Les informations du slot
     */
    private SlotInfo buildSlotInfo(
        String skillPrefix,
        Ref<EntityStore> entityRef,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        if (skillPrefix == null || skillPrefix.isEmpty()) {
            return SlotInfo.empty();
        }

        // Determiner le type de skill et recuperer son etat
        if (SkillIds.isFlyingSkill(skillPrefix)) {
            return buildFlyingSlotInfo(skillPrefix, entityRef, commandBuffer);
        }
        
        // Pour les skills passifs (WaterBreathing, Stamina, etc.)
        // Ils n'ont pas d'etat ACTIVE/COOLDOWN, donc toujours READY
        return buildPassiveSlotInfo(skillPrefix, entityRef, commandBuffer);
    }

    /**
     * Construit les informations d'un slot pour le skill Flying.
     */
    private SlotInfo buildFlyingSlotInfo(
        String skillPrefix,
        Ref<EntityStore> entityRef,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        FlyingSkillComponent flyingComp = commandBuffer.getComponent(
            entityRef,
            FlyingSkillComponent.getComponentType()
        );

        if (flyingComp == null) {
            // Skill equipe mais composant non trouve - afficher comme READY
            String skillId = SkillIds.getSkillId(skillPrefix, 1);
            return SlotInfo.ready(skillId);
        }

        // Construire l'ID complet du skill avec le niveau actuel
        String skillId = SkillIds.getFlyingSkillId(flyingComp.getLevel());
        if (skillId == null) {
            skillId = SkillIds.getSkillId(skillPrefix, 1);
        }

        FlyingState state = flyingComp.getState();

        return switch (state) {
            case READY -> SlotInfo.ready(skillId);
            case FLYING -> {
                if (flyingComp.isUnlimitedFlight()) {
                    // Vol illimite - pas de timer
                    yield SlotInfo.ready(skillId);
                }
                int remainingSeconds = (int) Math.ceil(
                    flyingComp.getRemainingFlyTimeMs() / 1000.0
                );
                yield SlotInfo.active(skillId, remainingSeconds);
            }
            case COOLDOWN -> {
                int remainingSeconds = (int) Math.ceil(
                    flyingComp.getRemainingCooldownMs() / 1000.0
                );
                yield SlotInfo.cooldown(skillId, remainingSeconds);
            }
        };
    }

    /**
     * Construit les informations d'un slot pour un skill passif.
     * Les skills passifs n'ont pas d'etat actif/cooldown.
     */
    private SlotInfo buildPassiveSlotInfo(
        String skillPrefix,
        Ref<EntityStore> entityRef,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        // Recuperer le niveau du skill passif
        int level = getPassiveSkillLevel(skillPrefix, entityRef, commandBuffer);
        String skillId = SkillIds.getSkillId(skillPrefix, level);
        
        if (skillId == null) {
            skillId = skillPrefix + "A"; // Fallback
        }

        return SlotInfo.ready(skillId);
    }

    /**
     * Recupere le niveau d'un skill passif.
     */
    private int getPassiveSkillLevel(
        String skillPrefix,
        Ref<EntityStore> entityRef,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        // Verifier chaque type de skill passif
        if (SkillIds.isWaterBreathingSkill(skillPrefix)) {
            var comp = commandBuffer.getComponent(
                entityRef,
                lheido.skills.components.WaterBreathingSkillComponent.getComponentType()
            );
            return (comp != null) ? comp.getLevel() : 1;
        }

        if (SkillIds.isStaminaSkill(skillPrefix)) {
            var comp = commandBuffer.getComponent(
                entityRef,
                lheido.skills.components.StaminaSkillComponent.getComponentType()
            );
            return (comp != null) ? comp.getLevel() : 1;
        }

        if (SkillIds.isPoisonResistanceSkill(skillPrefix)) {
            var comp = commandBuffer.getComponent(
                entityRef,
                lheido.skills.components.PoisonResistanceSkillComponent.getComponentType()
            );
            return (comp != null) ? comp.getLevel() : 1;
        }

        if (SkillIds.isFireResistanceSkill(skillPrefix)) {
            var comp = commandBuffer.getComponent(
                entityRef,
                lheido.skills.components.FireResistanceSkillComponent.getComponentType()
            );
            return (comp != null) ? comp.getLevel() : 1;
        }

        if (SkillIds.isLifeStealSkill(skillPrefix)) {
            var comp = commandBuffer.getComponent(
                entityRef,
                lheido.skills.components.LifeStealSkillComponent.getComponentType()
            );
            return (comp != null) ? comp.getLevel() : 1;
        }

        return 1; // Default
    }
}
