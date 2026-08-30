/**
 * Game Messages Dictionary & Formatter (SSOT for Client)
 */
(function (global) {
    'use strict';

    const MESSAGES = {
        // System & Common
        'system.move_blocked': '전투 중에는 이동할 수 없습니다.',
        'system.resource_lack': '{0}이(가) 부족합니다.',
        'system.inventory.full': '인벤토리가 가득 찼습니다 (최대 {0}칸)',
        'system.notice.coming_soon': '추후 구현 예정입니다.',
        'system.error.weapon_swap': '무기 스왑 중 오류가 발생했습니다.',
        'system.error.gathering': '채집 중 오류가 발생했습니다.',
        'system.error.generic': '요청을 처리할 수 없습니다.',

        // Action Log & Battle Notifications
        'battle.ambush': '매복하고 있던 {0}이(가) 기습해옵니다!',
        'battle.monster_slain': '{0}이(가) 쓰러졌습니다!',
        'battle.death': '정신을 잃고 쓰러졌습니다… 티르코네일에서 되살아납니다.',
        'battle.flee.success': '도망 성공!',

        // Town & Facilities
        'town.heal.success': '치료되었습니다!',
        'town.repair.success': '🔨 수리 성공!',
        'town.repair.failure': '💥 수리 실패 (최대 내구도 1 감소)',
        'town.repair.cannot_repair': '수리할 수 없습니다.',
        'shop.cannot_buy': '구매할 수 없습니다.',
        'shop.cannot_sell': '판매할 수 없습니다.',

        // Economy & Exceptions
        'exception.equip.shield_conflict': '방패와 함께 착용할 수 없습니다.',
        'exception.equip.unequip_before_sell': '장착을 해제한 후 판매할 수 있습니다.',
        'exception.equip.cannot_equip': '착용할 수 없습니다.',
        'exception.economy.insufficient_gold': '골드가 부족합니다.',
        'exception.vital.insufficient_stamina': '스태미나가 부족합니다 (필요: {0} SP)',
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
