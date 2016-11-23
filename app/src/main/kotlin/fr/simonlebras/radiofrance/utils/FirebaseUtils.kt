package fr.simonlebras.radiofrance.utils

import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.atomic.AtomicBoolean

object FirebaseUtils {
    private val isInitialized = AtomicBoolean()

    /**
     * Enables Firebase Database persistence.
     * An AtomicBoolean ensures that the operation is only executed once.
     */
    fun enableFirebaseDatabasePersistence() {
        if (isInitialized.compareAndSet(false, true)) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        }
    }
}
