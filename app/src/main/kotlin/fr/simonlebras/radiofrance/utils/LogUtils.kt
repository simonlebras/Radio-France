package fr.simonlebras.radiofrance.utils

object LogUtils {
    private const val LOG_PREFIX = "radio_"
    private const val LOG_PREFIX_LENGTH = LOG_PREFIX.length
    private const val MAX_LOG_TAG_LENGTH = 23

    /**
     * Creates a custom logging tag using the specified tag and a prefix.
     * If the result exceeds 23 characters, the tag is truncated.
     *
     * @param[tag] The base logging tag.
     * @return The custom logging tag.
     */
    fun makeLogTag(tag: String): String {
        if (tag.length > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + tag.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1)
        }

        return LOG_PREFIX + tag
    }
}
