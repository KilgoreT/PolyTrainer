package me.apomazkin.polytrainer.di.module.flags

import android.content.Context
import dagger.Module
import dagger.Provides
import me.apomazkin.flags.FlagProvider
import me.apomazkin.flags.FlagProviderImpl

@Module
class FlagProviderModule {

    @Provides
    fun provideFlagProvider(context: Context): FlagProvider =
        FlagProviderImpl(context)
}