package com.myapps.web.myrpg.application.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.application.exception.ItemDataException;
import com.myapps.web.myrpg.domain.model.BonusTarget;
import com.myapps.web.myrpg.domain.model.EquipBonus;
import com.myapps.web.myrpg.domain.model.EquipmentItem;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.PotionItem;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 아이템 카탈로그 파싱 검증 프로퍼티 테스트.
 *
 * <p>유효한 아이템 JSON 입력은 올바른 크기의 불변 목록을 반환하고,
 * 결함(미지 type/kind/target, 중복 id, 필수 필드 누락, 장비 maxDurability 누락)이
 * 주입된 입력은 {@link ItemDataException}을 던짐을 검증한다.
 *
 * <p>Feature: 006-gold-item-inventory, Property 4: 아이템 카탈로그 검증
 *
 * <p><b>Validates: Requirements 5.2, 5.4, 5.5, 5.6, 5.7, 5.8</b>
 */
class ItemCatalogParsingPropertyTest {

    private static final int MAX_ITEM_COUNT = 5;
    private static final int ID_MIN_LENGTH = 3;
    private static final int ID_MAX_LENGTH = 12;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 8;
    private static final int HEAL_HP_MIN = 10;
    private static final int HEAL_HP_MAX = 200;
    private static final int MAX_DURABILITY_MIN = 5;
    private static final int MAX_DURABILITY_MAX = 50;
    private static final int BONUS_AMOUNT_MIN = 1;
    private static final int BONUS_AMOUNT_MAX = 30;
    private static final int BUY_PRICE_MIN = 10;
    private static final int BUY_PRICE_MAX = 500;

    private static final String[] VALID_EQUIPMENT_TYPES = {"weapon", "armor"};
    private static final String[] VALID_EQUIPMENT_KINDS = {
            "one_handed_sword", "two_handed_sword", "shield", "armor_body"};
    private static final String[] VALID_BONUS_TARGETS = {
            "STR", "DEX", "INT", "CRITICAL", "DEF", "HP", "MP", "STAMINA"};
    private static final String[] REQUIRED_FIELDS = {"id", "name", "type"};

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ItemCatalogService service = new ItemCatalogService(objectMapper);

    /**
     * 유효한 아이템 JSON 입력을 파싱하면 올바른 크기의 불변 목록을 반환한다.
     *
     * @param dataset 임의 생성된 유효 아이템 데이터셋
     */
    @Property(tries = 100)
    void should_returnImmutableListWithCorrectSize_when_validInput(
            @ForAll("validItemDataset") final List<ItemInputData> dataset) {
        final String json = serializeToJson(dataset);
        final InputStream inputStream = toInputStream(json);

        final List<Item> result = service.loadFromStream(inputStream);

        assertThat(result).hasSize(dataset.size());
        assertThatThrownBy(() -> result.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * 미지 type 문자열이 포함된 입력은 {@link ItemDataException}을 던진다.
     *
     * @param dataset 유효 데이터셋 (결함 주입 전)
     * @param invalidType 미지 type 문자열
     */
    @Property(tries = 100)
    void should_throwItemDataException_when_unknownType(
            @ForAll("validItemDataset") final List<ItemInputData> dataset,
            @ForAll("invalidTypeString") final String invalidType) {
        if (dataset.isEmpty()) {
            return;
        }
        final List<ItemInputData> defective = injectUnknownType(dataset, invalidType);
        final String json = serializeToJson(defective);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> service.loadFromStream(inputStream))
                .isInstanceOf(ItemDataException.class);
    }

    /**
     * 장비의 미지 kind 문자열이 포함된 입력은 {@link ItemDataException}을 던진다.
     *
     * @param dataset 유효 장비 데이터셋 (결함 주입 전)
     * @param invalidKind 미지 kind 문자열
     */
    @Property(tries = 100)
    void should_throwItemDataException_when_unknownKind(
            @ForAll("validEquipmentDataset") final List<ItemInputData> dataset,
            @ForAll("invalidKindString") final String invalidKind) {
        if (dataset.isEmpty()) {
            return;
        }
        final List<ItemInputData> defective = injectUnknownKind(dataset, invalidKind);
        final String json = serializeToJson(defective);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> service.loadFromStream(inputStream))
                .isInstanceOf(ItemDataException.class);
    }

    /**
     * 장비의 미지 bonuses.target 문자열이 포함된 입력은 {@link ItemDataException}을 던진다.
     *
     * @param dataset 유효 장비 데이터셋 (결함 주입 전)
     * @param invalidTarget 미지 target 문자열
     */
    @Property(tries = 100)
    void should_throwItemDataException_when_unknownBonusTarget(
            @ForAll("validEquipmentDataset") final List<ItemInputData> dataset,
            @ForAll("invalidTargetString") final String invalidTarget) {
        if (dataset.isEmpty()) {
            return;
        }
        final List<ItemInputData> defective = injectUnknownBonusTarget(dataset, invalidTarget);
        final String json = serializeToJson(defective);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> service.loadFromStream(inputStream))
                .isInstanceOf(ItemDataException.class);
    }

    /**
     * 중복 id가 포함된 입력은 {@link ItemDataException}을 던진다.
     *
     * @param dataset 유효 데이터셋 (2개 이상)
     */
    @Property(tries = 100)
    void should_throwItemDataException_when_duplicateId(
            @ForAll("validItemDatasetAtLeastTwo") final List<ItemInputData> dataset) {
        final List<ItemInputData> defective = injectDuplicateId(dataset);
        final String json = serializeToJson(defective);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> service.loadFromStream(inputStream))
                .isInstanceOf(ItemDataException.class);
    }

    /**
     * 필수 필드(id/name/type)가 누락된 입력은 {@link ItemDataException}을 던진다.
     *
     * @param dataset 유효 데이터셋 (결함 주입 전)
     * @param fieldToRemove 누락시킬 필드명
     */
    @Property(tries = 100)
    void should_throwItemDataException_when_requiredFieldMissing(
            @ForAll("validItemDataset") final List<ItemInputData> dataset,
            @ForAll("requiredFieldName") final String fieldToRemove) {
        if (dataset.isEmpty()) {
            return;
        }
        final String json = serializeToJsonWithMissingField(dataset, fieldToRemove);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> service.loadFromStream(inputStream))
                .isInstanceOf(ItemDataException.class);
    }

    /**
     * 장비에 maxDurability가 누락된 입력은 {@link ItemDataException}을 던진다.
     *
     * @param dataset 유효 장비 데이터셋 (결함 주입 전)
     */
    @Property(tries = 100)
    void should_throwItemDataException_when_equipmentMissingMaxDurability(
            @ForAll("validEquipmentDataset") final List<ItemInputData> dataset) {
        if (dataset.isEmpty()) {
            return;
        }
        final String json = serializeToJsonWithoutMaxDurability(dataset);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> service.loadFromStream(inputStream))
                .isInstanceOf(ItemDataException.class);
    }

    /**
     * buyPrice가 누락된 유효 입력을 파싱하면 결과의 buyPrice가 null이다.
     *
     * @param dataset 유효 아이템 데이터셋 (buyPrice 없음)
     */
    @Property(tries = 100)
    void should_haveBuyPriceNull_when_buyPriceMissing(
            @ForAll("validItemDatasetWithoutBuyPrice") final List<ItemInputData> dataset) {
        final String json = serializeToJson(dataset);
        final InputStream inputStream = toInputStream(json);

        final List<Item> result = service.loadFromStream(inputStream);

        for (final Item item : result) {
            assertThat(item.buyPrice()).isNull();
        }
    }

    /**
     * bonuses가 누락된 장비 입력을 파싱하면 결과의 bonuses가 빈 목록이다.
     *
     * @param dataset 유효 장비 데이터셋 (bonuses 없음)
     */
    @Property(tries = 100)
    void should_haveEmptyBonuses_when_bonusesMissing(
            @ForAll("validEquipmentDatasetWithoutBonuses") final List<ItemInputData> dataset) {
        final String json = serializeToJsonWithoutBonuses(dataset);
        final InputStream inputStream = toInputStream(json);

        final List<Item> result = service.loadFromStream(inputStream);

        for (final Item item : result) {
            if (item instanceof EquipmentItem equipmentItem) {
                assertThat(equipmentItem.bonuses()).isEmpty();
            }
        }
    }

    // ── Providers ──

    /**
     * 유효한 아이템 데이터셋(포션+장비 혼합, 유일 id)을 생성한다.
     *
     * @return 유효 아이템 데이터셋 Arbitrary
     */
    @Provide
    Arbitrary<List<ItemInputData>> validItemDataset() {
        return Arbitraries.integers().between(1, MAX_ITEM_COUNT)
                .flatMap(this::buildUniqueItemList);
    }

    /**
     * 최소 2개 아이템을 포함하는 유효 데이터셋(중복 id 테스트용).
     *
     * @return 유효 아이템 데이터셋 Arbitrary (최소 2개)
     */
    @Provide
    Arbitrary<List<ItemInputData>> validItemDatasetAtLeastTwo() {
        return Arbitraries.integers().between(2, MAX_ITEM_COUNT)
                .flatMap(this::buildUniqueItemList);
    }

    /**
     * 장비만 포함하는 유효 데이터셋을 생성한다.
     *
     * @return 유효 장비 데이터셋 Arbitrary
     */
    @Provide
    Arbitrary<List<ItemInputData>> validEquipmentDataset() {
        return Arbitraries.integers().between(1, MAX_ITEM_COUNT)
                .flatMap(this::buildUniqueEquipmentList);
    }

    /**
     * buyPrice가 없는 유효 아이템 데이터셋을 생성한다.
     *
     * @return buyPrice 없는 유효 아이템 데이터셋 Arbitrary
     */
    @Provide
    Arbitrary<List<ItemInputData>> validItemDatasetWithoutBuyPrice() {
        return Arbitraries.integers().between(1, MAX_ITEM_COUNT)
                .flatMap(this::buildUniqueItemListWithoutBuyPrice);
    }

    /**
     * bonuses가 없는 장비 데이터셋을 생성한다.
     *
     * @return bonuses 없는 장비 데이터셋 Arbitrary
     */
    @Provide
    Arbitrary<List<ItemInputData>> validEquipmentDatasetWithoutBonuses() {
        return Arbitraries.integers().between(1, MAX_ITEM_COUNT)
                .flatMap(this::buildUniqueEquipmentList);
    }

    /**
     * 유효하지 않은 type 문자열을 생성한다.
     *
     * @return 미지 type 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> invalidTypeString() {
        return Arbitraries.of("invalid", "UNKNOWN", "shield", "food", "material");
    }

    /**
     * 유효하지 않은 kind 문자열을 생성한다.
     *
     * @return 미지 kind 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> invalidKindString() {
        return Arbitraries.of("invalid_kind", "UNKNOWN", "dagger", "spear", "crossbow");
    }

    /**
     * 유효하지 않은 bonuses.target 문자열을 생성한다.
     *
     * @return 미지 target 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> invalidTargetString() {
        return Arbitraries.of("INVALID", "UNKNOWN", "AGI", "LUCK", "SPEED");
    }

    /**
     * 필수 필드명 중 하나를 임의로 선택한다.
     *
     * @return 필수 필드명 Arbitrary
     */
    @Provide
    Arbitrary<String> requiredFieldName() {
        return Arbitraries.of(REQUIRED_FIELDS);
    }

    // ── Defect Injection ──

    private List<ItemInputData> injectUnknownType(final List<ItemInputData> dataset,
                                                   final String invalidType) {
        final List<ItemInputData> result = new ArrayList<>(dataset);
        final ItemInputData first = result.getFirst();
        result.set(0, new ItemInputData(
                first.id(), first.name(), invalidType, first.kind(),
                first.bonusTarget(), first.bonusAmount(), first.healHp(),
                first.maxDurability(), first.buyPrice()));
        return List.copyOf(result);
    }

    private List<ItemInputData> injectUnknownKind(final List<ItemInputData> dataset,
                                                   final String invalidKind) {
        final List<ItemInputData> result = new ArrayList<>(dataset);
        final ItemInputData first = result.getFirst();
        result.set(0, new ItemInputData(
                first.id(), first.name(), first.type(), invalidKind,
                first.bonusTarget(), first.bonusAmount(), first.healHp(),
                first.maxDurability(), first.buyPrice()));
        return List.copyOf(result);
    }

    private List<ItemInputData> injectUnknownBonusTarget(final List<ItemInputData> dataset,
                                                          final String invalidTarget) {
        final List<ItemInputData> result = new ArrayList<>(dataset);
        final ItemInputData first = result.getFirst();
        result.set(0, new ItemInputData(
                first.id(), first.name(), first.type(), first.kind(),
                invalidTarget, first.bonusAmount(), first.healHp(),
                first.maxDurability(), first.buyPrice()));
        return List.copyOf(result);
    }

    private List<ItemInputData> injectDuplicateId(final List<ItemInputData> dataset) {
        final List<ItemInputData> result = new ArrayList<>(dataset);
        final ItemInputData first = result.getFirst();
        final ItemInputData second = result.get(1);
        result.set(1, new ItemInputData(
                first.id(), second.name(), second.type(), second.kind(),
                second.bonusTarget(), second.bonusAmount(), second.healHp(),
                second.maxDurability(), second.buyPrice()));
        return List.copyOf(result);
    }

    // ── Serialization ──

    private String serializeToJson(final List<ItemInputData> dataset) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        for (final ItemInputData data : dataset) {
            final ObjectNode node = buildItemNode(data);
            rootArray.add(node);
        }
        return rootArray.toString();
    }

    private String serializeToJsonWithMissingField(final List<ItemInputData> dataset,
                                                    final String fieldToRemove) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        for (int i = 0; i < dataset.size(); i++) {
            final ItemInputData data = dataset.get(i);
            final ObjectNode node = buildItemNode(data);
            if (i == 0) {
                node.remove(fieldToRemove);
            }
            rootArray.add(node);
        }
        return rootArray.toString();
    }

    private String serializeToJsonWithoutMaxDurability(final List<ItemInputData> dataset) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        for (int i = 0; i < dataset.size(); i++) {
            final ItemInputData data = dataset.get(i);
            final ObjectNode node = buildItemNode(data);
            if (i == 0) {
                node.remove("maxDurability");
            }
            rootArray.add(node);
        }
        return rootArray.toString();
    }

    private String serializeToJsonWithoutBonuses(final List<ItemInputData> dataset) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        for (final ItemInputData data : dataset) {
            final ObjectNode node = buildItemNode(data);
            node.remove("bonuses");
            rootArray.add(node);
        }
        return rootArray.toString();
    }

    private ObjectNode buildItemNode(final ItemInputData data) {
        final ObjectNode node = objectMapper.createObjectNode();
        node.put("id", data.id());
        node.put("name", data.name());
        node.put("type", data.type());

        if ("potion".equals(data.type())) {
            node.put("healHp", data.healHp());
        } else {
            node.put("kind", data.kind());
            node.put("maxDurability", data.maxDurability());
            if (data.bonusTarget() != null) {
                final ArrayNode bonusesArray = node.putArray("bonuses");
                final ObjectNode bonusNode = bonusesArray.addObject();
                bonusNode.put("target", data.bonusTarget());
                bonusNode.put("amount", data.bonusAmount());
            }
        }

        if (data.buyPrice() != null) {
            node.put("buyPrice", data.buyPrice());
        }

        return node;
    }

    // ── List Builders ──

    private Arbitrary<List<ItemInputData>> buildUniqueItemList(final int count) {
        return mixedItemInputDataArbitrary()
                .list().ofSize(count)
                .map(this::ensureUniqueIds);
    }

    private Arbitrary<List<ItemInputData>> buildUniqueEquipmentList(final int count) {
        return equipmentInputDataArbitrary()
                .list().ofSize(count)
                .map(this::ensureUniqueIds);
    }

    private Arbitrary<List<ItemInputData>> buildUniqueItemListWithoutBuyPrice(final int count) {
        return mixedItemInputDataWithoutBuyPriceArbitrary()
                .list().ofSize(count)
                .map(this::ensureUniqueIds);
    }

    private List<ItemInputData> ensureUniqueIds(final List<ItemInputData> rawList) {
        final List<ItemInputData> result = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            final ItemInputData original = rawList.get(i);
            final String uniqueId = original.id() + "_" + i;
            result.add(new ItemInputData(
                    uniqueId, original.name(), original.type(), original.kind(),
                    original.bonusTarget(), original.bonusAmount(), original.healHp(),
                    original.maxDurability(), original.buyPrice()));
        }
        return List.copyOf(result);
    }

    // ── Arbitrary Factories ──

    private Arbitrary<ItemInputData> mixedItemInputDataArbitrary() {
        return Arbitraries.oneOf(potionInputDataArbitrary(), equipmentInputDataArbitrary());
    }

    private Arbitrary<ItemInputData> mixedItemInputDataWithoutBuyPriceArbitrary() {
        return Arbitraries.oneOf(
                potionInputDataWithoutBuyPriceArbitrary(),
                equipmentInputDataWithoutBuyPriceArbitrary());
    }

    private Arbitrary<ItemInputData> potionInputDataArbitrary() {
        final Arbitrary<String> ids = Arbitraries.strings()
                .alpha().ofMinLength(ID_MIN_LENGTH).ofMaxLength(ID_MAX_LENGTH);
        final Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(NAME_MIN_LENGTH).ofMaxLength(NAME_MAX_LENGTH);
        final Arbitrary<Integer> healHps = Arbitraries.integers()
                .between(HEAL_HP_MIN, HEAL_HP_MAX);
        final Arbitrary<Integer> buyPrices = Arbitraries.integers()
                .between(BUY_PRICE_MIN, BUY_PRICE_MAX);

        return Combinators.combine(ids, names, healHps, buyPrices)
                .as((id, name, healHp, buyPrice) -> new ItemInputData(
                        id, name, "potion", null,
                        null, 0, healHp, 0, buyPrice));
    }

    private Arbitrary<ItemInputData> potionInputDataWithoutBuyPriceArbitrary() {
        final Arbitrary<String> ids = Arbitraries.strings()
                .alpha().ofMinLength(ID_MIN_LENGTH).ofMaxLength(ID_MAX_LENGTH);
        final Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(NAME_MIN_LENGTH).ofMaxLength(NAME_MAX_LENGTH);
        final Arbitrary<Integer> healHps = Arbitraries.integers()
                .between(HEAL_HP_MIN, HEAL_HP_MAX);

        return Combinators.combine(ids, names, healHps)
                .as((id, name, healHp) -> new ItemInputData(
                        id, name, "potion", null,
                        null, 0, healHp, 0, null));
    }

    private Arbitrary<ItemInputData> equipmentInputDataArbitrary() {
        final Arbitrary<String> ids = Arbitraries.strings()
                .alpha().ofMinLength(ID_MIN_LENGTH).ofMaxLength(ID_MAX_LENGTH);
        final Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(NAME_MIN_LENGTH).ofMaxLength(NAME_MAX_LENGTH);
        final Arbitrary<String> types = Arbitraries.of(VALID_EQUIPMENT_TYPES);
        final Arbitrary<String> kinds = Arbitraries.of(VALID_EQUIPMENT_KINDS);
        final Arbitrary<String> targets = Arbitraries.of(VALID_BONUS_TARGETS);
        final Arbitrary<Integer> amounts = Arbitraries.integers()
                .between(BONUS_AMOUNT_MIN, BONUS_AMOUNT_MAX);
        final Arbitrary<Integer> durabilities = Arbitraries.integers()
                .between(MAX_DURABILITY_MIN, MAX_DURABILITY_MAX);
        final Arbitrary<Integer> buyPrices = Arbitraries.integers()
                .between(BUY_PRICE_MIN, BUY_PRICE_MAX);

        return Combinators.combine(ids, names, types, kinds, targets, amounts, durabilities, buyPrices)
                .as((id, name, type, kind, target, amount, durability, buyPrice) ->
                        new ItemInputData(id, name, type, kind, target, amount, 0, durability, buyPrice));
    }

    private Arbitrary<ItemInputData> equipmentInputDataWithoutBuyPriceArbitrary() {
        final Arbitrary<String> ids = Arbitraries.strings()
                .alpha().ofMinLength(ID_MIN_LENGTH).ofMaxLength(ID_MAX_LENGTH);
        final Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(NAME_MIN_LENGTH).ofMaxLength(NAME_MAX_LENGTH);
        final Arbitrary<String> types = Arbitraries.of(VALID_EQUIPMENT_TYPES);
        final Arbitrary<String> kinds = Arbitraries.of(VALID_EQUIPMENT_KINDS);
        final Arbitrary<Integer> durabilities = Arbitraries.integers()
                .between(MAX_DURABILITY_MIN, MAX_DURABILITY_MAX);

        return Combinators.combine(ids, names, types, kinds, durabilities)
                .as((id, name, type, kind, durability) ->
                        new ItemInputData(id, name, type, kind, null, 0, 0, durability, null));
    }

    private InputStream toInputStream(final String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 프로퍼티 테스트용 아이템 입력 데이터 레코드.
     *
     * @param id            아이템 고유 식별자
     * @param name          표시용 이름
     * @param type          아이템 타입 문자열
     * @param kind          장비 종류 문자열 (포션이면 null)
     * @param bonusTarget   보너스 대상 문자열 (없으면 null)
     * @param bonusAmount   보너스 수치
     * @param healHp        포션 회복량 (장비이면 0)
     * @param maxDurability 최대 내구도 (포션이면 0)
     * @param buyPrice      구매가 (nullable)
     */
    record ItemInputData(
            String id,
            String name,
            String type,
            String kind,
            String bonusTarget,
            int bonusAmount,
            int healHp,
            int maxDurability,
            Integer buyPrice
    ) {
    }
}
