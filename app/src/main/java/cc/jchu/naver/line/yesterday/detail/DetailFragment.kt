package cc.jchu.naver.line.yesterday.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cc.jchu.naver.line.yesterday.databinding.FragmentDetailBinding
import cc.jchu.naver.line.yesterday.viewbinding.viewBinding

class DetailFragment : Fragment() {
    private val viewModel by lazy { ViewModelProvider(this)[DetailViewModel::class.java] }
    private val binding by viewBinding(FragmentDetailBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentDetailBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenName.text = screenLabel()
    }

    private fun screenLabel(): String =
        if (hasValidArguments()) viewModel.screenName else "Invalid detail"

    private fun hasValidArguments(): Boolean {
        val source = activity?.intent?.getStringExtra(DetailActivity.EXTRA_SOURCE)
        val id = activity?.intent?.getStringExtra(DetailActivity.EXTRA_ID)
        return source in validSources && !id.isNullOrBlank()
    }

    private companion object {
        val validSources = setOf(
            DetailActivity.SOURCE_DUMMY_JSON,
            DetailActivity.SOURCE_SPACE_FLIGHT,
        )
    }
}
