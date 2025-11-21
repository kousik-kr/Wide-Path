#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PORT=${BACKEND_PORT:-8080}
FRONTEND_PORT=${FRONTEND_PORT:-5173}
API_BASE="${API_BASE:-}"
MODE="full"
NVM_VERSION="${NVM_VERSION:-24}"
BACKEND_LOG="${BACKEND_LOG:-"$ROOT_DIR/.backend.log"}"

default_api_base() {
  echo "${API_BASE:-http://localhost:${BACKEND_PORT}/api}"
}

usage() {
  local default_base
  default_base=$(default_api_base)
  cat <<USAGE
Wide-Path helper script

Usage: ./run.sh [options]

Options:
  --backend-only, --no-frontend   Build and run only the Java API server.
  --frontend-only                 Start the Vite dev server without the Java backend.
  --port <number>                 Port for the Java API server (default: ${BACKEND_PORT}).
  --frontend-port <number>        Port for the Vite dev server (default: ${FRONTEND_PORT}).
  --api-base <url>                Override API base URL passed to Vite (default: ${default_base}).
  -h, --help                      Show this help.
USAGE
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

maybe_load_nvm() {
  if command -v nvm >/dev/null 2>&1; then
    return 0
  fi

  export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
  if [ -s "${NVM_DIR}/nvm.sh" ]; then
    # shellcheck disable=SC1090
    . "${NVM_DIR}/nvm.sh"
  fi
}

ensure_node() {
  maybe_load_nvm
  if command -v nvm >/dev/null 2>&1; then
    if nvm use "${NVM_VERSION}" >/dev/null 2>&1; then
      echo "Using Node via nvm ($(node -v))"
    else
      echo "Warning: nvm found but Node ${NVM_VERSION} is not installed; using current nvm default." >&2
      nvm use default >/dev/null 2>&1 || true
    fi
  fi

  require_cmd node
}

build_backend() {
  require_cmd javac
  mkdir -p "$ROOT_DIR/build"
  echo "Compiling Java sources..."
  javac -d "$ROOT_DIR/build" "$ROOT_DIR"/src/*.java
}

start_backend() {
  build_backend
  require_cmd java
  echo "Starting API server on port ${BACKEND_PORT}..."
  (cd "$ROOT_DIR/build" && exec java ApiServer "${BACKEND_PORT}")
}

start_backend_background() {
  build_backend
  require_cmd java
  echo "Starting API server (requested port ${BACKEND_PORT})..."
  : >"$BACKEND_LOG"
  (cd "$ROOT_DIR/build" && java ApiServer "${BACKEND_PORT}") >"$BACKEND_LOG" 2>&1 &
  BACKEND_PID=$!
  trap 'kill ${BACKEND_PID} 2>/dev/null || true' EXIT
  actual_port=$(wait_for_backend_port)
  if [ -n "$actual_port" ]; then
    echo "Backend reported port ${actual_port}"
    API_BASE="${API_BASE:-http://localhost:${actual_port}/api}"
  else
    echo "Warning: could not detect backend port from log; defaulting API_BASE=${API_BASE}"
  fi
}

wait_for_backend_port() {
  for _ in $(seq 1 30); do
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
      return
    fi
    if grep -m1 -o 'API server started on port [0-9]*' "$BACKEND_LOG" >/dev/null; then
      grep -m1 -o 'API server started on port [0-9]*' "$BACKEND_LOG" | awk '{print $6}'
      return
    fi
    sleep 1
  done
}

start_frontend() {
  ensure_node
  require_cmd npm
  cd "$ROOT_DIR/frontend"
  if [ ! -d node_modules ]; then
    echo "Installing frontend dependencies..."
    npm install
  fi
  echo "Starting Vite dev server on port ${FRONTEND_PORT} (API base: ${API_BASE})..."
  VITE_API_BASE="${API_BASE}" npm run dev -- --host 0.0.0.0 --port "${FRONTEND_PORT}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --backend-only|--no-frontend)
      MODE="backend"
      shift
      ;;
    --frontend-only)
      MODE="frontend"
      shift
      ;;
    --port)
      BACKEND_PORT="$2"
      shift 2
      ;;
    --frontend-port)
      FRONTEND_PORT="$2"
      shift 2
      ;;
    --api-base)
      API_BASE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
    esac
done

API_BASE=$(default_api_base)

case "$MODE" in
  backend)
    start_backend
    ;;
  frontend)
    start_frontend
    ;;
  full)
    start_backend_background
    start_frontend
    ;;
  *)
    echo "Unknown mode: ${MODE}" >&2
    exit 1
    ;;
esac
