#!/usr/bin/env python3
"""
Build a unified NPC asset index from a Hytale asset dump.

This scraper is intentionally descriptive rather than prescriptive:
- it aggregates NPC-related facts from several asset trees
- it keeps source paths and variants
- it derives lightweight flags from configurable rules
- it does NOT assign gameplay tiers directly

Example:
    python3 tools/scrape_hytale_npc_assets.py \
      --assets-root "/var/home/lheido/Téléchargements/Assets (2)" \
      --rules tools/npc_scraper_rules.json \
      --output docs/npc-assets-index.json \
      --pretty
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

DEFAULT_RULES_PATH = "tools/npc_scraper_rules.json"

ROLE_FILENAME_PREFIXES = ()
AUDIO_CATEGORY_PREFIX = "AudioCat_NPC_"
INTERACTION_ROOT_PREFIX = "Root_NPC_"

ALNUM_SPLIT_RE = re.compile(r"[^A-Za-z0-9]+")
CAMEL_SPLIT_RE = re.compile(r"(?<!^)(?=[A-Z])")

INTERACTION_SUFFIX_RE = re.compile(
    r"_(?:"
    r"Attack|Death|Shield_Block|Summon|Charge|BasicAttacks|EquipSecondSword|"
    r"Stomp|Swipe(?:_Left|_Right)?|Scratch(?:_Left|_Right)?|"
    r"Bite|Blast|Kick|Ram|Headbutt|Throw(?:_Double)?|"
    r"Swing(?:_Up|_Down|_Left|_Right)?|Stab|Burst(?:_Left|_Right)?|Nova"
    r")$"
)

NON_NPC_NAME_RE = re.compile(
    r"^(?:"
    r"Attack(?:_|$)|"
    r"BlankTemplate$|"
    r"Component(?:_|$)|"
    r"Death$|"
    r"Empty$|"
    r"Spawn(?:_|$)"
    r")"
)

ATTITUDE_ONLY_NAMES = {
    "Aggressive",
    "Critters",
    "Empty",
    "Neutral",
    "Passive",
    "Predators",
    "PredatorsBig",
    "Prey",
    "PreyBig",
    "Vermin",
    "Void",
}

TECHNICAL_PATH_SEGMENTS = {
    "OLD_INTERACTIONS",
    "Templates",
}

TAMED_PREFIX = "Tamed_"


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def stable_sorted(values: Iterable[str]) -> list[str]:
    return sorted(set(v for v in values if v))


def lower_tokenize(value: str) -> list[str]:
    if not value:
        return []
    normalized = value.replace("-", "_").replace(" ", "_")
    parts = [p for p in normalized.split("_") if p]
    tokens: list[str] = []
    for part in parts:
        camel_parts = CAMEL_SPLIT_RE.split(part)
        for token in camel_parts:
            token = token.strip().lower()
            if token:
                tokens.append(token)
    return tokens


def path_to_posix(path: Path) -> str:
    return path.as_posix()


def unique_extend(target: list[str], values: Iterable[str]) -> None:
    seen = set(target)
    for value in values:
        if value and value not in seen:
            target.append(value)
            seen.add(value)


@dataclass
class SourceEntry:
    source_type: str
    rel_path: str
    variant_name: str
    grouped_name: str
    path_segments: list[str]
    inferred_segments: list[str] = field(default_factory=list)
    variant_kind: str = "entity"


@dataclass
class NPCRecord:
    npc_id: str
    normalized_id: str
    variants: set[str] = field(default_factory=set)
    source_paths: dict[str, set[str]] = field(default_factory=lambda: defaultdict(set))
    taxonomy_segments: dict[str, set[str]] = field(
        default_factory=lambda: {
            "role_segments": set(),
            "attitude_segments": set(),
            "audio_category_segments": set(),
            "sound_segments": set(),
            "interaction_segments": set(),
            "all_segments": set(),
        }
    )
    derived_flags: dict[str, bool] = field(default_factory=dict)
    inferred_attitudes: set[str] = field(default_factory=set)
    raw_entries: list[dict[str, Any]] = field(default_factory=list)
    interaction_names: set[str] = field(default_factory=set)

    def add_entry(self, entry: SourceEntry, include_raw_segments: bool) -> None:
        if entry.variant_kind == "entity":
            self.variants.add(entry.variant_name)
        elif entry.variant_kind == "interaction":
            self.interaction_names.add(entry.variant_name)

        self.source_paths[entry.source_type].add(entry.rel_path)

        segment_field_map = {
            "roles": "role_segments",
            "attitudes": "attitude_segments",
            "audio_categories": "audio_category_segments",
            "sound_events": "sound_segments",
            "interactions": "interaction_segments",
        }
        field_name = segment_field_map.get(entry.source_type)
        if field_name:
            self.taxonomy_segments[field_name].update(entry.path_segments)
            self.taxonomy_segments["all_segments"].update(entry.path_segments)
            self.taxonomy_segments["all_segments"].update(entry.inferred_segments)

        if include_raw_segments:
            self.raw_entries.append(
                {
                    "source_type": entry.source_type,
                    "path": entry.rel_path,
                    "variant_name": entry.variant_name,
                    "grouped_name": entry.grouped_name,
                    "path_segments": sorted(set(entry.path_segments)),
                    "inferred_segments": sorted(set(entry.inferred_segments)),
                    "variant_kind": entry.variant_kind,
                }
            )


class ScraperError(Exception):
    pass


class RuleSet:
    def __init__(self, data: dict[str, Any]) -> None:
        self.data = data
        self.normalization = data.get("normalization", {})
        self.sources = data.get("sources", {})
        self.flags = data.get("flags", {})
        self.attitude_inference = data.get("attitude_inference", {})
        self.family_priority = data.get("family_priority", [])
        self.output = data.get("output", {})

        self.variant_suffixes = set(self.normalization.get("variant_suffixes", []))
        self.stop_segments = set(self.normalization.get("stop_segments", []))
        self.ignored_path_segments = set(
            self.normalization.get("ignored_path_segments", [])
        )

    @classmethod
    def load(cls, path: Path) -> "RuleSet":
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except FileNotFoundError as exc:
            raise ScraperError(f"Rules file not found: {path}") from exc
        except json.JSONDecodeError as exc:
            raise ScraperError(f"Invalid JSON in rules file {path}: {exc}") from exc
        return cls(data)

    def source_roots(self) -> dict[str, list[str]]:
        result: dict[str, list[str]] = {}
        for source_name, source_config in self.sources.items():
            if not source_config.get("enabled", True):
                continue
            roots = source_config.get("roots", [])
            result[source_name] = [r.strip("/").replace("\\", "/") for r in roots if r]
        return result

    def include_raw_segments(self) -> bool:
        return bool(self.output.get("include_raw_segments", True))

    def include_source_paths(self) -> bool:
        return bool(self.output.get("include_source_paths", True))

    def include_variants(self) -> bool:
        return bool(self.output.get("include_variants", True))

    def include_unmatched_records(self) -> bool:
        return bool(self.output.get("include_unmatched_records", True))


def compute_relative_path(root: Path, path: Path) -> str:
    return path.relative_to(root).as_posix()


def normalize_segment(segment: str) -> str:
    return segment.strip().replace("-", "_").replace(" ", "_")


def strip_known_prefixes(name: str) -> str:
    if name.startswith(TAMED_PREFIX):
        return name[len(TAMED_PREFIX) :]
    return name


def collect_path_segments(
    rel_path: str,
    source_root: str,
    rules: RuleSet,
    include_filename: bool = False,
) -> list[str]:
    rel_parts = Path(rel_path).parts
    root_parts = Path(source_root).parts
    parts = list(rel_parts[len(root_parts) :])

    if not include_filename and parts:
        parts = parts[:-1]

    segments: list[str] = []
    for part in parts:
        normalized = normalize_segment(part)
        if not normalized or normalized in rules.ignored_path_segments:
            continue
        if normalized.endswith(".json"):
            normalized = normalized[:-5]
        if (
            normalized
            and normalized not in rules.stop_segments
            and normalized not in TECHNICAL_PATH_SEGMENTS
        ):
            segments.append(normalized)
    return stable_sorted(segments)


def strip_extension(name: str) -> str:
    return name[:-5] if name.endswith(".json") else name


def strip_prefix(value: str, prefix: str) -> str:
    if value.startswith(prefix):
        return value[len(prefix) :]
    return value


def clean_interaction_name(name: str) -> str:
    name = strip_extension(name)
    name = strip_prefix(name, INTERACTION_ROOT_PREFIX)
    name = INTERACTION_SUFFIX_RE.sub("", name)
    return name


def split_variant_tokens(name: str) -> list[str]:
    return [
        token for token in strip_extension(name).replace("-", "_").split("_") if token
    ]


def normalize_grouped_name(name: str, rules: RuleSet) -> str:
    name = strip_known_prefixes(name)
    tokens = split_variant_tokens(name)
    if not tokens:
        return name

    while len(tokens) > 1 and tokens[-1] in rules.variant_suffixes:
        tokens.pop()

    if len(tokens) >= 2 and tokens[-2:] == ["Man", "Of"]:
        tokens = tokens[:-2]

    if not tokens:
        return name
    return "_".join(tokens)


def maybe_simplify_group_name(grouped_name: str, source_segments: list[str]) -> str:
    """
    Conservative final simplification for common grouped families.

    Examples:
    - Deer_Doe / Deer_Stag -> Deer
    - Wolf_Black / Wolf_White -> Wolf
    - Owl_Brown / Owl_Snow -> Owl
    - Tang_Blue / Tang_Sailfin -> Tang
    """
    tokens = split_variant_tokens(grouped_name)
    if len(tokens) <= 1:
        return grouped_name

    protected_pairs = {
        ("Toad", "Rhino"),
        ("Eye", "Void"),
        ("Spawn", "Void"),
        ("Crawler", "Void"),
        ("Bear", "Grizzly"),
        ("Bear", "Polar"),
        ("Tiger", "Sabertooth"),
        ("Leopard", "Snow"),
        ("Moose", "Bull"),
        ("Moose", "Cow"),
        ("Dragon", "Fire"),
        ("Dragon", "Frost"),
        ("Golem", "Earth"),
        ("Golem", "Frost"),
        ("Golem", "Sand"),
        ("Golem", "Firesteel"),
        ("Skeleton", "Praetorian"),
        ("Raptor", "Cave"),
        ("Rex", "Cave"),
    }
    if tuple(tokens[:2]) in protected_pairs:
        return grouped_name

    taxon_segments = set(source_segments)
    if any(
        segment in taxon_segments
        for segment in [
            "Avian",
            "Aquatic",
            "Aerial",
            "Marine",
            "Freshwater",
            "Abyssal",
            "Creature",
            "Mammal",
            "Livestock",
            "Critter",
            "Reptile",
            "Vermin",
            "Tamed",
        ]
    ):
        return tokens[0]

    return grouped_name


def infer_variant_and_group(
    source_type: str,
    rel_path: str,
    source_root: str,
    rules: RuleSet,
) -> tuple[str, str, str]:
    path = Path(rel_path)
    name = strip_extension(path.name)

    if source_type == "audio_categories":
        variant_name = strip_prefix(name, AUDIO_CATEGORY_PREFIX)
        grouped_name = normalize_grouped_name(variant_name, rules)
        return variant_name, maybe_simplify_group_name(grouped_name, []), "entity"

    if source_type == "sound_events":
        root_parts = Path(source_root).parts
        path_parts = path.parts
        remainder = path_parts[len(root_parts) :]
        variant_name = ""
        if len(remainder) >= 2:
            variant_name = remainder[1]
        elif len(remainder) >= 1:
            variant_name = remainder[0]
        if not variant_name:
            variant_name = strip_extension(path.name)
        grouped_name = normalize_grouped_name(variant_name, rules)
        return (
            variant_name,
            maybe_simplify_group_name(grouped_name, list(remainder[:-1])),
            "entity",
        )

    if source_type == "interactions":
        variant_name = clean_interaction_name(path.name)
        grouped_name = normalize_grouped_name(variant_name, rules)
        return (
            variant_name,
            maybe_simplify_group_name(grouped_name, list(path.parts)),
            "interaction",
        )

    variant_name = name
    grouped_name = normalize_grouped_name(variant_name, rules)
    return (
        variant_name,
        maybe_simplify_group_name(grouped_name, list(path.parts)),
        "entity",
    )


def normalized_id_from_name(name: str) -> str:
    tokens = lower_tokenize(strip_known_prefixes(strip_extension(name)))
    return "_".join(tokens)


def gather_inferred_segments(entry: SourceEntry) -> list[str]:
    name_tokens = split_variant_tokens(strip_known_prefixes(entry.variant_name))
    grouped_tokens = split_variant_tokens(entry.grouped_name)
    values = []
    values.extend(
        segment
        for segment in entry.path_segments
        if segment not in TECHNICAL_PATH_SEGMENTS
    )
    values.extend(name_tokens)
    values.extend(grouped_tokens)
    return stable_sorted(values)


def is_attitude_only_entry(entry: SourceEntry) -> bool:
    return (
        entry.source_type == "attitudes" and entry.grouped_name in ATTITUDE_ONLY_NAMES
    )


def is_valid_npc_entry(entry: SourceEntry) -> bool:
    name = strip_extension(entry.grouped_name)
    if not name:
        return False
    if NON_NPC_NAME_RE.match(name):
        return False
    if is_attitude_only_entry(entry):
        return False
    return True


def matches_rule(rule: dict[str, Any], segments: set[str], id_tokens: set[str]) -> bool:
    segment_needles = set(rule.get("any_segment", []))
    token_needles = set(rule.get("any_id_token", []))
    if segment_needles and any(segment in segments for segment in segment_needles):
        return True
    if token_needles and any(token in id_tokens for token in token_needles):
        return True
    return False


def derive_flags(record: NPCRecord, rules: RuleSet) -> dict[str, bool]:
    all_segments = set(record.taxonomy_segments["all_segments"])
    id_tokens = set(lower_tokenize(record.npc_id))
    variant_tokens: set[str] = set()
    for variant in record.variants:
        variant_tokens.update(lower_tokenize(variant))
    id_tokens.update(variant_tokens)

    derived: dict[str, bool] = {}
    for flag_name, rule in rules.flags.items():
        derived[flag_name] = matches_rule(rule, all_segments, id_tokens)
    return derived


def infer_attitudes(record: NPCRecord, rules: RuleSet) -> set[str]:
    segments = set(record.taxonomy_segments["all_segments"])
    attitudes: set[str] = set()

    passive = set(rules.attitude_inference.get("passive_segments", []))
    neutral = set(rules.attitude_inference.get("neutral_segments", []))
    hostile = set(rules.attitude_inference.get("hostile_segments", []))

    if any(segment in segments for segment in passive):
        attitudes.add("PASSIVE")
    if any(segment in segments for segment in neutral):
        attitudes.add("NEUTRAL")
    if any(segment in segments for segment in hostile):
        attitudes.add("HOSTILE")

    if record.derived_flags.get("passive_like"):
        attitudes.add("PASSIVE")
    if record.derived_flags.get("aggressive_like"):
        attitudes.add("HOSTILE")

    return attitudes


def pick_primary_family(record: NPCRecord, rules: RuleSet) -> str | None:
    segments = set(record.taxonomy_segments["all_segments"])
    for family in rules.family_priority:
        if family in segments:
            return family

    prioritized_flags = [
        ("boss_like", "Boss"),
        ("void_like", "Void"),
        ("undead_like", "Undead"),
        ("intelligent_like", "Intelligent"),
        ("elemental_like", "Elemental"),
        ("aquatic_like", "Aquatic"),
        ("flying_like", "Avian"),
        ("livestock_like", "Livestock"),
        ("critter_like", "Critter"),
        ("vermin_like", "Vermin"),
        ("predator_like", "Predator"),
    ]
    for flag, label in prioritized_flags:
        if record.derived_flags.get(flag):
            return label
    return None


def scan_source_root(
    assets_root: Path,
    source_name: str,
    source_root: str,
    rules: RuleSet,
) -> list[SourceEntry]:
    abs_root = assets_root / Path(source_root)
    if not abs_root.exists():
        return []

    entries: list[SourceEntry] = []
    for path in abs_root.rglob("*"):
        if not path.is_file():
            continue

        rel_path = compute_relative_path(assets_root, path)
        variant_name, grouped_name, variant_kind = infer_variant_and_group(
            source_name, rel_path, source_root, rules
        )
        path_segments = collect_path_segments(
            rel_path,
            source_root,
            rules,
            include_filename=False,
        )

        entry = SourceEntry(
            source_type=source_name,
            rel_path=rel_path,
            variant_name=variant_name,
            grouped_name=grouped_name,
            path_segments=path_segments,
            variant_kind=variant_kind,
        )
        entry.inferred_segments = gather_inferred_segments(entry)
        if is_valid_npc_entry(entry):
            entries.append(entry)
    return entries


def build_index(
    assets_root: Path,
    rules: RuleSet,
    include_unknown: bool,
) -> dict[str, Any]:
    records: dict[str, NPCRecord] = {}

    source_roots = rules.source_roots()
    all_entries: list[SourceEntry] = []
    missing_source_roots: list[str] = []

    for source_name, roots in source_roots.items():
        for source_root in roots:
            abs_root = assets_root / Path(source_root)
            if not abs_root.exists():
                missing_source_roots.append(source_root)
                continue
            all_entries.extend(
                scan_source_root(assets_root, source_name, source_root, rules)
            )

    for entry in all_entries:
        npc_id = entry.grouped_name
        normalized_id = normalized_id_from_name(npc_id)
        if not normalized_id:
            continue

        if normalized_id not in records:
            records[normalized_id] = NPCRecord(
                npc_id=npc_id,
                normalized_id=normalized_id,
            )
        records[normalized_id].add_entry(entry, rules.include_raw_segments())

    for record in records.values():
        record.derived_flags = derive_flags(record, rules)
        record.inferred_attitudes = infer_attitudes(record, rules)

    npc_items: list[dict[str, Any]] = []
    unmatched_count = 0

    for normalized_id in sorted(records):
        record = records[normalized_id]
        primary_family = pick_primary_family(record, rules)
        has_classification = bool(primary_family or record.inferred_attitudes)

        if (
            not has_classification
            and not include_unknown
            and not rules.include_unmatched_records()
        ):
            continue

        if not has_classification:
            unmatched_count += 1

        item: dict[str, Any] = {
            "npc_id": record.npc_id,
            "normalized_id": record.normalized_id,
            "primary_family": primary_family,
            "inferred_attitudes": sorted(record.inferred_attitudes),
            "flags": {k: bool(v) for k, v in sorted(record.derived_flags.items())},
            "taxonomy": {
                key: sorted(values)
                for key, values in sorted(record.taxonomy_segments.items())
            },
        }

        if rules.include_variants():
            item["variants"] = sorted(record.variants)

        if record.interaction_names:
            item["interaction_names"] = sorted(record.interaction_names)

        if rules.include_source_paths():
            item["sources"] = {
                key: sorted(values)
                for key, values in sorted(record.source_paths.items())
            }

        if rules.include_raw_segments():
            item["raw_entries"] = sorted(
                record.raw_entries,
                key=lambda entry: (entry["source_type"], entry["path"]),
            )

        npc_items.append(item)

    summary = {
        "npc_count": len(npc_items),
        "source_entry_count": len(all_entries),
        "unmatched_record_count": unmatched_count,
        "missing_source_roots": stable_sorted(missing_source_roots),
    }

    return {
        "generated_at": utc_now_iso(),
        "assets_root": path_to_posix(assets_root.resolve()),
        "rules_path_version": rules.data.get("version"),
        "sources_scanned": sorted(
            [
                source_root
                for roots in source_roots.values()
                for source_root in roots
                if (assets_root / Path(source_root)).exists()
            ]
        ),
        "summary": summary,
        "npcs": npc_items,
    }


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build a unified NPC asset index from Hytale assets."
    )
    parser.add_argument(
        "--assets-root",
        required=True,
        help="Path to the extracted Hytale assets root directory.",
    )
    parser.add_argument(
        "--rules",
        default=DEFAULT_RULES_PATH,
        help=f"Path to the scraper rules JSON file (default: {DEFAULT_RULES_PATH}).",
    )
    parser.add_argument(
        "--output",
        required=True,
        help="Output JSON file path.",
    )
    parser.add_argument(
        "--pretty",
        action="store_true",
        help="Pretty-print JSON output with indentation.",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Fail if any configured source root is missing.",
    )
    parser.add_argument(
        "--include-unknown",
        action="store_true",
        help="Include NPC records that have no obvious family or inferred attitude.",
    )
    return parser.parse_args(argv)


def validate_assets_root(path: Path) -> None:
    if not path.exists():
        raise ScraperError(f"Assets root does not exist: {path}")
    if not path.is_dir():
        raise ScraperError(f"Assets root is not a directory: {path}")


def ensure_parent_directory(path: Path) -> None:
    if path.parent and not path.parent.exists():
        path.parent.mkdir(parents=True, exist_ok=True)


def write_output(path: Path, payload: dict[str, Any], pretty: bool) -> None:
    ensure_parent_directory(path)
    if pretty:
        content = json.dumps(payload, indent=2, ensure_ascii=False, sort_keys=False)
        content += "\n"
    else:
        content = json.dumps(payload, separators=(",", ":"), ensure_ascii=False)
    path.write_text(content, encoding="utf-8")


def main(argv: list[str]) -> int:
    args = parse_args(argv)

    assets_root = Path(args.assets_root).expanduser()
    rules_path = Path(args.rules).expanduser()
    output_path = Path(args.output).expanduser()

    try:
        validate_assets_root(assets_root)
        rules = RuleSet.load(rules_path)
        index = build_index(
            assets_root=assets_root,
            rules=rules,
            include_unknown=bool(args.include_unknown),
        )

        missing_roots = index.get("summary", {}).get("missing_source_roots", [])
        if missing_roots and args.strict:
            raise ScraperError(
                "Missing configured source roots: " + ", ".join(missing_roots)
            )

        write_output(output_path, index, pretty=bool(args.pretty))

        print(
            (
                f"Wrote NPC asset index to {output_path} "
                f"({index['summary']['npc_count']} NPCs, "
                f"{index['summary']['source_entry_count']} source entries)"
            ),
            file=sys.stderr,
        )
        if missing_roots:
            print(
                "Warning: missing source roots: " + ", ".join(missing_roots),
                file=sys.stderr,
            )
        return 0
    except ScraperError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 2
    except Exception as exc:  # pragma: no cover
        print(f"Unexpected error: {exc}", file=sys.stderr)
        return 3


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
