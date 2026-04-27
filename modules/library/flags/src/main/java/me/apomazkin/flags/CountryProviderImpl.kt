package me.apomazkin.flags

import android.content.Context
import com.blongho.country_data.World
import com.blongho.country_data.exception.CountryDataException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class CountryInfo(
    val numericCode: Int,
    val name: String,
)

interface CountryProvider {
    suspend fun getFlagRes(numericCode: Int): Int
    fun getAllCountries(): List<CountryInfo>
    fun getLanguagesForCountry(numericCode: Int): List<String>
}

class CountryProviderImpl(
    context: Context
) : CountryProvider {

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

    override fun getAllCountries(): List<CountryInfo> {
        return World.getAllCountries().map { CountryInfo(it.id, it.name) }
    }

    override fun getLanguagesForCountry(numericCode: Int): List<String> {
        return World.getLanguagesFrom(numericCode)
    }
}