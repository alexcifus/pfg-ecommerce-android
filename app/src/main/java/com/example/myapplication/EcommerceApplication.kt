package com.example.myapplication

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

class EcommerceApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            // Keep Coil's default memory and disk caches and reuse downloaded images.
            .respectCacheHeaders(false)
            .build()
}
