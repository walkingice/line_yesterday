package cc.jchu.naver.line.yesterday.feed

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
        val adapter = FeedAdapter({}, {})
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.screenName.apply {
            text = viewModel.screenName
            setOnClickListener { openFixedDetail() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderFeedState(binding, adapter, state)
                }
            }
        }
    }

    private fun openFixedDetail() {
        startActivity(
            DetailActivity.createIntent(requireContext(), DetailActivity.SOURCE_DUMMY_JSON, "1"),
        )
    }
}

internal fun renderFeedState(
    binding: FragmentFeedBinding,
    adapter: FeedAdapter,
    state: FeedUiState,
) {
    adapter.submitFeed(state.items, state.footerState)
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
