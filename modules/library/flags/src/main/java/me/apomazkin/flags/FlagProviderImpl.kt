package me.apomazkin.flags

import android.content.Context
import com.blongho.country_data.World
import com.blongho.country_data.exception.CountryDataException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface FlagProvider {
    suspend fun getFlagRes(numericCode: Int): Int
}

class FlagProviderImpl(
    context: Context
) : FlagProvider {

    init {
        World.init(context)
    }

    override suspend fun getFlagRes(numericCode: Int): Int = suspendCoroutine { continuation ->
        try {
            val flagRes = World.getFlagOf(numericCode)
            continuation.resume(flagRes)
        } catch (e: Exception) {
            when (e) {
                is CountryDataException -> {
                    continuation.resumeWithException(IllegalStateException(e.message))
                }
                else -> {
                    continuation.resumeWithException(IllegalStateException("Unknown error"))
                }
            }
        }
    }
}