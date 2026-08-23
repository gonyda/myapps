# Active Context

> 최종 업데이트: 2026-08-24 00:18 (Asia/Seoul)

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
- **신규 장비 42종 카탈로그 & NPC 상점 판매 매핑 (`item.json`, `npc.json`)**: 총 57종 장비 확충, 밸런스 검증 0건 오차 완료.

---

## 2. 현재 작업 맥락 및 상태

- **스킬 시스템 확장 (29종 전체 전면 개편) 상세 기획 및 설계 확정 (`docs/todo.md` Section 4 & `docs/skill-system-dev-guide.md`)**:
  - **단일 전면 개편 아키텍처**: 29종 전체 스킬에 대해 `Skill` Sealed Interface 8종 Record + 10종 `SkillType` + `BattleState`/`CharacterSkill` 확장을 단일 SDD 아키텍처로 진행.
  - **전투 상성 & 특수 메커니즘 확정**:
    - `ULTIMATE`: 절대 우위(Super-Priority) - 적의 모든 행동(일반/강/방어)을 압도하여 100% 관통 및 적중(방어 무시), 해당 턴 몬스터 공격 차단. 쿨타임 F:30승 ~ MASTER:10승 (승리로만 차감).
    - `RECOVERY` (힐링) & `BUFF` (마나 실드): 시전 턴 몬스터 공격에 100% 무방비 피격되나 힐링 즉시 회복 및 마나실드는 해당 턴부터 MP 감쇄 흡수 적용.
    - `CC` (스파이더 샷): 시전 턴 피격 후 성공 시(20%~50%) **다음 턴 1턴간** 몬스터 행동 불능(턴 스킵).
    - `BUFF` (마나 실드): 5턴 지속, INT 비례 감쇄율 상승. MP 고갈 시 잔여 피해 HP 전가 및 버프 유지. 재시전 시 지속시간 갱신(Refresh).
    - `DOT` (미라지 미사일): 즉발 30% 피해 + 1~5턴 독 피해. 재시전 시 지속시간 및 수치 갱신(Refresh).
    - `DEBUFF` (레이지 임팩트): 60%~120% 피해 + 다음 공격 피해 +30% 증폭 (1회성).
    - `아이스 스피어`: 2타 적중 후 **다음 턴 빙결 CC 확률(F 20% ~ MASTER 50%) 발동**.
    - `라이트닝 로드`: **적 방어력(DEF)을 0으로 계산**하여 100% 방어 관통 피해.
  - **패시브 및 메디테이션 규칙**:
    - 패시브 6종은 **스킬 팝업 `공용(common)` 탭**에서 `디펜스`와 함께 관리 (전투 슬롯 미등록, F~MASTER 선형 스탯 분배).
    - `메디테이션`: 랭크업 시 `MP +30` 누적 + **전투 중 매 턴 종료 시(공방 해결 후 다음 턴 개시 전) `MP +1~+5` 자연 회복** (필드 이동 시 미회복, 전투 간 회복 상태 보존).
  - **스킬 습득 및 시드 정책**:
    - 신규 캐릭터 기본 4종(`slash`, `aimed_shot`, `mana_bolt`, `defense`) 시드 유지.
    - 나머지 25종 스킬은 추후 NPC 상점 스킬북 구매 및 학습 시스템으로 습득.

---

## 3. 다음 단계 (Next Steps)

1. **SDD 014 스킬 시스템 확장 Spec 3종 작성 (`.kiro/specs/myrpg/014-skill-system-expansion/`)**:
   - `requirements.md`: 29종 스킬, 10종 SkillType, 전투 상성(궁극기 절대우위, 힐/버프 피격, CC/빙결 다음턴 속박, 도트/버프 갱신, 메디테이션 턴종료 재생) 명세.
   - `design.md`: Sealed Interface 8종 Record, `SkillCatalogService`, `BattleService`, `BattleResolver`, `CharacterSkill`/`BattleState` 엔티티 변경, UI(`공용` 탭 및 힐링 필드사용/궁극기 쿨타임) 설계.
   - `tasks.md`: 순차 구현 및 5대 품질 가드레일 검증 Task 분할.