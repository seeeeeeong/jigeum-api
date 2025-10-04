package com.jigeumopen.jigeum.external.exception

sealed class GooglePlacesException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    
    class BadRequest(message: String, cause: Throwable? = null) : 
        GooglePlacesException(message, cause)
    
    class Unauthorized(message: String, cause: Throwable? = null) : 
        GooglePlacesException(message, cause)
    
    class NotFound(message: String, cause: Throwable? = null) : 
        GooglePlacesException(message, cause)
    
    class RateLimited(message: String, cause: Throwable? = null) : 
        GooglePlacesException(message, cause)
    
    class ServerError(message: String, cause: Throwable? = null) : 
        GooglePlacesException(message, cause)
    
    class UnknownError(message: String, cause: Throwable? = null) : 
        GooglePlacesException(message, cause)
}
