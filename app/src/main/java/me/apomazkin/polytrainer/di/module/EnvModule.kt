package me.apomazkin.polytrainer.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.polytrainer.env.EnvParams
import me.apomazkin.polytrainer.env.EnvParamsImpl

@Module
interface EnvModule {
    
    @Binds
    fun bindAppVersion(impl: EnvParamsImpl): EnvParams
}