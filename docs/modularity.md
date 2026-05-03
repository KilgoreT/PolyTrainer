# Модульность DSL — исследование

## Идея

Расширение простейшего DSL модулями. Модуль — сквозная функциональность, которая оборачивает шаги flow дополнительными действиями (до, после, вместо).

---

## Простейший DSL

Минимальный flow — только имена шагов:

```yaml
name: feature
steps:
  - task
  - spec
  - contract
  - implement
```

Runner: читает имя → находит step-файл → запускает суб-агента → следующий. Ничего больше.

---

## Проблема

Хочется сквозную функциональность:
- **Verification** — после каждого шага проверить/дополнить чеклист
- **Review** — после определённых шагов запустить ревьюеров
- **Logging** — лог каждого шага
- **Guides** — инжектировать гайды как input

Сейчас это хардкожено: `verification:` поле в flow, `review:` поле на шаге, логирование в conductor. Каждая новая фича = изменение runner'а/conductor'а/DSL.

---

## Модуль = обёртка шага

Модуль описывает **что делать вокруг шага**:

```
modules/
├── verification/
│   └── module.yml
├── review/
│   └── module.yml
└── logging/
    └── module.yml
```

### module.yml

```yaml
name: verification
hooks:
  before_step:
    - snapshot_roots
  after_step:
    - diff_roots
    - append_subitems
```

### Подключение к flow

```yaml
name: feature
modules: [verification, logging]
steps:
  - task
  - spec
  - contract
  - implement
```

Runner видит `modules` → для каждого шага оборачивает:

```
for step in steps:
    for module in modules:
        module.before_step(step)
    execute(step)
    for module in modules:
        module.after_step(step)
```

---

## Примитивные flow без модулей

Если `modules` не указан — runner просто идёт по шагам. Никаких обёрток.

```yaml
name: simple
steps:
  - task
  - implement
```

---

## Hooks

| Hook | Когда | Пример |
|------|-------|--------|
| `before_step` | до запуска шага | snapshot verification, инжект guides |
| `after_step` | после завершения шага | diff verification, запуск review, логирование |
| `on_error` | при ошибке шага | запись finding, уведомление |
| `on_start` | при старте flow | инициализация verification файла |
| `on_finish` | при завершении flow | финальный отчёт |

---

## Действия модуля

### Механические (runner сам):
- `snapshot(file)` — сохранить копию файла
- `diff_lines(pattern, snapshot, current)` — regex diff
- `inject_input(file)` — добавить файл в input шага
- `read(file)` / `write(file)` / `append(file)`

### Семантические (LLM):
- `run_agent(agent_name, input)` — запустить агента
- `extract_subitems(output, target)` — извлечь подпункты и дополнить файл

---

## Пример: модуль verification

```yaml
name: verification
on_start:
  - create_file: verification.md
    from: steps/verification_feature.md
after_step:
  - snapshot: verification.md
  - run_agent: verification_updater
    input: [step.output, verification.md]
  - diff_lines:
      pattern: "^- \\["
      snapshot: verification.md.snapshot
      current: verification.md
      on_diff: error
```

---

## Пример: модуль logging

```yaml
name: logging
before_step:
  - append: log.md
    text: "[{time}] step: {step.name} → начало"
after_step:
  - append: log.md
    text: "[{time}] step: {step.name} → {step.status}"
```

Чистый механический модуль. Без LLM.

---

## Что это даёт

- Conductor тупой: читает modules, вызывает hooks
- Новая функция = новая папка в modules/
- Модули компонуются: `modules: [verification, review, logging]`
- Модуль можно отключить: убрал из списка

---

## Вопросы

- Порядок модулей в after_step — по порядку в `modules:` списке?
- Модуль-специфичные поля DSL (review добавляет `review:` на шаг) — как валидировать?
- Состояние модуля — в dir фичи?
- Модуль vs step — verification_feature это step или on_start hook?
- Overlay модулей?

---

## MVP

1. Runner поддерживает `modules:` поле
2. Один модуль: `logging` (механический)
3. Hooks: `before_step`, `after_step`
4. Примитивы: `append(file, text)`

Если работает — добавить verification, review, findings.
