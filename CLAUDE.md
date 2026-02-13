# LheidoSkills - Mod Hytale

Mod Hytale pour un système de skills/compétences.

## Aperçu du projet

- **Type:** Plugin serveur Hytale (JavaPlugin)
- **Point d'entrée:** `lheido.skills.LheidoSkillsPlugin`
- **Build:** `./gradlew clean build`
- **Dépendance:** `libs/HytaleServer.jar`

## Skills Claude disponibles

Consulter `.claude/skills/` pour les guides détaillés:

- **create-hytale-skill.md** - Guide pour créer un nouveau skill
- **project-structure.md** - Structure complète du projet

## Commandes rapides

### Créer un nouveau skill

Pour créer un skill, il faut:
1. Créer le JSON dans `src/main/resources/Server/Item/Items/Upgrades/`
2. Ajouter les traductions dans `src/main/resources/Server/Languages/en-US/server.lang`

Un skill doit être lié à du code java en utilisant le system d'intéraction.

```java
/**
 * Exemple d'interaction personnalisée pour un skill
 */
public class MyCustomInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<MyCustomInteraction> CODEC = BuilderCodec.builder(
            MyCustomInteraction.class, MyCustomInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nonnull CooldownHandler cooldownHandler) {
        // Custom behavior when the item is used
    }
}
```
Une intéraction est register dans la methode setup du plugin (`LheidoSkillsPlugin.java`):
```java
this.getCodecRegistry(Interaction.CODEC).register("my_custom_interaction_id", MyCustomInteraction.class, MyCustomInteraction.CODEC);
```
documentation interaction : https://hytalemodding.dev/en/docs/guides/plugin/item-interaction
document codec : https://hytalemodding.dev/en/docs/guides/ecs/hytale-ecs-theory#codec

### Build

```bash
./gradlew clean build
```

## Structure des items

| Dossier | Contenu |
|---------|---------|
| `Server/Item/Items/Ingredient/` | Ingrédients (ex: Skill Essence) |
| `Server/Item/Items/Upgrades/` | Skills/Upgrades |

## Convention de nommage des skills

- **Format:** `Skill_<NomDuSkill>_<Niveau>`
- **Niveaux:** A=1, B=2, C=3, etc.
- **Exemple:** `Skill_Flying_A` = Flying niveau 1
