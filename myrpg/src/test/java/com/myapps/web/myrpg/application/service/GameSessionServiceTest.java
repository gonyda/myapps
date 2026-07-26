package com.myapps.web.myrpg.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myapps.web.myrpg.application.exception.PlayerNotFoundException;
import com.myapps.web.myrpg.domain.exception.IllegalActionException;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerActiveRun;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;
import com.myapps.web.myrpg.domain.model.PlayerWeaponStat;
import com.myapps.web.myrpg.domain.model.StatType;
import com.myapps.web.myrpg.domain.model.WeaponType;
import com.myapps.web.myrpg.domain.model.vo.LevelUpResult;
import com.myapps.web.myrpg.domain.model.vo.RolledWeapon;
import com.myapps.web.myrpg.domain.model.vo.StatRoll;
import com.myapps.web.myrpg.domain.repository.PlayerActiveRunRepository;
import com.myapps.web.myrpg.domain.repository.PlayerArmorRepository;
import com.myapps.web.myrpg.domain.repository.PlayerArmorStatRepository;
import com.myapps.web.myrpg.domain.repository.PlayerDungeonProgressRepository;
import com.myapps.web.myrpg.domain.repository.PlayerInventoryRepository;
import com.myapps.web.myrpg.domain.repository.PlayerRepository;
import com.myapps.web.myrpg.domain.repository.PlayerWeaponRepository;
import com.myapps.web.myrpg.domain.repository.PlayerWeaponSkillRepository;
import com.myapps.web.myrpg.domain.repository.PlayerWeaponStatRepository;
import com.myapps.web.myrpg.domain.service.CharacterService;
import com.myapps.web.myrpg.domain.service.DropService;
import com.myapps.web.myrpg.domain.service.EquipmentService;
import com.myapps.web.myrpg.domain.service.ShopService;
import com.myapps.web.myrpg.domain.template.MonsterTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;
import com.myapps.web.myrpg.domain.model.DamageType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GameSessionService 단위 테스트.
 *
 * <p>도메인 서비스와 리포지터리를 모킹하여 오케스트레이션 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class GameSessionServiceTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerWeaponRepository playerWeaponRepository;
    @Mock
    private PlayerWeaponStatRepository playerWeaponStatRepository;
    @Mock
    private PlayerWeaponSkillRepository playerWeaponSkillRepository;
    @Mock
    private PlayerArmorRepository playerArmorRepository;
    @Mock
    private PlayerArmorStatRepository playerArmorStatRepository;
    @Mock
    private PlayerInventoryRepository playerInventoryRepository;
    @Mock
    private PlayerDungeonProgressRepository playerDungeonProgressRepository;
    @Mock
    private PlayerActiveRunRepository playerActiveRunRepository;
    @Mock
    private CharacterService characterService;
    @Mock
    private DropService dropService;
    @Mock
    private ShopService shopService;
    @Mock
    private EquipmentService equipmentService;
    @Mock
    private MasterDataLoader masterDataLoader;

    private GameSessionService gameSessionService;

    @BeforeEach
    void setUp() {
        gameSessionService = new GameSessionService(
                playerRepository, playerWeaponRepository, playerWeaponStatRepository,
                playerWeaponSkillRepository, playerArmorRepository, playerArmorStatRepository,
                playerInventoryRepository, playerDungeonProgressRepository,
                playerActiveRunRepository, characterService, dropService,
                shopService, equipmentService, masterDataLoader);
    }

    @Test
    void should_createCharacter_and_grantStarterWeapon() {
        final Player player = new Player("영웅", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 0);
        final WeaponTemplate template = new WeaponTemplate(1L, "낡은 검", WeaponType.SWORD, 10, 2, 2, 20);
        final RolledWeapon rolled = new RolledWeapon(1L, WeaponType.SWORD, Grade.COMMON, 1,
                12, 2, 2, 1, List.of(new StatRoll(StatType.ATTACK, 1)), "[일반] 낡은 검");

        when(characterService.createInitialCharacter("영웅")).thenReturn(player);
        when(playerRepository.save(player)).thenReturn(player);
        when(masterDataLoader.findWeaponTemplate(1L)).thenReturn(template);
        when(dropService.buildWeaponInstance(template, Grade.COMMON, 1)).thenReturn(rolled);
        when(playerWeaponRepository.save(any(PlayerWeapon.class))).thenAnswer(invocation -> {
            final PlayerWeapon w = invocation.getArgument(0);
            return w;
        });
        when(playerWeaponStatRepository.save(any(PlayerWeaponStat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final Player result = gameSessionService.createCharacter("영웅");

        assertThat(result.getName()).isEqualTo("영웅");
        verify(characterService).createInitialCharacter("영웅");
        verify(playerWeaponRepository).save(any(PlayerWeapon.class));
    }

    @Test
    void should_grantBattleReward_addGoldAndExp() {
        final Player player = new Player("영웅", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 0);
        final MonsterTemplate monster = new MonsterTemplate(1L, "고블린", 50, 8, 3, 3,
                DamageType.PHYSICAL, 30, 15, false);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(masterDataLoader.findMonster(1L)).thenReturn(monster);
        when(characterService.gainExp(player, 30)).thenReturn(new LevelUpResult(1, 0, 30));

        final LevelUpResult result = gameSessionService.grantBattleReward(1L, 1L);

        assertThat(player.getGold()).isEqualTo(15);
        assertThat(result.remainingExp()).isEqualTo(30);
    }

    @Test
    void should_applyDeathPenalty_deleteRunAndRestoreToTown() {
        final Player player = new Player("영웅", 2, 50, 80, 120, 40, 60, 13, 7, 6, 1, 100);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        gameSessionService.applyDeathPenalty(1L);

        verify(characterService).applyExpPenalty(player, 0.10);
        verify(playerActiveRunRepository).deleteByPlayerId(1L);
        verify(characterService).restoreToTown(player);
    }

    @Test
    void should_applyFleePenalty_with5PercentExpLoss() {
        final Player player = new Player("영웅", 2, 50, 80, 120, 40, 60, 13, 7, 6, 1, 100);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        gameSessionService.applyFleePenalty(1L);

        verify(characterService).applyExpPenalty(player, 0.05);
        verify(playerActiveRunRepository).deleteByPlayerId(1L);
        verify(characterService).restoreToTown(player);
    }

    @Test
    void should_enterDungeon_createActiveRun() {
        final Player player = new Player("영웅", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 0);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerActiveRunRepository.findByPlayerId(1L)).thenReturn(Optional.empty());
        when(playerActiveRunRepository.save(any(PlayerActiveRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final PlayerActiveRun result = gameSessionService.enterDungeon(1L, 1L);

        assertThat(result.getPlayerId()).isEqualTo(1L);
        assertThat(result.getDungeonId()).isEqualTo(1L);
        assertThat(result.getClearedStage()).isZero();
        assertThat(result.getCheckpointHp()).isEqualTo(100);
        assertThat(result.getCheckpointMp()).isEqualTo(50);
    }

    @Test
    void should_throwIllegalAction_whenDungeonAlreadyInProgress() {
        final Player player = new Player("영웅", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 0);
        final PlayerActiveRun existingRun = new PlayerActiveRun(1L, 1L, 2, 80, 40, LocalDateTime.now());
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerActiveRunRepository.findByPlayerId(1L)).thenReturn(Optional.of(existingRun));

        assertThatThrownBy(() -> gameSessionService.enterDungeon(1L, 2L))
                .isInstanceOf(IllegalActionException.class);
    }

    @Test
    void should_abandonDungeon_withNoExpPenalty() {
        final Player player = new Player("영웅", 2, 50, 80, 120, 40, 60, 13, 7, 6, 1, 100);
        final PlayerActiveRun activeRun = new PlayerActiveRun(1L, 1L, 2, 80, 40, LocalDateTime.now());
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerActiveRunRepository.findByPlayerId(1L)).thenReturn(Optional.of(activeRun));

        gameSessionService.abandonDungeon(1L);

        verify(playerActiveRunRepository).deleteByPlayerId(1L);
        verify(characterService).restoreToTown(player);
    }

    @Test
    void should_throwIllegalAction_whenAbandonWithNoActiveRun() {
        final Player player = new Player("영웅", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 0);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerActiveRunRepository.findByPlayerId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameSessionService.abandonDungeon(1L))
                .isInstanceOf(IllegalActionException.class);
    }

    @Test
    void should_saveCheckpoint_updateActiveRun() {
        final PlayerActiveRun activeRun = new PlayerActiveRun(1L, 1L, 0, 100, 50, LocalDateTime.now());
        when(playerActiveRunRepository.findByPlayerId(1L)).thenReturn(Optional.of(activeRun));

        gameSessionService.saveCheckpoint(1L, 3, 70, 30);

        assertThat(activeRun.getClearedStage()).isEqualTo(3);
        assertThat(activeRun.getCheckpointHp()).isEqualTo(70);
        assertThat(activeRun.getCheckpointMp()).isEqualTo(30);
    }

    @Test
    void should_resumeDungeon_restoreHpMp() {
        final Player player = new Player("영웅", 2, 50, 60, 120, 20, 60, 13, 7, 6, 1, 100);
        final PlayerActiveRun activeRun = new PlayerActiveRun(1L, 1L, 2, 90, 45, LocalDateTime.now());
        when(playerActiveRunRepository.findByPlayerId(1L)).thenReturn(Optional.of(activeRun));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        final Optional<PlayerActiveRun> result = gameSessionService.resumeDungeon(1L);

        assertThat(result).isPresent();
        assertThat(player.getHp()).isEqualTo(90);
        assertThat(player.getMp()).isEqualTo(45);
    }

    @Test
    void should_completeDungeon_updateProgressAndRestore() {
        final Player player = new Player("영웅", 2, 50, 80, 120, 40, 60, 13, 7, 6, 1, 100);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerDungeonProgressRepository.findByPlayerIdAndDungeonId(1L, 1L))
                .thenReturn(Optional.empty());

        gameSessionService.completeDungeon(1L, 1L);

        verify(playerDungeonProgressRepository).save(any());
        verify(playerActiveRunRepository).deleteByPlayerId(1L);
        verify(characterService).restoreToTown(player);
    }

    @Test
    void should_saveSkillBookDrop_incrementQuantity() {
        final PlayerInventory existing = new PlayerInventory(1L, ItemType.SKILL_BOOK, 1L, 2);
        when(playerInventoryRepository.findByPlayerIdAndItemTypeAndItemRefId(1L, ItemType.SKILL_BOOK, 1L))
                .thenReturn(Optional.of(existing));

        gameSessionService.saveSkillBookDrop(1L, 1L);

        assertThat(existing.getQuantity()).isEqualTo(3);
    }

    @Test
    void should_saveGoldReward_addGold() {
        final Player player = new Player("영웅", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 50);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        gameSessionService.saveGoldReward(1L, 30);

        assertThat(player.getGold()).isEqualTo(80);
    }

    @Test
    void should_getPlayer_throwWhenNotFound() {
        when(playerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameSessionService.getPlayer(999L))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void should_getPlayer_returnPlayerWhenExists() {
        final Player player = new Player("영웅", 1, 0, 100, 100, 50, 50, 10, 5, 5, 0, 0);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        final Player result = gameSessionService.getPlayer(1L);

        assertThat(result.getName()).isEqualTo("영웅");
    }
}
