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

#### 2.1. `requirements.md`
- **목적**: 해결하려는 문제와 요구사항을 명확히 정의
- **작성 항목**: 배경/목적, 기능 요구사항, 비기능 요구사항, 제약 조건, 유스케이스

#### 2.2. `design.md`
- **목적**: 요구사항을 만족하는 구체적인 설계 정의
- **작성 항목**: 시스템 아키텍처/컴포넌트 구성, 데이터 모델(엔티티/테이블), API/화면 설계, 비즈니스 로직 규칙, 협의 확정값

#### 2.3. `tasks.md`
- **목적**: 구현 단위 작업을 순차적 체크포인트로 분할
- **작성 항목**: Task 목록(체크박스), 파일별 구현 범위, 테스트 코드 작성 계획 (모든 Task 필수, 5대 가드레일 검증 포함)

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

## Spec 문서 저장 위치

모든 Spec 문서는 `.kiro/specs/` 디렉토리에 저장됩니다.

```
.kiro/specs/
└── {모듈명}/
    └── {순번}-{기능명}/
        ├── requirements.md
        ├── design.md
        └── tasks.md
```

- 폴더 구조 및 네이밍 규칙: `rules/project/spec-conventions.md` 참고
- Spec 문서는 범용 마크다운 형식

---

## 참조 규칙 파일

SDD 실행 중 아래 규칙 파일을 함께 참조해야 합니다.

| 규칙 파일 | 참조 시점 |
|---|---|
| `rules/project/spec-conventions.md` | Spec 문서 작성/수정 시 |
| `rules/workflow/task-build-validation.md` | 각 Task 완료 후 검증 시 |
| `rules/workflow/codegraph-first.md` | 코드 구조 분석 시 (MCP 우선) |
| `rules/workflow/git-workflow.md` | 커밋/PR 시 |
| `rules/project/pom-conventions.md` | pom.xml 수정 시 |
| `rules/module/module-template.md` | 새 모듈 생성 시 |
| `rules/module/new-module-guard.md` | 새 모듈 생성 전 |
| `rules/coding/code-style.md` | Java 소스 코드 작성/수정 시 |
| `rules/project/tech-stack.md` | 기술 스택 확인 시 |
| `rules/infra/deployment.md` | 배포 시 |
| `skills/myrpg-data-balance/SKILL.md` | myrpg 데이터 밸런스 조정 시 |