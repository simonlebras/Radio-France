package fr.simonlebras.radiofrance.utils

import fr.simonlebras.radiofrance.BuildConfig

object DebugUtils {
    /**
     * Executes the specified code if the Gradle build type is debug.
     *
     * @param[code] The code to be executed.
     */
    inline fun executeInDebugMode(code: () -> Unit) {
        if (BuildConfig.DEBUG) {
            code()
        }
    }
}
