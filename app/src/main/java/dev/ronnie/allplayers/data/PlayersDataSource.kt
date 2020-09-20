package dev.ronnie.allplayers.data

import androidx.paging.PagingSource
import dev.ronnie.allplayers.api.PlayersApi
import dev.ronnie.allplayers.models.Player
import dev.ronnie.allplayers.utils.retrofit

class PlayersDataSource : PagingSource<Int, Player>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Player> {
        val page = params.key ?: 1

        val playersApi = retrofit().create(PlayersApi::class.java)

        val response = playersApi.getPlayers(params.loadSize, page)

        return try {

            val players = response.playersList
            LoadResult.Page(
                data = players,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (response.meta.next_page == null) null else page + 1
            )


        } catch (exception: Exception) {

            LoadResult.Error(exception)
        }

    }
}