package com.myapps.web.mycalendar.application.exception;

/**
 * 일정 ID에 해당하는 일정이 존재하지 않을 때 발생하는 예외.
 *
 * <p>HTTP 404 상태 코드에 매핑됩니다.
 */
public class ScheduleNotFoundException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 오류 상세 메시지
     */
    public ScheduleNotFoundException(final String message) {
        super(message);
    }
}
