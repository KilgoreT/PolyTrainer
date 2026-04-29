package me.apomazkin.flags

import android.content.Context
import com.blongho.country_data.World

data class CountryInfo(
    val numericCode: Int,
    val name: String,
    val alpha2: String,
)

interface CountryProvider {
    fun getFlagRes(numericCode: Int): Int
    fun getAllCountries(): List<CountryInfo>
    fun getLanguagesForCountry(numericCode: Int): List<String>
}

class CountryProviderImpl(
    context: Context
) : CountryProvider {

    init {
        World.init(context)
    }

    override fun getFlagRes(numericCode: Int): Int {
        return World.getFlagOf(numericCode)
    }

    override fun getAllCountries(): List<CountryInfo> {
        return World.getAllCountries().map { CountryInfo(it.id, it.name, it.alpha2) }
    }

    override fun getLanguagesForCountry(numericCode: Int): List<String> {
        return World.getLanguagesFrom(numericCode)
    }
}