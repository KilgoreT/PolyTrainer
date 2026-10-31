package me.apomazkin.core_network.di

import dagger.Component
import me.apomazkin.core_network_api.CoreNetworkProvider
import me.apomazkin.core_network_impl.NetworkOptions
import me.apomazkin.core_network_impl.di.NetworkComponent
import me.apomazkin.logger.LexemeLogger

/**
 * Фасад слоя сети, как `CoreDbComponent` у базы: `App` поднимает его один
 * раз и отдаёт в `AppComponent` как [CoreNetworkProvider].
 */
@Component(
        dependencies = [CoreNetworkProvider::class]
)
interface CoreNetworkComponent : CoreNetworkProvider {

    @Component.Factory
    interface Factory {
        fun create(
                coreNetworkProvider: CoreNetworkProvider,
        ): CoreNetworkComponent
    }

    companion object {

        private lateinit var coreNetworkComponent: CoreNetworkComponent

        /**
         * [baseUrl] — адрес сервера слов (`BuildConfig.LEXEME_BASE_URL`),
         * [accessKey] — ключ доступа (`BuildConfig.LEXEME_ACCESS_KEY`, пустой —
         * запросы не уходят, ACC-19). Только HTTPS (LFL-21).
         */
        fun init(
                baseUrl: String,
                accessKey: String,
                logger: LexemeLogger,
        ): CoreNetworkComponent {
            if (!::coreNetworkComponent.isInitialized) {
                synchronized(CoreNetworkComponent::class) {
                    if (!::coreNetworkComponent.isInitialized) {
                        val networkComponent = NetworkComponent.get(
                                options = NetworkOptions(
                                        baseUrl = baseUrl.withTrailingSlash(),
                                        accessKey = accessKey,
                                ),
                                logger = logger,
                        )
                        coreNetworkComponent = DaggerCoreNetworkComponent
                            .factory()
                            .create(networkComponent)
                    }
                }
            }
            return coreNetworkComponent
        }

        /** Retrofit требует базовый адрес со слэшем на конце. */
        private fun String.withTrailingSlash(): String =
            if (endsWith("/")) this else "$this/"
    }
}
