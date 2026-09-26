package me.apomazkin.polytrainer.di.module.navigation

import dagger.Module
import dagger.Provides
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.polytrainer.navigation.AppNavigationExecutor
import me.apomazkin.polytrainer.navigation.appNavTable
import javax.inject.Singleton

/**
 * Сборка навигации: ОДИН shared [MateNavigationHandler] на всё
 * приложение (навигация — глобальный ресурс: стек один, очередь
 * переходов одна) поверх таблицы [appNavTable] и живого исполнителя
 * [AppNavigationExecutor]. Инстанс кладётся в effectHandlers каждого
 * экранного Mate.
 */
@Module
class NavigationModule {

    @Provides
    @Singleton
    fun provideNavigationHandler(executor: AppNavigationExecutor): MateNavigationHandler =
        MateNavigationHandler(
            table = appNavTable,
            executor = executor,
            readiness = executor.readiness,
        )
}
