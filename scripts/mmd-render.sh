#!/usr/bin/env bash
# Рендер схем Mermaid (*.mmd) и Graphviz (*.dot) в PNG. В имя картинки
# входит хеш содержимого схемы: превью Android Studio кеширует картинку по
# пути и после перерисовки показывает старую, пока имя то же. Ссылки в .md
# той же папки переписываются на новое имя, старые картинки той же схемы
# удаляются. Graphviz берётся там, где Mermaid не справляется с раскладкой:
# узел сбоку на том же уровне (rank=same).
#
# Использование: scripts/mmd-render.sh <папка со схемами>
set -euo pipefail

dir="${1:?папка со схемами}"
cd "$dir"

for src in *.mmd *.dot; do
  [ -e "$src" ] || continue
  name="${src%.*}"
  hash=$(md5 -q "$src" | perl -ne 'print substr($_, 0, 8)')
  out="$name.$hash.png"
  if [ ! -f "$out" ]; then
    case "$src" in
      *.mmd)
        # Масштаб растра: строка «%% scale: 3» в начале схемы, по умолчанию 2.
        scale=$(grep -m1 -E '^%% scale: ' "$src" | perl -pe 's/^%% scale: //' || true)
        scale="${scale:-2}"
        npx -y @mermaid-js/mermaid-cli -i "$src" -o "$out" -b white -s "$scale" 2>&1 \
          | grep -v -E '^Generating' || true
        ;;
      *.dot)
        dot -Tpng -Gdpi=144 "$src" -o "$out"
        ;;
    esac
  fi
  if [ ! -f "$out" ]; then
    echo "ОШИБКА: $src не отрендерился, старая картинка и ссылки оставлены"
    continue
  fi
  for old in "$name".*.png; do
    [ "$old" = "$out" ] || rm -f "$old"
  done
  perl -pi -e "s/\Q$name\E(\.[0-9a-f]{8})?\.png/$out/g" ./*.md
  echo "$out"
done
