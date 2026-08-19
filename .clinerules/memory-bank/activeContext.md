# Active Context

> 최종 업데이트: 2026-08-19 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`.clinerules/cline-global-rules.md` 반영)

> **전역 규칙 핵심 요약**:
> - 개발 워크플로우: **Spec 문서 3종 → 사용자 검토 → 구현**
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **빌드 검증**: 각 Task 완료 전 `mvn test` + `mvn clean install` `BUILD SUCCESS` 확인
> - 소스 정리: 미사용 import 제거, 매직넘버 상수화, 메서드 분리 (50줄 초과 시)

## 1. 현재 작업 중 (Spec 010: NPC 행동 실기능) + 전역 규칙 반영

**스펙명**: `myrpg/010-npc-actions-shop-repair-heal` — NPC 행동 실기능(상점 구매/판매 · 수리(대장간) · 치료(힐러집)) 및 인챈트 플레이스홀더

### 진행 상태 요약

| 단계 | 내용 | 상태 |
|---|---|---|
| A | 도메인 모델 및 데이터 확장 (`InventoryService`·`NpcService`·`OwnedItem`·`item.json`·`npc.json`) | ✅ 완료 (Task 1~3) |
| B | 핵심 애플리케이션 서비스 (`ShopService`·DTO 5종) | ✅ 완료 (Task 4~6) |
| C | 컨트롤러 계층 + **테스트 7종(Property 6~9 + @WebMvcTest 3종)** | ✅ **완료** (Task 7~8) |
| D | UI 템플릿 및 정적 리소스 (`shop-popup.html`·`repair-popup.html`·`center.html`·`myrpg.css`·`myrpg.js`) | 🔄 진행 중 (Task 9~11, 9.1 스텁 완료) |
| E | 최종 검증 및 회귀 확인 | ⬜ 미착수 (Task 12) |

### 다음 단계: D단계 Task 9~11 (UI 템플릿 및 정적 리소스)
- `shop-popup.html`: 은행의 모바일 세로 배치 패턴(상점물건 위 / 소지품 아래 / 골드 하단) 복제
- `repair-popup.html`: 인벤토리의 `.inventory-item`/`.item-info`/`.item-meta` CSS 클래스 재사용
- `center.html`: NPC 버튼 onclick에 `talkingNpcId` 주입 (`npcAction(label, npcId)` 연동)
- `play.html`: shop-popup, repair-popup 프래그먼트 include
- `myrpg.css`: 상점/수리 팝업 스타일
- `myrpg.js`: `npcAction` 분기, `openShop`, `openRepair`, `heal`, `refreshTopBar`

---

## 작업 트리 (미커밋 상태)

현재 `git status`상 **스펙 010의 A/B단계 결과물이 커밋되지 않은 상태**다.

- **수정됨**: `InventoryService.java`, `NpcService.java`, `Npc.java`, `NpcType.java`, `OwnedItem.java`, `item.json`, `npc.json`, `GoldItemContextLoadSmokeTest`, `ItemCatalogLoadIntegrationTest`, `NpcTypeTest`, `PlayScreenController.java` (talkingNpcId 바인딩)
- **신규 (Untracked)**: DTO 5종(`ShopView`, `ShopBuyItemView`, `ShopSellItemView`, `RepairView`, `RepairItemView`), `ShopService.java`, 컨트롤러 3종(`ShopController`, `RepairController`, `HealController`), 신규 테스트(`ShopServiceTest`, `ShopServiceSellValuePropertyTest`, `ShopServiceBuyValidationPropertyTest`, `ShopServiceSellEquippedProtectionPropertyTest`, `NpcTest`, `NpcServiceShopItemsParsingPropertyTest`, `NpcTypeActionLabelsPropertyTest`, `OwnedItemTest`, `OwnedItemRepairByPropertyTest`, `ItemCatalogServiceTest`)
- **신규 (Untracked)**: `.clinerules/memory-bank/` (본 메모리뱅크), `.kiro/specs/myrpg/010-npc-actions-shop-repair-heal/` (spec 문서 3종), `.clinerules/memory-bank/activeContext.md`

---
---

## 스펙 010 핵심 설계 (확정값)

- **판매가 모델** (`ShopService`): `기본가 + 인스턴스보너스 × 가중치`
  - `기본가` 배타 규칙: `buyPrice`는 `round(buyPrice × 0.5)` / 없으면 드랍 전용 `Σ(카탈로그 amount × weightOf)`
  - `weightOf`: CRITICAL=1, 그 외(STR·DEX·INT·DEF·HP·MP·STAMINA)=10(`WEIGHT`) — CRITICAL 0.1%단위 보정
  - 상수: `SELL_RATIO=0.5`, `WEIGHT=10`, `CRITICAL_WEIGHT=1`
- **수리**: 1포인트 수리(`OwnedItem.repairBy(amount, max)`, max 상한) + 성공 확률 95% 고정, 수리비 = 판매가 그대로 재사용, 실패 시 골드 환불 없음
- **치료**: 100골드 고정, HP/MP/스테미나 풀회복, 팝업 없이 `alert("치료되었습니다!")`
- **NPC별 상점**: `npc.json`에 `shopItems` optional + NPC 마법학교/학교는 빈 목록
- **item.json 신규**: `short_sword`(STR+8, buyPrice 300), `long_sword`(STR+12, buyPrice 700) — 초보 장비는 buyPrice 미지정(드랍 전용·상점 미판매)
- **Correctness Property 1~10**: 각각 독립 jqwik `@Property(tries=100)` + `Mockito.mock()` 직접 사용, 태그 주석 `Feature: 010-npc-actions-shop-repair-heal, Property {번호}: …` 부착

---

## 스펙 010 완료 대상 (GlobalExceptionHandler 재사용)

- 골드 부족: `InsufficientGoldException`, 인벤토리 초과: `InventoryFullException`, 장착 충돌: `EquipConflictException` — 모두 기존 예외 재사용

## 워크플로우 규칙

- 각 Task 완료 전 `mvn test -pl myrpg` 통과 → `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 필수
- 생성자 주입만(`@Autowired` 금지), Lombok/`var` 금지, VO/DTO는 `record`, 커스텀 예외(`RuntimeException` 직접 금지)
- 소스 정리: 미사용 import 제거 · 매직넘버 상수(`private static final`) · 메서드 분리(50줄 초과 시)

## 최근 커밋 하이라이트 (main 브랜치)

| 해시 | 내용 |
|---|---|
| `1c86cad` | docs(rules): `.clinerules` 정리 (스티어링 참조 섹션 재구성) |
| `2fbed3` | fix: `formatDurability` Math.ceil 올림 처리 (M18) |
| `02c6a1d` | feat: 인벤토리 용량 제한, 아이템 이동/스택 PBT |
| `ce898e9` | 내구도 감소량 0.2→0.05 (M20) + 인벤토리 내구도 표시 개선 |
| `ca274ee` | feat(mycalendar): 달력 하단 주간 일정 섹션 |
| `0703df4` | feat: 첫 캐릭터 생성 시 초보자 장비 6종 자동 장착 |
| `559c6b9` | fix: 너구리 attackPower 48→42 하향 |

- 주요 스펙 커밋: 006 골드아이템(1fab054) → 007 몬스터(63d8aab) → 008 전투(a0a20355) → 009 스킬 차별화(4c89d7e) → 현재 010 진행 중

## 다음 단계 및 의사사항

1. **D단계 실행**: UI 템플릿 및 정적 리소스 (Task 9~11) — `shop-popup.html`, `repair-popup.html`, `center.html` `talkingNpcId` 주입, `play.html` include, `myrpg.css`, `myrpg.js` (`npcAction` 분기, `openShop`, `openRepair`, `heal`, `refreshTopBar`)
2. **E단계**: 최종 검증 및 회귀 확인 (Task 12) — `@WebMvcTest` 컨트롤러 테스트 7종 + Property 6~9
3. 스펙 010 완료 후 **커밋** — 현재 A/B/C 산출물 미커밋 상태
4. 로드맵 갱신: `docs/todo.md` 7순위 NPC 기능 완료 표시(상세문서 `npc-actions-system.md` 삭제 — 현재는 보존 중)
5. ⚠️ `data-balance-guide.md`(스티어링) 내구도 문구(0.2/100턴)가 실제 코드(0.05)와 불일치 — 추후 갱신 필요
6. 스펙 순서 적용: 스펙 문서 3종 → 사용자 검토 → tasks.md 기반 구현

## 프로젝트 개관

- Monorepo (Maven multi-module): `myrpg`, `mycalendar`, `mycrawler`, `mystudy`
- 각 모듈 DDD 4계층: application·domain·infrastructure·interfaces