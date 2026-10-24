package com.aggreyshield.app.di

import com.aggreyshield.app.core.network.AndroidNetworkMonitor
import com.aggreyshield.app.core.network.NetworkMonitor
import com.aggreyshield.app.core.security.SecureTokenStorage
import com.aggreyshield.app.core.security.TokenStorage
import com.aggreyshield.app.data.local.EvidenceSourceImpl
import com.aggreyshield.app.data.local.SettingsRepositoryImpl
import com.aggreyshield.app.data.repository.AlertRepositoryImpl
import com.aggreyshield.app.data.repository.AuthRepositoryImpl
import com.aggreyshield.app.data.repository.HealthRepositoryImpl
import com.aggreyshield.app.data.repository.IncidentRepositoryImpl
import com.aggreyshield.app.data.repository.NotificationRepositoryImpl
import com.aggreyshield.app.data.repository.ScanRepositoryImpl
import com.aggreyshield.app.data.repository.ThreatIntelRepositoryImpl
import com.aggreyshield.app.data.repository.TrainingRepositoryImpl
import com.aggreyshield.app.data.repository.UserRepositoryImpl
import com.aggreyshield.app.domain.repository.AlertRepository
import com.aggreyshield.app.domain.repository.AuthRepository
import com.aggreyshield.app.domain.repository.EvidenceSource
import com.aggreyshield.app.domain.repository.HealthRepository
import com.aggreyshield.app.domain.repository.IncidentRepository
import com.aggreyshield.app.domain.repository.NotificationRepository
import com.aggreyshield.app.domain.repository.ScanRepository
import com.aggreyshield.app.domain.repository.SettingsRepository
import com.aggreyshield.app.domain.repository.ThreatIntelRepository
import com.aggreyshield.app.domain.repository.TrainingRepository
import com.aggreyshield.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTokenStorage(impl: SecureTokenStorage): TokenStorage

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindEvidenceSource(impl: EvidenceSourceImpl): EvidenceSource

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindScanRepository(impl: ScanRepositoryImpl): ScanRepository

    @Binds
    @Singleton
    abstract fun bindIncidentRepository(impl: IncidentRepositoryImpl): IncidentRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds
    @Singleton
    abstract fun bindThreatIntelRepository(impl: ThreatIntelRepositoryImpl): ThreatIntelRepository

    @Binds
    @Singleton
    abstract fun bindTrainingRepository(impl: TrainingRepositoryImpl): TrainingRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindHealthRepository(impl: HealthRepositoryImpl): HealthRepository
}
