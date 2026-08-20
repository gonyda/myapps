package com.myapps.web.myrpg.domain.model;

/**
 * 스킬 랭크업에 필요한 조건(사용 횟수 + 막타 처치 수)을 나타내는 불변 record.
 *
 * <p>각 랭크 전이(현재→다음)마다 고유한 요구치가 존재하며, 랭크가 오를수록 두 값 모두 단조 증가한다.
 *
 * @param requiredUsage 다음 랭크로 승급하기 위한 필요 사용 횟수 (양수)
 * @param requiredKills 다음 랭크로 승급하기 위한 필요 막타 처치 수 (양수)
 */
public record RankUpRequirement(int requiredUsage, int requiredKills) {}
