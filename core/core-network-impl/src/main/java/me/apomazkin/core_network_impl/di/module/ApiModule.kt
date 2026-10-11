package me.apomazkin.core_network_impl.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.core_network_api.CoreNetworkApi
import me.apomazkin.core_network_impl.WordsApiImpl
import javax.inject.Singleton

@Module
internal abstract class ApiModule {

    @Binds
    @Singleton
    abstract fun bindWordsApi(impl: WordsApiImpl): CoreNetworkApi.WordsApi
}
