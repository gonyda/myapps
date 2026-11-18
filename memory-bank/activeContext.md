# Active Context

> 최종 업데이트: 2026-11-18 19:34 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` & `memory-bank/memory-bank.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(원칙·맥락) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(Compaction 갱신)**
> - **관리 원칙**: 슬라이딩 윈도우 & 이전 작업 압축(Compaction) 필수, 무한 누적 금지
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

---

## 1. 이전 완료 작업 요약 (Compacted)

- **핵심 시스템 & UI/UX 기반 완비 (001~019)**:
  - 알비 던전/전투 턴/데이터 격리, 모바일(360~480px) 팝업군(인벤/장비/상점/대치).
  - 인게임 6대 시간대 스카이 그라디언트 및 천체 궤적 연출, 장작 채집, 메시지/프로퍼티 외부화.
- **스킬 타입 다형화 및 전투 디스패치 Visitor 전환 (2026-09-03, 020-skill-type-polymorphism, 커밋 `6252297`)**:
  - 도메인 다형 계약 확장(8종 서브타입) 및 `SkillCombatVisitor<R>` 도입. `instanceof` 체인을 다형 호출 및 sealed switch로 완전 치환.
- **`InventoryService` God Class 협력 객체 분해 및 파사드화 (2026-09-03, 021-inventory-service-decomposition, 커밋 `41f978f`)**:
  - 1,708줄 거대 클래스를 순수 위임 파사드(483줄, 72% 축소)로 개편. 27개 public API 및 2개 생성자 계약 100% 보존.
  - 관심사별 협력 객체 8종 분리, 무기 세트 1/2 슬롯 분기 단일화 및 중복 쿼리 제거.
- **`BattleService` God Class 협력 객체 분해 및 파사드화 (2026-09-07, 022-battle-service-decomposition, 커밋 `f285060`)**:
  - 1,752줄 거대 클래스를 슬림한 오케스트레이터(693줄, 61% 축소)로 개편. 7개 public API 및 2개 생성자(18-arg/16-arg) 계약 100% 보존.
  - 협력 객체 6종 분리(`CombatStatResolver`, `SkillCombatResolver`, `TurnEffectApplier`, `BattleRewardService`, `BattleViewAssembler`, `BattleTurnResultFactory`), `@SuppressWarnings` 완전 제거, 5대 가드레일 및 1,313개 테스트 All-Green.
- **`mycalendar` GitHub 단방향 스냅샷 동기화 구현 (2026-11-18, `mycalendar/003-github-calendar-sync`)**:
  - `ScheduleService` CUD 커밋 완료(`AFTER_COMMIT`) 시 `SingleThreadExecutor`로 직렬화된 비동기 전체 스냅샷 푸시(`gonyda/mike` `calendar/mycalendar.json`), `409 Conflict` 1회 재시도, Fail-safe 격리, `POST /schedules/sync/github` 수동 재동기화 엔드포인트 구현.
  - `mycalendar` 5대 품질 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo, PMD/CPD) 및 117개 테스트 All-Green (`BUILD SUCCESS`).

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **5대 품질 가드레일 상태**:
  - `mycalendar`: 117개 전체 테스트 All-Green (`BUILD SUCCESS`, Spotless/Error Prone/ArchUnit/JaCoCo/PMD/CPD 통과)
  - `myrpg`: 1,313개 전체 테스트 All-Green
- **CodeGraph**: 최신 동기화 완료 (`Already up to date`)
- **현재 활성 작업**: `mycalendar/003-github-calendar-sync` 구현 및 검증 완료 (미커밋 상태)

---

## 3. 다음 단계 (Next Steps / 백로그)

### 3.1. 신규 게임 컨텐츠 기획 및 SDD 개발 연계
`skills/myrpg-system-design` 활용 신규 컨텐츠 기획서(GDD) 산출 및 SDD 워크플로우 연계:
1. **캠프파이어 & 야간 위험도 시스템**: 휴식 회복, 쉐어링 버프, 야간 몬스터 위험도
2. **마을 아르바이트 & 축복의 포션 시스템**: 성당/대장간/힐러집 알바, 축복 버프
3. **타이틀 & 업적 도감 시스템**: 칭호 수집 스탯 보너스, 업적 달성 연동
4. **기믹 던전 & 장비 인챈트/수리 연계**: 퍼즐/열쇠 기믹, 인챈트 스크롤, 대장간 수리

