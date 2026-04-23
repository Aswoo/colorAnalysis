# Authentication

Spring Security + JWT 기반 인증 시스템입니다.  
로그인은 Security Filter Chain에서 처리하고, 이후 요청은 JWT 토큰으로 인가합니다.

---

## 1. 전체 흐름

### 회원가입

```
POST /api/auth/signup
    → AuthController.signup()
    → UserService.signup()
        → BCryptPasswordEncoder.encode(rawPassword)
        → User 저장 (해시된 비밀번호만 DB에 기록)
    → 200 OK { id, email, nickname }
```

### 로그인

```
POST /api/auth/login
    → CustomLoginFilter.attemptAuthentication()
        → JSON 파싱 → UsernamePasswordAuthenticationToken 생성
        → AuthenticationManager.authenticate()
            → DaoAuthenticationProvider
                → UserDetailsServiceImpl.loadUserByUsername(email)
                → BCrypt로 비밀번호 대조
    ├─ 성공 → SuccessHandler
    │           → JwtUtil.createToken(email)
    │           → Response Header: Authorization: Bearer <token>
    │           → 200 OK { id, email, nickname }
    └─ 실패 → FailureHandler
                → 401 { status: 401, message: "아이디 또는 비밀번호가 잘못되었습니다." }
```

### 인가 (JWT 검증)

```
모든 요청
    → JwtAuthorizationFilter (OncePerRequestFilter)
        → Authorization 헤더에서 "Bearer " 접두사 제거
        → JwtUtil.validateToken()
        → JwtUtil.getUserInfoFromToken() → email 추출
        → UserDetailsServiceImpl.loadUserByUsername(email)
        → SecurityContextHolder에 Authentication 저장
    → 이후 필터/컨트롤러에서 @AuthenticationPrincipal로 유저 접근 가능
```

> 토큰이 없거나 유효하지 않으면 SecurityContext에 저장하지 않고 다음 필터로 넘깁니다.  
> 현재 모든 엔드포인트가 `permitAll()`이므로 토큰 없이도 접근은 가능하지만,  
> `@AuthenticationPrincipal`이 필요한 API(`GET /api/analysis/history`)는 실질적으로 JWT가 필요합니다.

---

## 2. 컴포넌트 구조

```
security/
├── CustomLoginFilter        # 로그인 필터 — JSON 파싱 및 인증 위임
├── JwtAuthorizationFilter   # 인가 필터 — 매 요청마다 토큰 검증
├── JwtUtil                  # 토큰 생성 / 검증 / 클레임 추출
├── UserDetailsImpl          # UserDetails 구현체 — User 엔티티 래핑
└── UserDetailsServiceImpl   # email로 DB 조회, UserDetails 반환

config/
└── SecurityConfig           # 필터 체인 조립, Handler 설정
```

### 필터 등록 순서

```
JwtAuthorizationFilter  →  CustomLoginFilter  →  ...
     (인가)                    (로그인)
```

`JwtAuthorizationFilter`를 `CustomLoginFilter` 앞에 배치합니다.  
로그인 요청(`/api/auth/login`)은 `CustomLoginFilter`가 가로채서 처리하고 직접 응답하므로 컨트롤러에 도달하지 않습니다.

---

## 3. JWT 스펙

| 항목 | 값 |
|------|----|
| 알고리즘 | HS256 |
| 만료 시간 | 24시간 |
| 헤더 키 | `Authorization` |
| 접두사 | `Bearer ` |
| 클레임 Subject | 이메일 |
| 시크릿 키 출처 | `.env` → `JWT_SECRET_KEY` (Base64 인코딩) |

---

## 4. 에러 응답 규격

모든 인증 오류는 `GlobalExceptionHandler` 또는 FailureHandler를 통해 동일한 형식으로 반환합니다.

```json
{
  "status": 401,
  "message": "아이디 또는 비밀번호가 잘못되었습니다."
}
```

| 상황 | 상태 코드 | 메시지 |
|------|-----------|--------|
| 아이디/비밀번호 불일치 | 401 | 아이디 또는 비밀번호가 잘못되었습니다. |
| 중복 이메일 가입 | 400 | 이미 존재하는 이메일입니다. |
| 유효하지 않은 JWT | — | 필터에서 차단, 응답 없이 체인 종료 |
| 서버 내부 오류 | 500 | 서버 내부 오류가 발생했습니다. |

---

## 5. SecurityConfig 설정 요약

```java
.csrf(csrf -> csrf.disable())
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
```

- **CSRF 비활성화**: REST API 서버는 쿠키 기반 세션을 사용하지 않으므로 불필요
- **Stateless 세션**: JWT 기반이므로 서버에 세션을 저장하지 않음
- **CORS**: `http://localhost:5173` 허용, `Authorization` 헤더 노출 설정

---

## 6. API

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | `/api/auth/signup` | 불필요 | 회원가입 |
| POST | `/api/auth/login` | 불필요 | 로그인 — 필터에서 처리, 컨트롤러 없음 |

**회원가입 요청**
```json
{ "email": "user@example.com", "password": "password123", "nickname": "user01" }
```

**로그인 성공 응답**
```
Header: Authorization: Bearer eyJhbGci...

Body: { "id": 1, "email": "user@example.com", "nickname": "user01" }
```
