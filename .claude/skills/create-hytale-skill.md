# Skill: Créer un nouveau Skill Hytale

Ce skill explique comment créer un nouveau skill dans le mod LheidoSkills pour Hytale.

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
        { "Type": "CheckUniqueItemUsage" }
      ]
    },
    "Secondary": {
      "Interactions": [
        { "Type": "CheckUniqueItemUsage" }
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

## Convention de nommage

- **Nom du fichier:** `Skill_<NomDuSkill>_<Niveau>.json`
- **Niveaux:** A, B, C, D... (A = niveau 1, B = niveau 2, etc.)
- **Traduction du nom:** `<Nom lisible> (<niveau en chiffre>)`

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

## Vérification

Après création, builder le projet:
```bash
./gradlew clean build
```
