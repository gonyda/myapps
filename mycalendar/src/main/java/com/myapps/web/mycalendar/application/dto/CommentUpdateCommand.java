package com.myapps.web.mycalendar.application.dto;

/**
 * 댓글 수정 커맨드.
 *
 * <p>기존 댓글을 수정할 때 필요한 데이터를 전달하는 DTO입니다.
 */
public record CommentUpdateCommand(
    String content
) {}
