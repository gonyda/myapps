package com.myapps.web.mycalendar.application.exception;

/**
 * 댓글 유효성 검증 실패 시 발생하는 예외.
 *
 * <p>내용 누락, 글자 수 초과, 작성자 미선택 등의 상황에서 발생하며
 * HTTP 400 상태 코드에 매핑됩니다.
 */
public class InvalidCommentException extends RuntimeException {

    /**
     * 오류 메시지를 포함하여 예외를 생성합니다.
     *
     * @param message 유효성 검증 실패 상세 메시지
     */
    public InvalidCommentException(final String message) {
        super(message);
    }
}
