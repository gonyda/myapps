# Active Context

> 최종 업데이트: 2026-08-25 20:30 (Asia/Seoul)

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
- **TODO 1번 섹션 긴급 버그 2종 수정**: 선제공격 타임아웃 무피격/선제권 소멸 및 보조스킬 사용 시 몬스터 데미지 0 처리, 상점 장비 내구도 `MAX/MAX` 표기 완료.
- **TODO 3번 섹션 게임 데이터/콘텐츠 확장**:
  - `skill.json` 35종 전체 공식 명칭 대조 및 유저 친화적 설명 문구 개편 완료.
  - `map.json` 신규 지역 '두갈드 아일'(`dugald-isle`, y=2) 노드 신설 및 북-남 경로 연결 완료.
- **TODO 2번 섹션 UI/UX 및 편의성 개선 3종 구현 완료**:
  - 전투 뷰 스킬 목록 색상 구분(타입별 뱃지 일치) 및 정렬(일반 ➔ 강 ➔ 방어 ➔ 보조 ➔ 궁극기) 완료.
  - 연타 스킬 전투 로그 안 2 확정 포맷(`{스킬명}({타입}) {N}연타 ({d1} · {d2}💥) ➔ {합계} 피해`) 적용 완료.
  - 미니맵/전체 월드맵 연결선 밝은 톤 교체, 맵 간격(`--map-gap: 20px`) 확장 및 라벨 시인성 강화 완료.
- **TODO 1번 섹션 UI/UX 개선 (인벤토리 골드 분리)**:
  - 인벤토리 팝업 내 골드 표시 영역을 하단 스크롤 종속에서 상단 고정 바(`.inventory-gold-bar`)로 분리.
  - 아이템 목록만 독립 스크롤(`.inventory-scroll-container`)되도록 레이아웃 및 테마 스타일링 개선 완료.
- **프로덕션 원격 배포 완료**:
  - Oracle Cloud VM (134.185.116.35:8083) `deploy.sh myrpg` 실행을 통한 자동 빌드·재시작·헬스체크 통과 (`DEPLOY_SUCCESS`).

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **배포 상태**: 프로덕션 배포 완료 (Port 8083 정상 서비스 중)
- **문서 동기화**: `docs/todo.md` 1번 섹션 '인벤토리 골드 표시 영역 분리' 완료 상태 반영.
- **5대 품질 가드레일 상태**: 전체 단위/프로퍼티 테스트 통과, Spotless 포맷팅 일치 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그 후보)

1. **`docs/todo.md` 1번 섹션 잔여 UI/UX 개선**:
   - **UI 전면 개편**: 정보 / 스킬 / 인벤토리 탭 UI RPG스러운 스타일로 개선
   - **상점 편의성**: 상점 장비 돋보기 클릭 시 현재 착용 중인 장비와 비교 팝업 제공
   - **환경 연출**: 인게임 시간대별 웹 배경화면 색상 동적 전환
2. **`docs/todo.md` 2번 섹션 환경 & 시스템 확장 백로그**:
   - 인게임 시간 시스템, 야간 기습 50% 상향, 캠프파이어/야영, 필드 랜덤 보스