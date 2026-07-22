package me.apomazkin.core_db_impl.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_impl.entity.ComponentTypeDb
import java.util.Date

/**
 * DAO для `component_types`. IS481 (M13).
 *
 * Read-методы выбирают актуальные (не soft-deleted) типы. Built-in идут первыми
 * (system_key NOT NULL), внутри — по position.
 *
 * `getById` намеренно БЕЗ фильтра `removed_at IS NULL` — read-only views / lookup
 * при rename / soft-delete-impact. Write paths (`addComponentValue`,
 * `addLexemeWithComponents` в `LexemeApiImpl`) обязаны explicit-проверять
 * `type.removedAt != null` после `getById(...)` (F170).
 */
@Dao
interface ComponentTypeDao {

    /**
     * Все актуальные типы для словаря: built-in (global) + per-dictionary user-defined.
     */
    @Query(
        """
        SELECT * FROM component_types
        WHERE (dictionary_id = :dictionaryId OR dictionary_id IS NULL)
          AND removed_at IS NULL
        ORDER BY (system_key IS NULL) ASC, position ASC
        """
    )
    fun flowTypesForDictionary(dictionaryId: Long): Flow<List<ComponentTypeDb>>

    @Query(
        """
        SELECT * FROM component_types
        WHERE (dictionary_id = :dictionaryId OR dictionary_id IS NULL)
          AND removed_at IS NULL
        ORDER BY (system_key IS NULL) ASC, position ASC
        """
    )
    suspend fun getTypesForDictionary(dictionaryId: Long): List<ComponentTypeDb>

    @Query("SELECT * FROM component_types WHERE system_key IS NOT NULL AND removed_at IS NULL")
    suspend fun getBuiltInTypes(): List<ComponentTypeDb>

    /**
     * IS486: builtin пословарные — lookup строго в рамках словаря.
     * Глобальный getBySystemKey удалён: с пословарными строками он возвращал
     * произвольную из N и ломал привязку значений.
     */
    @Query(
        """
        SELECT * FROM component_types
        WHERE system_key = :key AND dictionary_id = :dictionaryId AND removed_at IS NULL
        """
    )
    suspend fun getBySystemKeyForDictionary(key: String, dictionaryId: Long): ComponentTypeDb?

    @Query("SELECT * FROM component_types WHERE id = :id")
    suspend fun getById(id: Long): ComponentTypeDb?

    /**
     * IS486, каскад-модуль: полный граф словаря ВКЛЮЧАЯ soft-deleted —
     * ссылки зависимостей не зануляются, планировщику нужен весь лес (spec §8).
     */
    @Query("SELECT * FROM component_types WHERE dictionary_id = :dictionaryId")
    suspend fun getAllForDictionaryWithRemoved(dictionaryId: Long): List<ComponentTypeDb>

    @Insert
    suspend fun insert(type: ComponentTypeDb): Long

    @Update
    suspend fun update(type: ComponentTypeDb)

    /**
     * Soft-delete только для user-defined типов. Built-in (`system_key NOT NULL`)
     * защищён на уровне SQL — UPDATE не сработает (см. 04 § «Built-in не удаляется»).
     * Touches `updated_at` тоже (semantic — изменение состояния row).
     */
    @Query("UPDATE component_types SET removed_at = :now, updated_at = :now WHERE id = :id AND system_key IS NULL")
    suspend fun softDelete(id: Long, now: Date): Int

    /**
     * Atomic rename + update `updated_at`. Built-in защищён `WHERE system_key IS NULL`.
     * @return количество затронутых rows (0 если built-in либо id не найден).
     */
    @Query("UPDATE component_types SET name = :newName, updated_at = :now WHERE id = :id AND system_key IS NULL")
    suspend fun renameUserDefined(id: Long, newName: String, now: Date): Int

    /**
     * Lookup по identity-tuple для two-prong SELECT
     * (aspect `soft_delete_unique_collision`). Per-dict branch.
     */
    @Query(
        """
        SELECT * FROM component_types
        WHERE dictionary_id = :dictId AND name = :name
          AND system_key IS NULL AND removed_at IS NULL
        LIMIT 1
        """
    )
    suspend fun findActiveUserDefinedByName(dictId: Long, name: String): ComponentTypeDb?

    /**
     * Global branch (`dictionary_id IS NULL`) — отдельный @Query т.к. SQL
     * `dictionary_id = NULL` всегда даёт UNKNOWN (F032).
     */
    @Query(
        """
        SELECT * FROM component_types
        WHERE dictionary_id IS NULL AND name = :name
          AND system_key IS NULL AND removed_at IS NULL
        LIMIT 1
        """
    )
    suspend fun findActiveGlobalByName(name: String): ComponentTypeDb?

    /**
     * Cross-scope коллизия (userdefined_identity_invariant, F039):
     * при создании global "X" проверить отсутствие active per-dict "X" в любом словаре.
     */
    @Query(
        """
        SELECT COUNT(*) FROM component_types
        WHERE dictionary_id IS NOT NULL AND name = :name
          AND system_key IS NULL AND removed_at IS NULL
        """
    )
    suspend fun countActivePerDictByName(name: String): Int

    /**
     * Подписка на все user-defined active (для `flowAllUserDefinedTypesWithUsage`).
     */
    @Query(
        """
        SELECT * FROM component_types
        WHERE system_key IS NULL AND removed_at IS NULL
        ORDER BY position ASC, created_at ASC
        """
    )
    fun flowAllUserDefined(): Flow<List<ComponentTypeDb>>

    /**
     * Подписка на типы применимые к словарю (active user-defined per-dict + global).
     * Для `flowUserDefinedTypesForDictionary` (read-side).
     */
    @Query(
        """
        SELECT * FROM component_types
        WHERE (dictionary_id = :dictId OR dictionary_id IS NULL)
          AND system_key IS NULL AND removed_at IS NULL
        ORDER BY (dictionary_id IS NULL) DESC, position ASC, created_at ASC
        """
    )
    fun flowUserDefinedForDictionary(dictId: Long): Flow<List<ComponentTypeDb>>
}
