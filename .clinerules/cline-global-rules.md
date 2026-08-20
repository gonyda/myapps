# .clinerules — Cline 전역 규칙 (Kiro IDE 워크플로우 적용)

이 파일은 myapps 프로젝트에서 Cline이 따라야 할 규칙들을 정의합니다.
아래 규칙은 `.kiro/` 디렉토리의 Kiro IDE 설정을 기반으로 합니다.

> **단일 진실 공급원(SSOT) 원칙**: 규칙의 상세 내용은 `.kiro/steering/` 문서가 원본(source of truth)입니다.
> 본 파일은 **워크플로우 흐름과 참조 인덱스** 역할을 수행하며, 상세 규칙은 중복 서술하지 않습니다.
> 두 곳의 내용이 어긋날 때는 항상 `.kiro/steering/` 문서를 기준으로 합니다.
>
> 참조 대상 스티어링 문서들은 `inclusion: always`로 설정되어 항상 컨텍스트에 포함되므로, 상세 규칙은 해당 문서를 직접 참조하면 됩니다.

---

## 1. 개발 워크플로우: 메모리뱅크 → Spec 문서 3종 → 구현 및 가드레일 검증

모든 작업(코드 작성, 리팩토링, 버그 수정, 문서 작업 포함)은 아래 순서를 따릅니다:

```
0. 메모리뱅크 읽기 (모든 작업 시작 전 필수)
   ├── .clinerules/memory-bank/activeContext.md (현재 작업 상태·최근 결정사항)
   └── 없거나 비어 있으면 §4 메모리뱅크 갱신 절차에 따라 초기화

1. Spec 문서 3종 작성 (PLAN MODE, 기능 개발 시)
   ├── requirements.md  (요구사항 정의)
   ├── design.md        (설계)
   └── tasks.md         (구현 태스크 목록)
   └── 구조·네이밍·작성 규칙: .kiro/steering/project/spec-conventions.md 참고

2. 사용자 검토 및 승인

3. tasks.md 기반 구현 (ACT MODE)
   └── 각 Task 완료 시 4대 가드레일 검증 및 CodeGraph 동기화 필수
   └── 검증 명령어·성공/실패 기준: .kiro/steering/workflow/task-build-validation.md 참고

4. 메모리뱅크 자동 업데이트 (§4 절차에 따라 작업 결과 반영)
```

---

## 2. 스티어링 파일 참조 (SSOT 인덱스)

중요 결정사항은 아래 스티어링 파일을 우선 참조합니다.
각 파일이 해당 주제의 단일 소스이며, 상세 내용은 반드시 해당 파일을 읽습니다.

### 2.1. 코드 관련

- **코딩 스타일**: `.kiro/steering/coding/code-style.md`
  - **참조 시점**: 새 Java/Kotlin 파일 생성, 기존 파일 수정, 리팩토링, 코드 포맷 변경 등 소스 코드를 변경할 때 반드시 읽고 따라야 합니다.
  - **참조 목적**: 네이밍 규칙, 포맷팅, 주석 스타일, 코드 구조 컨벤션을 준수하기 위해 참조합니다.

- **코드그래프 우선**: `.kiro/steering/workflow/codegraph-first.md`
  - **참조 시점**: 코드 구조 파악, 심볼 탐색, 호출 관계 분석이 필요할 때 항상 `codegraph`를 사용하기 전에 읽습니다.
  - **참조 목적**: `codegraph_explore` MCP 사용 방법과 우선 순위를 확인하기 위해 참조합니다.

### 2.2. 작업 워크플로우 관련

- **깃 워크플로우**: `.kiro/steering/workflow/git-workflow.md`
  - **참조 시점**: 브랜치 생성, 커밋, PR 생성, push, rebase 등 git 명령을 실행하기 전에 반드시 읽습니다.
  - **참조 목적**: 브랜치 전략(`main` 직접 push 금지), 커밋 메시지 컨벤션, PR 규칙을 준수하기 위해 참조합니다.

- **빌드 및 품질 가드레일 검증**: `.kiro/steering/workflow/task-build-validation.md`
  - **참조 시점**: 각 Task 완료 후 빌드 및 가드레일 검증이 필요할 때 읽습니다.
  - **참조 목적**: 4대 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo) 검증 파이프라인과 성공/실패 처리 기준을 확인하기 위해 참조합니다.

### 2.3. 프로젝트 설정 관련

- **스펙 문서 규칙**: `.kiro/steering/project/spec-conventions.md`
  - **참조 시점**: spec 문서(requirements.md, design.md, tasks.md)를 작성하거나 수정할 때 읽습니다.
  - **참조 목적**: spec 문서 구조, 용어 정의 규칙, 저장 위치 컨벤션을 준수하기 위해 참조합니다.

- **POM 컨벤션**: `.kiro/steering/project/pom-conventions.md`
  - **참조 시점**: `pom.xml` 파일을 수정하거나 의존성을 추가/제거할 때 읽습니다.
  - **참조 목적**: 모듈 구조, 의존성 버전 관리, 4대 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo) 설정을 준수하기 위해 참조합니다.

- **기술 스택**: `.kiro/steering/project/tech-stack.md`
  - **참조 시점**: 기술 스택을 선정하거나 변경할 때, 새 라이브러리/프레임워크 도입을 검토할 때 읽습니다.
  - **참조 목적**: 표준 기술 스택과 허용/금지 기술을 확인하기 위해 참조합니다.

### 2.4. 모듈 관련

- **모듈 템플릿**: `.kiro/steering/module/module-template.md`
  - **참조 시점**: 신규 Maven 모듈을 추가하거나 기존 모듈의 구조를 참조할 때 읽습니다.
  - **참조 목적**: 표준 모듈 디렉토리 구조(application, domain, infrastructure, interfaces)를 준수하기 위해 참조합니다.

- **신규 모듈 가드**: `.kiro/steering/module/new-module-guard.md`
  - **참조 시점**: 신규 모듈을 추가하기 전에 반드시 읽습니다.
  - **참조 목적**: 신규 모듈 추가 시 확인해야 할 사전 조건과 가이드라인을 확인하기 위해 참조합니다.

### 2.5. 도메인/인프라 관련

- **밸런스 가이드**: `.kiro/steering/myrpg/data-balance-guide.md`
  - **참조 시점**: `myrpg` 모듈의 게임 데이터(장비, 몬스터, 스킬), 게임 밸런스를 조정할 때 읽습니다.
  - **참조 목적**: 데이터 검증 스크립트 사용법, 밸런스 수치 규칙, 데이터 포맷을 준수하기 위해 참조합니다.

- **배포**: `.kiro/steering/infra/deployment.md`
  - **참조 시점**: 배포 설정을 변경하거나 CI/CD 파이프라인을 구성할 때 읽습니다.
  - **참조 목적**: 배포 프로세스, 환경 설정(로컬/프로덕션), 배포 스크립트 사용법을 확인하기 위해 참조합니다.

---

## 3. MCP 툴 우선 사용

코드 탐색 시 아래 순서로 툴을 사용합니다.
(직접 read 허용 조건 등 상세 규칙: `.kiro/steering/workflow/codegraph-first.md` 참고)

1. **`codegraph_explore`** (MCP) — 1순위. 심볼 구조, 호출 관계, 의존성 파악 및 코드 읽기
2. `read_file` / `search_files` — codegraph로 부족한 세부 내용 확인 시에만 사용

### 서버 접속 및 DB 조회

- **Oracle Cloud 서버**: `oracle-cloud-ssh` MCP (`read-command`, `run-command`, `sftp-upload`, `sftp-download`)를 우선 사용
- **Oracle DB**: `oracle-db` MCP로 DB 연결 및 조회 (`connect`, `sql_run`, `schema_information`)를 우선 사용

---

## 4. 메모리뱅크 자동 관리

> 메모리뱅크의 일반적인 개념·역할·갱신 원칙은 `.clinerules/memory-bank.md` 참고.
> 아래는 이 프로젝트(myapps)의 구체적 운영 절차입니다.

### 4.1. 메모리뱅크 파일 위치

```
.clinerules/memory-bank/
└── activeContext.md   (현재 작업 상태·최근 결정사항·다음 단계)
```

### 4.2. 작업 시작 전 (필수)

- 모든 작업(코드 작성, 리팩토링, 버그 수정, 문서 작업 포함)을 시작하기 전에 반드시 `activeContext.md`를 읽는다.
- 파일이 없거나 비어 있으면:
  1. `git --no-pager log --oneline -30` 로 최근 커밋을 확인
  2. `git status --short` 로 미커밋 상태를 확인
  3. 진행 중인 spec(`.kiro/specs/`)과 관련 docs를 확인
  4. 위 내용으로 `activeContext.md`를 초기화한다.

### 4.3. 작업 종료 후 (필수)

작업이 끝나면 `activeContext.md`에 다음을 반영한다.

| 갱신 항목 | 설명 |
|---|---|
| 진행 상태 요약 | 단계별 체크박스 완료/미완료 갱신 |
| 작업 트리 | 수정/신규 파일 목록(+커밋 여부) 반영 |
| 현재 스펙의 확정 설계값 | 새로 확정된 상수·규칙·수치 기록 |
| 다음 단계 | 이어서 처리할 다음 Task/체크포인트 명시 |
| 의사사항 | 스펙 문서 갱신 필요 등 후속 조치 기록 |
| 최종 업데이트 일자 | `Asia/Seoul` 기준 갱신 |

### 4.4. 메모리뱅크 갱신 시점 (자동)

아래 시점마다 갱신한다 (사용자에게 갱신 요청을 받았을 때에만 하면 안 됨):

1. **Task/체크포인트 완료** — 진행 상태 요약의 체크박스 갱신
2. **협의 확정값 등장** — 설계값이 확정되면 즉시 기록
3. **Spec 문서 3종 완료** — 시작 전에 스펙 개요를 기록
4. **커밋/빌드 완료** — 새 커밋 해시를 최근 커밋 하이라이트에 반영
5. **다음 단계 이동 시** — 다음에 할 일을 명시