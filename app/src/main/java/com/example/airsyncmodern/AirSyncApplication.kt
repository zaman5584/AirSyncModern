package com.example.airsyncmodern

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for Air Sync Remote Z.
 * 
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 * throughout the application.
 */
@HiltAndroidApp
class AirSyncApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Application initialization code here
    }
}
