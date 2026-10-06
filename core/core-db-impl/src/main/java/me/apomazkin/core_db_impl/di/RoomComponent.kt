package me.apomazkin.core_db_impl.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.core_db_api.entity.DictionaryLanguageDefaults
import me.apomazkin.core_db_api.entity.ReservedGroupNames
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
            // IS493 Э3 (D12.3): резерв имён групп («Все» всех локалей) —
            // собирает app из ресурсов, конструкторная инъекция в GroupApiImpl.
            @BindsInstance reservedGroupNames: ReservedGroupNames,
            // IS525: правила языков словаря по умолчанию — для миграции 15→16.
            @BindsInstance dictionaryLanguageDefaults: DictionaryLanguageDefaults,
        ): RoomComponent
    }

    companion object {

        lateinit var roomComponent: RoomComponent

        fun get(
            context: Context,
            logger: LexemeLogger,
            reservedGroupNames: ReservedGroupNames,
            dictionaryLanguageDefaults: DictionaryLanguageDefaults,
        ): RoomComponent {
            if (!::roomComponent.isInitialized) {
                synchronized(RoomComponent::class) {
                    if (!::roomComponent.isInitialized) {
                        roomComponent = DaggerRoomComponent
                            .factory()
                            .create(context, logger, reservedGroupNames, dictionaryLanguageDefaults)
                    }
                }
            }
            return roomComponent
        }
    }

}