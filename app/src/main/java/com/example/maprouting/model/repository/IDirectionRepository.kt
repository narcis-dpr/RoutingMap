package com.example.maprouting.model.repository

import com.example.maprouting.utiles.ResultWrapper
import kotlinx.coroutines.flow.Flow
import org.neshan.common.model.LatLng
import org.neshan.servicessdk.direction.model.NeshanDirectionResult

interface IDirectionRepository {
    suspend fun getFlowDirections(
        sourceLocation: LatLng,
        destinationLocation: LatLng,
    ): Flow<ResultWrapper<NeshanDirectionResult>>
}
