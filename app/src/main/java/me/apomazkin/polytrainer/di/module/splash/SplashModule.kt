package me.apomazkin.polytrainer.di.module.splash

import dagger.Binds
import dagger.Module
import me.apomazkin.splash.SplashUseCase

@Module
interface SplashModule {

    @Binds
    fun bindSplashUseCaseImpl(impl: SplashUseCaseImpl): SplashUseCase
}