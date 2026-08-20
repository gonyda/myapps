# Active Context

> 최종 업데이트: 2026-08-20 23:02 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`.clinerules/cline-global-rules.md` 반영)

> **전역 규칙 핵심 요약**:
> - 개발 워크플로우: **Spec 문서 3종 → 사용자 검토 → 구현**
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **빌드 및 4대 가드레일 검증**: 각 Task 완료 전 `mvn -B -q spotless:apply -pl {modulename} && (mvn -B clean install -pl {modulename} -am > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log && codegraph sync` 필수 실행 (-B: 토큰 절약)
> - 소스 정리: 미사용 import 제거, 매직넘버 상수화, 메서드 분리 (50줄 초과 시)

## 1. 최근 작업 내역

### 1.3. `.clinerules/cline-global-rules.md` 가드레일 명령어 정합성 검증 및 최신화 (2026-08-20 22:46)
- **검증 결과**: 파일 내 가드레일 명령어가 3가지 버전으로 혼재 확인
  - §1(26행): 최신 버전 (스마트 Tail, `/tmp/mvn.log`) ✅
  - §2(64행): 구버전 (`mvn spotless:apply && ...`) ❌ → **수정 완료**
  - §3.2(90행): 중간버전 (`mvn -B spotless:apply && ...`) ❌ → **수정 완료**
- **통일 기준**: `.kiro/steering/workflow/task-build-validation.md`와 동일한 최신 명령어로 통일
  ```
  mvn -B -q spotless:apply -pl {modulename} && (mvn -B clean install -pl {modulename} -am > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log && codegraph sync
  ```

### 1.4. `.clinerules` vs `.kiro/steering` 정합성 검증 및 SSOT 리팩토링 (2026-08-20 22:55)
- **검증 결과**: `.clinerules/`와 `.kiro/steering/` 문서 간 중복·불일치 다수 확인
  - **중복**: 4대 가드레일 명령어, Spec 3종 규칙, CodeGraph/MCP 우선순위, 메모리뱅크 수명주기 — `.kiro/steering/` 문서가 원본임에도 `cline-global-rules.md`에 상세 복사되어 동기화 여지
  - **불일치**: `pom.xml`/`pom-conventions.md`는 Java 21, `tech-stack.md`/`code-style.md`/`deployment.md`는 Java 25 명시
  - **유령 모듈 예시**: `git-workflow.md`/`code-style.md`/`tech-stack.md`/`spec-conventions.md`/`new-module-guard.md`에 실존하지 않는 `mysender`/`myreceiver`/`myjob` 잔존
- **리팩토링 완료 (`.clinerules/cline-global-rules.md`)**:
  - SSOT 원칙 도입: 상세 규칙 중복 기술 제거하고 `.kiro/steering/` 참조 인덱스 중심으로 재구성
  - 가드레일 명령어·Spec 포맷·CodeGraph 상세 설명 인라인 제거 → 해당 steering 문서 참조로 대체
  - 섹션 번호 재구성: §1(워크플로우) · §2(스티어링 참조 SSOT 인덱스) · §3(MCP 툴 우선) · §4(메모리뱅크 운영 절차)
- **사용자 결정**: `memory-bank.md`는 수정하지 않음 (일반 개념 문서로 유지, 구체 운영 절차는 `cline-global-rules.md` §4 참조)
- **추가 발견 (미해결 이슈, 후속 조치 필요)**:
  - ~~`pom.xml`의 `<java.version>`이 21인데 `tech-stack.md`/`code-style.md`는 Java 25 표준 명시~~ → **해결 완료**
  - ~~`.kiro/steering/` 문서들에 실제로 없는 모듈 예시(`mysender`, `myreceiver`, `myjob`) 잔존~~ → **해결 완료**

### 1.5. 미해결 이슈 해결 — Java 버전 정합성 및 유령 모듈 예시 정리 (2026-08-20 23:02)
- **Java 버전 통일 완료**: `pom.xml`(`<java.version>21`) 기준으로 정합
  - `tech-stack.md`: "Java 21 (타겟) / JDK 25 (빌드 환경)"으로 명확화
  - `code-style.md`: "Java 21 (target)"로 수정, "Java 25 record" → "Java 21 record"
- **유령 모듈 예시 제거 완료**: 실제 없는 `mysender`/`myreceiver`/`myjob`/`MessageSender` 등 전부 제거
  - `git-workflow.md`: 브랜치/scope/커밋 예시 → `myrpg`/`mycalendar`/`mystudy` 기반으로 교체
  - `code-style.md`: 패키지/클래스/메서드/테스트 예시 → `myrpg` 전투 도메인(`BattleService`, `executeTurn`)으로 교체
  - `spec-conventions.md`: 스펙 예시 → 실제 `myrpg` 스펙(`001-character-progress-and-map-movement`, `002-npc-system`)으로 교체
  - `tech-stack.md`: 모듈 예시 → `mystudy`/`mycalendar`/`myrpg`, Batch 주석 명시
  - `new-module-guard.md`: 모듈명 예시 → 실제 모듈 스타일로 교체
- **검증**: `.kiro/steering/` 전체 `search_files`로 `mysender|myreceiver|myjob` 0건, `sendMessage` 등 잔여 예시 0건 확인 완료
- **남은 참고사항**: `deployment.md`/`tech-stack.md`의 JDK 25 표기는 **빌드 환경 사실**이므로 유지 (Java 타겟 21과 모순 없음)

### 1.0. mycrawler 삭제 커밋 푸시 및 3개 모듈 서버 재배포 (2026-08-20 22:40)
- **커밋**: `963e9ce` — `chore: mycrawler 모듈 삭제 및 인프라/설정 정리` (47 files, +31/-4930)
- **푸시**: `bd10a99..963e9ce main -> main` (GitHub `gonyda/myapps`)
- **서버 재배포 완료** (Oracle Cloud `/home/ubuntu/app/deploy.sh`):
  | 모듈 | 포트 | PID | 결과 |
  |---|---|---|---|
  | mystudy | 8080 | 2607435 | DEPLOY_SUCCESS |
  | mycalendar | 8082 | 2607668 | DEPLOY_SUCCESS |
  | myrpg | 8083 | 2607912 | DEPLOY_SUCCESS |
- **참고**: `deploy.sh`에 실행 권한(x)이 없어 `bash deploy.sh <모듈>`로 실행함

### 1.1. mycrawler 모듈 완전 삭제 및 인프라/설정 정리 (2026-08-20)
- **삭제 대상**: `mycrawler/` 모듈 및 `.kiro/specs/mycrawler/` 스펙 디렉토리 완전 삭제
- **설정 정리**:
  - `pom.xml`: `<module>mycrawler</module>` 및 `<dependencyManagement>` 내 `playwright` 의존성 제거
  - `.vscode/tasks.json`: `mycrawler: Run (local)` 태스크 제거
  - `.kiro/steering/infra/deployment.md` 및 `.clinerules/cline-global-rules.md`: 모듈 및 포트(8081) 목록 최신화
  - Oracle Cloud 원격 서버 (`/home/ubuntu/app/deploy.sh`): `mycrawler` 포트 매핑 케이스 제거 및 스크립트 갱신
- **검증**: `mystudy`, `mycalendar`, `myrpg` 3개 모듈 `BUILD SUCCESS` 및 CodeGraph 동기화 완료

### 1.2. AI 코딩 품질 관리를 위한 4대 가드레일 도입
| 가드레일 | 도구 / 플러그인 | 설정 및 역할 |
|---|---|---|
| **1. Spotless** | `com.diffplug.spotless:spotless-maven-plugin:2.44.5` | `googleJavaFormat(AOSP)` 기반 4칸 들여쓰기, 미사용 import 자동 제거, 개행 및 공백 정렬 |
| **2. Error Prone** | `com.google.errorprone:error_prone_core:2.36.0` | `maven-compiler-plugin` 내부 `<annotationProcessorPaths>` 및 compilerArgs(`--add-exports`, `--add-opens`) 연결, 컴파일 타임 정적 결함 차단 |
| **3. ArchUnit** | `com.tngtech.archunit:archunit-junit5:1.4.0` | Parent POM 공통 test 의존성 및 모듈별 `ArchitectureRuleTest.java` 작성 (Interfaces/Domain/Application 계층 규칙 강제) |
| **4. JaCoCo** | `org.jacoco:jacoco-maven-plugin:0.8.13` | `prepare-agent`(initialize), `report`(verify), `check`(verify) 바인딩 및 커버리지 검증 |
| **5. CodeGraph Sync** | `codegraph sync` | 변경된 코드베이스 인덱스를 지식 그래프에 즉시 동기화 |

---

## 현재 프로젝트 활성 모듈 및 포트 매핑

| 모듈 | 포트 | 설명 |
|---|---|---|
| `mystudy` | 8080 | 영어 학습 웹 애플리케이션 |
| `mycalendar` | 8082 | 캘린더/일정 관리 웹 애플리케이션 |
| `myrpg` | 8083 | 텍스트/웹 기반 RPG 게임 애플리케이션 |
| *(8081)* | - | *(구 mycrawler 포트 - 현재 비어 있음, 신규 모듈용 예약 가능)* |

---

## 작업 트리 (수정/신규 파일 목록)

| 파일 | 변경 구분 | 내용 |
|---|---|---|
| `.kiro/steering/project/tech-stack.md` | 수정 | Java 21(타겟)/JDK 25(빌드 환경) 명확화, 모듈 예시 실제 모듈로 교체 |
| `.kiro/steering/coding/code-style.md` | 수정 | Java 21 표기 및 예시를 myrpg 전투 도메인으로 교체 (유령 모듈 예시 제거) |
| `.kiro/steering/workflow/git-workflow.md` | 수정 | 브랜치·scope·커밋 예시를 실제 모듈(myrpg/mycalendar/mystudy)로 교체 |
| `.kiro/steering/project/spec-conventions.md` | 수정 | 스펙 폴더 예시를 실제 myrpg 스펙으로 교체 |
| `.kiro/steering/module/new-module-guard.md` | 수정 | 모듈명 예시 최신화 (`myreceiver`/`myjob` 제거) |
| `.clinerules/cline-global-rules.md` | 수정 | SSOT 원칙 기반 전면 리팩토링 (가드레일 명령·Spec 포맷·CodeGraph 상세 중복 제거, steering 참조 인덱스 구조화, §1~§4 재구성) |
| `mycrawler/` | 삭제 | 모듈 소스코드, 리소스, 테스트 전체 삭제 |
| `.kiro/specs/mycrawler/` | 삭제 | 크롤러 스펙 문서 전체 삭제 |
| `pom.xml` | 수정 | mycrawler 모듈 및 playwright 의존성 제거 |
| `.vscode/tasks.json` | 수정 | mycrawler 로컬 실행 태스크 제거 |
| `.kiro/steering/infra/deployment.md` | 수정 | 포트 매핑 및 배포 설명에서 mycrawler 제거 |
| `.clinerules/cline-global-rules.md` | 수정 | 모듈 목록 예시 최신화 |
| Oracle Cloud `/home/ubuntu/app/deploy.sh` | 수정 | get_port() 함수 내 mycrawler 케이스 제거 |
| `.clinerules/memory-bank/activeContext.md` | 수정 | mycrawler 삭제 및 모듈 구성 최신화 |
