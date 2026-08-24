package cc.jchu.naver.line.yesterday.favorites

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.jchu.naver.line.yesterday.R

class FavoritesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FavoritesFragment())
                .commit()
        }
    }
}
