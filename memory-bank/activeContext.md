# Active Context

> 최종 업데이트: 2026-09-03 23:05 (Asia/Seoul)

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
  - 도메인 다형 계약 확장(8종 서브타입에 계산 규칙 위임) 및 `SkillCombatVisitor<R>` 방문자 패턴 도입.
  - `SkillService`/`BattleService` 수십 회의 `instanceof` 체인을 다형 메서드 호출 및 sealed switch 매칭으로 완전 치환.
  - 1,264개 테스트 All-Green, 5대 가드레일 통과 및 CodeGraph 동기화 완료.
- **`InventoryService` God Class 협력 객체 분해 및 파사드화 (2026-09-03, 021-inventory-service-decomposition)**:
  - **파사드 + 위임 아키텍처**: 1,708줄의 거대 클래스를 순수 위임 파사드(483줄, 72% 축소)로 개편. 27개 public API 및 2개 생성자(9-arg/7-arg) 시그니처 100% 보존.
  - **관심사별 협력 객체 8종 분리**: `InventoryQuerySupport`(공유 조회/용량/무기재능), `EquipmentService`(착용/해제/스마트/스왑/후보), `ItemStorageService`(은행↔인벤 이동/용량), `ItemAcquisitionService`(드랍/구매/시딩), `EquipmentBonusService`(보너스합산/내구도감소), `PotionConsumptionService`(포션 소비 회복), `InventoryViewAssembler`(인벤/은행/장비 팝업 뷰 조립 및 describe), `CombatSkillLoadoutService`(전투 10슬롯 버튼 조립).
  - **중복 통합 및 불변 검증**: 무기 세트 1/2 슬롯 분기 단일화, 카탈로그/캐릭터 폴백 중복 쿼리 제거, 클래스 레벨 복잡도 억제 어노테이션 제거. 1,270개 전체 테스트 All-Green, 5대 가드레일 통과 및 CodeGraph 동기화 완료.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **5대 품질 가드레일 상태**: `myrpg` 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).
- **Spec 상태**:
  - `021-inventory-service-decomposition` 전 태스크(A~E) 구현 및 검증 완료. 사용자 승인·요청 시 Git 커밋 착수 준비 완료.
  - `022-battle-service-decomposition` SDD 3종 Spec 작성 완료 (사용자 검토 대기, 구현 미착수).

### 2.1. 진행 중 Spec — 022 BattleService God Class 분해 (SDD Step 2 완료, 검토 대기)
- **위치**: `.kiro/specs/myrpg/022-battle-service-decomposition/` (requirements/design/tasks 3종)
- **대상**: `BattleService`(약 1,752줄, 의존성 19개) — 리팩토링 3순위.
- **최우선 제약(사용자 확정)**: 기존 로직 **1%도 동작 안 바뀜**(전투 결과·로그 라우팅·예외·**RNG 소비 순서**·
  트랜잭션 경계 보존) + 중복코드 제거 + 확장성 개편.
- **확정 설계값**:
  - **파사드 + 오케스트레이션/위임**: public 7메서드(`start`/`startClash`/`takeTurn`/`flee`/`resumeIfActive`/
    `combatSkills`/`swapWeapon`)와 두 생성자(18-arg/16-arg) 100% 보존. 2컨트롤러·20여 테스트(16-arg 직접 생성) 무변경.
  - **협력 객체 6종(빈 아님, 파사드가 두 생성자에서 raw 의존성으로 조립하는 plain 객체)**:
    `CombatStatResolver`(스탯/자원 해석), `SkillCombatResolver`(스킬 전투 처리·디스패치, 020의
    `CombatDispatchVisitor` 내부화), `TurnEffectApplier`(마나실드/DoT/명상), `BattleRewardService`(처치/던전/
    사망 결산), `BattleViewAssembler`(대치 뷰/뱃지), `BattleTurnResultFactory`(결과 빌드).
  - **내부 전달 타입**: `SkillCombatContext`/`SkillCombatOutcome`(신규), `TurnCombatResult`/`KillResolution`(이동).
  - **배치**: `application/service/battle` 패키지. 파사드→협력객체→`CombatStatResolver`/`BattleResolver` 단방향(순환 방지).
  - **RNG 결정성**: 파사드의 동일 `Random` 인스턴스를 협력 객체에 공유 주입해 소비 순서 보존.
  - **중복 통합 5건**: 스킬 조회(5→1), 스탯 합산(3→1), 몬스터 배율(2→1), `BattleTurnResult` 빌드(6→팩토리),
    턴 종료 확정 블록(3→`finalizeTurn`). `takeTurn` 오케스트레이션은 파사드 유지(단계 순서 보존).
  - **검증**: 기존 전투 회귀/jqwik(시드 고정) 무수정 그린(Property 1~4) + 미커버 지점 characterization 선행.
- **다음 액션**: 사용자 Spec 검토·승인 후 tasks.md 순차 구현 착수.

---

## 3. 다음 단계 (Next Steps / 백로그)

### 3.1. 기술 부채 리팩토링 백로그 (`docs/myrpg-refactoring-candidates.md`)
1. **[🔴 3순위] `BattleService` (1,752줄) God Class 분리** — *Spec 022 작성 완료, 검토·구현 대기*:
   - 파사드 + 협력 객체 6종으로 분해(§2.1 참조). SRP 회복 + 중복 통합(5건) + 동작·RNG 100% 보존.
2. **신규 게임 컨텐츠 기획 백로그 연계**:
   - 캠프파이어/야간 위험도, 마을 아르바이트 & 축복의 포션, 타이틀/업적 도감 등 기획 연계.
