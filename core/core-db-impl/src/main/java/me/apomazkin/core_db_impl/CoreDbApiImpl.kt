package me.apomazkin.core_db_impl

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_api.entity.WordWithDefinition
import me.apomazkin.core_db_api.entity.WriteQuiz
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.mapper.DefinitionMapper
import me.apomazkin.core_db_impl.mapper.WordMapper
import me.apomazkin.core_db_impl.mapper.WordWithDefinitionsMapper
import me.apomazkin.core_db_impl.mapper.WriteQuizMapper
import me.apomazkin.core_db_impl.room.WordDao
import javax.inject.Inject

class CoreDbApiImpl @Inject constructor(
    private val wordDao: WordDao
) : CoreDbApi {

    override fun addWord(value: String) {
        wordDao.addWord(WordDb(word = value))
    }

    override fun getWord(id: Long): Single<Word> {
        val mapper = WordMapper()
        return wordDao
            .getWordById(id)
            .map { value -> mapper.map(value) }
    }

    override fun removeWord(id: Long) {
        wordDao
            .removeWord(id)
    }

    override fun addDefinition(definition: Definition): Completable {
        val mapper = DefinitionMapper()
        return wordDao.addDefinition(mapper.reverseMap(definition))
            .flatMapCompletable { id ->
                wordDao.addWriteQuiz(WriteQuizDb(definitionId = id))
            }
//        wordDao.addWriteQuiz(WriteQuizDb(definitionId = definition.id))
    }

    override fun getDefinition(id: Long): Single<Definition> {
        val mapper = DefinitionMapper()
        return wordDao.getDefinitionById(id)
            .map { value -> mapper.map(value) }
    }


    override fun deleteDefinition(id: Long): Completable {
        return wordDao.deleteDefinition(id)
    }

    override fun getWordWithDefinition(): Observable<List<WordWithDefinition>> {
        val mapper = WordWithDefinitionsMapper()
        return wordDao
            .getWordListWithDefinition()
            .map { list -> list.map { item -> mapper.map(item) } }
    }

    override fun deleteWord(id: Long): Completable {
        return wordDao.getWord(id)
            .flatMap { word ->
                wordDao.removeWord(id)
                    .toSingle { word.definitionDbList }
            }
            .flatMapCompletable { list ->
                wordDao.deleteWordWithDefinition(*list.toTypedArray())
            }
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

    override fun getWriteQuizList(grade: Int, limit: Int): Single<List<WriteQuiz>> {
        val mapper = WriteQuizMapper()
        return wordDao.getWriteQuizList(grade, limit)
            .map { list -> mapper.map(list) }
    }

    override fun updateWriteQuizList(writeQuiz: WriteQuiz): Completable {
        val mapper = WriteQuizMapper()
        return wordDao.updateWriteQuiz(mapper.reverseMap(writeQuiz))
    }

}