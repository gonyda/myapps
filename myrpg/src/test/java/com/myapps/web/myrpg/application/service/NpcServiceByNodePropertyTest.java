package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.TimeOfDay;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * 노드별 NPC 조회 필터 및 순서 프로퍼티 테스트.
 *
 * <p>임의 NPC 데이터셋과 임의 노드 id(그래프에 없는 임의 문자열 포함)에 대해 {@code byNode(nodeId)}가 일치 Npc만 정의 순서대로 반환하며, 미일치
 * 시 오류 없이 빈 목록을 반환함을 검증한다.
 *
 * <p>Feature: 002-npc-system, Property 3: 노드별 NPC 조회 필터 및 순서
 *
 * <p><b>Validates: Requirements 2.1, 2.2</b>
 */
class NpcServiceByNodePropertyTest {

    private static final int MAX_NPC_COUNT = 8;
    private static final int MIN_NPC_COUNT = 2;
    private static final int MAX_LINE_COUNT = 3;
    private static final int MAX_LINE_LENGTH = 15;
    private static final int ID_MIN_LENGTH = 3;
    private static final int ID_MAX_LENGTH = 12;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 8;

    private static final String[] VALID_TYPES = {
        "chief", "blacksmith", "magic-school", "school", "healer", "bank"
    };

    private static final String[] NODE_IDS = {"tir-chonaill", "dunbarton", "bangor", "emain-macha"};

    private static final String[] TIME_OF_DAY_KEYS;

    static {
        final TimeOfDay[] values = TimeOfDay.values();
        TIME_OF_DAY_KEYS = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            TIME_OF_DAY_KEYS[i] = values[i].key();
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 유효 데이터셋 내에 존재하는 nodeId로 조회하면, 해당 nodeId를 가진 Npc만 정의(원본) 순서대로 반환됨을 검증한다.
     *
     * @param dataset 임의 생성된 유효 NPC 데이터셋(최소 2개 항목, 유일 id)
     * @param nodeIdIndex 조회할 nodeId 선택 인덱스
     */
    @Property(tries = 100)
    void should_returnMatchingNpcsInDefinitionOrder_when_nodeIdExists(
            @ForAll("validNpcDataset") final List<NpcInputData> dataset,
            @ForAll("nodeIdIndex") final int nodeIdIndex) {

        final String targetNodeId = NODE_IDS[nodeIdIndex % NODE_IDS.length];
        final NpcService npcService = buildServiceWithData(dataset);

        final List<Npc> result = npcService.byNode(targetNodeId);

        // 기대값: dataset에서 targetNodeId와 일치하는 항목을 정의 순서대로 추출
        final List<String> expectedIds = new ArrayList<>();
        for (final NpcInputData data : dataset) {
            if (targetNodeId.equals(data.nodeId())) {
                expectedIds.add(data.id());
            }
        }

        // 반환 결과의 id 목록이 기대값과 정확히 일치 (순서 포함)
        final List<String> actualIds = new ArrayList<>();
        for (final Npc npc : result) {
            actualIds.add(npc.id());
        }
        assertThat(actualIds).isEqualTo(expectedIds);

        // 반환된 모든 Npc의 nodeId가 targetNodeId와 일치
        for (final Npc npc : result) {
            assertThat(npc.nodeId()).isEqualTo(targetNodeId);
        }
    }

    /**
     * 데이터셋에 존재하지 않는 임의 nodeId로 조회하면, 오류 없이 빈 목록을 반환함을 검증한다.
     *
     * @param dataset 임의 생성된 유효 NPC 데이터셋(최소 2개 항목, 유일 id)
     * @param nonExistentNodeId 데이터셋에 존재하지 않는 임의 문자열
     */
    @Property(tries = 100)
    void should_returnEmptyList_when_nodeIdDoesNotExist(
            @ForAll("validNpcDataset") final List<NpcInputData> dataset,
            @ForAll("nonExistentNodeId") final String nonExistentNodeId) {

        final NpcService npcService = buildServiceWithData(dataset);

        final List<Npc> result = npcService.byNode(nonExistentNodeId);

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Arbitrary Providers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 유효한 NPC 데이터셋(유일 id, 유효 type, 최소 2개)을 생성하는 Arbitrary 제공자.
     *
     * @return 임의의 유효 NPC 데이터셋 Arbitrary
     */
    @Provide
    Arbitrary<List<NpcInputData>> validNpcDataset() {
        return Arbitraries.integers()
                .between(MIN_NPC_COUNT, MAX_NPC_COUNT)
                .flatMap(this::buildUniqueNpcList);
    }

    /**
     * 유효 노드 ID 배열 인덱스를 선택하는 Arbitrary 제공자.
     *
     * @return 비음수 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> nodeIdIndex() {
        return Arbitraries.integers().between(0, NODE_IDS.length - 1);
    }

    /**
     * 데이터셋에 존재하지 않는 임의의 nodeId 문자열을 생성하는 Arbitrary 제공자.
     *
     * <p>유효 4개 노드 ID에 속하지 않는 임의 문자열을 반환한다.
     *
     * @return 미존재 nodeId 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> nonExistentNodeId() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(15)
                .filter(s -> !isKnownNodeId(s));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Dataset Construction Helpers
    // ──────────────────────────────────────────────────────────────────────

    private Arbitrary<List<NpcInputData>> buildUniqueNpcList(final int count) {
        return npcInputDataArbitrary().list().ofSize(count).map(this::ensureUniqueIds);
    }

    private List<NpcInputData> ensureUniqueIds(final List<NpcInputData> rawList) {
        final List<NpcInputData> result = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            final NpcInputData original = rawList.get(i);
            final String uniqueId = original.id() + "-" + i;
            result.add(
                    new NpcInputData(
                            uniqueId,
                            original.name(),
                            original.typeString(),
                            original.nodeId(),
                            original.personality(),
                            original.defaultLines(),
                            original.byTime()));
        }
        return List.copyOf(result);
    }

    private Arbitrary<NpcInputData> npcInputDataArbitrary() {
        final Arbitrary<String> ids =
                Arbitraries.strings().alpha().ofMinLength(ID_MIN_LENGTH).ofMaxLength(ID_MAX_LENGTH);
        final Arbitrary<String> names =
                Arbitraries.strings()
                        .alpha()
                        .ofMinLength(NAME_MIN_LENGTH)
                        .ofMaxLength(NAME_MAX_LENGTH);
        final Arbitrary<String> typeStrings = Arbitraries.of(VALID_TYPES);
        final Arbitrary<String> nodeIds = Arbitraries.of(NODE_IDS);
        final Arbitrary<String> personalities =
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(MAX_LINE_LENGTH);
        final Arbitrary<List<String>> defaultLines = lineListArbitrary();
        final Arbitrary<Map<String, List<String>>> byTime = byTimeMapArbitrary();

        return Combinators.combine(
                        ids, names, typeStrings, nodeIds, personalities, defaultLines, byTime)
                .as(NpcInputData::new);
    }

    private Arbitrary<List<String>> lineListArbitrary() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(MAX_LINE_LENGTH)
                .list()
                .ofMinSize(0)
                .ofMaxSize(MAX_LINE_COUNT);
    }

    private Arbitrary<Map<String, List<String>>> byTimeMapArbitrary() {
        return Arbitraries.of(TIME_OF_DAY_KEYS)
                .set()
                .ofMinSize(0)
                .ofMaxSize(TIME_OF_DAY_KEYS.length)
                .flatMap(
                        keys -> {
                            if (keys.isEmpty()) {
                                return Arbitraries.just(Map.of());
                            }
                            final List<String> keyList = new ArrayList<>(keys);
                            return lineListArbitrary()
                                    .list()
                                    .ofSize(keyList.size())
                                    .map(
                                            valueLists -> {
                                                final Map<String, List<String>> map =
                                                        new LinkedHashMap<>();
                                                for (int i = 0; i < keyList.size(); i++) {
                                                    map.put(keyList.get(i), valueLists.get(i));
                                                }
                                                return Map.copyOf(map);
                                            });
                        });
    }

    // ──────────────────────────────────────────────────────────────────────
    // Service Setup Helper
    // ──────────────────────────────────────────────────────────────────────

    private NpcService buildServiceWithData(final List<NpcInputData> dataset) {
        final String json = serializeToJson(dataset);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final NpcService npcService = new NpcService(objectMapper);
        final List<Npc> loaded = npcService.loadFromStream(inputStream);
        setNpcsField(npcService, loaded);
        return npcService;
    }

    private void setNpcsField(final NpcService npcService, final List<Npc> npcs) {
        try {
            final Field field = NpcService.class.getDeclaredField("npcs");
            field.setAccessible(true);
            field.set(npcService, npcs);
        } catch (final NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("NpcService.npcs 필드 접근 실패", exception);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // JSON Serialization
    // ──────────────────────────────────────────────────────────────────────

    private String serializeToJson(final List<NpcInputData> dataset) {
        final ArrayNode rootArray = objectMapper.createArrayNode();

        for (final NpcInputData data : dataset) {
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

            final ObjectNode byTimeNode = linesNode.putObject("byTime");
            for (final Map.Entry<String, List<String>> entry : data.byTime().entrySet()) {
                final ArrayNode timeArray = byTimeNode.putArray(entry.getKey());
                for (final String line : entry.getValue()) {
                    timeArray.add(line);
                }
            }
        }

        return rootArray.toString();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────────────────────────────

    private boolean isKnownNodeId(final String nodeId) {
        for (final String known : NODE_IDS) {
            if (known.equals(nodeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 프로퍼티 테스트용 NPC 입력 데이터 레코드.
     *
     * @param id NPC 고유 식별자
     * @param name NPC 표시 이름
     * @param typeString NPC 유형 문자열
     * @param nodeId 배치 노드 ID
     * @param personality 성격 서술
     * @param defaultLines 기본 대사 목록
     * @param byTime 시간대별 대사 맵
     */
    record NpcInputData(
            String id,
            String name,
            String typeString,
            String nodeId,
            String personality,
            List<String> defaultLines,
            Map<String, List<String>> byTime) {}
}
