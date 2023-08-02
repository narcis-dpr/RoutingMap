package com.example.maprouting.di

import android.content.Context
import com.example.maprouting.model.api.Api
import com.example.maprouting.utiles.Constants
import com.example.maprouting.utiles.location.LocationClient
import com.example.maprouting.utiles.location.LocationClientImp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import org.neshan.common.model.LatLng
import org.neshan.servicessdk.direction.NeshanDirection
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppComponent {
    @Singleton
    @Provides
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Singleton
    @Provides
    fun provideFusedLocation(context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides
    fun provideLocationClient(
        context: Context,
        fusedLocationProviderClient: FusedLocationProviderClient,
    ): LocationClient {
        return LocationClientImp(context, fusedLocationProviderClient)
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            addInterceptor(
                LoggingInterceptor.Builder()
                    .setLevel(Level.BASIC)
                    .log(Platform.INFO)
                    .request("LOG")
                    .response("LOG")
                    .build(),
            )
        }.build()
    }

    @Provides
    @Singleton
    fun provideRetrofitConnection(
        okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.neshan.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGithubUsersApi(retrofit: Retrofit): Api {
        return retrofit.create(Api::class.java)
    }

//    @Provides
//    @Singleton
//    fun provideNeshanDirection(sourceLocation: LatLng, destinationLocation: LatLng): NeshanDirection? {
//        return NeshanDirection.Builder(Constants.API_KEY, sourceLocation, destinationLocation)
//            .build()
//    }
}
