# 012 디펜스 및 카운터 어택 스킬 재설계 (Tasks)

- [x] 1. 데이터 카탈로그 및 모델 갱신 (`skill.json`, `DefenseSkill`, `verify_skill.py`)
  - [x] 1.1. `skill.json`의 `defense`와 `counter_attack` 데이터를 개편 확정 수치(디펜스 blockRate 100/counter 0/resourceCostByRank 5->1, 카운터 blockRate 100/counter 100->200/critBonus 0->200)로 갱신
  - [x] 1.2. `DefenseSkill.java` 및 `SkillCatalogService.java`에 랭크별 `resourceCostByRank`, `critBonusByRank` 파싱 지원
  - [x] 1.3. `tools/balance/verify_skill.py` 및 `rules/myrpg/data-balance-guide.md` 상한(200%) 동기화 및 스크립트 검증 실행

- [x] 2. 전투 해결 엔진 개편 (`BattleResolver`, `BattleService`, `BattleLogFormatter`)
  - [x] 2.1. `BattleResolver`에서 100% 완전 방어(0 피격) 및 counter 0 처리 지원
  - [x] 2.2. `BattleResolver`에 카운터 어택 전용 판정 로직(상대 공격력 비례 반격, 스매시/일반 모두 반격, 디펜스 상대 시 0 피해) 구현
  - [x] 2.3. `BattleService`에서 스킬 랭크별 자원 소모(`resourceCost`) 및 실효 크리티컬 연동
  - [x] 2.4. `BattleLogFormatter`에 완전 방어, 공격 막힘, 카운터 반격, 카운터 헛방 로그 텍스트 포맷팅 추가

- [x] 3. 랭크업 영구 스탯 연동 (`SkillRankupBonus`, `ProgressionService`, `CharacterProgress`)
  - [x] 3.1. `SkillRankupBonus`에서 `defense` 스킬에 대해 랭크당 DEF +1 및 HP +5 누적 산출, `counter_attack`은 0 산출
  - [x] 3.2. 최대 HP 계산 경로(`vitalMax`, `PlayScreenViewHelper`, `BattleService`, `HealController`, `InventoryService`)에 스킬 랭크업 HP 보너스 합산 반영
  - [x] 3.3. 랭크업 시 최대 HP 증가 및 현재 HP 유지 처리 검증

- [x] 4. 스킬 승급 모달(UI) 뷰 모델 갱신 (`SkillService.buildRankUpView`, `skill-popup.html`)
  - [x] 4.1. 디펜스 승급 모달에서 불필요한 반격 배율 숨김 및 스태미나 소모 변화 표시 지원
  - [x] 4.2. 카운터 어택 승급 모달에서 100%->200% 반격 배율 및 크리티컬 보너스 표시 검증

- [x] 5. 단위 및 속성 테스트 작성 및 5대 품질 가드레일 검증
  - [x] 5.1. `BattleResolver` 및 `BattleService`의 디펜스/카운터 어택 9칸 매트릭스 테스트 작성 및 갱신
  - [x] 5.2. `SkillService` 및 `SkillRankupBonus`의 DEF/HP 스탯 가산 테스트 작성
  - [x] 5.3. 5대 품질 가드레일 통합 검증 (`spotless`, `errorprone`, `archunit`, `jacoco`, `pmd/cpd`) 및 `codegraph sync` 완료
