package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.exception.SkillDataException;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
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
 * 스킬 카탈로그 파싱 검증 프로퍼티 테스트.
 *
 * <p>유효한 스킬 JSON 입력은 올바른 크기의 불변 목록을 반환하고, 결함(미지 type/talent, 중복 id, 필수 필드 누락, 랭크맵 15키)이 주입된 입력은
 * {@link SkillDataException}을 던짐을 검증한다.
 *
 * <p>Feature: 005-skill-system, Property 7: 카탈로그 검증
 *
 * <p><b>Validates: Requirements 1.2, 1.4, 1.5, 1.6</b>
 */
class SkillCatalogParsingPropertyTest {

    private static final int MAX_SKILL_COUNT = 5;
    private static final int ID_MIN_LENGTH = 3;
    private static final int ID_MAX_LENGTH = 12;
    private static final int LABEL_MIN_LENGTH = 2;
    private static final int LABEL_MAX_LENGTH = 8;
    private static final int RESOURCE_COST_MIN = 1;
    private static final int RESOURCE_COST_MAX = 20;
    private static final int MULTIPLIER_MIN = 50;
    private static final int MULTIPLIER_MAX = 300;

    private static final String[] VALID_TYPES = {"NORMAL", "HEAVY", "DEFENSE"};
    private static final String[] VALID_TALENTS = {"MELEE", "ARCHERY", "MAGIC", "COMMON"};
    private static final String[] REQUIRED_FIELDS = {
        "id", "label", "type", "talent", "resourceCost", "description"
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SkillCatalogService skillCatalogService = new SkillCatalogService(objectMapper);

    /**
     * 유효한 스킬 JSON 입력을 파싱하면 올바른 크기의 불변 목록을 반환한다.
     *
     * @param dataset 임의 생성된 유효 스킬 데이터셋
     */
    @Property(tries = 100)
    void should_returnImmutableListWithCorrectSize_when_validInput(
            @ForAll("validSkillDataset") final List<SkillInputData> dataset) {
        final String json = serializeToJson(dataset);
        final InputStream inputStream = toInputStream(json);

        final List<Skill> result = skillCatalogService.loadFromStream(inputStream);

        assertThat(result).hasSize(dataset.size());
        assertThatThrownBy(() -> result.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * 미지 type 문자열이 포함된 입력은 {@link SkillDataException}을 던진다.
     *
     * @param dataset 유효 데이터셋 (결함 주입 전)
     * @param invalidType 미지 type 문자열
     */
    @Property(tries = 100)
    void should_throwSkillDataException_when_unknownType(
            @ForAll("validSkillDataset") final List<SkillInputData> dataset,
            @ForAll("invalidTypeString") final String invalidType) {
        if (dataset.isEmpty()) {
            return;
        }
        final List<SkillInputData> defective = injectUnknownType(dataset, invalidType);
        final String json = serializeToJson(defective);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /**
     * 미지 talent 문자열이 포함된 입력은 {@link SkillDataException}을 던진다.
     *
     * @param dataset 유효 데이터셋 (결함 주입 전)
     * @param invalidTalent 미지 talent 문자열
     */
    @Property(tries = 100)
    void should_throwSkillDataException_when_unknownTalent(
            @ForAll("validSkillDataset") final List<SkillInputData> dataset,
            @ForAll("invalidTalentString") final String invalidTalent) {
        if (dataset.isEmpty()) {
            return;
        }
        final List<SkillInputData> defective = injectUnknownTalent(dataset, invalidTalent);
        final String json = serializeToJson(defective);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /**
     * 중복 id가 포함된 입력은 {@link SkillDataException}을 던진다.
     *
     * @param dataset 유효 데이터셋 (2개 이상)
     */
    @Property(tries = 100)
    void should_throwSkillDataException_when_duplicateId(
            @ForAll("validSkillDatasetAtLeastTwo") final List<SkillInputData> dataset) {
        final List<SkillInputData> defective = injectDuplicateId(dataset);
        final String json = serializeToJson(defective);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /**
     * 필수 필드가 누락된 입력은 {@link SkillDataException}을 던진다.
     *
     * @param dataset 유효 데이터셋 (결함 주입 전)
     * @param fieldToRemove 누락시킬 필드명
     */
    @Property(tries = 100)
    void should_throwSkillDataException_when_requiredFieldMissing(
            @ForAll("validSkillDataset") final List<SkillInputData> dataset,
            @ForAll("requiredFieldName") final String fieldToRemove) {
        if (dataset.isEmpty()) {
            return;
        }
        final String json = serializeToJsonWithMissingField(dataset, fieldToRemove);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /**
     * 랭크맵이 15키(1개 누락)인 입력은 {@link SkillDataException}을 던진다.
     *
     * @param dataset 유효 데이터셋 (결함 주입 전)
     * @param rankToRemove 누락시킬 랭크
     */
    @Property(tries = 100)
    void should_throwSkillDataException_when_rankMapHas15Keys(
            @ForAll("validSkillDataset") final List<SkillInputData> dataset,
            @ForAll("anySkillRank") final SkillRank rankToRemove) {
        if (dataset.isEmpty()) {
            return;
        }
        final String json = serializeToJsonWithMissingRankKey(dataset, rankToRemove);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    // ── Providers ──

    /**
     * 유효한 스킬 데이터셋(유일 id, 유효 type/talent, 16키 랭크맵)을 생성한다.
     *
     * @return 유효 스킬 데이터셋 Arbitrary
     */
    @Provide
    Arbitrary<List<SkillInputData>> validSkillDataset() {
        return Arbitraries.integers()
                .between(1, MAX_SKILL_COUNT)
                .flatMap(this::buildUniqueSkillList);
    }

    /**
     * 최소 2개 스킬을 포함하는 유효 데이터셋(중복 id 테스트용).
     *
     * @return 유효 스킬 데이터셋 Arbitrary (최소 2개)
     */
    @Provide
    Arbitrary<List<SkillInputData>> validSkillDatasetAtLeastTwo() {
        return Arbitraries.integers()
                .between(2, MAX_SKILL_COUNT)
                .flatMap(this::buildUniqueSkillList);
    }

    /**
     * 유효하지 않은 type 문자열을 생성한다.
     *
     * @return 미지 type 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> invalidTypeString() {
        return Arbitraries.of("INVALID", "UNKNOWN", "FIRE", "SPECIAL", "attack", "normal");
    }

    /**
     * 유효하지 않은 talent 문자열을 생성한다.
     *
     * @return 미지 talent 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> invalidTalentString() {
        return Arbitraries.of("INVALID", "UNKNOWN", "FIRE", "WATER", "melee", "archery");
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

    /**
     * 임의의 {@link SkillRank}를 선택한다.
     *
     * @return 스킬 랭크 Arbitrary
     */
    @Provide
    Arbitrary<SkillRank> anySkillRank() {
        return Arbitraries.of(SkillRank.values());
    }

    // ── Helpers ──

    private Arbitrary<List<SkillInputData>> buildUniqueSkillList(final int count) {
        return skillInputDataArbitrary().list().ofSize(count).map(this::ensureUniqueIds);
    }

    private List<SkillInputData> ensureUniqueIds(final List<SkillInputData> rawList) {
        final List<SkillInputData> result = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            final SkillInputData original = rawList.get(i);
            final String uniqueId = original.id() + "_" + i;
            result.add(
                    new SkillInputData(
                            uniqueId,
                            original.label(),
                            original.type(),
                            original.talent(),
                            original.resourceCost(),
                            original.description()));
        }
        return List.copyOf(result);
    }

    private Arbitrary<SkillInputData> skillInputDataArbitrary() {
        final Arbitrary<String> ids =
                Arbitraries.strings().alpha().ofMinLength(ID_MIN_LENGTH).ofMaxLength(ID_MAX_LENGTH);
        final Arbitrary<String> labels =
                Arbitraries.strings()
                        .alpha()
                        .ofMinLength(LABEL_MIN_LENGTH)
                        .ofMaxLength(LABEL_MAX_LENGTH);
        final Arbitrary<String> types = Arbitraries.of(VALID_TYPES);
        final Arbitrary<String> talents = Arbitraries.of(VALID_TALENTS);
        final Arbitrary<Integer> costs =
                Arbitraries.integers().between(RESOURCE_COST_MIN, RESOURCE_COST_MAX);
        final Arbitrary<String> summaries =
                Arbitraries.strings()
                        .alpha()
                        .ofMinLength(LABEL_MIN_LENGTH)
                        .ofMaxLength(LABEL_MAX_LENGTH);

        return Combinators.combine(ids, labels, types, talents, costs, summaries)
                .as(SkillInputData::new);
    }

    private List<SkillInputData> injectUnknownType(
            final List<SkillInputData> dataset, final String invalidType) {
        final List<SkillInputData> result = new ArrayList<>(dataset);
        final SkillInputData first = result.getFirst();
        result.set(
                0,
                new SkillInputData(
                        first.id(),
                        first.label(),
                        invalidType,
                        first.talent(),
                        first.resourceCost(),
                        first.description()));
        return List.copyOf(result);
    }

    private List<SkillInputData> injectUnknownTalent(
            final List<SkillInputData> dataset, final String invalidTalent) {
        final List<SkillInputData> result = new ArrayList<>(dataset);
        final SkillInputData first = result.getFirst();
        result.set(
                0,
                new SkillInputData(
                        first.id(),
                        first.label(),
                        first.type(),
                        invalidTalent,
                        first.resourceCost(),
                        first.description()));
        return List.copyOf(result);
    }

    private List<SkillInputData> injectDuplicateId(final List<SkillInputData> dataset) {
        final List<SkillInputData> result = new ArrayList<>(dataset);
        final SkillInputData first = result.getFirst();
        final SkillInputData second = result.get(1);
        result.set(
                1,
                new SkillInputData(
                        first.id(),
                        second.label(),
                        second.type(),
                        second.talent(),
                        second.resourceCost(),
                        second.description()));
        return List.copyOf(result);
    }

    private String serializeToJson(final List<SkillInputData> dataset) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        for (final SkillInputData data : dataset) {
            final ObjectNode skillNode = buildSkillNode(data);
            rootArray.add(skillNode);
        }
        return rootArray.toString();
    }

    private String serializeToJsonWithMissingField(
            final List<SkillInputData> dataset, final String fieldToRemove) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        for (int i = 0; i < dataset.size(); i++) {
            final SkillInputData data = dataset.get(i);
            final ObjectNode skillNode = buildSkillNode(data);
            if (i == 0) {
                skillNode.remove(fieldToRemove);
            }
            rootArray.add(skillNode);
        }
        return rootArray.toString();
    }

    private String serializeToJsonWithMissingRankKey(
            final List<SkillInputData> dataset, final SkillRank rankToRemove) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        for (int i = 0; i < dataset.size(); i++) {
            final SkillInputData data = dataset.get(i);
            final ObjectNode skillNode = buildSkillNodeWithMissingRank(data, i == 0, rankToRemove);
            rootArray.add(skillNode);
        }
        return rootArray.toString();
    }

    private ObjectNode buildSkillNode(final SkillInputData data) {
        final ObjectNode skillNode = objectMapper.createObjectNode();
        skillNode.put("id", data.id());
        skillNode.put("label", data.label());
        skillNode.put("type", data.type());
        skillNode.put("talent", data.talent());
        skillNode.put("resourceCost", data.resourceCost());
        skillNode.put("description", data.description());

        if ("DEFENSE".equals(data.type())) {
            addFullRankMap(skillNode, "blockRateByRank");
            addFullRankMap(skillNode, "counterMultiplierByRank");
        } else {
            addFullRankMap(skillNode, "multiplierByRank");
        }
        return skillNode;
    }

    private ObjectNode buildSkillNodeWithMissingRank(
            final SkillInputData data, final boolean injectDefect, final SkillRank rankToRemove) {
        final ObjectNode skillNode = objectMapper.createObjectNode();
        skillNode.put("id", data.id());
        skillNode.put("label", data.label());
        skillNode.put("type", data.type());
        skillNode.put("talent", data.talent());
        skillNode.put("resourceCost", data.resourceCost());
        skillNode.put("description", data.description());

        if ("DEFENSE".equals(data.type())) {
            if (injectDefect) {
                addRankMapMissingOne(skillNode, "blockRateByRank", rankToRemove);
                addFullRankMap(skillNode, "counterMultiplierByRank");
            } else {
                addFullRankMap(skillNode, "blockRateByRank");
                addFullRankMap(skillNode, "counterMultiplierByRank");
            }
        } else {
            if (injectDefect) {
                addRankMapMissingOne(skillNode, "multiplierByRank", rankToRemove);
            } else {
                addFullRankMap(skillNode, "multiplierByRank");
            }
        }
        return skillNode;
    }

    private void addFullRankMap(final ObjectNode parent, final String fieldName) {
        final ObjectNode rankMap = parent.putObject(fieldName);
        int value = MULTIPLIER_MIN;
        for (final SkillRank rank : SkillRank.values()) {
            rankMap.put(rank.name(), value);
            value += 5;
        }
    }

    private void addRankMapMissingOne(
            final ObjectNode parent, final String fieldName, final SkillRank rankToRemove) {
        final ObjectNode rankMap = parent.putObject(fieldName);
        int value = MULTIPLIER_MIN;
        for (final SkillRank rank : SkillRank.values()) {
            if (rank != rankToRemove) {
                rankMap.put(rank.name(), value);
            }
            value += 5;
        }
    }

    private InputStream toInputStream(final String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 프로퍼티 테스트용 스킬 입력 데이터 레코드.
     *
     * @param id 스킬 고유 식별자
     * @param label 표시용 라벨
     * @param type 스킬 타입 문자열
     * @param talent 스킬 재능 문자열
     * @param resourceCost 자원 소모량
     * @param description 스킬 설명
     */
    record SkillInputData(
            String id,
            String label,
            String type,
            String talent,
            int resourceCost,
            String description) {}
}
