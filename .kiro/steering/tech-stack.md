---
inclusion: always
---

# Tech Stack

## 런타임 & 빌드 도구

- **Java**: 25 (JDK 25, Homebrew `openjdk@25`)
- **Build Tool**: Apache Maven 3.9.9
- **Framework**: Spring Boot 4.0.0
- **Spring Framework**: 7.0.0 (Spring Boot 4.0 내장)
- **프로젝트 버전**: `1.0.0-SNAPSHOT`
- **GitHub 레포지토리명**: `myapps`

## 프로젝트 구조

이 프로젝트는 **단일 레포지토리 + Maven 멀티모듈** 구조를 사용합니다.

```
myapps/                        # Git 레포지토리 루트 (Project_Root)
├── pom.xml                    # Parent POM (packaging: pom)
├── mysender/                  # 첫 번째 Spring Boot 애플리케이션 모듈
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       └── test/java/
└── .kiro/
```

## 모듈 네이밍 규칙

- 모든 애플리케이션 모듈은 `my` 접두사를 사용합니다 (예: `mysender`, `myreceiver`)
- Maven `<artifactId>`는 모듈 디렉터리명과 동일하게 설정합니다
- Maven `<groupId>`는 `com.myapps`로 통일합니다

## 패키지 구조 규칙

- 기본 패키지: `com.myapps.{modulename}`
- 예: `mysender` 모듈 → `com.myapps.mysender`
- 메인 클래스: `com.myapps.{modulename}.{ModuleName}Application`

## 주요 의존성 기준

- Spring Boot Starter: `spring-boot-starter` (버전은 Parent POM에서 관리)
- 테스트: `spring-boot-starter-test` (JUnit 5 포함)
- 버전은 반드시 Parent POM의 `<dependencyManagement>`에서 중앙 관리

## 환경 변수

- `JAVA_HOME`: `/opt/homebrew/opt/openjdk@25`
- PATH에 `/opt/homebrew/opt/openjdk@25/bin` 포함 필요
