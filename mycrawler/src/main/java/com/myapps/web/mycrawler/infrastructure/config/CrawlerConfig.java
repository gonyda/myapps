package com.myapps.web.mycrawler.infrastructure.config;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.myapps.web.mycrawler.domain.model.CrawlTarget;

/**
 * 크롤러 설정을 바인딩하는 레코드.
 *
 * <p>application.yml의 crawler 접두사 하위 속성을 매핑하며,
 * 타겟 목록에 대한 유효성 검증 기능을 제공합니다.
 *
 * @param cron           크롤링 스케줄 cron 표현식
 * @param timeoutSeconds 페이지 로드 타임아웃 (초)
 * @param browsersPath   Playwright 브라우저 캐시 디렉터리 경로
 * @param targets        크롤링 대상 목록
 */
@ConfigurationProperties(prefix = "crawler")
public record CrawlerConfig(
    String cron,
    long timeoutSeconds,
    String browsersPath,
    List<TargetConfig> targets
) {

    private static final Logger log = LoggerFactory.getLogger(CrawlerConfig.class);

    /**
     * 크롤링 대상 설정 항목을 나타내는 중첩 레코드.
     *
     * @param name 크롤링 대상의 식별 이름
     * @param url  크롤링할 대상 URL
     */
    public record TargetConfig(String name, String url) {
    }

    /**
     * 유효성 검증을 통과한 크롤링 대상 목록을 반환합니다.
     *
     * <p>다음 조건에 해당하는 항목은 필터링되고 경고 로그가 출력됩니다:
     * <ul>
     *   <li>이름이 null이거나 빈 문자열인 경우</li>
     *   <li>URL이 null이거나 빈 문자열인 경우</li>
     *   <li>URL 형식이 유효하지 않은 경우</li>
     *   <li>이름이 중복된 경우 (첫 번째 항목만 유지)</li>
     * </ul>
     *
     * @return 유효한 {@link CrawlTarget} 목록
     */
    public List<CrawlTarget> validTargets() {
        if (targets == null || targets.isEmpty()) {
            log.warn("크롤링 대상 목록이 비어있습니다. 설정을 확인하세요.");
            return List.of();
        }

        final List<CrawlTarget> validList = new ArrayList<>();
        final Set<String> seenNames = new HashSet<>();

        for (final TargetConfig target : targets) {
            if (!isValidTarget(target, seenNames)) {
                continue;
            }
            seenNames.add(target.name());
            validList.add(new CrawlTarget(target.name(), target.url()));
        }

        if (validList.isEmpty()) {
            log.warn("유효한 크롤링 대상이 없습니다. 모든 항목이 필터링되었습니다.");
        }

        return List.copyOf(validList);
    }

    private boolean isValidTarget(final TargetConfig target, final Set<String> seenNames) {
        if (target.name() == null || target.name().isBlank()) {
            log.warn("크롤링 대상의 이름이 누락되었습니다. 해당 항목을 무시합니다. url={}", target.url());
            return false;
        }

        if (target.url() == null || target.url().isBlank()) {
            log.warn("크롤링 대상의 URL이 누락되었습니다. 해당 항목을 무시합니다. name={}", target.name());
            return false;
        }

        if (!isValidUrl(target.url())) {
            log.warn("크롤링 대상의 URL 형식이 유효하지 않습니다. 해당 항목을 무시합니다. name={}, url={}",
                    target.name(), target.url());
            return false;
        }

        if (seenNames.contains(target.name())) {
            log.warn("크롤링 대상의 이름이 중복됩니다. 해당 항목을 무시합니다. name={}", target.name());
            return false;
        }

        return true;
    }

    private boolean isValidUrl(final String url) {
        try {
            final URI uri = new URI(url);
            uri.toURL();
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (final URISyntaxException | MalformedURLException | IllegalArgumentException exception) {
            return false;
        }
    }
}
