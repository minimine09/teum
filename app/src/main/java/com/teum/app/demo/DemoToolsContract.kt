package com.teum.app.demo

object DemoToolsContract {
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    const val CHROME_PACKAGE = "com.android.chrome"

    const val YOUTUBE_NAME = "YouTube"
    const val INSTAGRAM_NAME = "Instagram"
    const val CHROME_NAME = "Chrome"

    val targetPackages: Set<String> = linkedSetOf(
        YOUTUBE_PACKAGE,
        INSTAGRAM_PACKAGE,
        CHROME_PACKAGE
    )
}
