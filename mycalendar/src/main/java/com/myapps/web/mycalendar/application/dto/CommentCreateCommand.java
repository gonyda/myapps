package com.myapps.web.mycalendar.application.dto;

import com.myapps.web.mycalendar.domain.model.Author;

/**
 * 댓글 생성 커맨드.
 *
 * <p>새로운 댓글을 생성할 때 필요한 데이터를 전달하는 DTO입니다.
 */
public record CommentCreateCommand(
    Author author,
    String content
) {}
