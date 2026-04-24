# Спецификации фич

Контракты до реализации: entity, API, бизнес-логика, UI-логика, состояния.
Используются как вход для design_tree и implement в любом flow.

---

## По экранам

- [Управление словарями](dictionary-management.md) (CreateDictionary / LangScreen)
- Splash
- Главный экран (Main)
  - Словарь (DictionaryTab / VocabularyTab)
  - Выбор квиза (QuizTab)
  - Статистика (StatTab)
  - Настройки (SettingsTab)
- Карточка слова (WordCard)
- Квиз-чат (QuizChat)

## Общие

- Доменная модель (entity, Room, API контракты)
- Навигация
- DI
