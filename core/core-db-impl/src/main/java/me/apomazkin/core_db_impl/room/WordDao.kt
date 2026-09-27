package me.apomazkin.core_db_impl.room

import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_impl.entity.ComponentValueDb
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.LexemeDbEntity
import me.apomazkin.core_db_impl.entity.QuizConfigDb
import me.apomazkin.core_db_impl.entity.TermDbEntity
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.entity.WriteQuizDbEntity

/**
 * IS500: проекция счётчика группы для квиз-пикера
 * ([WordDao.flowQuizGroupCounts]).
 */
data class QuizGroupCountDb(
    @ColumnInfo(name = "groupId") val groupId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "wordCount") val wordCount: Int,
)

// TODO: 20.03.2021 переименгвать Dao
@Dao
interface WordDao {

    /**
     * Dictionaries
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun _addDictionaryRow(dictionaryDb: DictionaryDb): Long

    @Insert
    suspend fun _addQuizConfigRow(config: QuizConfigDb): Long

    /**
     * IS481 (AGG-4 реверс): atomic INSERT dictionary + default quiz_config row(s)
     * в одной транзакции. F1 invariant — каждый dictionary имеет default config
     * `[BuiltIn(TRANSLATION)]` для `quiz_mode='write'` сразу после создания.
     */
    @Transaction
    suspend fun addDictionary(dictionaryDb: DictionaryDb): Long {
        val newDictionaryId = _addDictionaryRow(dictionaryDb)
        _addQuizConfigRow(
            QuizConfigDb(
                dictionaryId = newDictionaryId,
                quizMode = "write",
                componentRefs = """[{"type":"builtin","key":"translation"}]""",
            )
        )
        return newDictionaryId
    }

    @Query("SELECT * FROM dictionaries WHERE numericCode = :numericCode")
    suspend fun getDictionaryByNumeric(numericCode: Int): DictionaryDb?

    @Query("SELECT * FROM dictionaries")
    suspend fun getDictionaries(): List<DictionaryDb>

    @Query("SELECT * FROM dictionaries WHERE id = :id")
    suspend fun getDictionaryById(id: Long): DictionaryDb?

    @Query("UPDATE dictionaries SET name = :name, numericCode = :numericCode, changeDate = :changeDate WHERE id = :id")
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?, changeDate: Long)

    @Query("DELETE FROM dictionaries WHERE id = :id")
    suspend fun deleteDictionary(id: Long)

    @Query("SELECT * FROM dictionaries")
    fun flowDictionaries(): Flow<List<DictionaryDb>>

    /**
     * WORD
     */
    @Insert
    fun addWordSuspend(wordDb: WordDb): Long

    @Update
    fun updateWorldSuspend(wordDb: WordDb): Int

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun removeWordSuspend(id: Long): Int

    /**
     * TERM
     */

    @Transaction
    @Query("SELECT * FROM words WHERE dictionary_id = :langId ORDER BY id DESC")
    suspend fun getTermList(langId: Int): List<TermDbEntity>


    @Transaction
    @Query("SELECT * FROM words WHERE value LIKE :pattern AND dictionary_id = :langId ORDER BY id DESC")
    suspend fun searchTerms(pattern: String, langId: Long): List<TermDbEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM words
             WHERE (:pattern = '' OR value LIKE :pattern || '%')
             AND dictionary_id = :langId
             ORDER BY id DESC
    """
    )
    fun searchTermsPaging(pattern: String, langId: Int): PagingSource<Int, TermDbEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM words
            WHERE (:pattern = '' OR value LIKE :pattern || '%')
            AND dictionary_id = :langId
            ORDER BY id
            DESC
            LIMIT :limit
            OFFSET :offset
    """
    )
    suspend fun searchTermsManual(
        pattern: String,
        langId: Int,
        limit: Int,
        offset: Int
    ): List<TermDbEntity>

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id ORDER BY id DESC")
    suspend fun getTermById(id: Long): TermDbEntity?

    /**
     * IS493 (Р4, Э3-задел): контент слов по готовым id. Порядок id DESC.
     * Bind-лимит SQLite (999) покрыт контрактом вызова: ids ≤50.
     * В Э2 «Все» использует [flowTermsWindow]; этот метод — для узлов Э3.
     */
    @Transaction
    @Query("SELECT * FROM words WHERE id IN (:ids) ORDER BY id DESC")
    suspend fun getTermsByIds(ids: List<Long>): List<TermDbEntity>

    /**
     * IS493 Э2 (живое окно): контент первых [limit] слов словаря —
     * ПРЕДИКАТНЫЙ live-запрос (как searchTermsPaging): вставки в голову,
     * правки лексем и удаления переэмичиваются Room'ом сами. «Ещё»
     * расширяет limit; свернул узел — подписка гаснет на вызывающей стороне.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM words
            WHERE dictionary_id = :dictionaryId
            ORDER BY id DESC
            LIMIT :limit
        """
    )
    fun flowTermsWindow(dictionaryId: Long, limit: Int): Flow<List<TermDbEntity>>

    /**
     * IS493 Э5 (D20.1): живое окно слов ГРУППЫ — membership ∩ words,
     * id DESC LIMIT (зеркало [flowTermsWindow]). Обе таблицы в тексте
     * запроса — Room-инвалидация наблюдает и правки слов, и add/remove
     * membership.
     */
    @Transaction
    @Query(
        """
        SELECT w.* FROM words w
            JOIN word_groups wg ON wg.word_id = w.id
            WHERE wg.group_id = :groupId
            ORDER BY w.id DESC
            LIMIT :limit
        """
    )
    fun flowGroupTermsWindow(groupId: Long, limit: Int): Flow<List<TermDbEntity>>

    /**
     * IS493 Э5 (D20.2, ревью Data-2): словарь слова для JOIN-check
     * membership-мутаций; null — слово удалено (у words hard-delete,
     * removed_at нет).
     */
    @Query("SELECT dictionary_id FROM words WHERE id = :id")
    suspend fun getWordDictionaryId(id: Long): Long?

    /**
     * LEXEME
     */
    /**
     * Сырой INSERT лексемы — строительный блок compound-вставок
     * ([addLexemeWithQuiz], [addLexemeWithComponents]) и тестов.
     * Domain-инвариант «лексема ⇔ write_quiz» держат ТОЛЬКО
     * compound-методы: прямой вызов из production-кода создаст
     * лексему-невидимку для квизов (IS500 считает пригодность
     * по этому инварианту).
     */
    @Insert
    suspend fun addLexeme(lexemeDb: LexemeDb): Long

    @Update
    suspend fun updateLexeme(lexemeDb: LexemeDb): Int

    @Transaction
    @Query("SELECT * FROM lexemes WHERE id = :id")
    suspend fun getLexemeById(id: Long): LexemeDbEntity?

    @Query("UPDATE lexemes SET word_class = :value WHERE id = :id")
    suspend fun updateLexemeCategory(id: Long, value: String): Int

    @Query("DELETE FROM lexemes WHERE id = :id")
    suspend fun deleteLexemeById(id: Long): Int


    /**
     * Other
     */
    @Query("DELETE FROM lexemes WHERE id = :id")
    suspend fun deleteDefinitionSuspend(vararg id: Long): Int

    @Delete
    suspend fun deleteDefinitionsSuspend(vararg definition: LexemeDb)

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordSuspend(id: Long): TermDbEntity

    /**
     * QUIZ
     */
    @Insert
    suspend fun addWriteQuiz(writeQuizDb: WriteQuizDb): Long

    @Insert
    suspend fun _insertComponentValue(value: ComponentValueDb): Long

    /**
     * Atomic INSERT лексемы + write-quiz записи в одной транзакции.
     * Гарантирует domain-инвариант «у каждой лексемы есть write-quiz».
     */
    @Transaction
    suspend fun addLexemeWithQuiz(lexemeDb: LexemeDb, dictionaryId: Long): Long {
        val newLexemeId = addLexeme(lexemeDb)
        addWriteQuiz(WriteQuizDb.create(dictionaryId = dictionaryId, lexemeId = newLexemeId))
        return newLexemeId
    }

    /**
     * IS481 (MIN-9 + M13): atomic compound INSERT — lexeme + write_quiz +
     * N component_values в одной транзакции. FK violation на любом шаге → rollback
     * всего (regression test IS479 F1 + MIN-9 atomicity).
     *
     * **F171 (M13):** cardinality pre-check (F169 / F170) выполняется в
     * `LexemeApiImpl.addLexemeWithComponents` (Room `@Dao interface` не имеет
     * cross-DAO access — отсюда нельзя вызвать `componentTypeDao.getById(...)`).
     * WordDao выполняет ТОЛЬКО cascading INSERTs.
     *
     * @param components список full `ComponentValueDb` entities (с createdAt/updatedAt).
     *   `lexemeId` будет перезаписан на новый id после INSERT lexeme.
     */
    @Transaction
    suspend fun addLexemeWithComponents(
        lexemeDb: LexemeDb,
        dictionaryId: Long,
        components: List<ComponentValueDb>,
    ): Long {
        val newLexemeId = addLexeme(lexemeDb)
        addWriteQuiz(WriteQuizDb.create(dictionaryId = dictionaryId, lexemeId = newLexemeId))
        components.forEach { cv ->
            _insertComponentValue(cv.copy(lexemeId = newLexemeId))
        }
        return newLexemeId
    }

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateWriteQuiz(writeQuizDb: List<WriteQuizDb>): Int

    @Query(
        """
        SELECT id from write_quiz
        WHERE grade = :grade AND dictionary_id = :langId
            AND (:groupId IS NULL OR EXISTS (
                SELECT 1 FROM lexemes l
                JOIN word_groups wg ON wg.word_id = l.word_id
                JOIN dictionary_groups dg ON dg.id = wg.group_id
                    AND dg.removed_at IS NULL
                WHERE l.id = write_quiz.lexeme_id
                    AND wg.group_id = :groupId))
    """
    )
    suspend fun getWriteQuizIds(
        grade: Int,
        langId: Long,
        groupId: Long?,
    ): List<Long>

    @Transaction
    @Query("SELECT * from write_quiz WHERE id IN (:ids)")
    suspend fun getWriteQuizByIds(
        ids: List<Long>
    ): List<WriteQuizDbEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM write_quiz
        WHERE dictionary_id = :langId
            AND (:groupId IS NULL OR EXISTS (
                SELECT 1 FROM lexemes l
                JOIN word_groups wg ON wg.word_id = l.word_id
                JOIN dictionary_groups dg ON dg.id = wg.group_id
                    AND dg.removed_at IS NULL
                WHERE l.id = write_quiz.lexeme_id
                    AND wg.group_id = :groupId))
        ORDER BY last_select_date ASC
        LIMIT :limit
    """
    )
    suspend fun getEarliest(
        limit: Int,
        langId: Long,
        groupId: Long?,
    ): List<WriteQuizDbEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM write_quiz
        WHERE dictionary_id = :langId
            AND (:groupId IS NULL OR EXISTS (
                SELECT 1 FROM lexemes l
                JOIN word_groups wg ON wg.word_id = l.word_id
                JOIN dictionary_groups dg ON dg.id = wg.group_id
                    AND dg.removed_at IS NULL
                WHERE l.id = write_quiz.lexeme_id
                    AND wg.group_id = :groupId))
        ORDER BY error_count DESC
        LIMIT :limit
    """
    )
    suspend fun getFrequentMistakes(
        limit: Int,
        langId: Long,
        groupId: Long?,
    ): List<WriteQuizDbEntity>

    /**
     * IS500: живые группы словаря со счётчиком слов уровня 1 — слово
     * считается, только если имеет хотя бы одну лексему (инвариант
     * «лексема ⇔ write_quiz»). Двойной LEFT JOIN: группа без слов (или
     * со словами без лексем) возвращается со счётчиком 0 — порог
     * годности живёт ТОЛЬКО в домене, SQL вторым порогом не является.
     */
    @Query(
        """
        SELECT dg.id AS groupId, dg.name AS name,
               COUNT(DISTINCT l.word_id) AS wordCount
        FROM dictionary_groups dg
        LEFT JOIN word_groups wg ON wg.group_id = dg.id
        LEFT JOIN lexemes l ON l.word_id = wg.word_id
        WHERE dg.dictionary_id = :dictionaryId AND dg.removed_at IS NULL
        GROUP BY dg.id
    """
    )
    fun flowQuizGroupCounts(dictionaryId: Long): Flow<List<QuizGroupCountDb>>

    /**
     * IS500: счётчик уровня 1 для «Все» — слова словаря, имеющие хотя
     * бы одну лексему.
     */
    @Query(
        """
        SELECT COUNT(*) FROM words w
        WHERE w.dictionary_id = :dictionaryId
            AND EXISTS (SELECT 1 FROM lexemes l WHERE l.word_id = w.id)
    """
    )
    fun flowDictionaryQuizWordCount(dictionaryId: Long): Flow<Int>

    /**
     * STATISTIC
     */

    @Transaction
    @Query("SELECT COUNT(*) FROM words WHERE dictionary_id = :langId")
    fun flowWordCount(langId: Int): Flow<Int>

    @Transaction
    @Query(
        """
            SELECT COUNT(*)
            FROM lexemes
            INNER JOIN words ON lexemes.word_id = words.id
            WHERE words.dictionary_id = :langId
        """
    )
    fun flowLexemeCount(langId: Int): Flow<Int>


    @Query("SELECT COUNT(*) FROM write_quiz WHERE dictionary_id = :langId AND grade = :grade")
    fun flowQuizCount(langId: Int, grade: Int): Flow<Int>

}
