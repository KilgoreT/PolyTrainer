package me.apomazkin.polytrainer.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.createdictionary.CreateDictionaryUseCase
import me.apomazkin.polytrainer.MainActivity
import me.apomazkin.polytrainer.api.AppProvider
import me.apomazkin.polytrainer.di.module.LoggerModule
import me.apomazkin.polytrainer.di.module.createDictionary.CreateDictionaryModule
import me.apomazkin.polytrainer.di.module.flags.FlagProviderModule
import me.apomazkin.polytrainer.di.module.prefs.PrefsProviderModule
import me.apomazkin.polytrainer.di.module.splash.SplashModule
import me.apomazkin.polytrainer.di.module.vocabulary.VocabularyModule
import me.apomazkin.polytrainer.di.module.wordCard.WordCardModule
import me.apomazkin.splash.SplashUseCase
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.vocabulary.deps.VocabularyUseCase
import me.apomazkin.wordcard.deps.WordCardUseCase
import javax.inject.Singleton

@Component(
    modules = [AppModule::class],
    dependencies = [CoreDbProvider::class]
)
@Singleton
interface AppComponent : AppProvider {

    fun inject(mainActivity: MainActivity)

    @Component.Factory
    interface AppComponentFactory {
        fun create(
            @BindsInstance appContext: Context,
            coreDbProvider: CoreDbProvider,
        ): AppComponent
    }

    fun getSplashUseCase(): SplashUseCase
    fun getCreateDictionaryUseCase(): CreateDictionaryUseCase
    fun getVocabularyUseCase(): VocabularyUseCase
    fun getWordCardUseCase(): WordCardUseCase
    fun getLogger(): LexemeLogger

    @Component(dependencies = [CoreDbProvider::class])
    interface CoreDbDependenciesComponent : CoreDbProvider
}

@Module(
    includes = [
        SplashModule::class,
        CreateDictionaryModule::class,
        VocabularyModule::class,
        WordCardModule::class,
        FlagProviderModule::class,
        PrefsProviderModule::class,
        LoggerModule::class,
    ]
)
interface AppModule