package com.zbrowser.app

data class BrowserTab(
    val id: Int,
    var title: String = "New Tab",
    var url: String = "",
    var webViewIndex: Int = -1,
    var isDesktopMode: Boolean = false
)
