package com.myapps.web.myrpg.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestConstructor;

import com.myapps.web.myrpg.domain.model.ArmorSlot;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerActiveRun;
import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerDungeonProgress;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;
import com.myapps.web.myrpg.domain.model.WeaponType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 플레이어 관련 리포지터리 슬라이스 테스트.
 *
 * <p>{@code @DataJpaTest}를 통해 JPA 계층만 로드하여 리포지터리 쿼리 메서드를 검증한다.
 */
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PlayerRepositoryTest {

    private final TestEntityManager em;
    private final PlayerRepository playerRepository;
    private final PlayerWeaponRepository playerWeaponRepository;
    private final PlayerArmorRepository playerArmorRepository;
    private final PlayerInventoryRepository playerInventoryRepository;
    private final PlayerDungeonProgressRepository playerDungeonProgressRepository;
    private final PlayerActiveRunRepository playerActiveRunRepository;

    PlayerRepositoryTest(final TestEntityManager em,
                         final PlayerRepository playerRepository,
                         final PlayerWeaponRepository playerWeaponRepository,
                         final PlayerArmorRepository playerArmorRepository,
                         final PlayerInventoryRepository playerInventoryRepository,
                         final PlayerDungeonProgressRepository playerDungeonProgressRepository,
                         final PlayerActiveRunRepository playerActiveRunRepository) {
        this.em = em;
        this.playerRepository = playerRepository;
        this.playerWeaponRepository = playerWeaponRepository;
        this.playerArmorRepository = playerArmorRepository;
        this.playerInventoryRepository = playerInventoryRepository;
        this.playerDungeonProgressRepository = playerDungeonProgressRepository;
        this.playerActiveRunRepository = playerActiveRunRepository;
    }

    @Test
    @DisplayName("PlayerRepository - 플레이어를 저장하고 조회한다")
    void should_saveAndFindPlayer_when_validPlayerGiven() {
        final Player player = new Player("용사", 1, 0, 100, 100, 50, 50, 10, 5, 8, 3, 0);
        final Player saved = playerRepository.save(player);

        em.flush();
        em.clear();

        final Optional<Player> found = playerRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("용사");
        assertThat(found.get().getLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("PlayerWeaponRepository - 플레이어 ID로 무기 목록을 조회한다")
    void should_findWeaponsByPlayerId_when_weaponsExist() {
        final Player player = em.persist(new Player("전사", 5, 0, 200, 200, 80, 80, 20, 10, 10, 5, 100));
        em.persist(new PlayerWeapon(player.getId(), 1L, "[일반] 단검", WeaponType.DAGGER,
                Grade.COMMON, 5, 15, 3, 2, 1, true));
        em.persist(new PlayerWeapon(player.getId(), 2L, "[희귀] 장검", WeaponType.SWORD,
                Grade.RARE, 5, 25, 0, 5, 2, false));
        em.flush();
        em.clear();

        final List<PlayerWeapon> weapons = playerWeaponRepository.findByPlayerId(player.getId());

        assertThat(weapons).hasSize(2);
    }

    @Test
    @DisplayName("PlayerArmorRepository - 플레이어 ID와 부위로 방어구를 조회한다")
    void should_findArmorByPlayerIdAndSlot_when_armorsExist() {
        final Player player = em.persist(new Player("기사", 10, 0, 300, 300, 100, 100, 30, 20, 8, 5, 500));
        em.persist(new PlayerArmor(player.getId(), 1L, "[일반] 가죽 투구", ArmorSlot.HELMET,
                Grade.COMMON, 3, 5, false));
        em.persist(new PlayerArmor(player.getId(), 2L, "[일반] 가죽 갑옷", ArmorSlot.CHEST,
                Grade.COMMON, 5, 5, true));
        em.flush();
        em.clear();

        final List<PlayerArmor> headArmors = playerArmorRepository.findByPlayerIdAndArmorSlot(
                player.getId(), ArmorSlot.HELMET);
        final List<PlayerArmor> bodyArmors = playerArmorRepository.findByPlayerIdAndArmorSlot(
                player.getId(), ArmorSlot.CHEST);

        assertThat(headArmors).hasSize(1);
        assertThat(bodyArmors).hasSize(1);
        assertThat(headArmors.get(0).getDisplayName()).isEqualTo("[일반] 가죽 투구");
    }

    @Test
    @DisplayName("PlayerInventoryRepository - 플레이어·아이템타입·참조ID로 인벤토리를 조회한다")
    void should_findInventoryByPlayerIdAndItemTypeAndRefId_when_itemExists() {
        final Player player = em.persist(new Player("마법사", 3, 0, 80, 80, 150, 150, 8, 3, 10, 5, 200));
        em.persist(new PlayerInventory(player.getId(), ItemType.POTION, 1L, 5));
        em.persist(new PlayerInventory(player.getId(), ItemType.SKILL_BOOK, 2L, 1));
        em.flush();
        em.clear();

        final Optional<PlayerInventory> potion = playerInventoryRepository
                .findByPlayerIdAndItemTypeAndItemRefId(player.getId(), ItemType.POTION, 1L);
        final Optional<PlayerInventory> notFound = playerInventoryRepository
                .findByPlayerIdAndItemTypeAndItemRefId(player.getId(), ItemType.POTION, 99L);

        assertThat(potion).isPresent();
        assertThat(potion.get().getQuantity()).isEqualTo(5);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("PlayerDungeonProgressRepository - 플레이어·던전 ID로 진행 이력을 조회한다")
    void should_findProgressByPlayerIdAndDungeonId_when_progressExists() {
        final Player player = em.persist(new Player("모험가", 8, 0, 250, 250, 100, 100, 25, 15, 10, 7, 300));
        em.persist(new PlayerDungeonProgress(player.getId(), 1L, true, 5));
        em.persist(new PlayerDungeonProgress(player.getId(), 2L, false, 3));
        em.flush();
        em.clear();

        final Optional<PlayerDungeonProgress> progress = playerDungeonProgressRepository
                .findByPlayerIdAndDungeonId(player.getId(), 1L);
        final List<PlayerDungeonProgress> allProgress = playerDungeonProgressRepository
                .findByPlayerId(player.getId());

        assertThat(progress).isPresent();
        assertThat(progress.get().isCleared()).isTrue();
        assertThat(allProgress).hasSize(2);
    }

    @Test
    @DisplayName("PlayerActiveRunRepository - 플레이어 ID로 활성 런을 조회한다")
    void should_findActiveRunByPlayerId_when_runExists() {
        final Player player = em.persist(new Player("탐험가", 12, 0, 400, 400, 120, 120, 35, 20, 12, 8, 1000));
        final LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 30, 0);
        em.persist(new PlayerActiveRun(player.getId(), 3L, 2, 350, 100, now));
        em.flush();
        em.clear();

        final Optional<PlayerActiveRun> activeRun = playerActiveRunRepository
                .findByPlayerId(player.getId());

        assertThat(activeRun).isPresent();
        assertThat(activeRun.get().getDungeonId()).isEqualTo(3L);
        assertThat(activeRun.get().getClearedStage()).isEqualTo(2);
    }

    @Test
    @DisplayName("PlayerActiveRunRepository - 플레이어 ID로 활성 런을 삭제한다")
    void should_deleteActiveRunByPlayerId_when_runExists() {
        final Player player = em.persist(new Player("전사", 15, 0, 500, 500, 150, 150, 40, 25, 15, 10, 2000));
        final LocalDateTime now = LocalDateTime.of(2026, 2, 20, 14, 0, 0);
        em.persist(new PlayerActiveRun(player.getId(), 2L, 1, 450, 130, now));
        em.flush();
        em.clear();

        playerActiveRunRepository.deleteByPlayerId(player.getId());
        em.flush();
        em.clear();

        final Optional<PlayerActiveRun> deleted = playerActiveRunRepository
                .findByPlayerId(player.getId());

        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("PlayerActiveRunRepository - 동일 플레이어 ID로 두 번째 활성 런 저장 시 제약 위반 예외가 발생한다")
    void should_throwException_when_duplicatePlayerIdActiveRun() {
        final Player player = em.persist(new Player("중복테스트", 10, 0, 300, 300, 100, 100, 25, 15, 10, 5, 500));
        final LocalDateTime now = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

        em.persist(new PlayerActiveRun(player.getId(), 1L, 1, 280, 90, now));
        em.flush();

        assertThrows(PersistenceException.class, () -> {
            em.persist(new PlayerActiveRun(player.getId(), 2L, 0, 300, 100, now));
            em.flush();
        });
    }
}
