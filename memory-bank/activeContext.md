# Active Context

> 최종 업데이트: 2026-08-25 22:26 (Asia/Seoul)

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
- **TODO UI/UX 및 편의성 전면 개편**:
  - **정보 / 스킬 / 인벤토리 / 장비 탭 개편**: 앤틱 골드 프레임, 영웅 프로필 카드, 5종 스킬 세그먼트 탭 & 승급 모달, 상단 골드바/정렬 칩 & 아이템 상세 모달, 3x3 장비 팝업 완료.
  - **메인화면 상단바 & 좌측 사이드바 개편**: 앤틱 골드 레벨 뱃지, EXP 앰버 샤인 바, 3색 바이탈 게이지, 도어 로그아웃 칩, 4종 수직 탭, 컴팩트 치트 칩.
  - **하단 활동 로그(Action Log) UI/UX 개편 및 노이즈 로그 제거 완료**:
    - **노이즈 로그 8종 제거**: 장비 착용/해제, 보스방 경고, 룸 클리어, 힐러 치료, 수리 로그 삭제 (수리는 alert로 즉시 안내).
    - **핵심 성장/전투 피드백 집중**: 승리 정산 1줄 압축(`승리! EXP +N | Gold +N | 드랍템`), 레벨업(`🎉 레벨업! Lv.N 달성! (AP +N)`), 스킬승급(`✨ [스킬명] N랭크로 승급되었습니다!`), 포션명 명시(`포션 사용: 생명력 30 포션`).
    - **다크 판타지 UI**: `HH:mm:ss` 타임스탬프 + 5종 전용 컬러 뱃지(`[전투]`, `[성장]`, `[획득]`, `[던전]`, `[알림]`) + 앤틱 골드 상단 라인.
- **스킬 시스템 정합성 및 UI 편의성 개선 완료**:
  - **승급 버튼 활성화(`ready`) 조건 개선**: 스킬 목록 팝업의 승급 버튼 활성화 조건을 단순 `progressPercent == 100`에서 수련치 + 보유 AP를 모두 검증하는 `row.rankable()`로 수정. (패시브 스킬 및 수련 완료 스킬이 AP 부족 시 무조건 활성화되던 현상 해결)
  - **도메인 다형성(Effect-Driven Polymorphism)**: `SkillEffectRowView`, `SkillRankupBonusDelta` 신설 및 8종 스킬 도메인 모델에 다형적 인터페이스 구현.
  - **스탯 및 수련 조건 정합성 해결**: 메디테이션 패시브 스탯 정상 반영, 디펜스 막타 처치 면제, 패시브 안내 박스 렌더링.
  - **Python 밸런스 검증 도구 동기화**: `tools/balance/verify_skill.py` 8개 전 스킬 도메인(31종 스킬) 16키 및 단조성 검증 확장.
  - **가드레일 검증**: 전체 1,176개 테스트 100% 통과 (`BUILD SUCCESS`), PMD 인지 복잡도 0건, CodeGraph 동기화 완료.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **배포 상태**: 프로덕션 배포 완료 (Port 8083 정상 서비스 중)
- **5대 품질 가드레일 상태**: 1,176개 테스트 100% 통과, 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그 후보)

1. **`docs/todo.md` 1번 섹션 잔여 UI/UX 개선**:
   - **상점 편의성**: 상점 장비 돋보기 클릭 시 현재 착용 중인 장비와 비교 팝업 제공
   - **환경 연출**: 인게임 시간대별 웹 배경화면 색상 동적 전환
2. **`docs/todo.md` 2번 섹션 환경 & 시스템 확장 백로그**:
   - 인게임 시간 시스템, 야간 기습 50% 상향, 캠프파이어/야영, 필드 랜덤 보스, 무기 세트 스왑 시스템