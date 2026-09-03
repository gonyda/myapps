package com.myapps.web.myrpg.application.service.inventory;

import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.application.service.SkillCatalogService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.PotionItem;
import com.myapps.web.myrpg.domain.model.SkillRankupBonus;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.VitalMax;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.List;

/** 포션 소비 시 HP/MP/스태미나 회복 계산 및 수량 차감/삭제를 담당하는 협력 객체. */
public final class PotionConsumptionService {

    private final InventoryQuerySupport query;
    private final OwnedItemRepository ownedItemRepository;
    private final ItemCatalogService itemCatalogService;
    private final StatProgression statProgression;
    private final EquipmentBonusService equipmentBonusService;
    private final CharacterSkillRepository characterSkillRepository;
    private final SkillCatalogService skillCatalogService;
    private final SkillRankupBonus skillRankupBonus;

    /**
     * PotionConsumptionService를 생성한다.
     *
     * @param query 공유 조회 지원 컴포넌트
     * @param ownedItemRepository 보유 아이템 저장소
     * @param itemCatalogService 아이템 카탈로그 서비스
     * @param statProgression 스탯 성장 계산기
     * @param equipmentBonusService 장비 보너스 서비스
     * @param characterSkillRepository 캐릭터 스킬 저장소
     * @param skillCatalogService 스킬 카탈로그 서비스
     * @param skillRankupBonus 스킬 랭크업 보너스 계산기
     */
    public PotionConsumptionService(
            final InventoryQuerySupport query,
            final OwnedItemRepository ownedItemRepository,
            final ItemCatalogService itemCatalogService,
            final StatProgression statProgression,
            final EquipmentBonusService equipmentBonusService,
            final CharacterSkillRepository characterSkillRepository,
            final SkillCatalogService skillCatalogService,
            final SkillRankupBonus skillRankupBonus) {
        this.query = query;
        this.ownedItemRepository = ownedItemRepository;
        this.itemCatalogService = itemCatalogService;
        this.statProgression = statProgression;
        this.equipmentBonusService = equipmentBonusService;
        this.characterSkillRepository = characterSkillRepository;
        this.skillCatalogService = skillCatalogService;
        this.skillRankupBonus = skillRankupBonus;
    }

    /**
     * 인벤토리의 포션 아이템을 1개 사용한다.
     *
     * <p>HP, MP, Stamina를 각각 {@code min(current + heal, max)}로 회복하고 수량을 1 감소시킨다. 수량이 0이 되면 해당 행을
     * 삭제한다.
     *
     * @param ownedItemId 사용할 포션 보유 아이템 PK
     * @return 사용된 포션 카탈로그 아이템
     */
    public PotionItem usePotion(final long ownedItemId) {
        final OwnedItem target = query.findOwnedItemOrThrow(ownedItemId);
        final Item catalogItem =
                itemCatalogService
                        .byId(target.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + target.getItemId()));

        if (!(catalogItem instanceof PotionItem potionItem)) {
            throw new IllegalStateException("포션이 아닌 아이템은 사용할 수 없습니다: " + target.getItemId());
        }

        final CharacterProgress character = query.loadCharacter(target.getCharacterId());
        final VitalMax vitalMax =
                statProgression.vitalMaxFor(character.getCurrentLevel(), character.getTalent());
        final VitalMax equipVitalBonus =
                equipmentBonusService.equippedBonus(character.getId()).vitalBonus();
        final List<CharacterSkill> ownedSkills =
                characterSkillRepository.findByCharacterId(character.getId());
        final VitalMax skillVitalBonus =
                skillRankupBonus.sumVital(ownedSkills, skillCatalogService::byId);
        final int hpMax = vitalMax.hp() + equipVitalBonus.hp() + skillVitalBonus.hp();
        final int mpMax = vitalMax.mp() + equipVitalBonus.mp() + skillVitalBonus.mp();
        final int staminaMax =
                vitalMax.stamina() + equipVitalBonus.stamina() + skillVitalBonus.stamina();

        final int healedHp = Math.min(character.getHpCurrent() + potionItem.healHp(), hpMax);
        final int healedMp = Math.min(character.getMpCurrent() + potionItem.healMp(), mpMax);
        final int healedStamina =
                Math.min(character.getStaminaCurrent() + potionItem.healStamina(), staminaMax);

        character.fullRecover(new VitalMax(healedHp, healedMp, healedStamina));

        target.decreaseQuantity(1);
        if (target.getQuantity() == 0) {
            ownedItemRepository.delete(target);
        }
        return potionItem;
    }
}
