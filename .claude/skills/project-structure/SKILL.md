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

## Architecture

Le projet utilise une architecture ECS (Entity Component System):

| Dossier | Description |
|---------|-------------|
| `interactions/` | Classes d'interaction pour les skills (comportement) |
| `components/` | Composants de données (à implémenter) |
| `systems/` | Logique de jeu (à implémenter) |

## Système d'interactions

Les skills utilisent le système d'interaction de Hytale pour définir leur comportement.

### Enregistrement d'une interaction

Dans `LheidoSkillsPlugin.java`, méthode `setup()`:
```java
this.getCodecRegistry(Interaction.CODEC).register("interaction_id", MyInteraction.class, MyInteraction.CODEC);
```

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
        // Comportement du skill
    }
}
```

## Documentation externe

- **Interactions:** https://hytalemodding.dev/en/docs/guides/plugin/item-interaction
- **Codec:** https://hytalemodding.dev/en/docs/guides/ecs/hytale-ecs-theory#codec
