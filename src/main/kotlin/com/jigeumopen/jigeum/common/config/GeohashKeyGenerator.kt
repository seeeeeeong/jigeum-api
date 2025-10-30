package com.jigeumopen.jigeum.common.config

import com.jigeumopen.jigeum.cafe.dto.CafeRequest
import com.jigeumopen.jigeum.common.util.GeohashUtils
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.stereotype.Component
import java.lang.reflect.Method


@Component("geohashKeyGenerator")
class GeohashKeyGenerator(
    private val geohashUtils: GeohashUtils
) : KeyGenerator {
    
    override fun generate(target: Any, method: Method, vararg params: Any?): Any {
        val request = params.firstOrNull() as? CafeRequest
            ?: return params.joinToString(":")
        
        val geohash = geohashUtils.encode(
            request.lat, 
            request.lng, 
            precision = 6
        )
        
        val roundedRadius = (request.radius / 100) * 100
        return "$geohash:$roundedRadius:${request.time}"
    }
}
