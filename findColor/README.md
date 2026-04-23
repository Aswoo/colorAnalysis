# findColor — Backend

Spring Boot 기반의 이미지 색상 분석 REST API 서버입니다.  
사용자가 업로드한 이미지에서 OpenCV(JavaCV)를 활용해 HSV 색공간 분석을 수행하고, 타겟 색상과의 유사도를 계산합니다.

---

## Tech Stack

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.4 |
| Database | MySQL 8 |
| ORM | Spring Data JPA (Hibernate) |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Image Processing | JavaCV / OpenCV 1.5.10 |
| Storage | AWS S3 (SDK v2) |
| Build | Maven |

---

## Architecture

```
src/main/java/com/example/findcolor/
├── controller/          # REST API 엔드포인트
│   ├── AuthController           # 회원가입/로그인
│   ├── MissionImageController   # 이미지 업로드 및 분석 상태 조회
│   └── AnalysisController       # 분석 히스토리
├── service/             # 비즈니스 로직
│   ├── MissionService           # 미션(분석 요청) 처리
│   ├── ColorAnalysisService     # OpenCV 색상 분석 (비동기)
│   ├── S3Service                # AWS S3 업로드
│   └── UserService              # 사용자 관리
├── security/            # JWT 인증/인가
│   ├── JwtUtil                  # 토큰 생성·검증
│   ├── CustomLoginFilter        # 로그인 처리 필터
│   └── JwtAuthorizationFilter   # 요청별 토큰 검증 필터
├── entity/              # JPA 엔티티
├── dto/                 # 요청/응답 DTO
├── repository/          # Spring Data JPA Repository
├── exception/           # 글로벌 예외 처리
└── config/              # Security, S3 설정
```

### 색상 분석 흐름 (비동기)

```
클라이언트 이미지 업로드
    → S3 업로드 (원본 저장)
    → AnalysisRequest 생성 (PROCESSING 상태)
    → @Async ColorAnalysisService 실행
        → JavaCV로 HSV 변환 및 픽셀 분석
        → ColorType Enum 기준 유사도 계산
        → AnalysisResult 저장 (COMPLETED / FAILED)
클라이언트 폴링 (2.5초 간격)
    → GET /api/images/analysis/{requestId}
    → 상태 및 결과 반환
```

---

## API Endpoints

### Auth

| Method | URL | Description | Auth |
|--------|-----|-------------|------|
| POST | `/api/auth/signup` | 회원가입 | 불필요 |
| POST | `/api/auth/login` | 로그인 (JWT 발급) | 불필요 |

**회원가입 요청**
```json
{
  "username": "user1",
  "password": "password123"
}
```

**로그인 응답** — Response Header에 `Authorization: Bearer <token>` 포함

---

### Image Analysis

| Method | URL | Description | Auth |
|--------|-----|-------------|------|
| POST | `/api/images/perform` | 이미지 업로드 및 분석 요청 | 불필요 |
| GET | `/api/images/analysis/{requestId}` | 분석 상태 폴링 | 불필요 |

**POST `/api/images/perform`** — `multipart/form-data`
- `request` (JSON part): `{ "userId": 1, "targetSentiment": "Green" }`
- `file` (Binary part): 이미지 파일 (최대 10MB)

**응답**
```json
{
  "requestId": 42
}
```

**GET `/api/images/analysis/{requestId}?targetSentiment=Green`**
```json
{
  "status": "COMPLETED",
  "matched": true,
  "similarityScore": 87.3,
  "colorPalettes": [
    { "hex": "#4CAF50", "ratio": 0.42 },
    { "hex": "#388E3C", "ratio": 0.28 }
  ]
}
```

---

### Analysis History

| Method | URL | Description | Auth |
|--------|-----|-------------|------|
| GET | `/api/analysis/history` | 분석 히스토리 (페이지네이션) | JWT 필요 |

Query: `?page=0&size=20`

---

## Design Decisions

### 왜 RGB가 아닌 HSV인가?

색상 분석에 RGB를 쓰는 것은 직관적이지만, 치명적인 약점이 있습니다.

```
동일한 "녹색 잔디"라도:
  밝은 햇빛 아래  →  RGB (120, 180, 80)
  그늘 속         →  RGB (40,  80,  30)   ← 완전히 다른 값
  흐린 날         →  RGB (80,  120, 55)
```

RGB에서 빛의 세기는 R, G, B 세 채널 전부에 영향을 미치므로 동일한 색상이 전혀 다른 수치로 나타납니다.

HSV는 색의 본질을 세 가지 독립된 축으로 분리합니다.

| 축 | 의미 | 역할 |
|----|------|------|
| **H** (Hue) | 색상 자체 | "이것은 녹색이다" |
| **S** (Saturation) | 색의 진함 | "얼마나 선명한 녹색인가" |
| **V** (Value) | 밝기 | "밝은 곳인가, 어두운 곳인가" |

빛의 세기가 바뀌어도 H 값은 안정적으로 유지됩니다. 이 덕분에 그늘진 숲, 흐린 날의 하늘, 강한 조명 아래 꽃 등 **조명 조건이 다양한 실사 이미지에서도 색상을 신뢰할 수 있게 판별**할 수 있습니다.

```java
// ColorType.isMatch() — S, V가 너무 낮으면 무채색으로 간주하여 제외
if (s < 25 || v < 25) return false;
// 이후 H 값만으로 색상 판별 → 조명 변화에 강건
if (h >= minH && h <= maxH) return true;
```

---

### 왜 단순 매칭이 아닌 유사도 점수인가?

"해당 색이 있다 / 없다"는 이진 판별은 실용성이 낮습니다.  
붉은 계열 사진인데 타겟 색 범위 경계에 아슬아슬하게 걸쳐 있다면, 단순 True/False로는 그 차이가 보이지 않습니다.

이 프로젝트는 픽셀이 색상 범위 **중심에 얼마나 가까운지** + **채도가 얼마나 높은지**를 합산한 연속 점수를 사용합니다.

```
score = 100 - (H 중심에서의 거리 / 범위 × 30)   // 위치 기반 70~100점
      × (S / 255 × 0.2 + 0.8)                    // 채도 가중치
```

결과적으로 "87.3% 일치"처럼 정량적인 피드백을 제공할 수 있습니다.

---

### 왜 비동기 처리 + 폴링인가?

OpenCV 픽셀 분석은 CPU 집약적 작업입니다. 고해상도 이미지 한 장의 전체 픽셀을 순회하며 HSV 계산을 수행하면 수 초가 소요될 수 있습니다.

이를 HTTP 요청 스레드에서 동기로 처리하면:
- 요청 하나가 스레드를 수 초간 점유
- 동시 요청이 늘어날수록 스레드 풀 고갈
- 클라이언트는 응답 없이 수 초간 대기

`@Async`로 분석을 별도 스레드에 위임하면 요청 스레드는 즉시 `requestId`를 반환하고 해제됩니다. 클라이언트는 2.5초 간격 폴링으로 완료 여부를 확인하며, 서버는 처리 중에도 다른 요청을 받을 수 있습니다.

---

## Supported Colors

`ColorType` Enum으로 HSV 범위 정의. 지원 색상:

| 색상 | H 범위 (OpenCV 기준) |
|------|---------------------|
| RED | 0–15 또는 160–180 |
| PINK | 140–170 |
| YELLOW | 15–40 |
| GREEN | 35–95 |
| BLUE | 90–145 |
| PURPLE | 135–155 |

---

## Getting Started

### 사전 요구사항

- Java 17+
- MySQL 8.x
- AWS 계정 (S3 버킷)

### 1. 데이터베이스 생성

```sql
CREATE DATABASE findcolor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 환경변수 설정

프로젝트 루트에 `.env` 파일 생성:

```env
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key
```

### 3. `application.properties` 확인

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/findcolor?serverTimezone=Asia/Seoul
spring.datasource.username=root
spring.datasource.password=your_password

cloud.aws.s3.bucket=your-bucket-name
cloud.aws.region.static=ap-southeast-2
```

### 4. 실행

```bash
./mvnw spring-boot:run
```

서버 기동: `http://localhost:8080`

---

## Environment Variables

| 변수명 | 설명 |
|--------|------|
| `AWS_ACCESS_KEY` | AWS IAM 액세스 키 |
| `AWS_SECRET_KEY` | AWS IAM 시크릿 키 |

> `.env` 파일은 절대 git에 커밋하지 마세요. `.gitignore`에 포함되어 있습니다.
