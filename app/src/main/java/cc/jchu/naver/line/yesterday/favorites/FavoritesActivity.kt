package cc.jchu.naver.line.yesterday.favorites

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.jchu.naver.line.yesterday.databinding.ActivityFavoritesBinding

class FavoritesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, FavoritesFragment())
                .commit()
        }
    }
}
