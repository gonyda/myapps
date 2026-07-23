/**
 * 캘린더 스와이프 네비게이션 모듈.
 *
 * Hammer.js를 활용하여 좌/우 스와이프로 월 이동을 구현합니다.
 * - 좌 스와이프(swipeleft) → 다음 월 이동
 * - 우 스와이프(swiperight) → 이전 월 이동
 *
 * Requirements: 9.5, 9.6
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var calendarContainer = document.querySelector('[data-swipe-container]');
        if (!calendarContainer) {
            return;
        }

        var prevUrl = calendarContainer.getAttribute('data-prev-url');
        var nextUrl = calendarContainer.getAttribute('data-next-url');

        if (typeof Hammer === 'undefined') {
            return;
        }

        var hammer = new Hammer(calendarContainer);
        hammer.get('swipe').set({ direction: Hammer.DIRECTION_HORIZONTAL });

        hammer.on('swipeleft', function () {
            if (nextUrl) {
                window.location.href = nextUrl;
            }
        });

        hammer.on('swiperight', function () {
            if (prevUrl) {
                window.location.href = prevUrl;
            }
        });
    });
})();
