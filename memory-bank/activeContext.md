# Active Context

> 최종 업데이트: 2026-08-29 17:05 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`AGENTS.md` & `memory-bank/memory-bank.md` 참조)

> **AI Agent 교체 가능 아키텍처**:
> - 개발 워크플로우: **memory-bank(원칙·맥락) → SDD 3종 Spec → 사용자 검토 → 구현 → 5대 가드레일 검증 → memory-bank(Compaction 갱신)**
> - **관리 원칙**: 슬라이딩 윈도우 & 이전 작업 압축(Compaction) 필수, 무한 누적 금지
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **5대 품질 가드레일**: Spotless + Error Prone + ArchUnit + JaCoCo + PMD/CPD
> - **CodeGraph Sync**: 변경 후 `codegraph sync` 필수

---

## 1. 이전 완료 작업 요약 (Compacted)

- **핵심 시스템 & UI/UX 완비**: 알비 던전/전투 턴/데이터 격리, 모바일(360~480px) 최적화 팝업군, 무기 세트 I/II 스왑, 스킬 슬롯 해제 방어, 상점 장비 착용 비교 팝업 구축 완료.
- **인게임 시간대별 앰비언트 스카이 & 천체 궤적 (2026-08-28)**: 6대 시간대(`TimeOfDay`) 딥 다크 스카이 그라디언트, 천체(해/달) 궤적 및 소프트 글로우 동적 연출 완비.
- **다중 계정/캐릭터 간 장비 공유 및 장착 해제 간섭 버그 해결 (2026-08-29)**: `InventoryService` 격리 조회 일원화 및 격리 테스트 5종 완료.
- **상황 멘트 및 NPC 대사의 게임 내 시간(InGameTime) 불일치 버그 해결 (2026-08-29)**: `CharacterProgress.getInGameHour()` 전달로 인게임 시간대 100% 동기화.
- **6대 시간대 구간 최적화 조정 (2026-08-29)**: 오후/황혼/밤/심야/새벽/오전 6구간 통일.
- **스킬 승급 조건 단순화 (막타 처치 항목 전면 제거 & 사용 횟수 단일화) (2026-08-29)**:
  - `RankUpRequirement(requiredUsage)` 단일화, `killCount` 및 막타 수련 로직 완전 제거.
- **스킬 목록 팝업 내 스킬 유형(Type) 뱃지 표기 (2026-08-29)**:
  - `SkillRowView` 및 `SkillRankUpView`에 `typeName`, `typeLabel` 필드 추가 및 하위호환 생성자 제공.
  - `SkillService.buildRow` / `buildRankUpView`에서 `catalog.type().name()` 및 `catalog.type().label()` 바인딩.
  - `skill-popup.html`의 스킬 행 헤더(`skill-title-group`) 및 승급 모달(`rankup-name-row`)에 10대 스킬 유형별 다크 판타지 컬러 뱃지(`<span class="skill-type-badge">`) 적용 및 우측 중복 텍스트 정돈.
  - `myrpg.css`에 10대 스킬 유형별(.type-normal, .type-heavy, .type-defense, .type-recovery, .type-ultimate, .type-passive, .type-buff, .type-debuff, .type-cc, .type-dot) 테마 스타일 구축.
  - `SkillServiceViewTest` 등 전체 1242개 단위/통합 테스트 100% 성공 및 5대 품질 가드레일 올클리어.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **5대 품질 가드레일 상태**: 전체 멀티모듈 (`mystudy`, `mycalendar`, `myrpg`) 100% 그린, 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그)

1. **[기획 1] 캠프파이어 & 야간 위험도 시스템 (표준형)**:
   - 야간(20~05시) 기습 50% & 장작 소비형 모닥불 야영(아침 08:00 스킵, 바이탈 완충, 음식 굽기 버프).
2. **[기획 2] 마을 아르바이트 & 축복의 포션 시스템**:
   - 시간대별 NPC 일일 의뢰 및 축복의 포션(내구도 보호) 보상 (추후 상세설계).
3. **[기획 3] 타이틀(칭호) & 업적 도감 시스템**:
   - 업적 기반 고유 칭호 장착 및 스탯 보너스, 타이틀 도감 팝업.
4. **[기획 4] 필드 보스 랜덤 스폰 (Field Boss Encounters)**:
   - 필드 랜덤 시간 + 랜덤 위치 보스 등장.
5. **[기획 5] 인챈트 & 세공 장비 커스터마이징 시스템**:
   - 접두/접미 인챈트 스크롤 및 마법 가루 성공률, 세공 옵션 부여.
