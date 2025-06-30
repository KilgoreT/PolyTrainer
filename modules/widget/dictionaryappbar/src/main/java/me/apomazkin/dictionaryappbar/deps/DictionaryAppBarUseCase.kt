package me.apomazkin.dictionaryappbar.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity

interface DictionaryAppBarUseCase {
    fun flowAvailableDict(): Flow<List<DictUiEntity>>
    fun flowCurrentDict(): Flow<DictUiEntity>
    suspend fun changeDict(numericCode: Int)
}