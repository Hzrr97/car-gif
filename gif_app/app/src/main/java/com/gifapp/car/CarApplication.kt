package com.gifapp.car

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.VideoFrameDecoder
import com.gifapp.car.data.ServiceLocator

class CarApplication : Application() {
    lateinit var locator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        locator = ServiceLocator(this)

        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(GifDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)
    }
}