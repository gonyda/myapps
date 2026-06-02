---
inclusion: manual
---

# Module Template

새 Spring Boot 모듈을 추가할 때 이 가이드를 참고하세요.

## 새 모듈 추가 절차

### 0. 모듈 유형 확인 (필수)

**새 모듈을 추가하기 전에 반드시 사용자에게 모듈 유형을 질문해야 합니다.**

다음과 같이 질문하세요:

> "추가할 모듈의 유형을 선택해주세요:
> 1. **Web 모듈** — REST API 또는 웹 서비스 (spring-boot-starter-web 포함)
> 2. **Batch 모듈** — 배치 처리 작업 (spring-boot-starter-batch 포함)"

#### 모듈 유형별 기본 의존성

**Web 모듈 선택 시:**
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

**Batch 모듈 선택 시:**
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

#### 모듈 유형별 디렉터리 구조 차이

**Web 모듈:**
```
com.myapps.{modulename}/
├── {ModuleName}Application.java
├── controller/
├── service/
├── repository/
├── domain/
├── dto/
└── config/
```

**Batch 모듈:**
```
com.myapps.{modulename}/
├── {ModuleName}Application.java
├── job/          # Job 설정 클래스
├── step/         # Step 설정 클래스
├── tasklet/      # Tasklet 구현체
├── reader/       # ItemReader 구현체
├── processor/    # ItemProcessor 구현체
├── writer/       # ItemWriter 구현체
└── config/
```

---

### 1. 디렉터리 생성

```
{modulename}/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/myapps/{modulename}/
    │   │       └── {ModuleName}Application.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/
            └── com/myapps/{modulename}/
                └── {ModuleName}ApplicationTest.java
```

### 2. 모듈 pom.xml 템플릿

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
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 3. 메인 애플리케이션 클래스 템플릿

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

### 4. Parent POM 업데이트

루트 `pom.xml`의 `<modules>` 섹션에 추가:

```xml
<modules>
    <module>mysender</module>
    <module>{modulename}</module>  <!-- 여기에 추가 -->
</modules>
```

### 5. 완료 체크리스트

- [ ] 모듈 디렉터리 생성
- [ ] `pom.xml` 생성 (Parent POM 참조, 버전 미선언)
- [ ] `src/main/java/com/myapps/{modulename}/` 디렉터리 생성
- [ ] `{ModuleName}Application.java` 메인 클래스 생성
- [ ] `src/main/resources/application.yml` 생성
- [ ] `src/test/java/com/myapps/{modulename}/` 디렉터리 생성
- [ ] Parent POM `<modules>`에 모듈명 추가
- [ ] `mvn test -pl {modulename}` 실행하여 테스트 통과 확인
- [ ] `mvn clean install -pl {modulename} -am` 실행하여 빌드 성공 확인
- [ ] 기존 모듈 `pom.xml` 변경 여부 확인 (변경 없어야 함)
