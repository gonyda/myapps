package com.myapps.web.mystudy.application.dto;

import java.util.List;

/**
 * 퀴즈 API 응답을 나타내는 DTO.
 *
 * <p>생성된 퀴즈 문제 목록을 클라이언트에 전달합니다.
 *
 * @param questions 퀴즈 문제 목록
 */
public record QuizResponse(List<QuizQuestionDto> questions) {}
