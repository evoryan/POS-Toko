package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "user") val user: UserDto? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "username") val username: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "role") val role: String = "kasir", // "superadmin", "admin", "kasir"
    @Json(name = "storeName") val storeName: String? = "Toko Akbar Media Group",
    @Json(name = "isActive") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CreateUserRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String // "admin" or "kasir"
)
