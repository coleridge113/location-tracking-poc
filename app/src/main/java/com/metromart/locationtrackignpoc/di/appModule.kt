package com.metromart.locationtrackignpoc.di

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.metromart.locationtrackignpoc.data.local.LocationDatabase
import com.metromart.locationtrackignpoc.data.local.repository.LocalRepository
import com.metromart.locationtrackignpoc.data.local.repository.LocalRepositoryImpl

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            LocationDatabase::class.java,
            "location_db"
        ).build()
    }
    single {
        get<LocationDatabase>().locationDao()
    }
    single<LocalRepository> {
        LocalRepositoryImpl(get())
    }
}