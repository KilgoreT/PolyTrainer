package me.apomazkin.polytrainer.di.module.createDictionary

import dagger.Binds
import dagger.Module
import me.apomazkin.createdictionary.CreateDictionaryUseCase

@Module
interface CreateDictionaryModule {
    @Binds
    fun bindCreateDictionaryUseCase(impl: CreateDictionaryUseCaseImpl): CreateDictionaryUseCase
}