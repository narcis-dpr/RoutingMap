package com.example.maprouting.model.api

import com.example.maprouting.utiles.Constants
import org.neshan.common.model.LatLng
import org.neshan.servicessdk.direction.model.NeshanDirectionResult
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface Api {

    @Headers(Constants.API_KEY)
    @GET("v2/direction")
    suspend fun getDirections(
        @Query("origin") sourceLocation: String,
        @Query("destination") destinationLocation: String,
        @Query("avoidTrafficZone") avoidTrafficZone: Boolean = false,
        @Query("avoidOddEvenZone") avoidOddEvenZone: Boolean = false,
        @Query("alternative") alternative: Boolean = false
    ): NeshanDirectionResult
}
