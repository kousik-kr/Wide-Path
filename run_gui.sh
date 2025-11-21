#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

build_backend() {
  require_cmd javac
  mkdir -p "$ROOT_DIR/build"
  echo "Compiling Java sources for GUI..."
  javac -d "$ROOT_DIR/build" "$ROOT_DIR"/src/*.java
}

run_gui() {
  require_cmd java
  echo "Launching Java GUI..."
  (cd "$ROOT_DIR/build" && exec java GuiLauncher)
}

build_backend
run_gui
