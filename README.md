# Jigeum API

> **"지금 이 순간, 영업 중인 카페를 찾아드립니다"**
>
> 실시간 위치 기반 영업 중인 카페 검색 서비스

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)

---

## 프로젝트 소개

**"지금 당장 갈 수 있는 카페를 찾고 싶은데, 영업시간을 일일이 확인하는 게 번거롭다"**

이러한 문제를 해결하기 위해 시작된 프로젝트입니다. 기존 지도 앱들은 단순히 가까운 카페를 보여주지만, **지금 이 시간에 영업 중인지**를 바로 알 수 없습니다.

---

<p align="center">
  <img src="https://github.com/user-attachments/assets/ca8e2ddd-eacb-42aa-a112-0a92ae77ef01" width="30%" />
  <img src="https://github.com/user-attachments/assets/ce5782a7-20ba-477a-9609-bac28fd6b051" width="30%" />
  <img src="https://github.com/user-attachments/assets/42bb3e8f-f7db-468d-adf0-c600142b5069" width="30%" />
</p>

---

## API 명세

### Base URL

```
Local: http://localhost:8080
```

### 1. 카페 검색

```http
GET /api/v1/cafes/search
```

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| lat | Double | ✓ | 위도 | 37.4979 |
| lng | Double | ✓ | 경도 | 127.0276 |
| radius | Integer | ✓ | 반경 (m) | 1000 |
| time | String | ✓ | 시간 (HH:mm) | 14:00 |
| page | Integer | ✗ | 페이지 번호 | 0 |
| size | Integer | ✗ | 페이지 크기 | 20 |

**Response:**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "placeId": "ChIJ...",
        "name": "스타벅스 강남역점",
        "address": "서울특별시 강남구...",
        "latitude": 37.497900,
        "longitude": 127.027600,
        "distance": 125.5
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3
  },
  "timestamp": "2025-10-30T14:30:00"
}
```

### 2. 카페 상세 조회

```http
GET /api/v1/cafes/{cafeId}
```

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| cafeId | Long | ✓ | 카페 ID |

**Response:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "스타벅스 강남역점",
    "address": "서울특별시 강남구...",
    "latitude": 37.497900,
    "longitude": 127.027600,
    "operatingHours": [
      {
        "dayOfWeek": 0,
        "dayName": "일요일",
        "openTime": "08:00",
        "closeTime": "22:00"
      }
    ]
  }
}
```

### 3. 배치 작업 시작 (관리자)

```http
POST /api/v1/admin/batch/collect
POST /api/v1/admin/batch/process
```

### 4. 배치 작업 조회

```http
GET /api/v1/admin/batch/jobs/{batchId}
GET /api/v1/admin/batch/statistics
GET /api/v1/admin/batch/statistics/{jobType}
```

---

**배치 작업 흐름:**
```
1. Nearby Search (70개 거점)
   ↓
2. Raw Data 저장 (cafe_raw_data)
   ↓
3. Place Details 조회 (상세 정보)
   ↓
4. 중복 제거 (place_id 기준)
   ↓
5. 최종 데이터 저장 (cafes, cafe_operating_hours)
```

**아키텍처:**
```
┌─────────────┐
│ Scheduler   │  → 매월 말일 자동 실행
└──────┬──────┘
       ↓
┌─────────────┐
│ Collector   │  → Google Places API 호출 (70개 지점)
└──────┬──────┘
       ↓
┌─────────────┐
│ Raw Data DB │  → 원본 데이터 저장 (cafe_raw_data)
└──────┬──────┘
       ↓
┌─────────────┐
│ Processor   │  → 데이터 정제 및 변환
└──────┬──────┘
       ↓
┌─────────────┐
│ Cafes DB    │  → 서비스용 데이터 (cafes, cafe_operating_hours)
└─────────────┘
```
---

## 실행 방법

### 1. 사전 요구사항

```bash
# Java 17 이상
java -version

# Docker & Docker Compose
docker --version
docker-compose --version

# (Optional) PostgreSQL CLI
psql --version
```

### 2. 환경 변수 설정

`.env` 파일 생성:

```bash
# Google Places API
GOOGLE_API_KEY=your_api_key_here

# Database
DB_USERNAME=leesinseong
DB_PASSWORD=

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 3. Database 실행

```bash
# PostgreSQL + Redis 시작
docker-compose up -d

# 상태 확인
docker-compose ps
```

### 4. Application 실행

```bash
# Gradle로 실행
./gradlew bootRun

# 또는 JAR 빌드 후 실행
./gradlew clean build -x test
java -jar build/libs/jigeum-api-1.0.0.jar
```

### 5. 동작 확인

```bash
# Health Check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html

# 카페 검색 테스트
curl "http://localhost:8080/api/v1/cafes/search?lat=37.4979&lng=127.0276&radius=1000&time=14:00"
```

### 6. 배치 작업 실행

```bash
# 데이터 수집 (10-15분 소요)
curl -X POST http://localhost:8080/api/v1/admin/batch/collect

# 진행 상황 확인
curl http://localhost:8080/api/v1/admin/batch/statistics

# 데이터 처리
curl -X POST http://localhost:8080/api/v1/admin/batch/process
```


---
