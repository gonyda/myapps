---
inclusion: fileMatch
fileMatchPattern: "**/pom.xml"
---

# POM Conventions

## Parent POM 필수 구조

```xml
<groupId>com.myapps</groupId>
<artifactId>myapps</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>

<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.0.0</version>
</parent>

<properties>
  <java.version>21</java.version>
  <spotless.version>2.44.5</spotless.version>
  <errorprone.version>2.36.0</errorprone.version>
  <archunit.version>1.4.0</archunit.version>
  <jacoco.version>0.8.13</jacoco.version>
</properties>
```

## 버전 관리 규칙

- 모든 공통 의존성 버전은 Parent POM의 `<dependencyManagement>`에서만 선언
- Child Module의 `<dependencies>`에는 `<version>` 태그 절대 금지
- 플러그인 버전도 Parent POM의 `<pluginManagement>`에서만 선언

## 4대 품질 가드레일 플러그인 설정

### 1. Spotless (`spotless-maven-plugin`)
- 코드 스타일 강제 및 import 정리
- `googleJavaFormat` (AOSP style, 4-space indent) 사용

### 2. Error Prone (`maven-compiler-plugin`)
- 컴파일 타임 정적 버그/안티패턴 차단
- `<fork>true</fork>` 및 Javac `--add-exports`/`--add-opens` 설정 적용

### 3. ArchUnit (`archunit-junit5`)
- Parent POM `<dependencies>`에 test scope로 공통 상속
- 각 모듈 `architecture` 패키지에 아키텍처 규칙 테스트 작성

### 4. JaCoCo (`jacoco-maven-plugin`)
- `prepare-agent`(initialize), `report`(verify), `check`(verify) 바인딩
- 커버리지 최소 기준 검증

## Parent POM 공통 의존성

아래 의존성은 Parent POM의 `<dependencies>` 섹션에 선언하여 모든 Child Module에 자동 상속됩니다.
모듈별 pom.xml에서 중복 선언하지 않습니다.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <!-- Oracle JDBC 드라이버 및 Wallet 보안 -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc11</artifactId>
    </dependency>
    <dependency>
        <groupId>com.oracle.database.security</groupId>
        <artifactId>oraclepki</artifactId>
    </dependency>
    <dependency>
        <groupId>com.oracle.database.security</groupId>
        <artifactId>osdt_core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.oracle.database.security</groupId>
        <artifactId>osdt_cert</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <!-- ArchUnit JUnit5 -->
    <dependency>
        <groupId>com.tngtech.archunit</groupId>
        <artifactId>archunit-junit5</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

> Oracle 의존성 버전은 `<dependencyManagement>`에서 관리합니다 (현재 21.5.0.0).

## Child Module pom.xml 규칙

```xml
<parent>
  <groupId>com.myapps</groupId>
  <artifactId>myapps</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</parent>

<artifactId>{modulename}</artifactId>
<packaging>jar</packaging>
```

- `<groupId>`와 `<version>`은 parent에서 상속되므로 별도 선언 금지
- 모듈 고유 의존성만 `<dependencies>`에 버전 없이 추가

## 새 모듈 추가 시 체크리스트

1. Parent POM `<modules>` 섹션에 모듈명 추가
2. 기존 Child Module의 `pom.xml`은 절대 수정하지 않음
3. 새 모듈 전용 의존성은 해당 모듈 `pom.xml`에만 추가
4. 새 모듈 `src/test/java/.../architecture/ArchitectureRuleTest.java` 아키텍처 테스트 추가
