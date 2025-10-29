package com.jigeumopen.jigeum.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Value("\${server.port:8080}")
    private val serverPort: Int = 8080

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("지금영업중 API")
                    .version("1.0.0")
                    .description("""
                        # 지금영업중 - 실시간 카페 영업 정보 API
                        
                        현재 위치 기반으로 영업중인 카페를 실시간으로 검색할 수 있는 API
                        
                        ## 주요 기능
                        - 위치 기반 카페 검색 (반경 500m ~ 5km)
                        - 시간대별 영업중인 카페 필터링
                        - 카페 상세 정보 및 운영시간 조회
                        - 배치 작업을 통한 데이터 자동 수집
                                           
                        ## 기술 스택
                        - Kotlin + Spring Boot
                        - PostgreSQL + PostGIS
                        - Redis
                        - Google Places API
                    """.trimIndent())
                    .contact(
                        Contact()
                            .name("jigeum")
                            .email("lsinsung@gmail.com")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:$serverPort")
                        .description("로컬 개발 서버")
                )
            )
    }
}
