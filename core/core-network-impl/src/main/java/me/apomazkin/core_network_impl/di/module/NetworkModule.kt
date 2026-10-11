package me.apomazkin.core_network_impl.di.module

import dagger.Module
import dagger.Provides
import me.apomazkin.core_network_impl.LexemeService
import me.apomazkin.core_network_impl.LexemeServiceFactory
import me.apomazkin.core_network_impl.NetworkOptions
import me.apomazkin.logger.LexemeLogger
import javax.inject.Singleton

@Module
internal class NetworkModule {

    @Provides
    @Singleton
    fun provideLexemeService(options: NetworkOptions, logger: LexemeLogger): LexemeService =
        LexemeServiceFactory.create(options, logger)
}
