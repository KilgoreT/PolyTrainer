package me.apomazkin.core_db_impl.room.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_impl.entity.GroupDb
import me.apomazkin.core_db_impl.entity.WordGroupDb

/**
 * Id-срез membership словаря (IS493, architecture §2.5 / А13):
 * строка на пару (слово, живая группа); слово вне групп — `(wordId, null)`.
 */
data class MembershipSliceDb(
    @ColumnInfo(name = "wordId") val wordId: Long,
    @ColumnInfo(name = "groupId") val groupId: Long?,
)

@Dao
interface GroupDao {

    /**
     * Id-срез словаря одним запросом. Каноническая форма (D6.3):
     * LEFT JOIN на предварительно склеенные ЖИВЫЕ membership — слово,
     * состоящее только в мёртвой группе, даёт `(wordId, null)` и не теряется;
     * слово в мёртвой + живой — только живую строку.
     *
     * `ORDER BY w.id DESC` — глобальный порядок «Все» (порядок вкладки
     * «Слова»); чанки режутся по этому списку.
     *
     * Все три таблицы (`words`, `word_groups`, `dictionary_groups`)
     * присутствуют в тексте запроса — Room-инвалидация наблюдает все три (А13).
     */
    @Query(
        """
        SELECT w.id AS wordId, m.group_id AS groupId
        FROM words w
        LEFT JOIN (
            SELECT wg.word_id, wg.group_id
            FROM word_groups wg
            JOIN dictionary_groups dg
              ON dg.id = wg.group_id AND dg.removed_at IS NULL
        ) m ON m.word_id = w.id
        WHERE w.dictionary_id = :dictionaryId
        ORDER BY w.id DESC
        """
    )
    fun membershipSlice(dictionaryId: Long): Flow<List<MembershipSliceDb>>

    // === Вставки: Э3 использует из транзакций CoreDbApiImpl (и тесты). ===

    @Insert
    suspend fun insertGroup(group: GroupDb): Long

    /**
     * IS493 Э5 (D20.2, ревью Data-4): `OR IGNORE` гасит ТОЛЬКО конфликт
     * составного PK (дубль membership) — возвращает rowid или `-1`
     * (ignore → AlreadyIn). FK-violation исключением НЕ гасится —
     * liveness-проверки обязаны идти в той же транзакции до вставки.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWordGroup(link: WordGroupDb): Long

    // === Э3 (D13.3): чтения/мутации для транзакций §2.4 ===

    /** Живая группа по id; null = отсутствует ∪ soft-deleted (А17/D-5). */
    @Query("SELECT * FROM dictionary_groups WHERE id = :id AND removed_at IS NULL")
    suspend fun getLivingGroupById(id: Long): GroupDb?

    /**
     * IS493 Э5 (D20.2): живая группа СТРОГО этого словаря — JOIN-check
     * membership-мутаций; null = мертва ∪ отсутствует ∪ чужой словарь.
     */
    @Query(
        "SELECT * FROM dictionary_groups " +
            "WHERE id = :id AND dictionary_id = :dictionaryId AND removed_at IS NULL"
    )
    suspend fun getLivingGroupByIdInDict(id: Long, dictionaryId: Long): GroupDb?

    /**
     * IS493 Э5 (D20.1): живые группы слова (для чипов/пикера карточки).
     * Обе таблицы в тексте запроса — инвалидация по membership И группам
     * (rename/delete группы переэмичивает). Сортировка — Collator на
     * вызывающей стороне (А8), не SQL.
     */
    @Query(
        """
        SELECT dg.* FROM dictionary_groups dg
            JOIN word_groups wg ON wg.group_id = dg.id
            WHERE wg.word_id = :wordId AND dg.removed_at IS NULL
        """
    )
    fun flowWordGroups(wordId: Long): Flow<List<GroupDb>>

    /**
     * IS493 Э5 (D20.2): снятие membership; rowcount 0 → NotFound
     * (идемпотентный успех — снимать нечего).
     */
    @Query("DELETE FROM word_groups WHERE word_id = :wordId AND group_id = :groupId")
    suspend fun deleteWordGroup(wordId: Long, groupId: Long): Int

    /** Живые группы словаря (SELECT сиблингов внутри транзакции). */
    @Query("SELECT * FROM dictionary_groups WHERE dictionary_id = :dictionaryId AND removed_at IS NULL")
    suspend fun livingGroups(dictionaryId: Long): List<GroupDb>

    /** Живые группы словаря — live-подписка (groupTree). */
    @Query("SELECT * FROM dictionary_groups WHERE dictionary_id = :dictionaryId AND removed_at IS NULL")
    fun flowLivingGroups(dictionaryId: Long): Flow<List<GroupDb>>

    /**
     * Rename живой группы; rowcount 0 → NotFound (liveness атомарно
     * в WHERE, без лишнего SELECT — ревью D-5).
     */
    @Query(
        "UPDATE dictionary_groups SET name = :name, updated_at = :updatedAt " +
            "WHERE id = :id AND removed_at IS NULL"
    )
    suspend fun updateGroupName(id: Long, name: String, updatedAt: java.util.Date): Int

    /**
     * Soft-delete списком — list-форма сразу (каскад Э4 работает
     * поддеревом; Э3 передаёт список из одного — ревью D-4).
     */
    @Query(
        "UPDATE dictionary_groups SET removed_at = :removedAt " +
            "WHERE id IN (:ids) AND removed_at IS NULL"
    )
    suspend fun softDeleteGroups(ids: List<Long>, removedAt: java.util.Date): Int

    /** Hard-delete membership'ов удаляемых групп (§3.2), list-форма. */
    @Query("DELETE FROM word_groups WHERE group_id IN (:ids)")
    suspend fun hardDeleteWordGroupsByGroups(ids: List<Long>)

    /**
     * Число membership-строк группы (включая осиротевшие у мёртвой группы —
     * прямое чтение таблицы, БЕЗ join'а живости). Тесты чистоты §3.2;
     * Э4 переиспользует для deleteImpact.
     */
    @Query("SELECT COUNT(*) FROM word_groups WHERE group_id = :groupId")
    suspend fun countWordGroups(groupId: Long): Int

    // === Э6 (D30): деструктивное удаление группы со словами ===

    /** Id слов группы по membership (прямое чтение, порядок не важен). */
    @Query("SELECT word_id FROM word_groups WHERE group_id = :groupId")
    suspend fun wordIdsOfGroup(groupId: Long): List<Long>

    /**
     * Hard-delete слов чанком (chunk на вызывающей стороне — bind-лимит).
     * FK CASCADE чистит lexemes/component_values/write_quiz/word_groups.
     */
    @Query("DELETE FROM words WHERE id IN (:ids)")
    suspend fun deleteWordsByIds(ids: List<Long>): Int
}
