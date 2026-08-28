package com.myapps.web.myrpg.application.dto;

import com.myapps.web.myrpg.domain.model.ActionLogEntry;
import java.util.List;

/**
 * 플레이 화면 전체 뷰를 집계하는 뷰 모델 레코드.
 *
 * <p>상단바, 미니맵, 전체지도, 상황 멘트, NPC 대화, 몬스터 조우, 상호작용 목록, NPC/몬스터 행동 버튼, 행동 로그, 정보 팝업 등 화면 렌더링에 필요한 모든
 * 데이터를 하나로 묶어 제공한다.
 *
 * <p>NPC 대사 슬롯과 몬스터 대사 슬롯은 동시에 활성되지 않는다. NPC 클릭 시 몬스터 슬롯이, 몬스터 클릭 시 NPC 슬롯이 비워진다(null).
 *
 * @param topBar 상단바 뷰 모델
 * @param minimap 미니맵 뷰 모델
 * @param fullMap 전체지도 뷰 모델
 * @param ambience 상황 멘트 텍스트
 * @param ambienceEmoji 상황 멘트 시간대 이모지 (예: "🌅", "☀️", "🌙")
 * @param npcName 현재 노드 NPC 이름 (없으면 null)
 * @param npcDialogue NPC 대사 텍스트 (없으면 null)
 * @param interactions 상호작용 대상 목록 (없으면 null)
 * @param npcActions NPC 행동 버튼 목록 (대화 중이 아니면 null)
 * @param monsterName 몬스터 이름 (조우 중이 아니면 null)
 * @param monsterDialogue 몬스터 대사 텍스트 (조우 중이 아니면 null)
 * @param monsterLevel 몬스터 레벨 (조우 중이 아니면 null)
 * @param monsterMaxHp 몬스터 최대 체력 (조우 중이 아니면 null)
 * @param monsterActions 몬스터 행동 버튼 목록 (조우 중이 아니면 null)
 * @param monsterBoss 보스 몬스터 여부
 * @param logs 행동 로그 항목 목록 (오름차순)
 * @param info 정보 팝업 뷰 모델 (상/중/하 3구역)
 */
public record PlayScreenView(
        TopBarView topBar,
        MinimapView minimap,
        FullMapView fullMap,
        String ambience,
        String ambienceEmoji,
        String timeOfDayKey,
        String inGameTime,
        String npcName,
        String npcDialogue,
        List<InteractionItem> interactions,
        List<ActionButton> npcActions,
        String monsterName,
        String monsterDialogue,
        Integer monsterLevel,
        Integer monsterMaxHp,
        List<ActionButton> monsterActions,
        boolean monsterBoss,
        List<ActionLogEntry> logs,
        InfoPopupView info) {

    /** 기존 18인자 보조 생성자 (하위 호환: inGameTime="08:00"). */
    public PlayScreenView(
            final TopBarView topBar,
            final MinimapView minimap,
            final FullMapView fullMap,
            final String ambience,
            final String ambienceEmoji,
            final String timeOfDayKey,
            final String npcName,
            final String npcDialogue,
            final List<InteractionItem> interactions,
            final List<ActionButton> npcActions,
            final String monsterName,
            final String monsterDialogue,
            final Integer monsterLevel,
            final Integer monsterMaxHp,
            final List<ActionButton> monsterActions,
            final boolean monsterBoss,
            final List<ActionLogEntry> logs,
            final InfoPopupView info) {
        this(
                topBar,
                minimap,
                fullMap,
                ambience,
                ambienceEmoji,
                timeOfDayKey,
                "08:00",
                npcName,
                npcDialogue,
                interactions,
                npcActions,
                monsterName,
                monsterDialogue,
                monsterLevel,
                monsterMaxHp,
                monsterActions,
                monsterBoss,
                logs,
                info);
    }

    /** 기존 17인자 보조 생성자 (하위 호환: timeOfDayKey="afternoon", inGameTime="08:00"). */
    public PlayScreenView(
            final TopBarView topBar,
            final MinimapView minimap,
            final FullMapView fullMap,
            final String ambience,
            final String ambienceEmoji,
            final String npcName,
            final String npcDialogue,
            final List<InteractionItem> interactions,
            final List<ActionButton> npcActions,
            final String monsterName,
            final String monsterDialogue,
            final Integer monsterLevel,
            final Integer monsterMaxHp,
            final List<ActionButton> monsterActions,
            final boolean monsterBoss,
            final List<ActionLogEntry> logs,
            final InfoPopupView info) {
        this(
                topBar,
                minimap,
                fullMap,
                ambience,
                ambienceEmoji,
                "afternoon",
                "08:00",
                npcName,
                npcDialogue,
                interactions,
                npcActions,
                monsterName,
                monsterDialogue,
                monsterLevel,
                monsterMaxHp,
                monsterActions,
                monsterBoss,
                logs,
                info);
    }

    /** 기존 15인자 보조 생성자 (하위 호환: ambienceEmoji="☀️", timeOfDayKey="afternoon", monsterBoss=false). */
    public PlayScreenView(
            final TopBarView topBar,
            final MinimapView minimap,
            final FullMapView fullMap,
            final String ambience,
            final String npcName,
            final String npcDialogue,
            final List<InteractionItem> interactions,
            final List<ActionButton> npcActions,
            final String monsterName,
            final String monsterDialogue,
            final Integer monsterLevel,
            final Integer monsterMaxHp,
            final List<ActionButton> monsterActions,
            final List<ActionLogEntry> logs,
            final InfoPopupView info) {
        this(
                topBar,
                minimap,
                fullMap,
                ambience,
                "☀️",
                "afternoon",
                npcName,
                npcDialogue,
                interactions,
                npcActions,
                monsterName,
                monsterDialogue,
                monsterLevel,
                monsterMaxHp,
                monsterActions,
                false,
                logs,
                info);
    }

    /**
     * 몬스터 슬롯 없는 기존 10인자 보조 생성자 (하위 호환).
     *
     * @param topBar 상단바 뷰 모델
     * @param minimap 미니맵 뷰 모델
     * @param fullMap 전체지도 뷰 모델
     * @param ambience 상황 멘트 텍스트
     * @param npcName NPC 이름 (없으면 null)
     * @param npcDialogue NPC 대사 (없으면 null)
     * @param interactions 상호작용 대상 목록
     * @param npcActions NPC 행동 버튼 목록
     * @param logs 행동 로그 항목 목록
     * @param info 정보 팝업 뷰 모델
     */
    public PlayScreenView(
            final TopBarView topBar,
            final MinimapView minimap,
            final FullMapView fullMap,
            final String ambience,
            final String npcName,
            final String npcDialogue,
            final List<InteractionItem> interactions,
            final List<ActionButton> npcActions,
            final List<ActionLogEntry> logs,
            final InfoPopupView info) {
        this(
                topBar,
                minimap,
                fullMap,
                ambience,
                "☀️",
                "afternoon",
                npcName,
                npcDialogue,
                interactions,
                npcActions,
                null,
                null,
                null,
                null,
                null,
                false,
                logs,
                info);
    }
}
