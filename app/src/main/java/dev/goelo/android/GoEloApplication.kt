package dev.goelo.android

import android.app.Application

class GoEloApplication : Application() {
    val container by lazy { AppContainer(this) }
}
