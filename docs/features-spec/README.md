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
