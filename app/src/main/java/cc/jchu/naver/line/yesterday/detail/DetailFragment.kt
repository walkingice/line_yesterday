package cc.jchu.naver.line.yesterday.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cc.jchu.naver.line.yesterday.databinding.FragmentDetailBinding
import cc.jchu.naver.line.yesterday.di.applicationComponent
import cc.jchu.naver.line.yesterday.viewbinding.viewBinding

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
    }

    private fun screenLabel(): String =
        if (viewModel.isArgumentsValid) viewModel.screenName else "Invalid detail"

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
