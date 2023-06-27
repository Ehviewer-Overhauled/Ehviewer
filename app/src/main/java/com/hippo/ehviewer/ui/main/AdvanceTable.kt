package com.hippo.ehviewer.ui.main

object AdvanceTable {
    const val SH = 0x1
    const val STO = 0x2
    const val SFL = 0x100
    const val SFU = 0x200
    const val SFT = 0x400
}

data class AdvancedSearchOption(
    val advanceSearch: Int = 0,
    val minRating: Int = 0,
    val fromPage: Int = -1,
    val toPage: Int = -1,
)
