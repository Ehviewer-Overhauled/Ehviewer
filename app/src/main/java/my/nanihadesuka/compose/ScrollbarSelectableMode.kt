package my.nanihadesuka.compose

/**
 * Scrollbar selection modes.
 */
enum class ScrollbarSelectableMode {
    /**
     * Enable selection in the whole scrollbar and thumb
     */
    Full,

    /**
     * Enable selection in the thumb
     */
    Thumb,

    /**
     * Disable selection
     */
    Disabled,
}
