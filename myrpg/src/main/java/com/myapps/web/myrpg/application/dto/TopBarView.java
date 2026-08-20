package com.myapps.web.myrpg.application.dto;

/**
 * 상단바 UI 영역의 뷰 모델 레코드.
 *
 * <p>닉네임, 레벨, EXP/HP/MP/Stamina 게이지 정보를 담아 Thymeleaf 템플릿에서 렌더링에 사용한다.
 *
 * @param nickname 캐릭터 닉네임
 * @param level 현재 레벨
 * @param exp 경험치 게이지
 * @param hp HP 게이지
 * @param mp MP 게이지
 * @param stamina Stamina 게이지
 */
public record TopBarView(
        String nickname, int level, GaugeView exp, GaugeView hp, GaugeView mp, GaugeView stamina) {}
