# Spring Data JPA Repository Guide

이 문서는 프로젝트에서 데이터베이스에 접근하기 위해 생성한 **Repository** 계층의 역할과 사용법을 설명합니다.

---

## 1. Repository란 무엇인가?

**Repository**는 데이터베이스의 데이터에 접근하는 객체(DAO, Data Access Object)의 현대적인 형태입니다. 
Spring Data JPA 덕분에 인터페이스를 정의하는 것만으로도 복잡한 SQL 쿼리 없이 데이터를 저장, 수정, 삭제, 조회할 수 있습니다.

### **핵심 구성**
```java
public interface UserRepository extends JpaRepository<User, Long> { ... }
```
*   **`JpaRepository<User, Long>`**: 
    *   첫 번째 파라미터(`User`): 이 레포지토리가 다룰 **엔티티** 클래스입니다.
    *   두 번째 파라미터(`Long`): 해당 엔티티의 **기본키(@Id)** 타입입니다.
*   **상속의 이점**: `save()`, `findById()`, `findAll()`, `deleteById()`, `count()` 등의 공통 메서드를 코딩 없이 바로 사용할 수 있습니다.

---

## 2. 생성된 Repository 상세 설명

### **1. UserRepository**
*   **특징**: `findByEmail(String email)` 메서드가 추가되었습니다.
*   **용도**: 회원 가입 시 이메일 중복 체크를 하거나, 로그인 시 사용자 정보를 가져올 때 사용합니다.

### **2. ImageRepository**
*   **특징**: `findByUserId(Long userId)` 메서드가 추가되었습니다.
*   **용도**: 특정 사용자가 업로드한 모든 이미지 목록을 불러올 때 사용합니다.

### **3. AnalysisRequestRepository**
*   **특징**: 
    *   `findByUserId(Long userId)`: 사용자의 전체 분석 히스토리를 조회합니다.
    *   `findByUserIdAndIsFavoriteTrue(Long userId)`: 사용자가 '즐겨찾기' 표시한 분석 요청만 따로 모아볼 때 사용합니다.
*   **용도**: 서비스의 핵심 기능인 '분석 기록 조회'와 '즐겨찾기 관리'를 담당합니다.

### **4. TargetColor / DetectedColor Repository**
*   **특징**: `findByAnalysisRequestId(Long analysisId)` 메서드가 추가되었습니다.
*   **용도**: 특정 분석 요청(Request)에 대해 사용자가 선택했던 색상(Target)과 시스템이 추출한 색상(Detected)을 각각 조회할 때 사용합니다.

---

## 3. 주니어 개발자를 위한 JPA 꿀팁

### **1. 쿼리 메서드 (Query Methods)**
메서드 이름을 규칙에 맞춰 지으면 JPA가 알아서 SQL로 변환해줍니다.
*   `findBy...`: 조회 (SELECT)
*   `countBy...`: 개수 확인 (COUNT)
*   `existsBy...`: 존재 여부 확인 (EXISTS)
*   `deleteBy...`: 삭제 (DELETE)

**예시**: `findByEmailAndStatus(String email, String status)`라고 지으면, 두 조건이 모두 일치하는 데이터를 찾습니다.

### **2. Optional의 활용**
`findByEmail`의 반환 타입이 `Optional<User>`인 이유:
*   조회 결과가 **있을 수도 있고 없을 수도 있기 때문**입니다.
*   `.orElseThrow()` 등을 사용하여 결과가 없을 때 깔끔하게 예외 처리를 할 수 있어 `null` 체크 지옥에서 벗어날 수 있습니다.

---

## 4. 실제 사용 예시 (Service 계층에서)

```java
@Service
@RequiredArgsConstructor // Repository를 자동으로 주입(Injection)해줍니다.
public class UserService {
    private final UserRepository userRepository;

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
}
```

---

## 5. 다음 학습 제안

Repository가 준비되었으니, 이제 이 데이터들을 가공하고 비즈니스 로직을 담는 **Service** 계층을 만들어 보는 것을 추천합니다. 
Service는 Repository를 사용하여 DB에서 데이터를 가져온 뒤, 필요한 계산이나 처리를 수행하는 곳입니다.
