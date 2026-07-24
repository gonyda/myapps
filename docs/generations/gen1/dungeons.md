# 세대1 던전 (dungeons.json)

세대1 던전 3종. 모두 5스테이지 고정 (1~4 일반몹, 5 보스), 세대1 장비 전체를 드랍.
던전 구조·체크포인트는 `systems/dungeon.md`, 파밍 모델은 `systems/farming.md` 참고.

- `floorCount`: 총 스테이지 수 (모든 던전 5 고정)
- `requiredLevel`: 권장 레벨 (= 드랍 아이템 itemLevel)
- `bossId`: 보스 몬스터 id (monsters.json 참조)
- `generation`: 장비 세대 (세대1 = 1)
- `weaponTypes[]` / `armorSlots[]`: 드랍 가능 장비 (세대1 전체 = 무기 6 + 방어구 4)
- `gradeChance`: 등급별 드랍 확률 (합 = 1.0)
- `treasureBaseGold`: 보물상자 골드 보상 기준값 (실제 지급 = `반올림(treasureBaseGold × (1 + 0.05 × requiredLevel))`, `systems/dungeon.md`)
- `monsters[]`: 스테이지별 등장 몬스터 (monsterId, minFloor~maxFloor, spawnWeight)

## 목록

| id | 이름 | 난이도 | 스테이지 | 권장레벨 | 보스 | 보물 기준골드 |
|----|------|--------|---------|---------|------|--------------|
| 1 | 숲 던전 | ★☆☆☆☆ | 5 | 1 | 고대 트렌트 | 30 |
| 2 | 광산 던전 | ★★★☆☆ | 5 | 5 | 광산왕 | 60 |
| 3 | 탑 던전 | ★★★★☆ | 5 | 10 | 흑마도사 | 100 |

## 등급 확률 (gradeChance)

| 던전(권장Lv) | COMMON | UNCOMMON | RARE | EPIC | LEGENDARY |
|------|------|------|------|------|------|
| 숲(1) | 0.700 | 0.220 | 0.060 | 0.018 | 0.002 |
| 광산(5) | 0.550 | 0.280 | 0.120 | 0.040 | 0.010 |
| 탑(10) | 0.400 | 0.300 | 0.180 | 0.090 | 0.030 |

## 등장 몬스터 구성

| 던전 | 스테이지 | 등장 몬스터 (가중치) |
|------|---------|--------------------|
| 숲 던전 | 1~4 | 고블린(5), 들늑대(3) |
| 숲 던전 | 5 | 보스: 고대 트렌트 |
| 광산 던전 | 1~4 | 동굴 박쥐(5), 바위 골렘(2) |
| 광산 던전 | 5 | 보스: 광산왕 |
| 탑 던전 | 1~4 | 견습 마법사(5), 가고일(2) |
| 탑 던전 | 5 | 보스: 흑마도사 |

## JSON

```json
[
  {
    "id": 1,
    "name": "숲 던전",
    "difficulty": 1,
    "floorCount": 5,
    "requiredLevel": 1,
    "bossId": 3,
    "generation": 1,
    "weaponTypes": ["SWORD", "DAGGER", "AXE", "BOW", "SPEAR", "STAFF"],
    "armorSlots": ["HELMET", "CHEST", "GLOVES", "BOOTS"],
    "gradeChance": { "COMMON": 0.700, "UNCOMMON": 0.220, "RARE": 0.060, "EPIC": 0.018, "LEGENDARY": 0.002 },
    "treasureBaseGold": 30,
    "monsters": [
      { "monsterId": 1, "minFloor": 1, "maxFloor": 4, "spawnWeight": 5 },
      { "monsterId": 2, "minFloor": 1, "maxFloor": 4, "spawnWeight": 3 }
    ]
  },
  {
    "id": 2,
    "name": "광산 던전",
    "difficulty": 3,
    "floorCount": 5,
    "requiredLevel": 5,
    "bossId": 6,
    "generation": 1,
    "weaponTypes": ["SWORD", "DAGGER", "AXE", "BOW", "SPEAR", "STAFF"],
    "armorSlots": ["HELMET", "CHEST", "GLOVES", "BOOTS"],
    "gradeChance": { "COMMON": 0.550, "UNCOMMON": 0.280, "RARE": 0.120, "EPIC": 0.040, "LEGENDARY": 0.010 },
    "treasureBaseGold": 60,
    "monsters": [
      { "monsterId": 4, "minFloor": 1, "maxFloor": 4, "spawnWeight": 5 },
      { "monsterId": 5, "minFloor": 1, "maxFloor": 4, "spawnWeight": 2 }
    ]
  },
  {
    "id": 3,
    "name": "탑 던전",
    "difficulty": 4,
    "floorCount": 5,
    "requiredLevel": 10,
    "bossId": 9,
    "generation": 1,
    "weaponTypes": ["SWORD", "DAGGER", "AXE", "BOW", "SPEAR", "STAFF"],
    "armorSlots": ["HELMET", "CHEST", "GLOVES", "BOOTS"],
    "gradeChance": { "COMMON": 0.400, "UNCOMMON": 0.300, "RARE": 0.180, "EPIC": 0.090, "LEGENDARY": 0.030 },
    "treasureBaseGold": 100,
    "monsters": [
      { "monsterId": 7, "minFloor": 1, "maxFloor": 4, "spawnWeight": 5 },
      { "monsterId": 8, "minFloor": 1, "maxFloor": 4, "spawnWeight": 2 }
    ]
  }
]
```
