# MyRPG 리팩토링 후보 분석

> 작성일: 2026-09-02 (Asia/Seoul)
> 대상 모듈: `myrpg`
> 분석 관점: 과도한 `if`/`instanceof` 분기, 하드코딩(매직 넘버), God Class(SRP 위반)

---

## 요약 (Executive Summary)

| 우선순위 | 대상 | 핵심 문제 | 예상 효과 |
|---|---|---|---|
| 🔴 1순위 | `SkillService`, `BattleService` | 스킬 서브타입별 `instanceof` 분기가 여러 메서드에 중복 | 산탄총 수술(shotgun surgery) 제거, 신규 스킬 추가 난이도 하락 |
| 🔴 2순위 | `InventoryService` (1708줄) | 의존성 10개, 책임 과다 God Class | SRP 회복, 테스트/유지보수성 향상 |
| 🟡 3순위 | `BattleService` (1625줄, 의존성 20개) | 전투 로직 비대화, 과도한 결합 | 결합도 감소 (1순위와 연계 시 자연 해소) |
| 🟢 참고 | 하드코딩 / switch | 대체로 양호 (이미 상수·외부화 완료) | 별도 대응 불필요 |

---

## 🔴 1순위 — 스킬 타입별 `instanceof` 분기 중복 (가장 심각)

### 현상
`SkillService`(instanceof 28회), `BattleService`(16회)에서 **동일한 스킬 서브타입 분기**가
여러 메서드에 흩어져 반복된다.

`SkillService.java` (795~835 라인 부근):

```java
private int primaryValue(final Skill catalog, final SkillRank rank) {
    if (catalog instanceof DamageSkill damageSkill)    { return ...; }
    if (catalog instanceof DefenseSkill defenseSkill)  { return ...; }
    if (catalog instanceof RecoverySkill recoverySkill){ return ...; }
    if (catalog instanceof UltimateSkill ultimateSkill){ return ...; }
    if (catalog instanceof BuffSkill buffSkill)        { return ...; }
    if (catalog instanceof CcSkill ccSkill)            { return ...; }
    if (catalog instanceof DotSkill dotSkill)          { return ...; }
    return 0;
}
```

- `primaryStatLabel()`, `primaryValue()`, `resourceCost()` 등 **거의 같은 8종 타입 체인**이
  메서드마다 복사·붙여넣기 형태로 존재한다.
- `BattleService`도 `resolvePlayerMultiplier` / `resolvePlayerBlockRate` /
  `resolveUltimateCombat` / `resolveRecoveryCombat` / `resolveBuffCombat` / `resolveCcCombat`로
  같은 타입 분기를 반복한다.

### 문제점
- **산탄총 수술(Shotgun Surgery)**: 새 스킬 타입을 추가하면 `SkillService`, `BattleService`의
  여러 메서드를 동시에 수정해야 한다.
- **OCP 위반**: 타입이 늘어날 때마다 기존 서비스 코드를 계속 열어 고쳐야 한다.
- **응집도 저하**: 각 스킬 타입의 계산 규칙이 서브타입 클래스가 아닌 서비스에 흩어져 있다.

### 개선 방향
- `Skill` 인터페이스에 다형성 메서드(`primaryValue(rank)`, `primaryStatLabel()`,
  `resourceCost(rank)` 등)를 정의하고, 각 서브타입 클래스가 자신의 계산 규칙을 구현한다.
- 혹은 Visitor 패턴을 도입해 분기 로직을 각 타입으로 위임한다.
- 결과적으로 서비스의 `instanceof` 체인이 단일 다형 호출로 축약된다.

---

## 🔴 2순위 — `InventoryService` God Class (1708줄)

### 현상
- 파일 크기 **1708줄**(모듈 내 최대), `if`/`else` 분기 117회.
- 생성자로 **의존성 10개** 주입:
  `OwnedItemRepository`, `ItemCatalogService`, `CharacterProgressRepository`,
  `StatProgression`, `ActionLog`, `SkillCatalogService`, `CharacterSkillRepository`,
  `SkillRankupBonus`, `GameMessageService`, `GameProperties`.
- public 메서드 20여 개가 서로 다른 관심사를 담당한다.

### 문제점 (SRP 위반)
한 클래스가 아래의 서로 다른 책임을 모두 처리한다.

- 장비 장착/해제/스마트 장착/양손무기·방패 자동 처리
- 포션 사용
- 은행 ↔ 인벤토리 이동 및 용량(capacity) 관리
- 인벤토리/은행/장비 뷰(View) 조립
- 기본 아이템 시딩(seed)
- 아이템 획득(acquire)
- 전투 스킬 버튼 구성
- 무기 세트 I/II 스왑
- 내구도 감소 및 자동 해제

### 개선 방향 (응집도 기준 분리 후보)
- `EquipmentEquipService` — 장착/해제/스왑/양손무기 로직
- `StorageService` — 은행/인벤 이동 및 용량 검증
- `InventoryViewAssembler` — 각종 뷰 조립 (도메인 로직과 분리)
- `ItemAcquisitionService` — 시딩/획득

---

## 🟡 3순위 — `BattleService` 비대화 (1625줄, 의존성 20개)

### 현상
- 파일 크기 **1625줄**, `if`/`else` 분기 99회.
- 생성자 주입 **의존성 20개** — 그 자체로 과도한 결합(coupling) 신호.
- `takeTurn`을 중심으로 선후공 판정, 자원 소모, 스킬 타입별 전투 처리,
  특수효과 사이드이펙트가 한 클래스에 집중돼 있다.

### 문제점
- 의존성 20개는 클래스가 지나치게 많은 협력 객체를 알고 있다는 의미로, 변경 파급이 크다.
- 스킬 타입별 전투 분기(`resolveUltimateCombat`, `resolveRecoveryCombat` 등)가
  1순위 문제와 겹친다.

### 개선 방향
- 순수 계산은 이미 `BattleResolver`(도메인 서비스)로 일부 분리돼 있으므로,
  스킬 타입별 전투 처리(`resolve*Combat`)를 전략(Strategy) 객체로 추출한다.
- 1순위(스킬 다형화)와 함께 진행하면 결합도가 자연스럽게 감소한다.

---

## 🟢 참고 — 하드코딩은 대체로 양호

- `BattleResolver`는 매직 넘버가 **이미 명명 상수로 잘 추출**돼 있다.
  (`CRITICAL_MULTIPLIER = 1.5`, `DRAW_COEFFICIENT = 0.5`, `WIN_COEFFICIENT = 1.0` 등)
- 메시지·게임 수치도 `messages.properties` + `GameProperties`로 외부화가 완료된 상태
  (activeContext 018 작업). "하드코딩 떡칠"급 문제는 거의 관측되지 않는다.
- `switch`문은 대부분 enum 대상 exhaustive switch로, 그 자체로는 문제가 아니다.
  다만 같은 enum(`talent`, `kind`, `intent`)을 여러 곳에서 반복 switch하는 부분은
  1순위 다형화와 함께 정리하면 좋다.

---

## 권장 진행 순서

1. **[1순위] 스킬 타입 다형화** — 임팩트 최대. `SkillService`/`BattleService` 양쪽 중복을
   동시에 줄이고 신규 스킬 추가 난이도를 낮춘다.
2. **[3순위] BattleService 전투 전략 추출** — 1순위와 연계 시 시너지.
3. **[2순위] InventoryService 책임 분리** — 독립적으로 진행 가능한 God Class 해체.

> 각 항목은 SDD 프로세스(requirements → design → tasks)로 착수 권장.
> 착수 시 5대 품질 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo, PMD/CPD) 검증 필수.
