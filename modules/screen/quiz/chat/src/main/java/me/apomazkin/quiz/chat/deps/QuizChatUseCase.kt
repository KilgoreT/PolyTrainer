package me.apomazkin.quiz.chat.deps

import me.apomazkin.lexeme.ComponentOption
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.QuizConfig
import me.apomazkin.quiz.QuizGroupLabel
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity

/**
 * Шаблоны компонентов, значение которых квиз умеет показать в вопросе.
 * Ядро с другим шаблоном в кандидаты пикера не попадает.
 */
val QUIZ_RENDERABLE_TEMPLATES: Set<ComponentTemplate> = setOf(ComponentTemplate.TEXT)

interface QuizChatUseCase {
    suspend fun getCurrentDictionaryId(): Long?
    suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int

    /**
     * Порция раунда. [coreTypeIds] — включённые ядра: в порцию попадают
     * только лексемы с живым значением хотя бы в одном из них (фильтр в
     * SQL грейдов и добавок). Пустой список → пусто, без запросов.
     */
    suspend fun getRandomWriteQuizList(
        limit: Int,
        maxGrade: Int,
        dictionaryId: Long,
        coreTypeIds: List<Long>,
    ): List<WriteQuiz>

    /**
     * IS481 (AGG-5, F2 fix). Lookup quiz config. null → row отсутствует
     * (F1 invariant нарушение, не crash). Domain `QuizConfig` (AGG-10).
     * Квизом не читается; кандидат на удаление.
     */
    suspend fun getQuizConfig(
        dictionaryId: Long,
        quizMode: String = "write",
    ): QuizConfig?

    /**
     * Ядра словаря, которыми квиз умеет спросить: `core`, включённые, не
     * удалённые, шаблон из [QUIZ_RENDERABLE_TEMPLATES]; по `position`.
     * Пусто — не null.
     */
    suspend fun getQuizCoreTypes(dictionaryId: Long): List<ComponentType>

    /**
     * Сохранённый набор ядер пикера для словаря. Пусто — не сохранено
     * либо все токены битые; вызывающий берёт всех кандидатов.
     */
    suspend fun getQuizPickerSelection(dictionaryId: Long): Set<ComponentTypeRef>

    /** Сохраняет набор ядер пикера через `PrefsProvider` raw-string API. */
    suspend fun setQuizPickerSelection(dictionaryId: Long, refs: Set<ComponentTypeRef>)

    /**
     * Живые опции встроенной «Части речи» словаря — для чипа в вопросе.
     * Типа нет → пусто.
     */
    suspend fun getPartOfSpeechOptions(dictionaryId: Long): List<ComponentOption>

    /**
     * Подпись набора групп, по которому пойдёт квиз (валидированный выбор
     * selection-store): первая по алфавиту и сколько ещё; null — «Все».
     * Групповой фильтр самой выборки применяется внутри
     * [getRandomWriteQuizList] — квиз о нём не знает.
     */
    suspend fun getSelectedQuizGroupLabel(dictionaryId: Long): QuizGroupLabel?
}
