package cc.jchu.naver.line.yesterday.di

import android.content.Context
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.getComponent

class ApplicationComponent private constructor(
    val applicationContext: Context,
) {
    companion object : ComponentFactory<ApplicationComponent>() {
        override fun createComponent(context: Context): ApplicationComponent =
            ApplicationComponent(context.applicationContext)
    }
}

fun Context.applicationComponent(): ApplicationComponent =
    getComponent(ApplicationComponent)
