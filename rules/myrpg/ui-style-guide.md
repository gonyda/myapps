# MyRPG UI/UX 및 CSS 스타일 개발 가이드

---

## 1. UI/UX 디자인 철학 (Design Philosophy)

MyRPG의 UI/UX는 **마비노기 감성의 다크 판타지(Dark Fantasy)**와 **모바일 세로모드(Portrait) 최적화**를 핵심 가치로 삼습니다. 모든 신규 화면 및 팝업 구현 시 본 가이드의 표준을 반드시 준수하여 일관된 룩앤필을 유지해야 합니다.

### 3대 핵심 원칙
1. **다크 판타지 & 앤틱 골드 테마 (Dark Fantasy & Antique Gold)**:
   - 깊이감 있는 흑철색·다크 네이비 배경(`linear-gradient`)과 앤틱 골드(`#d4af37`, `#ffd700`) 악센트로 고전 판타지 RPG의 감성을 전달합니다.
   - 글래스모피즘(`backdrop-filter: blur(6px)`)과 부드러운 박스 섀도우를 통해 시각적 깊이를 부여합니다.
2. **모바일 세로모드(Portrait, 폭 360~480px) 100% 최적화**:
   - 높이는 `100dvh`, 너비는 `max-width: 480px` 컨테이너 내에서 반응형 유동 스케일(`clamp()`)을 적용합니다.
   - 가로 공간이 협소하므로 2~3열 매트릭스 그리드 또는 상하 스크롤 카드 구조를 기본으로 합니다.
3. **100% 원터치 모바일 인터랙션 (One-Touch Interaction)**:
   - **PC 마우스 호버(Hover), 우클릭, 드래그 앤 드롭에 의존하는 UX는 전면 금지**합니다.
   - 탭/클릭 한 번으로 상세 정보를 확인하고 행동할 수 있는 **슬라이드업 바텀시트(Bottom Sheet)** 및 **퀵 픽커 모달(Quick Picker)** 방식을 사용합니다.

---

## 2. 디자인 토큰 및 CSS 변수 (Design Tokens)

모든 CSS 작성 시 `clamp()` 기반의 루트 토큰을 재사용하여 기기 해상도에 비례해 유동적으로 크기가 조절되도록 합니다.

### 2.1. 유동형 스케일 토큰 (`myrpg.css` `:root`)
```css
:root {
    /* 간격 및 여백 */
    --gap: clamp(8px, 2.6vw, 14px);
    --pad: clamp(10px, 3.2vw, 16px);
    --radius: clamp(6px, 1.6vw, 9px);

    /* 폰트 크기 계층 */
    --fs-xs: clamp(9px, 2.6vw, 11px);    /* 부가 설명, 내구도, 스탯 뱃지 */
    --fs-sm: clamp(11px, 3.2vw, 13px);   /* 본문, 버튼 서브텍스트 */
    --fs-md: clamp(13px, 3.8vw, 15px);   /* 아이템명, 카드 타이틀, 주요 버튼 */
    --fs-lg: clamp(15px, 4.6vw, 18px);   /* 패널 헤더 제목, 대형 강조 수치 */

    /* 터치 및 컴포넌트 규격 */
    --touch: clamp(44px, 12vw, 52px);   /* 최소 터치 타겟 높이 */
    --bar-h: clamp(9px, 2.6vw, 12px);    /* 게이지 바 높이 */
    --log-h: calc((var(--fs-sm) * 1.6 + 5px) * 4 + var(--pad) * 1.2);
}
```

### 2.2. 표준 컬러 팔레트 (Color Palette SSOT)

| 분류 | 색상 코드 / 값 | 사용처 |
|---|---|---|
| **골드 메인** | `#d4af37`, `#ffd700`, `#b8860b` | 헤더 아이콘, 주요 테두리, 활성 탭, 강조 텍스트, 1순위 액션 버튼 |
| **골드 글로우/알파** | `rgba(212, 175, 55, 0.35~0.6)` | 장착 슬롯 글로우, 모달 테두리, 칩 포커스 |
| **패널 배경 그라디언트** | `linear-gradient(165deg, rgba(28, 34, 48, 0.98), rgba(14, 17, 26, 0.99))` | 팝업 패널, 바텀시트, 픽커 카드 배경 |
| **슬롯 배경** | `linear-gradient(145deg, rgba(30, 36, 50, 0.9), rgba(18, 22, 32, 0.95))` | 장비 슬롯, 인벤토리 아이템 카드, 스킬 카드 |
| **경계선 (Borders)** | `#334155` (기본), `#1e293b` (다크), `#475569` (서브) | 카드 및 구분선 |
| **텍스트 메인** | `#f8fafc` | 아이템명, 스킬명, 주요 타이틀 |
| **텍스트 서브** | `#cbd5e1` | 스탯 수치, 설명 본문 |
| **텍스트 힌트** | `#94a3b8` / `#64748b` | 라벨, 미개방/잠금 텍스트 |
| **생명력 (HP)** | `#ef4444` (배경/바), `#fca5a5` (텍스트) | HP 게이지, 해제/취소 위험 버튼 |
| **마나 (MP)** | `#0ea5e9` (배경/바), `#7dd3fc` (텍스트) | MP 게이지, 마법 스킬 |
| **스태미나 (SP)** | `#eab308` (배경/바), `#fde047` (텍스트) | SP 게이지, 근접/원거리 스킬 |
| **방어력 (DEF)** | `#22c55e` (배경/바), `#86efac` (텍스트) | DEF 뱃지, 내구도 안전 상태 |
| **내구도 위험/경고** | `#ef4444` (20% 이하), `#f59e0b` (50% 이하) | 내구도 게이지 바 색상 분기 |

---

## 3. 공통 컴포넌트 표준 규격 (Component Standards)

### 3.1. 전체화면 팝업 오버레이 (`.overlay` & `.panel`)
- **구조**:
  ```html
  <div class="overlay {name}-overlay" id="{name}Overlay" th:fragment="{name}-popup">
      <div class="panel {name}-panel">
          <header class="panel-header">
              <div class="panel-header-title">
                  <span class="panel-header-icon">⚔️</span>
                  <h3>패널 제목</h3>
              </div>
              <button class="panel-close" onclick="close{Name}()" aria-label="닫기">✕</button>
          </header>
          <div class="panel-body {name}-panel-body" id="{name}Content">
              <!-- 비동기 fragment 주입 영역 -->
          </div>
      </div>
  </div>
  ```
- **스타일 규칙**:
  - `z-index`: 기본 팝업 `1000`, 2차 상세 모달/바텀시트 `1100`, 토스트 `1200`.
  - `.panel-close`: `28x28px` 메탈릭 원형 버튼 (`border-radius: 50%`, `rgba(255, 255, 255, 0.08)`).
  - 스크롤: 패널 전체는 `overflow: hidden`, `panel-body`에 `overflow-y: auto` 적용.

### 3.2. 슬라이드업 바텀시트 (`.equipment-action-sheet` 등)
모바일에서 장비 착용 해제, 아이템 사용 등의 상세 액션을 처리할 때 사용합니다.
- **애니메이션**: `@keyframes sheetSlideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }`
- **구조**: 상단 라운드(`border-radius: 16px 16px 0 0`), 골드 상단 보더(`border-top: 2px solid #d4af37`), 하단 풀와이드 액션 버튼.

### 3.3. 퀵 픽커 모달 (`.picker-card`)
빈 슬롯 터치 시 착용 가능한 아이템 목록을 고르는 소형 팝업입니다.
- **너비**: `90%`, `max-width: 380px`
- **리스트 행**: 터치 시 원터치 반영, 우측 `[착용]` 버튼 제공.

### 3.4. 화면 중앙 토스트 알림 (`.equipment-toast`)
미구현 슬롯, 착용 불가 등의 상태를 사용자에게 부드럽게 안내합니다.
- **위치**: 화면 정중앙 (`top: 45%; left: 50%; transform: translate(-50%, -50%)`)
- **디자인**: 다크 배경 + 앤틱 골드 보더 + 골드 텍스트, 2.2초 후 자동 페이드아웃.

---

## 4. 팝업별 UI 레이아웃 패턴 (Popup Patterns)

### 4.1. 장비 팝업 (`fragments/equipment-popup.html`)
- **3x3 슬롯 매트릭스**:
  - `ACC1(🔒)`, `HEAD`, `ACC2(🔒)` / `MAIN_HAND([I|II🔒])`, `BODY`, `OFF_HAND([I|II🔒])` / `HANDS`, `FEET`, `ROBE(🔒)`
  - 장착 슬롯: 아이콘 + 장비명 + 내구도 미니 게이지 바 + 골드 글로우
  - 빈 슬롯: 실루엣 아이콘 + 부위명 + `+` 뱃지 (점선 보더)
  - 양손무기 점유 슬롯: 빗금 무늬 + `⛔ 양손무기 점유`
- **장비 종합 스탯 요약 카드**:
  - 공격(STR/DEX/INT/CRIT) 뱃지, 방어(DEF/HP/MP/SP) 뱃지, 장착 부위 수 및 평균 내구도.
- **하단 액션**: `[🎒 소지품 가방 열기]` 풀와이드 버튼 (인벤토리 단일 전환).

### 4.2. 인벤토리 팝업 (`fragments/inventory-popup.html`)
- **상단 고정 헤더**: 골드 잔액 카드 + 3종 정렬 칩(`[획득순 | 이름순 | 타입순]`).
- **아이템 리스트 그리드**: 카드형 슬롯 (아이템 아이콘, 수량 뱃지, 장착중 뱃지, 내구도 바).
- **아이템 상세 모달 (`fragments/item-detail.html`)**: 툴팁형 상세 정보 모달.

### 4.3. 스킬 팝업 (`fragments/skill-popup.html`)
- **5종 세그먼트 탭**: `[전체 | 근접전투 | 활 | 마법 | 공용]`
- **스킬 카드 슬롯**: 스킬 아이콘, 스킬명, 랭크 컬러 뱃지, 네온 수련치 게이지(100% 달성 시 펄스 애니메이션), `[승급]` 버튼.
- **승급 모달 (`fragments/rankup-modal.html`)**: 앤틱 승급 모달 (스펙 비교 표, 필요 AP, 골드 연출).

### 4.4. 정보 팝업 (`fragments/info-popup.html`)
- **영웅 프로필 카드**: 판타지 아바타, 닉네임, 현재 레벨/누적 레벨, 재능 뱃지, 보유 AP 칩.
- **3대 바이탈 바**: HP(빨강), MP(파랑), SP(노랑) 게이지.
- **5대 스탯 2열 그리드**: STR, DEX, INT, CRIT, DEF (아이콘 + 기본값 + 장비/스킬 보너스 뱃지).
- **에픽 환생 카드**: 누적 레벨 보존, 환생 조건 및 `[환생하기]` 액션.

---

## 5. JavaScript 인터랙션 및 프론트엔드 통신 원칙

### 5.1. 팝업 단일 전환 원칙 (Single Overlay Transition)
- 팝업 위에 다른 팝업이 무한정 중첩되어 모바일 화면을 가리는 것을 방지합니다.
- 장비 ➔ 인벤토리 전환 예시:
  ```javascript
  function openInventoryFromEquipment() {
      closeEquipment();   // 이전 팝업 닫기
      openInventory();    // 새 팝업 열기
  }
  ```

### 5.2. 비동기 Fragment DOM 교체 (Async Fetch & Swap)
- 모든 팝업 조작(착용, 해제, 사용, 승급)은 REST API(`POST`) 호출 후 반환된 Thymeleaf Fragment HTML을 지정 영역에 `innerHTML`로 즉시 교체합니다.
- 조작 완료 후 상단 바이탈 바 및 전투 스킬 버튼을 실시간 동기화합니다:
  ```javascript
  refreshTopBar();
  if (battleActive) {
      refreshBattleSkills();
  }
  ```

---

## 6. 신규 화면/컴포넌트 개발 시 체크리스트 (Agent Guard)

AI Agent가 MyRPG의 새로운 화면, 팝업, 모달을 추가하거나 수정할 때는 다음 항목을 반드시 확인합니다:

- [ ] **모바일 세로모드(360~480px) 뷰포트 적합성**: 가로 스크롤이 발생하지 않는가?
- [ ] **디자인 토큰 사용**: `myrpg.css`의 `:root` 변수(`--gap`, `--pad`, `--fs-*`, `--radius`)를 사용했는가? (임의의 px 하드코딩 지양)
- [ ] **컬러 팔레트 일치**: 앤틱 골드(`#d4af37`), 다크 그라디언트 배경, 텍스트 계층(`#f8fafc`, `#cbd5e1`, `#94a3b8`)을 준수했는가?
- [ ] **100% 원터치 모바일 인터랙션**: PC 전용 호버나 우클릭 없이 탭/터치만으로 조작 가능한가?
- [ ] **팝업 프레임 일관성**: 헤더 아이콘, h3 제목, 메탈릭 원형 닫기 버튼(`.panel-close`) 구조를 준수했는가?
- [ ] **비동기 갱신 및 단일 전환**: 팝업 중첩 없이 깔끔하게 전환되며 `refreshTopBar()` 등이 연동되는가?
