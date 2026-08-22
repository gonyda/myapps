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

- **MyRPG 종합 게임 가이드 문서화 (`myrpg/README.md`)**:
  - 캐릭터 성장/스탯 공식, 9칸 전투 상성 및 감산형 데미지 공식, 16단계 스킬 랭크업/영구스탯/11종 수치표, 장비/인벤토리/내구도, 상점/대장간/은행/힐러집 경제 시스템, 맵/NPC 10인, 몬스터 6종 도감, 프로시저럴 던전 생성 엔진 등 소스코드 기반 전체 시스템 가이드 SSOT 구축 완료.
- **직전 스펙 완료**: `012 디펜스 및 카운터 어택 스킬 재설계` 5개 Task 전체 완료
- **기획 백로그 (`docs/todo.md`)**:
  1. **인게임 시간 시스템**: Day N, 00:00~23:59 가상 시간 영속화 및 맵 이동 시 시간 경과 (예: 10~30분)
  2. **야간 시간대 기습 시스템**: 야간 시간대(22:00~06:00) 몬스터 기습 확률 상향 (5% $\rightarrow$ 50%)
  3. **마비노기식 필드 캠프파이어**: 장작 아이템/스킬을 통한 야영 설치 및 휴식 회복(HP/MP/스태미나 점진적 회복)

---

## 3. 최근 변경사항 (Recent Changes)

- **[2026-08-23] MyRPG 원격 서버 배포 완료 (`134.185.116.35:8083`)**
  - 최신 main 브랜치 git pull, 빌드, 기존 프로세스 교체 및 Health check 통과 (PID: 2656751, HTTP 200)
- **[2026-08-23] MyRPG README.md 전수 검증 및 설계 가이드 보완 완료**
  - **오류 수정**:
    - 스킬 랭크 체계 정정 (`NOVICE` 삭제 $\rightarrow$ `F` ~ `MASTER` 16개 랭크, 승급 15구간)
    - 시간대 시스템(`TimeOfDay`) 구간을 소스코드([TimeOfDay.java](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/domain/model/TimeOfDay.java))와 1:1 일치하도록 정정 (`LATE_NIGHT`, `DAWN`, `MORNING`, `AFTERNOON`, `LATE_AFTERNOON`, `NIGHT`)
  - **누락/미비 사항 보완**:
    - `2.5절`: 신규 캐릭터 기본 지급 목록 신설 (기본 스킬 4종 F랭크, 기본 착용 장비 6종, 인벤토리 보관 무기 4종, 포션 15개, 0G)
    - `3.3절`: 공격력 산출식 및 무기 미착용/스킬별 주스탯 기준, 재능 일치 데미지 보너스(+10%) 명시
    - `3.4절`: `MonsterAiService` 행동 가중치(NORMAL 34% / HEAVY 33% / DEFENSE 33%) 및 기습(`ambush`) 시스템 명시
    - `4.5절`: 스킬 습득 체계 신설 (기본 4종 외 나머지 스킬은 스킬북을 통해 습득 예정인 미구현 기능으로 명시)
    - `6.1절`: 상점 판매가 공식 예시(상점템 50%, 드랍 전용 스탯 가중합산) 추가

---

## 4. 다음 단계 (Next Steps)

1. 사용자의 신규 기능 설계 및 개발 요청 대기 (인게임 가상 시간, 야간 기습, 캠프파이어, 스킬북 등)
2. 기능 추가 시 `rules/workflow/task-build-validation.md` 5대 가드레일 준수 및 구현 진행