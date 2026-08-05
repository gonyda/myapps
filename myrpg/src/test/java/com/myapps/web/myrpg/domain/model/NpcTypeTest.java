package com.myapps.web.myrpg.domain.model;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NPC 타입의 실제 매핑값이 요구사항 표와 정확히 일치하는지 검증하는 단위 테스트.
 *
 * <p>6개 타입의 {@code typeString}→{@code label}·{@code actionLabels} 매핑과
 * {@code fromType}의 미지 타입 처리를 확인한다.
 *
 * <p><b>Validates: Requirements 4.3, 5.4</b>
 */
class NpcTypeTest {

    /**
     * chief 타입의 라벨은 "촌장"이고 행동 라벨은 ["퀘스트"]임을 검증한다.
     */
    @Test
    void should_returnCorrectLabelAndActions_when_chief() {
        final Optional<NpcType> result = NpcType.fromType("chief");

        assertThat(result).isPresent();
        final NpcType npcType = result.get();
        assertThat(npcType).isEqualTo(NpcType.CHIEF);
        assertThat(npcType.label()).isEqualTo("촌장");
        assertThat(npcType.actionLabels()).isEqualTo(List.of("퀘스트"));
    }

    /**
     * blacksmith 타입의 라벨은 "대장간"이고 행동 라벨은 ["상점", "수리"]임을 검증한다.
     */
    @Test
    void should_returnCorrectLabelAndActions_when_blacksmith() {
        final Optional<NpcType> result = NpcType.fromType("blacksmith");

        assertThat(result).isPresent();
        final NpcType npcType = result.get();
        assertThat(npcType).isEqualTo(NpcType.BLACKSMITH);
        assertThat(npcType.label()).isEqualTo("대장간");
        assertThat(npcType.actionLabels()).isEqualTo(List.of("상점", "수리"));
    }

    /**
     * magic-school 타입의 라벨은 "마법학교"이고 행동 라벨은 ["상점"]임을 검증한다.
     */
    @Test
    void should_returnCorrectLabelAndActions_when_magicSchool() {
        final Optional<NpcType> result = NpcType.fromType("magic-school");

        assertThat(result).isPresent();
        final NpcType npcType = result.get();
        assertThat(npcType).isEqualTo(NpcType.MAGIC_SCHOOL);
        assertThat(npcType.label()).isEqualTo("마법학교");
        assertThat(npcType.actionLabels()).isEqualTo(List.of("상점"));
    }

    /**
     * school 타입의 라벨은 "학교"이고 행동 라벨은 ["상점"]임을 검증한다.
     */
    @Test
    void should_returnCorrectLabelAndActions_when_school() {
        final Optional<NpcType> result = NpcType.fromType("school");

        assertThat(result).isPresent();
        final NpcType npcType = result.get();
        assertThat(npcType).isEqualTo(NpcType.SCHOOL);
        assertThat(npcType.label()).isEqualTo("학교");
        assertThat(npcType.actionLabels()).isEqualTo(List.of("상점"));
    }

    /**
     * healer 타입의 라벨은 "힐러집"이고 행동 라벨은 ["상점", "치료받기"]임을 검증한다.
     */
    @Test
    void should_returnCorrectLabelAndActions_when_healer() {
        final Optional<NpcType> result = NpcType.fromType("healer");

        assertThat(result).isPresent();
        final NpcType npcType = result.get();
        assertThat(npcType).isEqualTo(NpcType.HEALER);
        assertThat(npcType.label()).isEqualTo("힐러집");
        assertThat(npcType.actionLabels()).isEqualTo(List.of("상점", "치료받기"));
    }

    /**
     * bank 타입의 라벨은 "은행"이고 행동 라벨은 ["아이템 보관", "골드 입/출금"]임을 검증한다.
     */
    @Test
    void should_returnCorrectLabelAndActions_when_bank() {
        final Optional<NpcType> result = NpcType.fromType("bank");

        assertThat(result).isPresent();
        final NpcType npcType = result.get();
        assertThat(npcType).isEqualTo(NpcType.BANK);
        assertThat(npcType.label()).isEqualTo("은행");
        assertThat(npcType.actionLabels()).isEqualTo(List.of("아이템 보관", "골드 입/출금"));
    }

    /**
     * 미지 타입 문자열에 대해 fromType은 빈 Optional을 반환함을 검증한다.
     */
    @Test
    void should_returnEmpty_when_unknownType() {
        assertThat(NpcType.fromType("unknown")).isEmpty();
        assertThat(NpcType.fromType("warrior")).isEmpty();
        assertThat(NpcType.fromType("")).isEmpty();
    }

    /**
     * null 입력에 대해 fromType은 빈 Optional을 반환함을 검증한다.
     */
    @Test
    void should_returnEmpty_when_nullType() {
        assertThat(NpcType.fromType(null)).isEmpty();
    }
}
