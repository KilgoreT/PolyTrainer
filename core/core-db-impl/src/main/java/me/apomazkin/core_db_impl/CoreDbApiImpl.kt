package me.apomazkin.core_db_impl

import android.annotation.SuppressLint
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override fun getLang(): Single<List<Language>> {
        return wordDao.getLanguages().map { it.toAppEntity() }
    }

    override suspend fun addLangSuspend(numericCode: Int, name: String): Long {
        val currentDate = Date(System.currentTimeMillis())
        return wordDao.addLanguageSuspend(
            LanguageDb(
                numericCode = numericCode,
                code = "",
                name = name,
                addDate = currentDate,
            )
        )
    }

    override suspend fun getLangSuspend(): List<Language> {
        return wordDao.getLanguagesSuspend().map { it.toAppEntity() }
    }

    override fun flowLang(): Flow<List<Language>> {
        return wordDao.flowLanguages().map { it.toAppEntity() }
    }

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

    override fun addWordSuspend(value: String, langId: Long): Long {
        val currentDate = Date(System.currentTimeMillis())
        return wordDao.addWordSuspend(
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

    override fun getAllWord(): Single<List<Word>> {
        return wordDao
            .getWord()
            .map { it.toAppEntity() }
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

    override suspend fun deleteWordSuspend(id: Long): Int {
        wordDao.getWordSuspend(id).also { word ->
            wordDao.removeSampleSuspend(
                *word.definitionSampleRelList.map { it.sampleDbList }.flatten().toTypedArray()
            )
            wordDao.deleteDefinitionsSuspend(
                *word.definitionSampleRelList.map { it.definitionDb }.toTypedArray()
            )
            return wordDao.removeWordSuspend(id)
        }
    }

    override suspend fun updateWordSuspend(id: Long, value: String): Int {
        val wordRel = wordDao.getWordSuspend(id)
        val wordDb = wordRel.wordDb.copy(word = value)
        return wordDao.updateWorldSuspend(wordDb)
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

    override suspend fun addLexemeSuspend(
        wordId: Long,
        category: String,
        definition: String,
    ): Long {
        // TODO: Не забыть добавить WriteQuizDb
        val definitionDb = DefinitionDb(
            wordId = wordId,
            definition = definition,
            wordClass = category,
        )
        return wordDao.addDefinitionSuspend(definitionDb)
    }

    override suspend fun editLexemeSuspend(
        wordId: Long,
        lexemeId: Long,
        category: String,
        definition: String
    ): Int {
        val definitionDb = DefinitionDb(
            id = lexemeId,
            wordId = wordId,
            definition = definition,
            wordClass = category,
        )
        return wordDao.updateDefinitionSuspend(definitionDb)
    }

    override suspend fun updateLexemeDefinition(definitionId: Long, value: String): Int {
        return wordDao.updateLexemeDefinition(definitionId, value)
    }

    override suspend fun updateLexemeCategory(lexemeId: Long, category: String): Int {
        return wordDao.updateLexemeCategory(lexemeId, category)
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

    override fun updateLexemeDefinition(definition: Definition): Completable {
        val mapper = DefinitionMapper()
        return wordDao.updateDefinition(mapper.reverseMap(definition))
    }

    override fun removeDefinition(vararg id: Long): Completable {
        return wordDao.deleteDefinition(*id.toTypedArray().toLongArray())
    }

    override suspend fun deleteLexemeSuspend(vararg id: Long): Int {
        return wordDao.deleteDefinitionSuspend(*id.toTypedArray().toLongArray())
    }

    private fun removeDefinition(vararg definition: DefinitionDb): Completable {
        return wordDao.deleteDefinitions(*definition.toList().toTypedArray())
    }

    override fun getTermList(): Observable<List<Term>> {
        return wordDao
            .getTermList()
            .map { list -> list.map(WordDefinitionRel::toAppData) }
    }

    override suspend fun getTermById(id: Long): TermMate {
        return wordDao
            .getTermById(id)
            .toMateApp()
    }

    override suspend fun getTermList(langId: Long): List<TermMate> {
        return wordDao
            .getTermList(langId)
            .map(WordDefinitionRel::toMateApp)
    }

    override fun searchTermList(pattern: String, langId: Long): Observable<List<Term>> {
        return wordDao
            .searchTerms(pattern, langId)
            .map { list -> list.map(WordDefinitionRel::toAppData) }
    }

    override fun wordCount(langId: Long): Single<Int> {
        return wordDao.getWordCount(langId)
    }

    override fun getDefinitionCount(): Single<Int> {
        return wordDao.getDefinitionCount()
    }

    override fun getDefinitionTypeCount(wordClass: String): Single<Int> {
        return wordDao.getDefinitionTypeCount(wordClass)
    }

    override fun getWriteQuizCountByGrade(tier: Int, langId: Long): Single<Int> {
        return wordDao.getWriteQuizCountByGrade(tier, langId)
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

    override fun getDump(): Single<Dump> {
        return Single.zip(
            wordDao.getLanguages(),
            wordDao.getWord(),
            wordDao.getAllDefinition(),
            wordDao.getAllHint(),
            wordDao.getAllSample(),
            wordDao.getAllWriteQuiz(),
        ) { languages, words, definitions, hints, sample, write ->
            Dump(
                languages = languages.toDumpEntity(),
                words = words.toDumpEntity(),
                definitions = definitions.map { it.toDumpEntity() },
                hints = hints.map { it.toDumpEntity() },
                samples = sample.map { it.toDumpEntity() },
                writes = write.map { it.toDump() }
            )
        }
    }

    @SuppressLint("CheckResult")
    override fun restoreDump(dump: Dump) {
        dump.languages.forEach { lang ->
            wordDao.addLanguage(lang.toDbEntity())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {}
        }
        dump.words.forEach { word ->
            wordDao.addWord(word.toDbEntity())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {}
        }
        dump.definitions.forEach { def ->
            wordDao.addDefinition(def.toDbEntity())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ -> }
        }
        dump.hints.forEach { hint ->
            wordDao.addHint(hint.toDbEntity())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {}
        }
        dump.samples.forEach { sample ->
            wordDao.addSample(sample.toDbEntity())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {}
        }
        dump.writes.forEach { write ->
            wordDao.addWriteQuiz(write.toDb())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {}
        }
    }
}