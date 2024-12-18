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
import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.Dump
import me.apomazkin.core_db_api.entity.Hint
import me.apomazkin.core_db_api.entity.Language
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.SampleApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.core_db_api.entity.WriteQuiz
import me.apomazkin.core_db_impl.entity.HintDb
import me.apomazkin.core_db_impl.entity.LanguageDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.SampleDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.toApiEntity
import me.apomazkin.core_db_impl.mapper.HintMapper
import me.apomazkin.core_db_impl.mapper.toApiEntity
import me.apomazkin.core_db_impl.mapper.toAppData
import me.apomazkin.core_db_impl.mapper.toDb
import me.apomazkin.core_db_impl.mapper.toDbEntity
import me.apomazkin.core_db_impl.mapper.toDump
import me.apomazkin.core_db_impl.mapper.toDumpEntity
import me.apomazkin.core_db_impl.room.WordDao
import java.util.Date
import javax.inject.Inject

class CoreDbApiImpl @Inject constructor(
    private val wordDao: WordDao
) : CoreDbApi {

    class TermApiImpl @Inject constructor(
        private val wordDao: WordDao
    ) : CoreDbApi.TermApi {

        override suspend fun getTermList(langId: Long): List<TermApiEntity> {
            return wordDao.getTermList(langId).map { it.toApiEntity() }
        }

        override suspend fun searchTerms(pattern: String, langId: Long): List<TermApiEntity> {
            return wordDao.searchTerms(pattern, langId).map { it.toApiEntity() }
        }

        override suspend fun getTermById(id: Long): TermApiEntity? {
            return wordDao.getTermById(id = id)?.let { it.toApiEntity() }
        }
    }

    class LexemeApiImpl @Inject constructor(
        private val wordDao: WordDao
    ) : CoreDbApi.LexemeApi {

        override suspend fun getLexemeById(id: Long): LexemeApiEntity? {
            return wordDao.getLexemeById(id)?.toApiEntity()
        }

        override suspend fun addLexeme(wordId: Long): Long {
            val date = Date(System.currentTimeMillis())
            return wordDao.addLexeme(
                LexemeDb(
                    wordId = wordId,
                    addDate = date,
                )
            )
        }

        override suspend fun addLexeme(wordId: Long, translation: TranslationApiEntity): Long {
            val date = Date(System.currentTimeMillis())
            return wordDao.addLexeme(
                LexemeDb(
                    wordId = wordId,
                    translation = translation.value,
                    addDate = date,
                )
            )
        }

        override suspend fun addLexeme(wordId: Long, definition: DefinitionApiEntity): Long {
            val date = Date(System.currentTimeMillis())
            return wordDao.addLexeme(
                LexemeDb(
                    wordId = wordId,
                    definition = definition.value,
                    addDate = date,
                )
            )
        }

        override suspend fun updateLexemeTranslation(
            id: Long,
            translation: TranslationApiEntity?
        ): Long? {
            val updatedRows = wordDao.updateLexemeTranslation(id, translation?.value)
            return if (updatedRows > 0) id else null
        }

        override suspend fun updateLexemeDefinition(
            id: Long,
            definition: DefinitionApiEntity?
        ): Long? {
            val updatedRows = wordDao.updateLexemeDefinition(id, definition?.value)
            return if (updatedRows > 0) id else null
        }

        override suspend fun deleteLexeme(id: Long): Int {
            return wordDao.deleteLexemeById(id)
        }
    }

    override fun getLang(): Single<List<Language>> {
        return wordDao.getLanguages().map { it.toApiEntity() }
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
        return wordDao.getLanguagesSuspend().map { it.toApiEntity() }
    }

    override fun flowLang(): Flow<List<Language>> {
        return wordDao.flowLanguages().map { it.toApiEntity() }
    }

    override fun addWord(value: String, langId: Long): Completable {
        val currentDate = Date(System.currentTimeMillis())
        return wordDao.addWord(
            WordDb(
                value = value,
                langId = langId,
                addDate = currentDate,
            )
        )
    }

    override fun addWordSuspend(value: String, langId: Long): Long {
        val currentDate = Date(System.currentTimeMillis())
        return wordDao.addWordSuspend(
            WordDb(
                value = value,
                langId = langId,
                addDate = currentDate,
            )
        )
    }

    override fun getWord(id: Long): Single<WordApiEntity> {
        return wordDao
            .getWordById(id)
            .map { value -> value.toApiEntity() }
    }

    override fun getAllWord(): Single<List<WordApiEntity>> {
        return wordDao
            .getWord()
            .map { it.toApiEntity() }
    }

    override fun updateWord(wordApiEntity: WordApiEntity): Completable {
        return wordDao
            .updateWorld(wordApiEntity.toDbEntity())
    }

    // TODO: 07.09.2021 всю эту логику нужно вынести в юзкейсы
    override fun removeWord(id: Long): Completable {
        return wordDao.getWord(id)
            .flatMap { word ->
                wordDao.removeWord(id).toSingle { word.lexemeListDb }
            }
            .flatMap { list ->
                removeDefinition(
                    *list
                        .map { item -> item.lexemeDb }
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
            .flatMapCompletable { item -> wordDao.removeWriteQuiz(item.lexemeDb.id ?: -1) }
    }

    override suspend fun deleteWordSuspend(id: Long): Int {
        wordDao.getWordSuspend(id).also { word ->
            wordDao.removeSampleSuspend(
                *word.lexemeListDb.map { it.sampleDbList }.flatten().toTypedArray()
            )
            wordDao.deleteDefinitionsSuspend(
                *word.lexemeListDb.map { it.lexemeDb }.toTypedArray()
            )
            return wordDao.removeWordSuspend(id)
        }
    }

    override suspend fun updateWordSuspend(id: Long, value: String): Boolean {
        val wordRel = wordDao.getWordSuspend(id)
        val wordDb = wordRel.wordDb.copy(value = value)
        return wordDao.updateWorldSuspend(wordDb) == 1
    }

    override suspend fun addLexemeSuspend(
        wordId: Long,
        category: String,
        definition: String,
    ): Long {
        // TODO: Не забыть добавить WriteQuizDb
        val lexemeDb = LexemeDb(
            wordId = wordId,
            definition = definition,
            wordClass = category,
            options = 0L,
            addDate = Date(System.currentTimeMillis())
        )
        return wordDao.addLexeme(lexemeDb)
    }

    override suspend fun editLexemeSuspend(
        wordId: Long,
        lexemeId: Long,
        category: String,
        definition: String
    ): Int {
        val lexemeDb = LexemeDb(
            id = lexemeId,
            wordId = wordId,
            definition = definition,
            wordClass = category,
            options = 0L,
            addDate = Date(System.currentTimeMillis())
        )
        return wordDao.updateLexeme(lexemeDb)
    }

    private fun removeDefinition(vararg definition: LexemeDb): Completable {
        return wordDao.deleteDefinitions(*definition.toList().toTypedArray())
    }

    @Deprecated(message = "Zhopa", level = DeprecationLevel.ERROR)
    override suspend fun getTermList(langId: Long): List<TermApiEntity> {
//        return wordDao
//            .getTermListRx(langId)
//            .map(TermDbEntity::toApiEntity)
        TODO("Not yet implemented")
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

    override fun addHint(lexemeId: Long, value: String): Completable {
        return wordDao.addHint(
            HintDb(
                lexemeId = lexemeId,
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

    override fun addSample(lexemeId: Long, value: String, source: String?): Completable {
        return wordDao.addSample(
            SampleDb(
                lexemeId = lexemeId,
                value = value,
                source = source,
                addDate = Date(System.currentTimeMillis())
            )
        )
    }

    override fun getSampleList(definitionId: Long): Single<List<SampleApiEntity>> {
//        return wordDao.getSampleListByDefinitionId(definitionId)
//            .map { list -> list.toApiEntity() }
        TODO("Not yet implemented")
    }

    override fun getSampleList(): Observable<List<SampleApiEntity>> {
//        return wordDao.getSampleList()
//            .map { list -> list.toApiEntity() }
        TODO("Not yet implemented")
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
            wordDao.addLexemeRx(def.toDbEntity())
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