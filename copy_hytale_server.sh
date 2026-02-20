#!/usr/bin/env bash
set -euo pipefail

SRC="$HOME/.var/app/com.hypixel.HytaleLauncher/data/Hytale/install/release/package/game/latest/Server/HytaleServer.jar"
DEST_DIR="libs"
DEST="$DEST_DIR/HytaleServer.jar"

mkdir -p "$DEST_DIR"

if [[ -f "$SRC" ]]; then
  cp -f "$SRC" "$DEST"
  echo "Copié: $SRC -> $DEST"
else
  echo "Fichier introuvable: $SRC" >&2
  exit 1
fi
