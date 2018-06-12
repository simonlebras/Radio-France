package com.simonlebras.radiofrance.ui.browser.exceptions

class SubscriptionException(parentId: String) : Exception("subscription to $parentId failed")
