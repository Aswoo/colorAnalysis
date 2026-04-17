# Spring Boot 아키텍처 및 전역 예외 처리 가이드

이 문서는 Spring Boot 애플리케이션의 요청 처리 흐름을 이해하고, 각 기능(로깅, 트랜잭션, 예외 처리 등)을 적절한 계층에 배치하기 위한 가이드를 제공합니다.

---

## 1. Spring 요청 처리 흐름 (Request Lifecycle)

유저의 요청이 들어와서 응답이 나가는 과정에서 거치는 주요 계층은 다음과 같습니다.

| 계층 (Layer) | 주요 역할 | 특징 |
| :--- | :--- | :--- |
| **Filter** | 서블릿 컨테이너 레벨의 공통 처리 | Spring Context 외부. CORS, 인증/인가, 로깅(Request/Response) |
| **Interceptor** | Spring Context 레벨의 공통 처리 | 컨트롤러 실행 전/후 처리. 세션 체크, 실행 시간 측정 |
| **AOP / Transaction** | 메서드 실행 전후의 부가 기능 | 프록시 기반 동작. 트랜잭션 관리, 특정 로직 로깅, 권한 검증 |
| **ControllerAdvice** | **전역 예외 처리 및 응답 변환** | 비즈니스 예외를 포착하여 JSON 응답으로 포맷팅 |

---

## 2. 계층별 역할 및 배치 가이드 (Responsibility Mapping)

"무엇을 어디에 넣어야 할지" 헷갈릴 때 아래 기준을 따릅니다.

### **2-1. 빈 초기화 및 설정 (Startup)**
- **위치**: `BeanPostProcessor (BPP)`, `@PostConstruct (init 메서드)`
- **용도**: 애플리케이션 로딩 시점에 빈이 제대로 생성됐는지 검증하거나, 특정 프록시를 씌우는 등 **설정** 관련 로직.
- **주의**: 유저의 매 요청마다 실행되는 로직을 여기에 두면 안 됩니다.

### **2-2. 데이터 원자성 (Database)**
- **위치**: `@Transactional` (Service Layer)
- **용도**: 데이터베이스 작업의 성공/실패를 하나의 단위로 묶어 관리(Rollback/Commit)할 때 사용합니다.

### **2-3. 공통 요청 처리 및 모니터링**
- **위치**: `Filter` 또는 `Interceptor`
- **용도**: "누가(IP), 어떤 URL을 호출했고, 얼마나 걸렸는가"와 같은 **인프라/성능 로깅**에 적합합니다.

### **2-4. 비즈니스 로직 에러 포장**
- **위치**: `@RestControllerAdvice` (Global Exception Handler)
- **용도**: 서비스 레이어에서 던진 예외(`RuntimeException` 등)를 클라이언트가 이해할 수 있는 **JSON 포맷**으로 변환합니다.

---

## 3. 전역 예외 처리 전략 (@RestControllerAdvice)

### **왜 사용하는가?**
1.  **관심사 분리**: 각 컨트롤러마다 `try-catch`를 남발하지 않고, 한곳에서 집중 관리합니다.
2.  **일관된 응답**: 어떤 에러가 발생해도 유저는 항상 동일한 구조의 에러 응답(`{code, message}`)을 받게 됩니다.
3.  **보안**: DB 에러 메시지 등 내부 시스템 정보가 유저에게 직접 노출되는 것을 방지합니다.

### **구현 예시 (ErrorResponse DTO 활용)**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getHttpStatus()).body(new ErrorResponse(e.getCode(), e.getMessage()));
    }
}
```

---

## 4. 로깅 전략 (Logging Strategy)

| 로깅 종류 | 권장 위치 | 설명 |
| :--- | :--- | :--- |
| **통합 로그** | Filter / Interceptor | 모든 HTTP 요청/응답 정보 기록 |
| **에러 로그** | @RestControllerAdvice | 예외 발생 시 스택 트레이스와 함께 기록 (디버깅용) |
| **비즈니스 로그** | Service Layer | 핵심 비즈니스 로직(예: 결제 완료, 분석 시작 등) 기록 |
| **성능 로그** | Interceptor / AOP | 특정 메서드의 실행 시간 측정 |

---

## 5. 요약 및 원칙

1.  **Filter/Interceptor**: 서비스 로직을 몰라도 되는 **공통 인프라** 영역.
2.  **Service/AOP**: 실제 비즈니스 가치가 발생하는 **도메인** 영역.
3.  **RestControllerAdvice**: 백엔드의 내부 사정을 유저에게 친절하게 설명하는 **번역가** 영역.
