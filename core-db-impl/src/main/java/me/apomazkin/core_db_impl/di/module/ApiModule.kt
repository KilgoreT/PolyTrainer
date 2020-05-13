package me.apomazkin.core_db_impl.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_impl.CoreDbApiImpl

@Module
interface ApiModule {

    @Binds
    fun provideApi(impl: CoreDbApiImpl): CoreDbApi

}