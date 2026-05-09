package me.apomazkin.core_db.di

import android.content.Context
import dagger.Component
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.core_db_impl.di.RoomComponent
import me.apomazkin.logger.LexemeLogger

@Component(
    dependencies = [CoreDbProvider::class]
)
interface CoreDbComponent : CoreDbProvider {

    @Component.Factory
    interface Factory {
        fun create(
            coreDbProvider: CoreDbProvider
        ): CoreDbComponent
    }

    companion object {

        private lateinit var coreDbComponent: CoreDbComponent

        fun init(context: Context, logger: LexemeLogger): CoreDbComponent {
            if (!::coreDbComponent.isInitialized) {
                synchronized(CoreDbComponent::class) {
                    if (!::coreDbComponent.isInitialized) {
                        coreDbComponent = DaggerCoreDbComponent
                            .factory()
                            .create(RoomComponent.get(context, logger))
                    }
                }
            }
            return coreDbComponent
        }

        fun get(context: Context): CoreDbComponent {
            if (::coreDbComponent.isInitialized) return coreDbComponent
            throw IllegalStateException("CoreDbComponent not initialized. Call init(context, logger) first.")
        }
    }

}