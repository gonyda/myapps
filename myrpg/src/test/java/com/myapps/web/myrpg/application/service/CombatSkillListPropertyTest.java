package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.domain.model.ActionLog;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 전투 스킬 목록 프로퍼티 테스트.
 *
 * <p>임의의 착용 무기와 스킬 카탈로그에 대해, {@code combatSkills}가
 * 착용 무기 재능 스킬 + 공통 스킬(방어)만 포함하고 다른 재능 스킬은 제외하며,
 * 무기 변경 시 목록이 그에 맞게 바뀌는지 검증한다.
 *
 * <p>Feature: 008-battle-system, Property 16: 전투 스킬 목록 = 무기 재능 + 공통
 *
 * <p><b>Validates: Requirements 16.1, 16.2, 20.4</b>
 */
// Feature: 008-battle-system, Property 16: 전투 스킬 목록 = 무기 재능 + 공통
class CombatSkillListPropertyTest {

    private static final long CHARACTER_ID = 1L;
    private static final int MAX_DURABILITY = 20;

    /**
     * 착용 무기 재능과 공통 스킬만 포함되고, 다른 재능 스킬은 제외됨을 검증한다.
     *
     * @param weaponTalent 착용 무기의 재능
     */
    @Property(tries = 100)
    void should_includeOnlyMatchingTalentAndCommon_when_weaponEquipped(
            @ForAll("weaponTalents") final SkillTalent weaponTalent) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        final EquipmentKind weaponKind = resolveWeaponKind(weaponTalent);
        final OwnedItem weapon = new OwnedItem("weapon_1", 1, StorageKind.INVENTORY, true, MAX_DURABILITY);
        final EquipmentItem weaponCatalog = new EquipmentItem(
                "weapon_1", "무기", ItemType.WEAPON, weaponKind, List.of(), null, MAX_DURABILITY);

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(weapon));
        when(itemCatalogService.byId("weapon_1")).thenReturn(Optional.of(weaponCatalog));

        // 캐릭터가 3재능 스킬 + 공통 스킬을 모두 보유
        final List<CharacterSkill> allSkills = List.of(
                new CharacterSkill(CHARACTER_ID, "melee_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "archery_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "magic_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "defense", SkillRank.F, 0, 0));

        when(characterSkillRepository.findByCharacterId(null)).thenReturn(allSkills);

        when(skillCatalogService.byId("melee_normal")).thenReturn(Optional.of(
                createDamageSkill("melee_normal", "근접일반", SkillType.NORMAL, SkillTalent.MELEE)));
        when(skillCatalogService.byId("archery_normal")).thenReturn(Optional.of(
                createDamageSkill("archery_normal", "활일반", SkillType.NORMAL, SkillTalent.ARCHERY)));
        when(skillCatalogService.byId("magic_normal")).thenReturn(Optional.of(
                createDamageSkill("magic_normal", "마법일반", SkillType.NORMAL, SkillTalent.MAGIC)));
        when(skillCatalogService.byId("defense")).thenReturn(Optional.of(
                createDefenseSkill("defense", "방어", SkillTalent.COMMON)));

        final CharacterProgress progress = createProgress();

        final List<BattleSkillButton> result = service.combatSkills(progress);

        // 무기 재능 스킬 + 공통만 포함
        assertThat(result).allSatisfy(button -> {
            final Optional<Skill> skill = skillCatalogService.byId(button.id());
            assertThat(skill).isPresent();
            final SkillTalent talent = skill.get().talent();
            assertThat(talent).isIn(weaponTalent, SkillTalent.COMMON);
        });

        // 공통(방어)은 반드시 포함
        assertThat(result).anyMatch(button -> button.id().equals("defense"));
    }

    /**
     * 다른 재능의 스킬이 결과에 포함되지 않음을 검증한다.
     *
     * @param weaponTalent 착용 무기의 재능
     */
    @Property(tries = 100)
    void should_excludeOtherTalentSkills_when_weaponEquipped(
            @ForAll("weaponTalents") final SkillTalent weaponTalent) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        final EquipmentKind weaponKind = resolveWeaponKind(weaponTalent);
        final OwnedItem weapon = new OwnedItem("weapon_1", 1, StorageKind.INVENTORY, true, MAX_DURABILITY);
        final EquipmentItem weaponCatalog = new EquipmentItem(
                "weapon_1", "무기", ItemType.WEAPON, weaponKind, List.of(), null, MAX_DURABILITY);

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(weapon));
        when(itemCatalogService.byId("weapon_1")).thenReturn(Optional.of(weaponCatalog));

        final List<CharacterSkill> allSkills = List.of(
                new CharacterSkill(CHARACTER_ID, "melee_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "archery_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "magic_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "defense", SkillRank.F, 0, 0));

        when(characterSkillRepository.findByCharacterId(null)).thenReturn(allSkills);

        when(skillCatalogService.byId("melee_normal")).thenReturn(Optional.of(
                createDamageSkill("melee_normal", "근접일반", SkillType.NORMAL, SkillTalent.MELEE)));
        when(skillCatalogService.byId("archery_normal")).thenReturn(Optional.of(
                createDamageSkill("archery_normal", "활일반", SkillType.NORMAL, SkillTalent.ARCHERY)));
        when(skillCatalogService.byId("magic_normal")).thenReturn(Optional.of(
                createDamageSkill("magic_normal", "마법일반", SkillType.NORMAL, SkillTalent.MAGIC)));
        when(skillCatalogService.byId("defense")).thenReturn(Optional.of(
                createDefenseSkill("defense", "방어", SkillTalent.COMMON)));

        final CharacterProgress progress = createProgress();

        final List<BattleSkillButton> result = service.combatSkills(progress);

        // 다른 재능 스킬은 제외
        for (final BattleSkillButton button : result) {
            final Optional<Skill> skill = skillCatalogService.byId(button.id());
            assertThat(skill).isPresent();
            final SkillTalent talent = skill.get().talent();
            assertThat(talent).isNotIn(otherTalents(weaponTalent));
        }
    }

    /**
     * 무기 미장착 시 공통 스킬(방어)만 반환됨을 검증한다.
     */
    @Property(tries = 100)
    void should_returnOnlyCommon_when_noWeaponEquipped() {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        // 무기 미장착 (장착 장비에 무기 없음)
        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of());

        final List<CharacterSkill> allSkills = List.of(
                new CharacterSkill(CHARACTER_ID, "melee_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "archery_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "defense", SkillRank.F, 0, 0));

        when(characterSkillRepository.findByCharacterId(null)).thenReturn(allSkills);

        when(skillCatalogService.byId("melee_normal")).thenReturn(Optional.of(
                createDamageSkill("melee_normal", "근접일반", SkillType.NORMAL, SkillTalent.MELEE)));
        when(skillCatalogService.byId("archery_normal")).thenReturn(Optional.of(
                createDamageSkill("archery_normal", "활일반", SkillType.NORMAL, SkillTalent.ARCHERY)));
        when(skillCatalogService.byId("defense")).thenReturn(Optional.of(
                createDefenseSkill("defense", "방어", SkillTalent.COMMON)));

        final CharacterProgress progress = createProgress();

        final List<BattleSkillButton> result = service.combatSkills(progress);

        // 공통 스킬만 포함
        assertThat(result).allSatisfy(button -> {
            final Optional<Skill> skill = skillCatalogService.byId(button.id());
            assertThat(skill).isPresent();
            assertThat(skill.get().talent()).isEqualTo(SkillTalent.COMMON);
        });
        assertThat(result).isNotEmpty();
    }

    /**
     * 무기 변경 시 스킬 목록이 새 무기 재능으로 바뀜을 검증한다.
     *
     * @param talentPair 변경 전/후 무기 재능 쌍
     */
    @Property(tries = 100)
    void should_changeSkillList_when_weaponChanges(
            @ForAll("differentTalentPairs") final SkillTalent talentPair) {

        final OwnedItemRepository ownedItemRepository = mock(OwnedItemRepository.class);
        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final CharacterProgressRepository characterProgressRepository = mock(CharacterProgressRepository.class);
        final ActionLog actionLog = new ActionLog(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final CharacterSkillRepository characterSkillRepository = mock(CharacterSkillRepository.class);

        final InventoryService service = new InventoryService(
                ownedItemRepository, itemCatalogService, characterProgressRepository,
                new StatProgression(), actionLog, skillCatalogService, characterSkillRepository);

        // 첫 번째 무기 (talentPair와 다른 재능)
        final SkillTalent firstTalent = getOtherTalent(talentPair);
        final EquipmentKind firstKind = resolveWeaponKind(firstTalent);
        final OwnedItem firstWeapon = new OwnedItem("weapon_first", 1, StorageKind.INVENTORY, true, MAX_DURABILITY);
        final EquipmentItem firstCatalog = new EquipmentItem(
                "weapon_first", "첫무기", ItemType.WEAPON, firstKind, List.of(), null, MAX_DURABILITY);

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(firstWeapon));
        when(itemCatalogService.byId("weapon_first")).thenReturn(Optional.of(firstCatalog));

        final List<CharacterSkill> allSkills = List.of(
                new CharacterSkill(CHARACTER_ID, "melee_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "archery_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "magic_normal", SkillRank.F, 0, 0),
                new CharacterSkill(CHARACTER_ID, "defense", SkillRank.F, 0, 0));

        when(characterSkillRepository.findByCharacterId(null)).thenReturn(allSkills);

        when(skillCatalogService.byId("melee_normal")).thenReturn(Optional.of(
                createDamageSkill("melee_normal", "근접일반", SkillType.NORMAL, SkillTalent.MELEE)));
        when(skillCatalogService.byId("archery_normal")).thenReturn(Optional.of(
                createDamageSkill("archery_normal", "활일반", SkillType.NORMAL, SkillTalent.ARCHERY)));
        when(skillCatalogService.byId("magic_normal")).thenReturn(Optional.of(
                createDamageSkill("magic_normal", "마법일반", SkillType.NORMAL, SkillTalent.MAGIC)));
        when(skillCatalogService.byId("defense")).thenReturn(Optional.of(
                createDefenseSkill("defense", "방어", SkillTalent.COMMON)));

        final CharacterProgress progress = createProgress();
        final List<BattleSkillButton> firstResult = service.combatSkills(progress);

        // 무기 변경: talentPair 재능으로 교체
        final EquipmentKind secondKind = resolveWeaponKind(talentPair);
        final OwnedItem secondWeapon = new OwnedItem("weapon_second", 1, StorageKind.INVENTORY, true, MAX_DURABILITY);
        final EquipmentItem secondCatalog = new EquipmentItem(
                "weapon_second", "두번째무기", ItemType.WEAPON, secondKind, List.of(), null, MAX_DURABILITY);

        when(ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY))
                .thenReturn(List.of(secondWeapon));
        when(itemCatalogService.byId("weapon_second")).thenReturn(Optional.of(secondCatalog));

        final List<BattleSkillButton> secondResult = service.combatSkills(progress);

        // 목록이 달라져야 함 (재능 스킬 ID가 다름)
        final List<String> firstIds = firstResult.stream().map(BattleSkillButton::id).sorted().toList();
        final List<String> secondIds = secondResult.stream().map(BattleSkillButton::id).sorted().toList();
        assertThat(firstIds).isNotEqualTo(secondIds);
    }

    // ─── Arbitrary Providers ────────────────────────────────────────────────

    /**
     * 무기에 매핑 가능한 SkillTalent (MELEE, ARCHERY, MAGIC)를 생성한다.
     *
     * @return SkillTalent Arbitrary
     */
    @Provide
    Arbitrary<SkillTalent> weaponTalents() {
        return Arbitraries.of(SkillTalent.MELEE, SkillTalent.ARCHERY, SkillTalent.MAGIC);
    }

    /**
     * 무기 변경 테스트용: MELEE/ARCHERY/MAGIC 중 하나를 반환한다.
     *
     * @return SkillTalent Arbitrary
     */
    @Provide
    Arbitrary<SkillTalent> differentTalentPairs() {
        return Arbitraries.of(SkillTalent.MELEE, SkillTalent.ARCHERY, SkillTalent.MAGIC);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * 스킬 재능에 대응하는 무기 종류를 결정한다.
     *
     * @param talent 스킬 재능
     * @return 대응하는 EquipmentKind
     */
    private EquipmentKind resolveWeaponKind(final SkillTalent talent) {
        return switch (talent) {
            case MELEE -> EquipmentKind.ONE_HANDED_SWORD;
            case ARCHERY -> EquipmentKind.BOW;
            case MAGIC -> EquipmentKind.WAND;
            default -> EquipmentKind.ONE_HANDED_SWORD;
        };
    }

    /**
     * 주어진 재능이 아닌 다른 무기 재능을 반환한다.
     *
     * @param talent 제외할 재능
     * @return 다른 재능
     */
    private SkillTalent getOtherTalent(final SkillTalent talent) {
        return switch (talent) {
            case MELEE -> SkillTalent.ARCHERY;
            case ARCHERY -> SkillTalent.MAGIC;
            case MAGIC -> SkillTalent.MELEE;
            default -> SkillTalent.MELEE;
        };
    }

    /**
     * 주어진 재능이 아닌 다른 재능 배열을 반환한다 (COMMON 제외).
     *
     * @param weaponTalent 착용 무기 재능
     * @return 제외 대상 재능 배열
     */
    private SkillTalent[] otherTalents(final SkillTalent weaponTalent) {
        return switch (weaponTalent) {
            case MELEE -> new SkillTalent[]{SkillTalent.ARCHERY, SkillTalent.MAGIC};
            case ARCHERY -> new SkillTalent[]{SkillTalent.MELEE, SkillTalent.MAGIC};
            case MAGIC -> new SkillTalent[]{SkillTalent.MELEE, SkillTalent.ARCHERY};
            default -> new SkillTalent[]{};
        };
    }

    /**
     * 테스트용 DamageSkill을 생성한다.
     *
     * @param id     스킬 ID
     * @param label  스킬 라벨
     * @param type   스킬 타입
     * @param talent 스킬 재능
     * @return DamageSkill 인스턴스
     */
    private DamageSkill createDamageSkill(final String id, final String label,
                                          final SkillType type, final SkillTalent talent) {
        final Map<SkillRank, Integer> multiplierByRank = createFullRankMap(100);
        return new DamageSkill(id, label, type, talent, 5, multiplierByRank, "테스트 스킬");
    }

    /**
     * 테스트용 DefenseSkill을 생성한다.
     *
     * @param id     스킬 ID
     * @param label  스킬 라벨
     * @param talent 스킬 재능
     * @return DefenseSkill 인스턴스
     */
    private DefenseSkill createDefenseSkill(final String id, final String label,
                                            final SkillTalent talent) {
        final Map<SkillRank, Integer> blockRateByRank = createFullRankMap(30);
        final Map<SkillRank, Integer> counterMultiplierByRank = createFullRankMap(50);
        return new DefenseSkill(id, label, SkillType.DEFENSE, talent, 3,
                blockRateByRank, counterMultiplierByRank, "방어 스킬");
    }

    /**
     * 모든 랭크에 동일 값을 넣은 랭크 맵을 생성한다.
     *
     * @param value 모든 랭크에 설정할 값
     * @return 불변 랭크 맵
     */
    private Map<SkillRank, Integer> createFullRankMap(final int value) {
        final Map<SkillRank, Integer> map = new EnumMap<>(SkillRank.class);
        for (final SkillRank rank : SkillRank.values()) {
            map.put(rank, value);
        }
        return Map.copyOf(map);
    }

    /**
     * 테스트용 CharacterProgress를 생성한다.
     *
     * @return CharacterProgress 인스턴스 (id는 CHARACTER_ID)
     */
    private CharacterProgress createProgress() {
        return new CharacterProgress(
                "테스트", 1, 1, 0L, TalentType.MELEE, null,
                100, 100, 100, "tir-chonaill", 0, 0L);
    }
}
