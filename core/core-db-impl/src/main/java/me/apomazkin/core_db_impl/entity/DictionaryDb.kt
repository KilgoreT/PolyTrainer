package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import java.util.Date

@Entity(
        tableName = "dictionaries",
)
data class DictionaryDb(
        // TODO: сделать ненулабельным
        //  https://github.com/KilgoreT/PolyTrainer/issues/375
        @PrimaryKey(autoGenerate = true)
        val id: Long? = null,
        val numericCode: Int? = null,
        val name: String = "",
        val addDate: Date,
        val changeDate: Date? = null,
        // IS525: языки словаря — код с региональным вариантом («es-MX», «ru»),
        // обязательные. DEFAULT 'en' в схеме — страховка SQLite (NOT NULL
        // без DEFAULT через ALTER TABLE не добавить) и зеркало правила «нет
        // флага — английский»; значение по умолчанию в Kotlin — для тестовых
        // конструкций, код приложения пишет языки явно (DictionaryApi).
        @ColumnInfo(name = "learning_language", defaultValue = "en")
        val learningLanguage: String = "en",
        @ColumnInfo(name = "translation_language", defaultValue = "en")
        val translationLanguage: String = "en",
)

fun DictionaryDb.toApiEntity() = DictionaryApiEntity(
        id = id ?: throw IllegalArgumentException("DictionaryDb id is null"),
        numericCode = numericCode,
        name = name,
        addDate = addDate,
        changeDate = changeDate,
        learningLanguage = learningLanguage,
        translationLanguage = translationLanguage,
)

fun List<DictionaryDb>.toApiEntity() = map { it.toApiEntity() }
