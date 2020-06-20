package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_impl.entity.WordDb

class WordMapper : Mapper<WordDb, Word>() {

    override fun map(value: WordDb) = Word(
        value.id,
        value.word
    )

    override fun reverseMap(value: Word) = WordDb(
        value.id,
        value.word
    )

}