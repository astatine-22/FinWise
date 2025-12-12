package com.example.finwise.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * AuthInterceptor automatically adds JWT Bearer token to all API requests.
 * 
 * This interceptor retrieves the stored token from SessionManager and
 * attaches it as an Authorization header to every outgoing request.
 * If no token is available, the request proceeds without the header.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Retrieve the token from SessionManager
        val token = sessionManager.fetchAuthToken()
        
        // If no token is available, proceed with the original request
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }
        
        // Create a new request with the Authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}
