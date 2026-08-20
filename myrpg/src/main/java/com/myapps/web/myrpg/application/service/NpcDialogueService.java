package com.myapps.web.myrpg.application.service;

import com.myapps.web.myrpg.domain.model.Npc;
import com.myapps.web.myrpg.domain.model.TimeOfDay;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

/**
 * NPC 대사 선택 서비스.
 *
 * <p>현재 시각으로부터 시간대(Time_Of_Day)를 산출하고, NPC의 기본 대사와 시간대별 대사를 병합한 후보 풀에서 균등 무작위로 대사 1개를 선택합니다. 후보가 없을
 * 경우 성격 기반 폴백 문구를 반환합니다.
 *
 * <p>계절(Season) 정보는 입력·사용하지 않습니다.
 */
@Service
public class NpcDialogueService {

    private static final String FALLBACK_TEMPLATE = "%s은(는) 말없이 고개를 끄덕인다.";

    private final Clock clock;
    private final Random random;

    /**
     * NpcDialogueService를 생성합니다.
     *
     * @param clock 시간대 산출용 Clock (테스트 시 고정 시각 주입 가능)
     * @param random 무작위 선택용 Random (테스트 시 시드 고정 가능)
     */
    public NpcDialogueService(final Clock clock, final Random random) {
        this.clock = clock;
        this.random = random;
    }

    /**
     * 현재 시각 기준으로 NPC 대사를 1개 선택하여 반환합니다.
     *
     * <p>현재 시각의 시(hour)를 {@link TimeOfDay}로 매핑한 뒤, {@code lines.default}와 {@code
     * lines.byTime[시간대]}를 병합한 후보 풀에서 균등 무작위로 선택합니다. 후보가 없으면 성격 기반 폴백 문구를 반환합니다.
     *
     * @param npc 대사를 선택할 NPC
     * @return 선택된 대사 문자열 (비어 있지 않음)
     */
    public String selectLine(final Npc npc) {
        final int hour = LocalDateTime.now(clock).getHour();
        return selectLine(npc, hour);
    }

    /**
     * 지정된 시각(hour) 기준으로 NPC 대사를 1개 선택하여 반환합니다.
     *
     * <p>테스트 시 시각을 직접 주입할 수 있는 오버로드입니다.
     *
     * @param npc 대사를 선택할 NPC
     * @param hour 0 이상 24 미만의 시각
     * @return 선택된 대사 문자열 (비어 있지 않음)
     */
    public String selectLine(final Npc npc, final int hour) {
        final TimeOfDay tod = TimeOfDay.fromHour(hour);
        final List<String> pool = buildCandidatePool(npc, tod);

        if (!pool.isEmpty()) {
            return pool.get(random.nextInt(pool.size()));
        }
        return personalityFallback(npc);
    }

    /**
     * NPC의 기본 대사와 시간대별 대사를 순서 보존으로 병합하여 후보 풀을 구성합니다.
     *
     * @param npc NPC
     * @param tod 현재 시간대
     * @return 순서 보존된 후보 풀 (불변)
     */
    private List<String> buildCandidatePool(final Npc npc, final TimeOfDay tod) {
        final List<String> defaultLines = npc.lines().defaultLines();
        final List<String> timeLines =
                npc.lines().byTime() != null
                        ? npc.lines().byTime().getOrDefault(tod.key(), List.of())
                        : List.of();

        if (timeLines.isEmpty()) {
            return defaultLines != null ? defaultLines : List.of();
        }

        if (defaultLines == null || defaultLines.isEmpty()) {
            return timeLines;
        }

        final List<String> merged = new ArrayList<>(defaultLines.size() + timeLines.size());
        merged.addAll(defaultLines);
        merged.addAll(timeLines);
        return Collections.unmodifiableList(merged);
    }

    /**
     * 대사 후보가 없을 때 NPC 이름 기반의 결정적 폴백 문구를 반환합니다.
     *
     * @param npc NPC
     * @return 비어 있지 않은 폴백 문구
     */
    private String personalityFallback(final Npc npc) {
        return FALLBACK_TEMPLATE.formatted(npc.name());
    }
}
