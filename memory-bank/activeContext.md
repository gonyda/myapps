# Active Context

> 최종 업데이트: 2026-09-02 22:05 (Asia/Seoul)

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
- **스킬 승급 조건 단순화 & 유형 뱃지 표기 (2026-08-29)**: `RankUpRequirement(requiredUsage)` 단일화, 10대 스킬 유형별 다크 판타지 컬러 뱃지 구축.
- **신규 아이템 '장작' 및 마을/필드 50% 나무 스폰 & 5초 채집 시스템 (2026-08-29, 017-firewood-gathering)**: `ItemType.MATERIAL`, `firewood` 아이템 등록, `GatheringService` (5 SP 소모, 50% 채집 성공/실패 롤), 5초 자동 완료형 채집 타이머 완비.
- **메시지 및 게임 프로퍼티 외부화 리팩토링 (2026-08-30, 018-message-and-properties-externalization)**: `messages.properties` 120여 개 키 외부화, `GameProperties` 바인딩, `GameMessageService` 구축.
- **화면 상단 상황 멘트 제거 및 잔여 코드 전수 정리 (2026-09-02, 019-remove-situation-ambience)**:
  - **프론트엔드**: `center.html`의 `<div class="situation" id="situation">` 마크업 및 `myrpg.css`의 `.situation` 스타일 전수 제거 (인게임 시간대별 앰비언트 스카이 배경 그라디언트, 해/달 천체 궤적, 24시간 시계는 온전히 보존).
  - **백엔드 DTO/헬퍼/어셈블러**: `PlayScreenView`에서 `ambience`/`ambienceEmoji` 컴포넌트 제거 및 생성자 정돈, `PlayScreenViewHelper.buildPlayScreen` 오버로드 단순화, `NodeViewAssembler` 및 `PlayScreenController`에서 `AmbienceService` 의존성 완전 제거.
  - **파일 영구 삭제**: `AmbienceService.java`, `AmbienceData.java`, `src/main/resources/data/ambience.json` 전수 삭제.
  - **테스트**: `AmbienceServiceTest` 및 PBT 3종 삭제, `InGameTimeNpcDialogueIntegrationTest` 분리 및 컨트롤러/헬퍼 테스트 전수 갱신.
  - **검증**: 5대 품질 가드레일 (Spotless, Error Prone, ArchUnit, JaCoCo 80%+, PMD/CPD) 및 `codegraph sync` All-Green 통과.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **Spec & Task 상태**: `019-remove-situation-ambience` 5개 Task 100% 완료
- **5대 품질 가드레일 상태**: `myrpg` 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그)

1. **[개선 1] 아이템 아이콘(이모지) 표시 불일치 수정 & 중앙 관리 (SSOT)**:
   - 상점 `[내 물품]`과 장비 팝업 간 이모지 표시 일원화 및 아이템 카탈로그 중앙 `icon` 필드 SSOT 구축.
2. **[기획 1] 캠프파이어 & 야간 위험도 시스템 (표준형)**:
   - 획득한 장작을 소비하여 모닥불 야영(야간 20~05시 위험 완화, 아침 08:00 스킵, 바이탈 완충, 음식 굽기 버프).
3. **[기획 2] 마을 아르바이트 & 축복의 포션 시스템**:
   - 시간대별 NPC 일일 의뢰 및 축복의 포션(내구도 보호) 보상 (추후 상세설계).
4. **[기획 3] 타이틀(칭호) & 업적 도감 시스템**:
   - 업적 기반 고유 칭호 장착 및 스탯 보너스, 타이틀 도감 팝업.
5. **[기획 4] 필드 보스 랜덤 스폰 (Field Boss Encounters)**:
   - 필드 랜덤 시간 + 랜덤 위치 보스 등장.
6. **[기획 5] 인챈트 & 세공 장비 커스터마이징 시스템**:
   - 접두/접미 인챈트 스크롤 및 마법 가루 성공률, 세공 옵션 부여.
