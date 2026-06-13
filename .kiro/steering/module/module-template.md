---
inclusion: manual
---

# Module Template

새 Spring Boot 모듈을 추가할 때 이 가이드를 참고하세요.

> **모듈 생성 전 필수**: 모듈 유형(Web/Batch)과 모듈명을 반드시 사용자에게 확인한 후 진행합니다. (`new-module-guard.md` 참고)

## 모듈 유형별 기본 의존성

> `spring-boot-starter-data-jpa`와 `spring-boot-starter-test`는 Parent POM에서 자동 상속되므로 모듈별 pom.xml에 선언하지 않습니다. (`pom-conventions.md` 참고)

**Web 모듈:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Batch 모듈:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
```

## 패키지 네이밍 규칙

모듈 유형에 따라 기본 패키지가 달라집니다:

| 모듈 유형 | 기본 패키지 |
|---|---|
| Web | `com.myapps.web.{modulename}` |
| Batch | `com.myapps.batch.{modulename}` |

## 모듈 유형별 패키지 구조 (DDD)

**Web 모듈** (`com.myapps.web.{modulename}/`):
```
├── {ModuleName}Application.java
├── domain/                    # 도메인 계층 (핵심 비즈니스 로직)
│   ├── model/                 #   @Entity 도메인 모델, 값 객체, 도메인 이벤트
│   ├── repository/            #   리포지토리 인터페이스 (포트)
│   └── service/               #   도메인 서비스
├── application/               # 응용 계층 (유스케이스 오케스트레이션)
│   ├── service/               #   애플리케이션 서비스 (유스케이스 구현)
│   ├── dto/                   #   커맨드, 쿼리, 응답 DTO
│   └── port/                  #   외부 시스템 포트 인터페이스
├── infrastructure/            # 인프라 계층 (기술 구현)
│   ├── persistence/           #   JPA 리포지토리 구현체 (Spring Data JPA)
│   ├── external/              #   외부 API 클라이언트, 어댑터
│   └── config/                #   인프라 관련 설정 (DB, 메시징 등)
└── interfaces/                # 인터페이스 계층 (외부 진입점)
    ├── api/                   #   REST 컨트롤러
    ├── dto/                   #   요청/응답 DTO (API 전용)
    └── config/                #   웹 관련 설정 (CORS, 시큐리티 등)
```

**Batch 모듈** (`com.myapps.batch.{modulename}/`):
```
├── {ModuleName}Application.java
├── domain/                    # 도메인 계층 (핵심 비즈니스 로직)
│   ├── model/                 #   @Entity 도메인 모델, 값 객체
│   ├── repository/            #   리포지토리 인터페이스 (포트)
│   └── service/               #   도메인 서비스
├── application/               # 응용 계층 (유스케이스 오케스트레이션)
│   ├── service/               #   애플리케이션 서비스
│   ├── dto/                   #   커맨드, 쿼리 DTO
│   └── port/                  #   외부 시스템 포트 인터페이스
├── infrastructure/            # 인프라 계층 (기술 구현)
│   ├── persistence/           #   JPA 리포지토리 구현체 (Spring Data JPA)
│   ├── external/              #   외부 API 클라이언트, 어댑터
│   └── config/                #   인프라 관련 설정
└── job/                       # 배치 작업 계층 (진입점)
    ├── config/                #   Job/Step 설정 (@Configuration)
    ├── reader/                #   ItemReader 구현
    ├── processor/             #   ItemProcessor 구현
    ├── writer/                #   ItemWriter 구현
    └── tasklet/               #   Tasklet 구현
```

## 디렉터리 구조

**Web 모듈:**
```
{modulename}/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/myapps/web/{modulename}/
    │   │   └── {ModuleName}Application.java
    │   └── resources/
    │       ├── templates/         # Thymeleaf 템플릿 (.html)
    │       ├── static/            # 정적 리소스 (CSS, JS, 이미지)
    │       ├── application.yml
    │       ├── application-local.yml
    │       └── application-prod.yml
    └── test/
        └── java/com/myapps/web/{modulename}/
            └── {ModuleName}ApplicationTest.java
```

**Batch 모듈:**
```
{modulename}/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/myapps/batch/{modulename}/
    │   │   └── {ModuleName}Application.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       └── application-prod.yml
    └── test/
        └── java/com/myapps/batch/{modulename}/
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
        <!-- 모듈 유형에 맞는 starter만 추가 (JPA, test, Oracle, spring-boot-maven-plugin은 Parent에서 상속) -->
    </dependencies>
</project>
```

## Application 설정 파일 템플릿

모든 모듈은 아래 3개의 설정 파일을 `src/main/resources/`에 생성합니다.
DB 설정 정보는 모든 모듈이 동일합니다.

### application.yml (공통 설정)

```yaml
logging:
  level:
    root: info
spring:
  profiles:
    active: local
  datasource:
    driver-class-name: oracle.jdbc.OracleDriver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        "[default_schema]": admin
```

### application-local.yml (로컬 개발 환경)

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@I38ZV6G9LRF9FI84_high?TNS_ADMIN=/Users/gony/oracle_cloud/db/Wallet_I38ZV6G9LRF9FI84
    username: admin
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
```

### application-prod.yml (운영 환경)

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@I38ZV6G9LRF9FI84_high?TNS_ADMIN=/home/ubuntu/app/Wallet_I38ZV6G9LRF9FI84
    username: admin
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20

server:
  port: {할당된 포트}
```

> **`server.port`**: `deployment.md`의 포트 규칙 표를 참고하여 다음 빈 포트를 할당합니다.

## 메인 애플리케이션 클래스 템플릿

**Web 모듈:**
```java
package com.myapps.web.{modulename};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {ModuleName} 웹 애플리케이션의 진입점.
 */
@SpringBootApplication
public class {ModuleName}Application {

    public static void main(String[] args) {
        SpringApplication.run({ModuleName}Application.class, args);
    }
}
```

**Batch 모듈:**
```java
package com.myapps.batch.{modulename};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {ModuleName} 배치 애플리케이션의 진입점.
 */
@SpringBootApplication
public class {ModuleName}Application {

    public static void main(String[] args) {
        SpringApplication.run({ModuleName}Application.class, args);
    }
}
```

## Parent POM 업데이트

루트 `pom.xml`의 `<modules>` 섹션에 기존 모듈 뒤에 추가:

```xml
<modules>
    <!-- 기존 모듈들 -->
    <module>{modulename}</module>
</modules>
```

## 완료 체크리스트

- [ ] 모듈 디렉터리 및 소스 구조 생성
- [ ] `pom.xml` 생성 (Parent POM 참조, 버전 미선언)
- [ ] `{ModuleName}Application.java` 메인 클래스 생성
- [ ] `application.yml` 생성
- [ ] `application-local.yml` 생성 (로컬 DB 설정)
- [ ] `application-prod.yml` 생성 (운영 DB 설정 + server.port)
- [ ] Parent POM `<modules>`에 모듈명 추가
- [ ] `deployment.md` 포트 규칙 표에 새 모듈 포트 추가
- [ ] 기존 모듈 `pom.xml` 변경 없음 확인
- [ ] `mvn test -pl {modulename}` 통과
- [ ] `mvn clean install -pl {modulename} -am` 성공
