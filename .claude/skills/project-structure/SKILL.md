---
name: project-structure
description: Structure complète du projet LheidoSkills pour Hytale. Utiliser pour comprendre l'organisation des fichiers, l'architecture du mod, et localiser les éléments du projet.
---

# Structure du Projet LheidoSkills

## Arborescence du projet

```
lheido.Skills/
├── .claude/
│   └── skills/                         # Skills Claude pour ce projet
│       ├── create-hytale-skill/        # Guide pour créer un skill
│       └── project-structure/          # Ce fichier
├── build.gradle.kts                    # Configuration Gradle (Kotlin DSL)
├── settings.gradle.kts                 # Nom du projet: LheidoSkillsPlugin
├── gradlew                             # Script Gradle wrapper
├── libs/
│   └── HytaleServer.jar                # API Hytale Server (seule dépendance)
└── src/
    └── main/
        ├── java/
        │   └── lheido/
        │       └── skills/
        │           ├── LheidoSkillsPlugin.java    # Point d'entrée du plugin
        │           ├── components/                 # Composants ECS
        │           │   └── FlyingSkillComponent.java
        │           ├── interactions/               # Interactions des skills
        │           │   ├── CheckFlyingUpgradeInteraction.java
        │           │   ├── SkillFlyingInteraction.java
        │           │   ├── SkillFlyingBInteraction.java
        │           │   ├── SkillFlyingCInteraction.java
        │           │   └── SkillFlyingXInteraction.java
        │           └── systems/                    # Systèmes ECS
        │               ├── FlyingSystem.java
        │               └── SkillEssenceDropSystem.java
        └── resources/
            ├── manifest.json                       # Manifest du mod
            ├── Common/
            │   ├── Icons/
            │   │   └── ItemsGenerated/             # Icônes des items
            │   │       ├── Ingredient_Skill_Essence.png
            │   │       └── Skill_Flying_A.png
            │   └── Resources/
            │       └── Skill_Essence_Texture.png   # Textures
            └── Server/
                ├── Item/
                │   └── Items/
                │       ├── Ingredient/             # Items ingrédients
                │       │   └── Ingredient_Skill_Essence.json
                │       └── Upgrades/               # Items skills/upgrades
                │           ├── Skill_Flying_A.json
                │           ├── Skill_Flying_B.json
                │           ├── Skill_Flying_C.json
                │           └── Skill_Flying_X.json
                └── Languages/
                    └── en-US/
                        └── server.lang             # Traductions anglaises
```

## Fichiers clés

### Point d'entrée Java
- **Chemin:** `src/main/java/lheido/skills/LheidoSkillsPlugin.java`
- **Classe:** `LheidoSkillsPlugin extends JavaPlugin`

### Manifest du mod
- **Chemin:** `src/main/resources/manifest.json`
- **Group:** `lheido.skills`
- **Name:** `LheidoSkills`
- **Main:** `lheido.skills.LheidoSkillsPlugin`

### Items existants

| Item | Type | Chemin | Description |
|------|------|--------|-------------|
| Skill Essence | Ingredient | `Server/Item/Items/Ingredient/Ingredient_Skill_Essence.json` | Ressource pour crafter les skills |
| Flying (1) | Upgrade | `Server/Item/Items/Upgrades/Skill_Flying_A.json` | Vol 10s, cooldown 20s |
| Flying (2) | Upgrade | `Server/Item/Items/Upgrades/Skill_Flying_B.json` | Vol 15s, cooldown 18s |
| Flying (3) | Upgrade | `Server/Item/Items/Upgrades/Skill_Flying_C.json` | Vol 20s, cooldown 15s |
| Flying (X) | Upgrade | `Server/Item/Items/Upgrades/Skill_Flying_X.json` | Vol illimité, sans cooldown |

### Interactions enregistrées

| ID | Classe | Description |
|----|--------|-------------|
| `skill_flying` | `SkillFlyingInteraction` | Déblocage du skill Flying niveau A |
| `skill_flying_b` | `SkillFlyingBInteraction` | Upgrade vers niveau B |
| `skill_flying_c` | `SkillFlyingCInteraction` | Upgrade vers niveau C |
| `skill_flying_x` | `SkillFlyingXInteraction` | Upgrade vers niveau X (ultime) |
| `check_flying_upgrade` | `CheckFlyingUpgradeInteraction` | Vérification des prérequis d'upgrade |

### Components enregistrés

| Classe | Description |
|--------|-------------|
| `FlyingSkillComponent` | Données du skill Flying (niveau, durée, cooldown, état) |

### Systems enregistrés

| Classe | Description |
|--------|-------------|
| `FlyingSystem` | Logique de vol (activation, timer, cooldown) |
| `SkillEssenceDropSystem` | Drop de Skill Essence quand les NPCs meurent |

### Traductions
- **Chemin:** `src/main/resources/Server/Languages/en-US/server.lang`
- **Format:** `items.<ItemId>.name=<Nom traduit>`

## Build

```bash
./gradlew clean build
```

## Configuration technique

- **Java:** 25 (via toolchain)
- **Gradle:** 9.2.0
- **Dépendance:** `libs/HytaleServer.jar` (local)

## Architecture ECS (Entity Component System)

Le projet utilise une architecture ECS pour les skills:

| Dossier | Description |
|---------|-------------|
| `components/` | Components ECS attachés au player (données persistantes du skill) |
| `interactions/` | Interactions pour ajouter/modifier les components au player |
| `systems/` | Systèmes ECS pour la logique de jeu (réagissent aux components) |

### Flux d'un skill (premier niveau)

1. **Item utilisé** → L'interaction est déclenchée
2. **CheckUniqueItemUsage** → Vérifie que l'item n'a pas déjà été utilisé
3. **Interaction** → Ajoute le component au player
4. **ModifyInventory** → Supprime l'item de l'inventaire
5. **Component** → Données persistantes sauvegardées via CODEC
6. **System** → Réagit au component pour la logique de jeu

### Flux d'un upgrade

1. **Item utilisé** → L'interaction est déclenchée
2. **Check*UpgradeInteraction** → Vérifie les prérequis (niveau requis)
3. **CheckUniqueItemUsage** → Marque l'item comme utilisé (seulement si prérequis OK)
4. **Skill*Interaction** → Met à jour le component (nouveau niveau)
5. **ModifyInventory** → Supprime l'item de l'inventaire

**IMPORTANT:** L'ordre est crucial! Le check des prérequis doit être AVANT `CheckUniqueItemUsage` pour éviter de bloquer l'item si les prérequis ne sont pas remplis.

## Système de CODEC (persistence)

Les CODECs permettent de sérialiser/désérialiser les données pour la sauvegarde.

### Enregistrement dans `LheidoSkillsPlugin.java`, méthode `setup()`:

```java
// Enregistrer une interaction
this.getCodecRegistry(Interaction.CODEC).register(
    "interaction_id",
    MyInteraction.class,
    MyInteraction.CODEC
);

// Enregistrer un component avec son ComponentType (pour la persistence)
ComponentType<EntityStore, MySkillComponent> componentType =
    this.getEntityStoreRegistry().registerComponent(
        MySkillComponent.class,
        "MySkillComponent",
        MySkillComponent.CODEC
    );
MySkillComponent.setComponentType(componentType);

// Enregistrer un système
this.getEntityStoreRegistry().registerSystem(new MySystem());
```

### Structure d'un component avec CODEC paramétrique

```java
public class MySkillComponent implements Component {

    public static final BuilderCodec<MySkillComponent> CODEC =
        BuilderCodec.builder(MySkillComponent.class, MySkillComponent::new)
        .append(
            new KeyedCodec<>("level", Codec.INTEGER),
            (data, value) -> data.level = value,
            data -> data.level
        )
        .add()
        .build();

    private static ComponentType<EntityStore, MySkillComponent> componentType;
    private int level = 1;

    public static ComponentType<EntityStore, MySkillComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, MySkillComponent> type) {
        componentType = type;
    }

    // Getters, setters, factory methods...
}
```

## Système d'interactions

### Interaction simple (niveau A)
```java
public class MySkillInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<MySkillInteraction> CODEC =
        BuilderCodec.builder(
            MySkillInteraction.class,
            MySkillInteraction::new,
            SimpleInstantInteraction.CODEC
        ).build();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, 
                           @Nonnull InteractionContext interactionContext, 
                           @Nonnull CooldownHandler cooldownHandler) {
        // Ajouter le component au player
    }
}
```

### Interaction paramétrique (check upgrade)

Pour les interactions configurables via JSON:
```java
public class CheckMySkillUpgradeInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<CheckMySkillUpgradeInteraction> CODEC =
        BuilderCodec.builder(
            CheckMySkillUpgradeInteraction.class,
            CheckMySkillUpgradeInteraction::new,
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

    private int requiredLevel = 0;
    private int targetLevel = 1;

    @Override
    protected void firstRun(...) {
        // Vérifier les prérequis avec requiredLevel et targetLevel
    }
}
```

Utilisation dans le JSON:
```json
{
  "Type": "check_my_skill_upgrade",
  "RequiredLevel": 1,
  "TargetLevel": 2,
  "Next": { ... }
}
```

## Chaîne d'interactions dans les JSON

### Pour un skill niveau A (sans prérequis)
```json
"Interactions": [
  {
    "Type": "CheckUniqueItemUsage",
    "Next": {
      "Type": "skill_my_skill",
      "Next": {
        "Type": "ModifyInventory",
        "AdjustHeldItemQuantity": -1
      }
    }
  }
]
```

### Pour un upgrade (avec prérequis)
```json
"Interactions": [
  {
    "Type": "check_my_skill_upgrade",
    "RequiredLevel": 1,
    "TargetLevel": 2,
    "Next": {
      "Type": "CheckUniqueItemUsage",
      "Next": {
        "Type": "skill_my_skill_b",
        "Next": {
          "Type": "ModifyInventory",
          "AdjustHeldItemQuantity": -1
        }
      }
    }
  }
]
```

## Documentation externe

- **Interactions:** https://hytalemodding.dev/en/docs/guides/plugin/item-interaction
- **Codec:** https://hytalemodding.dev/en/docs/guides/ecs/hytale-ecs-theory#codec
