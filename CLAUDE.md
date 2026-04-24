# findColor — Claude Code Guidelines

## Project Overview

Spring Boot 3 + React 19 모노레포.
이미지에서 HSV 기반 색상 분석을 수행하는 서비스.

- **Backend**: `findColor/` — Spring Boot 3.4, Java 17, MySQL, Spring Security + JWT, JavaCV/OpenCV, AWS S3
- **Frontend**: `findColorFE/` — React 19, TypeScript, Vite, Tailwind CSS v4

---

## Workflow Rules

### 계획 먼저
3단계 이상이거나 아키텍처 결정이 포함된 작업은 **반드시 계획을 먼저 작성**하고 승인을 받은 뒤 구현한다.

### 완료 기준
작동한다는 것을 증명하기 전까지 작업을 완료로 표시하지 않는다.
- 백엔드: `./mvnw test` 통과
- 핵심 로직 변경: 관련 테스트 케이스 추가 또는 수정

### 실수 반복 방지
수정 지시를 받으면 같은 실수를 반복하지 않도록 해당 패턴을 기억한다.

---

## Code Standards

### 공통
- 변경은 최소한으로. 요청된 것만 수정한다
- 근본 원인을 찾는다. 임시방편 금지
- 단순함 우선. 불필요한 추상화 금지

### Backend (Spring Boot)
- Java 17+ 기능 적극 활용 (record, sealed class, pattern matching)
- `@Transactional`은 Service 레이어에만
- Repository는 단순 데이터 접근만. 비즈니스 로직 금지
- Controller는 요청/응답 변환만. 비즈니스 로직은 Service로
- 모든 외부 입력은 DTO로 받고 Entity를 직접 노출하지 않는다
- CPU 집약적 작업(`ColorAnalysisService` 등)은 `@Async` 유지
- 예외는 `GlobalExceptionHandler`에서 중앙 처리

### 테스트
- 신규 비즈니스 로직에는 **정상 케이스 + 경계값 + 실패 케이스** 모두 작성
- 단위 테스트는 Spring Context 없이 (`@SpringBootTest` 최소화)
- Mockito로 외부 의존성(S3, DB) 격리

### Frontend (React + TypeScript)
- `any` 타입 금지. 모든 API 응답은 `types/` 에 인터페이스 정의
- API 호출은 `services/` 레이어에서만
- 컴포넌트는 UI만 담당. 비즈니스 로직은 서비스/훅으로 분리

---

## Architecture

```
findColor/src/main/java/com/example/findcolor/
├── controller/   # HTTP 요청/응답만
├── service/      # 비즈니스 로직, @Transactional
├── repository/   # DB 접근만
├── entity/       # JPA 엔티티
├── dto/          # 요청/응답 DTO
├── security/     # JWT 필터, UserDetails
├── exception/    # GlobalExceptionHandler
└── config/       # SecurityConfig, S3Config
```

### 핵심 설계 원칙
- **인증**: `CustomLoginFilter` (로그인) + `JwtAuthorizationFilter` (인가) — 필터 레이어에서 처리
- **색상 분석**: K-means(K=8) + HSV 변환. `ColorType` Enum이 유일한 범위 정의 소스
- **비동기**: 이미지 분석은 `@Async`, 클라이언트는 2.5초 폴링

---

## Key Files

| 파일 | 역할 |
|------|------|
| `entity/ColorType.java` | HSV 색상 범위 + 유사도 계산 공식 |
| `service/ColorAnalysisService.java` | OpenCV K-means 분석 (비동기) |
| `security/JwtUtil.java` | JWT 생성·검증 (HS256, 24h) |
| `config/SecurityConfig.java` | 필터 체인 조립 |
| `exception/GlobalExceptionHandler.java` | 전역 예외 → ErrorResponse 변환 |

---

## Environment Variables

`.env` 파일 필요 (절대 커밋 금지):
```
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
JWT_SECRET_KEY=   # Base64 인코딩된 HS256 시크릿
```
