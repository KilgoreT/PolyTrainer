package me.apomazkin.core_interactor.di

import me.apomazkin.core_interactor.CoreInteractorApi


interface CoreInteractorDependency {
    fun getCoreInteractorApi(): CoreInteractorApi
}
