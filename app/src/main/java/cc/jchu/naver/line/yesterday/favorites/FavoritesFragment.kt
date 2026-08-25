package cc.jchu.naver.line.yesterday.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import cc.jchu.naver.line.yesterday.detail.DetailActivity
import cc.jchu.naver.line.yesterday.data.domain.FeedFooterState
import cc.jchu.naver.line.yesterday.data.domain.FeedItem
import cc.jchu.naver.line.yesterday.data.domain.FeedSource
import cc.jchu.naver.line.yesterday.databinding.FragmentFavoritesBinding
import cc.jchu.naver.line.yesterday.di.applicationComponent
import cc.jchu.naver.line.yesterday.feed.FeedAdapter
import cc.jchu.naver.line.yesterday.viewbinding.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {
    private val viewModel by lazy {
        ViewModelProvider(this, requireContext().applicationComponent().favoritesViewModelFactory)[
            FavoritesViewModel::class.java
        ]
    }
    private val binding by viewBinding(FragmentFavoritesBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentFavoritesBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = FeedAdapter(
            onItemClick = { item ->
                startActivity(favoriteDetailIntent(requireContext(), item))
            },
            onFooterClick = { viewModel.loadMoreItems() },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.screenName.text = viewModel.screenName
        binding.screenName.contentDescription = viewModel.screenName
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderFavoritesState(binding, adapter, state)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}

internal fun favoriteDetailIntent(
    context: android.content.Context,
    item: FeedItem,
) = DetailActivity.createIntent(context, item.source.toDetailSource(), item.id)

private fun FeedSource.toDetailSource(): String = when (this) {
    FeedSource.DUMMY_JSON -> DetailActivity.SOURCE_DUMMY_JSON
    FeedSource.SPACE_FLIGHT -> DetailActivity.SOURCE_SPACE_FLIGHT
}

internal fun renderFavoritesState(
    binding: FragmentFavoritesBinding,
    adapter: FeedAdapter,
    state: cc.jchu.naver.line.yesterday.data.domain.FavoritesUiState,
) {
    val shouldResetListPosition = adapter.currentList.none { it is FeedAdapter.Row.Item } &&
        state.items.isNotEmpty()
    adapter.submitFeed(state.items, state.footerState) {
        if (shouldResetListPosition) {
            binding.recyclerView.scrollToPosition(0)
        }
    }
    binding.emptyContent.visibility = if (state.items.isEmpty()) View.VISIBLE else View.GONE
    binding.recyclerView.visibility = View.VISIBLE
}
