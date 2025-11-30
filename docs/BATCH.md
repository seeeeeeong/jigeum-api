# 배치 시스템 설계와 구현

---

## 문제 정의

서울시 전역의 카페 데이터를 수집하고 최신 상태를 유지해야 한다. 하지만 다음과 같은 제약사항이 존재한다:

1. **Google Places API 비용**
   - Text Search: $32/1000 requests
   - Nearby Search: $32/1000 requests
   - Place Details: $17/1000 requests
   - 월 $200 무료 크레딧

2. **API Rate Limit**
   - QPS(Queries Per Second) 제한
   - 과도한 요청 시 429 에러

### 설계 목표

- 월 1회 전체 데이터 갱신으로 비용 최소화
- API 호출 실패 시 재시도 및 부분 복구
- 장애 발생 시에도 서비스 영향 최소화 (기존 데이터 유지)
- 데이터 수집과 처리 과정의 추적 가능성

---

## 아키텍처 설계

### 전체 구조

```
┌──────────────────────────────────────────────────────────┐
                    Batch Scheduler                       
  - 매월 말일 03:00: Raw Data Collection                    
  - 매월 말일 04:00: Failed Processing Retry                
  - 매월 말일 05:00: Data Processing                        
└────────────────┬─────────────────────────────────────────┘
                 │
    ┌────────────┴────────────┐
    │                         │
    ▼                         ▼
┌─────────────────┐    ┌───────────────────┐
  Collection Job          Processing Job    
     (10-15분)                (5-10분)         
└────────┬────────┘    └────────┬──────────┘
         │                      │
         ▼                      ▼
┌──────────────────┐    ┌──────────────────┐
│ cafe_raw_data    │───▶│ cafes            │
│ (원본 데이터)       │    │ (서비스용)         │
│                  │    │                  │
│ - place_id       │    │ cafe_operating_  │
│ - raw JSON       │    │ hours            │
│ - processed flag │    └──────────────────┘
└──────────────────┘
```

### 2단계 분리 설계의 이유

**왜 Collection과 Processing을 분리했는가?**

초기에는 API 호출 → DB 저장을 한 번에 처리하려 했으나, 다음과 같은 문제가 발생했다:

1. **실패 시 재시도 비용**
   - API 호출은 성공했지만 DB 저장 실패 → API 재호출 비용 발생
   - Place Details 조회 중 네트워크 장애 → 전체 재시작

2. **부분 복구 불가**
   - 70개 지점 중 50개 성공 후 실패 → 처음부터 다시 시작
   - 어느 단계에서 실패했는지 파악 곤란

**분리 후 개선점:**

```kotlin
// Collection: API 호출 결과를 Raw Data로 저장
suspend fun collectRawData(): OperationResponse = coroutineScope {
    val collectionResult = collectFromAllLocations(batchId)
    // API 호출 성공 데이터는 즉시 DB에 저장
    // 실패한 location만 재시도 가능
}

// Processing: Raw Data를 서비스용 데이터로 변환
suspend fun processRawData(reprocessAll: Boolean): OperationResponse {
    // DB에서 읽어서 처리하므로 API 비용 없음
    // 실패한 레코드만 재처리 가능
}
```

---

## 데이터 수집 전략

### 그리드 기반 수집

서울시를 70개 그리드로 분할하여 각 지점에서 반경 3km 내 카페를 검색한다.

**왜 70개 그리드인가?**


| 그리드 수 | 평균 중복률 | 수집 카페 수 | API 호출 수 | 비용 |
|---------|----------|------------|-----------|------|
| 30개 (5km) | 45% | ~2,500 | 30 | $0.96 |
| 50개 (4km) | 35% | ~3,200 | 50 | $1.60 |
| **70개 (3km)** | **28%** | **~4,000** | **70** | **$2.24** |
| 100개 (2km) | 15% | ~4,200 | 100 | $3.20 |


- 3km 반경이 Nearby Search의 sweet spot (너무 넓으면 관련도 낮은 결과 포함)
- 중복률 28%는 acceptable (처리 단계에서 `place_id`로 중복 제거)
- 월 $2.24는 무료 크레딧($200) 내에서 충분

### Location 선정 전략

```kotlin
@Component
class SeoulGridLocations {
    private val locations = listOf(
        // 강남/서초 - 밀도 높음 (10개 지점)
        GridLocation("강남역", 37.4979, 127.0276),
        GridLocation("선릉", 37.5048, 127.0493),
        // ...

        // 강북 - 밀도 낮음 (5개 지점)
        GridLocation("노원", 37.6542, 127.0568),
        // ...
    )
}
```

- 강남/서초: 10개 지점 (카페 밀집 지역)
- 종로/중구: 8개 지점 (관광/상업 지역)
- 강북: 5개 지점 (주거 지역)

### 동시성 제어

```kotlin
companion object {
    private const val CONCURRENT_REQUESTS_LIMIT = 3
    private const val REQUEST_DELAY_MS = 1000L
}

private suspend fun collectFromAllLocations(batchId: String) = coroutineScope {
    val requestSemaphore = Semaphore(CONCURRENT_REQUESTS_LIMIT)

    val locationResults = gridLocations.map { gridLocation ->
        async {
            requestSemaphore.withPermit {
                delay(REQUEST_DELAY_MS)  // Rate limiting
                collectFromLocation(gridLocation, batchId)
            }
        }
    }.awaitAll()
}
```


| 동시 요청 수 | 총 소요 시간 | 429 에러율 | 평균 응답 시간 |
|---|-------|------|---------|
| 1 | 23분 | 0% | 1.2초 |
| 3 | 12분 | 0.5% | 1.3초 |
| 5 | 8분 | 3% | 1.8초 |
| 10 | 5분 | 15% | 2.5초 |

- 3개: 안정성과 속도의 균형점
- 429 에러율 0.5%는 재시도로 처리 가능
- 12분은 acceptable (새벽 시간대 배치 실행)

### 실패 처리

```kotlin
private suspend fun collectFromLocation(
    gridLocation: GridLocation,
    batchId: String
): Result<Int> = runCatching {
    val searchResponse = googlePlacesClient.searchNearbyCafes(...)
    val newPlacesCount = saveNewPlaces(foundPlaces, batchId)
    newPlacesCount
}.onFailure { exception ->
    logger.warn("Failed to collect from location: {}", gridLocation.name)
}
```

- 한 지점의 실패가 전체에 영향 주지 않음
- Result 타입으로 성공/실패를 명시적으로 처리
- 실패한 지점만 로그 기록 후 계속 진행


---

## 부하 테스트

**테스트 환경:**
- MacBook Pro M1, 32GB RAM
- PostgreSQL 15 
- 4,000개 카페 데이터


```
=== Collection Phase ===
Total locations: 70
Concurrent requests: 3
Total time: 11분 43초
Success rate: 98.6% (69/70)
API calls: 70
Cost: $2.24

=== Processing Phase ===
Total records: 4,127
Batch size: 100
Parallel degree: ~8
Total time: 5분 52초
Success rate: 99.2% (4,095/4,127)
Failed records: 32 (invalid opening hours)

=== Total ===
End-to-end: 17분 35초
Peak memory: 512MB
DB connections: 10개 (peak)
```

### 실패 시나리오 테스트

**Test 1: API 일부 실패**
```kotlin
// 10개 location에서 강제 실패 injection
Result:
- 성공: 60/70 (85.7%)
- 수집 카페: 3,421개
- 재시도 후: 68/70 (97.1%)
```

**Test 2: DB Connection 장애**
```kotlin
// Processing 중 DB connection pool 고갈
Result:
- HikariCP timeout exception
- BatchJob status: FAILED
- Raw data 보존됨
- 다음 retry에서 이어서 처리 가능
```

**Test 3: Out of Memory**
```kotlin
// Batch size를 1000으로 증가
Result:
- 2,341번째 record에서 OOM
- Processed flag가 2,341개까지 true로 변경됨
- 재시작 시 2,342번째부터 이어서 처리
```
---
