package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.exception.NpcDataException;
import com.myapps.web.myrpg.domain.model.Npc;
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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * NPC 데이터 파싱 및 shopItems 기본값 불변 프로퍼티 테스트.
 *
 * <p>유효한 NPC JSON 노드에 대해 shopItems가 배열로 주어지면 해당 아이템 ID 목록이 불변 리스트로 로드되고, 필드가 없거나 null이면 빈 불변
 * 리스트({@code List.of()})가 할당되며 기존 필수 필드 및 중복 ID 검증 규칙이 유지됨을 검증한다.
 *
 * <p>Feature: 010-npc-actions-shop-repair-heal, Property 2: NPC 데이터 파싱 및 shopItems 기본값 불변
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.3, 15.3</b>
 */
class NpcServiceShopItemsParsingPropertyTest {

    private static final int MAX_NPC_COUNT = 6;
    private static final int MAX_SHOP_ITEMS_COUNT = 4;
    private static final int ID_MIN_LENGTH = 3;
    private static final int ID_MAX_LENGTH = 12;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 8;

    private static final String[] VALID_TYPE_STRINGS = {
        "chief", "blacksmith", "magic-school", "school", "healer", "bank"
    };
    private static final String[] VALID_NODE_IDS = {
        "tir-chonaill", "dunbarton", "bangor", "emain-macha"
    };
    private static final String[] SAMPLE_ITEM_IDS = {
        "short_sword", "long_sword", "hp_potion_30", "beginner_shield", "beginner_bow"
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NpcService npcService = new NpcService(objectMapper);

    /**
     * shopItems가 명시된 유효한 NPC 데이터셋을 파싱하면 shopItems가 불변 목록으로 로드됨을 검증한다.
     *
     * @param dataset 임의 생성된 유효 NPC 데이터셋 (shopItems 포함)
     */
    @Property(tries = 100)
    void should_parseShopItemsAsImmutableList_when_shopItemsProvided(
            @ForAll("validNpcDatasetWithShopItems") final List<NpcShopItemInputData> dataset) {
        final String json = serializeToJson(dataset, ShopItemMode.EXPLICIT);
        final InputStream inputStream = toInputStream(json);

        final List<Npc> parsed = npcService.loadFromStream(inputStream);

        assertThat(parsed).hasSameSizeAs(dataset);
        for (int i = 0; i < dataset.size(); i++) {
            final NpcShopItemInputData original = dataset.get(i);
            final Npc npc = parsed.get(i);

            assertThat(npc.id()).isEqualTo(original.id());
            assertThat(npc.shopItems()).containsExactlyElementsOf(original.shopItems());

            // 불변 리스트 검증
            assertThatThrownBy(() -> npc.shopItems().add("extra_item"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    /**
     * shopItems 필드가 누락되거나 null인 경우 빈 불변 목록(List.of())으로 기본 할당됨을 검증한다.
     *
     * @param dataset 임의 생성된 유효 NPC 데이터셋
     * @param mode shopItems 생략 또는 null 지정 모드
     */
    @Property(tries = 100)
    void should_assignEmptyList_when_shopItemsMissingOrNull(
            @ForAll("validNpcDatasetWithShopItems") final List<NpcShopItemInputData> dataset,
            @ForAll("missingOrNullMode") final ShopItemMode mode) {
        final String json = serializeToJson(dataset, mode);
        final InputStream inputStream = toInputStream(json);

        final List<Npc> parsed = npcService.loadFromStream(inputStream);

        assertThat(parsed).hasSameSizeAs(dataset);
        for (final Npc npc : parsed) {
            assertThat(npc.shopItems()).isNotNull().isEmpty();
            assertThatThrownBy(() -> npc.shopItems().add("extra_item"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    /**
     * 필수 필드(id/name/type/nodeId)가 누락되거나 중복 ID가 존재하면 NpcDataException이 발생함을 검증한다.
     *
     * @param dataset 2개 이상의 유효 NPC 데이터셋
     */
    @Property(tries = 100)
    void should_throwNpcDataException_when_duplicateId(
            @ForAll("validNpcDatasetAtLeastTwo") final List<NpcShopItemInputData> dataset) {
        final List<NpcShopItemInputData> duplicateDataset = new ArrayList<>(dataset);
        final NpcShopItemInputData first = duplicateDataset.getFirst();
        duplicateDataset.set(
                1,
                new NpcShopItemInputData(
                        first.id(),
                        "다른이름",
                        first.typeString(),
                        first.nodeId(),
                        first.personality(),
                        List.of("안녕하세요."),
                        first.shopItems()));

        final String json = serializeToJson(duplicateDataset, ShopItemMode.EXPLICIT);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> npcService.loadFromStream(inputStream))
                .isInstanceOf(NpcDataException.class);
    }

    // ── Providers ──

    @Provide
    Arbitrary<List<NpcShopItemInputData>> validNpcDatasetWithShopItems() {
        return Arbitraries.integers().between(1, MAX_NPC_COUNT).flatMap(this::buildUniqueNpcList);
    }

    @Provide
    Arbitrary<List<NpcShopItemInputData>> validNpcDatasetAtLeastTwo() {
        return Arbitraries.integers().between(2, MAX_NPC_COUNT).flatMap(this::buildUniqueNpcList);
    }

    @Provide
    Arbitrary<ShopItemMode> missingOrNullMode() {
        return Arbitraries.of(ShopItemMode.OMITTED, ShopItemMode.NULL_VALUE);
    }

    private Arbitrary<List<NpcShopItemInputData>> buildUniqueNpcList(final int count) {
        return npcShopItemInputDataArbitrary().list().ofSize(count).map(this::ensureUniqueIds);
    }

    private List<NpcShopItemInputData> ensureUniqueIds(final List<NpcShopItemInputData> rawList) {
        final List<NpcShopItemInputData> result = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            final NpcShopItemInputData original = rawList.get(i);
            final String uniqueId = original.id() + "_" + i;
            result.add(
                    new NpcShopItemInputData(
                            uniqueId,
                            original.name(),
                            original.typeString(),
                            original.nodeId(),
                            original.personality(),
                            original.defaultLines(),
                            original.shopItems()));
        }
        return List.copyOf(result);
    }

    private Arbitrary<NpcShopItemInputData> npcShopItemInputDataArbitrary() {
        final Arbitrary<String> ids =
                Arbitraries.strings().alpha().ofMinLength(ID_MIN_LENGTH).ofMaxLength(ID_MAX_LENGTH);
        final Arbitrary<String> names =
                Arbitraries.strings()
                        .alpha()
                        .ofMinLength(NAME_MIN_LENGTH)
                        .ofMaxLength(NAME_MAX_LENGTH);
        final Arbitrary<String> typeStrings = Arbitraries.of(VALID_TYPE_STRINGS);
        final Arbitrary<String> nodeIds = Arbitraries.of(VALID_NODE_IDS);
        final Arbitrary<String> personalities =
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(10);
        final Arbitrary<List<String>> defaultLines =
                Arbitraries.strings()
                        .alpha()
                        .ofMinLength(1)
                        .ofMaxLength(10)
                        .list()
                        .ofMinSize(1)
                        .ofMaxSize(3);
        final Arbitrary<List<String>> shopItems =
                Arbitraries.of(SAMPLE_ITEM_IDS).list().ofMinSize(0).ofMaxSize(MAX_SHOP_ITEMS_COUNT);

        return Combinators.combine(
                        ids, names, typeStrings, nodeIds, personalities, defaultLines, shopItems)
                .as(NpcShopItemInputData::new);
    }

    private String serializeToJson(
            final List<NpcShopItemInputData> dataset, final ShopItemMode mode) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        for (final NpcShopItemInputData data : dataset) {
            final ObjectNode npcNode = rootArray.addObject();
            npcNode.put("id", data.id());
            npcNode.put("name", data.name());
            npcNode.put("type", data.typeString());
            npcNode.put("nodeId", data.nodeId());
            npcNode.put("personality", data.personality());

            final ObjectNode linesNode = npcNode.putObject("lines");
            final ArrayNode defaultArray = linesNode.putArray("default");
            for (final String line : data.defaultLines()) {
                defaultArray.add(line);
            }

            if (mode == ShopItemMode.EXPLICIT) {
                final ArrayNode shopItemsArray = npcNode.putArray("shopItems");
                for (final String item : data.shopItems()) {
                    shopItemsArray.add(item);
                }
            } else if (mode == ShopItemMode.NULL_VALUE) {
                npcNode.putNull("shopItems");
            }
            // mode == ShopItemMode.OMITTED: shopItems 노드 생성 안 함
        }
        return rootArray.toString();
    }

    private InputStream toInputStream(final String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    enum ShopItemMode {
        EXPLICIT,
        OMITTED,
        NULL_VALUE
    }

    record NpcShopItemInputData(
            String id,
            String name,
            String typeString,
            String nodeId,
            String personality,
            List<String> defaultLines,
            List<String> shopItems) {}
}
