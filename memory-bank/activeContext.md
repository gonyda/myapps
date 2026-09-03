# Active Context

> 최종 업데이트: 2026-09-03 21:25 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` & `memory-bank/memory-bank.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(원칙·맥락) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(Compaction 갱신)**
> - **관리 원칙**: 슬라이딩 윈도우 & 이전 작업 압축(Compaction) 필수, 무한 누적 금지
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

---

## 1. 이전 완료 작업 요약 (Compacted)

- **핵심 시스템 & UI/UX 완비**: 알비 던전/전투 턴/데이터 격리, 모바일(360~480px) 최적화 팝업군, 무기 세트 I/II 스왑, 스킬 슬롯 해제 방어, 상점 장비 착용 비교 팝업 구축 완료.
- **인게임 시간대별 앰비언트 스카이 & 천체 궤적 (2026-08-28)**: 6대 시간대(`TimeOfDay`) 딥 다크 스카이 그라디언트, 천체(해/달) 궤적 및 소프트 글로우 동적 연출 완비.
- **스킬 승급 조건 단순화 & 유형 뱃지 표기 (2026-08-29)**: `RankUpRequirement(requiredUsage)` 단일화, 10대 스킬 유형별 다크 판타지 컬러 뱃지 구축.
- **신규 아이템 '장작' 및 마을/필드 50% 나무 스폰 & 5초 채집 시스템 (2026-08-29, 017-firewood-gathering)**: `ItemType.MATERIAL`, `firewood` 아이템 등록, `GatheringService` (5 SP 소모, 50% 채집 성공/실패 롤), 5초 자동 완료형 채집 타이머 완비.
- **메시지 및 게임 프로퍼티 외부화 리팩토링 (2026-08-30, 018-message-and-properties-externalization)**: `messages.properties` 120여 개 키 외부화, `GameProperties` 바인딩, `GameMessageService` 구축.
- **화면 상단 상황 멘트 제거 및 잔여 코드 전수 정리 (2026-09-02, 019-remove-situation-ambience)**: `AmbienceService`/DTO/JSON/테스트 완전 제거, 인게임 스카이 그라디언트/천체 궤적 보존, 5대 가드레일 통과.
- **스킬 타입 다형화 및 전투 디스패치 Visitor 전환 (2026-09-03, 020-skill-type-polymorphism)**:
  - **도메인 다형 계약**: `Skill` 인터페이스에 `primaryStatLabel()`, `primaryValueAt(rank)`, `resourceCostAt(rank)`, `counterValueAt(rank)`, `critBonusValueAt(rank)` 및 `<R> R accept(SkillCombatVisitor<R>)` 확장. 8종 서브타입(`DamageSkill`, `DefenseSkill`, `RecoverySkill`, `UltimateSkill`, `PassiveSkill`, `BuffSkill`, `CcSkill`, `DotSkill`)이 자신의 계산 규칙을 소유하도록 위임.
  - **Visitor 패턴 도입**: `SkillCombatVisitor<R>` 도메인 인터페이스 선언 및 `BattleService` 내부 `CombatDispatchVisitor` 구현으로 전투 턴(`takeTurn`)의 궁극기/회복/버프/CC 디스패치를 `skill.accept(visitor)`로 전환.
  - **서비스 instanceof 체인 제거**: `SkillService` 및 `BattleService`에 산재했던 수십 회의 서브타입 판별 `instanceof` 체인을 다형 메서드 호출 및 sealed switch 매칭으로 완전 치환.
  - **회귀 및 PBT 검증**: `SkillPolymorphismTest`, `SkillCombatVisitorPropertyTest`(Property 1 동치성, Property 2 디스패치 정합성), `BattleCombatPreservationPropertyTest`(Property 3 동작 보존, Property 4 폴백 보존) 구축. 전체 1,264개 테스트 All-Green.
  - **5대 가드레일**: Spotless, Error Prone, ArchUnit, JaCoCo, PMD/CPD 전수 통과 및 `codegraph sync` 완료.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **5대 품질 가드레일 상태**: `myrpg` 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그)

### 3.1. 기술 부채 리팩토링 백로그 (`docs/myrpg-refactoring-candidates.md`)
1. **[🔴 2순위] `InventoryService` (1,708줄) God Class 분리**:
   - 의존성 10개, 책임 과다(인벤토리/장비/수리/판매 등) → SRP에 맞게 서비스 분리 및 결합도 완화.
2. **[🟡 3순위] `BattleService` (1,625줄) God Class 분리**:
   - 1순위 스킬 다형화 완료로 낮아진 결합도를 기반으로 전투 상태/보상/턴 서비스 분리.

### 3.2. 신규 게임 컨텐츠 기획 백로그
1. **[기획 1] 캠프파이어 & 야간 위험도 시스템 (표준형)**:
   - 획득한 장작을 소비하여 모닥불 야영(야간 20~05시 위험 완화, 아침 08:00 스킵, 바이탈 완충, 음식 굽기 버프).
2. **[기획 2] 마을 아르바이트 & 축복의 포션 시스템**:
   - 시간대별 NPC 일일 의뢰 및 축복의 포션(내구도 보호) 보상.
3. **[기획 3] 타이틀(칭호) & 업적 도감 시스템**:
   - 업적 기반 고유 칭호 장착 및 스탯 보너스, 타이틀 도감 팝업.
4. **[기획 4] 필드 보스 랜덤 스폰 (Field Boss Encounters)**:
   - 필드 랜덤 시간 + 랜덤 위치 보스 등장.
5. **[기획 5] 인챈트 & 세공 장비 커스터마이징 시스템**:
   - 접두/접미 인챈트 스크롤 및 마법 가루 성공률, 세공 옵션 부여.
6. **[기획 6] 도박 컨텐츠 (던바튼 한정)**:
   - 골드를 걸고 즐기는 도박 미니게임.
