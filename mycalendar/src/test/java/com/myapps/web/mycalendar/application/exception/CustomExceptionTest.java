package com.myapps.web.mycalendar.application.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 커스텀 예외 클래스의 동작을 검증하는 단위 테스트. */
class CustomExceptionTest {

    @Test
    @DisplayName("ScheduleNotFoundException은 RuntimeException을 상속하고 메시지를 전달한다")
    void should_createScheduleNotFoundException_when_messageProvided() {
        final String message = "일정을 찾을 수 없습니다: ID=1";

        final ScheduleNotFoundException exception = new ScheduleNotFoundException(message);

        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("InvalidScheduleException은 RuntimeException을 상속하고 메시지를 전달한다")
    void should_createInvalidScheduleException_when_messageProvided() {
        final String message = "200자를 초과할 수 없습니다";

        final InvalidScheduleException exception = new InvalidScheduleException(message);

        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(message, exception.getMessage());
    }
}
