# Tasks: 화면 상단 상황 멘트 및 잔여 코드 전수 제거

---

## 작업 목록 및 체크포인트

- [x] **Task 1: 프론트엔드 상황 멘트 바 및 스타일 제거**
  - [x] `myrpg/src/main/resources/templates/fragments/center.html`에서 `.situation` HTML 제거
  - [x] `myrpg/src/main/resources/static/css/myrpg.css`에서 `.situation` CSS 제거
  - *체크포인트*: 템플릿 및 CSS 문법 정상 확인

- [x] **Task 2: 백엔드 DTO, 헬퍼, 어셈블러, 컨트롤러 리팩토링**
  - [x] `PlayScreenView.java`에서 `ambience`, `ambienceEmoji` 컴포넌트 및 레거시 생성자 정리
  - [x] `PlayScreenViewHelper.java`에서 `buildPlayScreen` 오버로드 및 `ambience` 인자 제거
  - [x] `NodeViewAssembler.java`에서 `AmbienceService` 의존성 및 호출 제거
  - [x] `PlayScreenController.java`에서 `AmbienceService` 필드 및 생성자 인자 제거
  - *체크포인트*: 컴파일 에러 범위 파악 및 DTO 계약 단순화

- [x] **Task 3: 상황 멘트 전용 서비스/도메인/JSON 리소스 영구 삭제**
  - [x] `myrpg/src/main/java/com/myapps/web/myrpg/application/service/AmbienceService.java` 삭제
  - [x] `myrpg/src/main/java/com/myapps/web/myrpg/domain/model/AmbienceData.java` 삭제
  - [x] `myrpg/src/main/resources/data/ambience.json` 삭제
  - *체크포인트*: 미사용 백엔드 파일 0건 확인

- [x] **Task 4: 단위/PBT/통합 테스트 전수 수정 및 불필요 테스트 파일 삭제**
  - [x] `AmbienceServiceTest.java` 및 PropertyTest 3종 삭제
  - [x] `ContextLoadAndResourceSmokeTest.java` 수정
  - [x] `VisualJsPreservationAndJsonLoadingIntegrationTest.java` 수정
  - [x] `InGameTimeAmbienceDialogueIntegrationTest.java`를 `InGameTimeNpcDialogueIntegrationTest.java`로 리팩토링
  - [x] `NodeViewAssemblerDungeonTest.java`, `PlayScreenViewHelperTest.java` 수정
  - [x] `PlayScreenController*Test.java` 모음에서 `@MockitoBean AmbienceService` 제거
  - *체크포인트*: `mvn test -pl myrpg` 전체 통과

- [x] **Task 5: 문서 갱신 및 5대 품질 가드레일 빌드 검증**
  - [x] `docs/todo.md` 업데이트
  - [x] `myrpg/README.md` 업데이트
  - [x] 5대 가드레일 검증 (Spotless, Error Prone, ArchUnit, JaCoCo 80%+, PMD/CPD) 및 `codegraph sync`
  - [x] `memory-bank/activeContext.md` 갱신
  - *체크포인트*: `BUILD SUCCESS` 및 가드레일 올클리어
