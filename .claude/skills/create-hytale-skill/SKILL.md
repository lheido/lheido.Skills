---
name: create-hytale-skill
description: Guide pour créer un nouveau skill dans le mod LheidoSkills pour Hytale. Utiliser quand l'utilisateur demande de créer un skill, une compétence, ou un upgrade pour le mod.
---

# Créer un nouveau Skill Hytale

## Prérequis

- Le skill utilise le même icon/visuel que `Skill_Flying_A`
- Chaque skill est un item de type "Upgrade"
- Les skills sont craftables à l'Arcane Bench avec des Skill Essence
- Les items sont consommables et détruits après utilisation réussie

## Fichiers à créer/modifier

Pour créer un nouveau skill nommé `Skill_<NomDuSkill>_<Niveau>` (ex: `Skill_Swimming_A`):

### 1. Créer le fichier JSON du skill

**Chemin:** `src/main/resources/Server/Item/Items/Upgrades/Skill_<NomDuSkill>_<Niveau>.json`

**Template pour le niveau A (premier niveau, sans prérequis):**
```json
{
  "TranslationProperties": {
    "Name": "server.items.Skill_<NomDuSkill>_A.name",
    "Description": "server.items.Skill_<NomDuSkill>_A.description"
  },
  "Id": "Skill_<NomDuSkill>_A",
  "Icon": "Icons/ItemsGenerated/Skill_Flying_A.png",
  "Quality": "Uncommon",
  "MaxStack": 1,
  "ItemLevel": 20,
  "Categories": ["Upgrade"],
  "Recipe": {
    "TimeSeconds": 2,
    "Input": [
      {
        "ItemId": "Ingredient_Skill_Essence",
        "Quantity": 100
      }
    ],
    "BenchRequirement": [
      {
        "Id": "Arcanebench",
        "Type": "Crafting",
        "Categories": ["Arcane_Misc"],
        "RequiredTierLevel": 1
      }
    ]
  },
  "PlayerAnimationsId": "Item",
  "Model": "Items/Consumables/Recipes/Recipe.blockymodel",
  "Texture": "Items/Consumables/Recipes/Recipe_Texture.png",
  "IconProperties": {
    "Scale": 0.76,
    "Rotation": [33.92, 68.795, 40.67],
    "Translation": [-0.895, -1.11]
  },
  "Consumable": true,
  "Interactions": {
    "Primary": {
      "Interactions": [
        {
          "Type": "CheckUniqueItemUsage",
          "Next": {
            "Type": "skill_<nom_du_skill>",
            "Next": {
              "Type": "ModifyInventory",
              "AdjustHeldItemQuantity": -1
            }
          }
        }
      ]
    },
    "Secondary": {
      "Interactions": [
        {
          "Type": "CheckUniqueItemUsage",
          "Next": {
            "Type": "skill_<nom_du_skill>",
            "Next": {
              "Type": "ModifyInventory",
              "AdjustHeldItemQuantity": -1
            }
          }
        }
      ]
    }
  },
  "Tags": {
    "Family": ["Skill"],
    "Type": ["Upgrade"]
  }
}
```

### 2. Ajouter les traductions

**Chemin:** `src/main/resources/Server/Languages/en-US/server.lang`

**Ajouter:**
```properties
items.Skill_<NomDuSkill>_<Niveau>.name=<Nom Lisible> (<Niveau en chiffre>)
items.Skill_<NomDuSkill>_<Niveau>.description=<Description optionnelle>
```

**Exemple pour Swimming niveau A:**
```properties
items.Skill_Swimming_A.name=Swimming (1)
items.Skill_Swimming_A.description=Allows the player to swim faster
```

### 3. Créer le component du skill (données persistantes)

Chaque skill nécessite un component ECS qui sera attaché au player. Ce component stocke les données du skill et est sauvegardé via le système de CODEC.

**Chemin:** `src/main/java/lheido/skills/components/<NomDuSkill>SkillComponent.java`

**Template:**
```java
package lheido.skills.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Component pour le skill <NomDuSkill>
 * Attaché au player pour persister les données du skill
 */
public class <NomDuSkill>SkillComponent implements Component {

    public static final BuilderCodec<<NomDuSkill>SkillComponent> CODEC =
        BuilderCodec.builder(
            <NomDuSkill>SkillComponent.class,
            <NomDuSkill>SkillComponent::new
        )
        .append(
            new KeyedCodec<>("level", Codec.INTEGER),
            (data, value) -> data.level = value,
            data -> data.level
        )
        .add()
        .build();

    private static ComponentType<EntityStore, <NomDuSkill>SkillComponent> componentType;

    private int level = 1;

    public static ComponentType<EntityStore, <NomDuSkill>SkillComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(
        ComponentType<EntityStore, <NomDuSkill>SkillComponent> type
    ) {
        componentType = type;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    // Factory methods pour chaque niveau
    public static <NomDuSkill>SkillComponent createLevelA() {
        <NomDuSkill>SkillComponent component = new <NomDuSkill>SkillComponent();
        component.level = 1;
        return component;
    }

    public static <NomDuSkill>SkillComponent createLevelB() {
        <NomDuSkill>SkillComponent component = new <NomDuSkill>SkillComponent();
        component.level = 2;
        return component;
    }

    // Ajouter d'autres méthodes factory pour les niveaux supérieurs
}
```

### 4. Créer l'interaction Java (ajout du component au player)

L'interaction permet d'ajouter le component du skill au player quand l'item est utilisé.

**Chemin:** `src/main/java/lheido/skills/interactions/Skill<NomDuSkill>Interaction.java`

**Template:**
```java
package lheido.skills.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import lheido.skills.components.<NomDuSkill>SkillComponent;

/**
 * Interaction pour le skill <NomDuSkill> niveau A (premier niveau).
 * Ajoute le component au player lors de l'utilisation de l'item.
 */
public class Skill<NomDuSkill>Interaction extends SimpleInstantInteraction {

    public static final BuilderCodec<Skill<NomDuSkill>Interaction> CODEC =
        BuilderCodec.builder(
            Skill<NomDuSkill>Interaction.class,
            Skill<NomDuSkill>Interaction::new,
            SimpleInstantInteraction.CODEC
        ).build();

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    protected void firstRun(
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext interactionContext,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer =
            interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("Skill<NomDuSkill>Interaction: CommandBuffer is null");
            return;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atWarning().log("Skill<NomDuSkill>Interaction: Player is null");
            return;
        }

        // Vérifier si le joueur possède déjà le skill
        <NomDuSkill>SkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            <NomDuSkill>SkillComponent.getComponentType()
        );

        if (existingComponent != null) {
            player.sendMessage(Message.raw("You already have the <NomDuSkill> skill!"));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Ajouter le component au player
        <NomDuSkill>SkillComponent newComponent = <NomDuSkill>SkillComponent.createLevelA();
        commandBuffer.addComponent(
            ref,
            <NomDuSkill>SkillComponent.getComponentType(),
            newComponent
        );

        player.sendMessage(Message.raw("<NomDuSkill> skill unlocked!"));
        LOGGER.atInfo().log("Player unlocked <NomDuSkill> skill");
    }
}
```

### 5. Enregistrer le component et l'interaction dans le plugin

**Chemin:** `src/main/java/lheido/skills/LheidoSkillsPlugin.java`

Ajouter dans la méthode `setup()`:
```java
// Enregistrer l'interaction
this.getCodecRegistry(Interaction.CODEC).register(
    "skill_<nom_du_skill>",
    Skill<NomDuSkill>Interaction.class,
    Skill<NomDuSkill>Interaction.CODEC
);

// Enregistrer le component avec son ComponentType (pour la persistence)
ComponentType<EntityStore, <NomDuSkill>SkillComponent> componentType =
    this.getEntityStoreRegistry().registerComponent(
        <NomDuSkill>SkillComponent.class,
        "<NomDuSkill>SkillComponent",
        <NomDuSkill>SkillComponent.CODEC
    );
<NomDuSkill>SkillComponent.setComponentType(componentType);
```

### 6. (Optionnel) Créer un système ECS pour le comportement

Si le skill nécessite une logique de jeu (ex: double jump, flying), créer un système qui réagit au component.

**Chemin:** `src/main/java/lheido/skills/systems/<NomDuSkill>System.java`

---

## Créer des niveaux d'upgrade (B, C, X, etc.)

Les upgrades permettent d'améliorer un skill existant. Ils nécessitent que le joueur possède déjà le niveau précédent.

### Chaîne d'interactions pour les upgrades

**IMPORTANT:** Pour éviter le bug où `CheckUniqueItemUsage` marque l'item comme utilisé avant la vérification des prérequis, utiliser cette chaîne:

```
check_<nom_du_skill>_upgrade → CheckUniqueItemUsage → skill_<nom_du_skill>_<niveau> → ModifyInventory
```

### 1. Créer l'interaction de vérification des prérequis

Cette interaction paramétrable vérifie si le joueur a le niveau requis avant d'autoriser l'upgrade.

**Chemin:** `src/main/java/lheido/skills/interactions/Check<NomDuSkill>UpgradeInteraction.java`

**Template:**
```java
package lheido.skills.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import lheido.skills.components.<NomDuSkill>SkillComponent;

/**
 * Interaction pour vérifier les prérequis d'upgrade du skill <NomDuSkill>.
 * Paramétrable via JSON avec RequiredLevel et TargetLevel.
 * 
 * - RequiredLevel: Le niveau minimum requis (0 = aucun prérequis, pour niveau A)
 * - TargetLevel: Le niveau vers lequel on upgrade
 * 
 * Échoue si:
 * - Le joueur n'a pas le niveau requis
 * - Le joueur a déjà le niveau cible ou supérieur
 */
public class Check<NomDuSkill>UpgradeInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<Check<NomDuSkill>UpgradeInteraction> CODEC =
        BuilderCodec.builder(
            Check<NomDuSkill>UpgradeInteraction.class,
            Check<NomDuSkill>UpgradeInteraction::new,
            SimpleInstantInteraction.CODEC
        )
        .append(
            new KeyedCodec<>("RequiredLevel", Codec.INTEGER),
            (data, value) -> data.requiredLevel = value,
            data -> data.requiredLevel
        )
        .add()
        .append(
            new KeyedCodec<>("TargetLevel", Codec.INTEGER),
            (data, value) -> data.targetLevel = value,
            data -> data.targetLevel
        )
        .add()
        .build();

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private int requiredLevel = 0;
    private int targetLevel = 1;

    @Override
    protected void firstRun(
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext interactionContext,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        <NomDuSkill>SkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            <NomDuSkill>SkillComponent.getComponentType()
        );

        int currentLevel = existingComponent != null ? existingComponent.getLevel() : 0;

        // Vérifier si le joueur a le niveau requis
        if (currentLevel < requiredLevel) {
            String message = "You must have <NomDuSkill> (" + requiredLevel + ") before upgrading!";
            player.sendMessage(Message.raw(message));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Vérifier si le joueur a déjà le niveau cible ou supérieur
        if (currentLevel >= targetLevel) {
            player.sendMessage(Message.raw("You already have this level or higher!"));
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Prérequis validés, l'interaction suivante peut s'exécuter
        LOGGER.atInfo().log("<NomDuSkill> upgrade check passed: " + currentLevel + " -> " + targetLevel);
    }
}
```

### 2. Créer les interactions d'upgrade simplifiées

Les interactions d'upgrade ne vérifient plus les prérequis (délégués à `Check<NomDuSkill>UpgradeInteraction`).

**Chemin:** `src/main/java/lheido/skills/interactions/Skill<NomDuSkill>BInteraction.java`

**Template simplifié:**
```java
package lheido.skills.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import lheido.skills.components.<NomDuSkill>SkillComponent;

/**
 * Interaction pour upgrader le skill <NomDuSkill> vers le niveau B.
 * Les prérequis sont vérifiés par Check<NomDuSkill>UpgradeInteraction.
 */
public class Skill<NomDuSkill>BInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<Skill<NomDuSkill>BInteraction> CODEC =
        BuilderCodec.builder(
            Skill<NomDuSkill>BInteraction.class,
            Skill<NomDuSkill>BInteraction::new,
            SimpleInstantInteraction.CODEC
        ).build();

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    protected void firstRun(
        @Nonnull InteractionType interactionType,
        @Nonnull InteractionContext interactionContext,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Les prérequis sont vérifiés par Check<NomDuSkill>UpgradeInteraction
        <NomDuSkill>SkillComponent existingComponent = commandBuffer.getComponent(
            ref,
            <NomDuSkill>SkillComponent.getComponentType()
        );

        // Créer le nouveau component avec le niveau B
        <NomDuSkill>SkillComponent upgradedComponent = <NomDuSkill>SkillComponent.createLevelB();

        // Supprimer l'ancien et ajouter le nouveau
        if (existingComponent != null) {
            commandBuffer.removeComponent(ref, <NomDuSkill>SkillComponent.getComponentType());
        }
        commandBuffer.addComponent(
            ref,
            <NomDuSkill>SkillComponent.getComponentType(),
            upgradedComponent
        );

        player.sendMessage(Message.raw("<NomDuSkill> skill upgraded to level 2!"));
        LOGGER.atInfo().log("Player upgraded <NomDuSkill> skill to level B");
    }
}
```

### 3. Créer le fichier JSON de l'upgrade

**Template pour niveau B (et supérieurs):**
```json
{
  "TranslationProperties": {
    "Name": "server.items.Skill_<NomDuSkill>_B.name",
    "Description": "server.items.Skill_<NomDuSkill>_B.description"
  },
  "Id": "Skill_<NomDuSkill>_B",
  "Icon": "Icons/ItemsGenerated/Skill_Flying_A.png",
  "Quality": "Rare",
  "MaxStack": 1,
  "ItemLevel": 30,
  "Categories": ["Upgrade"],
  "Recipe": {
    "TimeSeconds": 3,
    "Input": [
      {
        "ItemId": "Ingredient_Skill_Essence",
        "Quantity": 200
      }
    ],
    "BenchRequirement": [
      {
        "Id": "Arcanebench",
        "Type": "Crafting",
        "Categories": ["Arcane_Misc"],
        "RequiredTierLevel": 1
      }
    ]
  },
  "PlayerAnimationsId": "Item",
  "Model": "Items/Consumables/Recipes/Recipe.blockymodel",
  "Texture": "Items/Consumables/Recipes/Recipe_Texture.png",
  "IconProperties": {
    "Scale": 0.76,
    "Rotation": [33.92, 68.795, 40.67],
    "Translation": [-0.895, -1.11]
  },
  "Consumable": true,
  "Interactions": {
    "Primary": {
      "Interactions": [
        {
          "Type": "check_<nom_du_skill>_upgrade",
          "RequiredLevel": 1,
          "TargetLevel": 2,
          "Next": {
            "Type": "CheckUniqueItemUsage",
            "Next": {
              "Type": "skill_<nom_du_skill>_b",
              "Next": {
                "Type": "ModifyInventory",
                "AdjustHeldItemQuantity": -1
              }
            }
          }
        }
      ]
    },
    "Secondary": {
      "Interactions": [
        {
          "Type": "check_<nom_du_skill>_upgrade",
          "RequiredLevel": 1,
          "TargetLevel": 2,
          "Next": {
            "Type": "CheckUniqueItemUsage",
            "Next": {
              "Type": "skill_<nom_du_skill>_b",
              "Next": {
                "Type": "ModifyInventory",
                "AdjustHeldItemQuantity": -1
              }
            }
          }
        }
      ]
    }
  },
  "Tags": {
    "Family": ["Skill"],
    "Type": ["Upgrade"]
  }
}
```

### 4. Enregistrer les nouvelles interactions

Dans `LheidoSkillsPlugin.java`, méthode `setup()`:
```java
// Interaction de vérification des prérequis
this.getCodecRegistry(Interaction.CODEC).register(
    "check_<nom_du_skill>_upgrade",
    Check<NomDuSkill>UpgradeInteraction.class,
    Check<NomDuSkill>UpgradeInteraction.CODEC
);

// Interaction d'upgrade niveau B
this.getCodecRegistry(Interaction.CODEC).register(
    "skill_<nom_du_skill>_b",
    Skill<NomDuSkill>BInteraction.class,
    Skill<NomDuSkill>BInteraction.CODEC
);
```

---

## Convention de nommage

| Élément | Format | Exemple |
|---------|--------|---------|
| Fichier JSON | `Skill_<NomDuSkill>_<Niveau>.json` | `Skill_Flying_B.json` |
| Component | `<NomDuSkill>SkillComponent` | `FlyingSkillComponent` |
| Interaction niveau A | `Skill<NomDuSkill>Interaction` | `SkillFlyingInteraction` |
| Interaction upgrade | `Skill<NomDuSkill><Niveau>Interaction` | `SkillFlyingBInteraction` |
| Check upgrade | `Check<NomDuSkill>UpgradeInteraction` | `CheckFlyingUpgradeInteraction` |
| ID interaction | `skill_<nom_du_skill>` ou `skill_<nom_du_skill>_<niveau>` | `skill_flying_b` |
| ID check | `check_<nom_du_skill>_upgrade` | `check_flying_upgrade` |
| Niveaux | A=1, B=2, C=3, X=ultime | A = niveau 1 |
| Quality par niveau | A=Uncommon, B=Rare, C=Epic, X=Legendary | |

## Paramètres personnalisables

| Paramètre | Description | Exemple par niveau |
|-----------|-------------|-------------------|
| `Quality` | Rareté de l'item | A=Uncommon, B=Rare, C=Epic, X=Legendary |
| `ItemLevel` | Niveau requis | A=20, B=30, C=40, X=50 |
| `Recipe.Input[0].Quantity` | Coût en Skill Essence | A=100, B=200, C=400, X=1000 |
| `Recipe.TimeSeconds` | Temps de craft | A=2, B=3, C=4, X=5 |
| `RequiredLevel` | Niveau requis pour upgrade (dans JSON) | B=1, C=2, X=3 |
| `TargetLevel` | Niveau cible de l'upgrade (dans JSON) | B=2, C=3, X=4 |

## Exemple complet : Skill Flying

Le skill Flying est implémenté avec 4 niveaux:

| Niveau | Fichier | Interaction | Coût | Durée vol | Cooldown |
|--------|---------|-------------|------|-----------|----------|
| A (1) | `Skill_Flying_A.json` | `skill_flying` | 100 | 10s | 20s |
| B (2) | `Skill_Flying_B.json` | `skill_flying_b` | 200 | 15s | 18s |
| C (3) | `Skill_Flying_C.json` | `skill_flying_c` | 400 | 20s | 15s |
| X (ultime) | `Skill_Flying_X.json` | `skill_flying_x` | 1000 | Illimité | 0s |

Fichiers Java:
- `FlyingSkillComponent.java` - Component avec niveau et timers
- `SkillFlyingInteraction.java` - Déblocage niveau A
- `SkillFlyingBInteraction.java` - Upgrade vers B
- `SkillFlyingCInteraction.java` - Upgrade vers C
- `SkillFlyingXInteraction.java` - Upgrade vers X (ultime)
- `CheckFlyingUpgradeInteraction.java` - Vérification des prérequis
- `FlyingSystem.java` - Logique de vol (activation/désactivation)

---

## API EntityStats (Modifier les stats du joueur)

Hytale fournit une API pour manipuler les stats des entités (santé, oxygène, stamina, etc.) via le système `EntityStatMap`.

### Classes principales

| Classe | Package | Description |
|--------|---------|-------------|
| `EntityStatMap` | `com.hypixel.hytale.server.core.modules.entitystats` | Component attaché aux entités, stocke les valeurs des stats |
| `EntityStatsModule` | `com.hypixel.hytale.server.core.modules.entitystats` | Module singleton pour accéder au ComponentType |
| `DefaultEntityStatTypes` | `com.hypixel.hytale.server.core.modules.entitystats.asset` | Stats par défaut (Health, Oxygen, Stamina, etc.) |
| `StaticModifier` | `com.hypixel.hytale.server.core.modules.entitystats.modifier` | Modificateur de stat (additif ou multiplicatif) |
| `Modifier` | `com.hypixel.hytale.server.core.modules.entitystats.modifier` | Classe de base pour les modificateurs |

### Stats disponibles par défaut

```java
int healthIndex = DefaultEntityStatTypes.getHealth();
int oxygenIndex = DefaultEntityStatTypes.getOxygen();
int staminaIndex = DefaultEntityStatTypes.getStamina();
int manaIndex = DefaultEntityStatTypes.getMana();
int signatureEnergyIndex = DefaultEntityStatTypes.getSignatureEnergy();
int ammoIndex = DefaultEntityStatTypes.getAmmo();
```

### Récupérer l'EntityStatMap d'une entité

```java
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;

// Dans un System ou une Interaction
ComponentType<EntityStore, EntityStatMap> statMapType =
    EntityStatsModule.get().getEntityStatMapComponentType();

EntityStatMap statMap = commandBuffer.getComponent(entityRef, statMapType);
```

### Ajouter un modificateur sur une stat

```java
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

// Créer un modificateur multiplicatif sur la valeur MAX d'une stat
StaticModifier oxygenModifier = new StaticModifier(
    Modifier.ModifierTarget.MAX,           // Cible: MAX, MIN, ou VALUE
    StaticModifier.CalculationType.MULTIPLICATIVE,  // ADDITIVE ou MULTIPLICATIVE
    1.5f  // Valeur du modificateur (ici x1.5)
);

// Appliquer le modificateur avec un ID unique
String modifierId = "my_skill_modifier";
int oxygenIndex = DefaultEntityStatTypes.getOxygen();

Modifier previousModifier = statMap.putModifier(
    EntityStatMap.Predictable.NONE,
    oxygenIndex,
    modifierId,
    oxygenModifier
);

// previousModifier est null si c'est un nouveau modifier,
// sinon c'est l'ancien modifier qui a été remplacé
```

### Types de modificateurs

| ModifierTarget | Description |
|----------------|-------------|
| `MAX` | Modifie la valeur maximale de la stat |
| `MIN` | Modifie la valeur minimale de la stat |
| `VALUE` | Modifie la valeur courante de la stat |

| CalculationType | Description |
|-----------------|-------------|
| `ADDITIVE` | Ajoute la valeur au total |
| `MULTIPLICATIVE` | Multiplie la valeur |

### Supprimer un modificateur

```java
Modifier removed = statMap.removeModifier(
    EntityStatMap.Predictable.NONE,
    oxygenIndex,
    modifierId
);
```

### Exemple complet : WaterBreathingSystem

Le skill WaterBreathing utilise l'API EntityStats pour augmenter l'oxygène max du joueur :

```java
package lheido.skills.systems;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

public class WaterBreathingSystem extends EntityTickingSystem<EntityStore> {
    
    private static final String MODIFIER_ID = "lheido_water_breathing";

    @Override
    public void tick(...) {
        // Récupérer le component du skill
        WaterBreathingSkillComponent skillComponent = ...;
        
        // Récupérer l'EntityStatMap
        ComponentType<EntityStore, EntityStatMap> statMapType =
            EntityStatsModule.get().getEntityStatMapComponentType();
        EntityStatMap statMap = commandBuffer.getComponent(entityRef, statMapType);
        
        // Récupérer l'index de la stat Oxygen
        int oxygenIndex = DefaultEntityStatTypes.getOxygen();
        
        // Calculer le multiplicateur selon le niveau du skill
        float multiplier = skillComponent.getOxygenMultiplier(); // 1.5, 2.0, 3.0, etc.
        
        // Créer et appliquer le modifier
        StaticModifier oxygenModifier = new StaticModifier(
            Modifier.ModifierTarget.MAX,
            StaticModifier.CalculationType.MULTIPLICATIVE,
            multiplier
        );
        
        statMap.putModifier(
            EntityStatMap.Predictable.NONE,
            oxygenIndex,
            MODIFIER_ID,
            oxygenModifier
        );
    }
}
```

---

## Documentation externe

- **Interactions:** https://hytalemodding.dev/en/docs/guides/plugin/item-interaction
- **Codec:** https://hytalemodding.dev/en/docs/guides/ecs/hytale-ecs-theory#codec

## Vérification

Après création, builder le projet:
```bash
./gradlew clean build
```
