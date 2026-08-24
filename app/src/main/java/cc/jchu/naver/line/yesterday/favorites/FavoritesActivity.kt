package cc.jchu.naver.line.yesterday.favorites

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.jchu.naver.line.yesterday.databinding.ActivityFavoritesBinding
import cc.jchu.naver.line.yesterday.view.applyTopAppBarInset

class FavoritesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fragmentContainer.applyTopAppBarInset()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, FavoritesFragment())
                .commit()
        }
    }
}
