# Geohash 기반 위치 캐싱

---

## 문제 정의

### 위치 기반 검색의 특성

사용자가 "강남역 근처 카페"를 검색할 때:

```
User A: lat=37.4979, lng=127.0276, radius=1000m, time=14:00
User B: lat=37.4980, lng=127.0277, radius=1000m, time=14:00
```

두 사용자의 위치는 약 15m 차이나지만, **검색 결과는 거의 동일**하다.

- User A: 20개 카페
- User B: 20개 카페 (19개 동일, 1개 다름)

**문제:** 캐시를 사용하지 않으면?

```kotlin
// 요청마다 DB 쿼리 실행
val cafes = cafeRepository.findNearbyOpenCafes(
    latitude = 37.4979,
    longitude = 127.0276,
    radius = 1000,
    ...
)  // PostGIS ST_DWithin 연산: ~200ms
```

- 평균 응답 시간: 250ms
- DB CPU: 40%
- 동시 요청 100개: DB connection pool 고갈

---

**1. 위도/경도를 그대로 캐시 키로 사용**

```kotlin
@Cacheable(value = ["nearby"], key = "#request.lat + ':' + #request.lng")
```

- 캐시 히트율: **3%**
- 이유: GPS 좌표는 소수점 7자리까지 정밀 → 매번 미세하게 다름

```
37.49790001, 127.02760001  → 캐시 miss
37.49790002, 127.02760002  → 캐시 miss
37.49790003, 127.02760003  → 캐시 miss
```

**2. 소수점 반올림**

```kotlin
key = "${round(lat, 3)}:${round(lng, 3)}"  // 37.498:127.028
```

```
User A: 37.4975 → 반올림 → 37.498
User B: 37.4984 → 반올림 → 37.498  캐시 히트
User C: 37.4985 → 반올림 → 37.499  캐시 miss 
```

37.4984와 37.4985는 1m 차이지만 다른 캐시 키를 갖는다.

---

## 캐시 키 설계


- **근접 좌표는 같은 키** (캐시 히트 ↑)
- **멀리 떨어진 좌표는 다른 키** (정확도 ↑)


| 방법 | 캐시 히트율 | 경계 문제 | 계산 비용 | 정확도 |
|-----|----------|---------|---------|--------|
| 원본 좌표 | 3% | 없음 | 낮음 | 완벽 |
| 반올림 | 15% | **심각** | 낮음 | 낮음 |
| Grid 분할 | 45% | 있음 | 낮음 | 중간 |
| **Geohash** | **72%** | **최소화** | **중간** | **높음** |
| H3 (Uber) | 78% | 최소화 | 높음 | 높음 |

**Geohash**
- H3보다 히트율은 낮지만, 구현이 단순하고 계산 비용 낮음
- Precision 조절로 grid size 제어 가능

---

## Geohash 알고리즘

### 작동 원리

Geohash는 지구를 **재귀적으로 4분할**하여 각 영역에 문자를 할당한다.

**Example: 강남역 (37.4979, 127.0276)**

```
1. 세계를 2분할 (위도 기준)
   -90~0: 0
   0~90:  1
   → 37.4979는 0~90 → "1"

2. 2분할 (경도 기준)
   -180~0: 0
   0~180:  1
   → 127.0276은 0~180 → "1"

3. 다시 위도 2분할
   0~45:   0
   45~90:  1
   → 37.4979는 0~45 → "0"

...반복...

최종: "1110010" (binary) → "wydjk" (base32)
```



### Precision별 Grid Size

| Precision | Grid 크기 | 면적 | 서울 기준 grid 수 |
|-----------|----------|------|----------------|
| 4 | 39km × 19km | 741 km² | 1개 |
| 5 | 4.9km × 4.9km | 24 km² | 26개 |
| **6** | **1.2km × 0.6km** | **0.72 km²** | **~850개** |
| 7 | 153m × 153m | 0.023 km² | ~27,000개 |

**왜 Precision 6인가?**


```kotlin
// 강남역 기준 테스트
val center = Location(37.4979, 127.0276)
val testPoints = generateRandomPointsWithinRadius(center, 1000m, 100개)

// Precision 5: 4.9km grid
geohash5 = testPoints.map { encode(it, 5) }.distinct()
// 결과: ["wydj7", "wydj5", "wydjk", "wydjh"]  → 4개 grid
// 문제: grid가 너무 커서 5km 떨어진 카페도 같은 키

// Precision 6: 1.2km grid
geohash6 = testPoints.map { encode(it, 6) }.distinct()
// 결과: ["wydjk8", "wydjkb"]  → 2개 grid
// 1km 반경 내 카페가 주로 1~2개 grid에 집중

// Precision 7: 153m grid
geohash7 = testPoints.map { encode(it, 6) }.distinct()
// 결과: ["wydjk8g", "wydjk8u", "wydjk8v", ...]  → 15개 grid
// 문제: grid가 너무 작아서 캐시 히트율 낮음
```

**Precision 6**
- 1km 반경 검색에 최적
- 대부분의 요청이 1~2개 geohash 내에 위치
- 캐시 히트율과 정확도의 균형점

---

## 성능 측정

### 캐시 히트율

**테스트 시나리오**
- 강남역 기준 1km 내에서 랜덤 위치 생성
- 1,000명의 가상 사용자가 검색
- 반경: 500m, 1km, 2km 

```kotlin
@Test
fun `geohash cache hit rate test`() {
    val center = Location(37.4979, 127.0276)
    val requests = generateRandomRequests(center, 1000개)

    var cacheHits = 0
    var cacheMisses = 0

    requests.forEach { request ->
        val result = cafeService.searchNearby(request)
        if (result.fromCache) cacheHits++ else cacheMisses++
    }

    val hitRate = cacheHits.toDouble() / requests.size
    println("Cache hit rate: ${hitRate * 100}%")
}
```

**결과**

| 방법 | 히트율 | 평균 응답 시간 | Query Count |
|-----|--------|-------------|-------------|
| 캐시 없음 | 0% | 250ms | 1,000       |
| 단순 key | 3% | 242ms | 970         |
| 반올림 | 15% | 213ms | 850         |
| Grid (5x5) | 45% | 140ms | 550         |
| **Geohash p6** | **72%** | **75ms** | **280**     |
| Geohash p7 | 38% | 158ms | 620         |

**분석**
- Geohash precision 6에서 최적 히트율
- DB 쿼리 70% 감소 → 인프라 비용 절감

### 응답 시간 분포

```
캐시 히트 시:
  min: 0.3ms
  p50: 0.5ms
  p95: 1.2ms
  p99: 2.5ms
  max: 5.1ms

캐시 미스 시:
  min: 180ms
  p50: 250ms
  p95: 420ms
  p99: 680ms
  max: 1,200ms
```

### 동시 요청 부하 테스트

```bash
# Apache Bench
ab -n 10000 -c 100 "http://localhost:8080/api/v1/cafes/search?lat=37.4979&lng=127.0276&radius=1000&time=14:00"
```

**캐시 없음**
```
Requests per second: 42.3 [#/sec]
Time per request: 2,364ms (mean, across all concurrent requests)
Failed requests: 23 (connection timeout)
```

**Geohash 캐시**
```
Requests per second: 1,247.8 [#/sec]  (29.5배 향상)
Time per request: 80ms (mean)
Failed requests: 0
```

---

## Trade-off

### 경계 문제 

Geohash도 경계 문제가 완전히 해결되지 않는다

```
┌──────────┬──────────┐
│  wydjk7  │  wydjk8  │  ← Geohash 경계
│          │     •A   │
│        B•│          │
└──────────┴──────────┘

A: (37.4979, 127.0276) → wydjk8
B: (37.4978, 127.0275) → wydjk7  (50m 차이)
```

A와 B는 50m밖에 안 떨어졌는데 다른 geohash → 캐시 미스

**해결 방법**

1. **이웃 geohash 검색** (시도했으나 복잡도 증가)
```kotlin
val neighbors = getNeighborGeohashes(geohash)  // 8개
// 문제: Redis에서 9번 조회 → 오버헤드
```

2. **Overlapping grid** (구현 복잡)
```kotlin
// geohash를 offset하여 2개 생성
// 문제: 캐시 중복 저장 → 메모리 2배
```

3. **현재 전략: 무시**
- 경계 케이스는 전체의 ~10%
- 캐시 미스여도 250ms면 acceptable
- 복잡도 vs 성능 gain이 맞지 않음

### Radius 반올림 문제

```kotlin
val roundedRadius = (request.radius / 100) * 100

// 999m → 900m로 반올림
// 1001m → 1000m로 반올림
```

- 999m 검색 → 900m 캐시 사용 → 99m 차이의 카페 누락 가능
- 실제 테스트: 999m vs 900m → 평균 0.3개 카페 차이 (20개 중)

**허용 가능한 이유**
- 사용자는 정확한 반경보다 "근처"에 관심
- 0.3개 차이는 사용자 경험에 영향 없음
- 캐시 히트율 20% 향상 


---

