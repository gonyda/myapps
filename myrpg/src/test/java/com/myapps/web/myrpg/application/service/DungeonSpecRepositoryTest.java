package com.myapps.web.myrpg.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myapps.web.myrpg.application.dto.DroppedItem;
import com.myapps.web.myrpg.application.dto.DungeonBossSpec;
import com.myapps.web.myrpg.application.dto.DungeonGenerationSpec;
import com.myapps.web.myrpg.application.dto.DungeonMonsterEntry;
import com.myapps.web.myrpg.application.dto.DungeonRewardSpec;
import com.myapps.web.myrpg.application.dto.DungeonSpec;
import com.myapps.web.myrpg.application.exception.DungeonDataException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link DungeonSpecRepository} 단위 테스트.
 *
 * <p>던전 메타데이터 JSON 파일 로드, 알비 던전 확정값 검증, 확장용 던전 파싱 및 비정상 데이터 예외 처리를 검증합니다.
 */
class DungeonSpecRepositoryTest {

    private ObjectMapper objectMapper;
    private DungeonSpecRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = new DungeonSpecRepository(objectMapper);
        repository.init();
    }

    @Nested
    @DisplayName("기본 카탈로그 로드 및 알비 던전 스펙 검증")
    class CatalogLoadAndAlbySpecTests {

        @Test
        @DisplayName("전체 던전 목록을 조회하면 알비, 키아, 라비 3종이 반환된다")
        void should_returnAllDungeons_when_findAllCalled() {
            // given (repository 초기화 완료 상태)

            // when
            final List<DungeonSpec> dungeons = repository.findAll();

            // then
            assertThat(dungeons).hasSize(3);
            assertThat(dungeons)
                    .extracting(DungeonSpec::id)
                    .containsExactly("alby", "ciar", "rabbie");
        }

        @Test
        @DisplayName("알비 던전(alby)의 기본 속성이 요구사항과 일치한다")
        void should_matchAlbyDungeonBasicSpec_when_findById() {
            // given
            final String dungeonId = "alby";

            // when
            final Optional<DungeonSpec> optionalSpec = repository.findById(dungeonId);

            // then
            assertThat(optionalSpec).isPresent();
            final DungeonSpec spec = optionalSpec.get();
            assertThat(spec.id()).isEqualTo("alby");
            assertThat(spec.name()).isEqualTo("알비 던전");
            assertThat(spec.entranceNodeId()).isEqualTo("alby-entrance");
            assertThat(spec.theme()).isEqualTo("dungeon-alby");
            assertThat(spec.implemented()).isTrue();
            assertThat(spec.chainCombatProbability()).isEqualTo(0.10);
        }

        @Test
        @DisplayName("알비 던전의 맵 생성 파라미터(generation)가 요구사항 확정값과 일치한다")
        void should_matchAlbyGenerationSpec_when_verified() {
            // given
            final String dungeonId = "alby";

            // when
            final DungeonSpec spec = repository.getById(dungeonId);
            final DungeonGenerationSpec gen = spec.generation();

            // then
            assertThat(gen.minDistanceToBoss()).isEqualTo(10);
            assertThat(gen.maxDistanceToBoss()).isEqualTo(10);
            assertThat(gen.minTotalRooms()).isEqualTo(20);
            assertThat(gen.maxTotalRooms()).isEqualTo(23);
            assertThat(gen.branchProbability()).isEqualTo(0.40);
            assertThat(gen.maxBranchDepth()).isEqualTo(3);
        }

        @Test
        @DisplayName("알비 던전의 몬스터 풀이 4종(spider, red-spider, goblin, black-spider)으로 구성된다")
        void should_matchAlbyMonsterPool_when_verified() {
            // given
            final String dungeonId = "alby";

            // when
            final DungeonSpec spec = repository.getById(dungeonId);
            final List<DungeonMonsterEntry> pool = spec.monsterPool();

            // then
            assertThat(pool).hasSize(4);
            assertThat(pool.get(0)).isEqualTo(new DungeonMonsterEntry("spider", 1, 2, 40));
            assertThat(pool.get(1)).isEqualTo(new DungeonMonsterEntry("red-spider", 1, 2, 25));
            assertThat(pool.get(2)).isEqualTo(new DungeonMonsterEntry("goblin", 1, 2, 25));
            assertThat(pool.get(3)).isEqualTo(new DungeonMonsterEntry("black-spider", 1, 1, 10));
        }

        @Test
        @DisplayName("알비 던전의 보스 및 보상 스펙이 확정값과 일치한다")
        void should_matchAlbyBossAndRewards_when_verified() {
            // given
            final String dungeonId = "alby";

            // when
            final DungeonSpec spec = repository.getById(dungeonId);
            final DungeonBossSpec boss = spec.boss();
            final DungeonRewardSpec rewards = spec.rewards();

            // then
            assertThat(boss.monsterId()).isEqualTo("giant-spider");
            assertThat(boss.name()).isEqualTo("거대거미");
            assertThat(boss.dialogue()).contains("거대한 거미");

            assertThat(rewards.exp()).isEqualTo(1000);
            assertThat(rewards.gold()).isEqualTo(2000);
            assertThat(rewards.items()).containsExactly(new DroppedItem("hp_potion_30", 3));
        }

        @Test
        @DisplayName("키아 및 라비 던전 템플릿이 implemented=false로 정상 로드된다")
        void should_loadCiarAndRabbieAsNotImplemented_when_queried() {
            // given & when
            final DungeonSpec ciar = repository.getById("ciar");
            final DungeonSpec rabbie = repository.getById("rabbie");

            // then
            assertThat(ciar.implemented()).isFalse();
            assertThat(ciar.generation().minDistanceToBoss()).isEqualTo(12);
            assertThat(ciar.boss().monsterId()).isEqualTo("golem");

            assertThat(rabbie.implemented()).isFalse();
            assertThat(rabbie.generation().minDistanceToBoss()).isEqualTo(14);
            assertThat(rabbie.boss().monsterId()).isEqualTo("succubus");
        }
    }

    @Nested
    @DisplayName("조회 메서드 경계값 테스트")
    class QueryMethodTests {

        @Test
        @DisplayName("존재하지 않는 ID나 null로 조회하면 빈 Optional을 반환한다")
        void should_returnEmptyOptional_when_idNotFoundOrNull() {
            // given
            final String nonExistentId = "non_existent";

            // when
            final Optional<DungeonSpec> notFoundResult = repository.findById(nonExistentId);
            final Optional<DungeonSpec> nullResult = repository.findById(null);

            // then
            assertThat(notFoundResult).isEmpty();
            assertThat(nullResult).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID로 getById 호출 시 DungeonDataException이 발생한다")
        void should_throwException_when_getByIdNotFound() {
            // given
            final String nonExistentId = "unknown_dungeon";

            // when & then
            assertThatThrownBy(() -> repository.getById(nonExistentId))
                    .isInstanceOf(DungeonDataException.class)
                    .hasMessageContaining("던전 스펙을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("데이터 검증 및 예외 케이스 테스트")
    class ValidationAndExceptionTests {

        @Test
        @DisplayName("최상위 구조에 dungeons 배열이 없으면 DungeonDataException이 발생한다")
        void should_throwException_when_dungeonsArrayMissing() {
            // given
            final String invalidJson = "{\"invalidKey\": []}";
            final InputStream inputStream =
                    new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

            // when & then
            assertThatThrownBy(() -> repository.loadFromStream(inputStream))
                    .isInstanceOf(DungeonDataException.class)
                    .hasMessageContaining("'dungeons' 배열이 없거나");
        }

        @Test
        @DisplayName("던전 ID가 중복되면 DungeonDataException이 발생한다")
        void should_throwException_when_duplicateDungeonId() {
            // given
            final String duplicateJson =
                    """
                    {
                      "dungeons": [
                        {
                          "id": "alby",
                          "name": "알비 던전 1",
                          "entranceNodeId": "alby-entrance",
                          "theme": "dungeon-alby",
                          "implemented": false,
                          "generation": {
                            "minDistanceToBoss": 5,
                            "maxDistanceToBoss": 5,
                            "minTotalRooms": 10,
                            "maxTotalRooms": 12,
                            "branchProbability": 0.3,
                            "maxBranchDepth": 2
                          },
                          "monsterPool": [],
                          "chainCombatProbability": 0.1,
                          "boss": { "monsterId": "boss1", "name": "보스1" },
                          "rewards": { "exp": 100, "gold": 100, "items": [] }
                        },
                        {
                          "id": "alby",
                          "name": "알비 던전 2",
                          "entranceNodeId": "alby-entrance",
                          "theme": "dungeon-alby",
                          "implemented": false,
                          "generation": {
                            "minDistanceToBoss": 5,
                            "maxDistanceToBoss": 5,
                            "minTotalRooms": 10,
                            "maxTotalRooms": 12,
                            "branchProbability": 0.3,
                            "maxBranchDepth": 2
                          },
                          "monsterPool": [],
                          "chainCombatProbability": 0.1,
                          "boss": { "monsterId": "boss2", "name": "보스2" },
                          "rewards": { "exp": 100, "gold": 100, "items": [] }
                        }
                      ]
                    }
                    """;
            final InputStream inputStream =
                    new ByteArrayInputStream(duplicateJson.getBytes(StandardCharsets.UTF_8));

            // when & then
            assertThatThrownBy(() -> repository.loadFromStream(inputStream))
                    .isInstanceOf(DungeonDataException.class)
                    .hasMessageContaining("중복됩니다");
        }

        @Test
        @DisplayName("생성 파라미터에서 minDistanceToBoss가 1 미만이면 예외가 발생한다")
        void should_throwException_when_minDistanceLessThanOne() {
            // given
            final String invalidJson =
                    createSingleDungeonJson("alby", false, 0, 5, 10, 12, 0.3, 2, List.of(), 0.1);
            final InputStream inputStream =
                    new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

            // when & then
            assertThatThrownBy(() -> repository.loadFromStream(inputStream))
                    .isInstanceOf(DungeonDataException.class)
                    .hasMessageContaining("minDistanceToBoss는 1 이상");
        }

        @Test
        @DisplayName("생성 파라미터에서 maxDistanceToBoss가 minDistanceToBoss보다 작으면 예외가 발생한다")
        void should_throwException_when_maxDistanceLessThanMinDistance() {
            // given
            final String invalidJson =
                    createSingleDungeonJson("alby", false, 10, 8, 15, 18, 0.3, 2, List.of(), 0.1);
            final InputStream inputStream =
                    new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

            // when & then
            assertThatThrownBy(() -> repository.loadFromStream(inputStream))
                    .isInstanceOf(DungeonDataException.class)
                    .hasMessageContaining("maxDistanceToBoss가 minDistanceToBoss보다 작습니다");
        }

        @Test
        @DisplayName("생성 파라미터에서 minTotalRooms가 maxDistanceToBoss 이하이면 예외가 발생한다")
        void should_throwException_when_minTotalRoomsNotGreaterThanMaxDistance() {
            // given
            final String invalidJson =
                    createSingleDungeonJson("alby", false, 10, 10, 10, 12, 0.3, 2, List.of(), 0.1);
            final InputStream inputStream =
                    new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

            // when & then
            assertThatThrownBy(() -> repository.loadFromStream(inputStream))
                    .isInstanceOf(DungeonDataException.class)
                    .hasMessageContaining("minTotalRooms는 maxDistanceToBoss보다 커야 합니다");
        }

        @Test
        @DisplayName("구현된 던전(implemented=true)의 monsterPool이 비어있으면 예외가 발생한다")
        void should_throwException_when_implementedDungeonHasEmptyMonsterPool() {
            // given
            final String invalidJson =
                    createSingleDungeonJson("alby", true, 10, 10, 20, 23, 0.4, 3, List.of(), 0.1);
            final InputStream inputStream =
                    new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

            // when & then
            assertThatThrownBy(() -> repository.loadFromStream(inputStream))
                    .isInstanceOf(DungeonDataException.class)
                    .hasMessageContaining("'monsterPool' 배열이 비어있습니다");
        }

        @Test
        @DisplayName("확률 필드가 0.0~1.0 범위를 벗어나면 예외가 발생한다")
        void should_throwException_when_probabilityOutOfRange() {
            // given
            final String invalidJson =
                    createSingleDungeonJson("alby", false, 10, 10, 20, 23, 1.5, 3, List.of(), 0.1);
            final InputStream inputStream =
                    new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

            // when & then
            assertThatThrownBy(() -> repository.loadFromStream(inputStream))
                    .isInstanceOf(DungeonDataException.class)
                    .hasMessageContaining("0.0~1.0 사이");
        }

        private String createSingleDungeonJson(
                final String id,
                final boolean implemented,
                final int minDistance,
                final int maxDistance,
                final int minRooms,
                final int maxRooms,
                final double branchProb,
                final int branchDepth,
                final List<String> monsterPoolEntries,
                final double chainCombatProb) {
            final String poolString = String.join(",", monsterPoolEntries);
            return """
                    {
                      "dungeons": [
                        {
                          "id": "%s",
                          "name": "테스트 던전",
                          "entranceNodeId": "test-entrance",
                          "theme": "dungeon-test",
                          "implemented": %b,
                          "generation": {
                            "minDistanceToBoss": %d,
                            "maxDistanceToBoss": %d,
                            "minTotalRooms": %d,
                            "maxTotalRooms": %d,
                            "branchProbability": %f,
                            "maxBranchDepth": %d
                          },
                          "monsterPool": [%s],
                          "chainCombatProbability": %f,
                          "boss": { "monsterId": "boss-id", "name": "보스이름" },
                          "rewards": { "exp": 100, "gold": 100, "items": [] }
                        }
                      ]
                    }
                    """
                    .formatted(
                            id,
                            implemented,
                            minDistance,
                            maxDistance,
                            minRooms,
                            maxRooms,
                            branchProb,
                            branchDepth,
                            poolString,
                            chainCombatProb);
        }
    }
}
