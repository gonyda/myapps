package com.myapps.web.myrpg.application.dto;

/**
 * 필드 스킬(힐링 등) 사용 결과 DTO.
 *
 * @param success 성공 여부
 * @param message 결과 메시지
 * @param hpCurrent 갱신된 현재 HP
 * @param maxHp 최대 HP
 * @param mpCurrent 갱신된 현재 MP
 * @param maxMp 최대 MP
 * @param healedAmount 회복된 HP 수치
 */
public record FieldSkillResult(
        boolean success,
        String message,
        int hpCurrent,
        int maxHp,
        int mpCurrent,
        int maxMp,
        int healedAmount) {

    /**
     * 실패 결과를 생성한다.
     *
     * @param message 실패 사유
     * @param hpCurrent 현재 HP
     * @param maxHp 최대 HP
     * @param mpCurrent 현재 MP
     * @param maxMp 최대 MP
     * @return 실패 FieldSkillResult
     */
    public static FieldSkillResult failure(
            final String message,
            final int hpCurrent,
            final int maxHp,
            final int mpCurrent,
            final int maxMp) {
        return new FieldSkillResult(false, message, hpCurrent, maxHp, mpCurrent, maxMp, 0);
    }

    /**
     * 성공 결과를 생성한다.
     *
     * @param message 성공 메시지
     * @param hpCurrent 회복 후 현재 HP
     * @param maxHp 최대 HP
     * @param mpCurrent 소모 후 현재 MP
     * @param maxMp 최대 MP
     * @param healedAmount 실제 회복된 HP
     * @return 성공 FieldSkillResult
     */
    public static FieldSkillResult success(
            final String message,
            final int hpCurrent,
            final int maxHp,
            final int mpCurrent,
            final int maxMp,
            final int healedAmount) {
        return new FieldSkillResult(
                true, message, hpCurrent, maxHp, mpCurrent, maxMp, healedAmount);
    }
}
