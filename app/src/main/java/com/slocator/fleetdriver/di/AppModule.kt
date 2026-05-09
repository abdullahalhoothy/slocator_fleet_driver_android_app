package com.slocator.fleetdriver.di

import com.slocator.fleetdriver.data.CompletionStore
import com.slocator.fleetdriver.data.PreferencesStore
import com.slocator.fleetdriver.data.RoutesRepository
import com.slocator.fleetdriver.ui.screens.LoginViewModel
import com.slocator.fleetdriver.ui.screens.routesscreen.data.RoutesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { PreferencesStore(androidContext()) }
    single { RoutesRepository() }
    single { CompletionStore(androidContext()) }

    viewModel { (onToggleLanguage: () -> Unit) ->
        RoutesViewModel(get(), get(), get(), onToggleLanguage)
    }
    viewModel { (onLoginSuccess: (String) -> Unit, onToggleLanguage: () -> Unit) ->
        LoginViewModel(get(), get(), onLoginSuccess, onToggleLanguage)
    }
}
