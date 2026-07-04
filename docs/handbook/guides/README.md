# PolyTrainer — Гайды разработки

Конвенции и паттерны для разработки фич в PolyTrainer.

**Reference реализации:**
- `modules/screen/quiz/chat` — ChatReducer (актуальный стиль редьюсера, inline extension chains)
- `modules/screen/wordcard` — WordCard (полный TEA с тестами, reference для стейта/расширений)

---

## Архитектура

- [Архитектура проекта](project-architecture.md) — модули, слои, DI, сборка
- [Навигация](navigation.md) — NavHost, табы, маршруты, передача зависимостей
- [Слой данных](data-layer.md) — Room, DataStore, UseCase, маппинг сущностей

## Mate (TEA State Management)

- [Mate Framework](mate-framework.md) — типы, цикл, подключение, конвенции
- [Моделирование State](state-modeling.md) — UDF-подход, ADT, sum/product, State как БД, selectors, Dependency Rule (конспект доклада М. Левченко)
- [Стейт и расширения](state-and-extensions.md) — иерархия стейта, extension-функции, использование в редьюсере
- [Сообщения](messages.md) — sealed interface, именование, категории
- [Паттерны редьюсера](reducer-patterns.md) — inline chains, условия, динамические эффекты, логирование
- [Эффект-хендлеры](effect-handlers.md) — datasource/ui эффекты, FlowHandler, подключение
- [ViewModel и DI](viewmodel-wiring.md) — структура ViewModel, Factory, пошаговый гайд нового экрана

## Тестирование

- [Тестирование расширений](testing-extensions.md) — тесты чистых трансформаций, проверка иммутабельности
- [Тестирование редьюсеров](testing-reducers.md) — testReduce, testScenario, проверка стейта+эффектов
- [Тестирование Room-миграций](testing-migrations.md) — BaseMigration, Schemable, checkData, пошаговый гайд новой миграции

## UI

- [Паттерны UI](ui-patterns.md) — двухуровневые composable, отправка сообщений, общие виджеты, превью, лейаут
- [UI-примитивы](ui-primitives.md) — формальный словарь atoms / layouts + правила построения виджета через слоты для спецификаций (`ui_layout.md`)

## Планирование фич

- [Пользовательские сценарии](user-scenarios.md) — формат `NN_user_scenarios.md`: Было→Шаги→Стало→🔧Тех, дельта стейта + ссылка на тест-контракт, раздел «Решения»

## Стиль и конвенции

- [Naming](naming.md) — единый гайд именования: пакеты, файлы, классы, БД (таблицы / колонки / FK), enum-значения, resources, тесты + Rules (machine-checkable)
- [Dagger DI](dagger-di.md) — граф компонентов, модули, порядок создания, конвенции
- [Стиль кода](code-style.md) — форматирование, git-конвенции, gradle (именование вынесено в naming.md)
- [Логирование](logging.md) — LexemeLogger, sink-паттерн, уровни, теги, конфигурация
- [Тема и ресурсы](theme-and-resources.md) — цвета, типографика, ResourceManager, иконки, превью
- [Preferences](prefs-datastore.md) — DataStore, PrefsProvider, nullable Flow, ключи
- [Утилиты](tools-utils.md) — хелперы для работы со списками (modifyFiltered, insertToEnd)
