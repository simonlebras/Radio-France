object Versions {
    const val androidPlugin = "3.1.3"

    const val kotlin = "1.2.50"

    const val coroutines = "0.23.3"

    const val compileSdk = 28
    const val buildTools = "28.0.0"
    const val minSdk = 21
    const val targetSdk = 28

    const val supportLibrary = "28.0.0-alpha3"

    const val constraintLayout = "1.1.2"

    const val lifecycle = "1.1.1"

    const val playServices = "15.0.1"

    const val firebaseCore = "16.0.0"
    const val firebasePerf = "16.0.0"
    const val firebaseFirestore = "17.0.1"

    const val dagger = "2.16"

    const val rxjava = "2.1.14"
    const val rxandroid = "2.0.2"
    const val rxkotlin = "2.2.0"
    const val rxbinding = "2.1.1"

    const val okhttp = "3.10.0"

    const val glide = "4.7.1"

    const val exoplayer = "2.8.1"

    const val timber = "4.7.0"

    const val leakcanary = "1.5.4"

    const val rxlint = "1.6.1"

    const val crashlytics = "2.9.3"
}

object Dependencies {
    const val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"

    const val coroutinesCore =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    const val coroutinesAndroid =
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"

    const val appcompat = "com.android.support:appcompat-v7:${Versions.supportLibrary}"
    const val recyclerview = "com.android.support:recyclerview-v7:${Versions.supportLibrary}"
    const val cardview = "com.android.support:cardview-v7:${Versions.supportLibrary}"
    const val coordinatorlayout = "com.android.support:coordinatorlayout:${Versions.supportLibrary}"
    const val design = "com.android.support:design:${Versions.supportLibrary}"
    const val mediarouter = "com.android.support:mediarouter-v7:${Versions.supportLibrary}"

    const val constraintLayout =
        "com.android.support.constraint:constraint-layout:${Versions.constraintLayout}"

    const val lifecycle = "android.arch.lifecycle:extensions:${Versions.lifecycle}"
    const val lifecycleJava8 = "android.arch.lifecycle:common-java8:${Versions.lifecycle}"

    const val databinding = "com.android.databinding:compiler:${Versions.androidPlugin}"

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

    const val rxjava = "io.reactivex.rxjava2:rxjava:${Versions.rxjava}"
    const val rxandroid = "io.reactivex.rxjava2:rxandroid:${Versions.rxandroid}"
    const val rxkotlin = "io.reactivex.rxjava2:rxkotlin:${Versions.rxkotlin}"
    const val rxbindingKotlin = "com.jakewharton.rxbinding2:rxbinding-kotlin:${Versions.rxbinding}"
    const val rxbindingAppcompatKotlin =
        "com.jakewharton.rxbinding2:rxbinding-appcompat-v7-kotlin:${Versions.rxbinding}"

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

    const val rxlint = "nl.littlerobots.rxlint:rxlint:${Versions.rxlint}"

    const val crashlytics = "com.crashlytics.sdk.android:crashlytics:${Versions.crashlytics}"
}
