package com.myapps.web.myshare.application.service;

import com.myapps.web.myshare.domain.model.MiniAppKey;
import com.myapps.web.myshare.domain.model.MiniAppStore;
import com.myapps.web.myshare.domain.repository.MiniAppStoreRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@code MINI_APP_STORE} 테이블에 대한 미니앱 JSON 조회 및 업서트(upsert) 트랜잭션을 처리하는 서비스. */
@Service
@Transactional(readOnly = true)
public class MiniAppStoreService {

    private static final String EMPTY_JSON_OBJECT = "{}";

    private final MiniAppStoreRepository repository;
    private final MiniAppJsonValidator jsonValidator;
    private final Clock clock;

    /**
     * MiniAppStoreService를 생성합니다.
     *
     * @param repository 미니앱 저장소 리포지토리
     * @param jsonValidator 원시 JSON 유효성 검증기
     * @param clock 수정 시각 계산용 시계
     */
    public MiniAppStoreService(
            final MiniAppStoreRepository repository,
            final MiniAppJsonValidator jsonValidator,
            final Clock clock) {
        this.repository = repository;
        this.jsonValidator = jsonValidator;
        this.clock = clock;
    }

    /**
     * 지정된 {@code appKey}의 저장된 JSON을 조회합니다.
     *
     * <p>아직 저장된 데이터가 없는 경우 프론트엔드 초기화 편의를 위해 빈 JSON 객체({@code {}})를 반환합니다.
     *
     * @param rawAppKey 요청 경로의 미니앱 키
     * @return 저장된 JSON 문자열 또는 {@code "{}"}
     */
    public String loadJson(final String rawAppKey) {
        final MiniAppKey appKey = MiniAppKey.of(rawAppKey);
        return repository
                .findById(appKey.value())
                .map(MiniAppStore::getDataJson)
                .orElse(EMPTY_JSON_OBJECT);
    }

    /**
     * 미니앱 키와 원시 JSON 바디를 검증한 뒤 DB에 저장(신규 삽입 또는 기존 갱신)합니다.
     *
     * @param rawAppKey 요청 경로의 미니앱 키
     * @param rawJson 원시 JSON 요청 바디
     * @return 검증·저장된 JSON 문자열
     */
    @Transactional
    public String upsertJson(final String rawAppKey, final String rawJson) {
        final MiniAppKey appKey = MiniAppKey.of(rawAppKey);
        final String validatedJson = jsonValidator.validate(rawJson);
        final LocalDateTime now = LocalDateTime.now(clock);

        final Optional<MiniAppStore> existing = repository.findById(appKey.value());
        if (existing.isPresent()) {
            final MiniAppStore store = existing.get();
            store.updateDataJson(validatedJson, now);
            repository.save(store);
        } else {
            repository.save(new MiniAppStore(appKey.value(), validatedJson, now));
        }
        return validatedJson;
    }
}
