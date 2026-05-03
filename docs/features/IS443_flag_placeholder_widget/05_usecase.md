# IS443 — UseCase: FlagPlaceholderWidget

## UseCase

**UseCase не требуется.**

Задача IS443 — перенос UI-виджета `FlagPlaceholderWidget` из `modules/screen/dictionary` в `modules/core/ui` и подстановка placeholder в трёх точках отображения. Это чистая UI-задача:

- Контракт Effects: **изменений нет** — нет новых side-эффектов, нет обращений к данным
- Контракт State: **изменений нет** — все данные (title, name, flagRes) уже доступны в State
- Контракт Messages: **изменений нет** — нет новых пользовательских действий

Правило step-файла: "методы UseCase вытекают из Effects контракта: каждый effect, который обращается к данным, соответствует методу UseCase". Effects = 0 → методов UseCase = 0 → интерфейс не создаётся.

Все изменения происходят на уровне composable-функций (conditional rendering) и перемещения файла. Data boundary не затрагивается.

## checklist_items
- root: "Пользователь открывает форму словаря без флага"
  items:
    - UseCase не требуется: нет Effects, нет обращений к данным [usecase]

## log_messages
- UseCase не требуется: задача IS443 — чистый UI-перенос, Effects = 0, data boundary не затрагивается

_model: claude-opus-4-6-20250502_
