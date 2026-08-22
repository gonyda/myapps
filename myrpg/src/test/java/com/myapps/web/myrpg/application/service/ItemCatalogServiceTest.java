package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.EquipmentKind;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.PotionItem;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * ItemCatalogService 단위 테스트.
 *
 * <p>신규 추가된 숏소드({@code short_sword})·롱소드({@code long_sword})의 속성 및 기존 포션·초보자 장비의 카탈로그 로드 상태를 검증한다.
 *
 * <p><b>Validates: Requirements 14.1, 14.2, 14.3, 14.4</b>
 */
class ItemCatalogServiceTest {

    private ItemCatalogService itemCatalogService;

    @BeforeEach
    void setUp() {
        final ObjectMapper objectMapper = new ObjectMapper();
        itemCatalogService = new ItemCatalogService(objectMapper);
        itemCatalogService.init();
    }

    /** 숏소드(short_sword) 카탈로그 데이터가 스펙 정의와 일치하는지 검증한다. */
    @Test
    void should_loadShortSwordCorrectly_when_catalogInitialized() {
        final Optional<Item> itemOpt = itemCatalogService.byId("short_sword");

        assertThat(itemOpt).isPresent();
        final Item item = itemOpt.get();
        assertThat(item.id()).isEqualTo("short_sword");
        assertThat(item.name()).isEqualTo("숏소드");
        assertThat(item.type()).isEqualTo(ItemType.WEAPON);
        assertThat(item.buyPrice()).isEqualTo(300);

        assertThat(item).isInstanceOf(EquipmentItem.class);
        final EquipmentItem equipment = (EquipmentItem) item;
        assertThat(equipment.kind()).isEqualTo(EquipmentKind.ONE_HANDED_SWORD);
        assertThat(equipment.maxDurability()).isEqualTo(15);
        assertThat(equipment.bonuses()).containsExactly(new EquipBonus(BonusTarget.STR, 8));
    }

    /** 롱소드(long_sword) 카탈로그 데이터가 스펙 정의와 일치하는지 검증한다. */
    @Test
    void should_loadLongSwordCorrectly_when_catalogInitialized() {
        final Optional<Item> itemOpt = itemCatalogService.byId("long_sword");

        assertThat(itemOpt).isPresent();
        final Item item = itemOpt.get();
        assertThat(item.id()).isEqualTo("long_sword");
        assertThat(item.name()).isEqualTo("롱소드");
        assertThat(item.type()).isEqualTo(ItemType.WEAPON);
        assertThat(item.buyPrice()).isEqualTo(700);

        assertThat(item).isInstanceOf(EquipmentItem.class);
        final EquipmentItem equipment = (EquipmentItem) item;
        assertThat(equipment.kind()).isEqualTo(EquipmentKind.ONE_HANDED_SWORD);
        assertThat(equipment.maxDurability()).isEqualTo(15);
        assertThat(equipment.bonuses()).containsExactly(new EquipBonus(BonusTarget.STR, 12));
    }

    /** 한손검 티어(초보자용 < 숏소드 < 롱소드)의 STR 보너스 순서를 검증한다. */
    @Test
    void should_satisfyOneHandedSwordTierProgression() {
        final EquipmentItem beginner =
                (EquipmentItem) itemCatalogService.byId("beginner_one_hand_sword").orElseThrow();
        final EquipmentItem shortSword =
                (EquipmentItem) itemCatalogService.byId("short_sword").orElseThrow();
        final EquipmentItem longSword =
                (EquipmentItem) itemCatalogService.byId("long_sword").orElseThrow();

        final int beginnerStr = beginner.bonuses().getFirst().amount();
        final int shortSwordStr = shortSword.bonuses().getFirst().amount();
        final int longSwordStr = longSword.bonuses().getFirst().amount();

        assertThat(beginnerStr).isEqualTo(5);
        assertThat(shortSwordStr).isEqualTo(8);
        assertThat(longSwordStr).isEqualTo(12);
        assertThat(beginnerStr).isLessThan(shortSwordStr);
        assertThat(shortSwordStr).isLessThan(longSwordStr);

        // 초보자용은 buyPrice가 없고, 숏소드/롱소드는 buyPrice가 존재함을 검증
        assertThat(beginner.buyPrice()).isNull();
        assertThat(shortSword.buyPrice()).isEqualTo(300);
        assertThat(longSword.buyPrice()).isEqualTo(700);
    }

    /** 기존 hp_potion_30의 buyPrice가 50으로 유지되는지 검증한다. */
    @Test
    void should_preserveHpPotionBuyPrice() {
        final Optional<Item> potionOpt = itemCatalogService.byId("hp_potion_30");

        assertThat(potionOpt).isPresent();
        final Item item = potionOpt.get();
        assertThat(item.buyPrice()).isEqualTo(50);
        assertThat(item).isInstanceOf(PotionItem.class);
        final PotionItem potion = (PotionItem) item;
        assertThat(potion.healHp()).isEqualTo(30);
        assertThat(potion.healMp()).isEqualTo(0);
        assertThat(potion.healStamina()).isEqualTo(0);
    }

    /** 신규 mp_potion_30의 로드 상태 및 회복량을 검증한다. */
    @Test
    void should_loadMpPotionCorrectly() {
        final Optional<Item> potionOpt = itemCatalogService.byId("mp_potion_30");

        assertThat(potionOpt).isPresent();
        final Item item = potionOpt.get();
        assertThat(item.id()).isEqualTo("mp_potion_30");
        assertThat(item.name()).isEqualTo("마나 30 포션");
        assertThat(item.buyPrice()).isEqualTo(50);
        assertThat(item).isInstanceOf(PotionItem.class);
        final PotionItem potion = (PotionItem) item;
        assertThat(potion.healHp()).isEqualTo(0);
        assertThat(potion.healMp()).isEqualTo(30);
        assertThat(potion.healStamina()).isEqualTo(0);
    }

    /** 신규 stamina_potion_30의 로드 상태 및 회복량을 검증한다. */
    @Test
    void should_loadStaminaPotionCorrectly() {
        final Optional<Item> potionOpt = itemCatalogService.byId("stamina_potion_30");

        assertThat(potionOpt).isPresent();
        final Item item = potionOpt.get();
        assertThat(item.id()).isEqualTo("stamina_potion_30");
        assertThat(item.name()).isEqualTo("스태미나 30 포션");
        assertThat(item.buyPrice()).isEqualTo(50);
        assertThat(item).isInstanceOf(PotionItem.class);
        final PotionItem potion = (PotionItem) item;
        assertThat(potion.healHp()).isEqualTo(0);
        assertThat(potion.healMp()).isEqualTo(0);
        assertThat(potion.healStamina()).isEqualTo(30);
    }
}
