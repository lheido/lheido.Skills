package lheido.skills.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Component attached to NPCs at spawn time to store the pre-calculated
 * Skill Essence drop quantity.
 *
 * The quantity is determined once at spawn based on the NPC's maxHealth
 * and flying state. A separate {@code SkillEssenceDropTickSystem} then
 * reads this component after the death animation completes and performs
 * the actual item drop.
 */
public class PendingSkillEssenceComponent implements Component<EntityStore> {

    public static final BuilderCodec<PendingSkillEssenceComponent> CODEC =
        BuilderCodec.builder(
            PendingSkillEssenceComponent.class,
            PendingSkillEssenceComponent::new
        )
        .append(
            new KeyedCodec<>("SkillEssenceQuantity", Codec.INTEGER),
            (data, value) -> data.quantity = value,
            (data) -> data.quantity
        )
        .add()
        .build();

    private static ComponentType<EntityStore, PendingSkillEssenceComponent> componentType;

    private int quantity;
    private boolean dropped;

    public PendingSkillEssenceComponent() {
        this.quantity = 0;
        this.dropped = false;
    }

    public PendingSkillEssenceComponent(int quantity) {
        this.quantity = quantity;
        this.dropped = false;
    }

    public PendingSkillEssenceComponent(PendingSkillEssenceComponent other) {
        this.quantity = other.quantity;
        this.dropped = other.dropped;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isDropped() {
        return dropped;
    }

    public void setDropped() {
        this.dropped = true;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new PendingSkillEssenceComponent(this);
    }

    @Nonnull
    public static ComponentType<EntityStore, PendingSkillEssenceComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(
        @Nonnull ComponentType<EntityStore, PendingSkillEssenceComponent> type
    ) {
        componentType = type;
    }
}
