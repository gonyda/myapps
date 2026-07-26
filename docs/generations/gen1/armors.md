# 세대1 방어구 (armors.json)

방어구 **템플릿** 4종 (부위별 1개). 등급·랜덤 능력치는 드랍 시 롤된다.
파워 모델·판매가 공식은 각각 `systems/items-grades.md`, `systems/shop.md` 참고.

- `armorSlot`: HELMET(투구) / CHEST(갑옷) / GLOVES(장갑) / BOOTS(신발)
- `baseDefense`: 기준 방어력 (실제 값은 유효 파워 레벨로 스케일 — 무기 `baseAttack`과 동일 공식)
- `baseValue`: 기본가치 (상점 판매가 계산 기준값)

## 목록

| id | 이름 | 부위 | 기준 방어력 | 기본가치 |
|----|------|------|-----------|---------|
| 1 | 가죽 투구 | HELMET | 3 | 15 |
| 2 | 천 장갑 | GLOVES | 2 | 15 |
| 3 | 강철 갑옷 | CHEST | 5 | 15 |
| 4 | 민첩의 신발 | BOOTS | 2 | 15 |

> 4부위 합산 기준 방어력 = 12. 무기 평균 기준 공격력(~10)과 유사한 수준으로 설계.

## JSON (템플릿)

```json
[
  { "id": 1, "name": "가죽 투구",   "armorSlot": "HELMET", "baseDefense": 3, "baseValue": 15 },
  { "id": 2, "name": "천 장갑",     "armorSlot": "GLOVES", "baseDefense": 2, "baseValue": 15 },
  { "id": 3, "name": "강철 갑옷",   "armorSlot": "CHEST",  "baseDefense": 5, "baseValue": 15 },
  { "id": 4, "name": "민첩의 신발", "armorSlot": "BOOTS",  "baseDefense": 2, "baseValue": 15 }
]
```

## 기본 방어력 스케일 공식

무기 `baseAttack`과 동일:
- **기본방어력(실제) = 반올림(baseDefense × (1 + 0.15 × P))**
- P = itemLevel + 등급 레벨 보너스

### 기본방어력 스케일 예시 (강철 갑옷, 기준 5)

| 드랍 던전 | 등급 | 유효레벨 P | 기본방어력 |
|-----------|------|-----------|-----------|
| 숲(1) | COMMON | 1 | 6 |
| 숲(1) | LEGENDARY | 11 | 13 |
| 탑(10) | COMMON | 10 | 13 |
| 탑(10) | LEGENDARY | 20 | 20 |

## 드랍 인스턴스 예시

```
[일반] 가죽 투구   (숲, itemLevel 1, COMMON → P=1)
  기본방어력 3 / 능력치 1개 (수치 1~1)  · 예) 체력 +1

[희귀] 강철 갑옷   (탑, itemLevel 10, RARE → P=15)
  기본방어력 16 / 능력치 2~3개 (수치 6~12)  · 예) 방어력 +11, 체력 +9, 속도 +7

[전설] 민첩의 신발 (탑, itemLevel 10, LEGENDARY → P=20)
  기본방어력 8 / 능력치 4~5개 (수치 8~16)  · 예) 속도 +15, 치명타 +12, 공격력 +11, 방어력 +13
```
