package com.myapps.web.myrpg.interfaces.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.myapps.web.myrpg.application.service.BattleSession;
import com.myapps.web.myrpg.application.service.BattleSessionService;
import com.myapps.web.myrpg.application.service.GameSessionService;
import com.myapps.web.myrpg.application.service.MasterDataLoader;
import com.myapps.web.myrpg.domain.model.DropCategory;
import com.myapps.web.myrpg.domain.model.EffectType;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerActiveRun;
import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerArmorStat;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;
import com.myapps.web.myrpg.domain.model.PlayerWeaponSkill;
import com.myapps.web.myrpg.domain.model.PlayerWeaponStat;
import com.myapps.web.myrpg.domain.model.StageEventType;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.model.vo.DropResult;
import com.myapps.web.myrpg.domain.model.vo.EffectiveStats;
import com.myapps.web.myrpg.domain.model.vo.LevelUpResult;
import com.myapps.web.myrpg.domain.model.vo.TreasureReward;
import com.myapps.web.myrpg.domain.repository.PlayerActiveRunRepository;
import com.myapps.web.myrpg.domain.repository.PlayerArmorRepository;
import com.myapps.web.myrpg.domain.repository.PlayerArmorStatRepository;
import com.myapps.web.myrpg.domain.repository.PlayerInventoryRepository;
import com.myapps.web.myrpg.domain.repository.PlayerWeaponRepository;
import com.myapps.web.myrpg.domain.repository.PlayerWeaponSkillRepository;
import com.myapps.web.myrpg.domain.repository.PlayerWeaponStatRepository;
import com.myapps.web.myrpg.domain.service.DungeonService;
import com.myapps.web.myrpg.domain.service.DropService;
import com.myapps.web.myrpg.domain.service.ShopService;
import com.myapps.web.myrpg.domain.service.StatCalculator;
import com.myapps.web.myrpg.domain.template.ArmorTemplate;
import com.myapps.web.myrpg.domain.template.DungeonTemplate;
import com.myapps.web.myrpg.domain.template.ItemTemplate;
import com.myapps.web.myrpg.domain.template.MonsterTemplate;
import com.myapps.web.myrpg.domain.template.SkillTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;
import com.myapps.web.myrpg.interfaces.dto.AttachSkillForm;
import com.myapps.web.myrpg.interfaces.dto.BattleResultViewModel;
import com.myapps.web.myrpg.interfaces.dto.BattleViewModel;
import com.myapps.web.myrpg.interfaces.dto.BuyPotionForm;
import com.myapps.web.myrpg.interfaces.dto.CreateCharacterForm;
import com.myapps.web.myrpg.interfaces.dto.DungeonExploreViewModel;
import com.myapps.web.myrpg.interfaces.dto.DungeonSelectViewModel;
import com.myapps.web.myrpg.interfaces.dto.EnterDungeonForm;
import com.myapps.web.myrpg.interfaces.dto.EquipArmorForm;
import com.myapps.web.myrpg.interfaces.dto.EquipWeaponForm;
import com.myapps.web.myrpg.interfaces.dto.EquipmentViewModel;
import com.myapps.web.myrpg.interfaces.dto.SellArmorForm;
import com.myapps.web.myrpg.interfaces.dto.SellWeaponForm;
import com.myapps.web.myrpg.interfaces.dto.ShopViewModel;
import com.myapps.web.myrpg.interfaces.dto.SkillAttackForm;
import com.myapps.web.myrpg.interfaces.dto.TownViewModel;
import com.myapps.web.myrpg.interfaces.dto.UseItemForm;

import jakarta.servlet.http.HttpSession;

/**
 * RPG 게임의 모든 화면 요청을 처리하는 컨트롤러.
 *
 * <p>마을, 장비, 던전, 전투, 상점 등 8개 화면의 GET/POST 엔드포인트를 제공한다.
 * HTTP 세션을 통해 playerId를 추적하며, BattleSession은 세션에 유지한다.
 */
@Controller
@RequestMapping("/rpg")
public class GameController {

    private static final String SESSION_PLAYER_ID = "PLAYER_ID";
    private static final int TOTAL_STAGES = 5;
    private static final int BOSS_STAGE = 5;

    private final GameSessionService gameSessionService;
    private final BattleSessionService battleSessionService;
    private final MasterDataLoader masterDataLoader;
    private final DungeonService dungeonService;
    private final DropService dropService;
    private final StatCalculator statCalculator;
    private final ShopService shopService;
    private final PlayerWeaponRepository playerWeaponRepository;
    private final PlayerArmorRepository playerArmorRepository;
    private final PlayerInventoryRepository playerInventoryRepository;
    private final PlayerActiveRunRepository playerActiveRunRepository;
    private final PlayerWeaponStatRepository playerWeaponStatRepository;
    private final PlayerArmorStatRepository playerArmorStatRepository;
    private final PlayerWeaponSkillRepository playerWeaponSkillRepository;

    /**
     * GameController를 생성한다.
     *
     * @param gameSessionService       게임 세션 오케스트레이션 서비스
     * @param battleSessionService     전투 세션 오케스트레이션 서비스
     * @param masterDataLoader         마스터 데이터 로더
     * @param dungeonService           던전 도메인 서비스
     * @param dropService              드랍 도메인 서비스
     * @param statCalculator           스탯 계산 서비스
     * @param shopService              상점 도메인 서비스
     * @param playerWeaponRepository   무기 리포지터리
     * @param playerArmorRepository    방어구 리포지터리
     * @param playerInventoryRepository 인벤토리 리포지터리
     * @param playerActiveRunRepository 활성 런 리포지터리
     * @param playerWeaponStatRepository 무기 능력치 리포지터리
     * @param playerArmorStatRepository  방어구 능력치 리포지터리
     * @param playerWeaponSkillRepository 무기 스킬 리포지터리
     */
    public GameController(final GameSessionService gameSessionService,
                          final BattleSessionService battleSessionService,
                          final MasterDataLoader masterDataLoader,
                          final DungeonService dungeonService,
                          final DropService dropService,
                          final StatCalculator statCalculator,
                          final ShopService shopService,
                          final PlayerWeaponRepository playerWeaponRepository,
                          final PlayerArmorRepository playerArmorRepository,
                          final PlayerInventoryRepository playerInventoryRepository,
                          final PlayerActiveRunRepository playerActiveRunRepository,
                          final PlayerWeaponStatRepository playerWeaponStatRepository,
                          final PlayerArmorStatRepository playerArmorStatRepository,
                          final PlayerWeaponSkillRepository playerWeaponSkillRepository) {
        this.gameSessionService = gameSessionService;
        this.battleSessionService = battleSessionService;
        this.masterDataLoader = masterDataLoader;
        this.dungeonService = dungeonService;
        this.dropService = dropService;
        this.statCalculator = statCalculator;
        this.shopService = shopService;
        this.playerWeaponRepository = playerWeaponRepository;
        this.playerArmorRepository = playerArmorRepository;
        this.playerInventoryRepository = playerInventoryRepository;
        this.playerActiveRunRepository = playerActiveRunRepository;
        this.playerWeaponStatRepository = playerWeaponStatRepository;
        this.playerArmorStatRepository = playerArmorStatRepository;
        this.playerWeaponSkillRepository = playerWeaponSkillRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // 캐릭터 생성
    // ─────────────────────────────────────────────────────────────

    /**
     * 캐릭터를 생성하고 마을로 리다이렉트한다.
     *
     * @param form    캐릭터 생성 폼
     * @param session HTTP 세션
     * @return 마을 리다이렉트
     */
    @PostMapping("/character/create")
    public String createCharacter(@ModelAttribute final CreateCharacterForm form,
                                  final HttpSession session) {
        final Player player = gameSessionService.createCharacter(form.name());
        session.setAttribute(SESSION_PLAYER_ID, player.getId());
        return "redirect:/rpg/town";
    }

    // ─────────────────────────────────────────────────────────────
    // 마을
    // ─────────────────────────────────────────────────────────────

    /**
     * 마을 화면을 표시한다.
     *
     * @param model   뷰 모델
     * @param session HTTP 세션
     * @return 마을 뷰 이름
     */
    @GetMapping("/town")
    public String town(final Model model, final HttpSession session) {
        final Long playerId = getPlayerId(session);
        if (playerId == null) {
            return "rpg/create";
        }
        final Player player = gameSessionService.getPlayer(playerId);
        final boolean hasActiveRun = playerActiveRunRepository.findByPlayerId(playerId).isPresent();
        final TownViewModel viewModel = new TownViewModel(
                player.getName(), player.getLevel(),
                player.getHp(), player.getMaxHp(),
                player.getMp(), player.getMaxMp(),
                player.getGold(), hasActiveRun);
        model.addAttribute("town", viewModel);
        return "rpg/town";
    }

    // ─────────────────────────────────────────────────────────────
    // 장비 관리
    // ─────────────────────────────────────────────────────────────

    /**
     * 장비 관리 화면(방어구 탭)을 표시한다.
     *
     * @param model   뷰 모델
     * @param session HTTP 세션
     * @return 장비 뷰 이름
     */
    @GetMapping("/equipment")
    public String equipment(final Model model, final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        model.addAttribute("equipment", buildEquipmentViewModel(playerId));
        model.addAttribute("activeTab", "armor");
        return "rpg/equipment";
    }

    /**
     * 장비 관리 화면(무기 탭)을 표시한다.
     *
     * @param model   뷰 모델
     * @param session HTTP 세션
     * @return 장비 뷰 이름
     */
    @GetMapping("/equipment/weapons")
    public String equipmentWeapons(final Model model, final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        model.addAttribute("equipment", buildEquipmentViewModel(playerId));
        model.addAttribute("activeTab", "weapon");
        return "rpg/equipment";
    }

    /**
     * 무기를 착용한다.
     *
     * @param form    무기 착용 폼
     * @param session HTTP 세션
     * @return 장비 무기 탭 리다이렉트
     */
    @PostMapping("/equipment/equip-weapon")
    public String equipWeapon(@ModelAttribute final EquipWeaponForm form,
                              final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        gameSessionService.equipWeapon(playerId, form.weaponId());
        return "redirect:/rpg/equipment/weapons";
    }

    /**
     * 방어구를 착용한다.
     *
     * @param form    방어구 착용 폼
     * @param session HTTP 세션
     * @return 장비 방어구 탭 리다이렉트
     */
    @PostMapping("/equipment/equip-armor")
    public String equipArmor(@ModelAttribute final EquipArmorForm form,
                             final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        gameSessionService.equipArmor(playerId, form.armorId());
        return "redirect:/rpg/equipment";
    }

    /**
     * 스킬북을 무기에 장착한다.
     *
     * @param form    스킬북 장착 폼
     * @param session HTTP 세션
     * @return 장비 무기 탭 리다이렉트
     */
    @PostMapping("/equipment/attach-skill")
    public String attachSkill(@ModelAttribute final AttachSkillForm form,
                              final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        final Optional<Integer> overwriteSlot = Optional.ofNullable(form.overwriteSlot());
        gameSessionService.attachSkillBook(playerId, form.weaponId(),
                form.skillId(), overwriteSlot);
        return "redirect:/rpg/equipment/weapons";
    }

    // ─────────────────────────────────────────────────────────────
    // 던전 선택 및 입장
    // ─────────────────────────────────────────────────────────────

    /**
     * 던전 선택 화면을 표시한다.
     *
     * @param model   뷰 모델
     * @param session HTTP 세션
     * @return 던전 선택 뷰 이름
     */
    @GetMapping("/dungeon/select")
    public String dungeonSelect(final Model model, final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        final Player player = gameSessionService.getPlayer(playerId);
        final List<DungeonTemplate> dungeons = masterDataLoader.allDungeons();
        final List<DungeonSelectViewModel.DungeonInfo> dungeonInfos = dungeons.stream()
                .map(d -> new DungeonSelectViewModel.DungeonInfo(
                        d.id(), d.name(), d.difficulty(), d.requiredLevel()))
                .toList();
        final DungeonSelectViewModel viewModel = new DungeonSelectViewModel(
                dungeonInfos, player.getLevel());
        model.addAttribute("dungeonSelect", viewModel);
        return "rpg/dungeon-select";
    }

    /**
     * 던전에 입장한다.
     *
     * @param form    던전 입장 폼
     * @param session HTTP 세션
     * @return 던전 탐색 리다이렉트
     */
    @PostMapping("/dungeon/enter")
    public String enterDungeon(@ModelAttribute final EnterDungeonForm form,
                               final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        gameSessionService.enterDungeon(playerId, form.dungeonId());
        return "redirect:/rpg/dungeon/explore";
    }

    // ─────────────────────────────────────────────────────────────
    // 던전 탐색
    // ─────────────────────────────────────────────────────────────

    /**
     * 던전 탐색 화면을 표시한다 (스테이지 이벤트 처리).
     *
     * @param model   뷰 모델
     * @param session HTTP 세션
     * @return 던전 탐색 뷰 이름 또는 전투/마을 리다이렉트
     */
    @GetMapping("/dungeon/explore")
    public String dungeonExplore(final Model model, final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        final PlayerActiveRun activeRun = findActiveRunOrRedirect(playerId);
        if (activeRun == null) {
            return "redirect:/rpg/town";
        }
        final int nextStage = activeRun.getClearedStage() + 1;
        final DungeonTemplate dungeon = masterDataLoader.findDungeon(activeRun.getDungeonId());
        final StageEventType eventType = dungeonService.rollStageEvent(nextStage);

        return handleStageEvent(model, session, playerId, activeRun,
                dungeon, nextStage, eventType);
    }

    /**
     * 다음 스테이지로 진행한다.
     *
     * @param session HTTP 세션
     * @return 던전 탐색 리다이렉트
     */
    @PostMapping("/dungeon/next-stage")
    public String nextStage(final HttpSession session) {
        requirePlayerId(session);
        return "redirect:/rpg/dungeon/explore";
    }

    /**
     * 던전을 포기하고 마을로 복귀한다.
     *
     * @param session HTTP 세션
     * @return 마을 리다이렉트
     */
    @PostMapping("/dungeon/abandon")
    public String abandonDungeon(final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        gameSessionService.abandonDungeon(playerId);
        battleSessionService.clearBattleSession(session);
        return "redirect:/rpg/town";
    }

    // ─────────────────────────────────────────────────────────────
    // 전투
    // ─────────────────────────────────────────────────────────────

    /**
     * 전투 화면을 표시한다.
     *
     * @param model   뷰 모델
     * @param session HTTP 세션
     * @return 전투 뷰 이름
     */
    @GetMapping("/battle")
    public String battle(final Model model, final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        final BattleSession battleSession = battleSessionService.getBattleSession(session);
        if (battleSession == null || !battleSession.isOngoing()) {
            return "redirect:/rpg/town";
        }
        final BattleViewModel viewModel = buildBattleViewModel(playerId, battleSession);
        model.addAttribute("battle", viewModel);
        return "rpg/battle";
    }

    /**
     * 기본 공격을 실행한다.
     *
     * @param session HTTP 세션
     * @return 전투 화면 또는 결과 리다이렉트
     */
    @PostMapping("/battle/attack")
    public String battleAttack(final HttpSession session) {
        requirePlayerId(session);
        battleSessionService.playerAttack(session);
        return redirectAfterBattleAction(session);
    }

    /**
     * 스킬 공격을 실행한다.
     *
     * @param form    스킬 공격 폼
     * @param session HTTP 세션
     * @return 전투 화면 또는 결과 리다이렉트
     */
    @PostMapping("/battle/skill")
    public String battleSkill(@ModelAttribute final SkillAttackForm form,
                              final HttpSession session) {
        requirePlayerId(session);
        battleSessionService.playerSkillAttack(session, form.skillId());
        return redirectAfterBattleAction(session);
    }

    /**
     * 아이템(포션)을 사용한다.
     *
     * @param form    아이템 사용 폼
     * @param session HTTP 세션
     * @return 전투 화면 또는 결과 리다이렉트
     */
    @PostMapping("/battle/item")
    public String battleItem(@ModelAttribute final UseItemForm form,
                             final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        final ItemTemplate item = masterDataLoader.findItem(form.itemId());
        final PlayerInventory inventory = playerInventoryRepository
                .findByPlayerIdAndItemTypeAndItemRefId(playerId, ItemType.POTION, form.itemId())
                .orElseThrow();
        final BattleSession battleSession = battleSessionService.getBattleSession(session);
        final boolean isHp = item.effectType() == EffectType.HEAL_HP;
        final int maxValue = isHp ? battleSession.getPlayerMaxHp()
                : battleSession.getPlayerCurrentMp() + item.effectAmount();
        battleSessionService.playerUsePotion(session, inventory, item.effectAmount(),
                maxValue, isHp);
        return redirectAfterBattleAction(session);
    }

    /**
     * 도망을 시도한다.
     *
     * @param session HTTP 세션
     * @return 전투 화면 또는 마을 리다이렉트
     */
    @PostMapping("/battle/flee")
    public String battleFlee(final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        final boolean fled = battleSessionService.playerFlee(session);
        if (fled) {
            gameSessionService.applyFleePenalty(playerId);
            battleSessionService.clearBattleSession(session);
            return "redirect:/rpg/town";
        }
        return redirectAfterBattleAction(session);
    }

    /**
     * 전투 결과(승리/드랍) 화면을 표시한다.
     *
     * @param model   뷰 모델
     * @param session HTTP 세션
     * @return 전투 결과 뷰 이름
     */
    @GetMapping("/battle/result")
    public String battleResult(final Model model, final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        final BattleSession battleSession = battleSessionService.getBattleSession(session);
        if (battleSession == null) {
            return "redirect:/rpg/town";
        }
        final BattleResultViewModel viewModel =
                (BattleResultViewModel) session.getAttribute("BATTLE_RESULT");
        if (viewModel == null) {
            return "redirect:/rpg/town";
        }
        model.addAttribute("result", viewModel);
        return "rpg/battle-result";
    }

    /**
     * 보상을 수령하고 다음 스테이지 또는 마을로 이동한다.
     *
     * @param session HTTP 세션
     * @return 던전 탐색 또는 마을 리다이렉트
     */
    @PostMapping("/battle/claim-reward")
    public String claimReward(final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        final BattleResultViewModel viewModel =
                (BattleResultViewModel) session.getAttribute("BATTLE_RESULT");
        session.removeAttribute("BATTLE_RESULT");
        battleSessionService.clearBattleSession(session);

        if (viewModel != null && viewModel.dungeonCleared()) {
            return "redirect:/rpg/town";
        }
        return "redirect:/rpg/dungeon/explore";
    }

    // ─────────────────────────────────────────────────────────────
    // 상점
    // ─────────────────────────────────────────────────────────────

    /**
     * 상점 화면을 표시한다.
     *
     * @param model   뷰 모델
     * @param session HTTP 세션
     * @return 상점 뷰 이름
     */
    @GetMapping("/shop")
    public String shop(final Model model, final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        final Player player = gameSessionService.getPlayer(playerId);
        final ShopViewModel viewModel = buildShopViewModel(playerId, player);
        model.addAttribute("shop", viewModel);
        return "rpg/shop";
    }

    /**
     * 무기를 판매한다.
     *
     * @param form    무기 판매 폼
     * @param session HTTP 세션
     * @return 상점 리다이렉트
     */
    @PostMapping("/shop/sell-weapon")
    public String sellWeapon(@ModelAttribute final SellWeaponForm form,
                             final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        gameSessionService.sellWeapon(playerId, form.weaponId());
        return "redirect:/rpg/shop";
    }

    /**
     * 방어구를 판매한다.
     *
     * @param form    방어구 판매 폼
     * @param session HTTP 세션
     * @return 상점 리다이렉트
     */
    @PostMapping("/shop/sell-armor")
    public String sellArmor(@ModelAttribute final SellArmorForm form,
                            final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        gameSessionService.sellArmor(playerId, form.armorId());
        return "redirect:/rpg/shop";
    }

    /**
     * 포션을 구매한다.
     *
     * @param form    포션 구매 폼
     * @param session HTTP 세션
     * @return 상점 리다이렉트
     */
    @PostMapping("/shop/buy")
    public String buyPotion(@ModelAttribute final BuyPotionForm form,
                            final HttpSession session) {
        final Long playerId = requirePlayerId(session);
        final ItemTemplate item = masterDataLoader.findItem(form.itemId());
        gameSessionService.buyPotion(playerId, form.itemId(), item.buyPrice());
        return "redirect:/rpg/shop";
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers — Session
    // ─────────────────────────────────────────────────────────────

    /**
     * 세션에서 playerId를 조회한다.
     *
     * @param session HTTP 세션
     * @return 플레이어 ID (없으면 null)
     */
    private Long getPlayerId(final HttpSession session) {
        return (Long) session.getAttribute(SESSION_PLAYER_ID);
    }

    /**
     * 세션에서 playerId를 조회하고, 없으면 예외를 던진다.
     *
     * @param session HTTP 세션
     * @return 플레이어 ID
     */
    private Long requirePlayerId(final HttpSession session) {
        final Long playerId = getPlayerId(session);
        if (playerId == null) {
            throw new IllegalStateException("세션에 플레이어 정보가 없습니다.");
        }
        return playerId;
    }

    /**
     * 활성 런을 조회한다 (없으면 null).
     *
     * @param playerId 플레이어 ID
     * @return 활성 런 또는 null
     */
    private PlayerActiveRun findActiveRunOrRedirect(final Long playerId) {
        return playerActiveRunRepository.findByPlayerId(playerId).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers — Stage event handling
    // ─────────────────────────────────────────────────────────────

    /**
     * 스테이지 이벤트를 처리하고 적절한 뷰를 반환한다.
     */
    private String handleStageEvent(final Model model, final HttpSession session,
                                    final Long playerId, final PlayerActiveRun activeRun,
                                    final DungeonTemplate dungeon, final int stage,
                                    final StageEventType eventType) {
        return switch (eventType) {
            case BATTLE -> handleBattleEvent(session, playerId, dungeon, stage);
            case REST -> handleRestEvent(model, playerId, dungeon, stage);
            case TRAP -> handleTrapEvent(model, playerId, dungeon, stage);
            case TREASURE -> handleTreasureEvent(model, session, playerId, dungeon, stage);
            case MERCHANT -> handleMerchantEvent(model, dungeon, stage);
        };
    }

    /**
     * 전투 이벤트를 처리한다.
     */
    private String handleBattleEvent(final HttpSession session, final Long playerId,
                                     final DungeonTemplate dungeon, final int stage) {
        final long monsterId = dungeonService.pickMonster(dungeon, stage);
        final Player player = gameSessionService.getPlayer(playerId);
        final EffectiveStats stats = computePlayerStats(playerId, player);
        battleSessionService.startBattle(session, player, stats, monsterId);
        return "redirect:/rpg/battle";
    }

    /**
     * 휴식 이벤트를 처리한다.
     */
    private String handleRestEvent(final Model model, final Long playerId,
                                   final DungeonTemplate dungeon, final int stage) {
        final Player player = gameSessionService.getPlayer(playerId);
        final int newHp = dungeonService.applyRest(player.getHp(), player.getMaxHp());
        player.changeHp(newHp);
        gameSessionService.saveCheckpoint(playerId, stage, newHp, player.getMp());
        final String result = "HP가 회복되었습니다. (현재 HP: " + newHp + "/" + player.getMaxHp() + ")";
        return showExploreView(model, dungeon, stage, StageEventType.REST, result);
    }

    /**
     * 함정 이벤트를 처리한다.
     */
    private String handleTrapEvent(final Model model, final Long playerId,
                                   final DungeonTemplate dungeon, final int stage) {
        final Player player = gameSessionService.getPlayer(playerId);
        final int newHp = dungeonService.applyTrap(player.getHp());
        player.changeHp(newHp);
        gameSessionService.saveCheckpoint(playerId, stage, newHp, player.getMp());
        final String result = "함정에 걸렸습니다! (현재 HP: " + newHp + "/" + player.getMaxHp() + ")";
        return showExploreView(model, dungeon, stage, StageEventType.TRAP, result);
    }

    /**
     * 보물상자 이벤트를 처리한다.
     */
    private String handleTreasureEvent(final Model model, final HttpSession session,
                                       final Long playerId,
                                       final DungeonTemplate dungeon, final int stage) {
        final List<WeaponTemplate> weaponTemplates = resolveWeaponTemplates(dungeon);
        final List<ArmorTemplate> armorTemplates = resolveArmorTemplates(dungeon);
        final TreasureReward reward = dungeonService.rollTreasure(
                dungeon, dropService, weaponTemplates, armorTemplates);
        final String result = applyTreasureReward(playerId, reward);
        final Player player = gameSessionService.getPlayer(playerId);
        gameSessionService.saveCheckpoint(playerId, stage, player.getHp(), player.getMp());
        return showExploreView(model, dungeon, stage, StageEventType.TREASURE, result);
    }

    /**
     * 상인 이벤트를 처리한다 (상점 리다이렉트 대신 뷰 표시).
     */
    private String handleMerchantEvent(final Model model,
                                       final DungeonTemplate dungeon, final int stage) {
        final String result = "떠돌이 상인을 만났습니다. 상점으로 이동하세요.";
        return showExploreView(model, dungeon, stage, StageEventType.MERCHANT, result);
    }

    /**
     * 던전 탐색 뷰 모델을 구성하고 뷰를 반환한다.
     */
    private String showExploreView(final Model model, final DungeonTemplate dungeon,
                                   final int stage, final StageEventType eventType,
                                   final String eventResult) {
        final boolean isBossStage = stage == BOSS_STAGE;
        final DungeonExploreViewModel viewModel = new DungeonExploreViewModel(
                dungeon.name(), stage, TOTAL_STAGES, eventType,
                eventResult, isBossStage, false);
        model.addAttribute("explore", viewModel);
        return "rpg/dungeon-explore";
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers — Battle resolution
    // ─────────────────────────────────────────────────────────────

    /**
     * 전투 행동 후 적절한 리다이렉트 경로를 반환한다.
     */
    private String redirectAfterBattleAction(final HttpSession session) {
        final BattleSession battleSession = battleSessionService.getBattleSession(session);
        if (battleSession == null) {
            return "redirect:/rpg/town";
        }
        return switch (battleSession.getStatus()) {
            case PLAYER_WON -> handlePlayerWon(session, battleSession);
            case PLAYER_DEAD -> handlePlayerDead(session, battleSession);
            case PLAYER_FLED -> "redirect:/rpg/town";
            case ONGOING -> "redirect:/rpg/battle";
        };
    }

    /**
     * 플레이어 승리 시 보상을 처리하고 결과 화면으로 리다이렉트한다.
     */
    private String handlePlayerWon(final HttpSession session,
                                   final BattleSession battleSession) {
        final Long playerId = getPlayerId(session);
        final long monsterId = battleSession.getMonsterId();
        final MonsterTemplate monster = masterDataLoader.findMonster(monsterId);

        final LevelUpResult levelUpResult = gameSessionService.grantBattleReward(
                playerId, monsterId);
        final DropResult dropResult = rollAndSaveDrop(playerId, monster, session);
        final String dropDescription = describeDropResult(dropResult);

        final PlayerActiveRun activeRun = findActiveRunOrRedirect(playerId);
        final boolean isBossKill = isBossMonster(activeRun, monster);
        final boolean dungeonCleared = handleDungeonProgressAfterWin(
                playerId, activeRun, isBossKill, session);

        final BattleResultViewModel resultViewModel = new BattleResultViewModel(
                monster.name(), monster.expReward(), monster.goldReward(),
                dropResult, levelUpResult, dropDescription,
                isBossKill, dungeonCleared);
        session.setAttribute("BATTLE_RESULT", resultViewModel);
        return "redirect:/rpg/battle/result";
    }

    /**
     * 플레이어 사망 시 페널티를 적용하고 마을로 리다이렉트한다.
     */
    private String handlePlayerDead(final HttpSession session,
                                    final BattleSession battleSession) {
        final Long playerId = getPlayerId(session);
        gameSessionService.applyDeathPenalty(playerId);
        battleSessionService.clearBattleSession(session);
        return "redirect:/rpg/town";
    }

    /**
     * 보스 몬스터인지 판정한다.
     */
    private boolean isBossMonster(final PlayerActiveRun activeRun,
                                  final MonsterTemplate monster) {
        if (activeRun == null) {
            return false;
        }
        final DungeonTemplate dungeon = masterDataLoader.findDungeon(activeRun.getDungeonId());
        return dungeon.bossId() == monster.id();
    }

    /**
     * 전투 승리 후 던전 진행을 업데이트한다.
     */
    private boolean handleDungeonProgressAfterWin(final Long playerId,
                                                  final PlayerActiveRun activeRun,
                                                  final boolean isBossKill,
                                                  final HttpSession session) {
        if (activeRun == null) {
            return false;
        }
        final int currentStage = activeRun.getClearedStage() + 1;
        if (isBossKill) {
            gameSessionService.completeDungeon(playerId, activeRun.getDungeonId());
            return true;
        }
        final Player player = gameSessionService.getPlayer(playerId);
        final BattleSession bs = battleSessionService.getBattleSession(session);
        final int hp = bs != null ? bs.getPlayerCurrentHp() : player.getHp();
        final int mp = bs != null ? bs.getPlayerCurrentMp() : player.getMp();
        gameSessionService.saveCheckpoint(playerId, currentStage, hp, mp);
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers — Drop
    // ─────────────────────────────────────────────────────────────

    /**
     * 드랍을 롤하고 결과를 저장한다.
     */
    private DropResult rollAndSaveDrop(final Long playerId, final MonsterTemplate monster,
                                       final HttpSession session) {
        final PlayerActiveRun activeRun = findActiveRunOrRedirect(playerId);
        if (activeRun == null) {
            return new DropResult(DropCategory.NONE, null, null, null);
        }
        final DungeonTemplate dungeon = masterDataLoader.findDungeon(activeRun.getDungeonId());
        final List<WeaponTemplate> weaponTemplates = resolveWeaponTemplates(dungeon);
        final List<ArmorTemplate> armorTemplates = resolveArmorTemplates(dungeon);
        final List<SkillTemplate> availableSkills = resolveSkillsForDungeon(dungeon);

        final DropResult result = dropService.rollDrop(monster, dungeon, availableSkills,
                weaponTemplates, armorTemplates, dungeon.requiredLevel());
        saveDropResult(playerId, result);
        return result;
    }

    /**
     * 드랍 결과를 DB에 저장한다.
     */
    private void saveDropResult(final Long playerId, final DropResult result) {
        switch (result.category()) {
            case WEAPON -> gameSessionService.saveWeaponDrop(playerId, result.weapon());
            case ARMOR -> gameSessionService.saveArmorDrop(playerId, result.armor());
            case SKILL_BOOK -> gameSessionService.saveSkillBookDrop(playerId, result.skillId());
            case NONE -> { }
        }
    }

    /**
     * 드랍 결과를 사용자 문구로 변환한다.
     */
    private String describeDropResult(final DropResult result) {
        return switch (result.category()) {
            case WEAPON -> "무기 획득: " + result.weapon().displayName();
            case ARMOR -> "방어구 획득: " + result.armor().displayName();
            case SKILL_BOOK -> {
                final SkillTemplate skill = masterDataLoader.findSkill(result.skillId());
                yield "스킬북 획득: " + skill.name();
            }
            case NONE -> "드랍 없음";
        };
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers — Treasure reward
    // ─────────────────────────────────────────────────────────────

    /**
     * 보물상자 보상을 적용하고 설명 문구를 반환한다.
     */
    private String applyTreasureReward(final Long playerId, final TreasureReward reward) {
        return switch (reward.kind()) {
            case GOLD -> {
                gameSessionService.saveGoldReward(playerId, reward.gold());
                yield "보물상자에서 " + reward.gold() + " 골드를 획득했습니다!";
            }
            case POTION -> {
                gameSessionService.savePotionDrop(playerId, reward.itemId());
                final ItemTemplate item = masterDataLoader.findItem(reward.itemId());
                yield "보물상자에서 " + item.name() + "을(를) 획득했습니다!";
            }
            case EQUIPMENT -> {
                final DropResult equipment = reward.equipment();
                saveDropResult(playerId, equipment);
                yield "보물상자에서 장비를 획득했습니다! " + describeDropResult(equipment);
            }
        };
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers — View model builders
    // ─────────────────────────────────────────────────────────────

    /**
     * 장비 관리 뷰 모델을 구성한다.
     */
    private EquipmentViewModel buildEquipmentViewModel(final Long playerId) {
        final List<PlayerWeapon> weapons = playerWeaponRepository.findByPlayerId(playerId);
        final List<PlayerArmor> armors = playerArmorRepository.findByPlayerId(playerId);
        final PlayerWeapon equippedWeapon = weapons.stream()
                .filter(PlayerWeapon::isEquipped)
                .findFirst().orElse(null);
        final List<PlayerArmor> equippedArmors = armors.stream()
                .filter(PlayerArmor::isEquipped)
                .toList();

        final List<PlayerWeaponSkill> weaponSkills = equippedWeapon != null
                ? playerWeaponSkillRepository.findByPlayerWeaponId(equippedWeapon.getId())
                : List.of();
        final List<SkillTemplate> availableSkills = equippedWeapon != null
                ? masterDataLoader.skillsForWeaponType(equippedWeapon.getWeaponType())
                : List.of();
        final List<EquipmentViewModel.SkillBookInfo> skillBooks = buildSkillBookInfos(playerId);

        return new EquipmentViewModel(weapons, armors, equippedWeapon, equippedArmors,
                weaponSkills, availableSkills, skillBooks);
    }

    /**
     * 보유 스킬북 정보 목록을 구성한다.
     */
    private List<EquipmentViewModel.SkillBookInfo> buildSkillBookInfos(final Long playerId) {
        final List<PlayerInventory> inventories = playerInventoryRepository
                .findByPlayerId(playerId);
        final List<EquipmentViewModel.SkillBookInfo> result = new ArrayList<>();
        for (final PlayerInventory inv : inventories) {
            if (inv.getItemType() == ItemType.SKILL_BOOK && inv.getQuantity() > 0) {
                final SkillTemplate skill = masterDataLoader.findSkill(inv.getItemRefId());
                result.add(new EquipmentViewModel.SkillBookInfo(
                        skill.id(), skill.name(), inv.getQuantity()));
            }
        }
        return result;
    }

    /**
     * 전투 화면 뷰 모델을 구성한다.
     */
    private BattleViewModel buildBattleViewModel(final Long playerId,
                                                 final BattleSession battleSession) {
        final List<BattleViewModel.SkillInfo> skills = buildBattleSkillInfos(playerId);
        final List<BattleViewModel.PotionInfo> potions = buildBattlePotionInfos(playerId);
        return new BattleViewModel(battleSession, skills, potions);
    }

    /**
     * 전투 중 사용 가능한 스킬 목록을 구성한다.
     */
    private List<BattleViewModel.SkillInfo> buildBattleSkillInfos(final Long playerId) {
        final List<PlayerWeapon> weapons = playerWeaponRepository.findByPlayerId(playerId);
        final PlayerWeapon equippedWeapon = weapons.stream()
                .filter(PlayerWeapon::isEquipped)
                .findFirst().orElse(null);
        if (equippedWeapon == null) {
            return List.of();
        }
        final List<PlayerWeaponSkill> weaponSkills =
                playerWeaponSkillRepository.findByPlayerWeaponId(equippedWeapon.getId());
        final List<BattleViewModel.SkillInfo> result = new ArrayList<>();
        for (final PlayerWeaponSkill ws : weaponSkills) {
            final SkillTemplate skill = masterDataLoader.findSkill(ws.getSkillId());
            result.add(new BattleViewModel.SkillInfo(
                    skill.id(), skill.name(), skill.mpCost(), skill.damageMultiplier()));
        }
        return result;
    }

    /**
     * 전투 중 사용 가능한 포션 목록을 구성한다.
     */
    private List<BattleViewModel.PotionInfo> buildBattlePotionInfos(final Long playerId) {
        final List<PlayerInventory> inventories = playerInventoryRepository
                .findByPlayerId(playerId);
        final List<BattleViewModel.PotionInfo> result = new ArrayList<>();
        for (final PlayerInventory inv : inventories) {
            if (inv.getItemType() == ItemType.POTION && inv.getQuantity() > 0) {
                final ItemTemplate item = masterDataLoader.findItem(inv.getItemRefId());
                final boolean isHp = item.effectType() == EffectType.HEAL_HP;
                result.add(new BattleViewModel.PotionInfo(
                        item.id(), item.name(), item.effectAmount(),
                        inv.getQuantity(), isHp));
            }
        }
        return result;
    }

    /**
     * 상점 뷰 모델을 구성한다.
     */
    private ShopViewModel buildShopViewModel(final Long playerId, final Player player) {
        final List<ShopViewModel.SellableWeapon> sellableWeapons =
                buildSellableWeapons(playerId);
        final List<ShopViewModel.SellableArmor> sellableArmors =
                buildSellableArmors(playerId);
        final List<ShopViewModel.BuyablePotion> buyablePotions =
                buildBuyablePotions(playerId);
        return new ShopViewModel(player.getGold(), sellableWeapons,
                sellableArmors, buyablePotions);
    }

    /**
     * 판매 가능한 무기 목록을 구성한다.
     */
    private List<ShopViewModel.SellableWeapon> buildSellableWeapons(final Long playerId) {
        final List<PlayerWeapon> weapons = playerWeaponRepository.findByPlayerId(playerId);
        final List<ShopViewModel.SellableWeapon> result = new ArrayList<>();
        for (final PlayerWeapon weapon : weapons) {
            if (!weapon.isEquipped()) {
                final WeaponTemplate template = masterDataLoader.findWeaponTemplate(
                        weapon.getWeaponTemplateId());
                final int price = shopService.sellPrice(template.baseValue(),
                        weapon.getGrade(), weapon.getItemLevel());
                result.add(new ShopViewModel.SellableWeapon(weapon, price));
            }
        }
        return result;
    }

    /**
     * 판매 가능한 방어구 목록을 구성한다.
     */
    private List<ShopViewModel.SellableArmor> buildSellableArmors(final Long playerId) {
        final List<PlayerArmor> armors = playerArmorRepository.findByPlayerId(playerId);
        final List<ShopViewModel.SellableArmor> result = new ArrayList<>();
        for (final PlayerArmor armor : armors) {
            if (!armor.isEquipped()) {
                final ArmorTemplate template = masterDataLoader.findArmorTemplate(
                        armor.getArmorTemplateId());
                final int price = shopService.sellPrice(template.baseValue(),
                        armor.getGrade(), armor.getItemLevel());
                result.add(new ShopViewModel.SellableArmor(armor, price));
            }
        }
        return result;
    }

    /**
     * 구매 가능한 포션 목록을 구성한다.
     */
    private List<ShopViewModel.BuyablePotion> buildBuyablePotions(final Long playerId) {
        final List<ItemTemplate> allItems = masterDataLoader.allItems();
        final List<ShopViewModel.BuyablePotion> result = new ArrayList<>();
        for (final ItemTemplate item : allItems) {
            if (item.itemType() == ItemType.POTION) {
                final int owned = getOwnedQuantity(playerId, ItemType.POTION, item.id());
                result.add(new ShopViewModel.BuyablePotion(
                        item.id(), item.name(), item.buyPrice(), owned));
            }
        }
        return result;
    }

    /**
     * 인벤토리에서 특정 아이템의 보유 수량을 조회한다.
     */
    private int getOwnedQuantity(final Long playerId, final ItemType itemType,
                                 final long itemRefId) {
        return playerInventoryRepository
                .findByPlayerIdAndItemTypeAndItemRefId(playerId, itemType, itemRefId)
                .map(PlayerInventory::getQuantity)
                .orElse(0);
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers — Stat computation
    // ─────────────────────────────────────────────────────────────

    /**
     * 플레이어의 유효 스탯을 계산한다.
     */
    private EffectiveStats computePlayerStats(final Long playerId, final Player player) {
        final List<PlayerWeapon> weapons = playerWeaponRepository.findByPlayerId(playerId);
        final PlayerWeapon equippedWeapon = weapons.stream()
                .filter(PlayerWeapon::isEquipped)
                .findFirst().orElse(null);
        final List<PlayerWeaponStat> weaponStats = equippedWeapon != null
                ? playerWeaponStatRepository.findByPlayerWeaponId(equippedWeapon.getId())
                : List.of();
        final List<PlayerArmor> equippedArmors = playerArmorRepository.findByPlayerId(playerId)
                .stream().filter(PlayerArmor::isEquipped).toList();
        final List<PlayerArmorStat> armorStats = collectArmorStats(equippedArmors);
        return statCalculator.compute(player, equippedWeapon, weaponStats,
                equippedArmors, armorStats);
    }

    /**
     * 착용 방어구들의 능력치를 수집한다.
     */
    private List<PlayerArmorStat> collectArmorStats(final List<PlayerArmor> equippedArmors) {
        final List<PlayerArmorStat> allStats = new ArrayList<>();
        for (final PlayerArmor armor : equippedArmors) {
            allStats.addAll(playerArmorStatRepository.findByPlayerArmorId(armor.getId()));
        }
        return allStats;
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers — Template resolution
    // ─────────────────────────────────────────────────────────────

    /**
     * 던전에서 드랍 가능한 무기 템플릿 목록을 조회한다.
     */
    private List<WeaponTemplate> resolveWeaponTemplates(final DungeonTemplate dungeon) {
        return masterDataLoader.weaponsForTypes(dungeon.weaponTypes());
    }

    /**
     * 던전에서 드랍 가능한 방어구 템플릿 목록을 조회한다.
     */
    private List<ArmorTemplate> resolveArmorTemplates(final DungeonTemplate dungeon) {
        return masterDataLoader.armorsForSlots(dungeon.armorSlots());
    }

    /**
     * 던전 무기 타입과 호환되는 스킬 템플릿 목록을 조회한다.
     */
    private List<SkillTemplate> resolveSkillsForDungeon(final DungeonTemplate dungeon) {
        final List<SkillTemplate> result = new ArrayList<>();
        for (final WeaponType wt : dungeon.weaponTypes()) {
            result.addAll(masterDataLoader.skillsForWeaponType(wt));
        }
        return result;
    }
}
