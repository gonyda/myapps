# .clinerules — Cline 전역 규칙 (Kiro IDE 워크플로우 적용)

이 파일은 myapps 프로젝트에서 Cline이 따라야 할 규칙들을 정의합니다.
아래 규칙은 `.kiro/` 디렉토리의 Kiro IDE 설정을 기반으로 합니다.

---

## 1. 개발 워크플로우: Spec 문서 3종 → 구현

모든 기능 개발은 아래 순서를 따릅니다:

```
1. Spec 문서 3종 작성 (PLAN MODE)
   ├── requirements.md  (요구사항 정의)
   ├── design.md        (설계)
   └── tasks.md         (구현 태스크 목록)

2. 사용자 검토 및 승인

3. tasks.md 기반 구현 (ACT MODE)
   └── 각 Task 완료 시 mvn test + mvn clean install 검증
```

### Spec 문서 위치 규칙

```
.kiro/specs/
└── {모듈명}/
    └── {순번}-{기능명}/
        ├── requirements.md
        ├── design.md
        └── tasks.md
```

- **모듈명**: Maven 모듈명과 동일 (myrpg, mycalendar, mycrawler, mystudy)
- **순번**: 3자리 0-padded 숫자 (001, 002, ...)
- **기능명**: kebab-case 영문 소문자

---

## 2. Spec 문서 3종 규칙

### requirements.md
- Introduction: 스펙 배경과 해결하려는 문제
- Glossary: 기존 용어 + 신규 용어 정의
- Requirements: User Story + Acceptance Criteria (Given/When/Then)

### design.md
- Overview: 핵심 원칙과 이번 스펙 범위
- Architecture: 변경될 모듈 구조 (신규/확장/유지)
- 데이터 변경: JSON 스키마 등
- 세부 설계: 각 요구사항별 구현 방안

### tasks.md
- Overview: 구현 순서 원칙
- Tasks: 체크박스 리스트 (하위 Task 포함)
- 각 Task 완료 전 테스트 + 빌드 검증

---

## 3. 구현 원칙

### 코딩 컨벤션
- **Java 25**, Spring Boot 4.0
- `var` 사용 금지, `@Autowired` 금지, Lombok 금지
- VO/DTO는 `record` 타입
- `final` 파라미터/지역변수 필수
- 커스텀 예외 사용 (`RuntimeException` 직접 금지)
- 매직 넘버 `private static final` 상수화
- 메서드 50줄 초과 시 분리 필수
- 의존성 주입은 순수 생성자 주입만 사용

### 테스트
- 기능 구현시 테스트 코드 필수 작성
- 서비스: 단위 + PBT (`@ExtendWith(MockitoExtension.class)` + jqwik)
- 컨트롤러: `@WebMvcTest` + `@MockitoBean`
- 레포지토리: `@DataJpaTest`
- jqwik PBT에서는 `@Mock` 대신 `Mockito.mock()` 직접 호출
- Spring Boot 4.0: `@MockBean`/`@SpyBean` 금지 → `@MockitoBean`/`@MockitoSpyBean` 사용
- Jackson 3: `com.fasterxml.jackson` → `tools.jackson` 패키지 사용

### 빌드 검증
- 각 Task 완료 전 반드시:
  1. 밸런스 검증 스크립트(`myrpg` 데이터 변경 시): `cd tools/balance && python3 verify_equipment.py` / `verify_monster.py` / `verify_skill.py` — `data-balance-guide.md §C-6` 참조
  2. `mvn test -pl {모듈명}`
  3. `mvn clean install -pl {모듈명} -am`
- `BUILD SUCCESS` 확인 후에만 Task 완료 처리

### 소스 수정 Task 완료 전 코드 정리 (필수)
- 미사용 import 제거 (와일드카드 import 금지)
- 미사용 변수/필드 제거
- 매직 넘버 상수화
- 중복 코드 메서드 추출
- 메서드 분리 (50줄 초과 시 필수)
- 불필요한 주석 제거 (`TODO`/`FIXME` 해결 후 제거)

---

## 4. 스티어링 파일 참조

중요 결정사항은 아래 스티어링 파일을 우선 참조합니다:

### 4.1. 코드 관련

- **코딩 스타일**: `.kiro/steering/coding/code-style.md`
  - **참조 시점**: 새 Java/Kotlin 파일 생성, 기존 파일 수정, 리팩토링, 코드 포맷 변경 등 소스 코드를 변경할 때 반드시 읽고 따라야 합니다.
  - **참조 목적**: 네이밍 규칙, 포맷팅, 주석 스타일, 코드 구조 컨벤션을 준수하기 위해 참조합니다.

- **코드그래프 우선**: `.kiro/steering/workflow/codegraph-first.md`
  - **참조 시점**: 코드 구조 파악, 심볼 탐색, 호출 관계 분석이 필요할 때 항상 `codegraph`를 사용하기 전에 읽습니다.
  - **참조 목적**: `codegraph_explore` MCP 사용 방법과 우선 순위를 확인하기 위해 참조합니다.

### 4.2. 작업 워크플로우 관련

- **깃 워크플로우**: `.kiro/steering/workflow/git-workflow.md`
  - **참조 시점**: 브랜치 생성, 커밋, PR 생성, push, rebase 등 git 명령을 실행하기 전에 반드시 읽습니다.
  - **참조 목적**: 브랜치 전략(`main` 직접 push 금지), 커밋 메시지 컨벤션, PR 규칙을 준수하기 위해 참조합니다.

- **빌드 검증**: `.kiro/steering/workflow/task-build-validation.md`
  - **참조 시점**: 각 Task 완료 후 빌드 검증이 필요할 때 읽습니다.
  - **참조 목적**: `mvn test`, `mvn clean install` 실행 순서와 검증 기준을 확인하기 위해 참조합니다.

### 4.3. 프로젝트 설정 관련

- **스펙 문서 규칙**: `.kiro/steering/project/spec-conventions.md`
  - **참조 시점**: spec 문서(requirements.md, design.md, tasks.md)를 작성하거나 수정할 때 읽습니다.
  - **참조 목적**: spec 문서 구조, 용어 정의 규칙, 저장 위치 컨벤션을 준수하기 위해 참조합니다.

- **POM 컨벤션**: `.kiro/steering/project/pom-conventions.md`
  - **참조 시점**: `pom.xml` 파일을 수정하거나 의존성을 추가/제거할 때 읽습니다.
  - **참조 목적**: 모듈 구조, 의존성 버전 관리, 플러그인 설정 컨벤션을 준수하기 위해 참조합니다.

- **기술 스택**: `.kiro/steering/project/tech-stack.md`
  - **참조 시점**: 기술 스택을 선정하거나 변경할 때, 새 라이브러리/프레임워크 도입을 검토할 때 읽습니다.
  - **참조 목적**: 표준 기술 스택(Java 25, Spring Boot 4.0 등)과 허용/금지 기술을 확인하기 위해 참조합니다.

### 4.4. 모듈 관련

- **모듈 템플릿**: `.kiro/steering/module/module-template.md`
  - **참조 시점**: 신규 Maven 모듈을 추가하거나 기존 모듈의 구조를 참조할 때 읽습니다.
  - **참조 목적**: 표준 모듈 디렉토리 구조(application, domain, infrastructure, interfaces)를 준수하기 위해 참조합니다.

- **신규 모듈 가드**: `.kiro/steering/module/new-module-guard.md`
  - **참조 시점**: 신규 모듈을 추가하기 전에 반드시 읽습니다.
  - **참조 목적**: 신규 모듈 추가 시 확인해야 할 사전 조건과 가이드라인을 확인하기 위해 참조합니다.

### 4.5. 도메인/인프라 관련

- **밸런스 가이드**: `.kiro/steering/myrpg/data-balance-guide.md`
  - **참조 시점**: `myrpg` 모듈의 게임 데이터(장비, 몬스터, 스킬), 게임 밸런스를 조정할 때 읽습니다.
  - **참조 목적**: 데이터 검증 스크립트 사용법, 밸런스 수치 규칙, 데이터 포맷을 준수하기 위해 참조합니다.

- **배포**: `.kiro/steering/infra/deployment.md`
  - **참조 시점**: 배포 설정을 변경하거나 CI/CD 파이프라인을 구성할 때 읽습니다.
  - **참조 목적**: 배포 프로세스, 환경 설정(로컬/프로덕션), 배포 스크립트 사용법을 확인하기 위해 참조합니다.

---

## 5. MCP 툴 우선 사용

코드 탐색 시 아래 순서로 툴을 사용합니다:

1. **`codegraph_explore`** (MCP) — 1순위. 심볼 구조, 호출 관계, 의존성 파악 및 코드 읽기
2. `read_file` / `search_files` — codegraph로 부족한 세부 내용 확인 시에만 사용

### 서버 접속 및 DB 조회

- **Oracle Cloud 서버**: `oracle-cloud-ssh` MCP (`read-command`, `run-command`, `sftp-upload`, `sftp-download`)를 우선 사용
- **Oracle DB**: `oracle-db` MCP로 DB 연결 및 조회 (`connect`, `sql_run`, `schema_information`)를 우선 사용
<!-- End of clinerules -->