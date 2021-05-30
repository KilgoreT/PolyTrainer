package me.apomazkin.core_db_impl.room.utils

import android.content.ContentValues
import me.apomazkin.core_db_impl.entity.DefinitionDb
import me.apomazkin.core_db_impl.entity.HintDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb

/**
 * Extensions to transform database entities to ContentValue list.
 */

@JvmName("asContentValueWordDb")
fun List<WordDb>.asContentValue() = this.map { word ->
    ContentValues().apply {
        put("id", word.id)
        put("word", word.word)
    }
}

@JvmName("asContentValueDefinitionDb")
fun List<DefinitionDb>.asContentValue() = this.map { definition ->
    ContentValues().apply {
        put("id", definition.id)
        put("wordId", definition.wordClass)
        put("definition", definition.definition)
        put("wordClass", definition.wordClass)
        put("options", definition.options)
    }
}

@JvmName("asContentValueHintDb")
fun List<HintDb>.asContentValue() = this.map { hint ->
    ContentValues().apply {
        put("id", hint.id)
        put("definitionId", hint.definitionId)
        put("value", hint.value)
        put("addDate", hint.addDate.time)
        hint.changeDate?.let {
            put("changeDate", it.time)
        } ?: putNull("changeDate")
    }
}

@JvmName("asContentValueWriteQuizDb")
fun List<WriteQuizDb>.asContentValue() = this.map { writeQuizDb ->
    ContentValues().apply {
        put("id", writeQuizDb.id)
        put("definitionId", writeQuizDb.definitionId)
        put("grade", writeQuizDb.grade)
        put("score", writeQuizDb.score)
    }
}