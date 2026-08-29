# Active Context

> 최종 업데이트: 2026-08-29 16:35 (Asia/Seoul)

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
- **다중 계정/캐릭터 간 장비 공유 및 장착 해제 간섭 버그 해결 (2026-08-29)**:
  - `InventoryService` 내 `findEquippedInventoryItems`, `equip` 등 `characterId` 격리 조회 일원화 및 seedDefault 캐릭터 ID 누락 수정.
  - `MultiCharacterEquipmentIsolationTest` 신규 테스트 5종 구축.
- **상황 멘트 및 NPC 대사의 게임 내 시간(InGameTime) 불일치 버그 해결 (2026-08-29)**:
  - `NodeViewAssembler` 및 `PlayScreenController`에서 `CharacterProgress.getInGameHour()` 전달로 인게임 시간대 100% 동기화.
  - `InGameTimeAmbienceDialogueIntegrationTest` 작성 완료.
- **6대 시간대 구간 최적화 조정 (2026-08-29)**:
  - 오후(`12:00~18:00`), 황혼(`18:00~20:00`), 밤(`20:00~24:00`), 심야(`00:00~05:00`), 새벽(`05:00~08:00`), 오전(`08:00~12:00`)으로 `TimeOfDay`, `ambience.json`, `myrpg.js`, `README.md` 동기화 완료.
  - 전체 1,250개 테스트 100% 통과 및 5대 가드레일 올클리어.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **5대 품질 가드레일 상태**: 1,250개 테스트 100% 통과, 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그)

1. **[시스템 개선] 스킬 승급 조건 단순화 (막타 처치 항목 전면 제거 & 사용 횟수 단일화)**:
   - 패시브(AP 단독 소모)를 제외한 모든 액티브 스킬의 승급 조건을 '스킬 사용 횟수(Usage Count) + AP 소모'로 단일화하고 막타(Kill Count) 로직 및 UI 완전 제거.
2. **[UI/UX 개선] 스킬 목록 팝업 내 스킬 유형(일반, 강, CC, 디버프, 버프 등) 뱃지 표기**:
   - `SkillRowView` 및 `skill-popup.html`에 `SkillType` 라벨/유형 뱃지(일반, 강, CC, 디버프, 버프, 방어, 궁극기)를 이름/랭크 옆에 시각적으로 노출.
3. **[기획 1] 캠프파이어 & 야간 위험도 시스템 (표준형)**:
   - 야간(20~05시) 기습 50% & 장작 소비형 모닥불 야영(아침 08:00 스킵, 바이탈 완충, 음식 굽기 버프).
4. **[기획 2] 마을 아르바이트 & 축복의 포션 시스템**:
   - 시간대별 NPC 일일 의뢰 및 축복의 포션(내구도 보호) 보상 (추후 상세설계).
5. **[기획 3] 타이틀(칭호) & 업적 도감 시스템**:
   - 업적 기반 고유 칭호 장착 및 스탯 보너스, 타이틀 도감 팝업.
