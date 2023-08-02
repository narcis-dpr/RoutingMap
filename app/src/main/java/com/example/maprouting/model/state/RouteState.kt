package com.example.maprouting.model.state

import org.neshan.servicessdk.direction.model.Route


data class RouteState(
    val isLoading: Boolean = false,
    val data: Route? = null,
    val error: String = ""
)