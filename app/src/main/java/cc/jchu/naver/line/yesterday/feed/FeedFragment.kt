package cc.jchu.naver.line.yesterday.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cc.jchu.naver.line.yesterday.detail.DetailActivity
import cc.jchu.naver.line.yesterday.databinding.FragmentFeedBinding
import cc.jchu.naver.line.yesterday.di.applicationComponent
import cc.jchu.naver.line.yesterday.viewbinding.viewBinding

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
        binding.screenName.apply {
            text = viewModel.screenName
            setOnClickListener { openFixedDetail() }
        }
    }

    private fun openFixedDetail() {
        startActivity(
            DetailActivity.createIntent(requireContext(), DetailActivity.SOURCE_DUMMY_JSON, "1"),
        )
    }
}
