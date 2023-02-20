package me.apomazkin.polytrainer.di.module.prefs

import android.content.Context
import dagger.Module
import dagger.Provides
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Singleton

@Module
class PrefsProviderModule {

    @Singleton
    @Provides
    fun providePrefsProvider(context: Context): PrefsProvider = PrefsProvider(context)
}