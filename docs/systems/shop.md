# 상점 시스템

세대 무관 공통 규칙. 마을 상점은 **전리품 판매**와 **HP/MP 포션 구매**를 제공한다. (장비 구매는 제공하지 않음)

## 1) 전리품 판매

보유한 무기·방어구 인스턴스를 골드로 되판다.

- **판매 대상**: 보유 무기·방어구 인스턴스 (스킬북·포션은 판매 불가)
- **착용 중 장비는 판매 불가** — 장비 메뉴에서 해제한 뒤에만 판매 가능
- 판매 시 해당 인스턴스는 **영구 삭제**되고 골드가 즉시 지급됨 (되사기 없음)

### 판매가 공식 (기본가치 × 등급 동적 배수)

장비마다 **기본가치(baseValue)**가 템플릿에 정의되어 있고, **등급과 아이템레벨에 따라 판매가가 동적으로 변한다**.

```
판매가 = 반올림( baseValue × 등급배수 × (1 + 0.05 × itemLevel) )
```

| 등급 | 등급배수 |
|------|---------|
| COMMON | 1.0 |
| UNCOMMON | 1.6 |
| RARE | 3.0 |
| EPIC | 6.0 |
| LEGENDARY | 12.0 |

- **baseValue**: 템플릿 고정값 (`generations/gen1/weapons.md`, `armors.md`에 정의)
- **등급배수**: 드랍 시 롤된 등급이 클수록 가치 급상승 (전설 = 일반의 12배)
- **itemLevel 보정**: 상위 던전 드랍(높은 itemLevel)일수록 같은 등급이라도 더 비싸다 — 파워 모델과 방향성 일치

### 판매가 예시

| 아이템 | baseValue | 등급 | itemLevel | 판매가 |
|--------|-----------|------|-----------|--------|
| [일반] 낡은 검 (숲) | 20 | COMMON | 1 | 21 |
| [전설] 낡은 검 (숲) | 20 | LEGENDARY | 1 | 252 |
| [희귀] 강철 갑옷 (탑) | 15 | RARE | 10 | 68 |
| [전설] 강철 갑옷 (탑) | 15 | LEGENDARY | 10 | 270 |

---

## 2) HP / MP 포션 구매

초기 상점은 회복 포션 2종만 판매한다. (포션은 세대 무관 공통 소모품)

| id | 아이템 | 효과 | 회복량 | 구매가 |
|----|--------|------|--------|--------|
| 1 | HP 포션 | HP 회복 | 50 | 30 Gold |
| 2 | MP 포션 | MP 회복 | 30 | 25 Gold |

- 구매 수량 제한 없음 (보유 골드 한도 내)
- 구매한 포션은 인벤토리(`rpg_player_inventory`, `item_type = POTION`)에 수량으로 누적
- **사용 시점**: 전투 중 `아이템 사용` 행동으로 소모 (전투 행동 1회 = 1턴). 회복량은 최대치를 초과하지 않음
- **판매 불가**: 소모품은 상점에 되팔 수 없음 (전리품 판매 대상은 무기·방어구만)

### items.json (마스터 데이터)

```json
[
  { "id": 1, "name": "HP 포션", "itemType": "POTION", "effectType": "HEAL_HP", "effectAmount": 50, "buyPrice": 30 },
  { "id": 2, "name": "MP 포션", "itemType": "POTION", "effectType": "HEAL_MP", "effectAmount": 30, "buyPrice": 25 }
]
```

- `itemType`: 소모품 종류 (현재 `POTION`만)
- `effectType`: `HEAL_HP`(HP 회복) / `HEAL_MP`(MP 회복)
- `effectAmount`: 회복량
- `buyPrice`: 상점 구매가 (Gold)
- `rpg_player_inventory.item_ref_id`가 이 id를 참조 (`systems/persistence.md`)
