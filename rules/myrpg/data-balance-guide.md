---
inclusion: fileMatch
fileMatchPattern: '**/data/*.json'
---

# MyRPG 데이터 밸런스 가이드 (질의응답 기반 authoring 프로토콜)

`item.json` · `monster.json` · `skill.json` 에 **아이템 / 몬스터 / 스킬을 추가·수정**할 때,
에이전트가 사용자와 **질의응답(Q&A)**으로 밸런스에 맞게 데이터를 **함께 구축**하기 위한 프로토콜이다.

> 이 문서는 "특정 아이템 카탈로그"가 아니라 **행동 지침 + 밸런스 규칙**이다.
> 개별 아이템/스킬/몬스터의 실제 수치는 **항상 해당 JSON을 읽어 baseline으로 삼는다**(하드코딩 금지).

---

## 이 가이드의 사용법 (에이전트 필독 · 최우선)

아이템/몬스터/스킬 **추가·수정 요청**을 받으면 아래를 반드시 따른다.

1. **하드코딩된 예시에 의존하지 않는다.** 먼저 대상 JSON(`item.json`/`monster.json`/`skill.json`)을 읽어 **현재 데이터를 기준선(baseline)으로 삼는다.** (이 문서엔 "초보자용 한손검=STR+5" 같은 카탈로그 값을 두지 않는다 — 데이터가 곧 진실.)
2. **한 번에 다 묻지 않는다.** `§B` 프로토콜의 **질문을 순서대로** 하나씩 진행한다.
3. **스탯 수치는 반드시 비교로 제시한다.** "같은 종류의 기존 아이템(직전/최상위)은 지금 이러이러한데, 새 건 **더 세게? 비슷하게? 뭐가 다르게?**"를 baseline과 함께 물어본다.
4. **값이 정해질 때마다 즉시 `§C` 원칙으로 검증**하고, 위반 시 그 자리에서 지적한다(1뎀 문제 / 밴드 초과 / 상한 초과 / 재능-주스탯 불일치 등).
5. 모든 값이 정해지면 **JSON 초안을 보여주고 사용자 확인**을 받은 뒤 반영한다.
6. 반영 후 **검증 실행**:
   - **밸런스 검증 스크립트**(`tools/balance/`): `python3 verify_equipment.py` / `python3 verify_monster.py` / `python3 verify_skill.py` — `§C` 규칙(CP·판매가·밴드·상한)을 코드로 즉시 검증
   - **빌드 검증**: `mvn test -pl myrpg` (카탈로그 파싱·16키·단조 테스트) → `mvn clean install -pl myrpg -am`

> 목표: "사용자가 이름과 대략의 컨셉만 말해도, 에이전트가 baseline·규칙을 근거로 질문·제안하여 밸런스가 깨지지 않는 값이 나오게 한다."

---

## A. 공통 진행 루프 (모든 카테고리 공통)

1. **대상 파일 읽기** → 같은 카테고리/종류의 기존 항목을 나열해 baseline 제시.
2. **컨셉 확인**: 이름, 종류, "기존 대비 상/하/유사 + 차별점".
3. `§B`의 해당 프로토콜 질문을 순서대로 Q&A. 각 답을 `§C` 원칙으로 검증.
4. 수치는 baseline 대비 **상대 위치**로 합의(절대값 강요 금지). 자동 계산되는 값(판매가·수리비 등)은 계산해서 보여준다.
5. JSON 초안 제시 → 확인 → 반영 → 테스트/빌드.

---

## B. 카테고리별 질의응답 프로토콜

### B-1. 무기 추가 (`item.json`, `type: weapon`)
먼저 `item.json`에서 **같은 `kind`의 기존 무기들**을 읽어 나열한다. 그 뒤 순서대로:

1. **이름**은?
2. **무기 종류(`kind`)**는? (`one_handed_sword`/`two_handed_sword`/`wand`/`staff`/`bow`)
   → `kind`가 **재능(근접/마법/활)과 손 점유(한손/양손)**를 결정한다(`§C-2`).
3. (같은 `kind` 기존 무기 목록·주스탯을 보여주며) **이 무기는 그중 어디 위치?** 최상위보다 강한가 / 특정 무기와 비슷한가 / 그 사이인가? **차별점은?**
4. **주스탯 보너스 수치**는? — 주스탯은 `kind`로 자동(근접=STR, 마법=INT, 활=DEX). baseline 대비 몇 포인트 위/아래인지로 합의. (활이면 `§C-2`대로 CRITICAL 보너스도 부여할지 확인.)
5. **`maxDurability`**는? (내구도는 그때그때 정함. 좋은 무기라 해서 내구도가 높진 않음.)
6. **상점 판매**할 건가? → 판매하면 `buyPrice`를 정한다. **판매가·수리비를 자동 계산해 제시**(`§C-3`). 안 팔면 drop 전용(`buyPrice` 없음).
7. **몬스터 드랍**시킬 건가? 어느 몬스터/확률? (드랍은 `monster.json`의 `itemDrops`)

검증: 주스탯↔재능 일치, 같은 `kind` 내 강함 단조성(상위 티어가 하위보다 약하지 않게), 양손=방패 불가, 활만 CRITICAL 부여.
→ **스크립트 검증**: `cd tools/balance && python3 verify_equipment.py --json '{신규 무기 JSON}'` — CP 순위·주스탯·CRITICAL 경고를 자동 확인.

### B-2. 방어구 추가 (`item.json`, `type: armor`)
`item.json`에서 **같은 슬롯(`kind`)의 기존 방어구**를 읽어 나열한 뒤:

1. **이름**은?
2. **슬롯(`kind`)**은? (`shield`/`armor_body`/`helmet`/`gloves`/`boots`)
3. (같은 슬롯 기존 방어구 DEF를 보여주며) **DEF 수치**는? baseline 대비 위치·차별점.
4. `maxDurability` / 상점 판매(`buyPrice`) / 드랍 여부 — B-1의 5~7과 동일.

검증: DEF가 그 구간 몬스터 `attackPower`를 **완전 무효화하지 않는 선**(`§C-2`), 슬롯당 유일 착용.
→ **스크립트 검증**: `cd tools/balance && python3 verify_equipment.py --json '{신규 방어구 JSON}'` — DEF합·EHP·CP 순위 확인.

### B-3. 포션 추가 (`item.json`, `type: potion`)
기존 포션들(`healHp`/`buyPrice`)을 읽어 나열한 뒤:

1. 이름 / 2. 회복량(`healHp`) / 3. `buyPrice`(회복량 대비 비례, `§C-2`) → 판매가 자동 계산 제시.
→ **스크립트 검증**: `cd tools/balance && python3 verify_equipment.py --json '{신규 포션 JSON}'`.

### B-4. 몬스터 추가 (`monster.json`)
`monster.json`에서 **인접 레벨대 기존 몬스터**를 읽어 나열하고, **그 레벨 플레이어의 성장 기준선(`§C-1`)과 유효 DEF 추정치**를 함께 제시한 뒤:

1. **이름 / `level` / 타입**(일반·보스)?
2. (그 레벨 플레이어 유효 DEF 추정을 보여주며) **`attackPower`** → `§C-4` 비율(유효 DEF의 2~2.5배)로 제안·합의.
3. **`maxHp`** → 플레이어 1타 추정 × 목표 처치 턴(일반 3~5, 보스 10+)으로 역산 제시.
4. **`defense`**(플레이어 1타를 다 씹지 않는 선) / **`critical`**(0.1% 단위) / **`experience`·`goldDrop`**(위협도 비례).
5. **`itemDrops`**(드랍 아이템·확률) / 방어 상수 오버라이드(`defenseBlockRate`/`defenseCounterRate`) 필요 시(기본 100% 완전 방어 / 0 반격).

검증: `attackPower` > 그 구간 플레이어 유효 DEF(1뎀 방지), 보상이 위협도에 비례.
→ **스크립트 검증**: `cd tools/balance && python3 verify_monster.py --level {레벨} --json '{신규 몬스터 JSON}'` (난이도계수는 `--difficulty`로 조정).

### B-5. 스킬 추가 (`skill.json`)
`skill.json`에서 **같은 재능·같은 `type`의 기존 스킬**과 그 밴드 위치를 읽어 제시한 뒤:

1. **이름 / 재능(`talent`) / `type`**(`NORMAL`·`HEAVY`·`DEFENSE`)?
2. **정체성**: 딜 스킬이면 **2축(`hitCount` 연타 / `critBonus` 크리특화)** 중 어디에 위치? (기존 스킬과 역할 겹치지 않게 — `§C-5`)
3. **배율**(`multiplierByRank`, 1히트당) → 해당 `type` 밴드(`§C-5`) 안에서 랭크별 F→MASTER 값 합의. `hitCount`·`critBonus` 확정.
4. **`resourceCost`**(근접/활=스태미나, 마법=MP; 기준 NORMAL 7/HEAVY 10/DEFENSE 4~8, 랭크별 감소 가능).
5. 방어 스킬이면 `blockRateByRank` / `counterMultiplierByRank` / `resourceCostByRank` / `critBonusByRank` 배분(`§C-5`).

검증: 랭크 맵 **16키(F→MASTER) 완비 + 단조성**, 밴드 초과 금지, `critBonus` 상한 +100(딜스킬)/+200(카운터), 마법은 `critBonus` 0, 반격율 상한 200%.
→ **스크립트 검증**: `cd tools/balance && python3 verify_skill.py --json '{신규 스킬 JSON}'` (SP 참조 크리는 `--ref-crit`로 조정).

---

## C. 밸런스 원칙 (Q&A 중 값 검증 기준)

> 아래는 **규칙/공식/밴드/비율**이다(특정 아이템 수치가 아님). 값 검증·제안 근거로 사용한다.

### C-0. 데미지 공식 (반드시 이해)
전투 데미지는 **감산형 + 최소 1**. 스킬은 **히트당 배율**(`multiplierByRank`)을 `hitCount`번 적용해 합산한다.

```
공격력   = round(주스탯 × 재능계수)                       // 재능계수: 근접 1.0 / 활 0.85 / 마법 1.2

[스킬 hitCount 만큼 반복하여 합산]
  기본피해(히트) = max(1, floor(공격력 × 히트당배율% / 100) − 대상.defense)
  보정피해(히트) = 기본피해 × 상성계수 × (크리티컬 ? 1.5 : 1)
  히트피해      = max(1, round(보정피해 × rand(0.90 ~ 1.10)))   // 히트마다 독립 편차
최종피해 = Σ 히트피해
```

- **상성계수**(가위바위보): 승 1.0 / 무승부 0.5 / 방어당함 `(1 − blockRate)` / 관통패 0.0.
- **크리티컬**: `random.nextInt(1000) < (critical + critBonus)`(0.1% 단위) → **×1.5**. 모든 결과(공격·반격·무승부)에 적용, 다단은 히트마다 독립 판정.
- **몬스터 배율**: 스킬 없이 행동별 상수 — 일반 100% / 강 150%(공격력=`attackPower`). 항상 단일 히트.
- **다단(`hitCount`≥2)**: 방어가 히트마다 차감 → 고방어에 급격히 약해지고, 크리·편차가 평균화(안정). 단일 히트는 관통·버스트.

**3대 규칙**:
1. **자동 레벨 스케일링 없음** — `monster.json`에 authoring한 만큼만 강하다.
2. **공격력이 상대 방어를 못 넘으면 전부 "1뎀"** → 공격 스탯은 항상 그 구간 상대 방어보다 위.
3. **비율(%) 값은 전 레벨 자동 스케일**(경감률·크리·스킬 배율·몬스터 방어 상수), **정액 값은 수동 조정**(무기 스탯·몬스터 attackPower/defense/maxHp).

> **±10% 편차**가 붙으므로 **평균값 기준**으로 밸런싱하고 "정확히 N턴 컷"에 의존하지 말 것.

### C-1. 플레이어 성장 기준선 (`StatProgression`, 코드 파생 공식)

| 항목 | 산출식 | Lv1 | Lv10 | Lv30 | Lv50 |
|---|---|---|---|---|---|
| 주스탯(STR/DEX/INT) | 10 + 3×(lv−1) `+재능 보너스` | 10 | 37 | 97 | 157 |
| DEF | 5 + 1×(lv−1) `+장비` | 5 | 14 | 34 | 54 |
| HP | 100 + 10×(lv−1) `+재능 HP` | 100 | 190 | 390 | 590 |
| 크리티컬 | 50 + 3×(lv−1) (0.1%) | 5.0% | 7.7% | 13.7% | 19.7% |

- 공격력 = 주스탯 × 재능계수(근접 1.0 / 활 0.85 / 마법 1.2). 이 표는 **장비·재능 보너스 제외 하한** → 몬스터는 이보다 여유 있게 위로.

### C-2. 아이템 규칙 (수치 예시 없음 — item.json이 baseline)
- **무기**: 데미지를 직접 주지 않고 **주스탯 보너스(`bonuses`)로 공격력에 기여**. 주스탯은 재능에 맞춤(근접=STR / 활=DEX / 마법=INT). **활만 CRITICAL 보너스**로 "활=크리↑" 특성 부여.
- **양손 무기**(양손검·활·스태프)는 방패 동시 착용 불가(`requiredSlots={MAIN_HAND,OFF_HAND}`). 한손(한손검·완드)은 방패 병용 가능.
- **`maxDurability`**: 내구도는 그때그때 아이템 마다 정함. 좋은 무기여도 내구도가 낮을수도 있음. **전투 공격 턴당 `0.05` 감소**(내구도 20 → 400턴, 1포인트=20턴). ← 확정값.
- **방어구**: DEF는 정액 감산 → **과도하면 몬스터 공격을 통째로 씹는다.** 신규 DEF는 그 티어 몬스터 `attackPower`를 완전 무효화하지 않는 선. 슬롯당 유일 착용.
- **포션**: `healHp`·`buyPrice`. 회복량 대비 가격 비례.
- 새 아이템의 강함은 **같은 `kind`의 기존 최상위/직전 아이템(item.json)** 대비 상대 위치로 정한다.

### C-3. 판매가 · 수리비 · 구매가 (확정)
- **판매가** = `기본가 + (인스턴스보너스 × weightOf)`.
  - `기본가`(배타): `buyPrice` 있으면 `round(buyPrice × 0.5)`, 없으면(드랍 전용) `Σ(카탈로그 amount × weightOf(target))`.
  - `인스턴스보너스` = 인챈트로 붙은 보너스(상점템·드랍템 공통으로 항상 덧셈). 인챈트 미구현 현재는 0.
  - **`weightOf(target)`: CRITICAL = 1, 그 외(STR/DEX/INT/DEF/HP/MP/STAMINA) = 10.** (CRITICAL amount는 0.1%단위(10=1%)라 ×10이면 과대 → 1/10 보정.)
- **구매가(`buyPrice`)**: 사용자가 아이템별 수기 지정. 없으면 상점 미판매(드랍 전용). 별도 `baseValue` 필드 없음(buyPrice가 곧 base).
- **수리비(1포인트당) = 판매가** 그대로(대장간). → 좋은/인챈트 장비일수록 수리비↑(가치 기반 골드 싱크).
- **인챈트(`ENCHANT`)**: 스펙 미확정 → 데이터 임의 추가 금지.

### C-4. 몬스터 규칙 (수치 예시 없음 — monster.json이 baseline)
필드: `level, maxHp, attackPower, defense, critical(0.1%), experience, goldDrop{min,max}, itemDrops[], defenseBlockRate(기본100), defenseCounterRate(기본0)`.

- **`attackPower` > 그 구간 플레이어 유효 DEF**(장비 포함 추정) — 1뎀 방지. 강공격 턴은 150%.
- 스케일링 비율: `attackPower` ≈ 유효 DEF의 **2~2.5배**, `maxHp` ≈ 플레이어 1타의 **4~6배**(목표 처치 턴), `defense`는 플레이어 1타를 다 씹지 않는 선(낮게).
- `critical`: 일반 낮게(1~5%), 보스만 소폭↑. `experience`·`goldDrop`: 위협도(HP×위협) 비례, 약체에 과보상 금지.
- 방어 상수(%, 레벨 무관 자동 스케일): 기본 **100% 완전 방어 (0 피격) / 0 반격** (일반/보스 공통 대칭 룰, 강공격/스매시에 관통).
- 기준 앵커(Lv1 최약체 등)는 **monster.json에서 읽어** 상대 위치의 기준으로 삼는다.

### C-5. 스킬 규칙 (밴드는 규칙 · 로스터는 skill.json이 baseline)
- `type`: `NORMAL`/`HEAVY`/`DEFENSE`. `talent`: `MELEE`/`ARCHERY`/`MAGIC`/`COMMON`(방어).
- **랭크 맵 16키(F→MASTER) 완비 + 단조성**(카탈로그 로드 테스트가 강제).
- `resourceCost`: 근접/활=스태미나, 마법=MP. 기준 NORMAL 7 / HEAVY 10 / DEFENSE 4~8.
- **딜 스킬 2축**(배율만 다른 복제품 금지):
  - 축 A `hitCount`: 단일(1, 관통·버스트) ↔ 다단(2~4, 안정·저방어 학살). 다단은 총배율을 단일보다 살짝 위, 히트당 배율은 낮게 쪼갬.
  - 축 B `critBonus`(0.1%): 표준(0) ↔ 크리특화(양수, 기본 배율 소폭↓). **상한 +100(+10%p)**.
- **배율 밴드**(1히트당, 규칙):
  - NORMAL 단일: F 90 → MASTER 170.
  - HEAVY 단일: F 130 → MASTER 250(활 최상위 매그넘류 F 140 → 260).
  - 다단(히트당): 3타 F 35→65(총 105→195), 4타 F 27→50(총 108→200).
- **재능 정체성**: 근접(계수1.0, 균형·크리특화는 HEAVY에만) / 활(계수0.85·크리↑·1턴 선제, 크리특화 집중) / 마법(계수1.2·캐스팅실패10%, **critBonus 0**·깡뎀 지향).
- **방어 스킬**(`COMMON`):
  - **디펜스 (`defense`)**: `blockRateByRank` 100% 완전 방어(0 피격), `counterMultiplierByRank` 0 (반격 없음), `resourceCostByRank` 5→1 점진 감소, 영구 스탯(DEF +1, HP +5 / 랭크당).
  - **카운터 어택 (`counter_attack`)**: `blockRateByRank` 100% 완전 회피, `counterMultiplierByRank` 100%→200% (상대 공격력 비례 반격, 상한 **200%**), `critBonusByRank` 0→200(+20%p), 자원 소모 8 고정, 영구 스탯 없음.
- 신규 딜 스킬은 먼저 3레버(배율/`hitCount`/`critBonus`)+저비용 수치 축(자원효율·편차폭·관통·크리위력)으로 채우고, 상태이상·흡혈·충전·처형·콤보 같은 **메커니즘 축은 별도 스펙**으로만 도입(축마다 대가, 기존 축과 역할 중복 금지).

---

### C-6. tools/balance 검증 스크립트 (규칙의 코드화)

`§C`의 규칙·공식·밴드·상한을 코드로 구현한 검증 도구. `--json '{...}'`으로 신규 후보를 기존 카탈로그와 즉시 비교·검증하며, 반영 후에는 항상 실행한다.

| 스크립트 | 대상 | 주요 검증 내용 |
|---|---|---|
| `verify_equipment.py` | `item.json` 장비·포션 | CP/ΔCP(무기 주스탯 기여, 방어구 EHP 기여), 판매가·수리비(`weightOf`·buyPrice×0.5), 같은 kind/slot 내 CP 순위, 주스탯↔재능 일치, 활만 CRITICAL 경고 |
| `verify_monster.py` | `monster.json` 몬스터 | CP(O×S), 난이도비(몬스터CP/플레이어 baseline CP), 1뎀 방지(attackPower>유효DEF), 처치 턴, 실피해, critical 상한 경고, 목표 스탯 제시(`--level`/`--difficulty`) |
| `verify_skill.py` | `skill.json` 스킬 | SP(총배율×크리계수×캐스팅성공×재능계수), 명목총배율 밴드(NORMAL/HEAVY), critBonus 상한 +100 · 마법 0, 반격율 상한 200%, 랭크 16키+단조성 |

**공통 계산식·상수는 `balance_core.py`가 단일 소스** — 재능계수, KIND_TO_TALENT, weightOf, RANK_KEYS, 몬스터 방어 상수, 플레이어 성장 기준선. 가이드(`§C`)와 이 코드가 어긋나면 이 스크립트를 기준으로 불일치를 확인한다.

```bash
cd tools/balance
python3 verify_equipment.py [--level N] [--json '{신규 아이템 JSON}']
python3 verify_monster.py  [--level N] [--difficulty D] [--json '{신규 몬스터 JSON}']
python3 verify_skill.py    [--ref-crit N] [--json '{신규 스킬 JSON}']
```

---

## D. 완료 체크리스트 (반영 전)

- [ ] baseline을 **JSON에서 읽어** 비교·제안했는가(하드코딩 값에 의존하지 않음)?
- [ ] 무기: 주스탯↔재능 일치, 같은 kind 내 강함 단조, 양손=방패불가, 활만 CRITICAL?
- [ ] 방어구: DEF가 그 구간 몬스터 attackPower를 완전 무효화하지 않나?
- [ ] 판매가/수리비: `weightOf`(CRITICAL=1) 적용, buyPrice×0.5 배타, 수리비=판매가 확인?
- [ ] 몬스터: attackPower가 그 구간 유효 DEF보다 위(1뎀 방지)? maxHp가 목표 처치 턴? critical 0.1%? 보상 비례?
- [ ] 스킬: 랭크 16키+단조성? NORMAL<HEAVY 밴드? 2축 차별화? critBonus 상한 +100(딜)/+200(카운터)? 마법 critBonus 0? 반격율 ≤200%?
- [ ] JSON 문법(쉼표·따옴표) 유효?
- [ ] **밸런스 스크립트 실행**: 변경 카테고리에 맞는 스크립트(`verify_equipment.py`/`verify_monster.py`/`verify_skill.py`)를 `--json`으로 실행해 경고 없음 확인?
- [ ] **검증 실행**: `mvn test -pl myrpg` → `mvn clean install -pl myrpg -am`.

