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

// ===== NPC 행동 버튼 (미구현 플레이스홀더) =====
function npcAction() {
    alert("구현 예정입니다");
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
    document.getElementById("infoOverlay").classList.add("open");
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
}

function loadSkillTab(tab) {
    fetch('/skills?tab=' + tab)
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('skillListArea').innerHTML = html;
            document.getElementById('rankupModalArea').style.display = 'none';
        });
}

function openRankUpModal(skillId) {
    fetch('/skills/' + skillId + '/rankup-modal')
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('rankupModalArea').innerHTML = html;
            document.getElementById('rankupModalArea').style.display = 'block';
        });
}

function closeRankUpModal() {
    document.getElementById('rankupModalArea').style.display = 'none';
}

function confirmRankUp(skillId) {
    if (!confirm('승급하시겠습니까?')) return;
    fetch('/skills/' + skillId + '/rankup', { method: 'POST' })
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('rankupModalArea').innerHTML = html;
        });
}

function fillUsage(skillId) {
    fetch('/skills/' + skillId + '/dev/fill-usage', { method: 'POST' })
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('rankupModalArea').innerHTML = html;
        });
}

function fillKill(skillId) {
    fetch('/skills/' + skillId + '/dev/fill-kill', { method: 'POST' })
        .then(function (r) { return r.text(); })
        .then(function (html) {
            document.getElementById('rankupModalArea').innerHTML = html;
        });
}
