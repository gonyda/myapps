# Active Context

> 최종 업데이트: 2026-08-24 22:54 (Asia/Seoul)

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

---

## 2. 현재 작업 맥락 및 상태

- **015 유저 계정 및 간이 로그인 시스템 (`.kiro/specs/myrpg/015-user-authentication-and-login/`) 전체 구현 및 검증 완료**:
  - **도메인/데이터 계층**: `UserAccount` JPA 엔티티, `UserSession` 불변 Record, `OwnedItem.characterId` 다중 캐릭터 격리, `UserAccountRepository`, PBT Property 1 자격증명 일치성 검증 완료.
  - **애플리케이션 계층**: `AuthService` 기동 시 `bbsk`(기존 캐릭터 1번 연결) 및 `admin`(관리자 캐릭터 2번, 35종 전체 스킬 F랭크 일괄 습득 + 초보자 풀장비/포션 세트 지급) 자동 시드, PBT Property 2/3 어드민 스킬 무결성 및 인벤토리 격리 검증 완료.
  - **다중 계정 격리 전면 보강**:
    - `InventoryService`, `ShopService`, `BattleService`, `DungeonService`, `PlayScreenViewHelper`, `HealController`: 캐릭터별 `characterId` 바인딩, 장착/해제/포션사용/상점/전투장비보너스 격리 및 기본 캐릭터(1L) 하위 호환성 완벽 지원.
  - **웹/인프라 계층**: `AuthInterceptor` 미인증 브라우저 접근 302 리다이렉트 및 AJAX 401 Unauthorized 처리, `WebMvcConfig`, `AuthController` (`GET/POST /login`, `GET/POST /logout`), 전체 컨트롤러 세션 캐릭터 식별자 연동 완료.
  - **프론트엔드 UI/UX**: `login.html` 다크 판타지 글래스모피즘 로그인 카드, `[👤 bbsk (고니)]` 및 `[👑 admin (전스킬+풀장비)]` 퀵 로그인 원클릭 버튼, `top-bar.html` 로그아웃 버튼, `myrpg.css` 및 `myrpg.js` 스크립트 연동 완료.
  - **5대 품질 가드레일**: Spotless 포맷팅, Error Prone 결함 차단, ArchUnit 계층 아키텍처, JaCoCo(1,161개 테스트 통과, 80%+ 커버리지), PMD/CPD 전체 무결점 통과 (`BUILD SUCCESS`).
  - **CodeGraph 동기화**: `codegraph sync` 완료.

---

## 3. 다음 단계 (Next Steps)

1. **사용자 확인 및 서버 재기동 안내**:
   - `admin` 계정 로그인 시 장비 착용 목록 및 스킬, 인벤토리 정상 출력 확인
2. **Git 커밋 및 브랜치 병합/푸시**:
   - `feature/015-user-authentication-and-login` 브랜치 커밋 및 main 병합 검토