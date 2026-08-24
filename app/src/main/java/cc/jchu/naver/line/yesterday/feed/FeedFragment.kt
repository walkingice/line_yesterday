package cc.jchu.naver.line.yesterday.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cc.jchu.naver.line.yesterday.detail.DetailActivity
import cc.jchu.naver.line.yesterday.databinding.FragmentFeedBinding

class FeedFragment : Fragment() {
    private val viewModel by lazy { ViewModelProvider(this)[FeedViewModel::class.java] }
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = checkNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenName.apply {
            text = viewModel.screenName
            setOnClickListener { openFixedDetail() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openFixedDetail() {
        startActivity(
            DetailActivity.createIntent(requireContext(), DetailActivity.SOURCE_DUMMY_JSON, "1"),
        )
    }
}
