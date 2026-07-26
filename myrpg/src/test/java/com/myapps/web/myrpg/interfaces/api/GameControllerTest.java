package com.myapps.web.myrpg.interfaces.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.myapps.web.myrpg.application.service.BattleSession;
import com.myapps.web.myrpg.application.service.BattleSessionService;
import com.myapps.web.myrpg.application.service.GameSessionService;
import com.myapps.web.myrpg.application.service.MasterDataLoader;
import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerActiveRun;
import com.myapps.web.myrpg.domain.model.vo.TurnOrder;
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
import com.myapps.web.myrpg.interfaces.dto.BattleResultViewModel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * GameController의 @WebMvcTest 슬라이스 테스트.
 *
 * <p>화면 렌더링(GET)과 행동 라우팅(POST) 엔드포인트를 검증한다.
 * Validates: Requirements 28.1, 28.2, 28.3, 28.4, 28.6
 */
@WebMvcTest(GameController.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class GameControllerTest {

    private static final Long PLAYER_ID = 1L;
    private static final String SESSION_PLAYER_ID = "PLAYER_ID";

    private final MockMvc mockMvc;

    @MockitoBean
    private GameSessionService gameSessionService;

    @MockitoBean
    private BattleSessionService battleSessionService;

    @MockitoBean
    private MasterDataLoader masterDataLoader;

    @MockitoBean
    private DungeonService dungeonService;

    @MockitoBean
    private DropService dropService;

    @MockitoBean
    private StatCalculator statCalculator;

    @MockitoBean
    private ShopService shopService;

    @MockitoBean
    private PlayerWeaponRepository playerWeaponRepository;

    @MockitoBean
    private PlayerArmorRepository playerArmorRepository;

    @MockitoBean
    private PlayerInventoryRepository playerInventoryRepository;

    @MockitoBean
    private PlayerActiveRunRepository playerActiveRunRepository;

    @MockitoBean
    private PlayerWeaponStatRepository playerWeaponStatRepository;

    @MockitoBean
    private PlayerArmorStatRepository playerArmorStatRepository;

    @MockitoBean
    private PlayerWeaponSkillRepository playerWeaponSkillRepository;

    GameControllerTest(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * 세션에 PLAYER_ID가 설정된 MockHttpSession을 생성한다.
     *
     * @return 플레이어 세션
     */
    private MockHttpSession createPlayerSession() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_PLAYER_ID, PLAYER_ID);
        return session;
    }

    /**
     * 테스트용 Player 객체를 생성한다.
     *
     * @return 기본 스탯 플레이어
     */
    private Player createTestPlayer() {
        return new Player("TestHero", 5, 100,
                80, 100, 40, 50,
                15, 10, 8, 3, 500);
    }

    // ─────────────────────────────────────────────────────────────
    // Req 28.1: 각 화면이 올바른 뷰를 반환하는지 검증
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Req 28.1 — 화면 렌더링")
    class ScreenRendering {

        @Test
        void should_returnTownView_when_playerSessionExists() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final Player player = createTestPlayer();
            when(gameSessionService.getPlayer(PLAYER_ID)).thenReturn(player);
            when(playerActiveRunRepository.findByPlayerId(PLAYER_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/rpg/town").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("rpg/town"))
                    .andExpect(model().attributeExists("town"));
        }

        @Test
        void should_returnCreateView_when_noPlayerSession() throws Exception {
            mockMvc.perform(get("/rpg/town"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("rpg/create"));
        }

        @Test
        void should_returnEquipmentViewWithArmorTab_when_equipmentRequested() throws Exception {
            final MockHttpSession session = createPlayerSession();
            when(playerWeaponRepository.findByPlayerId(PLAYER_ID)).thenReturn(List.of());
            when(playerArmorRepository.findByPlayerId(PLAYER_ID)).thenReturn(List.of());
            when(playerInventoryRepository.findByPlayerId(PLAYER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/rpg/equipment").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("rpg/equipment"))
                    .andExpect(model().attribute("activeTab", "armor"))
                    .andExpect(model().attributeExists("equipment"));
        }

        @Test
        void should_returnEquipmentViewWithWeaponTab_when_weaponsTabRequested() throws Exception {
            final MockHttpSession session = createPlayerSession();
            when(playerWeaponRepository.findByPlayerId(PLAYER_ID)).thenReturn(List.of());
            when(playerArmorRepository.findByPlayerId(PLAYER_ID)).thenReturn(List.of());
            when(playerInventoryRepository.findByPlayerId(PLAYER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/rpg/equipment/weapons").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("rpg/equipment"))
                    .andExpect(model().attribute("activeTab", "weapon"))
                    .andExpect(model().attributeExists("equipment"));
        }

        @Test
        void should_returnDungeonSelectView_when_dungeonSelectRequested() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final Player player = createTestPlayer();
            when(gameSessionService.getPlayer(PLAYER_ID)).thenReturn(player);
            when(masterDataLoader.allDungeons()).thenReturn(List.of());

            mockMvc.perform(get("/rpg/dungeon/select").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("rpg/dungeon-select"))
                    .andExpect(model().attributeExists("dungeonSelect"));
        }

        @Test
        void should_returnBattleView_when_activeBattleSessionExists() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final BattleSession battleSession = new BattleSession(
                    PLAYER_ID, 1L, "슬라임", 50, 8, 3, 5,
                    DamageType.PHYSICAL, TurnOrder.PLAYER_FIRST,
                    15, 10, 8, 3, 100, DamageType.PHYSICAL, 80, 40);
            session.setAttribute("BATTLE_SESSION", battleSession);
            when(battleSessionService.getBattleSession(any())).thenReturn(battleSession);
            when(playerWeaponRepository.findByPlayerId(PLAYER_ID)).thenReturn(List.of());
            when(playerInventoryRepository.findByPlayerId(PLAYER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/rpg/battle").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("rpg/battle"))
                    .andExpect(model().attributeExists("battle"));
        }

        @Test
        void should_redirectToTown_when_noBattleSession() throws Exception {
            final MockHttpSession session = createPlayerSession();
            when(battleSessionService.getBattleSession(any())).thenReturn(null);

            mockMvc.perform(get("/rpg/battle").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/town"));
        }

        @Test
        void should_returnBattleResultView_when_resultExists() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final BattleSession battleSession = new BattleSession(
                    PLAYER_ID, 1L, "슬라임", 50, 8, 3, 5,
                    DamageType.PHYSICAL, TurnOrder.PLAYER_FIRST,
                    15, 10, 8, 3, 100, DamageType.PHYSICAL, 80, 40);
            session.setAttribute("BATTLE_SESSION", battleSession);
            when(battleSessionService.getBattleSession(any())).thenReturn(battleSession);
            final BattleResultViewModel resultVm = new BattleResultViewModel(
                    "슬라임", 30, 10, null, null, "드랍 없음", false, false);
            session.setAttribute("BATTLE_RESULT", resultVm);

            mockMvc.perform(get("/rpg/battle/result").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("rpg/battle-result"))
                    .andExpect(model().attributeExists("result"));
        }

        @Test
        void should_returnShopView_when_shopRequested() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final Player player = createTestPlayer();
            when(gameSessionService.getPlayer(PLAYER_ID)).thenReturn(player);
            when(playerWeaponRepository.findByPlayerId(PLAYER_ID)).thenReturn(List.of());
            when(playerArmorRepository.findByPlayerId(PLAYER_ID)).thenReturn(List.of());
            when(masterDataLoader.allItems()).thenReturn(List.of());

            mockMvc.perform(get("/rpg/shop").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("rpg/shop"))
                    .andExpect(model().attributeExists("shop"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Req 28.2: 마을 화면에 레벨/HP/MP/Gold 표시 확인
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Req 28.2 — 마을 뷰 모델 속성")
    class TownViewModelAttributes {

        @Test
        void should_haveLevelHpMpGoldInTownModel_when_playerExists() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final Player player = createTestPlayer();
            when(gameSessionService.getPlayer(PLAYER_ID)).thenReturn(player);
            when(playerActiveRunRepository.findByPlayerId(PLAYER_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/rpg/town").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("rpg/town"))
                    .andExpect(model().attributeExists("town"));
        }

        @Test
        void should_showActiveRunStatus_when_playerHasActiveRun() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final Player player = createTestPlayer();
            final PlayerActiveRun activeRun = new PlayerActiveRun(
                    PLAYER_ID, 1L, 2, 80, 40, LocalDateTime.now());
            when(gameSessionService.getPlayer(PLAYER_ID)).thenReturn(player);
            when(playerActiveRunRepository.findByPlayerId(PLAYER_ID))
                    .thenReturn(Optional.of(activeRun));

            mockMvc.perform(get("/rpg/town").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("rpg/town"))
                    .andExpect(model().attributeExists("town"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Req 28.3 & 28.4: claim-reward 라우팅
    //   28.3: 스테이지 사이 → 다음 스테이지로 / 포기하고 마을로
    //   28.4: 보스(5스테이지) 승리 시 자동 마을 복귀
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Req 28.3, 28.4 — claim-reward 라우팅")
    class ClaimRewardRouting {

        @Test
        void should_redirectToDungeonExplore_when_dungeonNotCleared() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final BattleResultViewModel resultVm = new BattleResultViewModel(
                    "슬라임", 30, 10, null, null, "드랍 없음", false, false);
            session.setAttribute("BATTLE_RESULT", resultVm);

            mockMvc.perform(post("/rpg/battle/claim-reward").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/dungeon/explore"));
        }

        @Test
        void should_redirectToTown_when_bossStageCleared() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final BattleResultViewModel resultVm = new BattleResultViewModel(
                    "보스 드래곤", 100, 50, null, null, "무기 획득", true, true);
            session.setAttribute("BATTLE_RESULT", resultVm);

            mockMvc.perform(post("/rpg/battle/claim-reward").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/town"));
        }

        @Test
        void should_redirectToDungeonExplore_when_noResultInSession() throws Exception {
            final MockHttpSession session = createPlayerSession();

            mockMvc.perform(post("/rpg/battle/claim-reward").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/dungeon/explore"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Req 28.6: 전투 행동 라우팅 (기본공격·스킬·아이템·도망)
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Req 28.6 — 전투 행동 라우팅")
    class BattleActionRouting {

        @Test
        void should_redirectToBattle_when_attackAndBattleOngoing() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final BattleSession battleSession = createOngoingBattleSession();
            when(battleSessionService.getBattleSession(any())).thenReturn(battleSession);

            mockMvc.perform(post("/rpg/battle/attack").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/battle"));
        }

        @Test
        void should_redirectToBattleResult_when_attackAndPlayerWon() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final BattleSession battleSession = createWonBattleSession();
            when(battleSessionService.getBattleSession(any())).thenReturn(battleSession);
            when(gameSessionService.getPlayer(PLAYER_ID)).thenReturn(createTestPlayer());
            when(gameSessionService.grantBattleReward(anyLong(), anyLong())).thenReturn(null);
            when(playerActiveRunRepository.findByPlayerId(PLAYER_ID))
                    .thenReturn(Optional.empty());
            final com.myapps.web.myrpg.domain.template.MonsterTemplate monster =
                    new com.myapps.web.myrpg.domain.template.MonsterTemplate(
                            1L, "슬라임", 50, 8, 3, 5,
                            DamageType.PHYSICAL, 30, 10, false);
            when(masterDataLoader.findMonster(1L)).thenReturn(monster);

            mockMvc.perform(post("/rpg/battle/attack").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/battle/result"));
        }

        @Test
        void should_redirectToBattle_when_skillAttackAndBattleOngoing() throws Exception {
            final MockHttpSession session = createPlayerSession();
            final BattleSession battleSession = createOngoingBattleSession();
            when(battleSessionService.getBattleSession(any())).thenReturn(battleSession);

            mockMvc.perform(post("/rpg/battle/skill")
                            .param("skillId", "1")
                            .session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/battle"));
        }

        @Test
        void should_redirectToTown_when_fleeSucceeds() throws Exception {
            final MockHttpSession session = createPlayerSession();
            when(battleSessionService.playerFlee(any())).thenReturn(true);

            mockMvc.perform(post("/rpg/battle/flee").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/town"));
        }

        @Test
        void should_redirectToBattle_when_fleeFails() throws Exception {
            final MockHttpSession session = createPlayerSession();
            when(battleSessionService.playerFlee(any())).thenReturn(false);
            final BattleSession battleSession = createOngoingBattleSession();
            when(battleSessionService.getBattleSession(any())).thenReturn(battleSession);

            mockMvc.perform(post("/rpg/battle/flee").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/battle"));
        }

        /**
         * 진행 중인 전투 세션을 생성한다.
         */
        private BattleSession createOngoingBattleSession() {
            return new BattleSession(
                    PLAYER_ID, 1L, "슬라임", 50, 8, 3, 5,
                    DamageType.PHYSICAL, TurnOrder.PLAYER_FIRST,
                    15, 10, 8, 3, 100, DamageType.PHYSICAL, 80, 40);
        }

        /**
         * 플레이어 승리 상태의 전투 세션을 생성한다.
         */
        private BattleSession createWonBattleSession() {
            final BattleSession session = new BattleSession(
                    PLAYER_ID, 1L, "슬라임", 50, 8, 3, 5,
                    DamageType.PHYSICAL, TurnOrder.PLAYER_FIRST,
                    15, 10, 8, 3, 100, DamageType.PHYSICAL, 80, 40);
            session.changeStatus(BattleSession.BattleStatus.PLAYER_WON);
            return session;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // POST 행동 라우팅 — 기본 리다이렉트 검증
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST 행동 라우팅")
    class PostActionRouting {

        @Test
        void should_redirectToTown_when_characterCreated() throws Exception {
            final Player player = createTestPlayer();
            when(gameSessionService.createCharacter("NewHero")).thenReturn(player);

            mockMvc.perform(post("/rpg/character/create")
                            .param("name", "NewHero"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/town"));
        }

        @Test
        void should_redirectToEquipmentWeapons_when_weaponEquipped() throws Exception {
            final MockHttpSession session = createPlayerSession();

            mockMvc.perform(post("/rpg/equipment/equip-weapon")
                            .param("weaponId", "1")
                            .session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/equipment/weapons"));
        }

        @Test
        void should_redirectToEquipment_when_armorEquipped() throws Exception {
            final MockHttpSession session = createPlayerSession();

            mockMvc.perform(post("/rpg/equipment/equip-armor")
                            .param("armorId", "1")
                            .session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/equipment"));
        }

        @Test
        void should_redirectToEquipmentWeapons_when_skillAttached() throws Exception {
            final MockHttpSession session = createPlayerSession();

            mockMvc.perform(post("/rpg/equipment/attach-skill")
                            .param("weaponId", "1")
                            .param("skillId", "1")
                            .session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/equipment/weapons"));
        }

        @Test
        void should_redirectToDungeonExplore_when_dungeonEntered() throws Exception {
            final MockHttpSession session = createPlayerSession();

            mockMvc.perform(post("/rpg/dungeon/enter")
                            .param("dungeonId", "1")
                            .session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/dungeon/explore"));
        }

        @Test
        void should_redirectToDungeonExplore_when_nextStage() throws Exception {
            final MockHttpSession session = createPlayerSession();

            mockMvc.perform(post("/rpg/dungeon/next-stage").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/dungeon/explore"));
        }

        @Test
        void should_redirectToTown_when_dungeonAbandoned() throws Exception {
            final MockHttpSession session = createPlayerSession();

            mockMvc.perform(post("/rpg/dungeon/abandon").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/town"));
        }

        @Test
        void should_redirectToShop_when_weaponSold() throws Exception {
            final MockHttpSession session = createPlayerSession();

            mockMvc.perform(post("/rpg/shop/sell-weapon")
                            .param("weaponId", "1")
                            .session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/shop"));
        }

        @Test
        void should_redirectToShop_when_armorSold() throws Exception {
            final MockHttpSession session = createPlayerSession();

            mockMvc.perform(post("/rpg/shop/sell-armor")
                            .param("armorId", "1")
                            .session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/shop"));
        }

        @Test
        void should_redirectToShop_when_potionBought() throws Exception {
            final MockHttpSession session = createPlayerSession();
            when(masterDataLoader.findItem(1L)).thenReturn(
                    new com.myapps.web.myrpg.domain.template.ItemTemplate(
                            1L, "HP 포션", com.myapps.web.myrpg.domain.model.ItemType.POTION,
                            com.myapps.web.myrpg.domain.model.EffectType.HEAL_HP, 30, 50));

            mockMvc.perform(post("/rpg/shop/buy")
                            .param("itemId", "1")
                            .session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/rpg/shop"));
        }
    }
}
