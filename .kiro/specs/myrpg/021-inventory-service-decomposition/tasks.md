# Implementation Plan: InventoryService God Class 분해 (Inventory Service Decomposition)

> **폴더 위치**: `.kiro/specs/myrpg/021-inventory-service-decomposition/tasks.md`
> **관련 규칙**: `rules/workflow/task-build-validation.md`, `rules/workflow/git-workflow.md`

---

## Overview

본 작업 명세는 `myrpg` 모듈의 God Class `InventoryService`(약 1,708줄, 의존성 10개)를 **파사드 + 협력
객체**로 분해하고 중복 조회/판별 로직을 통합하기 위한 체크포인트 단위 작업 목록이다.
**동작 보존 리팩토링**이므로 각 단계에서 기존 테스트가 그린을 유지해야 한다.

### 구현 순서 및 원칙
1. **안전망 우선(Characterization)**: 변경 전 기존 인벤토리/장비/은행/전투 테스트가 전부 그린인지 확인하고,
   미커버 분해 지점의 현행 동작을 고정하는 테스트를 먼저 추가한다.
2. **Bottom-Up + 단일 이동**: **A. 공유 지원(`InventoryQuerySupport`)** → **B. 관심사별 협력 객체 추출**
   → **C. 파사드 위임 전환** → **D. 중복 통합·정리** → **E. 회귀 검증** → **F. 5대 가드레일** 순.
3. **원자적 그린**: 각 협력 객체를 하나 추출·위임할 때마다 `BUILD SUCCESS` + 전체 테스트 그린 유지.
   한 번에 하나의 관심사만 이동하여 회귀 지점을 국소화한다.
4. **계약 불변**: `InventoryService`의 public 메서드 시그니처와 두 생성자(9-arg/7-arg)는 절대 바꾸지 않는다.
5. **테스트 필수 & Given-When-Then**: `should_{동작}_when_{조건}`, Spring Boot 4.0 규약 준수.

---

## Tasks

### A. 안전망 및 공유 지원 컴포넌트 (Foundation)

- [x] 1. 변경 전 안전망 확인 및 characterization 테스트 선행 작성
  - [x] 1.1 기존 인벤토리/장비/은행/전투 테스트 전수 그린 확인
        (`mvn test -pl myrpg -Dtest="Inventory*,Equip*,Item*,UsePotion*,Durability*,Seed*,Bank*,CombatSkill*,MultiCharacter*"`)
  - [x] 1.2 분해 대상 메서드 목록을 design.md §3.1 매핑표와 대조 확정 (public/private 배정 검토)
  - [x] 1.3 **미커버 지점 characterization 테스트 선행 작성** — `swapWeapon`(양손무기/타세트 경계),
        `resolveOtherSetAssignedItemIds` 폴백, `findStorageItem` null 폴백, `findCharacterSafe`/`loadCharacter`
        의 `characterId != null ? … : 1L`/전체 조회 폴백, `findEquippableForSlot` 잘못된 slotCode 등
        현행 동작을 고정하는 테스트를 착수 전에 추가한다.
    - **판단 기준(무엇을 추가/생략할지)**: 각 협력 객체 추출 직전, 해당 관심사의 커버리지 공백을
      JaCoCo 리포트(`target/site/jacoco/…`)로 식별하고, 이관 대상 메서드가 기존 테스트로 커버되지
      **않는 분기**만 characterization 테스트를 추가한다. 이미 기존 회귀 테스트가 덮는 경로는 중복 작성하지
      않는다. (커버리지 라인 80%+/브랜치 70%+ 목표와 연동)
  - _Requirements: 4.6, 4.7_ / _Design: 6.1_

- [x] 2. `application/service/inventory` 패키지 및 `InventoryQuerySupport` 신규 생성
  - [x] 2.1 `inventory/InventoryQuerySupport.java` 생성 — 조회/판별/용량 지원 메서드 이관
        (`resolveEquipment(→Optional<EquipmentItem>)`, `resolveEquipmentOrThrow`, `findOwnedItemOrThrow`,
        `findEquippedInventoryItems`, `resolveEquippedSlotMap`, `resolveOtherSetAssignedItemIds`,
        `findCharacterSafe`, `loadCharacter`, `countStorage`, `checkCapacity`, `isInventoryFull`,
        `maxCapacity`, `resolveMaxDurability`, `findStorageItem`, `resolveEquippedWeaponTalent`,
        `mapKindToSkillTalent`) — 본문은 기존 그대로 이동(동작 불변)
  - [x] 2.2 `InventoryService`가 두 생성자에서 `InventoryQuerySupport`를 조립하고, 파사드 내부의 해당
        private helper를 query 호출로 치환 (중복 블록 1차 통합)
  - _Requirements: 3.1, 3.2, 3.5_ / _Design: 3.2, 2.3_

- [x] 3. **체크포인트 A** — 공유 지원 도입 후 빌드 & 전체 테스트 그린
  - `mvn test -pl myrpg` 통과 확인 (계약·동작 불변)

---

### B. 관심사별 협력 객체 추출 및 파사드 위임 (Extract & Delegate)

> 각 하위 태스크는 **1개 협력 객체 추출 → 파사드 위임 전환 → 전체 테스트 그린**을 원자 단위로 수행한다.

- [x] 4. `EquipmentService` 추출 (착용/해제/스왑/후보/무기재능)
  - [x] 4.1 `inventory/EquipmentService.java` 생성, `equip`/`unequip`/`smartEquip`/`swapWeapon`/
        `findEquippableForSlot`/`equippedWeaponTalent(+Long)`/`isBowEquipped(+Long)` 및 관련 helper 이관
  - [x] 4.2 `InventoryService`의 대응 public 메서드를 `equipmentService` 위임으로 전환(`@Transactional` 유지)
  - [x] 4.3 `mvn test -pl myrpg -Dtest="Equip*,Inventory*,MultiCharacter*"` 그린 확인
  - _Requirements: 1.1, 1.3, 2.1, 2.2, 4.1_ / _Design: 3.1, 3.3_

- [x] 5. `ItemStorageService` 추출 (은행↔인벤 이동/용량)
  - [x] 5.1 `inventory/ItemStorageService.java` 생성, `moveToBank`/`moveToInventory`/`movePotionToStorage` 이관
  - [x] 5.2 파사드 위임 전환 및 `ItemMove*/ItemStack*/InventoryCapacity*` 테스트 그린 확인
  - _Requirements: 1.1, 1.3, 2.1, 4.2_ / _Design: 3.1_

- [x] 6. `ItemAcquisitionService` 추출 (획득/시딩)
  - [x] 6.1 `inventory/ItemAcquisitionService.java` 생성, `acquire`/`acquireItem`(+오버로드)/`seedDefault`
        (+오버로드) 및 acquire/seed helper·`SEED_*_ID` 상수 이관
  - [x] 6.2 파사드 위임 전환 및 `InventoryAcquire*/Seed*/Material*` 테스트 그린 확인
  - _Requirements: 1.1, 1.3, 2.1, 4.2_ / _Design: 3.1_

- [x] 7. `EquipmentBonusService` 추출 (보너스 합산/내구도)
  - [x] 7.1 `inventory/EquipmentBonusService.java` 생성, `equippedBonus(+Long)`/
        `reduceDurabilityAndAutoUnequip` 및 `applyStatDelta`/`applyVitalDelta`/`autoUnequipBroken` 이관
  - [x] 7.2 파사드 위임 전환 및 `EquippedBonus*/DurabilityAutoUnequip*` 테스트 그린 확인
  - _Requirements: 1.1, 2.1, 4.4_ / _Design: 3.1_

- [x] 8. `PotionConsumptionService` 추출 (포션 사용)
  - [x] 8.1 `inventory/PotionConsumptionService.java` 생성, `usePotion` 및 VITAL 최대치 산정 로직 이관
        (`EquipmentBonusService`·`skillRankupBonus` 협력)
  - [x] 8.2 파사드 위임 전환 및 `UsePotion*` 테스트 그린 확인
  - _Requirements: 1.1, 1.3, 2.1, 4.3_ / _Design: 3.1_

- [x] 9. `InventoryViewAssembler` 추출 (뷰 조립/상세)
  - [x] 9.1 `inventory/InventoryViewAssembler.java` 생성, `buildInventoryView`/`buildBankView`/
        `buildEquipmentView`(+오버로드)/`describe` 및 뷰·포맷 helper 이관
  - [x] 9.2 파사드 위임 전환 및 `EquipmentViewService*/ItemDescribe*` + 컨트롤러 슬라이스 테스트 그린 확인
  - _Requirements: 1.1, 2.1, 4.5_ / _Design: 3.1_

- [x] 10. `CombatSkillLoadoutService` 추출 (전투 스킬 버튼)
  - [x] 10.1 `inventory/CombatSkillLoadoutService.java` 생성, `combatSkills` 및 `resolveOwnedSkills`/
        `buildSlotButton`/`resolveSkillIcon` 이관
  - [x] 10.2 파사드 위임 전환 및 `CombatSkillList*` 테스트 그린 확인
  - _Requirements: 1.1, 2.1, 4.4_ / _Design: 3.1_

- [x] 11. **체크포인트 B** — 전 협력 객체 추출·위임 완료 후 빌드 & 전체 테스트
  - `mvn test -pl myrpg` 통과 확인, `InventoryService`가 위임 파사드로 축소되었는지 확인

---

### C. 중복 통합 마감 및 정리 (Consolidation & Cleanup)

- [x] 12. 잔여 중복 통합 및 코드 정리
  - [x] 12.1 각 협력 객체 내부에 남은 카탈로그→장비 판별/캐릭터 폴백/용량 산정 잔재를
        `InventoryQuerySupport` 호출로 마저 치환 (CPD 중복 제거 마감)
  - [x] 12.2 무기 세트(1/2) 슬롯 처리 중복 분기 단일화 검토 및 적용 (동작 보존 범위 내)
  - [x] 12.3 미사용 import/변수/상수 제거, 메서드 길이(50줄 이하) 정리,
        `@SuppressWarnings("PMD.CyclomaticComplexity")` 제거/축소 가능 여부 확인
  - [x] 12.4 **미사용 private 메서드/오버로드 제거** — 코드 조사 결과 호출부가 없는 아래 헬퍼는 이관 대신
        제거한다(외부 관측 동작 불변이므로 동작 보존과 충돌하지 않음). 제거 전 grep으로 무참조 최종 확인:
        `checkCapacity(StorageKind)` 1-arg, `hasCapacity(StorageKind)`/`hasCapacity(Long,StorageKind)`,
        `isInventoryFull()` no-arg, `acquirePotionItem(String,int)` 2-arg, `skillTypePriority(SkillType)`.
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 2.4_ / _Design: 6.3_

---

### D. 전체 회귀 및 동작 보존 검증 (Regression)

- [x] 13. 모듈 전체 테스트 회귀
  - [x] 13.1 `mvn test -pl myrpg` 전체 단위/통합/슬라이스/프로퍼티 테스트 그린 확인
  - [x] 13.2 `InventoryService` public 메서드 시그니처·두 생성자 파라미터가 리팩토링 이전과 동일한지 확인
        (호출부·테스트 무수정 컴파일)
  - _Validates: Requirements 1.1, 1.2, 1.3, 1.4, 4.1~4.6_

---

### E. 5대 품질 가드레일 및 마무리 (Quality Guardrails)

- [x] 14. 5대 품질 가드레일 전체 파이프라인 검증
  - [x] 14.1 `mvn -B -q spotless:apply -pl myrpg` — 포맷팅 자동 교정
  - [x] 14.2 `mvn -B clean install -pl myrpg -am` — 컴파일/Error Prone/ArchUnit/JaCoCo(80%+)/PMD·CPD 전수 검증
  - [x] 14.3 PMD `CognitiveComplexity` 및 CPD 중복이 리팩토링 이전 대비 감소했는지 확인
  - _Requirements: 4(비기능) 1~4_ / _Design: 6.3_

- [x] 15. CodeGraph 인덱스 동기화
  - [x] 15.1 `codegraph sync` 실행하여 심볼/호출 관계 갱신
  - _Workflow 규칙 준수_

- [x] 16. Memory Bank 갱신 (Compaction)
  - [x] 16.1 `memory-bank/activeContext.md`에 021 완료 요약 및 다음 단계(3순위 BattleService) 기록
  - _AGENTS.md 규칙 준수_

- [ ] 17. Git 커밋 (사용자 요청 시)
  - [ ] 17.1 `rules/workflow/git-workflow.md` 준수 커밋 메시지 작성
  - _Workflow 규칙 준수_

---

## 요구사항 역추적 (Requirements Traceability)

| Requirement | 대응 Task |
|---|---|
| Req 1 (파사드 계약 보존: 시그니처/생성자/트랜잭션/예외) | 2.2, 4~10, 11, 13 |
| Req 2 (책임 분해 SRP + 계층 준수) | 2, 4~10, 12.3 |
| Req 3 (중복 제거·확장성 DRY/OCP) | 2, 12 |
| Req 4 (동작 보존·회귀 안전성 + characterization 선행) | 1, 3, 4.3~10.2, 11, 13 |
| Req 4(비기능, 5대 가드레일/CodeGraph/코딩규칙) | 14, 15 |
