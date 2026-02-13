---
name: create-hytale-skill
description: Guide pour créer un nouveau skill dans le mod LheidoSkills pour Hytale. Utiliser quand l'utilisateur demande de créer un skill, une compétence, ou un upgrade pour le mod.
---

# Créer un nouveau Skill Hytale

## Prérequis

- Le skill utilise le même icon/visuel que `Skill_Flying_A`
- Chaque skill est un item de type "Upgrade"
- Les skills sont craftables à l'Arcane Bench avec des Skill Essence

## Fichiers à créer/modifier

Pour créer un nouveau skill nommé `Skill_<NomDuSkill>_<Niveau>` (ex: `Skill_Swimming_A`):

### 1. Créer le fichier JSON du skill

**Chemin:** `src/main/resources/Server/Item/Items/Upgrades/Skill_<NomDuSkill>_<Niveau>.json`

**Template:**
```json
{
  "TranslationProperties": {
    "Name": "server.items.Skill_<NomDuSkill>_<Niveau>.name",
    "Description": "server.items.Skill_<NomDuSkill>_<Niveau>.description"
  },
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
        { "Type": "skill_<nom_du_skill>" }
      ]
    },
    "Secondary": {
      "Interactions": [
        { "Type": "skill_<nom_du_skill>" }
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

### 3. Créer l'interaction Java (comportement du skill)

Chaque skill doit être lié à du code Java via le système d'interaction.

**Chemin:** `src/main/java/lheido/skills/interactions/Skill<NomDuSkill>Interaction.java`

**Template:**
```java
package lheido.skills.interactions;

import com.hypixel.hytale.server.util.codec.BuilderCodec;
import com.hypixel.hytale.common.interaction.CooldownHandler;
import com.hypixel.hytale.common.interaction.InteractionContext;
import com.hypixel.hytale.common.interaction.InteractionType;
import com.hypixel.hytale.common.interaction.SimpleInstantInteraction;

import javax.annotation.Nonnull;

/**
 * Interaction pour le skill <NomDuSkill>
 */
public class Skill<NomDuSkill>Interaction extends SimpleInstantInteraction {
    public static final BuilderCodec<Skill<NomDuSkill>Interaction> CODEC = BuilderCodec.builder(
            Skill<NomDuSkill>Interaction.class, Skill<NomDuSkill>Interaction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nonnull CooldownHandler cooldownHandler) {
        // Comportement personnalisé quand le skill est utilisé
    }
}
```

### 4. Enregistrer l'interaction dans le plugin

**Chemin:** `src/main/java/lheido/skills/LheidoSkillsPlugin.java`

Ajouter dans la méthode `setup()`:
```java
this.getCodecRegistry(Interaction.CODEC).register("skill_<nom_du_skill>", Skill<NomDuSkill>Interaction.class, Skill<NomDuSkill>Interaction.CODEC);
```

## Convention de nommage

| Élément | Format | Exemple |
|---------|--------|---------|
| Fichier JSON | `Skill_<NomDuSkill>_<Niveau>.json` | `Skill_DoubleJump_A.json` |
| Classe Java | `Skill<NomDuSkill>Interaction` | `SkillDoubleJumpInteraction` |
| ID interaction | `skill_<nom_du_skill>` | `skill_double_jump` |
| Niveaux | A, B, C, D... (A=1, B=2, etc.) | A = niveau 1 |
| Traduction | `<Nom lisible> (<niveau>)` | `Double Jump (1)` |

## Paramètres personnalisables

| Paramètre | Description | Valeur par défaut |
|-----------|-------------|-------------------|
| `Quality` | Rareté de l'item | `"Uncommon"` |
| `ItemLevel` | Niveau requis | `20` |
| `Recipe.Input[0].Quantity` | Coût en Skill Essence | `100` |
| `Recipe.TimeSeconds` | Temps de craft | `2` |
| `Recipe.BenchRequirement[0].RequiredTierLevel` | Tier du bench requis | `1` |

## Exemple complet

Pour créer un skill "Double Jump" niveau A:

1. Créer `src/main/resources/Server/Item/Items/Upgrades/Skill_DoubleJump_A.json`
2. Ajouter dans `server.lang`:
   ```properties
   items.Skill_DoubleJump_A.name=Double Jump (1)
   items.Skill_DoubleJump_A.description=Allows the player to jump twice in the air
   ```
3. Créer `src/main/java/lheido/skills/interactions/SkillDoubleJumpInteraction.java`
4. Enregistrer dans `LheidoSkillsPlugin.java`:
   ```java
   this.getCodecRegistry(Interaction.CODEC).register("skill_double_jump", SkillDoubleJumpInteraction.class, SkillDoubleJumpInteraction.CODEC);
   ```

## Documentation externe

- **Interactions:** https://hytalemodding.dev/en/docs/guides/plugin/item-interaction
- **Codec:** https://hytalemodding.dev/en/docs/guides/ecs/hytale-ecs-theory#codec

## Vérification

Après création, builder le projet:
```bash
./gradlew clean build
```
