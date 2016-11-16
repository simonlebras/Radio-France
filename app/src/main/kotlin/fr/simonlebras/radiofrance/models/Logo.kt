package fr.simonlebras.radiofrance.models

/**
 * Class representing a radio's logo.
 *
 * @property availableSizes The different available sizes of the logo, using the following regexp: (w|h)(-\d+)+ .
 * @property urls Map containing the URLs of the logo by size(height or width).
 * @constructor Creates a logo.
 */
class Logo(var availableSizes: String = "",
           var urls: Map<String, String> = hashMapOf())