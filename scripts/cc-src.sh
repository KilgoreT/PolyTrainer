#!/usr/bin/env bash
# Usage:
#   ./scripts/cc-src.sh <fully.qualified.ClassName>   # один класс
#   ./scripts/cc-src.sh -a <artifact-name>            # весь sources.jar артефакта (напр. navigation-compose)
# Finds in gradle sources jars, unpacks to <repo>/tmp/sources/<artifact-version>/ (tmp/ is gitignored).
# Handles Kotlin Multiplatform jars (commonMain/androidMain/jvmMain prefixes).

MODE=class
if [ "$1" = "-a" ]; then
  MODE=artifact
  shift
fi

if [ "$#" -ne 1 ]; then
  echo "usage:" >&2
  echo "  $0 <fully.qualified.ClassName>   # один класс" >&2
  echo "  $0 -a <artifact-name>            # весь артефакт (напр. navigation-compose)" >&2
  exit 2
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$REPO_ROOT/tmp/sources"
mkdir -p "$DEST"

CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
if [ ! -d "$CACHE" ]; then
  echo "ERROR: no gradle cache at $CACHE" >&2
  exit 1
fi

# ===== Artifact mode: распаковать ВСЕ sources.jar артефакта целиком =====
# Матч по ДИРЕКТОРИИ артефакта в кэше (.../<group>/<artifact>/<version>/<hash>/<file>-sources.jar),
# а не по имени файла — у KMP-артефактов файл содержит суффикс -android/-jvm и версию.
if [ "$MODE" = "artifact" ]; then
  ARTIFACT="$1"
  echo "=== artifact mode: $ARTIFACT ==="
  echo "  cache: $CACHE"
  echo "  dest:  $DEST"
  echo
  FOUND=0
  while IFS= read -r jar; do
    art="$(basename "$(dirname "$(dirname "$(dirname "$jar")")")")"
    # точное имя ИЛИ KMP-вариант с платформенным суффиксом (navigation-compose → navigation-compose-android)
    case "$art" in
      "$ARTIFACT"|"$ARTIFACT"-*) ;;
      *) continue ;;
    esac
    ver="$(basename "$(dirname "$(dirname "$jar")")")"
    JAR_NAME="$(basename "$jar" .jar)"
    OUT_DIR="$DEST/${JAR_NAME%-sources}"
    echo "  MATCH: $(basename "$jar")  (version $ver)"
    mkdir -p "$OUT_DIR"
    if unzip -o "$jar" -d "$OUT_DIR" >/dev/null 2>&1; then
      echo "    unpacked → $OUT_DIR"
      FOUND=$((FOUND+1))
    else
      echo "    FAIL unzip: $jar" >&2
    fi
  done < <(find "$CACHE" -name '*-sources.jar' 2>/dev/null)
  echo
  if [ "$FOUND" -eq 0 ]; then
    echo "=== RESULT: artifact '$ARTIFACT' не найден ==="
    echo "  укажи ТОЧНОЕ имя артефакта = директория в gradle cache"
    echo "  примеры: navigation-compose · navigation-runtime · navigation-common · lifecycle-viewmodel-compose"
    exit 1
  fi
  echo "=== DONE: распакован(ы) $FOUND jar артефакта '$ARTIFACT' в $DEST ==="
  exit 0
fi

# ===== Class mode: распаковать один класс по FQN =====
FQN="$1"
PATH_IN_JAR="${FQN//.//}"

echo "=== STEP 1: parse FQN ==="
echo "  FQN:          $FQN"
echo "  path tail:    ${PATH_IN_JAR}.{kt,java}"
echo "  cache:        $CACHE"
echo "  dest:         $DEST"
echo

echo "=== STEP 2: collect *-sources.jar from cache ==="
JARS_FILE=$(mktemp)
find "$CACHE" -name '*-sources.jar' 2>/dev/null > "$JARS_FILE"
TOTAL=$(grep -c '' "$JARS_FILE")
echo "  found $TOTAL sources.jar files"
echo

if [ "$TOTAL" -eq 0 ]; then
  echo "ERROR: no sources.jar found in cache. Try: ./gradlew downloadSources" >&2
  rm -f "$JARS_FILE"
  exit 1
fi

echo "=== STEP 3: scan each jar; capture matching entries (handles KMP prefixes) ==="
HITS_FILE=$(mktemp)   # format: <jar>\t<entry>
i=0
FOUND_JARS=0
FOUND_ENTRIES=0
while IFS= read -r jar; do
  i=$((i+1))
  printf '  [%d/%d] %s\r' "$i" "$TOTAL" "$(basename "$jar")" >&2
  # unzip -l format: "  size  date  time   path"
  # capture path column (last whitespace-separated field), keep only matching entries
  ENTRIES=$(unzip -l "$jar" 2>/dev/null \
    | awk '{print $NF}' \
    | grep -E "(^|/)${PATH_IN_JAR}\.(kt|java)$" || true)
  if [ -n "$ENTRIES" ]; then
    printf '\n  MATCH in: %s\n' "$(basename "$jar")"
    FOUND_JARS=$((FOUND_JARS+1))
    while IFS= read -r entry; do
      [ -n "$entry" ] || continue
      printf '    entry: %s\n' "$entry"
      printf '%s\t%s\n' "$jar" "$entry" >> "$HITS_FILE"
      FOUND_ENTRIES=$((FOUND_ENTRIES+1))
    done <<< "$ENTRIES"
  fi
done < "$JARS_FILE"
printf '\n'
echo "  scanned: $i jars"
echo "  matched: $FOUND_JARS jars, $FOUND_ENTRIES entries"
echo

if [ "$FOUND_ENTRIES" -eq 0 ]; then
  echo "=== RESULT: not found ==="
  echo "  $FQN не найден ни в одном sources.jar"
  echo "  возможные причины:"
  echo "   - класс не имеет sources.jar в кэше (нужен ./gradlew downloadSources)"
  echo "   - в Kotlin filename != classname (DropdownMenu живёт в Menu.kt) — нужен grep-по-содержимому"
  echo "   - либо нужен весь артефакт: ./scripts/cc-src.sh -a <artifact-name>"
  rm -f "$JARS_FILE" "$HITS_FILE"
  exit 1
fi

echo "=== STEP 4: unpack matched entries ==="
RESULT_FILES=$(mktemp)
while IFS=$'\t' read -r jar entry; do
  JAR_NAME=$(basename "$jar" .jar)
  # artifact-version (без суффикса -sources) — видно версию библиотеки в пути.
  OUT_DIR="$DEST/${JAR_NAME%-sources}"
  mkdir -p "$OUT_DIR"
  unzip -o "$jar" "$entry" -d "$OUT_DIR" >/dev/null 2>&1
  OUT_FILE="$OUT_DIR/$entry"
  if [ -f "$OUT_FILE" ]; then
    echo "  unpacked: $OUT_FILE"
    echo "$OUT_FILE" >> "$RESULT_FILES"
  else
    echo "  FAIL extract: $entry" >&2
  fi
done < "$HITS_FILE"
echo

echo "=== STEP 5: extracted files ==="
RESULT=$(grep -c '' "$RESULT_FILES" 2>/dev/null || echo 0)
if [ "$RESULT" -eq 0 ]; then
  echo "ERROR: matched in jar listing but no file extracted" >&2
  rm -f "$JARS_FILE" "$HITS_FILE" "$RESULT_FILES"
  exit 1
fi
while IFS= read -r f; do
  echo "  $f"
done < "$RESULT_FILES"
echo
echo "=== DONE: $RESULT file(s) ready in $DEST ==="

rm -f "$JARS_FILE" "$HITS_FILE" "$RESULT_FILES"
exit 0
