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
  <java.version>25</java.version>
</properties>
```

## 버전 관리 규칙

- 모든 공통 의존성 버전은 Parent POM의 `<dependencyManagement>`에서만 선언
- Child Module의 `<dependencies>`에는 `<version>` 태그 절대 금지
- 플러그인 버전도 Parent POM의 `<pluginManagement>`에서만 선언

## maven-compiler-plugin 설정

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <release>${java.version}</release>
  </configuration>
</plugin>
```

`<release>`는 `<source>`, `<target>`, bootclasspath를 한 번에 설정하는 Java 21+ 표준 방식입니다.

## spring-boot-maven-plugin 설정

- Parent POM의 `<build><plugins>`에 선언
- 각 Child Module이 실행 가능한 fat JAR로 패키징되도록 보장

## Parent POM 공통 의존성

아래 의존성은 Parent POM의 `<dependencies>` 섹션에 선언하여 모든 Child Module에 자동 상속됩니다.
모듈별 pom.xml에서 중복 선언하지 않습니다.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

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
