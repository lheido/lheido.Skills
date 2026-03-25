# Unified NPC Asset Index

This document explains how to generate a single, exploitable NPC asset index from a Hytale asset dump.

## Goal

The purpose of the index is to consolidate NPC-related information found across the asset pack into one file that is easier to query for balancing work such as:

- mob tiering
- drop balancing
- hostility classification
- flying / aquatic detection
- future `Skill Essence` drop rules

The generated file is intended to be **descriptive**, not strongly opinionated.  
It should expose what exists in the assets without hardcoding gameplay tiers directly into the scraper.

---

## Recommended output

Generate a JSON file such as:

- `docs/npc-assets-index.json`
- or `build/generated/npc-assets-index.json`

JSON is preferred because it preserves nested metadata and source paths cleanly.

---

## Data sources to scan

The scraper should aggregate information from these asset folders when present:

- `Server/NPC/Roles/`
- `Server/NPC/Attitude/Roles/`
- `Server/Audio/AudioCategories/NPC/`
- `Server/Audio/SoundEvents/SFX/NPC/`
- `Server/Item/RootInteractions/NPCs/`

These folders expose complementary signals:

- canonical or semi-canonical NPC names
- taxonomy and role grouping
- attitude / behavioral hints
- sound grouping
- attack / interaction grouping

---

## Important design rule

The scraper should **not** hardcode gameplay tiers like:

- `Livestock => tier 0`
- `Undead => tier 2`
- `Void => tier 3`

Instead, it should:

1. extract facts
2. normalize them
3. expose them in one unified structure

Then a second step can map those facts to drop tiers.

This makes the system more maintainable when new assets or NPC variants are added.

---

## Suggested structure of the generated JSON

A good output shape looks like this:

```/dev/null/npc-assets-index.json#L1-36
{
  "generated_at": "2026-03-24T00:00:00Z",
  "assets_root": "/path/to/Assets",
  "sources_scanned": [
    "Server/NPC/Roles",
    "Server/NPC/Attitude/Roles",
    "Server/Audio/AudioCategories/NPC",
    "Server/Audio/SoundEvents/SFX/NPC",
    "Server/Item/RootInteractions/NPCs"
  ],
  "npcs": [
    {
      "npc_id": "Wolf",
      "normalized_id": "wolf",
      "variants": [
        "Wolf_Black",
        "Wolf_White"
      ],
      "sources": {
        "roles": [
          "Server/NPC/Roles/Creature/Mammal/Wolf_Black.json",
          "Server/NPC/Roles/Creature/Mammal/Wolf_White.json"
        ],
        "attitudes": [],
        "audio_categories": [
          "Server/Audio/AudioCategories/NPC/AudioCat_NPC_Wolf.json"
        ],
        "sound_events": [
          "Server/Audio/SoundEvents/SFX/NPC/Beasts/Wolf/..."
        ],
        "interactions": [
          "Server/Item/RootInteractions/NPCs/Creature/Mammal/Root_NPC_Wolf_Attack"
        ]
      },
      "taxonomy": {
        "role_segments": ["Creature", "Mammal"],
        "attitude_segments": [],
        "sound_segments": ["Beasts"],
        "interaction_segments": ["Creature", "Mammal"]
      },
      "flags": {
        "flying_like": false,
        "aquatic_like": false,
        "boss_like": false
      }
    }
  ]
}
```

---

## Suggested normalization strategy

Because assets often contain variants, the scraper should distinguish between:

- **variant name**
- **grouped NPC id**

Examples:

- `Wolf_Black` and `Wolf_White` should likely group under `Wolf`
- `Deer_Doe` and `Deer_Stag` may group under `Deer`
- `Skeleton_Praetorian` may stay distinct if needed
- `Toad_Rhino_Magma` may stay distinct or group under `Toad_Rhino`

The grouping strategy should be conservative and configurable.

### Recommended approach

Store both:

- `variant_name`
- `npc_id`

That way, downstream balancing can decide whether to merge or split them.

---

## Suggested derived fields

The scraper may derive generic flags from path segments, as long as the rules are externalized.

Examples:

- `flying_like`
- `aquatic_like`
- `undead_like`
- `void_like`
- `intelligent_like`
- `boss_like`

These should come from a rules file rather than being buried in code.

Example rule config:

```/dev/null/npc_scraper_rules.json#L1-18
{
  "flags": {
    "flying_like": ["Avian", "Aerial", "Birds", "Bat"],
    "aquatic_like": ["Aquatic", "Ocean", "Marine", "Freshwater", "Abyssal"],
    "undead_like": ["Undead", "Skeleton", "Zombie", "Wraith"],
    "void_like": ["Void"],
    "boss_like": ["Boss", "Dragon", "Praetorian"],
    "intelligent_like": ["Intelligent", "Goblin", "Trork", "Outlander", "Feran", "Klops"]
  }
}
```

This keeps the scraper flexible without forcing code changes for every balancing iteration.

---

## Generation process

### 1. Collect files

Walk each supported asset subtree and collect all relevant files.

### 2. Parse identity from filenames and paths

For each match:

- determine a variant name
- determine a normalized NPC id
- record the original relative path
- record the path segments that may indicate taxonomy

### 3. Merge by NPC id

Group all discovered sources under one record per normalized NPC id.

### 4. Preserve variants

Never discard variant names during merging.

### 5. Derive lightweight flags

Use path-segment rules from config to derive descriptive flags.

### 6. Write a single JSON file

Output a stable, sorted file for easy diffing.

---

## Recommended file layout in this project

Suggested helper files:

- `tools/scrape_hytale_npc_assets.py`
- `tools/npc_scraper_rules.json`
- `docs/npc-assets-index.md`
- generated output in either:
  - `docs/npc-assets-index.json`
  - or `build/generated/npc-assets-index.json`

### Recommendation

Use `build/generated/npc-assets-index.json` for generated output if you do not want to commit it.

Use `docs/npc-assets-index.json` if you want a versioned reference snapshot in Git.

---

## Example command

Example usage for a Python script:

```/dev/null/run-example.sh#L1-4
python3 tools/scrape_hytale_npc_assets.py \
  --assets-root "/var/home/lheido/Téléchargements/Assets (2)" \
  --rules tools/npc_scraper_rules.json \
  --output docs/npc-assets-index.json
```

---

## Suggested CLI options

A useful scraper interface should support:

- `--assets-root`
- `--rules`
- `--output`
- `--pretty`
- `--strict`
- `--include-unknown`

Example:

```/dev/null/help-example.txt#L1-8
python3 tools/scrape_hytale_npc_assets.py \
  --assets-root "/path/to/Assets" \
  --rules tools/npc_scraper_rules.json \
  --output build/generated/npc-assets-index.json \
  --pretty \
  --include-unknown
```

---

## Suggested JSON quality rules

The generated file should be:

- deterministic
- sorted
- stable across runs
- easy to diff
- easy to consume from Java, Python, or scripts

### Good practices
- sort arrays alphabetically where possible
- store project-relative or assets-relative paths
- preserve raw source paths
- do not silently discard ambiguous entries

---

## What the scraper should not do

The scraper should **not**:

- assign final gameplay tiers
- assign drop rates
- decide reward amounts
- discard variants too aggressively
- rely on a hardcoded closed list of mobs

That logic belongs in a second balancing step.

---

## How this will help the tier system later

Once the index exists, a tiering step can be built on top of fields like:

- `taxonomy.role_segments`
- `taxonomy.attitude_segments`
- `taxonomy.sound_segments`
- `flags.flying_like`
- `flags.aquatic_like`
- `flags.undead_like`
- `flags.void_like`
- `flags.boss_like`

That second step can then generate something like:

```/dev/null/npc-tier-map.json#L1-18
{
  "wolf": {
    "tier": 2,
    "drop_chance": 1.0,
    "base_quantity": 2
  },
  "rat": {
    "tier": 1,
    "drop_chance": 0.5,
    "base_quantity": 1
  },
  "dragon": {
    "tier": 4,
    "drop_chance": 1.0,
    "base_quantity": 5
  }
}
```

This separation keeps the pipeline clean:

1. scrape facts
2. define tiers
3. plug tiers into Java drop logic

---

## Summary

The unified NPC asset index should be a **single source of extracted facts** from the asset dump.

### Best practice
- keep the scraper generic
- keep rules configurable
- keep output rich and lossless
- do tiering in a separate step

That gives you a much better foundation for balancing `HEALTH_QUANTITY_THRESHOLDS`, hostility groups, and future drop rules.