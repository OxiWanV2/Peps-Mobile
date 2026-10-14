package be.lorenzogallo.pepsmobile.di

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class SessionScope : ViewModelStoreOwner {
    private var store = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = store

    @Synchronized
    fun reset() {
        store.clear()
        store = ViewModelStore()
    }
}
