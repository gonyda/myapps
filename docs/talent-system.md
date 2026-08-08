# 재능 시스템 & AP(어빌리티 포인트) 상세 설계

> 로드맵 우선순위 **1순위 AP**와 **2순위 재능 시스템**을 하나의 문서로 통합한다.
> AP는 개발 분량이 작고 "레벨업/환생 시 지급 → 스킬 랭크업에서 소모"라는 흐름이
> 재능·스킬 성장 축과 맞물리므로 함께 설계한다.
> 본 문서는 스펙(`.kiro/specs/myrpg/...`) 생성을 위한 사전 설계 노트이며,
> 실제 소스(`myrpg`, `com.myapps.web.myrpg`) 위에 어떻게 쌓을지를 기준으로 작성했다.

---

## 1. 목표와 범위

| 구분 | 포함 | 제외(연계) |
|---|---|---|
| AP | 지급 규칙(레벨업/환생), 보유량 저장, 정보 팝업 표시, 소모 API(정의만) | 실제 소모 로직 → 3순위 스킬 시스템 |
| 재능 | 재능 선택(3종 택1), 재능별 레벨업 보너스(주 스탯 + 보조 성장), 데미지 보너스 정의 | 데미지 실제 적용 → 7순위 전투 / 스킬 기여 → 3순위 스킬 |

핵심 방향:

1. **AP는 "누적레벨과 동기화되는 성장 포인트"** 로 설계한다. 현재 소스의 누적레벨(`accumulatedLevel`)은
   레벨업·환생 때마다 +1 되며, AP도 정확히 같은 시점에 +1 지급된다. 따라서
   **획득한 총 AP = `accumulatedLevel - 1`** 이라는 불변식이 자연스럽게 성립한다(소모 전 기준).
2. **재능 효과는 기존 "스탯을 저장하지 않고 레벨에서 계산" 원칙을 그대로 확장**한다.
   재능(`talent`)은 이미 영속 저장되고 한 생애 동안 불변이므로,
   `Level_Stat = 기본값 + 레벨파생(레벨, 재능)`과 `바이탈 최대치 = 기본값 + 레벨파생(레벨, 재능)`으로 계산하면
   **새 스탯 저장 필드 없이** 재능별 성장(주 스탯 + 보조 성장)이 구현된다.
   레벨 파생분은 저장하지 않으므로 환생(레벨 1 복귀) 시 재능 보너스도 자동 초기화된다.
   단, 근접(HP)·마법(MP)이 특정 바이탈만 올리므로 현재의 "단일 바이탈 최대치" 구조를 **바이탈별 최대치**로 바꾸는 소규모 리팩터가 필요하다(§4.2).
3. **재능 선택은 환생 흐름에 통합**한다. 003이 환생 시 재능을 `MELEE`로 고정하던 정책을
   "환생 시 3종 중 택1"로 대체한다.
4. **재능 목록·효과는 `TalentType` enum이 자체 보유**한다(기존 `NpcType`가 label/emoji/actions를 enum에 담는 것과 동일 패턴).
   재능은 개체(인스턴스)가 아니라 **타입**이므로 별도 JSON 파일을 두지 않고, enum 상수에 라벨·보너스·데미지%·효과 요약을 담아
   3종 완비·컴파일 타임 안정성을 보장한다.

---

## 2. 현재 소스 분석 (통합 대상)

003(`character-progression-and-rebirth`)까지 구현된 상태를 기준으로 한다.

### 2.1 이미 존재하여 재사용/확장하는 것

| 위치 | 현재 상태 | 본 설계에서의 역할 |
|---|---|---|
| `domain/model/TalentType.java` | `MELEE/ARCHERY/MAGIC` + 한글 라벨. `MELEE`만 실사용 | 라벨·주/보조 보너스·데미지%·효과 요약을 enum에 보유(`NpcType` 패턴)로 확장, 3종 모두 실사용 승격 |
| `domain/model/CharacterProgress.java` | `talent` 저장, `accumulatedLevel` 저장, 의도 드러내는 mutator | `abilityPoints` 필드 추가. 재능 선택을 위한 `setTalent` 재사용 |
| `domain/model/StatProgression.java` | `levelStatsFor(level)`, `vitalMaxFor(level)` 순수 계산 | **재능 인자를 받는 오버로드**로 확장(재능별 레벨 보너스) |
| `domain/model/Stats.java` | `record(str,dex,int,critical,defense)`, `ZERO` | 그대로 사용(재능 보너스 합산 결과 표현) |
| `application/service/ProgressionService.java` | `gainExperience`, `rebirth(p)`, 사망 패널티 | 레벨업/환생 시 **AP 지급** 추가, `rebirth`가 **선택 재능**을 받도록 시그니처 변경 |
| `application/dto/InfoPopupView.java` | 상/중/하 3구역 뷰 모델 | `abilityPoints`·`talentEffectSummary` 필드 추가 |
| `interfaces/api/PlayScreenViewHelper.java` | `buildInfo(progress, status)` | 중앙 스탯을 `levelStatsFor(level, talent)`로 계산, AP·효과 요약 매핑 |
| `interfaces/api/PlayScreenController.java` | `POST /rebirth` | `talent` 파라미터 수신 → `rebirth(p, talent)` 위임 |
| `resources/templates/fragments/info-popup.html` | 재능 라벨 표시, 하단 환생 버튼 | 재능 밑에 **보유 AP**·**재능 효과** 행 추가([환생하기] 버튼은 그대로) |
| `resources/static/js/myrpg.js` | `rebirth()` → confirm → `POST /rebirth` | 확인 후 재능 선택 팝업을 띄우고, 선택 재능을 파라미터로 전송 |

### 2.2 현재 값 흐름 (근거)

- `gainExperience(p, amount)`는 연속 레벨업 시 `increaseAccumulatedLevel(gained)`을 호출한다
  → **AP도 `gained`만큼 지급**하면 레벨업당 1 AP가 성립.
- `rebirth(p)`는 `increaseAccumulatedLevel(1)` + `setTalent(MELEE)`를 호출한다
  → **AP +1 지급**, `MELEE` 고정을 **선택 재능**으로 대체.
- 사망 패널티(`applyDeathPenalty`)는 레벨/누적/재능 불변 → **AP도 불변**.
- `buildInfo`는 `statProgression.levelStatsFor(level)` + `Stats.ZERO`(스킬 보너스)로 중앙 스탯을 만든다
  → 재능 보너스는 `levelStatsFor(level, talent)` 본체에 녹여 표시.

---

## 3. AP(어빌리티 포인트) 설계

### 3.1 지급/소모 규칙

| 이벤트 | AP 변화 | 근거 소스 |
|---|---|---|
| 첫 캐릭터 생성 | `0` | `CharacterProgress.createDefault()` |
| 레벨업(연속 포함) | `+gained` (레벨 1당 +1) | `ProgressionService.gainExperience` |
| 환생 | `+1` (누적레벨 +1과 동기) | `ProgressionService.rebirth` |
| 사망 패널티 | 변화 없음 | `applyDeathPenalty` |
| 스킬 랭크업 | `-cost` (소모) | 3순위 스킬 시스템(정의만) |

> **불변식**: 소모가 발생하기 전까지 `abilityPoints == accumulatedLevel - 1`.
> 소모 도입 후에는 `abilityPoints == (accumulatedLevel - 1) - 누적 소모량`.

### 3.2 데이터 모델

`CharacterProgress`에 저장 필드 1개를 추가한다.

| 필드 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `abilityPoints` | `int` (`@Column(name = "ability_points", nullable = false)`) | `0` | 현재 **보유** AP 잔량 |

- `createDefault()`에 `abilityPoints = 0` 반영, 생성자 인자에 추가.
- mutator(의도 드러내기, setter 남용 금지):
  - `increaseAbilityPoints(int amount)` — 레벨업/환생 지급
  - `spendAbilityPoints(int amount)` — 스킬 랭크업 소모(3순위에서 호출). `amount > 보유량`이면 도메인 규칙 위반으로 예외(스킬 시스템 스펙에서 구체화).

> **저장 방식 결정**: AP는 소모 때문에 레벨에서 순수 계산할 수 없으므로 **잔량을 직접 저장**한다
> (`experience`/`accumulatedLevel`을 저장하는 기존 패턴과 동일). 대안으로 "소모 누적량만 저장하고
> 잔량 = `(accumulatedLevel-1) - 소모량`으로 파생"하는 방식이 불변식을 강제로 보장하지만,
> 구현이 복잡하고 아직 소모 주체가 없으므로 **잔량 직접 저장**을 채택한다(불변식은 프로퍼티 테스트로 검증).

### 3.3 지급 로직 (ProgressionService)

```text
gainExperience(p, amount):
    ... 기존 연속 레벨업 처리 ...
    if (gained > 0):
        p.setCurrentLevel(level)
        p.increaseAccumulatedLevel(gained)
        p.increaseAbilityPoints(gained)      # ← 추가: 레벨 1당 AP 1
        p.fullRecover(vitalMaxFor(level, talent))

rebirth(p, chosenTalent):
    ... 쿨다운 검증 ...
    p.setCurrentLevel(1); p.setExperience(0)
    p.increaseAccumulatedLevel(1)
    p.increaseAbilityPoints(1)               # ← 추가: 환생 시 AP 1
    p.setTalent(chosenTalent)                # ← 변경: MELEE 고정 → 선택 재능
    p.setLastRebirthAt(now)
    p.fullRecover(vitalMaxFor(1, chosenTalent))
```

### 3.4 표시 (정보 팝업 상단)

`InfoPopupView`에 `int abilityPoints` 추가. `info-popup.html`의 **재능 행 바로 아래**에 보유 AP 행을 추가한다.

```html
<!-- info-popup.html : 재능 행 다음 -->
<div class="info-row">
    <span class="info-label">재능</span>
    <span th:text="${view.info.talentLabel}">근접전투</span>
</div>
<div class="info-row">
    <span class="info-label">보유 AP</span>
    <span th:text="${view.info.abilityPoints}">0</span>
</div>
```

- `PlayScreenViewHelper.buildInfo`에서 `progress.getAbilityPoints()`를 매핑.
- 갱신 경로는 기존 `progress-response`(top-bar + info-content + action-log 스왑)를 그대로 사용하므로,
  레벨업/환생 후 팝업이 열린 상태에서도 AP가 즉시 갱신된다(`swapProgressResponse` 재사용).

---

## 4. 재능 시스템 설계

### 4.1 재능 선택 흐름 (환생 시 2단계)

003은 신규 생성·환생 모두 `MELEE` 고정이었다. 재능 시스템은 환생 시 **재능 택1**로 대체한다.

- **신규 캐릭터**: 기본 `MELEE` 유지(최초 1회 선택 UI는 도입하지 않음 — 단순화).
- **환생 시 택1**: 기존 확인 흐름을 유지한 채, **확인 후 재능 선택 팝업**을 띄운다.

**2단계 흐름**:

```text
[환생하기] 클릭
  → 1단계: confirm("환생을 진행하시겠습니까?")   (기존 유지)
  → 예 선택
  → 2단계: 재능 선택 팝업("재능을 선택해주십시오")  ← 신규 작은 오버레이
           [근접전투] [활] [마법]  (+ [취소])
  → 재능 클릭
  → POST /rebirth?talent=MELEE|ARCHERY|MAGIC
  → progress-response 스왑(top-bar / info-content / action-log)
```

통합 지점:

```text
info-popup.html (하단):  [환생하기] 버튼은 그대로 (선택 UI를 여기 넣지 않음)
talent-select.html:      신규 작은 오버레이 프래그먼트(#talentSelectOverlay)
                         3종 버튼 + 취소, play.html에 include
myrpg.js:                rebirth()      = confirm 통과 시 openTalentSelect()만 수행
                         confirmRebirth(talent) = POST /rebirth?talent=… → swapProgressResponse → closeTalentSelect()
                         (기존 rebirth()가 바로 POST 하던 것을 → 팝업 오픈으로 변경)
PlayScreenController:     POST /rebirth 에 @RequestParam TalentType talent 추가
                         → progressionService.rebirth(p, talent)
```

- 서버는 환생 쿨다운(24h)을 재검증(003 정책 유지)하고, `talent` 미지정/이상값이면 `MELEE`로 폴백한다.
- 재능 선택 팝업의 기본 강조값은 현재 재능(`view.info.talent`)으로 둔다.
- 취소 시 팝업만 닫고 환생은 수행하지 않는다(상태 불변).

### 4.2 효과 A — 재능별 레벨업 보너스 (주 스탯 + 보조 성장)

기존 레벨업 성장(재능 무관)은 유지하고, 선택 재능에 대해 **주 스탯 1개 + 보조 성장 1개**를
레벨업당 추가로 부여한다. 보조 성장은 재능 아키타입에 맞게 배분한다.

| 재능 | 주 스탯(+2/Lv) | 보조 성장 | 아키타입 |
|---|---|---|---|
| MELEE(근접) | STR | **HP** 최대치 +5/Lv | 맷집/전열 (탱커형) |
| ARCHERY(활) | DEX | **Critical** +0.1%/Lv (저장값 +1) | 순수 딜 (글래스캐논) |
| MAGIC(마법) | INT | **MP** 최대치 +5/Lv | 자원/지속 (시전자) |

> 활은 생존 보너스 대신 치명타를 받아 "딜은 세지만 무르다"는 글래스캐논 성격을 갖는다(공격 스탯 DEX·Critical 2종).
> 근접(HP)·마법(MP)은 각각 생존·자원 축으로 차별화된다.

**Lv.100 기준 체감(제안 수치)**

| 항목 | 재능 무관(공통) | 재능 주력 | 차이 |
|---|---|---|---|
| 주 스탯 (STR/DEX/INT) | `10 + 3·99 = 307` | `+2·99 = +198` → **505** | 약 **+64%** |
| HP 또는 MP 최대치 (근접/마법) | `100 + 10·99 = 1090` | `+5·99 = +495` → **1585** | 약 **+45%** |
| Critical (활) | `50 + 3·99 = 347` (34.7%) | `+1·99 = +99` → **446 (44.6%)** | **+9.9%p** |

- 성장 상수(`+2` 주 스탯, `+5` 바이탈, `+1`=0.1% 치명)는 **밸런스 조절 노브**다.
  치명타는 데미지를 크게 좌우하므로 보수적으로 `+0.1%/Lv`(Lv.100 44.6%)를 기본값으로 두고,
  전투(7순위) 실측 후 필요하면 `+0.2%/Lv`(54.5%)로 상향한다.
- **환생 리셋과의 관계**: 스탯·바이탈 최대치 모두 저장하지 않고 레벨에서 계산하므로,
  환생(레벨 1) 시 재능 보너스(주 스탯·보조 성장 모두)가 0으로 자동 복귀한다.
  "레벨업 성장은 환생 시 초기화"라는 기존 전제를 그대로 유지 → 영구 인플레 없음.

#### 구현 — 대상 종류 기반 자동 적용

재능별 성장 수치·대상은 `TalentType` enum이 자체 보유한다(§4.5). 각 재능은 `주 보너스`·`보조 보너스`를
`(대상 target, 레벨당 증가 perLevel)` 쌍으로 갖고, `StatProgression`이 **대상 종류에 따라 자동 적용**한다.

- 대상이 **스탯**(STR/DEX/INT/CRITICAL)이면 `levelStatsFor(level, talent)`에서 반영.
- 대상이 **바이탈**(HP/MP/STAMINA)이면 `vitalMaxFor(level, talent)`에서 반영.

```java
// StatProgression (순수 정책 유지 — 재능 보너스는 TalentType가 자체 보유)
public Stats levelStatsFor(final int level, final TalentType talent) {
    Stats result = levelStatsFor(level);                        // 공통(재능 무관) 계산 재사용
    result = applyStatBonus(result, talent.primary(), level);
    result = applyStatBonus(result, talent.secondary(), level); // 대상이 STAT일 때만 반영
    return result;
}

public VitalMax vitalMaxFor(final int level, final TalentType talent) {
    final int common = BASE_VITAL + VITAL_PER_LEVEL * (level - 1);
    VitalMax result = new VitalMax(common, common, common);
    result = applyVitalBonus(result, talent.primary(), level);
    result = applyVitalBonus(result, talent.secondary(), level); // 대상이 VITAL일 때만 반영
    return result;
}
// applyStatBonus/applyVitalBonus: bonus.target().kind()로 STAT/VITAL 분기 후 perLevel×(level-1) 적용
```

구조 변경/헬퍼:

- **신규 도메인 VO** `VitalMax(int hp, int mp, int stamina)` — 바이탈별 최대치.
  현재 소스는 HP/MP/Stamina가 단일 최대치(`vitalMaxFor(level)` + `fullRecover(int max)`)를 공유하므로,
  재능이 특정 바이탈만 올리려면 **바이탈별 최대치**로 분리하는 소규모 리팩터가 필요하다.
- `Stats`에 델타 헬퍼(`withStrDelta`/`withDexDelta`/`withIntDelta`/`withCriticalDelta`) 추가(불변 record).
- `CharacterProgress.fullRecover(int max)` → `fullRecover(int hpMax, int mpMax, int staminaMax)`(또는 `fullRecover(VitalMax)`).
- `ProgressionService`: `gainExperience`/`rebirth`의 풀회복 호출을 `fullRecover(vitalMaxFor(level, talent))`로 교체.
- `PlayScreenViewHelper`: 상단바·정보 팝업 HP/MP/Stamina 게이지를 `VitalMax` 각 필드로 조립,
  중앙 스탯은 `levelStatsFor(level, talent)`(재능 보너스 포함, 괄호는 스킬분 `+0`).
- 기존 단일 `vitalMaxFor(int level)`는 제거 후 호출부 교체(권장).

> `StatProgression`은 순수 정책을 유지한다(카탈로그·주입 불필요). 재능 보너스 수치·대상은 `TalentType` enum이 직접 보유한다(§4.5).
> 이 변경은 003 산출물(`StatProgression`, `CharacterProgress.fullRecover`, `PlayScreenViewHelper`)과
> 관련 테스트에 영향을 준다(§9). 규모는 크지 않고 계층 내에 잘 격리된다.

### 4.3 효과 B — 데미지 보너스 (전투 연계, 정의만)

- 각 재능은 "재능과 일치하는 공격 타입"에 **데미지 +10%**를 부여한다
  (근접 재능 → 근접 공격 +10%, 활 → 원거리 +10%, 마법 → 마법 +10%).
- 전투(7순위)가 아직 없으므로 **modifier 값만 정의**하고 실제 적용은 전투 스펙으로 이연한다.
- 값은 `TalentType`이 보유하며(§4.5), 전투 시스템은 `talent.damageBonusPercent()` 훅을 소비한다.

> 대안으로 "재능 무관 전체 데미지 +10%"도 있으나, 세 재능이 동일해져 차별성이 사라지므로 **비추천**.
> 효과 A(주 스탯 성장)가 이미 재능 정체성을 만들고, 효과 B는 전투에서 그 정체성을 강화하는 역할로 둔다.

### 4.4 표시 — 재능 효과 요약 (확정: 표시함)

정보 팝업 상단, 재능 행·보유 AP 행과 함께 **재능 효과 요약**을 노출한다.
요약 문자열은 `TalentType.effectSummary()`를 사용한다.

- `InfoPopupView`에 `String talentEffectSummary` 추가, `buildInfo`에서 현재 재능의 `effectSummary()`를 매핑.
- `info-popup.html` 예(재능 행 아래):

```html
<div class="info-row">
    <span class="info-label">재능 효과</span>
    <span th:text="${view.info.talentEffectSummary}">근접 데미지 +10%, STR +2/Lv, HP +5/Lv</span>
</div>
```

### 4.5 재능 데이터 (TalentType enum)

재능은 개체(인스턴스)가 아니라 **타입**이므로, 기존 `NpcType`(enum에 label/emoji/actionLabels를 하드코딩)과 동일하게
**`TalentType` enum이 라벨·보너스·데미지%·효과 요약을 자체 보유**한다(별도 JSON 파일 없음).
enum은 3종 완비·컴파일 타임 안정성을 보장하고, 값 변경은 상수 수정 후 재빌드로 반영한다.

**모델**

- `domain/model/BonusTarget.java` — enum `STR, DEX, INT, CRITICAL, HP, MP, STAMINA` + `kind()`(`STAT`/`VITAL` 분류).
  스탯/바이탈 적용 분기의 단일 소스.
- `domain/model/TalentBonus.java` — `record TalentBonus(BonusTarget target, int perLevel)`.
- `domain/model/TalentType.java` — 각 상수가 라벨·주 보너스·보조 보너스·데미지%·효과 요약을 보유.

```java
public enum TalentType {
    MELEE("근접전투",
          new TalentBonus(BonusTarget.STR, 2),
          new TalentBonus(BonusTarget.HP, 5),
          10, "근접 데미지 +10%, STR +2/Lv, HP +5/Lv"),
    ARCHERY("활",
          new TalentBonus(BonusTarget.DEX, 2),
          new TalentBonus(BonusTarget.CRITICAL, 1),   // 0.1% 단위
          10, "원거리 데미지 +10%, DEX +2/Lv, 치명 +0.1%/Lv"),
    MAGIC("마법",
          new TalentBonus(BonusTarget.INT, 2),
          new TalentBonus(BonusTarget.MP, 5),
          10, "마법 데미지 +10%, INT +2/Lv, MP +5/Lv");

    private final String label;
    private final TalentBonus primary;
    private final TalentBonus secondary;
    private final int damageBonusPercent;
    private final String effectSummary;
    // 생성자 + 접근자: label(), primary(), secondary(), damageBonusPercent(), effectSummary()
}
```

- `target` 어휘: 스탯(`STR`/`DEX`/`INT`/`CRITICAL`) 또는 바이탈(`HP`/`MP`/`STAMINA`).
- `CRITICAL`의 `perLevel`은 0.1% 단위 정수(값 `1` = 레벨당 +0.1%), 바이탈 `perLevel`은 최대치 증가량.

**소비처**

- `StatProgression`: `talent.primary()`/`talent.secondary()`를 대상 종류에 따라 스탯/바이탈에 적용(§4.2).
- `InfoPopupView.talentLabel`·`talentEffectSummary`: `talent.label()`·`talent.effectSummary()` 매핑.
- 재능 선택 팝업(§4.1) 버튼 라벨: `TalentType.values()` + `label()`.
- 데미지 보너스(§4.3): `talent.damageBonusPercent()` 훅.

> `NpcType`가 label/emoji/actions를 enum에 보유하는 것과 동일한 구성이다. JSON은 이 코드베이스에서 **인스턴스 데이터**(npc/map/ambience)에만 쓰이며,
> 재능처럼 닫힌 소수의 **타입 메타데이터**는 enum이 담당한다. 재능이 크게 늘거나 밸런스를 빈번히 튜닝하게 되면 그때 `talent.json` 외부화로 리팩터할 수 있다(현 시점 YAGNI).

---

## 5. 통합 변경 요약 (파일별)

| 파일 | 변경 유형 | 내용 |
|---|---|---|
| `domain/model/CharacterProgress.java` | 확장 | `abilityPoints` 필드/생성자/`createDefault`/`increaseAbilityPoints`/`spendAbilityPoints`, `fullRecover`를 바이탈별(3인자)로 변경 |
| `domain/model/StatProgression.java` | 확장 | `levelStatsFor(int, TalentType)`, `vitalMaxFor(int, TalentType)→VitalMax` 오버로드(순수 정책 유지) |
| `domain/model/VitalMax.java` | **신규** | `record VitalMax(int hp, int mp, int stamina)` (바이탈별 최대치 VO) |
| `domain/model/Stats.java` | 확장 | 델타 조립 헬퍼(`withStrDelta`/`withDexDelta`/`withIntDelta`/`withCriticalDelta`, 불변 record) |
| `domain/model/TalentType.java` | 확장 | 라벨·주/보조 보너스(`TalentBonus`)·데미지%·효과 요약을 enum에 보유(`NpcType` 패턴), 3종 실사용 승격 |
| `domain/model/BonusTarget.java` | **신규** | enum(STR/DEX/INT/CRITICAL/HP/MP/STAMINA) + `kind()`(STAT/VITAL) |
| `domain/model/TalentBonus.java` | **신규** | `record TalentBonus(BonusTarget target, int perLevel)` |
| `application/service/ProgressionService.java` | 확장 | 레벨업/환생 AP 지급, `rebirth(p, TalentType)` 시그니처, 풀회복을 `vitalMaxFor(level, talent)` 기준으로 |
| `application/dto/InfoPopupView.java` | 확장 | `int abilityPoints`, `String talentEffectSummary` |
| `interfaces/api/PlayScreenViewHelper.java` | 확장 | 재능 반영 스탯 계산, HP/MP/Stamina 게이지를 `VitalMax` 각 필드로 조립, AP 매핑 |
| `interfaces/api/PlayScreenController.java` | 확장 | `POST /rebirth`에 `@RequestParam TalentType talent`(폴백 `MELEE`) |
| `templates/fragments/info-popup.html` | 확장 | 재능 밑에 **보유 AP** 행 + **재능 효과** 요약 행 추가([환생하기] 버튼은 그대로) |
| `templates/fragments/talent-select.html` | **신규** | 환생 재능 선택 팝업(3종 버튼 + 취소) |
| `templates/play.html` | 확장 | `talent-select` 프래그먼트 include |
| `static/js/myrpg.js` | 확장 | `rebirth()`=확인 후 `openTalentSelect()`, `confirmRebirth(talent)`=`POST /rebirth?talent=…`, `openTalentSelect/closeTalentSelect` |

> 새로운 영속 테이블/엔티티는 없다(AP는 단일 컬럼 추가). 재능 효과 수치는 `TalentType` enum이 보유하고,
> 재능별 성장은 전부 레벨 계산으로 해결한다. 코드 구조 변경은 "단일 바이탈 최대치 → 바이탈별 최대치(`VitalMax`)"뿐이며 계층 내에 격리된다.

---

## 6. 확정 사항

1. **재능 효과 개수** — ✅ **2가지로 확정**: A(재능별 레벨업 보너스: 주 스탯 + 보조 성장) + B(데미지 +10% 훅).
   데미지·회피 등 추가 효과는 전투(7순위) 도입 후 실측하여 검토한다.
2. **주 스탯 레벨업 보너스** — ✅ **주 스탯 +2/Lv 확정**(MELEE→STR, ARCHERY→DEX, MAGIC→INT).
3. **보조 성장** — ✅ 확정: **MELEE→HP +5/Lv, MAGIC→MP +5/Lv, ARCHERY→Critical +0.1%/Lv**(글래스캐논, 저장값 +1).
   근접/마법의 바이탈 보너스를 위해 "단일 바이탈 최대치 → 바이탈별 최대치(`VitalMax`)" 리팩터 동반(§4.2). 활 치명은 스탯 계산으로 처리.
4. **재능 선택 시점/방식** — ✅ **환생 시 2단계 확정**: `환생하시겠습니까?` 확인 → 예 → `재능을 선택해주십시오` 작은 팝업에서 택1.
   **첫 캐릭터는 근접(`MELEE`) 고정**(생성 시 선택 UI 없음)(§4.1).
5. **AP 저장 방식** — ✅ **잔량 직접 저장 확정.** 불변식 `획득 AP = 누적레벨 − 1`은 프로퍼티 테스트로 보증.
6. **재능 효과 요약 표시** — ✅ 정보 팝업에 **표시함**(재능 행 아래). 요약 문자열은 `TalentType.effectSummary()`(§4.4).
7. **재능 데이터 관리** — ✅ 재능 목록·효과(라벨/보너스/데미지%/요약)를 **`TalentType` enum이 보유**(`NpcType` 선례, §4.5). 별도 JSON 파일 없음. 향후 필요 시 JSON 외부화.
8. **기존 로컬 세이브 처리** — ✅ **(C) 로컬 DB 파일(`./data/myrpg`) 삭제 후 새로 시작.** 로컬은 H2 파일(ddl-auto: update)이라 세이브가 유지되므로, 새 캐릭터로 초기화해 불변식을 깨끗이 맞춘다. 프로덕션은 ddl-auto: create라 자동 초기화.

> 성장 수치(`+2` 주 스탯, `+5` 바이탈, `+1`=0.1% 치명, 데미지 `10%`)는 `TalentType` 상수에서 조정하며 재빌드로 반영한다.

---

## 7. 정합성 프로퍼티 (스펙 작성 시 시드)

- **P-AP-1 (AP 지급)**: 임의의 레벨업 시퀀스에서 AP 증가량 = 누적레벨 증가량.
- **P-AP-2 (불변식)**: 소모가 없는 한 `abilityPoints == accumulatedLevel - 1`.
- **P-AP-3 (사망 불변)**: 사망 패널티는 AP를 변경하지 않는다.
- **P-TAL-1 (주 스탯 성장)**: `levelStatsFor(L, talent)`의 주 스탯 = `기본 주 스탯(L) + 2·(L-1)`, 비주력 스탯은 재능 무관.
- **P-TAL-2a (보조 바이탈: 근접/마법)**: `vitalMaxFor(L, MELEE)`는 HP만 `common(L)+5·(L-1)`,
  `vitalMaxFor(L, MAGIC)`는 MP만 그러하며, 그 외 바이탈과 `ARCHERY`의 세 바이탈은 모두 `common(L)=100+10·(L-1)`.
- **P-TAL-2b (보조 치명: 활)**: `levelStatsFor(L, ARCHERY)`의 Critical = `공통 Critical(L) + 1·(L-1)`(0.1% 단위),
  다른 재능의 Critical은 공통값과 동일.
- **P-TAL-3 (환생 리셋)**: 환생 후 `currentLevel=1`이면 재능 보너스분(주 스탯·보조 성장 모두) = 0(기본값 복귀).
- **P-TAL-4 (환생 재능 반영)**: `rebirth(p, T)` 후 `talent == T`, `abilityPoints += 1`, `accumulatedLevel += 1`.
- **P-VITAL-RECOVER**: 레벨업/환생 풀회복 후 HP/MP/Stamina 현재값 = `vitalMaxFor(레벨, talent)`의 각 대응 필드.
- **P-TALENT-DATA (재능 정의 완비)**: `TalentType` 3종 각각 `primary`/`secondary`(유효 `target`·비음수 `perLevel`),
  `damageBonusPercent`(≥0), 비어있지 않은 `label`/`effectSummary`를 보유한다.
- **P-PERSIST**: `abilityPoints`·`talent` 포함 라운드트립 보존.

## 8. 테스트 전략

- **프로퍼티(jqwik)**: 위 P-* 항목. `@Mock` 금지 → `Mockito.mock()` 직접, `@Property(tries = 100)`.
- **단위/예시**: 신규 생성 AP=0, 레벨업 3회 시 AP=3, 환생 후 AP+1·재능 반영,
  재능별 `levelStatsFor` 샘플(예: MAGIC Lv.10 → INT `10 + 3·9 + 2·9 = 55`,
  ARCHERY Lv.10 → Critical `50 + 3·9 + 1·9 = 86`(8.6%)),
  재능별 `vitalMaxFor` 샘플(예: MELEE Lv.10 → HP `100 + 10·9 + 5·9 = 235`, MP/Stamina `190`; ARCHERY는 세 바이탈 모두 `190`),
  데미지 보너스 상수.
- **재능 정의(TalentType)**: 3종 완비 + 각 상수의 `label`/`primary`/`secondary`/`damageBonusPercent`/`effectSummary` 값 검증
  (`TalentTypeTest`, `NpcTypeCompletenessPropertyTest`류).
- **슬라이스/통합(Spring Boot 4.0)**:
  - `@WebMvcTest` — `GET /` 정보 팝업에 보유 AP·재능 효과 요약 노출, `POST /rebirth?talent=ARCHERY` 반영.
  - `@DataJpaTest`(+`@TestConstructor(ALL)`) — `abilityPoints` 라운드트립.
  - `@SpringBootTest` — 컨텍스트 로드.
- **빌드 검증**: Task 완료 전 `mvn test -pl myrpg` 통과 + `mvn clean install -pl myrpg -am` `BUILD SUCCESS`.

## 9. Migration 영향 범위 (기존 산출물)

- `CharacterProgress`: 컬럼/생성자/`createDefault` 변경 → `createDefault` 기본값 테스트,
  라운드트립 테스트(`CharacterProgressRepositoryTest`, `CharacterProgress*PropertyTest`) 갱신.
- `CharacterProgress.fullRecover`: 단일 인자 → 바이탈별(3인자 또는 `VitalMax`) 변경 →
  `LevelUpFullRecoveryPropertyTest` 등 풀회복 검증 테스트 갱신.
- `StatProgression`: 재능 오버로드 추가(`levelStatsFor`/`vitalMaxFor`), 단일 `vitalMaxFor(int)` 제거 시 호출부 전수 교체 →
  `StatProgressionPropertyTest` 보강.
- `ProgressionService`: `rebirth` 시그니처 변경(`rebirth(p, talent)`), AP 지급 추가, 풀회복 호출 변경 →
  `RebirthEffectPropertyTest`, `RebirthCooldownPropertyTest`, `ProgressionServiceTest`,
  `GainExperienceLevelUpPropertyTest`, `AccumulatedLevelInvariantPropertyTest`, `MaxLevelCapPropertyTest` 영향.
- `PlayScreenViewHelper` / `InfoPopupView`: 재능 반영 스탯·바이탈별 게이지·AP 매핑 →
  `PlayScreenViewHelperInfoTest`, `PlayScreenViewHelperGaugePropertyTest` 갱신.
- `PlayScreenController`: `/rebirth` 파라미터 → `PlayScreenControllerProgressionTest` 갱신.
- `TalentType`: 라벨 유지 + 주/보조 보너스·데미지%·효과 요약 필드 추가(`NpcType` 패턴) →
  `TalentTypeTest`를 라벨·보너스·효과 요약 값 검증으로 보강.
- 신규 산출물: `BonusTarget`/`TalentBonus`/`VitalMax` 값 타입 테스트 추가.
- 맵/이동/NPC/상황멘트 관련 산출물은 영향 없음.
