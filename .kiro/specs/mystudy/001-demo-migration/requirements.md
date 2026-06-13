# Requirements Document

## Introduction

demo 프로젝트(Gradle 기반 영어 학습 웹앱)의 englishstudy 기능을 myapps 프로젝트의 mystudy 모듈로 마이그레이션합니다. 빌드 시스템을 Gradle에서 Maven으로 전환하고, Java 17에서 Java 25로 업그레이드하며, 패키지 구조를 DDD 계층 구조로 재편성합니다. 동시에 기존 mysender 모듈을 삭제하여 프로젝트를 정리합니다.

## Glossary

- **Mystudy_Module**: myapps 프로젝트 내 영어 학습 기능을 담당하는 Maven 웹 모듈 (artifactId: mystudy)
- **Parent_POM**: myapps 프로젝트의 루트 pom.xml (공통 의존성 및 모듈 목록 관리)
- **EnglishStudy_Entity**: 영어 문장 학습 데이터를 표현하는 JPA 엔티티 (id, episode, koreanSentence, englishSentence)
- **EnglishStudy_Repository**: EnglishStudy_Entity에 대한 JPA 리포지토리 인터페이스
- **EnglishStudy_Service**: 영어 학습 데이터의 조회 및 저장 비즈니스 로직을 처리하는 애플리케이션 서비스
- **EnglishStudy_Controller**: 영어 학습 기능의 웹 진입점 (Thymeleaf 뷰 + REST API)
- **Oracle_Cloud_DB**: Oracle Cloud 인프라에서 제공하는 데이터베이스 (Wallet 기반 TNS 접속)
- **DDD_Package_Structure**: Domain-Driven Design 기반 패키지 구조 (domain, application, infrastructure, interfaces)
- **Mysender_Module**: myapps 프로젝트에서 제거할 기존 모듈 (artifactId: mysender)
- **Project_Root**: myapps Git 레포지토리의 루트 디렉터리

## Requirements

### Requirement 1: Maven 모듈 생성

**User Story:** As a 개발자, I want demo 프로젝트의 기능을 Maven 멀티모듈 구조로 마이그레이션, so that 통합된 빌드 시스템에서 일관되게 관리할 수 있습니다.

#### Acceptance Criteria

1. THE Mystudy_Module SHALL Maven 모듈로 생성되며, Parent_POM을 상위 프로젝트로 참조하는 `<parent>` 요소(groupId: com.myapps, artifactId: myapps, version: 1.0.0-SNAPSHOT)를 선언하고, 모듈 자체에는 `<groupId>`와 `<version>`을 선언하지 않는다
2. THE Mystudy_Module SHALL artifactId를 "mystudy"로 설정하고, packaging을 "jar"로 설정한다
3. THE Mystudy_Module SHALL spring-boot-starter-web과 spring-boot-starter-thymeleaf를 모듈의 `<dependencies>` 섹션에 `<version>` 태그 없이 선언한다
4. THE Mystudy_Module SHALL Oracle JDBC 의존성을 모듈의 `<dependencies>` 섹션에 `<version>` 태그 없이 선언한다: com.oracle.database.jdbc:ojdbc11, com.oracle.database.security:oraclepki, com.oracle.database.security:osdt_core, com.oracle.database.security:osdt_cert
5. THE Parent_POM SHALL modules 섹션에 "mystudy"를 포함한다
6. THE Parent_POM SHALL `<dependencyManagement>` 섹션에 Oracle 의존성(ojdbc11, oraclepki, osdt_core, osdt_cert) 4개의 버전을 21.5.0.0으로 선언한다

### Requirement 2: DDD 패키지 구조 구성

**User Story:** As a 개발자, I want 코드를 DDD 계층 구조로 재편성, so that 도메인 로직과 인프라 관심사가 명확히 분리됩니다.

#### Acceptance Criteria

1. THE Mystudy_Module SHALL 기본 패키지를 com.myapps.web.mystudy로 설정한다
2. THE Mystudy_Module SHALL domain/model 패키지에 EnglishStudy_Entity를 배치한다
3. THE Mystudy_Module SHALL domain/repository 패키지에 EnglishStudy_Repository 인터페이스를 배치한다
4. THE Mystudy_Module SHALL application/service 패키지에 EnglishStudy_Service를 배치한다
5. THE Mystudy_Module SHALL interfaces/api 패키지에 EnglishStudy_Controller를 배치한다
6. THE Mystudy_Module SHALL MystudyApplication 메인 클래스를 com.myapps.web.mystudy 패키지 루트에 배치한다
7. THE Mystudy_Module SHALL domain 계층이 application, infrastructure, interfaces 계층에 의존하지 않도록 패키지 간 의존 방향을 유지한다

### Requirement 3: 엔티티 마이그레이션

**User Story:** As a 개발자, I want EnglishStudy 엔티티를 Java 25 스타일로 변환, so that 최신 언어 기능을 활용하고 코드 스타일 규칙을 준수합니다.

#### Acceptance Criteria

1. THE EnglishStudy_Entity SHALL id(Long), episode(Long), koreanSentence(String), englishSentence(String) 필드를 유지한다
2. THE EnglishStudy_Entity SHALL JPA @Entity, @Id, @GeneratedValue(strategy = GenerationType.IDENTITY) 어노테이션을 유지한다
3. THE EnglishStudy_Entity SHALL 클래스 수준 JavaDoc(역할 및 책임 요약)과 모든 public 메서드에 대한 JavaDoc(@param, @return 태그 포함)을 포함한다
4. THE EnglishStudy_Entity SHALL Lombok을 사용하지 않고 모든 필드에 대해 명시적 public getter 및 setter 메서드를 제공한다
5. THE EnglishStudy_Entity SHALL JPA 엔티티 hydration을 위한 인자 없는 기본 생성자(no-arg constructor)를 포함한다

### Requirement 4: 리포지토리 마이그레이션

**User Story:** As a 개발자, I want EnglishStudy 리포지토리를 DDD 구조에 맞게 마이그레이션, so that 도메인 계층에서 데이터 접근 계약을 정의합니다.

#### Acceptance Criteria

1. THE EnglishStudy_Repository SHALL JpaRepository<EnglishStudy, Long>을 확장하며, @Repository 어노테이션을 선언하지 않는다 (Spring Data JPA 인터페이스는 자동 등록되므로 불필요)
2. THE EnglishStudy_Repository SHALL List<EnglishStudy> 타입을 반환하는 findAllByOrderByIdDesc() 메서드를 제공한다
3. THE EnglishStudy_Repository SHALL 인터페이스 수준 JavaDoc(역할 및 책임 요약)과 public 메서드 수준 JavaDoc(@return 태그 포함)을 포함한다
4. THE EnglishStudy_Repository SHALL domain/repository 패키지에 위치한다

### Requirement 5: 서비스 마이그레이션

**User Story:** As a 개발자, I want EnglishStudy 서비스를 코드 스타일 규칙에 맞게 리팩토링, so that 생성자 주입 방식과 JavaDoc 규칙을 준수합니다.

#### Acceptance Criteria

1. THE EnglishStudy_Service SHALL 단일 생성자를 통해 EnglishStudy_Repository를 private final 필드로 주입받는다
2. THE EnglishStudy_Service SHALL 모든 영어 학습 데이터를 ID 역순으로 조회하여 List<EnglishStudy> 타입으로 반환하는 public 메서드를 제공한다
3. THE EnglishStudy_Service SHALL 전달받은 EnglishStudy 엔티티를 저장하고 저장된 엔티티를 반환하는 public 메서드를 제공한다
4. THE EnglishStudy_Service SHALL @Autowired 필드 주입을 사용하지 않는다
5. THE EnglishStudy_Service SHALL 클래스 수준 JavaDoc에 서비스의 역할과 책임을 한 줄 요약으로 포함하고, 모든 public 메서드에 @param, @return, @throws 태그를 해당하는 경우 포함하는 JavaDoc을 작성한다
6. THE EnglishStudy_Service SHALL 미사용 import 문을 포함하지 않는다

### Requirement 6: 컨트롤러 마이그레이션

**User Story:** As a 개발자, I want EnglishStudy 컨트롤러를 myapps 코드 스타일에 맞게 마이그레이션, so that REST API와 Thymeleaf 뷰를 동시에 제공합니다.

#### Acceptance Criteria

1. WHEN GET /english-study 요청이 수신되면, THE EnglishStudy_Controller SHALL Thymeleaf 뷰 템플릿을 반환하며, ID 내림차순으로 정렬된 영어 학습 데이터 목록을 모델 속성으로 포함한다
2. WHEN GET /api/english-study 요청이 수신되면, THE EnglishStudy_Controller SHALL @ResponseBody를 사용하여 ID 내림차순으로 정렬된 모든 영어 학습 데이터를 JSON 형식으로 반환한다
3. WHEN POST /api/english-study 요청이 유효한 데이터와 함께 수신되면, THE EnglishStudy_Controller SHALL @ResponseBody를 사용하여 새 영어 학습 데이터를 저장하고 HTTP 201 상태코드와 함께 저장된 데이터를 JSON 형식으로 반환한다
4. THE EnglishStudy_Controller SHALL @Controller 어노테이션을 사용하고 interfaces/api 패키지에 위치하며, 생성자 주입 방식으로 EnglishStudy_Service를 주입받는다
5. THE EnglishStudy_Controller SHALL 클래스 수준 및 모든 public 메서드에 JavaDoc 주석을 포함하며, 메서드 JavaDoc에는 @param, @return 태그를 포함한다

### Requirement 7: Thymeleaf 템플릿 마이그레이션

**User Story:** As a 사용자, I want 기존 영어 학습 웹 화면을 동일하게 사용, so that 마이그레이션 후에도 동일한 학습 경험을 유지합니다.

#### Acceptance Criteria

1. THE Mystudy_Module SHALL english_study.html 템플릿을 src/main/resources/templates/englishstudy/ 경로에 배치하며, 기존 demo 프로젝트의 동일 파일과 동일한 HTML 구조 및 인라인 JavaScript를 포함한다
2. THE Mystudy_Module SHALL 클라이언트 측 페이지네이션을 제공하며, 한 페이지당 10개 항목을 표시하고, 이전/다음 버튼 및 페이지 번호 버튼을 통해 페이지를 전환한다
3. THE Mystudy_Module SHALL 등록된 문장 데이터 중 최대 회차(episode) 값을 기준으로 EBS 학습 링크(https://home.ebse.co.kr/beginnerenglish/...)를 자동 생성하여 새 탭으로 열리는 하이퍼링크를 표시한다
4. THE Mystudy_Module SHALL 영어 문장 등록 폼에서 회차(숫자), 영어 문장(텍스트), 한국어 해석(텍스트) 3개 필드를 제공하고, fetch API를 통해 POST /api/english-study 엔드포인트로 JSON 데이터를 전송한다
5. IF 등록 폼에서 회차, 영어 문장, 한국어 해석 중 하나라도 비어 있거나 회차가 숫자가 아닌 경우, THEN THE Mystudy_Module SHALL 등록 요청을 전송하지 않고 입력 오류를 알림으로 표시한다
6. THE Mystudy_Module SHALL 영어 문장을 초기 상태에서 숨김(CSS visibility: hidden) 처리하고, 해당 문장 영역 클릭 시 보기/숨기기를 토글한다
7. THE Mystudy_Module SHALL 페이지 로드 시 fetch API를 통해 GET /api/english-study 엔드포인트에서 학습 데이터를 조회하여 테이블을 렌더링한다

### Requirement 8: Oracle DB 접속 설정

**User Story:** As a 개발자, I want Oracle Cloud DB 접속 설정을 유지, so that 마이그레이션 후에도 기존 데이터베이스에 접속할 수 있습니다.

#### Acceptance Criteria

1. THE Mystudy_Module SHALL application.yml의 spring.datasource.driver-class-name에 oracle.jdbc.OracleDriver를 설정한다
2. THE Mystudy_Module SHALL spring.datasource.url에 jdbc:oracle:thin:@ 접두사와 TNS_ADMIN 파라미터를 포함하는 Wallet 기반 Oracle Cloud 접속 URL을 설정한다
3. THE Mystudy_Module SHALL spring.datasource.hikari.maximum-pool-size를 10으로 설정한다
4. THE Mystudy_Module SHALL JPA hibernate ddl-auto를 update로 설정한다
5. THE Mystudy_Module SHALL hibernate default_schema를 admin으로 설정한다
6. THE Mystudy_Module SHALL spring.datasource.username을 admin으로 설정하고, spring.datasource.password를 기존 demo 프로젝트와 동일한 값으로 설정한다
7. WHEN 애플리케이션이 시작될 때, THE Mystudy_Module SHALL Oracle Cloud DB에 정상적으로 커넥션을 획득하여 HikariCP 풀이 초기화된다

### Requirement 9: mysender 모듈 삭제

**User Story:** As a 개발자, I want 더 이상 사용하지 않는 mysender 모듈을 제거, so that 프로젝트에 불필요한 코드가 남지 않습니다.

#### Acceptance Criteria

1. THE Parent_POM SHALL modules 섹션에서 `<module>mysender</module>` 항목을 제거한다
2. THE Project_Root SHALL mysender/ 디렉터리와 그 하위의 모든 파일 및 디렉터리(pom.xml, src/, target/ 포함)를 삭제한다
3. WHEN 모듈 삭제 후 프로젝트 루트에서 `mvn clean install`을 실행하면, THE Build SHALL BUILD SUCCESS로 완료된다

### Requirement 10: 빌드 및 컴파일 검증

**User Story:** As a 개발자, I want 마이그레이션된 모듈이 정상적으로 빌드, so that 코드 품질과 의존성 무결성을 보장합니다.

#### Acceptance Criteria

1. WHEN mvn clean install -pl mystudy -am 명령을 실행하면, THE Mystudy_Module SHALL Maven 출력에 "BUILD SUCCESS"가 표시되고 프로세스 exit code 0을 반환하며 빌드가 완료된다
2. WHEN mvn test -pl mystudy 명령을 실행하면, THE Mystudy_Module SHALL Surefire 리포트에서 failures 0건, errors 0건으로 모든 테스트가 통과하며, 최소 1개 이상의 테스트(MystudyApplicationTest의 Spring Boot 컨텍스트 로딩 테스트 포함)가 실행된다
3. THE Mystudy_Module SHALL Java 25 컴파일러(maven-compiler-plugin, release=25)로 compilation error 0건으로 컴파일된다
4. WHEN 프로젝트 루트에서 mvn clean install 명령을 실행하면, THE myapps 프로젝트 SHALL mystudy 모듈을 포함한 전체 멀티모듈 빌드가 "BUILD SUCCESS"로 완료된다
