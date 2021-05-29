package me.apomazkin.core_db_impl

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Term
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_api.entity.WriteQuiz
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.mapper.DefinitionMapper
import me.apomazkin.core_db_impl.mapper.TermMapper
import me.apomazkin.core_db_impl.mapper.WordMapper
import me.apomazkin.core_db_impl.mapper.WriteQuizMapper
import me.apomazkin.core_db_impl.room.WordDao
import java.util.*
import javax.inject.Inject

class CoreDbApiImpl @Inject constructor(
    private val wordDao: WordDao
) : CoreDbApi {

    override fun addWord(value: String): Completable {
        val currentDate = Date(System.currentTimeMillis())
        return wordDao.addWord(
            WordDb(
                word = value,
                addDate = currentDate,
            )
        )
    }

    override fun getWord(id: Long): Single<Word> {
        val mapper = WordMapper()
        return wordDao
            .getWordById(id)
            .map { value -> mapper.map(value) }
    }

    override fun updateWord(word: Word): Completable {
        val mapper = WordMapper()
        return wordDao
            .updateWorld(mapper.reverseMap(word))
    }

    override fun removeWord(id: Long): Completable {
        return wordDao.getWord(id)
            .flatMap { word ->
                wordDao.removeWord(id).toSingle { word.definitionDbList }
            }
            .flatMap { list ->
                wordDao.deleteDefinitions(*list.toTypedArray()).toSingle { list }
            }
            .flattenAsObservable { list -> list.asIterable() }
            .flatMapCompletable { ttt -> wordDao.removeWriteQuiz(ttt.id ?: -1) }
    }

    override fun addDefinition(definition: Definition): Completable {
        val mapper = DefinitionMapper()
        return wordDao.addDefinition(mapper.reverseMap(definition))
            .flatMapCompletable { id ->
                val date = Date(System.currentTimeMillis())
                wordDao.addWriteQuiz(
                    WriteQuizDb(
                        definitionId = id,
                        addDate = date,
                        lastSelectDate = date,
                    )
                )
            }
    }

    override fun getDefinition(id: Long): Single<Definition> {
        val mapper = DefinitionMapper()
        return wordDao.getDefinitionById(id)
            .map { value -> mapper.map(value) }
    }

    override fun getDefinitionListByWordId(wordId: Long): Single<List<Definition>> {
        val mapper = DefinitionMapper()
        return wordDao.getDefinitionListByWordId(wordId)
            .map { list -> mapper.map(list) }
    }

    override fun updateDefinition(definition: Definition): Completable {
        val mapper = DefinitionMapper()
        return wordDao.updateDefinition(mapper.reverseMap(definition))
    }

    override fun removeDefinition(id: Long): Completable {
        return wordDao.deleteDefinition(id)
    }

    override fun getTermList(): Observable<List<Term>> {
        val mapper = TermMapper()
        return wordDao
            .getTermList()
            .map { list -> list.map { item -> mapper.map(item) } }
    }

    override fun searchTermList(pattern: String): Observable<List<Term>> {
        val mapper = TermMapper()
        return wordDao
            .searchTerms(pattern)
            .map { list -> list.map { item -> mapper.map(item) } }
    }

    override fun wordCount(): Single<Int> {
        return wordDao.getWordCount()
    }

    override fun getDefinitionCount(): Single<Int> {
        return wordDao.getDefinitionCount()
    }

    override fun getDefinitionTypeCount(wordClass: String): Single<Int> {
        return wordDao.getDefinitionTypeCount(wordClass)
    }

    override fun getWriteQuizCountByGrade(tier: Int): Single<Int> {
        return wordDao.getWriteQuizCountByGrade(tier)
    }

    override fun getWriteQuizList(grade: Int, limit: Int): Single<List<WriteQuiz>> {
        val mapper = WriteQuizMapper()
        return wordDao.getWriteQuizList(grade, limit)
            .map { list -> mapper.map(list) }
    }

    override fun updateWriteQuizList(writeQuiz: WriteQuiz): Completable {
        val mapper = WriteQuizMapper()
        return wordDao.updateWriteQuiz(mapper.reverseMap(writeQuiz))
    }

    override fun removeWriteQuiz(definitionId: Long): Completable {
        return wordDao.removeWriteQuiz(definitionId)
    }

}