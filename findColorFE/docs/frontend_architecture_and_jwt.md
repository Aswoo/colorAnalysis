# Frontend Architecture & JWT Implementation

이 문서는 `findColorFE` 프론트엔드 프로젝트의 구조와 JWT 기반 인증 시스템의 구현 방식을 설명합니다.

## 1. 프로젝트 구조 (Project Structure)

```text
src/
├── components/     # 공통 UI 컴포넌트
├── pages/          # 페이지 단위 컴포넌트 (Home, Login, Signup)
├── services/       # API 통신 및 비즈니스 로직
│   ├── apiClient.ts    # [핵심] Axios 인터셉터 설정
│   ├── authService.ts  # 인증(로그인/로그아웃) 관리
│   └── analysisService.ts # 이미지 분석 API
├── types/          # TypeScript 인터페이스 정의
└── utils/          # 유틸리티 함수
```

## 2. JWT 인증 시스템 구현 상세

### 2-1. 토큰 저장 및 관리 (`authService.ts`)
*   **로그인 시**: 서버 응답 헤더(`Authorization`)에서 토큰을 추출하여 브라우저의 `localStorage`에 저장합니다.
*   **로그아웃 시**: `localStorage`에 저장된 토큰과 유저 정보를 삭제합니다.

### 2-2. API 요청 자동화 (`apiClient.ts`)
모든 API 요청마다 헤더를 직접 설정하는 번거로움을 피하기 위해 **Axios Interceptor**를 사용합니다.

*   **Request Interceptor**: 모든 요청이 서버로 날아가기 직전에 실행됩니다. `localStorage`에 토큰이 있다면 `Authorization` 헤더를 자동으로 추가합니다.
*   **Response Interceptor**: 서버로부터 응답을 받은 직후 실행됩니다. 만약 응답 코드가 **401 (Unauthorized)**라면, 토큰이 만료된 것으로 판단하여 자동으로 로그아웃 처리 후 로그인 페이지로 이동시킵니다.

## 3. 이미지 분석 및 결과 시각화

### 3-1. 이미지 최적화 (`Home.tsx`)
*   사용자가 이미지를 업로드하기 전, 브라우저 단에서 **Canvas API**를 사용하여 JPEG로 변환 및 리사이징(최대 1200px)을 수행합니다.
*   이는 서버 부하를 줄이고 `.avif`, `.webp` 등 최신 포맷과의 호환성을 보장합니다.

### 3-2. 정량적 분석 시각화
*   서버에서 반환한 `colorPalettes` 데이터를 바탕으로 상위 8개 색상의 점유율을 그래프와 색상 칩으로 표시합니다.

## 4. 보안 고려사항 (Security)
*   **무상태성(Stateless)**: 서버 세션에 의존하지 않고 오직 클라이언트가 가진 토큰으로 본인을 증명합니다.
*   **CORS**: 백엔드 시큐리티 설정에서 `ExposedHeaders`를 허용하여 프론트엔드가 안전하게 토큰 헤더에 접근할 수 있게 설계되었습니다.
