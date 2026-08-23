# Active Context

> 최종 업데이트: 2026-08-23 17:22 (Asia/Seoul)

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
- **012 디펜스 및 카운터 어택 스킬 재설계 (`.kiro/specs/myrpg/012-defense-counter-skill-redesign/`)**: 디펜스 100% 완전 방어(0 피격), 랭크별 스태미나 감소, 랭크업 영구 스탯 보너스 완료.
- **013 액티브 전조 반응 전투 시스템 (`.kiro/specs/myrpg/013-active-telegraph-combat/`)**: 2단계 턴 사이클, B안 직관형 전조 뱃지, 타임아웃 무방비 피격, 스킬 alert 제거, 전체 가드레일 검증 완료.
- **던전 미니맵 노드 색상 구분 개선 (`MapViewFactory`, `myrpg.css`)**: 시작방(파란색: `#6b93bf`), 보스방(빨간색: `#c08585`), 일반 클리어방(실버: `#e2e8f0`), 일반 미클리어방(슬레이트 회색: `#475569`) 적용 완료.

---

## 2. 현재 작업 맥락 및 상태

- **던전 미니맵 노드 색상 구분 수정 및 5대 가드레일 검증 완료**:
  - `MapViewFactory`에 `TYPE_DUNGEON_START`, `TYPE_DUNGEON_BOSS` 상수 및 미니맵/전체지도 셀 타입 매핑 적용.
  - `myrpg.css`에 `.type-dungeon-start` (파란색, 마을과 동일), `.type-dungeon-boss` (빨간색, 던전 입구와 동일) 스타일 반영.
  - `docs/todo.md` 해당 항목 완료 처리.

---

## 3. 최근 변경사항 및 확정 설계값 (Recent Changes & Decisions)

- **[2026-08-23] 던전 미니맵 노드 색상 적용**:
  - 시작방: `dungeon-start` (파란색 `#6b93bf`)
  - 보스방: `dungeon-boss` (빨간색 `#c08585`)
  - 일반 클리어 방: `dungeon-cleared` (밝은 실버 `#e2e8f0`)
  - 일반 미클리어 방: `dungeon-uncleared` (회색 `#475569`)
- **[2026-08-23] `docs/todo.md` 잔여 핵심 시스템 기획 확정**:
  - **던전 연쇄 전투(Cascade Battle) 메카닉**:
    - 방 대기 몬스터 소모가 아닌, 처치한 몬스터와 동일 개체의 추가 난입(체인 스폰) 연속 전투.
    - 연쇄 전투가 몇 차례 발생하든 승리 후 방 복귀 시 **원래 교전했던 1마리만 방 목록에서 차감**되어 잔여 대기 몬스터(`[흰 거미, 붉은 거미]`) 정상 보존. N차 연속 발동 지원.
  - **디펜스 스킬 메카닉 재설계**:
    - 100% 완전 방어(0 피해) 폐지 $\rightarrow$ 캐릭터 방어력(DEF) + 랭크별 피해 경감률(%) 기반 공식으로 개편.
    - 디펜스 방어 성공 시 다음 턴 확정 선제 공격권 부여 (유저/몬스터 공통 적용).
  - **경험치 곡선 완화 & 로그 수량 표기**:
    - 초중반 레벨업 필요 경험치 곡선 완화.
    - 다중 개수 드랍(예: 포션 3개) 시 활동 로그에 `(3개)` 수량 명시.
  - **던전 클리어 결과 팝업 UI**:
    - 클리어 시 전용 모달 팝업으로 보상 목록 시각화 후 확인/닫기 시 입구 이동 및 로그 기록.

---

## 4. 다음 단계 (Next Steps)

1. **`docs/todo.md` 1번 섹션 잔여 태스크 진행**:
   - **던전 연쇄 전투 및 잔여 몬스터 처리 로직 개선** (최초 1마리만 차감, N차 연쇄 발동 지원)
   - **던전 클리어 보상 결과 팝업 UI**
2. **2번 섹션**: 디펜스 스킬 메카닉 재설계 (DEF 비례 피해 경감 & 확정 선제권)
3. **3번 섹션**: 성장 & 보상 밸런스 및 로그 편의성 개선 (경험치 곡선 완화, 드랍 수량 명시)