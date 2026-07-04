#!/usr/bin/env bash
# Usage: ./scripts/cc-build.sh <gradle-task> [<gradle-task>...]
# Output: errors / warnings / BUILD verdict / test summary / failed tasks.
# Full log: <repo>/tmp/cc-build.out (tmp/ is gitignored)
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG="$REPO_ROOT/tmp/cc-build.out"
mkdir -p "$(dirname "$LOG")"
: > "$LOG"

if [ "$#" -eq 0 ]; then
  echo "usage: $0 <gradle-task> [<gradle-task>...]" >&2
  exit 2
fi

FILTER='^(e:|w:|FAILURE:|BUILD (SUCCESSFUL|FAILED)|Execution failed|Tests run:|Installed on)|^.*error:|^> Task :.*FAILED$|[0-9]+ tests? completed'

./gradlew "$@" --console=plain 2>&1 \
  | tee "$LOG" \
  | grep --line-buffered -E "$FILTER" \
  || true

EXIT=${PIPESTATUS[0]}

if [ "$EXIT" -eq 0 ]; then
  echo "===== BUILD SUCCESSFUL (EXIT=0) ====="
else
  echo "===== BUILD FAILED (EXIT=$EXIT) ====="
fi
echo "===== log: $(grep -c '' "$LOG") lines | $LOG ====="
exit "$EXIT"
