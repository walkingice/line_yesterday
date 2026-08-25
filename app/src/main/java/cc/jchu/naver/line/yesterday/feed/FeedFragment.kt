package cc.jchu.naver.line.yesterday.feed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.jchu.naver.line.yesterday.R
import cc.jchu.naver.line.yesterday.detail.DetailActivity
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.FeedItem
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.data.domain.FeedUiState
import cc.jchu.naver.line.yesterday.databinding.FragmentFeedBinding
import cc.jchu.naver.line.yesterday.di.applicationComponent
import cc.jchu.naver.line.yesterday.viewbinding.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {
    private val viewModel by lazy {
        ViewModelProvider(this, requireContext().applicationComponent().feedViewModelFactory)[
            FeedViewModel::class.java
        ]
    }
    private val binding by viewBinding(FragmentFeedBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentFeedBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = FeedAdapter(
            onItemClick = { item ->
                startActivity(detailIntentFor(requireContext(), item))
            },
            onFooterClick = {
                val state = viewModel.uiState.value.footerState
                Log.d(TAG, "Load more footer clicked: state=$state")
                handleFooterAction(state, viewModel::loadMoreItems, viewModel::loadMoreItems)
            },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(
            FeedItemSpacingDecoration(resources.getDimensionPixelSize(R.dimen.feed_item_spacing)),
        )
        binding.recyclerView.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.screenName.apply {
            text = viewModel.screenName
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderFeedState(binding, adapter, state)
                }
            }
        }
    }

    private companion object {
        const val TAG = "FeedFragment"
    }
}

internal class FeedItemSpacingDecoration(
    private val spacing: Int,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        outRect.set(0, spacing, 0, 0)
    }
}

internal fun detailIntentFor(
    context: android.content.Context,
    item: FeedItem,
) = DetailActivity.createIntent(context, item.source.toDetailSource(), item.id)

private fun FeedSource.toDetailSource(): String = when (this) {
    FeedSource.DUMMY_JSON -> DetailActivity.SOURCE_DUMMY_JSON
    FeedSource.SPACE_FLIGHT -> DetailActivity.SOURCE_SPACE_FLIGHT
}

internal fun handleFooterAction(
    state: FeedFooterState,
    loadMore: () -> Unit,
    retry: () -> Unit,
) {
    when (state) {
        FeedFooterState.Ready -> loadMore()
        FeedFooterState.Error, FeedFooterState.Offline -> retry()
        FeedFooterState.Loading, FeedFooterState.NoMoreItems -> Unit
    }
}

internal fun renderFeedState(
    binding: FragmentFeedBinding,
    adapter: FeedAdapter,
    state: FeedUiState,
) {
    val shouldResetListPosition = adapter.currentList.none { it is FeedAdapter.Row.Item } &&
        state.items.isNotEmpty()
    adapter.submitFeed(state.items, state.footerState) {
        if (shouldResetListPosition) {
            binding.recyclerView.scrollToPosition(0)
        }
    }
    binding.swipeRefresh.isRefreshing = state.refreshing

    val hasItems = state.items.isNotEmpty()
    val showInitialLoading = state.initialLoading && !hasItems
    val showError = !hasItems && state.footerState in setOf(
        FeedFooterState.Error,
        FeedFooterState.Offline,
    )
    binding.initialLoading.visibility = if (showInitialLoading) View.VISIBLE else View.GONE
    binding.errorContent.visibility = if (showError) View.VISIBLE else View.GONE
    binding.emptyContent.visibility = if (!hasItems && !showInitialLoading && !showError) {
        View.VISIBLE
    } else {
        View.GONE
    }
    binding.recyclerView.visibility = if (showInitialLoading) View.GONE else View.VISIBLE
}
