package com.example.data.remote

import com.example.data.remote.dto.CreateTransactionRequest
import com.example.data.remote.dto.CreateUserRequest
import com.example.data.remote.dto.DailyReportDto
import com.example.data.remote.dto.LoginRequest
import com.example.data.remote.dto.LoginResponse
import com.example.data.remote.dto.ProductDto
import com.example.data.remote.dto.ProductResponse
import com.example.data.remote.dto.SingleProductResponse
import com.example.data.remote.dto.TransactionResponse
import com.example.data.remote.dto.UserDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PosApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("api/users")
    suspend fun getUsers(
        @Header("Authorization") token: String? = null
    ): Response<List<UserDto>>

    @POST("api/users")
    suspend fun createUser(
        @Header("Authorization") token: String? = null,
        @Body request: CreateUserRequest
    ): Response<LoginResponse>

    @DELETE("api/users/{id}")
    suspend fun deleteUser(
        @Path("id") id: Long,
        @Header("Authorization") token: String? = null
    ): Response<ResponseBody>

    @GET("api/products")
    suspend fun getProducts(
        @Header("Authorization") token: String? = null
    ): Response<List<ProductDto>>

    @POST("api/products")
    suspend fun createProduct(
        @Header("Authorization") token: String? = null,
        @Body product: ProductDto
    ): Response<SingleProductResponse>

    @PUT("api/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Long,
        @Header("Authorization") token: String? = null,
        @Body product: ProductDto
    ): Response<SingleProductResponse>

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: Long,
        @Header("Authorization") token: String? = null
    ): Response<ResponseBody>

    @POST("api/transactions")
    suspend fun submitTransaction(
        @Header("Authorization") token: String? = null,
        @Body transaction: CreateTransactionRequest
    ): Response<TransactionResponse>

    @GET("api/reports/daily")
    suspend fun getDailyReport(
        @Header("Authorization") token: String? = null,
        @Query("date") date: String
    ): Response<DailyReportDto>

    @GET("api/health")
    suspend fun healthCheck(): Response<ResponseBody>
}
