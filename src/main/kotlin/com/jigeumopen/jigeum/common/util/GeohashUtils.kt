package com.jigeumopen.jigeum.common.util

import org.springframework.stereotype.Component
import kotlin.math.floor

/**
 * Geohash 유틸리티
 * 위치를 격자로 나누어 근처 좌표들이 같은 캐시 키를 공유하도록 함
 */
@Component
class GeohashUtils {

    fun encode(latitude: Double, longitude: Double, precision: Int = 6): String {
        val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
        
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0
        
        val geohash = StringBuilder()
        var isEven = true
        var bit = 0
        var ch = 0
        
        while (geohash.length < precision) {
            if (isEven) {
                val mid = (lonMin + lonMax) / 2
                if (longitude > mid) {
                    ch = ch or (1 shl (4 - bit))
                    lonMin = mid
                } else {
                    lonMax = mid
                }
            } else {
                val mid = (latMin + latMax) / 2
                if (latitude > mid) {
                    ch = ch or (1 shl (4 - bit))
                    latMin = mid
                } else {
                    latMax = mid
                }
            }
            
            isEven = !isEven
            
            if (bit < 4) {
                bit++
            } else {
                geohash.append(base32[ch])
                bit = 0
                ch = 0
            }
        }
        
        return geohash.toString()
    }

    fun simpleGrid(latitude: Double, longitude: Double, gridSize: Double = 0.005): String {
        val latGrid = floor(latitude / gridSize) * gridSize
        val lngGrid = floor(longitude / gridSize) * gridSize
        return "${String.format("%.3f", latGrid)}:${String.format("%.3f", lngGrid)}"
    }
}
