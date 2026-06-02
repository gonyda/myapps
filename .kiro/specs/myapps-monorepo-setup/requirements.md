# Requirements Document

## Introduction

이 문서는 단일 Git 레포지토리(monorepo) 안에서 여러 Spring Boot 애플리케이션을 Maven 멀티모듈 구조로 관리하기 위한 프로젝트 초기 설정에 대한 요구사항을 정의합니다.

최상위에 parent `pom.xml`을 두고, 각 애플리케이션은 독립된 Maven 모듈로 구성됩니다. 첫 번째 애플리케이션 모듈은 `mysender`이며, Java 25 및 Maven 3.9.9 환경을 기반으로 합니다. 완성된 프로젝트 구조는 GitHub에 push될 예정입니다.

---

## Glossary

- **Monorepo**: 하나의 Git 레포지토리 안에 여러 프로젝트(모듈)를 함께 관리하는 구조
- **Parent_POM**: 모든 하위 모듈이 상속받는 최상위 Maven `pom.xml` 파일
- **Child_Module**: Parent POM을 상속받는 개별 Maven 모듈 (예: `mysender`)
- **mysender**: 이 프로젝트의 첫 번째 Spring Boot 애플리케이션 모듈
- **Project_Root**: Git 레포지토리의 루트 디렉터리
- **Build_Tool**: Apache Maven (버전 3.9.9)
- **Runtime**: Java 25 (JDK 25)

---

## Requirements

### Requirement 1: 프로젝트 루트 구조 초기화

**User Story:** 개발자로서, 하나의 Git 레포지토리 안에 여러 Spring Boot 앱을 관리할 수 있는 Maven 멀티모듈 구조를 원합니다. 그래야 공통 설정을 한 곳에서 관리하고 일관된 빌드 환경을 유지할 수 있습니다.

#### Acceptance Criteria

1. THE Project_Root SHALL `pom.xml` 파일(Parent_POM)을 포함해야 한다.
2. THE Parent_POM의 `<packaging>` 값은 `pom`이어야 한다.
3. THE Parent_POM의 `<groupId>`는 역방향 도메인 형식(예: `com.myapps`)을 따르며, 프로젝트 전체에서 고유한 값을 가져야 한다.
4. THE Parent_POM의 `<artifactId>`는 루트 프로젝트를 식별하는 고유한 값(예: `myapps`)을 가져야 한다.
5. THE Parent_POM의 `<java.version>` 프로퍼티는 `25`로 설정되어야 하며, `maven-compiler-plugin`의 `source` 및 `target` 설정도 동일한 `25` 값을 참조해야 한다.
6. THE Parent_POM의 `spring-boot-starter-parent` 버전은 Spring Boot 3.2.0 이상이어야 한다.
7. THE Parent_POM은 `<modules>` 섹션에 하나 이상의 Child_Module 목록을 포함해야 한다.

---

### Requirement 2: mysender 모듈 구성

**User Story:** 개발자로서, `mysender`라는 이름의 Spring Boot 애플리케이션 모듈을 독립적으로 빌드하고 실행할 수 있기를 원합니다. 그래야 각 앱을 독립적으로 개발 및 배포할 수 있습니다.

#### Acceptance Criteria

1. THE Project_Root SHALL `mysender/` 디렉터리를 포함해야 한다.
2. THE Child_Module(`mysender`)은 자체 `pom.xml`을 가져야 하며, 해당 파일의 `<parent>` 섹션은 Parent_POM의 `<groupId>`, `<artifactId>`, `<version>`을 정확히 참조해야 한다.
3. THE mysender 모듈의 `pom.xml`은 `<artifactId>mysender</artifactId>`를 포함해야 한다.
4. THE mysender 모듈은 `src/main/java`, `src/main/resources`, `src/test/java` 디렉터리 구조를 가져야 한다.
5. WHEN `mysender` 모듈의 `pom.xml`이 빌드될 때, THE mysender 모듈의 `pom.xml`은 Java 버전이나 플러그인 버전을 별도로 재선언하지 않아야 한다 (Parent_POM에서 상속).
6. THE mysender 모듈은 `spring-boot-starter` 및 `spring-boot-starter-test` 의존성을 `<version>` 태그 없이 포함해야 한다.
7. THE mysender 모듈은 `src/main/java` 하위에 `@SpringBootApplication` 어노테이션이 포함된 메인 애플리케이션 클래스를 포함해야 하며, `java -jar` 명령으로 독립적으로 실행 가능해야 한다.

---

### Requirement 3: 전체 프로젝트 빌드 가능성

**User Story:** 개발자로서, Project_Root에서 단일 명령으로 모든 모듈을 빌드할 수 있기를 원합니다. 그래야 CI/CD 파이프라인이나 로컬 환경에서 빠르게 전체 빌드를 검증할 수 있습니다.

#### Acceptance Criteria

1. WHEN Project_Root에서 `mvn clean install` 명령이 실행될 때, THE Build_Tool SHALL 모든 Child_Module을 의존성 순서에 따라 빌드해야 한다.
2. WHEN 전체 빌드가 완료될 때, THE Build_Tool SHALL 각 Child_Module의 `target/` 디렉터리에 실행 가능한 JAR 파일(`*-SNAPSHOT.jar` 또는 버전 포함 JAR)을 생성해야 하며, JAR 파일이 존재하지 않으면 빌드는 성공으로 간주되지 않아야 한다.
3. IF 특정 Child_Module의 컴파일이 실패하면, THEN THE Build_Tool SHALL 오류 메시지와 함께 전체 빌드를 중단하고 빌드 상태를 실패(FAILED)로 표시해야 한다.
4. WHEN `mysender` 모듈만 빌드할 때(`mvn clean install -pl mysender`), THE Build_Tool SHALL 해당 모듈만 독립적으로 빌드해야 한다.

---

### Requirement 4: 공통 의존성 및 플러그인 중앙 관리

**User Story:** 개발자로서, 공통으로 사용하는 라이브러리 버전과 플러그인 설정을 Parent_POM에서 한 곳에 관리하기를 원합니다. 그래야 모든 모듈의 버전 충돌을 방지하고 일관성을 유지할 수 있습니다.

#### Acceptance Criteria

1. THE Parent_POM은 `<dependencyManagement>` 섹션을 통해 Spring Boot 스타터 및 테스트 라이브러리를 포함한 공통 의존성의 버전을 중앙에서 관리해야 한다.
2. THE Parent_POM은 `<pluginManagement>` 섹션을 통해 `maven-compiler-plugin`의 `source` 및 `target` 버전을 `25`로 설정해야 한다.
3. THE Parent_POM은 `<build><plugins>` 섹션에 `spring-boot-maven-plugin`을 선언해야 하며, WHEN `mvn package`가 실행될 때 각 Child_Module의 `target/` 디렉터리에 실행 가능한 JAR가 생성되어야 한다.
4. WHEN Child_Module의 `<dependencies>` 섹션에 Parent_POM에서 선언된 의존성을 추가할 때, THE Child_Module의 `pom.xml`은 해당 의존성에 `<version>` 태그를 포함하지 않아야 한다.
5. IF Child_Module이 Parent_POM에서 관리되는 의존성에 `<version>`을 재선언하면, THEN 해당 모듈의 빌드 검증은 실패로 간주되어야 한다.

---

### Requirement 5: Git 레포지토리 관리 설정

**User Story:** 개발자로서, 불필요한 빌드 산출물이 Git에 추적되지 않고 GitHub에 깔끔하게 push할 수 있기를 원합니다. 그래야 저장소가 오염되지 않고 협업 시 혼란이 없습니다.

#### Acceptance Criteria

1. THE Project_Root SHALL `.gitignore` 파일을 포함해야 한다.
2. THE `.gitignore` 파일은 Maven 빌드 산출물 디렉터리(`target/`)를 추적 제외 대상으로 포함해야 한다.
3. THE `.gitignore` 파일은 IDE 관련 파일(`.idea/`, `*.iml`, `.classpath`, `.project`, `.settings/`)을 추적 제외 대상으로 포함해야 한다.
4. THE `.gitignore` 파일은 OS 생성 파일(`.DS_Store`, `Thumbs.db`)을 추적 제외 대상으로 포함해야 한다.
5. IF `target/` 디렉터리나 IDE 설정 파일이 Project_Root에 존재하더라도, THEN `git status` 실행 결과에서 해당 경로들은 추적 대상(tracked)으로 표시되지 않아야 한다.

---

### Requirement 6: 새 모듈 추가 확장성

**User Story:** 개발자로서, 향후 `mysender` 외에 새로운 Spring Boot 앱 모듈(예: `myreceiver`)을 손쉽게 추가할 수 있기를 원합니다. 그래야 모노레포 구조를 확장하면서도 기존 모듈에 영향을 주지 않을 수 있습니다.

#### Acceptance Criteria

1. WHEN 새로운 Child_Module 디렉터리가 생성되고 해당 `pom.xml`이 Parent_POM을 `<parent>`로 참조하며 Parent_POM의 `<modules>` 섹션에 해당 모듈명이 추가될 때, THE Build_Tool SHALL 루트에서 `mvn package` 실행 시 새 모듈을 컴파일하고 패키징해야 한다.
2. WHEN 새 Child_Module이 추가될 때, THE 기존 Child_Module의 `pom.xml` 내용은 새 모듈 추가 전후로 동일한 상태를 유지해야 한다.
3. WHERE 새 모듈이 Parent_POM의 `<dependencyManagement>` 섹션에 선언되지 않은 의존성을 필요로 할 경우, THE 새 Child_Module SHALL 해당 의존성을 자체 `pom.xml`에만 선언해야 하며, Parent_POM의 `<dependencyManagement>` 및 다른 모듈의 `pom.xml`은 수정하지 않아야 한다.
