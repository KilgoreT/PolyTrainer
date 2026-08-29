package me.apomazkin.polytrainer.di.module.groupstab

import dagger.Binds
import dagger.Module
import me.apomazkin.groupstab.deps.GroupsTabUseCase

@Module
interface GroupsTabModule {

    @Binds
    fun bindGroupsTabUseCase(impl: GroupsTabUseCaseImpl): GroupsTabUseCase
}
