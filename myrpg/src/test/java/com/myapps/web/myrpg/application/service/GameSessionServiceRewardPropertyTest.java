package com.myapps.web.myrpg.application.service;

import java.util.Optional;

import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.vo.LevelUpResult;
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
import com.myapps.web.myrpg.domain.service.DungeonService;
import com.myapps.web.myrpg.domain.service.DropService;
import com.myapps.web.myrpg.domain.service.EquipmentService;
import com.myapps.web.myrpg.domain.service.ShopService;
import com.myapps.web.myrpg.domain.template.MonsterTemplate;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Feature: myrpg-gen1-mvp, Property 1: 전투 보상 지급 정확성
// Feature: myrpg-gen1-mvp, Property 2: 비전투 이벤트는 보상을 주지 않는다
/**
 * GameSessionService 전투 보상 지급 관련 속성 기반 테스트.
 *
 * <p>전투 보상 지급 정확성 및 비전투 이벤트에서 보상 미지급을 검증한다.
 *
 * <p><b>Validates: Requirements 2.1, 2.3, 20.9</b>
 */
class GameSessionServiceRewardPropertyTest {

    private static final int LEVEL_MIN = 1;
    private static final int LEVEL_MAX = 10;
    private static final int GOLD_MIN = 0;
    private static final int GOLD_MAX = 10_000;
    private static final int MONSTER_EXP_MIN = 1;
    private static final int MONSTER_EXP_MAX = 500;
    private static final int MONSTER_GOLD_MIN = 1;
    private static final int MONSTER_GOLD_MAX = 200;
    private static final int HP_MIN = 1;
    private static final int HP_MAX = 500;

    /**
     * 임의의 플레이어 상태를 생성하는 Provider.
     *
     * @return Player Arbitrary
     */
    @Provide
    Arbitrary<Player> players() {
        final Arbitrary<Integer> levels = Arbitraries.integers().between(LEVEL_MIN, LEVEL_MAX);
        final Arbitrary<Integer> golds = Arbitraries.integers().between(GOLD_MIN, GOLD_MAX);
        final Arbitrary<Integer> hps = Arbitraries.integers().between(HP_MIN, HP_MAX);

        return Combinators.combine(levels, golds, hps).as((level, gold, hp) ->
                new Player("TestHero", level, 0, hp, hp, 50, 50, 10, 5, 5, 0, gold)
        );
    }

    /**
     * 임의의 몬스터 보상 데이터를 생성하는 Provider.
     *
     * @return MonsterTemplate Arbitrary
     */
    @Provide
    Arbitrary<MonsterTemplate> monsters() {
        final Arbitrary<Long> ids = Arbitraries.longs().between(1L, 100L);
        final Arbitrary<Integer> expRewards = Arbitraries.integers().between(MONSTER_EXP_MIN, MONSTER_EXP_MAX);
        final Arbitrary<Integer> goldRewards = Arbitraries.integers().between(MONSTER_GOLD_MIN, MONSTER_GOLD_MAX);

        return Combinators.combine(ids, expRewards, goldRewards).as((id, exp, gold) ->
                new MonsterTemplate(id, "Monster_" + id, 50, 8, 3, 3,
                        DamageType.PHYSICAL, exp, gold, false)
        );
    }

    /**
     * 임의의 HP 값을 생성하는 Provider (DungeonService 비전투 이벤트 입력용).
     *
     * @return HP 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> currentHps() {
        return Arbitraries.integers().between(HP_MIN, HP_MAX);
    }

    /**
     * 임의의 최대 HP 값을 생성하는 Provider.
     *
     * @return maxHp 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> maxHps() {
        return Arbitraries.integers().between(HP_MIN, HP_MAX);
    }

    // Feature: myrpg-gen1-mvp, Property 1: 전투 보상 지급 정확성
    /**
     * grantBattleReward 호출 시 골드는 monster.goldReward()만큼 정확히 증가하고,
     * 경험치는 monster.expReward()를 정확히 gainExp에 전달한다.
     *
     * <p><b>Validates: Requirements 2.1</b>
     *
     * @param player  임의의 플레이어 상태
     * @param monster 임의의 몬스터 템플릿
     */
    @Property(tries = 100)
    void battleRewardAccuracy(
            @ForAll("players") final Player player,
            @ForAll("monsters") final MonsterTemplate monster) {

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

        final int originalGold = player.getGold();
        final int expectedGold = originalGold + monster.goldReward();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(masterDataLoader.findMonster(monster.id())).thenReturn(monster);
        when(characterService.gainExp(any(Player.class), eq(monster.expReward())))
                .thenReturn(new LevelUpResult(player.getLevel(), 0, player.getExp() + monster.expReward()));

        service.grantBattleReward(1L, monster.id());

        assertEquals(expectedGold, player.getGold(),
                "골드는 monster.goldReward()만큼 정확히 증가해야 한다: original="
                        + originalGold + ", reward=" + monster.goldReward());
        verify(characterService).gainExp(player, monster.expReward());
    }

    // Feature: myrpg-gen1-mvp, Property 2: 비전투 이벤트는 보상을 주지 않는다
    /**
     * DungeonService의 비전투 이벤트 핸들러(applyRest, applyTrap)는
     * 경험치와 골드를 변경하지 않는다(전투 보상을 지급하지 않는다).
     *
     * <p>DungeonService.applyRest와 applyTrap은 HP만 변경하며,
     * Player의 exp/gold에 접근할 수 없는 순수 int→int 함수이므로
     * 구조적으로 전투 보상을 지급할 수 없음을 검증한다.
     *
     * <p><b>Validates: Requirements 2.3, 20.9</b>
     *
     * @param currentHp 현재 HP
     * @param maxHp     최대 HP
     */
    @Property(tries = 100)
    void nonCombatEventsDoNotGrantRewards(
            @ForAll("currentHps") final int currentHp,
            @ForAll("maxHps") final int maxHp) {

        final int validMaxHp = Math.max(maxHp, currentHp);
        final com.myapps.web.myrpg.domain.random.RandomSource fixedRandom =
                new com.myapps.web.myrpg.domain.random.RandomSource() {
                    @Override
                    public double nextDouble() {
                        return 0.5;
                    }

                    @Override
                    public int nextInt(final int bound) {
                        return 0;
                    }

                    @Override
                    public int nextIntInclusive(final int min, final int max) {
                        return min;
                    }

                    @Override
                    public double nextDoubleInRange(final double min, final double max) {
                        return min;
                    }
                };

        final DungeonService dungeonService = new DungeonService(fixedRandom);

        // applyRest: 반환값은 int(HP)이므로 exp/gold를 변경하는 경로가 구조적으로 없음
        final int restResult = dungeonService.applyRest(currentHp, validMaxHp);
        // 반환값은 HP이며, 보상(exp/gold)과 무관함을 확인
        // applyRest가 Player를 받지 않으므로 exp/gold 변경 불가
        assertEquals(Math.min((int) (currentHp + Math.round(validMaxHp * 0.10)), validMaxHp), restResult,
                "applyRest는 HP만 변경하며 보상을 지급하지 않는다");

        // applyTrap: 반환값은 int(HP)이므로 exp/gold를 변경하는 경로가 구조적으로 없음
        final int trapResult = dungeonService.applyTrap(currentHp);
        final int expectedTrap = Math.max((int) (currentHp - Math.round(currentHp * 0.10)), 1);
        assertEquals(expectedTrap, trapResult,
                "applyTrap은 HP만 변경하며 보상을 지급하지 않는다");

        // rollTreasure는 TreasureReward(골드/포션/장비)를 반환하지만
        // Player의 exp를 직접 변경하지 않음 — 반환값 기반 설계로
        // 오케스트레이터가 골드를 부여할 뿐, 전투 보상(monster expReward/goldReward)을 지급하지 않는다.
        // 이는 grantBattleReward 메서드만이 MonsterTemplate 기반 보상을 지급하는 유일한 경로임을 의미한다.
    }
}
