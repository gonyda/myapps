package com.myapps.web.myrpg.application.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.myapps.web.myrpg.domain.model.GoldDrop;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.ItemType;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.MonsterType;
import com.myapps.web.myrpg.domain.model.PotionItem;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 몬스터 카탈로그 파싱 라운드트립 프로퍼티 테스트.
 *
 * <p>유효한 몬스터 데이터셋을 JSON으로 직렬화한 뒤 {@code MonsterService.loadFromStream}으로
 * 파싱하여 모든 Monster의 필드가 순서까지 보존되고, {@code itemDrops} 미기재 시 빈 목록이 됨을 검증한다.
 *
 * <p>Feature: 007-monster-system, Property 3: 카탈로그 파싱·필드 보존
 *
 * <p><b>Validates: Requirements 1.2, 2.7, 4.1</b>
 */
class MonsterServiceParsingPropertyTest {

    private static final int MAX_MONSTER_COUNT = 5;
    private static final int ID_MIN_LENGTH = 3;
    private static final int ID_MAX_LENGTH = 12;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 8;
    private static final int LINE_MIN_LENGTH = 2;
    private static final int LINE_MAX_LENGTH = 20;
    private static final int MAX_ITEM_DROPS = 3;

    private static final String[] VALID_TYPES = {"normal", "boss"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 유효한 몬스터 데이터셋을 JSON으로 직렬화한 뒤 {@code MonsterService.loadFromStream}으로
     * 파싱하면, 항목 수가 보존되고 모든 필드(id, name, type, level, maxHp, attackPower,
     * defense, critical, experience, goldDrop, itemDrops, lines)가 정확히 보존됨을 검증한다.
     *
     * @param dataset 임의 생성된 유효 몬스터 데이터셋
     */
    @Property(tries = 100)
    void should_preserveAllFields_when_validDatasetParsed(
            @ForAll("validMonsterDataset") final List<MonsterInputData> dataset) {

        final String json = serializeToJson(dataset);
        final InputStream inputStream = new ByteArrayInputStream(
                json.getBytes(StandardCharsets.UTF_8));

        final MonsterService monsterService = buildServiceWithMocks();
        final List<Monster> parsed = monsterService.loadFromStream(inputStream);

        assertThat(parsed).hasSameSizeAs(dataset);
        assertThat(parsed).isUnmodifiable();

        for (int i = 0; i < dataset.size(); i++) {
            final MonsterInputData original = dataset.get(i);
            final Monster monster = parsed.get(i);

            assertThat(monster.id()).isEqualTo(original.id());
            assertThat(monster.name()).isEqualTo(original.name());

            final MonsterType expectedType = MonsterType.fromType(original.typeString()).orElseThrow();
            assertThat(monster.type()).isEqualTo(expectedType);

            assertThat(monster.level()).isEqualTo(original.level());
            assertThat(monster.maxHp()).isEqualTo(original.maxHp());
            assertThat(monster.attackPower()).isEqualTo(original.attackPower());
            assertThat(monster.defense()).isEqualTo(original.defense());
            assertThat(monster.critical()).isEqualTo(original.critical());
            assertThat(monster.experience()).isEqualTo(original.experience());

            assertThat(monster.goldDrop().min()).isEqualTo(original.goldDropMin());
            assertThat(monster.goldDrop().max()).isEqualTo(original.goldDropMax());

            assertThat(monster.itemDrops()).hasSameSizeAs(original.itemDrops());
            for (int j = 0; j < original.itemDrops().size(); j++) {
                final ItemDropInputData expectedDrop = original.itemDrops().get(j);
                assertThat(monster.itemDrops().get(j).itemId()).isEqualTo(expectedDrop.itemId());
                assertThat(monster.itemDrops().get(j).chancePercent()).isEqualTo(expectedDrop.chancePercent());
                assertThat(monster.itemDrops().get(j).minQuantity()).isEqualTo(expectedDrop.minQuantity());
                assertThat(monster.itemDrops().get(j).maxQuantity()).isEqualTo(expectedDrop.maxQuantity());
            }

            assertThat(monster.lines()).containsExactlyElementsOf(original.lines());
        }
    }

    /**
     * {@code itemDrops} 필드가 JSON에서 생략된 경우 빈 목록으로 파싱됨을 검증한다.
     *
     * @param dataset {@code itemDrops}가 없는 유효 몬스터 데이터셋
     */
    @Property(tries = 100)
    void should_haveEmptyItemDrops_when_itemDropsAbsent(
            @ForAll("validMonsterDatasetWithoutItemDrops") final List<MonsterInputData> dataset) {

        final String json = serializeToJsonWithoutItemDrops(dataset);
        final InputStream inputStream = new ByteArrayInputStream(
                json.getBytes(StandardCharsets.UTF_8));

        final MonsterService monsterService = buildServiceWithMocks();
        final List<Monster> parsed = monsterService.loadFromStream(inputStream);

        assertThat(parsed).hasSameSizeAs(dataset);
        for (final Monster monster : parsed) {
            assertThat(monster.itemDrops()).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Arbitrary Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 유효한 몬스터 데이터셋(유일 id, 유효 type, lines 3개)을 생성하는 Arbitrary 제공자.
     *
     * @return 임의의 유효 몬스터 데이터셋 Arbitrary
     */
    @Provide
    Arbitrary<List<MonsterInputData>> validMonsterDataset() {
        return Arbitraries.integers().between(1, MAX_MONSTER_COUNT)
                .flatMap(this::buildUniqueMonsterList);
    }

    /**
     * itemDrops가 없는 유효한 몬스터 데이터셋을 생성하는 Arbitrary 제공자.
     *
     * @return itemDrops 없는 몬스터 데이터셋 Arbitrary
     */
    @Provide
    Arbitrary<List<MonsterInputData>> validMonsterDatasetWithoutItemDrops() {
        return Arbitraries.integers().between(1, MAX_MONSTER_COUNT)
                .flatMap(count -> buildUniqueMonsterList(count)
                        .map(list -> list.stream()
                                .map(m -> new MonsterInputData(
                                        m.id(), m.name(), m.typeString(),
                                        m.level(), m.maxHp(), m.attackPower(),
                                        m.defense(), m.critical(), m.experience(),
                                        m.goldDropMin(), m.goldDropMax(),
                                        List.of(), m.lines()))
                                .toList()));
    }

    private Arbitrary<List<MonsterInputData>> buildUniqueMonsterList(final int count) {
        return monsterInputDataArbitrary()
                .list().ofSize(count)
                .map(this::ensureUniqueIds);
    }

    private List<MonsterInputData> ensureUniqueIds(final List<MonsterInputData> rawList) {
        final List<MonsterInputData> result = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            final MonsterInputData original = rawList.get(i);
            final String uniqueId = original.id() + "-" + i;
            result.add(new MonsterInputData(
                    uniqueId, original.name(), original.typeString(),
                    original.level(), original.maxHp(), original.attackPower(),
                    original.defense(), original.critical(), original.experience(),
                    original.goldDropMin(), original.goldDropMax(),
                    original.itemDrops(), original.lines()));
        }
        return List.copyOf(result);
    }

    private Arbitrary<MonsterInputData> monsterInputDataArbitrary() {
        final Arbitrary<String> ids = Arbitraries.strings()
                .alpha().ofMinLength(ID_MIN_LENGTH).ofMaxLength(ID_MAX_LENGTH);
        final Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(NAME_MIN_LENGTH).ofMaxLength(NAME_MAX_LENGTH);
        final Arbitrary<String> types = Arbitraries.of(VALID_TYPES);
        final Arbitrary<Integer> levels = Arbitraries.integers().between(1, 50);
        final Arbitrary<Integer> maxHps = Arbitraries.integers().between(1, 999);
        final Arbitrary<Integer> attackPowers = Arbitraries.integers().between(0, 100);
        final Arbitrary<Integer> defenses = Arbitraries.integers().between(0, 100);
        final Arbitrary<Integer> criticals = Arbitraries.integers().between(0, 500);
        final Arbitrary<Long> experiences = Arbitraries.longs().between(1L, 10000L);
        final Arbitrary<int[]> goldDropRange = goldDropArbitrary();
        final Arbitrary<List<ItemDropInputData>> itemDrops = itemDropListArbitrary();
        final Arbitrary<List<String>> lines = linesArbitrary();

        // Combinators.combine은 최대 8개까지 지원하므로 두 단계로 나누어 조합한다.
        final Arbitrary<MonsterStatGroup> statGroup = Combinators.combine(
                        levels, maxHps, attackPowers, defenses, criticals, experiences, goldDropRange)
                .as(MonsterStatGroup::new);

        return Combinators.combine(ids, names, types, statGroup, itemDrops, lines)
                .as((id, name, type, stats, drops, linesList) ->
                        new MonsterInputData(id, name, type,
                                stats.level(), stats.maxHp(), stats.attackPower(),
                                stats.defense(), stats.critical(), stats.experience(),
                                stats.goldDrop()[0], stats.goldDrop()[1],
                                drops, linesList));
    }

    private record MonsterStatGroup(
            int level, int maxHp, int attackPower,
            int defense, int critical, long experience,
            int[] goldDrop
    ) {
    }

    private Arbitrary<int[]> goldDropArbitrary() {
        return Arbitraries.integers().between(0, 100)
                .flatMap(min -> Arbitraries.integers().between(min, min + 100)
                        .map(max -> new int[]{min, max}));
    }

    private Arbitrary<List<ItemDropInputData>> itemDropListArbitrary() {
        return itemDropArbitrary().list().ofMinSize(0).ofMaxSize(MAX_ITEM_DROPS);
    }

    private Arbitrary<ItemDropInputData> itemDropArbitrary() {
        final Arbitrary<String> itemIds = Arbitraries.of(
                "hp_potion_50", "iron_sword", "leather_armor", "mp_potion_30");
        final Arbitrary<Integer> chances = Arbitraries.integers().between(1, 100);
        final Arbitrary<int[]> quantities = Arbitraries.integers().between(1, 5)
                .flatMap(min -> Arbitraries.integers().between(min, min + 3)
                        .map(max -> new int[]{min, max}));

        return Combinators.combine(itemIds, chances, quantities)
                .as((itemId, chance, qty) ->
                        new ItemDropInputData(itemId, chance, qty[0], qty[1]));
    }

    private Arbitrary<List<String>> linesArbitrary() {
        return Arbitraries.strings()
                .alpha().ofMinLength(LINE_MIN_LENGTH).ofMaxLength(LINE_MAX_LENGTH)
                .list().ofSize(3);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Service Setup
    // ──────────────────────────────────────────────────────────────────────

    private MonsterService buildServiceWithMocks() {
        final MapService mapService = mock(MapService.class);
        final MapGraph mapGraph = new MapGraph(List.of(), List.of(), "start");
        when(mapService.graph()).thenReturn(mapGraph);

        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final Item dummyItem = new PotionItem("dummy", "Dummy", 50, 100);
        when(itemCatalogService.byId(anyString())).thenReturn(Optional.of(dummyItem));

        return new MonsterService(objectMapper, mapService, itemCatalogService);
    }

    // ──────────────────────────────────────────────────────────────────────
    // JSON Serialization
    // ──────────────────────────────────────────────────────────────────────

    private String serializeToJson(final List<MonsterInputData> dataset) {
        final ArrayNode rootArray = objectMapper.createArrayNode();

        for (final MonsterInputData data : dataset) {
            final ObjectNode monsterNode = buildMonsterNode(rootArray, data);
            addItemDropsToNode(monsterNode, data.itemDrops());
        }

        return rootArray.toString();
    }

    private String serializeToJsonWithoutItemDrops(final List<MonsterInputData> dataset) {
        final ArrayNode rootArray = objectMapper.createArrayNode();

        for (final MonsterInputData data : dataset) {
            buildMonsterNode(rootArray, data);
            // itemDrops 필드 자체를 생략
        }

        return rootArray.toString();
    }

    private ObjectNode buildMonsterNode(final ArrayNode rootArray,
                                        final MonsterInputData data) {
        final ObjectNode monsterNode = rootArray.addObject();
        monsterNode.put("id", data.id());
        monsterNode.put("name", data.name());
        monsterNode.put("type", data.typeString());
        monsterNode.put("level", data.level());
        monsterNode.put("maxHp", data.maxHp());
        monsterNode.put("attackPower", data.attackPower());
        monsterNode.put("defense", data.defense());
        monsterNode.put("critical", data.critical());
        monsterNode.put("experience", data.experience());

        final ObjectNode goldDropNode = monsterNode.putObject("goldDrop");
        goldDropNode.put("min", data.goldDropMin());
        goldDropNode.put("max", data.goldDropMax());

        final ArrayNode linesArray = monsterNode.putArray("lines");
        for (final String line : data.lines()) {
            linesArray.add(line);
        }

        return monsterNode;
    }

    private void addItemDropsToNode(final ObjectNode monsterNode,
                                    final List<ItemDropInputData> itemDrops) {
        if (!itemDrops.isEmpty()) {
            final ArrayNode itemDropsArray = monsterNode.putArray("itemDrops");
            for (final ItemDropInputData drop : itemDrops) {
                final ObjectNode dropNode = itemDropsArray.addObject();
                dropNode.put("itemId", drop.itemId());
                dropNode.put("chancePercent", drop.chancePercent());
                dropNode.put("minQuantity", drop.minQuantity());
                dropNode.put("maxQuantity", drop.maxQuantity());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Input Data Records
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 프로퍼티 테스트용 몬스터 입력 데이터 레코드.
     *
     * @param id           몬스터 고유 식별자
     * @param name         몬스터 표시 이름
     * @param typeString   몬스터 유형 문자열
     * @param level        레벨
     * @param maxHp        최대 HP
     * @param attackPower  공격력
     * @param defense      방어력
     * @param critical     크리티컬
     * @param experience   경험치
     * @param goldDropMin  골드 드랍 최소값
     * @param goldDropMax  골드 드랍 최대값
     * @param itemDrops    아이템 드랍 목록
     * @param lines        조우 대사 목록 (3개)
     */
    record MonsterInputData(
            String id,
            String name,
            String typeString,
            int level,
            int maxHp,
            int attackPower,
            int defense,
            int critical,
            long experience,
            int goldDropMin,
            int goldDropMax,
            List<ItemDropInputData> itemDrops,
            List<String> lines
    ) {
    }

    /**
     * 프로퍼티 테스트용 아이템 드랍 입력 데이터 레코드.
     *
     * @param itemId       아이템 ID
     * @param chancePercent 드랍 확률
     * @param minQuantity  최소 수량
     * @param maxQuantity  최대 수량
     */
    record ItemDropInputData(
            String itemId,
            int chancePercent,
            int minQuantity,
            int maxQuantity
    ) {
    }
}
