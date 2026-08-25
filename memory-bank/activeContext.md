# Active Context

> 최종 업데이트: 2026-08-25 21:00 (Asia/Seoul)

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
- **신규 장비 42종 카탈로그 & NPC 상점 매핑 (`item.json`, `npc.json`)**: 총 57종 장비 확충 완료.
- **015 유저 계정 및 간이 로그인 시스템 (`.kiro/specs/myrpg/015-user-authentication-and-login/`)**: `UserAccount`, 퀵 로그인, 세션 기반 데이터 격리 완료.
- **TODO 긴급 버그 및 콘텐츠/UX 확장 완료**:
  - 선제공격 타임아웃/보조스킬 버그 수정, 상점 장비 내구도 `MAX/MAX` 표기 완료.
  - `skill.json` 35종 설명 문구 개편, `map.json` '두갈드 아일' 신설 완료.
  - 전투 스킬 색상/정렬 개선, 연타 로그 포맷 통일, 미니맵/월드맵 렌더링 시인성 강화 완료.
- **TODO 1번 섹션 UI/UX 개선 (정보 / 스킬 / 인벤토리 탭 RPG 테마 전면 개편)**:
  - **공통 판타지 패널 프레임**: 앤틱 골드 프레임, 글래스모피즘 백드롭, 메탈릭 원형 닫기 버튼.
  - **정보 탭**: 영웅 프로필 카드(아바타·닉네임·레벨·누적레벨), 재능 & AP 칩, 3색 바이탈 바(HP/MP/SP), 5대 핵심 스탯 2열 그리드(아이콘+수치+보너스), 에픽 환생 카드.
  - **스킬 탭 & 승급 모달**: 5종 세그먼트 탭(전체/근접/활/마법/공용), 카드형 스킬 슬롯, 랭크별 컬러 뱃지, 네온 수련 게이지 및 100% 펄스 애니메이션, 앤틱 승급 모달(설명·스펙 비교·수련체크·AP비용).
  - **인벤토리 탭 & 아이템 상세**: 상단 고정 골드바/정렬 칩, 아이템 카드 슬롯(카테고리 아이콘·장착중 뱃지·내구도 미니 게이지 바), 판타지 툴팁형 아이템 상세 모달.
  - **가드레일 검증**: 전체 1,164개 테스트 100% 통과, Spotless 정렬 및 CodeGraph 동기화 완료.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **배포 상태**: 프로덕션 배포 완료 (Port 8083 정상 서비스 중)
- **문서 동기화**: `docs/todo.md` 1번 섹션 '정보 / 스킬 / 인벤토리 탭 UI 개편' 완료 상태 반영.
- **5대 품질 가드레일 상태**: 전체 단위/프로퍼티 테스트 통과, Spotless 포맷팅 일치 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그 후보)

1. **`docs/todo.md` 1번 섹션 잔여 UI/UX 개선**:
   - **상점 편의성**: 상점 장비 돋보기 클릭 시 현재 착용 중인 장비와 비교 팝업 제공
   - **환경 연출**: 인게임 시간대별 웹 배경화면 색상 동적 전환
2. **`docs/todo.md` 2번 섹션 환경 & 시스템 확장 백로그**:
   - 인게임 시간 시스템, 야간 기습 50% 상향, 캠프파이어/야영, 필드 랜덤 보스