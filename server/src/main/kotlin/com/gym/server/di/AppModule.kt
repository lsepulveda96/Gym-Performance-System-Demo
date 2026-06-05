package com.gym.server.di

import com.gym.server.repository.*
import com.gym.server.service.*
import org.koin.dsl.module

val appModule = module {
    // Repositories
    single<PlanRepository> { PlanRepositoryImpl() }
    single<UserRepository> { UserRepositoryImpl() }
    single<PaymentRepository> { PaymentRepositoryImpl() }
    single<CheckInRepository> { CheckInRepositoryImpl() }

    // Services
    single { PlanService(get()) }
    single { AuthService(get()) }
    single { PaymentService(get()) }
    single { MemberService(get(), get(), get(), get()) }
    single { DashboardService(get(), get(), get(), get()) }
    single { AccessService(get(), get(), get(), get()) }
}
