package me.apomazkin.core_db_api

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.CreateComponentOutcome
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.DictionaryTypesSnapshot
import me.apomazkin.core_db_api.entity.EditComponentOutcome
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.QuizConfigApiEntity
import me.apomazkin.core_db_api.entity.RenameComponentOutcome
import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.UserDefinedTypesUsageSnapshot
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity
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
    }

    interface WordApi {
        fun addWordSuspend(value: String, dictionaryId: Int): Long
        suspend fun deleteWordSuspend(id: Long): Int
        suspend fun updateWordSuspend(id: Long, value: String): Boolean
    }

    interface LexemeApi {
        suspend fun getLexemeById(id: Long): LexemeApiEntity?
        suspend fun addLexeme(wordId: Long): Long
        suspend fun deleteLexeme(id: Long): Int

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
         * Реактивная подписка на active user-defined types применимые к словарю:
         * `(dictionary_id = :dictId OR dictionary_id IS NULL) AND system_key IS NULL
         *  AND removed_at IS NULL` + valueCount within dict.
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
        ): EditComponentOutcome

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

        // ===== Translation @Deprecated shim (A3) =====

        @Deprecated("Use addLexemeWithBuiltInComponent")
        suspend fun addLexemeWithTranslation(
            wordId: Long,
            dictionaryId: Long,
            translation: TranslationApiEntity,
        ): Long

        @Deprecated("Use updateComponentValue via generic path")
        suspend fun updateLexemeTranslation(
            id: Long,
            translation: TranslationApiEntity?,
        ): Long?

        // addLexemeWithDefinition / updateLexemeDefinition — УДАЛЕНЫ (AGG-6).
        // addLexeme(wordId, translation) / addLexeme(wordId, definition) overloads — УДАЛЕНЫ.
    }

    interface QuizApi {
        suspend fun addWriteQuiz(dictionaryId: Long, lexemeId: Long): Long
        suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertApiEntity>): Int

        suspend fun getWriteQuizIds(
            grade: Int,
            dictionaryId: Long,
        ): List<Long>

        suspend fun getWriteQuizByIds(
            ids: List<Long>,
        ): List<WriteQuizComplexEntity>

        suspend fun getEarliestWriteQuizList(
            limit: Int,
            dictionaryId: Long,
        ): List<WriteQuizComplexEntity>

        suspend fun getFrequentMistakesWriteQuizList(
            limit: Int,
            dictionaryId: Long,
        ): List<WriteQuizComplexEntity>
    }

    interface StatisticApi {
        fun flowWordCount(dictionaryId: Int): Flow<Int>
        fun flowLexemeCount(dictionaryId: Int): Flow<Int>
        fun flowQuizCount(dictionaryId: Int, maxGrade: Int): Flow<Map<Int, Int>>
    }
}
