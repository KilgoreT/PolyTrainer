# Сессия adaptive flow IS479 — checkpoint

**Дата:** 2026-05-18

## Текущее состояние

Adaptive flow `lexeme_adaptive` запущен. Прошёл первый шаг `task`. **Пауза** (mode=manual + pause:true).

**Следующий шаг:** `scope_analysis` (после явного подтверждения пользователя).

## Параметры запуска

- **Ticket:** `IS479` (GitHub issue #479: https://github.com/KilgoreT/PolyTrainer/issues/479)
- **Feature name:** `wordcard_lexeme_inline`
- **Workspace dir:** `docs/features/IS479_wordcard_lexeme_inline/`
- **Branch:** `IS479_wordcard_lexeme_inline`
- **Mode:** `manual` (пауза после каждого шага)
- **Overlay:** `docs/forgeflow-overlay`
- **Flow:** `docs/forgeflow-overlay/flows/adaptive.yml`

## Бриф задачи

**Название:** Замена UI добавления лексемы в карточке слова — с bottom sheet на inline-механизм.

**Что меняется в `modules/screen/wordcard`:**

1. Кнопка добавления лексемы заменяется на новую (frame `9154-82532`)
2. Механизм добавления — больше не bottom sheet (`AddLexemeBottomState`), а inline / встроенный UI (frame `9154-82519`)
3. Кнопка «Перевод» должна выглядеть как chip (образец стиля — frame `9154-82521`)
4. Все остальные кнопки на frame `9154-82625` — chip'ы того же стиля

**Что НЕ делаем:**

- **Пример (Example)** — исключить полностью

## Figma

- **File key:** `w8GmGCdOZJUi99Cuv4q4W9` (Lexeme — 21.07.2025)
- **URL base:** https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-
- **MCP подключён:** `figma-developer-mcp` (API token). Tools: `mcp__figma__get_figma_data`, `mcp__figma__download_figma_images`

### Основные frames с новым UI

- `9154-82509`
- `9154-86012`
- `9154-86182`
- `9154-86353`
- `9154-86499`

### Конкретные элементы

- Заменяемая кнопка: `9154-82532`
- Механизм добавления: `9154-82519`
- «Перевод» chip (образец): `9154-82521`
- Кнопки которые надо стилизовать под chip: `9154-82625`

## Артефакты в workspace

- `plan.yml` — план flow, шаг `task` со статусом `done`
- `00_task.md` — бриф задачи в формате task-шага
- `log.md` — лог flow (старт + done task)
- `_session_state.md` — этот файл (checkpoint)

## Технический нюанс

**Sub-agent (general-purpose) НЕ имеет Agent tool** — не может делегировать. Поэтому **main session берёт роль conductor'а** и делегирует каждый шаг свежему sub-agent через Agent tool. Это обход CLAUDE.md правила «conductor как sub-agent» — необходимо из-за ограничения окружения.

## Что делать после compact

Если пользователь говорит «продолжай / следующий / запускай scope_analysis» — main session:

1. Прочитать `plan.yml` (статус шагов)
2. Прочитать `00_task.md` (бриф)
3. Делегировать шаг `scope_analysis` свежему sub-agent через Agent tool. Sub-agent читает:
   - `parent.task.output` = `00_task.md` (по пути в plan.dir)
   - Step file: `docs/forgeflow-overlay/steps/scope_analysis.md`
   - Output: `02_scope.md` в plan.dir
   - **sets:** `infra_touched`, `business_touched`, `ui_touched`, `data_touched`, `needs_tests`, `needs_migration_tests`, `feature_has_ui_contract`, `spec_filename`
4. После возврата sub-agent — обновить `plan.yml` (статус scope_analysis → done), записать context-переменные в `plan.context`, обновить `log.md`, **пауза** (mode=manual + pause:true)

## Ключевые правила

- НИКОГДА не обновлять plan.yml несколькими Edit — только один Write
- НИКОГДА не выполнять содержание шага сам — делегировать sub-agent
- Mode=manual → пауза после каждого шага
- Время — РЕАЛЬНОЕ через `date` в Bash
- Все артефакты на русском, идентификаторы на английском
- ЖЕЛЕЗНОЕ ПРАВИЛО ПАУЗЫ: после шага показать результат и ждать. НЕ запускать следующий. НЕ писать «запускаю». Только результат — и молчать.

## Memory обновления в этой сессии

- `feedback_yaml_block_style.md` — block style для списков в YAML, не flow

## Состояние FF spec / overlay (то что готово до запуска flow)

- Adaptive flow + 4 sub-flow (business / infra / ui / data) + adaptive.yml — все с input/review/logging
- Step files в overlay: 5 контрактных (`contract_state`, `contract_ui_msg`, `contract_io`, `contract_usecase`, `contract_spec`) + `publish_spec` + `scope_analysis`
- Reviewer agents в overlay: `architect`, `qa_engineer`, `senior` (analyst остался base)
- Parent-ref механика добавлена в `docs/forgeflow/spec/dsl.md` и `runner.md`
- Design doc: `docs/features/FORGEFLOW_contract_design.md` (полный)

## Отложенные / accept-risks

- Checklist integration (ПРОКЛЯТО, заморожено)
- Headless `if:` (модель сама поймёт)
- LLM штамповка Y/N
- Grep'ы не runtime-валидируются
- Architect/qa overlap
- Длина promptов
- Миграция legacy спек — в `docs/Backlog.md → Срочное`
- Связь тестов с контрактом — в `docs/forgeflow/backlog.md`
