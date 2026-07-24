# 세대1 마스터 데이터 (JSON)

세대1 콘텐츠 정의를 실제 개발에서 사용하는 JSON 파일로 추출한 것. 각 md 문서의 JSON 블록이 원본(source of truth)이며, 이 폴더 파일은 그와 동기화되어야 한다.

| 파일 | 원본 문서 |
|------|-----------|
| `monsters.json` | `generations/gen1/monsters.md` |
| `weapons.json`  | `generations/gen1/weapons.md` |
| `armors.json`   | `generations/gen1/armors.md` |
| `skills.json`   | `generations/gen1/skills.md` |
| `dungeons.json` | `generations/gen1/dungeons.md` |
| `items.json`    | `systems/shop.md` |

## 사용

- `myrpg` 모듈 구현 시 `myrpg/src/main/resources/data/` 로 배치해 마스터 데이터 로더가 읽는다 (`systems/persistence.md`).
- JSON의 `id`를 DB에서 `template_id` / `item_ref_id`로 참조한다 (FK 제약 없음, 애플리케이션 레벨 매핑).

> 수치 변경 시 반드시 원본 md 문서와 이 JSON을 함께 갱신한다.
