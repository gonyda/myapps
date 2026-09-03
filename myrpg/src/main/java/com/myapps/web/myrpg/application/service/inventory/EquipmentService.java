package com.myapps.web.myrpg.application.service.inventory;

import com.myapps.web.myrpg.application.dto.OwnedItemView;
import com.myapps.web.myrpg.application.exception.EquipConflictException;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipSlot;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** 장비 착용, 해제, 스마트 장착, 무기 스왑, 슬롯별 착용 가능 목록 조회 및 무기 재능을 담당하는 협력 객체. */
public final class EquipmentService {

    private final InventoryQuerySupport query;
    private final OwnedItemRepository ownedItemRepository;
    private final CharacterProgressRepository characterProgressRepository;
    private final Function<OwnedItem, OwnedItemView> itemViewFunction;

    /**
     * EquipmentService를 생성한다.
     *
     * @param query 공유 조회 지원 컴포넌트
     * @param ownedItemRepository 보유 아이템 저장소
     * @param characterProgressRepository 캐릭터 진행상황 저장소
     * @param itemViewFunction 보유 아이템 뷰 변환 함수
     */
    public EquipmentService(
            final InventoryQuerySupport query,
            final OwnedItemRepository ownedItemRepository,
            final CharacterProgressRepository characterProgressRepository,
            final Function<OwnedItem, OwnedItemView> itemViewFunction) {
        this.query = query;
        this.ownedItemRepository = ownedItemRepository;
        this.characterProgressRepository = characterProgressRepository;
        this.itemViewFunction = itemViewFunction;
    }

    /**
     * 장비를 착용한다.
     *
     * @param ownedItemId 착용할 보유 아이템 PK
     * @throws EquipConflictException 착용 슬롯 충돌 시
     */
    public void equip(final long ownedItemId) {
        final OwnedItem target = query.findOwnedItemOrThrow(ownedItemId);
        final EquipmentItem equipmentItem = query.resolveEquipmentOrThrow(target);
        final EquipmentKind targetKind = equipmentItem.kind();
        final Set<EquipSlot> requiredSlots = targetKind.requiredSlots();

        final List<OwnedItem> equippedItems =
                query.findEquippedInventoryItems(target.getCharacterId());

        for (final EquipSlot slot : requiredSlots) {
            final Optional<OwnedItem> occupier = findSlotOccupier(equippedItems, slot, target);
            if (occupier.isPresent()) {
                final EquipmentItem occupierEquip = query.resolveEquipmentOrThrow(occupier.get());
                if (occupierEquip.kind().primarySlot() != targetKind.primarySlot()) {
                    throw new EquipConflictException("착용 할 수 없습니다.");
                }
            }
        }

        unequipSamePrimarySlot(equippedItems, targetKind.primarySlot(), target);
        target.equip();
    }

    /**
     * 장비를 장착 해제한다.
     *
     * @param ownedItemId 해제할 보유 아이템 PK
     */
    public void unequip(final long ownedItemId) {
        final OwnedItem target = query.findOwnedItemOrThrow(ownedItemId);
        target.unequip();

        final CharacterProgress progress = query.findCharacterSafe(target.getCharacterId());
        if (progress != null) {
            final boolean modified1 = clearWeaponSetSlot(progress, 1, ownedItemId);
            final boolean modified2 = clearWeaponSetSlot(progress, 2, ownedItemId);
            if (modified1 || modified2) {
                characterProgressRepository.save(progress);
            }
        }
    }

    /**
     * 슬롯 충돌을 자동으로 해결하며 장비를 착용한다.
     *
     * @param ownedItemId 착용할 보유 아이템 PK
     */
    public void smartEquip(final long ownedItemId) {
        final OwnedItem target = query.findOwnedItemOrThrow(ownedItemId);
        final EquipmentItem equipmentItem = query.resolveEquipmentOrThrow(target);
        final EquipmentKind targetKind = equipmentItem.kind();
        final List<OwnedItem> equippedItems =
                query.findEquippedInventoryItems(target.getCharacterId());

        autoUnequipShieldForTwoHanded(target, targetKind, equippedItems);
        autoUnequipTwoHandedForShield(target, targetKind, equippedItems);

        equip(ownedItemId);

        final CharacterProgress progress = query.findCharacterSafe(target.getCharacterId());
        if (progress != null) {
            clearFromOtherWeaponSet(progress, target.getId());
            if (targetKind.primarySlot() == EquipSlot.MAIN_HAND) {
                progress.setActiveMainWeaponId(target.getId());
                if (targetKind.requiredSlots().contains(EquipSlot.OFF_HAND)) {
                    progress.setActiveOffWeaponId(null);
                }
                characterProgressRepository.save(progress);
            } else if (targetKind.primarySlot() == EquipSlot.OFF_HAND) {
                progress.setActiveOffWeaponId(target.getId());
                characterProgressRepository.save(progress);
            }
        }
    }

    /**
     * 대치 페이즈 또는 장비창에서 무기 세트(I / II)를 즉시 스왑한다.
     *
     * <p>주무기(MAIN_HAND)뿐만 아니라 보조손 장비(방패 등 OFF_HAND)까지 각 무기 세트별로 저장 및 복원하여 완벽하게 페어로 전환한다.
     *
     * @param progressParam 캐릭터 진행 상태
     * @return 무기 스왑 성공 시 true, 스왑할 무기 세트가 없으면 false
     */
    public boolean swapWeapon(final CharacterProgress progressParam) {
        final CharacterProgress progress = resolveProgressForSwap(progressParam);
        if (progress == null) {
            return false;
        }

        final Long charId = progress.getId();
        final int currentSet = progress.getActiveWeaponSet();
        final int targetSet = currentSet == 1 ? 2 : 1;

        final List<OwnedItem> currentlyEquipped = query.findEquippedInventoryItems(charId);
        final Map<EquipSlot, OwnedItem> currentSlotMap =
                query.resolveEquippedSlotMap(currentlyEquipped);
        syncCurrentWeaponSetSlots(progress, currentSet, currentSlotMap);

        final Long targetMainId =
                resolveExistingOwnedId(
                        targetSet == 1 ? progress.getWeapon1MainId() : progress.getWeapon2MainId());
        final Long targetOffId =
                resolveExistingOwnedId(
                        targetSet == 1 ? progress.getWeapon1OffId() : progress.getWeapon2OffId());

        final boolean currentHasAny =
                currentSlotMap.containsKey(EquipSlot.MAIN_HAND)
                        || currentSlotMap.containsKey(EquipSlot.OFF_HAND);
        final boolean targetHasAny = targetMainId != null || targetOffId != null;

        if (!currentHasAny && !targetHasAny) {
            return false;
        }

        unequipWeaponSlots(currentSlotMap);
        equipTargetWeaponSet(targetMainId, targetOffId);

        progress.setActiveWeaponSet(targetSet);
        characterProgressRepository.save(progress);
        return true;
    }

    /**
     * 특정 슬롯에 착용 가능한 인벤토리 아이템 목록을 조회한다.
     *
     * @param characterId 캐릭터 PK
     * @param slotCode 슬롯 코드 (예: "HEAD", "BODY", "MAIN_HAND", "OFF_HAND", "HANDS", "FEET")
     * @return 착용 가능한 미장착 아이템 뷰 목록
     */
    public List<OwnedItemView> findEquippableForSlot(
            final Long characterId, final String slotCode) {
        final EquipSlot targetSlot;
        try {
            targetSlot = EquipSlot.valueOf(slotCode.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return List.of();
        }

        final List<OwnedItem> inventoryItems =
                characterId != null
                        ? ownedItemRepository.findByCharacterIdAndStorageOrderById(
                                characterId, StorageKind.INVENTORY)
                        : ownedItemRepository.findByStorageOrderById(StorageKind.INVENTORY);

        final Set<Long> otherSetAssignedItemIds = query.resolveOtherSetAssignedItemIds(characterId);

        return inventoryItems.stream()
                .filter(owned -> !owned.isEquipped())
                .filter(owned -> !otherSetAssignedItemIds.contains(owned.getId()))
                .filter(
                        owned -> {
                            final Optional<EquipmentItem> equipOpt = query.resolveEquipment(owned);
                            return equipOpt.isPresent()
                                    && equipOpt.get().kind().primarySlot() == targetSlot;
                        })
                .map(itemViewFunction)
                .toList();
    }

    /**
     * 기본 캐릭터의 현재 장착 중인 무기의 재능 분류를 반환한다.
     *
     * @return 장착 중인 무기의 재능 분류 (MELEE, ARCHERY, MAGIC, 또는 null)
     */
    public SkillTalent equippedWeaponTalent() {
        return equippedWeaponTalent(null);
    }

    /**
     * 특정 캐릭터의 현재 장착 중인 무기의 재능 분류를 반환한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 장착 중인 무기의 재능 분류 (MELEE, ARCHERY, MAGIC, 또는 null)
     */
    public SkillTalent equippedWeaponTalent(final Long characterId) {
        return query.resolveEquippedWeaponTalent(characterId);
    }

    /**
     * 기본 캐릭터의 현재 활(BOW) 계열 무기 장착 여부를 반환한다.
     *
     * @return 활을 장착 중이면 {@code true}, 아니면 {@code false}
     */
    public boolean isBowEquipped() {
        return isBowEquipped(null);
    }

    /**
     * 특정 캐릭터의 현재 활(BOW) 계열 무기 장착 여부를 반환한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 활을 장착 중이면 {@code true}, 아니면 {@code false}
     */
    public boolean isBowEquipped(final Long characterId) {
        return query.resolveEquippedWeaponTalent(characterId) == SkillTalent.ARCHERY;
    }

    private Optional<OwnedItem> findSlotOccupier(
            final List<OwnedItem> equippedItems,
            final EquipSlot slot,
            final OwnedItem excludeTarget) {
        for (final OwnedItem equipped : equippedItems) {
            if (equipped.getId().equals(excludeTarget.getId()) || !equipped.isEquipped()) {
                continue;
            }
            final Optional<EquipmentItem> equipOpt = query.resolveEquipment(equipped);
            if (equipOpt.isPresent() && equipOpt.get().kind().requiredSlots().contains(slot)) {
                return Optional.of(equipped);
            }
        }
        return Optional.empty();
    }

    private void unequipSamePrimarySlot(
            final List<OwnedItem> equippedItems,
            final EquipSlot primarySlot,
            final OwnedItem excludeTarget) {
        for (final OwnedItem equipped : equippedItems) {
            if (equipped.getId().equals(excludeTarget.getId()) || !equipped.isEquipped()) {
                continue;
            }
            final Optional<EquipmentItem> equipOpt = query.resolveEquipment(equipped);
            if (equipOpt.isPresent() && equipOpt.get().kind().primarySlot() == primarySlot) {
                equipped.unequip();
            }
        }
    }

    private void clearFromOtherWeaponSet(final CharacterProgress progress, final Long ownedItemId) {
        if (progress == null || ownedItemId == null) {
            return;
        }
        final int otherSet = progress.getActiveWeaponSet() == 1 ? 2 : 1;
        clearWeaponSetSlot(progress, otherSet, ownedItemId);
    }

    private boolean clearWeaponSetSlot(
            final CharacterProgress progress, final int setNumber, final Long ownedItemId) {
        boolean modified = false;
        if (setNumber == 1) {
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon1MainId())) {
                progress.setWeapon1MainId(null);
                modified = true;
            }
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon1OffId())) {
                progress.setWeapon1OffId(null);
                modified = true;
            }
        } else {
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon2MainId())) {
                progress.setWeapon2MainId(null);
                modified = true;
            }
            if (Long.valueOf(ownedItemId).equals(progress.getWeapon2OffId())) {
                progress.setWeapon2OffId(null);
                modified = true;
            }
        }
        return modified;
    }

    private void autoUnequipShieldForTwoHanded(
            final OwnedItem target,
            final EquipmentKind targetKind,
            final List<OwnedItem> equippedItems) {
        if (!targetKind.requiredSlots().contains(EquipSlot.OFF_HAND)
                || targetKind.primarySlot() != EquipSlot.MAIN_HAND) {
            return;
        }
        for (final OwnedItem equipped : equippedItems) {
            if (equipped.getId().equals(target.getId())) {
                continue;
            }
            final Optional<EquipmentItem> equipOpt = query.resolveEquipment(equipped);
            if (equipOpt.isPresent() && equipOpt.get().kind().primarySlot() == EquipSlot.OFF_HAND) {
                equipped.unequip();
            }
        }
    }

    private void autoUnequipTwoHandedForShield(
            final OwnedItem target,
            final EquipmentKind targetKind,
            final List<OwnedItem> equippedItems) {
        if (targetKind.primarySlot() != EquipSlot.OFF_HAND) {
            return;
        }
        for (final OwnedItem equipped : equippedItems) {
            if (equipped.getId().equals(target.getId())) {
                continue;
            }
            final Optional<EquipmentItem> equipOpt = query.resolveEquipment(equipped);
            if (equipOpt.isPresent()
                    && equipOpt.get().kind().primarySlot() == EquipSlot.MAIN_HAND
                    && equipOpt.get().kind().requiredSlots().contains(EquipSlot.OFF_HAND)) {
                equipped.unequip();
            }
        }
    }

    private CharacterProgress resolveProgressForSwap(final CharacterProgress progressParam) {
        if (progressParam != null) {
            return characterProgressRepository
                    .findById(progressParam.getId())
                    .orElse(progressParam);
        }
        return characterProgressRepository.findById(1L).orElse(null);
    }

    private void syncCurrentWeaponSetSlots(
            final CharacterProgress progress,
            final int currentSet,
            final Map<EquipSlot, OwnedItem> currentSlotMap) {
        final Long mainId =
                currentSlotMap.containsKey(EquipSlot.MAIN_HAND)
                        ? currentSlotMap.get(EquipSlot.MAIN_HAND).getId()
                        : null;
        final Long offId =
                currentSlotMap.containsKey(EquipSlot.OFF_HAND)
                        ? currentSlotMap.get(EquipSlot.OFF_HAND).getId()
                        : null;
        if (currentSet == 1) {
            progress.setWeapon1MainId(mainId);
            progress.setWeapon1OffId(offId);
        } else {
            progress.setWeapon2MainId(mainId);
            progress.setWeapon2OffId(offId);
        }
    }

    private Long resolveExistingOwnedId(final Long ownedItemId) {
        if (ownedItemId == null) {
            return null;
        }
        return ownedItemRepository
                .findById(ownedItemId)
                .filter(it -> it.getStorage() == StorageKind.INVENTORY)
                .map(OwnedItem::getId)
                .orElse(null);
    }

    private void unequipWeaponSlots(final Map<EquipSlot, OwnedItem> currentSlotMap) {
        if (currentSlotMap.containsKey(EquipSlot.MAIN_HAND)) {
            currentSlotMap.get(EquipSlot.MAIN_HAND).unequip();
        }
        if (currentSlotMap.containsKey(EquipSlot.OFF_HAND)) {
            currentSlotMap.get(EquipSlot.OFF_HAND).unequip();
        }
    }

    private void equipTargetWeaponSet(final Long targetMainId, final Long targetOffId) {
        boolean mainIsTwoHanded = false;
        if (targetMainId != null) {
            final Optional<OwnedItem> mainOpt = ownedItemRepository.findById(targetMainId);
            if (mainOpt.isPresent() && mainOpt.get().getStorage() == StorageKind.INVENTORY) {
                mainOpt.get().equip();
                final Optional<EquipmentItem> equipOpt = query.resolveEquipment(mainOpt.get());
                if (equipOpt.isPresent()) {
                    mainIsTwoHanded =
                            equipOpt.get().kind().requiredSlots().contains(EquipSlot.OFF_HAND);
                }
            }
        }

        if (targetOffId != null && !mainIsTwoHanded) {
            final Optional<OwnedItem> offOpt = ownedItemRepository.findById(targetOffId);
            if (offOpt.isPresent() && offOpt.get().getStorage() == StorageKind.INVENTORY) {
                offOpt.get().equip();
            }
        }
    }
}
