package me.apomazkin.core_db_api

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_api.entity.ComponentOptionApiEntity
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.CreateComponentOutcome
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.DictionaryTypesSnapshot
import me.apomazkin.core_db_api.entity.GroupApiEntity
import me.apomazkin.core_db_api.entity.EditComponentOutcome
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.MembershipSliceApiEntity
import me.apomazkin.core_db_api.entity.OptionCrudOutcome
import me.apomazkin.core_db_api.entity.QuizConfigApiEntity
import me.apomazkin.core_db_api.entity.QuizGroupCountApiEntity
import me.apomazkin.core_db_api.entity.RenameComponentOutcome
import me.apomazkin.core_db_api.entity.SetEnabledComponentOutcome
import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.UserDefinedTypesUsageSnapshot
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity
import me.apomazkin.group.AddMembershipOutcome
import me.apomazkin.group.CreateGroupOutcome
import me.apomazkin.group.DeleteGroupOutcome
import me.apomazkin.group.DeleteGroupWithWordsOutcome
import me.apomazkin.group.RemoveMembershipOutcome
import me.apomazkin.group.RenameGroupOutcome
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.TemplateValues

interface CoreDbApi {

    interface DbInstance {
        suspend fun instance(): String
        suspend fun closeDatabase()
        suspend fun openDatabase()
        suspend fun isDatabaseOpen(): Boolean
        fun getDbInfo(): DbInfo
    }

    data class DbInfo(
        val mem: String,
        val name: String,
        val version: Int,
        val path: String,
        val isOpen: Boolean,
    )

    /**
     *
     * Word = sequence of letters.
     *
     * Lexeme = translation + definition + category + options
     *
     * LexicalCategory - word class, for example, noun, verb etc
     *
     * Term = Word + Lexeme(s)
     */

    /**
     * xxx -> xxxApiEntity -> xxxDb
     */

    interface DictionaryApi {
        suspend fun addDictionary(name: String, numericCode: Int? = null): Long
        suspend fun getDictionary(numericCode: Int): DictionaryApiEntity?
        suspend fun getDictionaryById(id: Long): DictionaryApiEntity?
        suspend fun getDictionaryList(): List<DictionaryApiEntity>
        suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
        suspend fun deleteDictionary(id: Long)
        fun flowDictionaryList(): Flow<List<DictionaryApiEntity>>
    }

    interface TermApi {
        suspend fun getTermList(dictionaryId: Int): List<TermApiEntity>
        suspend fun searchTerms(
            pattern: String,
            dictionaryId: Long,
        ): List<TermApiEntity>

        fun searchTermsPaging(
            pattern: String,
            dictionaryId: Int,
        ): Flow<PagingData<TermApiEntity>>

        suspend fun getTermById(id: Long): TermApiEntity?

        /**
         * IS493 (Р4, Э3-задел): контент слов по готовым id, порядок id DESC.
         * Контракт вызова: ids ≤50 (bind-лимит SQLite 999 не достигается).
         */
        suspend fun getTermsByIds(ids: List<Long>): List<TermApiEntity>

        /**
         * IS493 Э2 (живое окно «Все»): контент первых [limit] слов словаря,
         * live-Flow (Room-инвалидация по words/lexemes/values) — вставки в
         * голову, правки и удаления переэмичиваются сами.
         */
        fun flowTermsWindow(dictionaryId: Long, limit: Int): Flow<List<TermApiEntity>>
    }

    /**
     * IS493: группы (подсловари). Мутации — транзакции §2.4
     * (read → validate → write в immediateTransaction); резерв имён и
     * locale — конструкторная инъекция impl (D12.3), в сигнатурах их нет.
     */
    interface GroupApi {
        /**
         * Id-срез membership словаря одним запросом (А13): наблюдает
         * `words` + `word_groups` + `dictionary_groups`; слово вне живых
         * групп — `(wordId, null)`; порядок — id DESC (порядок «Слов»).
         */
        fun membershipSlice(dictionaryId: Long): Flow<List<MembershipSliceApiEntity>>

        /** Живые группы словаря (`removed_at IS NULL`); Э3 — все корневые. */
        fun groupTree(dictionaryId: Long): Flow<List<GroupApiEntity>>

        /**
         * Э3: создание корневой группы ([parentId] — задел Э4).
         * Имя нормализуется валидатором (trim+NFC) — хранится нормализованное.
         */
        suspend fun createGroup(
            dictionaryId: Long,
            name: String,
            parentId: Long? = null,
        ): CreateGroupOutcome

        /**
         * Переименование; self-exclusion — смена регистра собственного
         * имени легальна (D-1/T-1). NotFound = отсутствует ∪ soft-deleted.
         */
        suspend fun renameGroup(groupId: Long, name: String): RenameGroupOutcome

        /**
         * Soft-delete группы + hard-delete её membership'ов одной
         * транзакцией (§3.2); каскад поддерева — Э4.
         */
        suspend fun deleteGroup(groupId: Long): DeleteGroupOutcome

        /**
         * Э6 (D30.2): ДЕСТРУКТИВ — удалить группу ВМЕСТЕ со всеми её
         * словами (hard-delete слов: каскад чистит лексемы/значения/
         * квиз-статистику/membership всех групп; legacy-samples чистятся
         * явно). Число удалённых — факт на момент транзакции.
         * @param chunkSize размер чанка DELETE IN (bind-лимит SQLite);
         *   параметр только для тестов границы чанка.
         */
        suspend fun deleteGroupWithWords(
            groupId: Long,
            chunkSize: Int = 999,
        ): DeleteGroupWithWordsOutcome

        // === Э5 (D20.1): membership слова + окно контента группы ===

        /**
         * Живые группы слова (чипы/пикер карточки). Наблюдает
         * `word_groups` + `dictionary_groups` — rename/delete группы
         * переэмичивает. Сортировка — Collator на вызывающей стороне (А8).
         */
        fun wordGroups(wordId: Long): Flow<List<GroupApiEntity>>

        /**
         * Живое окно слов группы: membership ∩ words, id DESC LIMIT
         * (зеркало flowTermsWindow «Все»). Add/remove membership и правки
         * слов переэмичиваются Room'ом сами.
         */
        fun flowGroupWordsWindow(groupId: Long, limit: Int): Flow<List<TermApiEntity>>

        /**
         * Добавить слово в группу. Порядок проверок (D20.2): слово →
         * группа-в-словаре-слова → INSERT OR IGNORE; всё в одной
         * транзакции. NotFound-исходы тихие (правит живая подписка).
         */
        suspend fun addWordToGroup(wordId: Long, groupId: Long): AddMembershipOutcome

        /**
         * Снять слово с группы; отсутствие строки — идемпотентный
         * NotFound-успех.
         */
        suspend fun removeWordFromGroup(wordId: Long, groupId: Long): RemoveMembershipOutcome
    }

    interface WordApi {
        fun addWordSuspend(value: String, dictionaryId: Int): Long
        suspend fun deleteWordSuspend(id: Long): Int
        suspend fun updateWordSuspend(id: Long, value: String): Boolean
    }

    interface LexemeApi {
        suspend fun getLexemeById(id: Long): LexemeApiEntity?
        suspend fun deleteLexeme(id: Long): Int

        // addLexeme(wordId) — УДАЛЁН (IS500): создавал лексему БЕЗ
        // write_quiz, ломая инвариант «лексема ⇔ write_quiz»
        // (production-вызовов не было). Создание лексемы — только
        // atomic-методы ниже.

        // ===== Generic component API (IS481, AGG-6) =====

        /**
         * Atomic INSERT lexeme + write_quiz + первый built-in component_value
         * в одной транзакции. Закрывает domain-инвариант
         * «у каждой лексемы есть write-quiz».
         */
        suspend fun addLexemeWithBuiltInComponent(
            wordId: Long,
            dictionaryId: Long,
            systemKey: BuiltInComponent,
            data: TemplateValues,
        ): Long

        /**
         * Atomic INSERT lexeme + write_quiz + первый user-defined component_value.
         * Lookup типа по `(dictionary_id, name, system_key=NULL)`.
         * @return null если type не найден в словаре.
         */
        suspend fun addLexemeWithUserDefinedComponent(
            wordId: Long,
            dictionaryId: Long,
            name: String,
            data: TemplateValues,
        ): Long?

        /**
         * Atomic compound INSERT lexeme + write_quiz + N component_values (MIN-9).
         * Используется в `restoreLexeme` для атомарного восстановления
         * translation + definition.
         */
        suspend fun addLexemeWithComponents(
            wordId: Long,
            dictionaryId: Long,
            components: List<Pair<ComponentTypeRef, TemplateValues>>,
        ): Long?

        suspend fun addComponentValue(
            lexemeId: Long,
            componentTypeId: Long,
            data: TemplateValues,
        ): Long

        suspend fun updateComponentValue(
            componentValueId: Long,
            data: TemplateValues,
        ): Int

        /**
         * @return количество оставшихся components у лексемы после delete.
         * Caller cascades lexeme delete если 0.
         */
        suspend fun deleteComponentValue(componentValueId: Long): Int

        suspend fun getComponentTypes(dictionaryId: Long): List<ComponentTypeApiEntity>

        /**
         * IS486: живые опции CHOICE-компонента (по position). Для не-CHOICE — пусто.
         */
        suspend fun getComponentOptions(componentTypeId: Long): List<ComponentOptionApiEntity>

        /**
         * IS491: подсказки caption для captioned_text-компонента — distinct живые
         * caption-значения строго этого типа; регистр значим (дедупа нет, Д4);
         * сортировка по частоте, tie-break — алфавит. One-shot.
         */
        suspend fun getCaptionSuggestions(componentTypeId: Long): List<String>

        /**
         * IS481: реактивный поток ВСЕХ active типов словаря (built-in global +
         * per-dict user-defined) — driver для WordCard ChipsRow. Делегат к
         * `ComponentTypeDao.flowTypesForDictionary` (built-in включён, `dictionary_id IS NULL`).
         */
        fun flowTypesForDictionary(dictionaryId: Long): Flow<List<ComponentTypeApiEntity>>

        suspend fun getQuizConfig(
            dictionaryId: Long,
            quizMode: String,
        ): QuizConfigApiEntity?

        // ===== Component constructor (IS481, M13) =====

        /**
         * Реактивная подписка на все user-defined active component_types
         * (`system_key IS NULL AND removed_at IS NULL`) + aggregated usage.
         */
        fun flowAllUserDefinedTypesWithUsage(): Flow<UserDefinedTypesUsageSnapshot>

        /**
         * Реактивная подписка на active types применимые к словарю:
         * `(dictionary_id = :dictId OR dictionary_id IS NULL) AND removed_at IS NULL`
         * + valueCount within dict. IS486 фаза 3: builtin-строки ВКЛЮЧЕНЫ
         * (рубильник enabled в конструкторе), optionsByType — живые опции CHOICE.
         */
        fun flowUserDefinedTypesForDictionary(dictionaryId: Long): Flow<DictionaryTypesSnapshot>

        /**
         * Atomic create user-defined component_type:
         *  1) two-prong SELECT для name collision (aspect `userdefined_identity_invariant`);
         *  2) INSERT row(s) — для `Scope.PerDictionaries(N)` создаётся N rows в одной
         *     транзакции;
         *  3) Возврат typed outcome со ВСЕМИ созданными rows (F2 iter1 review).
         */
        suspend fun createUserDefinedComponent(
            name: String,
            template: ComponentTemplate,
            isMultiple: Boolean,
            scope: Scope,
            // IS486 фаза 3: иерархия при создании. Дефолты = legacy-семантика (spec §11):
            // цель-лексема + ядро. XOR: заполнена максимум одна depends-ссылка.
            core: Boolean = true,
            dependsOnTypeId: Long? = null,
            dependsOnOptionId: Long? = null,
            // IS486: стартовые варианты CHOICE (лейблы); для не-CHOICE игнорируются.
            optionLabels: List<String> = emptyList(),
        ): CreateComponentOutcome

        /**
         * Atomic rename + cascade `quiz_configs.component_refs` (json_replace).
         * Built-in защищён `WHERE system_key IS NULL`.
         */
        suspend fun renameComponentType(
            typeId: Long,
            newName: String,
        ): RenameComponentOutcome

        /**
         * Edit user-defined component_type — UPDATE name / template / isMultiple
         * (IS481 phase 2).
         *
         * Template принимается параметром — immutability check на UseCase уровне
         * (F017); API возвращает [EditComponentOutcome.TemplateImmutable] как
         * defense-in-depth.
         *
         * Cascade `quiz_configs.component_refs` (json_replace) выполняется если
         * name изменился (parity с [renameComponentType]).
         *
         * Cardinality downgrade SELECT запускается ТОЛЬКО при
         * `isMultiple=false AND current.isMultiple=true` (F018) — иначе skip.
         *
         * Outcome ветки:
         * - [EditComponentOutcome.Removed] — `type.removed_at IS NOT NULL`.
         * - [EditComponentOutcome.BuiltInProtected] — `type.system_key IS NOT NULL`.
         * - [EditComponentOutcome.SameScopeCollision] / [EditComponentOutcome.CrossScopeCollision]
         *   — name занят (removed_at IS NULL фильтр).
         * - [EditComponentOutcome.CardinalityDowngradeBlocked] — `isMultiple: true → false`
         *   с impacted lexemes (`impactedLexemeIds` — полный deterministic список).
         * - [EditComponentOutcome.TemplateImmutable] — defensive (UseCase обычно перехватывает).
         * - [EditComponentOutcome.Success] — UPDATE прошёл.
         */
        suspend fun editComponentType(
            typeId: Long,
            name: String,
            template: ComponentTemplate,
            isMultiple: Boolean,
            // IS486 фаза 3: перепривязка цели. null-пара = цель-лексема (+core).
            // Умный сброс (решение 2026-07-21, spec §9.4): гаснут только значения
            // лексем, где НОВОЕ условие не выполнено (+ их поддеревья).
            core: Boolean = true,
            dependsOnTypeId: Long? = null,
            dependsOnOptionId: Long? = null,
        ): EditComponentOutcome

        /**
         * IS486: рубильник enabled (spec §6). Выключение последнего включённого
         * ядра словаря — [SetEnabledComponentOutcome.LastEnabledCore] (spec §7.8).
         * Каскадов нет: disable только убирает компонент из предложений.
         */
        suspend fun setComponentEnabled(
            typeId: Long,
            enabled: Boolean,
        ): SetEnabledComponentOutcome

        /** IS486 К1: добавить опцию CHOICE-компонента (label, в конец списка). */
        suspend fun addComponentOption(
            componentTypeId: Long,
            label: String,
        ): OptionCrudOutcome

        /** IS486 К2: переименовать опцию (label-override; id устойчив). */
        suspend fun renameComponentOption(
            optionId: Long,
            label: String,
        ): OptionCrudOutcome

        /**
         * IS486 К3–К5: soft-delete опции + комбинированный каскад (значения-выборы +
         * поддеревья зависимых; зависимые компоненты становятся degraded вычисляемо).
         */
        suspend fun deleteComponentOption(optionId: Long): OptionCrudOutcome

        /**
         * IS486: read-only preview удаления опции — dry-run комбинированного каскада
         * (значения-выборы + поддеревья зависимых) + деградирующие компоненты.
         * `null` — опция не найдена / уже removed.
         */
        suspend fun previewOptionDeletionImpact(optionId: Long): DeletionImpact?

        /**
         * IS486 (умный сброс, решение 2026-07-21): read-only preview перепривязки —
         * dry-run плана `TargetRebound` по графу с подменённой целью. Сбрасываются
         * только значения лексем, где новое условие не выполнено (+ их поддеревья);
         * `valueCount` — значения самого типа, `descendantValueCount` — потомки.
         * Нулевой impact = «безопасно». `null` — тип не найден / removed / builtin.
         */
        suspend fun previewRebindImpact(
            typeId: Long,
            core: Boolean,
            dependsOnTypeId: Long?,
            dependsOnOptionId: Long?,
        ): DeletionImpact?

        /**
         * Read-only preview: valueCount + dictionariesWithValues + affectedQuizConfigs +
         * affectedPrefs. JOIN component_values ⋈ lexemes ⋈ words ⋈ dictionaries +
         * scan quiz_configs.component_refs JSON. Prefs iterate — на UseCase-уровне.
         */
        suspend fun previewDeletionImpact(typeId: Long): DeletionImpact?

        /**
         * Atomic soft-delete + cascade:
         *  1) UPDATE component_types SET removed_at = :now WHERE id = ? AND system_key IS NULL;
         *  2) Cascade `quiz_configs.component_refs` (json_remove либо собрать новый JSON);
         *  3) Возврат `DeletionImpact` для UI snackbar.
         * Affected prefs сбрасываются ВНЕ транзакции на UseCase-уровне (DataStore, не Room).
         */
        suspend fun softDeleteComponentType(typeId: Long): SoftDeleteComponentOutcome

        // addLexemeWithDefinition / updateLexemeDefinition — УДАЛЕНЫ (AGG-6).
        // addLexeme(wordId, translation) / addLexeme(wordId, definition) overloads — УДАЛЕНЫ.
        // addLexemeWithTranslation / updateLexemeTranslation — УДАЛЕНЫ (IS486, фаза 1):
        // внешних вызовов не было; builtin пословарные, глобальный lookup не существует.
    }

    interface QuizApi {
        suspend fun addWriteQuiz(dictionaryId: Long, lexemeId: Long): Long
        suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertApiEntity>): Int

        /**
         * IS500: `groupId != null` сужает выборку до слов живой группы
         * (через лексему квиз-записи); null — весь словарь. Фильтр
         * применяется в WHERE самого запроса — до ORDER BY/LIMIT.
         */
        suspend fun getWriteQuizIds(
            grade: Int,
            dictionaryId: Long,
            groupId: Long? = null,
        ): List<Long>

        suspend fun getWriteQuizByIds(
            ids: List<Long>,
        ): List<WriteQuizComplexEntity>

        suspend fun getEarliestWriteQuizList(
            limit: Int,
            dictionaryId: Long,
            groupId: Long? = null,
        ): List<WriteQuizComplexEntity>

        suspend fun getFrequentMistakesWriteQuizList(
            limit: Int,
            dictionaryId: Long,
            groupId: Long? = null,
        ): List<WriteQuizComplexEntity>

        /**
         * IS500: живые группы словаря со счётчиком слов уровня 1 для
         * квиз-пикера (см. [QuizGroupCountApiEntity]). Живой Flow:
         * наблюдает группы, membership и лексемы.
         */
        fun flowQuizGroupCounts(dictionaryId: Long): Flow<List<QuizGroupCountApiEntity>>

        /**
         * IS500: счётчик уровня 1 для пункта «Все» — слова словаря с
         * хотя бы одной лексемой.
         */
        fun flowDictionaryQuizWordCount(dictionaryId: Long): Flow<Int>
    }

    interface StatisticApi {
        fun flowWordCount(dictionaryId: Int): Flow<Int>
        fun flowLexemeCount(dictionaryId: Int): Flow<Int>
        fun flowQuizCount(dictionaryId: Int, maxGrade: Int): Flow<Map<Int, Int>>
    }
}
