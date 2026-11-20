package com.myapps.web.myshare.interfaces.api;

import com.myapps.web.myshare.domain.exception.InvalidMiniAppKeyException;
import com.myapps.web.myshare.domain.exception.InvalidMiniAppPayloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** {@link MiniAppController}에서 발생하는 예외를 {@code application/json} 오류 응답으로 변환하는 전용 예외 처리기. */
@RestControllerAdvice(assignableTypes = MiniAppController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MiniAppExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MiniAppExceptionHandler.class);

    /**
     * 잘못된 미니앱 키 또는 잘못된 JSON 페이로드 요청을 400 Bad Request JSON으로 처리합니다.
     *
     * @param ex 입력 검증 예외
     * @return 400 JSON 오류 응답
     */
    @ExceptionHandler({InvalidMiniAppKeyException.class, InvalidMiniAppPayloadException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(final RuntimeException ex) {
        return jsonError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * 예상하지 못한 내부 서버 오류를 500 Internal Server Error JSON으로 처리합니다.
     *
     * @param ex 예기치 못한 예외
     * @return 500 JSON 오류 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(final Exception ex) {
        LOG.error("미니앱 저장소 API 처리 중 예기치 못한 오류가 발생했습니다: {}", ex.getMessage(), ex);
        return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
    }

    private ResponseEntity<ErrorResponse> jsonError(final HttpStatus status, final String message) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setCacheControl(CacheControl.noStore());
        headers.set("X-Content-Type-Options", "nosniff");
        return ResponseEntity.status(status)
                .headers(headers)
                .body(new ErrorResponse(message != null ? message : status.getReasonPhrase()));
    }

    /**
     * JSON API 오류 응답 본문 레코드.
     *
     * @param error 오류 메시지
     */
    public record ErrorResponse(String error) {}
}
