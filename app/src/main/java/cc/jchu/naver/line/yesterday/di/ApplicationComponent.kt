package cc.jchu.naver.line.yesterday.di

import android.content.Context
import androidx.room.Room
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.getComponent
import cc.jchu.naver.line.yesterday.data.database.AppDatabase

class ApplicationComponent private constructor(
    val applicationContext: Context,
    val database: AppDatabase,
) {
    companion object : ComponentFactory<ApplicationComponent>() {
        override fun createComponent(context: Context): ApplicationComponent {
            val applicationContext = context.applicationContext
            return ApplicationComponent(
                applicationContext = applicationContext,
                database = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).build(),
            )
        }

        private const val DATABASE_NAME = "line_yesterday.db"
    }
}

fun Context.applicationComponent(): ApplicationComponent =
    getComponent(ApplicationComponent)
