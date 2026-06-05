package com.gym.frontend.di

import com.gym.frontend.ui.admin.DashboardRepository
import com.gym.frontend.ui.admin.DashboardService
import com.gym.frontend.ui.admin.MembersRepository
import com.gym.frontend.ui.admin.MembersService
import com.gym.frontend.ui.admin.PaymentsRepository
import com.gym.frontend.ui.admin.PaymentsService
import com.gym.frontend.ui.admin.AdminDashboardViewModel
import com.gym.frontend.ui.admin.MembersListViewModel
import com.gym.frontend.ui.admin.PaymentDialogViewModel
import com.gym.frontend.ui.auth.AuthRepository
import com.gym.frontend.ui.auth.AuthService
import com.gym.frontend.ui.auth.LoginViewModel
import com.gym.frontend.ui.auth.TokenManager
import com.gym.frontend.ui.member.MemberHomeViewModel
import org.koin.dsl.module

val frontendModule = module {
    // Services
    single { AuthService() }
    single { MembersService() }
    single { DashboardService() }
    single { PaymentsService() }
    single { TokenManager() }

    // Repositories
    single { AuthRepository(get(), get()) }
    single { MembersRepository(get()) }
    single { DashboardRepository(get()) }
    single { PaymentsRepository(get()) }

    // ViewModels
    factory { LoginViewModel(get()) }
    factory { MemberHomeViewModel(get(), get()) }
    factory { AdminDashboardViewModel(get()) }
    factory { MembersListViewModel(get()) }
    factory { PaymentDialogViewModel(get()) }
}
