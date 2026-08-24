package com.myapps.web.myrpg.interfaces.api;

import com.myapps.web.myrpg.application.dto.UserSession;
import com.myapps.web.myrpg.application.exception.AuthenticationException;
import com.myapps.web.myrpg.application.service.AuthService;
import com.myapps.web.myrpg.infrastructure.interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 사용자 로그인 및 로그아웃 요청을 처리하는 웹 컨트롤러.
 *
 * <p>로그인 폼 렌더링, 자격증명 제출 처리, 세션 발급 및 무효화(로그아웃)를 담당합니다.
 */
@Controller
public class AuthController {

    private static final String LOGIN_VIEW = "login";
    private static final String REDIRECT_HOME = "redirect:/";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String MODEL_ATTR_ERROR = "error";

    private final AuthService authService;

    /**
     * AuthController를 생성한다.
     *
     * @param authService 인증 서비스
     */
    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    /**
     * 로그인 화면을 렌더링한다.
     *
     * <p>이미 로그인된 사용자의 경우 메인 게임 화면({@code /})으로 리다이렉트합니다.
     *
     * @param session 현재 HTTP 세션
     * @param model Spring MVC 모델
     * @return 뷰 이름 또는 리다이렉트 경로
     */
    @GetMapping("/login")
    public String loginPage(final HttpSession session, final Model model) {
        if (session != null && session.getAttribute(AuthInterceptor.SESSION_USER_KEY) != null) {
            return REDIRECT_HOME;
        }
        return LOGIN_VIEW;
    }

    /**
     * 로그인 폼 제출을 처리하고 세션을 발급한다.
     *
     * @param username 사용자 아이디
     * @param password 비밀번호
     * @param session HTTP 세션
     * @param model Spring MVC 모델
     * @return 성공 시 메인 화면 리다이렉트, 실패 시 에러 메시지와 함께 로그인 뷰 반환
     */
    @PostMapping("/login")
    public String login(
            @RequestParam("username") final String username,
            @RequestParam("password") final String password,
            final HttpSession session,
            final Model model) {
        try {
            final UserSession userSession = authService.authenticate(username, password);
            session.setAttribute(AuthInterceptor.SESSION_USER_KEY, userSession);
            return REDIRECT_HOME;
        } catch (final AuthenticationException exception) {
            model.addAttribute(MODEL_ATTR_ERROR, exception.getMessage());
            return LOGIN_VIEW;
        }
    }

    /**
     * 로그아웃을 처리하고 세션을 무효화한다.
     *
     * @param session HTTP 세션
     * @return 로그인 화면 리다이렉트
     */
    @GetMapping("/logout")
    public String logoutGet(final HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return REDIRECT_LOGIN;
    }

    /**
     * POST 방식 로그아웃을 처리한다.
     *
     * @param session HTTP 세션
     * @return 로그인 화면 리다이렉트
     */
    @PostMapping("/logout")
    public String logoutPost(final HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return REDIRECT_LOGIN;
    }
}
