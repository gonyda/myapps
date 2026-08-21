---
inclusion: always
---

# Tech Stack

## 런타임 & 빌드 도구

- **Java**: 21 (타겟) / JDK 25 (빌드 환경)
  - `pom.xml` `<java.version>` = 21 → 컴파일러 `<release>` 21
  - 로컬·서버는 JDK 25 설치 (Homebrew `openjdk@25`, Oracle JDK 25) — 해석·컴파일 실행
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
├── AGENTS.md                  # AI Agent 공통 진입점 및 전체 개발 규칙
├── rules/                     # 프로젝트 공통 개발/품질 규칙 (SSOT)
├── memory-bank/               # 프로젝트 작업 맥락 및 메모리
├── skills/                    # 스킬 로직 공통 원본 (SSOT)
├── .kiro/                     # Kiro 전용 (specs)
├── .cline/                    # Cline 전용 설정 (참조 포인터)
├── .agents/                   # Antigravity 전용 설정 (참조 포인터)
├── mystudy/                   # Spring Boot 웹 애플리케이션 모듈
├── mycalendar/                # Spring Boot 웹 애플리케이션 모듈
└── myrpg/                     # Spring Boot 게임 애플리케이션 모듈
```

## 모듈 네이밍 규칙

- 모든 애플리케이션 모듈은 `my` 접두사를 사용합니다 (예: `mystudy`, `mycalendar`, `myrpg`)
- Maven `<artifactId>`는 모듈 디렉터리명과 동일하게 설정합니다
- Maven `<groupId>`는 `com.myapps`로 통일합니다

## 패키지 구조 규칙

- 모듈 유형에 따라 기본 패키지가 결정됩니다:
  - Web 모듈: `com.myapps.web.{modulename}` (예: `mystudy` → `com.myapps.web.mystudy`)
  - Batch 모듈: `com.myapps.batch.{modulename}` (Batch 모듈은 현재 없음 — 신규 추가 시 이 규칙 적용)
- 메인 클래스: `com.myapps.{web|batch}.{modulename}.{ModuleName}Application`
- DDD 계층 구조 적용 (상세 패키지 구조 및 모듈 생성 절차는 `module-template.md` 참고)

## 주요 의존성 기준

- Spring Boot Starter: `spring-boot-starter` (버전은 Parent POM에서 관리)
- JPA: `spring-boot-starter-data-jpa` (모든 모듈 공통)
- 테스트: `spring-boot-starter-test` (모든 모듈 공통)
- Thymeleaf: `spring-boot-starter-thymeleaf` (Web 모듈 전용 — 서버사이드 템플릿 엔진)
- 버전은 반드시 Parent POM의 `<dependencyManagement>`에서 중앙 관리
- 공통 의존성 선언 위치 및 상세 규칙은 `pom-conventions.md` 참고

## 환경 변수

- `JAVA_HOME`: `/opt/homebrew/opt/openjdk@25`
- PATH에 `/opt/homebrew/opt/openjdk@25/bin` 포함 필요
