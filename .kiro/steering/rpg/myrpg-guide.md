---
inclusion: fileMatch
fileMatchPattern: 'myrpg/**'
---

# MyRPG 개발 가이드

`myrpg`는 텍스트 기반 턴제 모바일 웹 RPG (Spring Boot + Thymeleaf, `com.myapps.web.myrpg`).
설계 문서는 `docs/` 에 있으며, **세대 무관 공통 규칙(systems)**과 **세대별 콘텐츠(generations)**로 분리되어 있다.
아래 문서를 구현의 근거(source of truth)로 삼는다.

## 설계 문서 (근거)

- 개요·게임루프·전략: #[[file:docs/overview.md]]
- 문서 인덱스: #[[file:docs/README.md]]

### 세대 무관 공통 규칙 (systems)

- 캐릭터·성장·페널티: #[[file:docs/systems/character.md]]
- 전투·데미지·치명타·AI: #[[file:docs/systems/combat.md]]
- 무기·스킬 규칙: #[[file:docs/systems/weapons-skills.md]]
- 등급·2축 파워 모델·랜덤능력치: #[[file:docs/systems/items-grades.md]]
- 던전·이벤트·체크포인트·포기: #[[file:docs/systems/dungeon.md]]
- 드랍·파밍·세대 확장 모델: #[[file:docs/systems/farming.md]]
- 상점·판매가·포션: #[[file:docs/systems/shop.md]]
- 영속화·DB 테이블·ERD: #[[file:docs/systems/persistence.md]]
- 화면 목업(UI): #[[file:docs/systems/ui-screens.md]]

### 기술 구조

- 모듈/패키지 구조: #[[file:docs/tech/module-structure.md]]

### 세대1 콘텐츠 (현재 개발 범위)

- 세대1 개요: #[[file:docs/generations/gen1/README.md]]
- 몬스터: #[[file:docs/generations/gen1/monsters.md]]
- 무기: #[[file:docs/generations/gen1/weapons.md]]
- 방어구: #[[file:docs/generations/gen1/armors.md]]
- 스킬: #[[file:docs/generations/gen1/skills.md]]
- 던전: #[[file:docs/generations/gen1/dungeons.md]]

### 세대1 마스터 데이터 (실제 사용 JSON)

- 데이터 폴더 안내: #[[file:docs/generations/gen1/data/README.md]]
- 실제 파일: `docs/generations/gen1/data/*.json` (`monsters/weapons/armors/skills/items/dungeons`)
- 구현 시 `myrpg/src/main/resources/data/` 로 배치해 마스터 데이터 로더가 읽는다.

## 데이터 동기화 규칙

- 세대 콘텐츠 수치는 **md 문서의 JSON 블록이 원본**이며, `docs/generations/genN/data/*.json` 은 그와 항상 동기화되어야 한다.
- 수치를 바꾸면 **md 문서 + data JSON을 함께** 갱신한다. 한쪽만 수정 금지.

## 구현 우선순위 (PBT 핵심 대상)

- `DropService`(카테고리·등급·능력치 롤의 확률/범위 불변식), `BattleService`(데미지·최소값·치명타·선후공) 는 Property-Based Test 우선 대상이다.
- 착용 불변식(무기 1개, 방어구 부위별 1개)도 PBT로 검증한다 (`docs/systems/persistence.md`).

## 신규 세대 개발 프로세스 (필수 · 순서 준수)

세대2 이상을 추가할 때는 **반드시 아래 순서**를 따른다. 순서를 건너뛰고 곧바로 구현에 들어가지 않는다.

1. **설계문서 정의**
   - `docs/generations/genN/` 에 콘텐츠 문서(README·monsters·weapons·armors·skills·dungeons)를 작성한다.
   - `systems/` 공통 규칙은 재사용하고, 신규 규칙이 필요하면 systems 문서를 함께 갱신한다.
   - 세대 확장 규칙(레벨 밴드, 신규 장비 세트, gradeChance 이동)은 `docs/systems/farming.md`를 따른다.

2. **시뮬레이션 검증**
   - `docs/simulation/` 스크립트(`rpg_full.py` 등)의 `MON` / `DUNGEONS` / `GRADE_*` 상수에 신규 세대 수치를 반영한다.
   - 성장 곡선·보스 첫 격파 레벨을 예측하고 **목표 격파창**(예: 권장레벨 ±1 이내)을 설정해 검증한다.
   - 목표창을 벗어나면 수치를 조정하고 재시뮬한다. 시뮬 결과가 납득될 때까지 반복한다.
   - 시뮬레이터 사용법·전제·한계: #[[file:docs/simulation/README.md]]

3. **개발 시작**
   - 시뮬레이션 검증을 통과한 뒤에만 구현(스펙 작성 → 코드)에 착수한다.
   - 설계문서·data JSON이 확정된 상태에서 시작한다.

> 요약: **설계문서 정의 → 시뮬레이션 → 개발 시작**. 시뮬레이션 없이 신규 세대 수치를 구현에 반영하지 않는다.
