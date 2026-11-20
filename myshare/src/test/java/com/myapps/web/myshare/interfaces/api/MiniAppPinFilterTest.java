package com.myapps.web.myshare.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** {@link MiniAppPinFilter}의 경로 분기 및 Fail-Closed PIN 검증 단위 테스트. */
class MiniAppPinFilterTest {

    @Test
    @DisplayName("서버 PIN이 설정되지 않았으면 올바르지 않은 요청을 모두 401 Unauthorized로 차단한다")
    void should_return401_when_serverPinIsBlank() throws Exception {
        // given
        final MiniAppPinFilter filter = new MiniAppPinFilter("   ");
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/mini/todo");
        request.addHeader("X-Mini-Pin", "anything");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicBoolean chainCalled = new AtomicBoolean(false);
        final MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(
                            final jakarta.servlet.ServletRequest req,
                            final jakarta.servlet.ServletResponse res) {
                        chainCalled.set(true);
                    }
                };

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Unauthorized");
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    @DisplayName("기존 /share 및 /health 경로는 PIN 헤더 없이도 필터를 통과한다")
    void should_bypassPinCheck_when_nonMiniAppPathRequested() throws Exception {
        // given
        final MiniAppPinFilter filter = new MiniAppPinFilter("secret-1234");
        final MockHttpServletRequest shareReq =
                new MockHttpServletRequest("GET", "/myshare/share/demo");
        shareReq.setContextPath("/myshare");
        final MockHttpServletResponse shareRes = new MockHttpServletResponse();
        final AtomicBoolean sharePassed = new AtomicBoolean(false);

        // when
        filter.doFilter(shareReq, shareRes, (req, res) -> sharePassed.set(true));

        // then
        assertThat(sharePassed.get()).isTrue();
    }
}
