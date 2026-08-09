# 스킬 시스템 상세 문서 (skill-system.md)

> **3순위 기능** — `docs/todo.md` 로드맵 3순위. 개발 완료 후 삭제한다(로드맵 규칙).
> **대상 모듈**: `myrpg` (`com.myapps.web.myrpg`). Spring Boot 4.0 / DDD 4계층.
> **선행 스펙**: 004(AP·재능) 위에 쌓인다. 004가 남겨둔 훅(`spendAbilityPoints`, `Skill_Rankup_Bonus` 표시 자리, `TalentType.damageBonusPercent()`)을 실제로 채우는 스펙이다.
> **후속 스펙 폴더**: 실제 스펙은 `.kiro/specs/myrpg/005-skill-system/`(requirements/design/tasks)으로 생성 예정(spec-conventions 순번 규칙).
>
> 이 게임은 마비노기 계열 구조다(재능·AP·환생·랭크 F→1→Master). 아래 결정도 그 레퍼런스를 참고해 제안한다.

---

## 0. 먼저 결정해 주세요 (Open Decisions)

아래 항목은 스펙(005) 작성 전에 확정이 필요하다. **★ 표시**는 사용자가 직접 "의논 필요"라고 남긴 항목이고, **✅ 표시는 확정된 결정**이다(미표시는 추천값만 있는 미확정 상태). 각 항목의 상세 논의는 대응 섹션에서 이어진다.

| ID | 주제 | 핵심 선택지 | 추천안 | 관련 섹션 |
|---|---|---|---|---|
| **D1 ★** ✅ | 데미지 산출 방식 | (A) 스킬별 고정 배율(%) / (B) 랭크별 배율 테이블 | **B 확정** — 랭크업 시 배율 상승. 전체 전투 공식은 §4 계약 참조 | §4 |
| **D2** ✅ | 랭크 사다리 확정 | (A) F→E→D→C→B→A→9→8→7→6→5→4→3→2→1→Master (16단계) / (B) 축약 | **A 확정** (마비노기 정통, 사용자 원안) | §5 |
| **D3** ✅ | 랭크업 조건 수치 | 랭크별 (막타 처치 수 + 사용 횟수) 임계값 테이블을 정책으로 | **§6 테이블 확정** (누적 사용 ~12,500 / 막타 ~3,800) | §6 |
| **D4 ★** ✅ | 랭크업 시 영구 상승 스탯 | 스탯 계열만 vs 바이탈 포함 / 대칭 vs 재능데이터 그대로 | **확정** — 스탯 계열만 + **대칭(재능 주 스탯 1개 +1/랭크업)**: MELEE→STR, ARCHERY→DEX, MAGIC→INT, COMMON→DEF (바이탈 미포함, `vitalMaxFor` 무변경) | §8 |
| **D5** ✅ | 랭크업 AP 소모량 | (A) 랭크당 고정 / (B) 후반 급증 테이블 | **B 확정** — F→Master 총 200 AP (누적레벨 ~200에 한 스킬 마스터하도록 역산) | §9 |
| **D6 ★** ✅ | 방어 스킬의 재능 소속 | (A) 공용 재능 `COMMON`(성장축 아님) 신설 / (B) 스킬의 재능을 nullable로 / (C) 3재능 모두에 중복 정의 | **A 확정** (`TalentType`은 성장축이므로 건드리지 않고, 스킬용 `SkillTalent`에 `COMMON` 추가) | §7 |
| **D7** ✅ | 스킬 자원 소모 | 종류(재능파생) + 양(스킬별)·랭크 스케일 여부 | **확정** — MAGIC→MP·그 외→Stamina, 양은 JSON 명시·**랭크 무관 고정**(HEAVY 10/NORMAL 7/DEFENSE 4) | §10 |
| **D8** ✅ | 스킬 습득 방법 | 시작 보유 범위 + 이후 습득 수단 | **확정** — 신규 캐릭터는 **윈드밀 1개만 F랭크** 시드(디폴트 MELEE 재능). 나머지는 **스킬북(NPC 판매)**으로 습득하되 **추후 구현**(장비 5순위·NPC상점 8순위 의존) | §3, §11 |
| **D9** ✅ | 스킬 카탈로그 저장 방식 | (A) enum `Skill` / (B) JSON 카탈로그(`skill.json`) | **B 확정** — 스킬이 지속적으로 늘어날 예정이므로 `npc.json` 패턴으로 데이터 분리 | §2.4, §4, §11, §12 |

> **확정된 결정**: **D9 = JSON 카탈로그**. 스킬 목록·수치(라벨/타입참조/재능참조/배율/자원/소모량/효과요약)는 `classpath:data/skill.json`으로 관리하고 기동 시 로드·검증한다(`NpcService`/`npc.json` 선례). 단 **타입/랭크/재능/정책**(`SkillType`·`SkillRank`·`SkillTalent`·`SkillRankPolicy`)은 로직 결합·컴파일 안전성 때문에 **코드(enum/정책)로 유지**한다. JSON은 이들을 문자열로 참조하고 로드 시 검증한다.

> **확정된 결정**: **D1 = 전투 데미지 계약(§4)**. ①스킬 배율은 **랭크별 상승**. ②무기 정책은 **A방안(무기 자유)** — 아무 무기나 착용 가능, 데미지 스탯과 표시 스킬은 **장착 무기 재능**을 따르고, 데미지 +10%는 **무기 재능 == 캐릭터 환생 재능**일 때만. ③DEF는 **비율식**. ④크리는 **1.5배, 편차 적용 전**. ⑤최종은 **정수 반올림, 최소 1**. ⑥편차 **±10%가 마지막 단계**. 단 **실제 계산은 전투(7순위)에서 구현**하며 무기 의존이라 **장비(5순위) 선행**이 필요하다. 3순위 스킬 스펙이 제공하는 것은 **랭크별 배율 데이터 + 재능 매칭 정보**뿐이다.

> D1·D4·D6은 서로 얽혀 있다. "랭크업이 무엇을 강하게 만드는가"(데미지=D1 / 영구 스탯=D4)와 "방어 스킬을 어느 축에 넣는가"(D6)가 정해지면 나머지는 자동으로 따라온다.

---

## 1. 개요

스킬은 캐릭터가 전투에서 사용하는 행동이며, **타입(일반/강/방어)**, **랭크(F~Master)**, **소속 재능**을 가진다. 스킬을 쓰면 사용 횟수가 쌓이고, 그 스킬로 막타를 치면 처치 수가 쌓인다. 두 조건을 모두 충족하면 AP를 소모해 랭크업할 수 있고, 랭크업 시 스킬이 속한 재능에 따라 **영구 스탯**이 오른다.

핵심 설계 원칙(004 계승):

1. **계산형과 저장형을 구분한다.** 레벨·재능에서 계산되는 스탯(004)과 달리, 스킬의 랭크·사용 횟수·처치 수는 **계산으로 복원할 수 없으므로 영속 저장**한다 → 신규 엔티티가 필요하다.
2. **데이터는 성격에 따라 나눈다(D9 확정).** 로직에 결합된 **타입/랭크/재능/정책**(`SkillType`·`SkillRank`·`SkillTalent`·`SkillRankPolicy`)은 `NpcType`/`TalentType` 선례처럼 **코드(enum/정책)** 로 둔다. 반면 계속 늘어나는 **스킬 카탈로그**(스킬 목록·배율·자원 등 튜닝 수치)는 `npc.json` 선례처럼 **`data/skill.json`** 으로 분리하고 기동 시 로드·검증한다. JSON은 타입/재능을 문자열로 참조하며 로드 시점에 검증한다.
3. **004가 뚫어 둔 훅을 채운다.** AP 소모(`spendAbilityPoints`), 정보 팝업의 `Skill_Rankup_Bonus` 표시 자리(현재 `Stats.ZERO`), 재능 일치 데미지 +10%(`damageBonusPercent()`)를 실제로 연결한다.
4. **전투 적용은 7순위로 이연한다.** 데미지 배율·자원 소모의 실제 판정은 전투 시스템에서 소비한다. 본 스킬 스펙은 **스킬 데이터·랭크 진행·AP 소모·영구 스탯 보너스**까지를 책임진다.

---

## 2. 기존 소스 연계 지점 (실제 소스 분석)

스킬 시스템은 기존 코드에 이미 준비된 자리에 맞물린다. 새로 만들기 전에 **재사용/확장**할 지점을 정리한다.

### 2.1 AP 소모 훅 — 이미 존재 (`CharacterProgress`)

004가 소모 mutator를 정의만 하고 트리거를 이연해 두었다.

```java
// domain/model/CharacterProgress.java (기존)
public void increaseAbilityPoints(final int amount) { ... }   // 레벨업/환생 지급 (구현됨)
public void spendAbilityPoints(final int amount) {            // 스킬 랭크업 소모 (이 스펙의 트리거)
    if (amount > this.abilityPoints) {
        throw new IllegalArgumentException("AP 부족: ...");    // 임시 가드
    }
    this.abilityPoints -= amount;
}
public int getAbilityPoints() { ... }
```

- **스킬 스펙이 할 일**: `spendAbilityPoints`를 실제로 호출하는 랭크업 로직을 만들고, 004가 미룬 **정식 비즈니스 예외**(예: `InsufficientAbilityPointsException`)를 도입한다. 현재 `IllegalArgumentException` 임시 가드를 대체할지, 서비스 단에서 사전 검증할지 결정한다.
- **AP_Invariant 확장**: 004 요구사항 3.4가 이미 예고했다 — 소모 도입 후 불변식은 `abilityPoints == (accumulatedLevel - 1) - 누적 소모량`. 스킬 스펙의 프로퍼티 테스트는 이 확장식을 검증해야 한다.

### 2.2 `Skill_Rankup_Bonus` 표시 자리 — 이미 존재 (현재 `Stats.ZERO`)

정보 팝업 중앙 스탯은 `본체값 (+스킬보너스)` 형태로 이미 렌더링된다. 004는 스킬 보너스를 `Stats.ZERO`로 채워 두었다.

```java
// interfaces/api/PlayScreenViewHelper.java (기존 buildInfo)
final Stats levelStats = statProgression.levelStatsFor(level, talent);
final Stats skillBonus  = Stats.ZERO;                    // ← 스킬 시스템이 채울 자리
final List<StatLine> stats = buildStatLines(levelStats, skillBonus);
```

```html
<!-- templates/fragments/info-popup.html (기존) -->
<span class="stat-value" th:text="${stat.value}">10</span>
<span class="stat-bonus" th:text="'(' + ${stat.bonus} + ')'"> (+0)</span>
```

- **스킬 스펙이 할 일**: 모든 보유 스킬의 랭크업 누적 보너스를 합산한 `Stats`(및 필요 시 `VitalMax`)를 계산해 `skillBonus` 자리에 넣는다. 표시 구조(`StatLine.bonus`, `formatCriticalDelta`)는 그대로 재사용 가능 — **UI 변경 없이 값만 채우면 된다.**

### 2.3 재능 일치 데미지 보너스 — 이미 정의 (`TalentType.damageBonusPercent()`)

```java
// domain/model/TalentType.java (기존)
public int damageBonusPercent() { return damageBonusPercent; }  // 세 재능 모두 10 (+10%)
```

- 004 설계: "재능과 일치하는 공격 타입에 +10%". 스킬은 **소속 재능**을 가지므로, "스킬의 재능 == 캐릭터의 재능"이면 이 +10%가 적용된다. 실제 데미지 반영은 전투(7순위)지만, **스킬↔재능 매칭 정보는 이 스펙이 제공**한다(§7).

### 2.4 데이터 저장 방식 — 하이브리드 (enum 코드 + JSON 카탈로그, D9 확정)

이 레포는 **카테고리/로직 결합 데이터는 enum, 인스턴스/콘텐츠 데이터는 JSON** 으로 나누는 패턴이 이미 확립돼 있다.

- **JSON**: `data/npc.json`·`data/map.json`·`data/ambience.json` — 배치/대사/노드 등 개수 많고 늘어나는 데이터. `NpcService`가 기동 시 1회 파싱→검증→불변 목록으로 보관하고, 무결성 위반 시 `NpcDataException`으로 **기동을 실패**시킨다. JSON은 `"type": "chief"`처럼 enum을 **문자열로 참조**하고 로드 시 `NpcType.fromType(...).orElseThrow(...)`로 검증한다.
- **enum(코드)**: `NpcType`·`TalentType` — 라벨·행동·계산에 결합된 고정 카테고리. 완비성/폴백을 프로퍼티 테스트로 검증(`NpcTypeCompletenessPropertyTest`, `TalentTypeCompletenessPropertyTest`).

스킬은 이 분리를 그대로 따른다.

| 대상 | 저장 | 이유 |
|---|---|---|
| `SkillType`(일반/강/방어), `SkillTalent`(+COMMON), `SkillRank`(F~Master) | **enum(코드)** | switch·+10% 매칭·랭크 순서/`next()`·정책 인덱싱에 직접 결합. 컴파일 안전성 필요 |
| `SkillRankPolicy`(요구치·AP 소모) | **코드(순수 정책)** | 랭크 키 기반 순수 함수, 프로퍼티 테스트 용이(`ExperiencePolicy` 선례) |
| **스킬 카탈로그**(스킬별 id·라벨·타입참조·재능참조·배율·자원·소모량·효과요약) | **`data/skill.json`** | 개수 증가·밸런스 튜닝 잦음. `npc.json`과 성격 동일 → 재컴파일 없이 추가/조정 |

- **로더**: `NpcService`를 그대로 본떠 `SkillCatalogService`(기동 시 `skill.json` 1회 로드·검증)를 둔다. 파싱은 스트림 주입 방식(`loadFromStream`)으로 분리해 프로퍼티 테스트가 인메모리 데이터를 주입할 수 있게 한다.
- **참조 검증**: JSON의 `type`/`talent` 문자열은 로드 시 `SkillType`/`SkillTalent`로 해석하고, 실패하면 `SkillDataException`으로 기동 실패(`NpcType.fromType` 선례). id 중복도 검증.

### 2.5 값 타입 재사용 — `BonusTarget`/`TalentBonus`/`Stats`/`VitalMax`

004가 만든 성장 값 타입을 그대로 쓴다.

- `BonusTarget`(STR/DEX/INT/CRITICAL) + `BonusKind` — 랭크업 영구 보너스 대상 표현(D4-A는 스탯 계열만 사용).
- `Stats.withStrDelta`/`withDexDelta`/`withIntDelta`/`withCriticalDelta` — 스킬 보너스 합산에 재사용.
- `VitalMax`/HP·MP 계열 `BonusTarget`는 **스킬에선 미사용**(D4-A가 바이탈을 제외, §8). `vitalMaxFor(...)`도 무변경.

### 2.6 신규 영속 엔티티 필요 — `CharacterProgress`는 확장 지점만 제공

`CharacterProgress`는 스탯을 저장하지 않는 "계산형" 엔티티이고, 주석에 이미 확장 의도가 있다.

```java
// domain/model/CharacterProgress.java (기존 클래스 주석)
// "안정적인 기본 키(id)를 통해 향후 인벤토리, 장착 장비, 스킬 목록 등 별도 연관 엔티티를 확장할 수 있다."
```

- 스킬 진행(랭크/사용횟수/처치수)은 **저장형**이므로 `CharacterProgress`에 필드를 늘리지 말고, `character_id`로 연관되는 **신규 엔티티**(예: `CharacterSkill`)를 둔다(§11).

### 2.7 정책 클래스 패턴 — `ExperiencePolicy` 선례

레벨 경험치 곡선처럼 순수 정책 클래스로 임계값을 계산하는 선례가 있다.

```java
// domain/model/ExperiencePolicy.java (기존)
public long requiredForNext(final int level) { return 100L * level * level; }
```

- 랭크업 조건 임계값(막타 수·사용 횟수)과 AP 소모량은 **`SkillRankPolicy`** 같은 순수 정책으로 두면 밸런스 노브가 한 곳에 모이고 프로퍼티 테스트가 쉬워진다(§6, §9).

---

## 3. 스킬 타입 (일반 / 강 / 방어)

| 타입 | enum | 성격 | 같은 타입 간 차이 |
|---|---|---|---|
| 일반공격 | `NORMAL` | 기본 공격. 낮은 자원, 낮은 배율 | 배율·자원소모만 다름 (예: 후려치기 100% vs 밀어치기 50%) |
| 강공격 | `HEAVY` | 고배율·고자원 | 위와 동일하게 배율·자원소모로 차등 |
| 방어 | `DEFENSE` | 피해 경감/반격 등. 재능 무관 공용(§7) | 방어 성능·자원소모로 차등 |

**"같은 타입 스킬의 차이 = 데미지 배율 + 자원 소모량"** 이라는 사용자 정의를 그대로 반영한다. 즉 타입은 "카테고리"이고, 실제 수치 차이는 개별 스킬(카탈로그 항목)이 보유한다.

### 최종 MVP 스킬 목록 (확정)

패시브·지원기·광역/다중타격은 MVP에서 **제외**한다(실제 마비노기에서 이름만 차용, 광역기는 단일 대상으로 취급). 초안은 `docs/skills.json`(작업용, 런타임 파일은 `resources/data/skill.json`).

| id | 라벨 | 타입 | 재능 | 비고 |
|---|---|---|---|---|
| `smash` | 스매시 | HEAVY | MELEE | 강한 단일 일격 |
| `windmill` | 윈드밀 | NORMAL | MELEE | 회전 베기(단일 취급) |
| `magnum_shot` | 매그넘 샷 | HEAVY | ARCHERY | 강한 단발 사격 |
| `arrow_revolver` | 애로우 리볼버 | NORMAL | ARCHERY | 연속 사격(단일 취급) |
| `firebolt` | 파이어볼트 | HEAVY | MAGIC | 화염 마법 |
| `icebolt` | 아이스볼트 | NORMAL | MAGIC | 얼음 마법 |
| `defense` | 디펜스 | DEFENSE | COMMON | 피해 경감(공용) |

> **확정**: `DEFENSE`(디펜스)는 데미지를 산출하지 않으므로 데미지 배율 필드를 쓰지 않고 **전용 랭크별 맵 2개**를 가진다 — `blockRateByRank`(방어 성공 시 피해 경감률%), `counterMultiplierByRank`(반격 배율%). 로더는 타입(`DEFENSE` vs 딜스킬)에 따라 파싱 필드 셋을 분기한다. 반격/경감의 실제 판정은 전투(7순위, §4 가위바위보 계약).

> **D8 확정**: 신규 캐릭터는 **윈드밀 1개만 랭크 F로 보유**한 채 시작한다(디폴트 재능이 MELEE이므로 근접 일반공격 하나). 카탈로그의 나머지 6개는 정의만 되어 있고, **스킬북(NPC 판매)으로 습득**한다 — 단 스킬북은 **추후 구현**(아이템 5순위 + NPC상점 8순위 의존). 스킬 시스템은 "스킬 습득" 진입점(`learnSkill(skillId)` → `CharacterSkill` F랭크 추가)만 정의해 두고, 실제 판매/구매는 이후 스펙이 호출한다.

---

## 4. 스킬 데이터와 데미지 (D1 ★, 확정)

### 전투 데미지 계약 (전체 공식 — 전투 7순위에서 구현)

데미지 최종 산출 순서는 아래로 확정한다. **이 공식 전체는 무기(5순위)·몬스터 DEF(6순위)·크리/편차/로그(7순위)에 의존하므로 실제 구현은 전투 시스템(7순위)이 담당**한다. 3순위 스킬 스펙은 이 중 **스킬 배율 항과 재능 매칭 정보만** 제공한다.

```
1) 기본 공격력  = 무기 공격력 + f(주스탯)          // 주스탯 = 장착 무기 재능의 STR/DEX/INT (A방안)
2) 스킬 적용    = 기본 공격력 × 스킬 배율(랭크별, %)  // ← 3순위 스킬 스펙 제공
3) 재능 보너스  = 스킬 적용 × (1 + (무기재능==환생재능 ? 0.10 : 0))  // TalentType.damageBonusPercent()
4) 방어 적용    = 재능 보너스 × 몬스터_DEF_계수       // 비율식: 계수 = DEF/(DEF + K)를 뺀 잔여 비율
5) 크리티컬     = 크리 발동 시 × 1.5                  // 발동 확률 = 공격자 Critical(0.1%단위)/1000
6) 편차 적용    = × (0.9 ~ 1.1 균등난수) → 반올림 → max(1, 결과)   // 항상 마지막, 정수, 최소 1
```

확정 사항:

- **무기 정책 = A방안(무기 자유)**: 어떤 무기든 착용 가능. 데미지 주스탯과 전투 중 표시 스킬은 **장착 무기 재능**을 따른다(근접→STR, 활→DEX, 마법→INT). 이 매핑은 이미 `TalentType.primary().target()`과 동일하므로 재사용한다.
- **+10% 규칙**: **무기 재능 == 캐릭터 환생 재능**일 때만 적용. `COMMON`(방어) 스킬은 항상 +0%(§7).
- **DEF = 비율식**: 감산이 아닌 비율 감쇠(`DEF/(DEF+K)`). 고DEF에서 0/음수 데미지를 피한다. 계수 `K`는 밸런스 노브(전투 스펙에서 확정).
- **크리티컬**: 방어 적용 후 ×1.5, **편차 전**. 발동 판정은 기존 `Stats.critical`(0.1% 단위 정수) 확률 사용.
- **편차 ±10%**: 항상 마지막 단계. 예) 계산값 100 → 90~110 균등 난수. **정수 반올림, 최소 데미지 1.**
- **로그**: 일반/크리 데미지 모두 `ActionLog`에 기록하고, 크리 발동 시 별도 멘트. (7순위 소관, `ActionLog`/`ActionLogEntry` 재사용.)

### 가위바위보 삼각관계와 디펜스 반격 (전투 7순위 계약)

로드맵 7순위 전투가 **가위바위보 + 선후공**이므로, 스킬 **타입 3종이 곧 삼각형**이다.

```
일반(NORMAL)  →  강(HEAVY)      // 빠른 선공으로 강공격을 끊음
강(HEAVY)     →  방어(DEFENSE)  // 방어 관통
방어(DEFENSE) →  일반(NORMAL)   // 막고 반격
```

**DEF는 상시 경감(모든 피격에 적용), 디펜스 스킬은 조건부 추가 경감 + 반격**이다. 두 레이어를 분리한다.

```
받는 피해:
  [평상시/딜 교환]      상대 공격(§4) × DEF_비율경감                       // DEF는 방어자 누구에게나 상시 적용
  [디펜스로 일반 막음]  상대 공격(§4) × DEF_비율경감 × (1 - blockRate)      // 스킬 경감 추가 (부분경감, 랭크별)
  [디펜스로 강 맞음]    상대 공격(§4) × DEF_비율경감                        // 관통, blockRate 무효

반격 피해(일반 막았을 때만, 내 스탯 기반):
  반격 = 내 평타 데미지(§4 공식, 배율 100% 기준) × counterMultiplier(랭크별)
       // 크리/편차 §4 그대로 적용, 로그에 "○○의 반격!"
```

- **방어경감 = 랭크별 부분경감**(완전방어 아님). `blockRate` 예: F 50% → Master 85%.
- **반격 = 내 스탯 기반**(반사 아님). `counterMultiplier` 예: F 30% → Master 100%. 내 무기·주스탯으로 §4 공식을 태워 계산.
- 디펜스 랭크업 = `blockRate`·`counterMultiplier` 상승(스킬 자체 강화) + §8 영구 DEF 상승의 이중 보상.
- **DEF 성장 경로**: 레벨 커브(`5 + 1×(Lv-1)`) + 공용 스킬 랭크업(§8, DEF +1/랭크)뿐 — 세 재능(MELEE/ARCHERY/MAGIC)은 DEF를 키우지 않는다. 따라서 DEF는 보편 생존 스탯이되 성장은 방어 투자에 묶인다.
- 실제 삼각관계 승패·반격 판정은 **전투(7순위)** 가 소유. 3순위 스킬은 타입 태깅 + `blockRate*`/`counterMultiplier*` 데이터만 제공.

### ⚠️ 결정성/테스트 주의 (편차·크리의 난수)

이 레포는 프로퍼티 테스트가 핵심이라 난수가 결정성을 깬다. 환생 쿨다운이 `Clock`을 주입해 테스트한 선례(`RebirthCooldownRevalidatePropertyTest`)처럼, **난수원(`RandomGenerator`)을 주입**해야 한다. 프로퍼티는 `최종 ∈ [max(1, floor(0.9d)), ceil(1.1d)]`를 검증하고, 고정 시드로 정확값도 검증한다.

### ⚠️ 선행 의존 (스코프 경고)

무기(5순위)가 없으면 1)의 무기 공격력·주스탯 결정과 "무기 재능에 따른 표시 스킬 필터링"을 구현할 수 없다. 따라서 **3순위 스킬 스펙은 이 공식을 구현하지 않고**, 랭크별 배율 데이터와 재능 매칭만 제공한다. 전투 중 스킬 목록을 무기 재능으로 거르는 것도 5·7순위로 이연한다(§13).

### 데이터 형태(제안, D9=JSON 카탈로그)

스킬 카탈로그는 `classpath:data/skill.json`에 두고, 기동 시 로드해 불변 `Skill`(도메인 record) 목록으로 보관한다. `type`/`talent`은 문자열로 참조하고 로드 시 enum으로 검증한다.

스킬 배율은 **랭크별 상승(D1=B 확정)**이며, 값은 **랭크별로 명시(확정)**한다. F/Master만 두고 보간하는 대신 **F~Master 16개 값을 전부 JSON에 적는다** — 이유: (1) 스킬 팝업이 **현재 랭크의 배율·자원소모를 그대로 표시**(WYSIWYG, 보간 반올림 오차 없음), (2) 특정 랭크만 콕 집어 튜닝 가능. 랭크 키는 `SkillRank` enum 상수명(`F`/`E`/`D`/`C`/`B`/`A`/`R9`…`R1`/`MASTER`)을 써서 로더가 바로 매핑한다.

최종 7개 스킬 초안은 `docs/skills.json`에 있다(런타임은 `resources/data/skill.json`). 형식 예시:

```json
// 딜스킬: multiplierByRank (16개 랭크별 배율%), resourceCost는 단일(랭크 무관)
{ "id": "smash", "label": "스매시", "type": "HEAVY", "talent": "MELEE", "resourceCost": 10,
  "multiplierByRank": { "F": 130, "E": 138, "...": "...", "R1": 242, "MASTER": 250 },
  "effectSummary": "강력한 일격" }

// 방어스킬: 데미지 배율 대신 blockRateByRank(경감%) + counterMultiplierByRank(반격%)
{ "id": "defense", "label": "디펜스", "type": "DEFENSE", "talent": "COMMON", "resourceCost": 4,
  "blockRateByRank":         { "F": 50, "...": "...", "MASTER": 85 },
  "counterMultiplierByRank": { "F": 30, "...": "...", "MASTER": 100 },
  "effectSummary": "일반공격을 막고 반격, 강공격엔 관통당함" }
```

```java
// domain/model/Skill.java (JSON에서 로드되는 불변 도메인 record — enum 아님)
// 랭크별 값은 Map<SkillRank, Integer>로 로드
public record Skill(
        String id,                          // 카탈로그/저장 참조 키 (CharacterSkill.skillId FK)
        String label,
        SkillType type,                     // 로드 시 문자열 → enum 검증
        SkillTalent talent,                 // 로드 시 문자열 → enum 검증
        int resourceCost,                   // 랭크 무관 고정 (D7)
        Map<SkillRank, Integer> multiplierByRank,  // 딜스킬: 랭크별 데미지 배율%
        String effectSummary
) {}
// DEFENSE는 multiplierByRank 대신 blockRateByRank·counterMultiplierByRank를 가지는 변형
```

- **랭크별 값 조회**: `SkillDamagePolicy.multiplier(Skill, SkillRank)`는 보간이 아니라 **맵 조회**(`multiplierByRank.get(rank)`)로 반환한다. 팝업 표시도 동일하게 현재 랭크 값을 조회한다.
- **팝업 표시**: 스킬 팝업은 현재 랭크의 **자원소모량(`resourceCost`, 고정)**과 **보너스 데미지(`multiplierByRank[현재랭크]`)**를 보여준다(디펜스는 경감%·반격% 표시).
- **타입별 필드 차이(딜 vs 방어)**: 딜스킬(NORMAL/HEAVY)은 `multiplierByRank`를, `DEFENSE`는 `blockRateByRank`·`counterMultiplierByRank`를 가진다. 실제 모델은 **타입별 변형**(sealed 인터페이스 `Skill` → `DamageSkill`/`DefenseSkill`, 또는 공통 필드 + 옵셔널 맵)으로 두는 것을 권장한다(005에서 확정). 로더가 `type`으로 분기 파싱하고, 각 맵이 16개 랭크를 모두 갖는지·단조 증가인지 검증한다.
- **단일 소스 원칙**: 스킬 "정체성"의 집은 `skill.json`의 `id`다. `CharacterSkill`은 이 `id`를 문자열로 참조하고(§11), 로드 시 카탈로그에 존재하는지 검증한다.

---

## 5. 랭크 시스템 (D2)

사용자 표기: **"F - A - 9 - 1 - Master"** → 마비노기 정통 사다리로 해석.

### 추천 사다리 (16단계)

```
F → E → D → C → B → A → 9 → 8 → 7 → 6 → 5 → 4 → 3 → 2 → 1 → Master
```

### 데이터 형태(제안)

```java
// domain/model/SkillRank.java
public enum SkillRank {
    F, E, D, C, B, A, R9, R8, R7, R6, R5, R4, R3, R2, R1, MASTER;
    // 주의: 자바 식별자는 숫자로 시작 불가 → 9~1은 R9~R1로 두고 label()에서 "9".."1" 반환

    public String label();          // "F","E",...,"9",...,"1","Master"
    public boolean isMax();          // MASTER 여부
    public Optional<SkillRank> next(); // 다음 랭크 (MASTER면 empty)
    public int order();              // 0(F) ~ 15(MASTER), 정책 테이블 인덱스
}
```

- 신규 스킬은 **F**로 시작, **Master**가 최대(더 이상 랭크업 불가 → AP 소모/조건 검사 안 함).

---

## 6. 랭크업 조건 (D3)

사용자 정의: **두 조건을 모두 충족**해야 랭크업 가능.

1. 해당 스킬로 몬스터 **막타 처치** 수 ≥ 임계값
2. 해당 스킬 **사용 횟수** ≥ 임계값

### 카운팅 규칙(제안)

- **사용 횟수**: 스킬을 실제 사용(전투에서 발동)할 때마다 +1. (전투 7순위가 이벤트를 발생시키고, 스킬 스펙이 카운터를 올린다.)
- **막타 처치 수**: 그 스킬 사용으로 몬스터 HP를 0으로 만든 경우 +1. (몬스터 6순위/전투 7순위 연계 훅.)
- **랭크업 시점**: 자동이 아니라 **플레이어가 "랭크업" 버튼을 눌러** 소모/상승을 확정(왼쪽 스킬 팝업, 10순위 UI와 연계). 조건 미충족이거나 AP 부족이면 버튼 비활성/거부.

### 임계값 정책 테이블(제안, `SkillRankPolicy`)

저랭크(F~A)는 완만해 초반 성장감을 주고, **고랭크(9~Master)는 지수적으로 가팔라져** 마스터를 장기 목표로 만든다. **막타 처치가 실질 관문**(사용 횟수는 처치보다 빨리 쌓이는 보조 조건)이며, 특히 **1→Master는 큰 벽**이다. 아래는 **현재→다음** 랭크 요구치(밸런스 노브, 조정 가능).

| 현재 랭크 | 필요 사용 횟수 | 필요 막타 처치 |
|---|---|---|
| F→E | 5 | 1 |
| E→D | 10 | 3 |
| D→C | 20 | 6 |
| C→B | 35 | 10 |
| B→A | 60 | 18 |
| A→9 | 100 | 30 |
| 9→8 | 160 | 48 |
| 8→7 | 240 | 72 |
| 7→6 | 350 | 105 |
| 6→5 | 520 | 155 |
| 5→4 | 760 | 230 |
| 4→3 | 1,100 | 340 |
| 3→2 | 1,600 | 500 |
| 2→1 | 2,500 | 750 |
| 1→Master | 5,000 | 1,500 |

- **누적 F→Master**: 사용 약 **12,500회** / 막타 약 **3,800회** (원안 3,765 / 1,177 과 이전 강화안 19,500 / 5,900 의 중간).
- **1→Master 단독**: 사용 5,000 / 막타 1,500 — 여전히 마지막 벽이되 이전보다 완화.
- 대략 랭크마다 요구치가 1.4~1.5배씩 증가. 더 완화하려면 고랭크(9~Master) 배수를, 빡세게 하려면 막타 열을 조정한다.

> 순수 정책 클래스로 두면(예: `SkillRankPolicy.requirement(SkillRank current)` → `(usage, kills)`), 프로퍼티 테스트로 "모든 랭크 전이에 대해 요구치가 양수이고 단조 증가" 같은 속성을 검증할 수 있다(`ExperiencePolicyCurvePropertyTest` 선례).

---

## 7. 재능 소속과 방어 스킬 (D6 ★)

사용자 질문: **"방어 타입은 어느 재능에 속해야 하지? 방어 스킬은 어떤 재능이든 다 쓸 수 있어야 하는데."**

### 문제

- 근접/활/마법 스킬은 각각 `MELEE`/`ARCHERY`/`MAGIC`에 속하고, 캐릭터 재능과 일치하면 §2.3의 +10% 데미지 보너스를 받는다.
- 방어 스킬은 **재능과 무관하게 공용**이어야 한다.
- 그런데 `TalentType`은 **004의 성장축**(레벨업 스탯 보너스·환생 선택지)이다. 여기에 `COMMON`을 넣으면 환생 선택지에 "공용"이 생기고 성장 계산이 오염된다 → **`TalentType`은 건드리면 안 된다.**

### 추천안 (D6-A): 스킬 전용 `SkillTalent` 도입

성장축 `TalentType`과 분리된, **스킬 분류 전용** enum을 둔다.

```java
// domain/model/SkillTalent.java
public enum SkillTalent {
    MELEE(TalentType.MELEE),      // 근접 재능 스킬
    ARCHERY(TalentType.ARCHERY),  // 활 재능 스킬
    MAGIC(TalentType.MAGIC),      // 마법 재능 스킬
    COMMON(null);                 // 공용(방어 등) — 어떤 재능이든 사용 가능

    // 매칭 재능(성장축) 반환, 공용이면 empty
    public Optional<TalentType> matchingTalent();
}
```

- **+10% 데미지 보너스 규칙**: `skill.talent().matchingTalent()`이 캐릭터의 환생 `TalentType`과 같으면 적용. `COMMON`은 매칭 재능이 없으므로 **항상 +0%**(모두 사용 가능하되 재능 보너스 없음).
- **방어 스킬**은 `SkillType.DEFENSE` + `SkillTalent.COMMON` 조합 → 3재능 모두 사용 가능.
- 대안: (B) `Skill`이 `TalentType`을 nullable로 보유(null=공용) — 간단하지만 null 취급이 코드에 번짐. (C) 방어 스킬을 3재능에 중복 정의 — 데이터 중복·랭크 진행 분산 문제. → **A 추천**.

### 두 개의 "재능"과 무기 (A방안 확정)

무기 정책은 **A방안(무기 자유)**으로 확정됐다(§4). 이때 재능 개념이 둘로 나뉘므로 명확히 구분한다.

- **환생 재능**(`TalentType`, 004): 성장 보너스 + 데미지 +10%의 주체. 한 생애 동안 불변.
- **무기 재능**(장착 무기 종류): 데미지 주스탯(STR/DEX/INT)과 **전투 중 표시되는 스킬 목록**을 결정.

동작 규칙:

- 전투 중 사용 가능한 스킬 = **장착 무기 재능의 스킬** + **`COMMON`(방어) 스킬**(무기 무관 항상 노출).
- 스킬 데미지 주스탯은 **무기 재능**을 따른다(근접→STR, 활→DEX, 마법→INT). 예: MELEE 재능 캐릭터가 활을 들면 DEX로 계산.
- +10%는 **무기 재능 == 환생 재능**일 때만. 위 예(MELEE 캐릭터 + 활)는 불일치라 +0%. 비주력 무기가 약한 마비노기식 트레이드오프.
- **의존성 경고**: "무기 재능으로 스킬 목록 필터링"과 "무기 재능 주스탯"은 **장비(5순위)** 가 있어야 구현된다. 3순위 스킬 스펙은 스킬별 `SkillTalent`만 저장/제공하고, 필터링·주스탯 적용은 전투(7순위)로 이연한다(§13).

---

## 8. 랭크업 시 영구 스탯 상승 (D4 ★)

사용자 정의: **"랭크업할 때마다 스킬이 속한 재능에 따라 스탯이 영구 상승."** 질문: **"어떤 스탯들을 올릴지."**

### 004가 만든 자리

§2.2에서 봤듯 정보 팝업은 이미 `본체(+스킬보너스)`를 표시하고, 지금은 `Stats.ZERO`다. 이 스펙이 **모든 보유 스킬의 랭크업 누적 보너스 합**을 계산해 그 자리에 넣는다.

### 확정 (D4-A + 대칭): 재능별 주 스탯 1개만 +1/랭크업

랭크업 1회당 **재능의 주 스탯 +1**만 영구 상승한다(스탯 계열만, 바이탈 제외). 각 재능 1스탯으로 **대칭**을 맞추며, 활의 Critical은 제외한다 — 활 Critical은 이후 활 무기·재능 레벨업으로 충분히 오르므로 스킬 랭크업까지 중복으로 줄 필요가 없다.

| 스킬 재능 | 랭크업 1회당 영구 상승 | 근거 |
|---|---|---|
| `MELEE` | STR +1 | `TalentType.MELEE.primary()` 대상과 동일 |
| `ARCHERY` | DEX +1 | `TalentType.ARCHERY.primary()` 대상과 동일 |
| `MAGIC` | INT +1 | `TalentType.MAGIC.primary()` 대상과 동일 |
| `COMMON`(방어) | DEF +1 | 재능이 DEF를 안 키우므로 방어 투자의 주 성장원(§4) |

- **매핑 위치**: `SkillTalent`별 `(BonusTarget, +1)`을 코드로 둔다. MELEE/ARCHERY/MAGIC는 `TalentType.primary().target()`을 재사용, COMMON은 DEF. 별도 JSON 아님(재능 레벨 데이터).
- **합산 계산**: `Skill_Rankup_Bonus(Stats) = Σ(보유 스킬, skill.rank.order() × 재능주스탯보너스(skill.talent))`. `rank.order()`는 F=0 … Master=15. 순수 함수 → 저장 불필요(랭크만 저장, 004 "계산형" 유지). `Stats.withStrDelta`/`withDexDelta`/`withIntDelta` 등 재사용.
- **표시**: 이 `Stats`를 `PlayScreenViewHelper.buildInfo`의 `skillBonus` 자리(현재 `Stats.ZERO`)에 주입. 정보 팝업 `본체(+X)` 구조 그대로 → **UI/게이지 변경 없음**.
- **예시**: 윈드밀(MELEE) A랭크(order 5) → STR +5. 전부 마스터 시(스킬당 ×15): MELEE 2개 STR+30 / ARCHERY 2개 DEX+30 / MAGIC 2개 INT+30 / 디펜스 DEF+15.
- **바이탈 미포함(A방안 귀결)**: HP/MP/Stamina 최대치는 스킬로 올리지 않는다 → `vitalMaxFor(...)` **확장 불필요**, 004 계산·표시 경로 무변경. HP/MP 성장은 재능 레벨업 보너스(004)에 맡긴다.

---

## 9. 랭크업 AP 소모 (D5)

- AP는 레벨업 시 +1, 환생 시 +1로 이미 쌓인다(004 구현). 스킬 랭크업이 유일한 **소모처**가 된다.
- **AP 경제**: 누적레벨 `L`에서 미소모 AP = `L - 1`. 만렙(100) 1회 = 99 AP, **누적 200 ≈ 199 AP**.

### 캘리브레이션 목표 (확정)

**한 스킬 F→Master 총 비용 ≈ 200 AP.** 근거: 환생 2회 + 풀레벨 ≈ 누적레벨 ~200 ≈ AP ~199이므로, 한 스킬에 AP를 집중한 플레이어가 그 시점에 스킬 하나를 마스터한다(사용자 목표). 랭크가 오를수록 비용이 커지는 **후반 급증형**(`SkillRankPolicy.apCost(SkillRank current)`).

| 랭크업 | 소모 AP | 누적 AP(F→) |
|---|---|---|
| F→E | 1 | 1 |
| E→D | 2 | 3 |
| D→C | 3 | 6 |
| C→B | 4 | 10 |
| B→A | 5 | 15 |
| A→9 | 7 | 22 |
| 9→8 | 9 | 31 |
| 8→7 | 11 | 42 |
| 7→6 | 13 | 55 |
| 6→5 | 15 | 70 |
| 5→4 | 18 | 88 |
| 4→3 | 22 | 110 |
| 3→2 | 26 | 136 |
| 2→1 | 30 | 166 |
| 1→Master | 34 | **200** |

- **F→Master 총 200 AP** → 누적레벨 201에서 달성(AP = 201-1 = 200). "환생 2번쯤에 한 스킬 마스터" 목표 충족.
- **함의(의도된 장기 목표)**: 7개 스킬을 전부 마스터하려면 ≈ 7×200 = **1,400 AP** ≈ 누적레벨 ~1,400(≈14생애). 스킬 숙련은 레벨과 독립된 초장기 목표가 된다. 너무 길면 총합을 낮추고(예: 스킬당 ~120), 더 길게 하려면 고랭크 비용을 올린다.
- **소모 흐름**: 조건 충족(§6) + `abilityPoints ≥ apCost` → `spendAbilityPoints(apCost)` → 랭크 +1 → 영구 보너스 재계산 → 저장. AP 부족 시 정식 예외(§2.1)로 거부.
- **불변식**: 소모 도입으로 `abilityPoints == (accumulatedLevel - 1) - 총 소모 AP`(004 Req 3.4). "총 소모 AP = Σ 각 스킬이 F에서 현재 랭크까지 오는 데 든 AP"로 검증 가능.

---

## 10. 스킬 자원 소모 (D7)

기존 바이탈 **HP/MP/Stamina** 3종에서 스킬이 소모할 자원을 정한다. **종류는 재능 파생(계산), 양은 스킬별 JSON 명시, 랭크 무관 고정**(확정).

### 자원 종류 (재능에서 파생, 저장 안 함)

| 스킬 재능(`SkillTalent`) | 소모 자원 |
|---|---|
| `MAGIC` | MP |
| `MELEE` / `ARCHERY` / `COMMON` | Stamina |

- `MAGIC`만 MP, 나머지는 전부 Stamina. `SkillTalent.resourceKind()` 접근자로 계산하며 `skill.json`엔 종류를 저장하지 않는다.

### 소모량 (스킬별 JSON, 랭크 무관 고정)

- 소모량은 스킬 고유값 `resourceCost`(정수)로 `skill.json`에 명시한다. **랭크가 올라도 변하지 않는다**(랭크는 데미지/방어·반격만 강화, D5·D1·§4). 랭크 효율(숙련 시 감소)이 필요하면 후일 `costAtF/Master`로 확장(현재 스코프 밖).
- **컨벤션**: 강공격(HEAVY) > 일반공격(NORMAL) > 디펜스(DEFENSE).

| 타입 | 소모량(확정 초안) | 예시 스킬 |
|---|---|---|
| HEAVY | 10 | 스매시·매그넘샷·파이어볼트 |
| NORMAL | 7 | 윈드밀·애로우리볼버·아이스볼트 |
| DEFENSE | 4 (일반공격보다 적게) | 디펜스 |

- **디펜스는 일반공격보다 적은 Stamina**(4)를 소모 — 방어를 자주 쓰되 완전 무료는 아님.
- **밸런스 종속성**: 이 값들은 **자원 회복량(턴/시간당 Stamina·MP 회복)이 정해져야** 적정성 판단 가능 → 회복 규칙은 전투(7순위) 소관이라 **잠정치**다. 참고: Stamina는 세 재능 모두 안 키워 공통 곡선(`100+10×(Lv-1)`)이고, MP는 `MAGIC`이 `+5/Lv`로 키워 마법사 자원 여유가 크다.
- **실제 차감**은 전투(7순위)에서 수행. 이 스펙은 "어떤 자원을 얼마"(종류 규칙 + `resourceCost` 데이터)만 제공.

---

## 11. 영속 모델 (신규 엔티티)

스킬 진행은 저장형이므로 신규 엔티티를 둔다. 싱글 플레이(캐릭터 1개)지만 `id` 연관으로 확장성을 확보한다(§2.6).

```java
// domain/model/CharacterSkill.java (신규 JPA 엔티티, 개념)
@Entity @Table(name = "character_skill")
public class CharacterSkill {
    @Id @GeneratedValue Long id;
    Long characterId;                 // CharacterProgress.id 연관
    String skillId;                   // skill.json의 id 참조(FK). 카탈로그가 JSON이므로 enum이 아닌 문자열
    @Enumerated(EnumType.STRING) SkillRank rank;   // 현재 랭크 (기본 F) — 랭크는 enum 유지
    int usageCount;                   // 현재 랭크에서의 사용 횟수 (랭크업 시 0 리셋)
    int killCount;                    // 현재 랭크에서의 막타 처치 수 (랭크업 시 0 리셋)
}
```

### 저장 값 관련 결정 포인트

- **스킬 참조는 문자열 id(D9 귀결)**: 카탈로그가 JSON이므로 `@Enumerated Skill` 대신 `String skillId`로 저장한다. 대가로 **dangling 위험**(저장된 id가 카탈로그에서 사라짐)이 생기므로, 로드 시 `SkillCatalogService.byId(skillId)`로 존재를 검증하고 미존재 시 정책(무시/로그/기동실패)을 정한다. 랭크(`SkillRank`)는 코드 enum이라 `@Enumerated(EnumType.STRING)` 안전성을 그대로 유지.
- **카운터 리셋(확정)**: 랭크업 시 `usageCount`/`killCount`를 **0으로 리셋**한다. §6 요구치 테이블은 "현재→다음" 증가분 해석이며, §15.2 승급 후 모달이 `0/새 요구치`로 재세팅되는 것과 일치.
- **영구 스탯 보너스는 저장 안 함**: 랭크만 저장하면 §8 보너스는 매번 계산 가능(004 원칙 유지). 별도 컬럼 불필요.
- **초기 데이터(D8 확정)**: 신규 캐릭터에게 **윈드밀(`windmill`) 1개만 랭크 F로 시드**한다(디폴트 MELEE). 나머지 6개는 스킬북(NPC, 추후)으로 습득하며, 습득 시 `CharacterSkill`을 F랭크로 추가하는 `learnSkill(skillId)` 진입점을 둔다(중복 습득 방지 검증 포함).
- **환생 시**: 보유 스킬 목록과 각 랭크·사용횟수·처치수를 **유지**(리셋 안 함). 스킬 숙련은 레벨과 독립적인 자산이며, 이미 배운 스킬은 환생해도 남는다.

### 마이그레이션

- 004 선례대로 로컬 H2(`ddl-auto: update`)는 신규 테이블 자동 생성, 프로덕션(`ddl-auto: create`)은 재생성. 기존 세이브 영향은 신규 테이블 추가뿐이라 004보다 가볍다.

---

## 12. 예상 계층/파일 구조 (005 스펙 밑그림)

004 산출물 스타일을 따른 예상 배치. **[신규]/[확장]** 구분.

```
myrpg/src/main/java/com/myapps/web/myrpg/
├── domain/model/
│   ├── Skill.java                 [신규] record: skill.json에서 로드되는 불변 스킬(카탈로그 항목)
│   ├── SkillType.java             [신규] enum: NORMAL/HEAVY/DEFENSE
│   ├── SkillTalent.java           [신규] enum: MELEE/ARCHERY/MAGIC/COMMON (§7)
│   ├── SkillRank.java             [신규] enum: F~Master 사다리(order/next/label)
│   ├── SkillRankPolicy.java       [신규] 순수 정책: 랭크별 요구치·AP 소모(§6,§9)
│   ├── SkillDamagePolicy.java     [신규] 순수 정책: 랭크→배율/방어/반격 맵 조회(§4)
│   ├── CharacterSkill.java        [신규] JPA 엔티티: skillId(FK)/랭크/사용횟수/처치수
│   ├── CharacterProgress.java     [확장] spendAbilityPoints 트리거 연결(예외 정식화)
│   └── StatProgression.java       [확장?] 스킬 보너스 합산 반영(스탯 계열) / vitalMaxFor 확장(HP·MP 대상 채택 시)
├── domain/repository/
│   └── CharacterSkillRepository.java  [신규]
├── application/
│   ├── service/SkillCatalogService.java [신규] skill.json 기동 로드·검증·조회(NpcService 선례, loadFromStream 분리)
│   ├── service/SkillService.java  [신규] 사용/막타 카운팅, 랭크업(조건+AP 소모), 보너스 합산
│   ├── exception/SkillDataException.java [신규] 카탈로그 로드/검증 실패(NpcDataException 선례)
│   ├── exception/InsufficientAbilityPointsException.java [신규] (§2.1)
│   └── dto/SkillListView·SkillRowView·SkillRankUpView.java [신규] 스킬 팝업 뷰 모델(§15.3)
├── interfaces/api/
│   └── PlayScreenViewHelper.java  [확장] skillBonus 자리에 합산 보너스 주입(Stats.ZERO 대체)
└── (interfaces/api) 스킬 팝업 컨트롤러 엔드포인트 [신규/확장]

myrpg/src/main/resources/
├── data/skill.json     [신규] 스킬 카탈로그(id/라벨/타입참조/재능참조/배율/자원/소모량/효과요약)
├── templates/fragments/skill-popup.html [신규] 스킬 목록·랭크·랭크업 버튼 (10순위 UI 연계)
├── static/js/myrpg.js  [확장] 랭크업 요청/스왑
└── static/css/myrpg.css [확장] 스킬 팝업 스타일
```

> 테스트는 004처럼 **jqwik 프로퍼티 + 예시/슬라이스/통합** 이중 접근. 후보 정확성 속성: 랭크 사다리 단조성, 요구치 양수·단조 증가, 랭크업 조건 게이트(둘 다 충족해야 상승), AP 소모 후 불변식 확장, 보너스 합산 = Σ(랭크×재능보너스), MASTER 상한, `SkillTalent` 완비성/폴백, 영속 라운드트립.
> 카탈로그 관련 추가 속성/테스트: `skill.json` 파싱·검증(`loadFromStream` 인메모리 주입), 미지 `type`/`talent`·id 중복·필수 필드 누락 시 `SkillDataException`, `CharacterSkill.skillId`가 카탈로그에 존재(dangling 방지) — `NpcServiceParsingPropertyTest`/`NpcServiceLoadFailurePropertyTest` 선례.

---

## 13. 후속 스펙 연계 (경계 정리)

이 스펙(스킬)이 **책임지는 것**:

- 스킬 카탈로그(`data/skill.json`) 정의·로드·검증 + 타입/랭크/재능/정책 enum
- 랭크 사다리·랭크업 조건·AP 소모·영구 스탯 보너스
- 스킬 진행 영속화, 정보 팝업 스킬 보너스 표시

**이연/의존하는 것**:

- **장비(5순위)**: 무기 공격력·무기 재능. A방안(무기 자유)의 "무기 재능 주스탯"과 "전투 중 무기 재능으로 스킬 목록 필터링"은 장비 시스템이 있어야 구현된다. 3순위는 스킬별 `SkillTalent`만 저장/제공.
- **전투(7순위)**: §4 데미지 계약 전체 구현(무기 공격력·주스탯·비율식 DEF·크리 1.5배·±10% 편차·정수 반올림/최소1), 자원 차감, +10% 적용, 무기 재능 기반 스킬 필터링, 스킬 사용/막타 **이벤트 발생**. 스킬 스펙은 카운팅 훅(`onSkillUsed`, `onSkillKill`)과 랭크별 배율(`SkillDamagePolicy`)만 정의하고, 전투가 이를 호출·조합한다. 난수원(`RandomGenerator`) 주입 필요(§4).
- **몬스터(6순위)**: 막타 판정 대상 + DEF 제공(비율식 입력).
- **왼쪽 팝업 UI(10순위)**: 스킬 목록/랭크업 버튼의 최종 레이아웃. 스킬 스펙은 뷰 모델·엔드포인트까지, 레이아웃 통합은 10순위.
- **스킬 습득(스킬북, 추후)**: 윈드밀 외 스킬은 NPC가 파는 스킬북으로 배운다 → 아이템/인벤토리(5순위) + NPC 상점 행동(8순위)에 의존. 스킬 시스템은 `learnSkill(skillId)` 진입점만 제공하고, 판매·구매 흐름은 이후 스펙이 호출한다.

---

## 14. 3순위 구현 범위 · 임시 드라이버 · 지식 보존

> ⚠️ **이 문서(`skill-system.md`)는 개발 완료 후 삭제된다**(로드맵 규칙). 이 문서는 005 스펙을 쓰기 위한 밑거름일 뿐이므로, 여기 담긴 결정·근거·계약·임시장치는 **삭제돼도 남는 곳**(스펙 문서 + 코드 주석)으로 반드시 이관해야 한다. 아래는 005 스펙 작성·구현 시 지켜야 할 지침이다.

### 14.1 3순위가 실제로 구현하는 것 (완결 수직 슬라이스)

스킬 시스템은 하위 계층이라 **후순위(무기5·몬스터6·전투7·상점8)가 없어도 자체 완결·테스트 가능**하다. 3순위에서 실제로 동작해야 하는 범위:

- 카탈로그 로드/검증(`SkillCatalogService` + `skill.json`)
- 랭크 진행 트랜잭션: 조건 충족(§6) + AP 소모(§9) → 랭크업 → 영구 스탯 재계산(§8)
- `CharacterSkill` 영속(랭크·사용횟수·처치수), 신규 캐릭터 윈드밀 시드
- 영구 스탯 보너스를 정보 팝업 `skillBonus` 자리에 표시(§2.2)
- 스킬 팝업(목록·랭크·자원소모·배율·랭크업 버튼)

### 14.2 임시 드라이버 (004 선례)

전투(7순위)가 없으면 사용/막타 카운트가 안 쌓여 랭크업을 굴릴 수 없다. 004가 경험치 소스 없이 임시 **[경험치 업/다운]** 버튼으로 레벨업을 검증한 것처럼:

- **승급 팝업(§15.2) 안에 임시 `[사용횟수 업]`·`[막타 처치 업]` 버튼**을 둔다. 누르면 해당 카운트를 **다음 랭크 요구치까지 즉시 100% 충전** → 둘 다 채우면 `[승급]` 활성 → 랭크업 루프를 end-to-end로 시연·테스트한다.
- 이 버튼은 **전투(7순위)가 실제 `onSkillUsed`/`onSkillKill` 이벤트로 교체하며 제거**한다 → 005 tasks에 "임시 드라이버 추가" + "전투 스펙에서 제거"를 명시.

### 14.3 지식 보존 — 스펙 문서로 이관 (영구)

`.kiro/specs/myrpg/005-skill-system/`에 반드시 남긴다(삭제 안 됨):

- **Design Decisions**: 이 문서 §0의 D1~D9 + 근거 요약(특히 D1 전투 계약, D4 대칭, D5 200AP 역산, D9 하이브리드).
- **Correctness Properties**: 랭크 사다리 단조성, 요구치 양수·단조 증가, 랭크업 게이트(둘 다 충족), AP 불변식 확장(`ap == accLv-1-소모합`), 보너스 합산식, MASTER 상한, 카탈로그 검증/폴백, 영속 라운드트립.
- **이연 경계(§13)**: 어느 항목이 5·6·7·8·10순위 소관인지.

### 14.4 지식 보존 — 코드 JavaDoc으로 이관 (필수)

문서가 사라져도 "왜 이렇게 짰는지 / 누가 나중에 채우는지"가 코드에 남도록, 아래는 **상세 JavaDoc 필수**:

- **크로스 시스템 훅**: `onSkillUsed(skillId)`·`onSkillKill(skillId)`(전투7이 호출), `learnSkill(skillId)`(상점5·8이 호출), `SkillDamagePolicy.multiplier(skill, rank)`(전투7이 읽음) — 각 메서드에 **호출 주체·시점·"여기선 값/카운트만, 실제 적용은 N순위"**를 명시.
- **이연 seam**: 데미지 배율·자원소모 제공부에 `/** 배율/소모 데이터만 제공. 실제 데미지·자원 차감은 전투(7순위)가 수행. */` 식으로 포인터.
- **임시 드라이버**: `/** 임시: 3순위 검증용. 전투(7순위) 이벤트 연결 시 제거. */`.

> **code-style와의 조화**: `code-style.md`는 방치된 `TODO/FIXME`를 금지하지만, **크로스 스펙 이연·임시장치는 서술형 JavaDoc(제거 조건 포함)으로 남기는 것이 정당**하다. 나열식 `// TODO`가 아니라 "누가·언제·왜"를 설명하는 JavaDoc으로 쓴다.

---

## 15. 스킬 팝업 UI 레이아웃

> 참고 이미지(마비노기 스킬창)에서 착안하되, 우리 데이터(§4 배율/자원, §6 요구치, §9 AP)에 맞춰 재구성한다.
> **경계**: 3순위 스킬 스펙은 팝업 fragment + 뷰 모델 + 엔드포인트까지 만들어 **기능적으로 동작·테스트**되게 한다. 왼쪽 패널로의 **최종 레이아웃 통합은 10순위(ui-left-popup)**.

### 15.1 스킬 목록 팝업 (메인)

턴제 전투이므로 아이콘·사용 버튼 없이 **간결 구성**(스킬명·랭크·진행바·승급).

```
┌ 스킬 ──────────────────────────────────── [–][X] ┐
│ [전체] [근접전투] [활] [마법] [공용]              │  ← 탭 (확정)
├───────────────────────────────────────────────────┤
│ 윈드밀            랭크 F  ▓▓▓▓▓░░░░░  [승급]      │  ← 진행바 + 강조색 = 승급 가능
│ 스매시            랭크 F  ▓░░░░░░░░░  [승급]      │
│ ...                                                │
└───────────────────────────────────────────────────┘
```

**행(row) 구성**: `스킬명` · `랭크 X` · `진행바` · `[승급]`. (아이콘·`사용하기` 없음 — 턴제라 목록에서 "사용" 개념 불필요.)

- **진행바**(동일가중 평균, 확정): 다음 랭크까지 진행도 = `(min(사용/요구, 1) + min(막타/요구, 1)) / 2 × 100`. 두 조건을 각자 100%에서 캡한 뒤 **동일 가중으로 평균**한다 → 각 조건이 바의 절반씩 차지하고, **둘 다 채워야만 100%**(= 훈련 완료 = 승급 가능). 사용만 채우면 50%에서 멈춰 관문(막타)이 남았음을 정직하게 보여준다. AP는 바에 포함하지 않는다(별개 자원, 승급 버튼 색/모달에서 확인). 상세 수치(80/80, 10/22)는 승급 모달에서. MASTER면 바 대신 "MAX".
- **`[승급]` 버튼 색상**(사용자 요구): **승급 가능이면 강조색(초록)**, 아니면 회색/비활성. 승급 가능 = `사용 ≥ 요구 && 막타 ≥ 요구 && 보유 AP ≥ 필요 AP && 랭크 ≠ MASTER`. 즉 바가 꽉 차도 AP가 부족하면 회색. 누르면 승급 팝업(15.2)이 뜬다(진행도 확인 겸).

**탭**(확정): `전체`(기본, 보유 전부) · `근접전투` · `활` · `마법` · `공용`(디펜스). MVP는 윈드밀만 보유 → `전체`/`근접전투`에 1개, 나머지 탭은 "습득한 스킬 없음" 빈 상태.

### 15.2 스킬 승급 팝업 (작은 모달)

행의 `[승급]`을 누르면 뜨는 작은 팝업. 아이콘 없이 텍스트 중심으로 **현재 랭크 정보 + 다음 랭크 미리보기 + 진행도 + AP**를 보여준다.

```
┌ 스킬 승급 ─────────────── [–][X] ┐
│            매그넘 샷              │
저│      A랭크로 승급할 수 있습니다   │   ← 다음 랭크 (SkillRank.next().label()), 현재 B
│───────────────────────────────────│
│ 보너스 데미지  172% → 180%        │   ← 현재(B)→다음(A) 배율(multiplierByRank)
│ 소모 자원      스태미나 10        │   ← resourceCost(고정) + 재능 파생 종류
│───────────────────────────────────│
│ 사용 횟수      60 / 60            │   ← current / required (§6: B→A)
│ 막타 처치      18 / 18            │
│───────────────────────────────────│
│ 필요 AP  5   (보유 AP  16)        │   ← apCost(§9: B→A) / CharacterProgress.getAbilityPoints()
│  [사용횟수 업]     [막타 처치 업]  │   ← 임시 드라이버: 누르면 요구치까지 100% 충전
│      [ 승급 ]        [ 닫기 ]      │
└───────────────────────────────────┘
```

- **표시 항목**(사용자 요구): 사용 횟수·막타 처치(현재/요구), 현재 랭크의 **소모 자원**·**보너스 데미지**, `[승급]`·`[닫기]` 버튼.
- **다음 랭크 미리보기**: 배율(딜스킬) 또는 경감%/반격%(디펜스) 현재→다음. MASTER면 "최고 랭크" 표기, 승급/임시버튼 숨김.
- **디펜스**: "보너스 데미지" 대신 **피해 경감 50%→52%, 반격 30%→35%**로 표시.
- **임시 드라이버(사용자 지정, §14.2)**: `[사용횟수 업]`·`[막타 처치 업]` — 누르면 해당 카운트를 **다음 랭크 요구치까지 즉시 100% 충전**(테스트용). 둘 다 채우면 `[승급]`이 활성화된다. 전투(7순위) 연결 시 **제거**한다.
- **승급 흐름**(확정, 004 환생 confirm 패턴): `[승급]`은 승급 가능(15.1 조건)일 때만 활성 → 누르면 `confirm("승급하시겠습니까?")` → **확인** 시 랭크업 처리(AP 소모·랭크 +1·카운터 리셋·영구 보너스 재계산·저장). **취소** 시 아무 것도 안 함.
- **승급 후 모달 재세팅**: 팝업을 닫지 않고 **새 랭크 기준으로 갱신**한다 — 현재 랭크=방금 오른 랭크, 다음 랭크 미리보기·소모자원·보너스데미지·사용/막타(0/새 요구치)·필요AP/보유AP를 새 값으로 다시 채운다(임시 버튼도 다시 활성). 연속 승급을 바로 이어서 테스트 가능.
- **MASTER 도달 시**: 재세팅된 모달은 미리보기·`[승급]`·임시 버튼을 숨기고 "최고 랭크"만 표시.
- `[닫기]`는 팝업을 닫는다.

### 15.3 뷰 모델 (개념)

```java
// application/dto
record SkillListView(String activeTab, List<SkillRowView> rows) {}

record SkillRowView(
    String id, String label, String talentLabel,     // 아이콘 없음
    String rankLabel,        // "F".."1","Master"
    int progressPercent,     // (min(사용%,100)+min(막타%,100))/2 — 동일가중 평균 (MASTER면 100/MAX)
    boolean rankable,        // 승급 가능(훈련+AP) → 버튼 강조색
    boolean maxed            // MASTER
) {}

record SkillRankUpView(
    String label,                                    // 아이콘 없음
    String currentRankLabel, String nextRankLabel,   // MASTER면 nextRankLabel null
    int currentValue, int nextValue,                 // 딜: 배율% / 디펜스는 별도 두 쌍
    String resourceKindLabel, int resourceCost,      // "스태미나"/"MP" + 고정 소모량
    int usageCurrent, int usageRequired,
    int killCurrent, int killRequired,
    int apCost, int apOwned,
    boolean rankable
) {}
```

- 순수 조립: `SkillService`가 `CharacterSkill`(랭크·카운트) + 카탈로그(`Skill`) + `SkillRankPolicy`(요구치·AP) + `CharacterProgress`(보유 AP)를 모아 뷰를 만든다. `PlayScreenViewHelper` 패턴 재사용.
- **엔드포인트**: 목록 조회, 탭 전환(클라이언트 필터로도 가능), `[승급]` POST(→ 랭크업 트랜잭션 §9, 응답으로 **새 랭크 기준 `SkillRankUpView`**를 반환해 모달 재세팅), 임시 `[사용횟수 업]`·`[막타 처치 업]` POST(§14.2, 해당 카운트를 요구치까지 100% 충전 후 모달 갱신). 응답은 003/004식 fragment 스왑.

### 15.4 UI 확정 요약

- **탭**(확정): `전체` · `근접전투` · `활` · `마법` · `공용`(디펜스).
- **목록 행**(확정): 아이콘·`사용하기` **제거**, `스킬명 · 랭크 · 진행바 · [승급]`. 진행바 = `(min(사용%,100)+min(막타%,100))/2`(동일가중 평균, 둘 다 채워야 100%), 승급 가능 시 승급 버튼 강조색.
- **승급 모달**(확정): 아이콘 제거. 하단에 `[승급]`·`[닫기]` + 임시 `[사용횟수 업]`·`[막타 처치 업]`(누르면 요구치 100% 충전, 전투 7순위 연결 시 제거).

---

## 부록. 사용자 원문 메모 대응표

| 사용자 메모 | 반영 위치 |
|---|---|
| 스킬 타입: 일반/강/방어 | §3 (`SkillType`) |
| 같은 타입 차이 = 데미지 배율·자원소모 (후려치기 100%/밀어치기 50%) | §3, §4, §10 |
| 데미지 배율 vs 다른 공식 (의논) | **D1 ★ 확정** §4 (전투 데미지 계약) |
| 무기 자유(A방안)·무기재능 주스탯·크리 1.5배·±10% 편차·비율식 DEF | §4, §7 (전투 7순위 구현, 장비 5순위 선행) |
| 랭크 F-A-9-1-Master | **D2** §5 (`SkillRank`) |
| 랭크업 조건: 막타 처치 + 사용 횟수 | **D3** §6 (`SkillRankPolicy`) |
| 스킬은 재능에 속함, 재능 일치 시 +10% | §2.3, §7 (`SkillTalent`, `damageBonusPercent()`) |
| 랭크업마다 재능별 영구 스탯 상승 (어떤 스탯 의논) | **D4 ★ 확정** §8 (재능 주 스탯 +1/랭크업, 대칭) |
| 랭크업 시 AP 소모, AP는 레벨업당 +1 | **D5** §9 (`spendAbilityPoints`) |
| 방어 스킬은 어느 재능? 모두 사용 가능해야 | **D6 ★** §7 (`COMMON`) |
| 디펜스 반격(부분경감 + 내 스탯 기반 반격), DEF는 상시경감 스탯 | §4 (가위바위보 삼각관계 계약), §8 (COMMON→DEF) |
| 스킬 팝업(탭·보유스킬 목록·승급 버튼 강조색) + 승급 팝업(사용/막타·소모자원·보너스데미지·AP) | §15 (UI 레이아웃) |
