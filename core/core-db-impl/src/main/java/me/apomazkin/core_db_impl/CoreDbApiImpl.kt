package me.apomazkin.core_db_impl

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Quiz
import me.apomazkin.core_db_api.entity.WordWithDefinition
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.mapper.DefinitionMapper
import me.apomazkin.core_db_impl.mapper.QuizMapper
import me.apomazkin.core_db_impl.mapper.WordWithDefinitionsMapper
import me.apomazkin.core_db_impl.room.WordDao
import javax.inject.Inject

class CoreDbApiImpl @Inject constructor(
    private val wordDao: WordDao
) : CoreDbApi {

    override fun addWord(value: String) {
        wordDao.addWord(WordDb(word = value))
    }

    override fun removeWord(id: Long) {
        wordDao
            .removeWord(id)
    }

    override fun addDefinition(definition: Definition) {
        val mapper = DefinitionMapper()
        wordDao
            .addDefinition(mapper.reverseMap(definition))
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

    override fun getRandomQuizList(): Single<List<Quiz>> {
        val mapper = QuizMapper()
        return wordDao.getRandomDefinition()
            .map { list -> mapper.map(list) }
    }

}