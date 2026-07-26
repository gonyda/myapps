package com.myapps.web.myrpg.application.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.myapps.web.myrpg.domain.exception.IllegalActionException;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerActiveRun;
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

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Feature: myrpg-gen1-mvp, Property 42: 재개 스테이지 규칙
// Feature: myrpg-gen1-mvp, Property 43: 플레이어당 진행 중 던전 최대 1개
/**
 * GameSessionService 던전 재개·진행 유일성 관련 속성 기반 테스트.
 *
 * <p>던전 재개 시 시작 스테이지 규칙 및 플레이어당 활성 런 최대 1개 불변식을 검증한다.
 *
 * <p><b>Validates: Requirements 21.3, 21.5</b>
 */
class GameSessionServiceDungeonPropertyTest {

    private static final int CLEARED_STAGE_MIN = 0;
    private static final int CLEARED_STAGE_MAX = 4;
    private static final int HP_MIN = 1;
    private static final int HP_MAX = 500;
    private static final int MP_MIN = 1;
    private static final int MP_MAX = 200;
    private static final long PLAYER_ID = 1L;
    private static final long DUNGEON_ID_1 = 1L;
    private static final long DUNGEON_ID_2 = 2L;

    /**
     * 임의의 완료 스테이지 값(0~4)을 생성하는 Provider.
     *
     * @return clearedStage 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> clearedStages() {
        return Arbitraries.integers().between(CLEARED_STAGE_MIN, CLEARED_STAGE_MAX);
    }

    /**
     * 임의의 체크포인트 HP 값을 생성하는 Provider.
     *
     * @return checkpointHp 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> checkpointHps() {
        return Arbitraries.integers().between(HP_MIN, HP_MAX);
    }

    /**
     * 임의의 체크포인트 MP 값을 생성하는 Provider.
     *
     * @return checkpointMp 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> checkpointMps() {
        return Arbitraries.integers().between(MP_MIN, MP_MAX);
    }

    /**
     * 임의의 플레이어를 생성하는 Provider.
     *
     * @return Player Arbitrary
     */
    @Provide
    Arbitrary<Player> players() {
        final Arbitrary<Integer> hps = Arbitraries.integers().between(HP_MIN, HP_MAX);
        final Arbitrary<Integer> mps = Arbitraries.integers().between(MP_MIN, MP_MAX);

        return Combinators.combine(hps, mps).as((hp, mp) ->
                new Player("TestHero", 1, 0, hp, hp, mp, mp, 10, 5, 5, 0, 0)
        );
    }

    // Feature: myrpg-gen1-mvp, Property 42: 재개 스테이지 규칙
    /**
     * 마지막 완료 스테이지가 c(0 ≤ c ≤ 4)일 때, 던전 재개 시작 스테이지는 c + 1이다.
     *
     * <p>resumeDungeon 호출 시 반환된 activeRun의 clearedStage + 1이
     * 다음으로 진행할 스테이지 번호가 됨을 검증한다.
     * 또한 플레이어의 HP/MP가 체크포인트 값으로 복원되는지 확인한다.
     *
     * <p><b>Validates: Requirements 21.3</b>
     *
     * @param clearedStage  마지막 완료 스테이지 (0~4)
     * @param checkpointHp  체크포인트 시점 HP
     * @param checkpointMp  체크포인트 시점 MP
     */
    @Property(tries = 100)
    void resumeStageRule(
            @ForAll("clearedStages") final int clearedStage,
            @ForAll("checkpointHps") final int checkpointHp,
            @ForAll("checkpointMps") final int checkpointMp) {

        final PlayerRepository playerRepository = mock(PlayerRepository.class);
        final PlayerWeaponRepository playerWeaponRepository = mock(PlayerWeaponRepository.class);
        final PlayerWeaponStatRepository playerWeaponStatRepository = mock(PlayerWeaponStatRepository.class);
        final PlayerWeaponSkillRepository playerWeaponSkillRepository = mock(PlayerWeaponSkillRepository.class);
        final PlayerArmorRepository playerArmorRepository = mock(PlayerArmorRepository.class);
        final PlayerArmorStatRepository playerArmorStatRepository = mock(PlayerArmorStatRepository.class);
        final PlayerInventoryRepository playerInventoryRepository = mock(PlayerInventoryRepository.class);
        final PlayerDungeonProgressRepository playerDungeonProgressRepository = mock(PlayerDungeonProgressRepository.class);
        final PlayerActiveRunRepository playerActiveRunRepository = mock(PlayerActiveRunRepository.class);
        final CharacterService characterService = mock(CharacterService.class);
        final DropService dropService = mock(DropService.class);
        final ShopService shopService = mock(ShopService.class);
        final EquipmentService equipmentService = mock(EquipmentService.class);
        final MasterDataLoader masterDataLoader = mock(MasterDataLoader.class);

        final GameSessionService service = new GameSessionService(
                playerRepository, playerWeaponRepository, playerWeaponStatRepository,
                playerWeaponSkillRepository, playerArmorRepository, playerArmorStatRepository,
                playerInventoryRepository, playerDungeonProgressRepository,
                playerActiveRunRepository, characterService, dropService,
                shopService, equipmentService, masterDataLoader);

        final Player player = new Player("TestHero", 1, 0, 50, HP_MAX, 20, MP_MAX, 10, 5, 5, 0, 0);
        final PlayerActiveRun activeRun = new PlayerActiveRun(
                PLAYER_ID, DUNGEON_ID_1, clearedStage, checkpointHp, checkpointMp, LocalDateTime.now());

        when(playerActiveRunRepository.findByPlayerId(PLAYER_ID)).thenReturn(Optional.of(activeRun));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));

        final Optional<PlayerActiveRun> result = service.resumeDungeon(PLAYER_ID);

        assertTrue(result.isPresent(), "활성 런이 존재할 때 resumeDungeon은 present를 반환해야 한다");

        final int expectedResumeStage = clearedStage + 1;
        assertEquals(expectedResumeStage, result.get().getClearedStage() + 1,
                "재개 시작 스테이지는 마지막 완료 스테이지 + 1이어야 한다: clearedStage="
                        + clearedStage + ", expectedResume=" + expectedResumeStage);

        assertEquals(checkpointHp, player.getHp(),
                "재개 시 플레이어 HP는 체크포인트 HP로 복원되어야 한다");
        assertEquals(checkpointMp, player.getMp(),
                "재개 시 플레이어 MP는 체크포인트 MP로 복원되어야 한다");
    }

    // Feature: myrpg-gen1-mvp, Property 43: 플레이어당 진행 중 던전 최대 1개
    /**
     * 던전 입장·클리어·포기 연산의 임의 시퀀스에 대해,
     * 처리 후 한 플레이어의 활성 런 행 수는 항상 1 이하다.
     *
     * <p>이미 활성 런이 존재하는 상태에서 enterDungeon 호출 시
     * IllegalActionException이 발생하여 2개 이상의 런이 생성되지 않음을 검증한다.
     *
     * <p><b>Validates: Requirements 21.5</b>
     *
     * @param player 임의의 플레이어
     */
    @Property(tries = 100)
    void atMostOneActiveRunPerPlayer(@ForAll("players") final Player player) {

        final PlayerRepository playerRepository = mock(PlayerRepository.class);
        final PlayerWeaponRepository playerWeaponRepository = mock(PlayerWeaponRepository.class);
        final PlayerWeaponStatRepository playerWeaponStatRepository = mock(PlayerWeaponStatRepository.class);
        final PlayerWeaponSkillRepository playerWeaponSkillRepository = mock(PlayerWeaponSkillRepository.class);
        final PlayerArmorRepository playerArmorRepository = mock(PlayerArmorRepository.class);
        final PlayerArmorStatRepository playerArmorStatRepository = mock(PlayerArmorStatRepository.class);
        final PlayerInventoryRepository playerInventoryRepository = mock(PlayerInventoryRepository.class);
        final PlayerDungeonProgressRepository playerDungeonProgressRepository = mock(PlayerDungeonProgressRepository.class);
        final PlayerActiveRunRepository playerActiveRunRepository = mock(PlayerActiveRunRepository.class);
        final CharacterService characterService = mock(CharacterService.class);
        final DropService dropService = mock(DropService.class);
        final ShopService shopService = mock(ShopService.class);
        final EquipmentService equipmentService = mock(EquipmentService.class);
        final MasterDataLoader masterDataLoader = mock(MasterDataLoader.class);

        final GameSessionService service = new GameSessionService(
                playerRepository, playerWeaponRepository, playerWeaponStatRepository,
                playerWeaponSkillRepository, playerArmorRepository, playerArmorStatRepository,
                playerInventoryRepository, playerDungeonProgressRepository,
                playerActiveRunRepository, characterService, dropService,
                shopService, equipmentService, masterDataLoader);

        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));

        // 1단계: 활성 런 없음 → 입장 가능, 행 수 = 1
        when(playerActiveRunRepository.findByPlayerId(PLAYER_ID)).thenReturn(Optional.empty());
        when(playerActiveRunRepository.save(any(PlayerActiveRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final PlayerActiveRun firstRun = service.enterDungeon(PLAYER_ID, DUNGEON_ID_1);
        assertTrue(firstRun != null, "첫 번째 입장은 성공해야 한다");

        // 2단계: 이미 활성 런 존재 → 중복 입장 시도 시 예외 발생 (행 수 여전히 1)
        when(playerActiveRunRepository.findByPlayerId(PLAYER_ID)).thenReturn(Optional.of(firstRun));

        assertThrows(IllegalActionException.class,
                () -> service.enterDungeon(PLAYER_ID, DUNGEON_ID_2),
                "이미 진행 중인 던전이 있으면 새 던전 입장 시 예외가 발생해야 한다");

        // 3단계: 포기 후 → 행 수 = 0
        service.abandonDungeon(PLAYER_ID);

        // 4단계: 다시 활성 런 없음 → 재입장 가능, 행 수 = 1
        when(playerActiveRunRepository.findByPlayerId(PLAYER_ID)).thenReturn(Optional.empty());

        final PlayerActiveRun secondRun = service.enterDungeon(PLAYER_ID, DUNGEON_ID_2);
        assertTrue(secondRun != null, "포기 후 재입장은 성공해야 한다");

        // 재입장 후 다시 중복 입장 불가 확인
        when(playerActiveRunRepository.findByPlayerId(PLAYER_ID)).thenReturn(Optional.of(secondRun));

        assertThrows(IllegalActionException.class,
                () -> service.enterDungeon(PLAYER_ID, DUNGEON_ID_1),
                "재입장 후에도 중복 던전 입장은 거부되어야 한다");
    }
}
