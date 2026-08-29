package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.model.UltimateSkill;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import com.myapps.web.myrpg.domain.repository.UserAccountRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/** 어드민 프리셋 스킬 무결성 및 인벤토리 격리에 대한 Property-Based Test (Property 2, Property 3). */
class AdminPresetPropertyTest {

    @Provide
    Arbitrary<Long> characterIds() {
        return Arbitraries.longs().greaterOrEqual(100L).lessOrEqual(9999L);
    }

    @Provide
    Arbitrary<Integer> skillCounts() {
        return Arbitraries.integers().between(30, 40);
    }

    @Property(tries = 50)
    void should_acquireAllSkillsAsFRank_when_adminAccountInitialized(
            @ForAll("characterIds") final Long adminCharId,
            @ForAll("skillCounts") final Integer count) {
        // given
        final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        final CharacterProgressRepository characterProgressRepository =
                mock(CharacterProgressRepository.class);
        final CharacterSkillRepository characterSkillRepository =
                mock(CharacterSkillRepository.class);
        final SkillCatalogService skillCatalogService = mock(SkillCatalogService.class);
        final InventoryService inventoryService = mock(InventoryService.class);
        final CharacterService characterService = mock(CharacterService.class);

        when(userAccountRepository.findByUsername("bbsk")).thenReturn(Optional.empty());
        when(userAccountRepository.findByUsername("admin")).thenReturn(Optional.empty());

        final CharacterProgress goni = CharacterProgress.createDefault();
        when(characterService.loadOrCreateDefault()).thenReturn(goni);

        final CharacterProgress adminChar = CharacterProgress.createNamed("관리자");
        when(characterProgressRepository.save(any(CharacterProgress.class))).thenReturn(adminChar);

        final List<Skill> catalogSkills = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            catalogSkills.add(
                    new UltimateSkill(
                            "skill_" + i,
                            "스킬 " + i,
                            SkillType.NORMAL,
                            SkillTalent.MELEE,
                            10,
                            Map.of(SkillRank.F, 100),
                            Map.of(SkillRank.F, 1),
                            0,
                            Map.of(SkillRank.F, 10),
                            "설명"));
        }
        when(skillCatalogService.all()).thenReturn(catalogSkills);

        final List<CharacterSkill> savedSkills = new ArrayList<>();
        when(characterSkillRepository.save(any(CharacterSkill.class)))
                .thenAnswer(
                        invocation -> {
                            final CharacterSkill cs = invocation.getArgument(0);
                            savedSkills.add(cs);
                            return cs;
                        });

        final AuthService service =
                new AuthService(
                        userAccountRepository,
                        characterProgressRepository,
                        characterSkillRepository,
                        skillCatalogService,
                        inventoryService,
                        characterService);

        // when
        service.initDefaultAccounts();

        // then
        assertThat(savedSkills).hasSize(count);
        for (final CharacterSkill cs : savedSkills) {
            assertThat(cs.getRank()).isEqualTo(SkillRank.F);
            assertThat(cs.getUsageCount()).isEqualTo(0);
        }
    }

    @Property(tries = 50)
    void should_isolateInventory_when_differentCharacterIds(
            @ForAll("characterIds") final Long charId1,
            @ForAll("characterIds") final Long charId2) {
        // given
        final Long actualCharId2 = charId1.equals(charId2) ? charId2 + 1 : charId2;
        final OwnedItemRepository repository = mock(OwnedItemRepository.class);

        final List<OwnedItem> char1Items = new ArrayList<>();
        char1Items.add(
                new OwnedItem(charId1, "beginner_sword", 1, StorageKind.INVENTORY, true, 20.0));
        char1Items.add(
                new OwnedItem(charId1, "hp_potion_30", 5, StorageKind.INVENTORY, false, 0.0));

        final List<OwnedItem> char2Items = new ArrayList<>();
        char2Items.add(
                new OwnedItem(actualCharId2, "claymore", 1, StorageKind.INVENTORY, true, 20.0));

        when(repository.findByCharacterIdAndStorageOrderById(charId1, StorageKind.INVENTORY))
                .thenReturn(char1Items);
        when(repository.findByCharacterIdAndStorageOrderById(actualCharId2, StorageKind.INVENTORY))
                .thenReturn(char2Items);

        // when
        final List<OwnedItem> loaded1 =
                repository.findByCharacterIdAndStorageOrderById(charId1, StorageKind.INVENTORY);
        final List<OwnedItem> loaded2 =
                repository.findByCharacterIdAndStorageOrderById(
                        actualCharId2, StorageKind.INVENTORY);

        // then
        assertThat(loaded1).hasSize(2);
        assertThat(loaded2).hasSize(1);
        assertThat(loaded1.stream().map(OwnedItem::getItemId).toList()).doesNotContain("claymore");
        assertThat(loaded2.stream().map(OwnedItem::getItemId).toList())
                .doesNotContain("beginner_sword");
    }
}
