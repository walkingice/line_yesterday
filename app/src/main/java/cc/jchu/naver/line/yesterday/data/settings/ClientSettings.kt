package cc.jchu.naver.line.yesterday.data.settings

import android.content.Context

class ClientSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    var useRealClient: Boolean
        get() = preferences.getBoolean(USE_REAL_CLIENT, false)
        set(value) {
            preferences.edit().putBoolean(USE_REAL_CLIENT, value).apply()
        }

    companion object {
        const val USE_REAL_CLIENT = "USE_REAL_CLIENT"
        internal const val PREFERENCES_NAME = "client_settings"
    }
}
