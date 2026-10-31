package me.apomazkin.polytrainer.di.module.scancandidates

import dagger.Binds
import dagger.Module
import me.apomazkin.scancandidates.deps.ScanCandidatesUseCase

@Module
interface ScanCandidatesModule {

    @Binds
    fun bindScanCandidatesUseCase(impl: ScanCandidatesUseCaseImpl): ScanCandidatesUseCase
}
