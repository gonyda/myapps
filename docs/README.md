# MyRPG 설계 문서

텍스트 기반 턴제 웹 RPG(`myrpg` 모듈)의 설계 문서 모음.

## 문서 구조

문서는 **세대 무관 공통 규칙(systems)**과 **세대별 콘텐츠(generations)**로 분리한다.
규칙은 `systems/`에 한 번만 정의하고, 콘텐츠 데이터는 세대 폴더에서 그 규칙을 참조한다.

```
docs/
├── overview.md              # 개요 · 핵심 컨셉 · 게임 루프 · 전략 포인트
├── systems/                 # 세대 무관 공통 규칙
│   ├── character.md         # 스탯 · 레벨업 · 시작 지급 · 사망/도망 페널티
│   ├── combat.md            # 전투 규칙 · 데미지 공식 · 치명타 · 도망 · 몬스터 AI
│   ├── weapons-skills.md    # 무기 카테고리 · 스킬 시스템 규칙
│   ├── items-grades.md      # 등급 · 2축 파워 모델 · 랜덤 능력치 (무기/방어구 공통)
│   ├── dungeon.md           # 5스테이지 구조 · 체크포인트 저장/재개
│   ├── farming.md           # 드랍 판정 · 세대 파밍 모델 · gradeChance 개념
│   ├── shop.md              # 전리품 판매가 공식 · HP/MP 포션
│   ├── persistence.md       # JSON 마스터 데이터 · DB 테이블 · ERD
│   └── ui-screens.md        # 모바일 화면 목업
├── tech/
│   └── module-structure.md  # myrpg 모듈(패키지) 구조
├── simulation/              # 밸런스 시뮬레이션 (성장 속도·보스 격파 레벨 예측)
│   ├── README.md
│   ├── rpg_sim.py           # 숲 초반 파밍 분석
│   └── rpg_full.py          # 전체 진행(숲→광산→탑) 보스 격파 레벨 분석
└── generations/
    └── gen1/                # 세대 1 콘텐츠 (레벨 1~10) — 현재 개발 범위
        ├── README.md        # 세대1 개요 (밴드 · 던전 · 등급 확률)
        ├── monsters.md
        ├── weapons.md
        ├── armors.md
        ├── skills.md
        └── dungeons.md
```

## 현재 개발 범위

- **세대 1 (레벨 1~10)만** 진행한다. 세대 2 이상은 `generations/gen2/`... 로 추후 추가.
- 세대 확장 방식(레벨 밴드, 신규 장비 세트, 등급 확률 이동)은 `systems/farming.md` 참고.

## 읽는 순서 (추천)

1. `overview.md` — 게임이 무엇인지
2. `systems/character.md` → `systems/combat.md` — 캐릭터와 전투
3. `systems/items-grades.md` → `systems/farming.md` → `systems/shop.md` — 장비·파밍·경제
4. `systems/dungeon.md` — 던전 진행/저장
5. `generations/gen1/README.md` 및 하위 — 실제 세대1 수치
6. `systems/persistence.md`, `tech/module-structure.md` — 구현 참고
