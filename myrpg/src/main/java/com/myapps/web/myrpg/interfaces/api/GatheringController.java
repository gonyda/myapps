package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.dto.WoodcutResult;
import com.myapps.web.myrpg.application.service.CharacterService;
import com.myapps.web.myrpg.application.service.GatheringService;
import com.myapps.web.myrpg.domain.model.CharacterProgress;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 생활 채집(장작 패기 등) 웹 요청을 처리하는 컨트롤러.
 *
 * <p>{@code POST /gathering/woodcut} 요청을 수신하여 5 SP 차감 및 50% 확률 장작 획득을 실행하고, 결과 DTO({@link
 * WoodcutResult})를 JSON으로 반환합니다.
 */
@Controller
public class GatheringController {

    private final GatheringService gatheringService;
    private final CharacterService characterService;

    /**
     * GatheringController를 생성합니다.
     *
     * @param gatheringService 채집 관리 서비스
     * @param characterService 캐릭터 진행상황 서비스
     */
    public GatheringController(
            final GatheringService gatheringService, final CharacterService characterService) {
        this.gatheringService = gatheringService;
        this.characterService = characterService;
    }

    /**
     * 장작 패기 채집을 실행하고 결과를 반환합니다.
     *
     * @param session HTTP 세션
     * @return 채집 결과 JSON DTO
     */
    @PostMapping("/gathering/woodcut")
    @ResponseBody
    public WoodcutResult woodcut(final HttpSession session) {
        final CharacterProgress progress = resolveCurrentCharacter(session);
        return gatheringService.gatherWood(progress);
    }

    private CharacterProgress resolveCurrentCharacter(final HttpSession session) {
        if (session != null) {
            final Object sessionUser = session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
            if (sessionUser instanceof UserSession userSession
                    && userSession.characterId() != null) {
                return characterService.loadByCharacterId(userSession.characterId());
            }
        }
        return characterService.loadOrCreateDefault();
    }
}
