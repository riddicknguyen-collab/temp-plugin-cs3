package com.vsphim

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class VsphimPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(VsphimProvider())
    }
}
