package com.gym.server.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileImageRequest(
    val profileImageUrl: String
)
