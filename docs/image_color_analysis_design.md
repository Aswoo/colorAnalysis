# Image Color Analysis System Design (with User History)

## 1. Architecture

```mermaid
graph TD
    A[Frontend] --> B[Backend API]
    B --> C[Image Storage S3]
    B --> D[DB]
    B --> E[Analysis Service]

    E --> F[Color Extraction]
    E --> G[Similarity Calculation]
```

------

## 2. ERD (User History Included)

```mermaid
erDiagram
    USER ||--o{ IMAGE : uploads
    USER ||--o{ ANALYSIS_REQUEST : owns

    IMAGE ||--o{ ANALYSIS_REQUEST : has
    ANALYSIS_REQUEST ||--o{ TARGET_COLOR : wants
    ANALYSIS_REQUEST ||--o{ DETECTED_COLOR : produces
    ANALYSIS_REQUEST ||--|| ANALYSIS_RESULT : result

    USER {
        bigint id PK
        string email
        datetime created_at
    }

    IMAGE {
        bigint id PK
        bigint user_id FK
        string image_url
        datetime created_at
    }

    ANALYSIS_REQUEST {
        bigint id PK
        bigint user_id FK
        bigint image_id FK
        string status
        datetime created_at
        boolean is_favorite
    }

    TARGET_COLOR {
        bigint id PK
        bigint analysis_id FK
        string color_hex
    }

    DETECTED_COLOR {
        bigint id PK
        bigint analysis_id FK
        string color_hex
        float ratio
    }

    ANALYSIS_RESULT {
        bigint id PK
        bigint analysis_id FK
        float similarity_score
        boolean matched
    }
```

------

## 3. Sequence Diagram (Analysis + History)

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant B as Backend
    participant S as Storage
    participant A as Analysis Service
    participant DB as Database

    U->>F: 이미지 + 색상 선택
    F->>B: 분석 요청

    B->>S: 이미지 업로드
    B->>DB: AnalysisRequest 생성

    B->>A: 이미지 분석 요청
    A->>A: 색상 추출
    A->>A: 유사도 계산

    A-->>B: 결과 반환
    B->>DB: 결과 저장

    B-->>F: 분석 결과 응답
    F-->>U: 결과 표시

    U->>F: 히스토리 요청
    F->>B: GET /history
    B->>DB: user_id 기준 조회
    B-->>F: 히스토리 반환
```

------

## 4. Color Similarity Flow

```mermaid
flowchart TD
    A[Extracted Colors] --> B[Convert to LAB]
    C[Target Colors] --> D[Convert to LAB]

    B --> E[Distance Calculation ΔE]
    D --> E

    E --> F[Similarity Score 계산]
    F --> G[Threshold 비교]
    G --> H[Matched 여부 결정]
```

------

## 5. State Diagram

```

```

------

## 6. API Design

### 분석 요청

POST /api/images/analyze

### 히스토리 조회

GET /api/analysis/history?page=0&size=20

### 상세 조회

GET /api/analysis/{id}

------

## 7. Key Design Points

- AnalysisRequest 중심 설계
- user_id 기반 히스토리 조회 최적화
- 이미지 외부 저장 (S3)
- 색상 비교는 LAB 기반 ΔE 사용
- 비동기 처리 확장 가능
- 즐겨찾기(is_favorite) 지원

------

## 8. Summary

이미지 색상 분석 결과를 저장하고,
 유저별로 히스토리를 조회할 수 있는 확장 가능한 구조.
