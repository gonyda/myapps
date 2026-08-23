# Active Context

> 최종 업데이트: 2026-08-23 12:40 (Asia/Seoul)

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
- **012 디펜스 및 카운터 어택 스킬 재설계 (`.kiro/specs/myrpg/012-defense-counter-skill-redesign/`)**: 디펜스 100% 완전 방어(0 피격), 랭크별 스태미나 감소, 랭크업 영구 스탯 보너스 완료.
- **카운터 어택 밸런스 정상화 (`skills/myrpg-data-balance/`)**: 내 공격력 기준 전환, F 90% ~ MASTER 160%, 크리티컬 보너스 +10%p(+100) 반영 완료.
- **013 액티브 전조 반응 전투 시스템 (`.kiro/specs/myrpg/013-active-telegraph-combat/`)**:
  - 2단계 턴 사이클 (대치 ⏸️ ↔ 공방 ⚡) 및 B안 직관형 전조 뱃지(일반공격 1.0s, 강공격 1.5s, 방어태세 1.5s) 시스템 완성.
  - 도메인/DTO 확장(`BattleState`, `BattleView`), 서비스 계층(`startClash`, `takeTurn("timeout")` 무방비 피격), 컨트롤러(`POST /battle/clash`), 프론트엔드 UI(`battle-view.html`, `myrpg.css`, `myrpg.js` 실시간 타이머 및 alert 제거) 구현 및 검증 완료.

---

## 2. 현재 작업 맥락 및 상태

- **013 액티브 전조 반응 전투 시스템 전체 마일스톤(A~E) 완료**:
  - 도메인/영속성 계층: `standby`, `currentMonsterIntent` 필드 및 기본값 영속성 처리 완료.
  - 비즈니스 로직 계층: 공방 개시, 타임아웃 무방비 피격, 상성 해결 후 대치 복귀, 자원 부족 비차감 유지, 활 1턴 선제 사격 보장 완료.
  - 웹 컨트롤러 계층: `POST /battle/clash` 엔드포인트 및 모델 속성 동기화 완료.
  - 프론트엔드 계층: `.battle-stance-area` 전조 뱃지 및 카운트다운 타이머 바, 대치 중 스킬 비활성화, alert 팝업 제거 및 `VisualJsPreservation` 회귀 테스트 전수 통과.
  - 5대 품질 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo, PMD/CPD) 및 전체 1,081개 테스트 전수 통과 완료.

---

## 3. 최근 변경사항 및 확정 설계값 (Recent Changes & Decisions)

- **[2026-08-23] 013 프론트엔드 UI/UX 연동 (마일스톤 D: Task 15~19)**
  - `battle-view.html`: `.battle-stance-area`(대치 뱃지 + `[⚔️ 공방 개시]` 버튼, 공방 뱃지 + 실시간 게이지 바), `#battleSkills` 대치 중 비활성화 및 null-safe 반복문, `[도망]` 대치 조건부 노출 반영.
  - `myrpg.css`: B안 전조 뱃지(`.stance-badge`, `.badge-standby`, `.badge-stance-normal`, `.badge-stance-heavy`, `.badge-stance-defense`), 실시간 타이머 바(`.clash-timer-wrap`, `.clash-timer-bar`), 공방 개시 버튼(`.btn-clash-start`), 비활성화 스킬 버튼(`.disabled-skills .battle-skill-btn`) 스타일 추가.
  - `myrpg.js`: `swapBattleResponse(html)` 공통화, `startClash()`, `initClashTimer()`(CSS width 트랜지션 애니메이션 & duration 경과 시 timeout 전송), `battleTurn()` 및 `handleTurnResultSignal()`에서 alert 제거.
  - `VisualJsPreservationAndJsonLoadingIntegrationTest.java`: 신규 전투 함수(`startClash`, `initClashTimer`, `swapBattleResponse`) 및 템플릿 마커 검증 확장 통과.
- **[2026-08-23] 013 전체 통합 검증 및 품질 가드레일 (마일스톤 E: Task 20~22)**
  - `mvn -B -q spotless:apply -pl myrpg && mvn -B clean install -pl myrpg -am` 성공.
  - `codegraph sync` 인덱스 최신화 완료.

---

## 4. 다음 단계 (Next Steps)

1. **사용자 확인 및 실전 플레이 테스트**:
   - 브라우저에서 티르코네일 여우/거미 전투 진입 후 대치 페이즈(`[⚔️ 공방 개시]`) ➡️ 공방 페이즈(1.0~1.5초 타이머 및 전조 뱃지) ➡️ 스킬 상성 반응 및 타임아웃 피격 동작 확인.
2. **신규 요구사항 또는 다음 스펙 작업 준비**:
   - `docs/todo.md` 또는 차기 스펙(014) 기획 및 개발 진행.