package me.apomazkin.core_db_impl.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import me.apomazkin.core_db_impl.entity.ComponentOptionDb

/**
 * DAO для `component_options`. IS486 фаза 1.
 *
 * Фаза 1 — минимальный набор: insert (seed builtin-опций при создании словаря)
 * + чтение живых опций типа. CRUD опций конструктора (К1–К5) — фаза 3.
 */
@Dao
interface ComponentOptionDao {

    @Insert
    suspend fun insert(option: ComponentOptionDb): Long

    @Query(
        """
        SELECT * FROM component_options
        WHERE component_type_id = :typeId AND removed_at IS NULL
        ORDER BY position ASC
        """
    )
    suspend fun getForType(typeId: Long): List<ComponentOptionDb>

    /**
     * IS486, каскад-модуль: опции набора типов ВКЛЮЧАЯ soft-deleted
     * (планировщику нужен весь граф ссылок).
     */
    @Query("SELECT * FROM component_options WHERE component_type_id IN (:typeIds)")
    suspend fun getAllForTypes(typeIds: List<Long>): List<ComponentOptionDb>

    @Query("SELECT * FROM component_options WHERE id = :id")
    suspend fun getById(id: Long): ComponentOptionDb?

    /**
     * IS486: реактивные живые опции типов словаря (JOIN на component_types —
     * Room наблюдает обе таблицы; снапшот конструктора пере-эмитится при
     * CRUD опций, а не только типов).
     */
    @Query(
        """
        SELECT o.* FROM component_options o
        JOIN component_types t ON t.id = o.component_type_id
        WHERE (t.dictionary_id = :dictionaryId OR t.dictionary_id IS NULL)
          AND o.removed_at IS NULL
        ORDER BY o.position ASC
        """
    )
    fun flowForDictionary(dictionaryId: Long): kotlinx.coroutines.flow.Flow<List<ComponentOptionDb>>

    /** К2: переименование — label-override (id устойчив, лейбл меняется везде). */
    @Query("UPDATE component_options SET label = :label, updated_at = :now WHERE id = :id")
    suspend fun updateLabel(id: Long, label: String, now: java.util.Date): Int

    /** К3–К5: soft-delete опции (ссылки зависимостей НЕ зануляются — degraded вычисляем). */
    @Query("UPDATE component_options SET removed_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: java.util.Date): Int

    /** Следующая позиция для К1 (в конец списка живых). */
    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM component_options WHERE component_type_id = :typeId")
    suspend fun nextPosition(typeId: Long): Int
}
