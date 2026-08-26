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
- **Center UI '완전 고정 높이 4단 슬롯(Fixed 4-Tier Slot)' 전면 개편 및 모바일 최적화 완료**:
  - **Tier 1 (상황 멘트 슬림 바, 32px)**: 멘트 데이터 24자 이하 1줄 정제 + `nowrap/ellipsis` 가드로 1줄 완전 고정.
  - **Tier 2 (메인 인터랙션 스테이지, 110px)**: 헤더(22px) + 대사(38px, 2줄 고정 슬롯) + 액션바(32px)로 대사 길이에 관계없이 하단 버튼(`[상점]`, `[수리]`, `[은행]`, `[치료]`, `[전투]`) 위치/Y좌표 1픽셀도 흔들리지 않음 (버튼 잘림 100% 해결).
  - **Tier 3 (상호작용 칩 바, 44px)**: 앤틱 골드/크림슨 캡슐 칩 가로 스크롤 및 활성 대상 골드 글로우 하이라이트.
  - **Tier 4 (미니맵 존, 160px)**: 다크 판타지 미니맵 타일 및 나침반 D-pad.
  - **가드레일 검증**: 1,176개 테스트 100% 통과 (`BUILD SUCCESS`), 5대 품질 가드레일 올클리어, CodeGraph 동기화 완료.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **배포 상태**: 프로덕션 배포 대기 / 로컬 테스트 완료
- **5대 품질 가드레일 상태**: 1,176개 테스트 100% 통과, 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그 후보)

1. **`docs/todo.md` 1번 섹션 잔여 UI/UX 개선**:
   - **상점 편의성**: 상점 장비 돋보기 클릭 시 현재 착용 중인 장비와 비교 팝업 제공
   - **환경 연출**: 인게임 시간대별 웹 배경화면 색상 동적 전환
2. **`docs/todo.md` 2번 섹션 환경 & 시스템 확장 백로그**:
   - 인게임 시간 시스템, 야간 기습 50% 상향, 캠프파이어/야영, 필드 랜덤 보스, 무기 세트 스왑 시스템