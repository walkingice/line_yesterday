package cc.jchu.naver.line.yesterday.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cc.jchu.naver.line.yesterday.databinding.FragmentFavoritesBinding

class FavoritesFragment : Fragment() {
    private val viewModel by lazy { ViewModelProvider(this)[FavoritesViewModel::class.java] }
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = checkNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenName.text = viewModel.screenName
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
