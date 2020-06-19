package me.apomazkin.core_db_impl

import io.reactivex.Observable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.mapper.WordMapper
import me.apomazkin.core_db_impl.room.DefinitionDao
import javax.inject.Inject

class CoreDbApiImpl @Inject constructor(
    private val definitionDao: DefinitionDao
) : CoreDbApi {
    override fun insert(word: String) {
        definitionDao.insertWord(WordDb(word = word))
    }

    override fun getWordList(): Observable<List<Word>> {
        return definitionDao
            .getWordList()
            .map { list -> list.map { item -> WordMapper.map(item) } }
    }
}