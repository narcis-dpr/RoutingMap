package com.example.maprouting.utiles

sealed class ResultWrapper<out R> {
    data class Success<out T> (val data: T) : ResultWrapper<T>()
    data class Error(val exception: String) : ResultWrapper<Nothing>()
    data class Loading(val isLoading: Boolean) : ResultWrapper<Nothing>()
}
val <T> ResultWrapper<T>.data: T?
    get() {
        return (this as? ResultWrapper.Success)?.data
    }
