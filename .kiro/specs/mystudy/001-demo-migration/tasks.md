# Implementation Plan: demo 프로젝트 englishstudy 기능 mystudy 모듈 마이그레이션

## Overview

demo Gradle 프로젝트의 영어 학습(englishstudy) 기능을 myapps Maven 멀티모듈 프로젝트의 mystudy 모듈로 마이그레이션합니다. Parent POM 수정, mystudy 모듈 생성, DDD 패키지 구조로 소스 재편성, Oracle Cloud DB 설정, 테스트 작성, mysender 모듈 삭제를 순차적으로 수행합니다.

## Tasks

- [x] 1. Parent POM 수정 및 mystudy 모듈 POM 생성
  - [x] 1.1 Parent POM에 Oracle 의존성 버전 관리 및 mystudy 모듈 등록
    - `pom.xml`의 `<modules>` 섹션에 `<module>mystudy</module>` 추가
    - `<dependencyManagement>` 섹션에 Oracle JDBC 의존성 4개(ojdbc11, oraclepki, osdt_core, osdt_cert) 버전 21.5.0.0 선언
    - _Requirements: 1.5, 1.6_

  - [x] 1.2 mystudy 모듈 pom.xml 생성
    - `mystudy/pom.xml` 생성: parent(com.myapps:myapps:1.0.0-SNAPSHOT), artifactId=mystudy, packaging=jar
    - 모듈 고유 의존성 추가 (버전 없음): spring-boot-starter-web, spring-boot-starter-thymeleaf, ojdbc11, oraclepki, osdt_core, osdt_cert
    - `<groupId>`, `<version>` 선언하지 않음 (parent 상속)
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 2. Checkpoint - 빌드 검증
  - `mvn clean install -pl mystudy -am` 실행하여 POM 구조 검증 (BUILD SUCCESS 확인)
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. DDD 패키지 구조 및 Application 설정
  - [x] 3.1 MystudyApplication 메인 클래스 생성
    - `mystudy/src/main/java/com/myapps/web/mystudy/MystudyApplication.java` 생성
    - `@SpringBootApplication` 어노테이션, 클래스 수준 JavaDoc 작성
    - _Requirements: 2.1, 2.6_

  - [x] 3.2 application.yml 설정 파일 생성
    - `mystudy/src/main/resources/application.yml` 생성
    - Oracle Cloud DB Wallet 기반 TNS 접속 설정 (driver-class-name, url, username, password)
    - HikariCP maximum-pool-size=10, JPA ddl-auto=update, hibernate default_schema=admin
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

  - [x] 3.3 MystudyApplicationTest 생성
    - `mystudy/src/test/java/com/myapps/web/mystudy/MystudyApplicationTest.java` 생성
    - `@SpringBootTest` 어노테이션으로 컨텍스트 로딩 테스트
    - _Requirements: 10.2_

- [x] 4. 도메인 계층 구현 (Entity, Repository)
  - [x] 4.1 EnglishStudy 엔티티 생성
    - `com.myapps.web.mystudy.domain.model.EnglishStudy` 클래스 생성
    - 필드: id(Long), episode(Long), koreanSentence(String), englishSentence(String)
    - JPA 어노테이션: @Entity, @Id, @GeneratedValue(strategy=GenerationType.IDENTITY)
    - 명시적 getter/setter, no-arg 생성자, 클래스 및 메서드 JavaDoc 작성
    - Lombok 사용 금지
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 4.2 EnglishStudyRepository 인터페이스 생성
    - `com.myapps.web.mystudy.domain.repository.EnglishStudyRepository` 인터페이스 생성
    - `JpaRepository<EnglishStudy, Long>` 확장
    - `List<EnglishStudy> findAllByOrderByIdDesc()` 메서드 선언
    - @Repository 어노테이션 사용하지 않음
    - 인터페이스 및 메서드 JavaDoc 작성
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 5. 애플리케이션 계층 구현 (Service)
  - [x] 5.1 EnglishStudyService 구현
    - `com.myapps.web.mystudy.application.service.EnglishStudyService` 클래스 생성
    - @Service 어노테이션, 생성자 주입으로 EnglishStudyRepository를 private final 필드로 주입
    - `findAllOrderByIdDesc()`: ID 역순 전체 조회, `save()`: 엔티티 저장 및 반환
    - @Autowired 필드 주입 금지, 미사용 import 없이 작성
    - 클래스 및 메서드 JavaDoc 작성 (@param, @return 태그 포함)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [x] 5.2 EnglishStudyServiceTest 단위 테스트 작성
    - `@ExtendWith(MockitoExtension.class)` 기반 단위 테스트
    - EnglishStudyRepository mock을 이용한 findAllOrderByIdDesc(), save() 메서드 검증
    - 메서드명: `should_{기대동작}_when_{조건}` 형식
    - _Requirements: 5.2, 5.3, 10.2_

- [x] 6. 인터페이스 계층 구현 (Controller, Template)
  - [x] 6.1 EnglishStudyController 구현
    - `com.myapps.web.mystudy.interfaces.api.EnglishStudyController` 클래스 생성
    - @Controller 어노테이션, 생성자 주입으로 EnglishStudyService를 private final 필드로 주입
    - GET /english-study: Thymeleaf 뷰 반환 (모델에 englishStudies 속성 추가)
    - GET /api/english-study: @ResponseBody JSON 응답 (전체 목록)
    - POST /api/english-study: @ResponseBody, @RequestBody, 201 Created 반환
    - 클래스 및 메서드 JavaDoc 작성 (@param, @return 태그 포함)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 6.2 Thymeleaf 템플릿 마이그레이션
    - `mystudy/src/main/resources/templates/englishstudy/english_study.html` 생성
    - demo 프로젝트의 기존 HTML 구조 및 인라인 JavaScript 동일하게 복사
    - 페이지네이션(10개/페이지), EBS 링크 자동 생성, 문장 등록 폼, 영어 문장 숨기기/토글 기능 유지
    - fetch API를 통한 GET /api/english-study 및 POST /api/english-study 호출 유지
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [x] 6.3 EnglishStudyControllerTest 슬라이스 테스트 작성
    - `@WebMvcTest(EnglishStudyController.class)` 기반 슬라이스 테스트
    - MockMvc를 이용한 GET /english-study, GET /api/english-study, POST /api/english-study 엔드포인트 검증
    - HTTP 상태 코드, 응답 구조 검증
    - _Requirements: 6.1, 6.2, 6.3, 10.2_

- [x] 7. Checkpoint - mystudy 모듈 빌드 및 테스트 검증
  - `mvn clean install -pl mystudy -am` 실행하여 BUILD SUCCESS 확인
  - `mvn test -pl mystudy` 실행하여 failures 0, errors 0 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. mysender 모듈 삭제 및 최종 빌드 검증
  - [x] 8.1 mysender 모듈 삭제
    - Parent POM의 `<modules>` 섹션에서 `<module>mysender</module>` 제거
    - `mysender/` 디렉터리 전체 삭제 (pom.xml, src/, target/ 포함)
    - _Requirements: 9.1, 9.2_

  - [x] 8.2 전체 프로젝트 빌드 검증
    - 프로젝트 루트에서 `mvn clean install` 실행하여 BUILD SUCCESS 확인
    - mystudy 모듈 포함 전체 멀티모듈 빌드 성공 검증
    - _Requirements: 9.3, 10.1, 10.3, 10.4_

- [x] 9. Final checkpoint - 전체 검증 완료
  - 전체 프로젝트 `mvn clean install` BUILD SUCCESS 확인
  - mystudy 모듈 독립 빌드 및 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 Task는 특정 requirements를 참조하여 추적 가능
- Checkpoints에서 빌드 실패 시 이전 Task로 돌아가 수정
- Property-Based Testing은 적용하지 않음 (단순 CRUD 마이그레이션, 순수 함수 부재)
- 단위 테스트(Mockito)와 슬라이스 테스트(WebMvcTest)로 검증
- 빌드 명령: `mvn clean install -pl mystudy -am` (모듈 빌드), `mvn clean install` (전체 빌드)
- demo 프로젝트 소스 참조 경로: `/Users/gony/git/demo/`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["3.3", "4.1"] },
    { "id": 4, "tasks": ["4.2"] },
    { "id": 5, "tasks": ["5.1"] },
    { "id": 6, "tasks": ["5.2", "6.1", "6.2"] },
    { "id": 7, "tasks": ["6.3"] },
    { "id": 8, "tasks": ["8.1"] },
    { "id": 9, "tasks": ["8.2"] }
  ]
}
```
