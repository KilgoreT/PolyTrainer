package me.apomazkin.polytrainer.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.langpicker.LangPickerUseCase
import me.apomazkin.main.widget.top.MainTopBarUseCase
import me.apomazkin.polytrainer.MainActivity
import me.apomazkin.polytrainer.api.AppProvider
import me.apomazkin.polytrainer.di.module.flags.FlagProviderModule
import me.apomazkin.polytrainer.di.module.langPicker.LangPickerModule
import me.apomazkin.polytrainer.di.module.main.MainModule
import me.apomazkin.polytrainer.di.module.prefs.PrefsProviderModule
import me.apomazkin.polytrainer.di.module.splash.SplashModule
import me.apomazkin.splash.SplashUseCase
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
    fun getLangPickerUseCase(): LangPickerUseCase
    fun getMainUseCase(): MainTopBarUseCase

    @Component(dependencies = [CoreDbProvider::class])
    interface CoreDbDependenciesComponent : CoreDbProvider
}

@Module(
    includes = [
        SplashModule::class,
        LangPickerModule::class,
        MainModule::class,
        FlagProviderModule::class,
        PrefsProviderModule::class,
    ]
)
interface AppModule