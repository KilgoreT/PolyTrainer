package me.apomazkin.core_db_impl.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import me.apomazkin.core_db_impl.entity.ComponentValueDb
import java.util.Date

/**
 * DAO для `component_values`. IS481 (M13).
 *
 * Active фильтр `removed_at IS NULL` — convention для read paths
 * (aspect `dao_convention`).
 *
 * `delete` / `deleteByLexemeAndType` — hard-delete для legacy callsites
 * (`LexemeApiImpl.deleteComponentValue` / `updateLexemeTranslation`).
 */
@Dao
interface ComponentValueDao {

    @Query("SELECT * FROM component_values WHERE lexeme_id = :lexemeId AND removed_at IS NULL")
    suspend fun getForLexeme(lexemeId: Long): List<ComponentValueDb>

    @Query("SELECT * FROM component_values WHERE id = :id AND removed_at IS NULL")
    suspend fun getById(id: Long): ComponentValueDb?

    @Query(
        """
        SELECT * FROM component_values
        WHERE lexeme_id = :lexemeId AND component_type_id = :typeId AND removed_at IS NULL
        """
    )
    suspend fun getForLexemeAndType(lexemeId: Long, typeId: Long): ComponentValueDb?

    @Insert
    suspend fun insert(value: ComponentValueDb): Long

    @Update
    suspend fun update(value: ComponentValueDb)

    @Query("DELETE FROM component_values WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM component_values WHERE lexeme_id = :lexemeId AND component_type_id = :typeId")
    suspend fun deleteByLexemeAndType(lexemeId: Long, typeId: Long): Int

    @Query("SELECT COUNT(*) FROM component_values WHERE lexeme_id = :lexemeId AND removed_at IS NULL")
    suspend fun countForLexeme(lexemeId: Long): Int

    /**
     * Atomic insert одного value с cardinality-safety на `is_multiple=false`.
     * Если для (lexeme_id, component_type_id) уже есть active value (removed_at IS NULL)
     * и компонент с `is_multiple=false` — abort через IllegalStateException.
     *
     * `is_multiple` lookup делается caller-mapper'ом (`LexemeApiImpl`) и передаётся
     * параметром — DAO не должен JOIN'ить component_types.
     *
     * @throws IllegalStateException если попытка добавить single-value второй раз.
     */
    @Transaction
    suspend fun insertSingleSafe(value: ComponentValueDb, isMultiple: Boolean): Long {
        if (!isMultiple) {
            val existing = countActiveForLexemeAndType(value.lexemeId, value.componentTypeId)
            if (existing > 0) {
                throw IllegalStateException(
                    "Single-cardinality violation: typeId=${value.componentTypeId} " +
                        "already has active value for lexemeId=${value.lexemeId}"
                )
            }
        }
        return insert(value)
    }

    @Query(
        """
        SELECT COUNT(*) FROM component_values
        WHERE lexeme_id = :lexemeId AND component_type_id = :typeId
          AND removed_at IS NULL
        """
    )
    suspend fun countActiveForLexemeAndType(lexemeId: Long, typeId: Long): Int

    /**
     * Cascade soft-delete всех active values для компонента
     * (вызывается из `LexemeApiImpl.softDeleteComponentType` внутри withTransaction).
     */
    @Query(
        """
        UPDATE component_values
        SET removed_at = :now, updated_at = :now
        WHERE component_type_id = :typeId AND removed_at IS NULL
        """
    )
    suspend fun softDeleteByTypeId(typeId: Long, now: Date): Int

    /**
     * IS486, каскад-модуль: живые значения набора типов — вход планировщика
     * (`planCascade`), собирается внутри транзакции softDelete.
     */
    @Query(
        """
        SELECT * FROM component_values
        WHERE component_type_id IN (:typeIds) AND removed_at IS NULL
        """
    )
    suspend fun getActiveByTypeIds(typeIds: List<Long>): List<ComponentValueDb>

    /**
     * IS486, каскад-модуль: исполнение плана — soft-delete значений по id одним UPDATE.
     */
    @Query(
        """
        UPDATE component_values
        SET removed_at = :now, updated_at = :now
        WHERE id IN (:ids) AND removed_at IS NULL
        """
    )
    suspend fun softDeleteByIds(ids: List<Long>, now: Date): Int

    /**
     * COUNT active values для типа (preview deletion impact).
     */
    @Query(
        "SELECT COUNT(*) FROM component_values WHERE component_type_id = :typeId AND removed_at IS NULL"
    )
    suspend fun countActiveByTypeId(typeId: Long): Int

    /**
     * Lexeme ids у которых для данного type есть >1 active values —
     * cardinality downgrade guard (`editComponentType` multi→single transition).
     *
     * Deterministic ORDER: `MAX(updated_at) DESC, lexeme_id ASC` (tie-break)
     * — top-3 vs full filter живёт в Reducer (`ImpactedLexemesPreview`).
     * LIMIT не применяется здесь.
     */
    @Query(
        """
        SELECT lexeme_id
        FROM component_values
        WHERE component_type_id = :typeId AND removed_at IS NULL
        GROUP BY lexeme_id
        HAVING COUNT(*) > 1
        ORDER BY MAX(updated_at) DESC, lexeme_id ASC
        """
    )
    suspend fun findLexemesWithMultipleValuesForType(typeId: Long): List<Long>

    /**
     * Distinct dictionary ids среди active lexemes у которых есть active value этого типа.
     * JOIN: cv → lexemes (lexeme_id → id) → words (word_id → id).
     */
    @Query(
        """
        SELECT DISTINCT w.dictionary_id
        FROM component_values cv
        JOIN lexemes l ON l.id = cv.lexeme_id
        JOIN words w ON w.id = l.word_id
        WHERE cv.component_type_id = :typeId AND cv.removed_at IS NULL
        """
    )
    suspend fun dictionaryIdsForTypeId(typeId: Long): List<Long>

    /**
     * Aggregated count per type — для `UserDefinedTypesUsageSnapshot.valueCountByType`.
     */
    @Query(
        """
        SELECT component_type_id AS typeId, COUNT(*) AS count
        FROM component_values
        WHERE removed_at IS NULL
        GROUP BY component_type_id
        """
    )
    suspend fun aggregatedValueCountPerType(): List<TypeIdCount>

    /**
     * Per-dictionary count: для `DictionaryTypesSnapshot.valueCountByType`.
     */
    @Query(
        """
        SELECT cv.component_type_id AS typeId, COUNT(*) AS count
        FROM component_values cv
        JOIN lexemes l ON l.id = cv.lexeme_id
        JOIN words w ON w.id = l.word_id
        WHERE cv.removed_at IS NULL AND w.dictionary_id = :dictId
        GROUP BY cv.component_type_id
        """
    )
    suspend fun aggregatedValueCountPerTypeForDict(dictId: Long): List<TypeIdCount>

    /** IS486: реактивная версия — снапшот конструктора обновляет счётчики значений. */
    @Query(
        """
        SELECT cv.component_type_id AS typeId, COUNT(*) AS count
        FROM component_values cv
        JOIN lexemes l ON l.id = cv.lexeme_id
        JOIN words w ON w.id = l.word_id
        WHERE cv.removed_at IS NULL AND w.dictionary_id = :dictId
        GROUP BY cv.component_type_id
        """
    )
    fun flowAggregatedValueCountPerTypeForDict(dictId: Long): kotlinx.coroutines.flow.Flow<List<TypeIdCount>>

    /**
     * Distinct (type, dictionary) пары — для `UserDefinedTypesUsageSnapshot.dictionaryIdsByType`.
     */
    @Query(
        """
        SELECT cv.component_type_id AS typeId, w.dictionary_id AS dictId
        FROM component_values cv
        JOIN lexemes l ON l.id = cv.lexeme_id
        JOIN words w ON w.id = l.word_id
        WHERE cv.removed_at IS NULL
        GROUP BY cv.component_type_id, w.dictionary_id
        """
    )
    suspend fun typeDictPairs(): List<TypeDictPair>
}

/** Вспомогательный DTO для aggregated value count per type. */
data class TypeIdCount(val typeId: Long, val count: Int)

/** Вспомогательный DTO для (type, dictionary) пар. */
data class TypeDictPair(val typeId: Long, val dictId: Long)
