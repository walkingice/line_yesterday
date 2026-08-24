package cc.jchu.naver.line.yesterday.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import cc.jchu.naver.line.yesterday.R
import cc.jchu.naver.line.yesterday.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private var favoriteMenuItem: MenuItem? = null
    private var favoriteState = DetailUiState()
    private var isFavoriteActionVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    binding.fragmentContainer.id,
                    DetailFragment.newInstance(
                        intent.getStringExtra(EXTRA_SOURCE),
                        intent.getStringExtra(EXTRA_ID),
                    ),
                )
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        favoriteMenuItem = menu.add(Menu.NONE, MENU_FAVORITE, Menu.NONE, R.string.detail_add_favorite)
            .apply { setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS) }
        renderFavoriteAction(favoriteState)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        MENU_FAVORITE -> {
            (supportFragmentManager.fragments.singleOrNull() as? DetailFragment)?.toggleFavorite()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    internal fun renderFavoriteAction(state: DetailUiState) {
        favoriteState = state
        isFavoriteActionVisible = true
        favoriteMenuItem?.apply {
            isVisible = isFavoriteActionVisible
            isEnabled = state.detail != null && !state.isTogglingFavorite
            setIcon(
                if (state.isFavorite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off,
            )
            contentDescription = getString(
                if (state.isFavorite) R.string.detail_remove_favorite
                else R.string.detail_add_favorite,
            )
        }
    }

    internal fun hideFavoriteAction() {
        isFavoriteActionVisible = false
        favoriteMenuItem?.isVisible = false
    }

    companion object {
        private const val MENU_FAVORITE = 1
        const val EXTRA_SOURCE = "source"
        const val EXTRA_ID = "id"
        const val SOURCE_DUMMY_JSON = "dummy_json"
        const val SOURCE_SPACE_FLIGHT = "space_flight"

        fun createIntent(context: Context, source: String, id: String): Intent =
            Intent(context, DetailActivity::class.java)
                .putExtra(EXTRA_SOURCE, source)
                .putExtra(EXTRA_ID, id)
    }
}
