package com.myapps.web.mycalendar.application.dto;

import java.time.LocalDateTime;

import com.myapps.web.mycalendar.domain.model.Author;

/**
 * 댓글 조회 응답.
 *
 * <p>댓글 조회 시 클라이언트에 반환되는 응답 DTO입니다.
 */
public record CommentResponse(
    Long id,
    Author author,
    String content,
    LocalDateTime createdAt
) {}
