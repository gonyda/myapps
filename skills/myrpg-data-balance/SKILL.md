---
name: myrpg-data-balance
description: MyRPG 게임 데이터(아이템, 몬스터, 스킬, 맵)를 추가·수정할 때 대화형 질의응답(Q&A)과 파이썬 검증 스크립트를 통해 밸런스를 검증하고 JSON에 안전하게 반영하는 워크플로우 Skill입니다. (myrpg-date-balance)
---

# MyRPG 게임 데이터 밸런스 Skill (`myrpg-data-balance`)

`item.json` · `monster.json` · `skill.json` · `map.json` 에 **아이템 / 몬스터 / 스킬 / 맵을 추가·수정**할 때,
에이전트가 사용자와 **질의응답(Q&A)**으로 밸런스에 맞게 데이터를 **함께 구축**하고 **파이썬 검증 스크립트(`tools/balance/`)로 자동 검수**한 뒤 반영하는 프로토콜입니다.

> ⚠️ **핵심 원칙**:
> 1. 개별 아이템/스킬/몬스터의 실제 수치는 **항상 해당 JSON을 읽어 baseline으로 삼습니다**(하드코딩 금지).
> 2. 한 번에 모든 정보를 묻지 않고, **순서대로 1~2개씩 질문(Q&A)**을 진행합니다.
> 3. 모든 값이 정해지면 **파이썬 검증 스크립트를 실행하여 규칙 위반/CP/SP/1뎀 여부를 검증**한 뒤 사용자에게 제시합니다.

---

## 1. 전체 워크플로우 (5단계 파이프라인)

```
[1. 대상 카테고리 식별 & Baseline 로드]
  대상 JSON 파일 읽기 → 동일 카테고리/티어 기존 항목 목록 추출 및 사용자에게 제시
       ↓
[2. 단계별 1:1 대화형 질의응답 (Q&A Loop)]
  순서에 따라 질문 진행 → 답변마다 밸런스 룰(1뎀, DEF 상한, 밴드, 주스탯 일치) 즉각 점검
       ↓
[3. JSON 초안 구성 & Python 검증 스크립트 실행]
  `tools/balance/verify_*.py --json '{...}'` 실행하여 CP/SP/EHP/난이도/경고 자동 검증
       ↓
[4. 검증 리포트 & JSON 초안 제시 (사용자 승인)]
  파이썬 검증 결과(CP 순위, 경고 유무, 권장 가격 등)와 JSON 초안 제시 → 사용자 승인 획득
       ↓
[5. 대상 JSON 파일 반영 & 빌드 검증]
  JSON 파일에 반영 → `mvn test -pl myrpg` (카탈로그 16키/단조성/파싱 테스트) 실행
```

---

## 2. 카테고리별 대화형 Q&A 프로토콜

### 2.1. 무기 추가 (`item.json`, `type: weapon`)
먼저 `item.json`에서 **같은 `kind`의 기존 무기들**을 읽어 나열한 뒤 순서대로 질문합니다:

1. **이름**은?
2. **무기 종류(`kind`)**는? (`one_handed_sword` / `two_handed_sword` / `wand` / `staff` / `bow`)
   - `kind`가 **재능(근접/마법/활)과 손 점유(한손/양손)**를 결정합니다.
3. (같은 `kind` 기존 무기 목록·주스탯을 보여주며) **이 무기는 그중 어디 위치인가요?** 최상위보다 강한가 / 특정 무기와 비슷한가 / 그 사이인가? **차별점은?**
4. **주스탯 보너스 수치**는?
   - 주스탯은 `kind`로 자동 매핑 (근접=STR, 마법=INT, 활=DEX). baseline 대비 몇 포인트 위/아래인지 합의. (활이면 CRITICAL 보너스 부여 여부 확인)
5. **`maxDurability`**는? (내구도는 그때그때 정함. 좋은 무기라 해서 무조건 내구도가 높진 않음)
6. **상점 판매**할 건가요?
   - 판매 시 `buyPrice` 지정 $\rightarrow$ 판매가·수리비를 자동 계산하여 제시 (`§3.3`). 안 팔면 drop 전용(`buyPrice` 미설정).
7. **몬스터 드랍**시킬 건가요? 어느 몬스터/확률? (드랍은 `monster.json`의 `itemDrops`)

> **파이썬 검증 실행**:
> ```bash
> cd tools/balance && python3 verify_equipment.py --json '{신규 무기 JSON}'
> ```

---

### 2.2. 방어구 추가 (`item.json`, `type: armor`)
`item.json`에서 **같은 슬롯(`kind`)의 기존 방어구**를 읽어 나열한 뒤 순서대로 질문합니다:

1. **이름**은?
2. **슬롯(`kind`)**은? (`shield` / `armor_body` / `helmet` / `gloves` / `boots`)
3. (같은 슬롯 기존 방어구 DEF를 보여주며) **DEF 수치**는? baseline 대비 위치 및 차별점.
4. **`maxDurability`**, **상점 판매(`buyPrice`)**, **드랍 여부** 질문.

> **파이썬 검증 실행**:
> ```bash
> cd tools/balance && python3 verify_equipment.py --json '{신규 방어구 JSON}'
> ```

---

### 2.3. 포션 추가 (`item.json`, `type: potion`)
기존 포션 목록(`healHp`, `healMp`, `healStamina`, `buyPrice`)을 읽어 나열한 뒤:

1. **이름** / 2. **회복량**(`healHp` / `healMp` / `healStamina`) / 3. **`buyPrice`**(회복량 대비 비례) $\rightarrow$ 판매가 자동 계산 제시.

> **파이썬 검증 실행**:
> ```bash
> cd tools/balance && python3 verify_equipment.py --json '{신규 포션 JSON}'
> ```

---

### 2.4. 몬스터 추가/조정 (`monster.json`)
`monster.json`에서 **인접 레벨대 기존 몬스터**를 읽어 나열하고, **그 레벨 플레이어의 성장 기준선(`§3.1`)과 유효 DEF 추정치**를 함께 제시한 뒤:

1. **이름 / `level` / 타입**(일반·보스)?
2. (그 레벨 플레이어 유효 DEF 추정을 보여주며) **`attackPower`** $\rightarrow$ `§3.4` 비율(유효 DEF의 2~2.5배)로 제안 및 합의.
3. **`maxHp`** $\rightarrow$ 플레이어 1타 추정 × 목표 처치 턴(일반 3~5턴, 보스 10+턴)으로 역산 제시.
4. **`defense`**(플레이어 1타를 다 씹지 않는 선) / **`critical`**(0.1% 단위) / **`experience`·`goldDrop`**(위협도 비례).
5. **`itemDrops`**(드랍 아이템·확률) / 방어 상수 오버라이드(`defenseBlockRate`/`defenseCounterRate`) 필요 시(기본 100% 완전 방어 / 0 반격).

> **파이썬 검증 실행**:
> ```bash
> cd tools/balance && python3 verify_monster.py --level {레벨} --difficulty {난이도계수} --json '{신규 몬스터 JSON}'
> # 난이도계수: 0.6=매우쉬움, 0.9=쉬움, 1.2=대등, 2.0=강함, 보스급
> ```

---

### 2.5. 스킬 추가 (`skill.json`)
`skill.json`에서 **같은 재능·같은 `type`의 기존 스킬**과 그 밴드 위치를 읽어 제시한 뒤:

1. **이름 / 재능(`talent`) / `type`**(`NORMAL`·`HEAVY`·`DEFENSE`)?
2. **정체성**: 딜 스킬이면 **2축(`hitCount` 연타 / `critBonus` 크리특화)** 중 어디에 위치? (기존 스킬과 역할 중복 방지 — `§3.5`)
3. **배율**(`multiplierByRank`, 1히트당) $\rightarrow$ 해당 `type` 밴드(`§3.5`) 안에서 랭크별 F→MASTER 값 합의. `hitCount`·`critBonus` 확정.
4. **`resourceCost`**(근접/활=스태미나, 마법=MP; 기준 NORMAL 7/HEAVY 10/DEFENSE 4~8, 랭크별 감소 가능).
5. 방어 스킬이면 `blockRateByRank` / `counterMultiplierByRank` / `resourceCostByRank` / `critBonusByRank` 배분(`§3.5`).

> **파이썬 검증 실행**:
> ```bash
> cd tools/balance && python3 verify_skill.py --ref-crit 50 --json '{신규 스킬 JSON}'
> ```

---

### 2.6. 맵 노드 추가 (`map.json`)
`map.json`에서 기존 맵 노드와 연결 관계를 조회한 뒤:

1. **맵 ID / 이름 / 구역 타입**(`field` / `town` / `dungeon_entrance` 등)?
2. **연결 노드(`connectedMapIds`)**: 어느 맵 노드와 양방향 연결할 것인가?
3. **출현 몬스터(`spawnMonsterIds`)**: 해당 맵에 등장할 몬스터 ID 목록 및 가중치/레벨대 적합성 검토.
4. **상주 NPC(`npcIds`)**: 상점/대장간/힐러/퀘스트 NPC 배치 여부.

---

## 3. 밸런스 공식 및 원칙 (Q&A 중 실시간 검증 기준)

### 3.0. 전투 데미지 공식
전투 데미지는 **감산형 + 최소 1**. 스킬은 **히트당 배율**(`multiplierByRank`)을 `hitCount`번 적용해 합산합니다.

```
공격력   = round(주스탯 × 재능계수)                       // 재능계수: 근접 1.0 / 활 0.85 / 마법 1.2

[스킬 hitCount 만큼 반복하여 합산]
  기본피해(히트) = max(1, floor(공격력 × 히트당배율% / 100) − 대상.defense)
  보정피해(히트) = 기본피해 × 상성계수 × (크리티컬 ? 1.5 : 1)
  히트피해      = max(1, round(보정피해 × rand(0.90 ~ 1.10)))   // 히트마다 독립 편차
최종피해 = Σ 히트피해
```

- **상성계수**(가위바위보): 승 1.0 / 무승부 0.5 / 방어당함 `(1 − blockRate)` / 관통패 0.0.
- **크리티컬**: `random.nextInt(1000) < (critical + critBonus)`(0.1% 단위) $\rightarrow$ **×1.5**.
- **다단(`hitCount` $\ge$ 2)**: 방어력이 히트마다 차감 $\rightarrow$ 고방어에 급격히 약해지고 편차가 안정화. 단일 히트는 방어 관통 및 버스트 딜.

---

### 3.1. 플레이어 성장 기준선 (`StatProgression`)

| 항목 | 산출식 | Lv1 | Lv10 | Lv30 | Lv50 |
|---|---|---|---|---|---|
| 주스탯(STR/DEX/INT) | 10 + 3×(lv−1) `+재능 보너스` | 10 | 37 | 97 | 157 |
| DEF | 5 + 1×(lv−1) `+장비` | 5 | 14 | 34 | 54 |
| HP | 100 + 10×(lv−1) `+재능 HP` | 100 | 190 | 390 | 590 |
| 크리티컬 | 50 + 3×(lv−1) (0.1% 단위) | 5.0% | 7.7% | 13.7% | 19.7% |

---

### 3.2. 아이템 규칙
- **무기**: 데미지를 직접 주지 않고 **주스탯 보너스(`bonuses`)로 공격력에 기여**. (근접=STR, 활=DEX, 마법=INT). **활만 CRITICAL 보너스** 부여.
- **양손 무기**(양손검, 활, 스태프)는 방패 동시 착용 불가(`requiredSlots={MAIN_HAND, OFF_HAND}`).
- **`maxDurability`**: 전투 공격 턴당 `0.05` 감소 (내구도 20 $\rightarrow$ 400턴, 1포인트=20턴).
- **방어구**: DEF 정액 감산 $\rightarrow$ 몬스터 공격을 완전 무효화하지 않는 선으로 제한.

---

### 3.3. 판매가 · 수리비 · 구매가 (경제 공식)
- **판매가** = `기본가 + (인스턴스보너스 × weightOf)`.
  - `기본가`: `buyPrice` 있으면 `round(buyPrice × 0.5)`, 없으면(드랍 전용) `Σ(카탈로그 amount × weightOf(target))`.
  - **`weightOf(target)`: CRITICAL = 1, 그 외(STR/DEX/INT/DEF/HP/MP/STAMINA) = 10.**
- **구매가(`buyPrice`)**: 사용자가 수기 지정 (없으면 드랍 전용).
- **수리비(1포인트당) = 판매가** 그대로 (가치 기반 골드 싱크).

---

### 3.4. 몬스터 규칙
- **`attackPower` > 그 구간 플레이어 유효 DEF** (1뎀 방지, 통상 유효 DEF의 2~2.5배).
- **`maxHp`** $\approx$ 플레이어 1타의 4~6배 (일반 3~5턴 처치 기준).
- **방어 상수**: 기본 **100% 완전 방어 (0 피격) / 0 반격** (스매시/강공격 관통).

---

### 3.5. 스킬 규칙
- **랭크 맵 16키(F→MASTER) 완비 + 단조성 필수**.
- **배율 밴드**(1히트당):
  - NORMAL 단일: F 90% $\rightarrow$ MASTER 170%
  - HEAVY 단일: F 130% $\rightarrow$ MASTER 250% (활 최상위 F 140% $\rightarrow$ 260%)
  - 다단(히트당): 3타 F 35%$\rightarrow$65%(총 105%$\rightarrow$195%), 4타 F 27%$\rightarrow$50%(총 108%$\rightarrow$200%)
- **상한**: `critBonus` $\le$ +100(+10%p, 마법은 0), 카운터 어택 전용 `critBonus` $\le$ +200, 반격율 $\le$ 200%.

---

## 4. 완료 체크리스트 (JSON 파일 반영 전 필수 확인)

- [ ] baseline을 **대상 JSON에서 직접 읽어** 비교·제안했는가?
- [ ] 무기: 주스탯↔재능 일치, 같은 kind 내 강함 단조성, 양손=방패불가, 활만 CRITICAL?
- [ ] 방어구: DEF가 몬스터 attackPower를 완전 무효화하지 않는가?
- [ ] 판매가/수리비: `weightOf`(CRITICAL=1) 적용, buyPrice×0.5 배타, 수리비=판매가 확인?
- [ ] 몬스터: attackPower가 유효 DEF보다 위(1뎀 방지)? maxHp가 목표 처치 턴에 부합? critical 0.1% 단위?
- [ ] 스킬: 랭크 16키(F~MASTER) 완비 + 단조성? 배율 밴드 준수? critBonus 상한 준수?
- [ ] **파이썬 검증 스크립트 실행**: `verify_equipment.py` / `verify_monster.py` / `verify_skill.py` `--json` 실행 및 경고 없음 확인?
- [ ] **빌드 및 카탈로그 테스트**: `mvn test -pl myrpg` 실행으로 파싱 및 단조 테스트 통과 확인?
