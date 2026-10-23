package me.apomazkin.polytrainer.di.module.scancandidates

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.CoreNetworkApi
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LookupOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.RecognitionOutcome
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.polytrainer.di.module.candidates.CandidatesFormLoader
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.polytrainer.mapper.lookupRequestOf
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.scancandidates.deps.ScanCandidatesUseCase
import me.apomazkin.textrecognition.TextRecognizer
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

/** Потолок запроса: сервер принимает не больше 2000 токенов (WFT-28, WFP-15). */
private const val MAX_TOKENS = 2000

/** Номер запуска: у нового он больше любого прежнего в этом процессе (WFP-01). */
private val lastRunId = AtomicLong(0)

/**
 * Реализация порта экрана лексем-кандидатов со снимка: распознавание ML Kit,
 * запрос к серверу слов с деревом компонентов текущего словаря, форма словаря
 * из Room, запись пакета одной транзакцией. Правил не держит: случаи уходят
 * исходом, разбирает их хендлер экрана.
 */
class ScanCandidatesUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val wordsBatchApi: CoreDbApi.WordsBatchApi,
    private val currentDictionaryProvider: CurrentDictionaryProvider,
    private val textRecognizer: TextRecognizer,
    private val wordsApi: CoreNetworkApi.WordsApi,
    private val componentTree: ComponentTreeLoader,
    private val formLoader: CandidatesFormLoader,
    private val appContext: Context,
) : ScanCandidatesUseCase {

    // WFP Ш4–Ш5: токены как распознаны, с повторами и в исходном регистре; пустые
    // прочь до обрезки, затем первые 2000 (WFP-25, WFP-15). Снимок камеры (наш
    // FileProvider) после распознавания не нужен — удаляется в любом исходе
    // (WFP-08); картинка из галереи не наша, её не трогаем (WFP-09).
    override suspend fun recognize(uri: String): RecognitionOutcome {
        val image = Uri.parse(uri)
        return try {
            val tokens = textRecognizer
                .recognize(image)
                .words
                .map { it.toDomain() }
                .filter { it.text.isNotBlank() }
                .take(MAX_TOKENS)
            if (tokens.isEmpty()) RecognitionOutcome.Empty else RecognitionOutcome.Text(tokens)
        } finally {
            if (image.isCameraFile()) {
                runCatching { appContext.contentResolver.delete(image, null, null) }
            }
        }
    }

    // Доступ к картинке галереи закрепила вкладка при выборе (Д52). Отпускается
    // один раз; повторный вызов и чужой файл — не ошибка, исключения наружу не
    // идут. У файла камеры закреплять нечего.
    override suspend fun releaseImageAccess(uri: String) {
        runCatching {
            val image = Uri.parse(uri)
            if (!image.isCameraFile()) {
                appContext.contentResolver.releasePersistableUriPermission(
                    image,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
    }

    // WFP Ш6: один запрос с языками и деревом компонентов текущего словаря
    // (WFT-78). Исключений от сети нет — слой отдаёт исход. Без словаря сюда не
    // попасть (кнопка скрыта), поэтому его отсутствие — ошибка программы.
    override suspend fun lookupWords(tokens: List<LookupToken>): LookupOutcome {
        val dictionary = currentDictionary()
        val components = componentTree.treeOf(dictionary.id)
        val request = lookupRequestOf(dictionary, tokens, components)
        return wordsApi
            .lookup(request, lastRunId.incrementAndGet())
            .toDomain(dictionaryId = dictionary.id)
    }

    // AWP Ш1: словаря нет — исход «словарь удалён» (AWP-24.4); чтение упало —
    // исход «не собралось» (AWP-24.1); отмена пролетает.
    override suspend fun loadForm(
        dictionaryId: Long,
        lemmaTexts: List<String>,
    ): FormOutcome {
        val dictionary = dictionaryApi.getDictionaryById(dictionaryId)
            ?: return FormOutcome.DictionaryMissing
        return try {
            FormOutcome.Loaded(formLoader.formOf(dictionary, lemmaTexts))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            FormOutcome.Failed(cause = "${failure.javaClass.simpleName}: ${failure.message}")
        }
    }

    // AWP Ш4: исключение записи пролетает к эффекту — это «не добавилось» (AWP-21).
    override suspend fun addWords(batch: WordsBatch): AddWordsBatchOutcome =
        wordsBatchApi.addWordsBatch(batch)

    private suspend fun currentDictionary(): DictionaryApiEntity =
        currentDictionaryProvider.flowCurrentDict().first()
            ?: throw IllegalStateException("Dictionary not found")

    /** Authority нашего FileProvider: по ней видно, что файл снимка — наш, из кеша. */
    private val cameraAuthority: String
        get() = appContext.packageName + FILE_PROVIDER_SUFFIX

    private fun Uri.isCameraFile(): Boolean = authority == cameraAuthority
}
