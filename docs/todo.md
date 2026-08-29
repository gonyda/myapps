# MyRPG 개발 TODO

---

## 1. 버그 수정 및 안정성 개선

- **[완료] 다중 계정/캐릭터 간 장비 공유 및 장착 해제 간섭 버그 (2026-08-29 완료)**:
  - **현상**: '고니' 계정에서 초보자용 갑옷을 해제해도 착용 상태가 유지되거나, 이후 '관리자' 계정으로 로그인 시 갑옷이 함께 벗겨져 있는 등 복수 계정/캐릭터 간 장비 상태가 상호 간섭 및 공유되는 문제.
  - **해결 내용**:
    1. `InventoryService` 내 `findEquippedInventoryItems`, `equip`, `equippedBonus`, `reduceDurabilityAndAutoUnequip`, `resolveEquippedWeaponTalent`에서 하드코딩된 `1L` 및 캐릭터 조건 없는 전역 쿼리(`findByStorageAndEquippedTrue`) 제거, `characterId` 기준 엄격 격리 조회로 일원화.
    2. `InventoryService`의 `findStorageItem` 및 `countStorage`에서 타 캐릭터 아이템으로의 무분별한 fallback 결함 수정.
    3. `CharacterService.createAndSaveDefault()`에서 `inventoryService.seedDefault(saved.getId())`로 정확한 캐릭터 ID를 전달하도록 수정.
    4. `ShopService.buy()`의 불필요한 `1L` 특수 분기 정리.
    5. `MultiCharacterEquipmentIsolationTest` 단위/통합 테스트 5종을 구축하여 다중 캐릭터 간 장착/해제/스왑/내구도/용량 독립성 100% 검증 완료.
- **[완료] 상황 멘트 및 NPC 대사의 게임 내 시간(InGameTime) 불일치 버그 (2026-08-29 완료)**:
  - **현상**: 화면 상단바/배경 앰비언트는 인게임 시간(예: 심야 02:00, 노을 17:30 등)으로 표시되나, 필드 상황 멘트(`AmbienceService`) 및 NPC 대화(`NpcDialogueService`)가 서버의 현실 시각(`LocalDateTime.now().getHour()`)을 기준으로 멘트를 출력하여 인게임 시간과 멘트가 맞지 않는 문제.
  - **해결 내용**:
    1. `NodeViewAssembler.buildFieldView()`에서 `ambienceService.ambience(currentNode, progress.getInGameHour())`로 인게임 시각(Hour)을 전달하도록 수정 (계절은 실제 서버 월(Month) 유지).
    2. `PlayScreenController.talkToNpc()`에서 `npcDialogueService.selectLine(targetNpc.get(), progress.getInGameHour())`로 인게임 시각(Hour)을 전달하도록 수정.
    3. `InGameTimeAmbienceDialogueIntegrationTest` 통합 테스트를 구축하여 인게임 시간대별(심야/아침/오후/밤) 멘트 및 NPC 대사 연동 100% 검증 완료.

---

## 2. UI/UX 및 시스템 개선

- **스킬 승급 조건 단순화 (막타 처치 항목 전면 제거 & 사용 횟수 단일화)**:
  - **요구사항**: 스킬 승급 조건 중 번거롭고 불합리한 '막타 처치 수(requiredKills)' 조건을 완전히 제거.
  - **개선 내용**:
    1. 패시브 스킬(AP 단독 소모)을 제외한 모든 액티브 스킬(일반/강/CC/디버프/버프/방어/궁극기)의 승급 조건을 **'스킬 사용 횟수(Usage Count) + AP 소모'**로 단일화.
    2. `SkillRankPolicy`, `SkillService`, `SkillRankUpView`, `CharacterSkill` 등의 막타 검증 로직 제거.
    3. `skill-popup.html` 및 모달 UI에서 막타 달성도/프로그레스바 제거하여 깔끔한 UI 제공.
- **스킬 목록 팝업 내 스킬 유형/종류(일반, 강, CC, 디버프 등) 뱃지 표기**:
  - **요구사항**: 근접/궁술/마법 스킬 목록에서 각 스킬이 일반/강/CC/디버프/버프/방어/궁극기 중 어떤 메커니즘인지 직관적으로 파악하기 어려움.
  - **개선 내용**: `SkillRowView`에 `SkillType` 라벨/유형 정보를 바인딩하고, `skill-popup.html`의 스킬 이름/랭크 옆에 종류 뱃지(예: `[일반]`, `[강]`, `[CC]`, `[디버프]`, `[버프]`, `[방어]`, `[궁극기]`)를 시각적으로 추가.

---

## 3. 게임 내 환경 & 시스템 확장 (백로그)

- **캠프파이어 & 야간 위험도 시스템 (Campfire & Night Danger - 표준형)**:
  - 밤 시간대(19:00~05:00) 몬스터 기습(선공) 확률 50% 상향 및 처치 EXP +20%.
  - 필드에서 장작을 소비하여 `[🔥 모닥불 피우기]`(야영) 상호작용 실행.
  - 야영을 통해 위험한 밤 시간을 아침(08:00)으로 안전하게 스킵 & 바이탈(HP/MP/Stam) 완충.
  - 모닥불 음식 구워먹기 스탯 버프(STR/DEX/INT +5 등) 연출.
- **마을 아르바이트 & 축복의 포션 시스템 (Part-Time Jobs - 추후 상세설계)**:
  - 인게임 시간대별(오전/오후) 마을 NPC(성당, 대장간, 힐러집 등) 일일 의뢰 수주 및 납품.
  - 보상: 소정 골드 및 `축복의 포션` (장비 내구도 소모 50% 감소 및 사망 시 장비 보호).
- **타이틀(칭호) & 업적 시스템 (Titles & Achievements)**:
  - 업적 달성 시 고유 칭호 획득 (예: `[던전을 정복한]`, `[10살에 곰을 잡은]`, `[퍼거스의 친구]`).
  - 정보창에서 칭호 장착 시 닉네임 표기 및 고유 스탯 보너스 부여, 타이틀 도감 팝업 제공.
- **필드 보스 랜덤 스폰 (Field Boss Encounters)**:
  - 필드에 랜덤 시간 + 랜덤 위치로 보스 등장(스폰 설정 + 동적 배치 서비스 + 런타임 스폰 상태 관리).
