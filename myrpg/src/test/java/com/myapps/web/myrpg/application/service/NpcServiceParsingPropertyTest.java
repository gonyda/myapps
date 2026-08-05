package com.myapps.web.myrpg.application.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.NpcType;
import com.myapps.web.myrpg.domain.model.TimeOfDay;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NPC 데이터 파싱 라운드트립 프로퍼티 테스트.
 *
 * <p>유효한 NPC 데이터셋을 JSON으로 직렬화한 뒤 {@code NpcService.loadFromStream}으로
 * 파싱하여 모든 Npc의 필드가 순서까지 보존되고, {@code type}이 원본 문자열에 대응하는
 * {@code NpcType}으로 분류됨을 검증한다.
 *
 * <p>Feature: 002-npc-system, Property 1: NPC 데이터 파싱 라운드트립
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.4</b>
 */
class NpcServiceParsingPropertyTest {

    private static final int MAX_NPC_COUNT = 8;
    private static final int MAX_LINE_COUNT = 5;
    private static final int MAX_LINE_LENGTH = 20;
    private static final int ID_MIN_LENGTH = 3;
    private static final int ID_MAX_LENGTH = 15;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 10;

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
     * 유효한 NPC 데이터셋을 JSON으로 직렬화한 뒤 {@code NpcService.loadFromStream}으로
     * 파싱하면, 모든 Npc의 {@code id}/{@code name}/{@code nodeId}/{@code personality}
     * 및 {@code lines.default}·{@code lines.byTime}의 모든 원소가 순서까지 보존되고,
     * 각 Npc의 {@code type}이 원본 문자열에 대응하는 {@code NpcType}으로 분류됨을 검증한다.
     *
     * @param dataset 임의 생성된 유효 NPC 데이터셋
     */
    @Property(tries = 100)
    void should_preserveAllFields_when_validDatasetSerializedAndParsed(
            @ForAll("validNpcDataset") final List<NpcInputData> dataset) {
        // Given: 데이터셋을 JSON으로 직렬화
        final String json = serializeToJson(dataset);
        final InputStream inputStream = new ByteArrayInputStream(
                json.getBytes(StandardCharsets.UTF_8));

        // When: NpcService로 파싱
        final NpcService npcService = new NpcService(objectMapper);
        final List<Npc> parsed = npcService.loadFromStream(inputStream);

        // Then: 모든 필드가 순서까지 보존됨
        assertThat(parsed).hasSameSizeAs(dataset);

        for (int i = 0; i < dataset.size(); i++) {
            final NpcInputData original = dataset.get(i);
            final Npc npc = parsed.get(i);

            assertThat(npc.id()).isEqualTo(original.id());
            assertThat(npc.name()).isEqualTo(original.name());
            assertThat(npc.nodeId()).isEqualTo(original.nodeId());
            assertThat(npc.personality()).isEqualTo(original.personality());

            // type이 원본 문자열에 대응하는 NpcType으로 분류됨
            final NpcType expectedType = NpcType.fromType(original.typeString()).orElseThrow();
            assertThat(npc.type()).isEqualTo(expectedType);

            // lines.default 순서 보존
            assertThat(npc.lines().defaultLines())
                    .containsExactlyElementsOf(original.defaultLines());

            // lines.byTime 키·값 순서 보존
            assertThat(npc.lines().byTime()).hasSameSizeAs(original.byTime());
            for (final Map.Entry<String, List<String>> entry : original.byTime().entrySet()) {
                assertThat(npc.lines().byTime()).containsKey(entry.getKey());
                assertThat(npc.lines().byTime().get(entry.getKey()))
                        .containsExactlyElementsOf(entry.getValue());
            }
        }
    }

    /**
     * 유효한 NPC 데이터셋(유일 id, 유효 type, 유효 lines)을 생성하는 Arbitrary 제공자.
     *
     * @return 임의의 유효 NPC 데이터셋 Arbitrary
     */
    @Provide
    Arbitrary<List<NpcInputData>> validNpcDataset() {
        return Arbitraries.integers().between(1, MAX_NPC_COUNT)
                .flatMap(this::buildUniqueNpcList);
    }

    private Arbitrary<List<NpcInputData>> buildUniqueNpcList(final int count) {
        return npcInputDataArbitrary()
                .list().ofSize(count)
                .map(this::ensureUniqueIds);
    }

    private List<NpcInputData> ensureUniqueIds(final List<NpcInputData> rawList) {
        final List<NpcInputData> result = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            final NpcInputData original = rawList.get(i);
            final String uniqueId = original.id() + "-" + i;
            result.add(new NpcInputData(
                    uniqueId,
                    original.name(),
                    original.typeString(),
                    original.nodeId(),
                    original.personality(),
                    original.defaultLines(),
                    original.byTime()
            ));
        }
        return List.copyOf(result);
    }

    private Arbitrary<NpcInputData> npcInputDataArbitrary() {
        final Arbitrary<String> ids = Arbitraries.strings()
                .alpha().ofMinLength(ID_MIN_LENGTH).ofMaxLength(ID_MAX_LENGTH);
        final Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(NAME_MIN_LENGTH).ofMaxLength(NAME_MAX_LENGTH);
        final Arbitrary<String> typeStrings = Arbitraries.of(
                "chief", "blacksmith", "magic-school", "school", "healer", "bank");
        final Arbitrary<String> nodeIds = Arbitraries.of(
                "tir-chonaill", "dunbarton", "bangor", "emain-macha");
        final Arbitrary<String> personalities = Arbitraries.strings()
                .alpha().ofMinLength(0).ofMaxLength(MAX_LINE_LENGTH);
        final Arbitrary<List<String>> defaultLines = lineListArbitrary();
        final Arbitrary<Map<String, List<String>>> byTime = byTimeMapArbitrary();

        return Combinators.combine(ids, names, typeStrings, nodeIds, personalities, defaultLines, byTime)
                .as(NpcInputData::new);
    }

    private Arbitrary<List<String>> lineListArbitrary() {
        return Arbitraries.strings()
                .alpha().ofMinLength(1).ofMaxLength(MAX_LINE_LENGTH)
                .list().ofMinSize(0).ofMaxSize(MAX_LINE_COUNT);
    }

    private Arbitrary<Map<String, List<String>>> byTimeMapArbitrary() {
        return Arbitraries.of(TIME_OF_DAY_KEYS)
                .set().ofMinSize(0).ofMaxSize(TIME_OF_DAY_KEYS.length)
                .flatMap(keys -> {
                    if (keys.isEmpty()) {
                        return Arbitraries.just(Map.of());
                    }
                    final List<String> keyList = new ArrayList<>(keys);
                    return lineListArbitrary()
                            .list().ofSize(keyList.size())
                            .map(valueLists -> {
                                final Map<String, List<String>> map = new LinkedHashMap<>();
                                for (int i = 0; i < keyList.size(); i++) {
                                    map.put(keyList.get(i), valueLists.get(i));
                                }
                                return Map.copyOf(map);
                            });
                });
    }

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

    /**
     * 프로퍼티 테스트용 NPC 입력 데이터 레코드.
     *
     * @param id           NPC 고유 식별자
     * @param name         NPC 표시 이름
     * @param typeString   NPC 유형 문자열 (유효 6개 값 중 하나)
     * @param nodeId       배치 노드 ID
     * @param personality  성격 서술
     * @param defaultLines 기본 대사 목록
     * @param byTime       시간대별 대사 맵
     */
    record NpcInputData(
            String id,
            String name,
            String typeString,
            String nodeId,
            String personality,
            List<String> defaultLines,
            Map<String, List<String>> byTime
    ) {
    }
}
