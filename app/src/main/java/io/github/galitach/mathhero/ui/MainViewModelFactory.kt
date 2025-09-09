package io.github.galitach.mathhero.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

object MainViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                ?: throw IllegalStateException("Application is required for this ViewModel")
            val savedStateHandle = extras.createSavedStateHandle()
            return MainViewModel(application, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}