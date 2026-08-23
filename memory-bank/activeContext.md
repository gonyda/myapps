# Active Context

> 최종 업데이트: 2026-08-23 19:10 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` & `memory-bank/memory-bank.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(원칙·맥락) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(Compaction 갱신)**
> - **관리 원칙**: 슬라이딩 윈도우 & 이전 작업 압축(Compaction) 필수, 무한 누적 금지
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

---

## 1. 이전 완료 작업 요약 (Compacted)

- **AI Agent 아키텍처 & 5대 품질 가드레일**: `AGENTS.md`, `rules/`, `memory-bank/`, `skills/sdd/SKILL.md`, Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD 가드레일 구축.
- **011 알비 던전 시스템 (`.kiro/specs/myrpg/011-dungeon-system/`)**: 프로시저럴 던전 생성 엔진(`DungeonGenerator`), 안개 탐색, 연쇄 전투(10%), 턴제 방 이동 및 백트래킹 완료.
- **013 액티브 전조 반응 전투 시스템 (`.kiro/specs/myrpg/013-active-telegraph-combat/`)**: 2단계 턴 사이클, B안 직관형 전조 뱃지, 타임아웃 무방비 피격, 스킬 alert 제거, 전체 가드레일 검증 완료.
- **성장 & 보상 밸런스 및 로그 편의성 개선 완료**: 경험치 곡선 완화(`50L * L + 15L * L²`), 아이템 드랍 로그 수량 표기 통일.

---

## 2. 현재 작업 맥락 및 상태

- **디펜스 스킬 메카닉 재설계 & 확정 선제공격 시스템 완료 (5대 가드레일 검증 완료)**:
  - **DEF 비례 피해 경감 수식 반영 (`BattleResolver`, `skill.json`, `Monster`)**:
    - 기존 100% 완전 방어(0 피해)에서 플레이어 DEF 및 스킬 랭크별 경감률(70%~95%), 몬스터 DEF 및 기본 경감률(70% 고정) 수식으로 개편.
    - 동레벨 몬스터(Lv.1 너구리 vs DEF 5) 방어 성공 시 8~9의 미미한 피해, 보스 몬스터(Lv.7 거대거미 vs DEF 5) 방어 성공 시 20 이상의 "막아도 아픈" 체감 완성.
  - **확정 선제공격(Preemptive Strike) 시스템 도입 (`BattleState`, `BattleService`, `BattleLogFormatter`, `myrpg.css`)**:
    - 유저 디펜스 성공 (vs 적 일반공격) → 다음 턴 유저 확정 선제공격 찬스 (`⚡ 선제 공격 찬스!` 뱃지, 몬스터 경직, 100% 일방 공격).
    - 몬스터 디펜스 성공 (vs 유저 일반공격) → 다음 턴 몬스터 **확정 선제 일반공격** (`⚠️ 몬스터의 확정 선제 일반공격!` 뱃지, 몬스터 일반공격 100% 일방 적중).
    - `PreemptiveParty` 도메인 열거형 및 JPA 컬럼 매핑으로 턴 간 및 재접속 시에도 안전하게 영속화.
  - **가드레일 검증 100% 통과**: 1,090개 전체 단위/프로퍼티 테스트 통과 및 CodeGraph 동기화 완료.
  - `docs/todo.md` 1번 항목 완료 처리 및 잔여 목록 순번 재정렬 완료.

---

## 3. 최근 변경사항 및 확정 설계값 (Recent Changes & Decisions)

- **[2026-08-23] 디펜스 스킬 경감률 및 몬스터 기본 경감률**:
  - `defense` 스킬 `blockRateByRank`: F:70% ~ Master:95%
  - 모든 몬스터 기본 `defenseBlockRate`: 70% 고정 (`monster.json` 오버라이드 지원)
  - 고레벨 몬스터 난이도는 DEF 스탯과 HP로 밸런싱.
- **[2026-08-23] 선제공격 규칙**:
  - 유저 선제공격: 원하는 스킬(일반/강공격/마법 등) 사용 가능. 몬스터 0 피해.
  - 몬스터 선제공격: 몬스터는 무조건 일반공격(`SkillType.NORMAL`)으로 일방 공격.
- **[2026-08-23] `docs/todo.md` 잔여 태스크**:
  - **1번 섹션**: 인게임 시간대별 웹 배경화면 색상 동적 전환
  - **2번 섹션**: 간이 로그인 기능 (로그인 화면 우선 구현)
  - **3번 섹션**: 게임 내 환경 & 시스템 확장 (기존 백로그)

---

## 4. 다음 단계 (Next Steps)

1. **`docs/todo.md` 1번 섹션**: 인게임 시간대별 웹 배경화면 색상 동적 전환 (`TimeOfDay` 기반 CSS 테마 전환)
2. **2번 섹션**: 간이 로그인 기능 (로그인 화면 우선 구현)