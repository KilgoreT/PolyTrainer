package me.apomazkin.core_db_impl

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.*
import me.apomazkin.core_db_impl.entity.*
import me.apomazkin.core_db_impl.mapper.*
import me.apomazkin.core_db_impl.room.WordDao
import java.util.*
import javax.inject.Inject

class CoreDbApiImpl @Inject constructor(
    private val wordDao: WordDao
) : CoreDbApi {

    override fun addWord(value: String, langId: Long): Completable {
        val currentDate = Date(System.currentTimeMillis())
        return wordDao.addWord(
            WordDb(
                word = value,
                langId = langId,
                addDate = currentDate,
            )
        )
    }

    override fun getWord(id: Long): Single<Word> {
        return wordDao
            .getWordById(id)
            .map { value -> value.toAppEntity() }
    }

    override fun updateWord(word: Word): Completable {
        return wordDao
            .updateWorld(word.toDbEntity())
    }

    // TODO: 07.09.2021 всю эту логику нужно вынести в юзкейсы
    override fun removeWord(id: Long): Completable {
        return wordDao.getWord(id)
            .flatMap { word ->
                wordDao.removeWord(id).toSingle { word.definitionSampleRelList }
            }
            .flatMap { list ->
                removeDefinition(
                    *list
                        .map { item -> item.definitionDb }
                        .toTypedArray()
                )
                    .toSingle { list }
                    .flattenAsObservable { l -> l.asIterable() }
                    .flatMapCompletable { item ->
                        wordDao.removeSample(*item.sampleDbList.toTypedArray())
                    }
                    .toSingle { list }
            }
            .flattenAsObservable { list -> list.asIterable() }
            .flatMapCompletable { item -> wordDao.removeWriteQuiz(item.definitionDb.id ?: -1) }
    }

    override fun addDefinition(definition: Definition, langId: Long): Completable {
        val mapper = DefinitionMapper()
        return wordDao.addDefinition(mapper.reverseMap(definition))
            .flatMapCompletable { id ->
                val date = Date(System.currentTimeMillis())
                wordDao.addWriteQuiz(
                    WriteQuizDb(
                        definitionId = id,
                        langId = langId,
                        addDate = date,
                        lastSelectDate = date,
                    )
                )
            }
    }

    override fun getDefinitionAll(): Single<List<Definition>> {
        val mapper = DefinitionMapper()
        return wordDao.getAllDefinition()
            .map { value -> mapper.map(value) }
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

    override fun removeDefinition(vararg id: Long): Completable {
        return wordDao.deleteDefinition(*id.toTypedArray().toLongArray())
    }

    private fun removeDefinition(vararg definition: DefinitionDb): Completable {
        return wordDao.deleteDefinitions(*definition.toList().toTypedArray())
    }

    override fun getTermList(): Observable<List<Term>> {
        return wordDao
            .getTermList()
            .map { list -> list.map(WordDefinitionRel::toAppData) }
    }

    override fun searchTermList(pattern: String, langId: Long): Observable<List<Term>> {
        return wordDao
            .searchTerms(pattern, langId)
            .map { list -> list.map(WordDefinitionRel::toAppData) }
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

    override fun getWriteQuizList(langId: Long): Single<List<WriteQuiz>> {
        return wordDao.getWriteQuizList(langId)
            .map { list -> list.toAppData() }
    }

    override fun getWriteQuizList(limit: Int, langId: Long): Single<List<WriteQuiz>> {
        return wordDao.getWriteQuizList(limit, langId)
            .map { list -> list.toAppData() }
    }

    override fun getWriteQuizListByAccessTime(
        grade: Int,
        limit: Int,
        langId: Long
    ): Single<List<WriteQuiz>> {
        return wordDao.getWriteQuizListByAccessTime(grade, limit, langId)
            .map { list -> list.toAppData() }
    }

    override fun getRandomWriteQuizList(
        grade: Int,
        limit: Int,
        langId: Long
    ): Single<List<WriteQuiz>> {
        return wordDao.getRandomWriteQuizList(grade, limit, langId)
            .map { list -> list.toAppData() }
    }

    override fun updateWriteQuizList(writeQuiz: WriteQuiz): Completable {
        return wordDao.updateWriteQuiz(writeQuiz.toDb())
    }

    override fun removeWriteQuiz(definitionId: Long): Completable {
        return wordDao.removeWriteQuiz(definitionId)
    }

    override fun addHint(definitionId: Long, value: String): Completable {
        return wordDao.addHint(
            HintDb(
                definitionId = definitionId,
                value = value,
                addDate = Date(System.currentTimeMillis())
            )
        )
    }

    override fun removeHint(id: Long): Completable {
        return wordDao.removeHint(id)
    }

    override fun removeHint(hint: Hint): Completable {
        val mapper = HintMapper()
        return wordDao.removeHint(mapper.reverseMap(hint))
    }

    override fun updateHint(hint: Hint): Completable {
        val mapper = HintMapper()
        return wordDao.updateHint(mapper.reverseMap(hint))
    }

    override fun addSample(definitionId: Long, value: String, source: String?): Completable {
        return wordDao.addSample(
            SampleDb(
                definitionId = definitionId,
                value = value,
                source = source,
                addDate = Date(System.currentTimeMillis())
            )
        )
    }

    override fun getSampleList(definitionId: Long): Single<List<Sample>> {
        return wordDao.getSampleListByDefinitionId(definitionId)
            .map { list -> list.toAppEntity() }
    }

    override fun getSampleList(): Observable<List<Sample>> {
        return wordDao.getSampleList()
            .map { list -> list.toAppEntity() }
    }
}