package me.apomazkin.main.widget.top

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.apomazkin.main.entity.LangUiEntity

interface MainTopBarUseCase {
    fun getCurrentLang(): Flow<Int>
    fun getAvailableLang(): Flow<List<LangUiEntity>>
    suspend fun changeLang(numericCode: Int)
}

class PreviewMainTopBarUseCase : MainTopBarUseCase {
    override fun getCurrentLang(): Flow<Int> = flow {
        emit(1)
    }

    override fun getAvailableLang(): Flow<List<LangUiEntity>> = flow {
        emit(
            emptyList()
        )
    }

    override suspend fun changeLang(numericCode: Int) {}

}