package com.myapps.web.myrpg.application.service.inventory;

import com.myapps.web.myrpg.application.exception.InventoryFullException;
import com.myapps.web.myrpg.application.service.ItemCatalogService;
import com.myapps.web.myrpg.config.GameProperties;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.EquipSlot;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.OwnedItem;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.StorageKind;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.OwnedItemRepository;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 인벤토리 협력 객체들이 공통으로 사용하는 조회/판별/용량 지원 컴포넌트.
 *
 * <p>카탈로그→장비 해석, 장착 아이템/슬롯 맵 조회, 캐릭터 폴백 조회, 저장소 용량 산정 등 여러 곳에 중복되던 읽기 로직을 단일 지점으로 통합한다. 부수효과 없는 순수
 * 조회만 담당한다.
 */
public final class InventoryQuerySupport {

    private final OwnedItemRepository ownedItemRepository;
    private final ItemCatalogService itemCatalogService;
    private final CharacterProgressRepository characterProgressRepository;
    private final GameProperties gameProperties;

    /**
     * InventoryQuerySupport를 생성한다.
     *
     * @param ownedItemRepository 보유 아이템 저장소
     * @param itemCatalogService 아이템 카탈로그 서비스
     * @param characterProgressRepository 캐릭터 진행상황 저장소
     * @param gameProperties 게임 프로퍼티 설정
     */
    public InventoryQuerySupport(
            final OwnedItemRepository ownedItemRepository,
            final ItemCatalogService itemCatalogService,
            final CharacterProgressRepository characterProgressRepository,
            final GameProperties gameProperties) {
        this.ownedItemRepository = ownedItemRepository;
        this.itemCatalogService = itemCatalogService;
        this.characterProgressRepository = characterProgressRepository;
        this.gameProperties = gameProperties;
    }

    /**
     * 보유 아이템으로부터 장비 아이템 정보를 안전하게 해석한다.
     *
     * @param owned 보유 아이템
     * @return 장비 아이템 Optional (장비가 아니거나 카탈로그에 없으면 empty)
     */
    public Optional<EquipmentItem> resolveEquipment(final OwnedItem owned) {
        if (owned == null) {
            return Optional.empty();
        }
        return itemCatalogService
                .byId(owned.getItemId())
                .filter(EquipmentItem.class::isInstance)
                .map(EquipmentItem.class::cast);
    }

    /**
     * 보유 아이템으로부터 장비 아이템 정보를 조회하며, 없거나 장비가 아니면 예외를 던진다.
     *
     * @param owned 보유 아이템
     * @return 장비 아이템 정보
     * @throws IllegalStateException 카탈로그에 없거나 장비가 아닌 경우
     */
    public EquipmentItem resolveEquipmentOrThrow(final OwnedItem owned) {
        final Item catalogItem =
                itemCatalogService
                        .byId(owned.getItemId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "카탈로그에 아이템이 없습니다: " + owned.getItemId()));
        if (!(catalogItem instanceof EquipmentItem equipItem)) {
            throw new IllegalStateException("장비가 아닌 아이템입니다: " + owned.getItemId());
        }
        return equipItem;
    }

    /**
     * 보유 아이템 식별자로 아이템을 조회하며, 없으면 예외를 던진다.
     *
     * @param ownedItemId 보유 아이템 식별자
     * @return 보유 아이템 엔티티
     * @throws IllegalStateException 아이템을 찾을 수 없는 경우
     */
    public OwnedItem findOwnedItemOrThrow(final long ownedItemId) {
        return ownedItemRepository
                .findById(ownedItemId)
                .orElseThrow(() -> new IllegalStateException("보유 아이템을 찾을 수 없습니다: " + ownedItemId));
    }

    /**
     * 캐릭터의 인벤토리 내 장착 중인 아이템 목록을 조회한다.
     *
     * @param characterId 캐릭터 식별자 (null인 경우 1차 폴백 조회)
     * @return 장착 중인 보유 아이템 목록
     */
    public List<OwnedItem> findEquippedInventoryItems(final Long characterId) {
        List<OwnedItem> items = null;
        if (characterId != null) {
            items =
                    ownedItemRepository.findByCharacterIdAndStorageAndEquippedTrue(
                            characterId, StorageKind.INVENTORY);
        }
        if (items == null) {
            items = ownedItemRepository.findByStorageAndEquippedTrue(StorageKind.INVENTORY);
        }
        return items != null ? items : List.of();
    }

    /**
     * 장착 중인 아이템 목록을 장착 슬롯별 매핑으로 변환한다.
     *
     * @param equippedItems 장착 중인 아이템 목록
     * @return 슬롯별 보유 아이템 맵
     */
    public Map<EquipSlot, OwnedItem> resolveEquippedSlotMap(final List<OwnedItem> equippedItems) {
        final Map<EquipSlot, OwnedItem> slotMap = new EnumMap<>(EquipSlot.class);
        for (final OwnedItem owned : equippedItems) {
            final Optional<Item> catalogOpt = itemCatalogService.byId(owned.getItemId());
            if (catalogOpt.isPresent() && catalogOpt.get() instanceof EquipmentItem equipItem) {
                slotMap.put(equipItem.kind().primarySlot(), owned);
            }
        }
        return slotMap;
    }

    /**
     * 비활성 무기 세트에 배정된 아이템 식별자 집합을 조회한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 비활성 무기 세트에 할당된 아이템 ID 집합
     */
    public Set<Long> resolveOtherSetAssignedItemIds(final Long characterId) {
        final CharacterProgress progress = findCharacterSafe(characterId);
        if (progress == null) {
            return Set.of();
        }
        final int otherSet = progress.getActiveWeaponSet() == 1 ? 2 : 1;
        final Long mainId =
                otherSet == 1 ? progress.getWeapon1MainId() : progress.getWeapon2MainId();
        final Long offId = otherSet == 1 ? progress.getWeapon1OffId() : progress.getWeapon2OffId();

        final Set<Long> ids = new HashSet<>();
        if (mainId != null) {
            ids.add(mainId);
        }
        if (offId != null) {
            ids.add(offId);
        }
        return Set.copyOf(ids);
    }

    /**
     * 캐릭터 정보를 안전하게 조회하며, 미지정 또는 미존재 시 첫 번째 캐릭터로 폴백한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 캐릭터 진행상황 엔티티 (전체 미존재 시 null)
     */
    public CharacterProgress findCharacterSafe(final Long characterId) {
        if (characterId != null) {
            return characterProgressRepository
                    .findById(characterId)
                    .or(characterProgressRepository::findFirstByOrderByIdAsc)
                    .orElse(null);
        }
        return characterProgressRepository.findFirstByOrderByIdAsc().orElse(null);
    }

    /**
     * 캐릭터 정보를 조회하며, 없으면 첫 번째 캐릭터로 폴백하고, 전체 미존재 시 예외를 던진다.
     *
     * @param characterId 캐릭터 식별자
     * @return 캐릭터 진행상황 엔티티
     * @throws IllegalStateException 캐릭터가 존재하지 않는 경우
     */
    public CharacterProgress loadCharacter(final Long characterId) {
        if (characterId != null) {
            return characterProgressRepository.findById(characterId).orElseGet(this::loadCharacter);
        }
        return loadCharacter();
    }

    /**
     * 기본(첫 번째) 캐릭터 정보를 조회하며, 없으면 예외를 던진다.
     *
     * @return 캐릭터 진행상황 엔티티
     * @throws IllegalStateException 캐릭터가 존재하지 않는 경우
     */
    public CharacterProgress loadCharacter() {
        return characterProgressRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("캐릭터 진행상황을 찾을 수 없습니다."));
    }

    /**
     * 특정 저장소의 아이템 개수를 반환한다.
     *
     * @param characterId 캐릭터 식별자
     * @param storage 저장소 종류
     * @return 저장된 아이템 개수
     */
    public long countStorage(final Long characterId, final StorageKind storage) {
        if (characterId != null) {
            return ownedItemRepository.countByCharacterIdAndStorage(characterId, storage);
        }
        return ownedItemRepository.countByStorage(storage);
    }

    /**
     * 특정 캐릭터의 저장소 용량을 확인하고 초과 시 예외를 던진다.
     *
     * @param characterId 캐릭터 식별자
     * @param storage 저장소 종류
     * @throws InventoryFullException 용량 초과 시
     */
    public void checkCapacity(final Long characterId, final StorageKind storage) {
        final long currentCount = countStorage(characterId, storage);
        if (currentCount >= maxCapacity()) {
            throw new InventoryFullException(
                    storage.name() + " 용량(" + maxCapacity() + ")을 초과했습니다.");
        }
    }

    /**
     * 특정 캐릭터의 인벤토리가 가득 찼는지 확인한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 가득 찼으면 {@code true}
     */
    public boolean isInventoryFull(final Long characterId) {
        return countStorage(characterId, StorageKind.INVENTORY) >= maxCapacity();
    }

    /**
     * 인벤토리 최대 슬롯 용량을 반환한다.
     *
     * @return 최대 슬롯 수
     */
    public int maxCapacity() {
        return gameProperties.inventory().maxSlots();
    }

    /**
     * 아이템의 최대 내구도를 확인한다.
     *
     * @param itemId 아이템 식별자
     * @return 최대 내구도
     */
    public int resolveMaxDurability(final String itemId) {
        final Optional<Item> catalogOpt = itemCatalogService.byId(itemId);
        if (catalogOpt.isPresent() && catalogOpt.get() instanceof EquipmentItem equipItem) {
            return equipItem.maxDurability();
        }
        return gameProperties.inventory().equipmentMaxDurability();
    }

    /**
     * 저장소에서 특정 아이템을 조회하며 캐릭터 폴백을 적용한다.
     *
     * @param characterId 캐릭터 식별자
     * @param storage 저장소 종류
     * @param itemId 아이템 식별자
     * @return 아이템 Optional
     */
    public Optional<OwnedItem> findStorageItem(
            final Long characterId, final StorageKind storage, final String itemId) {
        if (characterId != null) {
            final Optional<OwnedItem> item =
                    ownedItemRepository.findByCharacterIdAndStorageAndItemId(
                            characterId, storage, itemId);
            if (item != null) {
                return item;
            }
        }
        final Optional<OwnedItem> fallback =
                ownedItemRepository.findByStorageAndItemId(storage, itemId);
        return fallback != null ? fallback : Optional.empty();
    }

    /**
     * 특정 캐릭터의 현재 장착 중인 무기의 재능 분류를 반환한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 장착 중인 무기의 재능 분류 (MELEE, ARCHERY, MAGIC, 또는 null)
     */
    public SkillTalent resolveEquippedWeaponTalent(final Long characterId) {
        final List<OwnedItem> equippedItems = findEquippedInventoryItems(characterId);

        for (final OwnedItem equipped : equippedItems) {
            final Optional<Item> catalogOpt = itemCatalogService.byId(equipped.getItemId());
            if (catalogOpt.isEmpty()) {
                continue;
            }
            final Item catalogItem = catalogOpt.get();
            if (catalogItem instanceof EquipmentItem equipItem
                    && catalogItem.type() == ItemType.WEAPON) {
                return mapKindToSkillTalent(equipItem.kind());
            }
        }
        return null;
    }

    /**
     * 장비 종류를 스킬 재능 분류로 매핑한다.
     *
     * @param kind 장비 종류
     * @return 대응하는 스킬 재능 분류
     */
    public SkillTalent mapKindToSkillTalent(final EquipmentKind kind) {
        return switch (kind) {
            case ONE_HANDED_SWORD, TWO_HANDED_SWORD -> SkillTalent.MELEE;
            case BOW -> SkillTalent.ARCHERY;
            case WAND, STAFF -> SkillTalent.MAGIC;
            default -> SkillTalent.MELEE;
        };
    }

    /**
     * 캐릭터 식별자를 기본값(1L)으로 정규화한다.
     *
     * @param characterId 캐릭터 식별자
     * @return 정규화된 캐릭터 식별자 (null인 경우 1L)
     */
    public Long normalizeCharacterId(final Long characterId) {
        return characterId != null ? characterId : 1L;
    }
}
