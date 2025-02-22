package me.apomazkin.polytrainer.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.polytrainer.resource.ResourceManagerImpl
import me.apomazkin.ui.resource.ResourceManager

@Module
interface ResourceModule {
    @Binds
    fun bindResourceManager(impl: ResourceManagerImpl): ResourceManager
}