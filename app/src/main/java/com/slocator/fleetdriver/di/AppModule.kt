package com.slocator.fleetdriver.di

import com.slocator.fleetdriver.data.CompletionStore
import com.slocator.fleetdriver.data.PreferencesStore
import com.slocator.fleetdriver.data.RoutesRepository
import com.slocator.fleetdriver.ui.screens.login.data.LoginViewModel
import com.slocator.fleetdriver.ui.screens.routesscreen.data.RoutesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { PreferencesStore(androidContext()) }
    single { RoutesRepository() }
    single { CompletionStore(androidContext()) }
    viewModelOf(::RoutesViewModel)
    viewModelOf(::LoginViewModel)

}
