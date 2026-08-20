package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.exception.MonsterDataException;
import com.myapps.web.myrpg.domain.model.Item;
import com.myapps.web.myrpg.domain.model.MapGraph;
import com.myapps.web.myrpg.domain.model.PotionItem;
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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 몬스터 카탈로그 검증 실패 프로퍼티 테스트.
 *
 * <p>유효한 몬스터 데이터셋에 결함(중복 id, 미지 type, 필드 누락, 범위 위반, lines≠3)을 주입하여 {@code loadFromStream}이 {@link
 * MonsterDataException}을 던짐을 검증한다.
 *
 * <p>Feature: 007-monster-system, Property 4: 카탈로그 검증 실패
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6</b>
 */
class MonsterServiceLoadFailurePropertyTest {

    private static final int MAX_MONSTER_COUNT = 5;
    private static final int MIN_MONSTER_COUNT = 2;
    private static final int ID_MIN_LENGTH = 3;
    private static final int ID_MAX_LENGTH = 12;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 8;
    private static final int LINE_MIN_LENGTH = 2;
    private static final int LINE_MAX_LENGTH = 15;

    private static final String[] VALID_TYPES = {"normal", "boss"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 유효 데이터셋에 {@code id} 중복을 주입하면 {@code MonsterDataException}이 발생함을 검증한다.
     *
     * @param dataset 유효 몬스터 데이터셋(최소 2개, 유일 id)
     * @param duplicateTargetIndex 중복 id를 주입할 대상 항목의 인덱스
     */
    @Property(tries = 100)
    void should_throwMonsterDataException_when_duplicateId(
            @ForAll("validMonsterDataset") final List<MonsterInputData> dataset,
            @ForAll("targetIndex") final int duplicateTargetIndex) {

        final int targetIndex = 1 + (duplicateTargetIndex % (dataset.size() - 1));
        final List<MonsterInputData> corrupted = injectDuplicateId(dataset, 0, targetIndex);

        final String json = serializeToJson(corrupted);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final MonsterService monsterService = buildServiceWithMocks();

        assertThatThrownBy(() -> monsterService.loadFromStream(inputStream))
                .isInstanceOf(MonsterDataException.class);
    }

    /**
     * 유효 데이터셋에 미지 {@code type}을 주입하면 {@code MonsterDataException}이 발생함을 검증한다.
     *
     * @param dataset 유효 몬스터 데이터셋
     * @param corruptionIndex 미지 type을 주입할 항목 인덱스
     * @param unknownType 유효 타입에 속하지 않는 임의 문자열
     */
    @Property(tries = 100)
    void should_throwMonsterDataException_when_unknownType(
            @ForAll("validMonsterDataset") final List<MonsterInputData> dataset,
            @ForAll("targetIndex") final int corruptionIndex,
            @ForAll("unknownType") final String unknownType) {

        final int targetIdx = corruptionIndex % dataset.size();
        final List<MonsterInputData> corrupted = injectUnknownType(dataset, targetIdx, unknownType);

        final String json = serializeToJson(corrupted);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final MonsterService monsterService = buildServiceWithMocks();

        assertThatThrownBy(() -> monsterService.loadFromStream(inputStream))
                .isInstanceOf(MonsterDataException.class);
    }

    /**
     * 유효 데이터셋에 필수 필드 누락을 주입하면 {@code MonsterDataException}이 발생함을 검증한다.
     *
     * <p>필수 필드(id, name, type, level, maxHp, attackPower, defense, critical, experience) 중 하나를
     * null이나 누락시켜 검증한다.
     *
     * @param dataset 유효 몬스터 데이터셋
     * @param corruptionIndex 필드를 누락시킬 항목 인덱스
     * @param fieldIndex 누락시킬 필드 인덱스 (0~2: 문자열 필드)
     */
    @Property(tries = 100)
    void should_throwMonsterDataException_when_requiredFieldMissing(
            @ForAll("validMonsterDataset") final List<MonsterInputData> dataset,
            @ForAll("targetIndex") final int corruptionIndex,
            @ForAll("stringFieldIndex") final int fieldIndex) {

        final int targetIdx = corruptionIndex % dataset.size();
        final String json = serializeWithMissingField(dataset, targetIdx, fieldIndex);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final MonsterService monsterService = buildServiceWithMocks();

        assertThatThrownBy(() -> monsterService.loadFromStream(inputStream))
                .isInstanceOf(MonsterDataException.class);
    }

    /**
     * 유효 데이터셋에 goldDrop 범위 위반(min > max 또는 min < 0)을 주입하면 {@code MonsterDataException}이 발생함을 검증한다.
     *
     * @param dataset 유효 몬스터 데이터셋
     * @param corruptionIndex 범위를 위반시킬 항목 인덱스
     * @param violationType 0: min > max, 1: min < 0
     */
    @Property(tries = 100)
    void should_throwMonsterDataException_when_goldDropRangeViolation(
            @ForAll("validMonsterDataset") final List<MonsterInputData> dataset,
            @ForAll("targetIndex") final int corruptionIndex,
            @ForAll("violationType") final int violationType) {

        final int targetIdx = corruptionIndex % dataset.size();
        final List<MonsterInputData> corrupted =
                injectGoldDropViolation(dataset, targetIdx, violationType);

        final String json = serializeToJson(corrupted);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final MonsterService monsterService = buildServiceWithMocks();

        assertThatThrownBy(() -> monsterService.loadFromStream(inputStream))
                .isInstanceOf(MonsterDataException.class);
    }

    /**
     * 유효 데이터셋에 chancePercent 범위 위반(0 또는 101 이상)을 주입하면 {@code MonsterDataException}이 발생함을 검증한다.
     *
     * @param dataset 유효 몬스터 데이터셋
     * @param corruptionIndex 범위를 위반시킬 항목 인덱스
     * @param invalidChance 유효 범위 밖의 chancePercent 값
     */
    @Property(tries = 100)
    void should_throwMonsterDataException_when_chancePercentOutOfRange(
            @ForAll("validMonsterDataset") final List<MonsterInputData> dataset,
            @ForAll("targetIndex") final int corruptionIndex,
            @ForAll("invalidChancePercent") final int invalidChance) {

        final int targetIdx = corruptionIndex % dataset.size();
        final List<MonsterInputData> corrupted =
                injectInvalidChancePercent(dataset, targetIdx, invalidChance);

        final String json = serializeToJson(corrupted);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final MonsterService monsterService = buildServiceWithMocks();

        assertThatThrownBy(() -> monsterService.loadFromStream(inputStream))
                .isInstanceOf(MonsterDataException.class);
    }

    /**
     * 유효 데이터셋에 lines 개수 위반(3개가 아닌 경우)을 주입하면 {@code MonsterDataException}이 발생함을 검증한다.
     *
     * @param dataset 유효 몬스터 데이터셋
     * @param corruptionIndex lines를 위반시킬 항목 인덱스
     * @param invalidLineCount 3이 아닌 lines 개수 (0~2 또는 4~6)
     */
    @Property(tries = 100)
    void should_throwMonsterDataException_when_linesCountNotThree(
            @ForAll("validMonsterDataset") final List<MonsterInputData> dataset,
            @ForAll("targetIndex") final int corruptionIndex,
            @ForAll("invalidLineCount") final int invalidLineCount) {

        final int targetIdx = corruptionIndex % dataset.size();
        final List<MonsterInputData> corrupted =
                injectInvalidLinesCount(dataset, targetIdx, invalidLineCount);

        final String json = serializeToJson(corrupted);
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        final MonsterService monsterService = buildServiceWithMocks();

        assertThatThrownBy(() -> monsterService.loadFromStream(inputStream))
                .isInstanceOf(MonsterDataException.class);
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
                .between(MIN_MONSTER_COUNT, MAX_MONSTER_COUNT)
                .flatMap(this::buildUniqueMonsterList);
    }

    /**
     * 데이터셋 내 항목 인덱스를 선택하는 Arbitrary 제공자.
     *
     * @return 비음수 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> targetIndex() {
        return Arbitraries.integers().between(0, MAX_MONSTER_COUNT - 1);
    }

    /**
     * 필수 문자열 필드 인덱스를 선택하는 Arbitrary 제공자 (0=id, 1=name, 2=type).
     *
     * @return 0~2 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> stringFieldIndex() {
        return Arbitraries.integers().between(0, 2);
    }

    /**
     * 유효 타입에 속하지 않는 임의 문자열을 생성하는 Arbitrary 제공자.
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

    /**
     * goldDrop 위반 유형을 선택하는 Arbitrary 제공자 (0: min > max, 1: min < 0).
     *
     * @return 0~1 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> violationType() {
        return Arbitraries.integers().between(0, 1);
    }

    /**
     * 유효 범위 밖의 chancePercent 값을 생성하는 Arbitrary 제공자.
     *
     * @return 0 또는 101~200 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> invalidChancePercent() {
        return Arbitraries.oneOf(Arbitraries.just(0), Arbitraries.integers().between(101, 200));
    }

    /**
     * 3이 아닌 lines 개수를 생성하는 Arbitrary 제공자.
     *
     * @return 0~2 또는 4~6 범위의 정수 Arbitrary
     */
    @Provide
    Arbitrary<Integer> invalidLineCount() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(0, 2), Arbitraries.integers().between(4, 6));
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
                            original.itemDrops(),
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
                                        List.of(),
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
    // Corruption Helpers
    // ──────────────────────────────────────────────────────────────────────

    private List<MonsterInputData> injectDuplicateId(
            final List<MonsterInputData> dataset, final int sourceIndex, final int targetIndex) {

        final List<MonsterInputData> result = new ArrayList<>(dataset);
        final MonsterInputData source = result.get(sourceIndex);
        final MonsterInputData target = result.get(targetIndex);

        final MonsterInputData duplicated =
                new MonsterInputData(
                        source.id(),
                        target.name(),
                        target.typeString(),
                        target.level(),
                        target.maxHp(),
                        target.attackPower(),
                        target.defense(),
                        target.critical(),
                        target.experience(),
                        target.goldDropMin(),
                        target.goldDropMax(),
                        target.itemDrops(),
                        target.lines());

        result.set(targetIndex, duplicated);
        return List.copyOf(result);
    }

    private List<MonsterInputData> injectUnknownType(
            final List<MonsterInputData> dataset, final int targetIndex, final String unknownType) {

        final List<MonsterInputData> result = new ArrayList<>(dataset);
        final MonsterInputData original = result.get(targetIndex);

        final MonsterInputData corrupted =
                new MonsterInputData(
                        original.id(),
                        original.name(),
                        unknownType,
                        original.level(),
                        original.maxHp(),
                        original.attackPower(),
                        original.defense(),
                        original.critical(),
                        original.experience(),
                        original.goldDropMin(),
                        original.goldDropMax(),
                        original.itemDrops(),
                        original.lines());

        result.set(targetIndex, corrupted);
        return List.copyOf(result);
    }

    private List<MonsterInputData> injectGoldDropViolation(
            final List<MonsterInputData> dataset, final int targetIndex, final int violationType) {

        final List<MonsterInputData> result = new ArrayList<>(dataset);
        final MonsterInputData original = result.get(targetIndex);

        final int corruptedMin;
        final int corruptedMax;
        if (violationType == 0) {
            // min > max
            corruptedMin = 50;
            corruptedMax = 10;
        } else {
            // min < 0
            corruptedMin = -5;
            corruptedMax = 10;
        }

        final MonsterInputData corrupted =
                new MonsterInputData(
                        original.id(),
                        original.name(),
                        original.typeString(),
                        original.level(),
                        original.maxHp(),
                        original.attackPower(),
                        original.defense(),
                        original.critical(),
                        original.experience(),
                        corruptedMin,
                        corruptedMax,
                        original.itemDrops(),
                        original.lines());

        result.set(targetIndex, corrupted);
        return List.copyOf(result);
    }

    private List<MonsterInputData> injectInvalidChancePercent(
            final List<MonsterInputData> dataset, final int targetIndex, final int invalidChance) {

        final List<MonsterInputData> result = new ArrayList<>(dataset);
        final MonsterInputData original = result.get(targetIndex);

        // 아이템 드랍을 하나 추가하되 유효하지 않은 chancePercent 사용
        final List<ItemDropInputData> corruptedDrops =
                List.of(new ItemDropInputData("hp_potion_50", invalidChance, 1, 1));

        final MonsterInputData corrupted =
                new MonsterInputData(
                        original.id(),
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
                        corruptedDrops,
                        original.lines());

        result.set(targetIndex, corrupted);
        return List.copyOf(result);
    }

    private List<MonsterInputData> injectInvalidLinesCount(
            final List<MonsterInputData> dataset, final int targetIndex, final int lineCount) {

        final List<MonsterInputData> result = new ArrayList<>(dataset);
        final MonsterInputData original = result.get(targetIndex);

        final List<String> invalidLines = new ArrayList<>();
        for (int i = 0; i < lineCount; i++) {
            invalidLines.add("line" + i);
        }

        final MonsterInputData corrupted =
                new MonsterInputData(
                        original.id(),
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
                        original.itemDrops(),
                        List.copyOf(invalidLines));

        result.set(targetIndex, corrupted);
        return List.copyOf(result);
    }

    private String serializeWithMissingField(
            final List<MonsterInputData> dataset, final int targetIndex, final int fieldIndex) {

        final ArrayNode rootArray = objectMapper.createArrayNode();

        for (int i = 0; i < dataset.size(); i++) {
            final MonsterInputData data = dataset.get(i);
            final ObjectNode monsterNode = rootArray.addObject();

            if (i == targetIndex && fieldIndex == 0) {
                monsterNode.putNull("id");
            } else {
                monsterNode.put("id", data.id());
            }

            if (i == targetIndex && fieldIndex == 1) {
                monsterNode.putNull("name");
            } else {
                monsterNode.put("name", data.name());
            }

            if (i == targetIndex && fieldIndex == 2) {
                monsterNode.putNull("type");
            } else {
                monsterNode.put("type", data.typeString());
            }

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

            if (!data.itemDrops().isEmpty()) {
                final ArrayNode itemDropsArray = monsterNode.putArray("itemDrops");
                for (final ItemDropInputData drop : data.itemDrops()) {
                    final ObjectNode dropNode = itemDropsArray.addObject();
                    dropNode.put("itemId", drop.itemId());
                    dropNode.put("chancePercent", drop.chancePercent());
                    dropNode.put("minQuantity", drop.minQuantity());
                    dropNode.put("maxQuantity", drop.maxQuantity());
                }
            }

            final ArrayNode linesArray = monsterNode.putArray("lines");
            for (final String line : data.lines()) {
                linesArray.add(line);
            }
        }

        return rootArray.toString();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────────────────────────────

    private boolean isValidType(final String type) {
        for (final String validType : VALID_TYPES) {
            if (validType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Input Data Records
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
     * @param itemDrops 아이템 드랍 목록
     * @param lines 조우 대사 목록
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
            List<String> lines) {}

    /**
     * 프로퍼티 테스트용 아이템 드랍 입력 데이터 레코드.
     *
     * @param itemId 아이템 ID
     * @param chancePercent 드랍 확률
     * @param minQuantity 최소 수량
     * @param maxQuantity 최대 수량
     */
    record ItemDropInputData(String itemId, int chancePercent, int minQuantity, int maxQuantity) {}
}
