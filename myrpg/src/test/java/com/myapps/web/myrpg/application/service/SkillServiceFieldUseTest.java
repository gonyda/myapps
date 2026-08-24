package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myapps.web.myrpg.application.dto.FieldSkillResult;
import com.myapps.web.myrpg.application.dto.SkillListView;
import com.myapps.web.myrpg.application.dto.SkillRowView;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.CharacterSkill;
import com.myapps.web.myrpg.domain.model.SkillRank;
import com.myapps.web.myrpg.domain.model.TalentType;
import com.myapps.web.myrpg.domain.repository.CharacterProgressRepository;
import com.myapps.web.myrpg.domain.repository.CharacterSkillRepository;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** {@link SkillService} 필드 스킬 사용 및 4대 수련 체계 단위 테스트. */
class SkillServiceFieldUseTest {

    private static final Long CHARACTER_ID = 1L;

    private CharacterSkillRepository characterSkillRepository;
    private CharacterProgressRepository characterProgressRepository;
    private SkillCatalogService skillCatalogService;
    private SkillService skillService;

    @BeforeEach
    void setUp() {
        characterSkillRepository = mock(CharacterSkillRepository.class);
        characterProgressRepository = mock(CharacterProgressRepository.class);
        skillCatalogService = new SkillCatalogService(new ObjectMapper());
        final InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream("data/skill.json");
        skillCatalogService.loadFromStream(inputStream);
        skillCatalogService.init();

        skillService =
                new SkillService(
                        characterSkillRepository, characterProgressRepository, skillCatalogService);
    }

    private CharacterProgress createProgress(final int hp, final int mp, final int ap) {
        final CharacterProgress progress =
                new CharacterProgress(
                        "테스터", 1, 1, 0L, TalentType.MELEE, null, hp, mp, 100, "node-1", ap, 1000L);
        try {
            final Field idField = CharacterProgress.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(progress, CHARACTER_ID);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException("id 설정 실패", exception);
        }
        return progress;
    }

    @Nested
    @DisplayName("필드 힐링(useFieldSkill) 테스트")
    class FieldSkillUseTests {

        @Test
        @DisplayName("힐링 스킬 정상 사용 시 HP 회복, MP 소모 및 사용 횟수 증가")
        void should_healHpAndDeductMp_when_healingSkillUsed() {
            // given: HP 50/100, MP 30/100, F랭크 힐링(회복량 30, 소모 MP 12)
            final CharacterProgress progress = createProgress(50, 30, 100);
            final CharacterSkill healingSkill = CharacterSkill.newSkill(CHARACTER_ID, "healing");

            when(characterProgressRepository.findById(CHARACTER_ID))
                    .thenReturn(Optional.of(progress));
            when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, "healing"))
                    .thenReturn(Optional.of(healingSkill));

            // when
            final FieldSkillResult result =
                    skillService.useFieldSkill(CHARACTER_ID, "healing", 100, 100);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.hpCurrent()).isEqualTo(80); // 50 + 30
            assertThat(result.mpCurrent()).isEqualTo(18); // 30 - 12
            assertThat(result.healedAmount()).isEqualTo(30);
            assertThat(healingSkill.getUsageCount()).isEqualTo(1);
            verify(characterProgressRepository).save(progress);
            verify(characterSkillRepository).save(healingSkill);
        }

        @Test
        @DisplayName("이미 최대 체력일 경우 필드 힐링 실패")
        void should_fail_when_hpIsAlreadyMax() {
            // given: HP 100/100
            final CharacterProgress progress = createProgress(100, 30, 100);
            final CharacterSkill healingSkill = CharacterSkill.newSkill(CHARACTER_ID, "healing");

            when(characterProgressRepository.findById(CHARACTER_ID))
                    .thenReturn(Optional.of(progress));
            when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, "healing"))
                    .thenReturn(Optional.of(healingSkill));

            // when
            final FieldSkillResult result =
                    skillService.useFieldSkill(CHARACTER_ID, "healing", 100, 100);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("이미 최대 체력");
            assertThat(healingSkill.getUsageCount()).isZero();
        }

        @Test
        @DisplayName("MP 부족 시 필드 힐링 실패")
        void should_fail_when_mpIsInsufficient() {
            // given: HP 50/100, MP 5/100 (필요 MP 12)
            final CharacterProgress progress = createProgress(50, 5, 100);
            final CharacterSkill healingSkill = CharacterSkill.newSkill(CHARACTER_ID, "healing");

            when(characterProgressRepository.findById(CHARACTER_ID))
                    .thenReturn(Optional.of(progress));
            when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, "healing"))
                    .thenReturn(Optional.of(healingSkill));

            // when
            final FieldSkillResult result =
                    skillService.useFieldSkill(CHARACTER_ID, "healing", 100, 100);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("MP가 부족");
            assertThat(healingSkill.getUsageCount()).isZero();
        }

        @Test
        @DisplayName("비회복 스킬(예: slash) 필드 사용 시 실패")
        void should_fail_when_nonRecoverySkillUsed() {
            final CharacterProgress progress = createProgress(50, 50, 100);
            final CharacterSkill slashSkill = CharacterSkill.newSkill(CHARACTER_ID, "slash");

            when(characterProgressRepository.findById(CHARACTER_ID))
                    .thenReturn(Optional.of(progress));
            when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, "slash"))
                    .thenReturn(Optional.of(slashSkill));

            // when
            final FieldSkillResult result =
                    skillService.useFieldSkill(CHARACTER_ID, "slash", 100, 100);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("필드에서 사용할 수 없는 스킬");
        }
    }

    @Nested
    @DisplayName("4대 수련 체계 랭크업(rankUp) 테스트")
    class FourWayRankUpTests {

        @Test
        @DisplayName("1) 패시브 스킬(combat_mastery): 0사용 0처치 상태에서도 AP만 있으면 즉시 승급")
        void should_rankUpInstantly_when_passiveSkillHasEnoughAp() {
            final CharacterProgress progress = createProgress(100, 100, 50);
            final CharacterSkill passiveSkill =
                    CharacterSkill.newSkill(CHARACTER_ID, "combat_mastery");

            when(characterSkillRepository.findByCharacterIdAndSkillId(
                            CHARACTER_ID, "combat_mastery"))
                    .thenReturn(Optional.of(passiveSkill));

            // when
            final boolean success = skillService.rankUp(progress, "combat_mastery");

            // then
            assertThat(success).isTrue();
            assertThat(passiveSkill.getRank()).isEqualTo(SkillRank.E);
            assertThat(progress.getAbilityPoints()).isEqualTo(49); // 50 - 1 (F->E AP cost = 1)
            verify(characterSkillRepository).save(passiveSkill);
        }

        @Test
        @DisplayName("2) 지원/회복 스킬(healing): 사용 횟수 충족 + 막타 0처치 상태에서 승급 가능")
        void should_rankUp_when_recoverySkillHasUsageEvenWithZeroKills() {
            final CharacterProgress progress = createProgress(100, 100, 50);
            final CharacterSkill healingSkill = CharacterSkill.newSkill(CHARACTER_ID, "healing");
            // F->E 요구: usage 10회
            for (int i = 0; i < 10; i++) {
                healingSkill.increaseUsage();
            }

            when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, "healing"))
                    .thenReturn(Optional.of(healingSkill));

            // when
            final boolean success = skillService.rankUp(progress, "healing");

            // then
            assertThat(success).isTrue();
            assertThat(healingSkill.getRank()).isEqualTo(SkillRank.E);
            verify(characterSkillRepository).save(healingSkill);
        }

        @Test
        @DisplayName("3) 궁극기 스킬(final_hit): 전용 사용 횟수(F->E: 1회) 충족 + 막타 0처치 상태에서 승급 가능")
        void should_rankUp_when_ultimateSkillHasUsageEvenWithZeroKills() {
            final CharacterProgress progress = createProgress(100, 100, 50);
            final CharacterSkill ultimateSkill = CharacterSkill.newSkill(CHARACTER_ID, "final_hit");
            ultimateSkill.increaseUsage(); // 1회 사용

            when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, "final_hit"))
                    .thenReturn(Optional.of(ultimateSkill));

            // when
            final boolean success = skillService.rankUp(progress, "final_hit");

            // then
            assertThat(success).isTrue();
            assertThat(ultimateSkill.getRank()).isEqualTo(SkillRank.E);
            verify(characterSkillRepository).save(ultimateSkill);
        }

        @Test
        @DisplayName("4) 직접 공격 스킬(slash): 사용 횟수 충족했으나 막타 처치 미충족 시 승급 불가")
        void should_failRankUp_when_damageSkillHasUsageButInsufficientKills() {
            final CharacterProgress progress = createProgress(100, 100, 50);
            final CharacterSkill slashSkill = CharacterSkill.newSkill(CHARACTER_ID, "slash");
            for (int i = 0; i < 10; i++) {
                slashSkill.increaseUsage();
            }
            // 막타 killCount = 0 (요구치 2 미충족)

            when(characterSkillRepository.findByCharacterIdAndSkillId(CHARACTER_ID, "slash"))
                    .thenReturn(Optional.of(slashSkill));

            // when
            final boolean success = skillService.rankUp(progress, "slash");

            // then
            assertThat(success).isFalse();
            assertThat(slashSkill.getRank()).isEqualTo(SkillRank.F);
        }
    }

    @Nested
    @DisplayName("스킬 목록 탭 필터링 테스트")
    class SkillListTabFilterTests {

        @Test
        @DisplayName("COMMON 탭 조회 시 디펜스 스킬과 패시브 6종이 함께 표시됨")
        void should_showDefenseAndPassives_inCommonTab() {
            final CharacterProgress progress = createProgress(100, 100, 50);
            final List<CharacterSkill> owned =
                    List.of(
                            CharacterSkill.newSkill(CHARACTER_ID, "defense"),
                            CharacterSkill.newSkill(CHARACTER_ID, "combat_mastery"),
                            CharacterSkill.newSkill(CHARACTER_ID, "magic_mastery"),
                            CharacterSkill.newSkill(CHARACTER_ID, "slash"));

            when(characterProgressRepository.findById(CHARACTER_ID))
                    .thenReturn(Optional.of(progress));
            when(characterSkillRepository.findByCharacterId(CHARACTER_ID)).thenReturn(owned);

            // when: COMMON 탭 조회
            final SkillListView view = skillService.buildListView(CHARACTER_ID, "common");

            // then: defense, combat_mastery, magic_mastery 포함, slash 제외
            final List<String> skillIds = view.rows().stream().map(SkillRowView::id).toList();
            assertThat(skillIds)
                    .containsExactlyInAnyOrder("defense", "combat_mastery", "magic_mastery");
        }

        @Test
        @DisplayName("MELEE 탭 조회 시 액티브 근접 스킬만 표시되고 패시브는 제외됨")
        void should_showOnlyActiveMeleeSkills_inMeleeTab() {
            final CharacterProgress progress = createProgress(100, 100, 50);
            final List<CharacterSkill> owned =
                    List.of(
                            CharacterSkill.newSkill(CHARACTER_ID, "slash"),
                            CharacterSkill.newSkill(CHARACTER_ID, "combat_mastery"));

            when(characterProgressRepository.findById(CHARACTER_ID))
                    .thenReturn(Optional.of(progress));
            when(characterSkillRepository.findByCharacterId(CHARACTER_ID)).thenReturn(owned);

            // when: MELEE 탭 조회
            final SkillListView view = skillService.buildListView(CHARACTER_ID, "melee");

            // then: slash만 포함, combat_mastery는 제외
            final List<String> skillIds = view.rows().stream().map(SkillRowView::id).toList();
            assertThat(skillIds).containsExactly("slash");
        }
    }
}
