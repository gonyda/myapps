package com.myapps.web.myrpg.application.service.inventory;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.service.SkillCatalogService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.PassiveSkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import java.util.List;
import java.util.Optional;

/** 착용 무기 재능과 캐릭터 보유 스킬을 바탕으로 전투 화면의 10개 스킬 슬롯 버튼 목록을 조립하는 협력 객체. */
public final class CombatSkillLoadoutService {

    private final InventoryQuerySupport query;
    private final CharacterSkillRepository characterSkillRepository;
    private final SkillCatalogService skillCatalogService;

    /**
     * CombatSkillLoadoutService를 생성한다.
     *
     * @param query 공유 조회 지원 컴포넌트
     * @param characterSkillRepository 캐릭터 스킬 저장소
     * @param skillCatalogService 스킬 카탈로그 서비스
     */
    public CombatSkillLoadoutService(
            final InventoryQuerySupport query,
            final CharacterSkillRepository characterSkillRepository,
            final SkillCatalogService skillCatalogService) {
        this.query = query;
        this.characterSkillRepository = characterSkillRepository;
        this.skillCatalogService = skillCatalogService;
    }

    /**
     * 전투에서 사용할 10개 스킬 슬롯 버튼 목록을 반환한다.
     *
     * <p>0~9번 10개 슬롯을 순서대로 생성하며, 착용 중인 무기 계열({@code weaponTalent})과 일치하거나 공통(COMMON) 스킬만 {@code
     * enabled=true}로 활성화된다. 착용 무기와 다른 계열의 스킬은 슬롯 자리는 유지하되 {@code enabled=false}로 비활성화된다.
     *
     * @param progress 캐릭터 진행상황 (보유 스킬·착용 장비 조회용)
     * @return 10개 슬롯의 BattleSkillButton 목록
     */
    public List<BattleSkillButton> combatSkills(final CharacterProgress progress) {
        final Long charId = progress != null ? progress.getId() : null;
        final SkillTalent weaponTalent = query.resolveEquippedWeaponTalent(charId);
        final List<CharacterSkill> ownedSkills = resolveOwnedSkills(charId);

        final BattleSkillButton[] slotArray = new BattleSkillButton[10];
        for (int i = 0; i < 10; i++) {
            slotArray[i] = BattleSkillButton.emptySlot(i);
        }

        for (final CharacterSkill characterSkill : ownedSkills) {
            final Integer slotIdx = characterSkill.getSlotIndex();
            if (slotIdx != null && slotIdx >= 0 && slotIdx < 10) {
                final BattleSkillButton button =
                        buildSlotButton(characterSkill, slotIdx, weaponTalent);
                if (button != null) {
                    slotArray[slotIdx] = button;
                }
            }
        }
        return List.of(slotArray);
    }

    private List<CharacterSkill> resolveOwnedSkills(final Long charId) {
        List<CharacterSkill> ownedSkills = List.of();
        if (charId != null) {
            ownedSkills = characterSkillRepository.findByCharacterId(charId);
        }
        if (ownedSkills == null || ownedSkills.isEmpty()) {
            ownedSkills = characterSkillRepository.findByCharacterId(null);
        }
        return ownedSkills != null ? ownedSkills : List.of();
    }

    private BattleSkillButton buildSlotButton(
            final CharacterSkill characterSkill,
            final int slotIdx,
            final SkillTalent weaponTalent) {
        final Optional<Skill> catalogOpt = skillCatalogService.byId(characterSkill.getSkillId());
        if (catalogOpt.isEmpty() || catalogOpt.get() instanceof PassiveSkill) {
            return null;
        }
        final Skill catalog = catalogOpt.get();
        final boolean isCommon = catalog.talent() == SkillTalent.COMMON;
        final boolean isMatched = weaponTalent != null && catalog.talent() == weaponTalent;
        final boolean enabled = isCommon || isMatched;
        final String disabledReason =
                enabled ? null : (weaponTalent == null ? "무기 장착 필요" : "장착 무기 불일치");
        final int cooldown = characterSkill.getUltimateCooldown();
        final boolean ready = catalog.type() == SkillType.ULTIMATE && cooldown == 0;
        final String icon = resolveSkillIcon(catalog);

        return new BattleSkillButton(
                catalog.id(),
                catalog.label(),
                catalog.type(),
                catalog.talent().resourceKind(),
                catalog.resourceCost(),
                cooldown,
                ready,
                slotIdx,
                enabled,
                disabledReason,
                false,
                icon);
    }

    private String resolveSkillIcon(final Skill catalog) {
        if (catalog == null) {
            return "⚔️";
        }
        if (catalog.talent() == SkillTalent.COMMON || catalog instanceof PassiveSkill) {
            return "👑";
        }
        return switch (catalog.talent()) {
            case MELEE -> "⚔️";
            case ARCHERY -> "🏹";
            case MAGIC -> "🔮";
            case COMMON -> "👑";
        };
    }
}
