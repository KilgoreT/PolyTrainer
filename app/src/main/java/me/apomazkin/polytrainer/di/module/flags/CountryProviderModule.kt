package me.apomazkin.polytrainer.di.module.flags

import android.content.Context
import dagger.Module
import dagger.Provides
import me.apomazkin.flags.CountryProvider
import me.apomazkin.flags.CountryProviderImpl
import javax.inject.Singleton

@Module
class CountryProviderModule {

    @Singleton
    @Provides
    fun provideCountryProvider(context: Context): CountryProvider =
        CountryProviderImpl(context)
}