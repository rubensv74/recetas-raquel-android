package com.rmm.recetasraquel

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.rmm.recetasraquel.app.AppContainer
import com.rmm.recetasraquel.app.RecipePhotoPathMapper

class RecetasRaquelApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(RecipePhotoPathMapper(filesDir))
        }
        .build()
}
