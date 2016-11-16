package fr.simonlebras.radiofrance.models

/**
 * Class representing a radio.
 *
 * @property id The identifier of the radio.
 * @property name The name of the radio.
 * @property description The description of the radio.
 * @property website The URL of the website.
 * @property twitter The URL of the Twitter account.
 * @property facebook The URL of the Facebook account.
 * @property stream The URL of the audio stream.
 * @property logo The logo of the radio
 * @constructor Creates a radio.
 */
data class Radio(var id: String = "",
                 var name: String = "",
                 var description: String = "",
                 var website: String = "",
                 var twitter: String = "",
                 var facebook: String = "",
                 var stream: String = "",
                 var logo: Logo = Logo())