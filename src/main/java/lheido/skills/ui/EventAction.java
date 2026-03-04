package lheido.skills.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Simple classe wrapper pour les événements UI.
 * Utilise une seule clé "Data" avec format "action:param"
 * (ex: "close", "select:SkillId", "remove:0")
 */
public class EventAction {
    
    private String data;
    
    public static final BuilderCodec<EventAction> CODEC = BuilderCodec.builder(
        EventAction.class, EventAction::new
    )
    .append(new KeyedCodec<>("Data", Codec.STRING),
            (e, v) -> e.data = v,
            e -> e.data)
    .add()
    .build();
    
    public EventAction() {
        this.data = "";
    }
    
    public String getData() {
        return data;
    }
}
