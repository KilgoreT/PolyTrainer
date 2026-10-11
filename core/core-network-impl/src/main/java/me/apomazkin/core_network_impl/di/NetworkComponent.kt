package me.apomazkin.core_network_impl.di

import dagger.BindsInstance
import dagger.Component
import me.apomazkin.core_network_api.CoreNetworkProvider
import me.apomazkin.core_network_impl.NetworkOptions
import me.apomazkin.core_network_impl.di.module.ApiModule
import me.apomazkin.core_network_impl.di.module.NetworkModule
import me.apomazkin.logger.LexemeLogger
import javax.inject.Singleton

/** Компонент реализации, как `RoomComponent` у базы. Один на процесс. */
@Singleton
@Component(
        modules = [NetworkModule::class, ApiModule::class]
)
interface NetworkComponent : CoreNetworkProvider {

    @Component.Factory
    interface NetworkComponentFactory {
        fun create(
                @BindsInstance options: NetworkOptions,
                @BindsInstance logger: LexemeLogger,
        ): NetworkComponent
    }

    companion object {

        private lateinit var networkComponent: NetworkComponent

        fun get(options: NetworkOptions, logger: LexemeLogger): NetworkComponent {
            if (!::networkComponent.isInitialized) {
                synchronized(NetworkComponent::class) {
                    if (!::networkComponent.isInitialized) {
                        networkComponent = DaggerNetworkComponent
                            .factory()
                            .create(options, logger)
                    }
                }
            }
            return networkComponent
        }
    }
}
