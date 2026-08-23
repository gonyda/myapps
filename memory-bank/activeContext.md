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

- **전투 공방 페이즈 스킬 선택 UI/UX & 지연 전송(Deferred Submission) 메카닉 완료 (5대 가드레일 통과)**:
  - **스킬 선택 시각화 (`.battle-skill-btn.selected`, `myrpg.css`)**:
    - 황금빛 테두리(`border: 1px solid #ffd700`) + 네온 글로우(`box-shadow: 0 0 10px rgba(255, 215, 0, 0.6)`) + 미세 확대(`scale(1.03)`)로 선택된 스킬 하이라이트.
  - **지연 전송 & 타임아웃 연계 (`myrpg.js`)**:
    - 스킬 클릭 즉시 서버로 넘어가지 않고 클라이언트 상태(`selectedSkillId`)만 저장.
    - 몬스터의 태세 준비 시간(타이머: 1.0s~1.5s)이 다 끝났을 때 최종 선택된 스킬로 `/battle/turn?skillId=...` 전송.
    - 시간 내 스킬 자유 교체 및 재클릭 시 토글 해제(A안) 지원.
  - **자원 부족 실시간 검증 & 피드백 (`myrpg.js`, `battle-view.html`)**:
    - 스킬의 `data-resource-kind`, `data-resource-cost`와 현재 자원(스태미나/마나) 비교.
    - 자원 부족 시 흔들림 애니메이션(`insufficient-shake`) + 하단 로그("스태미나/마나가 부족합니다.") 출력 후 선택 거절.
    - 자원 부족 등으로 아무것도 선택 안 된 채 시간 만료 시 `timeout` 무방비 피격 처리.
  - **선제공격 턴 방어 스킬(DEFENSE) 예외 처리 버그 픽스 (`BattleService`, `BattleLogFormatter`)**:
    - 선제공격 찬스에서 디펜스/카운터 사용 시 100% 공격 배율로 몬스터에게 데미지가 들어가던 결함을 수정하여 0 데미지 및 `"선제 공격 기회였으나 방어 태세를 취했다!"` 로그 출력 후 턴 소비 정상화.
  - **5대 가드레일 검증 100% 통과**: 1,092개 전체 단위/프로퍼티 테스트 통과 및 CodeGraph 동기화 완료.

---

## 3. 최근 변경사항 및 확정 설계값 (Recent Changes & Decisions)

- **[2026-08-23] 전투 공방 스킬 선택 UI/UX**:
  - 스킬 선택 토글: A안(선택된 스킬 재클릭 시 선택 해제) 채택.
  - 자원 부족 시: 스킬 미선택 상태 유지 + 흔들림 애니메이션 및 하단 로그 출력.
  - 타이머 만료 시점: 최종 선택 스킬 전송 (미선택 시 `timeout` 전송).
- **[2026-08-23] 디펜스 스킬 경감률 및 몬스터 기본 경감률**:
  - `defense` 스킬 `blockRateByRank`: F:70% ~ Master:95%
  - 모든 몬스터 기본 `defenseBlockRate`: 70% 고정 (`monster.json` 오버라이드 지원)
- **[2026-08-23] `docs/todo.md` 잔여 태스크**:
  - **1번 섹션**: 인게임 시간대별 웹 배경화면 색상 동적 전환
  - **2번 섹션**: 간이 로그인 기능 (로그인 화면 우선 구현)
  - **3번 섹션**: 게임 내 환경 & 시스템 확장 (기존 백로그)

---

## 4. 다음 단계 (Next Steps)

1. **`docs/todo.md` 1번 섹션**: 인게임 시간대별 웹 배경화면 색상 동적 전환 (`TimeOfDay` 기반 CSS 테마 전환)
2. **2번 섹션**: 간이 로그인 기능 (로그인 화면 우선 구현)