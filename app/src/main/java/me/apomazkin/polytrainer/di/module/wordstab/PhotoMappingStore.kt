package me.apomazkin.polytrainer.di.module.wordstab

import me.apomazkin.candidates.PhotoDataKind
import me.apomazkin.candidates.PhotoTarget
import me.apomazkin.candidates.PhotoMapping
import me.apomazkin.candidates.resolveMapping
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.prefs.photoWordsMappingPrefKey
import me.apomazkin.wordstab.LogTags
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IS532: соответствия окна добавления по словарю (AWP-06) — pref
 * `photo_words_mapping_dict_<id>`, прецедент `QuizGroupSelectionStore`.
 * Хранится id компонента на вид данных, «не добавлять» — явное `none`.
 * Действительность компонентов проверяет окно по форме словаря
 * (`resolveMapping`): здесь только разбор и запись. Группа не хранится
 * (AWP-06.8).
 */
@Singleton
class PhotoMappingStore @Inject constructor(
    private val prefsProvider: PrefsProvider,
    private val logger: LexemeLogger,
) {

    /** Запомненные соответствия или null — ключа нет либо он не разбирается. */
    suspend fun read(dictionaryId: Long): PhotoMapping? {
        val raw = prefsProvider.getStringByRawKey(photoWordsMappingPrefKey(dictionaryId))
        val mapping = raw?.let { decode(it) }
        logger.d(
            tag = LogTags.VOCAB,
            message = "photoMappingStore: read dict=$dictionaryId raw=${raw ?: "none"} parsed=${mapping != null}",
        )
        return mapping
    }

    suspend fun write(dictionaryId: Long, mapping: PhotoMapping) {
        val encoded = encode(mapping)
        prefsProvider.setStringByRawKey(key = photoWordsMappingPrefKey(dictionaryId), value = encoded)
        logger.d(tag = LogTags.VOCAB, message = "photoMappingStore: write dict=$dictionaryId value=$encoded")
    }

    companion object {
        private const val NONE = "none"

        /** `translation=12,gloss=none,examples=18,partOfSpeech=15` — виды в порядке [PhotoDataKind]. */
        fun encode(mapping: PhotoMapping): String =
            PhotoDataKind.entries.joinToString(",") { kind -> "${kind.key}=${mapping.targetOf(kind).encoded()}" }

        /** Все четыре вида обязательны; лишние и неразборные пары — null целиком. */
        fun decode(raw: String): PhotoMapping? {
            val pairs = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val targets = mutableMapOf<PhotoDataKind, PhotoTarget>()
            for (pair in pairs) {
                val (name, value) = pair.split("=", limit = 2).takeIf { it.size == 2 } ?: return null
                val kind = PhotoDataKind.entries.firstOrNull { it.key == name } ?: return null
                targets[kind] = decodeTarget(value) ?: return null
            }
            if (targets.size != PhotoDataKind.entries.size) return null
            return PhotoMapping(
                translation = targets.getValue(PhotoDataKind.TRANSLATION),
                gloss = targets.getValue(PhotoDataKind.GLOSS),
                examples = targets.getValue(PhotoDataKind.EXAMPLES),
                partOfSpeech = targets.getValue(PhotoDataKind.PART_OF_SPEECH),
            )
        }

        private fun decodeTarget(value: String): PhotoTarget? = when {
            value == NONE -> PhotoTarget.None
            else -> value.toLongOrNull()?.let { PhotoTarget.Component(it) }
        }

        private fun PhotoTarget.encoded(): String = when (this) {
            is PhotoTarget.Component -> componentId.toString()
            PhotoTarget.None -> NONE
        }

        private val PhotoDataKind.key: String
            get() = when (this) {
                PhotoDataKind.TRANSLATION -> "translation"
                PhotoDataKind.GLOSS -> "gloss"
                PhotoDataKind.EXAMPLES -> "examples"
                PhotoDataKind.PART_OF_SPEECH -> "partOfSpeech"
            }
    }
}
