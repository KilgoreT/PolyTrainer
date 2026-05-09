<br>[05:15:42] flow: lexeme_bugfix → старт
<br>[05:20:53] step: triage → done
<br>[05:20:53] step: triage | Корневая причина: orphaned `write_quiz` записи ссылаются на несуществующие лексемы, Room крашится на NON-NULL `@Relation`
<br>[05:20:53] step: triage | Тип бага: simple (needs_spec_update = false), фикс в entity-слое core-db-impl
<br>[05:20:53] step: triage | Основная гипотеза: данные стали несогласованными при миграции 10→11, где FK constraints не enforce'ились
<br>[05:28:04] step: research → done
<br>[05:28:04] step: research | Исследованы WriteQuizDbEntity, LexemeDbWithWordDbRelation — двухуровневый @Relation с NON-NULL полями, оба уязвимы к orphaned данным
<br>[05:28:04] step: research | Миграция 10→11 копирует данные без FK enforcement и без cleanup orphaned записей — подтверждённый источник проблемы
<br>[05:28:04] step: research | Три метода DAO (getRandomWriteQuizList, getEarliest, getFrequentMistakes) подвержены одной и той же уязвимости
<br>[05:31:19] step: solutions → done
<br>[05:31:19] step: solutions | Предложены 4 варианта: A (nullable entity), B (миграция cleanup), C (A+B комбо), D (raw SQL JOIN)
<br>[05:31:19] step: solutions | Все варианты решают краш, различаются глубиной защиты и объёмом изменений
<br>[05:33:29] step: impact_analysis → done
<br>[05:33:29] step: impact_analysis | Проанализированы 4 варианта по 6 критериям
<br>[05:33:29] step: impact_analysis | Рекомендован вариант C (nullable entity + миграция cleanup) — баланс между защитой от краша и очисткой данных
<br>[05:33:29] step: impact_analysis | Вариант D (raw SQL) отклонён как over-engineering для багфикса
