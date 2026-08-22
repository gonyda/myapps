package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.PlayScreenView;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.DungeonService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 던전 입장, 나가기 및 방 이동을 처리하는 웹 컨트롤러.
 *
 * <p>{@code POST /dungeon/enter}로 던전에 진입하고, {@code POST /dungeon/leave}로 시작방에서 던전을 자발적으로 나가며,
 * {@code POST /dungeon/move}로 연결된 인접 방으로 이동한다.
 *
 * <p>응답은 갱신된 플레이 화면 프래그먼트({@code fragments/move-response})를 반환하여 상단바, 미니맵, 센터 화면 및 액션 로그를 동기화한다.
 */
@Controller
@RequestMapping("/dungeon")
public class DungeonController {

    private static final String MOVE_RESPONSE_FRAGMENT = "fragments/move-response";

    private final DungeonService dungeonService;
    private final CharacterService characterService;
    private final NodeViewAssembler nodeViewAssembler;

    /**
     * DungeonController를 생성한다.
     *
     * @param dungeonService 던전 생명주기 및 이동 처리 서비스
     * @param characterService 캐릭터 조회/저장 서비스
     * @param nodeViewAssembler 뷰 모델 조립 컴포넌트
     */
    public DungeonController(
            final DungeonService dungeonService,
            final CharacterService characterService,
            final NodeViewAssembler nodeViewAssembler) {
        this.dungeonService = dungeonService;
        this.characterService = characterService;
        this.nodeViewAssembler = nodeViewAssembler;
    }

    /**
     * 던전에 입장하고 시작방 플레이 화면 프래그먼트를 반환한다.
     *
     * @param dungeonId 입장할 던전 식별자 (예: "alby")
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름
     */
    @PostMapping("/enter")
    public String enter(@RequestParam final String dungeonId, final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        dungeonService.enterDungeon(progress.getId(), dungeonId);
        final PlayScreenView view = nodeViewAssembler.fromProgress(progress);
        model.addAttribute("view", view);
        return MOVE_RESPONSE_FRAGMENT;
    }

    /**
     * 시작방에서 던전을 나가고 필드의 던전 입구 노드 플레이 화면 프래그먼트를 반환한다.
     *
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름
     */
    @PostMapping("/leave")
    public String leave(final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        dungeonService.leaveDungeon(progress.getId());
        final PlayScreenView view = nodeViewAssembler.fromProgress(progress);
        model.addAttribute("view", view);
        return MOVE_RESPONSE_FRAGMENT;
    }

    /**
     * 던전 내 연결된 인접 방으로 이동하고 갱신된 플레이 화면 프래그먼트를 반환한다.
     *
     * @param targetRoomId 이동할 대상 방 ID
     * @param model Spring MVC 모델
     * @return 프래그먼트 뷰 이름
     */
    @PostMapping("/move")
    public String move(@RequestParam final String targetRoomId, final Model model) {
        final CharacterProgress progress = characterService.loadOrCreateDefault();
        dungeonService.moveToRoom(progress.getId(), targetRoomId);
        final PlayScreenView view = nodeViewAssembler.fromProgress(progress);
        model.addAttribute("view", view);
        return MOVE_RESPONSE_FRAGMENT;
    }
}
