# Skill: Structure du Projet LheidoSkills

Ce skill décrit la structure complète du projet mod Hytale "LheidoSkills".

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

## Architecture prévue

Le projet utilise une architecture ECS (Entity Component System):
- `components/` - Composants de données (à implémenter)
- `systems/` - Logique de jeu (à implémenter)
