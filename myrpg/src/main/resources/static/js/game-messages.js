/**
 * Game Messages Dictionary & Formatter (SSOT for Client)
 */
(function (global) {
    'use strict';

    const MESSAGES = {
        // UI & System Alerts
        'ui.cannot_move_in_combat': '전투 중에는 이동할 수 없습니다.',
        'ui.ambush_alert': '매복하고 있던 {0}이(가) 기습해옵니다!',
        'ui.monster_defeated': '{0}이(가) 쓰러졌습니다!',
        'ui.player_defeated': '정신을 잃고 쓰러졌습니다… 티르코네일에서 되살아납니다.',
        'ui.flee_success': '도망 성공!',
        'ui.feature_planned': '추후 설계 예정입니다.',
        'ui.feature_not_implemented': '구현 예정입니다',
        'ui.generic_error': '요청을 처리할 수 없습니다.',

        // Town & Facilities
        'town.heal.success': '치료되었습니다!',
        'town.repair.success': '🔨 수리 성공!',
        'town.repair.failure': '💥 수리 실패 (최대 내구도 1 감소)',
        'town.repair.cannot_repair': '수리할 수 없습니다.',
        'shop.cannot_buy': '구매할 수 없습니다.',
        'shop.cannot_sell': '판매할 수 없습니다.',
        'exception.equip.cannot_equip': '착용 할 수 없습니다',

        // Economy & Inventory
        'economy.insufficient_gold': '골드가 부족합니다.',
        'bank.invalid_amount': '1 이상의 금액을 입력해주세요.',
        'skill.invalid_slot': '1부터 10 사이의 슬롯 번호를 입력해 주세요.',

        // Dialogues & Confirms
        'confirm.dungeon_leave': '던전을 나가시겠습니까?',
        'confirm.rebirth': '환생을 진행하시겠습니까?',
        'confirm.rankup': '승급하시겠습니까?',
        'confirm.clear_all_slots': '모든 스킬 슬롯 배정을 비우시겠습니까?',
        'confirm.clear_slot': '{0}번 슬롯을 비우시겠습니까?'
    };

    function format(template, args) {
        if (!template) return '';
        return template.replace(/\{(\d+)\}/g, function (match, index) {
            const idx = parseInt(index, 10);
            return args[idx] !== undefined ? args[idx] : match;
        });
    }

    global.GAME_MESSAGES = {
        get: function (key) {
            const args = Array.prototype.slice.call(arguments, 1);
            const template = MESSAGES[key] || key;
            return format(template, args);
        },
        has: function (key) {
            return Object.prototype.hasOwnProperty.call(MESSAGES, key);
        }
    };
})(typeof window !== 'undefined' ? window : globalThis);
