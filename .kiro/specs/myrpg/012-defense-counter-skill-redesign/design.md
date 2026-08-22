# 012 디펜스 및 카운터 어택 스킬 재설계 (Design)

## 1. 아키텍처 및 시스템 변경 개요

```
[클라이언트/전투 화면]
      │ (스킬 선택: defense / counter_attack / slash / smash)
      ▼
[BattleService]
      │
      ├── 1. 자원 소모 (Skill.resourceCostByRank or fixed cost)
      │      - defense: 랭크별 5 -> 1
      │      - counter_attack: 8 고정
      │
      ├── 2. 전투 해결 (BattleResolver)
      │      - 디펜스 vs 일반공격 -> 양측 0 피해 (완전 차단)
      │      - 일반공격 vs 디펜스 -> 양측 0 피해 (칼 튕김)
      │      - 카운터 vs 일반/강공격 -> 플레이어 0 피해, 몬스터 (monster.attackPower * counterMultiplier - def)
      │      - 카운터 vs 디펜스 -> 양측 0 피해 (헛방)
      │      - 강공격 vs 디펜스 -> 몬스터 130% 풀 관통 피해
      │
      └── 3. 랭크업 스탯 보너스 (SkillRankupBonus)
             - defense: 랭크당 DEF +1, HP +5
             - counter_attack: 영구 보너스 없음 (0)
```

---

## 2. 데이터 모델 및 스키마 변경

### 2.1. `skill.json` 데이터 갱신

#### 🛡️ `defense` 스킬 정의
```json
{
  "id": "defense",
  "label": "디펜스",
  "type": "DEFENSE",
  "talent": "COMMON",
  "resourceCost": 5,
  "resourceCostByRank": {
    "F": 5, "E": 5, "D": 4, "C": 4, "B": 4, "A": 3,
    "R9": 3, "R8": 3, "R7": 2, "R6": 2, "R5": 2, "R4": 1,
    "R3": 1, "R2": 1, "R1": 1, "MASTER": 1
  },
  "blockRateByRank": {
    "F": 100, "E": 100, "D": 100, "C": 100, "B": 100, "A": 100,
    "R9": 100, "R8": 100, "R7": 100, "R6": 100, "R5": 100, "R4": 100,
    "R3": 100, "R2": 100, "R1": 100, "MASTER": 100
  },
  "counterMultiplierByRank": {
    "F": 0, "E": 0, "D": 0, "C": 0, "B": 0, "A": 0,
    "R9": 0, "R8": 0, "R7": 0, "R6": 0, "R5": 0, "R4": 0,
    "R3": 0, "R2": 0, "R1": 0, "MASTER": 0
  },
  "description": "방패나 무기로 적의 일반 공격을 100% 완벽하게 막아내어 피해를 전혀 입지 않는다. 반격 공격은 하지 않지만, 랭크가 오를수록 자원 소모가 줄어들고 영구적인 방어력과 생명력이 크게 증가한다. 강공격(스매시)에는 관통당한다."
}
```

#### ⚡ `counter_attack` 스킬 정의
```json
{
  "id": "counter_attack",
  "label": "카운터 어택",
  "type": "DEFENSE",
  "talent": "COMMON",
  "resourceCost": 8,
  "critBonus": 0,
  "critBonusByRank": {
    "F": 0, "E": 10, "D": 20, "C": 30, "B": 40, "A": 50,
    "R9": 70, "R8": 90, "R7": 110, "R6": 130, "R5": 150, "R4": 160,
    "R3": 170, "R2": 180, "R1": 190, "MASTER": 200
  },
  "blockRateByRank": {
    "F": 100, "E": 100, "D": 100, "C": 100, "B": 100, "A": 100,
    "R9": 100, "R8": 100, "R7": 100, "R6": 100, "R5": 100, "R4": 100,
    "R3": 100, "R2": 100, "R1": 100, "MASTER": 100
  },
  "counterMultiplierByRank": {
    "F": 100, "E": 105, "D": 110, "C": 118, "B": 126, "A": 135,
    "R9": 145, "R8": 155, "R7": 165, "R6": 175, "R5": 182, "R4": 188,
    "R3": 192, "R2": 195, "R1": 198, "MASTER": 200
  },
  "description": "적의 공격을 완벽히 흘려내며 적의 공격력에 비례한 치명적인 반격 일격을 가한다. 랭크가 오를수록 반격 배율과 크리티컬 확률이 증가한다. 적이 공격하지 않으면 자원만 소모된다."
}
```

---

## 3. 핵심 비즈니스 로직 상세 설계

### 3.1. `BattleResolver` 및 `BattleService` 전투 판정
- `TurnInput`에 `isCounterAttack` 여부 전달 (`"counter_attack".equals(skill.id())`).
- **방어 vs 일반 (`resolveDefenseWinsNormal`)**:
  - `blockRate == 100` ➡️ 플레이어 피격 `monsterDmg = 0`.
  - `counterPercent == 0` ➡️ 몬스터 피격 `counterDamage = 0`.
- **일반 vs 방어 (`resolveNormalLosesToDefense`)**:
  - 몬스터 `blockRate == 100` ➡️ 플레이어 공격 `totalDamage = 0`.
  - 몬스터 `counterPercent == 0` ➡️ 플레이어 피격 `counterDamage = 0`.
- **카운터 어택 vs 공격 (일반 or 스매시)**:
  - 플레이어 피격 = `0`.
  - 반격 데미지 = `floor(monster.attackPower() * counterPercent / 100) - monster.defense()`.
  - 반격 크리티컬 = 스킬 `critBonusByRank` + 캐릭터 크리티컬 적용 (×1.5배).
- **카운터 어택 vs 디펜스**:
  - 양측 `0` 피해 (교착/헛방).

### 3.2. `SkillRankupBonus` 및 HP 영구 스탯 연동
- `SkillRankupBonus`에서 스킬별 영구 보너스 매핑:
  - `defense`: `rank.order() * DEF(+1)`, `rank.order() * HP(+5)`.
  - `counter_attack`: `Stats.ZERO` (영구 스탯 없음).
  - 그 외: 기존 재능별 단일 주스탯(+1) 유지.
- `CharacterProgress` / `PlayScreenViewHelper` / `BattleService` / `HealController` / `ProgressionService`:
  - `vitalMax` 산출 시 `skillBonus.hp()` 가산.
  - 랭크업 시에는 최대 HP만 늘어나고 `hpCurrent`는 그대로 유지 (포션/휴식 회복).

### 3.3. `BattleLogFormatter` 로그 연출
- 디펜스 완전 방어: `디펜스(방어)로 공격을 완벽히 방어했다!`
- 디펜스 공격 막힘: `상대의 방어에 공격이 가로막혔다!`
- 카운터 어택 성공: `카운터 어택(방어)으로 적의 공격을 흘려내며 치명적인 반격! (X 피해)`
- 카운터 어택 헛방: `적이 공격하지 않아 카운터 어택이 빗나갔다!`

### 3.4. 밸런스 검증 도구 (`tools/balance/verify_skill.py`) 및 규칙 문서
- `COUNTER_CAP` = 200 (반격 상한 200%)
- `CRITBONUS_CAP` = 200 (크리티컬 보너스 상한 +20%p)
- `rules/myrpg/data-balance-guide.md` §B-5 및 §C-5 갱신
