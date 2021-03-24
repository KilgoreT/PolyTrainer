package me.apomazkin.core_interactor.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import me.apomazkin.core_db.di.CoreDbComponent
import javax.inject.Singleton

@Module
class RepositoryModule {

    @Singleton
    @Provides
    fun provideCoreDbApi(context: Context) = CoreDbComponent.get(context).getCoreDbApi()
}