package com.myapps.web.myrpg.application.dto;

import java.io.Serializable;

/**
 * 로그인 세션에 보관되는 불변 사용자 인증 세션 레코드.
 *
 * @param userId 사용자 계정 식별자 (PK)
 * @param username 로그인 아이디
 * @param nickname 사용자 표시 닉네임
 * @param characterId 연결된 캐릭터 식별자
 */
public record UserSession(Long userId, String username, String nickname, Long characterId)
        implements Serializable {}
