package com.example.maprouting.di

import com.example.maprouting.model.repository.DirectionRepository
import com.example.maprouting.model.repository.IDirectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindDirectionRepository(directionRepository: DirectionRepository): IDirectionRepository
}
