package com.myapps.web.myrpg.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.myapps.web.myrpg.application.exception.MapDataException;
import com.myapps.web.myrpg.domain.model.AmbienceData;
import com.myapps.web.myrpg.domain.model.MapNode;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.ObjectMapper;

/**
 * 상황 멘트 선택 서비스.
 *
 * <p>현재 시각으로부터 계절(Season)과 시간대(Time_Of_Day)를 산출하고,
 * 노드의 테마에 맞는 멘트 후보를 선택합니다.
 * 후보 선택 시 폴백 규칙을 적용하여 항상 유효한 멘트를 반환합니다.
 */
@Service
public class AmbienceService {

    private static final String AMBIENCE_JSON_PATH = "data/ambience.json";
    private static final String DEFAULT_MESSAGE_TEMPLATE = "%s 주변을 둘러봅니다.";

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Random random;
    private AmbienceData ambienceData;

    /**
     * AmbienceService를 생성합니다.
     *
     * @param objectMapper Jackson 3 ObjectMapper
     * @param clock        시간 산출용 Clock (테스트 시 고정 시각 주입 가능)
     * @param random       무작위 선택용 Random (테스트 시 시드 고정 가능)
     */
    public AmbienceService(final ObjectMapper objectMapper, final Clock clock, final Random random) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.random = random;
    }

    /**
     * 애플리케이션 기동 시 ambience.json을 로드합니다.
     *
     * @throws MapDataException JSON 파싱 실패 시
     */
    @PostConstruct
    void init() {
        final ClassPathResource resource = new ClassPathResource(AMBIENCE_JSON_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            this.ambienceData = objectMapper.readValue(inputStream, AmbienceData.class);
        } catch (final IOException exception) {
            throw new MapDataException(
                    "상황 멘트 JSON 파일 로딩 실패: " + AMBIENCE_JSON_PATH, exception);
        }
    }

    /**
     * 현재 시각과 노드 정보를 기반으로 상황 멘트를 반환합니다.
     *
     * <p>테마·계절·시간대 조합의 후보에서 무작위로 선택하며,
     * 후보가 없을 경우 폴백 규칙을 적용합니다.
     *
     * @param node 현재 위치의 맵 노드
     * @return 선택된 상황 멘트 문자열
     */
    public String ambience(final MapNode node) {
        final LocalDateTime now = LocalDateTime.now(clock);
        final String season = resolveSeason(now.getMonthValue());
        final String timeOfDay = resolveTimeOfDay(now.getHour());
        final String theme = resolveTheme(node);

        final List<String> candidates = findCandidates(theme, season, timeOfDay);
        if (candidates.isEmpty()) {
            return String.format(DEFAULT_MESSAGE_TEMPLATE, node.name());
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * 월 값(1~12)으로부터 계절 키를 산출합니다.
     *
     * @param month 월 값 (1~12)
     * @return 계절 키 문자열
     */
    String resolveSeason(final int month) {
        for (final Map.Entry<String, List<Integer>> entry : ambienceData.season().entrySet()) {
            if (entry.getValue().contains(month)) {
                return entry.getKey();
            }
        }
        return "spring";
    }

    /**
     * 시각(0~23)으로부터 시간대 키를 산출합니다.
     *
     * @param hour 시각 (0~23)
     * @return 시간대 키 문자열
     */
    String resolveTimeOfDay(final int hour) {
        for (final Map.Entry<String, AmbienceData.TimeBucket> entry : ambienceData.timeOfDay().entrySet()) {
            final AmbienceData.TimeBucket bucket = entry.getValue();
            if (hour >= bucket.from() && hour < bucket.to()) {
                return entry.getKey();
            }
        }
        return "night";
    }

    private String resolveTheme(final MapNode node) {
        if (node.theme() != null && !node.theme().isBlank()) {
            return node.theme();
        }
        return node.type();
    }

    private List<String> findCandidates(final String theme, final String season, final String timeOfDay) {
        final Map<String, Map<String, Map<String, List<String>>>> themes = ambienceData.themes();
        final Map<String, Map<String, List<String>>> themeMap = themes.get(theme);
        if (themeMap == null) {
            return List.of();
        }

        final List<String> exactMatch = findExactMatch(themeMap, season, timeOfDay);
        if (!exactMatch.isEmpty()) {
            return exactMatch;
        }

        final List<String> seasonFallback = findSeasonFallback(themeMap, season);
        if (!seasonFallback.isEmpty()) {
            return seasonFallback;
        }

        return findThemeFallback(themeMap);
    }

    private List<String> findExactMatch(
            final Map<String, Map<String, List<String>>> themeMap,
            final String season,
            final String timeOfDay) {
        final Map<String, List<String>> seasonMap = themeMap.get(season);
        if (seasonMap == null) {
            return List.of();
        }
        final List<String> messages = seasonMap.get(timeOfDay);
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages;
    }

    private List<String> findSeasonFallback(
            final Map<String, Map<String, List<String>>> themeMap,
            final String season) {
        final Map<String, List<String>> seasonMap = themeMap.get(season);
        if (seasonMap == null) {
            return List.of();
        }
        final List<String> allMessages = new ArrayList<>();
        for (final List<String> todMessages : seasonMap.values()) {
            if (todMessages != null) {
                allMessages.addAll(todMessages);
            }
        }
        return allMessages;
    }

    private List<String> findThemeFallback(final Map<String, Map<String, List<String>>> themeMap) {
        final List<String> allMessages = new ArrayList<>();
        for (final Map<String, List<String>> seasonMap : themeMap.values()) {
            if (seasonMap != null) {
                for (final List<String> todMessages : seasonMap.values()) {
                    if (todMessages != null) {
                        allMessages.addAll(todMessages);
                    }
                }
            }
        }
        return allMessages;
    }

    /**
     * 로드된 AmbienceData를 반환합니다 (테스트 용도).
     *
     * @return 불변 AmbienceData 인스턴스
     */
    AmbienceData ambienceData() {
        return ambienceData;
    }
}
