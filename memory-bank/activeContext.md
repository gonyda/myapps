# Active Context

> 최종 업데이트: 2026-08-25 07:29 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` & `memory-bank/memory-bank.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(원칙·맥락) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(Compaction 갱신)**
> - **관리 원칙**: 슬라이딩 윈도우 & 이전 작업 압축(Compaction) 필수, 무한 누적 금지
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

---

## 1. 이전 완료 작업 요약 (Compacted)

- **AI Agent 아키텍처 & 5대 품질 가드레일**: `AGENTS.md`, `rules/`, `memory-bank/`, `skills/sdd/SKILL.md`, Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD 가드레일 구축.
- **011 알비 던전 시스템 (`.kiro/specs/myrpg/011-dungeon-system/`)**: 프로시저럴 던전 생성 엔진(`DungeonGenerator`), 안개 탐색, 연쇄 전투(10%), 턴제 방 이동 및 백트래킹 완료.
- **013 액티브 전조 반응 전투 시스템 (`.kiro/specs/myrpg/013-active-telegraph-combat/`)**: 2단계 턴 사이클, B안 직관형 전조 뱃지, 타임아웃 무방비 피격, 스킬 alert 제거, 전체 가드레일 검증 완료.
- **신규 장비 42종 카탈로그 & NPC 상점 판매 매핑 (`item.json`, `npc.json`)**: 총 57종 장비 확충, 밸런스 검증 0건 오차 완료.
- **015 유저 계정 및 간이 로그인 시스템 (`.kiro/specs/myrpg/015-user-authentication-and-login/`)**:
  - `UserAccount` JPA 엔티티, `AuthService` 기반 `bbsk`(고니) 및 `admin`(관리자: 35종 전스킬 F랭크 + 풀장비 + 포션 15개) 자동 시드 및 퀵 로그인 프리셋 구현.
  - `AuthInterceptor` 미인증 접근 제어 및 `/login` 리다이렉트, 세션 기반 다중 캐릭터 데이터(인벤토리, 장비, 스킬, 상점, 은행, 던전, 전투) 완전 격리.
  - 1,161개 전체 테스트 통과 및 `main` 브랜치 병합/푸시 완료 (`fd4df96`).

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main` (작업 트리 깨끗함, 원격 `origin/main`과 동기화 완료)
- **문서 동기화**: `docs/todo.md`에 015 로그인/계정 시스템 완료 상태 반영 및 백로그 정리 완료.
- **5대 품질 가드레일 상태**: 1,161개 테스트 통과, PMD/CPD/JaCoCo 80%+ 커버리지 충족, Spotless 포맷팅 일치 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그 후보)

1. **차기 기능 기획 및 SDD Spec 작성 (`docs/todo.md` 참조)**:
   - **후보 A**: 인게임 시간대별(`TimeOfDay`: 새벽, 아침, 낮, 노을, 밤, 심야) 웹 배경화면 색상 동적 전환 및 은은한 테마 UI 연출
   - **후보 B**: 게임 내 가상 시간 시스템(`In-Game Time`) 영속화 및 밤 시간대 기습(선공) 확률 상향 (5% ➜ 50%)
   - **후보 C**: 필드 노드 캠프파이어 & 야영 시스템 (`[모닥불 피우기]`, 밤 시간 스킵, 휴식 회복)
   - **후보 D**: 필드 랜덤 보스 스폰 시스템
2. **사용자 요구사항 확인 및 새 기능 브랜치 생성 (`feature/...`)**