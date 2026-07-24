# 기술 구조 (myrpg 모듈)

`myrpg`는 Spring Boot + Thymeleaf 기반 Web 모듈이며, DDD 계층 구조를 따른다.
(모듈 생성 규칙·패키지 컨벤션은 `.kiro/steering` 참고 — `com.myapps.web.myrpg`)

## 패키지 구조 (초안)

```
myrpg/
├── domain/
│   ├── model/
│   │   ├── Player.java
│   │   ├── Monster.java
│   │   ├── Weapon.java
│   │   ├── Armor.java
│   │   ├── Skill.java
│   │   ├── SkillBook.java
│   │   ├── Dungeon.java
│   │   └── DungeonRun.java        # 진행 중 던전 상태 (체크포인트)
│   ├── repository/
│   │   └── PlayerRepository.java
│   └── service/
│       ├── BattleService.java     # 데미지·치명타·턴 처리
│       ├── DropService.java       # 카테고리·등급·능력치 롤 (PBT 핵심 대상)
│       ├── DungeonService.java    # 스테이지 진행·체크포인트
│       └── ShopService.java       # 판매가 계산·포션 구매
├── application/
│   └── service/
│       └── GameSessionService.java
└── interfaces/
    └── api/
        └── GameController.java
```

## 구현 시 참고

- **마스터 데이터 로딩**: `resources/data/*.json` 로더 → 도메인 템플릿 (`systems/persistence.md`)
- **PBT 우선 대상**: `DropService`(확률·능력치 롤 불변식), `BattleService`(데미지·최소값·치명타)
- 세대1 실제 콘텐츠 수치는 `generations/gen1/`
