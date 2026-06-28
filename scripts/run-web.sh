#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PORT="${1:-18765}"
HOST="127.0.0.1"

cd "$REPO_ROOT"

# Keep Android/Gradle state inside the repo-local dev directories.
# shellcheck source=scripts/dev-env.sh
source "$SCRIPT_DIR/dev-env.sh"

./gradlew :web:build

echo
echo "RaumBaller web build is ready."
echo "Open: http://$HOST:$PORT/index.html"
echo "Press Ctrl+C to stop the server."
echo

exec python3 -m http.server "$PORT" --bind "$HOST" --directory web/build/dist
