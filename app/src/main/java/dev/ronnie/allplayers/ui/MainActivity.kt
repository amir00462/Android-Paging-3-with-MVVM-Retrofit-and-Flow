package dev.ronnie.allplayers.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.ronnie.allplayers.R
import dev.ronnie.allplayers.adapters.PlayersAdapter
import dev.ronnie.allplayers.adapters.PlayersLoadingStateAdapter
import dev.ronnie.allplayers.databinding.ActivityMainBinding
import dev.ronnie.allplayers.utils.RecyclerViewItemDecoration
import dev.ronnie.allplayers.viewmodels.MainViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val adapter =
        PlayersAdapter { name: String -> snackBarClickedPlayer(name) }

    private var searchJob: Job? = null

    @ExperimentalPagingApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setUpAdapter()
        startSearchJob()
        binding.swipeRefreshLayout.setOnRefreshListener {

            adapter.refresh()

        }

    }

    @ExperimentalPagingApi
    private fun startSearchJob() {

        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            viewModel.searchPlayers()
                .collectLatest {
                    adapter.submitData(it)
                }
        }
        /**
         * Same thing but with Livedata
         */

//        searchJob?.cancel()
//        searchJob = lifecycleScope.launch {
//            viewModel.searchPlayersLiveData().observe(this@MainActivity, {
//
//                adapter.submitData(this@MainActivity.lifecycle, it)
//
//            })
//
//        }
    }

    private fun snackBarClickedPlayer(name: String) {
        val parentLayout = findViewById<View>(android.R.id.content)
        Snackbar.make(parentLayout, name, Snackbar.LENGTH_LONG)
            .show()
    }

    private fun setUpAdapter() {

        binding.allProductRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
            addItemDecoration(RecyclerViewItemDecoration())
        }
        binding.allProductRecyclerView.adapter = adapter.withLoadStateFooter(
            footer = PlayersLoadingStateAdapter { retry() }
        )

        adapter.addLoadStateListener { loadState ->

            if (loadState.mediator?.refresh is LoadState.Loading) {

                if (adapter.snapshot().isEmpty()) {
                    binding.progress.isVisible = true
                }
                binding.errorTxt.isVisible = false

            } else {
                binding.progress.isVisible = false
                binding.swipeRefreshLayout.isRefreshing = false

                val error = when {
                    loadState.mediator?.prepend is LoadState.Error -> loadState.mediator?.prepend as LoadState.Error
                    loadState.mediator?.append is LoadState.Error -> loadState.mediator?.append as LoadState.Error
                    loadState.mediator?.refresh is LoadState.Error -> loadState.mediator?.refresh as LoadState.Error

                    else -> null
                }
                error?.let {
                    if (adapter.snapshot().isEmpty()) {
                        binding.errorTxt.isVisible = true
                        binding.errorTxt.text = it.error.localizedMessage
                    }

                }

            }
        }

    }

    private fun retry() {
        adapter.retry()
    }


}
