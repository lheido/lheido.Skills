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
        │           ├── interactions/               # Interactions des skills
        │           ├── components/                 # Composants ECS (futur)
        │           └── systems/                    # Systèmes ECS (futur)
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
                │           └── Skill_Flying_A.json
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

| Item | Type | Chemin |
|------|------|--------|
| Skill Essence | Ingredient | `Server/Item/Items/Ingredient/Ingredient_Skill_Essence.json` |
| Flying (1) | Upgrade/Skill | `Server/Item/Items/Upgrades/Skill_Flying_A.json` |

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
| `interactions/` | Interactions pour ajouter les components au player |
| `systems/` | Systèmes ECS pour la logique de jeu (réagissent aux components) |

### Flux d'un skill

1. **Item utilisé** → L'interaction est déclenchée
2. **Interaction** → Ajoute le component au player
3. **Component** → Données persistantes sauvegardées via CODEC
4. **System** → Réagit au component pour la logique de jeu

## Système de CODEC (persistence)

Les CODECs permettent de sérialiser/désérialiser les données pour la sauvegarde.

### Enregistrement dans `LheidoSkillsPlugin.java`, méthode `setup()`:

```java
// Enregistrer un component (pour la persistence)
this.getCodecRegistry(Component.CODEC).register("component_id", MyComponent.class, MyComponent.CODEC);

// Enregistrer une interaction
this.getCodecRegistry(Interaction.CODEC).register("interaction_id", MyInteraction.class, MyInteraction.CODEC);
```

### Structure d'un component

```java
public class MySkillComponent implements Component {
    public static final BuilderCodec<MySkillComponent> CODEC = BuilderCodec.builder(
            MySkillComponent.class, MySkillComponent::new
    ).build();

    // Données du skill
}
```

## Système d'interactions

Les interactions permettent d'ajouter des components au player quand un item est utilisé.

### Structure d'une interaction

```java
public class MySkillInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<MySkillInteraction> CODEC = BuilderCodec.builder(
            MySkillInteraction.class, MySkillInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, 
                           @Nonnull InteractionContext interactionContext, 
                           @Nonnull CooldownHandler cooldownHandler) {
        // Ajouter le component au player
    }
}
```

## Documentation externe

- **Interactions:** https://hytalemodding.dev/en/docs/guides/plugin/item-interaction
- **Codec:** https://hytalemodding.dev/en/docs/guides/ecs/hytale-ecs-theory#codec
