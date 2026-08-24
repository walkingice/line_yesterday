package cc.jchu.naver.line.yesterday.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cc.jchu.naver.line.yesterday.databinding.FragmentFavoritesBinding
import cc.jchu.naver.line.yesterday.di.applicationComponent
import cc.jchu.naver.line.yesterday.viewbinding.viewBinding

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
        binding.screenName.text = viewModel.screenName
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
