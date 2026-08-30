package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.EquippedBonusResult;
import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.InventoryService;
import com.myapps.web.myrpg.application.service.SkillService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 힐러집 치료를 처리하는 컨트롤러.
 *
 * <p>팝업 없이 {@code POST /heal} 단일 호출로 100골드를 소모하고, 상단바 최대치(레벨·재능 + 장비 바이탈 보너스 + 스킬 바이탈 보너스)를 기준으로
 * HP/MP/스태미나를 풀회복한다.
 *
 * <p>응답은 {@code 200 OK}이며, 클라이언트(myrpg.js)가 {@code alert("치료되었습니다!")}와 {@code refreshTopBar()}를
 * 수행한다.
 */
@Controller
@RequestMapping("/heal")
public class HealController {

    private static final int DEFAULT_HEAL_COST = 100;
    private static final String LOG_TYPE_ITEM = "item";

    private final CharacterService characterService;
    private final StatProgression statProgression;
    private final InventoryService inventoryService;
    private final SkillService skillService;
    private final com.myapps.web.myrpg.config.GameProperties gameProperties;

    /** HealController를 생성한다 (Spring 주입용). */
    @org.springframework.beans.factory.annotation.Autowired
    public HealController(
            final CharacterService characterService,
            final StatProgression statProgression,
            final InventoryService inventoryService,
            final SkillService skillService,
            @org.springframework.lang.Nullable
                    final com.myapps.web.myrpg.config.GameProperties gameProperties) {
        this.characterService = characterService;
        this.statProgression = statProgression;
        this.inventoryService = inventoryService;
        this.skillService = skillService;
        this.gameProperties = gameProperties;
    }

    /** 이전 호환용 생성자. */
    public HealController(
            final CharacterService characterService,
            final StatProgression statProgression,
            final InventoryService inventoryService,
            final SkillService skillService) {
        this(characterService, statProgression, inventoryService, skillService, null);
    }

    /**
     * 100골드를 소모하고 HP/MP/스태미나를 풀회복한다.
     *
     * @param session HTTP 세션
     * @return 200 OK
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<Void> heal(final HttpSession session) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        final int cost =
                gameProperties != null && gameProperties.town() != null
                        ? gameProperties.town().healCost()
                        : DEFAULT_HEAL_COST;
        progress.spendGold(cost);

        final VitalMax maxVitals = calculateEffectiveVitalMax(progress);
        progress.fullRecover(maxVitals);
        characterService.saveTurn(progress);

        return ResponseEntity.ok().build();
    }

    private VitalMax calculateEffectiveVitalMax(final CharacterProgress progress) {
        final VitalMax base =
                statProgression.vitalMaxFor(progress.getCurrentLevel(), progress.getTalent());
        final EquippedBonusResult bonus =
                progress.getId() != null
                        ? Optional.ofNullable(inventoryService.equippedBonus(progress.getId()))
                                .orElseGet(inventoryService::equippedBonus)
                        : inventoryService.equippedBonus();
        final VitalMax eq = bonus != null ? bonus.vitalBonus() : new VitalMax(0, 0, 0);
        final VitalMax sk =
                Optional.ofNullable(skillService.rankupVitalBonus(progress.getId()))
                        .orElse(new VitalMax(0, 0, 0));
        return new VitalMax(
                base.hp() + eq.hp() + sk.hp(),
                base.mp() + eq.mp() + sk.mp(),
                base.stamina() + eq.stamina() + sk.stamina());
    }

    /**
     * 세션 없는 직접 호출을 위한 오버로드.
     *
     * @return 200 OK
     */
    public ResponseEntity<Void> heal() {
        return heal(null);
    }

    private CharacterProgress resolveCurrentCharacter(final HttpSession session) {
        if (session != null) {
            final Object sessionUser = session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
            if (sessionUser instanceof UserSession userSession
                    && userSession.characterId() != null) {
                return characterService.loadByCharacterId(userSession.characterId());
            }
        }
        return characterService.loadOrCreateDefault();
    }
}
