package cc.jchu.naver.line.yesterday.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.jchu.naver.line.yesterday.R

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DetailFragment())
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
