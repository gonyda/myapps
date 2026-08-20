package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.exception.SkillDataException;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 스킬 카탈로그의 hitCount·critBonus 파싱 검증 프로퍼티 테스트.
 *
 * <p>hitCount/critBonus 부재 시 기본값(1/0)으로 로드, 범위 밖이거나 숫자가 아닌 값이면 {@link SkillDataException} 발생,
 * multiplierByRank 16키·단조 검증 유지를 확인한다.
 *
 * <p>Feature: 009-skill-differentiation-and-battle-log, Property 6: 카탈로그 파싱 기본값·검증
 *
 * <p><b>Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.6</b>
 */
class SkillCatalogHitCountCritBonusPropertyTest {

    private static final int DEFAULT_HIT_COUNT = 1;
    private static final int MIN_HIT_COUNT = 1;
    private static final int MAX_HIT_COUNT = 8;
    private static final int DEFAULT_CRIT_BONUS = 0;
    private static final int MIN_CRIT_BONUS = 0;
    private static final int MAX_CRIT_BONUS = 100;
    private static final int MULTIPLIER_BASE = 90;
    private static final int MULTIPLIER_INCREMENT = 5;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SkillCatalogService skillCatalogService = new SkillCatalogService(objectMapper);

    /** hitCount와 critBonus가 부재하면 기본값(hitCount=1, critBonus=0)으로 로드된다. */
    @Property(tries = 100)
    void should_loadWithDefaults_when_hitCountAndCritBonusAbsent() {
        final String json = buildSingleDamageSkillJson(null, null);
        final InputStream inputStream = toInputStream(json);

        final List<Skill> result = skillCatalogService.loadFromStream(inputStream);

        assertThat(result).hasSize(1);
        final DamageSkill skill = (DamageSkill) result.getFirst();
        assertThat(skill.hitCount()).isEqualTo(DEFAULT_HIT_COUNT);
        assertThat(skill.critBonus()).isEqualTo(DEFAULT_CRIT_BONUS);
    }

    /**
     * 유효 범위 내의 hitCount와 critBonus가 올바르게 파싱된다.
     *
     * @param hitCount 유효 범위 [1,8] 내 hitCount
     * @param critBonus 유효 범위 [0,100] 내 critBonus
     */
    @Property(tries = 100)
    void should_parseCorrectly_when_hitCountAndCritBonusInValidRange(
            @ForAll("validHitCount") final int hitCount,
            @ForAll("validCritBonus") final int critBonus) {
        final String json = buildSingleDamageSkillJson(hitCount, critBonus);
        final InputStream inputStream = toInputStream(json);

        final List<Skill> result = skillCatalogService.loadFromStream(inputStream);

        assertThat(result).hasSize(1);
        final DamageSkill skill = (DamageSkill) result.getFirst();
        assertThat(skill.hitCount()).isEqualTo(hitCount);
        assertThat(skill.critBonus()).isEqualTo(critBonus);
    }

    /**
     * hitCount가 1 미만이면 {@link SkillDataException}이 발생한다.
     *
     * @param invalidHitCount 0 이하의 hitCount 값
     */
    @Property(tries = 100)
    void should_throwException_when_hitCountBelowMin(
            @ForAll("hitCountBelowMin") final int invalidHitCount) {
        final String json = buildSingleDamageSkillJson(invalidHitCount, DEFAULT_CRIT_BONUS);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /**
     * hitCount가 8 초과이면 {@link SkillDataException}이 발생한다.
     *
     * @param invalidHitCount 9 이상의 hitCount 값
     */
    @Property(tries = 100)
    void should_throwException_when_hitCountAboveMax(
            @ForAll("hitCountAboveMax") final int invalidHitCount) {
        final String json = buildSingleDamageSkillJson(invalidHitCount, DEFAULT_CRIT_BONUS);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /**
     * critBonus가 0 미만이면 {@link SkillDataException}이 발생한다.
     *
     * @param invalidCritBonus 음수의 critBonus 값
     */
    @Property(tries = 100)
    void should_throwException_when_critBonusBelowMin(
            @ForAll("critBonusBelowMin") final int invalidCritBonus) {
        final String json = buildSingleDamageSkillJson(DEFAULT_HIT_COUNT, invalidCritBonus);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /**
     * critBonus가 100 초과이면 {@link SkillDataException}이 발생한다.
     *
     * @param invalidCritBonus 101 이상의 critBonus 값
     */
    @Property(tries = 100)
    void should_throwException_when_critBonusAboveMax(
            @ForAll("critBonusAboveMax") final int invalidCritBonus) {
        final String json = buildSingleDamageSkillJson(DEFAULT_HIT_COUNT, invalidCritBonus);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /**
     * hitCount 필드가 숫자가 아닌 문자열이면 {@link SkillDataException}이 발생한다.
     *
     * @param nonNumericValue 숫자가 아닌 문자열
     */
    @Property(tries = 100)
    void should_throwException_when_hitCountNotNumeric(
            @ForAll("nonNumericString") final String nonNumericValue) {
        final String json = buildSkillJsonWithStringField("hitCount", nonNumericValue);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /**
     * critBonus 필드가 숫자가 아닌 문자열이면 {@link SkillDataException}이 발생한다.
     *
     * @param nonNumericValue 숫자가 아닌 문자열
     */
    @Property(tries = 100)
    void should_throwException_when_critBonusNotNumeric(
            @ForAll("nonNumericString") final String nonNumericValue) {
        final String json = buildSkillJsonWithStringField("critBonus", nonNumericValue);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /**
     * hitCount/critBonus가 유효하더라도 multiplierByRank 16키 검증은 유지된다.
     *
     * @param rankToRemove 누락시킬 랭크 키
     */
    @Property(tries = 100)
    void should_throwException_when_rankMapIncomplete(
            @ForAll("anySkillRank") final SkillRank rankToRemove) {
        final String json = buildSkillJsonWithMissingRankKey(rankToRemove);
        final InputStream inputStream = toInputStream(json);

        assertThatThrownBy(() -> skillCatalogService.loadFromStream(inputStream))
                .isInstanceOf(SkillDataException.class);
    }

    /** 방어 스킬 노드에 hitCount/critBonus가 존재해도 무시되고 정상 파싱된다. */
    @Property(tries = 100)
    void should_ignoreHitCountCritBonus_when_defenseSkill(
            @ForAll("validHitCount") final int hitCount,
            @ForAll("validCritBonus") final int critBonus) {
        final String json = buildDefenseSkillJsonWithHitCountCritBonus(hitCount, critBonus);
        final InputStream inputStream = toInputStream(json);

        final List<Skill> result = skillCatalogService.loadFromStream(inputStream);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isNotInstanceOf(DamageSkill.class);
    }

    // ── Providers ──

    /**
     * 유효 범위 [1, 8] 내 hitCount를 생성한다.
     *
     * @return hitCount Arbitrary
     */
    @Provide
    Arbitrary<Integer> validHitCount() {
        return Arbitraries.integers().between(MIN_HIT_COUNT, MAX_HIT_COUNT);
    }

    /**
     * 유효 범위 [0, 100] 내 critBonus를 생성한다.
     *
     * @return critBonus Arbitrary
     */
    @Provide
    Arbitrary<Integer> validCritBonus() {
        return Arbitraries.integers().between(MIN_CRIT_BONUS, MAX_CRIT_BONUS);
    }

    /**
     * 최솟값 미만(0 이하)의 hitCount를 생성한다.
     *
     * @return 유효하지 않은 hitCount Arbitrary
     */
    @Provide
    Arbitrary<Integer> hitCountBelowMin() {
        return Arbitraries.integers().between(-100, 0);
    }

    /**
     * 최댓값 초과(9 이상)의 hitCount를 생성한다.
     *
     * @return 유효하지 않은 hitCount Arbitrary
     */
    @Provide
    Arbitrary<Integer> hitCountAboveMax() {
        return Arbitraries.integers().between(MAX_HIT_COUNT + 1, 200);
    }

    /**
     * 최솟값 미만(음수)의 critBonus를 생성한다.
     *
     * @return 유효하지 않은 critBonus Arbitrary
     */
    @Provide
    Arbitrary<Integer> critBonusBelowMin() {
        return Arbitraries.integers().between(-100, -1);
    }

    /**
     * 최댓값 초과(101 이상)의 critBonus를 생성한다.
     *
     * @return 유효하지 않은 critBonus Arbitrary
     */
    @Provide
    Arbitrary<Integer> critBonusAboveMax() {
        return Arbitraries.integers().between(MAX_CRIT_BONUS + 1, 500);
    }

    /**
     * 숫자가 아닌 문자열을 생성한다.
     *
     * @return 비숫자 문자열 Arbitrary
     */
    @Provide
    Arbitrary<String> nonNumericString() {
        return Arbitraries.of("abc", "true", "null", "three", "NaN", "");
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

    private String buildSingleDamageSkillJson(final Integer hitCount, final Integer critBonus) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        final ObjectNode skillNode = buildBaseDamageSkillNode();
        if (hitCount != null) {
            skillNode.put("hitCount", hitCount);
        }
        if (critBonus != null) {
            skillNode.put("critBonus", critBonus);
        }
        rootArray.add(skillNode);
        return rootArray.toString();
    }

    private String buildSkillJsonWithStringField(final String fieldName, final String stringValue) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        final ObjectNode skillNode = buildBaseDamageSkillNode();
        skillNode.put(fieldName, stringValue);
        rootArray.add(skillNode);
        return rootArray.toString();
    }

    private String buildSkillJsonWithMissingRankKey(final SkillRank rankToRemove) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        final ObjectNode skillNode = objectMapper.createObjectNode();
        skillNode.put("id", "test_skill");
        skillNode.put("label", "테스트");
        skillNode.put("type", "NORMAL");
        skillNode.put("talent", "MELEE");
        skillNode.put("resourceCost", 5);
        skillNode.put("description", "테스트 스킬");
        skillNode.put("hitCount", 1);
        skillNode.put("critBonus", 0);

        final ObjectNode rankMap = skillNode.putObject("multiplierByRank");
        int value = MULTIPLIER_BASE;
        for (final SkillRank rank : SkillRank.values()) {
            if (rank != rankToRemove) {
                rankMap.put(rank.name(), value);
            }
            value += MULTIPLIER_INCREMENT;
        }
        rootArray.add(skillNode);
        return rootArray.toString();
    }

    private String buildDefenseSkillJsonWithHitCountCritBonus(
            final int hitCount, final int critBonus) {
        final ArrayNode rootArray = objectMapper.createArrayNode();
        final ObjectNode skillNode = objectMapper.createObjectNode();
        skillNode.put("id", "defense_test");
        skillNode.put("label", "방어");
        skillNode.put("type", "DEFENSE");
        skillNode.put("talent", "COMMON");
        skillNode.put("resourceCost", 3);
        skillNode.put("description", "방어 스킬");
        skillNode.put("hitCount", hitCount);
        skillNode.put("critBonus", critBonus);

        addFullRankMap(skillNode, "blockRateByRank");
        addFullRankMap(skillNode, "counterMultiplierByRank");

        rootArray.add(skillNode);
        return rootArray.toString();
    }

    private ObjectNode buildBaseDamageSkillNode() {
        final ObjectNode skillNode = objectMapper.createObjectNode();
        skillNode.put("id", "test_skill");
        skillNode.put("label", "테스트");
        skillNode.put("type", "NORMAL");
        skillNode.put("talent", "MELEE");
        skillNode.put("resourceCost", 5);
        skillNode.put("description", "테스트 스킬");
        addFullRankMap(skillNode, "multiplierByRank");
        return skillNode;
    }

    private void addFullRankMap(final ObjectNode parent, final String fieldName) {
        final ObjectNode rankMap = parent.putObject(fieldName);
        int value = MULTIPLIER_BASE;
        for (final SkillRank rank : SkillRank.values()) {
            rankMap.put(rank.name(), value);
            value += MULTIPLIER_INCREMENT;
        }
    }

    private InputStream toInputStream(final String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
