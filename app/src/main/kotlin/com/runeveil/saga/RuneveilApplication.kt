package com.runeveil.saga

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.runeveil.saga.audio.AudioEngine
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Two things happen here and nowhere else:
 *  1. Hilt builds the singleton graph.
 *  2. The audio engine is bound to the *process* lifecycle, so music pauses
 *     when the game goes to the background — even if the activity is
 *     recreated by a configuration change.
 */
@HiltAndroidApp
class RuneveilApplication : Application() {

    @Inject
    lateinit var audioEngine: AudioEngine

    override fun onCreate() {
        super.onCreate()
        audioEngine.initialise()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = audioEngine.resume()
                override fun onStop(owner: LifecycleOwner) = audioEngine.pause()
            },
        )
    }

    override fun onTerminate() {
        audioEngine.release()
        super.onTerminate()
    }
}
