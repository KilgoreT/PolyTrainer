package me.apomazkin.quiztab.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity

interface QuizTabUseCase {
    suspend fun getCurrentDict(): DictUiEntity
    suspend fun getAvailableDict(): List<DictUiEntity>
    fun flowAvailableDict(): Flow<List<DictUiEntity>>
    suspend fun changeDict(numericCode: Int)
}