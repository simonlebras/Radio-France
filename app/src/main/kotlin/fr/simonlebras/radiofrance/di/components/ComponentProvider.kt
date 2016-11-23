package fr.simonlebras.radiofrance.di.components

interface ComponentProvider {
    fun provideComponent(): BaseComponent<*>
}
