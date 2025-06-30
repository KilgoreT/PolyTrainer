package me.apomazkin.polytrainer.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.createdictionary.CreateDictionaryUseCase
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.polytrainer.MainActivity
import me.apomazkin.polytrainer.api.AppProvider
import me.apomazkin.polytrainer.di.module.EnvModule
import me.apomazkin.polytrainer.di.module.LoggerModule
import me.apomazkin.polytrainer.di.module.ResourceModule
import me.apomazkin.polytrainer.di.module.createDictionary.CreateDictionaryModule
import me.apomazkin.polytrainer.di.module.dictionarytab.DictionaryTabModule
import me.apomazkin.polytrainer.di.module.flags.FlagProviderModule
import me.apomazkin.polytrainer.di.module.prefs.PrefsProviderModule
import me.apomazkin.polytrainer.di.module.quizchat.QuizChatModule
import me.apomazkin.polytrainer.di.module.quiztab.QuizTabModule
import me.apomazkin.polytrainer.di.module.settingstab.SettingsModule
import me.apomazkin.polytrainer.di.module.splash.SplashModule
import me.apomazkin.polytrainer.di.module.statistictab.StatisticModule
import me.apomazkin.polytrainer.di.module.widget.DictionaryAppBarModule
import me.apomazkin.polytrainer.di.module.wordCard.WordCardModule
import me.apomazkin.polytrainer.env.EnvParams
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiztab.deps.QuizTabUseCase
import me.apomazkin.settingstab.deps.SettingsTabUseCase
import me.apomazkin.splash.SplashUseCase
import me.apomazkin.stattab.deps.StatisticUseCase
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.resource.ResourceManager
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
    fun getVocabularyUseCase(): DictionaryTabUseCase
    fun getWordCardUseCase(): WordCardUseCase
    fun getQuizTabUseCase(): QuizTabUseCase
    fun getQuizChatUseCase(): QuizChatUseCase
    fun getStatisticUseCase(): StatisticUseCase
    fun getSettingsTabUseCase(): SettingsTabUseCase
    fun getDictionaryAppBarUseCase(): DictionaryAppBarUseCase
    fun getResourceManager(): ResourceManager
    fun getPrefsProvider(): PrefsProvider
    fun getEnvParams(): EnvParams
    fun getLogger(): LexemeLogger
    
    @Component(dependencies = [CoreDbProvider::class])
    interface CoreDbDependenciesComponent : CoreDbProvider
}

@Module(
    includes = [
        SplashModule::class,
        CreateDictionaryModule::class,
        DictionaryTabModule::class,
        WordCardModule::class,
        QuizTabModule::class,
        QuizChatModule::class,
        StatisticModule::class,
        SettingsModule::class,
        DictionaryAppBarModule::class,
        FlagProviderModule::class,
        PrefsProviderModule::class,
        LoggerModule::class,
        ResourceModule::class,
        EnvModule::class,
    ]
)
interface AppModule