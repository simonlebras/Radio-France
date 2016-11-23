package fr.simonlebras.radiofrance.ui.browser.exceptions

class SubscriptionException(parentId: String) : Exception("subscription to $parentId failed")
