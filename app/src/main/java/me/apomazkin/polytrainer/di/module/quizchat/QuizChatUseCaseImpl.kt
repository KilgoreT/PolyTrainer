package me.apomazkin.polytrainer.di.module.quizchat

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentOption
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.QuizConfig
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.polytrainer.di.module.quizgroup.QuizGroupSelectionStore
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.prefs.quizPickerPrefKey
import me.apomazkin.quiz.QuizGroupLabel
import me.apomazkin.quiz.QuizTypes
import me.apomazkin.quiz.quizGroupLabel
import me.apomazkin.quiz.chat.LogTags
import me.apomazkin.quiz.chat.deps.QUIZ_RENDERABLE_TEMPLATES
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.entity.QuizType
import me.apomazkin.quiz.chat.entity.Word
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity
import javax.inject.Inject

class QuizChatUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val quizApi: CoreDbApi.QuizApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val prefsProvider: PrefsProvider,
    private val quizGroupSelectionStore: QuizGroupSelectionStore,
    private val logger: LexemeLogger,
) : QuizChatUseCase {
    
    override suspend fun getCurrentDictionaryId(): Long? {
        prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?.let { id ->
                dictionaryApi.getDictionaryById(id)
                    ?.let { return it.id }
            }
            ?: dictionaryApi.getDictionaryList()
                .firstOrNull()
                ?.let {
                    prefsProvider.setLong(
                        PrefKey.CURRENT_DICTIONARY_ID_LONG,
                        it.id
                    )
                    return it.id
                }

        // IS476: словарь отсутствует — null вместо throw
        return null
    }
    
    override suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int {
        return quizApi.updateWriteQuiz(entity = entity.toApiEntity())
    }
    
    override suspend fun getRandomWriteQuizList(
        limit: Int,
        maxGrade: Int,
        dictionaryId: Long,
        coreTypeIds: List<Long>,
    ): List<WriteQuiz> {
        // Без включённых ядер спрашивать нечем: порция пустая, в БД не ходим
        // (пустой IN () у Room всё равно дал бы ноль строк).
        if (coreTypeIds.isEmpty()) {
            logger.w(tag = LogTags.CHAT, message = "getRandomWriteQuizList: no core types")
            return emptyList()
        }
        // Групповой фильтр резолвится ЗДЕСЬ — в код квиза не зашивается;
        // прецедент внутренних чтений prefs — isEarliestOn ниже. Пустой
        // набор = «Все». Порядок id фиксирован — стабильный лог и запросы.
        val groupIds = quizGroupSelectionStore
            .getValidatedSelection(
                quizType = QuizTypes.CHAT,
                dictionaryId = dictionaryId,
            )
            .sorted()
        val groupFilter = if (groupIds.isEmpty()) "all" else groupIds.joinToString(",")
        logger.d(
            tag = LogTags.CHAT,
            message = "getRandomWriteQuizList: groupFilter=$groupFilter cores=$coreTypeIds",
        )

        val allByGrades: Map<Int, List<WriteQuiz>> = (0..maxGrade)
            .associateWith { grade ->
                val ids = quizApi.getWriteQuizIds(
                    grade = grade,
                    dictionaryId = dictionaryId,
                    groupIds = groupIds,
                    coreTypeIds = coreTypeIds,
                )
                val randomIds = ids.shuffled().take(limit)
                if (randomIds.isEmpty()) return@associateWith emptyList()
                quizApi.getWriteQuizByIds(randomIds)
                    .toDomainEntity(type = QuizType.GRADES)
            }
        val sortedGrades = allByGrades.toSortedMap()

        // IS508: порция ключуется лексемой — один вопрос один раз, это
        // свойство контейнера, а не отдельных фильтров (Д1, Д7).
        val portion = LinkedHashMap<LexemeId, WriteQuiz>()

        // Берёт в порцию до count кандидатов: лексема ещё не в порции;
        // первый проход — только новые слова, второй — остальные (Д4).
        fun pick(candidates: List<WriteQuiz>, count: Int): List<WriteQuiz> {
            val picked = mutableListOf<WriteQuiz>()
            fun tryTake(quiz: WriteQuiz, allowSameWord: Boolean) {
                if (picked.size >= count) return
                if (quiz.lexeme.lexemeId in portion) return
                if (!allowSameWord && portion.values.any { it.word.id == quiz.word.id }) return
                portion[quiz.lexeme.lexemeId] = quiz
                picked += quiz
            }
            candidates.forEach { tryTake(it, allowSameWord = false) }
            candidates.forEach { tryTake(it, allowSameWord = true) }
            return picked
        }

        var remaining = limit
        for ((_, list) in sortedGrades) {
            if (remaining <= 0) break
            val expectedCount = if (portion.isEmpty()) {
                limit / 2
            } else {
                remaining / 2
            }.coerceAtLeast(1)
            remaining -= pick(list, expectedCount).size
        }

        if (portion.size < limit) {
            pick(sortedGrades.values.flatten().shuffled(), limit - portion.size)
        }
        val gradesCount = portion.size

        var earliestAdded = 0
        var earliestCandidates = 0
        val isEarliestOn = prefsProvider.getBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN)
                ?: false
        if (isEarliestOn) {
            val candidates = quizApi
                    .getEarliestWriteQuizList(limit, dictionaryId, groupIds, coreTypeIds)
                    .toDomainEntity(type = QuizType.EARLIEST)
            earliestCandidates = candidates.size
            earliestAdded = pick(candidates.shuffled(), ADDON_SIZE).size
        }
        var errorsAdded = 0
        var errorsCandidates = 0
        val isFrequentMistakesOn = prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN)
                ?: false
        if (isFrequentMistakesOn) {
            val candidates = quizApi
                .getFrequentMistakesWriteQuizList(limit, dictionaryId, groupIds, coreTypeIds)
                .toDomainEntity(type = QuizType.ERRORS)
            errorsCandidates = candidates.size
            errorsAdded = pick(candidates.shuffled(), ADDON_SIZE).size
        }

        // Выключенная опция — «off», чтобы не путать с «включена, кандидатов нет» (+0/0).
        val earliestMark = if (isEarliestOn) "+$earliestAdded/$earliestCandidates" else "off"
        val errorsMark = if (isFrequentMistakesOn) "+$errorsAdded/$errorsCandidates" else "off"
        logger.d(
            tag = LogTags.CHAT,
            message = "getRandomWriteQuizList: portion grades=$gradesCount " +
                "earliest=$earliestMark errors=$errorsMark " +
                "total=${portion.size} words=${portion.values.distinctBy { it.word.id }.size}",
        )
        return portion.values.shuffled()
    }

    override suspend fun getQuizConfig(
        dictionaryId: Long,
        quizMode: String,
    ): QuizConfig? = try {
        lexemeApi.getQuizConfig(dictionaryId, quizMode)?.toDomain()
    } catch (e: Exception) {
        logger.e(tag = LogTags.CHAT, message = "getQuizConfig failed: ${e.message}")
        null
    }

    // ===== quiz picker: ядра =====

    override suspend fun getQuizCoreTypes(dictionaryId: Long): List<ComponentType> {
        // Спросить лексему можно только ядром (word-model §2.6): живым,
        // включённым и с шаблоном, который квиз умеет показать.
        val cores = lexemeApi.getComponentTypes(dictionaryId)
            .map { it.toDomain() }
            .filter { it.core && it.enabled && it.removedAt == null }
            .filter { it.template in QUIZ_RENDERABLE_TEMPLATES }
            .sortedBy { it.position }
        logger.d(
            tag = LogTags.CHAT,
            message = "quizPicker: cores=[" +
                cores.joinToString { it.name ?: it.systemKey?.key.orEmpty() } + "]",
        )
        return cores
    }

    override suspend fun getQuizPickerSelection(dictionaryId: Long): Set<ComponentTypeRef> {
        val raw = prefsProvider.getStringByRawKey(quizPickerPrefKey(dictionaryId)) ?: return emptySet()
        return decodeRefs(raw)
    }

    override suspend fun setQuizPickerSelection(dictionaryId: Long, refs: Set<ComponentTypeRef>) {
        prefsProvider.setStringByRawKey(quizPickerPrefKey(dictionaryId), encodeRefs(refs))
    }

    override suspend fun getPartOfSpeechOptions(dictionaryId: Long): List<ComponentOption> {
        val posType = lexemeApi.getComponentTypes(dictionaryId)
            .firstOrNull { it.systemKey == BuiltInComponent.PART_OF_SPEECH && it.removedAt == null }
            ?: return emptyList()
        return lexemeApi.getComponentOptions(posType.id).map { it.toDomain() }
    }

    // ===== группы тренировки =====

    override suspend fun getSelectedQuizGroupLabel(dictionaryId: Long): QuizGroupLabel? {
        // Один снапшот: набор и отсортированные группы — из одного чтения store.
        val state = quizGroupSelectionStore.getValidatedState(
            quizType = QuizTypes.CHAT,
            dictionaryId = dictionaryId,
        )
        return quizGroupLabel(state.options.groups, state.selectedGroupIds)
    }
}

/** IS508: размер добавки «Самые давние» / «Частые ошибки». */
private const val ADDON_SIZE = 2

private const val PREFIX_BUILTIN = "builtin:"
private const val PREFIX_USER = "user:"

/**
 * Разделитель токенов набора в prefs: unit separator не встречается в
 * именах (с клавиатуры его не ввести), в отличие от запятой и перевода
 * строки. Одиночное значение без разделителя читается как набор из
 * одного — миграция старых prefs не нужна.
 */
private const val REF_SEPARATOR = "\u001F"

private fun encodeRef(ref: ComponentTypeRef): String = when (ref) {
    is ComponentTypeRef.BuiltIn -> "$PREFIX_BUILTIN${ref.key.key}"
    is ComponentTypeRef.UserDefined -> "$PREFIX_USER${ref.name}"
}

private fun encodeRefs(refs: Set<ComponentTypeRef>): String =
    refs.joinToString(REF_SEPARATOR) { encodeRef(it) }

/**
 * Decode `builtin:<key>` / `user:<name>`. Anything else → null.
 *
 * - `builtin:<key>` — unknown key → null (future-proof для новых built-in типов).
 * - `user:<name>` — name восстанавливается через `substringAfter(':')`, корректно
 *   для names с `:`, unicode, любым `String` (включая пустую строку).
 * - Иной prefix (`USER:...`, `garbage`, `user`) → null.
 */
private fun decodeRef(raw: String): ComponentTypeRef? = when {
    raw.startsWith(PREFIX_BUILTIN) -> BuiltInComponent
        .fromKey(raw.substringAfter(':'))
        ?.let { ComponentTypeRef.BuiltIn(it) }
    raw.startsWith(PREFIX_USER) -> ComponentTypeRef.UserDefined(raw.substringAfter(':'))
    else -> null
}

/** Битые токены отбрасываются; пустая строка → пустой набор. */
private fun decodeRefs(raw: String): Set<ComponentTypeRef> =
    raw.split(REF_SEPARATOR)
        .mapNotNull { decodeRef(it) }
        .toSet()

fun WordApiEntity.toDomainEntity() = Word(
    id = id,
    value = value,
)

fun WriteQuizComplexEntity.toDomainEntity(type: QuizType?) = WriteQuiz(
        id = quizData.id,
        dictionaryId = quizData.dictionaryId,
        grade = quizData.grade,
        score = quizData.score,
        errorCount = quizData.errorCount,
        addDate = quizData.addDate,
        lastCorrectAnswerDate = quizData.lastCorrectAnswerDate,
        lexeme = lexemeData.toDomain(),
        word = wordData.toDomainEntity(),
        type = type
)

fun List<WriteQuizComplexEntity>.toDomainEntity(
        type: QuizType?
) = map { it.toDomainEntity(type) }

fun WriteQuizUpsertEntity.toApiEntity() = WriteQuizUpsertApiEntity(
        id = id,
        dictionaryId = dictionaryId,
        lexemeId = lexemeId,
        grade = grade,
        score = score,
        errorCount = errorCount,
        addDate = addDate,
        lastCorrectAnswerDate = lastCorrectAnswerDate
)

fun List<WriteQuizUpsertEntity>.toApiEntity() = map { it.toApiEntity() }