package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.exception.SkillDataException;
import com.myapps.web.myrpg.domain.model.DamageSkill;
import com.myapps.web.myrpg.domain.model.DefenseSkill;
import com.myapps.web.myrpg.domain.model.Skill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.SkillTalent;
import com.myapps.web.myrpg.domain.model.SkillType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link SkillCatalogService}의 파싱·검증 로직 단위 테스트.
 *
 * <p>인메모리 JSON을 주입하여 정상 파싱, 필수 필드 누락, 미지 enum, 중복 id, 랭크 맵 키 부족 시나리오를 검증합니다.
 */
class SkillCatalogServiceTest {

    private SkillCatalogService skillCatalogService;

    @BeforeEach
    void setUp() {
        skillCatalogService = new SkillCatalogService(new ObjectMapper());
    }

    @Test
    void should_parseDamageSkill_when_validNormalSkillJson() {
        final String json =
                """
                [{
                  "id": "windmill",
                  "label": "윈드밀",
                  "type": "NORMAL",
                  "talent": "MELEE",
                  "resourceCost": 7,
                  "multiplierByRank": {
                    "F":90,"E":95,"D":101,"C":106,"B":112,"A":117,
                    "R9":123,"R8":128,"R7":134,"R6":139,"R5":145,"R4":150,
                    "R3":156,"R2":161,"R1":166,"MASTER":170
                  },
                  "description": "회전 베기"
                }]
                """;

        final List<Skill> skills = loadFromString(json);

        assertThat(skills).hasSize(1);
        final Skill skill = skills.getFirst();
        assertThat(skill).isInstanceOf(DamageSkill.class);
        assertThat(skill.id()).isEqualTo("windmill");
        assertThat(skill.label()).isEqualTo("윈드밀");
        assertThat(skill.type()).isEqualTo(SkillType.NORMAL);
        assertThat(skill.talent()).isEqualTo(SkillTalent.MELEE);
        assertThat(skill.resourceCost()).isEqualTo(7);

        final DamageSkill damageSkill = (DamageSkill) skill;
        assertThat(damageSkill.multiplierByRank()).hasSize(16);
        assertThat(damageSkill.multiplierByRank().get(SkillRank.F)).isEqualTo(90);
        assertThat(damageSkill.multiplierByRank().get(SkillRank.MASTER)).isEqualTo(170);
    }

    @Test
    void should_parseDefenseSkill_when_validDefenseSkillJson() {
        final String json =
                """
                [{
                  "id": "defense",
                  "label": "디펜스",
                  "type": "DEFENSE",
                  "talent": "COMMON",
                  "resourceCost": 4,
                  "blockRateByRank": {
                    "F":50,"E":52,"D":55,"C":57,"B":59,"A":61,
                    "R9":64,"R8":66,"R7":68,"R6":70,"R5":73,"R4":75,
                    "R3":77,"R2":80,"R1":82,"MASTER":85
                  },
                  "counterMultiplierByRank": {
                    "F":30,"E":35,"D":39,"C":44,"B":49,"A":53,
                    "R9":58,"R8":63,"R7":67,"R6":72,"R5":77,"R4":81,
                    "R3":86,"R2":91,"R1":95,"MASTER":100
                  },
                  "description": "방어 후 반격"
                }]
                """;

        final List<Skill> skills = loadFromString(json);

        assertThat(skills).hasSize(1);
        final Skill skill = skills.getFirst();
        assertThat(skill).isInstanceOf(DefenseSkill.class);
        assertThat(skill.type()).isEqualTo(SkillType.DEFENSE);
        assertThat(skill.talent()).isEqualTo(SkillTalent.COMMON);

        final DefenseSkill defenseSkill = (DefenseSkill) skill;
        assertThat(defenseSkill.blockRateByRank()).hasSize(16);
        assertThat(defenseSkill.counterMultiplierByRank()).hasSize(16);
        assertThat(defenseSkill.blockRateByRank().get(SkillRank.MASTER)).isEqualTo(85);
        assertThat(defenseSkill.counterMultiplierByRank().get(SkillRank.MASTER)).isEqualTo(100);
        assertThat(defenseSkill.resourceCostAt(SkillRank.F)).isEqualTo(4);
        assertThat(defenseSkill.resourceCostAt(SkillRank.MASTER)).isEqualTo(4);
        assertThat(defenseSkill.critBonusAt(SkillRank.MASTER)).isZero();
    }

    @Test
    void should_parseDefenseSkillWithOptionalRankMaps_when_validJson() {
        final String json =
                """
                [{
                  "id": "defense",
                  "label": "디펜스",
                  "type": "DEFENSE",
                  "talent": "COMMON",
                  "resourceCost": 5,
                  "resourceCostByRank": {
                    "F":5,"E":5,"D":4,"C":4,"B":4,"A":3,
                    "R9":3,"R8":3,"R7":2,"R6":2,"R5":2,"R4":1,
                    "R3":1,"R2":1,"R1":1,"MASTER":1
                  },
                  "critBonusByRank": {
                    "F":0,"E":10,"D":20,"C":30,"B":40,"A":50,
                    "R9":70,"R8":90,"R7":110,"R6":130,"R5":150,"R4":160,
                    "R3":170,"R2":180,"R1":190,"MASTER":200
                  },
                  "blockRateByRank": {
                    "F":100,"E":100,"D":100,"C":100,"B":100,"A":100,
                    "R9":100,"R8":100,"R7":100,"R6":100,"R5":100,"R4":100,
                    "R3":100,"R2":100,"R1":100,"MASTER":100
                  },
                  "counterMultiplierByRank": {
                    "F":0,"E":0,"D":0,"C":0,"B":0,"A":0,
                    "R9":0,"R8":0,"R7":0,"R6":0,"R5":0,"R4":0,
                    "R3":0,"R2":0,"R1":0,"MASTER":0
                  },
                  "description": "완전 방어"
                }]
                """;

        final List<Skill> skills = loadFromString(json);

        assertThat(skills).hasSize(1);
        final DefenseSkill skill = (DefenseSkill) skills.getFirst();
        assertThat(skill.resourceCostByRank()).hasSize(16);
        assertThat(skill.critBonusByRank()).hasSize(16);
        assertThat(skill.resourceCostAt(SkillRank.F)).isEqualTo(5);
        assertThat(skill.resourceCostAt(SkillRank.MASTER)).isEqualTo(1);
        assertThat(skill.critBonusAt(SkillRank.F)).isZero();
        assertThat(skill.critBonusAt(SkillRank.MASTER)).isEqualTo(200);
    }

    @Test
    void should_throwSkillDataException_when_rootIsNotArray() {
        final String json =
                """
                {"id": "windmill"}
                """;

        assertThatThrownBy(() -> loadFromString(json))
                .isInstanceOf(SkillDataException.class)
                .hasMessageContaining("배열");
    }

    @Test
    void should_throwSkillDataException_when_requiredFieldMissing() {
        final String json =
                """
                [{
                  "id": "windmill",
                  "type": "NORMAL",
                  "talent": "MELEE",
                  "resourceCost": 7,
                  "multiplierByRank": {
                    "F":90,"E":95,"D":101,"C":106,"B":112,"A":117,
                    "R9":123,"R8":128,"R7":134,"R6":139,"R5":145,"R4":150,
                    "R3":156,"R2":161,"R1":166,"MASTER":170
                  },
                  "description": "테스트"
                }]
                """;

        assertThatThrownBy(() -> loadFromString(json))
                .isInstanceOf(SkillDataException.class)
                .hasMessageContaining("label");
    }

    @Test
    void should_throwSkillDataException_when_unknownType() {
        final String json =
                """
                [{
                  "id": "test",
                  "label": "테스트",
                  "type": "UNKNOWN_TYPE",
                  "talent": "MELEE",
                  "resourceCost": 5,
                  "multiplierByRank": {
                    "F":90,"E":95,"D":101,"C":106,"B":112,"A":117,
                    "R9":123,"R8":128,"R7":134,"R6":139,"R5":145,"R4":150,
                    "R3":156,"R2":161,"R1":166,"MASTER":170
                  },
                  "description": "테스트"
                }]
                """;

        assertThatThrownBy(() -> loadFromString(json))
                .isInstanceOf(SkillDataException.class)
                .hasMessageContaining("type");
    }

    @Test
    void should_throwSkillDataException_when_unknownTalent() {
        final String json =
                """
                [{
                  "id": "test",
                  "label": "테스트",
                  "type": "NORMAL",
                  "talent": "INVALID_TALENT",
                  "resourceCost": 5,
                  "multiplierByRank": {
                    "F":90,"E":95,"D":101,"C":106,"B":112,"A":117,
                    "R9":123,"R8":128,"R7":134,"R6":139,"R5":145,"R4":150,
                    "R3":156,"R2":161,"R1":166,"MASTER":170
                  },
                  "description": "테스트"
                }]
                """;

        assertThatThrownBy(() -> loadFromString(json))
                .isInstanceOf(SkillDataException.class)
                .hasMessageContaining("talent");
    }

    @Test
    void should_throwSkillDataException_when_duplicateIds() {
        final String json =
                """
                [{
                  "id": "dup",
                  "label": "첫번째",
                  "type": "NORMAL",
                  "talent": "MELEE",
                  "resourceCost": 5,
                  "multiplierByRank": {
                    "F":90,"E":95,"D":101,"C":106,"B":112,"A":117,
                    "R9":123,"R8":128,"R7":134,"R6":139,"R5":145,"R4":150,
                    "R3":156,"R2":161,"R1":166,"MASTER":170
                  },
                  "description": "테스트"
                },{
                  "id": "dup",
                  "label": "두번째",
                  "type": "HEAVY",
                  "talent": "ARCHERY",
                  "resourceCost": 10,
                  "multiplierByRank": {
                    "F":130,"E":138,"D":146,"C":154,"B":162,"A":170,
                    "R9":178,"R8":186,"R7":194,"R6":202,"R5":210,"R4":218,
                    "R3":226,"R2":234,"R1":242,"MASTER":250
                  },
                  "description": "테스트"
                }]
                """;

        assertThatThrownBy(() -> loadFromString(json))
                .isInstanceOf(SkillDataException.class)
                .hasMessageContaining("중복");
    }

    @Test
    void should_throwSkillDataException_when_rankMapHasLessThan16Keys() {
        final String json =
                """
                [{
                  "id": "test",
                  "label": "테스트",
                  "type": "NORMAL",
                  "talent": "MELEE",
                  "resourceCost": 5,
                  "multiplierByRank": {
                    "F":90,"E":95,"D":101
                  },
                  "description": "테스트"
                }]
                """;

        assertThatThrownBy(() -> loadFromString(json))
                .isInstanceOf(SkillDataException.class)
                .hasMessageContaining("랭크");
    }

    @Test
    void should_returnImmutableList_when_validInput() {
        final String json =
                """
                [{
                  "id": "windmill",
                  "label": "윈드밀",
                  "type": "NORMAL",
                  "talent": "MELEE",
                  "resourceCost": 7,
                  "multiplierByRank": {
                    "F":90,"E":95,"D":101,"C":106,"B":112,"A":117,
                    "R9":123,"R8":128,"R7":134,"R6":139,"R5":145,"R4":150,
                    "R3":156,"R2":161,"R1":166,"MASTER":170
                  },
                  "description": "회전 베기"
                }]
                """;

        final List<Skill> skills = loadFromString(json);

        assertThatThrownBy(() -> skills.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_returnSkill_when_byIdCalledWithExistingId() {
        final String json =
                """
                [{
                  "id": "firebolt",
                  "label": "파이어볼트",
                  "type": "HEAVY",
                  "talent": "MAGIC",
                  "resourceCost": 10,
                  "multiplierByRank": {
                    "F":130,"E":138,"D":146,"C":154,"B":162,"A":170,
                    "R9":178,"R8":186,"R7":194,"R6":202,"R5":210,"R4":218,
                    "R3":226,"R2":234,"R1":242,"MASTER":250
                  },
                  "description": "화염 폭발"
                }]
                """;

        loadIntoService(json);

        final Optional<Skill> result = skillCatalogService.byId("firebolt");
        assertThat(result).isPresent();
        assertThat(result.get().label()).isEqualTo("파이어볼트");
    }

    @Test
    void should_returnEmpty_when_byIdCalledWithNonExistingId() {
        final String json =
                """
                [{
                  "id": "firebolt",
                  "label": "파이어볼트",
                  "type": "HEAVY",
                  "talent": "MAGIC",
                  "resourceCost": 10,
                  "multiplierByRank": {
                    "F":130,"E":138,"D":146,"C":154,"B":162,"A":170,
                    "R9":178,"R8":186,"R7":194,"R6":202,"R5":210,"R4":218,
                    "R3":226,"R2":234,"R1":242,"MASTER":250
                  },
                  "description": "화염 폭발"
                }]
                """;

        loadIntoService(json);

        assertThat(skillCatalogService.byId("nonexistent")).isEmpty();
        assertThat(skillCatalogService.byId(null)).isEmpty();
    }

    private List<Skill> loadFromString(final String json) {
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        return skillCatalogService.loadFromStream(inputStream);
    }

    private void loadIntoService(final String json) {
        final InputStream inputStream =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        final List<Skill> skills = skillCatalogService.loadFromStream(inputStream);
        try {
            final java.lang.reflect.Field field =
                    SkillCatalogService.class.getDeclaredField("skills");
            field.setAccessible(true);
            field.set(skillCatalogService, skills);
        } catch (final Exception exception) {
            throw new RuntimeException("테스트 설정 실패", exception);
        }
    }
}
