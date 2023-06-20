package com.hippo.ehviewer.client.data

interface AbstractGalleryInfo {
    var gid: Long
    var token: String?
    var title: String?
    var titleJpn: String?
    var thumbKey: String?
    var category: Int
    var posted: String?
    var uploader: String?
    var disowned: Boolean
    var rating: Float
    var rated: Boolean
    var simpleTags: ArrayList<String>?
    var pages: Int
    var thumbWidth: Int
    var thumbHeight: Int
    var simpleLanguage: String?
    var favoriteSlot: Int
    var favoriteName: String?
}
