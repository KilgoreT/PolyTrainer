# Спецификации фич

Контракты до реализации: entity, API, бизнес-логика, UI-логика, состояния.
Используются как вход для design_tree и implement в любом flow.

**Структура:** одна папка = одна фича (экран / фича без экрана / часть экрана).
Внутри — `spec.md` (контракт), при наличии `ui.md` (UI-логика) и `user-scenarios.md` (сценарии редьюсера: Было→Шаги→Стало→🔧Тех).

---

## Общие

- [Доменная модель — Lexeme](lexeme-domain/spec.md) (entity, Room, API контракты)
- [Навигация](navigation/spec.md)
- [DI — принципы графа](dagger-di-principles/spec.md)
- [Логирование](logger/spec.md)

## По экранам

- Словари
  - [Список](dictionary-list/spec.md)
  - [Создание/редактирование](dictionary-create/spec.md)
- Splash
- Главный экран (Main)
  - Словарь (DictionaryTab / VocabularyTab)
  - Выбор квиза (QuizTab)
  - Статистика (StatTab)
  - Настройки (SettingsTab)
  - [WebView](webview-screen/spec.md)
- Карточка слова (WordCard)
  - [Бизнес / инварианты](wordcard/spec.md)
  - [UI-логика](wordcard/ui.md)
  - [Сценарии редьюсера](wordcard/user-scenarios.md)
- Квиз-чат (QuizChat)

## Фичи без экрана / общие

- [Конструктор компонентов](component-constructor/spec.md)

## Виджеты

- [DictionaryAppBar](dictionary-appbar/spec.md)
- [FlagPlaceholderWidget](flag-placeholder-widget/spec.md)

---

## Известные пробелы

Спеки для DictionaryTab/VocabularyTab и QuizChat пока не написаны. Часть инвариантов, относящихся к этим модулям, временно зафиксирована в `dictionary-list/spec.md`:

- раздел «Инварианты pref'а текущего словаря» — общий контракт для всех читателей `CURRENT_DICTIONARY_ID_LONG` (включая DictionaryTab и QuizChat);
- раздел «Системное ограничение: QuizChat» — поведение `getCurrentDictionaryId()` и `QuizGameImpl` при отсутствии словарей.

При написании отдельных спек для DictionaryTab и QuizChat соответствующие инварианты переносятся в них с кросс-ссылкой обратно.
