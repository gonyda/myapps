# Active Context

> 최종 업데이트: 2026-08-22 01:37 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(읽기) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(갱신)**
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

## 1. 최근 작업 내역

### 1.1. AI Agent 교체 가능 아키텍처 구축 (2026-08-22 00:53)
- **목적**: Kiro → Cline → Antigravity로 이동해도 실행 Agent만 교체되고 프로젝트 규칙/지식/스킬은 유지되는 구조
- **변경 사항**:
  - `AGENTS.md` (신규): 모든 AI Agent 공통 진입점
  - `rules/` (신규): `.kiro/steering/` 11개 문서 이관 (SSOT)
  - `memory-bank/` (신규): `activeContext.md` + `memory-bank.md`
  - `skills/sdd/SKILL.md` (신규): SDD 프로세스 SSOT 원본
  - `.cline/skills/sdd/SKILL.md` (신규): Cline 참조 포인터
  - `.agents/skills/sdd/SKILL.md` (신규): Antigravity 참조 포인터
  - `.kiro/steering/` 삭제: `rules/`로 통합 완료
  - `.clinerules/` 삭제: `.cline/` 및 루트 SSOT로 통합 완료

### 1.2. PMD & CPD 정적 분석 가드레일 도입 (2026-08-22 00:25)
- `pmd-ruleset.xml` + `maven-pmd-plugin:3.26.0` 도입
- 5대 가드레일 통합 빌드 `BUILD SUCCESS` 및 CodeGraph 동기화 완료

### 1.3. `.clinerules/cline-global-rules.md` → `AGENTS.md` 통합
- 기존 `cline-global-rules.md`의 SSOT 역할을 `rules/` + `AGENTS.md`로 이관
- `.clinerules/` 디렉토리 완전 삭제

## 현재 프로젝트 활성 모듈 및 포트 매핑

| 모듈 | 포트 | 설명 |
|---|---|---|
| `mystudy` | 8080 | 영어 학습 웹 애플리케이션 |
| `mycalendar` | 8082 | 캘린더/일정 관리 웹 애플리케이션 |
| `myrpg` | 8083 | 텍스트/웹 기반 RPG 게임 애플리케이션 |

## 작업 트리 (수정/신규 파일 목록)

| 파일 | 변경 구분 | 내용 |
|---|---|---|
| `AGENTS.md` | 신규 | AI Agent 공통 진입점 및 전체 개발 규칙 |
| `rules/` | 신규 | `rules/coding/`, `rules/infra/`, `rules/module/`, `rules/myrpg/`, `rules/project/`, `rules/workflow/` — steering 문서 11개 이관 |
| `memory-bank/memory-bank.md` | 신규 | 메모리뱅크 개념 및 갱신 규칙 |
| `memory-bank/activeContext.md` | 신규 | 현재 작업 상태 및 다음 단계 |
| `skills/sdd/SKILL.md` | 신규 | SDD 프로세스 SSOT 원본 |
| `.cline/skills/sdd/SKILL.md` | 신규 | Cline 참조 포인터 |
| `.agents/skills/sdd/SKILL.md` | 신규 | Antigravity 참조 포인터 |
| `.kiro/steering/` | 삭제 | `rules/`로 통합 완료 |
| `.clinerules/` | 삭제 | `.cline/` 및 루트 SSOT로 통합 완료 |

## 다음 단계

- [x] 구조 변경에 따른 기존 문서(`.clinerules/` 등) 정리 및 이관 확인 완료
- [x] `AGENTS.md` 내 `rules/`, `memory-bank/`, `skills/` 링크 및 참조 정합성 검증 완료
- [x] Antigravity / Cline 스킬 참조 포인터 및 YAML frontmatter 표준화 완료
- [x] CodeGraph 재동기화 (`codegraph sync`) 완료