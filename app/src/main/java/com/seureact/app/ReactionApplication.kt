package com.seureact.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReactionApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.projectRepository.markRecordingProjectsInterrupted()
        }
    }
}
