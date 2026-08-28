# Active Context

> 최종 업데이트: 2026-08-27 22:15 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` & `memory-bank/memory-bank.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(원칙·맥락) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(Compaction 갱신)**
> - **관리 원칙**: 슬라이딩 윈도우 & 이전 작업 압축(Compaction) 필수, 무한 누적 금지
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

---

## 1. 이전 완료 작업 요약 (Compacted)

- **AI Agent 아키텍처 & 5대 품질 가드레일**: `AGENTS.md`, `rules/`, `skills/sdd/SKILL.md`, 5대 가드레일 구축.
- **주요 시스템 구축 (011~015 Spec)**: 알비 던전 시스템, 전조 반응형 전투 턴 사이클, 계정 및 세션 기반 데이터 격리 완료.
- **UI/UX 전면 개편 & 모바일 최적화 (360~480px)**: 상점·은행·수리 팝업, 스킬 팝업, 3단 전투뷰, 10개 스킬 슬롯 매트릭스, 3D 앤틱 맵 팝업 완료.
- **MyRPG 시스템 6대 개선 완료 (2026-08-27)**: 정보 팝업 환생 스크롤 최적화, 상황 멘트 시간대별 이모지 연동, 보스 👑 표기 일원화, 공용 스킬 👑 통일, 수리 멘트/내구도 밸런스 개선.
- **무기 세트 I / II (주무기 + 방패 페어 & 맨손) 스왑 시스템 완비 (2026-08-27)**:
  - `CharacterProgress`에 세트별 무기/방패 ID 영속화 및 스왑 시 방패(`OFF_HAND`) 동시 복원.
  - 빈손(맨손) 스왑 정상 허용 및 타 세트 배정 장비 픽커(Picker) 후보 제외 필터링 적용.
- **MyRPG 전문 게임 시스템 기획 스킬 (`myrpg-system-design`) 구축 (2026-08-28)**:
  - `myrpg/README.md` 기반 전문 게임 시스템/경제 기획자 페르소나 및 4대 핵심 설계 철학(판타지 라이프, 결정적 규칙, 지속 가능한 경제, 모바일 UX) 정립.
  - 5단계 파이프라인(현황 진단 → 3대 대안 제안 → 대화형 Q&A → 표준 GDD 작성 → SDD Spec 분해 이관) 구축.
  - `skills/myrpg-system-design/SKILL.md` (SSOT), `.agents/`, `.cline/` Thin Wrapper 완비.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **배포 상태**: **Oracle Cloud 프로덕션 배포 완료 (`DEPLOY_SUCCESS`, port 8083)**
- **5대 품질 가드레일 상태**: 1,225개 테스트 100% 통과, 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그)

1. **상점 장비 착용 비교 팝업 기능**:
   - 상점 물건 돋보기 클릭 시, 현재 착용 중인 동일 부위 장비(무기/방어구 등)를 위아래로 함께 띄워 스펙을 비교할 수 있도록 개선 (미착용 부위는 상점 장비만 단독 표시).
2. **인게임 시간대별 웹 배경화면 색상 동적 전환**:
   - 현재 세팅된 인게임 시간대(`TimeOfDay`: 새벽, 아침, 낮, 노을, 밤, 심야)에 따라 웹 전체 배경화면 색상이 동적으로 변경되도록 연출.
3. **게임 내 가상 시간 시스템 (In-Game Time) & 야간 위험도 증대 / 캠프파이어 시스템 (백로그)**.
