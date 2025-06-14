package me.apomazkin.core_db_impl

import androidx.paging.PagingSource
import androidx.paging.PagingState
import me.apomazkin.core_db_impl.entity.TermDbEntity
import me.apomazkin.core_db_impl.room.WordDao
import javax.inject.Inject

class RoomPaging @Inject constructor(
        private val dao: WordDao,
        private val langId: Int,
) : PagingSource<Int, TermDbEntity>() {

    override fun getRefreshKey(state: PagingState<Int, TermDbEntity>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition)
        return anchorPage?.prevKey?.plus(1)
                ?: anchorPage?.nextKey?.minus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TermDbEntity> {
        val page = params.key ?: 0
        val limit = params.loadSize

        val offset = page * limit

        return try {
            val items = dao.searchTermsManual("pattern", langId, limit, offset)
            LoadResult.Page(
                    data = items,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (items.size < limit) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}