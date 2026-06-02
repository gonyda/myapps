---
inclusion: manual
---

# Module Template

새 Spring Boot 모듈을 추가할 때 이 가이드를 참고하세요.

> **모듈 생성 전 필수**: 모듈 유형(Web/Batch)과 모듈명을 반드시 사용자에게 확인한 후 진행합니다. (`new-module-guard.md` 참고)

## 모듈 유형별 기본 의존성

**Web 모듈:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Batch 모듈:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## 모듈 유형별 패키지 구조

**Web 모듈** (`com.myapps.{modulename}/`):
```
├── {ModuleName}Application.java
├── controller/
├── service/
├── repository/
├── domain/
├── dto/
└── config/
```

**Batch 모듈** (`com.myapps.{modulename}/`):
```
├── {ModuleName}Application.java
├── job/
├── step/
├── tasklet/
├── reader/
├── processor/
├── writer/
└── config/
```

## 디렉터리 구조

```
{modulename}/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/myapps/{modulename}/
    │   │   └── {ModuleName}Application.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/myapps/{modulename}/
            └── {ModuleName}ApplicationTest.java
```

## 모듈 pom.xml 템플릿

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.myapps</groupId>
        <artifactId>myapps</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>{modulename}</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- 모듈 유형에 맞는 starter 추가 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

## 메인 애플리케이션 클래스 템플릿

```java
package com.myapps.{modulename};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class {ModuleName}Application {

    public static void main(String[] args) {
        SpringApplication.run({ModuleName}Application.class, args);
    }
}
```

## Parent POM 업데이트

루트 `pom.xml`의 `<modules>` 섹션에 추가:

```xml
<modules>
    <module>mysender</module>
    <module>{modulename}</module>
</modules>
```

## 완료 체크리스트

- [ ] 모듈 디렉터리 및 소스 구조 생성
- [ ] `pom.xml` 생성 (Parent POM 참조, 버전 미선언)
- [ ] `{ModuleName}Application.java` 메인 클래스 생성
- [ ] `application.yml` 생성
- [ ] Parent POM `<modules>`에 모듈명 추가
- [ ] 기존 모듈 `pom.xml` 변경 없음 확인
- [ ] `mvn test -pl {modulename}` 통과
- [ ] `mvn clean install -pl {modulename} -am` 성공
