package cc.jchu.naver.line.yesterday.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import coil3.load
import cc.jchu.naver.line.yesterday.databinding.FragmentDetailBinding
import cc.jchu.naver.line.yesterday.di.applicationComponent
import cc.jchu.naver.line.yesterday.viewbinding.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetailFragment : Fragment() {
    private val viewModel by lazy {
        ViewModelProvider(this, requireContext().applicationComponent().detailViewModelFactory(detailArguments()))[
            DetailViewModel::class.java
        ]
    }
    private val binding by viewBinding(FragmentDetailBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentDetailBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenName.text = screenLabel()
        if (!viewModel.isArgumentsValid) {
            binding.detailErrorPanel.visibility = View.VISIBLE
            binding.detailError.visibility = View.VISIBLE
            (activity as? DetailActivity)?.hideFavoriteAction()
            return
        }
        binding.detailRetry.setOnClickListener { viewModel.retry() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderDetailState(binding, state)
                    (activity as? DetailActivity)?.renderFavoriteAction(state)
                }
            }
        }
    }

    private fun screenLabel(): String =
        if (viewModel.isArgumentsValid) viewModel.screenName else "Invalid detail"

    internal fun toggleFavorite() {
        viewModel.toggleFavorite()
    }

    private fun detailArguments(): DetailArguments? = DetailArguments.from(
        arguments?.getString(ARG_SOURCE),
        arguments?.getString(ARG_ID),
    )

    companion object {
        private const val ARG_SOURCE = "source"
        private const val ARG_ID = "id"

        fun newInstance(source: String?, id: String?): DetailFragment =
            DetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SOURCE, source)
                    putString(ARG_ID, id)
                }
            }
    }
}

internal fun renderDetailState(binding: FragmentDetailBinding, state: DetailUiState) {
    val detail = state.detail
    binding.detailLoading.visibility = if (state.isLoading && detail == null) {
        View.VISIBLE
    } else {
        View.GONE
    }
    binding.detailContent.visibility = if (detail == null) View.GONE else View.VISIBLE
    binding.detailErrorPanel.visibility = if (detail == null && state.error != null) {
        View.VISIBLE
    } else {
        View.GONE
    }
    binding.detailRetry.visibility = if (state.canRetry) View.VISIBLE else View.GONE
    binding.detailRefreshError.visibility = if (detail != null && state.error != null) {
        View.VISIBLE
    } else {
        View.GONE
    }
    if (detail == null) return

    binding.detailImage.load(detail.imgUrl)
    binding.detailTitle.text = detail.title
    binding.detailDescription.text = detail.description
    binding.detailExtraInformation.text = detail.extraInformation
}
