object Versions {
    const val kotlin = "1.2.51"

    const val coroutines = "0.23.4"

    const val compileSdk = 28
    const val buildTools = "28.0.1"
    const val minSdk = 21
    const val targetSdk = 28

    const val appcompat = "1.0.0-beta01"
    const val recyclerview = "1.0.0-beta01"
    const val coordinatorlayout = "1.0.0-beta01"
    const val material = "1.0.0-beta01"
    const val mediarouter = "1.0.0-beta01"
    const val fragment = "1.0.0-beta01"

    const val constraintlayout = "2.0.0-alpha1"

    const val coreKtx = "1.0.0-beta01"

    const val lifecycle = "2.0.0-alpha1"

    const val navigation = "1.0.0-alpha02"

    const val playServices = "15.0.1"

    const val firebaseCore = "16.0.1"
    const val firebasePerf = "16.0.0"
    const val firebaseFirestore = "17.0.2"

    const val dagger = "2.16"

    const val okhttp = "3.10.0"

    const val glide = "4.8.0-SNAPSHOT"

    const val exoplayer = "2.8.2"

    const val timber = "4.7.1"

    const val leakcanary = "1.5.4"

    const val crashlytics = "2.9.3"
}

object Dependencies {
    const val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"

    const val coroutinesCore =
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    const val coroutinesAndroid =
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"

    const val appcompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
    const val recyclerview = "androidx.recyclerview:recyclerview:${Versions.recyclerview}"
    const val coordinatorlayout = "androidx.coordinatorlayout:coordinatorlayout:${Versions.coordinatorlayout}"
    const val material = "com.google.android.material:material:${Versions.material}"
    const val mediarouter = "androidx.mediarouter:mediarouter:${Versions.mediarouter}"
    const val fragment = "androidx.fragment:fragment:${Versions.fragment}"
    const val fragmentKtx = "androidx.fragment:fragment-ktx:${Versions.fragment}"

    const val constraintlayout =
            "androidx.constraintlayout:constraintlayout:${Versions.constraintlayout}"

    const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"

    const val lifecycle = "androidx.lifecycle:lifecycle-extensions:${Versions.lifecycle}"
    const val lifecycleJava8 = "androidx.lifecycle:lifecycle-common-java8:${Versions.lifecycle}"
    const val lifecycleViewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"

    const val navigationFragment = "android.arch.navigation:navigation-fragment:${Versions.navigation}"
    const val navigationUi = "android.arch.navigation:navigation-ui:${Versions.navigation}"
    const val navigationFragmentKtx = "android.arch.navigation:navigation-fragment-ktx:${Versions.navigation}"
    const val navigationUiKtx = "android.arch.navigation:navigation-ui-ktx:${Versions.navigation}"

    const val playServicesCastFramework =
            "com.google.android.gms:play-services-cast-framework:${Versions.playServices}"

    const val firebaseCore = "com.google.firebase:firebase-core:${Versions.firebaseCore}"
    const val firebasePerf = "com.google.firebase:firebase-perf:${Versions.firebasePerf}"
    const val firebaseFirestore =
            "com.google.firebase:firebase-firestore:${Versions.firebaseFirestore}"

    const val dagger = "com.google.dagger:dagger:${Versions.dagger}"
    const val daggerCompiler = "com.google.dagger:dagger-compiler:${Versions.dagger}"
    const val daggerAndroidSupport = "com.google.dagger:dagger-android-support:${Versions.dagger}"
    const val daggerAndroidProcessor =
            "com.google.dagger:dagger-android-processor:${Versions.dagger}"

    const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"

    const val glide = "com.github.bumptech.glide:glide:${Versions.glide}"
    const val glideCompiler = "com.github.bumptech.glide:compiler:${Versions.glide}"
    const val glideOkhttp = "com.github.bumptech.glide:okhttp3-integration:${Versions.glide}"
    const val glideRecyclerview =
            "com.github.bumptech.glide:recyclerview-integration:${Versions.glide}"

    const val exoplayerCore = "com.google.android.exoplayer:exoplayer-core:${Versions.exoplayer}"

    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"

    const val leakcanary = "com.squareup.leakcanary:leakcanary-android:${Versions.leakcanary}"
    const val leakcanaryNoOp =
            "com.squareup.leakcanary:leakcanary-android-no-op:${Versions.leakcanary}"

    const val crashlytics = "com.crashlytics.sdk.android:crashlytics:${Versions.crashlytics}"
}
