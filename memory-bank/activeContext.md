# Active Context

> 최종 업데이트: 2026-08-22 22:35 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` & `memory-bank/memory-bank.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(원칙·맥락) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(Compaction 갱신)**
> - **관리 원칙**: 슬라이딩 윈도우 & 이전 작업 압축(Compaction) 필수, 무한 누적 금지
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

---

## 1. 이전 완료 작업 요약 (Compacted)

- **AI Agent 교체 가능 아키텍처 & 품질 가드레일**: `AGENTS.md`, `rules/`, `memory-bank/`, `skills/sdd/SKILL.md`, PMD/CPD 플러그인 도입 및 `.agents/mcp_config.json` 로컬화 완료.
- **011 알비 던전 시스템 (`.kiro/specs/myrpg/011-dungeon-system/`)**: 프로시저럴 던전 생성 엔진(`DungeonGenerator`), 안개 탐색(Fog of War), 연쇄 전투(10%), 턴제 방 이동 및 백트래킹, E2E 라이프사이클 통합 완료.
- **버그 수정 & UX 개선**: 던전 내 D-Pad 이동 404 수정, 방 몬스터 클릭 조우 `NodeNotFoundException` 수정, 던전 미니맵 슬레이트/실버 톤 팔레트 적용.
- **포션 시스템 확장**: `mp_potion_30`, `stamina_potion_30` 아이템 추가, `PotionItem`/`InventoryService` 다중 스탯 회복 지원, 딜리스/마누스 상점 입고.
- **던전 클리어 보상 확률형 드랍 개편**: 알비 던전 보상에 숏소드(20% 1개), HP/MP/스태미나 포션(각각 독립 50% 1~3개) `itemDrops` 스펙 및 `MonsterRewardService` 공용 추첨 연동.

---

## 2. 현재 작업 맥락 및 상태

- **현재 상태**: 대기 중 (모든 가드레일 통과 및 커밋 완료)
- **최근 커밋**: `92870d3` (`feat(myrpg): 알비 던전 클리어 보상 확률형 드랍 체계 개편`)
- **테스트 현황**: 전체 1,025개 테스트 통과, 5대 품질 가드레일 준수 (`BUILD SUCCESS`)
- **기획 백로그 (`docs/todo.md`)**:
  - 인게임 시간 시스템 (Day N, 00:00~23:59 가상 시간 영속화 및 맵 이동 시 경과)
  - 야간 시간대 몬스터 기습 확률 상향 (5% $\rightarrow$ 50%)
  - 마비노기식 필드 캠프파이어(야영/휴식 회복) 시스템

---

## 3. 다음 단계

- [ ] 사용자 요청에 따른 신규 피처(인게임 시간/캠프파이어 등) SDD 프로세스 시작 또는 추가 밸런스 조정