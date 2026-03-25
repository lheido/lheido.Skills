# NPC Tier Map

This document describes how to derive a practical `npc-tier-map.json` from the generated asset index in `docs/npc-assets-index.json`.

## Goal

The tier map is the balancing layer that sits **after** scraping.

Pipeline:

1. scrape raw NPC facts into `docs/npc-assets-index.json`
2. derive gameplay tiers into `docs/npc-tier-map.json`
3. use that tier map in Java drop logic

The scraper stays descriptive.  
The tier map becomes the gameplay opinion.

---

## Recommended output shape

A good generated tier map looks like this:

```/dev/null/npc-tier-map.json#L1-42
{
  "generated_at": "2026-03-24T00:00:00Z",
  "source_index": "docs/npc-assets-index.json",
  "rules_version": 1,
  "tiers": {
    "0": {
      "label": "Passive / trivial",
      "drop_chance": 0.0,
      "base_quantity": 0
    },
    "1": {
      "label": "Low threat",
      "drop_chance": 0.35,
      "base_quantity": 1
    },
    "2": {
      "label": "Standard hostile",
      "drop_chance": 1.0,
      "base_quantity": 2
    },
    "3": {
      "label": "Elite / dangerous",
      "drop_chance": 1.0,
      "base_quantity": 3
    },
    "4": {
      "label": "Boss / exceptional",
      "drop_chance": 1.0,
      "base_quantity": 5
    }
  },
  "npcs": {
    "wolf": {
      "npc_id": "Wolf",
      "tier": 2,
      "drop_chance": 1.0,
      "base_quantity": 2,
      "reasons": ["predator_like", "family:Creature"]
    },
    "dragon": {
      "npc_id": "Dragon",
      "tier": 4,
      "drop_chance": 1.0,
      "base_quantity": 5,
      "reasons": ["family:Boss", "boss_like"]
    }
  }
}
```

---

## Recommended tier model

## Tier 0 — Passive / trivial

Examples:
- livestock
- critters
- prey
- harmless fauna

Suggested values:
- `drop_chance = 0.0`
- `base_quantity = 0`

Use this for:
- `PASSIVE`
- `livestock_like`
- `critter_like`
- obvious harmless fauna

---

## Tier 1 — Low threat

Examples:
- small vermin
- weak neutral creatures
- minor opportunistic mobs

Suggested values:
- `drop_chance = 0.35`
- `base_quantity = 1`

Use this for:
- `vermin_like`
- some `NEUTRAL`
- small dangerous fauna that should reward a little without being farm-optimal

---

## Tier 2 — Standard hostile

Examples:
- wolf
- goblin
- trork
- skeleton
- zombie
- regular predators

Suggested values:
- `drop_chance = 1.0`
- `base_quantity = 2`

Use this for:
- `HOSTILE`
- `predator_like`
- standard `Undead`
- standard `Intelligent`

---

## Tier 3 — Elite / dangerous

Examples:
- void mobs
- mythic creatures
- elementals / golems
- stronger intelligent factions
- heavy special mobs

Suggested values:
- `drop_chance = 1.0`
- `base_quantity = 3`

Use this for:
- `primary_family = Mythic`
- `primary_family = Void`
- `primary_family = Elemental`
- elite `Undead`
- elite `Intelligent`

---

## Tier 4 — Boss / exceptional

Examples:
- dragon
- true bosses
- rare named exceptional enemies

Suggested values:
- `drop_chance = 1.0`
- `base_quantity = 5`

Use this for:
- `primary_family = Boss`
- strong `boss_like` cases that you explicitly approve

---

## Recommended derivation order

Tiering should be deterministic and rule-based.

Apply rules in priority order:

### 1. Hard exclusions
If the NPC is clearly non-rewarding:
- passive livestock
- critters
- prey animals

=> `tier = 0`

### 2. Boss override
If:
- `primary_family == "Boss"`

=> `tier = 4`

### 3. Elite families
If:
- `primary_family in ["Mythic", "Void", "Elemental"]`

=> `tier = 3`

### 4. Standard hostiles
If:
- `primary_family in ["Undead", "Intelligent"]`
- or `HOSTILE` inferred
- or `predator_like`

=> `tier = 2`

### 5. Weak hostiles / neutral danger
If:
- `vermin_like`
- or `NEUTRAL`

=> `tier = 1`

### 6. Fallback
If nothing matches:
- either `tier = 1`
- or mark for manual review

For balancing safety, a review queue is recommended.

---

## Suggested rule table

A practical first-pass table:

```/dev/null/tier-rules.md#L1-20
Boss            -> tier 4
Mythic          -> tier 3
Void            -> tier 3
Elemental       -> tier 3
Undead          -> tier 2
Intelligent     -> tier 2
Predator        -> tier 2
Vermin          -> tier 1
Aquatic         -> tier 1 or 2 depending on hostility
Avian           -> tier 1 or 2 depending on hostility
Livestock       -> tier 0
Critter         -> tier 0
PASSIVE         -> tier 0
NEUTRAL         -> tier 1
HOSTILE         -> tier 2
```

This is intentionally simple and should be refined after playtesting.

---

## Suggested conflict resolution

Sometimes signals will disagree.

Example:
- `boss_like = true`
- but `primary_family = Undead`

Do not let weak heuristic flags override stronger structural classification automatically.

### Recommended priority
1. explicit family
2. inferred attitude
3. descriptive flags
4. manual override

That means:
- `primary_family = Boss` beats everything
- `primary_family = Elemental` beats a generic `HOSTILE`
- `passive_like` should beat weak predator hints for livestock animals

---

## Manual override support

The tier map generator should support a manual override file.

Example:

```/dev/null/npc-tier-overrides.json#L1-18
{
  "wolf": {
    "tier": 2,
    "drop_chance": 1.0,
    "base_quantity": 2,
    "reason": "Validated manually"
  },
  "dragon": {
    "tier": 4,
    "drop_chance": 1.0,
    "base_quantity": 6,
    "reason": "Boss reward override"
  }
}
```

This is useful because asset-derived heuristics will never be perfect.

---

## Suggested generation script

A second script should read:

- `docs/npc-assets-index.json`

and produce:

- `docs/npc-tier-map.json`

Recommended script path:

- `tools/generate_npc_tier_map.py`

Recommended optional config:

- `tools/npc_tier_rules.json`
- `tools/npc_tier_overrides.json`

---

## Example generation command

```/dev/null/generate-tier-map.sh#L1-4
python3 tools/generate_npc_tier_map.py \
  --index docs/npc-assets-index.json \
  --output docs/npc-tier-map.json \
  --pretty
```

With rules and overrides:

```/dev/null/generate-tier-map.sh#L1-6
python3 tools/generate_npc_tier_map.py \
  --index docs/npc-assets-index.json \
  --rules tools/npc_tier_rules.json \
  --overrides tools/npc_tier_overrides.json \
  --output docs/npc-tier-map.json \
  --pretty
```

---

## Recommended per-NPC output fields

Each NPC entry in the tier map should ideally contain:

- `npc_id`
- `tier`
- `drop_chance`
- `base_quantity`
- `reasons`
- `review_required`

Example:

```/dev/null/example-tier-entry.json#L1-12
{
  "npc_id": "Snake",
  "tier": 1,
  "drop_chance": 0.35,
  "base_quantity": 1,
  "reasons": [
    "vermin_like",
    "family:Creature",
    "attitude:HOSTILE"
  ],
  "review_required": false
}
```

For uncertain cases:

```/dev/null/example-tier-entry.json#L1-12
{
  "npc_id": "UnknownSpecialMob",
  "tier": 1,
  "drop_chance": 0.35,
  "base_quantity": 1,
  "reasons": [
    "fallback"
  ],
  "review_required": true
}
```

---

## Recommended review buckets

When generating the tier map, also compute review buckets such as:

- missing family
- missing attitude
- conflicting boss/passive signals
- too many variants
- only one weak heuristic matched

This makes balancing much easier.

Example:

```/dev/null/review-summary.json#L1-16
{
  "review_summary": {
    "needs_manual_review": 42,
    "missing_family": 18,
    "missing_attitude": 27,
    "fallback_assignments": 14
  }
}
```

---

## Mapping from tiers to drop values

A practical first-pass mapping:

```/dev/null/drop-balance-table.md#L1-10
Tier 0 -> chance 0.00, base 0
Tier 1 -> chance 0.35, base 1
Tier 2 -> chance 1.00, base 2
Tier 3 -> chance 1.00, base 3
Tier 4 -> chance 1.00, base 5
```

Then your Java drop system can still apply:
- health threshold bonuses
- flying bonus
- special overrides

So final quantity becomes:

```/dev/null/final-quantity.txt#L1-3
final_quantity =
  tier_base_quantity + health_bonus + flying_bonus
```

---

## Recommended integration with `SkillEssenceDropSystem`

Instead of deciding everything from:
- attitude
- health
- flying

the system should:
1. resolve an NPC identity
2. look up its tier data
3. use tier-based drop chance and base quantity
4. add health and mobility modifiers

That gives you a much better balancing model.

---

## Example Java-side usage idea

```/dev/null/usage-idea.txt#L1-7
1. Build MobDropProfile
2. Resolve npc normalized id
3. Read tier map entry
4. Apply drop chance from tier
5. Start quantity from tier base
6. Add health threshold bonus
7. Add flying bonus if desired
```

---

## Best practice

Keep these responsibilities separated:

### Scraper
- extracts facts
- does not decide gameplay rewards

### Tier generator
- decides gameplay meaning
- applies balancing rules
- emits reviewable output

### Java system
- consumes final tier data
- applies runtime drop logic

This separation will make iteration much easier.

---

## Summary

The NPC tier map should be a **second-stage generated artifact** built from `docs/npc-assets-index.json`.

Recommended next files:

- `tools/generate_npc_tier_map.py`
- `tools/npc_tier_rules.json`
- `tools/npc_tier_overrides.json`
- generated `docs/npc-tier-map.json`

Recommended next action:
- generate an initial tier map with conservative defaults
- review uncertain entries
- then wire it into `SkillEssenceDropSystem`
