# Active Context

> 최종 업데이트: 2026-08-24 00:18 (Asia/Seoul)

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

- **014 스킬 시스템 확장 (`.kiro/specs/myrpg/014-skill-system-expansion/`) 전체 구현 및 검증 완료**:
  - **Task A (도메인 & 계산 정책)**: `SkillType` 10종, 8종 Sealed Records(`Skill`), JPA 엔티티(`BattleState`, `CharacterSkill`), `SkillRankupBonus`(패시브 선형 누적 & 메디테이션 MP 회복), `SkillRankPolicy`(4대 수련 조건), `SkillDamagePolicy`, `BattleResolver`(방어 관통 0 & 3연타/5연타), PBT 검증 완료.
  - **Task B (카탈로그 & 애플리케이션 서비스)**: 35종 카탈로그 완비, 8종 Jackson 3 파싱, `SkillService` 육성/필드 힐링/승급 분기, `BattleService` 특수 턴 해결(궁극기 절대우위/힐/마나실드/CC/도트/디버프/메디테이션 재생/승리 쿨타임 차감), 단위/PBT 테스트 완료.
  - **Task C (웹 컨트롤러 계층)**: `SkillRowView.java` (`fieldUsable`, `cooldownBadgeText`), `BattleSkillButton.java` (`ultimateCooldown`, `ready`), `SkillController.java` (`POST /skills/{id}/use`), 컨트롤러 슬라이스 테스트 완료.
  - **Task D (프론트엔드 UI/UX)**: `skill-popup.html` (`공용` 탭 디펜스+패시브, `[사용]` 버튼, 궁극기 쿨다운 뱃지), `battle-view.html` (궁극기 쿨다운 잠금 뱃지, 준비 완료 황금 펄스), `myrpg.js` (`useFieldSkill`), `myrpg.css` (.skill-use-btn, .badge-cooldown, @keyframes ultimate-ready-pulse), UI 보존 통합 테스트 통과.
  - **Task E (5대 가드레일 & 통합 검증)**: 전체 1,128개 테스트 100% PASS, Spotless + Error Prone + ArchUnit + JaCoCo(80%+) + PMD/CPD + CodeGraph sync 완료.

3. **`main` 브랜치 머지 및 프로덕션 배포 완료**:
   - 머지: PR #2 (`feature/014-skill-system-expansion` ➜ `main`, 커밋 `605c607`)
   - 배포: Oracle Cloud VM (`134.185.116.35:8083`) 자동 배포 (`deploy.sh myrpg`) 및 Health Check (200 OK) 완료

---

## 3. 다음 단계 (Next Steps)

1. **사용자 확인 및 다음 기능/스펙 진행**:
   - 배포된 `myrpg` 서비스 점검
   - 다음 신규 기능 개발(`docs/todo.md` 백로그 등) 또는 사용자 추가 요청 사항 처리