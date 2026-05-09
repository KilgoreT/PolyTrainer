package me.apomazkin.core_db_impl.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.core_db_impl.di.module.ApiModule
import me.apomazkin.core_db_impl.di.module.RoomModule
import me.apomazkin.logger.LexemeLogger
import javax.inject.Singleton

@Singleton
@Component(
    modules = [RoomModule::class, ApiModule::class]
)
interface RoomComponent : CoreDbProvider {

    @Component.Factory
    interface RoomComponentFactory {
        fun create(
            @BindsInstance context: Context,
            @BindsInstance logger: LexemeLogger,
        ): RoomComponent
    }

    companion object {

        lateinit var roomComponent: RoomComponent

        fun get(context: Context, logger: LexemeLogger): RoomComponent {
            if (!::roomComponent.isInitialized) {
                synchronized(RoomComponent::class) {
                    if (!::roomComponent.isInitialized) {
                        roomComponent = DaggerRoomComponent
                            .factory()
                            .create(context, logger)
                    }
                }
            }
            return roomComponent
        }
    }

}