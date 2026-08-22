# Implementation Plan: 013 액티브 전조 반응 전투 시스템 (Active Telegraph Combat)

## Overview

본 작업 명세(013)는 `myrpg` 모듈(`com.myapps.web.myrpg`)에 **2단계 턴 사이클(대치 ⏸️ ↔ 공방 ⚡), B안 초심플 직관형 Visual Badge 전조, 1.0~1.5초 실시간 타이머 게이지, 타임아웃 무방비 피격, 불필요한 Alert 제거**를 점진적으로 구현한다.

`requirements.md`와 `design.md`의 설계를 바탕으로 DDD 4계층 구조를 따라 점진적으로 구현하며, 각 단계마다 단위 테스트 및 5대 품질 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo 80%+, PMD/CPD)을 준수한다.

---

## Tasks

### A. 도메인 엔티티 & DTO 확장 (Data & Domain Layer)

- [ ] 1. `BattleState` JPA 엔티티 확장
  - [ ] 1.1 `BattleState.java`에 `currentMonsterIntent` (`SkillType`, nullable) 및 `standby` (`boolean`, default `true`) 필드 추가
  - [ ] 1.2 Getter/Setter (`getCurrentMonsterIntent`, `setCurrentMonsterIntent`, `isStandby`, `setStandby`) 구현
  - _Requirements: 2.1_ / _Design: 2.1_

- [ ] 2. `BattleView` DTO 레코드 확장
  - [ ] 2.1 `BattleView.java` 레코드 필드 확장 (총 12개 필드: `monsterName`, `monsterLevel`, `monsterCurrentHp`, `monsterMaxHp`, `skills`, `fleeAvailable`, `standby`, `monsterIntent`, `clashDurationMs`, `monsterStanceBadgeLabel`, `monsterStanceBadgeClass`, `bowFirstStrike`)
  - _Requirements: 2.1, 2.2_ / _Design: 2.2_

- [ ] 3. 도메인 & 리포지토리 단위 테스트
  - [ ] 3.1 `BattleStateStandbyTest.java`[신규] — 대치/공방 플래그 기본값(`true`) 및 몬스터 의도 저장/조회 단위 테스트
  - [ ] 3.2 `BattleStateRepositoryTest.java`[확장] — 신규 필드 영속성 및 DB 조회 검증
  - _Validates: Requirements 2.1_

- [ ] 4. **체크포인트 A** — 도메인 계층 빌드 & 단위 테스트 검증
  - `mvn test -pl myrpg -Dtest="BattleState*"` 통과 확인

---

### B. 애플리케이션 서비스 로직 확장 (Application Service Layer)

- [ ] 5. `InventoryService` 착용 무기 재능 조회 지원
  - [ ] 5.1 `InventoryService.java`에 `equippedWeaponTalent()`, `isBowEquipped()` 퍼블릭 메서드 추가 (기존 `private resolveEquippedWeaponTalent` 노출)
  - [ ] 5.2 `InventoryServiceEquipTalentTest.java`[신규] — 검/활/지팡이 착용 시 재능 반환 및 활 판정 검증
  - _Requirements: 2.4 (3)_ / _Design: 3.1_

- [ ] 6. `BattleService` 공방 개시 (`startClash`) 구현
  - [ ] 6.1 `BattleService.java`에 `@Transactional public BattleView startClash(CharacterProgress, BattleState)` 구현
  - [ ] 6.2 착용 무기 기준 활 1턴 선제 사격(`isBowEquipped() && turnCount == 1`) 판정
  - [ ] 6.3 몬스터 의도 추첨(`monsterAiService.nextAction()`) 및 `state.setCurrentMonsterIntent(intent)` 영속
  - [ ] 6.4 `state.setStandby(false)` 설정 및 B안 뱃지 라벨/CSS 클래스/지속시간 매핑 뷰 생성
  - _Requirements: 2.1, 2.2, 2.4_ / _Design: 3.2 (1)_

- [ ] 7. `BattleService` 타임아웃 턴 처리 (`takeTurn("timeout")`) 구현
  - [ ] 7.1 `takeTurn` 내 `skillId.equalsIgnoreCase("timeout")` 분기 추가
  - [ ] 7.2 `state.getCurrentMonsterIntent()`(또는 `SkillType.NORMAL`) 기준 `resolveMonsterOnlyDamage`로 100% 무방비 피격 적용
  - [ ] 7.3 타임아웃 및 피격 로그 작성, 사망 판정, 턴수 증가, `state.setStandby(true)`, `state.setCurrentMonsterIntent(null)` 복귀
  - _Requirements: 2.3 (2)_ / _Design: 3.2 (2)_

- [ ] 8. `BattleService` 상성 턴 해결, 자원 부족 로그 기록 및 대치 복귀 흐름 구현
  - [ ] 8.1 자원 부족 시 `actionLog.add(...)` 기록 및 `buildInsufficientResult` 반환 (턴 유지)
  - [ ] 8.2 스킬 선택 시 `state.getCurrentMonsterIntent()`(미지 시 `monsterAiService.nextAction()` Fallback)로 `resolveCombat` 수행
  - [ ] 8.3 상성 해결 후 `state.setStandby(true)`, `state.setCurrentMonsterIntent(null)`로 대치 페이즈 복귀
  - [ ] 8.4 도망 실패(`flee`) 시 `SkillType.NORMAL` 1회 피격 후 `state.setStandby(true)` 대치 복귀 확인
  - _Requirements: 2.3 (1), 2.4 (1, 2)_ / _Design: 3.2 (2, 3, 4)_

- [ ] 9. `BattleService` 단위 테스트 작성
  - [ ] 9.1 `BattleServiceClashTest.java`[신규]:
    - `startClash` 의도 추첨 및 `standby = false` 검증
    - `takeTurn("timeout")` 타임아웃 무방비 피격 및 대치 복귀 검증
    - 정상 스킬 입력 시 상성 해결 및 `standby = true` 대치 복귀 검증
    - 자원 부족 시 자원 미차감 및 미진행 검증
    - 활 1턴 선제 사격 시 몬스터 의도 없음 및 스킬 선택 발동 검증
    - 도망 실패 시 `SkillType.NORMAL` 피격 및 대치 복귀 검증
  - _Validates: Requirements 2.1, 2.2, 2.3, 2.4_

- [ ] 10. **체크포인트 B** — 서비스 계층 빌드 & 단위 테스트 검증
  - `mvn test -pl myrpg -Dtest="BattleService*,InventoryService*"` 통과 확인

---

### C. 웹 컨트롤러 계층 구현 (Web Controller Layer)

- [ ] 11. `BattleController` `buildBattleView` 확장 및 `POST /battle/clash` 엔드포인트 구현
  - [ ] 11.1 `BattleController.java`에 `POST /battle/clash` 엔드포인트 구현 (`battleService.startClash` 호출 및 `battle-response` 렌더링)
  - [ ] 11.2 `buildBattleView` 헬퍼 메서드에서 `state.isStandby()`, 뱃지 정보, `bowFirstStrike` 매핑
  - [ ] 11.3 `POST /battle/turn` 및 `POST /battle/flee` 응답 시 대치 복귀 뷰 모델 검증
  - _Requirements: 2.1, 2.2_ / _Design: 4_

- [ ] 12. `PlayScreenController` 활성 전투 재개 뷰(`GET /`) 동기화
  - [ ] 12.1 `PlayScreenController.java:138`에서 활성 전투 재개 시 `state.isStandby()`를 반영하여 `BattleView` 생성
  - _Requirements: 2.1_ / _Design: 2.2, 4_

- [ ] 13. 웹 컨트롤러 슬라이스 테스트 작성 및 갱신
  - [ ] 13.1 `BattleControllerClashTest.java`[신규] — `POST /battle/clash` 호출 시 모델 속성(`standby=false`, 전조 뱃지, 타이머 바) 검증
  - [ ] 13.2 `BattleControllerTest.java`[확장] — `POST /battle/turn`, `POST /battle/flee`, `GET /battle/skills` 응답 검증
  - [ ] 13.3 `PlayScreenControllerBattleTest.java`[확장] — 활성 전투 재접속 시 `standby=true` 뷰 렌더링 검증
  - _Validates: Requirements 2.1, 2.2, 2.4_

- [ ] 14. **체크포인트 C** — 컨트롤러 계층 빌드 & 웹 테스트 검증
  - `mvn test -pl myrpg -Dtest="BattleController*,PlayScreenController*"` 통과 확인

---

### D. 프론트엔드 UI/UX (Thymeleaf & JS & CSS)

- [ ] 15. `battle-view.html` 템플릿 마크업 확장
  - [ ] 15.1 `.battle-stance-area` 추가: 1단계 대치 페이즈(`standby=true`) 뱃지 + `[⚔️ 공방 개시]` 버튼, 2단계 공방 페이즈(`standby=false`) B안 전조 뱃지 + 실시간 카운트다운 타이머 바
  - [ ] 15.2 `#battleSkills` 영역: `th:fragment="battle-skills"` 유지, 대치 중 `disabled-skills` 클래스 및 버튼 `th:disabled`, `th:each="skill : ${skills != null ? skills : battleView.skills}"`
  - [ ] 15.3 `[도망]` 버튼: `th:if="${battleView.standby && battleView.fleeAvailable}"` 대치 상태에서만 노출
  - _Requirements: 2.1, 2.2_ / _Design: 5.1_

- [ ] 16. `myrpg.css` 스타일 정의
  - [ ] 16.1 B안 전조 뱃지 스타일 정의 (`.stance-badge`, `.badge-standby`, `.badge-stance-normal`, `.badge-stance-heavy`, `.badge-stance-defense`)
  - [ ] 16.2 실시간 타이머 바 스타일 정의 (`.clash-timer-wrap`, `.clash-timer-bar`)
  - [ ] 16.3 공방 개시 버튼 스타일 정의 (`.btn-clash-start`, hover 효과)
  - [ ] 16.4 대치 중 비활성화 스킬 버튼 스타일 정의 (`.disabled-skills .battle-skill-btn`)
  - _Requirements: 2.2_ / _Design: 5.3_

- [ ] 17. `myrpg.js` 공방 타이머 및 DOM 교체 함수 구현
  - [ ] 17.1 `swapBattleResponse(html)` 공통 함수 구현 (TopBar, Center, ActionLog 교체 및 `handleTurnResultSignal` 호출)
  - [ ] 17.2 `startClash()` 구현 (`POST /battle/clash` 호출 및 `initClashTimer()` 실행)
  - [ ] 17.3 `initClashTimer()` 구현 (CSS 애니메이션 게이지 감소 및 duration 경과 시 `battleTurn("timeout")` 자동 전송)
  - [ ] 17.4 `battleTurn(skillId)` 수정 (기존 `alert` 제거, `clearTimeout`, `POST /battle/turn`)
  - [ ] 17.5 `flee()` 수정 (`clearTimeout`, `POST /battle/flee`)
  - [ ] 17.6 `handleTurnResultSignal(container)` 수정 (자원 부족 시 `alert` 제거 및 로그 확인 유도)
  - _Requirements: 2.1, 2.2, 2.3, 2.4_ / _Design: 5.2_

- [ ] 18. JS 및 시각 UI 보존 회귀 테스트 검증
  - [ ] 18.1 `VisualJsPreservationAndJsonLoadingIntegrationTest.java` 실행하여 필수 함수 및 CSS 토큰 보존 확인
  - _Validates: Requirements 3.1_

- [ ] 19. **체크포인트 D** — 프론트엔드 연동 빌드 & 뷰 테스트 검증
  - `mvn test -pl myrpg` 전체 단위/통합 테스트 통과 확인

---

### E. 전체 통합 검증 및 5대 품질 가드레일 (Integration & Quality Guardrails)

- [ ] 20. 5대 품질 가드레일 전체 파이프라인 검증
  - [ ] 20.1 `mvn -B -q spotless:apply -pl myrpg` — Java 소스코드 포맷팅 자동 교정
  - [ ] 20.2 `mvn -B clean install -pl myrpg -am` — Error Prone 컴파일, ArchUnit 아키텍처, JaCoCo 80%+ 커버리지, PMD/CPD 정적 분석 전수 통과
  - _Requirements: 3.1_ / _Design: 6_

- [ ] 21. CodeGraph 인덱스 동기화
  - [ ] 21.1 `codegraph sync` 실행하여 최신 심볼 및 호출 그래프 갱신
  - _Requirements: 3.3_

- [ ] 22. Memory Bank 갱신 (Compaction)
  - [ ] 22.1 `memory-bank/activeContext.md`에 013 구현 완료 요약 및 다음 단계 기록
  - _AGENTS.md 규칙 준수_
