package fr.simonlebras.radiofrance.utils

import android.os.Build

object VersionUtils {
    /**
     * Checks that the device's API level is high enough and executes the specified code if the
     * device is compatible.
     *
     * @param[sdkVersion] The lowest API level with which the specified code is compatible.
     * @param[code] The code to be executed.
     */
    inline fun supportsSdkVersion(sdkVersion: Int, code: () -> Unit) {
        if (Build.VERSION.SDK_INT >= sdkVersion) {
            code()
        }
    }
}
