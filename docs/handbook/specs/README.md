# Спецификации фич

Контракты до реализации: entity, API, бизнес-логика, UI-логика, состояния.
Используются как вход для design_tree и implement в любом flow.

**Структура:** одна папка = одна фича (экран / фича без экрана / часть экрана).
Внутри — `spec.md` (контракт), при наличии `ui.md` (UI-логика) и `user-scenarios.md` (сценарии редьюсера: Было→Шаги→Стало→🔧Тех).

---

## Общие

- [Слово и лексема — доменная модель](word-model/spec.md) (термины и правила без кода: слово, лексема, ядро, атрибуты, что можно спросить в тренировке)
- [Доменная модель — Lexeme](lexeme-domain/spec.md) (entity, Room, API контракты)
- [Навигация](navigation/spec.md)
- [DI — принципы графа](dagger-di-principles/spec.md)
- [Логирование](logger/spec.md)

## По экранам

- Словари
  - [Список](dictionary-list/spec.md)
  - [Создание/редактирование](dictionary-create/spec.md)
  - Группы (подсловари)
    - [Домен / данные / логика](dictionary-groups/spec.md)
    - [UI-раскладка](dictionary-groups/ui.md)
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
  - [Поток, вопрос, меню, строки](quiz-chat/spec.md)

## Фичи без экрана / общие

- [Конструктор компонентов](component-constructor/spec.md)
- [Квиз по группе (фильтр выборки тренировки)](quiz-group-filter/spec.md)

## Виджеты

- [DictionaryAppBar](dictionary-appbar/spec.md)
- [FlagPlaceholderWidget](flag-placeholder-widget/spec.md)

---

## Известные пробелы

Спека для DictionaryTab/VocabularyTab пока не написана (QuizChat — написана при IS511, `quiz-chat/spec.md`). Часть инвариантов, относящихся к этим модулям, временно зафиксирована в `dictionary-list/spec.md`:

- раздел «Инварианты pref'а текущего словаря» — общий контракт для всех читателей `CURRENT_DICTIONARY_ID_LONG` (включая DictionaryTab и QuizChat);
- раздел «Системное ограничение: QuizChat» — поведение `getCurrentDictionaryId()` и `QuizGameImpl` при отсутствии словарей.

Инвариант порции квиза (без повторов лексем, предпочтение разных слов, добавки — IS508) временно зафиксирован в `quiz-group-filter/spec.md` §7.1.

При написании отдельных спек для DictionaryTab и QuizChat соответствующие инварианты переносятся в них с кросс-ссылкой обратно.
