# Design: 화면 상단 상황 멘트 및 잔여 코드 전수 제거

---

## 1. 아키텍처 및 핵심 설계 결정

### 1.1. 설계 결정 및 트레이드오프
| 설계 결정 | 대안 | 선택 이유 |
|---|---|---|
| **AmbienceService 및 AmbienceData 완전 삭제** | Deprecated 처리 후 유지 | 요구사항(잔여 코드 0건)에 부합하며, 상황 멘트 외 용도가 없으므로 불필요한 코드 및 유지보수 비용 방지 |
| **PlayScreenView DTO 필드 축소** | `ambience`를 빈 문자열로 유지 | HTML 템플릿과 DTO 간 불필요한 필드 잔재를 없애고 타입 안전성 및 메모리 절약 |
| **TimeOfDay 및 시간대 연동 유지** | TimeOfDay 관련 코드 전체 삭제 | 시간대별 하늘 그라디언트, 천체 궤적, 24시간 시계, NPC 대사는 MyRPG의 핵심 UX이므로 온전히 보존 |

---

## 2. 세부 변경 설계

### 2.1. 프론트엔드
- `center.html`:
  - `div.situation#situation` 제거
  - 상단 여백은 `.center` 컨테이너의 패딩과 8px 갭으로 자연스럽게 유지
- `myrpg.css`:
  - `.situation`, `.situation .situation-icon`, `.situation .situation-text` CSS 제거

### 2.2. DTO & Assembler
- `PlayScreenView`:
  ```java
  public record PlayScreenView(
          TopBarView topBar,
          MinimapView minimap,
          FullMapView fullMap,
          String timeOfDayKey,
          String inGameTime,
          String npcName,
          String npcDialogue,
          List<InteractionItem> interactions,
          List<ActionButton> npcActions,
          String monsterName,
          String monsterDialogue,
          Integer monsterLevel,
          Integer monsterMaxHp,
          List<ActionButton> monsterActions,
          boolean monsterBoss,
          List<ActionLogEntry> logs,
          InfoPopupView info)
  ```
- `PlayScreenViewHelper`: `buildPlayScreen` 오버로드들에서 `ambience` 파라미터 및 `ambienceEmoji` 설정 로직 제거.
- `NodeViewAssembler`: `AmbienceService` 필드 및 호출 제거, `assemblePlayScreen` 단순화.
- `PlayScreenController`: `AmbienceService` 의존성 제거.

### 2.3. 파일 삭제 목록
- `AmbienceService.java`
- `AmbienceData.java`
- `ambience.json`
- `AmbienceServiceTest.java`
- `AmbienceServiceCandidateSelectionPropertyTest.java`
- `AmbienceServiceSeasonTimeMappingPropertyTest.java`
- `AmbienceServiceThemeDeterminationPropertyTest.java`

---

## 3. 검증 전략

- 멀티모듈 빌드 및 5대 품질 가드레일 (Spotless, Error Prone, ArchUnit, JaCoCo, PMD/CPD) All-Green.
- `InGameTimeNpcDialogueIntegrationTest`로 시간대별 NPC 대사 연동 보존 검증.
- `codegraph sync`로 지식 그래프 동기화.
