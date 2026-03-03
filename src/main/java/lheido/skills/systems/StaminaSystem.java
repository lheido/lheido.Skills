package lheido.skills.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lheido.skills.components.StaminaSkillComponent;

public class StaminaSystem extends EntityTickingSystem<EntityStore> {

    private static final ComponentType<EntityStore, PlayerRef> PLAYER_REF_TYPE =
        PlayerRef.getComponentType();
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String MODIFIER_ID = "lheido_stamina";

    private static final float UNLIMITED_MULTIPLIER = 1000.0f;

    public StaminaSystem() {
        super();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return StaminaSkillComponent.getComponentType();
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

        StaminaSkillComponent staminaComponent = commandBuffer.getComponent(
            entityRef,
            StaminaSkillComponent.getComponentType()
        );
        if (staminaComponent == null) {
            return;
        }

        Player player = commandBuffer.getComponent(
            entityRef,
            Player.getComponentType()
        );
        if (player == null) {
            return;
        }

        ComponentType<EntityStore, EntityStatMap> statMapType =
            EntityStatsModule.get().getEntityStatMapComponentType();

        EntityStatMap statMap = commandBuffer.getComponent(entityRef, statMapType);
        if (statMap == null) {
            LOGGER.atWarning().log(
                "StaminaSystem: EntityStatMap is null for player"
            );
            return;
        }

        int staminaStatIndex = DefaultEntityStatTypes.getStamina();
        if (staminaStatIndex < 0) {
            LOGGER.atWarning().log(
                "StaminaSystem: Stamina stat index not found"
            );
            return;
        }

        float effectiveMultiplier = staminaComponent.isUnlimitedStamina()
            ? UNLIMITED_MULTIPLIER
            : staminaComponent.getStaminaMultiplier();

        boolean isUnlimited = staminaComponent.isUnlimitedStamina();

        applyStaminaModifier(statMap, staminaStatIndex, effectiveMultiplier, isUnlimited);
    }

    private void applyStaminaModifier(
        EntityStatMap statMap,
        int staminaStatIndex,
        float multiplier,
        boolean isUnlimited
    ) {
        StaticModifier staminaModifier = new StaticModifier(
            Modifier.ModifierTarget.MAX,
            StaticModifier.CalculationType.MULTIPLICATIVE,
            multiplier
        );

        Modifier previousModifier = statMap.putModifier(
            EntityStatMap.Predictable.NONE,
            staminaStatIndex,
            MODIFIER_ID,
            staminaModifier
        );

        if (isUnlimited) {
            statMap.maximizeStatValue(staminaStatIndex);
        }
    }
}
