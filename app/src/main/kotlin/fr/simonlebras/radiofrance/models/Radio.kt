package fr.simonlebras.radiofrance.models

/**
 * Class representing a radio.
 *
 * @property id The identifier of the radio.
 * @property name The name of the radio.
 * @property description The description of the radio.
 * @property stream The URL of the audio stream.
 * @property website The URL of the website.
 * @property twitter The URL of the Twitter account.
 * @property facebook The URL of the Facebook account.
 * @property smallLogo The URL of the logo in small size.
 * @property mediumLogo The URL of the logo in medium size.
 * @property largeLogo The URL of the logo in large size.
 * @constructor Creates a radio.
 */
data class Radio(var id: String = "",
                 var name: String = "",
                 var description: String = "",
                 var stream: String = "",
                 var website: String = "",
                 var twitter: String = "",
                 var facebook: String = "",
                 var smallLogo: String = "",
                 var mediumLogo: String = "",
                 var largeLogo: String = "")
