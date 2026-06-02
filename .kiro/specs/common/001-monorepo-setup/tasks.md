# Implementation Plan: myapps-monorepo-setup

## Overview

Maven 멀티모듈 구조의 monorepo 초기 설정을 단계적으로 구현합니다.
Parent POM 생성 → mysender 모듈 구성 → 빌드 검증 순서로 진행하며,
각 단계는 이전 단계 결과물 위에 누적됩니다.

## Tasks

- [x] 1. Parent POM 생성 및 루트 프로젝트 초기화
  - `myapps/pom.xml` 파일 생성
  - `<groupId>com.myapps</groupId>`, `<artifactId>myapps</artifactId>`, `<version>1.0.0-SNAPSHOT</version>`, `<packaging>pom</packaging>` 설정
  - `spring-boot-starter-parent` 4.0.x 버전 parent로 설정
  - `<properties>`에 `<java.version>25</java.version>` 선언
  - `<dependencyManagement>` 섹션 구성 (Spring Boot BOM 상속으로 버전 별도 선언 불필요)
  - `<pluginManagement>`에 `maven-compiler-plugin` source/target을 `${java.version}`으로 설정
  - `<build><plugins>`에 `spring-boot-maven-plugin` 선언
  - `<modules>`에 `<module>mysender</module>` 추가
  - 빌드 검증: `mvn clean install` → `BUILD SUCCESS` 확인
  - _요구사항: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 4.1, 4.2, 4.3_

- [x] 2. .gitignore 파일 생성
  - `myapps/.gitignore` 파일 생성
  - Maven 빌드 산출물 패턴 추가: `target/`
  - IntelliJ IDEA 관련 패턴 추가: `.idea/`, `*.iml`
  - Eclipse 관련 패턴 추가: `.classpath`, `.project`, `.settings/`
  - macOS 시스템 파일 패턴 추가: `.DS_Store`
  - Windows 시스템 파일 패턴 추가: `Thumbs.db`
  - 빌드 검증: `mvn validate` → `BUILD SUCCESS` 확인
  - _요구사항: 5.1, 5.2, 5.3, 5.4_

- [x] 3. mysender 모듈 디렉터리 구조 및 Child POM 생성
  - [x] 3.1 mysender 모듈 디렉터리 구조 생성
    - `mysender/src/main/java/com/myapps/mysender/` 디렉터리 생성
    - `mysender/src/main/resources/` 디렉터리 생성
    - `mysender/src/test/java/com/myapps/mysender/` 디렉터리 생성
    - 빌드 검증: 구조 생성 후 `mvn clean install` → `BUILD SUCCESS` 확인 (POM만으로 검증)
    - _요구사항: 2.1, 2.4_

  - [x] 3.2 mysender Child POM 생성
    - `mysender/pom.xml` 파일 생성
    - `<parent>` 섹션에 `com.myapps:myapps:1.0.0-SNAPSHOT` 참조 선언
    - `<artifactId>mysender</artifactId>`, `<packaging>jar</packaging>` 선언 (`<groupId>`, `<version>` 미선언)
    - `<dependencies>`에 `spring-boot-starter` 추가 (`<version>` 태그 없이)
    - `<dependencies>`에 `spring-boot-starter-test` 추가 (scope=test, `<version>` 태그 없이)
    - 빌드 검증: `mvn clean install -pl mysender -am` → `BUILD SUCCESS` 확인
    - _요구사항: 2.2, 2.3, 2.5, 2.6, 4.4, 4.5_

- [x] 4. 체크포인트 — 루트 빌드 및 구조 검증
  - 루트에서 `mvn clean install` 실행하여 전체 빌드 성공 확인
  - `mysender/target/` 디렉터리에 JAR 파일이 생성되었는지 확인
  - 문제 발생 시 사용자에게 질문하세요.

- [x] 5. MysenderApplication 메인 클래스 및 통합 테스트 구현
  - [x] 5.1 MysenderApplication 메인 클래스 작성
    - `mysender/src/main/java/com/myapps/mysender/MysenderApplication.java` 생성
    - `@SpringBootApplication` 어노테이션 선언
    - `SpringApplication.run(MysenderApplication.class, args)` 호출하는 `main` 메서드 작성
    - 빌드 검증: `mvn clean install -pl mysender -am` → `BUILD SUCCESS` 확인
    - _요구사항: 2.7_

  - [x] 5.2 Spring Boot 통합 테스트 작성
    - `mysender/src/test/java/com/myapps/mysender/MysenderApplicationTest.java` 생성
    - `@SpringBootTest` 어노테이션 선언
    - `contextLoads()` 테스트 메서드 작성 — ApplicationContext 오류 없이 로드되는지 검증
    - 테스트 실행: `mvn test -pl mysender` → `BUILD SUCCESS` 확인
    - 빌드 검증: `mvn clean install -pl mysender -am` → `BUILD SUCCESS` 확인
    - _요구사항: 2.7, 3.1, 3.2_

- [x] 6. application.yml 설정 파일 생성
  - `mysender/src/main/resources/application.yml` 파일 생성
  - 애플리케이션 이름 설정: `spring.application.name=mysender`
  - 빌드 검증: `mvn clean install -pl mysender -am` → `BUILD SUCCESS` 확인
  - _요구사항: 2.4_

- [x] 7. 최종 체크포인트 — 전체 빌드 및 단독 빌드 검증
  - 루트에서 `mvn clean install` 실행 → `BUILD SUCCESS` 및 `mysender/target/*.jar` 존재 확인
  - `mvn clean install -pl mysender` 실행 → mysender 단독 빌드 성공 확인
  - 모든 테스트 통과 여부 확인
  - 문제 발생 시 사용자에게 질문하세요.

## Notes

- `*` 표시 태스크는 선택 사항이며 MVP 구현 시 건너뛸 수 있습니다 (이 피처에는 해당 없음)
- 각 태스크는 요구사항 번호를 추적 가능하도록 참조합니다
- PBT(Property-Based Testing) 미적용: 이 피처는 Maven 빌드 설정/파일 구조 검증으로, 비즈니스 로직이 없어 단순 빌드 검증 및 `@SpringBootTest` 통합 테스트로 대체합니다
- 모든 태스크는 `BUILD SUCCESS` 확인 후에만 완료 처리합니다 (task-build-validation 규칙 준수)
- Child POM(`mysender/pom.xml`)에 `<groupId>`, `<version>`, 의존성 `<version>` 태그를 절대 포함하지 않습니다 (pom-conventions 규칙 준수)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["3.1"] },
    { "id": 1, "tasks": ["3.2"] },
    { "id": 2, "tasks": ["5.1"] },
    { "id": 3, "tasks": ["5.2"] }
  ]
}
```
