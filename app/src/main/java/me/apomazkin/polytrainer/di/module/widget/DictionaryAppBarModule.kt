package me.apomazkin.polytrainer.di.module.widget

import dagger.Binds
import dagger.Module
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase

@Module
interface DictionaryAppBarModule {
    
    @Binds
    fun bindDictionaryAppBarUseCaseUseCase(impl: DictionaryAppBarUseCaseImpl): DictionaryAppBarUseCase
}