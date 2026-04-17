# Backend Setup & JPA Entity Design Guide

이 문서는 주니어 개발자를 위해 프로젝트의 백엔드 설정 및 JPA 엔티티 설계의 핵심 개념과 이유를 설명합니다.

---

## 1. 프로젝트 설정 및 의존성

### **의존성 (pom.xml)**
*   **Spring Data JPA**: 자바 객체와 데이터베이스 테이블을 연결해주는 표준 인터페이스입니다. 반복적인 SQL 작성 없이 데이터를 관리할 수 있게 해줍니다.
*   **MySQL Driver**: 자바 애플리케이션이 실제 MySQL 데이터베이스 서버와 통신할 수 있게 해주는 필수 라이브러리입니다.
*   **Lombok**: `@Getter`, `@Setter` 등을 사용하여 반복적인 코드를 줄여주는 생산성 도구입니다.

### **설정 (application.properties)**
*   **`spring.jpa.hibernate.ddl-auto=update`**: 
    *   **이유**: 자바 코드(엔티티)의 구조가 바뀌면 DB 테이블 구조를 자동으로 업데이트합니다. 개발 초기 단계에서 테이블을 일일이 수정하는 번거로움을 줄여줍니다.
*   **`spring.jpa.show-sql=true`**: 
    *   **이유**: JPA가 내부적으로 생성하는 SQL 쿼리를 콘솔에 출력합니다. 의도한 대로 쿼리가 날아가는지 확인하고 학습하는 데 큰 도움이 됩니다.

---

## 2. JPA 엔티티(Entity) 설계 핵심

엔티티는 **데이터베이스의 테이블과 1:1로 매칭되는 클래스**입니다.

### **주요 어노테이션**
*   **`@Entity`**: 이 클래스가 JPA 엔티티임을 선언합니다.
*   **`@Id`, `@GeneratedValue`**: 테이블의 기본키(PK)를 지정합니다. `IDENTITY` 전략은 MySQL이 번호를 자동으로 하나씩 올려가며 부여하게 합니다.
*   **`@CreationTimestamp`**: 데이터가 생성된 시간을 자동으로 기록합니다. 별도의 코드 없이 생성 시점을 추적할 수 있습니다.

---

## 3. 연관 관계 설계 (Relationships)

객체 간의 관계를 맺어주는 것이 JPA의 가장 중요한 부분입니다.

### **1. 일대일 관계 (1:1)**
*   **대상**: `AnalysisRequest` ↔ `AnalysisResult`
*   **이유**: 하나의 분석 요청에는 반드시 하나의 분석 결과만 존재하기 때문입니다.
*   **설정**: `@OneToOne`을 사용하여 연결합니다.

### **2. 다대일 관계 (N:1)**
*   **대상**: `User` ↔ `Image`, `User` ↔ `AnalysisRequest` 등
*   **이유**: 한 명의 사용자가 여러 장의 사진을 올리거나 여러 번의 분석을 요청할 수 있기 때문입니다.
*   **최적화 (`FetchType.LAZY`)**: 
    *   데이터를 조회할 때 연관된 데이터를 즉시 가져오지 않고, **실제로 필요할 때(메서드 호출 시)** 가져옵니다. 불필요한 데이터 로딩을 줄여 서버 성능을 높이는 핵심 기법입니다.

### **3. 양방향 관계와 `mappedBy`**
*   관계의 주인(실제 DB 외래키를 가진 쪽)이 아닌 반대쪽에서 참조를 위해 사용합니다. 
*   예: `User` 클래스에서 `mappedBy = "user"`를 쓰면, "나는 관계의 주인이 아니며, 저쪽(`Image`)에 있는 `user` 필드에 의해 관리된다"는 뜻입니다.

---

## 4. 코드 예시 분석 (`AnalysisRequest`)

```java
@Entity
public class AnalysisRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩으로 성능 최적화
    @JoinColumn(name = "user_id") // 외래키 컬럼 이름 지정
    private User user;

    @OneToOne(mappedBy = "analysisRequest", cascade = CascadeType.ALL) 
    // 부모(Request)가 삭제되면 자식(Result)도 함께 삭제되도록 설정
    private AnalysisResult analysisResult;
}
```

---

## 5. 주니어 개발자를 위한 다음 단계

1.  **Repository 생성**: 엔티티를 이용해 DB에 데이터를 저장/조회하는 인터페이스를 만듭니다.
2.  **Service 개발**: "이미지 분석 요청을 받고 결과를 저장한다"와 같은 비즈니스 로직을 구현합니다.
3.  **DTO (Data Transfer Object) 활용**: 엔티티 클래스를 직접 외부에 노출하지 않고, 통신용 객체를 따로 만들어 데이터를 주고받는 습관을 들여보세요.
