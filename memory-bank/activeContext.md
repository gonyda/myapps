# Active Context

> 최종 업데이트: 2026-08-30 10:37 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` & `memory-bank/memory-bank.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(원칙·맥락) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(Compaction 갱신)**
> - **관리 원칙**: 슬라이딩 윈도우 & 이전 작업 압축(Compaction) 필수, 무한 누적 금지
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

---

## 1. 이전 완료 작업 요약 (Compacted)

- **핵심 시스템 & UI/UX 완비**: 알비 던전/전투 턴/데이터 격리, 모바일(360~480px) 최적화 팝업군, 무기 세트 I/II 스왑, 스킬 슬롯 해제 방어, 상점 장비 착용 비교 팝업 구축 완료.
- **인게임 시간대별 앰비언트 스카이 & 천체 궤적 (2026-08-28)**: 6대 시간대(`TimeOfDay`) 딥 다크 스카이 그라디언트, 천체(해/달) 궤적 및 소프트 글로우 동적 연출 완비.
- **스킬 승급 조건 단순화 & 유형 뱃지 표기 (2026-08-29)**: `RankUpRequirement(requiredUsage)` 단일화, 10대 스킬 유형별 다크 판타지 컬러 뱃지 구축.
- **신규 아이템 '장작' 및 마을/필드 50% 나무 스폰 & 5초 채집 시스템 (2026-08-29, 017-firewood-gathering)**: `ItemType.MATERIAL`, `firewood` 아이템 등록, `GatheringService` (5 SP 소모, 50% 채집 성공/실패 롤), 5초 자동 완료형 채집 타이머 완비.
- **메시지 및 게임 프로퍼티 외부화 리팩토링 (2026-08-30, 018-message-and-properties-externalization)**:
  - **인프라**: `messages.properties` (로그, 전투, 묘사, 예외 110여 개 키 전수 외부화), `application-game.yml` 및 `GameProperties` 불변 Record (전투 계수, 밸런스, 마을/이동 상수 바인딩), `GameMessageService` 구현.
  - **서비스 & 컨트롤러**: `GatheringService`, `ShopService`, `InventoryService`, `DungeonService`, `ProgressionService`, `BattleService`, `BattleLogFormatter`, `SkillController`, `HealController`, `RepairController`, `MovementService`, `GlobalExceptionHandler` 전수 연동 (Strict Invariance 완벽 보장).
  - **프론트엔드**: `game-messages.js` 클라이언트 메시지 번들 및 `window.GAME_MESSAGES.get()` 연동, `myrpg.js` 내 하드코딩 알림/Confirm/토스트 23건 치환.
  - **검증**: `GameMessagePropertyTest`, `GamePropertiesPropertyTest` 등 PBT 및 1267개 전체 단위 테스트 100% 통과, 5대 품질 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo 80%+, PMD/CPD) 및 `codegraph sync` All-Green.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **Spec & Task 상태**: `.kiro/specs/myrpg/018-message-and-properties-externalization/tasks.md` 전 태스크(1~18) 100% 완료
- **5대 품질 가드레일 상태**: 전체 멀티모듈 (`mystudy`, `mycalendar`, `myrpg`) 100% 그린, 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그)

1. **[기획 1] 캠프파이어 & 야간 위험도 시스템 (표준형)**:
   - 획득한 장작을 소비하여 모닥불 야영(야간 20~05시 위험 완화, 아침 08:00 스킵, 바이탈 완충, 음식 굽기 버프).
2. **[기획 2] 마을 아르바이트 & 축복의 포션 시스템**:
   - 시간대별 NPC 일일 의뢰 및 축복의 포션(내구도 보호) 보상 (추후 상세설계).
3. **[기획 3] 타이틀(칭호) & 업적 도감 시스템**:
   - 업적 기반 고유 칭호 장착 및 스탯 보너스, 타이틀 도감 팝업.
4. **[기획 4] 필드 보스 랜덤 스폰 (Field Boss Encounters)**:
   - 필드 랜덤 시간 + 랜덤 위치 보스 등장.
5. **[기획 5] 인챈트 & 세공 장비 커스터마이징 시스템**:
   - 접두/접미 인챈트 스크롤 및 마법 가루 성공률, 세공 옵션 부여.
