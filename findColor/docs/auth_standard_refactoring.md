# Spring Security Standard Authentication Refactoring

이 문서는 `findColor` 프로젝트의 인증 로직을 Spring Security 표준 방식(`DaoAuthenticationProvider`)으로 리팩토링한 내용을 설명합니다.

## 1. 아키텍처 변화 (Architecture Change)

리팩토링 전에는 `UserService`가 직접 DB에서 유저를 조회하고 비밀번호를 비교했습니다. 리팩토링 후에는 이 권한을 Spring Security의 중앙 관리자인 `AuthenticationManager`에 위임했습니다.

### **인증 흐름 (Standard Flow)**
1.  **`UserService`**: 로그인 요청을 받아 `AuthenticationManager`에 인증 토큰을 전달.
2.  **`AuthenticationManager`**: 등록된 `Provider`를 통해 인증 수행.
3.  **`UserDetailsService`**: DB에서 유저 정보를 조회하여 `UserDetails` 객체 반환.
4.  **`DaoAuthenticationProvider`**: 조회된 정보와 입력된 비밀번호를 비교.
5.  **결과 반환**: 인증 성공 시 `Authentication` 객체를 `UserService`에 반환.

## 2. 주요 코드 변경 사항

### **2-1. UserService.java**
*   `PasswordEncoder`를 사용한 수동 비교 로직 삭제.
*   `AuthenticationManager` 주입 및 `authenticate()` 메서드 사용.
*   `Authentication.getPrincipal()`을 통한 인증된 사용자 정보 획득.

### **2-2. Security 관련 신규 클래스**
*   **`UserDetailsImpl`**: `UserDetails` 인터페이스 구현체. `User` 엔티티를 캡슐화.
*   **`UserDetailsServiceImpl`**: `UserDetailsService` 인터페이스 구현체. 이메일 기반 유저 조회 로직 포함.

### **2-3. SecurityConfig.java**
*   `AuthenticationManager`를 Bean으로 노출하여 서비스 계층에서 접근 가능하도록 설정.

## 3. 리팩토링의 의의

1.  **보안성 향상**: 비밀번호 비교 로직이 프레임워크 내부에서 처리되어 실수를 줄임.
2.  **유지보수성**: 유저 조회 로직(`UserDetailsService`)과 인증 로직(`Manager`)이 분리됨.
3.  **JWT 전환 용이성**: 표준 객체(`UserDetails`)를 사용하므로 토큰 기반 인증으로의 전환이 매우 매끄러움.
