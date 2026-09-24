package io.github.chalexey.cashpet.app

import android.app.Application
import android.content.Context

/** Приложение: держит единственный [AppContainer]. Прописано в AndroidManifest.xml. */
class CashPetApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Контейнер из любого Context — для фабрик ViewModel и экранов. */
val Context.appContainer: AppContainer
    get() = (applicationContext as CashPetApp).container
