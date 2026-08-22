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

- **테스트 & 디버깅용 치트 버튼 구현 완료 (`docs/todo.md` 4번 섹션)**:
  - 좌측 사이드바에 `+1K EXP` 및 `+1K Gold` 버튼 배치
  - `POST /cheat/exp` (1,000 EXP 획득, 레벨업/AP/풀회복 반영), `POST /cheat/gold` (1,000 Gold 획득)
  - 5대 품질 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo, PMD/CPD) 및 1,047개 단위/통합 테스트 전수 통과
- **기획 백로그 (`docs/todo.md` 현황)**:
  1. **전투 심리전/전략성 개선**: 단순 반복 탈피, 가위바위보 심리전 및 컨트롤로 1레벨도 상위 몬스터 공략 가능한 전략적 여지 확보
  2. **[완료] 맵/몬스터 데이터 추가 & 밸런스 조정**: `흰 거미` 개명/하향, `공동 묘지` 맵 추가, `붉은 여우` 추가, `너구리` 하향, 포션 3종 드랍 완료
  3. **UI/UX 개선**: 스킬 사용 시 alert 팝업 제거, 인게임 시간대별 은은한 웹 배경화면 색상 전환
  4. **[완료] 개발/테스트 도구**: 1,000 EXP / 1,000 Gold 획득 치트 버튼
  5. **시스템/계정**: 간이 로그인 화면(DB 직접 등록), 인게임 가상 시간/야간 기습/캠프파이어/필드 보스 등 확장 백로그

---

## 3. 최근 변경사항 (Recent Changes)

- **[2026-08-23] 테스트용 치트 버튼 구현 (`docs/todo.md` 4번 섹션)**
  - `PlayScreenController`: `POST /cheat/exp`, `POST /cheat/gold` 엔드포인트 추가.
  - `left-sidebar.html`, `myrpg.css`: 좌측 네비바에 `+1K EXP`, `+1K Gold` 치트 버튼 스타일링 추가.
  - `myrpg.js`: `cheatExp()`, `cheatGold()` Ajax 비동기 호출 및 UI 프래그먼트 자동 스왑 연동.
  - `PlayScreenControllerProgressionTest`: 웹 슬라이스 단위 테스트 2건 추가.
- **[2026-08-23] 맵 & 몬스터 데이터 조정 및 신규 추가 (`docs/todo.md` 2번 섹션)**
  - `monster.json`: 너구리 스탯 하향(HP 38/ATK 36/DEF 2), 거미 $\rightarrow$ 흰 거미 개명 및 스탯 하향(HP 50/ATK 40/DEF 2), 붉은 여우(Lv2, HP 44/ATK 38/DEF 2) 추가. 너구리/붉은여우/흰거미 포션 3종(HP/MP/스태미나) 드랍 추가, 붉은거미/고블린/검은거미 아이템 드랍 제거(골드 전용).
  - `map.json`: `graveyard` (공동 묘지, x:1, y:-1, 흰 거미 스폰) 추가, `east-hill`에 붉은 여우 스폰 및 `graveyard` 링크 연결.
  - `myrpg/README.md`: 맵 그래프 및 몬스터 7종 테이블 최신화.
  - `MonsterServiceLoadIntegrationTest` 및 관련 테스트 전수 갱신 및 통과.
- **[2026-08-23] MyRPG 데이터 밸런스 Skill (`myrpg-data-balance`) 구축 완료**
  - `skills/myrpg-data-balance/SKILL.md` (SSOT), `.agents/skills/myrpg-data-balance/SKILL.md`, `.cline/skills/myrpg-data-balance/SKILL.md` 생성.
  - 아이템/몬스터/스킬/맵 데이터 추가 시 1:1 대화형 Q&A + `tools/balance/` 파이썬 자동 검증 + `mvn test` 파이프라인 구축.
- **[2026-08-23] MyRPG 원격 서버 배포 완료 (`134.185.116.35:8083`)**
  - 최신 main 브랜치 git pull, 빌드, 기존 프로세스 교체 및 Health check 통과 (PID: 2656751, HTTP 200)

---

## 4. 다음 단계 (Next Steps)

1. `docs/todo.md` 다음 섹션 진행:
   - **3번 섹션**: UI/UX 개선 (스킬 사용 시 alert 팝업창 제거, 인게임 시간대별 웹 배경화면 동적 색상 전환)
   - 또는 **4번 섹션**: 테스트 & 디버깅 편의 치트 버튼 (1,000 EXP / 1,000 Gold)
   - 또는 **1번 섹션**: 전투 시스템 및 전략성 개선 (심리전 & 스킬 전략화)
2. 기능 추가 시 `rules/workflow/task-build-validation.md` 5대 가드레일 준수 및 구현 진행