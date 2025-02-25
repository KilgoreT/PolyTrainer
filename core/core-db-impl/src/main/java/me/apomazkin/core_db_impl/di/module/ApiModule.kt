package me.apomazkin.core_db_impl.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_impl.CoreDbApiImpl

@Module
interface ApiModule {

    @Binds
    fun provideApi(impl: CoreDbApiImpl): CoreDbApi
    
    @Binds
    fun provideLangApi(impl: CoreDbApiImpl.LangApiImpl): CoreDbApi.LangApi

    @Binds
    fun provideTermApi(impl: CoreDbApiImpl.TermApiImpl): CoreDbApi.TermApi

    @Binds
    fun provideLexemeApi(impl: CoreDbApiImpl.LexemeApiImpl): CoreDbApi.LexemeApi
    
    @Binds
    fun provideQuizApi(impl: CoreDbApiImpl.QuizApiImpl): CoreDbApi.QuizApi
}