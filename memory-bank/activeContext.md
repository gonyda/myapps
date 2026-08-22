# Active Context

> 최종 업데이트: 2026-08-23 06:12 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` & `memory-bank/memory-bank.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(원칙·맥락) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(Compaction 갱신)**
> - **관리 원칙**: 슬라이딩 윈도우 & 이전 작업 압축(Compaction) 필수, 무한 누적 금지
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

---

## 1. 이전 완료 작업 요약 (Compacted)

- **AI Agent 교체 가능 아키텍처 & 품질 가드레일**: `AGENTS.md`, `rules/`, `memory-bank/`, `skills/sdd/SKILL.md`, PMD/CPD 플러그인 도입 및 `.agents/mcp_config.json` 로컬화 완료.
- **011 알비 던전 시스템 (`.kiro/specs/myrpg/011-dungeon-system/`)**: 프로시저럴 던전 생성 엔진(`DungeonGenerator`), 안개 탐색(Fog of War), 연쇄 전투(10%), 턴제 방 이동 및 백트래킹 완료.
- **012 디펜스 및 카운터 어택 스킬 재설계 (`.kiro/specs/myrpg/012-defense-counter-skill-redesign/`)**: 디펜스 100% 완전 방어(0 피격), 랭크별 스태미나 감소(5→1), 랭크업 영구 DEF/HP 스탯 보너스 완료.
- **맵 & 몬스터 데이터 조정 / 신규 몬스터 (`docs/todo.md` 2번)**: `흰 거미` 개명/하향, `공동 묘지` 노드, `붉은 여우` 추가, 포션 3종 드랍 완료.
- **테스트 & 디버깅 치트 버튼 (`docs/todo.md` 4번)**: `+1K EXP`, `+1K Gold` 엔드포인트 및 네비바 UI 연동 완료.
- **데이터 밸런스 가이드 Skill (`skills/myrpg-data-balance/SKILL.md`)**: Q&A + 파이썬 자동 검증 워크플로우 구축 완료.

---

## 2. 현재 작업 맥락 및 상태

- **스펙 013: 액티브 전조 반응 전투 시스템 (`.kiro/specs/myrpg/013-active-telegraph-combat/`) 진행 중**:
  - **카운터 어택 밸런스 정상화 완료**: 상대 공격력 기준에서 **내 공격력 기준**으로 전환, 반격 배율 F 90% ➡️ MASTER 160%, 크리티컬 보너스 +10%p(+100), 5대 가드레일 및 1,047개 테스트 전수 통과 완료.
  - **전투 전략성 및 가위바위보 심리전 고도화 기획 확정**:
    1. **2단계 턴 사이클 (대치 ⏸️ ↔ 공방 ⚡)**:
       - **대치 페이즈 (시간 정지)**: 시간에 쫓기지 않고 포션 복용, 장비 스왑, 도망, 로그 확인 수행. 스킬 비활성화 & `[⚔️ 공방 개시]` 버튼 활성화.
       - **공방 페이즈 (1.0~1.5초 실시간 피지컬)**: 몬스터 전조 뱃지 노출 + 실시간 카운트다운 타이머 게이지 작동. 시간 내 상성 스킬 선택.
    2. **스킬별 전역 공통 시간 (SSOT)**: 일반공격 `1.0초` / 강공격 `1.5초` / 방어태세 `1.5초`.
    3. **전조 연출 B안 (초심플 직관형 Visual Badge)**: 0.1초 인지 가능한 `[ ⚡ 일반공격 태세 ]`, `[ 💥 강공격 차징 중! ]`, `[ 🛡️ 방어 태세 ]` 색상/아이콘 뱃지 + CSS 게이지 바.
    4. **규칙 및 예외 처리**: 타임아웃 시 몬스터 의도 100% 성공(무방비 피격), 자원 부족 시 alert 없이 활동로그만 기록 & 시간 내 재선택, 도망 실패 시 몬스터 일반공격 1회 피격 후 대치 복귀, 활 1턴 선제 사격 스킬 선택 보장, 법사 10% 캐스팅 실패 유지.
  - **SDD 문서 현황**:
    - `requirements.md`: 작성 및 사용자 승인 완료.
    - `design.md`: 코드 레벨 상세 설계 작성 및 실제 소스코드 정합성 검증/보완 완료.
    - `tasks.md`: 점진적 5단계 마일스톤(도메인/DTO → 서비스 → 컨트롤러 → 프론트엔드 → 5대 가드레일 통합 검증) 작성 완료.

---

## 3. 최근 변경사항 및 확정 설계값 (Recent Changes & Decisions)

- **[2026-08-23] 카운터 어택 데미지 공식 및 랭크 밸런스 정상화 (구현 및 검증 완료)**
  - `BattleResolver.java`: `resolveCounterAttackWins`에서 `input.playerAttackPower()` 사용.
  - `skill.json`: `counter_attack` 배율 F 90% ~ MASTER 160%, 크리티컬 F +0.0%p ~ MASTER +10.0%p(+100), 스태미나 8.
  - `verify_skill.py` 파이썬 밸런스 검증 및 5대 품질 가드레일(Spotless, Error Prone, ArchUnit, JaCoCo 80%+, PMD/CPD) 전수 통과.
- **[2026-08-23] SDD 표준 Spec 템플릿 3종 구축 및 SKILL.md 연동 완료**
  - `.kiro/specs/` 하위에 `_requirements.md`, `_design.md`, `_tasks.md` 표준 템플릿 생성.
  - `skills/sdd/SKILL.md` 및 `rules/project/spec-conventions.md`에 새 스펙 작성 시 템플릿 복사 및 필수 사용 규칙 명시.
- **[2026-08-23] 013 액티브 전조 반응 전투 시스템 Spec 작성 완료**
  - `.kiro/specs/myrpg/013-active-telegraph-combat/requirements.md`: 2단계 사이클, B안 뱃지, 공통 시간(1.0s/1.5s), 타임아웃, 예외 처리 명세.
  - `.kiro/specs/myrpg/013-active-telegraph-combat/design.md`: `BattleState`(intent, standby), `BattleView`, `BattleService`(startClash, takeTurn timeout), `BattleController`(/battle/clash), `battle-view.html`, `myrpg.js`, `myrpg.css` 설계 완료.
  - `.kiro/specs/myrpg/013-active-telegraph-combat/tasks.md`: 점진적 5단계 마일스톤 및 체크포인트 작업 명세 완료.

---

## 4. 다음 단계 (Next Steps)

1. **`013-active-telegraph-combat/tasks.md` 작성**: 체크포인트 단위 작업 목록 정의 및 사용자 검토/승인.
2. **tasks.md 순차 구현 & 5대 품질 가드레일 검증**:
   - Task 1: 도메인 엔티티(`BattleState`) & DTO(`BattleView`) 확장
   - Task 2: `BattleService` 공방 개시(`startClash`) 및 타임아웃/턴 해결 로직 구현 및 단위 테스트
   - Task 3: `BattleController` `/battle/clash` 엔드포인트 구현 및 웹 슬라이스 테스트
   - Task 4: 프론트엔드 Thymeleaf 템플릿(`battle-view.html`), CSS 애니메이션, `myrpg.js` 타이머 연동
   - Task 5: E2E 통합 테스트 및 5대 품질 가드레일(`spotless`, `clean install`, `codegraph sync`) 검증
3. **Memory Bank Compaction 및 완료 보고**.