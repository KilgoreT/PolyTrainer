package me.apomazkin.polytrainer.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.dictionary.form.DictionaryFormViewModel
import me.apomazkin.dictionary.list.DictionaryListViewModel
import me.apomazkin.dictionaryappbar.DictionaryAppBarViewModel
import me.apomazkin.dictionarytab.ui.DictionaryTabViewModel
import me.apomazkin.quiztab.QuizTabViewModel
import me.apomazkin.settingstab.SettingsTabViewModel
import me.apomazkin.stattab.StatisticViewModel
import me.apomazkin.quiz.chat.ChatViewModel
import me.apomazkin.polytrainer.MainActivity
import me.apomazkin.polytrainer.di.module.EnvModule
import me.apomazkin.polytrainer.di.module.ResourceModule
import me.apomazkin.polytrainer.di.module.dictionary.DictionaryModule
import me.apomazkin.polytrainer.di.module.dictionarytab.DictionaryTabModule
import me.apomazkin.polytrainer.di.module.flags.CountryProviderModule
import me.apomazkin.polytrainer.di.module.prefs.PrefsProviderModule
import me.apomazkin.polytrainer.di.module.quizchat.QuizChatModule
import me.apomazkin.polytrainer.di.module.quiztab.QuizTabModule
import me.apomazkin.polytrainer.di.module.settingstab.SettingsModule
import me.apomazkin.polytrainer.di.module.splash.SplashModule
import me.apomazkin.polytrainer.di.module.statistictab.StatisticModule
import me.apomazkin.polytrainer.di.module.widget.DictionaryAppBarModule
import me.apomazkin.polytrainer.di.module.wordCard.WordCardModule
import me.apomazkin.polytrainer.env.EnvParams
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.splash.SplashUseCase
import me.apomazkin.splash.SplashViewModel
import me.apomazkin.wordcard.WordCardViewModel
import javax.inject.Singleton

@Component(
    modules = [AppModule::class],
    dependencies = [CoreDbProvider::class]
)
@Singleton
interface AppComponent {
    
    fun inject(mainActivity: MainActivity)
    
    @Component.Factory
    interface AppComponentFactory {
        fun create(
            @BindsInstance appContext: Context,
            @BindsInstance logger: LexemeLogger,
            coreDbProvider: CoreDbProvider,
        ): AppComponent
    }
    
    fun getSplashUseCase(): SplashUseCase
    fun getSplashViewModelFactory(): SplashViewModel.Factory
    fun getDictionaryUseCase(): DictionaryUseCase
    fun getDictionaryFormViewModelFactory(): DictionaryFormViewModel.Factory
    fun getDictionaryListViewModelFactory(): DictionaryListViewModel.Factory
    fun getWordCardViewModelFactory(): WordCardViewModel.Factory
    fun getChatViewModelFactory(): ChatViewModel.Factory
    fun getDictionaryAppBarViewModelFactory(): DictionaryAppBarViewModel.Factory
    fun getDictionaryTabViewModelFactory(): DictionaryTabViewModel.Factory
    fun getQuizTabViewModelFactory(): QuizTabViewModel.Factory
    fun getStatisticViewModelFactory(): StatisticViewModel.Factory
    fun getSettingsTabViewModelFactory(): SettingsTabViewModel.Factory
    fun getEnvParams(): EnvParams
    fun getLogger(): LexemeLogger

    @Component(dependencies = [CoreDbProvider::class])
    interface CoreDbDependenciesComponent : CoreDbProvider
}

@Module(
    includes = [
        SplashModule::class,
        DictionaryModule::class,
        DictionaryTabModule::class,
        WordCardModule::class,
        QuizTabModule::class,
        QuizChatModule::class,
        StatisticModule::class,
        SettingsModule::class,
        DictionaryAppBarModule::class,
        CountryProviderModule::class,
        PrefsProviderModule::class,
        ResourceModule::class,
        EnvModule::class,
    ]
)
interface AppModule