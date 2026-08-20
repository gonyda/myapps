package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.exception.NpcDataException;
import com.myapps.web.myrpg.domain.model.TimeOfDay;
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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * NPC 데이터 로드 실패 및 무생성(all-or-nothing) 프로퍼티 테스트.
 *
 * <p>유효한 NPC 데이터셋에 (a) 필수 필드 누락, (b) {@code id} 중복, (c) 미지 {@code type} 중 하나 이상을 주입하여 로드가 {@link
 * NpcDataException}을 던지고 어떤 목록(부분 목록 포함)도 제공하지 않음을 검증한다.
 *
 * <p>Feature: 002-npc-system, Property 2: NPC 데이터 로드 실패 및 무생성(all-or-nothing)
 *
 * <p><b>Validates: Requirements 1.5, 1.7</b>
 */
class NpcServiceLoadFailurePropertyTest {

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
     * 유효 데이터셋에 필수 필드 누락을 주입하면 {@code NpcDataException}이 발생함을 검증한다.
     *
     * <p>필수 필드({@code id}, {@code name}, {@code type}, {@code nodeId}) 중 하나를 {@code null} 또는 빈 문자열로
     * 설정한 항목을 삽입하면, 로드가 실패하고 부분 목록도 제공되지 않는다.
     *
     * @param dataset 임의 생성된 유효 NPC 데이터셋(최소 2개 항목, 유일 id)
     * @param corruptionIndex 필수 필드를 누락시킬 항목의 인덱스(0-based, dataset 크기 내)
     * @param fieldIndex 누락시킬 필수 필드의 인덱스 (0=id, 1=name, 2=type, 3=nodeId)
     * @param useNull true이면 필드를 null로, false이면 빈 문자열로 설정
     */
    @Property(tries = 100)
    void should_throwNpcDataException_when_requiredFieldMissing(
            @ForAll("validNpcDataset") final List<NpcInputData> dataset,
            @ForAll("corruptionIndex") final int corruptionIndex,
            @ForAll("fieldIndex") final int fieldIndex,
            @ForAll("useNullOrBlank") final boolean useNull) {

        final int targetIndex = corruptionIndex % dataset.size();
        final List<NpcInputData> corrupted =
                corruptRequiredField(dataset, targetIndex, fieldIndex, useNull);

        final String json = serializeToJson(corrupted);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final NpcService npcService = new NpcService(objectMapper);

        assertThatThrownBy(() -> npcService.loadFromStream(inputStream))
                .isInstanceOf(NpcDataException.class);
    }

    /**
     * 유효 데이터셋에 {@code id} 중복을 주입하면 {@code NpcDataException}이 발생함을 검증한다.
     *
     * <p>두 개 이상의 NPC가 동일한 {@code id}를 가지도록 주입하면, 로드가 실패하고 부분 목록도 제공되지 않는다.
     *
     * @param dataset 임의 생성된 유효 NPC 데이터셋(최소 2개 항목, 유일 id)
     * @param duplicateTargetIndex 중복 id를 주입할 대상 항목의 인덱스
     */
    @Property(tries = 100)
    void should_throwNpcDataException_when_duplicateId(
            @ForAll("validNpcDataset") final List<NpcInputData> dataset,
            @ForAll("duplicateTargetIndex") final int duplicateTargetIndex) {

        final int sourceIndex = 0;
        final int targetIndex = 1 + (duplicateTargetIndex % (dataset.size() - 1));
        final List<NpcInputData> corrupted = injectDuplicateId(dataset, sourceIndex, targetIndex);

        final String json = serializeToJson(corrupted);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final NpcService npcService = new NpcService(objectMapper);

        assertThatThrownBy(() -> npcService.loadFromStream(inputStream))
                .isInstanceOf(NpcDataException.class);
    }

    /**
     * 유효 데이터셋에 미지 {@code type}을 주입하면 {@code NpcDataException}이 발생함을 검증한다.
     *
     * <p>6개 유효 타입 중 어느 것도 아닌 {@code type} 값을 가진 항목을 삽입하면, 로드가 실패하고 부분 목록도 제공되지 않는다.
     *
     * @param dataset 임의 생성된 유효 NPC 데이터셋(최소 2개 항목, 유일 id)
     * @param corruptionIndex 미지 type을 주입할 항목의 인덱스
     * @param unknownType 유효 타입에 속하지 않는 임의 문자열
     */
    @Property(tries = 100)
    void should_throwNpcDataException_when_unknownType(
            @ForAll("validNpcDataset") final List<NpcInputData> dataset,
            @ForAll("corruptionIndex") final int corruptionIndex,
            @ForAll("unknownType") final String unknownType) {

        final int targetIndex = corruptionIndex % dataset.size();
        final List<NpcInputData> corrupted = injectUnknownType(dataset, targetIndex, unknownType);

        final String json = serializeToJson(corrupted);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final NpcService npcService = new NpcService(objectMapper);

        assertThatThrownBy(() -> npcService.loadFromStream(inputStream))
                .isInstanceOf(NpcDataException.class);
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
     * 데이터셋 내 항목 인덱스를 선택하는 Arbitrary 제공자.
     *
     * @return 비음수 정수 Arbitrary (dataset.size()로 mod 연산하여 사용)
     */
    @Provide
    Arbitrary<Integer> corruptionIndex() {
        return Arbitraries.integers().between(0, MAX_NPC_COUNT - 1);
    }

    /**
     * 중복 id 주입 대상 인덱스를 선택하는 Arbitrary 제공자.
     *
     * @return 비음수 정수 Arbitrary (dataset.size()-1로 mod 연산하여 사용)
     */
    @Provide
    Arbitrary<Integer> duplicateTargetIndex() {
        return Arbitraries.integers().between(0, MAX_NPC_COUNT - 1);
    }

    /**
     * 필수 필드 인덱스를 선택하는 Arbitrary 제공자 (0=id, 1=name, 2=type, 3=nodeId).
     *
     * @return 0~3 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> fieldIndex() {
        return Arbitraries.integers().between(0, 3);
    }

    /**
     * null 또는 빈 문자열 여부를 선택하는 Arbitrary 제공자.
     *
     * @return boolean Arbitrary
     */
    @Provide
    Arbitrary<Boolean> useNullOrBlank() {
        return Arbitraries.of(true, false);
    }

    /**
     * 유효 6개 타입에 속하지 않는 임의 문자열을 생성하는 Arbitrary 제공자.
     *
     * @return 유효 타입이 아닌 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> unknownType() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10)
                .filter(s -> !isValidType(s));
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
        final Arbitrary<String> nodeIds =
                Arbitraries.of("tir-chonaill", "dunbarton", "bangor", "emain-macha");
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
    // Corruption Helpers
    // ──────────────────────────────────────────────────────────────────────

    private List<NpcInputData> corruptRequiredField(
            final List<NpcInputData> dataset,
            final int targetIndex,
            final int fieldIndex,
            final boolean useNull) {

        final List<NpcInputData> result = new ArrayList<>(dataset);
        final NpcInputData original = result.get(targetIndex);
        final String corruptedValue = useNull ? null : "   ";

        final NpcInputData corrupted =
                switch (fieldIndex) {
                    case 0 ->
                            new NpcInputData(
                                    corruptedValue,
                                    original.name(),
                                    original.typeString(),
                                    original.nodeId(),
                                    original.personality(),
                                    original.defaultLines(),
                                    original.byTime());
                    case 1 ->
                            new NpcInputData(
                                    original.id(),
                                    corruptedValue,
                                    original.typeString(),
                                    original.nodeId(),
                                    original.personality(),
                                    original.defaultLines(),
                                    original.byTime());
                    case 2 ->
                            new NpcInputData(
                                    original.id(),
                                    original.name(),
                                    corruptedValue,
                                    original.nodeId(),
                                    original.personality(),
                                    original.defaultLines(),
                                    original.byTime());
                    default ->
                            new NpcInputData(
                                    original.id(),
                                    original.name(),
                                    original.typeString(),
                                    corruptedValue,
                                    original.personality(),
                                    original.defaultLines(),
                                    original.byTime());
                };

        result.set(targetIndex, corrupted);
        return List.copyOf(result);
    }

    private List<NpcInputData> injectDuplicateId(
            final List<NpcInputData> dataset, final int sourceIndex, final int targetIndex) {

        final List<NpcInputData> result = new ArrayList<>(dataset);
        final NpcInputData source = result.get(sourceIndex);
        final NpcInputData target = result.get(targetIndex);

        final NpcInputData duplicated =
                new NpcInputData(
                        source.id(),
                        target.name(),
                        target.typeString(),
                        target.nodeId(),
                        target.personality(),
                        target.defaultLines(),
                        target.byTime());

        result.set(targetIndex, duplicated);
        return List.copyOf(result);
    }

    private List<NpcInputData> injectUnknownType(
            final List<NpcInputData> dataset, final int targetIndex, final String unknownType) {

        final List<NpcInputData> result = new ArrayList<>(dataset);
        final NpcInputData original = result.get(targetIndex);

        final NpcInputData corrupted =
                new NpcInputData(
                        original.id(),
                        original.name(),
                        unknownType,
                        original.nodeId(),
                        original.personality(),
                        original.defaultLines(),
                        original.byTime());

        result.set(targetIndex, corrupted);
        return List.copyOf(result);
    }

    // ──────────────────────────────────────────────────────────────────────
    // JSON Serialization
    // ──────────────────────────────────────────────────────────────────────

    private String serializeToJson(final List<NpcInputData> dataset) {
        final ArrayNode rootArray = objectMapper.createArrayNode();

        for (final NpcInputData data : dataset) {
            final ObjectNode npcNode = rootArray.addObject();

            if (data.id() != null) {
                npcNode.put("id", data.id());
            } else {
                npcNode.putNull("id");
            }

            if (data.name() != null) {
                npcNode.put("name", data.name());
            } else {
                npcNode.putNull("name");
            }

            if (data.typeString() != null) {
                npcNode.put("type", data.typeString());
            } else {
                npcNode.putNull("type");
            }

            if (data.nodeId() != null) {
                npcNode.put("nodeId", data.nodeId());
            } else {
                npcNode.putNull("nodeId");
            }

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

    private boolean isValidType(final String type) {
        for (final String validType : VALID_TYPES) {
            if (validType.equals(type)) {
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
