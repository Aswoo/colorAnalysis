# findColor — Frontend

React + TypeScript 기반의 이미지 색상 분석 웹 클라이언트입니다.  
이미지를 업로드하고 타겟 색상을 선택하면 백엔드 API와 통신해 분석 결과를 실시간으로 표시합니다.

---

## Tech Stack

| 분류 | 기술 |
|------|------|
| Language | TypeScript 6 |
| Framework | React 19 |
| Build Tool | Vite 5 |
| Routing | React Router v6 |
| Styling | Tailwind CSS v4 |
| HTTP Client | Axios |
| Icons | Lucide React |

---

## Pages

| 경로 | 컴포넌트 | 설명 |
|------|----------|------|
| `/login` | `Login.tsx` | 로그인 |
| `/signup` | `Signup.tsx` | 회원가입 |
| `/` | `Home.tsx` | 이미지 업로드 및 색상 분석 |
| `/history` | `History.tsx` | 분석 히스토리 조회 |

---

## Project Structure

```
src/
├── pages/
│   ├── Login.tsx        # 로그인 폼
│   ├── Signup.tsx       # 회원가입 폼
│   ├── Home.tsx         # 메인 분석 화면
│   └── History.tsx      # 히스토리 목록
├── services/
│   ├── apiClient.ts     # Axios 인스턴스 (baseURL, 인터셉터)
│   ├── authService.ts   # 로그인/로그아웃/토큰 관리
│   └── analysisService.ts # 이미지 업로드 및 분석 상태 폴링
└── types/
    ├── auth.ts          # User, LoginRequest 등 인증 타입
    └── analysis.ts      # AnalysisStatusResponse, ColorPalette 등
```

---

## Key Features

### 이미지 최적화 (클라이언트 사이드)
업로드 전 Canvas API로 이미지를 자동 리사이즈/압축합니다.
- 최대 해상도: 1200 × 1200px
- 포맷: JPEG (quality 0.8)

### 비동기 분석 폴링
분석은 서버에서 비동기로 처리되므로, 요청 접수 후 2.5초 간격으로 상태를 폴링합니다.

```
performAnalysis() → requestId 수신
    ↓
setInterval(2500ms)
    ↓
getAnalysisStatus(requestId) 반복 호출
    ↓
status === 'COMPLETED' | 'FAILED' → 폴링 종료, 결과 렌더링
```

### 지원 색상

`Home.tsx`에서 선택 가능한 타겟 색상:

`Red` / `Green` / `Blue` / `Yellow` / `Purple` / `Pink`

---

## Design Decisions

### 왜 업로드 전에 클라이언트에서 이미지를 압축하는가?

스마트폰으로 찍은 사진은 대부분 3000~6000px 이상이고 파일 크기는 5–15MB에 달합니다.  
이를 그대로 서버로 전송하면 두 가지 문제가 생깁니다.

1. **네트워크 대기 시간** — 모바일 환경에서 10MB 업로드는 체감상 느림
2. **분석 불필요한 정밀도** — OpenCV HSV 분석은 색상 분포를 보는 것이므로, 1200px 이하에서도 결과가 동일함

Canvas API로 리사이즈 + JPEG 0.8 압축을 적용하면 평균 파일 크기를 **원본의 5–10%** 수준으로 줄이면서 분석 정확도는 그대로 유지됩니다.

```
6000×4000px, 12MB 원본
    ↓  Canvas 리사이즈 (max 1200px) + JPEG 0.8
1200×800px, ~600KB   →  전송 속도 20배 향상, 분석 결과 동일
```

---

### 왜 WebSocket이 아닌 폴링인가?

분석이 비동기인 이상, 완료 시점을 클라이언트에 전달하는 방법은 두 가지입니다.

| 방식 | 장점 | 단점 |
|------|------|------|
| **WebSocket** | 즉각적인 푸시 알림 | 연결 유지 비용, 서버/클라이언트 모두 추가 구현 필요 |
| **폴링 (2.5s)** | 구현 단순, 무상태 HTTP 유지 | 최대 2.5초 지연 |

색상 분석은 보통 **3–8초** 내에 완료됩니다. 2.5초 간격 폴링이면 체감 지연은 0–2.5초로 사용자 경험상 충분하며, 서버 측에서 별도 연결 관리가 필요 없어 아키텍처가 단순하게 유지됩니다.

---

## Getting Started

### 사전 요구사항

- Node.js 18+
- npm
- 백엔드 서버 실행 중 (`localhost:8080`)

### 1. 의존성 설치

```bash
npm install
```

### 2. API 엔드포인트 확인

`src/services/apiClient.ts`에서 `baseURL`이 백엔드 주소와 일치하는지 확인합니다.

```ts
const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
});
```

### 3. 개발 서버 실행

```bash
npm run dev
```

브라우저: `http://localhost:5173`

### 4. 프로덕션 빌드

```bash
npm run build
```

빌드 결과물은 `dist/` 디렉토리에 생성됩니다.

---

## Available Scripts

| 스크립트 | 설명 |
|----------|------|
| `npm run dev` | 개발 서버 실행 (HMR) |
| `npm run build` | 프로덕션 빌드 |
| `npm run preview` | 빌드 결과물 미리보기 |
| `npm run lint` | ESLint 정적 분석 |
