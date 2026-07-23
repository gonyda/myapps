package com.myapps.web.mycalendar.application.exception;

/**
 * 일정 유효성 검증 실패 시 발생하는 예외.
 *
 * <p>필수 필드 누락, 글자 수 초과, 날짜 범위 오류 등의 상황에서 발생하며
 * HTTP 400 상태 코드에 매핑됩니다.
 */
public class InvalidScheduleException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 유효성 검증 실패 상세 메시지
     */
    public InvalidScheduleException(final String message) {
        super(message);
    }
}
