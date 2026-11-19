# Active Context

> 최종 업데이트: 2026-11-19 19:56 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` & `memory-bank/memory-bank.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(원칙·맥락) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(Compaction 갱신)**
> - **관리 원칙**: 슬라이딩 윈도우 & 이전 작업 압축(Compaction) 필수, 무한 누적 금지
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

---

## 1. 이전 완료 작업 요약 (Compacted)

- **핵심 시스템 & UI/UX 기반 완비 (001~019)**:
  - 알비 던전/전투 턴/데이터 격리, 모바일(360~480px) 팝업군(인벤/장비/상점/대치), 인게임 시간대 스카이 그라디언트, 메시지/프로퍼티 외부화.
- **스킬 다형화 & 거대 서비스 분해 (020~022, 커밋 `6252297`, `41f978f`, `f285060`)**:
  - `SkillCombatVisitor<R>` 다형 디스패치, `InventoryService`·`BattleService` 협력 객체 분해 및 파사드화, 1,313개 테스트 All-Green.
- **`mycalendar` GitHub 단방향 스냅샷 동기화 (`mycalendar/003-github-calendar-sync`, 2026-11-18)**:
  - `AFTER_COMMIT` + `SingleThreadExecutor` 직렬화로 `gonyda/mike` `calendar/mycalendar.json` 푸시(`MIKE_GITHUB_TOKEN`), `409 Conflict` 1회 재시도, 117개 테스트 All-Green.
- **`myshare` Mike HTML 공유 웹 서빙 모듈 신설 (`myshare/001-mike-html-share`, 2026-11-19)**:
  - 신규 Web 모듈 `myshare`(`com.myapps.web.myshare`, 포트 `8084`, DB/JPA AutoConfig 제외) 추가.
  - `RestClientGitHubShareClient`: `gonyda/mike`(`main`, `share/{slug}.html`) 읽기 전용 조회(`MIKE_GITHUB_TOKEN` 공유), Base64 UTF-8 디코딩, 1MB 초과 `raw+json` 폴백, `If-None-Match` 304 지원.
  - `InMemoryShareCache`(TTL 20s, 최대 200건) + `ShareController`(`GET /share/{slug}`, slug 정규식 `^[a-zA-Z0-9_-]{1,200}$` 검증 및 Path Traversal `400`, `/share` 인덱스 `404` 차단, 모바일 인라인 HTML 서빙).
  - 46개 테스트 및 5대 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo, PMD/CPD) All-Green (`BUILD SUCCESS`).

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **5대 품질 가드레일 상태**:
  - `myshare`: 46개 전체 테스트 All-Green (`BUILD SUCCESS`, Spotless/Error Prone/ArchUnit/JaCoCo/PMD/CPD 통과)
  - `mycalendar`: 117개 전체 테스트 All-Green
  - `myrpg`: 1,313개 전체 테스트 All-Green
- **CodeGraph**: `codegraph sync` 완료 (`Already up to date`)
- **현재 활성 작업**: `myshare/001-mike-html-share` 구현 및 검증 완료 (`main` 반영 및 운영 배포 진행)

---

## 3. 다음 단계 (Next Steps / 백로그)

### 3.1. `myshare` 운영 배포 및 프록시 연동
- Oracle Cloud VM `/home/ubuntu/app/deploy.sh`의 `get_port()`에 `myshare) echo 8084 ;;` 추가 및 `.env`에 `MIKE_GITHUB_TOKEN` 설정 확인 후 배포.
- 리버스 프록시(Nginx 등) `/share/` → `http://127.0.0.1:8084` 라우팅 매핑 적용.

### 3.2. `myrpg` 신규 게임 컨텐츠 기획 및 SDD 개발 연계
`skills/myrpg-system-design` 활용 신규 컨텐츠 기획서(GDD) 산출 및 SDD 워크플로우 연계:
1. **캠프파이어 & 야간 위험도 시스템**: 휴식 회복, 쉐어링 버프, 야간 몬스터 위험도
2. **마을 아르바이트 & 축복의 포션 시스템**: 성당/대장간/힐러집 알바, 축복 버프
3. **타이틀 & 업적 도감 시스템**: 칭호 수집 스탯 보너스, 업적 달성 연동
4. **기믹 던전 & 장비 인챈트/수리 연계**: 퍼즐/열쇠 기믹, 인챈트 스크롤, 대장간 수리
