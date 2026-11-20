package com.myapps.web.myshare.application.service;

import com.myapps.web.myshare.domain.exception.InvalidMiniAppPayloadException;
import java.nio.charset.StandardCharsets;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 미니앱 저장소 요청 바디(raw {@link String})의 크기 제한 및 JSON 문법 유효성을 검증하는 서비스.
 *
 * <p>미니앱마다 자유로운 JSON 스키마를 허용하되, 비어 있는 본문, 최대 바이트 크기 초과, 손상된 JSON 문법 및 후행 토큰이 포함된 입력을 차단합니다.
 */
public class MiniAppJsonValidator {

    private static final int DEFAULT_MAX_PAYLOAD_BYTES = 1_048_576;

    private final int maxPayloadBytes;
    private final ObjectMapper strictObjectMapper;

    /**
     * MiniAppJsonValidator를 생성합니다.
     *
     * @param maxPayloadBytes 허용할 최대 UTF-8 바이트 크기 (0 이하일 경우 기본 1MB 적용)
     * @param objectMapper JSON 파싱에 사용할 Jackson 3 {@link ObjectMapper}
     */
    public MiniAppJsonValidator(final int maxPayloadBytes, final ObjectMapper objectMapper) {
        this.maxPayloadBytes = maxPayloadBytes > 0 ? maxPayloadBytes : DEFAULT_MAX_PAYLOAD_BYTES;
        this.strictObjectMapper =
                objectMapper
                        .rebuild()
                        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                        .build();
    }

    /**
     * 원시 JSON 문자열의 크기와 문법을 검증하고, 유효한 경우 원문 문자열을 그대로 반환합니다.
     *
     * @param rawJson 클라이언트가 전송한 원시 요청 바디
     * @return 검증을 통과한 원시 JSON 문자열
     * @throws InvalidMiniAppPayloadException 본문이 비어 있거나, 최대 크기를 초과하거나, 유효한 JSON이 아닌 경우
     */
    public String validate(final String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new InvalidMiniAppPayloadException("요청 본문이 비어 있습니다.");
        }
        final int byteLength = rawJson.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength > maxPayloadBytes) {
            throw new InvalidMiniAppPayloadException(
                    "요청 JSON 크기가 허용 한도(" + maxPayloadBytes + " bytes)를 초과했습니다.");
        }
        try {
            final JsonNode parsed = strictObjectMapper.readTree(rawJson);
            if (parsed == null) {
                throw new InvalidMiniAppPayloadException("유효한 JSON 형식이 아닙니다.");
            }
            return rawJson;
        } catch (final JacksonException ex) {
            throw new InvalidMiniAppPayloadException("유효한 JSON 형식이 아닙니다.", ex);
        }
    }
}
