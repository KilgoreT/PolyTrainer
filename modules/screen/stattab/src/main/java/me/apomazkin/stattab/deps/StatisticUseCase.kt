package me.apomazkin.stattab.deps

import kotlinx.coroutines.flow.Flow

interface StatisticUseCase {
    suspend fun flowWordCount(): Flow<Int>
    suspend fun flowLexemeCount(): Flow<Int>
}