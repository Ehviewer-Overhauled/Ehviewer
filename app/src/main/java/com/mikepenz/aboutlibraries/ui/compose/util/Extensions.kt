package com.mikepenz.aboutlibraries.ui.compose.util

import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License

val Library.author: String
    get() = developers.takeIf { it.isNotEmpty() }?.map { it.name }?.joinToString(", ") ?: organization?.name ?: ""

val License.htmlReadyLicenseContent: String?
    get() = licenseContent?.replace("\n", "<br />")

val License.strippedLicenseContent: String?
    get() = licenseContent?.replace("<br />", "\n")?.replace("<br/>", "\n")
