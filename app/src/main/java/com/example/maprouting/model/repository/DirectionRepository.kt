package com.example.maprouting.model.repository

import com.example.maprouting.model.api.Api
import com.example.maprouting.utiles.ResultWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.neshan.common.model.LatLng
import org.neshan.servicessdk.direction.model.NeshanDirectionResult
import retrofit2.HttpException
import javax.inject.Inject

class DirectionRepository @Inject constructor(
    private val api: Api,
) : IDirectionRepository {
    override suspend fun getFlowDirections(
        sourceLocation: LatLng,
        destinationLocation: LatLng,
    ): Flow<ResultWrapper<NeshanDirectionResult>> = flow {
        emit(ResultWrapper.Loading(true))
        try {
            val result = api.getDirections(
                latLngToString(sourceLocation)!!,
                latLngToString(destinationLocation)!!,
            )
            emit(ResultWrapper.Success(result))
        } catch (e: HttpException) {
            emit(ResultWrapper.Error(e.toString()))
        }
    }

    private fun latLngToString(LatLng: LatLng): String? {
        return LatLng.latitude.toString() + "," + LatLng.longitude
    }
}
