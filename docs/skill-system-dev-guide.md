# MyRPG 스킬 시스템 확장 개발 가이드

> **작성일**: 2026-08-23 (최종 업데이트: 2026-08-24)  
> **목적**: `docs/todo.md` Section 4에 정의된 29종 스킬(기존 11종 + 신규 18종)을 구현하기 위한 **정확한 소스 기반 상세 설계 가이드**.  
> **대상 독자**: 이 문서를 바탕으로 SDD Spec 문서(`.kiro/specs/`)를 작성하는 AI Agent 또는 개발자.

---

## 1. 현행 아키텍처 분석 (As-Is)

### 1.1. 도메인 모델 계층 구조

```
Skill (sealed interface)
├── DamageSkill (record)    ← NORMAL, HEAVY, DEBUFF 타입
└── DefenseSkill (record)   ← DEFENSE 타입
```

#### `Skill` 인터페이스 — [`Skill.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/domain/model/Skill.java)

```java
public sealed interface Skill permits DamageSkill, DefenseSkill {
    String id();
    String label();
    SkillType type();
    SkillTalent talent();
    int resourceCost();
    String description();
}
```

- **`sealed interface`**: 구현체가 `DamageSkill`과 `DefenseSkill`로 **컴파일 타임에 고정**.
- 현재 `permits` 절에 **2종만 허용**되어 있으므로, 신규 스킬 타입(RECOVERY, BUFF, CC, DOT, ULTIMATE, PASSIVE)을 완비하려면 **`permits` 절을 확장**해야 함.

#### `DamageSkill` record — [`DamageSkill.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/domain/model/DamageSkill.java)

```java
public record DamageSkill(
    String id, String label, SkillType type, SkillTalent talent,
    int resourceCost,
    Map<SkillRank, Integer> multiplierByRank,    // 16키 단조 비감소
    String description,
    int hitCount,       // 타격 횟수 (1~8, 기본 1)
    int critBonus       // 크리티컬 가산 (0~100, 기본 0)
) implements Skill { ... }
```

**현행 제약사항**:
- `hitCount`: 고정 정수 필드 (현재 랜덤 타수 미지원)
- `critBonus`: 고정 정수 필드 (랭크별 변동 미지원, 상한 100)
- `multiplierByRank`: 16키 랭크맵 필수, `Map<SkillRank, Integer>`

#### `DefenseSkill` record — [`DefenseSkill.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/domain/model/DefenseSkill.java)

```java
public record DefenseSkill(
    String id, String label, SkillType type, SkillTalent talent,
    int resourceCost,
    Map<SkillRank, Integer> blockRateByRank,           // 경감률
    Map<SkillRank, Integer> counterMultiplierByRank,    // 반격 배율
    String description,
    Map<SkillRank, Integer> resourceCostByRank,         // 선택적 랭크별 소모
    Map<SkillRank, Integer> critBonusByRank             // 선택적 랭크별 크리보너스
) implements Skill { ... }
```

---

### 1.2. 열거형 (Enum) 현황

#### `SkillType` — [`SkillType.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/domain/model/SkillType.java)

```java
public enum SkillType {
    NORMAL("일반"),
    HEAVY("강"),
    DEFENSE("방어");
}
```

> ⚠️ 현재 3종만 정의되어 있으며, 10종 표준 타입 체계(`NORMAL`, `HEAVY`, `DEFENSE`, `RECOVERY`, `ULTIMATE`, `PASSIVE`, `BUFF`, `DEBUFF`, `CC`, `DOT`)로 확장이 필요함.

#### `SkillTalent` — [`SkillTalent.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/domain/model/SkillTalent.java)

```java
public enum SkillTalent {
    MELEE(TalentType.MELEE, ResourceKind.STAMINA, BonusTarget.STR),
    ARCHERY(TalentType.ARCHERY, ResourceKind.STAMINA, BonusTarget.DEX),
    MAGIC(TalentType.MAGIC, ResourceKind.MP, BonusTarget.INT),
    COMMON(null, ResourceKind.STAMINA, BonusTarget.DEF);
}
```

- 각 재능 메타데이터: `matchingTalent()`, `resourceKind()`, `rankupStatTarget()`
- **UI 탭 정책**: `COMMON` 탭에는 `디펜스` 및 `패시브 스킬 6종`이 매핑되어 관리됨.

---

## 2. 확장 아키텍처 및 29종 스킬 시스템 설계 (To-Be)

29종 전체 스킬에 대해 **Sealed Interface 8종 Record + 10종 `SkillType` + 전투 상성 메커니즘**을 일괄 전면 개편합니다.

```
Skill (sealed interface)
├── DamageSkill (record)    ← NORMAL, HEAVY, DEBUFF 타입 (기존 확장: minHits/maxHits, defensePierce, freezeRate)
├── DefenseSkill (record)   ← DEFENSE 타입
├── RecoverySkill (record)  ← RECOVERY 타입 (HP 즉시 회복, 전투/필드 공용)
├── UltimateSkill (record)  ← ULTIMATE 타입 (절대 우위 일격필살기, 30~10승 쿨타임)
├── PassiveSkill (record)   ← PASSIVE 타입 (전투 슬롯 제외, 공용 탭, 영구 스탯/재생 누적)
├── BuffSkill (record)      ← BUFF 타입 (마나 실드: 5턴 고정, INT 비례 감쇄율, MP 전가)
├── CcSkill (record)        ← CC 타입 (스파이더 샷: 데미지 0, 확률 속박, 다음 턴 몬스터 스킵)
└── DotSkill (record)       ← DOT 타입 (미라지 미사일: 즉발 30% + 1~5턴 독 지속 데미지)
```

---

## 3. 상세 구현 가이드 (스킬 타입별)

### 3.1. 랜덤 타수 (배쉬, 크래시 샷, 썬더)

#### `DamageSkill` record 확장

```java
public record DamageSkill(
    String id, String label, SkillType type, SkillTalent talent,
    int resourceCost,
    Map<SkillRank, Integer> multiplierByRank,
    String description,
    int hitCount,       // 기대 타수 (표시용, 기존 호환)
    int minHits,        // 최소 타수 (기본값 = hitCount)
    int maxHits,        // 최대 타수 (기본값 = hitCount)
    int critBonus,
    boolean defensePierce,                          // 방어력 0 계산 관통 여부 (라이트닝 로드)
    Map<SkillRank, Integer> freezeRateByRank        // 적중 후 다음 턴 빙결 확률 (아이스 스피어)
) implements Skill { ... }
```

#### `BattleService` 랜덤 타수 산출 로직

```java
private int resolvePlayerHitCount(final Skill skill) {
    if (skill instanceof DamageSkill ds) {
        if (ds.minHits() != ds.maxHits()) {
            return ds.minHits() + random.nextInt(ds.maxHits() - ds.minHits() + 1);
        }
        return ds.hitCount();
    }
    return 1;
}
```

---

### 3.2. RECOVERY (힐링)

#### 신규 record: `RecoverySkill`

```java
public record RecoverySkill(
    String id,
    String label,
    SkillType type,              // RECOVERY
    SkillTalent talent,          // MAGIC
    int resourceCost,            // 기본 마나 소모 (10)
    Map<SkillRank, Integer> healAmountByRank,       // 랭크별 회복량 (30~240)
    Map<SkillRank, Integer> resourceCostByRank,     // 랭크별 마나 소모 (12~6)
    String description
) implements Skill { ... }
```

#### 전투 및 필드 사용 규칙

1. **전투 내 사용**:
   - 턴 소모 + MP 차감 + HP 회복 즉시 적용.
   - **피격 규칙**: 몬스터가 공격(NORMAL/HEAVY) 시 플레이어는 **100% 무방비 피격**을 받음 (HP 순변화 = +회복량 - 피격피해).
   - 몬스터가 DEFENSE 선택 시 피격 없이 안전하게 회복.
2. **필드 사용 (스킬 팝업 UI/UX)**:
   - **엔드포인트**: `POST /skills/{id}/use` (전투 밖 필드/마을에서 사용).
   - **UI 위치**: 스킬 팝업 목록(`skill-popup.html`)에서 `fieldUsable` 스킬에 한해 **승급 버튼 왼쪽에 `[사용]` 버튼 배치**.
   - **유효성 검증 & Alert 피드백**:
     - MP 부족 시: 상태 불변, `alert("마나가 부족합니다.")`
     - 이미 HP 최대치(`hp >= maxHp`): 상태 불변, `alert("이미 최대 체력입니다.")`
     - 정상 사용 시: MP 차감 + HP 회복 + `usageCount + 1` 증가 → 상단 바 게이지 및 스킬 팝업 실시간 갱신.

---

### 3.3. ULTIMATE (메테오 스트라이크, 파이널 히트, 파이널 샷)

#### 신규 record: `UltimateSkill`

```java
public record UltimateSkill(
    String id,
    String label,
    SkillType type,              // ULTIMATE
    SkillTalent talent,
    int resourceCost,            // 스태미나/마나 소모
    Map<SkillRank, Integer> multiplierByRank,       // 히트당 배율
    Map<SkillRank, Integer> hitCountByRank,          // 랭크별 타수 (F~9:5타, 8~5:6타, 4~1:7타, M:8타)
    int critBonus,                                   // 크리티컬 가산 (100)
    Map<SkillRank, Integer> coolWinsByRank,           // 랭크별 쿨타임(전투 승리 횟수: F 30승 ~ M 10승)
    String description
) implements Skill { ... }
```

#### 전투 상성 및 쿨타임 규칙

1. **전투 상성: 절대 우위 (Super-Priority)**:
   - 몬스터의 행동(일반/강/방어)을 완전히 압도하여 **100% 관통 및 적중(방어 무시)**.
   - 해당 턴 **몬스터의 공격/행동을 완전 차단(0 피격)**.
2. **쿨타임 관리 (`CharacterSkill.ultimateCooldown`)**:
   - 궁극기 시전 시: `ultimateCooldown = coolWinsByRank.get(rank)` 설정.
   - 몬스터 처치 승리 시: `ultimateCooldown = Math.max(0, ultimateCooldown - 1)` 차감.
   - `ultimateCooldown == 0`일 때만 사용 가능 (휴식/야영으로 초기화 불가).
3. **UI/UX 표기**:
   - 전투 화면: 쿨타임 중 `disabled` + `[🔒 (N승 남음)]`, 준비 완료 시 `[⚡ (READY!)]` 강조 펄스.
   - 스킬 팝업: `[⚔️ N승 남음]` / `[⚡ 준비 완료]` 뱃지 표시.

---

### 3.4. PASSIVE (컴뱃/레인지/매직/실드 마스터리, 메디테이션, 크리티컬 히트)

#### 신규 record: `PassiveSkill`

```java
public record PassiveSkill(
    String id,
    String label,
    SkillType type,              // PASSIVE
    SkillTalent talent,          // COMMON (공용 탭에서 관리)
    int resourceCost,            // 0
    Map<BonusTarget, Integer> totalStatBonus,   // F→MASTER 전체 누적 보너스
    String description
) implements Skill { ... }
```

#### 패시브 스탯 계산 및 메디테이션 규칙

1. **스탯 분배**: F랭크(0)부터 MASTER(15)까지 랭크 단계(`rank.order()`)에 비례하여 선형 누적 가산.
2. **UI 탭**: **`공용(COMMON)` 탭에서 `디펜스`와 함께 관리**. 전투 액션 슬롯에는 등록되지 않음.
3. **메디테이션 MP 자연 재생**:
   - **발동 시점**: **전투 중 매 턴 종료 시점(공방 해결 후 다음 턴 개시 전)**에 `MP +1~+5` 자연 회복.
   - **필드 이동**: 필드 이동 시에는 MP 회복이 발생하지 않음 (오직 야영/포션/여관으로만 회복).
   - **전투 간 영속성**: 2턴에 전투가 끝나 2회 회복된 경우, 잔여/누적 상태는 다음 전투로 자연스럽게 이어짐.

---

### 3.5. BUFF (마나 실드)

#### 신규 record: `BuffSkill`

```java
public record BuffSkill(
    String id,
    String label,
    SkillType type,              // BUFF
    SkillTalent talent,          // MAGIC
    int resourceCost,            // 마나 15
    int durationTurns,           // 5턴 고정
    Map<SkillRank, Integer> absorbRateByRank,    // 감쇄율 50%~85% (INT 스탯 비례 추가 보너스)
    String description
) implements Skill { ... }
```

#### 전투 규칙

1. **시전 턴 즉시 적용**: 시전 턴 몬스터 공격에 피격되나, 마나 실드가 즉시 발동하여 해당 턴 피격부터 MP 감쇄 흡수 적용.
2. **MP 고갈 시 처리**: 피격 시 보유 MP 한도까지만 감쇄 흡수하고, 부족한 피해는 HP로 전가되며 버프는 유지됨.
3. **재시전 시 갱신(Refresh)**: 버프 지속 중 재시전 시 지속 턴 수(5턴) 및 감쇄율을 최신 수치로 갱신.

---

### 3.6. CC (스파이더 샷)

#### 신규 record: `CcSkill`

```java
public record CcSkill(
    String id,
    String label,
    SkillType type,              // CC
    SkillTalent talent,          // ARCHERY
    int resourceCost,            // 스태미나 8
    Map<SkillRank, Integer> successRateByRank,   // 성공률 20%~50%
    String description
) implements Skill { ... }
```

#### 전투 규칙

1. **데미지 0**: 적에게 피해를 입히지 않음.
2. **시전 턴 피격 후 다음 턴 속박**: 시전 턴에는 몬스터 공격을 그대로 피격받고, 성공 시(20%~50%) **다음 턴 1턴간 몬스터를 행동 불능(턴 스킵)** 상태로 만듦 (`BattleState.monsterStunnedTurns = 1`).
3. 실패 시: 자원만 소모되고 다음 턴 정상 진행.

---

### 3.7. DOT (미라지 미사일)

#### 신규 record: `DotSkill`

```java
public record DotSkill(
    String id,
    String label,
    SkillType type,              // DOT
    SkillTalent talent,          // ARCHERY
    int resourceCost,            // 스태미나 10
    Map<SkillRank, Integer> initialMultiplierByRank,   // 즉발 배율 30%
    Map<SkillRank, Integer> dotPerTurnByRank,           // 턴당 독 배율 (28%)
    Map<SkillRank, Integer> dotTurnsByRank,              // 지속 턴수 1~5턴
    String description
) implements Skill { ... }
```

#### 전투 규칙

1. **즉발 30% 피해** + 매 턴 독 피해 적용 (`BattleState.dotDamagePerTurn`, `dotTurnsLeft`).
2. **재시전 갱신**: 지속 중 재시전 시 지속시간 및 독 피해를 최신 값으로 갱신(Refresh).

---

### 3.8. 특수 공격 스킬 (라이트닝 로드, 아이스 스피어)

1. **라이트닝 로드 (`lightning_rod`)**:
   - `DamageSkill` (HEAVY, 마나 30, 220%~400%).
   - **DEF 100% 관통**: 적 방어력(DEF)을 0으로 계산하여 순수 피해 산출.
2. **아이스 스피어 (`ice_spear`)**:
   - `DamageSkill` (HEAVY, 마나 20, 2타 160%~280%).
   - **다음 턴 빙결 CC**: 2타 적중 후 랭크별 확률(F 20% ~ MASTER 50%)로 다음 턴 몬스터 1턴 빙결(`monsterStunnedTurns = 1`).

---

### 3.9. 스킬 승급(수련) 조건 정책

1. **AP 소모 규칙 (공통)**: F $\rightarrow$ MASTER 총 200 AP 소모.
2. **스킬 타입별 수련치 4대 체계**:
   - **1. 직접 공격형 (`NORMAL`, `HEAVY`)**: 사용 횟수 + 막타 처치 수 (표준 5회/1킬 ~ 5,000회/1,500킬).
   - **2. 지원/특수형 (`DEFENSE`, `RECOVERY`, `BUFF`, `CC`, `DOT`, `DEBUFF`)**: 막타 면제(`killExempt`) + 사용 횟수만 요구.
   - **3. 궁극기형 (`ULTIMATE`)**: 막타 면제 + 전용 소량 사용 수련치 (F→E 1회 ~ 1→Master 20회).
   - **4. 패시브형 (`PASSIVE`)**: 수련 조건 완전 면제, AP만으로 즉시 승급.

---

### 3.10. 스킬 습득 및 시드 정책

1. **초기 시드**: 캐릭터 생성 시 기본 4종(`slash`, `aimed_shot`, `mana_bolt`, `defense`)만 F랭크로 지급.
2. **추후 확장**: 나머지 25종 스킬은 NPC 상점 스킬북 아이템 구매 및 학습 시스템으로 습득.

---

## 4. Sealed Interface & DB 확장 스키마

### 4.1. `Skill` sealed interface permits

```java
public sealed interface Skill permits
    DamageSkill, DefenseSkill,
    RecoverySkill, UltimateSkill, PassiveSkill,
    BuffSkill, CcSkill, DotSkill {
    String id();
    String label();
    SkillType type();
    SkillTalent talent();
    int resourceCost();
    String description();
}
```

### 4.2. `BattleState` JPA Entity 컬럼 추가

| 추가 컬럼 | 타입 | 기본값 | 용도 |
|---|---|---|---|
| `next_attack_amp_percent` | INT | 0 | DEBUFF(레이지 임팩트) 다음 피해 +30% 증폭 |
| `mana_shield_turns_left` | INT | 0 | BUFF(마나 실드) 남은 지속 턴 |
| `mana_shield_absorb_rate` | INT | 0 | BUFF(마나 실드) MP 감쇄 흡수율 |
| `monster_stunned_turns` | INT | 0 | CC(스파이더 샷/아이스 스피어) 다음 턴 속박/빙결 |
| `dot_damage_per_turn` | INT | 0 | DOT(미라지 미사일) 턴당 독 피해 |
| `dot_turns_left` | INT | 0 | DOT(미라지 미사일) 남은 독 턴 |

### 4.3. `CharacterSkill` JPA Entity 컬럼 추가

| 추가 컬럼 | 타입 | 기본값 | 용도 |
|---|---|---|---|
| `ultimate_cooldown` | INT | 0 | 궁극기 남은 전투 승리 횟수 (0 = 준비 완료) |

---

## 5. 빌드 검증 파이프라인 (Task 완료 시 필수)

```bash
mvn -B -q spotless:apply -pl myrpg && \
(mvn -B clean install -pl myrpg -am > /tmp/mvn.log 2>&1 || \
 (tail -n 30 /tmp/mvn.log && exit 1)) && \
tail -n 12 /tmp/mvn.log && \
codegraph sync
```

5대 가드레일: Spotless → Error Prone → ArchUnit → JaCoCo(80%) → PMD/CPD
