package com.myapps.web.mycalendar.interfaces.dto;

import com.myapps.web.mycalendar.domain.model.Author;

/**
 * 댓글 생성 폼 바인딩용 DTO.
 *
 * <p>Thymeleaf 폼에서 사용자가 입력한 댓글 데이터를 바인딩하기 위한 record입니다.
 * author와 content 모두 필수 입력 필드입니다.
 *
 * @param author  댓글 작성자 (SEUNGKWON 또는 CHIWON)
 * @param content 댓글 내용 (필수, 최대 200자)
 */
public record CommentForm(
    Author author,
    String content
) {}
