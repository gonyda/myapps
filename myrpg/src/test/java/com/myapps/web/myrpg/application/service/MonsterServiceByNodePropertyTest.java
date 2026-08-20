package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.MapNode;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.PotionItem;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
 * 노드별 몬스터 조회 순서·관용성 프로퍼티 테스트.
 *
 * <p>임의 몬스터 목록과 맵 노드 배치에 대해 {@code byNode(nodeId)}가 맵 정의 순서를 보존하며, 미지 노드·null에는 빈 목록을 반환함을 검증한다.
 *
 * <p>Feature: 007-monster-system, Property 5: 노드별 조회 순서·관용성
 *
 * <p><b>Validates: Requirements 5.5, 5.6</b>
 */
class MonsterServiceByNodePropertyTest {

    private static final int MAX_MONSTER_COUNT = 5;
    private static final int ID_MIN_LENGTH = 3;
    private static final int ID_MAX_LENGTH = 10;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 8;
    private static final int LINE_MIN_LENGTH = 2;
    private static final int LINE_MAX_LENGTH = 15;

    private static final String[] VALID_TYPES = {"normal", "boss"};
    private static final String TARGET_NODE_ID = "test-node";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * {@code byNode}가 맵 노드의 {@code monsters} 배열 순서를 보존한 목록을 반환함을 검증한다.
     *
     * @param dataset 임의 생성된 유효 몬스터 데이터셋
     * @param nodeMonsterIndices 노드에 배치할 몬스터의 인덱스 순열
     */
    @Property(tries = 100)
    void should_preserveMapOrder_when_byNodeCalled(
            @ForAll("validMonsterDataset") final List<MonsterInputData> dataset,
            @ForAll("nodeMonsterIndices") final List<Integer> nodeMonsterIndices) {

        // 유효 인덱스만 추출하여 몬스터 ID 목록 구성
        final List<String> nodeMonsterIds = new ArrayList<>();
        for (final int index : nodeMonsterIndices) {
            final int validIndex = index % dataset.size();
            final String monsterId = dataset.get(validIndex).id();
            if (!nodeMonsterIds.contains(monsterId)) {
                nodeMonsterIds.add(monsterId);
            }
        }

        final MonsterService monsterService = buildServiceWithNodeMapping(dataset, nodeMonsterIds);

        final List<Monster> result = monsterService.byNode(TARGET_NODE_ID);

        final List<String> resultIds = result.stream().map(Monster::id).toList();
        assertThat(resultIds).isEqualTo(nodeMonsterIds);
    }

    /**
     * 미지 노드 ID로 조회하면 빈 목록을 반환함을 검증한다.
     *
     * @param dataset 임의 생성된 유효 몬스터 데이터셋
     * @param unknownNodeId 데이터셋에 존재하지 않는 임의 문자열
     */
    @Property(tries = 100)
    void should_returnEmptyList_when_unknownNodeId(
            @ForAll("validMonsterDataset") final List<MonsterInputData> dataset,
            @ForAll("unknownNodeId") final String unknownNodeId) {

        final MonsterService monsterService = buildServiceWithNodeMapping(dataset, List.of());

        final List<Monster> result = monsterService.byNode(unknownNodeId);

        assertThat(result).isEmpty();
    }

    /**
     * null로 조회하면 빈 목록을 반환함을 검증한다.
     *
     * @param dataset 임의 생성된 유효 몬스터 데이터셋
     */
    @Property(tries = 100)
    void should_returnEmptyList_when_nodeIdIsNull(
            @ForAll("validMonsterDataset") final List<MonsterInputData> dataset) {

        final MonsterService monsterService = buildServiceWithNodeMapping(dataset, List.of());

        final List<Monster> result = monsterService.byNode(null);

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Arbitrary Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 유효한 몬스터 데이터셋(유일 id, 최소 2개)을 생성하는 Arbitrary 제공자.
     *
     * @return 임의의 유효 몬스터 데이터셋 Arbitrary
     */
    @Provide
    Arbitrary<List<MonsterInputData>> validMonsterDataset() {
        return Arbitraries.integers()
                .between(2, MAX_MONSTER_COUNT)
                .flatMap(this::buildUniqueMonsterList);
    }

    /**
     * 노드에 배치할 몬스터 인덱스 목록을 생성하는 Arbitrary 제공자.
     *
     * @return 비음수 정수 목록 Arbitrary
     */
    @Provide
    Arbitrary<List<Integer>> nodeMonsterIndices() {
        return Arbitraries.integers()
                .between(0, MAX_MONSTER_COUNT - 1)
                .list()
                .ofMinSize(1)
                .ofMaxSize(MAX_MONSTER_COUNT);
    }

    /**
     * 미지 노드 ID를 생성하는 Arbitrary 제공자.
     *
     * @return 미지 노드 ID 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> unknownNodeId() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(15)
                .filter(s -> !TARGET_NODE_ID.equals(s));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Dataset Construction Helpers
    // ──────────────────────────────────────────────────────────────────────

    private Arbitrary<List<MonsterInputData>> buildUniqueMonsterList(final int count) {
        return monsterInputDataArbitrary().list().ofSize(count).map(this::ensureUniqueIds);
    }

    private List<MonsterInputData> ensureUniqueIds(final List<MonsterInputData> rawList) {
        final List<MonsterInputData> result = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            final MonsterInputData original = rawList.get(i);
            final String uniqueId = original.id() + "-" + i;
            result.add(
                    new MonsterInputData(
                            uniqueId,
                            original.name(),
                            original.typeString(),
                            original.level(),
                            original.maxHp(),
                            original.attackPower(),
                            original.defense(),
                            original.critical(),
                            original.experience(),
                            original.goldDropMin(),
                            original.goldDropMax(),
                            original.lines()));
        }
        return List.copyOf(result);
    }

    private Arbitrary<MonsterInputData> monsterInputDataArbitrary() {
        final Arbitrary<String> ids =
                Arbitraries.strings().alpha().ofMinLength(ID_MIN_LENGTH).ofMaxLength(ID_MAX_LENGTH);
        final Arbitrary<String> names =
                Arbitraries.strings()
                        .alpha()
                        .ofMinLength(NAME_MIN_LENGTH)
                        .ofMaxLength(NAME_MAX_LENGTH);
        final Arbitrary<String> types = Arbitraries.of(VALID_TYPES);
        final Arbitrary<Integer> levels = Arbitraries.integers().between(1, 50);
        final Arbitrary<Integer> maxHps = Arbitraries.integers().between(1, 999);
        final Arbitrary<Integer> attackPowers = Arbitraries.integers().between(0, 100);
        final Arbitrary<Integer> defenses = Arbitraries.integers().between(0, 100);
        final Arbitrary<Integer> criticals = Arbitraries.integers().between(0, 500);
        final Arbitrary<Long> experiences = Arbitraries.longs().between(1L, 10000L);
        final Arbitrary<int[]> goldDropRange = goldDropArbitrary();
        final Arbitrary<List<String>> lines = linesArbitrary();

        // Combinators.combine은 최대 8개까지 지원하므로 두 단계로 나누어 조합한다.
        final Arbitrary<MonsterStatGroup> statGroup =
                Combinators.combine(
                                levels,
                                maxHps,
                                attackPowers,
                                defenses,
                                criticals,
                                experiences,
                                goldDropRange)
                        .as(MonsterStatGroup::new);

        return Combinators.combine(ids, names, types, statGroup, lines)
                .as(
                        (id, name, type, stats, linesList) ->
                                new MonsterInputData(
                                        id,
                                        name,
                                        type,
                                        stats.level(),
                                        stats.maxHp(),
                                        stats.attackPower(),
                                        stats.defense(),
                                        stats.critical(),
                                        stats.experience(),
                                        stats.goldDrop()[0],
                                        stats.goldDrop()[1],
                                        linesList));
    }

    private record MonsterStatGroup(
            int level,
            int maxHp,
            int attackPower,
            int defense,
            int critical,
            long experience,
            int[] goldDrop) {}

    private Arbitrary<int[]> goldDropArbitrary() {
        return Arbitraries.integers()
                .between(0, 100)
                .flatMap(
                        min ->
                                Arbitraries.integers()
                                        .between(min, min + 100)
                                        .map(max -> new int[] {min, max}));
    }

    private Arbitrary<List<String>> linesArbitrary() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(LINE_MIN_LENGTH)
                .ofMaxLength(LINE_MAX_LENGTH)
                .list()
                .ofSize(3);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Service Setup
    // ──────────────────────────────────────────────────────────────────────

    private MonsterService buildServiceWithNodeMapping(
            final List<MonsterInputData> dataset, final List<String> nodeMonsterIds) {

        // loadFromStream으로 몬스터 파싱
        final String json = serializeToJson(dataset);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final MapNode targetNode =
                new MapNode(
                        TARGET_NODE_ID,
                        "Test Node",
                        "field",
                        null,
                        0,
                        0,
                        null,
                        null,
                        List.of(),
                        nodeMonsterIds);

        final MapGraph mapGraph = new MapGraph(List.of(targetNode), List.of(), TARGET_NODE_ID);

        final MapService mapService = mock(MapService.class);
        when(mapService.graph()).thenReturn(mapGraph);

        final ItemCatalogService itemCatalogService = mock(ItemCatalogService.class);
        final Item dummyItem = new PotionItem("dummy", "Dummy", 50, 100);
        when(itemCatalogService.byId(anyString())).thenReturn(Optional.of(dummyItem));

        final MonsterService monsterService =
                new MonsterService(objectMapper, mapService, itemCatalogService);

        final List<Monster> loaded = monsterService.loadFromStream(inputStream);
        setField(monsterService, "monsters", loaded);

        final Map<String, Monster> byIdMap =
                loaded.stream().collect(Collectors.toUnmodifiableMap(Monster::id, m -> m));
        setField(monsterService, "byIdMap", byIdMap);

        return monsterService;
    }

    private void setField(final Object target, final String fieldName, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("MonsterService." + fieldName + " 필드 접근 실패", exception);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // JSON Serialization
    // ──────────────────────────────────────────────────────────────────────

    private String serializeToJson(final List<MonsterInputData> dataset) {
        final ArrayNode rootArray = objectMapper.createArrayNode();

        for (final MonsterInputData data : dataset) {
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
        }

        return rootArray.toString();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Input Data Record
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 프로퍼티 테스트용 몬스터 입력 데이터 레코드.
     *
     * @param id 몬스터 고유 식별자
     * @param name 몬스터 표시 이름
     * @param typeString 몬스터 유형 문자열
     * @param level 레벨
     * @param maxHp 최대 HP
     * @param attackPower 공격력
     * @param defense 방어력
     * @param critical 크리티컬
     * @param experience 경험치
     * @param goldDropMin 골드 드랍 최소값
     * @param goldDropMax 골드 드랍 최대값
     * @param lines 조우 대사 목록 (3개)
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
            List<String> lines) {}
}
