package me.apomazkin.core_interactor.useCase.term

import io.reactivex.Observable
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.Term
import javax.inject.Inject

interface SearchTermUseCase {
    fun getTermList(pattern: String): Observable<List<Term>>

    class SearchTermUseCaseImpl @Inject constructor(
        private val dbApi: CoreDbApi
    ) : SearchTermUseCase {
        override fun getTermList(pattern: String): Observable<List<Term>> {
            return dbApi.searchTermList(pattern)
        }
    }
}
