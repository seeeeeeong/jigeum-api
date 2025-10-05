package com.jigeumopen.jigeum.common.interceptor

import com.jigeumopen.jigeum.common.exception.BusinessException
import com.jigeumopen.jigeum.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminAuthInterceptor(
    @Value("\${admin.api.key:admin-secret-key}")
    private val adminApiKey: String
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val adminKey = request.getHeader(ADMIN_KEY_HEADER)

        if (adminKey != adminApiKey) {
            throw BusinessException(ErrorCode.ADMIN_ONLY)
        }

        return true
    }

    companion object {
        private const val ADMIN_KEY_HEADER = "X-Admin-Key"
    }
}
