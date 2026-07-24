# 세대1 방어구 (armors.json)

방어구 **템플릿** 4종 (부위별 1개). 등급·랜덤 능력치는 드랍 시 롤된다.
파워 모델·판매가 공식은 각각 `systems/items-grades.md`, `systems/shop.md` 참고.

- `armorSlot`: HELMET(투구) / CHEST(갑옷) / GLOVES(장갑) / BOOTS(신발)
- `baseValue`: 기본가치 (상점 판매가 계산 기준값)
- 방어구는 기본 스탯이 없고, 모든 성능은 랜덤 능력치로만 부여된다.

## 목록

| id | 이름 | 부위 | 기본가치 |
|----|------|------|---------|
| 1 | 가죽 투구 | HELMET | 15 |
| 2 | 천 장갑 | GLOVES | 15 |
| 3 | 강철 갑옷 | CHEST | 15 |
| 4 | 민첩의 신발 | BOOTS | 15 |

## JSON (템플릿)

```json
[
  { "id": 1, "name": "가죽 투구",   "armorSlot": "HELMET", "baseValue": 15 },
  { "id": 2, "name": "천 장갑",     "armorSlot": "GLOVES", "baseValue": 15 },
  { "id": 3, "name": "강철 갑옷",   "armorSlot": "CHEST",  "baseValue": 15 },
  { "id": 4, "name": "민첩의 신발", "armorSlot": "BOOTS",  "baseValue": 15 }
]
```

## 드랍 인스턴스 예시

```
[일반] 가죽 투구   (숲, itemLevel 1, COMMON → P=1)
  능력치 1개 (수치 1~1)  · 예) 방어력 +1

[희귀] 강철 갑옷   (탑, itemLevel 10, RARE → P=15)
  능력치 2~3개 (수치 6~12)  · 예) 방어력 +11, 체력 +9, 속도 +7

[전설] 민첩의 신발 (탑, itemLevel 10, LEGENDARY → P=20)
  능력치 4~5개 (수치 8~16)  · 예) 속도 +15, 치명타 +12, 공격력 +11, 방어력 +13
```
