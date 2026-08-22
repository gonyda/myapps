---
name: sdd
description: SDD (Spec-Driven Development) 프로세스의 단일 진실 공급원(SSOT) 정의서입니다.
---

# SDD (Spec-Driven Development) Skill

## 개요

SDD(Spec-Driven Development)는 요구사항 → 설계 → 작업 명세 → 구현 → 검증의 5단계로 이루어지는 개발 방법론입니다. 모든 코드 변경은 Spec 문서 3종 작성으로 시작하며, 사용자 검토 후 구현합니다.

---

## SDD 워크플로우 (5단계 프로세스)

### 1. 작업 맥락 파악 (Context Check)

- `memory-bank/memory-bank.md`의 관리 원칙(슬라이딩 윈도우, 무한 누적 금지, 압축 규칙)을 준수
- `memory-bank/activeContext.md`를 읽어 현재 작업 상태, 최근 결정사항, 다음 단계 확인
- 세션이 새로 시작되었거나 맥락이 불분명한 경우 `git status` 및 최근 커밋 내역 확인

### 2. Spec 문서 3종 작성 (Spec Authoring)

작업 대상(모듈/기능)에 대해 `.kiro/specs/{모듈명}/{순번}-{기능명}/` 위치에 아래 3개 문서를 작성합니다.  
> ⚠️ **필수 규칙**: Spec 문서 작성 시 반드시 `.kiro/specs/` 폴더에 위치한 **표준 템플릿 3종(`_requirements.md`, `_design.md`, `_tasks.md`)을 복사하여 기반으로 작성**해야 합니다.

```bash
# 템플릿 복사 예시
mkdir -p .kiro/specs/{모듈명}/{순번}-{기능명}
cp .kiro/specs/_requirements.md .kiro/specs/{모듈명}/{순번}-{기능명}/requirements.md
cp .kiro/specs/_design.md       .kiro/specs/{모듈명}/{순번}-{기능명}/design.md
cp .kiro/specs/_tasks.md        .kiro/specs/{모듈명}/{순번}-{기능명}/tasks.md
```

#### 2.1. `requirements.md` (템플릿: `.kiro/specs/_requirements.md`)
- **목적**: 해결하려는 문제와 요구사항을 명확히 정의
- **작성 항목**: 배경/목적, In/Out-of-Scope, 도메인 용어사전(`Glossary`), User Story 및 EARS 기반 `Acceptance Criteria`(WHEN, THE SHALL, IF THEN, WHERE), 5대 품질 가드레일

#### 2.2. `design.md` (템플릿: `.kiro/specs/_design.md`)
- **목적**: 요구사항을 만족하는 구체적인 아키텍처 및 상세 설계 정의
- **작성 항목**: 핵심 설계 결정 및 트레이드오프 표, DDD 4계층 패키지 구조, Mermaid 시퀀스 다이어그램, 계층별 세부 인터페이스, 데이터 모델(엔티티/Record/JSON 카탈로그), jqwik PBT용 `Correctness Properties`

#### 2.3. `tasks.md` (템플릿: `.kiro/specs/_tasks.md`)
- **목적**: 구현 단위 작업을 순차적 체크포인트로 분할
- **작성 항목**: Bottom-Up 5단계 마일스톤(도메인/DTO → 서비스 → 컨트롤러 → 프론트엔드 → 5대 가드레일 통합 검증), 단계별 빌드/테스트 **체크포인트**(모든 Task 필수), 요구사항 역추적 링크

### 3. 사용자 검토 및 승인 (User Review & Approval)

- 작성된 Spec 문서 3종을 사용자에게 제시
- 사용자가 검토하고 승인할 때까지 구현 단계로 넘어가지 않음
- 피드백 반영 후 재검토 필요 시 Spec 문서 갱신

### 4. 구현 및 5대 품질 가드레일 검증 (Implementation & Guardrails)

- `tasks.md`에 정의된 순서대로 Task를 하나씩 구현
- 각 Task 완료 시 반드시 5대 품질 가드레일 검증 실행:
  - Spotless
  - Error Prone
  - ArchUnit
  - JaCoCo
  - PMD & CPD
- 상세 검증 명령어 및 기준은 `rules/workflow/task-build-validation.md` 준수
- 코드 변경 후 `codegraph sync` 실행하여 지식 그래프 동기화

### 5. Memory Bank 갱신 (Compaction)

- `memory-bank/memory-bank.md`의 압축 원칙에 따라 완료된 과거 작업은 요약(Compacted)
- `memory-bank/activeContext.md`에 현재/다음 작업 맥락(진행 상태, 작업 트리, 확정 설계값, 다음 단계) 중심으로 슬림하게 갱신

---

## Spec 문서 저장 위치 및 표준 템플릿

모든 Spec 문서는 `.kiro/specs/` 디렉토리에 저장되며, 최상위의 `_` 접두사 템플릿을 사용하여 작성합니다.

```
.kiro/specs/
├── _requirements.md                   # [표준 템플릿] 요구사항 명세서
├── _design.md                         # [표준 템플릿] 상세 설계서
├── _tasks.md                          # [표준 템플릿] 점진적 구현 작업 명세서
└── {모듈명}/
    └── {순번}-{기능명}/
        ├── requirements.md            # _requirements.md 기반 작성
        ├── design.md                  # _design.md 기반 작성
        └── tasks.md                   # _tasks.md 기반 작성
```

- 폴더 구조 및 네이밍 규칙: `rules/project/spec-conventions.md` 참고
- Spec 문서는 범용 마크다운 형식

---

## 참조 규칙 및 템플릿 파일

SDD 실행 중 아래 파일들을 반드시 참조해야 합니다.

| 파일 경로 | 분류 | 참조 시점 / 목적 |
|---|---|---|
| `.kiro/specs/_requirements.md` | **템플릿** | `requirements.md` 작성 시 복사하여 사용 |
| `.kiro/specs/_design.md` | **템플릿** | `design.md` 작성 시 복사하여 사용 |
| `.kiro/specs/_tasks.md` | **템플릿** | `tasks.md` 작성 시 복사하여 사용 |
| `rules/project/spec-conventions.md` | 규칙 | Spec 폴더 네이밍 및 3자리 순번 결정 시 |
| `rules/workflow/task-build-validation.md` | 규칙 | 각 Task 완료 후 5대 가드레일 검증 시 |
| `rules/workflow/codegraph-first.md` | 규칙 | 코드 구조 분석 시 (CodeGraph MCP 1순위) |
| `rules/workflow/git-workflow.md` | 규칙 | 커밋 및 푸시 / PR 작성 시 |
| `rules/project/pom-conventions.md` | 규칙 | pom.xml 수정 시 |
| `rules/module/module-template.md` | 규칙 | 새 모듈 생성 시 |
| `rules/module/new-module-guard.md` | 규칙 | 새 모듈 생성 전 사전 점검 시 |
| `rules/coding/code-style.md` | 규칙 | Java 소스 코드 작성/수정 시 |
| `rules/project/tech-stack.md` | 규칙 | 기술 스택 및 라이브러리 버전 확인 시 |
| `rules/infra/deployment.md` | 규칙 | 원격 VM 배포 시 |
| `skills/myrpg-data-balance/SKILL.md` | 스킬 | MyRPG 게임 데이터 밸런스 조정 시 |