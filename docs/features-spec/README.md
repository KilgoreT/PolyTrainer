# Спецификации фич

Контракты до реализации: entity, API, бизнес-логика, UI-логика, состояния.
Используются как вход для design_tree и implement в любом flow.

---

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
- Карточка слова (WordCard)
- Квиз-чат (QuizChat)

## Виджеты

- [DictionaryAppBar](dictionary-appbar.md)
- [FlagPlaceholderWidget](flag-placeholder-widget.md)

## Общие

- Доменная модель (entity, Room, API контракты)
- Навигация
- DI
