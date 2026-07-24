# 세대1 몬스터 (monsters.json)

일반 몬스터 6종 + 보스 3종, 총 9종. 드랍 판정 규칙은 `systems/farming.md` 참고.

- 몬스터는 드랍 **카테고리**(ARMOR/WEAPON/SKILL_BOOK)와 확률만 정의한다
- 실제 아이템 종류·등급은 던전 설정(`weaponTypes` / `gradeChance`)으로 롤된다
- 속성 상성 없음 (물리/마법 데미지 공식만 적용, `systems/combat.md`)
- **`damageType`**: 몬스터 기본공격의 데미지 타입. 마법사류(견습 마법사·흑마도사)는 `MAGICAL`(방어 계수 0.2)로 플레이어 방어력을 덜 받는다. 나머지는 `PHYSICAL`(방어 계수 0.5)

## 목록

| id | 이름 | HP | 공격력 | 방어력 | 속도 | 데미지타입 | 경험치 | 골드 | 보스 | 던전 |
|----|------|-----|-------|-------|------|-----------|-------|------|------|------|
| 1 | 고블린 | 50 | 12 | 5 | 8 | 물리 | 20 | 10 | ✗ | 숲 |
| 2 | 들늑대 | 65 | 15 | 4 | 14 | 물리 | 28 | 12 | ✗ | 숲 |
| 3 | 고대 트렌트 | 150 | 20 | 18 | 6 | 물리 | 150 | 80 | ✓ | 숲 |
| 4 | 동굴 박쥐 | 80 | 20 | 6 | 18 | 물리 | 48 | 22 | ✗ | 광산 |
| 5 | 바위 골렘 | 160 | 26 | 22 | 4 | 물리 | 95 | 52 | ✗ | 광산 |
| 6 | 광산왕 | 340 | 36 | 26 | 10 | 물리 | 300 | 170 | ✓ | 광산 |
| 7 | 견습 마법사 | 130 | 30 | 10 | 13 | 마법 | 120 | 48 | ✗ | 탑 |
| 8 | 가고일 | 210 | 36 | 25 | 11 | 물리 | 150 | 65 | ✗ | 탑 |
| 9 | 흑마도사 | 500 | 44 | 30 | 16 | 마법 | 480 | 320 | ✓ | 탑 |

## 드랍 카테고리 (드랍률은 systems/farming.md 고정값)

| 유형 | 방어구 | 무기 | 스킬북 |
|------|--------|------|--------|
| 일반몹 (1,2,4,5,7,8) | 5% | — | — |
| 보스몹 (3,6,9) | — | 15% | 15% |

> 실제로 어떤 무기 종류·등급이 나올지는 해당 던전의 `weaponTypes` / `gradeChance`로 결정된다.
> 세대1 던전(숲·광산·탑)은 모두 6종 무기·4부위 방어구 전체를 드랍하며, 등급 확률(과 itemLevel)만 다르다.

## JSON

```json
[
  { "id": 1, "name": "고블린",      "hp": 50,  "attack": 12, "defense": 5,  "speed": 8,  "damageType": "PHYSICAL", "expReward": 20,  "goldReward": 10,  "boss": false, "drops": [ { "category": "ARMOR", "rate": 0.05 } ] },
  { "id": 2, "name": "들늑대",      "hp": 65,  "attack": 15, "defense": 4,  "speed": 14, "damageType": "PHYSICAL", "expReward": 28,  "goldReward": 12,  "boss": false, "drops": [ { "category": "ARMOR", "rate": 0.05 } ] },
  { "id": 3, "name": "고대 트렌트",  "hp": 150, "attack": 20, "defense": 18, "speed": 6,  "damageType": "PHYSICAL", "expReward": 150, "goldReward": 80,  "boss": true,  "drops": [ { "category": "WEAPON", "rate": 0.15 }, { "category": "SKILL_BOOK", "rate": 0.15 } ] },
  { "id": 4, "name": "동굴 박쥐",    "hp": 80,  "attack": 20, "defense": 6,  "speed": 18, "damageType": "PHYSICAL", "expReward": 48,  "goldReward": 22,  "boss": false, "drops": [ { "category": "ARMOR", "rate": 0.05 } ] },
  { "id": 5, "name": "바위 골렘",    "hp": 160, "attack": 26, "defense": 22, "speed": 4,  "damageType": "PHYSICAL", "expReward": 95,  "goldReward": 52,  "boss": false, "drops": [ { "category": "ARMOR", "rate": 0.05 } ] },
  { "id": 6, "name": "광산왕",      "hp": 340, "attack": 36, "defense": 26, "speed": 10, "damageType": "PHYSICAL", "expReward": 300, "goldReward": 170, "boss": true,  "drops": [ { "category": "WEAPON", "rate": 0.15 }, { "category": "SKILL_BOOK", "rate": 0.15 } ] },
  { "id": 7, "name": "견습 마법사",  "hp": 130, "attack": 30, "defense": 10, "speed": 13, "damageType": "MAGICAL",  "expReward": 120, "goldReward": 48,  "boss": false, "drops": [ { "category": "ARMOR", "rate": 0.05 } ] },
  { "id": 8, "name": "가고일",      "hp": 210, "attack": 36, "defense": 25, "speed": 11, "damageType": "PHYSICAL", "expReward": 150, "goldReward": 65,  "boss": false, "drops": [ { "category": "ARMOR", "rate": 0.05 } ] },
  { "id": 9, "name": "흑마도사",    "hp": 500, "attack": 44, "defense": 30, "speed": 16, "damageType": "MAGICAL",  "expReward": 480, "goldReward": 320, "boss": true,  "drops": [ { "category": "WEAPON", "rate": 0.15 }, { "category": "SKILL_BOOK", "rate": 0.15 } ] }
]
```
