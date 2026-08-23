# Requirements Document: 014-skill-system-expansion

> **폴더 위치 가이드**: `.kiro/specs/myrpg/014-skill-system-expansion/requirements.md`  
> **관련 규칙**: `rules/project/spec-conventions.md`, `rules/project/tech-stack.md`, `docs/skill-system-dev-guide.md`, `docs/todo.md` Section 4

---

## 1. Introduction (개요 및 배경)

### 1.1. 배경 및 목적
- **현재 상태 및 문제점**:
  - 현행 MyRPG 스킬 시스템은 `Skill` sealed interface가 `DamageSkill`과 `DefenseSkill` 2종의 record로만 제한되어 있고, `SkillType` 역시 `NORMAL`, `HEAVY`, `DEFENSE` 3종만 지원합니다.
  - 카탈로그 등록 스킬이 11종에 불과하며, 힐링, 패시브 마스터리, 버프(마나 실드), CC(스파이더 샷), 도트(미라지 미사일), 궁극기(메테오 스트라이크/파이널 히트/파이널 샷), 랜덤 타수(배쉬/썬더 등)를 표현할 도메인 모델과 전투 엔진 처리 로직이 부재합니다.
- **핵심 목표**:
  - `docs/todo.md` Section 4 및 `docs/skill-system-dev-guide.md`에 정의된 **29종 전체 스킬(기존 11종 + 신규 18종)**을 단일 SDD 아키텍처로 전면 구축합니다.
  - `Skill` sealed interface를 8종 Record로 확장하고, 10종 `SkillType` 표준 분류 체계 및 정밀한 1:1 턴제 전투 상성/특수 메커니즘을 완성합니다.
  - 스킬 수련 4대 체계(직접공격, 지원특수, 궁극기, 패시브) 및 UI(공용 탭 관리, 필드 힐링, 궁극기 쿨타임 표기)를 구현합니다.
- **선행 스펙과의 연계**:
  - `005-skill-system`: 스킬 습득 및 기본 랭크업 시스템 확장
  - `008-battle-system` & `009-skill-differentiation-and-battle-log`: 1:1 턴제 전투 엔진 및 다단히트/로그 시스템 확장
  - `012-defense-counter-skill-redesign`: 디펜스/카운터 어택 공방 상성 연동
  - `013-active-telegraph-combat`: 2단계 턴 사이클(전조 Stance → 공방 Clash) 연동

### 1.2. 이번 스펙의 범위 (In-Scope)
1. **29종 스킬 카탈로그 확장 (`data/skill.json`)**:
   - 근접 7종, 궁술 7종, 마법 9종, 공통/패시브 6종 총 29종 스킬 카탈로그 데이터 완비.
2. **Sealed Interface 다형성 도메인 모델 확장**:
   - `Skill` permits 8종: `DamageSkill`, `DefenseSkill`, `RecoverySkill`, `UltimateSkill`, `PassiveSkill`, `BuffSkill`, `CcSkill`, `DotSkill`.
   - `SkillType` enum 10종: `NORMAL`, `HEAVY`, `DEFENSE`, `RECOVERY`, `ULTIMATE`, `PASSIVE`, `BUFF`, `DEBUFF`, `CC`, `DOT`.
3. **전투 엔진 상성 및 특수 메커니즘 확장 (`BattleResolver`, `BattleService`)**:
   - 랜덤 타수(3~5타: 배쉬, 크래시 샷, 썬더) 및 기대 타수 산출.
   - 궁극기(`ULTIMATE`): 적 행동(일반/강/방어)을 압도하는 절대 우위(Super-Priority) 100% 관통 및 적 공격 차단.
   - 자가 시전(`RECOVERY`, `BUFF`): 시전 턴 몬스터 공격에 100% 무방비 피격을 받되 효과 즉시 발동.
   - 마나 실드(`BUFF`): 5턴 지속, INT 비례 감쇄율, MP 고갈 시 잔여 피해 HP 전가 및 버프 유지, 재시전 갱신(Refresh).
   - 제어(`CC`: 스파이더 샷, 빙결: 아이스 스피어): 시전 턴 피격 후 성공 시(20~50%) **다음 턴 1턴간 몬스터 행동 불능(턴 스킵)**.
   - 지속 피해(`DOT`: 미라지 미사일): 즉발 30% 피해 + 1~5턴 매 턴 독 피해, 재시전 갱신.
   - 방어 관통(`DamageSkill.defensePierce`): 라이트닝 로드 적 방어력(DEF)을 0으로 계산하여 100% 순수 피해.
   - 피해 증폭(`DEBUFF`: 레이지 임팩트): 60%~120% 타격 + 다음 공격 피해 +30% 증폭 (1회성).
4. **패시브 마스터리 6종 및 메디테이션 턴 종료 자연 재생**:
   - 패시브 6종 F→MASTER 선형 스탯/바이탈 영구 가산 (`SkillRankupBonus` 확장).
   - 메디테이션: **전투 중 매 턴 종료 시점(공방 해결 후 다음 턴 개시 전) `MP +1~+5` 자연 회복** (필드 이동 미회복, 전투 간 잔여 상태 보존).
5. **영속 계층 엔티티 확장 (`BattleState`, `CharacterSkill`)**:
   - `BattleState`: `next_attack_amp_percent`, `mana_shield_turns_left`, `mana_shield_absorb_rate`, `monster_stunned_turns`, `dot_damage_per_turn`, `dot_turns_left`.
   - `CharacterSkill`: `ultimate_cooldown` (전투 승리 횟수 기반 쿨타임).
6. **스킬 수련 4대 체계 및 AP 소모 (`SkillRankPolicy`, `SkillService`)**:
   - 전 스킬 F→MASTER 총 200 AP.
   - 4대 체계: 직접공격(사용+막타), 지원특수(막타면제+사용), 궁극기(막타면제+소량사용 1~20회), 패시브(수련면제 AP즉시).
7. **프론트엔드 UI/UX 연동**:
   - 스킬 팝업: `공용(common)` 탭에 디펜스 + 패시브 6종 배치.
   - 필드 힐링: `POST /skills/{id}/use` 엔드포인트 및 `[사용]` 버튼 연동, MP 부족/체력 최대 시 `alert()` 안내.
   - 전투 화면: 궁극기 쿨타임 중 `disabled` + `[🔒 (N승 남음)]`, 충전 완료 시 `[⚡ (READY!)]` 강조 펄스, 패시브 스킬 전투 액션 슬롯 자동 제외.

### 1.3. 제외 및 이연 범위 (Out-of-Scope / Deferred)
- **NPC 상점 스킬북 아이템 구매 시스템**: 스킬북 아이템 추가 및 상점 구매/학습 플로우는 본 스펙 완료 후 후속 스펙에서 개발 (본 스펙에서는 기본 4종 시드 + `SkillService.learnSkill` API 기반 동작).
- **시간대별 웹 배경색 동적 전환 (`todo.md` Section 1)**: 별도 프론트엔드 테마 작업으로 이연.
- **간이 로그인 기능 (`todo.md` Section 2)**: 별도 인증 스펙으로 이연.
- **인게임 시간/야영/필드 보스 (`todo.md` Section 3)**: 별도 환경 확장 스펙으로 이연.

---

## 2. Glossary (용어 사전)

### 2.1. 기존 재사용 용어
- **`Skill`**: 스킬 카탈로그의 불변 계약 인터페이스 (`domain.model.Skill`).
- **`DamageSkill`**: 배율, 타수, 크리티컬 가산을 보유한 공격 스킬 레코드 (`domain.model.DamageSkill`).
- **`DefenseSkill`**: 경감률 및 반격 배율을 보유한 방어 스킬 레코드 (`domain.model.DefenseSkill`).
- **`SkillRank`**: F(0)부터 MASTER(15)까지의 16단계 스킬 숙련도 열거형 (`domain.model.SkillRank`).
- **`SkillTalent`**: MELEE, ARCHERY, MAGIC, COMMON 재능 분류 열거형 (`domain.model.SkillTalent`).
- **`BattleState`**: 턴제 전투의 가변 상태를 저장하는 JPA 엔티티 (`domain.model.BattleState`).
- **`CharacterSkill`**: 캐릭터가 습득한 스킬과 랭크/수련치를 영속화하는 JPA 엔티티 (`domain.model.CharacterSkill`).

### 2.2. 본 스펙 신규 용어 (`Pascal_Snake_Case`)
- **`RecoverySkill`**: 자원(MP)을 소모하여 HP를 즉시 회복하는 스킬 모델 레코드 (`RECOVERY` 타입).
- **`UltimateSkill`**: 전투 승리 횟수를 쿨타임으로 소비하며 전투를 종결짓는 절대 우위 결전기 레코드 (`ULTIMATE` 타입).
- **`PassiveSkill`**: 전투 슬롯에 등록되지 않고 승급 시 캐릭터 기본 능력치를 영구 향상시키는 패시브 모델 레코드 (`PASSIVE` 타입).
- **`BuffSkill`**: 일정 턴 동안 피격 피해를 MP로 흡수 감쇄하는 버프 모델 레코드 (`BUFF` 타입).
- **`CcSkill`**: 데미지 없이 확률적으로 적을 행동 불능으로 만드는 제어 모델 레코드 (`CC` 타입).
- **`DotSkill`**: 즉발 데미지와 함께 매 턴 지속 독 피해를 주는 도트 모델 레코드 (`DOT` 타입).
- **`Super_Priority`**: 궁극기 시전 시 적의 일반/강공격/방어 행동을 모두 무력화하고 100% 확정 관통 타격을 입히는 절대 판정 규칙.
- **`Ultimate_Cooldown`**: 궁극기 재사용을 위해 요구되는 잔여 몬스터 처치(전투 승리) 횟수.
- **`Stunned_Turn`**: CC 또는 빙결 효과로 인해 몬스터가 행동하지 못하고 턴을 스킵당하는 상태.
- **`Meditation_Turn_End_Regen`**: 전투 공방 종료 후 다음 턴 개시 직전에 발동하는 메디테이션 전용 MP 자연 재생 메커니즘.
- **`Field_Skill_Use`**: 전투 밖 필드/마을에서 스킬 팝업을 통해 스킬을 시전하는 기능 (`POST /skills/{id}/use`).

---

## 3. Requirements (기능 요구사항)

### Requirement 1: 29종 스킬 카탈로그 및 Sealed Interface 8종 다형성 구축

**User Story:**  
플레이어 및 게임 엔진으로서, 근접/궁술/마법/공통 4개 계열 총 29종의 다양한 스킬 데이터를 타입 안전한 Sealed Interface 구조로 조회하고 활용하고 싶다.  
그래야 각 스킬 고유의 전투/육성 메커니즘이 컴파일 타임 무결성을 유지하며 동작할 수 있다.

#### Acceptance Criteria

1. **THE** `Skill` sealed interface **SHALL** `DamageSkill`, `DefenseSkill`, `RecoverySkill`, `UltimateSkill`, `PassiveSkill`, `BuffSkill`, `CcSkill`, `DotSkill` 8종 record만 구현체로 허용(`permits`)한다.
2. **THE** `SkillType` enum **SHALL** `NORMAL`, `HEAVY`, `DEFENSE`, `RECOVERY`, `ULTIMATE`, `PASSIVE`, `BUFF`, `DEBUFF`, `CC`, `DOT` 10종 상수를 제공한다.
3. **THE** `SkillCatalogService` **SHALL** `data/skill.json`으로부터 29종 전체 스킬을 로드하여 16키(F~MASTER) 랭크맵 및 메타데이터를 불변 컬렉션으로 초기화한다.
4. **WHEN** 카탈로그 조회가 요청되면, **THE** `SkillCatalogService` **SHALL** 스킬 ID 기반으로 적절한 `Skill` 하위 record 인스턴스를 반환한다.
5. **THE** 신규 생성 캐릭터는 기본 4종(`slash`, `aimed_shot`, `mana_bolt`, `defense`)만 F랭크로 보유하며, 나머지 25종은 카탈로그에 존재하되 습득(`learnSkill`) 전까지 `character_skill`에 등록되지 않는다.

---

### Requirement 2: 직접 공격 및 특수 딜링 메커니즘 (랜덤 타수, 방어 관통, 디버프 증폭)

**User Story:**  
플레이어로서, 배쉬/크래시샷/썬더의 랜덤 연타, 라이트닝 로드의 방어 관통, 레이지 임팩트의 피해 증폭 등 개성 있는 공격 스킬을 전략적으로 사용하고 싶다.  
그래야 전투의 박진감과 상성에 따른 콤보 재미를 극대화할 수 있다.

#### Acceptance Criteria

1. **WHEN** 플레이어가 랜덤 타수 스킬(`bash`, `crash_shot`, `thunder`)을 시전하면, **THE** `BattleService` **SHALL** `minHits`(3) ~ `maxHits`(5) 범위 내 균등 난수로 타수를 결정하여 각 타격별 독립 데미지 및 크리티컬을 산출한다.
2. **WHEN** 플레이어가 방어 관통 스킬(`lightning_rod`)을 시전하면, **THE** `BattleResolver` **SHALL** 대상의 방어력(DEF)을 0으로 계산하여 100% 온전한 피해를 입힌다.
3. **WHEN** 플레이어가 디버프 스킬(`rage_impact`)을 적중시키면, **THE** `BattleService` **SHALL** 기본 타격 피해를 입히고 `BattleState.nextAttackAmpPercent`를 30으로 설정한다.
4. **WHEN** `BattleState.nextAttackAmpPercent > 0`인 상태에서 다음 공격이 적중하면, **THE** `BattleService` **SHALL** 최종 피해에 1.3배(+30%)를 곱하여 적용하고 플래그를 0으로 초기화한다. 단, 궁극기(`ULTIMATE`) 시전 시에는 증폭이 적용되지 않는다.

---

### Requirement 3: 결전 궁극기 (ULTIMATE) 절대 우위 및 전투 승리 쿨타임 관리

**User Story:**  
플레이어로서, 많은 전투를 승리하여 충전한 궁극기(메테오 스트라이크, 파이널 히트, 파이널 샷)로 어떤 적이든 압도적인 일격을 가하고 싶다.  
그래야 긴 쿨타임에 걸맞은 확실한 결전 승리의 쾌감을 얻을 수 있다.

#### Acceptance Criteria

1. **THE** `CharacterSkill` 엔티티 **SHALL** 남은 전투 승리 쿨타임을 저장하는 `ultimateCooldown` 필드(기본값 0)를 보유한다.
2. **WHEN** 플레이어가 궁극기를 시전하면, **THE** `BattleService` **SHALL** 몬스터의 행동(NORMAL, HEAVY, DEFENSE)을 무시하고 **절대 우위(Super-Priority)**로 100% 적중 피해를 입히며 해당 턴 몬스터의 공격을 완전 차단(0 피격)한다.
3. **WHEN** 궁극기 시전이 완료되면, **THE** `BattleService` **SHALL** 해당 궁극기의 현재 랭크 기준 쿨타임(`coolWinsByRank`, F:30승 ~ MASTER:10승)을 `CharacterSkill.ultimateCooldown`에 저장한다.
4. **WHEN** 전투에서 몬스터를 처치하여 승리하면, **THE** `BattleService` **SHALL** 캐릭터가 보유한 모든 궁극기의 `ultimateCooldown`을 `Math.max(0, cooldown - 1)`로 1회씩 차감한다.
5. **IF** `ultimateCooldown > 0`인 궁극기가 존재하면, **THEN THE** 전투 화면은 해당 스킬 버튼을 `disabled` 처리하고 남은 승리 횟수(`[🔒 메테오 (N승 남음)]`)를 표시하며 시전을 차단한다.
6. **WHERE** 플레이어가 여관 휴식, 야영, 또는 포션을 사용하더라도, **THE** 궁극기 쿨타임은 초기화되지 않으며 오직 몬스터 처치 승리로만 차감된다.

---

### Requirement 4: 자가 시전 지원 스킬 (RECOVERY, BUFF) 전투 및 필드 사용

**User Story:**  
플레이어로서, 힐링으로 HP를 회복하고 마나 실드로 피해를 MP로 흡수 감쇄하며 위기를 극복하고 싶다.  
그래야 마법사 및 서포트 빌드의 생존력과 유지력을 확보할 수 있다.

#### Acceptance Criteria

1. **WHEN** 전투 중 플레이어가 `healing`(`RECOVERY`)을 시전하면, **THE** `BattleService` **SHALL** 즉시 해당 랭크의 `healAmount`만큼 HP를 회복하고 MP를 차감한다. 이때 몬스터가 공격(NORMAL/HEAVY) 시 플레이어는 100% 무방비 피격을 받는다 (몬스터 DEFENSE 시 무피해).
2. **WHEN** 필드(전투 밖)에서 `POST /skills/healing/use` 요청이 발생하면, **THE** `SkillService` **SHALL** MP를 차감하고 HP를 회복시키며 `usageCount`를 1 증가시킨다.
   - **IF** `mpCurrent < resourceCost`이면, **THEN** 상태 불변 및 `"마나가 부족합니다."` 오류 피드백을 반환한다.
   - **IF** `hpCurrent >= maxHp`이면, **THEN** 상태 불변 및 `"이미 최대 체력입니다."` 오류 피드백을 반환한다.
3. **WHEN** 플레이어가 `mana_shield`(`BUFF`)를 시전하면, **THE** `BattleState` **SHALL** `manaShieldTurnsLeft = 5` 및 `manaShieldAbsorbRate = absorbRate`를 설정하고, 시전 턴에 들어오는 몬스터 공격부터 즉시 MP 감쇄 흡수를 적용한다.
4. **WHEN** 마나 실드가 활성화된 상태에서 피격을 받으면, **THE** `BattleService` **SHALL** `피해량 × absorbRate%`만큼 MP를 차감하고 나머지 피해만 HP로 차감한다.
   - **IF** 보유 MP가 감쇄 흡수량보다 적으면, **THEN** 남은 MP 전액을 소모하고 흡수하지 못한 잔여 피해를 HP로 전가하며 마나 실드 버프는 유지된다.
5. **WHEN** 마나 실드 지속 중 다시 `mana_shield`를 시전하면, **THE** `BattleService` **SHALL** 지속 턴 수를 5턴으로 갱신(Refresh)하고 최신 랭크 감쇄율로 덮어쓴다.

---

### Requirement 5: 군중 제어 (CC, 빙결) 및 지속 피해 (DOT) 상태 관리

**User Story:**  
플레이어로서, 스파이더 샷의 속박, 아이스 스피어의 빙결, 미라지 미사일의 독 지속 피해로 적의 행동을 억제하고 누적 딜을 누적하고 싶다.  
그래야 전술적인 턴 제어와 안정적인 사냥이 가능하다.

#### Acceptance Criteria

1. **WHEN** 플레이어가 `spider_shot`(`CC`, 데미지 0)을 시전하면, **THE** 플레이어는 해당 턴 몬스터 공격을 그대로 피격받으며, 성공률(F:20% ~ MASTER:50%) 판정에 성공 시 `BattleState.monsterStunnedTurns = 1`을 설정한다.
2. **WHEN** 플레이어가 `ice_spear`(`HEAVY`, 2타)를 적중시키면, **THE** `BattleService` **SHALL** 2타 피해를 입히고 빙결 확률(F:20% ~ MASTER:50%) 판정에 성공 시 `BattleState.monsterStunnedTurns = 1`을 설정한다.
3. **WHEN** 턴 시작 시 `BattleState.monsterStunnedTurns > 0`이면, **THE** `BattleService` **SHALL** 몬스터의 행동을 스킵(플레이어 일방 공격 턴 보장)시키고 `monsterStunnedTurns`를 1 차감한다.
4. **WHEN** 플레이어가 `mirage_missile`(`DOT`)을 시전하면, **THE** `BattleService` **SHALL** 즉발 30% 피해를 가하고 `BattleState.dotDamagePerTurn = 28` 및 `BattleState.dotTurnsLeft = dotTurns`를 설정한다. 지속 중 재시전 시 지속시간과 수치는 최신으로 갱신(Refresh)된다.
5. **WHEN** 턴이 진행될 때 `BattleState.dotTurnsLeft > 0`이면, **THE** `BattleService` **SHALL** 몬스터에게 `dotDamagePerTurn` 피해를 입히고 `dotTurnsLeft`를 1 차감한다.

---

### Requirement 6: 패시브 마스터리 6종 및 메디테이션 전투 턴 종료 MP 자연 재생

**User Story:**  
플레이어로서, 컴뱃/레인지/매직/실드 마스터리, 크리티컬 히트, 메디테이션 등 패시브 스킬을 수련하여 캐릭터의 기초 체급과 턴당 마나 재생력을 끌어올리고 싶다.  
그래야 AP 투자가 누적 스탯과 전투 지속력으로 직결되는 성취감을 얻을 수 있다.

#### Acceptance Criteria

1. **THE** 패시브 스킬 6종(`combat_mastery`, `range_combat_mastery`, `magic_mastery`, `shield_mastery`, `meditation`, `critical_hit`)은 스킬 팝업의 **`공용(COMMON)` 탭에서 `defense`와 함께 관리**된다.
2. **THE** 패시브 스킬 6종은 전투 액션 선택 버튼 목록(`combatSkillList`)에서 항상 제외된다.
3. **WHEN** 패시브 스킬이 랭크업되면, **THE** `SkillRankupBonus` **SHALL** F(0)부터 MASTER(15)까지 랭크 단계(`rank.order()`)에 비례하여 영구 스탯/바이탈 보너스를 선형 누적 가산한다.
4. **WHEN** `meditation`을 습득한 캐릭터가 전투 턴 공방을 완료하면, **THE** `BattleService` **SHALL** 다음 턴 개시 전 시점에 현재 랭크 기준 `MP +1~+5`를 자연 회복시킨다.
5. **WHERE** 필드 이동 시에는 메디테이션에 의한 MP 회복이 발생하지 않으며, 전투 중 회복된 최종 MP는 캐릭터의 현재 MP(`mpCurrent`)에 그대로 유지되어 다음 전투 및 필드로 이어집니다.

---

### Requirement 7: 스킬 수련 4대 체계 및 승급 판정

**User Story:**  
플레이어로서, 스킬 타입별(직접공격, 지원특수, 궁극기, 패시브)로 합리적이고 직관적인 수련 조건을 달성하여 승급하고 싶다.  
그래야 공격 스킬은 처치를 통해, 보조/궁극기는 사용을 통해, 패시브는 AP만으로 막힘없이 성장할 수 있다.

#### Acceptance Criteria

1. **THE** 모든 29종 스킬은 F랭크에서 MASTER까지 승급하는 데 **총 200 AP**가 소모된다 (`SkillRankPolicy` 표준).
2. **WHEN** 직접 공격형(`NORMAL`, `HEAVY`) 승급 요청 시, **THE** `SkillService` **SHALL** 사용 횟수와 막타 처치 수 요구치를 모두 검증한다.
3. **WHEN** 지원/특수형(`DEFENSE`, `RECOVERY`, `BUFF`, `CC`, `DOT`, `DEBUFF`) 승급 요청 시, **THE** `SkillService` **SHALL** 막타 처치 조건을 면제(`killExempt = true`)하고 사용 횟수만 검증한다.
4. **WHEN** 궁극기형(`ULTIMATE`) 승급 요청 시, **THE** `SkillService` **SHALL** 막타 처치를 면제하고 궁극기 전용 소량 사용 수련치(1~20회)를 검증한다.
5. **WHEN** 패시브형(`PASSIVE`) 승급 요청 시, **THE** `SkillService` **SHALL** 사용 횟수 및 막타 처치를 모두 면제하고 보유 AP만 검증하여 즉시 승급한다.

---

## 4. Non-Functional & Quality Requirements (비기능 및 품질 요구사항)

1. **5대 품질 가드레일 (Task 완료 필수 기준)**:
   - **Spotless**: Java 포맷팅 자동 교정 (`mvn spotless:apply`).
   - **Error Prone**: 정적 결함 컴파일 타임 차단 (컴파일 경고 0건).
   - **ArchUnit**: DDD 4계층(`interfaces` → `application` → `domain`) 아키텍처 규칙 준수.
   - **JaCoCo**: 신규 및 변경 코드 대상 테스트 라인 커버리지 80% 이상 달성.
   - **PMD & CPD**: 복잡도(`CognitiveComplexity`), 안티패턴 및 중복 코드 0건.
2. **데이터 무결성 및 밸런스 정합성 검증**:
   - `tools/balance/verify_all_skills.py` 파이썬 검증 스크립트를 통해 29종 전 스킬 16키 랭크맵 완비, 단조 비감소, SP(기대딜지수) 밴드 적합성, critBonus 상한(+100) 0건 오류 일치.
3. **CodeGraph 동기화**:
   - 코드베이스 변경 후 `codegraph sync` 필수 수행.
