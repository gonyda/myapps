# Requirements: 화면 상단 상황 멘트 및 잔여 코드 전수 제거

---

## 1. 배경 및 목적 (Background & Objectives)

### 1.1. 배경
- MyRPG 플레이 화면 상단에는 계절·시간대·노드 테마를 조합한 상황 멘트를 표시하는 32px 슬림 바(`.situation`)가 존재했음.
- 모바일 세로모드(360~480px) 환경에서 상단 바의 세로 공간 점유로 인해 주요 상호작용 카드와 미니맵 등의 가시성이 제한되는 이슈가 있음.

### 1.2. 목적
- 메인 플레이 화면에서 상단 상황 멘트 영역을 전면 제거하여 모바일 뷰포트 공간을 효율화.
- 상황 멘트 전용 백엔드 컴포넌트(`AmbienceService`, `AmbienceData`, `ambience.json`)와 뷰 모델 필드(`ambience`, `ambienceEmoji`)를 완전 제거하여 클린 아키텍처 및 미사용 잔여 코드 0건 달성.
- 시간대별 배경 그라디언트, 천체(해/달) 일주 연출, NPC 시간대별 대사 등 인게임 시간 시스템은 온전히 유지.

---

## 2. 범위 (Scope)

### 2.1. In-Scope
- 프론트엔드: `center.html` 내 `.situation` 마크업 제거, `myrpg.css` 내 `.situation` 관련 스타일 제거.
- 백엔드 DTO/헬퍼: `PlayScreenView`, `PlayScreenViewHelper`, `NodeViewAssembler`, `PlayScreenController`에서 `ambience`/`ambienceEmoji`/`AmbienceService` 제거.
- 파일 삭제: `AmbienceService.java`, `AmbienceData.java`, `data/ambience.json`, `AmbienceServiceTest` 등 전용 테스트 4종.
- 연동 테스트 갱신: `ContextLoadAndResourceSmokeTest`, `VisualJsPreservationAndJsonLoadingIntegrationTest`, 컨트롤러 테스트 등에서 Ambience mock/assertion 제거.
- 문서 갱신: `docs/todo.md`, `myrpg/README.md`.

### 2.2. Out-of-Scope
- `TimeOfDay` enum 및 시간대별 하늘/천체 궤적 연출 변경 없음.
- `NpcDialogueService`의 시간대별 대사 분기 로직 변경 없음.
- `MapNode.theme` 필드 모델 구조 변경 없음.

---

## 3. 도메인 용어사전 (Glossary)

| 용어 | 정의 |
|---|---|
| **상황 멘트 (Ambience Bar)** | 화면 상단에 시간대 이모지와 테마별 텍스트를 출력하던 1단 슬림 바 UI |
| **PlayScreenView** | 플레이 화면 렌더링에 필요한 모든 뷰 데이터를 집계하는 불변 레코드 |
| **TimeOfDay** | 6대 시간대(심야, 새벽, 오전, 오후, 황혼, 밤)를 정의하는 열거형 (유지) |

---

## 4. 요구사항 명세 (Requirements & EARS)

- **REQ-019-01 (UI 제거)**: WHEN 사용자가 메인 플레이 화면을 조회할 때, THE 시스템 SHALL 상황 멘트 슬림 바(`#situation`) 없이 상단 앰비언트 스카이 배경 아래 즉시 메인 인터랙션 스테이지(`.interaction-stage`)를 렌더링해야 한다.
- **REQ-019-02 (뷰 모델 정리)**: THE `PlayScreenView` 레코드 SHALL `ambience` 및 `ambienceEmoji` 컴포넌트를 포함하지 않아야 한다.
- **REQ-019-03 (컴포넌트 의존성 제거)**: THE `NodeViewAssembler` 및 `PlayScreenController` SHALL `AmbienceService`에 의존하지 않아야 한다.
- **REQ-019-04 (잔여 코드 0건)**: THE 시스템 SHALL `AmbienceService`, `AmbienceData`, `ambience.json` 및 관련 전용 테스트 파일을 완전히 제거해야 한다.
- **REQ-019-05 (5대 품질 가드레일)**: THE 시스템 SHALL Spotless, Error Prone, ArchUnit, JaCoCo 80%+, PMD/CPD 검증을 100% 통과해야 한다.
