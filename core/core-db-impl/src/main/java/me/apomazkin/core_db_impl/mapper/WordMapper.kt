package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_impl.entity.WordDb

class WordMapper {

    companion object {

        fun map(value: WordDb) = Word(
            value.id,
            value.word
        )

        fun reverseMap(value: Word) = WordDb(
            value.id,
            value.word
        )
    }

}