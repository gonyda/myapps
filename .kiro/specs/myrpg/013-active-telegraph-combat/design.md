# 013 액티브 전조 반응 전투 시스템 (Design)

## 1. 아키텍처 및 시스템 변경 개요

```
[클라이언트 (myrpg.js & battle-view.html)]
   │
   ├── 1. [대치 페이즈 (시간 정지 ⏸️)]
   │      - 인벤토리/포션 복용, 장비 교체, 도망 자유롭게 수행
   │      - [⚔️ 공방 개시] 버튼 클릭 ──▶ POST /battle/clash
   │
   ├── 2. [공방 페이즈 (1.0~1.5초 실시간 피지컬 ⚡)]
   │      - B안 직관형 전조 뱃지 + 실시간 게이지 타이머 작동
   │      │
   │      ├── [시간 내 스킬 클릭] ──▶ POST /battle/turn?skillId={id} (clearTimeout)
   │      └── [시간 초과 (Timeout)] ──▶ POST /battle/turn?skillId=timeout
   │
   └── 3. [공방 해결 및 결과 수신]
          - 턴 결과 해결 (선제타 / 반격 / 가드브레이크 / 피격 / 무승부)
          - 전투 지속 시: 즉시 1번 [대치 페이즈 (시간 정지 ⏸️)]로 복귀
          - 전투 종료 시: 필드/던전 탐험 화면으로 복원
```

---

## 2. 데이터 모델 및 엔티티 변경

### 2.1. `BattleState` (JPA 엔티티 수정)
- 파일: [`myrpg/src/main/java/com/myapps/web/myrpg/domain/model/BattleState.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/domain/model/BattleState.java)
- **추가/수정 필드**:
  - `currentMonsterIntent` (`SkillType`, nullable): 이번 턴 몬스터가 준비한 행동 (`NORMAL`, `HEAVY`, `DEFENSE`). 대치 중일 때는 `null`이며, 공방 개시 시 확정 및 영속 저장.
  - `standby` (`boolean`, default `true`): 현재 대치 페이즈 여부 (`true` = 대치/시간정지, `false` = 공방 중).

```java
@Enumerated(EnumType.STRING)
@Column(name = "current_monster_intent")
private SkillType currentMonsterIntent;

@Column(name = "standby", nullable = false)
private boolean standby = true;

// Getter / Setter
public SkillType getCurrentMonsterIntent() { return currentMonsterIntent; }
public void setCurrentMonsterIntent(final SkillType currentMonsterIntent) { this.currentMonsterIntent = currentMonsterIntent; }
public boolean isStandby() { return standby; }
public void setStandby(final boolean standby) { this.standby = standby; }
```

---

### 2.2. DTO 설계 (`BattleView`)
- 파일: [`myrpg/src/main/java/com/myapps/web/myrpg/application/dto/BattleView.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/application/dto/BattleView.java)
- 뷰 모델 레코드 확장 (총 12개 필드):

```java
public record BattleView(
        String monsterName,
        int monsterLevel,
        int monsterCurrentHp,
        int monsterMaxHp,
        List<BattleSkillButton> skills,
        boolean fleeAvailable,
        boolean standby,
        SkillType monsterIntent,
        int clashDurationMs,
        String monsterStanceBadgeLabel,
        String monsterStanceBadgeClass,
        boolean bowFirstStrike) {}
```

#### 호출처 동기화 대상:
1. `BattleController.java` (`buildBattleView`): 대치 상태와 공방 상태에 맞춰 뷰 모델 생성.
2. `PlayScreenController.java` (line 138): 활성 전투 재개(`GET /`) 시 `state.isStandby()`를 반영하여 `BattleView` 생성.
3. 관련 단위/컨트롤러 테스트(`PlayScreenControllerBattleTest` 등).

#### 뱃지 라벨 및 스타일 매핑 (B안 SSOT):
- `SkillType.NORMAL`:
  - `clashDurationMs`: `1000` (1.0초)
  - `monsterStanceBadgeLabel`: `"⚡ 일반공격 태세"`
  - `monsterStanceBadgeClass`: `"badge-stance-normal"` (Amber/주황 네온)
- `SkillType.HEAVY`:
  - `clashDurationMs`: `1500` (1.5초)
  - `monsterStanceBadgeLabel`: `"💥 강공격 차징 중!"`
  - `monsterStanceBadgeClass`: `"badge-stance-heavy"` (Red 크림슨 펄스)
- `SkillType.DEFENSE`:
  - `clashDurationMs`: `1500` (1.5초)
  - `monsterStanceBadgeLabel`: `"🛡️ 방어 태세"`
  - `monsterStanceBadgeClass`: `"badge-stance-defense"` (Cyan 푸른색 방패)
- **대치 상태 (`standby = true`)**:
  - `clashDurationMs`: `0`
  - `monsterIntent`: `null`
  - `monsterStanceBadgeLabel`: `null`
  - `monsterStanceBadgeClass`: `null`

---

## 3. 핵심 비즈니스 로직 및 서비스 설계

### 3.1. `InventoryService` 착용 무기 재능 조회 지원
- 파일: [`myrpg/src/main/java/com/myapps/web/myrpg/application/service/InventoryService.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/application/service/InventoryService.java)
- 기존 `private SkillTalent resolveEquippedWeaponTalent()`를 활용하여 현재 착용 무기 재능을 제공하는 퍼블릭 메서드 추가:
  ```java
  public SkillTalent equippedWeaponTalent() {
      return resolveEquippedWeaponTalent();
  }

  public boolean isBowEquipped() {
      return resolveEquippedWeaponTalent() == SkillTalent.ARCHERY;
  }
  ```

---

### 3.2. `BattleService` 공방 개시 및 턴 해결 흐름

#### 1. 공방 개시 메서드 (`startClash`)
```java
@Transactional
public BattleView startClash(final CharacterProgress progress, final BattleState state) {
    final Monster monster = monsterService.byId(state.getMonsterId())
            .orElseThrow(() -> new IllegalStateException("몬스터 정보를 찾을 수 없습니다."));

    // 1. 착용 무기 기준 활 1턴 선제 사격 체크 (환생 재능이 아닌 착용 장비 기준)
    final boolean isBow = inventoryService.isBowEquipped();
    final boolean bowFirstStrike = state.getTurnCount() == 1 && isBow;

    // 2. 몬스터 의도 결정 (활 1턴 선제 사격 시에는 의도 없음/무방비)
    final SkillType intent = bowFirstStrike ? null : monsterAiService.nextAction();
    state.setCurrentMonsterIntent(intent);
    state.setStandby(false);
    battleStateRepository.save(state);

    return buildClashBattleView(state, monster, progress, intent, bowFirstStrike);
}
```

#### 2. 턴 진행 메서드 (`takeTurn` - 타임아웃 및 상성 해결)
- 파일: [`BattleService.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/application/service/BattleService.java#L189-L315)
- **타임아웃 (`skillId.equals("timeout")`) 처리**:
  ```java
  if ("timeout".equalsIgnoreCase(skillId)) {
      final SkillType monsterAction = state.getCurrentMonsterIntent() != null 
              ? state.getCurrentMonsterIntent() 
              : SkillType.NORMAL;
      
      final int monsterDamage = resolveMonsterOnlyDamage(progress, monster, monsterAction);
      applyDamage(progress, state, 0, monsterDamage);
      
      final List<String> combatLines = List.of(
              "시간 초과! 몬스터의 공격에 무방비로 피격되었습니다!",
              monster.name() + "의 " + monsterAction.label() + "! " + monsterDamage + " 피해를 입었습니다."
      );
      
      // 사망 체크, 턴 카운트 증가, standby = true 복귀 및 의도 초기화
      state.setCurrentMonsterIntent(null);
      state.setStandby(true);
      state.setTurnCount(state.getTurnCount() + 1);
      battleStateRepository.save(state);
      characterService.saveTurn(progress);
      
      // BattleTurnResult 반환...
  }
  ```
- **시간 내 스킬 선택 처리 및 하위 호환성 Fallback**:
  - `monsterAction`은 `state.getCurrentMonsterIntent()`를 우선 사용하며, `null`인 경우(직접 호출/기존 테스트) `monsterAiService.nextAction()`으로 안전하게 폴백:
    ```java
    final SkillType monsterAction = state.getCurrentMonsterIntent() != null 
            ? state.getCurrentMonsterIntent() 
            : monsterAiService.nextAction();
    ```
  - 상성 해결 후 `state.setStandby(true)`, `state.setCurrentMonsterIntent(null)`로 설정하여 **즉시 대치 페이즈로 복귀**.

#### 3. 자원 부족 시 처리
- 스태미나/마나 부족 시 턴을 끝내지 않고 `actionLog`에만 기록하며 `resourceInsufficient = true` 반환 (alert 팝업 없음, 실시간 카운트다운 유지되어 시간 내 다른 스킬 재선택 가능):
  ```java
  if (!hasEnoughResource(progress, resourceKind, resourceCost)) {
      actionLog.add(resourceKind.label() + "이(가) 부족합니다.", LOG_TYPE_COMBAT);
      return buildInsufficientResult(skill, resourceKind);
  }
  ```

#### 4. 도망 실패 시 일반 공격 고정
- `handleFleeFailure`에서 몬스터 데미지 계산 시 `SkillType.NORMAL`을 사용하여 1회 피격 후 `state.setStandby(true)` 상태로 대치 복귀.

---

## 4. 웹 컨트롤러 및 API 엔드포인트 설계

- 파일: [`BattleController.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/interfaces/api/BattleController.java)
- 파일: [`PlayScreenController.java`](file:///Users/gony/git/myapps/myrpg/src/main/java/com/myapps/web/myrpg/interfaces/api/PlayScreenController.java)

| 메서드 | URL | 설명 | 응답 뷰 |
|---|---|---|---|
| `GET` | `/` | 플레이 화면 진입 (전투 중 재접속 시 대치 상태로 렌더) | `play` (`battleActive=true`, `standby=true`) |
| `POST` | `/battle/start?monsterId={id}` | 전투 시작 (대치 페이즈 진입) | `fragments/battle-view :: battle-response` (`standby=true`) |
| `POST` | `/battle/clash` | `[⚔️ 공방 개시]` 클릭 시 공방 페이즈 시작 | `fragments/battle-view :: battle-response` (`standby=false`, 전조 노출) |
| `POST` | `/battle/turn?skillId={id}` | 스킬 선택 또는 `timeout` 턴 진행 후 대치 복귀 | `fragments/battle-view :: battle-response` (`standby=true`) |
| `POST` | `/battle/flee` | 대치 페이즈에서 도망 시도 | 성공 시 `center`, 실패 시 `battle-response` |
| `GET` | `/battle/skills` | 무기 스왑 후 스킬 목록 갱신 | `fragments/battle-view :: battle-skills` |

---

## 5. 프론트엔드 UI/UX 상세 설계

### 5.1. Thymeleaf 템플릿 (`battle-view.html`)
- 파일: [`myrpg/src/main/resources/templates/fragments/battle-view.html`](file:///Users/gony/git/myapps/myrpg/src/main/resources/templates/fragments/battle-view.html)

```html
<!-- 몬스터 전조 뱃지 & 실시간 게이지 영역 -->
<div class="battle-stance-area" th:id="'battleStanceArea'">
    <!-- 1단계: 대치 페이즈 (시간 정지 ⏸️) -->
    <th:block th:if="${battleView.standby}">
        <div class="stance-badge badge-standby">⏸️ 대치 중 (정비 가능)</div>
        <button class="btn-clash-start" onclick="startClash()">⚔️ 공방 개시</button>
    </th:block>

    <!-- 2단계: 액티브 공방 페이즈 (1.0~1.5초 실시간 피지컬 ⚡) -->
    <th:block th:unless="${battleView.standby}">
        <div class="stance-badge" th:classappend="${battleView.monsterStanceBadgeClass}"
             th:text="${battleView.monsterStanceBadgeLabel}">💥 강공격 차징 중!</div>
        
        <!-- 실시간 카운트다운 게이지 -->
        <div class="clash-timer-wrap">
            <div class="clash-timer-bar" id="clashTimerBar"
                 th:attr="data-duration=${battleView.clashDurationMs}"></div>
        </div>
    </th:block>
</div>

<!-- 전투 스킬 버튼 영역: th:fragment="battle-skills" 유지 및 null-safe 반복문 -->
<div id="battleSkills" th:fragment="battle-skills"
     class="battle-skills-grid"
     th:classappend="${battleView != null && battleView.standby ? 'disabled-skills' : ''}">
    <button th:each="skill : ${skills != null ? skills : battleView.skills}"
            th:text="${skill.label()}"
            th:attr="data-skill-id=${skill.id()}"
            class="battle-skill-btn"
            th:disabled="${battleView != null && battleView.standby}"
            onclick="battleTurn(this.getAttribute('data-skill-id'))">
        스킬
    </button>
</div>

<!-- 도망 버튼 (대치 페이즈에서만 노출) -->
<button th:if="${battleView.standby && battleView.fleeAvailable}"
        class="flee-btn" onclick="flee()">🏃 도망</button>
```

---

### 5.2. 클라이언트 자바스크립트 (`myrpg.js`)
- 파일: [`myrpg/src/main/resources/static/js/myrpg.js`](file:///Users/gony/git/myapps/myrpg/src/main/resources/static/js/myrpg.js#L420-L545)

```javascript
let clashTimerTimeoutId = null;

// 1. 공방 응답 DOM 교체 공통 함수 (신규 추출)
function swapBattleResponse(html) {
    if (!html) return;
    const container = document.createElement("div");
    container.innerHTML = html;

    const newTopBar = container.querySelector(".top-bar");
    const newCenter = container.querySelector(".center");
    const newActionLog = container.querySelector(".action-log");

    if (newTopBar) {
        const oldTopBar = document.querySelector(".top-bar");
        if (oldTopBar) oldTopBar.replaceWith(newTopBar);
    }
    if (newCenter) {
        const oldCenter = document.querySelector(".center");
        if (oldCenter) oldCenter.replaceWith(newCenter);
    }
    if (newActionLog) {
        const oldActionLog = document.querySelector(".action-log");
        if (oldActionLog) {
            oldActionLog.replaceWith(newActionLog);
            newActionLog.scrollTop = newActionLog.scrollHeight;
        }
    }

    handleTurnResultSignal(container);
}

// 2. 공방 개시 (POST /battle/clash)
function startClash() {
    fetch("/battle/clash", { method: "POST" })
        .then(r => r.text())
        .then(html => {
            swapBattleResponse(html);
            initClashTimer();
        });
}

// 3. 타이머 게이지 시작 및 타임아웃 예약
function initClashTimer() {
    const timerBar = document.getElementById("clashTimerBar");
    if (!timerBar) return;

    const durationMs = parseInt(timerBar.getAttribute("data-duration"), 10) || 1500;
    
    // CSS 애니메이션으로 게이지 100% -> 0% 선형 감소
    timerBar.style.transition = `width ${durationMs}ms linear`;
    requestAnimationFrame(() => {
        timerBar.style.width = "0%";
    });

    if (clashTimerTimeoutId) {
        clearTimeout(clashTimerTimeoutId);
    }

    // 시간 초과 시 자동으로 timeout 전송
    clashTimerTimeoutId = setTimeout(() => {
        battleTurn("timeout");
    }, durationMs);
}

// 4. 스킬 선택 시 턴 진행 (POST /battle/turn) - alert() 제거
function battleTurn(skillId) {
    if (clashTimerTimeoutId) {
        clearTimeout(clashTimerTimeoutId);
        clashTimerTimeoutId = null;
    }
    
    fetch("/battle/turn?skillId=" + encodeURIComponent(skillId), { method: "POST" })
        .then(r => r.text())
        .then(html => {
            swapBattleResponse(html);
        });
}

// 5. 도망 시도 (POST /battle/flee)
function flee() {
    if (clashTimerTimeoutId) {
        clearTimeout(clashTimerTimeoutId);
        clashTimerTimeoutId = null;
    }
    fetch("/battle/flee", { method: "POST" })
        .then(r => r.text())
        .then(html => {
            swapBattleResponse(html);
        });
}
```

> **자원 부족 및 불필요한 Alert 제거**:
> 기존 `handleTurnResultSignal`에서 `resourceInsufficient` 발생 시 띄우던 `alert("MP/스태미나가 부족합니다!")` 팝업을 제거하여, 타이머가 멈추지 않고 실시간으로 다른 스킬을 즉시 재선택할 수 있도록 합니다.

---

### 5.3. CSS 스타일링 (`myrpg.css`)

```css
/* 전조 뱃지 B안 스타일 */
.stance-badge {
    display: inline-block;
    padding: 8px 16px;
    font-size: 1.1rem;
    font-weight: bold;
    border-radius: 8px;
    margin-bottom: 8px;
}
.badge-standby {
    background: rgba(255, 255, 255, 0.1);
    color: #a0aec0;
    border: 1px solid #4a5568;
}
.badge-stance-normal {
    background: rgba(237, 137, 54, 0.2);
    color: #f6ad55;
    border: 1px solid #ed8936;
    box-shadow: 0 0 10px rgba(237, 137, 54, 0.4);
}
.badge-stance-heavy {
    background: rgba(229, 62, 62, 0.25);
    color: #fc8181;
    border: 1px solid #e53e3e;
    box-shadow: 0 0 12px rgba(229, 62, 62, 0.6);
    animation: pulse 0.8s infinite alternate;
}
.badge-stance-defense {
    background: rgba(49, 130, 206, 0.2);
    color: #63b3ed;
    border: 1px solid #3182ce;
    box-shadow: 0 0 10px rgba(49, 130, 206, 0.4);
}

/* 실시간 타이머 바 */
.clash-timer-wrap {
    width: 100%;
    height: 6px;
    background: #2d3748;
    border-radius: 3px;
    overflow: hidden;
    margin: 6px 0 12px 0;
}
.clash-timer-bar {
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, #f6ad55, #e53e3e);
}

/* 공방 개시 버튼 */
.btn-clash-start {
    display: block;
    width: 100%;
    padding: 12px;
    font-size: 1.15rem;
    font-weight: bold;
    color: #fff;
    background: linear-gradient(135deg, #e53e3e, #dd6b20);
    border: none;
    border-radius: 8px;
    cursor: pointer;
    margin-bottom: 12px;
    box-shadow: 0 4px 12px rgba(229, 62, 62, 0.3);
    transition: transform 0.1s, box-shadow 0.1s;
}
.btn-clash-start:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(229, 62, 62, 0.5);
}

/* 비활성화 스킬 버튼 스타일 (대치 상태) */
.disabled-skills .battle-skill-btn {
    opacity: 0.5;
    cursor: not-allowed;
    filter: grayscale(0.5);
}
```

---

## 6. 테스트 및 품질 검증 전략

1. **단위 테스트 (Unit Tests)**:
   - `BattleStateStandbyTest`: 대치/공방 상태 플래그 및 몬스터 의도 저장/초기화 검증.
   - `InventoryServiceEquipTalentTest`: 착용 무기 기준 재능 판정 및 활 장착 여부 검증.
   - `BattleServiceClashTest`:
     - `startClash()` 호출 시 몬스터 의도 추첨 및 `standby = false` 전환 검증.
     - `takeTurn("timeout")` 호출 시 몬스터 의도 100% 성공 및 무방비 피격 검증.
     - 유효 스킬 입력 시 상성 해결 후 `standby = true` 복귀 검증.
     - 도망 실패 시 `SkillType.NORMAL` 1회 피격 및 대치 복귀 검증.
     - 활 1턴 선제 사격 스킬 선택 발동 검증.
2. **웹 슬라이스 테스트 (WebSlice Tests)**:
   - `BattleControllerClashTest`: `POST /battle/clash` 호출 시 전조 뱃지 및 타이머 데이터 모델 렌더링 검증.
   - `PlayScreenControllerBattleTest`: 전투 중 `GET /` 진입 시 `standby = true` 뷰 모델 구성 검증.
3. **품질 5대 가드레일**:
   - `mvn -B -q spotless:apply -pl myrpg && mvn -B clean install -pl myrpg -am && codegraph sync` 전수 통과.
