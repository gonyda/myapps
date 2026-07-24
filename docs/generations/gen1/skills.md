# 세대1 스킬 (skills.json)

무기 타입(6종)별로 1개씩, 총 6개. 6종 모두 보스 드랍으로 획득 가능.
스킬 시스템 규칙(장착·소멸 등)은 `systems/weapons-skills.md`, 데미지 공식은 `systems/combat.md` 참고.

- `weaponType`: 장착 가능한 무기 종류
- `damageType`: PHYSICAL(물리) / MAGICAL(마법)
- `damageMultiplier`: 데미지 배율 (기본공격 = 1.0)
- `mpCost`: 사용 시 MP 소모량
- 쿨다운·상태이상 없음

## 목록

| id | 이름 | 필요 무기 | 데미지 타입 | 배율 | MP |
|----|------|----------|------------|------|-----|
| 1 | 강타 | SWORD | 물리 | 1.5 | 15 |
| 2 | 회전베기 | AXE | 물리 | 1.8 | 20 |
| 3 | 연속찌르기 | DAGGER | 물리 | 1.3 | 10 |
| 4 | 파이어볼 | STAFF | 마법 | 1.9 | 20 |
| 5 | 관통사격 | BOW | 물리 | 1.6 | 18 |
| 6 | 창돌진 | SPEAR | 물리 | 1.7 | 18 |

## JSON

```json
[
  { "id": 1, "name": "강타",       "weaponType": "SWORD",  "damageType": "PHYSICAL", "damageMultiplier": 1.5, "mpCost": 15 },
  { "id": 2, "name": "회전베기",   "weaponType": "AXE",    "damageType": "PHYSICAL", "damageMultiplier": 1.8, "mpCost": 20 },
  { "id": 3, "name": "연속찌르기", "weaponType": "DAGGER", "damageType": "PHYSICAL", "damageMultiplier": 1.3, "mpCost": 10 },
  { "id": 4, "name": "파이어볼",   "weaponType": "STAFF",  "damageType": "MAGICAL",  "damageMultiplier": 1.9, "mpCost": 20 },
  { "id": 5, "name": "관통사격",   "weaponType": "BOW",    "damageType": "PHYSICAL", "damageMultiplier": 1.6, "mpCost": 18 },
  { "id": 6, "name": "창돌진",     "weaponType": "SPEAR",  "damageType": "PHYSICAL", "damageMultiplier": 1.7, "mpCost": 18 }
]
```
