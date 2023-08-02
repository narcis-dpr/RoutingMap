package com.example.maprouting.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maprouting.model.repository.IDirectionRepository
import com.example.maprouting.utiles.ResultWrapper
import com.example.maprouting.utiles.data
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.neshan.common.model.LatLng
import org.neshan.servicessdk.direction.model.Route
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val directionRepository: IDirectionRepository,
) : ViewModel() {
    private val _route: MutableLiveData<Route?> = MutableLiveData()
    val route: LiveData<Route?> = _route
    fun getDirections(
        sourceLocation: LatLng,
        destinationLocation: LatLng,
    ) {
        viewModelScope.launch {
            directionRepository.getFlowDirections(
                sourceLocation,
                destinationLocation,
            ).collect { result ->
                when (result) {
                    is ResultWrapper.Error -> println("**")
                    is ResultWrapper.Loading -> println("***")
                    is ResultWrapper.Success -> {
                        _route.postValue(result.data.routes[0])
                    }
                }

            }

        }
    }

    override fun onCleared() {
        super.onCleared()
        println("*** im cleared ")
    }
}
