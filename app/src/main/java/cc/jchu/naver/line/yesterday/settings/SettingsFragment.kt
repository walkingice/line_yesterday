package cc.jchu.naver.line.yesterday.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import cc.jchu.naver.line.yesterday.R
import cc.jchu.naver.line.yesterday.data.settings.ClientSettings
import cc.jchu.naver.line.yesterday.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = checkNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = ClientSettings(requireContext())
        binding.useRealClient.isChecked = settings.useRealClient
        binding.useRealClient.setOnCheckedChangeListener { _, isChecked ->
            if (settings.useRealClient != isChecked) {
                settings.useRealClient = isChecked
                Toast.makeText(requireContext(), R.string.settings_restart_required, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
