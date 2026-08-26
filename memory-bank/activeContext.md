# Active Context

> 최종 업데이트: 2026-08-26 22:20 (Asia/Seoul)

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
- **011 알비 던전 시스템 (`.kiro/specs/myrpg/011-dungeon-system/`)**: 던전 생성 엔진, 안개 탐색, 연쇄 전투, 턴제 이동 완료.
- **013 액티브 전조 반응 전투 시스템 (`.kiro/specs/myrpg/013-active-telegraph-combat/`)**: 2단계 턴 사이클, 전조 뱃지, 실시간 타이머, 전체 가드레일 검증 완료.
- **015 유저 계정 및 간이 로그인 시스템 (`.kiro/specs/myrpg/015-user-authentication-and-login/`)**: `UserAccount`, 퀵 로그인, 세션 기반 데이터 격리 완료.
- **스킬 시스템 정합성 및 UI 편의성 개선 완료**: 도메인 다형성, 승급 조건 개선, verify_skill.py 8개 도메인 16키 검증 동기화 완료.
- **상점·은행·수리 팝업 UI/UX 전면 개편 및 모바일 세로모드(360~480px) 100% 최적화 완료**:
  - 하단 여백 100% 제거 스크롤 레이아웃, 상점 2종 탭, 은행 2열 대시보드, 퍼거스 대장간 테마 수리 팝업.
- **스킬 팝업 UI 레이아웃 개편 및 모바일 최적화 완료**:
  - 2행 다크 판타지 카드 레이아웃, 탤런트 계열별 아이콘, 풀너비 수련치 게이지 분리.
- **Center UI 4열 그리드 확장, 전투시작 버튼 여백 및 미니맵 앤틱 판타지 노드 디자인 고도화 완료**:
  - **Tier 2 (인터랙션 스테이지)**: 몬스터 전투 시작 버튼(`⚔️ 전투 시작`) 크기 슬림화(`height: 28px`, `max-width: 150px`, 중앙 정렬) 및 카드 하단 패딩 확보(`padding: 8px 12px 10px 12px`)로 버튼 하단 여백 완전 해소.
  - **Tier 3 (상호작용 영역)**: 한 줄당 4개씩 자동 줄바꿈되는 4열 그리드(`grid-template-columns: repeat(4, 1fr)`)에 **4열 2행(74px) 최소 고정 슬롯(`grid-auto-rows: 34px`, `min-height: 74px`, `align-content: start`)**을 적용하여 대상 수 변화에도 미니맵 Y좌표가 들쭉날쭉 흔들리지 않도록 완벽 고정.
  - **던전 입장 버튼 통일**: 기존에 알비 던전만 '알비 던전 입장'으로 하드코딩되던 분기 로직을 제거하고, 모든 던전 입구 버튼명을 **`던전 입장 ⚔️`**으로 일관되게 통일.
  - **Tier 4 (미니맵 & 전체지도 팝업)**: 전체 지도 팝업을 공통 패널 규격(`.panel.map-panel`, `.panel-header-title`, `.panel-close`, 앤틱 골드 줌/위치 초기화 컨트롤 `zoom-btn`)으로 전면 개편하고, 3D 앤틱 보석/타일 그라디언트(마을 사파이어, 필드 에메랄드, 던전 루비/블러드스톤, 클리어 실버, 미탐색 옵시디언)와 황금빛 발광 연결선(`link-*`) 및 마법 비콘 펄스 애니메이션으로 전면 고도화.
  - **가드레일 검증**: 1,181개 테스트 100% 통과 (`BUILD SUCCESS`), Spotless 포맷팅 및 CodeGraph 동기화 완료.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **배포 상태**: **Oracle Cloud 프로덕션 배포 완료 (포트 8083, `DEPLOY_SUCCESS`)**
- **5대 품질 가드레일 상태**: 1,181개 테스트 100% 통과, 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그 후보)

1. **`docs/todo.md` 1번 섹션 잔여 UI/UX 개선**:
   - **상점 편의성**: 상점 장비 돋보기 클릭 시 현재 착용 중인 장비와 비교 팝업 제공
   - **환경 연출**: 인게임 시간대별 웹 배경화면 색상 동적 전환
2. **`docs/todo.md` 2번 섹션 환경 & 시스템 확장 백로그**:
   - 인게임 시간 시스템, 야간 기습 50% 상향, 캠프파이어/야영, 필드 랜덤 보스, 무기 세트 스왑 시스템