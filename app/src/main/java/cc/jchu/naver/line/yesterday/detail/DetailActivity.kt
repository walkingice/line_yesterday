package cc.jchu.naver.line.yesterday.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.jchu.naver.line.yesterday.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding

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

    companion object {
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
