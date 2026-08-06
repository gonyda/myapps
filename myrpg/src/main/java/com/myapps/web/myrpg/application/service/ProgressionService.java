package com.myapps.web.myrpg.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.application.dto.DeathResult;
import com.myapps.web.myrpg.application.dto.LevelUpResult;
import com.myapps.web.myrpg.application.dto.RebirthResult;
import com.myapps.web.myrpg.application.dto.RebirthStatus;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.domain.model.ExperiencePolicy;
import com.myapps.web.myrpg.domain.model.StatProgression;
import com.myapps.web.myrpg.domain.model.TalentType;

/**
 * 경험치 획득·레벨업·사망 패널티·환생 규칙을 캡슐화하는 애플리케이션 서비스.
 *
 * <p>{@link CharacterProgress}를 변경하고 결과를 반환하며,
 * 저장은 컨트롤러가 {@code CharacterService.saveTurn}으로 수행한다.
 */
@Service
public class ProgressionService {

    private static final int MAX_LEVEL = 100;
    private static final Duration REBIRTH_COOLDOWN = Duration.ofHours(24);
    private static final double DEATH_PENALTY_RATE = 0.10;

    private final ExperiencePolicy experiencePolicy;
    private final StatProgression statProgression;
    private final Clock clock;

    /**
     * ProgressionService를 생성한다.
     *
     * @param experiencePolicy 경험치 곡선 정책
     * @param statProgression  레벨 기반 스탯·바이탈 계산 정책
     * @param clock            시간 산출용 Clock (테스트 시 고정 시각 주입 가능)
     */
    public ProgressionService(final ExperiencePolicy experiencePolicy,
                              final StatProgression statProgression,
                              final Clock clock) {
        this.experiencePolicy = experiencePolicy;
        this.statProgression = statProgression;
        this.clock = clock;
    }

    /**
     * 경험치를 획득하고 연속 레벨업을 처리한다.
     *
     * <p>최대레벨(100)이면 아무 변경 없이 반환한다. 음수 획득량은 0으로 취급한다.
     * 레벨업이 1회 이상 발생하면 풀회복을 적용한다.
     * 최대레벨 도달 시 잔여 경험치는 0으로 폐기된다.
     *
     * @param p      변경할 캐릭터 진행상황
     * @param amount 획득할 경험치량 (음수는 0 취급)
     * @return 레벨업 결과 (획득 레벨 수, 최종 레벨)
     */
    public LevelUpResult gainExperience(final CharacterProgress p, final long amount) {
        if (p.getCurrentLevel() == MAX_LEVEL) {
            return new LevelUpResult(0, MAX_LEVEL);
        }

        final long effectiveAmount = Math.max(0L, amount);
        long exp = p.getExperience() + effectiveAmount;
        int level = p.getCurrentLevel();
        int gained = 0;

        while (level < MAX_LEVEL && exp >= experiencePolicy.requiredForNext(level)) {
            exp -= experiencePolicy.requiredForNext(level);
            level++;
            gained++;
        }

        if (level == MAX_LEVEL) {
            exp = 0;
        }

        p.setExperience(exp);
        if (gained > 0) {
            p.setCurrentLevel(level);
            p.increaseAccumulatedLevel(gained);
            p.fullRecover(statProgression.vitalMaxFor(level));
        }

        return new LevelUpResult(gained, level);
    }

    /**
     * 사망 패널티를 적용한다.
     *
     * <p>최대레벨이면 아무 변경 없이 반환한다.
     * 현재 레벨의 다음 레벨 필요 경험치의 10%를 차감하며,
     * 경험치가 0 미만이 되지 않도록 보장한다.
     * 레벨·누적레벨·재능은 불변이다.
     *
     * @param p 변경할 캐릭터 진행상황
     * @return 사망 결과 (실제 차감된 경험치량)
     */
    public DeathResult applyDeathPenalty(final CharacterProgress p) {
        if (p.getCurrentLevel() == MAX_LEVEL) {
            return new DeathResult(0);
        }

        final long required = experiencePolicy.requiredForNext(p.getCurrentLevel());
        final long loss = (long) Math.floor(required * DEATH_PENALTY_RATE);
        final long prevExp = p.getExperience();
        final long newExp = Math.max(0L, prevExp - loss);
        p.setExperience(newExp);

        return new DeathResult(prevExp - newExp);
    }

    /**
     * 환생 가능 여부 및 쿨다운 상태를 조회한다.
     *
     * <p>환생 이력이 없으면 즉시 가능하다.
     * 마지막 환생으로부터 24시간 이상 경과했으면 가능하다.
     *
     * @param p 조회할 캐릭터 진행상황
     * @return 환생 상태 (가능 여부, 경과 시간, 남은 쿨다운)
     */
    public RebirthStatus rebirthStatus(final CharacterProgress p) {
        final LocalDateTime now = LocalDateTime.now(clock);

        if (p.getLastRebirthAt() == null) {
            return new RebirthStatus(true, false, null, null);
        }

        final Duration elapsed = Duration.between(p.getLastRebirthAt(), now);
        final boolean available = elapsed.compareTo(REBIRTH_COOLDOWN) >= 0;
        final Duration remaining = available ? Duration.ZERO : REBIRTH_COOLDOWN.minus(elapsed);

        return new RebirthStatus(available, true, elapsed, remaining);
    }

    /**
     * 환생을 시도한다.
     *
     * <p>쿨다운이 활성 상태이면 거부하고 상태를 변경하지 않는다.
     * 환생 성공 시 레벨 1, 경험치 0, 누적 레벨 +1, 재능 MELEE,
     * 현재 시각을 환생 시각으로 기록하고 풀회복을 적용한다.
     *
     * @param p 변경할 캐릭터 진행상황
     * @return 환생 결과 (성공 또는 쿨다운 활성)
     */
    public RebirthResult rebirth(final CharacterProgress p) {
        final RebirthStatus status = rebirthStatus(p);

        if (!status.available()) {
            return new RebirthResult.CooldownActive(status.remaining());
        }

        p.setCurrentLevel(1);
        p.setExperience(0);
        p.increaseAccumulatedLevel(1);
        p.setTalent(TalentType.MELEE);
        p.setLastRebirthAt(LocalDateTime.now(clock));
        p.fullRecover(statProgression.vitalMaxFor(1));

        return new RebirthResult.Reborn();
    }
}
