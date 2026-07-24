# 데이터 & 영속화

세대 무관 공통 규칙. 마스터 데이터(JSON)와 플레이어 데이터(DB)를 분리 관리한다.

## 데이터 관리 전략

- **JSON 파일**: 마스터 데이터 (게임 콘텐츠 정의) → 코드 수정 없이 콘텐츠 추가 가능
- **DB 테이블**: 플레이어 데이터만 (캐릭터 상태, 보유 아이템, 진행도) → 플레이하면서 변하는 것

---

## JSON 파일 구조 (마스터 데이터)

세대별 콘텐츠는 세대 폴더에 정의하고, 런타임 리소스로 통합 배치한다.

```
resources/data/
├── monsters.json       # 몬스터 정의 + 드랍 카테고리
├── skills.json         # 스킬 정의
├── weapons.json        # 무기 템플릿 (baseAttack + baseValue)
├── armors.json         # 방어구 템플릿 (baseValue)
├── items.json          # 소모품 정의 (HP/MP 포션) — systems/shop.md
└── dungeons.json       # 던전 정의 + 등장 몬스터 + gradeChance
```

- 세대1 실제 JSON 내용은 `generations/gen1/` 각 문서에 수록
- JSON의 ID를 DB에서 `template_id` / `item_ref_id`로 참조 (FK 제약 없음, 애플리케이션 레벨 매핑)

---

## DB 테이블 구조 (플레이어 데이터)

> **테이블 명명 규칙**: 모든 플레이어 데이터 테이블은 `rpg_` 접두사를 사용한다(다른 모듈 테이블과 구분·조회 편의). JPA 엔티티 클래스명은 접두사 없이 깔끔하게 두고(`Player`, `PlayerWeapon` 등), `@Table(name = "rpg_player")`처럼 매핑으로 접두사를 부여한다. 컬럼명(FK 포함)은 접두사 없이 유지한다.

### rpg_player (캐릭터 기본)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR | 캐릭터명 |
| level | INT | 레벨 |
| exp | INT | 현재 경험치 |
| hp / max_hp | INT | 현재/최대 HP |
| mp / max_mp | INT | 현재/최대 MP |
| attack | INT | 기본 공격력 |
| defense | INT | 기본 방어력 |
| speed | INT | 기본 속도 |
| critical | INT | 기본 치명타 |
| gold | INT | 보유 골드 |

### rpg_player_weapon (보유 무기 인스턴스)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| player_id | BIGINT FK | 소유자 |
| weapon_template_id | BIGINT | JSON 무기 ID 참조 |
| grade | VARCHAR | 드랍 시 롤된 등급 (COMMON~LEGENDARY) |
| item_level | INT | 드랍 던전 권장레벨 (유효 파워 레벨 계산용) |
| base_attack | INT | 롤된 기본공격력 (유효 파워 레벨로 스케일) |
| base_speed | INT | 무기 타입 고유 속도 보너스 (템플릿 고정값 복사) |
| base_critical | INT | 무기 타입 고유 치명타 보너스 (템플릿 고정값 복사) |
| skill_slots | INT | 등급에서 파생된 스킬슬롯 수 |
| is_equipped | BOOLEAN | 착용 여부 |

> **착용 불변식**: 한 플레이어는 무기를 **최대 1개만** 착용할 수 있다 (`is_equipped = true`인 `rpg_player_weapon`은 player_id당 최대 1행). 무기 교체 시 기존 착용 무기를 해제한 뒤 새 무기를 착용한다. 애플리케이션 레벨에서 강제하며 PBT로 검증한다.

### rpg_player_weapon_stat (무기 랜덤 능력치)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| player_weapon_id | BIGINT FK | 무기 인스턴스 |
| stat_type | VARCHAR | ATTACK/DEFENSE/HP/SPEED/CRITICAL |
| stat_value | INT | 수치 |

### rpg_player_weapon_skill (무기에 장착된 스킬)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| player_weapon_id | BIGINT FK | 무기 인스턴스 |
| skill_id | BIGINT | JSON 스킬 ID 참조 |
| slot_index | INT | 슬롯 위치 (0, 1, 2...) |

### rpg_player_armor (보유 방어구 인스턴스)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| player_id | BIGINT FK | 소유자 |
| armor_template_id | BIGINT | JSON 방어구 ID 참조 |
| grade | VARCHAR | 드랍 시 롤된 등급 |
| item_level | INT | 드랍 던전 권장레벨 |
| is_equipped | BOOLEAN | 착용 여부 |

> **착용 불변식**: 한 플레이어는 방어구 **부위(HELMET/CHEST/GLOVES/BOOTS)별로 최대 1개만** 착용할 수 있다 (같은 부위에 `is_equipped = true`인 행은 player_id당 최대 1개). 같은 부위 방어구 착용 시 기존 착용분을 자동 해제한다. 애플리케이션 레벨에서 강제하며 PBT로 검증한다.

### rpg_player_armor_stat (방어구 랜덤 능력치)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| player_armor_id | BIGINT FK | 방어구 인스턴스 |
| stat_type | VARCHAR | ATTACK/DEFENSE/HP/SPEED/CRITICAL |
| stat_value | INT | 수치 |

### rpg_player_inventory (소모품/스킬북)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| player_id | BIGINT FK | 소유자 |
| item_type | VARCHAR | SKILL_BOOK / POTION |
| item_ref_id | BIGINT | 참조 ID (skill_id 또는 items.json id) |
| quantity | INT | 수량 |

### rpg_player_dungeon_progress (던전 클리어 이력)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| player_id | BIGINT FK | |
| dungeon_id | BIGINT | JSON 던전 ID 참조 |
| is_cleared | BOOLEAN | 클리어 여부 |
| best_stage | INT | 최고 도달 스테이지 (1~5) |

### rpg_player_active_run (진행 중 던전 체크포인트)

플레이어당 **최대 1개** (동시에 하나의 던전만 진행). 사망·명시적 도망 시 행 삭제, 웹 종료 시 유지. (`systems/dungeon.md`)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| player_id | BIGINT FK | 소유자 (UNIQUE — 1인 1런) |
| dungeon_id | BIGINT | 진행 중인 던전 id |
| cleared_stage | INT | 마지막으로 완료한 스테이지 (0=없음, 재개=cleared_stage+1) |
| checkpoint_hp | INT | 체크포인트 시점 HP |
| checkpoint_mp | INT | 체크포인트 시점 MP |
| updated_at | TIMESTAMP | 마지막 스테이지 클리어 시각 |

- 스테이지 클리어 시 `cleared_stage` 증가 + `checkpoint_hp/mp` 갱신
- 5스테이지(보스) 클리어 → `rpg_player_dungeon_progress` 갱신 후 이 행 삭제
- 사망 / 명시적 도망 / 던전 포기 → 이 행 삭제 (`systems/dungeon.md`)

---

## ERD 관계 요약

```
[JSON 마스터 데이터]              [DB 플레이어 데이터]
monsters.json ─ ─ ─ ─ ─ ┐
skills.json ─ ─ ─ ─ ─ ─ ┤      rpg_player
weapons.json ─ ─ ─ ─ ─ ─┤       ├── rpg_player_weapon
armors.json ─ ─ ─ ─ ─ ─ ┤       │    ├── rpg_player_weapon_stat
items.json ─ ─ ─ ─ ─ ─ ─┤       │    └── rpg_player_weapon_skill
dungeons.json ─ ─ ─ ─ ─ ┘       ├── rpg_player_armor
                                 │    └── rpg_player_armor_stat
  (template_id / item_ref_id     ├── rpg_player_inventory   (POTION → items.json)
   로 참조)                       ├── rpg_player_dungeon_progress
                                 └── rpg_player_active_run  (진행 중 체크포인트, 1인 1런)
```

- 같은 템플릿이라도 드랍할 때마다 다른 인스턴스 생성 (랜덤 능력치)
