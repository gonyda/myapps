package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.VitalMax;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 힐러집 치료를 처리하는 컨트롤러.
 *
 * <p>팝업 없이 {@code POST /heal} 단일 호출로 100골드를 소모하고, 상단바 최대치(레벨·재능 + 장비 바이탈 보너스)를 기준으로 HP/MP/스태미나를
 * 풀회복한다.
 *
 * <p>응답은 {@code 200 OK}이며, 클라이언트(myrpg.js)가 {@code alert("치료되었습니다!")}와 {@code refreshTopBar()}를
 * 수행한다.
 */
@Controller
@RequestMapping("/heal")
public class HealController {

    private static final int HEAL_COST = 100;
    private static final String LOG_TYPE_ITEM = "item";

    private final CharacterService characterService;
    private final StatProgression statProgression;
    private final InventoryService inventoryService;
    private final ActionLog actionLog;

    /**
     * HealController를 생성한다.
     *
     * @param characterService 캐릭터 진행상황 서비스
     * @param statProgression 스탯/바이탈 계산 정책
     * @param inventoryService 인벤토리 서비스 (장비 바이탈 보너스)
     * @param actionLog 활동 로그 (세션 스코프)
     */
    public HealController(
            final CharacterService characterService,
            final StatProgression statProgression,
            final InventoryService inventoryService,
            final ActionLog actionLog) {
        this.characterService = characterService;
        this.statProgression = statProgression;
        this.inventoryService = inventoryService;
        this.actionLog = actionLog;
    }

    /**
     * 100골드를 소모하고 HP/MP/스태미나를 풀회복한다.
     *
     * <p>상단바 최대치와 동일한 기준(레벨·재능 + 장비 바이탈 보너스)으로 {@link VitalMax}를 산출하여 {@code
     * fullRecover(vitalMax)}를 호출한다. 골드 부족 시 {@code InsufficientGoldException}이 발생하여 {@code
     * GlobalExceptionHandler}에서 처리된다.
     *
     * @return 200 OK
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<Void> heal() {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        progress.spendGold(HEAL_COST);

        final VitalMax baseVitalMax =
                statProgression.vitalMaxFor(progress.getCurrentLevel(), progress.getTalent());
        final VitalMax equipVitalBonus = inventoryService.equippedBonus().vitalBonus();
        final VitalMax vitalMax =
                baseVitalMax
                        .withHpDelta(equipVitalBonus.hp())
                        .withMpDelta(equipVitalBonus.mp())
                        .withStaminaDelta(equipVitalBonus.stamina());

        progress.fullRecover(vitalMax);
        characterService.saveTurn(progress);
        actionLog.add("힐러에게 치료를 받았습니다.", LOG_TYPE_ITEM);

        return ResponseEntity.ok().build();
    }
}
