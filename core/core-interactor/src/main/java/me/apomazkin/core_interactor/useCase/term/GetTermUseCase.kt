package me.apomazkin.core_interactor.useCase.term

import io.reactivex.Observable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Term
import javax.inject.Inject

interface GetTermUseCase {
    fun getTermList(): Observable<List<Term>>

    class GetTermUseCaseImpl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : GetTermUseCase {
        override fun getTermList(): Observable<List<Term>> {
            return dbApi.getTermList()
        }
    }
}

