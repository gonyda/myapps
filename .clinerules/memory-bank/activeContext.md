# Active Context

> 최종 업데이트: 2026-08-20 22:40 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`.clinerules/cline-global-rules.md` 반영)

> **전역 규칙 핵심 요약**:
> - 개발 워크플로우: **Spec 문서 3종 → 사용자 검토 → 구현**
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **빌드 및 4대 가드레일 검증**: 각 Task 완료 전 `mvn -B -q spotless:apply -pl {modulename} && (mvn -B clean install -pl {modulename} -am > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log && codegraph sync` 필수 실행 (-B: 토큰 절약)
> - 소스 정리: 미사용 import 제거, 매직넘버 상수화, 메서드 분리 (50줄 초과 시)

## 1. 최근 작업 내역

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
| `mycrawler/` | 삭제 | 모듈 소스코드, 리소스, 테스트 전체 삭제 |
| `.kiro/specs/mycrawler/` | 삭제 | 크롤러 스펙 문서 전체 삭제 |
| `pom.xml` | 수정 | mycrawler 모듈 및 playwright 의존성 제거 |
| `.vscode/tasks.json` | 수정 | mycrawler 로컬 실행 태스크 제거 |
| `.kiro/steering/infra/deployment.md` | 수정 | 포트 매핑 및 배포 설명에서 mycrawler 제거 |
| `.clinerules/cline-global-rules.md` | 수정 | 모듈 목록 예시 최신화 |
| Oracle Cloud `/home/ubuntu/app/deploy.sh` | 수정 | get_port() 함수 내 mycrawler 케이스 제거 |
| `.clinerules/memory-bank/activeContext.md` | 수정 | mycrawler 삭제 및 모듈 구성 최신화 |
