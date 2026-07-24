# 세대1 무기 (weapons.json)

무기 **템플릿** 6종 (타입별 1개). 등급·스킬슬롯·실제 기본공격력·랜덤 능력치는 드랍 시 롤된다.
파워 모델·판매가 공식은 각각 `systems/items-grades.md`, `systems/shop.md` 참고.

- `weaponType`: SWORD / AXE / SPEAR / DAGGER / STAFF / BOW
- `baseAttack`: 기준 공격력 (실제 값은 유효 파워 레벨로 스케일)
- `baseSpeed` / `baseCritical`: 무기 타입 고유 속도·치명타 보너스 (**고정값**, 착용 시 캐릭터 스탯에 합산 — `systems/combat.md`)
- `baseValue`: 기본가치 (상점 판매가 계산 기준값)

## 목록

| id | 이름 | 종류 | 기준 공격력 | 속도 | 치명타 | 기본가치 | 특성 |
|----|------|------|------------|-----:|------:|---------|------|
| 1 | 낡은 검 | SWORD | 10 | +2 | +2 | 20 | 밸런스 |
| 2 | 녹슨 단검 | DAGGER | 8 | +6 | +8 | 16 | 저공격 대신 속도·치명 최고 |
| 3 | 사냥용 창 | SPEAR | 11 | +3 | +1 | 22 | 준수한 공격 + 약간 빠름 |
| 4 | 강철 도끼 | AXE | 13 | −3 | 0 | 26 | 최고 공격력, 속도 페널티(후공 잦음) |
| 5 | 사냥꾼의 활 | BOW | 10 | +5 | +4 | 20 | 원거리 = 선공·치명 유리 |
| 6 | 견습생의 지팡이 | STAFF | 11 | +1 | +1 | 22 | 마법(방어 관통)이 강점 |

> `baseSpeed`/`baseCritical`은 유효 파워 레벨로 스케일되지 않는 **고정값**이다 (무기 타입 개성 유지). 수치 성장은 랜덤 능력치가 담당한다.

## JSON (템플릿)

```json
[
  { "id": 1, "name": "낡은 검",         "weaponType": "SWORD",  "baseAttack": 10, "baseSpeed": 2,  "baseCritical": 2, "baseValue": 20 },
  { "id": 2, "name": "녹슨 단검",       "weaponType": "DAGGER", "baseAttack": 8,  "baseSpeed": 6,  "baseCritical": 8, "baseValue": 16 },
  { "id": 3, "name": "사냥용 창",       "weaponType": "SPEAR",  "baseAttack": 11, "baseSpeed": 3,  "baseCritical": 1, "baseValue": 22 },
  { "id": 4, "name": "강철 도끼",       "weaponType": "AXE",    "baseAttack": 13, "baseSpeed": -3, "baseCritical": 0, "baseValue": 26 },
  { "id": 5, "name": "사냥꾼의 활",     "weaponType": "BOW",    "baseAttack": 10, "baseSpeed": 5,  "baseCritical": 4, "baseValue": 20 },
  { "id": 6, "name": "견습생의 지팡이", "weaponType": "STAFF",  "baseAttack": 11, "baseSpeed": 1,  "baseCritical": 1, "baseValue": 22 }
]
```

## 드랍 인스턴스 예시

같은 "낡은 검"이라도 드랍 던전(itemLevel)·등급에 따라 다르게 롤된다. (P = itemLevel + 등급보너스)
아래 랜덤 능력치와 별개로, 무기 타입 고정값(낡은 검 = 속도 +2 / 치명타 +2)은 등급·레벨과 무관하게 항상 적용된다.

```
[일반] 낡은 검   (숲, itemLevel 1, COMMON → P=1)
  기본공격력 12 / 슬롯 1 / 능력치 1개 (수치 1~1)  · 예) 공격력 +1

[희귀] 낡은 검   (탑, itemLevel 10, RARE → P=15)
  기본공격력 33 / 슬롯 3 / 능력치 2~3개 (수치 6~12)  · 예) 공격력 +10, 치명타 +8

[전설] 낡은 검   (숲, itemLevel 1, LEGENDARY → P=11)
  기본공격력 27 / 슬롯 5 / 능력치 4~5개 (수치 4~9)  · 예) 공격력 +9, 치명타 +7, 속도 +6, 체력 +8
```

### 기본공격력 스케일 예시 (낡은 검, 기준 10)

| 드랍 던전 | 등급 | 유효레벨 P | 기본공격력 |
|-----------|------|-----------|-----------|
| 숲(1) | COMMON | 1 | 12 |
| 숲(1) | LEGENDARY | 11 | 27 |
| 탑(10) | COMMON | 10 | 25 |
| 탑(10) | LEGENDARY | 20 | 40 |
