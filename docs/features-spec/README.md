# Спецификации фич

Контракты до реализации: entity, API, бизнес-логика, UI-логика, состояния.
Используются как вход для design_tree и implement в любом flow.

---

## Общие

- Доменная модель (entity, Room, API контракты)
- [Навигация](navigation.md)
- [DI — принципы графа](dagger-di-principles.md)
- [Логирование](logger.md)

## По экранам

- Словари
  - [Список](dictionary-list.md)
  - [Создание/редактирование](dictionary-create.md)
- Splash
- Главный экран (Main)
  - Словарь (DictionaryTab / VocabularyTab)
  - Выбор квиза (QuizTab)
  - Статистика (StatTab)
  - Настройки (SettingsTab)
  - [WebView](webview-screen.md)
- Карточка слова (WordCard)
- Квиз-чат (QuizChat)

## Виджеты

- [DictionaryAppBar](dictionary-appbar.md)
- [FlagPlaceholderWidget](flag-placeholder-widget.md)

---

## Известные пробелы

Спеки для DictionaryTab/VocabularyTab и QuizChat пока не написаны. Часть инвариантов, относящихся к этим модулям, временно зафиксирована в `dictionary-list.md`:

- раздел «Инварианты pref'а текущего словаря» — общий контракт для всех читателей `CURRENT_DICTIONARY_ID_LONG` (включая DictionaryTab и QuizChat);
- раздел «Системное ограничение: QuizChat» — поведение `getCurrentDictionaryId()` и `QuizGameImpl` при отсутствии словарей.

При написании отдельных спек для DictionaryTab и QuizChat соответствующие инварианты переносятся в них с кросс-ссылкой обратно.
