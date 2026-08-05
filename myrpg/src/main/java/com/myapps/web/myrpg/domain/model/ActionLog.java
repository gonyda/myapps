package com.myapps.web.myrpg.domain.model;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 행동 로그를 관리하는 도메인 클래스.
 *
 * <p>HTTP 세션에 보관되며, 최대 10개의 로그 항목을 유지한다.
 * 초과 시 가장 오래된 항목부터 제거하고, 표시 시 타임스탬프 오름차순으로 정렬한다.
 * 타입이 지정되지 않은 항목은 기본적으로 {@code move} 타입으로 설정된다.
 *
 * <p>테스트 용이성을 위해 {@link Clock}을 주입받아 결정적 타임스탬프를 생성한다.
 */
public class ActionLog {

    private static final int MAX_ENTRIES = 10;
    private static final String DEFAULT_TYPE = "move";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

    private final Clock clock;
    private final List<ActionLogEntry> entries;

    /**
     * 지정된 시계를 사용하는 ActionLog를 생성한다.
     *
     * @param clock 타임스탬프 생성에 사용할 시계
     */
    public ActionLog(final Clock clock) {
        this.clock = clock;
        this.entries = new ArrayList<>();
    }

    /**
     * 메시지와 타입으로 로그 항목을 추가한다.
     *
     * <p>타입이 {@code null}이면 기본 타입 {@code move}가 사용된다.
     * 항목 수가 최대치(10개)를 초과하면 가장 오래된 항목부터 제거한다.
     *
     * @param message 로그 메시지 텍스트
     * @param type    로그 타입 ({@code null}이면 {@code move})
     * @return 생성된 로그 항목
     */
    public ActionLogEntry add(final String message, final String type) {
        final String resolvedType = (type == null) ? DEFAULT_TYPE : type;
        final String timestamp = formatTimestamp();
        final ActionLogEntry entry = new ActionLogEntry(timestamp, message, resolvedType);

        entries.add(entry);
        trimExcess();

        return entry;
    }

    /**
     * 메시지만으로 로그 항목을 추가한다 (타입은 기본값 {@code move}).
     *
     * @param message 로그 메시지 텍스트
     * @return 생성된 로그 항목
     */
    public ActionLogEntry add(final String message) {
        return add(message, null);
    }

    /**
     * 현재 로그 항목 목록을 타임스탬프 오름차순으로 반환한다.
     *
     * <p>반환되는 목록은 수정 불가능한 복사본이다.
     *
     * @return 오름차순 정렬된 로그 항목 목록
     */
    public List<ActionLogEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * 현재 보관 중인 로그 항목 수를 반환한다.
     *
     * @return 로그 항목 수
     */
    public int size() {
        return entries.size();
    }

    /**
     * 현재 시각을 지정된 형식으로 포맷한다.
     *
     * @return 포맷된 타임스탬프 문자열
     */
    private String formatTimestamp() {
        final LocalDateTime now = LocalDateTime.now(clock);
        return now.format(FORMATTER);
    }

    /**
     * 항목 수가 최대치를 초과하면 가장 오래된 항목부터 제거한다.
     */
    private void trimExcess() {
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }
}
