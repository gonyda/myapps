package com.myapps.web.myrpg.application.dto;

/**
 * 로그인 폼 바인딩용 DTO 레코드.
 *
 * @param username 사용자 입력 로그인 아이디
 * @param password 사용자 입력 비밀번호
 */
public record LoginRequest(String username, String password) {}
