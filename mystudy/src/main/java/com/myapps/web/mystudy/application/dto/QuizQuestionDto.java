package com.myapps.web.mystudy.application.dto;

import java.util.List;

/**
 * 개별 퀴즈 문제를 나타내는 DTO.
 *
 * <p>문제 텍스트, 객관식 보기 목록, 정답 인덱스를 포함합니다.
 *
 * @param question 문제 텍스트 (영어 또는 한국어 문장)
 * @param choices 객관식 보기 목록
 * @param answerIndex 정답 보기의 인덱스 (0-based)
 */
public record QuizQuestionDto(String question, List<String> choices, int answerIndex) {}
