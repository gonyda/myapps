package com.myapps.web.myrpg.application.service.battle;

import com.myapps.web.myrpg.application.dto.BattleSkillButton;
import com.myapps.web.myrpg.application.dto.BattleView;
import com.myapps.web.myrpg.domain.model.BattleState;
import com.myapps.web.myrpg.domain.model.Monster;
import com.myapps.web.myrpg.domain.model.PreemptiveParty;
import com.myapps.web.myrpg.domain.model.SkillType;
import com.myapps.web.myrpg.support.GameMessageService;
import java.util.List;

/** 대치(Clash) 페이즈의 뷰 모델 및 스탠스 뱃지·클래시 지속시간 조립을 담당하는 협력 객체. */
public final class BattleViewAssembler {

    private static final int NORMAL_CLASH_DURATION_MS = 1000;
    private static final int DEFAULT_CLASH_DURATION_MS = 1500;

    private static final String BADGE_CLASS_NORMAL = "badge-stance-normal";
    private static final String BADGE_CLASS_HEAVY = "badge-stance-heavy";
    private static final String BADGE_CLASS_DEFENSE = "badge-stance-defense";
    private static final String BADGE_CLASS_PREEMPTIVE_PLAYER = "badge-stance-preemptive-player";
    private static final String BADGE_CLASS_PREEMPTIVE_MONSTER = "badge-stance-preemptive-monster";

    private final GameMessageService gameMessageService;

    public BattleViewAssembler(final GameMessageService gameMessageService) {
        this.gameMessageService = gameMessageService;
    }

    /** 공방 대치 페이즈의 BattleView를 조립한다. */
    public BattleView buildClashBattleView(
            final BattleState state,
            final Monster monster,
            final List<BattleSkillButton> skills,
            final SkillType intent,
            final boolean bowFirstStrike,
            final PreemptiveParty preemptiveParty) {
        final int durationMs = resolveClashDuration(intent);
        final String badgeLabel = resolveStanceBadgeLabel(intent, bowFirstStrike, preemptiveParty);
        final String badgeClass = resolveStanceBadgeClass(intent, preemptiveParty);

        return new BattleView(
                monster.name(),
                monster.level(),
                state.getMonsterCurrentHp(),
                monster.maxHp(),
                skills,
                false,
                false,
                intent,
                durationMs,
                badgeLabel,
                badgeClass,
                bowFirstStrike);
    }

    /** 몬스터 의도에 따른 클래시 지속 시간(ms)을 계산한다. */
    public int resolveClashDuration(final SkillType intent) {
        if (intent == SkillType.NORMAL) {
            return NORMAL_CLASH_DURATION_MS;
        }
        return DEFAULT_CLASH_DURATION_MS;
    }

    /** 스탠스 뱃지 텍스트를 결정한다. */
    public String resolveStanceBadgeLabel(
            final SkillType intent,
            final boolean bowFirstStrike,
            final PreemptiveParty preemptiveParty) {
        if (bowFirstStrike) {
            return gameMessageService.get("badge.stance.bow_first_strike");
        }
        if (preemptiveParty == PreemptiveParty.PLAYER) {
            return gameMessageService.get("badge.stance.preemptive_player");
        }
        if (preemptiveParty == PreemptiveParty.MONSTER) {
            return gameMessageService.get("badge.stance.preemptive_monster");
        }
        if (intent == null) {
            return null;
        }
        return switch (intent) {
            case NORMAL -> gameMessageService.get("badge.stance.normal");
            case HEAVY -> gameMessageService.get("badge.stance.heavy");
            case DEFENSE -> gameMessageService.get("badge.stance.defense");
            default -> gameMessageService.get("badge.stance.normal");
        };
    }

    /** 스탠스 뱃지 CSS 클래스를 결정한다. */
    public String resolveStanceBadgeClass(
            final SkillType intent, final PreemptiveParty preemptiveParty) {
        if (preemptiveParty == PreemptiveParty.PLAYER) {
            return BADGE_CLASS_PREEMPTIVE_PLAYER;
        }
        if (preemptiveParty == PreemptiveParty.MONSTER) {
            return BADGE_CLASS_PREEMPTIVE_MONSTER;
        }
        if (intent == null) {
            return null;
        }
        return switch (intent) {
            case NORMAL -> BADGE_CLASS_NORMAL;
            case HEAVY -> BADGE_CLASS_HEAVY;
            case DEFENSE -> BADGE_CLASS_DEFENSE;
            default -> BADGE_CLASS_NORMAL;
        };
    }
}
