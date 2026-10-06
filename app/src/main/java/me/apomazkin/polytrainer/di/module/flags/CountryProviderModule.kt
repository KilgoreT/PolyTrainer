package me.apomazkin.polytrainer.di.module.flags

import android.content.Context
import dagger.Module
import dagger.Provides
import me.apomazkin.flags.CountryProvider
import me.apomazkin.flags.CountryProviderImpl
import me.apomazkin.polytrainer.di.module.dictionary.DictionaryLanguageRules
import javax.inject.Singleton

@Module
class CountryProviderModule {

    @Singleton
    @Provides
    fun provideCountryProvider(context: Context): CountryProvider =
        CountryProviderImpl(context)

    /** IS525: те же правила языков, что получила миграция, — для юзкейса словаря. */
    @Provides
    fun provideDictionaryLanguageRules(countryProvider: CountryProvider): DictionaryLanguageRules =
        DictionaryLanguageRules(countryProvider = lazyOf(countryProvider))
}