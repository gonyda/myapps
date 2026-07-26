package com.myapps.web.myrpg.application.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import com.myapps.web.myrpg.domain.model.DamageType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerActiveRun;
import com.myapps.web.myrpg.domain.model.PlayerDungeonProgress;
import com.myapps.web.myrpg.domain.model.vo.DamageResult;
import com.myapps.web.myrpg.domain.model.vo.EffectiveStats;
import com.myapps.web.myrpg.domain.model.vo.TurnOrder;
import com.myapps.web.myrpg.domain.random.FixedRandomSource;
import com.myapps.web.myrpg.domain.repository.PlayerActiveRunRepository;
import com.myapps.web.myrpg.domain.repository.PlayerDungeonProgressRepository;
import com.myapps.web.myrpg.domain.repository.PlayerRepository;
import com.myapps.web.myrpg.domain.service.BattleService;
import com.myapps.web.myrpg.domain.service.CharacterService;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// Feature: myrpg-gen1-mvp, Property 18: 전투 종료 보장
/**
 * 전투 종료 보장 속성 기반 테스트 및 체크포인트·포기·보스 클리어 상태전이 통합 테스트.
 *
 * <p>Property 18은 유한 HP를 갖는 두 엔티티 간 전투 루프가 반드시 종료됨을 검증한다.
 * 통합 테스트는 {@code @DataJpaTest}로 실제 DB와의 상호작용을 검증한다.
 *
 * <p><b>Validates: Requirements 8.4, 21.1, 21.4, 21.6, 22.2, 22.3, 22.5</b>
 */
class BattleTerminationAndStateTransitionTest {

    private static final int PLAYER_HP_MIN = 1;
    private static final int PLAYER_HP_MAX = 500;
    private static final int MONSTER_HP_MIN = 1;
    private static final int MONSTER_HP_MAX = 300;
    private static final int ATTACK_MIN = 1;
    private static final int ATTACK_MAX = 100;
    private static final int DEFENSE_MIN = 0;
    private static final int DEFENSE_MAX = 50;
    private static final int SPEED_MIN = 1;
    private static final int SPEED_MAX = 30;
    private static final int CRITICAL_MIN = 0;
    private static final int CRITICAL_MAX = 50;
    private static final int MAX_TURNS = 10000;

    // --- Providers ---

    /**
     * 임의의 플레이어 HP를 생성하는 Provider.
     *
     * @return playerHp Arbitrary
     */
    @Provide
    Arbitrary<Integer> playerHps() {
        return Arbitraries.integers().between(PLAYER_HP_MIN, PLAYER_HP_MAX);
    }

    /**
     * 임의의 몬스터 HP를 생성하는 Provider.
     *
     * @return monsterHp Arbitrary
     */
    @Provide
    Arbitrary<Integer> monsterHps() {
        return Arbitraries.integers().between(MONSTER_HP_MIN, MONSTER_HP_MAX);
    }

    /**
     * 임의의 공격력을 생성하는 Provider.
     *
     * @return attack Arbitrary
     */
    @Provide
    Arbitrary<Integer> attacks() {
        return Arbitraries.integers().between(ATTACK_MIN, ATTACK_MAX);
    }

    /**
     * 임의의 방어력을 생성하는 Provider.
     *
     * @return defense Arbitrary
     */
    @Provide
    Arbitrary<Integer> defenses() {
        return Arbitraries.integers().between(DEFENSE_MIN, DEFENSE_MAX);
    }

    /**
     * 임의의 속도를 생성하는 Provider.
     *
     * @return speed Arbitrary
     */
    @Provide
    Arbitrary<Integer> speeds() {
        return Arbitraries.integers().between(SPEED_MIN, SPEED_MAX);
    }

    /**
     * 임의의 치명타를 생성하는 Provider.
     *
     * @return critical Arbitrary
     */
    @Provide
    Arbitrary<Integer> criticals() {
        return Arbitraries.integers().between(CRITICAL_MIN, CRITICAL_MAX);
    }

    /**
     * [0.0, 1.0) 범위의 난수 값 Provider.
     *
     * @return roll Arbitrary
     */
    @Provide
    Arbitrary<Double> randomRolls() {
        return Arbitraries.doubles().between(0.0, 0.99).ofScale(2);
    }

    // Feature: myrpg-gen1-mvp, Property 18: 전투 종료 보장
    /**
     * 유한한 양의 HP를 갖는 플레이어와 몬스터 간 전투 루프는
     * 반드시 유한 턴 내에 종료되며, 종료 시 적어도 한쪽의 HP가 0 이하이다.
     *
     * <p>최소 데미지 1이 보장되므로, 최악의 경우에도
     * playerHp + monsterHp 턴 이내에 전투가 종료된다.
     *
     * <p><b>Validates: Requirements 8.4</b>
     *
     * @param playerHp      플레이어 시작 HP (1~500)
     * @param monsterHp     몬스터 시작 HP (1~300)
     * @param playerAttack  플레이어 공격력 (1~100)
     * @param monsterAttack 몬스터 공격력 (1~100)
     * @param playerDefense 플레이어 방어력 (0~50)
     * @param monsterDefense 몬스터 방어력 (0~50)
     * @param playerSpeed   플레이어 속도 (1~30)
     * @param monsterSpeed  몬스터 속도 (1~30)
     * @param playerCritical 플레이어 치명타 (0~50)
     * @param randomRoll    난수 값 (0.0~0.999)
     */
    @Property(tries = 100)
    void battleAlwaysTerminatesInFiniteTurns(
            @ForAll("playerHps") final int playerHp,
            @ForAll("monsterHps") final int monsterHp,
            @ForAll("attacks") final int playerAttack,
            @ForAll("attacks") final int monsterAttack,
            @ForAll("defenses") final int playerDefense,
            @ForAll("defenses") final int monsterDefense,
            @ForAll("speeds") final int playerSpeed,
            @ForAll("speeds") final int monsterSpeed,
            @ForAll("criticals") final int playerCritical,
            @ForAll("randomRolls") final double randomRoll) {

        final FixedRandomSource randomSource = new FixedRandomSource(randomRoll);
        final BattleService battleService = new BattleService(randomSource);

        final EffectiveStats playerStats = new EffectiveStats(
                playerAttack, playerDefense, playerSpeed, playerCritical,
                playerHp, DamageType.PHYSICAL);

        int currentPlayerHp = playerHp;
        int currentMonsterHp = monsterHp;
        int turnCount = 0;

        final TurnOrder turnOrder = battleService.decideTurnOrder(playerSpeed, monsterSpeed);
        boolean playerTurn = (turnOrder == TurnOrder.PLAYER_FIRST);

        while (currentPlayerHp > 0 && currentMonsterHp > 0 && turnCount < MAX_TURNS) {
            if (playerTurn) {
                final DamageResult playerDamage = battleService.computeDamage(
                        playerAttack, 1.0, DamageType.PHYSICAL, monsterDefense, playerStats);
                currentMonsterHp -= playerDamage.damage();
            } else {
                final DamageResult monsterDmg = battleService.monsterDamage(
                        monsterAttack, DamageType.PHYSICAL, playerDefense);
                currentPlayerHp -= monsterDmg.damage();
            }
            playerTurn = !playerTurn;
            turnCount++;
        }

        assertThat(turnCount).as("전투는 유한 턴 내에 종료되어야 한다")
                .isLessThan(MAX_TURNS);
        assertThat(currentPlayerHp <= 0 || currentMonsterHp <= 0)
                .as("종료 시 적어도 한쪽의 HP가 0 이하여야 한다")
                .isTrue();

        final int maxPossibleTurns = playerHp + monsterHp;
        assertThat(turnCount).as("최악의 경우에도 playerHp+monsterHp 턴 이내에 종료")
                .isLessThanOrEqualTo(maxPossibleTurns);
    }

    // ─────────────────────────────────────────────────────────────
    // 통합 테스트 (Inner class with @DataJpaTest)
    // ─────────────────────────────────────────────────────────────

    /**
     * 체크포인트 저장/재개, 던전 포기, 보스 클리어 자동 복귀의 상태전이를
     * 실제 DB와 함께 검증하는 통합 테스트.
     *
     * <p><b>Validates: Requirements 21.1, 21.4, 21.6, 22.2, 22.3, 22.5</b>
     */
    @DataJpaTest
    @Import(CharacterService.class)
    @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
    static class StateTransitionIntegrationTest {

        private final TestEntityManager em;
        private final PlayerRepository playerRepository;
        private final PlayerActiveRunRepository playerActiveRunRepository;
        private final PlayerDungeonProgressRepository playerDungeonProgressRepository;
        private final CharacterService characterService;

        StateTransitionIntegrationTest(final TestEntityManager em,
                                       final PlayerRepository playerRepository,
                                       final PlayerActiveRunRepository playerActiveRunRepository,
                                       final PlayerDungeonProgressRepository playerDungeonProgressRepository,
                                       final CharacterService characterService) {
            this.em = em;
            this.playerRepository = playerRepository;
            this.playerActiveRunRepository = playerActiveRunRepository;
            this.playerDungeonProgressRepository = playerDungeonProgressRepository;
            this.characterService = characterService;
        }

        /**
         * GameSessionService를 실제 리포지터리와 CharacterService로 조립하는 헬퍼.
         *
         * @return 실제 DB 연동 GameSessionService
         */
        private GameSessionService createService() {
            return new GameSessionService(
                    playerRepository,
                    mock(com.myapps.web.myrpg.domain.repository.PlayerWeaponRepository.class),
                    mock(com.myapps.web.myrpg.domain.repository.PlayerWeaponStatRepository.class),
                    mock(com.myapps.web.myrpg.domain.repository.PlayerWeaponSkillRepository.class),
                    mock(com.myapps.web.myrpg.domain.repository.PlayerArmorRepository.class),
                    mock(com.myapps.web.myrpg.domain.repository.PlayerArmorStatRepository.class),
                    mock(com.myapps.web.myrpg.domain.repository.PlayerInventoryRepository.class),
                    playerDungeonProgressRepository,
                    playerActiveRunRepository,
                    characterService,
                    mock(com.myapps.web.myrpg.domain.service.DropService.class),
                    mock(com.myapps.web.myrpg.domain.service.ShopService.class),
                    mock(com.myapps.web.myrpg.domain.service.EquipmentService.class),
                    mock(MasterDataLoader.class));
        }

        @Test
        @DisplayName("Req 21.1, 21.4 - 체크포인트 저장 후 재개 시 HP/MP가 복원된다")
        void should_restoreHpMp_when_resumingFromCheckpoint() {
            final Player player = em.persist(
                    new Player("용사", 5, 100, 200, 200, 80, 80, 20, 10, 8, 3, 100));
            em.flush();
            em.clear();

            final GameSessionService service = createService();
            final Long playerId = player.getId();

            // 던전 진입
            service.enterDungeon(playerId, 1L);
            em.flush();
            em.clear();

            // 스테이지 2 클리어 후 체크포인트 저장 (HP 150, MP 60)
            service.saveCheckpoint(playerId, 2, 150, 60);
            em.flush();
            em.clear();

            // 체크포인트 데이터 검증 (Req 21.1)
            final Optional<PlayerActiveRun> savedRun = playerActiveRunRepository.findByPlayerId(playerId);
            assertThat(savedRun).isPresent();
            assertThat(savedRun.get().getClearedStage()).isEqualTo(2);
            assertThat(savedRun.get().getCheckpointHp()).isEqualTo(150);
            assertThat(savedRun.get().getCheckpointMp()).isEqualTo(60);
            assertThat(savedRun.get().getDungeonId()).isEqualTo(1L);

            // 재개 시 HP/MP 복원 검증 (Req 21.4)
            final Optional<PlayerActiveRun> resumedRun = service.resumeDungeon(playerId);
            assertThat(resumedRun).isPresent();

            final Player restoredPlayer = playerRepository.findById(playerId).orElseThrow();
            assertThat(restoredPlayer.getHp()).isEqualTo(150);
            assertThat(restoredPlayer.getMp()).isEqualTo(60);
        }

        @Test
        @DisplayName("Req 22.2, 22.3 - 던전 포기 시 경험치 페널티 없이 진행상태가 삭제된다")
        void should_deleteProgressWithoutExpPenalty_when_abandoningDungeon() {
            final Player player = em.persist(
                    new Player("전사", 3, 50, 150, 150, 60, 60, 15, 8, 6, 2, 200));
            em.flush();
            em.clear();

            final GameSessionService service = createService();
            final Long playerId = player.getId();

            // 던전 진입 후 체크포인트 저장
            service.enterDungeon(playerId, 2L);
            service.saveCheckpoint(playerId, 3, 100, 40);
            em.flush();
            em.clear();

            // 포기 전 상태 확인
            final Player beforeAbandon = playerRepository.findById(playerId).orElseThrow();
            final int expBeforeAbandon = beforeAbandon.getExp();

            // 던전 포기
            service.abandonDungeon(playerId);
            em.flush();
            em.clear();

            // Req 22.2: 경험치 페널티 없음
            final Player afterAbandon = playerRepository.findById(playerId).orElseThrow();
            assertThat(afterAbandon.getExp()).isEqualTo(expBeforeAbandon);

            // Req 22.3: 진행상태 삭제 (다음 진입 시 1스테이지부터)
            final Optional<PlayerActiveRun> deletedRun = playerActiveRunRepository.findByPlayerId(playerId);
            assertThat(deletedRun).isEmpty();

            // HP/MP 완전 회복 (마을 복귀)
            assertThat(afterAbandon.getHp()).isEqualTo(afterAbandon.getMaxHp());
            assertThat(afterAbandon.getMp()).isEqualTo(afterAbandon.getMaxMp());
        }

        @Test
        @DisplayName("Req 21.6, 22.5 - 보스(5스테이지) 클리어 시 이력 갱신, 진행삭제, 마을 복귀")
        void should_updateProgressAndRestoreToTown_when_bossCleared() {
            final Player player = em.persist(
                    new Player("마법사", 7, 200, 250, 250, 100, 100, 25, 12, 10, 5, 500));
            em.flush();
            em.clear();

            final GameSessionService service = createService();
            final Long playerId = player.getId();

            // 던전 진입
            service.enterDungeon(playerId, 1L);
            service.saveCheckpoint(playerId, 4, 180, 70);
            em.flush();
            em.clear();

            // 보스 클리어 (completeDungeon)
            service.completeDungeon(playerId, 1L);
            em.flush();
            em.clear();

            // Req 21.6: 던전 클리어 이력 갱신
            final Optional<PlayerDungeonProgress> progress =
                    playerDungeonProgressRepository.findByPlayerIdAndDungeonId(playerId, 1L);
            assertThat(progress).isPresent();
            assertThat(progress.get().isCleared()).isTrue();
            assertThat(progress.get().getBestStage()).isEqualTo(5);

            // Req 22.5: 활성 런 삭제
            final Optional<PlayerActiveRun> deletedRun = playerActiveRunRepository.findByPlayerId(playerId);
            assertThat(deletedRun).isEmpty();

            // Req 22.5: 마을 복귀 (HP/MP 완전 회복)
            final Player restoredPlayer = playerRepository.findById(playerId).orElseThrow();
            assertThat(restoredPlayer.getHp()).isEqualTo(restoredPlayer.getMaxHp());
            assertThat(restoredPlayer.getMp()).isEqualTo(restoredPlayer.getMaxMp());
        }

        @Test
        @DisplayName("Req 22.5 - 보스 클리어 후 포기 기능 미제공 (활성 런 부재)")
        void should_notAllowAbandon_after_bossCleared() {
            final Player player = em.persist(
                    new Player("기사", 8, 300, 300, 300, 120, 120, 30, 15, 12, 7, 800));
            em.flush();
            em.clear();

            final GameSessionService service = createService();
            final Long playerId = player.getId();

            // 던전 진입 후 보스 클리어
            service.enterDungeon(playerId, 1L);
            em.flush();
            service.completeDungeon(playerId, 1L);
            em.flush();
            em.clear();

            // 보스 클리어 후 포기 시도 → 활성 런 없어 예외 발생
            org.junit.jupiter.api.Assertions.assertThrows(
                    com.myapps.web.myrpg.domain.exception.IllegalActionException.class,
                    () -> service.abandonDungeon(playerId));
        }
    }
}
