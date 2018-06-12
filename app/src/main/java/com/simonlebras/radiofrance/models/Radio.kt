package com.simonlebras.radiofrance.models

/**
 * Class representing a radio.
 *
 * @property id The identifier of the radio.
 * @property name The name of the radio.
 * @property description The description of the radio.
 * @property stream The URL of the audio stream.
 * @property logo The URL of the logo.
 */
data class Radio(
        val id: String = "",
        val name: String = "",
        val description: String = "",
        val stream: String = "",
        val logo: String = ""
)
