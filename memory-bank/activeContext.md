# Active Context

> 최종 업데이트: 2026-08-22 16:35 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(읽기) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(갱신)**
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

## 1. 이전 완료 작업 요약 (Compacted)
- **AI Agent 교체 가능 아키텍처 구축**: `AGENTS.md`, `rules/`, `memory-bank/`, `skills/sdd/SKILL.md` 구축 완료
- **품질 가드레일 및 MCP 로컬화**: PMD/CPD 플러그인 도입 및 `.agents/mcp_config.json` 로컬화 완료

---

## 2. 완료된 작업: `011 알비 던전 시스템 (Dungeon System)`

- **Spec 위치**: `.kiro/specs/myrpg/011-dungeon-system/`
- **목적**: 인스턴스 랜덤 던전 생성, 안개 탐색(Fog of War), 연쇄 전투, 방 클리어 기반 이동 제약 및 라이프사이클 E2E 통합

### 완료된 전 단계 핵심 내용
- **Phase A (Task 1~4)**: 던전 몬스터 5종(`spider`, `red-spider`, `goblin`, `black-spider`, `giant-spider`), `dungeons.json` 메타데이터 로더, 도메인 불변 모델(`DungeonInstance`, `DungeonRoomState`), JPA 영속성 엔티티(`DungeonProgressEntity`) 구축 및 단위/프로퍼티 테스트 통과.
- **Phase B (Task 5~7)**: 프로시저럴 던전 생성 엔진(`DungeonGenerator`: 보스방 최단거리 10 고정, 20~23개 방, 분기율 40%), 안개(Fog of War) 및 방 상태(`cleared`/`uncleared`) 렌더링 뷰 팩토리(`MapViewFactory`) 구현 및 jqwik 불변식 프로퍼티 테스트 통과.
- **Phase C (Task 8~10)**: 던전 서비스(`DungeonService`) 오케스트레이션, `BattleService` 10% 연쇄 전투 및 보스전 분기 연동, 백트래킹 이동 규칙 검증, jqwik 프로퍼티 테스트 2종 통과.
- **Phase D (Task 11~14)**:
  - `DungeonController`: `POST /dungeon/enter`, `POST /dungeon/leave`, `POST /dungeon/move` 웹 엔드포인트 구현 및 `DungeonControllerTest` MockMvc 테스트.
  - `NodeViewAssembler` & `PlayScreenViewHelper`: 활성 던전 방 및 월드 맵 던전 입구에 따른 상호작용 버튼/안개 뷰 분기 조립 및 `NodeViewAssemblerDungeonTest` 검증.
  - UI 템플릿, CSS 및 JS 확장: `center.html`, `myrpg.css` (던전 버튼, 클리어/미클리어 노드 스타일), `myrpg.js` (던전 입장/퇴장/이동 및 프래그먼트 스왑 함수).
  - `GlobalExceptionHandler`: `BlockedMovementException` 및 `DungeonNotImplementedException` 400 Bad Request 핸들러 추가.
- **Phase E (Task 15~16)**:
  - `DungeonLifecycleIntegrationTest`: 던전 입장 $\rightarrow$ 안개 해제/방 이동 $\rightarrow$ 미클리어 방 전진 차단/후퇴 허용 $\rightarrow$ 몬스터 격퇴 $\rightarrow$ 보스 격퇴 및 보상 수령(EXP 1000, Gold 2000, 포션 3개) $\rightarrow$ 입구 복귀 및 엔티티 정리, 자발적 퇴장 및 사망 리스폰 E2E 시나리오 검증.
  - 5대 품질 가드레일 통합 검증 통과 (`BUILD SUCCESS`, 1014개 테스트 전원 통과, JaCoCo 커버리지 충족, PMD/CPD 준수, `codegraph sync` 완료).

---

## 3. 작업 트리 (011 던전 시스템 주요 파일)

| 파일 | 구분 | 설명 |
|---|---|---|
| `docs/dungeon_design.md` | 신규 | 던전 시스템 상세 설계 문서 |
| `.kiro/specs/myrpg/011-dungeon-system/*` | 신규 | SDD 3종 명세서 (`requirements`, `design`, `tasks`) |
| `myrpg/.../data/monster.json` | 수정 | 알비 던전 몬스터 5종 데이터 |
| `myrpg/.../data/dungeons.json` | 신규 | 던전 메타데이터 및 스펙 JSON |
| `myrpg/.../application/dto/Dungeon*Spec.java` | 신규 | 던전 메타데이터 DTO records |
| `myrpg/.../application/dto/InteractionItem.java` | 수정 | 상호작용 액션 타입 및 파라미터 확장 |
| `myrpg/.../application/service/DungeonSpecRepository.java` | 신규 | 던전 스펙 로더/저장소 |
| `myrpg/.../domain/model/DungeonRoomState.java` | 신규 | 방 상태 불변 record |
| `myrpg/.../domain/model/DungeonInstance.java` | 신규 | 던전 런타임 도메인 집계 |
| `myrpg/.../domain/model/DungeonProgressEntity.java` | 신규 | 던전 진행 JPA 엔티티 |
| `myrpg/.../domain/service/DungeonGenerator.java` | 신규 | 프로시저럴 던전 생성 엔진 |
| `myrpg/.../domain/service/MapViewFactory.java` | 수정 | 던전 안개(Fog of War) 및 방 상태 뷰 생성 지원 |
| `myrpg/.../application/service/DungeonService.java` | 신규 | 던전 생명주기 및 이동/전투/보상 오케스트레이션 |
| `myrpg/.../application/service/BattleService.java` | 수정 | 10% 연쇄 전투 및 던전 보스전/사망 연동 |
| `myrpg/.../interfaces/api/DungeonController.java` | 신규 | 던전 입장/퇴장/방 이동 웹 컨트롤러 |
| `myrpg/.../interfaces/api/NodeViewAssembler.java` | 수정 | 던전 인스턴스 존재 여부에 따른 뷰 모델 조립 분기 |
| `myrpg/.../interfaces/api/GlobalExceptionHandler.java` | 수정 | 던전 차단 이동 및 미구현 던전 예외 핸들러 |
| `myrpg/.../templates/fragments/center.html` | 수정 | 던전 상호작용 버튼 데이터 속성 바인딩 |
| `myrpg/.../static/css/myrpg.css` | 수정 | 던전 노드 및 버튼 스타일 |
| `myrpg/.../static/js/myrpg.js` | 수정 | 던전 진입/퇴장/방 이동 JS 비동기 호출 및 DOM 스왑 |
| `myrpg/src/test/.../DungeonLifecycleIntegrationTest.java` | 신규 | 던전 E2E 전체 라이프사이클 통합 테스트 |

---

## 4. 다음 단계

- [x] **011 알비 던전 시스템 전체 구현 및 검증 완료 (Task 1 ~ 16 완료)**
- [ ] 다음 마일스톤 또는 추가 피처 진행 (사용자 요청 대기)