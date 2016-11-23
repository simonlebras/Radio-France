package fr.simonlebras.radiofrance.di.components

interface BaseComponent<in T> {
    fun inject(target: T)
}
