package cc.jchu.naver.line.yesterday.feed

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import cc.jchu.naver.line.yesterday.favorites.FavoritesActivity
import cc.jchu.naver.line.yesterday.R

class FeedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FeedFragment())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_FAVORITES, Menu.NONE, R.string.favorites_screen_name)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (item.itemId == MENU_FAVORITES) {
            startActivity(android.content.Intent(this, FavoritesActivity::class.java))
            true
        } else {
            super.onOptionsItemSelected(item)
        }

    internal companion object {
        const val MENU_FAVORITES = 1
    }
}
