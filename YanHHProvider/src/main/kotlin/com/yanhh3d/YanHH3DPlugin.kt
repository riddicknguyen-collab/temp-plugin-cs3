package com.yanhh3d

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class YanHH3DPlugin : Plugin() {
    override fun load(context: Context) {
        // The provider must only ever be registered once.
        registerMainAPI(YanHH3DProvider())
    }
}
