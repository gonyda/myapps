# AGENTS.md — AI Agent 공통 진입점 및 전체 개발 규칙

---

## 1. 아키텍처 철학

```
myapps/                                # Git 레포지토리 루트
│
├── AGENTS.md                          # [공통 진입점] 모든 AI Agent의 최우선 규칙 및 가이드
│
├── rules/                             # [규칙 SSOT] 프로젝트 공통 개발·아키텍처·품질 규칙 원본
│   ├── coding/                        # Java 코딩 컨벤션, final, 불변성, 테스트 규칙
│   ├── infra/                         # 배포 절차, 인프라 및 포트 매핑 규칙
│   ├── module/                        # 신규 모듈 생성 가드 및 DDD 템플릿
│   ├── project/                       # pom.xml 규칙, 기술 스택, Spec 작성 표준
│   └── workflow/                      # CodeGraph 우선, 빌드 검증 5대 가드레일, Git 전략
│
├── memory-bank/                       # [지식 지속성] 세션 간 작업 맥락 및 영속 기억
│   ├── activeContext.md               # 현재 작업 상태, 최근 변경사항, 다음 단계 (필수 갱신)
│   └── memory-bank.md                 # 메모리뱅크 개념, 작성 원칙 및 갱신 프로토콜
│
├── skills/                            # [스킬 SSOT] 모든 워크플로우의 실제 실행 로직 원본
│   ├── sdd/
│   │   └── SKILL.md                   # SDD(Spec-Driven Development) 5단계 실행 프로세스 정의
│   └── myrpg-data-balance/
│       └── SKILL.md                   # MyRPG 게임 데이터 밸런싱 및 대화형 Q&A/파이썬 검증 프로토콜
│
├── tools/                             # [도구 모음]
│   └── balance/                       # 파이썬 밸런스 검증 엔진 (verify_equipment/monster/skill.py)
│
├── .kiro/                             # [Kiro 전용 영역]
│   └── specs/                         # 모듈별 요구사항(requirements) → 설계(design) → 작업(tasks) Spec
│
├── .cline/                            # [Cline 전용 설정]
│   └── skills/
│       ├── sdd/SKILL.md               # Cline /sdd 명령용 Thin Wrapper (skills/sdd/SKILL.md 참조)
│       └── myrpg-data-balance/SKILL.md# Cline /myrpg-data-balance 명령용 Thin Wrapper
│
├── .agents/                           # [Antigravity 전용 설정]
│   ├── mcp_config.json                # Antigravity 전용 MCP 서버 설정
│   └── skills/
│       ├── sdd/SKILL.md               # Antigravity /sdd 명령용 Thin Wrapper (skills/sdd/SKILL.md 참조)
│       └── myrpg-data-balance/SKILL.md# Antigravity /myrpg-data-balance 명령용 Thin Wrapper
│
└── {애플리케이션 모듈}/                # mystudy, mycalendar, myrpg 등
```

### 3대 핵심 원칙
1. **SSOT (Single Source of Truth)**: 모든 규칙은 `rules/`, 모든 스킬 로직은 `skills/`에만 단일 원본으로 존재합니다.
2. **Thin Wrapper (참조 포인터)**: `.cline/`, `.agents/` 등 도구 전용 디렉토리는 공통 원본(`skills/`)을 가리키는 최소한의 참조 포인터만 유지합니다.
3. **Agent 독립적 Spec & Memory**: `.kiro/specs/`와 `memory-bank/`는 도구에 종속되지 않는 범용 마크다운 형식으로 관리되어 모든 Agent가 자유롭게 읽고 씁니다.

---

## 2. 작업 시작 전 필수 절차

모든 작업(코드 작성, 리팩토링, 버그 수정, 문서 작업)을 시작하기 전에 반드시 아래 단계를 수행합니다:

### 2.1. Memory Bank 읽기 (맥락 및 관리 원칙 파악)
1. `memory-bank/memory-bank.md`의 관리 원칙(슬라이딩 윈도우, 무한 누적 금지, 이전 작업 압축 규칙)을 준수합니다.
2. `memory-bank/activeContext.md`를 읽어 현재 작업 상태, 최근 결정사항, 다음 단계를 확인합니다.
3. 만약 파일이 비어있거나 세션이 새로 시작된 경우 `git status` 및 최근 커밋 내역을 확인하여 맥락을 동기화합니다.

### 2.2. Rules 및 Skills 확인 (해당 작업 규칙 로드)
수행할 작업 유형에 맞춰 아래 `rules/` 및 `skills/`의 관련 정의를 확인합니다:

| 작업 유형 | 확인 대상 규칙/스킬 파일 |
|---|---|
| **기능 개발 / 리팩토링** | `skills/sdd/SKILL.md` (5단계 SDD 워크플로우) |
| **게임 데이터/밸런스 추가·수정** | `skills/myrpg-data-balance/SKILL.md` (대화형 Q&A + 파이썬 검증) |
| **Spec 문서 작성/수정** | `rules/project/spec-conventions.md` |
| **코드 구조 및 심볼 분석** | `rules/workflow/codegraph-first.md` |
| **Java 소스 코드 작성/수정** | `rules/coding/code-style.md`, `rules/project/tech-stack.md` |
| **pom.xml 의존성/플러그인 수정** | `rules/project/pom-conventions.md` |
| **신규 모듈 생성** | `rules/module/new-module-guard.md`, `rules/module/module-template.md` |
| **Task 완료 및 빌드 검증** | `rules/workflow/task-build-validation.md` |
| **Git 커밋 / PR 생성** | `rules/workflow/git-workflow.md` |
| **원격 서버 배포** | `rules/infra/deployment.md` |

---

## 3. 개발 워크플로우 (SDD: Spec-Driven Development)

모든 기능 개발 및 리팩토링은 `skills/sdd/SKILL.md`에 정의된 **5단계 SDD 프로세스**를 따릅니다.

```
[1. 맥락 파악]
  memory-bank/memory-bank.md(원칙) & memory-bank/activeContext.md(맥락) 확인
       ↓
[2. Spec 문서 3종 작성]
  .kiro/specs/{모듈명}/{순번}-{기능명}/
  ├── requirements.md (요구사항)
  ├── design.md       (설계 및 확정값)
  └── tasks.md        (체크포인트 단위 작업 목록)
       ↓
[3. 사용자 검토 및 승인]
  사용자 피드백 수렴 및 Spec 확정
       ↓
[4. tasks.md 순차 구현 & 5대 품질 가드레일 검증]
  Task 완료마다 Spotless → Error Prone → ArchUnit → JaCoCo → PMD/CPD 검증
  코드 변경 후 `codegraph sync` 동기화
       ↓
[5. Memory Bank 갱신 (Compaction)]
  memory-bank.md 원칙에 따라 완료된 작업은 요약(Compacted)하고,
  activeContext.md에 현재/다음 작업 맥락 중심으로 슬림하게 갱신
```

### 5대 품질 가드레일 (Task 완료 필수 조건)
1. **Spotless**: Java 소스 포맷팅 자동 교정
2. **Error Prone**: 정적 결함 컴파일 타임 차단
3. **ArchUnit**: 계층형 아키텍처 규칙 강제
4. **JaCoCo**: 테스트 커버리지 80% 달성 검증
5. **PMD & CPD**: 복잡도/안티패턴/중복 코드 검증

> **통합 검증 파이프라인 (Task 완료 시 필수 실행)**:
> ```bash
> mvn -B -q spotless:apply -pl {modulename} && (mvn -B clean install -pl {modulename} -am > /tmp/mvn.log 2>&1 || (tail -n 30 /tmp/mvn.log && exit 1)) && tail -n 12 /tmp/mvn.log && codegraph sync
> ```
> 상세 규칙 및 실패 시 대처법은 `rules/workflow/task-build-validation.md`를 준수합니다.

---

## 4. MCP 툴 우선 사용 원칙

코드베이스 분석 및 시스템 작업 시 다음 우선순위로 툴을 사용합니다:

1. **CodeGraph** (`codegraph_explore`): 코드 구조, 심볼 선언, 호출 관계 파악 시 1순위 사용
2. **Oracle Cloud SSH** (`oracle-cloud-ssh`): 원격 서버 인프라 상태 점검 및 배포
3. **Oracle DB** (`oracle-db`): 데이터베이스 스키마 및 데이터 조회
4. **파일 도구** (`view_file` / `grep_search`): CodeGraph 탐색 후 세부 구현 확인 및 수정 시 사용

> 상세 규칙: `rules/workflow/codegraph-first.md` 참고

---

## 5. rules/ 및 skills/ 전체 인덱스 (SSOT)

### 5.1. Rules (개발·아키텍처 규칙)
| 분류 | 규칙 파일 | 핵심 내용 |
|---|---|---|
| **Coding** | `rules/coding/code-style.md` | Java 21 문법, 불변성(`final`), Lombok 금지, Spring Boot 4.0 테스트 표준 |
| **Infra** | `rules/infra/deployment.md` | Oracle Cloud VM 배포 절차, 환경변수, 모듈별 포트 매핑 (mystudy:8080, mycalendar:8082, myrpg:8083) |
| **Module** | `rules/module/module-template.md` | 신규 모듈용 DDD 패키지 구조 및 템플릿 |
| **Module** | `rules/module/new-module-guard.md` | 신규 모듈 추가 전 사전 점검 체크리스트 |
| **Project** | `rules/project/pom-conventions.md` | Parent POM 및 모듈 POM 설정 표준, 5대 가드레일 플러그인 정의 |
| **Project** | `rules/project/spec-conventions.md` | Spec 3종 문서 구조 및 3자리 순번 폴더 네이밍 규칙 |
| **Project** | `rules/project/tech-stack.md` | 기술 스택 (Java 21/JDK 25, Spring Boot 4.0, Maven 멀티모듈) |
| **Workflow** | `rules/workflow/codegraph-first.md` | 탐색 툴 우선순위 및 CodeGraph 동기화 규칙 |
| **Workflow** | `rules/workflow/git-workflow.md` | Git 브랜치 전략, 커밋 메시지 컨벤션, PR 규칙 |
| **Workflow** | `rules/workflow/task-build-validation.md` | 5대 가드레일 빌드 검증 명령어 및 성공 기준 |

### 5.2. Skills (실행 워크플로우)
| 스킬명 | SSOT 위치 | 핵심 내용 |
|---|---|---|
| **sdd** | `skills/sdd/SKILL.md` | SDD 5단계 개발 및 5대 품질 가드레일 검증 워크플로우 |
| **myrpg-data-balance** | `skills/myrpg-data-balance/SKILL.md` | 게임 데이터(아이템/몬스터/스킬/맵) Q&A 작성 및 `tools/balance/` 파이썬 자동 검증 워크플로우 |

---

## 6. AI Agent별 연동 및 실행 가이드

| 도구 (Agent) | 진입 및 연동 방식 | 역할 및 동작 |
|---|---|---|
| **Kiro** | 시스템 프롬프트 / 워크스페이스 로드 | `.kiro/specs/` 내 Spec 문서를 관리하며 작업 수행 |
| **Cline** | `.cline/skills/{스킬명}/SKILL.md` | `/sdd`, `/myrpg-data-balance` 명령 시 `skills/` 원본을 읽고 워크플로우 실행 |
| **Antigravity** | `.agents/skills/{스킬명}/SKILL.md` | `/sdd`, `/myrpg-data-balance` 명령 또는 스킬 감지 시 `skills/` 원본을 읽고 워크플로우 실행 |

> **에이전트 공통 행동 지침**:
> 어떤 AI Agent이든 실행 즉시 이 파일(`AGENTS.md`)을 진입점으로 인식하고, **`memory-bank/memory-bank.md`(관리 원칙) 및 `memory-bank/activeContext.md`(현재 맥락)를 읽은 후 작업을 시작**해야 합니다. 모든 규칙과 스킬은 `rules/`와 `skills/`의 SSOT를 참조합니다.