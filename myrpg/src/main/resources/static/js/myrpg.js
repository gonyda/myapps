// ===== 전체 지도: 줌/이동 (지도 앱처럼) =====
const MIN_SCALE = 0.5;
const MAX_SCALE = 4;

let mapScale = 1;
let mapTx = 0;
let mapTy = 0;

const mapViewport = document.getElementById("mapViewport");
const mapCanvas = document.getElementById("mapCanvas");

function applyMapTransform() {
    mapCanvas.style.transform =
        "translate(" + mapTx + "px," + mapTy + "px) scale(" + mapScale + ")";
}

// 뷰포트 기준 좌표 (px, py)를 중심으로 factor 만큼 확대/축소
function zoomAt(factor, px, py) {
    const newScale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, mapScale * factor));
    const applied = newScale / mapScale;
    mapTx = px - (px - mapTx) * applied;
    mapTy = py - (py - mapTy) * applied;
    mapScale = newScale;
    applyMapTransform();
}

// 줌 버튼: 뷰포트 중앙 기준
function mapZoom(factor) {
    const rect = mapViewport.getBoundingClientRect();
    zoomAt(factor, rect.width / 2, rect.height / 2);
}

function openMap() {
    document.getElementById("mapOverlay").classList.add("open");
    resetMapView();
}
function closeMap() {
    document.getElementById("mapOverlay").classList.remove("open");
}

// 팝업 열 때 지도를 뷰포트 가운데로 정렬
function resetMapView() {
    mapScale = 1;
    requestAnimationFrame(function () {
        const vp = mapViewport.getBoundingClientRect();
        mapTx = (vp.width - mapCanvas.offsetWidth) / 2;
        mapTy = (vp.height - mapCanvas.offsetHeight) / 2;
        applyMapTransform();
    });
}

function viewportPoint(clientX, clientY) {
    const rect = mapViewport.getBoundingClientRect();
    return { x: clientX - rect.left, y: clientY - rect.top };
}

// ----- 터치 제스처 (팬 + 핀치 줌) -----
let lastPanX = 0;
let lastPanY = 0;
let pinchDist = 0;
let isPanning = false;

function touchDistance(t1, t2) {
    const dx = t1.clientX - t2.clientX;
    const dy = t1.clientY - t2.clientY;
    return Math.hypot(dx, dy);
}

mapViewport.addEventListener("touchstart", function (e) {
    if (e.touches.length === 1) {
        isPanning = true;
        lastPanX = e.touches[0].clientX;
        lastPanY = e.touches[0].clientY;
    } else if (e.touches.length === 2) {
        isPanning = false;
        pinchDist = touchDistance(e.touches[0], e.touches[1]);
    }
}, { passive: false });

mapViewport.addEventListener("touchmove", function (e) {
    e.preventDefault();
    if (e.touches.length === 1 && isPanning) {
        const t = e.touches[0];
        mapTx += t.clientX - lastPanX;
        mapTy += t.clientY - lastPanY;
        lastPanX = t.clientX;
        lastPanY = t.clientY;
        applyMapTransform();
    } else if (e.touches.length === 2) {
        const newDist = touchDistance(e.touches[0], e.touches[1]);
        if (pinchDist > 0) {
            const midX = (e.touches[0].clientX + e.touches[1].clientX) / 2;
            const midY = (e.touches[0].clientY + e.touches[1].clientY) / 2;
            const p = viewportPoint(midX, midY);
            zoomAt(newDist / pinchDist, p.x, p.y);
        }
        pinchDist = newDist;
    }
}, { passive: false });

mapViewport.addEventListener("touchend", function (e) {
    if (e.touches.length === 0) {
        isPanning = false;
        pinchDist = 0;
    } else if (e.touches.length === 1) {
        isPanning = true;
        lastPanX = e.touches[0].clientX;
        lastPanY = e.touches[0].clientY;
        pinchDist = 0;
    }
});

// ----- 마우스 (데스크톱 테스트용): 드래그 팬 + 휠 줌 -----
let mouseDown = false;

mapViewport.addEventListener("mousedown", function (e) {
    mouseDown = true;
    lastPanX = e.clientX;
    lastPanY = e.clientY;
});

window.addEventListener("mousemove", function (e) {
    if (!mouseDown) {
        return;
    }
    mapTx += e.clientX - lastPanX;
    mapTy += e.clientY - lastPanY;
    lastPanX = e.clientX;
    lastPanY = e.clientY;
    applyMapTransform();
});

window.addEventListener("mouseup", function () {
    mouseDown = false;
});

mapViewport.addEventListener("wheel", function (e) {
    e.preventDefault();
    const p = viewportPoint(e.clientX, e.clientY);
    zoomAt(e.deltaY < 0 ? 1.1 : 0.9, p.x, p.y);
}, { passive: false });

// ===== 패널 팝업 (장비/인벤토리/스킬/정보) =====
function openPanel(title) {
    document.getElementById("panelTitle").textContent = title;
    document.getElementById("panelOverlay").classList.add("open");
}
function closePanel() {
    document.getElementById("panelOverlay").classList.remove("open");
}

// ===== 이동 패드: POST /move 호출 + DOM fragment swap =====
function move(dx, dy) {
    fetch("/move?dx=" + dx + "&dy=" + dy, { method: "POST" })
        .then(function (response) {
            if (!response.ok) {
                return;
            }
            return response.text();
        })
        .then(function (html) {
            if (!html) {
                return;
            }
            var container = document.createElement("div");
            container.innerHTML = html;

            var newTopBar = container.querySelector(".top-bar");
            var newCenter = container.querySelector(".center");
            var newActionLog = container.querySelector(".action-log");

            if (newTopBar) {
                var oldTopBar = document.querySelector(".top-bar");
                if (oldTopBar) {
                    oldTopBar.replaceWith(newTopBar);
                }
            }
            if (newCenter) {
                var oldCenter = document.querySelector(".center");
                if (oldCenter) {
                    oldCenter.replaceWith(newCenter);
                }
            }
            if (newActionLog) {
                var oldActionLog = document.querySelector(".action-log");
                if (oldActionLog) {
                    oldActionLog.replaceWith(newActionLog);
                    newActionLog.scrollTop = newActionLog.scrollHeight;
                }
            }

            var newMap = container.querySelector(".map-overlay");
            if (newMap) {
                var oldGrid = document.getElementById("mapGrid");
                var newGrid = newMap.querySelector("#mapGrid");
                if (oldGrid && newGrid) {
                    oldGrid.setAttribute("style", newGrid.getAttribute("style"));
                    oldGrid.innerHTML = newGrid.innerHTML;
                }
            }
        });
}

// ===== NPC 대화: POST /npc/talk 호출 + .center swap =====
function talkToNpc(npcId) {
    fetch("/npc/talk?npcId=" + npcId, { method: "POST" })
        .then(function (response) {
            if (!response.ok) {
                return;
            }
            return response.text();
        })
        .then(function (html) {
            if (!html) {
                return;
            }
            var container = document.createElement("div");
            container.innerHTML = html;

            var newCenter = container.querySelector(".center");
            if (newCenter) {
                var oldCenter = document.querySelector(".center");
                if (oldCenter) {
                    oldCenter.replaceWith(newCenter);
                }
            }
        });
}

// ===== NPC 행동 버튼 (라벨에 따라 분기) =====
function npcAction(label) {
    if (label === '은행') {
        openBank();
    } else {
        alert("구현 예정입니다");
    }
}

// ===== 인벤토리 팝업 열기/닫기 =====
function openInventory() {
    fetch('/inventory')
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('inventoryListArea').innerHTML = html;
            document.getElementById('inventoryOverlay').classList.add('open');
        });
}

function closeInventory() {
    document.getElementById('inventoryOverlay').classList.remove('open');
}

// ===== 인벤토리 아이템 사용 =====
function usePotion(ownedItemId) {
    fetch('/inventory/use?ownedItemId=' + ownedItemId, { method: 'POST' })
        .then(function (r) { return r.text(); })
        .then(function (html) {
            if (html) {
                document.getElementById('inventoryListArea').innerHTML = html;
            }
        });
}

// ===== 인벤토리 장비 착용 =====
function equipItem(ownedItemId) {
    fetch('/inventory/equip?ownedItemId=' + ownedItemId, { method: 'POST' })
        .then(function (r) {
            if (!r.ok) {
                return r.text().then(function (html) {
                    var container = document.createElement('div');
                    container.innerHTML = html;
                    var msg = container.querySelector('.error-container p');
                    alert(msg ? msg.textContent : '착용 할 수 없습니다');
                    return null;
                });
            }
            return r.text();
        })
        .then(function (html) {
            if (html) {
                document.getElementById('inventoryListArea').innerHTML = html;
            }
        });
}

// ===== 인벤토리 장비 해제 =====
function unequipItem(ownedItemId) {
    fetch('/inventory/unequip?ownedItemId=' + ownedItemId, { method: 'POST' })
        .then(function (r) { return r.text(); })
        .then(function (html) {
            if (html) {
                document.getElementById('inventoryListArea').innerHTML = html;
            }
        });
}

// ===== 아이템 상세 모달 열기/닫기 (임베드 데이터 활용) =====
function openItemDetail(element) {
    var detailData = element.getAttribute('data-detail');
    var nameEl = element.querySelector('.item-name');
    var itemName = nameEl ? nameEl.textContent : (element.getAttribute('data-name') || '아이템 상세');

    document.getElementById('itemDetailTitle').textContent = itemName;

    var body = document.getElementById('itemDetailBody');
    body.innerHTML = '';

    if (detailData) {
        var lines = detailData.split('||');
        for (var i = 0; i < lines.length; i++) {
            var p = document.createElement('p');
            p.textContent = lines[i];
            body.appendChild(p);
        }
    }

    document.getElementById('itemDetailOverlay').classList.add('open');
}

function closeItemDetail() {
    document.getElementById('itemDetailOverlay').classList.remove('open');
}

// ===== 인벤토리 클라이언트 정렬 =====
function sortInventory(criteria) {
    var list = document.getElementById('inventoryList');
    if (!list) { return; }

    var items = Array.prototype.slice.call(list.querySelectorAll('.inventory-item'));
    if (items.length === 0) { return; }

    // 정렬 버튼 활성 상태 전환
    var buttons = document.querySelectorAll('.sort-btn');
    for (var i = 0; i < buttons.length; i++) {
        buttons[i].classList.remove('active');
        if (buttons[i].getAttribute('data-sort') === criteria) {
            buttons[i].classList.add('active');
        }
    }

    items.sort(function (a, b) {
        if (criteria === 'name') {
            var nameA = a.getAttribute('data-name') || '';
            var nameB = b.getAttribute('data-name') || '';
            return nameA.localeCompare(nameB, 'ko');
        } else if (criteria === 'type') {
            var typeA = a.getAttribute('data-type') || '';
            var typeB = b.getAttribute('data-type') || '';
            if (typeA !== typeB) {
                return typeA.localeCompare(typeB);
            }
            var nameA2 = a.getAttribute('data-name') || '';
            var nameB2 = b.getAttribute('data-name') || '';
            return nameA2.localeCompare(nameB2, 'ko');
        }
        // 'default' - 획득순 (DOM 순서 = 서버 id asc)
        var idA = parseInt(a.getAttribute('data-owned-id'), 10) || 0;
        var idB = parseInt(b.getAttribute('data-owned-id'), 10) || 0;
        return idA - idB;
    });

    for (var j = 0; j < items.length; j++) {
        list.appendChild(items[j]);
    }
}

// ===== 임시 골드 버튼 =====
function goldGain() {
    fetch('/gold/gain', { method: 'POST' })
        .then(function (r) {
            if (!r.ok) { return; }
            return r.text();
        })
        .then(function (html) {
            if (!html) { return; }
            swapProgressResponse(html);
        });
}

function goldSpend() {
    fetch('/gold/spend', { method: 'POST' })
        .then(function (r) {
            if (!r.ok) { return; }
            return r.text();
        })
        .then(function (html) {
            if (!html) { return; }
            swapProgressResponse(html);
        });
}

// ===== 은행 팝업 열기/닫기 =====
function openBank() {
    fetch('/bank')
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('bankContent').innerHTML = html;
            document.getElementById('bankOverlay').classList.add('open');
        });
}

function closeBank() {
    document.getElementById('bankOverlay').classList.remove('open');
    closeBankModal();
}

// ===== 은행 입금/출금 소형 모달 =====
var bankModalMode = 'deposit';

function openDepositModal() {
    bankModalMode = 'deposit';
    document.getElementById('bankModalTitle').textContent = '입금';
    document.getElementById('bankModalAmount').value = '';
    document.getElementById('bankModalOverlay').style.display = 'flex';
}

function openWithdrawModal() {
    bankModalMode = 'withdraw';
    document.getElementById('bankModalTitle').textContent = '출금';
    document.getElementById('bankModalAmount').value = '';
    document.getElementById('bankModalOverlay').style.display = 'flex';
}

function closeBankModal() {
    var overlay = document.getElementById('bankModalOverlay');
    if (overlay) {
        overlay.style.display = 'none';
    }
}

function confirmBankModal() {
    var amountInput = document.getElementById('bankModalAmount');
    var amount = parseInt(amountInput.value, 10);
    if (!amount || amount < 1) {
        alert('1 이상의 금액을 입력해주세요.');
        return;
    }
    var url = bankModalMode === 'deposit'
        ? '/bank/deposit?amount=' + amount
        : '/bank/withdraw?amount=' + amount;

    fetch(url, { method: 'POST' })
        .then(function (response) {
            if (!response.ok) {
                return response.text().then(function (html) {
                    var container = document.createElement('div');
                    container.innerHTML = html;
                    var msg = container.querySelector('.error-message') || container.querySelector('p');
                    alert(msg ? msg.textContent : '골드가 부족합니다.');
                    return null;
                });
            }
            return response.text();
        })
        .then(function (html) {
            if (!html) { return; }
            refreshBankPopup(html);
            closeBankModal();
        });
}

// ===== 아이템 맡기기/찾기 =====
function depositItem(ownedItemId) {
    fetch('/bank/item/deposit?ownedItemId=' + ownedItemId, { method: 'POST' })
        .then(function (response) {
            if (!response.ok) {
                return response.text().then(function (html) {
                    var container = document.createElement('div');
                    container.innerHTML = html;
                    var msg = container.querySelector('.error-message') || container.querySelector('p');
                    alert(msg ? msg.textContent : '요청을 처리할 수 없습니다.');
                    return null;
                });
            }
            return response.text();
        })
        .then(function (html) {
            if (!html) { return; }
            refreshBankPopup(html);
        });
}

function withdrawItem(ownedItemId) {
    fetch('/bank/item/withdraw?ownedItemId=' + ownedItemId, { method: 'POST' })
        .then(function (response) {
            if (!response.ok) {
                return response.text().then(function (html) {
                    var container = document.createElement('div');
                    container.innerHTML = html;
                    var msg = container.querySelector('.error-message') || container.querySelector('p');
                    alert(msg ? msg.textContent : '요청을 처리할 수 없습니다.');
                    return null;
                });
            }
            return response.text();
        })
        .then(function (html) {
            if (!html) { return; }
            refreshBankPopup(html);
        });
}

function refreshBankPopup(html) {
    document.getElementById('bankContent').innerHTML = html;
}

// 페이지 로드 시 행동 로그를 맨 아래로 스크롤
document.addEventListener("DOMContentLoaded", function () {
    var actionLog = document.getElementById("actionLog");
    if (actionLog) {
        actionLog.scrollTop = actionLog.scrollHeight;
    }
});

// ===== 정보 팝업 열기/닫기 =====
function openInfo() {
    fetch('/info')
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('infoContent').innerHTML = html;
            document.getElementById('infoOverlay').classList.add('open');
        });
}
function closeInfo() {
    document.getElementById("infoOverlay").classList.remove("open");
}

// ===== 경험치 증가/감소: POST → 3영역 스왑 =====
function expUp() {
    fetch("/exp/up", { method: "POST" })
        .then(function (response) {
            if (!response.ok) { return; }
            return response.text();
        })
        .then(function (html) {
            if (!html) { return; }
            swapProgressResponse(html);
        });
}

function expDown() {
    fetch("/exp/down", { method: "POST" })
        .then(function (response) {
            if (!response.ok) { return; }
            return response.text();
        })
        .then(function (html) {
            if (!html) { return; }
            swapProgressResponse(html);
        });
}

// ===== 환생 2단계: 1단계 confirm → 재능 선택 팝업, 2단계 재능 선택 → POST =====
function rebirth() {
    if (!confirm("환생을 진행하시겠습니까?")) {
        return;
    }
    openTalentSelect();
}

function confirmRebirth(talent) {
    fetch("/rebirth?talent=" + talent, { method: "POST" })
        .then(function (response) {
            if (!response.ok) { return; }
            return response.text();
        })
        .then(function (html) {
            if (!html) { return; }
            swapProgressResponse(html);
            closeTalentSelect();
        });
}

function openTalentSelect() {
    document.getElementById("talentSelectOverlay").style.display = "flex";
}

function closeTalentSelect() {
    document.getElementById("talentSelectOverlay").style.display = "none";
}

// ===== 성장 응답 공통 스왑: .top-bar, #infoContent, .action-log =====
function swapProgressResponse(html) {
    var container = document.createElement("div");
    container.innerHTML = html;

    var newTopBar = container.querySelector(".top-bar");
    var newInfoContent = container.querySelector("#infoContent");
    var newActionLog = container.querySelector(".action-log");

    if (newTopBar) {
        var oldTopBar = document.querySelector(".top-bar");
        if (oldTopBar) {
            oldTopBar.replaceWith(newTopBar);
        }
    }
    if (newInfoContent) {
        var oldInfoContent = document.getElementById("infoContent");
        if (oldInfoContent) {
            oldInfoContent.innerHTML = newInfoContent.innerHTML;
        }
    }
    if (newActionLog) {
        var oldActionLog = document.querySelector(".action-log");
        if (oldActionLog) {
            oldActionLog.replaceWith(newActionLog);
            newActionLog.scrollTop = newActionLog.scrollHeight;
        }
    }
}

// ─── 스킬 팝업 ─────────────────────────────
function openSkillPopup() {
    document.getElementById('skillOverlay').classList.add('open');
    loadSkillTab('all');
}

function closeSkillPopup() {
    document.getElementById('skillOverlay').classList.remove('open');
    closeRankUpModal();
}

function loadSkillTab(tab) {
    fetch('/skills?tab=' + tab)
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('skillListArea').innerHTML = html;
        });
}

function openRankUpModal(skillId) {
    fetch('/skills/' + skillId + '/rankup-modal')
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('rankupModalArea').innerHTML = html;
            document.getElementById('rankupOverlay').classList.add('open');
        });
}

function closeRankUpModal() {
    document.getElementById('rankupOverlay').classList.remove('open');
}

function confirmRankUp(skillId) {
    if (!confirm('승급하시겠습니까?')) return;
    fetch('/skills/' + skillId + '/rankup', { method: 'POST' })
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('rankupModalArea').innerHTML = html;
            // 목록도 갱신
            loadSkillTab(getCurrentSkillTab());
        });
}

function fillUsage(skillId) {
    fetch('/skills/' + skillId + '/dev/fill-usage', { method: 'POST' })
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('rankupModalArea').innerHTML = html;
            loadSkillTab(getCurrentSkillTab());
        });
}

function fillKill(skillId) {
    fetch('/skills/' + skillId + '/dev/fill-kill', { method: 'POST' })
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('rankupModalArea').innerHTML = html;
            loadSkillTab(getCurrentSkillTab());
        });
}

function getCurrentSkillTab() {
    var activeTab = document.querySelector('.skill-tab.active');
    if (!activeTab) return 'all';
    var text = activeTab.textContent.trim();
    switch (text) {
        case '근접전투': return 'melee';
        case '활': return 'archery';
        case '마법': return 'magic';
        case '공용': return 'common';
        default: return 'all';
    }
}
