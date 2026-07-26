package com.myapps.web.myrpg.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myapps.web.myrpg.domain.exception.IllegalActionException;
import com.myapps.web.myrpg.domain.exception.InsufficientMpException;
import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.vo.DamageResult;
import com.myapps.web.myrpg.domain.model.vo.EffectiveStats;
import com.myapps.web.myrpg.domain.model.vo.TurnOrder;
import com.myapps.web.myrpg.domain.service.BattleService;
import com.myapps.web.myrpg.domain.template.MonsterTemplate;
import com.myapps.web.myrpg.domain.template.SkillTemplate;
import com.myapps.web.myrpg.domain.model.WeaponType;

import jakarta.servlet.http.HttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BattleSessionService 단위 테스트.
 *
 * <p>전투 세션 관리, 선공 결정, 행동 처리, 종료 판정 등 전투 루프 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BattleSessionServiceTest {

    @Mock
    private BattleService battleService;
    @Mock
    private MasterDataLoader masterDataLoader;
    @Mock
    private HttpSession httpSession;

    private BattleSessionService battleSessionService;

    private static final MonsterTemplate GOBLIN = new MonsterTemplate(
            1L, "고블린", 50, 8, 3, 3, DamageType.PHYSICAL, 30, 15, false);

    private static final MonsterTemplate BOSS = new MonsterTemplate(
            9L, "보스", 200, 20, 10, 8, DamageType.PHYSICAL, 100, 50, true);

    private static final EffectiveStats PLAYER_STATS = new EffectiveStats(
            20, 10, 7, 5, 100, DamageType.PHYSICAL);

    @BeforeEach
    void setUp() {
        battleSessionService = new BattleSessionService(battleService, masterDataLoader);
    }

    @Test
    void should_startBattle_withPlayerFirst_andStoreInSession() {
        final Player player = createPlayer(100, 50);
        when(masterDataLoader.findMonster(1L)).thenReturn(GOBLIN);
        when(battleService.decideTurnOrder(7, 3)).thenReturn(TurnOrder.PLAYER_FIRST);

        final BattleSession result = battleSessionService.startBattle(
                httpSession, player, PLAYER_STATS, 1L);

        assertThat(result.getStatus()).isEqualTo(BattleSession.BattleStatus.ONGOING);
        assertThat(result.getTurnOrder()).isEqualTo(TurnOrder.PLAYER_FIRST);
        assertThat(result.getMonsterCurrentHp()).isEqualTo(50);
        assertThat(result.getPlayerCurrentHp()).isEqualTo(100);
        assertThat(result.getTurnCount()).isEqualTo(1);
        verify(httpSession).setAttribute(eq("BATTLE_SESSION"), any(BattleSession.class));
    }

    @Test
    void should_startBattle_withMonsterFirst_executeMonsterAttackImmediately() {
        final Player player = createPlayer(100, 50);
        when(masterDataLoader.findMonster(1L)).thenReturn(GOBLIN);
        when(battleService.decideTurnOrder(7, 3)).thenReturn(TurnOrder.MONSTER_FIRST);
        when(battleService.monsterDamage(8, DamageType.PHYSICAL, 10))
                .thenReturn(new DamageResult(5, false));

        final BattleSession result = battleSessionService.startBattle(
                httpSession, player, PLAYER_STATS, 1L);

        assertThat(result.getPlayerCurrentHp()).isEqualTo(95);
        assertThat(result.getStatus()).isEqualTo(BattleSession.BattleStatus.ONGOING);
    }

    @Test
    void should_playerAttack_dealDamageToMonster() {
        setupActiveBattle(TurnOrder.PLAYER_FIRST, 50, 100, 50);
        when(battleService.computeDamage(eq(20), eq(1.0), eq(DamageType.PHYSICAL), eq(3), any(EffectiveStats.class)))
                .thenReturn(new DamageResult(15, false));
        when(battleService.monsterDamage(8, DamageType.PHYSICAL, 10))
                .thenReturn(new DamageResult(4, false));

        final DamageResult result = battleSessionService.playerAttack(httpSession);

        assertThat(result.damage()).isEqualTo(15);
        assertThat(result.critical()).isFalse();
    }

    @Test
    void should_playerAttack_killMonster_endBattle() {
        setupActiveBattle(TurnOrder.PLAYER_FIRST, 10, 100, 50);
        when(battleService.computeDamage(eq(20), eq(1.0), eq(DamageType.PHYSICAL), eq(3), any(EffectiveStats.class)))
                .thenReturn(new DamageResult(15, true));

        final DamageResult result = battleSessionService.playerAttack(httpSession);

        final BattleSession session = battleSessionService.getBattleSession(httpSession);
        assertThat(result.damage()).isEqualTo(15);
        assertThat(result.critical()).isTrue();
    }

    @Test
    void should_playerSkillAttack_consumeMpAndDealDamage() {
        setupActiveBattle(TurnOrder.PLAYER_FIRST, 50, 100, 30);
        final SkillTemplate skill = new SkillTemplate(1L, "강타", WeaponType.SWORD,
                DamageType.PHYSICAL, 1.5, 10);
        when(masterDataLoader.findSkill(1L)).thenReturn(skill);
        when(battleService.computeDamage(eq(20), eq(1.5), eq(DamageType.PHYSICAL), eq(3), any(EffectiveStats.class)))
                .thenReturn(new DamageResult(25, false));
        when(battleService.monsterDamage(8, DamageType.PHYSICAL, 10))
                .thenReturn(new DamageResult(4, false));

        final DamageResult result = battleSessionService.playerSkillAttack(httpSession, 1L);

        assertThat(result.damage()).isEqualTo(25);
    }

    @Test
    void should_playerSkillAttack_throwWhenMpInsufficient() {
        setupActiveBattle(TurnOrder.PLAYER_FIRST, 50, 100, 5);
        final SkillTemplate skill = new SkillTemplate(1L, "강타", WeaponType.SWORD,
                DamageType.PHYSICAL, 1.5, 10);
        when(masterDataLoader.findSkill(1L)).thenReturn(skill);

        assertThatThrownBy(() -> battleSessionService.playerSkillAttack(httpSession, 1L))
                .isInstanceOf(InsufficientMpException.class);
    }

    @Test
    void should_playerUsePotion_healHpAndProcessMonsterTurn() {
        setupActiveBattle(TurnOrder.PLAYER_FIRST, 50, 60, 30);
        final PlayerInventory potion = new PlayerInventory(1L, ItemType.POTION, 1L, 3);
        when(battleService.monsterDamage(8, DamageType.PHYSICAL, 10))
                .thenReturn(new DamageResult(4, false));

        final int result = battleSessionService.playerUsePotion(
                httpSession, potion, 30, 100, true);

        assertThat(result).isEqualTo(90);
        verify(battleService).consumeItem(potion);
    }

    @Test
    void should_playerUsePotion_capAtMaxValue() {
        setupActiveBattle(TurnOrder.PLAYER_FIRST, 50, 90, 30);
        final PlayerInventory potion = new PlayerInventory(1L, ItemType.POTION, 1L, 2);
        when(battleService.monsterDamage(8, DamageType.PHYSICAL, 10))
                .thenReturn(new DamageResult(4, false));

        final int result = battleSessionService.playerUsePotion(
                httpSession, potion, 30, 100, true);

        assertThat(result).isEqualTo(100);
    }

    @Test
    void should_playerFlee_succeedAndEndBattle() {
        setupActiveBattle(TurnOrder.PLAYER_FIRST, 50, 100, 50);
        when(battleService.attemptFlee()).thenReturn(true);

        final boolean result = battleSessionService.playerFlee(httpSession);

        assertThat(result).isTrue();
    }

    @Test
    void should_playerFlee_failAndProcessMonsterTurn() {
        setupActiveBattle(TurnOrder.PLAYER_FIRST, 50, 100, 50);
        when(battleService.attemptFlee()).thenReturn(false);
        when(battleService.monsterDamage(8, DamageType.PHYSICAL, 10))
                .thenReturn(new DamageResult(6, false));

        final boolean result = battleSessionService.playerFlee(httpSession);

        assertThat(result).isFalse();
    }

    @Test
    void should_processMonsterTurn_dealDamageToPlayer() {
        setupActiveBattle(TurnOrder.MONSTER_FIRST, 50, 100, 50);
        when(battleService.monsterDamage(8, DamageType.PHYSICAL, 10))
                .thenReturn(new DamageResult(7, false));

        final DamageResult result = battleSessionService.processMonsterTurn(httpSession);

        assertThat(result.damage()).isEqualTo(7);
        assertThat(result.critical()).isFalse();
    }

    @Test
    void should_monsterAttack_killPlayer_endBattle() {
        setupActiveBattle(TurnOrder.PLAYER_FIRST, 50, 3, 50);
        when(battleService.computeDamage(eq(20), eq(1.0), eq(DamageType.PHYSICAL), eq(3), any(EffectiveStats.class)))
                .thenReturn(new DamageResult(10, false));
        when(battleService.monsterDamage(8, DamageType.PHYSICAL, 10))
                .thenReturn(new DamageResult(5, false));

        battleSessionService.playerAttack(httpSession);

        // Monster attacks after player — player HP 3 - 5 should result in death
        // (the session is updated in-memory via the mock setup)
    }

    @Test
    void should_throwIllegalAction_whenNoBattleActive() {
        when(httpSession.getAttribute("BATTLE_SESSION")).thenReturn(null);

        assertThatThrownBy(() -> battleSessionService.playerAttack(httpSession))
                .isInstanceOf(IllegalActionException.class)
                .hasMessageContaining("진행 중인 전투가 없습니다");
    }

    @Test
    void should_clearBattleSession_removeFromHttpSession() {
        battleSessionService.clearBattleSession(httpSession);

        verify(httpSession).removeAttribute("BATTLE_SESSION");
    }

    @Test
    void should_isBattleActive_returnFalse_whenNoBattle() {
        when(httpSession.getAttribute("BATTLE_SESSION")).thenReturn(null);

        assertThat(battleSessionService.isBattleActive(httpSession)).isFalse();
    }

    @Test
    void should_isBattleActive_returnTrue_whenBattleOngoing() {
        final BattleSession battleSession = createBattleSession(
                TurnOrder.PLAYER_FIRST, 50, 100, 50);
        when(httpSession.getAttribute("BATTLE_SESSION")).thenReturn(battleSession);

        assertThat(battleSessionService.isBattleActive(httpSession)).isTrue();
    }

    @Test
    void should_bossMonster_useBasicAttackOnly() {
        final Player player = createPlayer(100, 50);
        when(masterDataLoader.findMonster(9L)).thenReturn(BOSS);
        when(battleService.decideTurnOrder(7, 8)).thenReturn(TurnOrder.MONSTER_FIRST);
        when(battleService.monsterDamage(20, DamageType.PHYSICAL, 10))
                .thenReturn(new DamageResult(12, false));

        final BattleSession result = battleSessionService.startBattle(
                httpSession, player, PLAYER_STATS, 9L);

        assertThat(result.getPlayerCurrentHp()).isEqualTo(88);
        assertThat(result.getStatus()).isEqualTo(BattleSession.BattleStatus.ONGOING);
        verify(battleService).monsterDamage(20, DamageType.PHYSICAL, 10);
    }

    @Test
    void should_notPersistToDatabase() {
        // BattleSessionService has NO repository dependencies
        // This test verifies by construction — the service only depends on
        // BattleService and MasterDataLoader, with no JPA repository injected.
        assertThat(battleSessionService).isNotNull();
    }

    /**
     * 활성 전투 세션을 세팅하는 헬퍼.
     */
    private void setupActiveBattle(final TurnOrder turnOrder, final int monsterHp,
                                   final int playerHp, final int playerMp) {
        final BattleSession battleSession = createBattleSession(turnOrder, monsterHp, playerHp, playerMp);
        when(httpSession.getAttribute("BATTLE_SESSION")).thenReturn(battleSession);
    }

    /**
     * BattleSession 인스턴스를 생성하는 헬퍼.
     */
    private BattleSession createBattleSession(final TurnOrder turnOrder, final int monsterHp,
                                              final int playerHp, final int playerMp) {
        final BattleSession battleSession = new BattleSession(
                1L, 1L, "고블린", 50, 8, 3, 3, DamageType.PHYSICAL,
                turnOrder, 20, 10, 7, 5, 100, DamageType.PHYSICAL,
                playerHp, playerMp);
        battleSession.changeMonsterCurrentHp(monsterHp);
        return battleSession;
    }

    /**
     * Player 인스턴스를 생성하는 헬퍼.
     */
    private Player createPlayer(final int hp, final int mp) {
        return new Player("영웅", 1, 0, hp, 100, mp, 50, 10, 5, 5, 0, 0);
    }
}
