# Active Context

> 최종 업데이트: 2026-08-23 01:30 (Asia/Seoul)

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
- **012 디펜스 및 카운터 어택 스킬 재설계 (`.kiro/specs/myrpg/012-defense-counter-skill-redesign/`)**:
  - 디펜스: 100% 완전 방어(0 피격), 0 반격, 랭크별 스태미나 소모 감소(5→1), 랭크업 영구 DEF +1/rank 및 HP +5/rank 보너스 연동, 승급 모달 불필요 반격 배율 숨김 및 스태미나/영구 스탯 안내 UI 반영.
  - 카운터 어택: 100% 완전 회피, 상대 공격력 비례 반격(100%→200%), 크리티컬 보너스(0→200/+20.0%p), 디펜스 상대 시 0 피해(헛방 교착), 승급 모달 반격/크리 보너스 UI 반영.
  - 몬스터 디펜스 대칭성(일반 공격 막힘/칼 튕김 0뎀, 스매시 관통) 및 9칸 매트릭스 속성/단위 테스트 전수 통과.
- **포션 & 시드 아이템 확장**: `mp_potion_30`, `stamina_potion_30` 추가, 다중 스탯 회복 및 신규 캐릭터 생성 시 마나/스태미나 포션 각 5개 기본 지급 연동.

---

## 2. 현재 작업 맥락 및 상태

- **스펙 완료**: `012 디펜스 및 카운터 어택 스킬 재설계` 5개 Task 전체 완료 (Task 1~5)
- **빌드 및 품질 검증**: 5대 품질 가드레일 통과 (`BUILD SUCCESS`), 전체 1,038+ 단위/속성 테스트 통과, `verify_skill.py` 밸런스 검증 통과, `codegraph sync` 완료.
- **기획 백로그 (`docs/todo.md`)**:
  1. **인게임 시간 시스템**: Day N, 00:00~23:59 가상 시간 영속화 및 맵 이동 시 시간 경과 (예: 10~30분)
  2. **야간 시간대 기습 시스템**: 야간 시간대(22:00~06:00) 몬스터 기습 확률 상향 (5% $\rightarrow$ 50%)
  3. **마비노기식 필드 캠프파이어**: 장작 아이템/스킬을 통한 야영 설치 및 휴식 회복(HP/MP/스태미나 점진적 회복)

---

## 3. 다음 단계

- [ ] 기획 백로그 중 다음 기능 선정 (예: **013 인게임 시간 시스템 & 야간 기습 시스템** SDD Spec 작성)
- [ ] 또는 운영 서버 배포 진행 (`rules/infra/deployment.md`)