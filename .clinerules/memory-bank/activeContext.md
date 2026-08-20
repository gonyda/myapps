# Active Context

> 최종 업데이트: 2026-08-20 (Asia/Seoul)

## 0. 핵심 전역 규칙 (`.clinerules/cline-global-rules.md` 반영)

> **전역 규칙 핵심 요약**:
> - 개발 워크플로우: **Spec 문서 3종 → 사용자 검토 → 구현**
> - **MCP 툴 우선 사용** (코드 탐색 시 `codegraph_explore` MCP 1순위)
> - **빌드 검증**: 각 Task 완료 전 `mvn test` + `mvn clean install` `BUILD SUCCESS` 확인
> - 소스 정리: 미사용 import 제거, 매직넘버 상수화, 메서드 분리 (50줄 초과 시)

## 1. 완료된 작업 (Spec 010: NPC 행동 실기능)

**스펙명**: `myrpg/010-npc-actions-shop-repair-heal` — NPC 행동 실기능(상점 구매/판매 · 수리(대장간) · 치료(힐러집)) 및 인챈트 플레이스홀더

### 진행 상태 요약

| 단계 | 내용 | 상태 |
|---|---|---|
| A | 도메인 모델 및 데이터 확장 (`InventoryService`·`NpcService`·`OwnedItem`·`item.json`·`npc.json`) | ✅ 완료 (Task 1~3) |
| B | 핵심 애플리케이션 서비스 (`ShopService`·DTO 5종) | ✅ 완료 (Task 4~6) |
| C | 컨트롤러 계층 + **테스트 7종(Property 6~9 + @WebMvcTest 3종)** | ✅ 완료 (Task 7~8) |
| D | UI 템플릿 및 정적 리소스 (`shop-popup.html`·`repair-popup.html`·`center.html`·`myrpg.css`·`myrpg.js`) | ✅ 완료 (Task 9~11) |
| E | 최종 검증 및 회귀 확인 | ✅ 완료 (Task 12) |

### Task 11 (D단계 체크포인트) 결과
- `mvn test -pl myrpg`: **928 tests pass, 0 failures** ✅
- `mvn clean install -pl myrpg -am`: **BUILD SUCCESS** ✅

### Task 12 (최종 검증) 결과
- `mvn test -pl myrpg`: **928 tests pass, 0 failures** ✅ (회귀 없음)
- `mvn clean install -pl myrpg -am`: **BUILD SUCCESS** ✅
- Code-style 정리: 수정된 Java 파일은 test 파일 2종(`NpcContextLoadSmokeTest.java`, `VisualJsPreservationAndJsonLoadingIntegrationTest.java`)이며, 미사용 import/변수 없음. HTML/CSS/JS는 code-style 대상 아님
- 기존 001~009 기능(전투, 인벤토리, 스킬, 은행 등) 모두 무회귀 확인

---

## 작업 트리 (미커밋 상태)

스펙 010의 D/E 단계 산출물은 아직 커밋되지 않았다. 스테이징되지 않은 변경사항:

| 파일 | 변경 |
|---|---|
| `.clinerules/memory-bank/activeContext.md` ✅ (staged) | 메모리뱅크 갱신 |
| `.kiro/specs/myrpg/010-npc-actions-shop-repair-heal/tasks.md` | 체크박스 완료 갱신 |
| `myrpg/src/main/resources/static/css/myrpg.css` | 상점/수리 팝업 스타일 추가 |
| `myrpg/src/main/resources/static/js/myrpg.js` | NPC 행동 라우팅, openShop/closeShop/openRepair/closeRepair/heal/refreshTopBar |
| `myrpg/src/main/resources/templates/fragments/center.html` | talkingNpcId onclick 전달 |
| `myrpg/src/main/resources/templates/fragments/repair-popup.html` | 신규 프래그먼트 |
| `myrpg/src/main/resources/templates/fragments/shop-popup.html` | 신규 프래그먼트 |
| `myrpg/src/main/resources/templates/play.html` | shop-popup/repair-popup include |
| `myrpg/src/test/java/.../NpcContextLoadSmokeTest.java` | shopItems 로딩 + action beans 스모크 검증 |
| `myrpg/src/test/java/.../VisualJsPreservationAndJsonLoadingIntegrationTest.java` | 신규 JS 함수/프래그먼트 검증 |

- **커밋 `3b5d1cb`**: spec 문서 3종 + 메모리뱅크
- **커밋 `b7ef2ed`**: 메모리뱅크 커밋 하이라이트 갱신
- **커밋 `aa8ea01`**: A/B/C 단계 산출물 (40 files, +3740)

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

## 스펙 010 완료 대상 (GlobalExceptionHandler 재사용)

- 골드 부족: `InsufficientGoldException`, 인벤토리 초과: `InventoryFullException`, 장착 충돌: `EquipConflictException` — 모두 기존 예외 재사용

## 워크플로우 규칙

- 각 Task 완료 전 `mvn test -pl myrpg` 통과 → `mvn clean install -pl myrpg -am` `BUILD SUCCESS` 확인 필수
- 생성자 주입만(`@Autowired` 금지), Lombok/`var` 금지, VO/DTO는 `record`, 커스텀 예외(`RuntimeException` 직접 금지)
- 소스 정리: 미사용 import 제거 · 매직넘버 상수(`private static final`) · 메서드 분리(50줄 초과 시)

## 최근 커밋 하이라이트 (main 브랜치)

| 해시 | 내용 |
|---|---|
| `aa8ea01` | feat(myrpg): NPC 상점 구매/판매·수리·치료 실기능 구현 (스펙 010 A/B/C 단계) |
| `b7ef2ed` | docs: 메모리뱅크 최근 커밋 하이라이트 갱신 |
| `3b5d1cb` | docs: 스펙 010 NPC 행동 실기능 spec 문서 3종 및 메모리뱅크 추가 |
| `1c86cad` | docs(rules): `.clinerules` 정리 (스티어링 참조 섹션 재구성) |
| `2fbed3` | fix: `formatDurability` Math.ceil 올림 처리 (M18) |
| `02c6a1d` | feat: 인벤토리 용량 제한, 아이템 이동/스택 PBT |
| `ce898e9` | 내구도 감소량 0.2→0.05 (M20) + 인벤토리 내구도 표시 개선 |
| `ca274ee` | feat(mycalendar): 달력 하단 주간 일정 섹션 |
| `0703df4` | feat: 첫 캐릭터 생성 시 초보자 장비 6종 자동 장착 |
| `559c6b9` | fix: 너구리 attackPower 48→42 하향 |

- 주요 스펙 커밋: 006 골드아이템(1fab054) → 007 몬스터(63d8aab) → 008 전투(a0a20355) → 009 스킬 차별화(4c89d7e) → 010 NPC 행동 실기능(aa8ea01 A/B/C + D/E 미커밋)

## 다음 단계 및 의사사항

1. **스펙 010 D/E 단계 커밋 필요** — 9개 파일 미커밋 상태, `feature` 브랜치 생성 후 PR 권장
2. 로드맵 갱신: `docs/todo.md` 7순위 NPC 기능 완료 표시
3. ⚠️ `data-balance-guide.md`(스티어링) 내구도 문구(0.2/100턴)가 실제 코드(0.05)와 불일치 — 추후 갱신 필요
4. 다음 스펙 예정: 스펙 011 (미정)

## 프로젝트 개관

- Monorepo (Maven multi-module): `myrpg`, `mycalendar`, `mycrawler`, `mystudy`
- 각 모듈 DDD 4계층: application·domain·infrastructure·interfaces