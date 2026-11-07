package com.seureact.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.seureact.app.AppContainer

class MainViewModelFactory(
    private val application: Application,
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(application, container) as T
}
