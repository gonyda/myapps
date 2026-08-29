package com.myapps.web.myrpg.domain.model;

/**
 * 스킬 랭크업에 필요한 사용 횟수 조건을 나타내는 불변 record.
 *
 * <p>각 랭크 전이(현재→다음)마다 고유한 요구치가 존재하며, 랭크가 오를수록 사용 횟수 요구치가 단조 증가한다. 패시브 스킬의 요구치는 0이다.
 *
 * @param requiredUsage 다음 랭크로 승급하기 위한 필요 사용 횟수 (양수, 패시브는 0)
 */
public record RankUpRequirement(int requiredUsage) {}
