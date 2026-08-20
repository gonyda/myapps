# Active Context

> 최종 업데이트: 2026-08-20 21:54 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`.clinerules/cline-global-rules.md` 반영)

> **전역 규칙 핵심 요약**:
> - 개발 워크플로우: **Spec 문서 3종 → 사용자 검토 → 구현**
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **빌드 및 4대 가드레일 검증**: 각 Task 완료 전 `mvn -B spotless:apply && mvn -B clean install -pl {modulename} -am && codegraph sync` 필수 실행 (-B: 토큰 절약)
> - 소스 정리: 미사용 import 제거, 매직넘버 상수화, 메서드 분리 (50줄 초과 시)

## 1. 완료된 작업: AI 코딩 품질 관리를 위한 4대 가드레일 도입

| 가드레일 | 도구 / 플러그인 | 설정 및 역할 |
|---|---|---|
| **1. Spotless** | `com.diffplug.spotless:spotless-maven-plugin:2.44.5` | `googleJavaFormat(AOSP)` 기반 4칸 들여쓰기, 미사용 import 자동 제거, 개행 및 공백 정렬 |
| **2. Error Prone** | `com.google.errorprone:error_prone_core:2.36.0` | `maven-compiler-plugin` 내부 `<annotationProcessorPaths>` 및 compilerArgs(`--add-exports`, `--add-opens`) 연결, 컴파일 타임 정적 결함 차단 |
| **3. ArchUnit** | `com.tngtech.archunit:archunit-junit5:1.4.0` | Parent POM 공통 test 의존성 및 모듈별 `ArchitectureRuleTest.java` 작성 (Interfaces/Domain/Application 계층 규칙 강제) |
| **4. JaCoCo** | `org.jacoco:jacoco-maven-plugin:0.8.13` | `prepare-agent`(initialize), `report`(verify), `check`(verify) 바인딩 및 커버리지 검증 |
| **5. CodeGraph Sync** | `codegraph sync` | 변경된 코드베이스 인덱스를 지식 그래프에 즉시 동기화 |

### 진행 상태 요약

| 항목 | 내용 | 상태 |
|---|---|---|
| A | Parent `pom.xml`에 4대 가드레일 의존성 및 플러그인 설정 | ✅ 완료 |
| B | `mycalendar`, `mycrawler`, `myrpg`, `mystudy` 4개 모듈에 ArchUnit 아키텍처 검증 테스트 작성 | ✅ 완료 |
| C | `mvn spotless:apply` 전체 모듈 코드 포맷팅 일괄 정렬 | ✅ 완료 |
| D | `mvn spotless:apply && mvn clean install && codegraph sync` 전체 파이프라인 검증 통과 | ✅ 완료 (1,126개 테스트 통과, BUILD SUCCESS, CodeGraph 동기화) |
| E | 스티어링 문서 3종 및 전역 룰(`.clinerules`) 검증 체인 갱신 | ✅ 완료 |

---

## 작업 트리 (수정/신규 파일 목록)

| 파일 | 변경 구분 | 내용 |
|---|---|---|
| `pom.xml` | 수정 | Spotless, Error Prone, ArchUnit, JaCoCo 플러그인 및 의존성 구성, release 21 설정 |
| `mycalendar/src/test/.../architecture/ArchitectureRuleTest.java` | 신규 | mycalendar 아키텍처 가드레일 테스트 |
| `mycrawler/src/test/.../architecture/ArchitectureRuleTest.java` | 신규 | mycrawler 아키텍처 가드레일 테스트 |
| `myrpg/src/test/.../architecture/ArchitectureRuleTest.java` | 신규 | myrpg 아키텍처 가드레일 테스트 |
| `mystudy/src/test/.../architecture/ArchitectureRuleTest.java` | 신규 | mystudy 아키텍처 가드레일 테스트 |
| `.kiro/steering/workflow/task-build-validation.md` | 수정 | 4대 가드레일 및 `mvn spotless:apply && mvn clean install && codegraph sync` 검증 체인 명문화 |
| `.kiro/steering/project/pom-conventions.md` | 수정 | 4대 가드레일 플러그인 설정 컨벤션 추가 |
| `.clinerules/cline-global-rules.md` | 수정 | 가드레일 검증 및 CodeGraph sync 워크플로우 반영 |
| `.clinerules/memory-bank/activeContext.md` | 수정 | 4대 가드레일 도입 상태 및 최신화 |

---

## 확정된 기술적 결정사항

- **JDK 25 + `--release 21` 바이트코드 컴파일**:
  - `JAVA_HOME` 및 런타임은 OpenJDK 25를 그대로 유지
  - ArchUnit 1.4.0 및 JaCoCo 0.8.13의 바이트코드 파싱 안정성을 위해 컴파일 타깃을 `release 21`로 구성하여 모든 정적 분석/커버리지/아키텍처 도구 100% 정상 작동
- **초경량 스마트 Tail 검증 체인 표준화**:
  - `mvn -B -q spotless:apply -pl {modulename} && (mvn -B clean install -pl {modulename} -am > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log && codegraph sync`
  - (루트 전체: `mvn -B -q spotless:apply && (mvn -B clean install > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log && codegraph sync`)
- **테스트 작성 시 Given-When-Then BDD 패턴 필수 강제**:
  - 모든 단위/통합 테스트에 `// given`, `// when`, `// then` 3단계를 명시하여 경계값, 실패 케이스, 엣지 케이스 등 다양한 테스트 케이스 도출 강제 (`code-style.md`, `task-build-validation.md` 반영)
