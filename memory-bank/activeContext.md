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
- **화면 상단 상황 멘트 제거 및 잔여 코드 전수 정리 (2026-09-02, 019-remove-situation-ambience)**: `AmbienceService`/DTO/JSON/테스트 완전 제거, 인게임 스카이 그라디언트/천체 궤적 보존, 5대 가드레일 통과.
- **아이템 아이콘(이모지) 일원화 및 Java Enum(SSOT) 중앙 관리 (2026-09-02)**:
  - **도메인 SSOT**: `ItemType`(`🧪`, `🗡️`, `🛡️`, `📦`) 및 `EquipmentKind`(`🗡️`, `🛡️`, `🥋`, `🏹`, `🔮`, `🪖`, `🧤`, `👢`)에 `emoji` 필드 중앙 정의, `Item.icon()` 인터페이스 및 `PotionItem`/`MaterialItem`/`EquipmentItem` 구현체 일원화.
  - **DTO 확장**: `OwnedItemView`, `ShopBuyItemView`, `ShopSellItemView`, `RepairItemView`, `DungeonClearItemView`에 `icon` 필드 추가 및 백엔드 뷰 조립 시 `item.icon()` 주입.
  - **프론트엔드 템플릿 단순화**: 상점(`shop-popup.html`), 인벤토리(`inventory-popup.html`), 은행(`bank-popup.html`), 수리(`repair-popup.html`), 장비 착용 피커(`equipment-popup.html`), 던전 보상(`dungeon-clear-modal.html`)의 불완전했던 40여 줄의 `th:if` 분기를 전수 제거하고 `th:text="${item.icon}"` 단일 바인딩으로 일원화. 장작 등 재료 아이템 `📦`, 장비탭 슬롯 실루엣과 100% 일치.
  - **검증**: 5대 품질 가드레일 (Spotless, Error Prone, ArchUnit, JaCoCo, PMD/CPD) 및 `codegraph sync` All-Green 완료.

---

## 2. 현재 작업 맥락 및 상태

- **현재 브랜치**: `main`
- **5대 품질 가드레일 상태**: `myrpg` 5대 가드레일 올클리어 (`BUILD SUCCESS`).
- **CodeGraph**: 최신 코드베이스와 동기화 완료 (`codegraph sync`).

---

## 3. 다음 단계 (Next Steps / 백로그)

1. **[기획 1] 캠프파이어 & 야간 위험도 시스템 (표준형)**:
   - 획득한 장작을 소비하여 모닥불 야영(야간 20~05시 위험 완화, 아침 08:00 스킵, 바이탈 완충, 음식 굽기 버프).
2. **[기획 2] 마을 아르바이트 & 축복의 포션 시스템**:
   - 시간대별 NPC 일일 의뢰 및 축복의 포션(내구도 보호) 보상 (추후 상세설계).
3. **[기획 3] 타이틀(칭호) & 업적 도감 시스템**:
   - 업적 기반 고유 칭호 장착 및 스탯 보너스, 타이틀 도감 팝업.
4. **[기획 4] 필드 보스 랜덤 스폰 (Field Boss Encounters)**:
   - 필드 랜덤 시간 + 랜덤 위치 보스 등장.
5. **[기획 5] 인챈트 & 세공 장비 커스터마이징 시스템**:
   - 접두/접미 인챈트 스크롤 및 마법 가루 성공률, 세공 옵션 부여.
6. **[기획 6] 도박 컨텐츠 (던바튼 한정)**:
   - 골드를 걸고 즐기는 도박 미니게임.
