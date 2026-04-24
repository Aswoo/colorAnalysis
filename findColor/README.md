# findColor — Backend

Spring Boot 기반의 이미지 색상 분석 REST API 서버입니다.  
사용자가 업로드한 이미지에서 OpenCV(JavaCV)를 활용해 Lab/LCH 색공간 분석을 수행하고, 타겟 색상과의 유사도를 계산합니다.

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
        → 150×150 리사이징
        → BGR → Lab 변환
        → K-means(K=8) 클러스터링 (Lab 공간)
        → LCH Hue 각도 기반 유사도 계산
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

## Supported Colors

`ColorType` Enum으로 LCH Hue 각도 기준 정의. 지원 색상:

| 색상 | 기준 Hue (°) | 허용 편차 (°) |
|------|-------------|-------------|
| RED | 40 | ±30 |
| PINK | 5 | ±25 |
| YELLOW | 85 | ±30 |
| GREEN | 130 | ±40 |
| BLUE | 285 | ±35 |
| PURPLE | 335 | ±40 |

---

## Design Decisions

### 왜 Lab/LCH 색공간인가?

RGB는 빛의 세기 변화가 R, G, B 세 채널 모두에 영향을 줍니다. HSV의 H 값은 조명에 안정적이지만, 명도(V)가 낮으면 같은 색도 무채색으로 분류되어 그림자 영역이 분석에서 탈락합니다.

Lab은 인간 시각에 선형적으로 대응하는 색공간으로, K-means를 Lab 공간에서 수행하면 "사람 눈에 같아 보이는 색"끼리 같은 클러스터로 묶입니다. 여기서 a, b 채널만으로 Hue 각도(`atan2(b, a)`)를 계산하면 L(명도)을 완전히 무시하므로, 밝은 노랑과 어두운 황갈색이 동일한 색으로 인식됩니다.

> 색상 분석 알고리즘의 상세 설명과 HSV → Lab ΔE → LCH 변천 과정은 아래 문서를 참고하세요.  
> - [색상 분석 로직](docs/color_analysis.md)  
> - [알고리즘 변천사](docs/color_analysis_evolution.md)

---

### 왜 단순 매칭이 아닌 유사도 점수인가?

"해당 색이 있다 / 없다"는 이진 판별은 실용성이 낮습니다. 이 프로젝트는 Hue 각도가 기준점에 **얼마나 가까운지**를 0~100 점수로 계산하고, 클러스터 비율과 밝기 가중치를 곱해 최종 판정합니다.

```
유사도 = 100 - (hueDist / maxHueDist) × 40   // 중심: 100점, 경계: 60점
matched = weightedScoreSum ≥ 15.0             // 조건 1: 절대 임계값
       && targetRatio × 2 > otherChromatic    // 조건 2: 유채색 중 dominant
```

---

### 왜 비동기 처리 + 폴링인가?

OpenCV K-means 분석은 CPU 집약적 작업입니다. `@Async`로 분석을 별도 스레드에 위임하면 요청 스레드는 즉시 `requestId`를 반환하고 해제됩니다. 클라이언트는 2.5초 간격 폴링으로 완료 여부를 확인하며, 서버는 처리 중에도 다른 요청을 받을 수 있습니다.

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
JWT_SECRET_KEY=your_base64_encoded_secret
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
| `JWT_SECRET_KEY` | Base64 인코딩된 HS256 시크릿 |

> `.env` 파일은 절대 git에 커밋하지 마세요. `.gitignore`에 포함되어 있습니다.
