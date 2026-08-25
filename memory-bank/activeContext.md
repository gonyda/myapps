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
- **TODO 1번 섹션 UI/UX 개선 (정보 / 스킬 / 인벤토리 / 장비 탭 RPG 테마 전면 개편)**:
  - **정보 / 스킬 / 인벤토리 탭 개편**: 앤틱 골드 프레임, 영웅 프로필 카드, 5종 스킬 세그먼트 탭 & 승급 모달, 상단 골드바/정렬 칩 & 아이템 상세 모달 완료 (`2354ef6`).
  - **3x3 마비노기 스타일 장비 팝업 & 스마트 장착 시스템**:
    - **모바일 세로모드 3x3 매트릭스 슬롯**: 악세1(🔒), 머리, 악세2(🔒) / 주무기(`[I|II🔒]`), 갑옷, 보조손(`[I|II🔒]`) / 손, 발, 로브(🔒).
    - **슬롯 상태 시각화**: 장착(아이콘+장비명+내구도 컬러 바), 빈 슬롯(실루엣+`+`버튼), 양손무기 점유(⛔), 미개방(🔒).
    - **장비 종합 스탯 요약 카드**: 공격(STR/DEX/INT/CRIT), 방어(DEF/HP/MP/SP), 무기 계열 및 평균 내구도 요약.
    - **모바일 원터치 인터랙션**: 장착 슬롯 터치 시 슬라이드업 바텀시트(`[↩️ 장착 해제]`), 빈 슬롯 터치 시 착용 가능 후보 픽커 모달, 하단 풀와이드 `[🎒 소지품 가방 열기]` 단일 전환.
    - **스마트 무기 스왑**: 양손무기(활/양손검/스태프) 착용 시 방패 자동 해제, 방패 착용 시 양손무기 자동 해제.
  - **가드레일 검증**: 전체 1,173개 테스트 100% 통과 (`BUILD SUCCESS`), 5대 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo, PMD/CPD) 올클리어, CodeGraph 동기화 완료.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **배포 상태**: 프로덕션 배포 완료 (Port 8083 정상 서비스 중)
- **5대 품질 가드레일 상태**: 1,173개 테스트 100% 통과, 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그 후보)

1. **`docs/todo.md` 1번 섹션 잔여 UI/UX 개선**:
   - **상점 편의성**: 상점 장비 돋보기 클릭 시 현재 착용 중인 장비와 비교 팝업 제공
   - **환경 연출**: 인게임 시간대별 웹 배경화면 색상 동적 전환
2. **`docs/todo.md` 2번 섹션 환경 & 시스템 확장 백로그**:
   - 인게임 시간 시스템, 야간 기습 50% 상향, 캠프파이어/야영, 필드 랜덤 보스