---
invoke: auto
---

# cc-src — найти library source по FQN

**Использовать ВМЕСТО угадывания library API.** Любое решение опирающееся на поведение Compose / Room / DataStore / Material / Lifecycle и т.п. — сначала Read реальный исходник через `cc-src.sh`, потом писать код / контракт / дизайн.

## Когда использовать

- Перед дизайн-решением где API библиотеки определяет правильность подхода.
- Когда пользователь повторил вопрос про library — это сигнал что я гадаю; немедленно Read source.
- Когда хочется написать «обычно Compose API делает X» — стоп, проверь через скрипт.

## Как

Два режима:

```
./scripts/cc-src.sh <fully.qualified.ClassName>   # один класс по FQN
./scripts/cc-src.sh -a <artifact-name>            # ВСЕ исходники артефакта целиком
```

Примеры (один класс):
- `./scripts/cc-src.sh androidx.room.RoomDatabase`
- `./scripts/cc-src.sh androidx.compose.material3.SnackbarHostState`
- `./scripts/cc-src.sh androidx.datastore.preferences.core.Preferences`

Примеры (весь артефакт):
- `./scripts/cc-src.sh -a navigation-compose`  (ловит и KMP-вариант `navigation-compose-android`)
- `./scripts/cc-src.sh -a room-runtime`

Скрипт:
- Сканирует `~/.gradle/caches/modules-2/files-2.1/` (~465 sources.jar, ~30 сек в class-режиме).
- Class-режим: 5 STEP'ов (parse / collect / scan с progress per-jar на stderr / unpack / result).
- Artifact-режим: матч по **директории артефакта** в кэше (точное имя ИЛИ `<name>-<platform>`), распаковка всего sources.jar.
- Поддерживает Kotlin Multiplatform префиксы (`commonMain/`, `androidMain/`, `jvmMain/`).
- Распаковывает в **`<repo>/tmp/sources/<artifact-version>/...`** (версия в пути; `tmp/` в `.gitignore` — исходники остаются на диске, в git не попадают).

После запуска — **Read** распакованный файл, искать нужный метод / класс.

## Ограничение (важно)

Class-режим находит класс **только когда имя файла = имя класса** (Java convention; в Kotlin часто так же для top-level классов).

**НЕ работает в class-режиме** для случаев типа Compose, где много функций упаковано в один файл:
- `androidx.compose.material3.DropdownMenu` → живёт в `Menu.kt`, не `DropdownMenu.kt` → `not found`.
- Решение: либо запустить с базовым именем файла (`androidx.compose.material3.Menu`), либо взять **весь артефакт** через `-a` и искать по распакованным файлам:
  `./scripts/cc-src.sh -a material3` → `tmp/sources/material3-android-<ver>/...`

## Корневая причина зачем skill существует

Из IS481 retro: повтор вопроса пользователя про library API = сигнал что я гадаю по тренированной памяти. Лекарство — Read реального source, но без автоматизации это manual hunt в `~/.gradle/caches/`. Скрипт делает это в одну команду.
