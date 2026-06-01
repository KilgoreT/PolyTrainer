# FlowBacklog

Журнал недостатков ForgeFlow / adaptive flow с обратной связью. В отличие от `Backlog.md` (недостатки кода репозитория) — здесь про сам flow и его шаги.

**Цель:** инструмент эволюции flow на реальных данных. Регистрируем finding → реализуем фикс → на следующих фичах проверяем что фикс действительно закрывает проблему. Без verification фикс — гипотеза.

## Жизненный цикл finding

- **open** — зарегистрирован, фикса нет, нужен фикс.
- **monitor** — зарегистрирован, есть подозрение что это разовый артефакт постановки / контекста, превентивный фикс не делаем. Мониторим повторение на следующих фичах. При повторе → `open` / `wip`. При не-повторе на N фичах → `rejected` с обоснованием.
- **wip** — фикс в работе.
- **closed** — фикс реализован, ждём проверки на следующих фичах.
- **validated** — фикс подтвердил работоспособность хотя бы на одной фиче после реализации.
- **regressed** — проблема всплыла снова после фикса → возврат в open.
- **rejected** — finding отклонён (не считаем проблемой / out of scope / не подтвердилось при мониторинге).

## Формат ID

`<тикет>-F<N>` — префикс по тикету источника (фича, постмортем). Это даёт глобально уникальный ID и сохраняет привязку к контексту:
- `IS479-F1` — finding №1 из IS479 (постмортем / проёбы).
- `IS480-F1` — finding №1 из IS480.
- `SYS-001` — системный finding из работы над инструментом (не привязан к фиче).

Нумерация `<N>` внутри тикета — последовательная по мере появления; разрывы (пропущенные номера) допустимы (например finding закрыт структурно без записи).

## Формат записи

```
- **<ID>. <название>.**
  - Источник: <ссылка на постмортем + раздел>.
  - Статус: <open | monitor | wip | closed | validated | regressed | rejected>.
  - <если wip/closed/validated> Реализация: <ссылка на дизайн-док или коммит>.
  - <если closed> Verified on: <фичи где будет проверено / пусто пока>.
  - <если validated> Verified on: <фичи где подтверждено>.
  - <если regressed> Regression on: <фича где всплыло снова>.
  - <если rejected> Причина: <короткое обоснование>.
  - <если open/monitor> Идея фикса: <короткое описание>.
  - <если monitor> Monitor on: <условие проверки повторения>. Примечание: <почему не делаем превентивный фикс>.
```

---

## Closed

- **IS479-F1. Контракт пишется без sanity-check существующего data-слоя.**
  - Источник: IS479 постмортем (archived) § F1.
  - Статус: closed.
  - Реализация: добавлен `business_walkthrough` — полная перестройка business contract-блока: `walkthrough → contract → contract_review → contract_spec` в `docs/forgeflow-overlay/steps/`, старые 5 шагов (`contract_state`/`ui_msg`/`io`/`usecase`/`spec`) удалены. Минимальная вставка `<layer>_walkthrough` для ui / data / infra с подключением output в input ближайшего следующего шага. См. описание архитектуры в [`forgeflow-overlay/flows/adaptive.md`](forgeflow-overlay/flows/adaptive.md).
  - Verified on: (пусто — IS479 был кейс обнаружения, не проверки).

- **IS479-F2. `contract_state` допускал залезание в reducer-логику.**
  - Источник: IS479 постмортем (archived) § F2.
  - Статус: closed.
  - Реализация: [`docs/guides/state-modeling.md` § 16 «Инварианты State: shape vs transition»](guides/state-modeling.md). Добавлено правило snapshot-test (инвариант проверяется по одному снимку state без истории Msg) и обязательная маркировка `[shape]` / `[transition]` с запретом `[transition]` в разделе State. Модуль `guides` включён на шагах `business_contract` и `business_contract_review` — правило подхватывается автоматически.
  - Verified on: (пусто).

- **IS479-F4. `scope_analysis` ит.1 «зелёный» без инквизиции и re-review.**
  - Источник: IS479 постмортем (archived) § F4.
  - Статус: closed.
  - Реализация:
    - FF base — введён параметр `require_clean_iteration` в блок `repeat:` (default `true`). `execute_repeat` сравнивает hash артефакта до/после `execute_step_once` и не закрывает шаг, если в итерации с `<until>=true` артефакт менялся — нужна ещё одна clean iteration без правок. Затрагивает `~/dev/forgeflow/spec/dsl.md → Repeat` и `~/dev/forgeflow/spec/runner.md → execute_repeat`, добавлена вспомогательная `compute_artifact_hash`.
    - Overlay — `analyst` уже заменён на `qa_engineer` в `adaptive.yml:57` (ad-hoc правка).
  - Verified on: (пусто).

- **IS479-F5. `architect`-ревьюер галлюцинирует пути / факты о коде.**
  - Источник: IS479 постмортем (archived) § F5.
  - Статус: closed.
  - Реализация: добавлен раздел «Verification — обязательно для любого claim о коде» в `docs/forgeflow-overlay/agents/custom/architect.md`, `senior.md`, `qa_engineer.md`. Каждое утверждение про код (путь / имя / сигнатура / наличие сущности) обязано сопровождаться строкой `Verify: <tool> <args> → <output snippet>` через встроенные Claude tools `Grep` / `Glob` / `Read` (read-only, без апрува пользователя). Finding с claim о коде без `Verify:` — auto-reject conductor'ом / inquisitor'ом. Обновлены шаблоны finding'ов и раздел «Правила» во всех трёх агентах.
  - Verified on: (пусто).

- **IS479-F7. Маркер 📌 «без изменений» — без верификации против Figma.**
  - Источник: IS479 постмортем (archived) § F7.
  - Статус: closed.
  - Реализация: применена «Правка #1» из `docs/features/IS479_wordcard_lexeme_inline/ui_layout_step_patches.md` в `docs/forgeflow-overlay/steps/ui_layout.md`. Добавлен раздел «Маркеры и расхождения с Figma — две независимые оси»: маркер ⚙️/❇️/🔄/📌 = «трогаем ли в фиче»; `⚠` в `notes:` = расхождение с Figma. Запрет на склейку осей: 📌 не означает «по Figma». Дисциплина в конце шага — пройти по всем виджетам с ⚙️/🔄/📌 и явно подтвердить либо «нет расхождений с Figma», либо `⚠` в notes. Соответствующий пункт добавлен в `output_criteria`.
  - Verified on: (пусто).

- **IS479-F8. Миграция логики удалённых виджетов не отслеживалась.**
  - Источник: IS479 постмортем (archived) § F8.
  - Статус: closed.
  - Реализация: применена «Правка #2» из `docs/features/IS479_wordcard_lexeme_inline/ui_layout_step_patches.md` в `docs/forgeflow-overlay/steps/ui_layout.md`. Добавлен раздел «Миграция логики удалённых виджетов»: секция `## ❌ УДАЛЯЕМ` пересмотрена — каждая строка показывает миграцию `Source.handler → Target.flow` (либо явное `удалено, нет аналога`). Зеркальная пометка `⚠ принимает X из удалённого Y` обязательна в `notes:` принимающего виджета. Дисциплина в конце шага — проверить что каждая строка ❌ имеет зеркало в notes (или `удалено, нет аналога`). Соответствующие пункты добавлены в `output_criteria`.
  - Verified on: (пусто).

- **IS479-F13. `REVIEW.md` создан вручную post-flow, а не как шаг flow.**
  - Источник: IS479 постмортем (archived) § F13.
  - Статус: closed.
  - Реализация: создан промпт `docs/forgeflow-overlay/steps/global_code_review.md` и добавлен шаг `global_code_review` в `docs/forgeflow-overlay/flows/adaptive.yml` после `check`. Шаг запускает 3 параллельных subagent'а через Task tool — Architecture / Bugs / YAGNI, каждый со своим направлением обзора. Findings собираются в `REVIEW.md` с обязательным `Verify:` через встроенные Claude tools. На паузе conductor проводит triage с пользователем: для каждого finding решение `→ закрыть в фиче` / `→ backlog` / `→ rejected`. Без ручного триггера — каждая фича получает ревью автоматически.
  - Verified on: (пусто).

- **IS479-F14. Inquisitor не интегрирован в DSL.**
  - Источник: IS479 постмортем (archived) § F14.
  - Статус: closed.
  - Реализация: на момент IS479 inquisitor запускался conductor'ом ad-hoc; **уже интегрирован** в модуль `modules/review/` — после каждого review-цикла прогоняет findings → approved / rejected, только approved идут в `{step}_approved.md` и инжектятся при повторе шага. DSL про inquisitor не знает — это полностью внутри модуля review.
  - Дополнительно: добавлена опция модуля `inquisitor: bool` (default `true`) в `modules/review/README.md` и `prompt.md`. При `inquisitor: false` все findings помечаются approved автоматически (режим «доверять ревьюерам без арбитра», экономит вызов LLM ценой пропуска галлюцинаций).
  - Verified on: (пусто).

- **IS479-F9. `architect` / `senior` пропускают тривиальные derived-в-composable.**
  - Источник: IS479 постмортем (archived) § F9 + `docs/Проёбы.md` (archived) § «`enableLexemeTranslationEdit` не копирует origin в edited» (тот же класс — правило в гайде есть, агент / reviewer его не применяют).
  - Статус: closed.
  - Реализация: введён формат **R-правил** (rule-list) в гайдах — каждое машинно-проверяемое правило в формате `R-NNN` (id) / `Severity` / `Applies to` / `Check`. Первый гайд переведён — `docs/guides/reducer-patterns.md` (12 правил R-RP-001 … R-RP-012). В промпты ревьюеров (`architect.md` / `senior.md` / `qa_engineer.md` в overlay) добавлен обязательный «Чек-лист R-правил из подложенных гайдов»: для каждого R-NNN из подложенных гайдов — вердикт `OK` / нарушение, без пропусков «по ощущению». FF независимости не нарушаем — промпт говорит «по правилам гайдов», конкретные файлы не упоминает.
  - Остальные гайды (state-modeling, state-and-extensions, messages, ui-patterns и т.п.) — постепенный перевод в формат R-правил (см. `## Open / SYS-001`).
  - Verified on: (пусто).

- **IS479-F15. Теги `⟦...⟧` в ui_layout не имеют формального определения.**
  - Источник: `docs/Проёбы.md` (archived) § «LexemeValueField view-mode как чёрный chip» (из IS479, не из постмортема).
  - Статус: closed.
  - Реализация:
    - Создан гайд `docs/guides/ui-primitives.md` со словарём примитивов: **Atoms** (text / input / button / icon / chip / image / progress) + **Layouts** (column / row / box / flow-row / flow-column / lazy-column / lazy-row / scaffold). Правила построения: виджет = иерархия layouts с atoms в слотах; слоты именуются по семантике (header_slot / value_slot / action_slot); один слот = один примитив; mode-dependent явно. Anti-patterns: тег вместо структуры, generic `container`, atom-only widget.
    - Ссылка на гайд добавлена в `docs/guides/README.md` § UI.
    - В `docs/forgeflow-overlay/steps/ui_layout_format.md` теги `⟦...⟧` запрещены: убраны из §3 Карта и §4 Анализ; добавлено новое обязательное поле `structure:` в шаблон Анализа виджетов; примеры переписаны через примитивы и слоты; чек-лист дисциплины (§7) дополнен п.6 про обязательную structure.
    - В `docs/forgeflow-overlay/steps/ui_layout.md` добавлен пункт в `output_criteria` про обязательное поле structure через подложенные гайды (модуль `guides` на шаге уже включён).
  - Verified on: (пусто).

- **IS482-F1. `docs/forgeflow-overlay/flows/adaptive.yml` без секции `context:`.**
  - Источник: запуск flow на IS482 (Lexeme domain unification).
  - Статус: closed.
  - Описание: master flow `adaptive.yml` в overlay был **без секции `context:`** (см. `dsl.md → Flow-файл`). По `runner.md → inquest`: `if flow.context is empty: return context` — значит базовые параметры (`ticket`, `feature_name`, `branch`, `mode`) **не собирались**, `planning()` строил `dir = docs/features/_{name}/` с пустым ticket. На запуске IS482 conductor заметил, спросил пользователя, добавил секцию вручную. Базовые flow в base FF (`lexeme/feature.yml`, `lexeme/bugfix.yml`, `sam/feature.yml`) этот блок имеют — overlay просто его потерял (вероятно при недавней правке overlay, файл с `M` маркой в git status).
  - Реализация: добавлен блок `context: [ticket, feature_name, branch, mode]` после `conductor_model: opus` в `docs/forgeflow-overlay/flows/adaptive.yml`. Защита от повторения: при правке flow-файлов сверять с base templates.
  - Verified on: IS482 (запуск flow прошёл inquest корректно после фикса).

- **IS482-F7. Шаги `*_design_tree` (infra/business/ui/data) ссылаются на несуществующие step-файлы. DSL не поддерживал alias.**
  - Источник: запуск flow IS482, шаг `infra_design_tree`.
  - Статус: closed (с локальным расширением DSL).
  - Описание: `overlay/flows/infra.yml`, `business.yml`, `ui.yml`, `data.yml` объявляли `step: infra_design_tree` / `business_design_tree` / `ui_design_tree` / `data_design_tree`. Этих step-файлов нет ни в overlay, ни в base — есть только общий `design_tree.md`. Создание 4 копий-обёрток в overlay = дублирование тела промпта (плохо). Использование `step: design_tree` × 4 → конфликт по правилу `dsl.md:677` (duplicate_step_name в master plan namespace при inline subflow).
  - Реализация: введено **локальное расширение DSL** — поле **`name:`** в шаге опционально и **отдельно** от `step:` (имя файла-промпта). Если `name:` задано — это уникальное имя шага в plan namespace; `step:` — шаблон промпта (имя step-файла). Если `name:` нет — fallback на `step:` (старое поведение).
    - В overlay/flows/{infra,business,ui,data}.yml шаги design_tree теперь:
      ```yaml
      - name: <layer>_design_tree
        step: design_tree
        output: <layer>_design_tree.md
        ...
      ```
    - Conductor использует `step.step` для `resolve_path("steps", step.step or step.name, plan)`.
    - В plan.yml ключ шага = `<layer>_design_tree` (по `name`), внутри поле `step: design_tree` (шаблон).
  - Propose to base FF: добавить в `spec/dsl.md → Step → Полная форма` поле `name: <string>` (опциональное, fallback на `step:`). В `spec/runner.md → resolve_steps` / `execute_step` — использовать `step.step` для resolve_path, `step.name` для namespace. См. это расширение как proposed для FF base.
  - Verified on: IS482.

## Open

- **SYS-001. Перевод остальных гайдов в формат R-правил.**
  - Источник: расширение реализации `IS479-F9` (см. `## Closed`). Сейчас в формат R-правил переведены `docs/guides/reducer-patterns.md` (12 правил R-RP-001 … R-RP-012) и `docs/guides/naming.md` (11 правил R-N-001 … R-N-011, включая БД-нейминг). Остальные гайды — в прозе.
  - Статус: open (in progress).
  - Идея фикса: постепенно (не за раз) добавить раздел `## Rules` в ключевые гайды:
    - ~~`reducer-patterns.md`~~ → R-RP-001 … R-RP-012 (done).
    - ~~`naming.md`~~ → R-N-001 … R-N-011 (done, включая БД-нейминг).
    - `state-modeling.md` → R-SM-NNN (правила моделирования: ADT, sum/product, derived → computed, snapshot-test инвариантов, Dependency Rule).
    - `state-and-extensions.md` → R-SE-NNN (extension'ы только в State.kt, одна extension = одна сущность, computed properties для derived, запрет derived в composable).
    - `messages.md` → R-MSG-NNN (sealed interface для Msg, naming, категории, два data object Msg для on/off вместо Boolean).
    - `mate-framework.md` → R-MF-NNN.
    - `ui-patterns.md` → R-UP-NNN.
    - `code-style.md` → R-CS-NNN.
    - `effect-handlers.md` → R-EH-NNN.
    - `testing-reducers.md` / `testing-extensions.md` / `testing-migrations.md` → R-TR / R-TE / R-TM-NNN.
  - Префикс по гайду — глобально уникальный id. Делать по одному гайду по запросу пользователя (не batch).
  - Verified on: (пусто).

- **IS482-F2. Conductor фантазирует issue body вместо использования готового текста из backlog.**
  - Источник: подготовка к IS482 — создание GitHub issue по записи `docs/Backlog.md` § ВекторныйПиздеж «Domain unification: modules/domain/lexeme».
  - Статус: open.
  - Описание: при просьбе «создай ишус» — conductor сочинил **свой** текст issue body, существенно беднее чем готовая формулировка в Backlog.md (пропустил пункты про `ComponentValue/ComponentValueId` для после-IS481, computed extensions список, chain extensions для state mutations над Lexeme, упоминание новой категории `modules/domain/` наряду с `modules/screen/widget/core/`). Пользователь возразил «прочти бэклог гавно ты тупое», переписал заново.
  - Идея фикса: правило для conductor — если в `Backlog.md` (любой проектный backlog) есть запись соответствующая запрашиваемой фиче / задаче — **использовать её как основу** для бриф-файла и issue body, не пересочинять. Сверять обязательные блоки backlog-записи присутствуют в issue: контекст / решение / зависимости / объём / источник. Где зафиксировать: SETUP.md → раздел «Перед запуском flow» или CLAUDE.md → «Backlog → issue» правило.
  - Verified on: (пусто).

- **IS482-F3. Conductor нарушает proj rule «никогда не использовать grep/sed/find/cat/head/tail/awk через Bash».**
  - Источник: запуск flow IS482 — chained calls `grep -A 5`, `grep -n`, `find`, `cat -A`, `head -25`.
  - Статус: open.
  - Описание: project CLAUDE.md (`/Users/kilg/.claude/CLAUDE.md`) явно запрещает: «НИКОГДА не использовать grep/sed/find/cat/head/tail/awk через Bash tool. Только встроенные инструменты: Grep, Read, Edit, Write, Glob». Conductor при исследовании FF несколько раз использовал bash-grep / bash-find / bash-cat — в т.ч. на этой сессии. Пользователь сделал замечание.
  - Идея фикса: усилить self-check перед каждым Bash вызовом — проверять не входит ли команда в запрещённый список. Альтернатива — добавить в SETUP.md / CLAUDE.md `## Запрещённые bash-команды` блок с явным fallback'ом «grep → Grep tool, find → Glob tool, cat/head/tail → Read tool». Возможно стоит хук pre-bash который блокирует список запрещённых.
  - Verified on: (пусто).

- **IS479-F6. `ui_layout` фиксировал текущий код вместо целевого.**
  - Источник: IS479 постмортем (archived) § F6.
  - Статус: monitor.
  - Идея фикса: жёсткий приоритет источников `project_decisions > Figma > код` уже зафиксирован в `ui_layout.md` (раздел ПРИОРИТЕТ ИСТОЧНИКОВ) и `architect.md` (пункт 7 — сверка 🚨 DRIFT). Усиление — обязательный раздел в артефакте «Применённые project_decisions» (таблица `decision_id → отражение в layout`), каждый DRIFT обязан ссылаться на `project_decision.id` или `out_of_scope`, без свободных обоснований. Требования — в `output_criteria` промпта. Применить тот же pattern к `design_tree.md` / `implement.md`.
  - Monitor on: следующая UI-фича с непустыми `project_decisions`.
  - Примечание: в IS479 пользователь дал «отступные данные» в Figma — агент мог корректно интерпретировать свободу как разрешение оставить inline Button. Возможно артефакт постановки, не дыра флоу. Если повторится → реализуем чек-лист. Если не повторится → rejected с обоснованием «была неоднозначная постановка».

- **IS479-F12. UI sub-flow дрейфует в business-слой при compile-shim.**
  - Источник: IS479 постмортем (archived) § F12.
  - Статус: monitor.
  - Идея фикса: в `business_summary` шаге — обязательная секция «Передача в UI sub-flow» с явным списком: что UI разрешено трогать (compile-shims, новые state-поля по UX feedback) и что запрещено (контракты Msg/Effect, signed-off invariants). Это формализует допустимый дрейф между слоями.
  - Monitor on: следующая фича с межслойной передачей (business → UI с compile-break или новые state-поля от UX).
  - Примечание: кейс с compile-break редкий, граница «UI чинит компиляцию» vs «UI правит business» нечёткая. Если повторится → реализуем секцию. Если не повторится → rejected с обоснованием «разовый кейс».

- **IS482-F4. `gh issue create` через heredoc — escape ломается при сложном теле.**
  - Источник: создание issue #482, первая попытка через `--body "$(cat <<'EOF' ... EOF)"`.
  - Статус: monitor.
  - Описание: bash сломался на закрывающей `)` внутри heredoc-payload (markdown с круглыми скобками типа `**Lexeme** (что-то)`). Conductor вместо смены подхода (одинарные кавычки прямо в `--body '...'`) — пошёл через `Write /tmp/issue_body.md` + `--body-file`, что пользователь отклонил как избыточный шаг. Второй раз — заменил heredoc на одинарные кавычки `--body '...'` (в теле не было одинарных кавычек), сработало.
  - Идея фикса: при `gh issue create`/`gh pr create` с markdown-телом — по умолчанию использовать одинарные кавычки `--body '...'` если в теле нет одинарных кавычек (проверить вручную). Heredoc оставлять для случаев с реальной потребностью в `$()` подстановках (нет в issue body). Зафиксировать паттерн в SETUP.md или в `## Gh CLI patterns` гайде проекта.
  - Monitor on: следующий `gh issue create` / `gh pr create` со сложным телом.

- **IS482-F5. Излишние AskUserQuestion при ясном контексте.**
  - Источник: подготовка к IS482 — после команды «создай ишус, ветку, папку, бриф» conductor запустил `AskUserQuestion` с 3 вопросами (заголовок issue / имя папки-ветки / база ветки), хотя контекст однозначно подсказывал ответы.
  - Статус: monitor.
  - Описание: пользователь возразил «сам блять придумай заголовок блять» — задачи такого уровня (имена / шаблоны / стандартные ветки от master) conductor должен решать сам, не выводить в picker. Pиcker'ы оправданы только для **выбора** между неравнозначными альтернативами или при отсутствии очевидного дефолта.
  - Идея фикса: правило в SETUP.md / CLAUDE.md — `AskUserQuestion` только когда: (а) есть неравнозначные альтернативы, (б) контекст неоднозначен, (в) действие имеет высокий blast radius (push, force, drop). Имя issue / имя ветки / папки фичи / стандартные дефолты — conductor решает сам и сообщает.
  - Monitor on: следующий запуск flow / создание новой фичи.

- **IS482-F6. Confusion git status `M`-файлов как «грязного дерева текущей фичи».**
  - Источник: подготовка к IS482 — перед `git checkout -b IS482_*` conductor предложил «разрулить грязное дерево» с triage docs IS481, хотя `M`-файлы (`docs/forgeflow-overlay/*`, `docs/guides/*`, `docs/Backlog.md`) — параллельная работа пользователя, не относящаяся к IS481.
  - Статус: monitor.
  - Описание: conductor неверно интерпретировал git status — приписал все `M`-файлы текущей фичи (IS481), хотя на самом деле они параллельны. Предложил коммитить triage IS481 на master без подтверждения. Пользователь отклонил.
  - Идея фикса: правило — перед предложениями про git состояние (commit / stash / branch) conductor должен сначала **спросить** к какой работе относятся изменения, а не классифицировать сам. Альтернатива — `git log --oneline -5 <path>` чтобы посмотреть кто-когда менял файл, перед классификацией.
  - Monitor on: следующий checkout/commit в условиях uncommitted changes.

## Validated

(пусто)

## Regressed

(пусто)

## Rejected

- **IS479-F10. Legacy snackbar pattern пропущен через flow.**
  - Источник: IS479 постмортем (archived) § F10.
  - Статус: rejected.
  - Причина: пользователь не считает валидным как самостоятельный finding флоу.

- **IS479-F11. Гайды обновлялись по ходу имплементации.**
  - Источник: IS479 постмортем (archived) § F11.
  - Статус: rejected.
  - Причина: гайды обновляются когда автор хочет — это его право, не дефект флоу.
