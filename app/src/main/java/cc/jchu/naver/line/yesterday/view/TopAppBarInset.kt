package cc.jchu.naver.line.yesterday.view

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun View.applyTopAppBarInset() {
    val initialPadding = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        view.updatePadding(
            top = initialPadding + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
