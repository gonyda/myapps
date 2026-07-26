package com.myapps.web.myrpg.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myapps.web.myrpg.application.exception.PlayerNotFoundException;
import com.myapps.web.myrpg.domain.exception.IllegalActionException;
import com.myapps.web.myrpg.domain.model.Grade;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.Player;
import com.myapps.web.myrpg.domain.model.PlayerActiveRun;
import com.myapps.web.myrpg.domain.model.PlayerArmor;
import com.myapps.web.myrpg.domain.model.PlayerArmorStat;
import com.myapps.web.myrpg.domain.model.PlayerDungeonProgress;
import com.myapps.web.myrpg.domain.model.PlayerInventory;
import com.myapps.web.myrpg.domain.model.PlayerWeapon;
import com.myapps.web.myrpg.domain.model.PlayerWeaponSkill;
import com.myapps.web.myrpg.domain.model.PlayerWeaponStat;
import com.myapps.web.myrpg.domain.model.vo.LevelUpResult;
import com.myapps.web.myrpg.domain.model.vo.RolledArmor;
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
import com.myapps.web.myrpg.domain.template.ArmorTemplate;
import com.myapps.web.myrpg.domain.template.MonsterTemplate;
import com.myapps.web.myrpg.domain.template.SkillTemplate;
import com.myapps.web.myrpg.domain.template.WeaponTemplate;

/**
 * 게임 세션 오케스트레이션 서비스.
 *
 * <p>캐릭터 생성, 전투 보상, 사망/도망 페널티, 던전 진입/체크포인트/재개/완료/포기,
 * 드랍 저장, 장비 착용, 상점 연동 등 유스케이스를 조립한다.
 */
@Service
@Transactional
public class GameSessionService {

    private static final long STARTER_WEAPON_TEMPLATE_ID = 1L;
    private static final int STARTER_WEAPON_ITEM_LEVEL = 1;
    private static final int BOSS_STAGE = 5;
    private static final double DEATH_PENALTY_RATIO = 0.10;
    private static final double FLEE_PENALTY_RATIO = 0.05;

    private final PlayerRepository playerRepository;
    private final PlayerWeaponRepository playerWeaponRepository;
    private final PlayerWeaponStatRepository playerWeaponStatRepository;
    private final PlayerWeaponSkillRepository playerWeaponSkillRepository;
    private final PlayerArmorRepository playerArmorRepository;
    private final PlayerArmorStatRepository playerArmorStatRepository;
    private final PlayerInventoryRepository playerInventoryRepository;
    private final PlayerDungeonProgressRepository playerDungeonProgressRepository;
    private final PlayerActiveRunRepository playerActiveRunRepository;
    private final CharacterService characterService;
    private final DropService dropService;
    private final ShopService shopService;
    private final EquipmentService equipmentService;
    private final MasterDataLoader masterDataLoader;

    /**
     * GameSessionService를 생성한다.
     *
     * @param playerRepository              플레이어 리포지터리
     * @param playerWeaponRepository        무기 리포지터리
     * @param playerWeaponStatRepository    무기 능력치 리포지터리
     * @param playerWeaponSkillRepository   무기 스킬 리포지터리
     * @param playerArmorRepository         방어구 리포지터리
     * @param playerArmorStatRepository     방어구 능력치 리포지터리
     * @param playerInventoryRepository     인벤토리 리포지터리
     * @param playerDungeonProgressRepository 던전 진행 리포지터리
     * @param playerActiveRunRepository     활성 런 리포지터리
     * @param characterService              캐릭터 도메인 서비스
     * @param dropService                   드랍 도메인 서비스
     * @param shopService                   상점 도메인 서비스
     * @param equipmentService              장비 도메인 서비스
     * @param masterDataLoader              마스터 데이터 로더
     */
    public GameSessionService(final PlayerRepository playerRepository,
                              final PlayerWeaponRepository playerWeaponRepository,
                              final PlayerWeaponStatRepository playerWeaponStatRepository,
                              final PlayerWeaponSkillRepository playerWeaponSkillRepository,
                              final PlayerArmorRepository playerArmorRepository,
                              final PlayerArmorStatRepository playerArmorStatRepository,
                              final PlayerInventoryRepository playerInventoryRepository,
                              final PlayerDungeonProgressRepository playerDungeonProgressRepository,
                              final PlayerActiveRunRepository playerActiveRunRepository,
                              final CharacterService characterService,
                              final DropService dropService,
                              final ShopService shopService,
                              final EquipmentService equipmentService,
                              final MasterDataLoader masterDataLoader) {
        this.playerRepository = playerRepository;
        this.playerWeaponRepository = playerWeaponRepository;
        this.playerWeaponStatRepository = playerWeaponStatRepository;
        this.playerWeaponSkillRepository = playerWeaponSkillRepository;
        this.playerArmorRepository = playerArmorRepository;
        this.playerArmorStatRepository = playerArmorStatRepository;
        this.playerInventoryRepository = playerInventoryRepository;
        this.playerDungeonProgressRepository = playerDungeonProgressRepository;
        this.playerActiveRunRepository = playerActiveRunRepository;
        this.characterService = characterService;
        this.dropService = dropService;
        this.shopService = shopService;
        this.equipmentService = equipmentService;
        this.masterDataLoader = masterDataLoader;
    }

    /**
     * 캐릭터를 생성하고 시작 무기([일반] 낡은 검)를 지급하여 착용시킨다.
     *
     * @param name 캐릭터명
     * @return 생성된 플레이어 엔티티
     */
    public Player createCharacter(final String name) {
        final Player player = characterService.createInitialCharacter(name);
        final Player savedPlayer = playerRepository.save(player);
        grantStarterWeapon(savedPlayer);
        return savedPlayer;
    }

    /**
     * 전투 보상(경험치 + 골드)을 지급한다.
     *
     * @param playerId  플레이어 식별자
     * @param monsterId 처치한 몬스터 ID
     * @return 레벨업 결과
     */
    public LevelUpResult grantBattleReward(final Long playerId, final long monsterId) {
        final Player player = findPlayerOrThrow(playerId);
        final MonsterTemplate monster = masterDataLoader.findMonster(monsterId);
        player.changeGold(player.getGold() + monster.goldReward());
        return characterService.gainExp(player, monster.expReward());
    }

    /**
     * 사망 페널티를 적용한다 (경험치 10% 감소, 활성 런 삭제, 마을 복귀).
     *
     * @param playerId 플레이어 식별자
     */
    public void applyDeathPenalty(final Long playerId) {
        final Player player = findPlayerOrThrow(playerId);
        characterService.applyExpPenalty(player, DEATH_PENALTY_RATIO);
        playerActiveRunRepository.deleteByPlayerId(playerId);
        characterService.restoreToTown(player);
    }

    /**
     * 도망 페널티를 적용한다 (경험치 5% 감소, 활성 런 삭제, 마을 복귀).
     *
     * @param playerId 플레이어 식별자
     */
    public void applyFleePenalty(final Long playerId) {
        final Player player = findPlayerOrThrow(playerId);
        characterService.applyExpPenalty(player, FLEE_PENALTY_RATIO);
        playerActiveRunRepository.deleteByPlayerId(playerId);
        characterService.restoreToTown(player);
    }

    /**
     * 던전을 포기한다 (경험치 페널티 없이 활성 런 삭제 후 마을 복귀).
     *
     * <p>전투 중에는 포기할 수 없다 (caller가 전투 중이 아님을 보장해야 하며,
     * 활성 런이 존재하지 않으면 IllegalActionException을 던진다).
     *
     * @param playerId 플레이어 식별자
     * @throws IllegalActionException 활성 던전 런이 없는 경우
     */
    public void abandonDungeon(final Long playerId) {
        final Player player = findPlayerOrThrow(playerId);
        final Optional<PlayerActiveRun> activeRun = playerActiveRunRepository.findByPlayerId(playerId);
        if (activeRun.isEmpty()) {
            throw new IllegalActionException("포기할 진행 중인 던전이 없습니다.");
        }
        playerActiveRunRepository.deleteByPlayerId(playerId);
        characterService.restoreToTown(player);
    }

    /**
     * 던전에 진입한다 (PlayerActiveRun 생성).
     *
     * <p>플레이어당 최대 1개의 활성 런만 허용하며, 이미 존재 시 예외를 던진다.
     *
     * @param playerId  플레이어 식별자
     * @param dungeonId 진입할 던전 ID
     * @return 생성된 PlayerActiveRun
     * @throws IllegalActionException 이미 진행 중인 던전이 있는 경우
     */
    public PlayerActiveRun enterDungeon(final Long playerId, final long dungeonId) {
        final Player player = findPlayerOrThrow(playerId);
        validateNoActiveRun(playerId);
        final PlayerActiveRun activeRun = new PlayerActiveRun(
                playerId, dungeonId, 0, player.getHp(), player.getMp(), LocalDateTime.now());
        return playerActiveRunRepository.save(activeRun);
    }

    /**
     * 체크포인트를 저장한다 (활성 런의 스테이지/HP/MP 갱신).
     *
     * @param playerId     플레이어 식별자
     * @param clearedStage 완료한 스테이지 번호
     * @param currentHp    현재 HP
     * @param currentMp    현재 MP
     */
    public void saveCheckpoint(final Long playerId, final int clearedStage,
                               final int currentHp, final int currentMp) {
        final PlayerActiveRun activeRun = findActiveRunOrThrow(playerId);
        activeRun.changeClearedStage(clearedStage);
        activeRun.changeCheckpointHp(currentHp);
        activeRun.changeCheckpointMp(currentMp);
        activeRun.changeUpdatedAt(LocalDateTime.now());
    }

    /**
     * 던전 재개 정보를 반환한다 (HP/MP를 체크포인트 값으로 복원).
     *
     * @param playerId 플레이어 식별자
     * @return 활성 런 (없으면 empty)
     */
    public Optional<PlayerActiveRun> resumeDungeon(final Long playerId) {
        final Optional<PlayerActiveRun> activeRunOpt = playerActiveRunRepository.findByPlayerId(playerId);
        if (activeRunOpt.isPresent()) {
            final Player player = findPlayerOrThrow(playerId);
            final PlayerActiveRun activeRun = activeRunOpt.get();
            player.changeHp(activeRun.getCheckpointHp());
            player.changeMp(activeRun.getCheckpointMp());
        }
        return activeRunOpt;
    }

    /**
     * 던전을 완료한다 (보스 클리어 후 진행 기록 갱신, 활성 런 삭제, 마을 복귀).
     *
     * @param playerId  플레이어 식별자
     * @param dungeonId 클리어한 던전 ID
     */
    public void completeDungeon(final Long playerId, final long dungeonId) {
        final Player player = findPlayerOrThrow(playerId);
        updateDungeonProgress(playerId, dungeonId);
        playerActiveRunRepository.deleteByPlayerId(playerId);
        characterService.restoreToTown(player);
    }

    /**
     * 무기 드랍을 저장한다 (RolledWeapon → PlayerWeapon + PlayerWeaponStat 영속화).
     *
     * @param playerId     플레이어 식별자
     * @param rolledWeapon 롤된 무기 인스턴스
     * @return 저장된 PlayerWeapon
     */
    public PlayerWeapon saveWeaponDrop(final Long playerId, final RolledWeapon rolledWeapon) {
        final PlayerWeapon weapon = buildPlayerWeapon(playerId, rolledWeapon);
        final PlayerWeapon savedWeapon = playerWeaponRepository.save(weapon);
        saveWeaponStats(savedWeapon.getId(), rolledWeapon.stats());
        return savedWeapon;
    }

    /**
     * 방어구 드랍을 저장한다 (RolledArmor → PlayerArmor + PlayerArmorStat 영속화).
     *
     * @param playerId    플레이어 식별자
     * @param rolledArmor 롤된 방어구 인스턴스
     * @return 저장된 PlayerArmor
     */
    public PlayerArmor saveArmorDrop(final Long playerId, final RolledArmor rolledArmor) {
        final PlayerArmor armor = buildPlayerArmor(playerId, rolledArmor);
        final PlayerArmor savedArmor = playerArmorRepository.save(armor);
        saveArmorStats(savedArmor.getId(), rolledArmor.stats());
        return savedArmor;
    }

    /**
     * 스킬북 드랍을 저장한다 (인벤토리에 추가 또는 수량 증가).
     *
     * @param playerId 플레이어 식별자
     * @param skillId  드랍된 스킬 ID
     */
    public void saveSkillBookDrop(final Long playerId, final long skillId) {
        addOrIncrementInventory(playerId, ItemType.SKILL_BOOK, skillId);
    }

    /**
     * 포션 드랍을 저장한다 (인벤토리에 추가 또는 수량 증가).
     *
     * @param playerId 플레이어 식별자
     * @param itemId   드랍된 아이템 ID
     */
    public void savePotionDrop(final Long playerId, final long itemId) {
        addOrIncrementInventory(playerId, ItemType.POTION, itemId);
    }

    /**
     * 골드 보상을 지급한다.
     *
     * @param playerId 플레이어 식별자
     * @param gold     지급할 골드량
     */
    public void saveGoldReward(final Long playerId, final int gold) {
        final Player player = findPlayerOrThrow(playerId);
        player.changeGold(player.getGold() + gold);
    }

    /**
     * 무기를 판매한다.
     *
     * @param playerId 플레이어 식별자
     * @param weaponId 판매할 무기 ID
     * @return 판매 수익 골드
     */
    public int sellWeapon(final Long playerId, final Long weaponId) {
        final Player player = findPlayerOrThrow(playerId);
        final PlayerWeapon weapon = playerWeaponRepository.findById(weaponId)
                .orElseThrow(() -> new IllegalActionException("무기를 찾을 수 없습니다."));
        final WeaponTemplate template = masterDataLoader.findWeaponTemplate(weapon.getWeaponTemplateId());
        final int price = shopService.sellWeapon(player, weapon, template.baseValue());
        deleteWeaponWithRelations(weaponId);
        return price;
    }

    /**
     * 방어구를 판매한다.
     *
     * @param playerId 플레이어 식별자
     * @param armorId  판매할 방어구 ID
     * @return 판매 수익 골드
     */
    public int sellArmor(final Long playerId, final Long armorId) {
        final Player player = findPlayerOrThrow(playerId);
        final PlayerArmor armor = playerArmorRepository.findById(armorId)
                .orElseThrow(() -> new IllegalActionException("방어구를 찾을 수 없습니다."));
        final ArmorTemplate template = masterDataLoader.findArmorTemplate(armor.getArmorTemplateId());
        final int price = shopService.sellArmor(player, armor, template.baseValue());
        deleteArmorWithRelations(armorId);
        return price;
    }

    /**
     * 포션을 구매한다.
     *
     * @param playerId 플레이어 식별자
     * @param itemId   구매할 포션 아이템 ID
     * @param buyPrice 구매가
     */
    public void buyPotion(final Long playerId, final long itemId, final int buyPrice) {
        final Player player = findPlayerOrThrow(playerId);
        final PlayerInventory inventory = findOrCreateInventory(playerId, ItemType.POTION, itemId);
        shopService.buyPotion(player, inventory, buyPrice);
    }

    /**
     * 무기를 착용한다.
     *
     * @param playerId 플레이어 식별자
     * @param weaponId 착용할 무기 ID
     * @throws IllegalActionException 던전 진행 중 장비 변경 시도 시
     */
    public void equipWeapon(final Long playerId, final Long weaponId) {
        findPlayerOrThrow(playerId);
        final boolean isInDungeon = playerActiveRunRepository.findByPlayerId(playerId).isPresent();
        final List<PlayerWeapon> playerWeapons = playerWeaponRepository.findByPlayerId(playerId);
        final PlayerWeapon weaponToEquip = playerWeapons.stream()
                .filter(w -> w.getId().equals(weaponId))
                .findFirst()
                .orElseThrow(() -> new IllegalActionException("무기를 찾을 수 없습니다."));
        equipmentService.equipWeapon(playerWeapons, weaponToEquip, isInDungeon);
    }

    /**
     * 방어구를 착용한다.
     *
     * @param playerId 플레이어 식별자
     * @param armorId  착용할 방어구 ID
     * @throws IllegalActionException 던전 진행 중 장비 변경 시도 시
     */
    public void equipArmor(final Long playerId, final Long armorId) {
        findPlayerOrThrow(playerId);
        final boolean isInDungeon = playerActiveRunRepository.findByPlayerId(playerId).isPresent();
        final List<PlayerArmor> playerArmors = playerArmorRepository.findByPlayerId(playerId);
        final PlayerArmor armorToEquip = playerArmors.stream()
                .filter(a -> a.getId().equals(armorId))
                .findFirst()
                .orElseThrow(() -> new IllegalActionException("방어구를 찾을 수 없습니다."));
        equipmentService.equipArmor(playerArmors, armorToEquip, isInDungeon);
    }

    /**
     * 스킬북을 무기에 장착한다.
     *
     * @param playerId      플레이어 식별자
     * @param weaponId      대상 무기 ID
     * @param skillId       장착할 스킬 ID
     * @param overwriteSlot 덮어쓸 슬롯 인덱스 (빈 슬롯 사용 시 empty)
     * @return 새로 생성된 스킬 장착 (빈 슬롯 사용 시), 덮어쓰기 시 empty
     */
    public Optional<PlayerWeaponSkill> attachSkillBook(final Long playerId, final Long weaponId,
                                                       final long skillId,
                                                       final Optional<Integer> overwriteSlot) {
        findPlayerOrThrow(playerId);
        final PlayerWeapon weapon = playerWeaponRepository.findById(weaponId)
                .orElseThrow(() -> new IllegalActionException("무기를 찾을 수 없습니다."));
        final List<PlayerWeaponSkill> currentSkills = playerWeaponSkillRepository.findByPlayerWeaponId(weaponId);
        final SkillTemplate skillTemplate = masterDataLoader.findSkill(skillId);
        final PlayerInventory inventoryItem = findInventoryOrThrow(playerId, ItemType.SKILL_BOOK, skillId);
        final Optional<PlayerWeaponSkill> result = equipmentService.attachSkillBook(
                weapon, currentSkills, skillTemplate, inventoryItem, overwriteSlot);
        result.ifPresent(playerWeaponSkillRepository::save);
        cleanupEmptyInventory(inventoryItem);
        return result;
    }

    /**
     * 플레이어를 조회한다.
     *
     * @param playerId 플레이어 식별자
     * @return 플레이어 엔티티
     * @throws PlayerNotFoundException 존재하지 않는 플레이어 ID인 경우
     */
    public Player getPlayer(final Long playerId) {
        return findPlayerOrThrow(playerId);
    }

    // ─────────────────────────────────────────────────────────────
    // Private helper methods
    // ─────────────────────────────────────────────────────────────

    private Player findPlayerOrThrow(final Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("플레이어를 찾을 수 없습니다. ID: " + playerId));
    }

    private PlayerActiveRun findActiveRunOrThrow(final Long playerId) {
        return playerActiveRunRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalActionException("진행 중인 던전이 없습니다."));
    }

    private void validateNoActiveRun(final Long playerId) {
        if (playerActiveRunRepository.findByPlayerId(playerId).isPresent()) {
            throw new IllegalActionException("이미 진행 중인 던전이 있습니다.");
        }
    }

    private void grantStarterWeapon(final Player savedPlayer) {
        final WeaponTemplate template = masterDataLoader.findWeaponTemplate(STARTER_WEAPON_TEMPLATE_ID);
        final RolledWeapon rolled = dropService.buildWeaponInstance(
                template, Grade.COMMON, STARTER_WEAPON_ITEM_LEVEL);
        final PlayerWeapon weapon = buildPlayerWeapon(savedPlayer.getId(), rolled);
        weapon.changeEquipped(true);
        final PlayerWeapon savedWeapon = playerWeaponRepository.save(weapon);
        saveWeaponStats(savedWeapon.getId(), rolled.stats());
    }

    private PlayerWeapon buildPlayerWeapon(final Long playerId, final RolledWeapon rolled) {
        return new PlayerWeapon(
                playerId, rolled.templateId(), rolled.displayName(), rolled.weaponType(),
                rolled.grade(), rolled.itemLevel(), rolled.baseAttack(),
                rolled.baseSpeed(), rolled.baseCritical(), rolled.skillSlots(), false);
    }

    private PlayerArmor buildPlayerArmor(final Long playerId, final RolledArmor rolled) {
        final ArmorTemplate template = masterDataLoader.findArmorTemplate(rolled.templateId());
        return new PlayerArmor(
                playerId, rolled.templateId(), rolled.displayName(), rolled.slot(),
                rolled.grade(), template.baseDefense(), rolled.itemLevel(), false);
    }

    private void saveWeaponStats(final Long weaponId, final List<StatRoll> stats) {
        for (final StatRoll stat : stats) {
            final PlayerWeaponStat weaponStat = new PlayerWeaponStat(
                    weaponId, stat.statType(), stat.value());
            playerWeaponStatRepository.save(weaponStat);
        }
    }

    private void saveArmorStats(final Long armorId, final List<StatRoll> stats) {
        for (final StatRoll stat : stats) {
            final PlayerArmorStat armorStat = new PlayerArmorStat(
                    armorId, stat.statType(), stat.value());
            playerArmorStatRepository.save(armorStat);
        }
    }

    private void addOrIncrementInventory(final Long playerId, final ItemType itemType,
                                         final long itemRefId) {
        final PlayerInventory inventory = findOrCreateInventory(playerId, itemType, itemRefId);
        inventory.changeQuantity(inventory.getQuantity() + 1);
    }

    private PlayerInventory findOrCreateInventory(final Long playerId, final ItemType itemType,
                                                  final long itemRefId) {
        return playerInventoryRepository
                .findByPlayerIdAndItemTypeAndItemRefId(playerId, itemType, itemRefId)
                .orElseGet(() -> playerInventoryRepository.save(
                        new PlayerInventory(playerId, itemType, itemRefId, 0)));
    }

    private PlayerInventory findInventoryOrThrow(final Long playerId, final ItemType itemType,
                                                 final long itemRefId) {
        return playerInventoryRepository
                .findByPlayerIdAndItemTypeAndItemRefId(playerId, itemType, itemRefId)
                .orElseThrow(() -> new IllegalActionException("인벤토리에 해당 아이템이 없습니다."));
    }

    private void cleanupEmptyInventory(final PlayerInventory inventory) {
        if (inventory.getQuantity() <= 0) {
            playerInventoryRepository.delete(inventory);
        }
    }

    private void deleteWeaponWithRelations(final Long weaponId) {
        playerWeaponSkillRepository.deleteByPlayerWeaponId(weaponId);
        playerWeaponStatRepository.deleteByPlayerWeaponId(weaponId);
        playerWeaponRepository.deleteById(weaponId);
    }

    private void deleteArmorWithRelations(final Long armorId) {
        playerArmorStatRepository.deleteByPlayerArmorId(armorId);
        playerArmorRepository.deleteById(armorId);
    }

    private void updateDungeonProgress(final Long playerId, final long dungeonId) {
        final Optional<PlayerDungeonProgress> existingOpt =
                playerDungeonProgressRepository.findByPlayerIdAndDungeonId(playerId, dungeonId);
        if (existingOpt.isPresent()) {
            final PlayerDungeonProgress progress = existingOpt.get();
            progress.changeCleared(true);
            if (progress.getBestStage() < BOSS_STAGE) {
                progress.changeBestStage(BOSS_STAGE);
            }
        } else {
            final PlayerDungeonProgress progress = new PlayerDungeonProgress(
                    playerId, dungeonId, true, BOSS_STAGE);
            playerDungeonProgressRepository.save(progress);
        }
    }
}
